#!/usr/bin/env bash
set -e

## Run from automation/
## Pulls templatized configs and renders them to src/test/resources

# Defaults
WORKING_DIR=$PWD
ENV=dev
VAULT_TOKEN=$(cat ~/.vault-token)

# Parameters
ENV=${1:-$ENV}
VAULT_TOKEN=${2:-$VAULT_TOKEN}

docker pull broadinstitute/dsde-toolbox:dev

# render application.conf
docker run -it --rm -e VAULT_TOKEN="${VAULT_TOKEN}" \
  -e ENVIRONMENT="$ENV" \
  -e ROOT_DIR="${WORKING_DIR}" \
  -e OUT_PATH=/output/src/test/resources \
  -v "$PWD":/output \
  -e INPUT_PATH=/input \
  -v "$PWD"/configs:/input \
  broadinstitute/dsde-toolbox:dev render-templates.sh

# render role service account credential files
if [ "$ENV" == "local" ]; then
  secretPath="/secret/dsde/firecloud/dev/consent/automation"
else
  secretPath="/secret/dsde/firecloud/${ENV}/consent/automation"
fi

listOfRoles="admin
chair
member
researcher"

for role in $listOfRoles; do
  echo "Writing $role file from $secretPath/duos-automation-$role.json"
  docker run -it --rm \
    -v "$HOME":/root \
    broadinstitute/dsde-toolbox:dev vault read \
    --format=json \
    "$secretPath"/duos-automation-"$role".json |
    jq .data \
    >src/test/resources/accounts/duos-automation-"$role".json
done
