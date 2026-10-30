#!/bin/bash
#
# Usage: scripts/index-es.sh [CONFIG_DIR]
#
# Fills the local Elasticsearch `dataset` index from the local database, through the admin
# endpoint POST /api/dataset/index. Run it after `docker compose up`, and again after each new
# dump. The gcloud account must have the Admin role in the loaded database.
#

set -euo pipefail
unset CDPATH

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"

CONFIG_DIR="${1:-$SCRIPT_DIR/../config}"
HOST="local.dsde-dev.broadinstitute.org:27443"
ES="http://localhost:9200/dataset"
BODY=$(mktemp "${TMPDIR:-/tmp}/index-es.XXXXXX")
trap 'rm -f "$BODY"' EXIT

# curl reads the secrets from stdin, so they are never on a command line.
es_curl() {
  printf 'user = "elastic:%s"\n' "$(env_get "$CONFIG_DIR/.env" ELASTIC_PASSWORD)" | curl -sS -K - "$@"
}

# An old index can hold dataset IDs that are not in a new dump. 404 means no index. Any other
# error, such as 401 for a wrong ELASTIC_PASSWORD, stops the script.
code=$(es_curl -o /dev/null -w '%{http_code}' -X DELETE "$ES")
if [[ "$code" != 2?? && "$code" != 404 ]]; then
  echo "ERROR: Could not delete the old index (HTTP $code). Check ELASTIC_PASSWORD in $CONFIG_DIR/.env." >&2
  exit 1
fi

echo "Indexing all datasets"
# Only the host has the /etc/hosts entry for this name. --resolve also works in the Dev Container,
# and curl still checks the certificate against the name.
printf 'header = "Authorization: Bearer %s"\n' "$(gcloud auth print-access-token)" \
  | curl -sS -f -K - --resolve "$HOST:127.0.0.1" -o "$BODY" -X POST "https://$HOST/api/dataset/index"

# The endpoint streams its result, so the status is 200 even when the indexing fails. Elasticsearch
# also returns 200 when some documents fail, and Consent returns that body as it is.
if grep -q "Error indexing datasets" "$BODY" || ! jq -s -e 'length > 0 and all(.errors == false)' "$BODY" > /dev/null 2>&1; then
  head -c 2000 "$BODY" >&2
  echo >&2
  echo "ERROR: The index is not complete. See the response above and the consent log." >&2
  exit 1
fi
echo "Indexed all datasets."
