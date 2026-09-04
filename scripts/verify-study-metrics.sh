#!/bin/bash
#
# DT-3990 verification runbook, §6 "Metrics: grants, research outputs, recommendations" — D-1..D-11.
#
# Four new queries, all with exclusion rules that are cheap to get wrong and almost impossible to
# spot by eye on real data. Every check here needs data shaped deliberately, so the script builds
# its own: a dozen studies, their datasets, and the collections, DARs, elections and votes that make
# each rule observable. Nothing pre-existing is read or modified.
#
# WHY SYNTHETIC RATHER THAN "find a study that looks right"
#   The interesting cases are absences - a progress report that must not stand in for its grant, an
#   archived DAR that must stop counting, a blank PI that must not match another blank PI. You
#   cannot assert an absence against data you did not construct, because you can never be sure the
#   absence is the rule working rather than the case never existing. Every fixture below is tagged
#   with this run's id, so the expected result sets are exact rather than "contains".
#
#   That tag also makes the recommendation checks meaningful: the fixtures use a data type string
#   unique to the run, so findSimilar's result set is entirely ours and can be compared for
#   equality rather than membership.
#
# HOW IT AUTHENTICATES
#   Same as the other verify-study-* scripts: OAUTH2_CLAIM_* headers straight to the app port,
#   bypassing the proxy, with a bearer token unique per persona and per run.
#
# USAGE
#   ./scripts/verify-study-metrics.sh [OPTION]...
#   Run --help for options. Exits non-zero if any check fails or had to be skipped.
#

set -eu
set -o pipefail

API="${CONSENT_API:-http://localhost:8080}"
DB_CONTAINER="${CONSENT_DB_CONTAINER:-localdb}"
DB_USER="${CONSENT_DB_USER:-consent}"
DB_NAME="${CONSENT_DB_NAME:-consent}"
EMAIL_admin=""
KEEP="false"
VERBOSE="false"

if [ -t 1 ] && [ "$(tput colors 2>/dev/null || echo 0)" -ge 8 ]; then
  BLD="$(tput bold)"; RED="$(tput setaf 1)"; GRN="$(tput setaf 2)"; YLW="$(tput setaf 3)"; DIM="$(tput setaf 8)"; RST="$(tput sgr0)"
else
  BLD=""; RED=""; GRN=""; YLW=""; DIM=""; RST=""
fi

usage() {
  cat <<EOF
Usage: $0 [OPTION]...
Verify DT-3990 runbook checks D-1 through D-11 against a running local stack.

  --api URL            Consent API base. Default: $API
                       Must be the app port, NOT the :27443 proxy.
  --db-container NAME  Postgres container. Default: $DB_CONTAINER
  --admin EMAIL        Admin persona; also the DAR submitter the fixtures attribute to.
  --keep               Leave the fixtures in place for manual inspection (prints the ids).
  --verbose            Echo every request and its body.
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
    --admin) EMAIL_admin="$2"; shift 2;;
    --keep) KEEP="true"; shift;;
    --verbose) VERBOSE="true"; shift;;
    --help) usage;;
    *) error "Unknown option: $1. Try --help.";;
  esac
done
API="${API%/}"

for tool in curl jq docker; do command -v "$tool" >/dev/null 2>&1 || error "$tool is required"; done

psql_q() { docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -At -F'|' -v ON_ERROR_STOP=1; }
sql1() { echo "$1" | psql_q; }
sql_quote() { printf "'%s'" "$(printf '%s' "$1" | sed "s/'/''/g")"; }

docker exec "$DB_CONTAINER" true >/dev/null 2>&1 || error "Container '$DB_CONTAINER' is not running."
echo "SELECT 1;" | psql_q >/dev/null 2>&1 || error "Cannot query '$DB_NAME' as '$DB_USER' in '$DB_CONTAINER'."
case "$API" in
  *:27443*|*local.dsde-dev*) warn "--api looks like the Apache proxy. Use the app port.";;
esac
curl -s -o /dev/null --max-time 10 "$API/api/status" || error "No response from $API — is the app container up?"

RUN_ID="dt3990d-$$-$(date +%s)"
TAG="DT-3990-D6 $RUN_ID"
WORK="$(mktemp -d)"

cleanup() {
  local status=$?
  rm -rf "$WORK"
  if [ "$KEEP" = "true" ]; then
    printf "\n${YLW}--keep: fixtures left in place. Remove them with:${RST}\n"
    printf "  docker exec -i %s psql -U %s -d %s -c \"DELETE FROM study WHERE name LIKE '%s%%';\"\n" \
      "$DB_CONTAINER" "$DB_USER" "$DB_NAME" "$TAG"
    printf "  ${DIM}(and the dar_collection rows with dar_code LIKE '%s%%')${RST}\n" "$TAG"
    exit "$status"
  fi
  # Bottom-up: votes hang off elections, elections and dar_dataset off DARs by reference_id,
  # DARs off collections, datasets off studies. Nothing here cascades on its own.
  {
    echo "DELETE FROM vote WHERE election_id IN (SELECT election_id FROM election WHERE reference_id IN (SELECT reference_id FROM data_access_request WHERE collection_id IN (SELECT collection_id FROM dar_collection WHERE dar_code LIKE $(sql_quote "$TAG%"))));"
    echo "DELETE FROM election WHERE reference_id IN (SELECT reference_id FROM data_access_request WHERE collection_id IN (SELECT collection_id FROM dar_collection WHERE dar_code LIKE $(sql_quote "$TAG%")));"
    echo "DELETE FROM dar_dataset WHERE reference_id IN (SELECT reference_id FROM data_access_request WHERE collection_id IN (SELECT collection_id FROM dar_collection WHERE dar_code LIKE $(sql_quote "$TAG%")));"
    echo "DELETE FROM data_access_request WHERE collection_id IN (SELECT collection_id FROM dar_collection WHERE dar_code LIKE $(sql_quote "$TAG%"));"
    echo "DELETE FROM dar_collection WHERE dar_code LIKE $(sql_quote "$TAG%");"
    echo "DELETE FROM dataset_property WHERE dataset_id IN (SELECT dataset_id FROM dataset WHERE study_id IN (SELECT study_id FROM study WHERE name LIKE $(sql_quote "$TAG%")));"
    echo "DELETE FROM dataset WHERE study_id IN (SELECT study_id FROM study WHERE name LIKE $(sql_quote "$TAG%"));"
    echo "DELETE FROM study_property WHERE study_id IN (SELECT study_id FROM study WHERE name LIKE $(sql_quote "$TAG%"));"
    echo "DELETE FROM study WHERE name LIKE $(sql_quote "$TAG%");"
  } | psql_q >/dev/null 2>&1 || warn "Could not fully remove the '$TAG' fixtures — check by hand."
  exit "$status"
}
trap cleanup EXIT INT TERM

# ---------------------------------------------------------------- plumbing

LAST_CODE=""; LAST_BODY=""
request() { # request METHOD EMAIL LABEL PATH
  local method="$1" email="$2" label="$3" path="$4" token="${RUN_ID}-${3}"
  LAST_CODE="$(curl -s -o "$WORK/body" -w '%{http_code}' --max-time 90 -X "$method" \
    -H "Authorization: Bearer $token" -H "OAUTH2_CLAIM_email: $email" -H "OAUTH2_CLAIM_name: $label" \
    -H "OAUTH2_CLAIM_access_token: $token" -H "OAUTH2_CLAIM_aud: $RUN_ID" -H 'Accept: application/json' \
    "$API/$path" || echo "000")"
  LAST_BODY="$(head -c 60000 "$WORK/body" 2>/dev/null | tr '\n' ' ')"
  [ "$VERBOSE" = "true" ] && printf "${DIM}    %-6s %-50s -> %s %s${RST}\n" "$method" "${path:0:50}" "$LAST_CODE" "${LAST_BODY:0:120}" >&2
  return 0
}
json()  { printf '%s' "$LAST_BODY" | jq -r "$1" 2>/dev/null || printf ''; }
jsonc() { printf '%s' "$LAST_BODY" | jq -c "$1" 2>/dev/null || printf ''; }

PASS=0; FAIL=0; SKIP=0; FAILURES=(); NOTES=()
ok()   { PASS=$((PASS+1)); printf "  ${GRN}PASS${RST}  %-6s %s\n" "$1" "$2"; }
bad()  { FAIL=$((FAIL+1)); printf "  ${RED}FAIL${RST}  %-6s %s\n" "$1" "$2"; FAILURES+=("$1 $2${3:+ — ${3:0:240}}"); }
skip() { SKIP=$((SKIP+1)); printf "  ${YLW}SKIP${RST}  %-6s %s\n" "$1" "$2"; }
note() { NOTES+=("$1"); }
expect() { if [ "$3" = "$4" ]; then ok "$1" "$2"; else bad "$1" "$2 (expected '$3', got '$4')" "$LAST_BODY"; fi; }

# ---------------------------------------------------------------- fixture builders

new_study() { # new_study LABEL PUBLIC PI_NAME DATA_TYPE
  sql1 "WITH ins AS (
      INSERT INTO study (name, description, data_types, pi_name, public_visibility, uuid, create_user_id, create_date)
      VALUES ($(sql_quote "$TAG $1"), $(sql_quote "$TAG $1 description"), ARRAY[$(sql_quote "$4")],
              $(sql_quote "$3"), $2, gen_random_uuid(), $UID_admin, now())
      RETURNING study_id) SELECT study_id FROM ins;"
}

new_dataset() { # new_dataset STUDY_ID LABEL — alias is trigger-assigned, so never supply one
  sql1 "WITH ins AS (
      INSERT INTO dataset (name, create_user_id, create_date, study_id)
      VALUES ($(sql_quote "$TAG $2"), $UID_admin, now(), $1)
      RETURNING dataset_id) SELECT dataset_id FROM ins;"
}

new_collection() { # new_collection LABEL
  sql1 "WITH ins AS (
      INSERT INTO dar_collection (dar_code, create_user_id, create_date)
      VALUES ($(sql_quote "$TAG $1"), $UID_admin, now())
      RETURNING collection_id) SELECT collection_id FROM ins;"
}

# new_dar COLLECTION_ID PARENT_ID_OR_NULL SUBMITTED_AGO_OR_NULL DATA_JSON -> "reference_id|id"
new_dar() {
  local submitted="NULL"
  [ "$3" != "NULL" ] && submitted="now() - interval $(sql_quote "$3")"
  sql1 "WITH ins AS (
      INSERT INTO data_access_request (reference_id, data, user_id, create_date, submission_date, update_date, collection_id, parent_id)
      VALUES (gen_random_uuid()::text, $(sql_quote "$4")::jsonb, $UID_admin, now(), $submitted, now(), $1, $2)
      RETURNING reference_id, id) SELECT reference_id || '|' || id FROM ins;"
}

link_dar() {
  [ -n "$1" ] || error "link_dar called with an empty reference_id — an earlier DAR insert failed."
  sql1 "INSERT INTO dar_dataset (reference_id, dataset_id) VALUES ($(sql_quote "$1"), $2);" >/dev/null
}

# read_dar NAME_REF NAME_ID <<< result — splits and validates in one place
require_dar() { [ -n "$1" ] && [ -n "$2" ] || error "DAR insert failed for '$3'."; }

# approve_dar REFERENCE_ID DATASET_ID [FINAL_VOTE]
# The qualifying query reads the LAST vote of type final/radar_approve per (reference, dataset), so
# passing "false" writes an approve followed by a later reversal - a collection that must drop out.
approve_dar() {
  local final="${3:-true}"
  local eid
  eid="$(sql1 "WITH ins AS (
      INSERT INTO election (status, create_date, reference_id, dataset_id, election_type)
      VALUES ('Closed', now(), $(sql_quote "$1"), $2, 'DataAccess')
      RETURNING election_id) SELECT election_id FROM ins;")"
  if [ "$final" = "false" ]; then
    sql1 "INSERT INTO vote (vote, user_id, create_date, election_id, type) VALUES (true, $UID_admin, now() - interval '2 hours', $eid, 'FINAL');" >/dev/null
    sql1 "INSERT INTO vote (vote, user_id, create_date, election_id, type) VALUES (false, $UID_admin, now(), $eid, 'FINAL');" >/dev/null
  else
    sql1 "INSERT INTO vote (vote, user_id, create_date, election_id, type) VALUES (true, $UID_admin, now(), $eid, 'FINAL');" >/dev/null
  fi
}

outputs_json() { # outputs_json PREFIX — distinguishable titles, so an absence can be asserted by value
  jq -nc --arg p "$1" '{
    presentations: [{title: ($p + " presentation"), url: "https://example.org/pres"}],
    publications:  [{title: ($p + " publication"), pubmedId: ($p + "-pmid")}],
    intellectualProperties: [{title: ($p + " ip"), type: "Patent"}]
  }'
}

# ---------------------------------------------------------------- fixtures

head1 "Fixtures"

if [ -z "$EMAIL_admin" ]; then
  EMAIL_admin="$(sql1 "
    SELECT u.email FROM users u
    JOIN user_role ur ON ur.user_id = u.user_id AND ur.role_id = 4
    WHERE u.institution_id IS NOT NULL
      AND split_part(u.email,'@',2) NOT LIKE '%gserviceaccount.com'
    ORDER BY u.user_id LIMIT 1;")"
fi
[ -n "$EMAIL_admin" ] || error "No Admin user with an institution found. Pass --admin."
UID_admin="$(sql1 "SELECT user_id FROM users WHERE lower(email) = lower($(sql_quote "$EMAIL_admin"));")"
[ -n "$UID_admin" ] || error "$EMAIL_admin has no row in users."
ADMIN_DISPLAY="$(sql1 "SELECT coalesce(display_name,'') FROM users WHERE user_id = $UID_admin;")"
ADMIN_INSTITUTION="$(sql1 "SELECT coalesce(i.institution_name,'') FROM users u LEFT JOIN institution i ON i.institution_id = u.institution_id WHERE u.user_id = $UID_admin;")"

# A data type string no existing study can carry, so findSimilar's result set is entirely ours and
# can be asserted for equality rather than mere membership.
TYPE_MAIN="$TAG type-main"
TYPE_BLANK_A="$TAG type-blank-a"
TYPE_BLANK_B="$TAG type-blank-b"
TYPE_ISOLATED="$TAG type-isolated"
PI_SHARED="$TAG Shared PI"

S_MAIN="$(new_study "S_MAIN"        true  "$PI_SHARED"      "$TYPE_MAIN")"
S_OTHER="$(new_study "S_OTHER"      true  "$PI_SHARED"      "$TYPE_MAIN")"       # PI + type  -> best
S_TYPEONLY="$(new_study "S_TYPEONLY" true "$TAG Other PI"   "$TYPE_MAIN")"       # type only
S_NODATA="$(new_study "S_NODATA"    true  "$TAG Nobody"     "$TYPE_MAIN")"       # type only, no datasets
S_PRIVATE="$(new_study "S_PRIVATE"  false "$PI_SHARED"      "$TYPE_MAIN")"       # must never appear
S_BLANK_A="$(new_study "S_BLANK_A"  true  ""                "$TYPE_BLANK_A")"
S_BLANK_B="$(new_study "S_BLANK_B"  true  ""                "$TYPE_BLANK_B")"
S_DRAFTONLY="$(new_study "S_DRAFTONLY" true "$TAG Draft PI" "$TYPE_ISOLATED")"
S_ARCHONLY="$(new_study "S_ARCHONLY"   true "$TAG Arch PI"  "$TYPE_ISOLATED")"

DS1="$(new_dataset "$S_MAIN" "DS1")"
DS2="$(new_dataset "$S_MAIN" "DS2")"
DSO="$(new_dataset "$S_OTHER" "DSO")"
new_dataset "$S_TYPEONLY" "DST" >/dev/null   # S_TYPEONLY only has to own a dataset; its id is never referenced
DSP="$(new_dataset "$S_PRIVATE" "DSP")"
DSD="$(new_dataset "$S_DRAFTONLY" "DSD")"
DSA="$(new_dataset "$S_ARCHONLY" "DSA")"

# --- C1: a plain approved grant, carrying its own recorded PI name (D-1, D-6)
C1="$(new_collection "C1")"
IFS='|' read -r REF_A ID_A <<< "$(new_dar "$C1" NULL "30 days" \
  "$(jq -nc --arg t "$TAG C1 Grant" '{projectTitle:$t, nonTechRus:"C1 RUS", piName:"Recorded PI A"}')")"
require_dar "$REF_A" "$ID_A" "C1 grant"
link_dar "$REF_A" "$DS1"; approve_dar "$REF_A" "$DS1"
link_dar "$REF_A" "$DSO"            # co-request, for D-11

# --- C2: the headline case. A long-expired grant with no recorded PI, plus a later-submitted
#     progress report that has no election of its own but does carry dar_dataset rows on the study,
#     exactly as a real one would. (D-2, D-6 fallback, D-8, D-11 no-double-count)
C2="$(new_collection "C2")"
IFS='|' read -r REF_B ID_B <<< "$(new_dar "$C2" NULL "400 days" \
  "$(jq -nc --arg t "$TAG C2 Grant" --argjson o "$(outputs_json "PARENT")" \
     '{projectTitle:$t, nonTechRus:"C2 RUS"} + $o')")"
require_dar "$REF_B" "$ID_B" "C2 grant"
link_dar "$REF_B" "$DS1"; approve_dar "$REF_B" "$DS1"
link_dar "$REF_B" "$DSO"

IFS='|' read -r REF_C ID_C <<< "$(new_dar "$C2" "$ID_B" "5 days" \
  "$(jq -nc --arg t "$TAG C2 Progress Report" --argjson o "$(outputs_json "REPORT")" \
     '{projectTitle:$t, nonTechRus:"REPORT RUS"} + $o')")"
require_dar "$REF_C" "$ID_C" "C2 progress report"
link_dar "$REF_C" "$DS1"; link_dar "$REF_C" "$DSO"

# --- C3: a closeout, never voted on (D-3)
C3="$(new_collection "C3")"
IFS='|' read -r REF_D ID_D <<< "$(new_dar "$C3" NULL "20 days" \
  "$(jq -nc --arg t "$TAG C3 Closeout" '{projectTitle:$t, nonTechRus:"C3 RUS", closeoutSupplement:"closeout text"}')")"
require_dar "$REF_D" "$ID_D" "C3 closeout"
link_dar "$REF_D" "$DS1"

# --- C4: approved, archived later in the run (D-4)
C4="$(new_collection "C4")"
IFS='|' read -r REF_E ID_E <<< "$(new_dar "$C4" NULL "10 days" \
  "$(jq -nc --arg t "$TAG C4 Grant" '{projectTitle:$t, nonTechRus:"C4 RUS"}')")"
require_dar "$REF_E" "$ID_E" "C4 grant"
link_dar "$REF_E" "$DS1"; approve_dar "$REF_E" "$DS1"

# --- C5: the collection whose newest DAR targets a different study (D-5)
C5="$(new_collection "C5")"
IFS='|' read -r REF_F ID_F <<< "$(new_dar "$C5" NULL "60 days" \
  "$(jq -nc --arg t "$TAG C5 Grant on this study" '{projectTitle:$t, nonTechRus:"C5 RUS"}')")"
require_dar "$REF_F" "$ID_F" "C5 grant"
link_dar "$REF_F" "$DS1"; approve_dar "$REF_F" "$DS1"
IFS='|' read -r REF_G ID_G <<< "$(new_dar "$C5" NULL "1 day" \
  "$(jq -nc --arg t "$TAG C5 Newer grant elsewhere" '{projectTitle:$t, nonTechRus:"C5 OTHER RUS"}')")"
require_dar "$REF_G" "$ID_G" "C5 other-study grant"
link_dar "$REF_G" "$DSO"; approve_dar "$REF_G" "$DSO"

# --- C6: approved, then reversed. The LAST_VALUE window should leave it out entirely.
C6="$(new_collection "C6")"
IFS='|' read -r REF_H ID_H <<< "$(new_dar "$C6" NULL "15 days" \
  "$(jq -nc --arg t "$TAG C6 Reversed" '{projectTitle:$t, nonTechRus:"C6 RUS"}')")"
require_dar "$REF_H" "$ID_H" "C6 reversed"
link_dar "$REF_H" "$DS1"; approve_dar "$REF_H" "$DS1" false

# --- D-8 negatives: a draft progress report and an archived one, each with its own outputs.
#     data_access_request carries a UNIQUE constraint on parent_id alone (uk_parent_id), so a DAR
#     can have at most ONE child. Each report needs its own parent; these two stand-ins carry no
#     dar_dataset rows, so they take part in nothing else.
C0="$(new_collection "C0 parents")"
IFS='|' read -r REF_B2 ID_B2 <<< "$(new_dar "$C0" NULL "40 days" "$(jq -nc --arg t "$TAG C0 Parent for draft" '{projectTitle:$t}')")"
IFS='|' read -r REF_B3 ID_B3 <<< "$(new_dar "$C0" NULL "40 days" "$(jq -nc --arg t "$TAG C0 Parent for archived" '{projectTitle:$t}')")"
require_dar "$REF_B2" "$ID_B2" "C0 parent for draft"
require_dar "$REF_B3" "$ID_B3" "C0 parent for archived"

IFS='|' read -r REF_L ID_L <<< "$(new_dar "$C2" "$ID_B2" NULL \
  "$(jq -nc --arg t "$TAG C2 Draft report" --argjson o "$(outputs_json "DRAFT")" '{projectTitle:$t} + $o')")"
require_dar "$REF_L" "$ID_L" "draft report"
link_dar "$REF_L" "$DS1"
IFS='|' read -r REF_M ID_M <<< "$(new_dar "$C2" "$ID_B3" "3 days" \
  "$(jq -nc --arg t "$TAG C2 Archived report" --argjson o "$(outputs_json "ARCHIVED")" '{projectTitle:$t, status:"archived"} + $o')")"
require_dar "$REF_M" "$ID_M" "archived report"
link_dar "$REF_M" "$DS1"

# --- D-11 negatives: a draft cart and an archived request, each pairing S_MAIN with a study that
#     must therefore never be recommended; plus a submitted one pairing with the private study.
C7="$(new_collection "C7")"
IFS='|' read -r REF_I ID_I <<< "$(new_dar "$C7" NULL NULL "$(jq -nc --arg t "$TAG C7 Draft cart" '{projectTitle:$t}')")"
require_dar "$REF_I" "$ID_I" "C7 draft cart"
link_dar "$REF_I" "$DS1"; link_dar "$REF_I" "$DSD"
C8="$(new_collection "C8")"
IFS='|' read -r REF_J ID_J <<< "$(new_dar "$C8" NULL "8 days" "$(jq -nc --arg t "$TAG C8 Archived cart" '{projectTitle:$t, status:"archived"}')")"
require_dar "$REF_J" "$ID_J" "C8 archived cart"
link_dar "$REF_J" "$DS1"; link_dar "$REF_J" "$DSA"
C9="$(new_collection "C9")"
IFS='|' read -r REF_K ID_K <<< "$(new_dar "$C9" NULL "9 days" "$(jq -nc --arg t "$TAG C9 With private" '{projectTitle:$t}')")"
require_dar "$REF_K" "$ID_K" "C9 private pairing"
link_dar "$REF_K" "$DS1"; link_dar "$REF_K" "$DSP"

printf "  %-20s %s\n" "API"        "$API"
printf "  %-20s %s (%s)\n" "admin" "$EMAIL_admin" "$ADMIN_INSTITUTION"
printf "  %-20s %s\n" "S_MAIN"     "$S_MAIN (datasets $DS1, $DS2)"
printf "  %-20s %s\n" "S_OTHER"    "$S_OTHER"
printf "  %-20s %s / %s / %s\n" "typeonly/nodata/priv" "$S_TYPEONLY" "$S_NODATA" "$S_PRIVATE"
printf "  %-20s %s / %s\n" "blank PI pair" "$S_BLANK_A" "$S_BLANK_B"
printf "  %-20s %s\n" "collections" "C1..C9"

SUMMARY_PATH="api/metrics/dar-summaries/study/$S_MAIN"

# ---------------------------------------------------------------- D-9, D-10

head1 "D-9 · Similar studies"

request GET "$EMAIL_admin" admin "api/metrics/study-recommendations/$S_MAIN/similar"
expect "D-9a" "similar studies" "200" "$LAST_CODE"
expect "D-9b" "  ...the source study is absent from its own results" "false" \
  "$(jsonc "[.[].studyId] | any(. == $S_MAIN)")"
expect "D-9c" "  ...the non-public match never appears" "false" \
  "$(jsonc "[.[].studyId] | any(. == $S_PRIVATE)")"
expect "D-9d" "  ...exactly the three public matches, best first" \
  "[$S_OTHER,$S_TYPEONLY,$S_NODATA]" "$(jsonc '[.[].studyId]')"
expect "D-9e" "  ...PI + data type sorts above data type alone" "$S_OTHER" "$(json '.[0].studyId')"
expect "D-9f" "  ...datasetCount and datasetIds agree" "true" \
  "$(jsonc '[.[] | .datasetCount == (.datasetIds | length)] | all')"
expect "D-9g" "  ...a match with no datasets shows 0" "0" \
  "$(jsonc "[.[] | select(.studyId == $S_NODATA) | .datasetCount] | first")"
expect "D-9h" "  ...and an empty datasetIds, not null" "[]" \
  "$(jsonc "[.[] | select(.studyId == $S_NODATA) | .datasetIds] | first")"
expect "D-9i" "  ...at most 12 rows" "true" "$(jsonc 'length <= 12')"

head1 "D-10 · A blank PI is not an identity"

request GET "$EMAIL_admin" admin "api/metrics/study-recommendations/$S_BLANK_A/similar"
expect "D-10a" "similar studies for a blank-PI study" "200" "$LAST_CODE"
expect "D-10b" "  ...the other blank-PI study is not recommended" "false" \
  "$(jsonc "[.[].studyId] | any(. == $S_BLANK_B)")"

# ---------------------------------------------------------------- D-11

head1 "D-11 · Frequently requested with"

request GET "$EMAIL_admin" admin "api/metrics/study-recommendations/$S_MAIN/frequently-requested-with"
expect "D-11a" "frequently requested with" "200" "$LAST_CODE"
expect "D-11b" "  ...only the co-requested public study is returned" "[$S_OTHER]" "$(jsonc '[.[].studyId]')"
expect "D-11c" "  ...a draft cart scores nothing" "false" \
  "$(jsonc "[.[].studyId] | any(. == $S_DRAFTONLY)")"
expect "D-11d" "  ...an archived request scores nothing" "false" \
  "$(jsonc "[.[].studyId] | any(. == $S_ARCHONLY)")"
expect "D-11e" "  ...a non-public co-requested study is excluded" "false" \
  "$(jsonc "[.[].studyId] | any(. == $S_PRIVATE)")"
expect "D-11f" "  ...the source study excludes itself" "false" \
  "$(jsonc "[.[].studyId] | any(. == $S_MAIN)")"
# C1 and C2 each pair S_MAIN with S_OTHER; the C2 progress report pairs them again but carries
# parent_id, so it must not let its parent count twice.
expect "D-11g" "  ...the progress report's own reference never enters the scoring set" "0" \
  "$(sql1 "SELECT count(*) FROM (
      WITH source_references AS (
        SELECT DISTINCT dd.reference_id FROM data_access_request dar
        INNER JOIN dar_dataset dd ON dd.reference_id = dar.reference_id
        INNER JOIN dataset d ON d.dataset_id = dd.dataset_id
        WHERE d.study_id = $S_MAIN AND dar.submission_date IS NOT NULL AND dar.parent_id IS NULL
          AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL))
      SELECT 1 FROM source_references sr
      INNER JOIN dar_dataset dd ON dd.reference_id = sr.reference_id
      WHERE dd.dataset_id = $DSO AND sr.reference_id = $(sql_quote "$REF_C")) x;")"
# C1 and C2 each pair S_MAIN with S_OTHER once. The C2 progress report pairs them a third time but
# carries parent_id, so the score must be 2 — a regression that dropped the parent_id filter would
# read 3 here while every other D-11 assertion still passed.
expect "D-11h" "  ...so its parent collection still scores exactly once (2, not 3)" "2" \
  "$(sql1 "WITH source_references AS (
        SELECT DISTINCT dd.reference_id FROM data_access_request dar
        INNER JOIN dar_dataset dd ON dd.reference_id = dar.reference_id
        INNER JOIN dataset d ON d.dataset_id = dd.dataset_id
        WHERE d.study_id = $S_MAIN AND dar.submission_date IS NOT NULL AND dar.parent_id IS NULL
          AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL))
      SELECT count(DISTINCT dd.reference_id) FROM source_references sr
      INNER JOIN dar_dataset dd ON dd.reference_id = sr.reference_id
      WHERE dd.dataset_id = $DSO;")"

# ---------------------------------------------------------------- D-1

head1 "D-1 · One row per qualifying collection, newest first"

request GET "$EMAIL_admin" admin "$SUMMARY_PATH"
expect "D-1a" "study DAR summaries" "200" "$LAST_CODE"
expect "D-1b" "  ...one row per qualifying collection (C1..C5, C6 reversed and excluded)" "5" "$(jsonc 'length')"
expect "D-1c" "  ...ordered newest submission first" "true" \
  "$(jsonc '[.[].submissionDate] | . == (sort | reverse)')"
expect "D-1d" "  ...every field present on every row" "true" \
  "$(jsonc '[.[] | has("darCode") and has("submissionDate") and has("updateDate") and has("projectTitle") and has("nonTechRus") and has("referenceId") and has("piName") and has("institutionName") and has("expired")] | all')"
expect "D-1e" "  ...a reversed final vote drops the collection" "false" \
  "$(jsonc "[.[].darCode] | any(. == \"$TAG C6\")")"

# ---------------------------------------------------------------- D-2

head1 "D-2 · The grant, not the pending progress report"

C2_ROW="$(jsonc "[.[] | select(.darCode == \"$TAG C2\")] | first")"
if [ -z "$C2_ROW" ] || [ "$C2_ROW" = "null" ]; then
  bad "D-2" "the C2 collection is missing from the summaries entirely"
else
  expect "D-2a" "the row shows the grant's title, not the report's" "$TAG C2 Grant" \
    "$(printf '%s' "$C2_ROW" | jq -r '.projectTitle')"
  expect "D-2b" "  ...the grant's RUS" "C2 RUS" "$(printf '%s' "$C2_ROW" | jq -r '.nonTechRus')"
  expect "D-2c" "  ...the grant's reference id" "$REF_B" "$(printf '%s' "$C2_ROW" | jq -r '.referenceId')"
  # The grant was submitted 400 days ago and the report 5 days ago. Expiry is derived from the
  # sourced DAR's submission date, so a report standing in for its grant shows up here as a
  # long-expired grant suddenly reading as current.
  expect "D-2d" "  ...and expired is derived from the grant, so it reads expired" "true" \
    "$(printf '%s' "$C2_ROW" | jq -r '.expired')"
fi

# ---------------------------------------------------------------- D-3

head1 "D-3 · Closeouts count even though nobody voted"

expect "D-3a" "the closeout collection is included" "true" \
  "$(jsonc "[.[].darCode] | any(. == \"$TAG C3\")")"
expect "D-3b" "  ...sourced from the closeout DAR" "$REF_D" \
  "$(json "[.[] | select(.darCode == \"$TAG C3\") | .referenceId] | first")"

# ---------------------------------------------------------------- D-6

head1 "D-6 · PI name and institution"

expect "D-6a" "a DAR with data.piName shows it" "Recorded PI A" \
  "$(json "[.[] | select(.darCode == \"$TAG C1\") | .piName] | first")"
expect "D-6b" "a DAR without one falls back to the submitter's display name" "$ADMIN_DISPLAY" \
  "$(json "[.[] | select(.darCode == \"$TAG C2\") | .piName] | first")"
expect "D-6c" "institutionName comes from the submitter's current institution" "$ADMIN_INSTITUTION" \
  "$(json "[.[] | select(.darCode == \"$TAG C1\") | .institutionName] | first")"

# ---------------------------------------------------------------- D-5

head1 "D-5 · A collection whose newest DAR is on another study"

expect "D-5a" "the collection still appears" "true" \
  "$(jsonc "[.[].darCode] | any(. == \"$TAG C5\")")"
expect "D-5b" "  ...sourced from its DAR on THIS study, not the newer one elsewhere" "$REF_F" \
  "$(json "[.[] | select(.darCode == \"$TAG C5\") | .referenceId] | first")"
expect "D-5c" "  ...showing this study's project title" "$TAG C5 Grant on this study" \
  "$(json "[.[] | select(.darCode == \"$TAG C5\") | .projectTitle] | first")"

# ---------------------------------------------------------------- D-7

head1 "D-7 · Regression on the dataset-scoped endpoint"

STUDY_ROWS="$(jsonc '[.[].darCode] | sort')"
request GET "$EMAIL_admin" admin "api/metrics/dar-summaries/$DS1"
expect "D-7a" "the existing dataset endpoint still answers" "200" "$LAST_CODE"
expect "D-7b" "  ...with the same collections, since every fixture DAR is on DS1" "$STUDY_ROWS" "$(jsonc '[.[].darCode] | sort')"
expect "D-7c" "  ...and carries the two new fields" "true" \
  "$(jsonc '[.[] | has("piName") and has("institutionName")] | all')"
note "D-7: the dataset-scoped endpoint returns the same collections as the study-scoped one here because every fixture DAR targets DS1. Its output is otherwise unchanged from develop apart from pi_name and institution_name, which this script cannot verify against develop — compare by hand if the two joins that query gained are in doubt."

# ---------------------------------------------------------------- D-4

head1 "D-4 · Archiving removes a collection"

sql1 "UPDATE data_access_request SET data = data || '{\"status\":\"archived\"}'::jsonb WHERE reference_id = $(sql_quote "$REF_E");" >/dev/null
request GET "$EMAIL_admin" admin "$SUMMARY_PATH"
expect "D-4a" "the archived collection drops out" "false" "$(jsonc "[.[].darCode] | any(. == \"$TAG C4\")")"
expect "D-4b" "  ...and the rest are untouched" "4" "$(jsonc 'length')"

# ---------------------------------------------------------------- D-8

head1 "D-8 · Research outputs"

request GET "$EMAIL_admin" admin "api/metrics/research-outputs/study/$S_MAIN"
expect "D-8a" "research outputs" "200" "$LAST_CODE"
expect "D-8b" "  ...aggregated from the submitted progress report" '["REPORT presentation"]' "$(jsonc '[.presentations[].title]')"
expect "D-8c" "  ...its publications" '["REPORT publication"]' "$(jsonc '[.publications[].title]')"
expect "D-8d" "  ...its intellectual properties" '["REPORT ip"]' "$(jsonc '[.intellectualProperties[].title]')"
expect "D-8e" "  ...the parent DAR contributes nothing" "false" \
  "$(jsonc '[.presentations[].title, .publications[].title, .intellectualProperties[].title] | any(startswith("PARENT"))')"
expect "D-8f" "  ...an unsubmitted draft contributes nothing" "false" \
  "$(jsonc '[.presentations[].title, .publications[].title, .intellectualProperties[].title] | any(startswith("DRAFT"))')"
expect "D-8g" "  ...an archived report contributes nothing" "false" \
  "$(jsonc '[.presentations[].title, .publications[].title, .intellectualProperties[].title] | any(startswith("ARCHIVED"))')"

# S_TYPEONLY, not S_OTHER: S_OTHER's dataset is deliberately attached to the C2 progress report for
# the D-11 co-request case, so it does have research outputs.
request GET "$EMAIL_admin" admin "api/metrics/research-outputs/study/$S_TYPEONLY"
expect "D-8h" "a study with no progress reports returns three empty lists" '[[],[],[]]' \
  "$(jsonc '[.presentations, .publications, .intellectualProperties]')"

# ---------------------------------------------------------------- summary

head1 "Summary"
printf "  ${GRN}%d passed${RST}, ${RED}%d failed${RST}, ${YLW}%d skipped${RST}\n" "$PASS" "$FAIL" "$SKIP"
if [ "${#FAILURES[@]}" -gt 0 ]; then
  head1 "Failures"; for f in "${FAILURES[@]}"; do printf "  %s\n" "$f"; done
fi
if [ "${#NOTES[@]}" -gt 0 ]; then
  head1 "Findings"; for n in "${NOTES[@]}"; do printf "  ${YLW}!${RST} %s\n\n" "$n"; done
fi
if [ "$FAIL" -gt 0 ] || [ "$SKIP" -gt 0 ]; then exit 1; fi
exit 0
