#!/bin/bash
#
# DT-3990 verification runbook, §4 "PI details, and the new PATCH authorization" — checks A-1 to A-11.
#
# Two things are under test and they pull in opposite directions:
#   * A-1 to A-3: PATCH now demands ownership on top of the role. StudyResource.patchStudyById calls
#     isCreatorCustodianOrAdmin after the visibility check, so a caller holding a study-editing role
#     who owns nothing is refused where develop let them through. A-2 is the regression half - the
#     new gate must not have locked out real editors.
#   * A-4 to A-9: the four PI fields are columns, not study properties, so they follow JSON
#     semantics rather than the blank-string-deletes convention: absent is a no-op, explicit null
#     clears, empty string is normalised to a clear, a value sets.
#
# A SEQUENCE, NOT A MATRIX
#   PATCH short-circuits: StudyPatch.isPatchable returns 304 when nothing in the body differs from
#   what is stored. So every check that expects a 200 has to send a value that actually changes,
#   and A-9 depends on knowing exactly what the preceding checks left behind. The order below is
#   deliberate and the checks are not individually selectable.
#
# WHAT IT WRITES
#   Everything happens on two throwaway studies the script creates - one public, one private - so
#   no real study is patched. A-10 additionally creates a throwaway dataset on the public one, and
#   A-11 registers a brand new study through the API. All of it is removed on exit.
#
#   The synthetic studies exist because A-2 needs a study whose creator AND data custodian are
#   people we control: the @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER}) gate means a creator
#   without one of those roles is refused before ownership is ever consulted, which would read as a
#   regression when it is really a fixture problem.
#
# HOW IT AUTHENTICATES AS FOUR PEOPLE
#   Same mechanism as scripts/verify-study-visibility.sh: the OAUTH2_CLAIM_* headers go straight to
#   the app port, bypassing the proxy, with a bearer token unique per persona and per run. See that
#   script's header for why.
#
# USAGE
#   ./scripts/verify-study-pi-details.sh [OPTION]...
#   Run --help for options. Exits non-zero if any check fails or had to be skipped.
#

set -eu
set -o pipefail

API="${CONSENT_API:-http://localhost:8080}"
DB_CONTAINER="${CONSENT_DB_CONTAINER:-localdb}"
DB_USER="${CONSENT_DB_USER:-consent}"
DB_NAME="${CONSENT_DB_NAME:-consent}"

EMAIL_creator=""
EMAIL_custodian=""
EMAIL_outsider=""
EMAIL_admin=""
SKIP_CONVERT="false"
SKIP_REGISTER="false"
VERBOSE="false"

if [ -t 1 ] && [ "$(tput colors 2>/dev/null || echo 0)" -ge 8 ]; then
  BLD="$(tput bold)"; RED="$(tput setaf 1)"; GRN="$(tput setaf 2)"; YLW="$(tput setaf 3)"; DIM="$(tput setaf 8)"; RST="$(tput sgr0)"
else
  BLD=""; RED=""; GRN=""; YLW=""; DIM=""; RST=""
fi

usage() {
  cat <<EOF
Usage: $0 [OPTION]...
Verify DT-3990 runbook checks A-1 through A-11 against a running local stack.

  --api URL          Consent API base. Default: $API
                     Must be the app port, NOT the :27443 proxy.
  --db-container NAME  Postgres container. Default: $DB_CONTAINER
  --creator EMAIL    Study-editing role, not Admin. Creator of the throwaway studies (A-2).
  --custodian EMAIL  Study-editing role, not Admin. Data custodian of the public one (A-2).
  --outsider EMAIL   Study-editing role, related to neither study (A-1, A-3).
  --admin EMAIL      Admin. Runs A-2's third case and everything from A-4 on.
  --skip-convert     Skip A-10 (dataset->study conversion; touches Elasticsearch).
  --skip-register    Skip A-11 (registers a study through /api/dataset/v3).
  --verbose          Echo every request and its body.
  --help             Display this help and exit.

Environment overrides: CONSENT_API, CONSENT_DB_CONTAINER, CONSENT_DB_USER, CONSENT_DB_NAME.
EOF
  exit 0
}

error() { printf "${RED}ERROR: %s${RST}\n" "$1" >&2; exit 1; }
warn()  { printf "${YLW}WARN: %s${RST}\n" "$1" >&2; }
head1() { printf "\n${BLD}%s${RST}\n" "$1"; }

while [ $# -gt 0 ]; do
  case "$1" in
    --api) API="$2"; shift 2;;
    --db-container) DB_CONTAINER="$2"; shift 2;;
    --creator) EMAIL_creator="$2"; shift 2;;
    --custodian) EMAIL_custodian="$2"; shift 2;;
    --outsider) EMAIL_outsider="$2"; shift 2;;
    --admin) EMAIL_admin="$2"; shift 2;;
    --skip-convert) SKIP_CONVERT="true"; shift;;
    --skip-register) SKIP_REGISTER="true"; shift;;
    --verbose) VERBOSE="true"; shift;;
    --help) usage;;
    *) error "Unknown option: $1. Try --help.";;
  esac
done
API="${API%/}"

for tool in curl jq docker; do
  command -v "$tool" >/dev/null 2>&1 || error "$tool is required"
done

psql_q() { docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -At -F'|' -v ON_ERROR_STOP=1; }
sql1() { echo "$1" | psql_q; }
sql_quote() { printf "'%s'" "$(printf '%s' "$1" | sed "s/'/''/g")"; }

docker exec "$DB_CONTAINER" true >/dev/null 2>&1 \
  || error "Container '$DB_CONTAINER' is not running. Bring the stack up first."
echo "SELECT 1;" | psql_q >/dev/null 2>&1 \
  || error "Cannot query database '$DB_NAME' as '$DB_USER' in container '$DB_CONTAINER'."

case "$API" in
  *:27443*|*local.dsde-dev*) warn "--api looks like the Apache proxy, which rewrites OAUTH2_CLAIM_* headers. Use the app port (http://localhost:8080).";;
esac
curl -s -o /dev/null --max-time 10 "$API/api/status" || error "No response from $API — is the app container up?"

WORK="$(mktemp -d)"
S_PUB=""
S_PRIV=""
CONVERT_DATASET=""
REGISTERED_STUDY=""

drop_study() { # drop_study STUDY_ID — datasets don't cascade from study, so go bottom-up
  [ -n "$1" ] || return 0
  {
    echo "DELETE FROM dataset_property WHERE dataset_id IN (SELECT dataset_id FROM dataset WHERE study_id = $1);"
    echo "DELETE FROM dataset WHERE study_id = $1;"
    echo "DELETE FROM study_property WHERE study_id = $1;"
    echo "DELETE FROM study WHERE study_id = $1;"
  } | psql_q >/dev/null 2>&1 || warn "Could not fully remove study $1 — check by hand."
}

cleanup() {
  local status=$?
  drop_study "$REGISTERED_STUDY"
  drop_study "$S_PUB"
  drop_study "$S_PRIV"
  rm -rf "$WORK"
  exit $status
}
trap cleanup EXIT INT TERM

# ---------------------------------------------------------------- fixtures

# PATCH is @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER}). Every persona needs one of those or
# they never reach the ownership check. Admins are excluded here because an admin passes
# isCreatorCustodianOrAdmin outright, which would make A-1 and the creator/custodian halves of A-2
# pass for the wrong reason. Service-account and test-harness domains are pushed to the back: a Sam
# registration attempt on those is a known source of 500s during authentication.
editor_pool() {
  echo "
    SELECT u.email FROM users u
    WHERE EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_id = u.user_id AND ur.role_id IN (2, 8))
      AND NOT EXISTS (SELECT 1 FROM user_role a WHERE a.user_id = u.user_id AND a.role_id = 4)
    ORDER BY (split_part(u.email, '@', 2) LIKE '%gserviceaccount.com'
              OR split_part(u.email, '@', 2) LIKE '%firecloud.org'), u.user_id
    LIMIT 12;" | psql_q
}

discover_editors() {
  local pool candidate
  mapfile -t pool < <(editor_pool)
  for candidate in "${pool[@]}"; do
    case "$candidate" in
      "$EMAIL_creator"|"$EMAIL_custodian"|"$EMAIL_outsider") continue;;
    esac
    if   [ -z "$EMAIL_creator" ];   then EMAIL_creator="$candidate"
    elif [ -z "$EMAIL_custodian" ]; then EMAIL_custodian="$candidate"
    elif [ -z "$EMAIL_outsider" ];  then EMAIL_outsider="$candidate"
    else break
    fi
  done
  [ -n "$EMAIL_creator" ]   || error "No non-admin user with a study-editing role found. Pass --creator."
  [ -n "$EMAIL_custodian" ] || error "Could not pick a second, distinct editor. Pass --custodian."
  [ -n "$EMAIL_outsider" ]  || error "Could not pick a third, distinct editor. Pass --outsider."
}

discover_admin() {
  [ -n "$EMAIL_admin" ] && return 0
  EMAIL_admin="$(echo "
    SELECT u.email FROM users u
    JOIN user_role ur ON ur.user_id = u.user_id AND ur.role_id = 4
    WHERE NOT EXISTS (SELECT 1 FROM library_card lc WHERE lc.user_id = u.user_id)
      AND split_part(u.email, '@', 2) NOT LIKE '%gserviceaccount.com'
    ORDER BY u.user_id LIMIT 1;" | psql_q)"
  [ -n "$EMAIL_admin" ] || error "No Admin user found. Pass --admin."
}

user_id_for() { sql1 "SELECT user_id FROM users WHERE lower(email) = lower($(sql_quote "$1"));"; }

head1 "Fixtures"
discover_editors
discover_admin

UID_creator="$(user_id_for "$EMAIL_creator")"
[ -n "$UID_creator" ] || error "$EMAIL_creator has no row in users."

for persona in creator custodian outsider admin; do
  eval "e=\${EMAIL_$persona}"
  [ -n "$(user_id_for "$e")" ] || error "$persona ($e) has no row in users."
done

# Two real institutions: A-4 sets the first, A-9 re-sends it, and having a second keeps A-4's
# assertion honest (a hardcoded id could coincide with whatever was already there — nothing is,
# on a study we just created, but the ids are also used for the A-8 miss).
INST_A="$(sql1 "SELECT institution_id FROM institution ORDER BY institution_id LIMIT 1;")"
INST_B="$(sql1 "SELECT institution_id FROM institution ORDER BY institution_id OFFSET 1 LIMIT 1;")"
INST_MISSING="$(sql1 "SELECT max(institution_id) + 1000 FROM institution;")"
[ -n "$INST_A" ] && [ -n "$INST_B" ] || error "Need at least two institution rows."
INST_A_NAME="$(sql1 "SELECT institution_name FROM institution WHERE institution_id = $INST_A;")"

make_study() { # make_study PUBLIC_VISIBILITY
  sql1 "
    WITH ins AS (
      INSERT INTO study (name, description, data_types, pi_name, public_visibility,
                         uuid, create_user_id, create_date)
      VALUES ('DT-3990 PI verification ' || gen_random_uuid(),
              'Throwaway study created by verify-study-pi-details.sh', ARRAY['Verification'],
              'DT-3990 Verification PI', $1, gen_random_uuid(), $UID_creator, now())
      RETURNING study_id
    ) SELECT study_id FROM ins;"
}

S_PUB="$(make_study true)"
S_PRIV="$(make_study false)"
[ -n "$S_PUB" ] && [ -n "$S_PRIV" ] || error "Could not create the throwaway studies."

# isCreatorOrCustodian reads dataCustodianEmail as a JSON array of strings, stored with
# PropertyType.Json — mirror how DatasetService writes it.
sql1 "
  INSERT INTO study_property (study_id, key, type, value)
  VALUES ($S_PUB, 'dataCustodianEmail', 'Json', $(sql_quote "[\"$EMAIL_custodian\"]"));" >/dev/null

LIBRARY_CARDS_BEFORE="$(sql1 "SELECT count(*) FROM library_card;")"

printf "  %-22s %s\n" "API"              "$API"
printf "  %-22s %s\n" "public study"     "$S_PUB (throwaway)"
printf "  %-22s %s\n" "private study"    "$S_PRIV (throwaway)"
printf "  %-22s %s\n" "creator"          "$EMAIL_creator"
printf "  %-22s %s\n" "custodian"        "$EMAIL_custodian"
printf "  %-22s %s\n" "outsider"         "$EMAIL_outsider"
printf "  %-22s %s\n" "admin"            "$EMAIL_admin"
printf "  %-22s %s / %s / %s\n" "institutions (A4/A9/A8)" "$INST_A" "$INST_B" "$INST_MISSING"

# ---------------------------------------------------------------- plumbing

RUN_ID="dt3990a-$$-$(date +%s)"
LAST_CODE=""; LAST_BODY=""; LAST_CURL=""

# request METHOD EMAIL LABEL PATH [BODY]
request() {
  local method="$1" email="$2" label="$3" path="$4"
  local token="${RUN_ID}-${label}"
  local body_file="$WORK/body"
  local -a args=(-s -o "$body_file" -w '%{http_code}' --max-time 90 -X "$method"
    -H "Authorization: Bearer $token"
    -H "OAUTH2_CLAIM_email: $email"
    -H "OAUTH2_CLAIM_name: $label"
    -H "OAUTH2_CLAIM_access_token: $token"
    -H "OAUTH2_CLAIM_aud: $RUN_ID"
    -H 'Accept: application/json')

  LAST_CURL="curl -X $method -H 'Authorization: Bearer $token' -H 'OAUTH2_CLAIM_email: $email' -H 'OAUTH2_CLAIM_access_token: $token' -H 'OAUTH2_CLAIM_aud: $RUN_ID'"
  if [ "$#" -ge 5 ]; then
    args+=(-H 'Content-Type: application/json' --data-binary "$5")
    LAST_CURL="$LAST_CURL -H 'Content-Type: application/json' --data-binary '$5'"
  fi
  LAST_CURL="$LAST_CURL '$API/$path'"

  LAST_CODE="$(curl "${args[@]}" "$API/$path" || echo "000")"
  LAST_BODY="$(head -c 4000 "$body_file" 2>/dev/null | tr '\n' ' ')"
  if [ "$VERBOSE" = "true" ]; then
    printf "${DIM}    %-6s %-10s %-46s -> %s %s${RST}\n" \
      "$method" "$label" "${path:0:46}" "$LAST_CODE" "${LAST_BODY:0:140}" >&2
  fi
  return 0
}

json() { printf '%s' "$LAST_BODY" | jq -r "$1" 2>/dev/null || printf ''; }

PASS=0; FAIL=0; SKIP=0
FAILURES=(); NOTES=()

ok()   { PASS=$((PASS + 1)); printf "  ${GRN}PASS${RST}  %-6s %s\n" "$1" "$2"; }
bad()  { FAIL=$((FAIL + 1)); printf "  ${RED}FAIL${RST}  %-6s %s\n" "$1" "$2"
         FAILURES+=("$1 $2${3:+ — ${3:0:200}}"); [ -n "${LAST_CURL:-}" ] && FAILURES+=("        repro: $LAST_CURL"); }
skip() { SKIP=$((SKIP + 1)); printf "  ${YLW}SKIP${RST}  %-6s %s\n" "$1" "$2"; }
note() { NOTES+=("$1"); }

expect() {
  if [ "$3" = "$4" ]; then ok "$1" "$2"; else bad "$1" "$2 (expected '$3', got '$4')" "$LAST_BODY"; fi
}

pi_columns() {
  sql1 "SELECT coalesce(pi_institution_id::text,'-') || '|' || coalesce(pi_orcid,'-') || '|' ||
               coalesce(pi_linkedin_url,'-') || '|' || coalesce(pi_website_url,'-')
        FROM study WHERE study_id = $S_PUB;"
}

PUB_PATH="api/dataset/study/$S_PUB"

# ---------------------------------------------------------------- persona smoke test

head1 "Persona smoke test — GET the public study as each caller"
declare -A PERSONA_OK=()
for persona in creator custodian outsider admin; do
  eval "email=\${EMAIL_$persona}"
  request GET "$email" "$persona" "$PUB_PATH"
  if [ "$LAST_CODE" = "200" ]; then
    PERSONA_OK[$persona]="true"
    printf "  %-10s ${GRN}%-5s${RST} %s\n" "$persona" "200" "$email"
  else
    PERSONA_OK[$persona]="false"
    printf "  %-10s ${RED}%-5s${RST} %s\n" "$persona" "$LAST_CODE" "$email"
    printf "             ${DIM}%s${RST}\n" "${LAST_BODY:0:160}"
    warn "$persona ($email) cannot even read the study; its checks will be skipped."
  fi
done

# ---------------------------------------------------------------- A-1, A-3 (no writes)

head1 "A-1 / A-3 · The new ownership gate"

DESC_BEFORE="$(sql1 "SELECT description FROM study WHERE study_id = $S_PUB;")"

if [ "${PERSONA_OK[outsider]}" = "true" ]; then
  request PATCH "$EMAIL_outsider" outsider "$PUB_PATH" '{"description":"outsider attempt"}'
  expect "A-1a" "editor who owns nothing is refused on a PUBLIC study" "403" "$LAST_CODE"
  expect "A-1b" "  ...with the ownership message" \
    "Study with ID $S_PUB is not updatable" "$(json '.message')"
  expect "A-1c" "  ...and nothing was written" "$DESC_BEFORE" \
    "$(sql1 "SELECT description FROM study WHERE study_id = $S_PUB;")"

  # Visibility is checked before ownership, so an invisible study must 404 rather than confirm it
  # exists with a 403.
  request PATCH "$EMAIL_outsider" outsider "api/dataset/study/$S_PRIV" '{"description":"outsider attempt"}'
  expect "A-3a" "same caller on a study they cannot see" "404" "$LAST_CODE"
  expect "A-3b" "  ...'Study not found', not the ownership message" "Study not found" "$(json '.message')"
else
  skip "A-1" "outsider persona unusable"
  skip "A-3" "outsider persona unusable"
fi

# ---------------------------------------------------------------- A-2 (the regression half)

head1 "A-2 · Real editors must not have lost access"

for persona in creator custodian admin; do
  eval "email=\${EMAIL_$persona}"
  if [ "${PERSONA_OK[$persona]}" != "true" ]; then
    skip "A-2" "$persona persona unusable"
    continue
  fi
  # isPatchable short-circuits to 304 when nothing differs, so each caller must send a genuinely
  # new description.
  want="patched by $persona at $RUN_ID"
  request PATCH "$email" "$persona" "$PUB_PATH" "{\"description\":$(printf '%s' "$want" | jq -Rs .)}"
  if [ "$LAST_CODE" = "200" ]; then
    expect "A-2" "$persona can PATCH — and the write landed" "$want" \
      "$(sql1 "SELECT description FROM study WHERE study_id = $S_PUB;")"
  else
    bad "A-2" "$persona was refused ($LAST_CODE) — the new ownership gate locked out a legitimate editor" "$LAST_BODY"
  fi
done

# ---------------------------------------------------------------- A-4 .. A-9 as admin

if [ "${PERSONA_OK[admin]}" != "true" ]; then
  warn "Admin persona unusable — A-4 through A-11 skipped."
  for a in A-4 A-5 A-6 A-7 A-8 A-9 A-10 A-11; do skip "$a" "admin persona unusable"; done
else

head1 "A-4 · Setting the PI fields"

ORCID="0000-0002-1825-0097"
LINKEDIN="https://www.linkedin.com/in/example"
WEBSITE="https://lab.example.edu"

request PATCH "$EMAIL_admin" admin "$PUB_PATH" "{
  \"piInstitutionId\": $INST_A,
  \"piOrcid\": \"$ORCID\",
  \"piLinkedinUrl\": \"$LINKEDIN\",
  \"piWebsiteUrl\": \"$WEBSITE\"
}"
expect "A-4a" "PATCH all four PI fields" "200" "$LAST_CODE"

request GET "$EMAIL_admin" admin "$PUB_PATH"
expect "A-4b" "GET returns piInstitution as an object with its id" "$INST_A" "$(json '.piInstitution.id')"
expect "A-4c" "  ...and its name" "$INST_A_NAME" "$(json '.piInstitution.name')"
expect "A-4d" "  ...piOrcid round-trips" "$ORCID" "$(json '.piOrcid')"
expect "A-4e" "  ...piLinkedinUrl round-trips" "$LINKEDIN" "$(json '.piLinkedinUrl')"
expect "A-4f" "  ...piWebsiteUrl round-trips" "$WEBSITE" "$(json '.piWebsiteUrl')"
expect "A-4g" "  ...and the columns agree" "$INST_A|$ORCID|$LINKEDIN|$WEBSITE" "$(pi_columns)"

head1 "A-5 · An absent field is never a clear"

PI_SNAPSHOT="$(pi_columns)"
request PATCH "$EMAIL_admin" admin "$PUB_PATH" '{"description":"description-only patch, PI fields absent"}'
expect "A-5a" "PATCH description alone" "200" "$LAST_CODE"
expect "A-5b" "  ...all four PI columns unchanged" "$PI_SNAPSHOT" "$(pi_columns)"

head1 "A-6 · null and empty string both clear"

request PATCH "$EMAIL_admin" admin "$PUB_PATH" '{"piOrcid": null}'
expect "A-6a" "PATCH piOrcid to explicit null" "200" "$LAST_CODE"
expect "A-6b" "  ...pi_orcid is SQL NULL" "t" \
  "$(sql1 "SELECT pi_orcid IS NULL FROM study WHERE study_id = $S_PUB;")"

request PATCH "$EMAIL_admin" admin "$PUB_PATH" "{\"piOrcid\": \"$ORCID\"}"
expect "A-6c" "re-set piOrcid to a fresh value" "200" "$LAST_CODE"

request PATCH "$EMAIL_admin" admin "$PUB_PATH" '{"piOrcid": ""}'
expect "A-6d" "PATCH piOrcid to an empty string" "200" "$LAST_CODE"
expect "A-6e" "  ...pi_orcid is SQL NULL, not the empty string" "t" \
  "$(sql1 "SELECT pi_orcid IS NULL FROM study WHERE study_id = $S_PUB;")"

head1 "A-7 · Unknown fields and coerced types"

request PATCH "$EMAIL_admin" admin "$PUB_PATH" '{"noSuchField": 1}'
expect "A-7a" "unknown field rejected (FAIL_ON_UNKNOWN_PROPERTIES)" "400" "$LAST_CODE"

request PATCH "$EMAIL_admin" admin "$PUB_PATH" '{"piOrcid": 12345}'
expect "A-7b" "int into a string field rejected (ForceStringDeserializer)" "400" "$LAST_CODE"
note "A-7: the type rejection surfaces as: $(json '.message' | head -c 150)"
expect "A-7c" "  ...and nothing was written" "t" \
  "$(sql1 "SELECT pi_orcid IS NULL FROM study WHERE study_id = $S_PUB;")"

head1 "A-8 · A PI institution that doesn't exist"

INST_BEFORE="$(sql1 "SELECT coalesce(pi_institution_id::text,'-') FROM study WHERE study_id = $S_PUB;")"
request PATCH "$EMAIL_admin" admin "$PUB_PATH" "{\"piInstitutionId\": $INST_MISSING}"
case "$LAST_CODE" in
  400)
    ok "A-8a" "unknown piInstitutionId rejected with a 400"
    note "A-8: {\"piInstitutionId\": $INST_MISSING} returns 400 — the value is validated before it reaches the database. The runbook expected a 500 from fk_study_pi_institution, so this is better than predicted."
    ;;
  500)
    ok "A-8a" "unknown piInstitutionId recorded: 500"
    note "A-8: {\"piInstitutionId\": $INST_MISSING} returns 500, not 400 — fk_study_pi_institution rejects it at the database and the constraint violation surfaces as a server error. A validation gap, as the runbook predicted: not a blocker, but worth a ticket."
    ;;
  *)
    bad "A-8a" "unknown piInstitutionId gave neither 400 nor 500 (got $LAST_CODE)" "$LAST_BODY"
    ;;
esac
expect "A-8b" "  ...pi_institution_id unchanged" "$INST_BEFORE" \
  "$(sql1 "SELECT coalesce(pi_institution_id::text,'-') FROM study WHERE study_id = $S_PUB;")"

head1 "A-9 · A patch that changes nothing is a 304"

# Echo the study back at itself. Read the current values rather than assuming, because A-6 cleared
# the orcid and A-5 rewrote the description.
request GET "$EMAIL_admin" admin "$PUB_PATH"
CUR_DESC="$(json '.description')"
CUR_NAME="$(json '.name')"
CUR_INST="$(json '.piInstitution.id')"
CUR_LINKEDIN="$(json '.piLinkedinUrl')"
CUR_WEBSITE="$(json '.piWebsiteUrl')"
UPDATED_BEFORE="$(sql1 "SELECT coalesce(update_date::text,'-') FROM study WHERE study_id = $S_PUB;")"

request PATCH "$EMAIL_admin" admin "$PUB_PATH" "{
  \"name\": $(printf '%s' "$CUR_NAME" | jq -Rs .),
  \"description\": $(printf '%s' "$CUR_DESC" | jq -Rs .),
  \"piInstitutionId\": $CUR_INST,
  \"piLinkedinUrl\": $(printf '%s' "$CUR_LINKEDIN" | jq -Rs .),
  \"piWebsiteUrl\": $(printf '%s' "$CUR_WEBSITE" | jq -Rs .)
}"
expect "A-9a" "re-sending the stored values" "304" "$LAST_CODE"
expect "A-9b" "  ...update_date untouched" "$UPDATED_BEFORE" \
  "$(sql1 "SELECT coalesce(update_date::text,'-') FROM study WHERE study_id = $S_PUB;")"

# The PI columns have to participate in isPatchable, not just the legacy fields.
request PATCH "$EMAIL_admin" admin "$PUB_PATH" "{\"piWebsiteUrl\": $(printf '%s' "$CUR_WEBSITE" | jq -Rs .)}"
expect "A-9c" "re-sending one PI field alone is also a 304" "304" "$LAST_CODE"

# ---------------------------------------------------------------- A-10

head1 "A-10 · PI details survive the dataset→study conversion"

if [ "$SKIP_CONVERT" = "true" ]; then
  skip "A-10" "--skip-convert"
else
  # Restore a full set of PI values first: a conversion that wipes nulls proves nothing.
  request PATCH "$EMAIL_admin" admin "$PUB_PATH" "{\"piOrcid\": \"$ORCID\", \"piInstitutionId\": $INST_A}"
  PI_SNAPSHOT="$(pi_columns)"

  # A throwaway dataset on the same study. data_use stays NULL and the conversion body carries no
  # dataUse or dacId, so the ontology service is never called.
  #
  # The alias is read back from the insert rather than chosen: a BEFORE INSERT trigger,
  # dataset_alias_allocate, assigns it and overrides whatever the statement supplies. Computing
  # max(alias)+1 up front yields an identifier that resolves to nothing.
  read -r CONVERT_DATASET CONVERT_ALIAS <<< "$(sql1 "
    WITH ins AS (
      INSERT INTO dataset (name, create_user_id, create_date, study_id)
      VALUES ('DT-3990 PI verification dataset ' || gen_random_uuid(),
              $UID_creator, now(), $S_PUB)
      RETURNING dataset_id, alias
    ) SELECT dataset_id || ' ' || alias::integer FROM ins;")"
  [ -n "$CONVERT_DATASET" ] && [ -n "$CONVERT_ALIAS" ] \
    || error "Could not create the throwaway dataset for A-10."
  IDENTIFIER="DUOS-$(printf '%06d' "$CONVERT_ALIAS")"

  # updateStudyFromConversion picks its branch on findStudyByName(conversion.getName()) — not on
  # the dataset's study — so the payload has to carry the study's exact name or the conversion
  # creates a second study instead of updating this one. The other four are echoed because the
  # update branch writes them straight through: omitting them would null out NOT NULL columns.
  CONV_PAYLOAD="$(sql1 "
    SELECT json_build_object(
             'name', s.name,
             'description', s.description,
             'dataTypes', array_to_json(s.data_types),
             'publicVisibility', s.public_visibility,
             'piName', s.pi_name)::text
    FROM study s WHERE s.study_id = $S_PUB;")"

  request PUT "$EMAIL_admin" admin "api/dataset/study/convert/$IDENTIFIER" "$CONV_PAYLOAD"
  if [ "$LAST_CODE" = "200" ]; then
    ok "A-10a" "conversion accepted for $IDENTIFIER"
    expect "A-10b" "  ...all four PI columns survived" "$PI_SNAPSHOT" "$(pi_columns)"
    expect "A-10c" "  ...and it updated this study rather than creating a second one" "1" \
      "$(sql1 "SELECT count(*) FROM study WHERE name = (SELECT name FROM study WHERE study_id = $S_PUB);")"
  else
    bad "A-10a" "conversion returned $LAST_CODE" "$LAST_BODY"
  fi
fi

# ---------------------------------------------------------------- A-11

head1 "A-11 · A brand-new registration writes both places"

if [ "$SKIP_REGISTER" = "true" ]; then
  skip "A-11" "--skip-register"
else
  REG_NAME="DT-3990 PI registration $RUN_ID"
  REG_PAYLOAD="$(jq -nc --arg n "$REG_NAME" --arg cg "cg-$RUN_ID" --argjson inst "$INST_A" '{
    studyName: $n,
    studyDescription: "Throwaway registration created by verify-study-pi-details.sh",
    dataTypes: ["Verification"],
    publicVisibility: true,
    nihAnvilUse: "I am not NHGRI funded and do not plan to store data in AnVIL",
    piName: "DT-3990 Verification PI",
    piInstitution: $inst,
    consentGroups: [{consentGroupName: $cg, accessManagement: "open", numberOfParticipants: 10}]
  }')"

  REG_TOKEN="${RUN_ID}-register"
  REG_CODE="$(curl -s -o "$WORK/reg" -w '%{http_code}' --max-time 90 -X POST \
    -H "Authorization: Bearer $REG_TOKEN" \
    -H "OAUTH2_CLAIM_email: $EMAIL_admin" \
    -H "OAUTH2_CLAIM_name: register" \
    -H "OAUTH2_CLAIM_access_token: $REG_TOKEN" \
    -H "OAUTH2_CLAIM_aud: $RUN_ID" \
    -F "dataset=$REG_PAYLOAD" \
    "$API/api/dataset/v3" || echo "000")"
  LAST_BODY="$(head -c 4000 "$WORK/reg" 2>/dev/null | tr '\n' ' ')"
  LAST_CURL="curl -X POST -F 'dataset=<payload>' '$API/api/dataset/v3'"

  REGISTERED_STUDY="$(sql1 "SELECT study_id FROM study WHERE name = $(sql_quote "$REG_NAME");")"
  if [ "$REG_CODE" = "200" ] || [ "$REG_CODE" = "201" ]; then
    ok "A-11a" "registration accepted ($REG_CODE), study $REGISTERED_STUDY"
    expect "A-11b" "  ...study.pi_institution_id written" "$INST_A" \
      "$(sql1 "SELECT coalesce(pi_institution_id::text,'-') FROM study WHERE study_id = $REGISTERED_STUDY;")"
    expect "A-11c" "  ...piInstitution study_property written too" "$INST_A" \
      "$(sql1 "SELECT coalesce(value,'-') FROM study_property WHERE study_id = $REGISTERED_STUDY AND key = 'piInstitution';")"
  else
    bad "A-11a" "registration returned $REG_CODE" "$LAST_BODY"
  fi
fi

fi  # admin usable

# ---------------------------------------------------------------- summary

LIBRARY_CARDS_AFTER="$(sql1 "SELECT count(*) FROM library_card;")"
[ "$LIBRARY_CARDS_AFTER" = "$LIBRARY_CARDS_BEFORE" ] \
  || note "library_card rows went from $LIBRARY_CARDS_BEFORE to $LIBRARY_CARDS_AFTER during this run."

head1 "Summary"
printf "  ${GRN}%d passed${RST}, ${RED}%d failed${RST}, ${YLW}%d skipped${RST}\n" "$PASS" "$FAIL" "$SKIP"
printf "  ${DIM}library_card rows: %s before, %s after${RST}\n" "$LIBRARY_CARDS_BEFORE" "$LIBRARY_CARDS_AFTER"

if [ "${#FAILURES[@]}" -gt 0 ]; then
  head1 "Failures"
  for f in "${FAILURES[@]}"; do printf "  %s\n" "$f"; done
fi

if [ "${#NOTES[@]}" -gt 0 ]; then
  head1 "Findings"
  for n in "${NOTES[@]}"; do printf "  ${YLW}!${RST} %s\n\n" "$n"; done
fi

if [ "$FAIL" -gt 0 ] || [ "$SKIP" -gt 0 ]; then
  exit 1
fi
exit 0
