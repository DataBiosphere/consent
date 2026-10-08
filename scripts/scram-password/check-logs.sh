#!/bin/bash
# Searches the Cloud SQL logs of one instance for the consent password and for hash text.
# See README.md (step 8).
#
# Usage: check-logs.sh PROJECT INSTANCE [FRESHNESS]
#   PROJECT    GCP project of the instance
#   INSTANCE   Cloud SQL instance name of the consent database
#   FRESHNESS  how far back to read (default 3h), for example 6h or 1d
#
# It reads the logs and the secret consent-postgres-creds. It decodes every JSON string
# before it matches, so a password with a quote or a backslash cannot hide in JSON escapes.
# It works in a private temporary directory and touches no file in the current directory.
# It prints counts only. It never prints the password.
#
# Exit codes: 0 every count is 0, 1 a count is not 0 or a read failed.
set -euo pipefail
PROJECT=${1:?usage: check-logs.sh PROJECT INSTANCE [FRESHNESS]}
INSTANCE=${2:?usage: check-logs.sh PROJECT INSTANCE [FRESHNESS]}
FRESHNESS=${3:-3h}

WORK=$(mktemp -d "${TMPDIR:-/tmp}/scramlogs.XXXXXX")
trap 'rm -rf "$WORK"' EXIT

gcloud logging read "resource.type=\"cloudsql_database\"
  AND resource.labels.database_id=\"$PROJECT:$INSTANCE\"" \
  --project "$PROJECT" --freshness="$FRESHNESS" --limit=60000 --format=json > "$WORK/logs.json"
[ "$(jq length "$WORK/logs.json")" -gt 0 ] || { echo "no log entries: the log read failed"; exit 1; }

PW=$(gcloud --project "$PROJECT" secrets versions access latest --secret=consent-postgres-creds | jq -j .password)
[ -n "$PW" ] || { echo "empty password: the secret read failed"; exit 1; }
export PW

python3 - "$WORK/logs.json" <<'PY'
import json, os, re, sys

pw = os.environ["PW"]
entries = json.load(open(sys.argv[1]))


def strings(x):
    """Yield every decoded string in the JSON value, keys and values."""
    if isinstance(x, str):
        yield x
    elif isinstance(x, dict):
        for k, v in x.items():
            yield k
            yield from strings(v)
    elif isinstance(x, list):
        for v in x:
            yield from strings(v)


checks = [
    ("plaintext password", lambda s: pw in s),
    ("SCRAM verifier", lambda s: "SCRAM-SHA-256$" in s),
    ("MD5 verifier", lambda s: re.search(r"md5[0-9a-f]{32}", s) is not None),
    ("ALTER ROLE text", lambda s: re.search(r"alter\s+(role|user)", s, re.I) is not None),
]
counts = {name: 0 for name, _ in checks}
for entry in entries:
    for s in strings(entry):
        for name, test in checks:
            if test(s):
                counts[name] += 1

print(f"entries read: {len(entries)}")
for name, n in counts.items():
    print(f"{name}: {n}")
if any(counts.values()):
    print("FOUND SENSITIVE TEXT")
    sys.exit(1)
print("clean")
PY
