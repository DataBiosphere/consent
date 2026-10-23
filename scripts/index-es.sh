#!/bin/bash
#
# Fills the local Elasticsearch `dataset` index from the local database, through the admin
# endpoint POST /api/dataset/index. Run it after `docker compose up`, and again after each new
# database dump: an old index can hold dataset IDs that are not in the new dump.
#
# Run it where compose runs: on the host, or in the Dev Container.
#
# The token comes from `gcloud auth print-access-token`. The gcloud account must have the Admin
# role in the loaded database. You MUST have gcloud, curl and jq installed.
#

set -eu
set -o pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"

HOST="local.dsde-dev.broadinstitute.org"
PORT=27443
API="https://$HOST:$PORT"
ES="http://localhost:9200"
INDEX="dataset"
STATUS_WAIT_SECONDS=300

RESET="true"
CONFIG_DIR="$REPO_ROOT/config"

usage() {
  cat << EOF
Usage: $0 [OPTION]...
Fill the local dataset index from the local database.

  --reset true|false   Delete the local dataset index before the reindex. Defaults to true.
  --config_dir DIR     The folder with .env. Defaults to config/ in the repo root.
  --help               Show this help.
EOF
  exit 0
}

error() {
  echo "ERROR: $1" >&2
  exit 1
}

parse_cli_args() {
  while [[ $# -gt 0 ]]; do
    [[ "$1" == "--help" || $# -ge 2 ]] || error "Option $1 needs a value. Try --help to see a list of all options."
    case "$1" in
      --reset) check_bool "$1" "$2"; RESET=$2; shift 2 ;;
      --config_dir) CONFIG_DIR=$2; shift 2 ;;
      --help) usage ;;
      *) error "Unknown option: $1. Try --help to see a list of all options." ;;
    esac
  done
}

# Only the host has the /etc/hosts entry for $HOST. --resolve sends the name to 127.0.0.1 in
# each place, and curl still checks the certificate against the name.
api_curl() {
  curl -sS --resolve "$HOST:$PORT:127.0.0.1" "$@"
}

# curl reads the options from stdin, so the password is not on the command line.
es_curl() {
  printf 'user = "elastic:%s"\n' "$ELASTIC_PASSWORD" | curl -sS -K - "$@"
}

parse_cli_args "$@"
for tool in gcloud curl jq; do
  command -v "$tool" > /dev/null || error "Missing tool: $tool."
done
ACCOUNT=$(gcloud_account)
[[ -n "$ACCOUNT" ]] || error "Google Cloud is not logged in. Run: gcloud auth login"
ELASTIC_PASSWORD=$(env_get "$CONFIG_DIR/.env" ELASTIC_PASSWORD)
[[ -n "$ELASTIC_PASSWORD" ]] || error "$CONFIG_DIR/.env has no ELASTIC_PASSWORD. Run: ./scripts/render-configs.sh --write_compose true"

BODY=$(mktemp "${TMPDIR:-/tmp}/index-es.XXXXXX")
trap 'rm -f "$BODY"' EXIT

echo "Waiting for $API/status"
waited=0
until api_curl -f -o /dev/null --max-time 5 "$API/status" 2> /dev/null; do
  if [[ $waited -ge $STATUS_WAIT_SECONDS ]]; then
    error "Consent did not answer on $API/status in $STATUS_WAIT_SECONDS seconds. Is the stack up?"
  fi
  sleep 5
  waited=$((waited + 5))
done

if [[ "$RESET" == "true" ]]; then
  echo "Deleting the local $INDEX index"
  code=$(es_curl -o /dev/null -w '%{http_code}' -X DELETE "$ES/$INDEX")
  # 404: a new volume has no index.
  if [[ "$code" != "200" && "$code" != "404" ]]; then
    error "Elasticsearch returned $code for DELETE /$INDEX."
  fi
fi

echo "Indexing all datasets as $ACCOUNT"
# The token goes to curl on stdin, so it is not on the command line. The script never prints it.
code=$(printf 'header = "Authorization: Bearer %s"\n' "$(gcloud auth print-access-token)" \
  | api_curl -K - -o "$BODY" -w '%{http_code}' -X POST "$API/api/dataset/index") \
  || error "The index request did not complete."

if [[ "$code" == "401" || "$code" == "403" ]]; then
  error "Consent returned $code. The gcloud account $ACCOUNT needs the Admin role in the loaded database."
fi
if [[ "$code" != 2* ]]; then
  cat "$BODY" >&2
  error "Consent returned $code."
fi

# The endpoint streams its result, so the status is 200 even when indexing fails.
[[ -s "$BODY" ]] || error "The index response is empty."
if grep -q "Error indexing datasets" "$BODY"; then
  error "Consent could not index the datasets. See the consent log for the cause."
fi
jq -e . "$BODY" > /dev/null 2>&1 || error "The index response is not JSON: $(head -c 300 "$BODY")"
# Elasticsearch returns 200 when some documents fail, and Consent returns the bulk body as it is.
if [[ "$(jq -r '.errors' "$BODY")" == "true" ]]; then
  echo "Some datasets failed to index (dataset ID, status, error type, reason):" >&2
  jq -r '.items[] | (.index // .create // .update) | select(.error)
    | "  \(._id)\t\(.status)\t\(.error.type)\t\(.error.reason)"' "$BODY" >&2
  error "The index is not complete."
fi

es_curl -o /dev/null -X POST "$ES/$INDEX/_refresh"
count=$(es_curl "$ES/$INDEX/_count" | jq -r '.count')
echo "The $INDEX index has $count documents."
