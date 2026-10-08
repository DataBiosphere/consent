#!/bin/bash
# Sets the consent role password to a SCRAM-SHA-256 verifier of the SAME password.
# See README.md for the full procedure.
#
# Usage: apply-scram.sh PROJECT PORT
#   PROJECT  GCP project that holds the consent-postgres-creds secret
#   PORT     local port of a Cloud SQL Auth Proxy that points at the target instance
#
# Optional environment:
#   PSQL                     path to psql (default: psql)
#   DB                       database name (default: consent; the secret has no "db" field)
#   CREDS_CMD                command that prints {"username": "...", "password": "..."}
#                            (default: read consent-postgres-creds from PROJECT);
#                            use it to rehearse against a local container
#   SCRAM_TEST_FORCE_FAIL=1  force the verification to fail, to rehearse the rollback
#   ALTER_CONFIRM_TRIES      how many 0.2 s polls to wait for the ALTER ROLE confirmation
#                            (default 50). Use 0 to rehearse a missing confirmation.
#
# Safety net: one authenticated psql session stays open during the change. Open
# sessions stay valid after ALTER ROLE, so if the new SCRAM login fails, the script
# uses that session to put the MD5 verifier back.
# The password stays in shell variables and pipes. It never reaches argv or a file.
#
# Exit codes: 0 success, 1 stopped before any change, 2 verification failed and
# the script tried to roll back (check the output).
set -u
PROJECT=${1:?usage: apply-scram.sh PROJECT PORT}
PORT=${2:?usage: apply-scram.sh PROJECT PORT}
HERE=$(cd "$(dirname "$0")" && pwd)
PSQL=${PSQL:-psql}
DB=${DB:-consent}

WORK=$(mktemp -d "${TMPDIR:-/tmp}/scram.XXXXXX") || exit 1
FIFO="$WORK/held.fifo"; OUT="$WORK/held.out"; PSQLPID=""
cleanup() {
  # Skip the write when the held psql is already gone: Bash 3.2 prints the text
  # to the terminal when a write to a dead pipe fails.
  if [ -n "$PSQLPID" ] && kill -0 "$PSQLPID" 2>/dev/null; then
    { printf '\\q\n' >&3; } 2>/dev/null
  fi
  { exec 3>&-; } 2>/dev/null
  [ -n "$PSQLPID" ] && wait "$PSQLPID" 2>/dev/null
  rm -rf "$WORK"
}
# The held psql session can die (for example, a bad password). A write to its pipe
# must then fail quietly, not kill this script with SIGPIPE and skip the cleanup.
trap '' PIPE
trap cleanup EXIT

if [ -n "${CREDS_CMD:-}" ]; then
  CREDS=$(bash -c "$CREDS_CMD") || { echo "cannot read the credentials"; exit 1; }
else
  CREDS=$(gcloud --project "$PROJECT" secrets versions access latest --secret=consent-postgres-creds) \
    || { echo "cannot read the secret"; exit 1; }
fi
PW=$(printf '%s' "$CREDS" | jq -j .password); U=$(printf '%s' "$CREDS" | jq -r .username); unset CREDS
[ -n "$PW" ] && [[ "$U" =~ ^[a-z_][a-z0-9_]*$ ]] || { echo "bad credential fields"; exit 1; }
CONN="host=127.0.0.1 port=$PORT dbname=$DB user=$U sslmode=disable"
export PGPASSWORD=$PW
probe() { python3 "$HERE/probe-auth.py" 127.0.0.1 "$PORT" "$U" "$DB"; }

BEFORE=$(probe); echo "before: $BEFORE"
case "$BEFORE" in *"authType 5"*) ;; *) echo "expected MD5 before the change - stopping, nothing changed"; exit 1;; esac

HASH=$(printf '%s' "$PW" | "$HERE/scram-hash") || { echo "hash failed - stopping, nothing changed"; exit 1; }
MD5H="md5$(printf '%s%s' "$PW" "$U" | python3 -c 'import sys,hashlib;print(hashlib.md5(sys.stdin.buffer.read()).hexdigest())')"
[[ "$MD5H" =~ ^md5[0-9a-f]{32}$ ]] || { echo "bad rollback verifier - stopping, nothing changed"; exit 1; }

: > "$OUT"; mkfifo "$FIFO"
"$PSQL" -tA -v ON_ERROR_STOP=0 "$CONN" < "$FIFO" > "$OUT" 2>&1 &
PSQLPID=$!
exec 3>"$FIFO"

pause() { python3 -c 'import time;time.sleep(0.2)'; }

waitfor() { # waitfor TEXT MINIMUM_COUNT [TRIES]: wait for text in the held session output
  local i tries=${3:-50}
  for ((i = 0; i < tries; i++)); do
    [ "$(grep -c -F -- "$1" "$OUT")" -ge "$2" ] && return 0
    pause
  done
  return 1
}

waitprobe() { # waitprobe TEXT: wait until the probe output contains TEXT
  local i
  for ((i = 0; i < 50; i++)); do
    case "$(probe 2>&1)" in *"$1"*) return 0;; esac
    pause
  done
  return 1
}

printf "select 'held-session-ok';\n" >&3
waitfor held-session-ok 1 || { echo "held session did not start - stopping, nothing changed"; exit 1; }
echo "held session open"

# From here on the role may have changed. Every failure or uncertain path must end in
# the verification below, and so in the rollback. Do not exit before it.
printf "ALTER ROLE %s PASSWORD '%s';\n" "$U" "$HASH" >&3; unset HASH
if waitfor "ALTER ROLE" 1 "${ALTER_CONFIRM_TRIES:-50}"; then
  echo "ALTER ROLE applied"
else
  echo "WARNING: ALTER ROLE was not confirmed in time. It may still apply. Checking the result."
fi

AFTER=$(probe); echo "after:  $AFTER"
LOGIN=$("$PSQL" -tA "$CONN" -c "select 'new-login-ok as '||current_user" 2>&1 | grep -v -i "deprecat")
echo "new login: $LOGIN"
case "$AFTER" in *"authType 10"*) P_OK=1;; *) P_OK=0;; esac
case "$LOGIN" in *new-login-ok*) L_OK=1;; *) L_OK=0;; esac
[ "${SCRAM_TEST_FORCE_FAIL:-}" = 1 ] && { echo "(test switch: forcing a verification failure)"; L_OK=0; }
if [ $P_OK = 1 ] && [ $L_OK = 1 ]; then
  echo "RESULT: success"
  exit 0
fi

echo "RESULT: verification failed - ROLLING BACK to the MD5 verifier"
printf "ALTER ROLE %s PASSWORD '%s';\n" "$U" "$MD5H" >&3
# Confirm with the probe, not with the output count: the first ALTER may be unconfirmed.
if waitprobe "authType 5"; then
  echo "rollback applied"
else
  echo "ROLLBACK DID NOT CONFIRM - check by hand"
fi
echo "after rollback: $(probe)"
exit 2
