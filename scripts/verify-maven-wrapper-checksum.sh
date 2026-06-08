#!/usr/bin/env bash
set -euo pipefail

readonly PROPERTIES_FILE=".mvn/wrapper/maven-wrapper.properties"
readonly EXPECTED_LINE="distributionSha256Sum=0d7125e8c91097b36edb990ea5934e6c68b4440eef4ea96510a0f6815e7eeadb"

ACTUAL_LINE="$(grep '^distributionSha256Sum=' "${PROPERTIES_FILE}" || true)"

if [[ "${ACTUAL_LINE}" != "${EXPECTED_LINE}" ]]; then
  echo "Expected ${EXPECTED_LINE}"
  echo "Found ${ACTUAL_LINE:-<missing>}"
  exit 1
fi

echo "Maven wrapper checksum pin verified."
