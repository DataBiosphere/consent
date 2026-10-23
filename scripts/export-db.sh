#!/bin/bash
#
# Writes a dump of the dev or staging consent database to config/, in the format that the local
# Postgres container loads, and sets CONSENT_DB_DUMP in config/.env.
#
# The Cloud SQL proxy and pg_dump both run as containers on a temporary Docker network. So the
# export works the same with Colima, Docker Desktop, Linux Docker Engine, Podman and the Dev
# Container. The proxy container publishes no ports.
#
# This script never exports prod. A dump holds real user data: do not copy, share or upload it.
# Make a new one with this script.
#
# You MUST have gcloud, docker, gzip and jq installed, and be logged in with gcloud.
#

set -eu
set -o pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"

CSQL_PROXY_IMAGE="gcr.io/cloud-sql-connectors/cloud-sql-proxy:2.18.0"
# The same image as the local database, so pg_dump writes only settings that it knows.
POSTGRES_IMAGE="postgres:16.14-alpine"
REGION="us-central1"
DB_NAME="consent"

DB_ENV="dev"
CONFIG_DIR="$REPO_ROOT/config"

usage() {
  cat << EOF
Usage: $0 [OPTION]...
Write a dump of the consent database to config/ and set CONSENT_DB_DUMP in config/.env.

  --db_env dev|staging  The environment to export. Defaults to dev. Prod is refused.
  --config_dir DIR      Where to write the dump. Defaults to config/ in the repo root.
  --help                Show this help.
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
      --db_env) DB_ENV=$2; shift 2 ;;
      --config_dir) CONFIG_DIR=$2; shift 2 ;;
      --help) usage ;;
      *) error "Unknown option: $1. Try --help to see a list of all options." ;;
    esac
  done
}

# The project comes only from this table: there is no --project option, and the gcloud default
# project has no effect. Prod is refused here, before the script reads a secret.
project_for() {
  case "$1" in
    dev) echo "broad-dsde-dev" ;;
    staging) echo "broad-dsde-staging" ;;
    prod) error "This script never exports prod. Use --db_env dev or --db_env staging." ;;
    *) error "--db_env must be dev or staging, not '$1'." ;;
  esac
}

parse_cli_args "$@"
PROJECT=$(project_for "$DB_ENV") || exit 1

for tool in gcloud docker gzip jq; do
  command -v "$tool" > /dev/null || error "Missing tool: $tool."
done
docker info > /dev/null 2>&1 || error "Docker is not running."
[[ -n "$(gcloud_account)" ]] || error "Google Cloud is not logged in. Run: gcloud auth login"

mkdir -p "$CONFIG_DIR"
chmod 700 "$CONFIG_DIR"
CONFIG_DIR=$(cd "$CONFIG_DIR" && pwd)
ENV_FILE="$CONFIG_DIR/.env"
DUMP_NAME="consentdb-$DB_ENV-$(date +%F).sql.gz"
NAME="consent-export-$$"
TMP_DUMP=""

cleanup() {
  docker rm -f "$NAME" > /dev/null 2>&1 || true
  docker network rm "$NAME" > /dev/null 2>&1 || true
  if [[ -n "$TMP_DUMP" ]]; then
    rm -f "$TMP_DUMP"
  fi
}
trap cleanup EXIT
trap 'exit 130' INT TERM

echo "Exporting the $DB_ENV database from $PROJECT"
creds=$(gcloud secrets versions access latest --project "$PROJECT" --secret consent-postgres-creds) \
  || error "Could not read the consent-postgres-creds secret in $PROJECT."
instance=$(echo "$creds" | jq -r .instance_name)
db_user=$(echo "$creds" | jq -r .username)
# Exported with no value on the docker command lines below, so Docker copies each value from
# this environment. The secrets are never on a command line or in the process list.
PGPASSWORD=$(echo "$creds" | jq -r .password)
CSQL_PROXY_TOKEN=$(gcloud auth print-access-token)
export PGPASSWORD CSQL_PROXY_TOKEN
unset creds

docker network create "$NAME" > /dev/null
# 0.0.0.0 is the address inside the proxy container. With no published ports, only the
# containers on this network can reach it.
docker run -d --name "$NAME" --network "$NAME" \
  -e CSQL_PROXY_TOKEN \
  -e CSQL_PROXY_ADDRESS=0.0.0.0 \
  -e CSQL_PROXY_PORT=5432 \
  -e CSQL_PROXY_INSTANCE_CONNECTION_NAME="$PROJECT:$REGION:$instance" \
  "$CSQL_PROXY_IMAGE" > /dev/null

echo "Waiting for the Cloud SQL proxy"
ready=false
for _ in $(seq 1 30); do
  if docker run --rm --network "$NAME" "$POSTGRES_IMAGE" \
    pg_isready -q -h "$NAME" -p 5432 -U "$db_user" -d "$DB_NAME" > /dev/null 2>&1; then
    ready=true
    break
  fi
  if [[ "$(docker inspect -f '{{.State.Running}}' "$NAME" 2> /dev/null)" != "true" ]]; then
    docker logs "$NAME" >&2 || true
    error "The Cloud SQL proxy stopped. See its log above."
  fi
  sleep 2
done
[[ "$ready" == "true" ]] || error "The Cloud SQL proxy did not accept connections in 60 seconds."

echo "Writing $DUMP_NAME. A full dump takes several minutes."
TMP_DUMP=$(mktemp "$CONFIG_DIR/.consentdb.XXXXXX")
docker run --rm --network "$NAME" -e PGPASSWORD "$POSTGRES_IMAGE" \
  pg_dump --no-owner --no-privileges -h "$NAME" -p 5432 -U "$db_user" -d "$DB_NAME" \
  | gzip > "$TMP_DUMP"

# pg_dump writes this line last. Without it, the dump stopped early.
gzip -dc "$TMP_DUMP" | tail -n 5 | grep -q "PostgreSQL database dump complete" \
  || error "The dump is not complete. The old dump in $CONFIG_DIR is unchanged."
# The Postgres image reads the dump as the postgres user. config/ has mode 700, so other users
# on this machine cannot read it.
chmod 644 "$TMP_DUMP"
mv "$TMP_DUMP" "$CONFIG_DIR/$DUMP_NAME"
TMP_DUMP=""
env_set "$ENV_FILE" CONSENT_DB_DUMP "$DUMP_NAME"
echo "Wrote $CONFIG_DIR/$DUMP_NAME, and set CONSENT_DB_DUMP in .env."

# Keep only the new dump. Delete only the dumps that this script made.
for old in "$CONFIG_DIR"/consentdb-dev-*.sql.gz "$CONFIG_DIR"/consentdb-staging-*.sql.gz; do
  [[ -e "$old" && "$(basename "$old")" != "$DUMP_NAME" ]] || continue
  rm -f "$old"
  echo "Deleted the older dump $(basename "$old")."
done

# List the other dumps. This script did not make them, so it does not delete them.
others=$(find "$CONFIG_DIR" -maxdepth 1 -type f \( -name '*.sql' -o -name '*.sql.gz' -o -name '*.dump' \) \
  ! -name 'consentdb-dev-*.sql.gz' ! -name 'consentdb-staging-*.sql.gz' -exec basename {} \; | sort)
if [[ -n "$others" ]]; then
  echo
  echo "These dumps in $CONFIG_DIR were not made by this script. Delete them when you no longer need them:"
  while IFS= read -r name; do
    echo "  $name"
  done <<< "$others"
fi

echo
echo "The local database loads a dump only when its container is new. To load this dump, remove the"
echo "container and its data volume, which holds a copy of the old dump:"
echo "  docker compose -f config/docker-compose.yaml -f config/docker-compose.override.yaml rm -sfv sqlproxy"
echo "  docker compose -f config/docker-compose.yaml -f config/docker-compose.override.yaml up -d"
echo "Then fill the index again: ./scripts/index-es.sh"
