#!/bin/bash
#
# Usage: scripts/export-db.sh [dev|staging] [CONFIG_DIR]
#
# Writes a dump of the dev (default) or staging consent database to config/consentdb-ENV.sql.gz,
# and sets CONSENT_DB_DUMP in config/.env. A new dump replaces the old dump of the same ENV.
# The Cloud SQL proxy and pg_dump run as containers, so no runtime needs a host proxy.
#
# This script never exports prod. A dump holds real user data: do not copy, share or upload it.
#

set -euo pipefail
unset CDPATH

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"

DB_ENV="${1:-dev}"
CONFIG_DIR="${2:-$SCRIPT_DIR/../config}"
# The same image as the local database, so pg_dump writes only settings that Postgres 16 knows.
POSTGRES_IMAGE="postgres:16.14-alpine"

# The project comes only from this table, so the gcloud default project has no effect. Refuse
# prod before the script reads a secret.
case "$DB_ENV" in
  dev) PROJECT="broad-dsde-dev" ;;
  staging) PROJECT="broad-dsde-staging" ;;
  *) echo "ERROR: The environment must be dev or staging, not '$DB_ENV'. This script never exports prod." >&2; exit 1 ;;
esac

mkdir -p "$CONFIG_DIR"
chmod 700 "$CONFIG_DIR"
DUMP_NAME="consentdb-$DB_ENV.sql.gz"
NAME="consent-export-$$"
TMP_DUMP=$(mktemp "$CONFIG_DIR/.consentdb.XXXXXX")
trap 'docker rm -f "$NAME" > /dev/null 2>&1 || true; rm -f "$TMP_DUMP"' EXIT
trap 'exit 130' INT TERM

echo "Exporting the $DB_ENV database from $PROJECT"
creds=$(gcloud secrets versions access latest --project "$PROJECT" --secret consent-postgres-creds)
db_user=$(jq -r .username <<< "$creds")
# Each docker command below names these variables with no value, so Docker copies the values from
# this environment. The secrets are never on a command line or in the process list.
PGPASSWORD=$(jq -r .password <<< "$creds")
CSQL_PROXY_TOKEN=$(gcloud auth print-access-token)
CSQL_PROXY_INSTANCE_CONNECTION_NAME="$PROJECT:us-central1:$(jq -r .instance_name <<< "$creds")"
export PGPASSWORD CSQL_PROXY_TOKEN CSQL_PROXY_INSTANCE_CONNECTION_NAME
unset creds

docker run -d --name "$NAME" -e CSQL_PROXY_TOKEN -e CSQL_PROXY_INSTANCE_CONNECTION_NAME \
  gcr.io/cloud-sql-connectors/cloud-sql-proxy:2.18.0 > /dev/null
# pg_dump runs in the network of the proxy container, so the proxy listens only on its own
# 127.0.0.1 and publishes no ports.
pg() {
  docker run --rm --network "container:$NAME" -e PGPASSWORD "$POSTGRES_IMAGE" "$@" -h 127.0.0.1 -U "$db_user" -d consent
}
for _ in $(seq 1 15); do
  pg pg_isready -q > /dev/null 2>&1 && break
  sleep 2
done

echo "Writing $DUMP_NAME. A full dump takes several minutes."
pg pg_dump --no-owner --no-privileges | gzip > "$TMP_DUMP"
# pg_dump writes this line last. Without it, the dump stopped early, and compose would load it
# with no error.
gzip -dc "$TMP_DUMP" | tail -n 5 | grep -q "PostgreSQL database dump complete" \
  || { echo "ERROR: The dump is not complete. The old dump is unchanged." >&2; exit 1; }
# The Postgres image reads the dump as the postgres user. config/ has mode 700.
chmod 644 "$TMP_DUMP"
mv "$TMP_DUMP" "$CONFIG_DIR/$DUMP_NAME"
env_set "$CONFIG_DIR/.env" CONSENT_DB_DUMP "$DUMP_NAME"
echo "Wrote $DUMP_NAME, and set CONSENT_DB_DUMP in .env. See DEVNOTES.md to load it."
