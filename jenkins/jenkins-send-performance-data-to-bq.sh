#!/usr/bin/env bash

LOG_LEVEL_DEBUG="${LOG_LEVEL_DEBUG:-0}"
LOG_LEVEL_INFO="${LOG_LEVEL_INFO:-${LOG_LEVEL_DEBUG:-0}}"

readonly script_name="${0}"

#
# Functions
#
log() {
    local _timestamp
    local _log_string

    if [[ -n ${1} ]]; then _log_string="${*}"; else printf 'ERROR-MISSING_%s_PARAM_%s: %s\n' "REQ" "1" "_log_string"; return 1; fi

    _timestamp="$(date +'%Y-%m-%dT%H:%M:%S%z')"

    printf '[%s]: %s\n' \
        "${_timestamp:?}" \
        "${_log_string:?}"
}


#
# Main
#
log "$(printf 'Executing script %s...\n' \
        "${script_name:?}" \
    )"

# Set CLOUDSDK_CONFIG to unique temp directory
export BUILD_TMP="${HOME}/${BUILD_TAG}"
mkdir -p "${BUILD_TMP}"
export CLOUDSDK_CONFIG=${BUILD_TMP}
echo "CLOUDSDK_CONFIG=${CLOUDSDK_CONFIG}"


# pull gcloud svc acct from vault
docker run -e VAULT_TOKEN=$(cat /etc/vault-token-dsde) broadinstitute/dsde-toolbox vault read --format=json secret/dsde/firecloud/dev/common/firecloud-account.json | jq '.data' > service-acct.json

# auth with gcloud svc acct
gcloud auth activate-service-account --key-file=service-acct.json


PROJECT=broad-dsde-dev

BUCKET=automated-performance-test-results-dev
NAMESPACE="automated_performance_tests"

listOfTables="simulation
scenario
request"

for table in $listOfTables; do
    echo "Writing to $table..."
    local_file_name="$(ls target/test-reports/ | grep $table.json | head -n 1)"
    local_table_file_fqn="target/test-reports/$local_file_name"
    gcs_bucket_table_file_url="gs://${BUCKET:?}/${local_file_name:?}"
    bq_dst_table_fqn="${PROJECT:?}:${NAMESPACE:?}.${table:?}"

    # copy the local results to the GCS bucket
    log "$(printf 'INFO-COPYING_LOCAL_TABLE_FILE_TO_GCS_BUCKET: %s -> %s' \
            "${local_table_file_fqn:?}" \
            "${gcs_bucket_table_file_url:?}"
            )"

    gsutil cp -p "${local_table_file_fqn:?}" "${gcs_bucket_table_file_url:?}"

    gsutil_cp_exit_status="$?"

    if [[ gsutil_cp_exit_status -eq 0 ]]; then
        log "$(printf 'INFO-GSUTIL_CP_SUCCEEDED: %s -> %s\n' \
                "${local_table_file_fqn:?}" \
                "${gcs_bucket_table_file_url:?}" \
                )"
    else
        log "$(printf 'ERROR-GSUTIL_CP_FAILED: %s -> %s. Exit status: %s' \
                "${local_table_file_fqn:?}" \
                "${gcs_bucket_table_file_url:?}" \
                "${gsutil_cp_exit_status:?}" \
                )"
        log "$(printf 'INFO-EXIT_STATUS_OF_COMMAND: %s: %s' \
                "gsutil cp" \
                "${gsutil_cp_exit_status:?}" \
                )"
    fi

    # upload results to bq using the load function
    if [[ ${gsutil_cp_exit_status:?} -eq 0 ]]; then
        log "$(printf 'INFO-EXECUTING_BQ_LOAD: %s -> %s' \
                "${local_table_file_fqn:?}" \
                "${gcs_bucket_table_file_url:?}" \
                )"

        if ((LOG_LEVEL_DEBUG)); then
            bq load \
                --apilog=stdout \
                --format=prettyjson \
                --headless \
                --project_id=${PROJECT} \
                --service_account_credential_file=service-acct.json \
                --source_format=NEWLINE_DELIMITED_JSON \
                "${bq_dst_table_fqn}" \
                "${gcs_bucket_table_file_url:?}"
        else
            bq load \
                --headless \
                --project_id=${PROJECT} \
                --service_account_credential_file=service-acct.json \
                --source_format=NEWLINE_DELIMITED_JSON \
                "${bq_dst_table_fqn}" \
                "${gcs_bucket_table_file_url:?}"
        fi

        bq_load_exit_status="$?"

        if [[ ${bq_load_exit_status} -eq 0 ]]; then
            log "$(printf 'INFO-BQ_LOAD_SUCCEEDED: %s -> %s' \
                    "${gcs_bucket_table_file_url:?}" \
                    "${bq_dst_table_fqn}" \
                    )"
        else
            log "$(printf 'ERROR-BQ_LOAD_FAILED: %s -> %s' \
                    "${gcs_bucket_table_file_url:?}" \
                    "${bq_dst_table_fqn}" \
                    )"
            log "$(printf 'INFO-EXIT_STATUS_OF_COMMAND: %s: %s' \
                    "bq load" \
                    "${bq_load_exit_status:?}" \
                    )"
        fi
    else
        log "$(printf 'WARN-SKIPPING_BQ_LOAD: %s -> %s' \
                "${local_table_file_fqn:?}" \
                "${gcs_bucket_table_file_url:?}" \
                )"
    fi
done

# exit with exit code of test script
exit $TEST_EXIT_CODE