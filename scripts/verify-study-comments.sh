#!/bin/bash
#
# DT-3990 verification runbook, §3 "Comments & ratings" — checks C-1 through C-11.
#
# Unlike §2, this section is a SEQUENCE, not a matrix: the write path is an upsert, so almost every
# check depends on the row state the previous one left behind. The order below is deliberate and
# the checks are not individually selectable.
#
# WHAT IT WRITES
#   By default the script creates its own throwaway publicly-visible study, does all its posting
#   there, and drops it on exit (which is also how C-11 proves the FK cascade). No real study is
#   touched except for one read-only DELETE probe in C-10 that is expected to fail with 404.
#   Pass --study ID to run against a real study instead; the script then refuses to start if any
#   persona already holds a comment there (the upsert would clobber it), deletes only the rows it
#   created, and skips C-11.
#
# HOW IT AUTHENTICATES AS FOUR PEOPLE
#   Same mechanism as scripts/verify-study-visibility.sh: it talks to the app port directly,
#   bypassing the proxy, and supplies the OAUTH2_CLAIM_* headers itself. Each persona gets a bearer
#   token unique to the persona and to the run, because ClaimsCache is get-if-absent with a 5 minute
#   TTL. See that script's header for the full explanation.
#
# THE FIXTURE THAT MATTERS
#   Commenting needs the Researcher role AND a library_card row, and
#   AuthorizationHelper.buildAuthUserFromHeaders re-runs enforceInstitutionAndLibraryCardRules on
#   every authenticated request. A card is DELETED unless:
#     * the user's email domain appears in institution_domains, and
#     * the card's issuer (library_card.create_user_id) has an email domain mapping to that SAME
#       institution.
#   Persona discovery below selects only researchers that satisfy both, so the card survives the
#   run. Card presence is asserted per persona at the end — if one disappears, every later check in
#   this section is meaningless and the script says so.
#
# USAGE
#   ./scripts/verify-study-comments.sh [OPTION]...
#   Run --help for options. Exits non-zero if any check fails or had to be skipped.
#

set -eu
set -o pipefail

API="${CONSENT_API:-http://localhost:8080}"
DB_CONTAINER="${CONSENT_DB_CONTAINER:-localdb}"
DB_USER="${CONSENT_DB_USER:-consent}"
DB_NAME="${CONSENT_DB_NAME:-consent}"

STUDY=""
REAL_STUDY=""
EMAIL_r1=""
EMAIL_r2=""
EMAIL_r3=""
EMAIL_nocard=""
EMAIL_cardonly=""
EMAIL_noroles=""
SKIP_CASCADE="false"
VERBOSE="false"

if [ -t 1 ] && [ "$(tput colors 2>/dev/null || echo 0)" -ge 8 ]; then
  BLD="$(tput bold)"; RED="$(tput setaf 1)"; GRN="$(tput setaf 2)"; YLW="$(tput setaf 3)"; DIM="$(tput setaf 8)"; RST="$(tput sgr0)"
else
  BLD=""; RED=""; GRN=""; YLW=""; DIM=""; RST=""
fi

usage() {
  cat <<EOF
Usage: $0 [OPTION]...
Verify DT-3990 runbook checks C-1 through C-11 against a running local stack.

  --api URL          Consent API base. Default: $API
                     Must be the app port, NOT the :27443 proxy.
  --db-container NAME  Postgres container. Default: $DB_CONTAINER
  --study ID         Run against this existing study instead of a throwaway one.
                     Refuses to start if a persona already has a comment there. Skips C-11.
  --researcher EMAIL     Persona for C-4/C-5/C-9/C-10. Needs Researcher role + a durable card.
  --researcher2 EMAIL    Second researcher, for C-6/C-7/C-8/C-10.
  --nocard EMAIL     Researcher with no library card, for C-1 and C-2.
  --cardonly EMAIL   Card holder WITHOUT the Researcher role, for C-3.
  --skip-cascade     Skip C-11 (the study delete / FK cascade check).
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
    --study) STUDY="$2"; shift 2;;
    --researcher) EMAIL_r1="$2"; shift 2;;
    --researcher2) EMAIL_r2="$2"; shift 2;;
    --researcher3) EMAIL_r3="$2"; shift 2;;
    --nocard) EMAIL_nocard="$2"; shift 2;;
    --cardonly) EMAIL_cardonly="$2"; shift 2;;
    --skip-cascade) SKIP_CASCADE="true"; shift;;
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
SYNTHETIC_STUDY=""
CREATED_COMMENT_USERS=()

cleanup() {
  local status=$?
  if [ -n "$SYNTHETIC_STUDY" ]; then
    # A no-op when C-11 already dropped it; the cascade takes any comments with it.
    printf "%s\n" "DELETE FROM study WHERE study_id = $SYNTHETIC_STUDY;" | psql_q >/dev/null 2>&1 \
      || warn "Could not drop throwaway study $SYNTHETIC_STUDY — remove it by hand."
  elif [ -n "$STUDY" ] && [ "${#CREATED_COMMENT_USERS[@]}" -gt 0 ]; then
    local ids
    ids="$(printf '%s,' "${CREATED_COMMENT_USERS[@]}")"
    printf "%s\n" "DELETE FROM study_comment WHERE study_id = $STUDY AND user_id IN (${ids%,});" | psql_q >/dev/null 2>&1 \
      || warn "Could not clean up comments on study $STUDY — check study_comment by hand."
  fi
  rm -rf "$WORK"
  exit "$status"
}
trap cleanup EXIT INT TERM

# ---------------------------------------------------------------- fixtures

# A researcher whose card survives enforceInstitutionAndLibraryCardRules: their own domain maps to
# an institution, they are assigned to it, and their card's issuer maps to the same institution.
durable_researchers() {
  echo "
    SELECT u.email FROM users u
    JOIN user_role ur ON ur.user_id = u.user_id AND ur.role_id = 5
    JOIN library_card lc ON lc.user_id = u.user_id
    JOIN institution_domains ud ON lower(ud.domain) = lower(split_part(u.email, '@', 2))
    JOIN users iss ON iss.user_id = lc.create_user_id
    JOIN institution_domains isd ON lower(isd.domain) = lower(split_part(iss.email, '@', 2))
    WHERE ud.institution_id = isd.institution_id
      AND u.institution_id = ud.institution_id
    ORDER BY (EXISTS (SELECT 1 FROM user_role a WHERE a.user_id = u.user_id AND a.role_id = 4)),
             u.user_id
    LIMIT 8;" | psql_q
}

discover_researchers() {
  local pool candidate
  mapfile -t pool < <(durable_researchers)
  for candidate in "${pool[@]}"; do
    case "$candidate" in
      "$EMAIL_r1"|"$EMAIL_r2"|"$EMAIL_r3") continue;;
    esac
    if   [ -z "$EMAIL_r1" ]; then EMAIL_r1="$candidate"
    elif [ -z "$EMAIL_r2" ]; then EMAIL_r2="$candidate"
    elif [ -z "$EMAIL_r3" ]; then EMAIL_r3="$candidate"
    else break
    fi
  done
  [ -n "$EMAIL_r1" ] || error "Could not pick a researcher with a durable library card. Pass --researcher."
  [ -n "$EMAIL_r2" ] || error "Could not pick a second, distinct researcher. Pass --researcher2."
  [ -n "$EMAIL_r3" ] || error "Could not pick a third, distinct researcher. Pass --researcher3."
  # One comment per user per study is enforced by uq_study_comment_study_user, so three separate
  # accounts are the only way to get three comments onto one study.
  if [ "$EMAIL_r1" = "$EMAIL_r2" ] || [ "$EMAIL_r1" = "$EMAIL_r3" ] || [ "$EMAIL_r2" = "$EMAIL_r3" ]; then
    error "--researcher, --researcher2 and --researcher3 must all be different people."
  fi
}

discover_nocard() {
  [ -n "$EMAIL_nocard" ] && return 0
  # Prefer a mapped domain: an unmapped one (gmail, say) gets institution_id nulled on every
  # request by handleUserWithoutInstitutionInMap, which is noise we don't need.
  EMAIL_nocard="$(echo "
    SELECT u.email FROM users u
    JOIN user_role ur ON ur.user_id = u.user_id AND ur.role_id = 5
    WHERE NOT EXISTS (SELECT 1 FROM library_card lc WHERE lc.user_id = u.user_id)
    ORDER BY (NOT EXISTS (SELECT 1 FROM institution_domains d
                          WHERE lower(d.domain) = lower(split_part(u.email, '@', 2)))),
             u.user_id
    LIMIT 1;" | psql_q)"
  [ -n "$EMAIL_nocard" ] || error "No Researcher without a library card found. Pass --nocard."
}

discover_cardonly() {
  [ -n "$EMAIL_cardonly" ] && return 0
  # Prefer a card holder who holds at least one non-Researcher role. A user with NO roles at all
  # takes a different path entirely — see EMAIL_noroles and C-3c below.
  EMAIL_cardonly="$(echo "
    SELECT u.email FROM users u
    JOIN library_card lc ON lc.user_id = u.user_id
    JOIN institution_domains ud ON lower(ud.domain) = lower(split_part(u.email, '@', 2))
    JOIN users iss ON iss.user_id = lc.create_user_id
    JOIN institution_domains isd ON lower(isd.domain) = lower(split_part(iss.email, '@', 2))
    WHERE ud.institution_id = isd.institution_id
      AND u.institution_id = ud.institution_id
      AND NOT EXISTS (SELECT 1 FROM user_role x WHERE x.user_id = u.user_id AND x.role_id = 5)
    ORDER BY ((SELECT count(*) FROM user_role x WHERE x.user_id = u.user_id) = 0), u.user_id
    LIMIT 1;" | psql_q)"
  [ -n "$EMAIL_cardonly" ] || error "No card holder without the Researcher role found. Pass --cardonly."
}

# A user with no user_role rows at all. AuthorizationHelper.authorize does
# user.getRoles().stream() with no null guard, and User.getRoles() is null for such a user, so the
# @RolesAllowed check throws instead of denying. C-3c pins that down.
discover_noroles() {
  EMAIL_noroles="$(echo "
    SELECT u.email FROM users u
    WHERE NOT EXISTS (SELECT 1 FROM user_role x WHERE x.user_id = u.user_id)
    ORDER BY u.user_id LIMIT 1;" | psql_q)"
}

user_id_for() { echo "SELECT user_id FROM users WHERE lower(email) = lower($(sql_quote "$1"));" | psql_q; }

head1 "Fixtures"
discover_researchers
discover_nocard
discover_cardonly
discover_noroles

UID_r1="$(user_id_for "$EMAIL_r1")"
UID_r2="$(user_id_for "$EMAIL_r2")"
UID_r3="$(user_id_for "$EMAIL_r3")"

# C-10 needs a real, visible study to aim a valid comment id at the wrong study. requireStudy runs
# before the ownership check, so an invisible one would 404 for the wrong reason.
REAL_STUDY="$(echo "
  SELECT study_id FROM study WHERE public_visibility IS TRUE ORDER BY study_id LIMIT 1;" | psql_q)"
[ -n "$REAL_STUDY" ] || error "No publicly visible study found for the C-10 wrong-study probe."

if [ -n "$STUDY" ]; then
  [ "$(echo "SELECT count(*) FROM study WHERE study_id = $STUDY;" | psql_q)" = "1" ] \
    || error "Study $STUDY does not exist."
  [ "$(echo "SELECT coalesce(public_visibility, false) FROM study WHERE study_id = $STUDY;" | psql_q)" = "t" ] \
    || error "Study $STUDY is not publicly visible; the personas could not all read it."
  # C-8a/C-8b assert an empty summary, C-2c and the payload checks assert a study-wide row count
  # of zero, and C-8d assumes the only two comments are the ones this run creates. Any pre-existing
  # comment breaks all of them — and an existing row for r1 or r2 would additionally be clobbered
  # by the upsert and then deleted during cleanup.
  existing="$(echo "SELECT count(*) FROM study_comment WHERE study_id = $STUDY;" | psql_q)"
  [ "$existing" = "0" ] \
    || error "Study $STUDY already has $existing comment(s). This section asserts an empty starting state, and the upsert would overwrite a real row belonging to $EMAIL_r1 or $EMAIL_r2. Pick a study with no comments, or omit --study to use a throwaway one."
  [ "$SKIP_CASCADE" = "true" ] || warn "C-11 deletes the study it runs on, so it is skipped when --study is given."
  SKIP_CASCADE="true"
else
  SYNTHETIC_STUDY="$(echo "
    WITH ins AS (
      INSERT INTO study (name, description, data_types, pi_name, public_visibility,
                         uuid, create_user_id, create_date)
      VALUES ('DT-3990 comment verification ' || gen_random_uuid(),
              'Throwaway study created by verify-study-comments.sh', ARRAY['Verification'],
              'DT-3990 Verification', true, gen_random_uuid(), $UID_r1, now())
      RETURNING study_id
    ) SELECT study_id FROM ins;" | psql_q)"
  [ -n "$SYNTHETIC_STUDY" ] || error "Could not create the throwaway study."
  STUDY="$SYNTHETIC_STUDY"
fi

LIBRARY_CARDS_BEFORE="$(echo "SELECT count(*) FROM library_card;" | psql_q)"

printf "  %-22s %s\n" "API"           "$API"
printf "  %-22s %s%s\n" "study"       "$STUDY" "${SYNTHETIC_STUDY:+ (throwaway, dropped on exit)}"
printf "  %-22s %s\n" "other study"   "$REAL_STUDY (read-only, for C-10)"
printf "  %-22s %s (id %s)\n" "researcher"  "$EMAIL_r1" "$UID_r1"
printf "  %-22s %s (id %s)\n" "researcher2" "$EMAIL_r2" "$UID_r2"
printf "  %-22s %s (id %s)\n" "researcher3" "$EMAIL_r3" "$UID_r3"
printf "  %-22s %s\n" "researcher, no card" "$EMAIL_nocard"
printf "  %-22s %s\n" "card, no researcher" "$EMAIL_cardonly"
printf "  %-22s %s\n" "no roles at all"     "${EMAIL_noroles:-none found — C-3c will be skipped}"

# ---------------------------------------------------------------- plumbing

RUN_ID="dt3990c-$$-$(date +%s)"
LAST_CODE=""; LAST_BODY=""; LAST_CURL=""

# request METHOD EMAIL LABEL PATH [BODY]
# Sets LAST_CODE / LAST_BODY / LAST_CURL. Runs in the current shell on purpose: a command
# substitution would take the failure detail down with the subshell.
request() {
  local method="$1" email="$2" label="$3" path="$4"
  local token="${RUN_ID}-${label}"
  local body_file="$WORK/body"
  local -a args=(-s -o "$body_file" -w '%{http_code}' --max-time 60 -X "$method"
    -H "Authorization: Bearer $token"
    -H "OAUTH2_CLAIM_email: $email"
    -H "OAUTH2_CLAIM_name: $label"
    -H "OAUTH2_CLAIM_access_token: $token"
    -H "OAUTH2_CLAIM_aud: $RUN_ID"
    -H 'Accept: application/json')

  LAST_CURL="curl -X $method -H 'Authorization: Bearer $token' -H 'OAUTH2_CLAIM_email: $email' -H 'OAUTH2_CLAIM_access_token: $token' -H 'OAUTH2_CLAIM_aud: $RUN_ID'"
  if [ "$#" -ge 5 ]; then
    # --data-binary, not --data: an empty payload must still go out as Content-Length: 0 so the
    # resource sees "" and Gson returns null. That is the C-7 empty-body case.
    args+=(-H 'Content-Type: application/json' --data-binary "$5")
    LAST_CURL="$LAST_CURL -H 'Content-Type: application/json' --data-binary '$5'"
  fi
  LAST_CURL="$LAST_CURL '$API/$path'"

  LAST_CODE="$(curl "${args[@]}" "$API/$path" || echo "000")"
  LAST_BODY="$(head -c 4000 "$body_file" 2>/dev/null | tr '\n' ' ')"
  if [ "$VERBOSE" = "true" ]; then
    printf "${DIM}    %-6s %-9s %-52s -> %s %s${RST}\n" \
      "$method" "$label" "${path:0:52}" "$LAST_CODE" "${LAST_BODY:0:140}" >&2
  fi
  return 0
}

json() { printf '%s' "$LAST_BODY" | jq -r "$1" 2>/dev/null || printf ''; }
sql1() { echo "$1" | psql_q; }

PASS=0; FAIL=0; SKIP=0
FAILURES=(); NOTES=()

ok()   { PASS=$((PASS + 1)); printf "  ${GRN}PASS${RST}  %-6s %s\n" "$1" "$2"; }
bad()  { FAIL=$((FAIL + 1)); printf "  ${RED}FAIL${RST}  %-6s %s\n" "$1" "$2"
         FAILURES+=("$1 $2${3:+ — ${3:0:200}}")
         [ -n "${LAST_CURL:-}" ] && FAILURES+=("        repro: $LAST_CURL")
         return 0; }   # never let an empty LAST_CURL make bad() fail under set -e
skip() { SKIP=$((SKIP + 1)); printf "  ${YLW}SKIP${RST}  %-6s %s\n" "$1" "$2"; }
note() { NOTES+=("$1"); }

# expect_num ID DESCRIPTION EXPECTED ACTUAL — compares as floats, since psql renders 3 where the
# JSON body carries 3.0.
expect_num() {
  if [ -n "$4" ] && [ "$4" != "null" ] \
     && [ "$(awk -v a="$3" -v b="$4" 'BEGIN{d=a-b; if(d<0)d=-d; print (d<0.0001)?"y":"n"}')" = "y" ]; then
    ok "$1" "$2 ($4)"
  else
    bad "$1" "$2 (expected '$3', got '$4')" "$LAST_BODY"
  fi
}

# expect ID DESCRIPTION EXPECTED ACTUAL
expect() {
  if [ "$3" = "$4" ]; then ok "$1" "$2"; else bad "$1" "$2 (expected '$3', got '$4')" "$LAST_BODY"; fi
}

COMMENTS="api/dataset/study/$STUDY/comments"

# ---------------------------------------------------------------- C-8 (empty), C-1, C-2, C-3
# Everything that must observe a study with no comments on it runs first.

head1 "Reads and the two 403 gates — before anything is written"

request GET "$EMAIL_nocard" nocard "$COMMENTS"
expect "C-8a" "empty study: averageRating is null, not 0" "null" "$(json '.averageRating')"
expect "C-8b" "empty study: comments is an empty list" "0" "$(json '.comments | length')"

request GET "$EMAIL_nocard" nocard "$COMMENTS"
expect "C-1" "GET comments with no library card" "200" "$LAST_CODE"

request POST "$EMAIL_nocard" nocard "$COMMENTS" '{"rating":4,"commentText":"no card"}'
expect "C-2a" "POST as Researcher with no card is refused" "403" "$LAST_CODE"
expect "C-2b" "  ...by the service, with its own message" \
  "Active Researcher Status is required to comment or rate this study." "$(json '.message')"

request POST "$EMAIL_cardonly" cardonly "$COMMENTS" '{"rating":4,"commentText":"card but no role"}'
expect "C-3a" "POST with a card but not the Researcher role is refused" "403" "$LAST_CODE"
# The point of C-3 is that @RolesAllowed rejects BEFORE StudyCommentService runs, so the service's
# own message must NOT be what comes back.
if [ "$(json '.message')" = "Active Researcher Status is required to comment or rate this study." ]; then
  bad "C-3b" "  ...by @RolesAllowed, not the service" "service message returned, so the role gate did not fire first"
else
  ok "C-3b" "  ...by @RolesAllowed, not the service"
  printf "        ${DIM}framework message: %s${RST}\n" "$(json '.message // .code // empty')"
fi

if [ -n "$EMAIL_noroles" ]; then
  request POST "$EMAIL_noroles" noroles "$COMMENTS" '{"rating":4,"commentText":"no roles"}'
  if [ "$LAST_CODE" = "403" ]; then
    ok "C-3c" "POST as a user with no roles at all is refused"
  elif [ "$LAST_CODE" = "500" ]; then
    bad "C-3c" "POST as a user with no roles at all returned 500, not 403" "$LAST_BODY"
    note "C-3c: a user with NO user_role rows gets a 500, not a 403, from every @RolesAllowed endpoint — including the new POST and DELETE on study comments. AuthorizationHelper.authorize (line 108) calls user.getRoles().stream() and User.getRoles() is null when the user has no roles, so the authorizer throws a NullPointerException instead of denying. AuthorizationHelper.java, User.java and UserDAO.java are all unchanged from develop, so this is pre-existing rather than a branch regression — but the branch's new POST/DELETE comment endpoints newly expose it, and User.hasAnyUserRole already null-guards getRoles() the way authorize() does not. $EMAIL_noroles is such a user. Worth its own ticket."
  else
    bad "C-3c" "POST as a user with no roles at all: expected 403, got $LAST_CODE" "$LAST_BODY"
  fi
else
  skip "C-3c" "no user without any roles found to test the roleless path"
fi

expect "C-2c" "no row written by any refused POST" "0" \
  "$(sql1 "SELECT count(*) FROM study_comment WHERE study_id = $STUDY;")"

# ---------------------------------------------------------------- C-6, C-7 (rejections)

head1 "Payload validation — still nothing written"

for bad_rating in 'null' '0' '6'; do
  request POST "$EMAIL_r2" r2 "$COMMENTS" "{\"rating\":$bad_rating}"
  expect "C-6" "rating $bad_rating rejected" "400" "$LAST_CODE"
  expect "C-6" "  ...with the range message" "Rating must be between 1 and 5." "$(json '.message')"
done

request POST "$EMAIL_r2" r2 "$COMMENTS" '{}'
expect "C-7a" "{} parses, then fails on the null rating" "400" "$LAST_CODE"
expect "C-7b" "  ...with the range message, not the payload message" \
  "Rating must be between 1 and 5." "$(json '.message')"

request POST "$EMAIL_r2" r2 "$COMMENTS" ''
expect "C-7c" "empty body is rejected as a non-object" "400" "$LAST_CODE"
expect "C-7d" "  ...with the payload message" "Comment payload must be a JSON object" "$(json '.message')"

# A rating must arrive as a JSON number. Gson would otherwise coerce a string, truncate a
# fraction, or take a boolean, and the Integer the service receives carries no trace of it.
for wrong_type in '"3"' '"three"' 'true' '4.5' '[3]' '{"value":3}' '99999999999999999999'; do
  request POST "$EMAIL_r2" r2 "$COMMENTS" "{\"rating\":$wrong_type}"
  expect "C-6t" "rating $wrong_type rejected as a non-number" "400" "$LAST_CODE"
  expect "C-6t" "  ...with the type message" \
    "Rating must be a whole number between 1 and 5." "$(json '.message')"
done

# 4.0 is a whole number that happens to be written as a decimal; it must still be accepted.
request POST "$EMAIL_r2" r2 "$COMMENTS" '{"rating":4.0}'
expect "C-6t" "rating 4.0 accepted as the whole number 4" "200" "$LAST_CODE"
expect "C-6t" "  ...stored as 4" "4" "$(json '.rating')"
CREATED_COMMENT_USERS+=("$UID_r2")
sql1 "DELETE FROM study_comment WHERE study_id = $STUDY AND user_id = $UID_r2;" >/dev/null

request POST "$EMAIL_r2" r2 "$COMMENTS" '"hello"'
expect "C-7e" "a JSON string body is rejected" "400" "$LAST_CODE"
expect "C-7f" "  ...with the payload message, not a Gson stacktrace" \
  "Comment payload must be a JSON object" "$(json '.message')"

expect "C-6/7" "no row written by any rejected payload" "0" \
  "$(sql1 "SELECT count(*) FROM study_comment WHERE study_id = $STUDY AND user_id = $UID_r2;")"

# ---------------------------------------------------------------- C-6 coercion (first write)

head1 "Writes"

# r2 then r3 post first, so the create_date order is r2 < r3 < r1 for the C-8 ordering check.
# Ratings are 3, 5 and (after C-5) 2 - a sum of 10 over 3 comments, so the mean is 3.3333 and an
# integer-division regression cannot hide.
request POST "$EMAIL_r2" r2 "$COMMENTS" '{"rating":3}'
[ "$LAST_CODE" = "200" ] || error "researcher2 ($EMAIL_r2) cannot post at all: $LAST_CODE $LAST_BODY"
ok "C-4pre" "researcher2 posts a rating of 3"
CREATED_COMMENT_USERS+=("$UID_r2")

request POST "$EMAIL_r3" r3 "$COMMENTS" '{"rating":5,"commentText":"Third opinion."}'
[ "$LAST_CODE" = "200" ] || error "researcher3 ($EMAIL_r3) cannot post at all: $LAST_CODE $LAST_BODY"
ok "C-4pre" "researcher3 posts a rating of 5"
CREATED_COMMENT_USERS+=("$UID_r3")
R2_COMMENT_ID="$(sql1 "SELECT study_comment_id FROM study_comment WHERE study_id = $STUDY AND user_id = $UID_r2;")"

request POST "$EMAIL_r1" r1 "$COMMENTS" '{"rating":5,"commentText":"Well documented cohort."}'
expect "C-4a" "POST as a fully eligible researcher" "200" "$LAST_CODE"
CREATED_COMMENT_USERS+=("$UID_r1")
R1_COMMENT_ID="$(json '.studyCommentId')"
if [ -n "$R1_COMMENT_ID" ] && [ "$R1_COMMENT_ID" != "null" ]; then
  ok "C-4b" "response carries studyCommentId ($R1_COMMENT_ID)"
else
  bad "C-4b" "response carries studyCommentId" "$LAST_BODY"
fi
expect "C-4c" "displayName populated from the caller's user row" \
  "$(sql1 "SELECT coalesce(display_name, '') FROM users WHERE user_id = $UID_r1;")" "$(json '.displayName')"
expect "C-4d" "institutionName populated from the caller's institution" \
  "$(sql1 "SELECT coalesce(i.institution_name, '') FROM users u LEFT JOIN institution i ON i.institution_id = u.institution_id WHERE u.user_id = $UID_r1;")" \
  "$(json '.institutionName')"

R1_CREATE_DATE="$(sql1 "SELECT create_date FROM study_comment WHERE study_comment_id = $R1_COMMENT_ID;")"

# ---------------------------------------------------------------- C-5 (upsert) and C-7 rating-only

# The runbook's C-5 is a re-post with a different rating AND a different text, so both replacements
# are exercised with real values rather than by clearing the text.
request POST "$EMAIL_r1" r1 "$COMMENTS" '{"rating":2,"commentText":"Revised after a second read."}'
expect "C-5a" "re-posting returns the same studyCommentId" "$R1_COMMENT_ID" "$(json '.studyCommentId')"
expect "C-5b" "still exactly one row for this user and study" "1" \
  "$(sql1 "SELECT count(*) FROM study_comment WHERE study_id = $STUDY AND user_id = $UID_r1;")"
expect "C-5c" "rating replaced (5 -> 2)" "2" \
  "$(sql1 "SELECT rating FROM study_comment WHERE study_comment_id = $R1_COMMENT_ID;")"
expect "C-5d" "comment_text replaced with the new text" "Revised after a second read." \
  "$(sql1 "SELECT coalesce(comment_text, '') FROM study_comment WHERE study_comment_id = $R1_COMMENT_ID;")"
expect "C-5e" "create_date unchanged" "$R1_CREATE_DATE" \
  "$(sql1 "SELECT create_date FROM study_comment WHERE study_comment_id = $R1_COMMENT_ID;")"
expect "C-5f" "update_date advanced past create_date" "t" \
  "$(sql1 "SELECT update_date > create_date FROM study_comment WHERE study_comment_id = $R1_COMMENT_ID;")"

# C-7's rating-only case, which also demonstrates that an omitted text clears a stored one.
request POST "$EMAIL_r1" r1 "$COMMENTS" '{"rating":2}'
expect "C-7g" "rating with no commentText is accepted — text is nullable" "200" "$LAST_CODE"
expect "C-7h" "  ...and the omitted text clears the stored one" "" \
  "$(sql1 "SELECT coalesce(comment_text, '') FROM study_comment WHERE study_comment_id = $R1_COMMENT_ID;")"

# ---------------------------------------------------------------- C-8 (average and ordering)

head1 "Aggregation"

# Give r2 the newest update_date while r1 keeps the newest create_date. Without this the two
# orderings agree and C-8d proves nothing about which column the DAO sorts on. Same rating, so the
# average is unaffected.
request POST "$EMAIL_r2" r2 "$COMMENTS" '{"rating":3,"commentText":"revised"}'
expect "C-8pre" "r2 revises its comment (newest update_date, oldest create_date)" "200" "$LAST_CODE"

request GET "$EMAIL_nocard" nocard "$COMMENTS"
expected_avg="$(sql1 "SELECT round(avg(rating)::numeric, 4)::float8 FROM study_comment WHERE study_id = $STUDY;")"
expect_num "C-8c" "averageRating is the arithmetic mean of $(json '.comments | length') ratings" \
  "$expected_avg" "$(json '.averageRating')"
# Ordering is create_date DESC and the upsert preserves create_date, so the first row is the one
# whose FIRST post came last — r1 — even though r2 has just been revised and holds the newer
# update_date. Sorting on update_date would put r2 first.
expect "C-8d" "ordered by create_date, not update_date (r1 first, despite r2's newer revision)" \
  "$UID_r1" "$(json '.comments[0].userId')"
expect "C-8e" "  ...second is r3" "$UID_r3" "$(json '.comments[1].userId')"
expect "C-8f" "  ...third is r2, the oldest create_date" "$UID_r2" "$(json '.comments[2].userId')"

# C-1 again, now that the study is populated: the runbook's claim is a 200 "with the full list",
# and the empty-study read earlier could not show that reading a real list needs no card either.
request GET "$EMAIL_nocard" nocard "$COMMENTS"
expect "C-1b" "GET a populated list with no library card" "200" "$LAST_CODE"
expect "C-1c" "  ...returns every comment on the study" \
  "$(sql1 "SELECT count(*) FROM study_comment WHERE study_id = $STUDY;")" "$(json '.comments | length')"
expect "C-1d" "  ...including another user's comment text" "Third opinion." \
  "$(json ".comments[] | select(.userId == $UID_r3) | .commentText")"
expect "C-1e" "  ...with displayName resolved for each" "0" \
  "$(json '[.comments[] | select(.displayName == null)] | length')"

# ---------------------------------------------------------------- C-9, C-10

head1 "Deletion"

request DELETE "$EMAIL_r1" r1 "$COMMENTS/$R1_COMMENT_ID"
expect "C-9a" "DELETE own comment" "204" "$LAST_CODE"
expect "C-9b" "row is gone" "0" \
  "$(sql1 "SELECT count(*) FROM study_comment WHERE study_comment_id = $R1_COMMENT_ID;")"

request GET "$EMAIL_nocard" nocard "$COMMENTS"
expected_avg="$(sql1 "SELECT round(avg(rating)::numeric, 4)::float8 FROM study_comment WHERE study_id = $STUDY;")"
expect_num "C-9c" "averageRating recalculates on the next GET" "$expected_avg" "$(json '.averageRating')"

request DELETE "$EMAIL_r1" r1 "$COMMENTS/$R2_COMMENT_ID"
expect "C-10a" "DELETE another user's comment" "404" "$LAST_CODE"
expect "C-10b" "  ...with 'Comment not found'" "Comment not found" "$(json '.message')"

# Aimed at a real, visible study so requireStudy passes and the 404 can only come from the
# study/comment mismatch.
request DELETE "$EMAIL_r2" r2 "api/dataset/study/$REAL_STUDY/comments/$R2_COMMENT_ID"
expect "C-10c" "DELETE own comment id under the wrong studyId" "404" "$LAST_CODE"
expect "C-10d" "  ...with 'Comment not found'" "Comment not found" "$(json '.message')"

expect "C-10e" "the row survived both refused deletes" "1" \
  "$(sql1 "SELECT count(*) FROM study_comment WHERE study_comment_id = $R2_COMMENT_ID;")"

# ---------------------------------------------------------------- C-11

head1 "FK cascade"

if [ "$SKIP_CASCADE" = "true" ]; then
  skip "C-11" "study delete cascade (needs the throwaway study; not run against --study)"
else
  before="$(sql1 "SELECT count(*) FROM study_comment WHERE study_id = $STUDY;")"
  if printf "%s\n" "DELETE FROM study WHERE study_id = $STUDY;" | psql_q >/dev/null 2>&1; then
    ok "C-11a" "study row deleted with $before comment(s) attached — no FK error"
    expect "C-11b" "comments cascaded away" "0" \
      "$(sql1 "SELECT count(*) FROM study_comment WHERE study_id = $STUDY;")"
    SYNTHETIC_STUDY=""
  else
    bad "C-11a" "deleting the study was blocked — the FK is not ON DELETE CASCADE"
  fi
fi

# ---------------------------------------------------------------- card survival

head1 "Library card survival"

for persona in r1 r2 r3 cardonly; do
  eval "email=\${EMAIL_$persona}"
  present="$(sql1 "SELECT count(*) FROM library_card lc JOIN users u ON u.user_id = lc.user_id WHERE lower(u.email) = lower($(sql_quote "$email"));")"
  if [ "$present" = "1" ]; then
    ok "CARD" "$persona still holds a library card ($email)"
  else
    bad "CARD" "$persona LOST their library card during the run ($email)" \
      "enforceInstitutionAndLibraryCardRules deleted it, so every write check above is suspect"
  fi
done

LIBRARY_CARDS_AFTER="$(sql1 "SELECT count(*) FROM library_card;")"
[ "$LIBRARY_CARDS_AFTER" = "$LIBRARY_CARDS_BEFORE" ] \
  || note "library_card rows went from $LIBRARY_CARDS_BEFORE to $LIBRARY_CARDS_AFTER during this run."

# ---------------------------------------------------------------- summary

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
