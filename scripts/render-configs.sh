#!/bin/bash
#
# Makes the local config in config/ that config/docker-compose.yaml needs. See DEVNOTES.md
# ("Render Configs") and docs/plans/render-local-configs-plan.md.
#
# A run with no options writes only the certs, which rotate every 3 months. Other options turn
# on the other files.
#
# You MUST be on the non-split Broad VPN for the cert step, which reads the dev cluster. You MUST
# be logged in with gcloud. The chart configs need git read access to
# broadinstitute/terra-helmfile, over HTTPS or SSH.
#
# Each step writes to a staging folder first, and moves its files into place only when the step
# succeeds. The script stops at the first step that fails, and that step leaves the old files as
# they were.
#

set -eu
set -o pipefail
# With CDPATH, `cd DIR` can print DIR, and then $(cd DIR && pwd) has the path two times.
unset CDPATH

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"

ZONE="us-central1-a"
CLUSTER="terra-dev"

# Default values that command line options can change.
PROJECT="broad-dsde-dev"
HELMFILE_REF="master"
HELMFILE_REF_SET="false"
HELMFILE_DIR=""
CONFIG_DIR="$REPO_ROOT/config"
WRITE_CHART_CONFIGS="false"

usage() {
  cat << EOF
Usage: $0 [OPTION]...
Make the local config files in config/. With no options, write only the certs.

  --project PROJECT                 Google project of the dev cluster, for the certs.
                                    Defaults to broad-dsde-dev.
  --helmfile_ref REF                The terra-helmfile branch or tag to render. Defaults to master.
  --helmfile_dir DIR                Render a local terra-helmfile checkout as it is, with changes
                                    that are not committed. Cannot be used with --helmfile_ref.
  --write_chart_configs true|false  Write consent.yaml, site.conf and oauth2.conf. Defaults to false.
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
  for tool in gcloud kubectl; do
    command -v "$tool" > /dev/null || error "Missing tool: $tool."
  done
  command -v gke-gcloud-auth-plugin > /dev/null || error "Missing tool: gke-gcloud-auth-plugin. Run: gcloud components install gke-gcloud-auth-plugin"
  [[ -n "$(gcloud auth list --filter=status:ACTIVE --format='value(account)' 2> /dev/null)" ]] || error "Google Cloud is not logged in. Run: gcloud auth login"
  echo "Getting credentials for the $CLUSTER cluster"
  export KUBECONFIG="$WORK_DIR/kubeconfig"
  gcloud container clusters get-credentials "$CLUSTER" --zone "$ZONE" --project "$PROJECT" > /dev/null 2>&1 \
    || error "Could not get credentials for the $CLUSTER cluster in $PROJECT."
  kubectl --request-timeout=10s get --raw /api/v1/namespaces/local-dev/secrets/local-dev-cert > /dev/null 2>&1 \
    || error "Cannot reach the $CLUSTER cluster. Connect the host to the non-split Broad VPN. Your account must be able to read the local-dev and terra-dev namespaces."
  CLUSTER_READY="true"
}

write_certs() {
  local stage="$STAGE_DIR/certs" f cert_pub key_pub
  command -v openssl > /dev/null || error "Missing tool: openssl."
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
  # A missing value makes kubectl write "<no value>", so check what each file holds.
  cert_pub=$(openssl x509 -noout -pubkey -in "$stage/server.crt" 2> /dev/null) || error "server.crt is not a certificate."
  key_pub=$(openssl pkey -pubout -in "$stage/server.key" 2> /dev/null) || error "server.key is not a private key."
  [[ "$cert_pub" == "$key_pub" ]] || error "server.key does not match server.crt."
  openssl x509 -noout -in "$stage/ca-bundle.crt" 2> /dev/null || error "ca-bundle.crt is not a certificate."
  move_into_place "$stage" "$CONFIG_DIR" server.crt server.key ca-bundle.crt
}

write_chart_configs() {
  local source_args=(--helmfile_ref "$HELMFILE_REF")
  if [[ -n "$HELMFILE_DIR" ]]; then
    source_args=(--helmfile_dir "$HELMFILE_DIR")
  fi
  # The chart script stages its files and backs up each old file that changes.
  "$SCRIPT_DIR/render-chart-configs.sh" --output_dir "$CONFIG_DIR" "${source_args[@]}"
}

parse_cli_args "$@"
# Check this before the certs step, which is slow.
if [[ "$WRITE_CHART_CONFIGS" == "false" && ( "$HELMFILE_REF_SET" == "true" || -n "$HELMFILE_DIR" ) ]]; then
  error "--helmfile_ref and --helmfile_dir need --write_chart_configs true."
fi
# config/ holds secrets. Mode 700 keeps other users on this machine out of it. The script sets the
# mode only on config/ and on a folder that it makes, not on an existing folder such as ~.
if [[ ! -d "$CONFIG_DIR" || "$CONFIG_DIR" == "$REPO_ROOT/config" ]]; then
  mkdir -p "$CONFIG_DIR" || error "Could not make $CONFIG_DIR."
  chmod 700 "$CONFIG_DIR"
fi
CONFIG_DIR=$(cd "$CONFIG_DIR" && pwd)

# WORK_DIR holds the kubeconfig. STAGE_DIR is in config/, so each move into place is a rename on
# the same file system. The trap removes both on each exit, also on Ctrl-C.
WORK_DIR=$(mktemp -d "${TMPDIR:-/tmp}/render-configs.XXXXXX")
STAGE_DIR=$(mktemp -d "$CONFIG_DIR/.render.XXXXXX")
cleanup() {
  rm -rf "$WORK_DIR" "$STAGE_DIR"
}
trap cleanup EXIT
trap 'exit 130' INT TERM

write_certs
if [[ "$WRITE_CHART_CONFIGS" == "true" ]]; then
  write_chart_configs
fi
