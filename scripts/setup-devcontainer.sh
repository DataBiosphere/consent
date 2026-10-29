#!/bin/bash
#
# Runs after the Dev Container is created. Installs gcloud, asks for the gcloud logins, then makes
# the local config files with render-configs.sh --all true. This follows the duos-ui script. Each
# step can run again safely, so run this script again after a failure or when the certs rotate.
#
# Developers who do not use the Dev Container run render-configs.sh directly.

set -eu
set -o pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

# On failure, tell the developer how to recover. Most failures come from a missing VPN
# connection, which blocks kubectl.
on_exit() {
  local status=$?
  if ((status != 0)); then
    echo >&2
    echo "Dev Container setup failed. Are you connected to the non-split Broad VPN?" >&2
    echo "Fix the issue, then rebuild the container or run: ./scripts/setup-devcontainer.sh" >&2
  fi
}
trap on_exit EXIT

# gcloud and the GKE auth plugin come from Google's own apt repository, as in duos-ui. jq,
# openssl and gzip are for the local config scripts.
install_tools() {
  curl -fsSL https://packages.cloud.google.com/apt/doc/apt-key.gpg \
    | sudo gpg --batch --yes --dearmor -o /usr/share/keyrings/cloud.google.gpg
  echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt cloud-sdk main" \
    | sudo tee /etc/apt/sources.list.d/google-cloud-sdk.list > /dev/null
  sudo apt-get update
  sudo apt-get install -y google-cloud-cli google-cloud-cli-gke-gcloud-auth-plugin jq openssl gzip
}

# Log in on each run, as duos-ui does, so each build, rebuild or manual run has fresh credentials.
# The compose template mounts the Application Default Credentials into the app.
install_consent_config() {
  printf "\n"
  gcloud auth login
  gcloud auth application-default login
  ./scripts/render-configs.sh --all true
}

install_tools
install_consent_config
