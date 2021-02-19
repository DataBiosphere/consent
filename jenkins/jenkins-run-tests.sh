#!/bin/bash
set -e

# This script is specific to DSP's jenkins CI server.
# Although this script lives in a `jenkins` directory, it is intended to be run from `automation`.
# It is here because it is only intended to be run from a jenkins job and not by developers.

TEST_IMAGE=automation-consent

mkdir -p target

# Build Test Image
docker build -f Dockerfile -t ${TEST_IMAGE} .

# Run Tests
docker-compose -f ../docker-compose-automation.yml up --build -d
if docker run -v "${PWD}/target":/app/target --net consent_default -e CONSENT_API_URL='http://consent-api:8000/' ${TEST_IMAGE} 
then
    TEST_EXIT_CODE=$?
    docker-compose -f ../docker-compose-automation.yml down
else
    TEST_EXIT_CODE=$?
    docker-compose -f ../docker-compose-automation.yml down
fi

# Parse Tests
docker run -v "${PWD}/scripts":/working -v "${PWD}/target":/working/target -w /working broadinstitute/dsp-toolbox python interpret_results.py

# exit with exit code of test script
exit $TEST_EXIT_CODE
