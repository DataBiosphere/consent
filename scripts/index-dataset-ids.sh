#!/bin/bash
#
# Sequentially index DUOS datasets in ElasticSearch
#
# You MUST have gcloud, curl, and jq installed to run this script.
#
# See usage section below for more information. All arguments are optional.
#

set -eu

usage() {
  cat << EOF
Usage: $0 [OPTION]...
Sequentially index DUOS datasets in ElasticSearch

  --env ENV     The environment to prepopulate, either dev, staging, or prod.
                Default is dev
EOF
  exit 0
}

error() {
  echo "ERROR: $1" >&2
  exit 1
}

# default values that may be overridden by command line arguments
ENV="dev"

parse_cli_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --env)
        case "$2" in
          dev|staging|prod)
            ;;
          *)
            error "Invalid environment: $2. Environment must be dev, staging, or prod."
            ;;
        esac
        ENV="$2"
        shift 2
        ;;
      --help)
        usage
        ;;
      *)
        error "Unknown option: $1. Try --help to see a list of all options."
        ;;
    esac
  done
  API="https://consent.dsde-$ENV.broadinstitute.org/api"
}

curl_post() {
  curl --silent -X POST --header 'Accept: application/json' --header 'Content-Type: application/json' --header "Authorization: Bearer ${AUTH_TOKEN}" "$1"
}

post_ids() {
  ID_LIST=$(curl --silent -X GET --header 'Accept: application/json' --header 'Content-Type: application/json' --header "Authorization: Bearer ${AUTH_TOKEN}" "$API/dataset/v3" | jq '.[].dataset_id')
  for ID in $ID_LIST; do
    echo "Indexing $ID"
    curl_post "$API/dataset/index/$ID" > /dev/null
  done
  echo "Finished indexing IDs"
}

AUTH_TOKEN=$(gcloud auth print-access-token)

parse_cli_args "$@"
post_ids
