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
#   SCRAM_TEST_PAUSE=N       wait N whole seconds after the ALTER is sent, to rehearse an
#                            interrupt (send the script TERM or INT during the pause)
#   VERIFY_TIMEOUT           seconds to wait for the verification login (default 15). A login
#                            that stalls counts as a failed verification, and so rolls back.
#   ROLLBACK_WAIT            seconds to wait for the rollback to run (default 30). The rollback
#                            waits behind a blocked ALTER on the same session.
#
# Safety net: one authenticated psql session stays open during the change. Open
# sessions stay valid after ALTER ROLE, so if the new SCRAM login fails, the script
# uses that session to put the MD5 verifier back.
# The password stays in shell variables and pipes. It never reaches argv or a file.
#
# Interrupts: after the ALTER is sent and before the result is verified, any exit
# (INT, TERM, HUP, or an error) first tries to put the MD5 verifier back.
# Bash runs a signal trap only when the current step ends. Every step has a time limit
# (the probe 10 s, the verification login VERIFY_TIMEOUT), so a signal waits at most that long.
#
# Exit codes: 0 success, 1 stopped before any change, 2 verification failed and
# the script tried to roll back (check the output), 129/130/143 interrupted (HUP/INT/TERM).
set -u
PROJECT=${1:?usage: apply-scram.sh PROJECT PORT}
PORT=${2:?usage: apply-scram.sh PROJECT PORT}
HERE=$(cd "$(dirname "$0")" && pwd)
# shellcheck source=lib.sh
. "$HERE/lib.sh"
PSQL=${PSQL:-psql}
DB=${DB:-consent}

WORK=$(mktemp -d "${TMPDIR:-/tmp}/scram.XXXXXX") || exit 1
FIFO="$WORK/held.fifo"; OUT="$WORK/held.out"; PSQLPID=""
# 1 from just before the ALTER is sent until the result is verified or rolled back.
PENDING_ROLLBACK=0

pause() { python3 -c 'import time;time.sleep(0.2)'; }

rollback() {
  PENDING_ROLLBACK=0
  if [ -n "$PSQLPID" ] && kill -0 "$PSQLPID" 2>/dev/null; then
    # The held session runs its commands in order. Queue a marker behind the rollback, and
    # wait for the marker before the probe. The first ALTER may still be blocked, and the
    # probe shows MD5 while it is. Only the marker proves that BOTH ALTER commands have run.
    { printf "ALTER ROLE %s PASSWORD '%s';\n" "$U" "$MD5H" >&3; printf "select 'rollback-ordered';\n" >&3; } 2>/dev/null
    if waitfor "rollback-ordered" 1 $(( ${ROLLBACK_WAIT:-30} * 5 )) && waitprobe "authType 5" 10; then
      echo "rollback applied"
    else
      echo "ROLLBACK DID NOT CONFIRM - check by hand"
    fi
  else
    echo "ROLLBACK NOT POSSIBLE: the held session is gone - check by hand"
  fi
  echo "after rollback: $(probe 2>&1)"
}

cleanup() {
  if [ "$PENDING_ROLLBACK" = 1 ]; then
    echo "stopped before the result was verified - ROLLING BACK to the MD5 verifier"
    rollback
  fi
  # Skip the write when the held psql is already gone: Bash 3.2 prints the text
  # to the terminal when a write to a dead pipe fails.
  if [ -n "$PSQLPID" ] && kill -0 "$PSQLPID" 2>/dev/null; then
    { printf '\\q\n' >&3; } 2>/dev/null
  fi
  { exec 3>&-; } 2>/dev/null
  if [ -n "$PSQLPID" ]; then
    # Give the held psql about 3 s to quit. A hung psql must not hang the cleanup.
    local i
    for ((i = 0; i < 15; i++)); do kill -0 "$PSQLPID" 2>/dev/null || break; pause; done
    if kill -0 "$PSQLPID" 2>/dev/null; then
      kill "$PSQLPID" 2>/dev/null; pause; kill -9 "$PSQLPID" 2>/dev/null
    fi
    wait "$PSQLPID" 2>/dev/null
  fi
  rm -rf "$WORK"
}
# The held psql session can die (for example, a bad password). A write to its pipe
# must then fail quietly, not kill this script with SIGPIPE and skip the cleanup.
trap '' PIPE
# A signal must run the EXIT trap, so that the cleanup can roll back.
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM
trap cleanup EXIT

if [ -n "${CREDS_CMD:-}" ]; then
  CREDS=$(bash -c "$CREDS_CMD") || { echo "cannot read the credentials"; exit 1; }
else
  CREDS=$(gcloud --project "$PROJECT" secrets versions access latest --secret=consent-postgres-creds) \
    || { echo "cannot read the secret"; exit 1; }
fi
parse_creds "$CREDS" || { echo "bad credential fields"; exit 1; }
unset CREDS
[[ "$U" =~ ^[a-z_][a-z0-9_]*$ ]] || { echo "bad credential fields"; exit 1; }
CONN="host=127.0.0.1 port=$PORT dbname=$DB user=$U sslmode=disable"
probe() { python3 "$HERE/probe-auth.py" 127.0.0.1 "$PORT" "$U" "$DB"; }

# bounded SECONDS CMD...: run CMD and print its output. Give up after SECONDS, print
# TIMEOUT and return 124. This covers connect, authentication and the query.
bounded() {
  local secs=$1; shift
  python3 -c 'import subprocess, sys
secs = float(sys.argv[1])
try:
    r = subprocess.run(sys.argv[2:], stdin=subprocess.DEVNULL, capture_output=True, text=True, timeout=secs)
except subprocess.TimeoutExpired:
    print("TIMEOUT: no answer in %s s" % sys.argv[1])
    sys.exit(124)
sys.stdout.write(r.stdout + r.stderr)
sys.exit(r.returncode)' "$secs" "$@"
}

BEFORE=$(probe); echo "before: $BEFORE"
case "$BEFORE" in *"authType 5"*) ;; *) echo "expected MD5 before the change - stopping, nothing changed"; exit 1;; esac

HASH=$(printf '%s' "$PW" | "$HERE/scram-hash") || { echo "hash failed - stopping, nothing changed"; exit 1; }
MD5H=$(printf '%s' "$PW" | python3 "$HERE/md5-verifier.py" "$U") || MD5H=""
[[ "$MD5H" =~ ^md5[0-9a-f]{32}$ ]] || { echo "bad rollback verifier - stopping, nothing changed"; exit 1; }

# Only the psql sessions need the password. Export it after the hashing, so that no
# libpq program that runs earlier can pick it up.
export PGPASSWORD=$PW

: > "$OUT"; mkfifo "$FIFO"
# -X must be the first option: it skips ~/.psqlrc, so a local setting such as
# AUTOCOMMIT off cannot change how the ALTER and the rollback behave.
"$PSQL" -X -tA -v ON_ERROR_STOP=0 "$CONN" < "$FIFO" > "$OUT" 2>&1 &
PSQLPID=$!
exec 3>"$FIFO"

waitfor() { # waitfor TEXT MINIMUM_COUNT [TRIES]: wait for text in the held session output
  local i tries=${3:-50}
  for ((i = 0; i < tries; i++)); do
    [ "$(grep -c -F -- "$1" "$OUT")" -ge "$2" ] && return 0
    pause
  done
  return 1
}

waitprobe() { # waitprobe TEXT [SECONDS]: wait until the probe output contains TEXT (default 30 s)
  local end=$((SECONDS + ${2:-30}))
  while :; do
    case "$(probe 2>&1)" in *"$1"*) return 0;; esac
    [ "$SECONDS" -ge "$end" ] && return 1
    pause
  done
}

printf "select 'held-session-ok';\n" >&3
waitfor held-session-ok 1 || { echo "held session did not start - stopping, nothing changed"; exit 1; }
echo "held session open"

# From here on the role may have changed. Every failure or uncertain path must end in
# the verification below, and so in the rollback. Do not exit before it.
PENDING_ROLLBACK=1
printf "ALTER ROLE %s PASSWORD '%s';\n" "$U" "$HASH" >&3; unset HASH
if [[ "${SCRAM_TEST_PAUSE:-}" =~ ^[0-9]+$ ]]; then
  echo "(test switch: pausing $SCRAM_TEST_PAUSE s after the ALTER)"
  python3 -c "import time;time.sleep($SCRAM_TEST_PAUSE)"
fi
if waitfor "ALTER ROLE" 1 "${ALTER_CONFIRM_TRIES:-50}"; then
  echo "ALTER ROLE applied"
else
  echo "WARNING: ALTER ROLE was not confirmed in time. It may still apply. Checking the result."
fi

AFTER=$(probe); echo "after:  $AFTER"
LOGIN=$(bounded "${VERIFY_TIMEOUT:-15}" "$PSQL" -X -tA "$CONN" -c "select 'new-login-ok as '||current_user" | grep -v -i "deprecat")
echo "new login: $LOGIN"
case "$AFTER" in *"authType 10"*) P_OK=1;; *) P_OK=0;; esac
case "$LOGIN" in *new-login-ok*) L_OK=1;; *) L_OK=0;; esac
[ "${SCRAM_TEST_FORCE_FAIL:-}" = 1 ] && { echo "(test switch: forcing a verification failure)"; L_OK=0; }
if [ $P_OK = 1 ] && [ $L_OK = 1 ]; then
  PENDING_ROLLBACK=0
  echo "RESULT: success"
  exit 0
fi

echo "RESULT: verification failed - ROLLING BACK to the MD5 verifier"
rollback
exit 2
