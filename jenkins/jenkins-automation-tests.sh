#!/bin/bash
set -e

# This script is specific to DSP's jenkins CI server.
# Although this script lives in a `jenkins` directory, it is intended to be run from `automation`.
# It is here because it is only intended to be run from a jenkins job and not by developers.

ENV=${1:-$ENV}
TEST_IMAGE=automation-consent

mkdir target

# Render Configurations
docker run --rm \
  -e ENVIRONMENT="$ENV" \
  -e ROOT_DIR="${PWD}" \
  -v /etc/vault-token-dsde:/root/.vault-token \
  -e OUT_PATH=/output/src/test/resources \
  -v "$PWD":/output \
  -e INPUT_PATH=/input \
  -v "$PWD"/configs:/input \
  broadinstitute/dsde-toolbox:dev render-templates.sh

# render role service account credential files
mkdir src/test/resources
mkdir src/test/resources/accounts
secretPath="/secret/dsde/firecloud/${ENV}/consent/automation"

listOfRoles="admin
chair
member
researcher"

for role in $listOfRoles; do
  echo "Writing $role file from $secretPath/duos-automation-$role.json"
  docker run --rm \
    -v "$HOME":/root \
    -v /etc/vault-token-dsde:/root/.vault-token \
    broadinstitute/dsde-toolbox:dev vault read \
    --format=json \
    "$secretPath"/duos-automation-"$role".json |
    jq .data \
    >src/test/resources/accounts/duos-automation-"$role".json
done

# Build Test Image
docker build -f Dockerfile -t ${TEST_IMAGE} .

# Run Tests
docker run -v "${PWD}/target":/app/target ${TEST_IMAGE}
TEST_EXIT_CODE=$?

# exit with exit code of test script
exit $TEST_EXIT_CODE
