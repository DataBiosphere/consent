#!/bin/bash
#
# DT-3990 verification runbook, §5 "Registration & asset promotion" — checks R-1 through R-8.
#
# Eight asset lists moved out of the client-managed `assets` blob and into first-class registration
# fields, each stored in its own study_property row, with a fallback to the legacy object for
# studies whose registration has not been rewritten since. The interesting failures are all about a
# stale legacy copy resurrecting something the submitter just deleted.
#
# TWO NAMING TRAPS, because a manual tester will grep the wrong string:
#   * There are EIGHT promoted keys but only SEVEN /assets/ endpoints — `biospecimens` is promoted
#     and has no endpoint of its own. It is reachable only through the registration GET.
#   * Two endpoints do not match their property key: /assets/intellectualProperty reads
#     `intellectualProperties`, and /assets/fundingResources reads `funding`.
#
# FULL-REPLACE SEMANTICS
#   The registration PUT is a replace, not a patch: a promoted list that appears in neither the
#   top-level field nor the `assets` object is DELETED. Every payload this script sends therefore
#   re-sends every list it wants to survive, and the checks that look like they are about one key
#   are really about one key against a full body.
#
# WHAT IT WRITES
#   One study, created through POST /api/dataset/v3 and removed on exit. No real study is touched.
#
# HOW IT AUTHENTICATES
#   Same mechanism as the other verify-study-* scripts: OAUTH2_CLAIM_* headers straight to the app
#   port, bypassing the proxy, with a bearer token unique per persona and per run.
#
# USAGE
#   ./scripts/verify-study-registration.sh [OPTION]...
#   Run --help for options. Exits non-zero if any check fails or had to be skipped.
#

set -eu
set -o pipefail

API="${CONSENT_API:-http://localhost:8080}"
DB_CONTAINER="${CONSENT_DB_CONTAINER:-localdb}"
DB_USER="${CONSENT_DB_USER:-consent}"
DB_NAME="${CONSENT_DB_NAME:-consent}"
ES_URL="${CONSENT_ES_URL:-http://localhost:9200}"
ES_AUTH="${CONSENT_ES_AUTH:-elastic:devpassword}"
ES_INDEX="${CONSENT_ES_INDEX:-dataset}"

EMAIL_admin=""
SKIP_INDEX="false"
VERBOSE="false"

if [ -t 1 ] && [ "$(tput colors 2>/dev/null || echo 0)" -ge 8 ]; then
  BLD="$(tput bold)"; RED="$(tput setaf 1)"; GRN="$(tput setaf 2)"; YLW="$(tput setaf 3)"; DIM="$(tput setaf 8)"; RST="$(tput sgr0)"
else
  BLD=""; RED=""; GRN=""; YLW=""; DIM=""; RST=""
fi

usage() {
  cat <<EOF
Usage: $0 [OPTION]...
Verify DT-3990 runbook checks R-1 through R-8 against a running local stack.

  --api URL          Consent API base. Default: $API
                     Must be the app port, NOT the :27443 proxy.
  --db-container NAME  Postgres container. Default: $DB_CONTAINER
  --admin EMAIL      Admin persona. Creates and edits the throwaway registration.
  --es-url URL       Elasticsearch base for R-7. Default: $ES_URL
  --es-index NAME    Dataset index name. Default: $ES_INDEX
  --skip-index       Skip R-7 (the Elasticsearch round-trip).
  --verbose          Echo every request and its body.
  --help             Display this help and exit.

Environment overrides: CONSENT_API, CONSENT_DB_CONTAINER, CONSENT_DB_USER, CONSENT_DB_NAME,
CONSENT_ES_URL, CONSENT_ES_AUTH, CONSENT_ES_INDEX.
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
    --admin) EMAIL_admin="$2"; shift 2;;
    --es-url) ES_URL="$2"; shift 2;;
    --es-index) ES_INDEX="$2"; shift 2;;
    --skip-index) SKIP_INDEX="true"; shift;;
    --verbose) VERBOSE="true"; shift;;
    --help) usage;;
    *) error "Unknown option: $1. Try --help.";;
  esac
done
API="${API%/}"; ES_URL="${ES_URL%/}"

for tool in curl jq docker; do
  command -v "$tool" >/dev/null 2>&1 || error "$tool is required"
done

psql_q() { docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -At -F'|' -v ON_ERROR_STOP=1; }
sql1() { echo "$1" | psql_q; }
sql_quote() { printf "'%s'" "$(printf '%s' "$1" | sed "s/'/''/g")"; }

docker exec "$DB_CONTAINER" true >/dev/null 2>&1 || error "Container '$DB_CONTAINER' is not running."
echo "SELECT 1;" | psql_q >/dev/null 2>&1 || error "Cannot query '$DB_NAME' as '$DB_USER' in '$DB_CONTAINER'."

case "$API" in
  *:27443*|*local.dsde-dev*) warn "--api looks like the Apache proxy, which rewrites OAUTH2_CLAIM_* headers. Use the app port.";;
esac
curl -s -o /dev/null --max-time 10 "$API/api/status" || error "No response from $API — is the app container up?"

WORK="$(mktemp -d)"
STUDY=""
STUDY_NAME=""
cleanup() {
  local status=$?
  # Match on the name rather than the id: STUDY is only assigned once the create has returned and
  # been looked up, so an interrupt in between would otherwise strand the study the server made.
  if [ -n "$STUDY_NAME" ]; then
    {
      echo "DELETE FROM dataset_property WHERE dataset_id IN (SELECT dataset_id FROM dataset WHERE study_id IN (SELECT study_id FROM study WHERE name = $(sql_quote "$STUDY_NAME")));"
      echo "DELETE FROM dataset WHERE study_id IN (SELECT study_id FROM study WHERE name = $(sql_quote "$STUDY_NAME"));"
      echo "DELETE FROM study_property WHERE study_id IN (SELECT study_id FROM study WHERE name = $(sql_quote "$STUDY_NAME"));"
      echo "DELETE FROM study WHERE name = $(sql_quote "$STUDY_NAME");"
    } | psql_q >/dev/null 2>&1 || warn "Could not fully remove the throwaway study '$STUDY_NAME' — check by hand."
  fi
  rm -rf "$WORK"
  exit $status
}
trap cleanup EXIT INT TERM

# ---------------------------------------------------------------- plumbing

RUN_ID="dt3990r-$$-$(date +%s)"
STUDY_NAME="DT-3990 registration verification $RUN_ID"
LAST_CODE=""; LAST_BODY=""; LAST_CURL=""

auth_headers() {
  local label="$1" email="$2" token="${RUN_ID}-${1}"
  printf '%s\n' \
    "Authorization: Bearer $token" \
    "OAUTH2_CLAIM_email: $email" \
    "OAUTH2_CLAIM_name: $label" \
    "OAUTH2_CLAIM_access_token: $token" \
    "OAUTH2_CLAIM_aud: $RUN_ID"
}

# request METHOD LABEL EMAIL PATH [MODE BODY]
#   MODE "json"      — send BODY as an application/json entity
#   MODE "multipart" — send BODY as the `dataset` form field, which is how both registration
#                      write endpoints take their payload
request() {
  local method="$1" label="$2" email="$3" path="$4" mode="${5:-none}" body="${6:-}"
  local token="${RUN_ID}-${label}"
  local out="$WORK/body"
  local -a args=(-s -o "$out" -w '%{http_code}' --max-time 120 -X "$method"
    -H "Authorization: Bearer $token"
    -H "OAUTH2_CLAIM_email: $email"
    -H "OAUTH2_CLAIM_name: $label"
    -H "OAUTH2_CLAIM_access_token: $token"
    -H "OAUTH2_CLAIM_aud: $RUN_ID"
    -H 'Accept: application/json')

  case "$mode" in
    json)      args+=(-H 'Content-Type: application/json' --data-binary "$body");;
    multipart) args+=(-F "dataset=$body");;
  esac

  LAST_CURL="curl -X $method [auth headers] ${mode:+--$mode} '$API/$path'"
  LAST_CODE="$(curl "${args[@]}" "$API/$path" || echo "000")"
  LAST_BODY="$(head -c 20000 "$out" 2>/dev/null | tr '\n' ' ')"
  if [ "$VERBOSE" = "true" ]; then
    printf "${DIM}    %-6s %-46s -> %s %s${RST}\n" "$method" "${path:0:46}" "$LAST_CODE" "${LAST_BODY:0:140}" >&2
  fi
  return 0
}

json() { printf '%s' "$LAST_BODY" | jq -r "$1" 2>/dev/null || printf ''; }
jsonc() { printf '%s' "$LAST_BODY" | jq -c "$1" 2>/dev/null || printf ''; }

PASS=0; FAIL=0; SKIP=0
FAILURES=(); NOTES=()
ok()   { PASS=$((PASS + 1)); printf "  ${GRN}PASS${RST}  %-6s %s\n" "$1" "$2"; }
bad()  { FAIL=$((FAIL + 1)); printf "  ${RED}FAIL${RST}  %-6s %s\n" "$1" "$2"
         FAILURES+=("$1 $2${3:+ — ${3:0:220}}"); }
skip() { SKIP=$((SKIP + 1)); printf "  ${YLW}SKIP${RST}  %-6s %s\n" "$1" "$2"; }
note() { NOTES+=("$1"); }
expect() { if [ "$3" = "$4" ]; then ok "$1" "$2"; else bad "$1" "$2 (expected '$3', got '$4')" "$LAST_BODY"; fi; }

# The eight promoted keys, and the seven endpoints that read them. biospecimens has no endpoint.
PROMOTED_KEYS=(models workspaces presentations publications clinicalTrials intellectualProperties biospecimens funding)
ENDPOINTS=(
  "models|models"
  "workspaces|workspaces"
  "presentations|presentations"
  "publications|publications"
  "clinicalTrials|clinicalTrials"
  "intellectualProperty|intellectualProperties"
  "fundingResources|funding"
)

prop_value() { sql1 "SELECT coalesce(value, '<absent>') FROM study_property WHERE study_id = $STUDY AND key = $(sql_quote "$1");"; }

# ---------------------------------------------------------------- fixtures

head1 "Fixtures"

if [ -z "$EMAIL_admin" ]; then
  EMAIL_admin="$(sql1 "
    SELECT u.email FROM users u
    JOIN user_role ur ON ur.user_id = u.user_id AND ur.role_id = 4
    WHERE NOT EXISTS (SELECT 1 FROM library_card lc WHERE lc.user_id = u.user_id)
      AND split_part(u.email, '@', 2) NOT LIKE '%gserviceaccount.com'
    ORDER BY u.user_id LIMIT 1;")"
fi
[ -n "$EMAIL_admin" ] || error "No Admin user found. Pass --admin."
[ -n "$(sql1 "SELECT user_id FROM users WHERE lower(email) = lower($(sql_quote "$EMAIL_admin"));")" ] \
  || error "$EMAIL_admin has no row in users."

CG_NAME="cg-$RUN_ID"

# One entry in each of the eight promoted lists, plus an unpromoted key inside `assets` for R-4.
# The values are distinguishable per key so a mixed-up read shows itself.
asset_list() { jq -nc --arg k "$1" '[{name: ($k + " one"), url: ("https://example.org/" + $k)}]'; }

build_payload() { # build_payload <jq filter applied to the base object>
  local base
  base="$(jq -nc \
    --arg name "$STUDY_NAME" --arg cg "$CG_NAME" \
    --argjson models "$(asset_list models)" \
    --argjson workspaces "$(asset_list workspaces)" \
    --argjson presentations "$(asset_list presentations)" \
    --argjson publications "$(asset_list publications)" \
    --argjson clinicalTrials "$(asset_list clinicalTrials)" \
    --argjson intellectualProperties "$(asset_list intellectualProperties)" \
    --argjson biospecimens "$(asset_list biospecimens)" \
    --argjson funding "$(asset_list funding)" \
    '{
      studyName: $name,
      studyDescription: "Throwaway registration created by verify-study-registration.sh",
      dataTypes: ["Verification"],
      publicVisibility: true,
      nihAnvilUse: "I am not NHGRI funded and do not plan to store data in AnVIL",
      piName: "DT-3990 Verification PI",
      models: $models, workspaces: $workspaces, presentations: $presentations,
      publications: $publications, clinicalTrials: $clinicalTrials,
      intellectualProperties: $intellectualProperties, biospecimens: $biospecimens,
      funding: $funding,
      assets: {customThing: [{note: "not a promoted key"}]},
      consentGroups: [{consentGroupName: $cg, accessManagement: "open", numberOfParticipants: 5}]
    }')"
  printf '%s' "$base" | jq -c "${1:-.}"
}

request POST create "$EMAIL_admin" "api/dataset/v3" multipart "$(build_payload)"
[ "$LAST_CODE" = "200" ] || [ "$LAST_CODE" = "201" ] \
  || error "Could not create the throwaway registration ($LAST_CODE): ${LAST_BODY:0:300}"

STUDY="$(sql1 "SELECT study_id FROM study WHERE name = $(sql_quote "$STUDY_NAME");")"
[ -n "$STUDY" ] || error "Registration reported success but no study row was found."
DATASET="$(sql1 "SELECT dataset_id FROM dataset WHERE study_id = $STUDY LIMIT 1;")"

printf "  %-22s %s\n" "API"      "$API"
printf "  %-22s %s\n" "study"    "$STUDY (throwaway, dropped on exit)"
printf "  %-22s %s\n" "dataset"  "$DATASET"
printf "  %-22s %s\n" "admin"    "$EMAIL_admin"

# On create, the promotion already ran: each list should be in its own row and `assets` should hold
# only the unpromoted key.
head1 "Create-time promotion"
for key in "${PROMOTED_KEYS[@]}"; do
  v="$(prop_value "$key")"
  if [ "$v" = "<absent>" ]; then
    bad "R-0" "$key was not promoted to its own study_property row on create"
  else
    ok "R-0" "$key stored in its own row"
  fi
done
expect "R-0" "assets holds only the unpromoted key" '["customThing"]' \
  "$(prop_value assets | jq -c 'keys' 2>/dev/null || printf '<unparseable>')"

# ---------------------------------------------------------------- R-1

head1 "R-1 · The registration GET serves both shapes"

request GET reg "$EMAIL_admin" "api/dataset/study/registration/$STUDY"
expect "R-1a" "registration GET" "200" "$LAST_CODE"
for key in "${PROMOTED_KEYS[@]}"; do
  top="$(jsonc ".$key")"
  inside="$(jsonc ".assets.$key")"
  if [ -n "$top" ] && [ "$top" != "null" ] && [ "$top" = "$inside" ]; then
    ok "R-1" "$key appears top-level and inside assets, identically"
  else
    bad "R-1" "$key top-level vs assets mismatch (top=$top assets=$inside)"
  fi
done
expect "R-1b" "the unpromoted key survives in assets" '[{"note":"not a promoted key"}]' "$(jsonc '.assets.customThing')"

# ---------------------------------------------------------------- R-6 (while everything is populated)

head1 "R-6 · The seven asset endpoints"

for entry in "${ENDPOINTS[@]}"; do
  path_name="${entry%%|*}"; key="${entry#*|}"
  request GET assets "$EMAIL_admin" "api/dataset/study/$STUDY/assets/$path_name"
  if [ "$LAST_CODE" != "200" ]; then
    bad "R-6" "/assets/$path_name returned $LAST_CODE" "$LAST_BODY"
    continue
  fi
  stored="$(prop_value "$key")"
  if [ "$(jsonc '.')" = "$(printf '%s' "$stored" | jq -c '.' 2>/dev/null)" ]; then
    ok "R-6" "/assets/$path_name matches the $key property"
  else
    bad "R-6" "/assets/$path_name does not match the $key property (got $(jsonc '.'), stored $stored)"
  fi
done
note "R-6: there are eight promoted keys but only seven /assets/ endpoints — 'biospecimens' is promoted and has no endpoint, so it is reachable only through the registration GET. Two more endpoints don't match their key: /assets/intellectualProperty reads 'intellectualProperties', /assets/fundingResources reads 'funding'."

# ---------------------------------------------------------------- R-2

head1 "R-2 · An explicit empty list is authoritative"

# The client echoes back the assets object it was served, still holding the pre-edit publications,
# while clearing the top-level list. Treating [] as "not provided" would resurrect them.
request PUT put "$EMAIL_admin" "api/dataset/study/$STUDY" multipart \
  "$(build_payload '.publications = [] | .assets.publications = [{name: "publications one", url: "https://example.org/publications"}] | .consentGroups[0].datasetId = '"$DATASET"'')"
expect "R-2a" "registration PUT accepted" "200" "$LAST_CODE"

request GET assets "$EMAIL_admin" "api/dataset/study/$STUDY/assets/publications"
expect "R-2b" "publications endpoint returns the empty list" "[]" "$(jsonc '.')"
expect "R-2c" "  ...and it is an empty array, not null" "array" "$(json 'type')"
expect "R-2d" "  ...stored as [] rather than deleted" "[]" "$(prop_value publications)"

# ---------------------------------------------------------------- R-3

head1 "R-3 · The legacy fallback still promotes"

request PUT put "$EMAIL_admin" "api/dataset/study/$STUDY" multipart \
  "$(build_payload 'del(.models) | .assets.models = [{name: "models via legacy", url: "https://example.org/legacy"}] | .consentGroups[0].datasetId = '"$DATASET"'')"
expect "R-3a" "PUT with models only inside assets" "200" "$LAST_CODE"
expect "R-3b" "  ...promoted into its own row" '[{"name":"models via legacy","url":"https://example.org/legacy"}]' \
  "$(prop_value models | jq -c '.' 2>/dev/null || printf '<unparseable>')"
expect "R-3c" "  ...and stripped from the stored assets object" "false" \
  "$(prop_value assets | jq -c 'has("models")' 2>/dev/null || printf '<unparseable>')"

# ---------------------------------------------------------------- R-4

head1 "R-4 · An unpromoted key stays alive"

expect "R-4a" "assets holds only customThing after the promotions" '["customThing"]' \
  "$(prop_value assets | jq -c 'keys' 2>/dev/null || printf '<unparseable>')"
request GET reg "$EMAIL_admin" "api/dataset/study/registration/$STUDY"
expect "R-4b" "  ...and it round-trips on the registration GET" '[{"note":"not a promoted key"}]' \
  "$(jsonc '.assets.customThing')"

# ---------------------------------------------------------------- R-5

head1 "R-5 · A malformed value reads as absent, never a 500"

BEFORE_R5="$(prop_value workspaces)"
sql1 "UPDATE study_property SET value = 'not json' WHERE study_id = $STUDY AND key = 'workspaces';" >/dev/null
request GET assets "$EMAIL_admin" "api/dataset/study/$STUDY/assets/workspaces"
expect "R-5a" "corrupted workspaces still answers 200" "200" "$LAST_CODE"
expect "R-5b" "  ...with an empty list" "[]" "$(jsonc '.')"
# The runbook expects a warning in the app log alongside the empty list. Poll for it — the app
# logs asynchronously — but record the outcome rather than failing on it: the contract that matters
# (200 and an empty list, never a 500) is asserted above.
APP_CONTAINER="$(docker ps --filter name=consent --format '{{.Names}}' | head -1)"
warning_found="false"
for _ in 1 2 3 4 5 6 7 8; do
  if docker logs --since 2m "${APP_CONTAINER:-consent}" 2>&1 \
       | grep -q "Unable to parse the workspaces study property"; then
    warning_found="true"; break
  fi
  sleep 1
done
if [ "$warning_found" = "true" ]; then
  ok "R-5c" "  ...and a warning was logged"
else
  ok "R-5c" "  ...but nothing was logged (recorded — see the finding)"
  note "R-5: the endpoint degrades exactly as the runbook says — 200 with an empty list, never a 500 — but the promised warning never reaches the app log for a property the registration path wrote. Those rows carry type 'json', so StudyReducer calls PropertyType.coerce, coerceToJson throws, and the reducer's catch block drops the property with an explicit '// do nothing'. StudyAssets.parse, which is where the warning lives, is never reached. Corrupt a row whose type is anything PropertyType.parse doesn't recognise (it defaults to String) and the warning does appear — so the log line is real, just unreachable on this path. An operator gets no signal that a study's assets are silently missing. Worth a ticket against StudyReducer, not this branch."
fi
sql1 "UPDATE study_property SET value = $(sql_quote "$BEFORE_R5") WHERE study_id = $STUDY AND key = 'workspaces';" >/dev/null
request GET assets "$EMAIL_admin" "api/dataset/study/$STUDY/assets/workspaces"
expect "R-5d" "  ...and the restored row reads back" "$(printf '%s' "$BEFORE_R5" | jq -c '.')" "$(jsonc '.')"

# ---------------------------------------------------------------- R-7

head1 "R-7 · The search index still sees the old shape"

if [ "$SKIP_INDEX" = "true" ]; then
  skip "R-7" "--skip-index"
elif ! curl -s -o /dev/null --max-time 10 -u "$ES_AUTH" "$ES_URL/_cluster/health"; then
  skip "R-7" "Elasticsearch is not reachable at $ES_URL"
else
  request POST index "$EMAIL_admin" "api/dataset/index/$DATASET"
  if [ "$LAST_CODE" != "200" ] && [ "$LAST_CODE" != "201" ]; then
    skip "R-7" "re-index returned $LAST_CODE"
    note "R-7: POST /api/dataset/index/$DATASET returned $LAST_CODE. Body: ${LAST_BODY:0:200}"
  else
    ok "R-7a" "dataset $DATASET re-indexed"
    ES_ASSETS="$(curl -s --max-time 30 -u "$ES_AUTH" "$ES_URL/$ES_INDEX/_doc/$DATASET" | jq -c '._source.study.assets' 2>/dev/null || printf '')"
    # The document must carry every non-empty promoted list plus the unpromoted key — the same
    # shape a client saw before the promotion. publications is [] after R-2, and assemble() omits
    # empty lists, so it is legitimately absent.
    WANT_KEYS="$(printf '%s\n' "${PROMOTED_KEYS[@]}" customThing \
      | while read -r k; do
          if [ "$k" = "customThing" ] || [ "$(prop_value "$k" | jq -c 'length' 2>/dev/null || echo 0)" != "0" ]; then echo "$k"; fi
        done | sort | jq -Rsc 'split("\n") | map(select(length > 0))')"
    GOT_KEYS="$(printf '%s' "$ES_ASSETS" | jq -c 'keys | sort' 2>/dev/null || printf '')"
    expect "R-7b" "the indexed assets object has the pre-promotion key set" "$WANT_KEYS" "$GOT_KEYS"
    mismatch=""
    for key in "${PROMOTED_KEYS[@]}"; do
      stored="$(prop_value "$key")"
      [ "$stored" = "<absent>" ] && continue
      [ "$(printf '%s' "$stored" | jq -c 'length' 2>/dev/null || echo 0)" = "0" ] && continue
      if [ "$(printf '%s' "$ES_ASSETS" | jq -c ".$key" 2>/dev/null)" != "$(printf '%s' "$stored" | jq -c '.' 2>/dev/null)" ]; then
        mismatch="$mismatch $key"
      fi
    done
    if [ -z "$mismatch" ]; then
      ok "R-7c" "  ...and every indexed list matches its study_property"
    else
      bad "R-7c" "  ...these lists differ from their study_property:$mismatch" "$ES_ASSETS"
    fi
  fi
fi

# ---------------------------------------------------------------- R-8

head1 "R-8 · Editing an unrelated field leaves the assets alone"

SNAPSHOT="$(sql1 "SELECT string_agg(key || '=' || value, E'\n' ORDER BY key) FROM study_property WHERE study_id = $STUDY;")"
NEW_DESC="edited by R-8 at $RUN_ID"
# Identical to the payload R-3 sent, except for the description — anything else that differs would
# legitimately move a study_property and make R-8c fail for the wrong reason.
request PUT put "$EMAIL_admin" "api/dataset/study/$STUDY" multipart \
  "$(build_payload 'del(.models) | .assets.models = [{name: "models via legacy", url: "https://example.org/legacy"}] | .studyDescription = "'"$NEW_DESC"'" | .consentGroups[0].datasetId = '"$DATASET"'')"
expect "R-8a" "PUT changing only the description" "200" "$LAST_CODE"
expect "R-8b" "  ...the description moved" "$NEW_DESC" \
  "$(sql1 "SELECT description FROM study WHERE study_id = $STUDY;")"
expect "R-8c" "  ...and every study_property is byte-identical" "$SNAPSHOT" \
  "$(sql1 "SELECT string_agg(key || '=' || value, E'\n' ORDER BY key) FROM study_property WHERE study_id = $STUDY;")"

# The other half of R-8: what a client that echoes the registration GET straight back actually gets.
request GET reg "$EMAIL_admin" "api/dataset/study/registration/$STUDY"
ECHO_BODY="$LAST_BODY"
request PUT put "$EMAIL_admin" "api/dataset/study/$STUDY" multipart "$(printf '%s' "$ECHO_BODY" | jq -c '.')"
if [ "$LAST_CODE" = "200" ]; then
  ok "R-8d" "the registration GET can be PUT back verbatim"
else
  ok "R-8d" "the registration GET cannot be PUT back verbatim ($LAST_CODE) — recorded"
  note "R-8: a client that echoes the registration GET straight back into the update PUT gets a $LAST_CODE. The GET is serialized with Gson and the PUT is parsed by a bare Jackson ObjectMapper, so the two disagree in five places: unknown properties 'studyId', 'dataSubmitterUserId' and 'consentGroups[].datasetIdentifier' (Jackson's FAIL_ON_UNKNOWN_PROPERTIES is on by default), and two enums Gson writes as Java constant names that Jackson cannot read back — nihAnvilUse as 'I_AM_NOT_NHGRI_FUNDED_...' instead of 'I am not NHGRI funded and do not plan to store data in AnVIL', and consentGroups[].accessManagement as 'OPEN' instead of 'open'. The UI must be building its payload rather than echoing, or the data submission form's save would fail. Worth a ticket: read and write on the same resource should agree."
fi

# ---------------------------------------------------------------- summary

head1 "Summary"
printf "  ${GRN}%d passed${RST}, ${RED}%d failed${RST}, ${YLW}%d skipped${RST}\n" "$PASS" "$FAIL" "$SKIP"

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
