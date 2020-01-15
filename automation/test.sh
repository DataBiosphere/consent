#!/usr/bin/env bash
set -e

SBT_CMD=${1-"gatling:test"}
echo $SBT_CMD

set -o pipefail

sbt -Djsse.enableSNIExtension=false clean "${SBT_CMD}"
TEST_EXIT_CODE=$?

if [[ $TEST_EXIT_CODE != 0 ]]; then exit $TEST_EXIT_CODE; fi
