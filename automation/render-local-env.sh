#!/usr/bin/env bash
set -e

## Run from automation/
## Pulls templatized configs and renders them to src/test/resources

# Defaults
WORKING_DIR=$PWD
FC_INSTANCE=fiab
VAULT_TOKEN=$(cat ~/.vault-token)
ENV=dev

# Parameters
FC_INSTANCE=${1:-$FC_INSTANCE}
VAULT_TOKEN=${2:-$VAULT_TOKEN}
ENV=${3:-$ENV}

# render application.conf
docker pull broadinstitute/dsde-toolbox:dev
docker run -it --rm -e VAULT_TOKEN=${VAULT_TOKEN} \
    -e ENVIRONMENT=${ENV} -e FC_INSTANCE=${FC_INSTANCE} -e ROOT_DIR=${WORKING_DIR} \
    -e OUT_PATH=/output/src/test/resources -e INPUT_PATH=/input \
    -v $PWD/configs:/input -v $PWD:/output \
    broadinstitute/dsde-toolbox:dev render-templates.sh
