#!/bin/bash
#
# DT-3990 verification runbook, §2 "Visibility, across every new endpoint" — checks V-1, V-2, V-3.
#
# Walks all twelve new @PermitAll GETs as four different callers and asserts every cell of the
# runbook's matrix, then re-runs them with a nonexistent study id (V-2) and a non-numeric one (V-3).
#
# HOW IT AUTHENTICATES AS FOUR PEOPLE
#   In a deployed environment the Apache proxy validates the bearer token and injects the
#   OAUTH2_CLAIM_* headers that RequestHeaderCacheFilter caches and OAuthAuthenticator reads back.
#   This script talks to the app port (8080) DIRECTLY, bypassing the proxy, and supplies those
#   headers itself. That is the only way to exercise four identities without four Google logins.
#   Point --api at the proxy (:27443) and every persona collapses into whoever your real token is,
#   so don't.
#
#   Two consequences worth knowing:
#     * ClaimsCache is get-if-absent keyed on the bearer token with a 5 minute TTL, so each persona
#       gets a token unique to the persona AND to this run. Reusing a token across emails would
#       silently authenticate as the first email seen.
#     * DuosUserAuthenticator 401s if the persona's email is not a row in `users`, and
#       AuthorizationHelper.getUserStatusInfo can 500 if Sam rejects the fabricated token. The
#       persona smoke test below catches both before the matrix runs, so a broken persona is
#       reported once instead of as twelve misleading failures.
#
#   Note also that AuthorizationHelper re-runs enforceInstitutionAndLibraryCardRules on every
#   authenticated request, which can DELETE a persona's library_card row when their email domain
#   doesn't map to an institution. Fixture discovery prefers users with no card for the personas it
#   is free to choose. Run this against a local dump, not a shared environment.
#
# USAGE
#   ./scripts/verify-study-visibility.sh [OPTION]...
#   Run --help for options. Exits non-zero if any cell fails or any check had to be skipped.
#

set -eu
set -o pipefail

API="${CONSENT_API:-http://localhost:8080}"
DB_CONTAINER="${CONSENT_DB_CONTAINER:-localdb}"
DB_USER="${CONSENT_DB_USER:-consent}"
DB_NAME="${CONSENT_DB_NAME:-consent}"

PUBLIC_STUDY=""
PRIVATE_STUDY=""
EMAIL_creator=""
EMAIL_custodian=""
EMAIL_admin=""
EMAIL_unrelated=""
MISSING_STUDY=""
PROVISION_CUSTODIAN="false"
ONLY=""
VERBOSE="false"

if [ -t 1 ] && [ "$(tput colors 2>/dev/null || echo 0)" -ge 8 ]; then
  BLD="$(tput bold)"; RED="$(tput setaf 1)"; GRN="$(tput setaf 2)"; YLW="$(tput setaf 3)"; DIM="$(tput setaf 8)"; RST="$(tput sgr0)"
else
  BLD=""; RED=""; GRN=""; YLW=""; DIM=""; RST=""
fi

usage() {
  cat <<EOF
Usage: $0 [OPTION]...
Verify DT-3990 runbook checks V-1, V-2 and V-3 against a running local stack.

  --api URL            Consent API base. Default: $API
                       Must be the app port, NOT the :27443 proxy. See header comment.
  --db-container NAME  Postgres container to run fixture queries in. Default: $DB_CONTAINER
  --public-study ID    Skip discovery, use this publicly visible study.
  --private-study ID   Skip discovery, use this study with public_visibility = false.
  --creator EMAIL      Persona for the "private / creator" column.
  --custodian EMAIL    Persona for the "private / custodian" column.
  --admin EMAIL        Persona holding the Admin role.
  --unrelated EMAIL    Persona who is none of the above.
  --provision-custodian
                       If the private study has no dataCustodianEmail entry (the dev dump ships
                       every array empty), temporarily append the custodian persona to it and
                       restore the original value on exit. Off by default: it writes to your
                       database. Without it the custodian column is reported as SKIP.
  --only LIST          Comma-separated subset of v1,v2,v3. Default: all three.
  --verbose            Echo every request's status and body snippet.
  --help               Display this help and exit.

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
    --public-study) PUBLIC_STUDY="$2"; shift 2;;
    --private-study) PRIVATE_STUDY="$2"; shift 2;;
    --creator) EMAIL_creator="$2"; shift 2;;
    --custodian) EMAIL_custodian="$2"; shift 2;;
    --admin) EMAIL_admin="$2"; shift 2;;
    --unrelated) EMAIL_unrelated="$2"; shift 2;;
    --provision-custodian) PROVISION_CUSTODIAN="true"; shift;;
    --only) ONLY="$2"; shift 2;;
    --verbose) VERBOSE="true"; shift;;
    --help) usage;;
    *) error "Unknown option: $1. Try --help.";;
  esac
done
API="${API%/}"

run_check() {
  case "${ONLY:-}" in
    "") return 0;;
    *) case ",$ONLY," in *",$1,"*) return 0;; *) return 1;; esac;;
  esac
}

# ---------------------------------------------------------------- preflight

for tool in curl jq docker; do
  command -v "$tool" >/dev/null 2>&1 || error "$tool is required"
done

psql_q() { docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -At -F'|' -v ON_ERROR_STOP=1; }

docker exec "$DB_CONTAINER" true >/dev/null 2>&1 \
  || error "Container '$DB_CONTAINER' is not running. Bring the stack up first (docker-compose -p consent -f config/docker-compose.yaml up)."

echo "SELECT 1;" | psql_q >/dev/null 2>&1 \
  || error "Cannot query database '$DB_NAME' as '$DB_USER' in container '$DB_CONTAINER'."

case "$API" in
  *:27443*|*local.dsde-dev*) warn "--api looks like the Apache proxy. It rewrites OAUTH2_CLAIM_* headers, so all four personas will collapse into your real identity. Use the app port (http://localhost:8080).";;
esac

curl -s -o /dev/null --max-time 10 "$API/api/status" \
  || error "No response from $API — is the app container up?"

WORK="$(mktemp -d)"
cleanup() {
  restore_custodian
  rm -rf "$WORK"
}
trap cleanup EXIT INT TERM

# ---------------------------------------------------------------- fixtures

# Populated by provision_custodian so cleanup can put the row back exactly as it was.
CUSTODIAN_ROW_BACKUP=""
CUSTODIAN_PROVISIONED="false"

restore_custodian() {
  [ "$CUSTODIAN_PROVISIONED" = "true" ] || return 0
  CUSTODIAN_PROVISIONED="false"
  printf "\n${DIM}Restoring dataCustodianEmail on study %s${RST}\n" "$PRIVATE_STUDY"
  printf "UPDATE study_property SET value = %s WHERE study_id = %s AND key = 'dataCustodianEmail';\n" \
    "$(sql_quote "$CUSTODIAN_ROW_BACKUP")" "$PRIVATE_STUDY" | psql_q >/dev/null \
    || warn "Could not restore dataCustodianEmail on study $PRIVATE_STUDY — expected value: $CUSTODIAN_ROW_BACKUP"
}

sql_quote() { printf "'%s'" "$(printf '%s' "$1" | sed "s/'/''/g")"; }

discover_public_study() {
  [ -n "$PUBLIC_STUDY" ] && return 0
  PUBLIC_STUDY="$(echo "
    SELECT s.study_id FROM study s
    JOIN users u ON u.user_id = s.create_user_id
    WHERE s.public_visibility IS TRUE
    ORDER BY s.study_id LIMIT 1;" | psql_q)"
  [ -n "$PUBLIC_STUDY" ] || error "No publicly visible study found. Pass --public-study."
}

# Prefer a private study whose custodian column can actually run, and whose custodian is not also
# an Admin (that would pass through the admin branch and prove nothing on its own).
private_study_with_custodian() { # private_study_with_custodian EXCLUDE_ADMINS
  local admin_clause=""
  [ "$1" = "true" ] && admin_clause="AND NOT EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_id = u2.user_id AND ur.role_id = 4)"
  echo "
    WITH cust AS MATERIALIZED (
      SELECT sp.study_id, btrim(e) AS email
      FROM study_property sp
      CROSS JOIN LATERAL jsonb_array_elements_text(sp.value::jsonb) AS t(e)
      WHERE sp.key = 'dataCustodianEmail' AND sp.value ~ '^\s*\['
    )
    SELECT s.study_id FROM study s
    JOIN users cu ON cu.user_id = s.create_user_id
    JOIN cust c ON c.study_id = s.study_id
    JOIN users u2 ON lower(u2.email) = lower(c.email)
    WHERE s.public_visibility IS NOT TRUE AND u2.user_id <> s.create_user_id
      $admin_clause
    ORDER BY s.study_id LIMIT 1;" | psql_q
}

discover_private_study() {
  if [ -z "$PRIVATE_STUDY" ]; then
    PRIVATE_STUDY="$(private_study_with_custodian true)"
  fi
  if [ -z "$PRIVATE_STUDY" ]; then
    PRIVATE_STUDY="$(private_study_with_custodian false)"
  fi
  if [ -z "$PRIVATE_STUDY" ]; then
    PRIVATE_STUDY="$(echo "
      SELECT s.study_id FROM study s
      JOIN users u ON u.user_id = s.create_user_id
      WHERE s.public_visibility IS NOT TRUE
      ORDER BY s.study_id LIMIT 1;" | psql_q)"
  fi
  [ -n "$PRIVATE_STUDY" ] || error "No study with public_visibility = false found. Pass --private-study."
}

discover_creator() {
  [ -n "$EMAIL_creator" ] && return 0
  EMAIL_creator="$(echo "
    SELECT u.email FROM study s JOIN users u ON u.user_id = s.create_user_id
    WHERE s.study_id = $PRIVATE_STUDY;" | psql_q)"
  [ -n "$EMAIL_creator" ] || error "Study $PRIVATE_STUDY has no resolvable creator. Pass --private-study or --creator."
}

discover_custodian() {
  [ -n "$EMAIL_custodian" ] && return 0
  EMAIL_custodian="$(echo "
    WITH cust AS MATERIALIZED (
      SELECT btrim(e) AS email
      FROM study_property sp
      CROSS JOIN LATERAL jsonb_array_elements_text(sp.value::jsonb) AS t(e)
      WHERE sp.key = 'dataCustodianEmail' AND sp.study_id = $PRIVATE_STUDY AND sp.value ~ '^\s*\['
    )
    SELECT u.email FROM cust c JOIN users u ON lower(u.email) = lower(c.email)
    WHERE u.user_id <> (SELECT create_user_id FROM study WHERE study_id = $PRIVATE_STUDY)
    -- A custodian who also holds Admin would pass the column through the admin branch instead,
    -- so take a plain custodian when one exists.
    ORDER BY (EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_id = u.user_id AND ur.role_id = 4)), u.user_id
    LIMIT 1;" | psql_q)"
}

is_admin() { # is_admin EMAIL
  [ -n "$1" ] || return 1
  [ "$(echo "
    SELECT count(*) FROM users u JOIN user_role ur ON ur.user_id = u.user_id AND ur.role_id = 4
    WHERE lower(u.email) = lower($(sql_quote "$1"));" | psql_q)" != "0" ]
}

# The custodian branch is the one path that depends on data the dev dump does not carry: every
# dataCustodianEmail array in it is empty. Opt in with --provision-custodian and we borrow a
# throwaway user for the duration of the run.
provision_custodian() {
  local candidate
  candidate="$(echo "
    SELECT u.email FROM users u
    WHERE u.user_id <> (SELECT create_user_id FROM study WHERE study_id = $PRIVATE_STUDY)
      AND NOT EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_id = u.user_id AND ur.role_id = 4)
      AND NOT EXISTS (SELECT 1 FROM library_card lc WHERE lc.user_id = u.user_id)
    ORDER BY u.user_id LIMIT 1;" | psql_q)"
  [ -n "$candidate" ] || { warn "No candidate user available to act as custodian."; return 1; }

  CUSTODIAN_ROW_BACKUP="$(echo "
    SELECT value FROM study_property
    WHERE study_id = $PRIVATE_STUDY AND key = 'dataCustodianEmail';" | psql_q)"
  [ -n "$CUSTODIAN_ROW_BACKUP" ] || { warn "Study $PRIVATE_STUDY has no dataCustodianEmail row to amend."; return 1; }

  local updated
  updated="$(printf '%s' "$CUSTODIAN_ROW_BACKUP" | jq -c --arg e "$candidate" '. + [$e]')" || {
    warn "dataCustodianEmail on study $PRIVATE_STUDY is not a JSON array; leaving it alone."
    return 1
  }
  printf "UPDATE study_property SET value = %s WHERE study_id = %s AND key = 'dataCustodianEmail';\n" \
    "$(sql_quote "$updated")" "$PRIVATE_STUDY" | psql_q >/dev/null
  CUSTODIAN_PROVISIONED="true"
  EMAIL_custodian="$candidate"
  printf "${DIM}Provisioned %s as custodian of study %s (original value restored on exit)${RST}\n" \
    "$candidate" "$PRIVATE_STUDY"
}

discover_admin() {
  [ -n "$EMAIL_admin" ] && return 0
  # Prefer an admin with no library card, so per-request library card enforcement has nothing
  # to delete, and one who is not also the creator of the private study.
  EMAIL_admin="$(echo "
    SELECT u.email FROM users u
    JOIN user_role ur ON ur.user_id = u.user_id AND ur.role_id = 4
    WHERE u.user_id <> (SELECT create_user_id FROM study WHERE study_id = $PRIVATE_STUDY)
    ORDER BY (EXISTS (SELECT 1 FROM library_card lc WHERE lc.user_id = u.user_id)), u.user_id
    LIMIT 1;" | psql_q)"
  [ -n "$EMAIL_admin" ] || error "No Admin user found. Pass --admin."
}

discover_unrelated() {
  [ -n "$EMAIL_unrelated" ] && return 0
  EMAIL_unrelated="$(echo "
    WITH cust AS MATERIALIZED (
      SELECT lower(btrim(e)) AS email
      FROM study_property sp
      CROSS JOIN LATERAL jsonb_array_elements_text(sp.value::jsonb) AS t(e)
      WHERE sp.key = 'dataCustodianEmail' AND sp.study_id = $PRIVATE_STUDY AND sp.value ~ '^\s*\['
    )
    SELECT u.email FROM users u
    WHERE u.user_id <> (SELECT create_user_id FROM study WHERE study_id = $PRIVATE_STUDY)
      AND lower(u.email) NOT IN (SELECT email FROM cust)
      AND NOT EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_id = u.user_id AND ur.role_id = 4)
      AND NOT EXISTS (SELECT 1 FROM library_card lc WHERE lc.user_id = u.user_id)
    ORDER BY u.user_id LIMIT 1;" | psql_q)"
  [ -n "$EMAIL_unrelated" ] || error "No unrelated (non-creator, non-custodian, non-admin) user found. Pass --unrelated."
}

lc() { printf '%s' "$1" | tr 'A-Z' 'a-z'; }

check_unrelated() {
  local why=""
  if [ "$(lc "$EMAIL_unrelated")" = "$(lc "$EMAIL_creator")" ]; then
    why="the creator"
  elif [ -n "$EMAIL_custodian" ] && [ "$(lc "$EMAIL_unrelated")" = "$(lc "$EMAIL_custodian")" ]; then
    why="a custodian"
  fi
  if [ -z "$why" ] && [ "$(echo "
    SELECT count(*) FROM users u JOIN user_role ur ON ur.user_id = u.user_id AND ur.role_id = 4
    WHERE lower(u.email) = lower($(sql_quote "$EMAIL_unrelated"));" | psql_q)" != "0" ]; then
    why="an Admin"
  fi
  [ -z "$why" ] || warn "Unrelated persona $EMAIL_unrelated is $why on study $PRIVATE_STUDY, so they are not unrelated. Every PRV/OUT cell will read 200 for a fixture reason, not a regression."
}

assert_visibility() { # assert_visibility STUDY_ID EXPECTED_BOOL LABEL
  local actual
  actual="$(echo "SELECT coalesce(public_visibility, false) FROM study WHERE study_id = $1;" | psql_q)"
  [ -n "$actual" ] || error "$3 study $1 does not exist."
  [ "$actual" = "$2" ] \
    || error "$3 study $1 has public_visibility = $actual, expected $2. The matrix means nothing with the wrong fixture."
}

FIXTURE_NOTES=()

head1 "Fixtures"
discover_public_study
discover_private_study
discover_creator
discover_custodian
if [ "$PROVISION_CUSTODIAN" = "true" ] && { [ -z "$EMAIL_custodian" ] || is_admin "$EMAIL_custodian"; }; then
  provision_custodian || true
fi
discover_admin
discover_unrelated

check_unrelated
if [ -n "$EMAIL_custodian" ] && is_admin "$EMAIL_custodian"; then
  FIXTURE_NOTES+=("The private/custodian column ran as $EMAIL_custodian, who also holds the Admin role — it passed through the admin branch and proves nothing the private/admin column doesn't. Re-run with --provision-custodian, or pass --custodian with a non-admin custodian.")
  warn "Custodian $EMAIL_custodian also holds the Admin role — see the note in the summary."
fi

assert_visibility "$PUBLIC_STUDY" "t" "Public"
assert_visibility "$PRIVATE_STUDY" "f" "Private"

MISSING_STUDY="$(echo "SELECT coalesce(max(study_id), 0) + 1000 FROM study;" | psql_q)"

# AuthorizationHelper.buildAuthUserFromHeaders re-runs enforceInstitutionAndLibraryCardRules on
# every authenticated request and can delete a card whose issuer or domain no longer lines up.
# Count before and after: a drop here is what breaks §3 of the runbook on this dump.
LIBRARY_CARDS_BEFORE="$(echo "SELECT count(*) FROM library_card;" | psql_q)"

printf "  %-22s %s\n" "API"                "$API"
printf "  %-22s %s\n" "public study"       "$PUBLIC_STUDY"
printf "  %-22s %s\n" "private study"      "$PRIVATE_STUDY"
printf "  %-22s %s\n" "nonexistent study"  "$MISSING_STUDY"
printf "  %-22s %s\n" "creator"            "$EMAIL_creator"
printf "  %-22s %s\n" "custodian"          "${EMAIL_custodian:-${YLW}none — see --provision-custodian${RST}}"
printf "  %-22s %s\n" "admin"              "$EMAIL_admin"
printf "  %-22s %s\n" "unrelated"          "$EMAIL_unrelated"

# ---------------------------------------------------------------- request plumbing

RUN_ID="dt3990-$$-$(date +%s)"
LAST_CODE=""
LAST_BODY=""
LAST_CURL=""

# request PERSONA_EMAIL PERSONA_LABEL PATH
# Sets LAST_CODE, LAST_BODY and LAST_CURL. Deliberately not a command substitution: those run in a
# subshell, and the failure detail would be lost with it.
request() {
  local email="$1" label="$2" path="$3"
  local token="${RUN_ID}-${label}"
  local body="$WORK/body"

  LAST_CURL="curl -s -w '%{http_code}' \\
  -H 'Authorization: Bearer $token' \\
  -H 'OAUTH2_CLAIM_email: $email' \\
  -H 'OAUTH2_CLAIM_name: $label' \\
  -H 'OAUTH2_CLAIM_access_token: $token' \\
  -H 'OAUTH2_CLAIM_aud: $RUN_ID' \\
  '$API/$path'"

  LAST_CODE="$(curl -s -o "$body" -w '%{http_code}' --max-time 60 \
    -H "Authorization: Bearer $token" \
    -H "OAUTH2_CLAIM_email: $email" \
    -H "OAUTH2_CLAIM_name: $label" \
    -H "OAUTH2_CLAIM_access_token: $token" \
    -H "OAUTH2_CLAIM_aud: $RUN_ID" \
    -H 'Accept: application/json' \
    "$API/$path" || echo "000")"

  LAST_BODY="$(head -c 2000 "$body" 2>/dev/null | tr '\n' ' ')"
  if [ "$VERBOSE" = "true" ]; then
    printf "${DIM}    %-9s %-58s -> %s %s${RST}\n" "$label" "$path" "$LAST_CODE" "${LAST_BODY:0:120}" >&2
  fi
  return 0
}

# The twelve @PermitAll GETs from the runbook matrix. "{}" is replaced with the study id.
ENDPOINTS=(
  "assets/publications|api/dataset/study/{}/assets/publications"
  "assets/models|api/dataset/study/{}/assets/models"
  "assets/workspaces|api/dataset/study/{}/assets/workspaces"
  "assets/presentations|api/dataset/study/{}/assets/presentations"
  "assets/clinicalTrials|api/dataset/study/{}/assets/clinicalTrials"
  "assets/intellectualProperty|api/dataset/study/{}/assets/intellectualProperty"
  "assets/fundingResources|api/dataset/study/{}/assets/fundingResources"
  "comments|api/dataset/study/{}/comments"
  "dar-summaries|api/metrics/dar-summaries/study/{}"
  "research-outputs|api/metrics/research-outputs/study/{}"
  "recs/similar|api/metrics/study-recommendations/{}/similar"
  "recs/freq-requested|api/metrics/study-recommendations/{}/frequently-requested-with"
)

PASS=0; FAIL=0; SKIP=0
FAILURES=()
NOTES=("${FIXTURE_NOTES[@]}")

record() { # record RESULT DESCRIPTION [DETAIL]
  case "$1" in
    PASS) PASS=$((PASS + 1));;
    FAIL) FAIL=$((FAIL + 1)); FAILURES+=("$2${3:+ — ${3:0:160}}");;
    SKIP) SKIP=$((SKIP + 1));;
  esac
}

# ---------------------------------------------------------------- persona smoke test

head1 "Persona smoke test — GET public study $PUBLIC_STUDY comments as each caller"

PERSONA_LABELS=(creator custodian admin unrelated)
declare -A PERSONA_OK=()

for label in "${PERSONA_LABELS[@]}"; do
  eval "email=\${EMAIL_$label}"
  if [ -z "$email" ]; then
    PERSONA_OK[$label]="false"
    printf "  %-10s ${YLW}%-6s${RST} no fixture\n" "$label" "SKIP"
    continue
  fi
  request "$email" "$label" "api/dataset/study/$PUBLIC_STUDY/comments"; code="$LAST_CODE"
  # 200 is the expected answer; 404 would mean the fixture is not really public, which
  # assert_visibility has already ruled out. Either way the persona authenticated.
  if [ "$code" = "200" ] || [ "$code" = "404" ]; then
    PERSONA_OK[$label]="true"
    printf "  %-10s ${GRN}%-6s${RST} %s\n" "$label" "$code" "$email"
  else
    PERSONA_OK[$label]="false"
    printf "  %-10s ${RED}%-6s${RST} %s\n" "$label" "$code" "$email"
    printf "             ${DIM}%s${RST}\n" "$LAST_BODY"
    case "$code" in
      401) warn "$label ($email) is not authenticating — DuosUserAuthenticator 401s when the email has no row in users.";;
      500) warn "$label ($email) 500s during authentication — Sam most likely rejected the fabricated token for this user. Try another --$label.";;
    esac
  fi
done

# ---------------------------------------------------------------- V-1

if run_check v1; then
  head1 "V-1 · Visibility matrix"
  echo "  Public study $PUBLIC_STUDY as an outsider, private study $PRIVATE_STUDY as four callers."
  echo "  Every private/unrelated cell must be 404 — a 403 confirms the study exists, which is the leak the 404 prevents."
  echo

  printf "  %-30s %-9s %-9s %-9s %-9s %-9s\n" "ENDPOINT" "PUB/OUT" "PRV/CRT" "PRV/CUS" "PRV/ADM" "PRV/OUT"
  printf "  %-30s %-9s %-9s %-9s %-9s %-9s\n" "------------------------------" "-------" "-------" "-------" "-------" "-------"

  # column label | persona | study id | expected status
  COLUMNS=(
    "PUB/OUT|unrelated|PUBLIC|200"
    "PRV/CRT|creator|PRIVATE|200"
    "PRV/CUS|custodian|PRIVATE|200"
    "PRV/ADM|admin|PRIVATE|200"
    "PRV/OUT|unrelated|PRIVATE|404"
  )

  for entry in "${ENDPOINTS[@]}"; do
    name="${entry%%|*}"; tmpl="${entry#*|}"
    line="$(printf "  %-30s" "$name")"
    for col in "${COLUMNS[@]}"; do
      IFS='|' read -r _ persona which expected <<< "$col"
      eval "email=\${EMAIL_$persona}"
      if [ "${PERSONA_OK[$persona]}" != "true" ]; then
        line+="$(printf " ${YLW}%-9s${RST}" "skip")"
        record SKIP
        continue
      fi
      if [ "$which" = "PUBLIC" ]; then sid="$PUBLIC_STUDY"; else sid="$PRIVATE_STUDY"; fi
      request "$email" "$persona" "${tmpl/\{\}/$sid}"; code="$LAST_CODE"
      if [ "$code" = "$expected" ]; then
        line+="$(printf " ${GRN}%-9s${RST}" "$code")"
        record PASS
      else
        line+="$(printf " ${RED}%-9s${RST}" "$code")"
        record FAIL "V-1 $name [$persona/study $sid] expected $expected got $code" "$LAST_BODY"
        FAILURES+=("      repro: $LAST_CURL")
        if [ "$expected" = "404" ] && [ "$code" = "403" ]; then
          NOTES+=("V-1 $name returned 403 to an unrelated caller on private study $sid. That confirms the study exists — the matrix calls for 404 precisely to avoid this leak.")
        fi
      fi
    done
    printf "%s\n" "$line"
  done
fi

# ---------------------------------------------------------------- V-2

if run_check v2; then
  head1 "V-2 · Nonexistent studyId ($MISSING_STUDY)"
  echo "  Run as the admin persona, so a 404 cannot be a visibility 404 in disguise."
  echo "  Message text differs by family and is recorded, not asserted — the runbook flags it as non-blocking."
  echo

  if [ "${PERSONA_OK[admin]}" = "true" ]; then
    for entry in "${ENDPOINTS[@]}"; do
      name="${entry%%|*}"; tmpl="${entry#*|}"
      request "$EMAIL_admin" "admin" "${tmpl/\{\}/$MISSING_STUDY}"; code="$LAST_CODE"
      msg="$(printf '%s' "$LAST_BODY" | jq -r '.message // empty' 2>/dev/null || true)"
      [ -n "$msg" ] || msg="$LAST_BODY"
      if [ "$code" = "404" ]; then
        printf "  %-30s ${GRN}%-5s${RST} ${DIM}%s${RST}\n" "$name" "$code" "${msg:0:70}"
        record PASS
      else
        printf "  %-30s ${RED}%-5s${RST} %s\n" "$name" "$code" "${msg:0:70}"
        record FAIL "V-2 $name expected 404 for missing study $MISSING_STUDY, got $code" "$LAST_BODY"
        FAILURES+=("      repro: $LAST_CURL")
      fi
    done
  else
    warn "Admin persona unusable — V-2 skipped."
    record SKIP
  fi
fi

# ---------------------------------------------------------------- V-3

if run_check v3; then
  head1 "V-3 · Non-numeric studyId (abc)"
  echo "  Sent with valid persona headers: path param conversion happens after the auth filter, so an"
  echo "  unauthenticated call would 401 and mask the check. Expect 404 from JAX-RS, never 500."
  echo

  if [ "${PERSONA_OK[admin]}" = "true" ]; then
    for entry in "${ENDPOINTS[@]}"; do
      name="${entry%%|*}"; tmpl="${entry#*|}"
      request "$EMAIL_admin" "admin" "${tmpl/\{\}/abc}"; code="$LAST_CODE"
      if [ "$code" = "404" ]; then
        printf "  %-30s ${GRN}%-5s${RST}\n" "$name" "$code"
        record PASS
      else
        printf "  %-30s ${RED}%-5s${RST} %s\n" "$name" "$code" "${LAST_BODY:0:70}"
        record FAIL "V-3 $name expected 404 for non-numeric studyId, got $code" "$LAST_BODY"
        FAILURES+=("      repro: $LAST_CURL")
        [ "$code" = "500" ] && NOTES+=("V-3 $name returned 500 on a non-numeric studyId — JAX-RS path matching should reject it as a 404 before the resource runs.")
      fi
    done
  else
    warn "Admin persona unusable — V-3 skipped."
    record SKIP
  fi
fi

# ---------------------------------------------------------------- summary

LIBRARY_CARDS_AFTER="$(echo "SELECT count(*) FROM library_card;" | psql_q)"
if [ "$LIBRARY_CARDS_AFTER" != "$LIBRARY_CARDS_BEFORE" ]; then
  NOTES+=("library_card rows went from $LIBRARY_CARDS_BEFORE to $LIBRARY_CARDS_AFTER during this run. enforceInstitutionAndLibraryCardRules deleted a card for one of the personas, which is enough to break §3 of the runbook on this database. Restore the dump before testing comments.")
fi

head1 "Summary"
printf "  ${GRN}%d passed${RST}, ${RED}%d failed${RST}, ${YLW}%d skipped${RST}\n" "$PASS" "$FAIL" "$SKIP"
printf "  ${DIM}library_card rows: %s before, %s after${RST}\n" "$LIBRARY_CARDS_BEFORE" "$LIBRARY_CARDS_AFTER"

if [ "${#FAILURES[@]}" -gt 0 ]; then
  head1 "Failures"
  for f in "${FAILURES[@]}"; do printf "  %s\n" "$f"; done
fi

if [ "${#NOTES[@]}" -gt 0 ]; then
  head1 "Notes"
  for n in "${NOTES[@]}"; do printf "  ${YLW}!${RST} %s\n" "$n"; done
fi

if [ -z "$EMAIL_custodian" ]; then
  printf "\n  ${YLW}The private/custodian column did not run.${RST} No study with public_visibility = false in\n"
  printf "  this database has a dataCustodianEmail entry that resolves to a user — the dev dump ships\n"
  printf "  every one of those arrays empty. Re-run with --provision-custodian to have the script\n"
  printf "  borrow a user for the duration, or pass --private-study/--custodian yourself.\n"
fi

if [ "$FAIL" -gt 0 ] || [ "$SKIP" -gt 0 ]; then
  exit 1
fi
exit 0
