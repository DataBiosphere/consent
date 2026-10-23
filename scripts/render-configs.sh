#!/bin/bash
#
# Makes the local config in config/ that config/docker-compose.yaml needs. See DEVNOTES.md
# ("Render Configs") and docs/plans/render-local-configs-plan.md.
#
# A run with no options writes only the certs, which rotate every 3 months. Other options turn
# on the other files. --all turns on all of them.
#
# You MUST be on the non-split Broad VPN for the steps that read the dev cluster: the certs, and
# the .env values that an old .env does not have. You MUST be logged in with gcloud. The chart
# configs need git read access to broadinstitute/terra-helmfile, over HTTPS or SSH.
#
# Each step writes to a staging folder first, and moves its files into place only when the step
# succeeds. The script stops at the first step that fails, and that step leaves the old files as
# they were.
#

set -eu
set -o pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"

COMPOSE_TEMPLATE="$SCRIPT_DIR/templates/docker-compose.yaml"
ZONE="us-central1-a"
CLUSTER="terra-dev"

# Default values that command line options can change.
PROJECT="broad-dsde-dev"
HELMFILE_REF="master"
HELMFILE_REF_SET="false"
HELMFILE_DIR=""
CONFIG_DIR="$REPO_ROOT/config"
WRITE_CHART_CONFIGS="false"
WRITE_COMPOSE="false"
FETCH_SENDGRID="false"
ALL="false"

usage() {
  cat << EOF
Usage: $0 [OPTION]...
Make the local config files in config/. With no options, write only the certs.

  --project PROJECT                 Google project of the dev cluster, for the certs and dev
                                    secrets. Defaults to broad-dsde-dev.
  --helmfile_ref REF                The terra-helmfile branch or tag to render. Defaults to master.
  --helmfile_dir DIR                Render a local terra-helmfile checkout as it is, with changes
                                    that are not committed. Cannot be used with --helmfile_ref.
  --write_chart_configs true|false  Write consent.yaml, site.conf and oauth2.conf. Defaults to false.
  --write_compose true|false        Write docker-compose.yaml and .env. Defaults to false.
  --fetch_sendgrid true|false       Put the dev SendGrid key in .env. Defaults to false.
  --all true|false                  Write the certs, the chart configs and the compose files.
                                    Defaults to false.
  --config_dir DIR                  Where to write the files. Defaults to config/ in the repo root.
  --help                            Show this help.
EOF
  exit 0
}

error() {
  echo "ERROR: $1" >&2
  exit 1
}

parse_cli_args() {
  while [[ $# -gt 0 ]]; do
    [[ "$1" == "--help" ]] && usage
    [[ $# -ge 2 ]] || error "Option $1 needs a value. Try --help to see a list of all options."
    case "$1" in
      --project) PROJECT=$2 ;;
      --helmfile_ref) HELMFILE_REF=$2; HELMFILE_REF_SET="true" ;;
      --helmfile_dir) HELMFILE_DIR=$2 ;;
      --write_chart_configs) check_bool "$1" "$2"; WRITE_CHART_CONFIGS=$2 ;;
      --write_compose) check_bool "$1" "$2"; WRITE_COMPOSE=$2 ;;
      --fetch_sendgrid) check_bool "$1" "$2"; FETCH_SENDGRID=$2 ;;
      --all) check_bool "$1" "$2"; ALL=$2 ;;
      --config_dir) CONFIG_DIR=$2 ;;
      *) error "Unknown option: $1. Try --help to see a list of all options." ;;
    esac
    shift 2
  done

  if [[ "$HELMFILE_REF_SET" == "true" && -n "$HELMFILE_DIR" ]]; then
    error "Use --helmfile_ref or --helmfile_dir, not both."
  fi
  if [[ -n "$HELMFILE_DIR" ]]; then
    HELMFILE_DIR=$(cd "$HELMFILE_DIR" 2> /dev/null && pwd) || error "--helmfile_dir: no such folder."
  fi
}

# Get the credentials for the dev cluster, one time. The script uses its own kubeconfig, so the
# current kubectl context of the developer does not change. A plain `kubectl get` tries API
# discovery 5 times, and takes more than a minute without the VPN. `get --raw` with a short
# timeout sends one request, so a missing VPN fails fast.
CLUSTER_READY="false"
use_cluster() {
  [[ "$CLUSTER_READY" == "false" ]] || return 0
  command -v kubectl > /dev/null || error "Missing tool: kubectl."
  [[ -n "$(gcloud_account)" ]] || error "Google Cloud is not logged in. Run: gcloud auth login"
  echo "Getting credentials for the $CLUSTER cluster"
  export KUBECONFIG="$WORK_DIR/kubeconfig"
  gcloud container clusters get-credentials "$CLUSTER" --zone "$ZONE" --project "$PROJECT" > /dev/null 2>&1 \
    || error "Could not get credentials for the $CLUSTER cluster in $PROJECT."
  kubectl --request-timeout=10s get --raw /api/v1/namespaces/local-dev/secrets/local-dev-cert > /dev/null 2>&1 \
    || error "Cannot reach the $CLUSTER cluster. Connect the host to the non-split Broad VPN. Your account must be able to read the local-dev and terra-dev namespaces."
  CLUSTER_READY="true"
}

# Print a value from a secret in the terra-dev namespace. Fail if it is empty.
dev_secret() {
  local value
  value=$(kubectl --request-timeout=30s -n terra-dev get secret "$1" -o "go-template={{ index .data \"$2\" | base64decode }}")
  [[ -n "$value" ]] || error "$1 in the terra-dev namespace has no $2."
  echo "$value"
}

write_certs() {
  local stage="$STAGE_DIR/certs" f
  use_cluster
  echo "Writing server.crt, server.key and ca-bundle.crt"
  mkdir -p "$stage"
  kubectl --request-timeout=30s -n local-dev get secrets local-dev-cert -o 'go-template={{ index .data "tls.crt" | base64decode }}' > "$stage/server.crt"
  kubectl --request-timeout=30s -n local-dev get secrets local-dev-cert -o 'go-template={{ index .data "tls.key" | base64decode }}' > "$stage/server.key"
  chmod 600 "$stage/server.key"
  kubectl --request-timeout=30s -n local-dev get configmaps kube-root-ca.crt -o 'go-template={{ index .data "ca.crt" }}' > "$stage/ca-bundle.crt"
  for f in server.crt server.key ca-bundle.crt; do
    [[ -s "$stage/$f" ]] || error "kubectl returned an empty $f."
  done
  openssl x509 -noout -in "$stage/server.crt" || error "server.crt is not a certificate."
  mv "$stage/server.crt" "$stage/server.key" "$stage/ca-bundle.crt" "$CONFIG_DIR/"
}

write_chart_configs() {
  local stage="$STAGE_DIR/chart"
  local source_args=(--helmfile_ref "$HELMFILE_REF")
  if [[ -n "$HELMFILE_DIR" ]]; then
    source_args=(--helmfile_dir "$HELMFILE_DIR")
  fi
  "$SCRIPT_DIR/render-chart-configs.sh" --output_dir "$stage" "${source_args[@]}"
  mv "$stage/consent.yaml" "$stage/site.conf" "$stage/oauth2.conf" "$CONFIG_DIR/"
  env_set "$ENV_FILE" PROXY_IMAGE "$(env_get "$stage/.env" PROXY_IMAGE)"
}

write_compose() {
  local stage="$STAGE_DIR/compose" pg_user pg_password es sendgrid b2c
  echo "Writing docker-compose.yaml and .env"
  mkdir -p "$stage"

  # Values in an old .env carry forward, so a new render never loses them. The script reads the
  # cluster only for values that the old .env does not have.
  pg_user=$(env_get "$ENV_FILE" POSTGRES_USER)
  pg_password=$(env_get "$ENV_FILE" POSTGRES_PASSWORD)
  if [[ -z "$pg_user" || -z "$pg_password" ]]; then
    # The dev database credentials, which duos-ui also writes to its .env.local. With them, the
    # duos-ui compose mode against a local consent stack connects with no change.
    use_cluster
    pg_user=$(dev_secret consent-secrets databaseUser)
    pg_password=$(dev_secret consent-secrets databasePassword)
  fi
  b2c=$(env_get "$ENV_FILE" B2C_APPLICATION_ID)
  if [[ -z "$b2c" ]]; then
    use_cluster
    b2c=$(dev_secret consent-proxy-b2c-secrets application-id)
  fi
  if [[ "$FETCH_SENDGRID" == "true" ]]; then
    use_cluster
    sendgrid=$(dev_secret consent-secrets sendgridApiKey)
  else
    sendgrid=$(env_get "$ENV_FILE" SENDGRID_API_KEY)
    [[ -n "$sendgrid" ]] || sendgrid="placeholder"
  fi
  es=$(env_get "$ENV_FILE" ELASTIC_PASSWORD)
  [[ -n "$es" ]] || es="devpassword"

  # Start from the old .env, so the lines that this script does not manage carry forward, as in
  # duos-ui. PROXY_IMAGE is one of them: the chart step sets it.
  if [[ -f "$ENV_FILE" ]]; then
    cp "$ENV_FILE" "$stage/.env"
  else
    cat > "$stage/.env" << EOF
# Written by scripts/render-configs.sh. Values in an old .env carry forward. This file holds
# secrets: do not share it. docker-compose.yaml reads it.
EOF
  fi
  chmod 600 "$stage/.env"
  env_set "$stage/.env" POSTGRES_USER "$pg_user"
  env_set "$stage/.env" POSTGRES_PASSWORD "$pg_password"
  env_set "$stage/.env" SENDGRID_API_KEY "$sendgrid"
  env_set "$stage/.env" ELASTIC_PASSWORD "$es"
  env_set "$stage/.env" B2C_APPLICATION_ID "$b2c"
  cp "$COMPOSE_TEMPLATE" "$stage/docker-compose.yaml"

  # Back up the old files, then move the new ones into place.
  if [[ -f "$ENV_FILE" ]]; then
    cp -p "$ENV_FILE" "$ENV_FILE.bak"
    chmod 600 "$ENV_FILE.bak"
  fi
  if [[ -f "$CONFIG_DIR/docker-compose.yaml" ]] && ! cmp -s "$COMPOSE_TEMPLATE" "$CONFIG_DIR/docker-compose.yaml"; then
    cp -p "$CONFIG_DIR/docker-compose.yaml" "$CONFIG_DIR/docker-compose.yaml.bak"
  fi
  mv "$stage/.env" "$ENV_FILE"
  mv "$stage/docker-compose.yaml" "$CONFIG_DIR/docker-compose.yaml"

  # Compose stops if a -f file does not exist. Never change an override that exists.
  if [[ ! -e "$CONFIG_DIR/docker-compose.override.yaml" ]]; then
    cat > "$CONFIG_DIR/docker-compose.override.yaml" << 'EOF'
# Changes to docker-compose.yaml for your own machine. render-configs.sh never changes this file.
services: {}
EOF
  fi
}

parse_cli_args "$@"
mkdir -p "$CONFIG_DIR" || error "Could not make $CONFIG_DIR."
# config/ holds secrets. Mode 700 keeps other users on this machine out of it.
chmod 700 "$CONFIG_DIR"
CONFIG_DIR=$(cd "$CONFIG_DIR" && pwd)
ENV_FILE="$CONFIG_DIR/.env"

# WORK_DIR holds the kubeconfig. STAGE_DIR is in config/, so each move into place is a rename on
# the same file system. The trap removes both on each exit, also on Ctrl-C.
WORK_DIR=$(mktemp -d "${TMPDIR:-/tmp}/render-configs.XXXXXX")
STAGE_DIR=$(mktemp -d "$CONFIG_DIR/.render.XXXXXX")
cleanup() {
  rm -rf "$WORK_DIR" "$STAGE_DIR"
}
trap cleanup EXIT
trap 'exit 130' INT TERM

if [[ "$ALL" == "true" ]]; then
  WRITE_CHART_CONFIGS="true"
  WRITE_COMPOSE="true"
fi

write_certs
if [[ "$WRITE_CHART_CONFIGS" == "true" ]]; then
  write_chart_configs
fi
if [[ "$WRITE_COMPOSE" == "true" ]]; then
  write_compose
fi

if [[ -s "$CONFIG_DIR/docker-compose.yaml" ]]; then
  echo
  echo "Next steps:"
  echo "  mvn clean compile"
  echo "  docker compose -f config/docker-compose.yaml -f config/docker-compose.override.yaml up -d"
fi
