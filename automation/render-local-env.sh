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

# render application.conf
docker pull broadinstitute/dsde-toolbox:dev
docker run -it --rm -e VAULT_TOKEN=${VAULT_TOKEN} \
    -e ENVIRONMENT=${ENV} \
    -e ROOT_DIR=${WORKING_DIR} \
    -e OUT_PATH=/output/src/test/resources \
    -v $PWD:/output \
    -e INPUT_PATH=/input \
    -v $PWD/configs:/input \
    broadinstitute/dsde-toolbox:dev render-templates.sh

# render role-user service account credentials
listOfRoles="admin
chair
member
researcher"

for role in $listOfRoles
  do
    echo "Writing $role file... to src/test/resources/accounts/"
    docker run -it --rm \
        -v $HOME:/root \
        broadinstitute/dsde-toolbox:dev vault read \
        --format=json \
        /secret/dsde/firecloud/dev/consent/automation/duos-automation-$role.json \
        | jq .data \
        > src/test/resources/accounts/duos-automation-$role.json
  done
