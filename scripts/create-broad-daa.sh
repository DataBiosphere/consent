#!/bin/sh
#
# Prepopulate the system with a Broad Data Access Agreement (DAA) and add it to
# all existing Data Access Committees (DACs). Next, add all users with a library
# card to the Broad DAA.
#
# You MUST have gcloud, curl, and jq installed to run this script.
#
# See usage section below for more information. All arguments are optional.
#

set -eu

usage() {
  cat << EOF
Usage: $0 [OPTION]...
Prepopulate the system with a Broad Data Access Agreement (DAA).

  --env ENV     The environment to prepopulate, either dev, staging, or prod.
                Default is dev
  --file FILE   The file to use for the Broad DAA.
                Default is DUOS_Uniform_Data_Access_Agreement.pdf
  --help        Display this help and exit.
EOF
  exit 0
}

error() {
  echo "ERROR: $1" >&2
  exit 1
}

# default values that may be overridden by command line arguments
ENV="dev"
FILE="DUOS_Uniform_Data_Access_Agreement.pdf"

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
        shift
        ;;
      --file)
        FILE="$2"
        shift
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

curl_get() {
  curl --silent -X GET  --header 'Accept: application/json' --header 'Content-Type: application/json' --header "Authorization: Bearer ${AUTH_TOKEN}" "$1"
}

curl_post() {
  curl --silent -X POST --header 'Accept: application/json' --header 'Content-Type: application/json' --header "Authorization: Bearer ${AUTH_TOKEN}" --data "$2" "$1"
}

curl_post_file() {
  curl --silent -X POST --header 'Accept: application/json' --header 'Content-Type: multipart/form-data' --header "Authorization: Bearer ${AUTH_TOKEN}" --form file="@$2" "$1"
}

curl_put() {
  curl --silent -X PUT  --header 'Accept: application/json' --header 'Content-Type: application/json' --header "Authorization: Bearer ${AUTH_TOKEN}" "$1"
}

check_broad_daa() {
  echo "Populating $ENV environment with Broad DAA"
  if [ "$DAA_NUM" -gt 0 ]; then
    if [ "$DAA_IS_BROAD" = "true" ]; then
      if [ "$ENV" = "dev" ]; then
        echo "WARNING: Broad DAA already exists" >&2
      else
        error "Broad DAA already exists"
      fi
    else
      error "A DAA already exists in the system, and it is not a Broad DAA"
    fi
  fi
}

get_broad_daa() {
  DAA_JSON=$(curl_get "$API/daa")
  DAA_NUM=$(echo "$DAA_JSON" | jq '. | length')
  BROAD_DAA_ID=$(echo "$DAA_JSON" | jq '.[0].daaId')
  DAA_IS_BROAD=$(echo "$DAA_JSON" | jq '.[0].broadDaa')
  DAA_DAC_ID=$(echo "$DAA_JSON" | jq '.[0].initialDacId')
}

get_broad_dac() {
  DAC_LIST=$(curl_get "$API/dac" | sed 's/\\n/ /g')
  if [ "$DAA_DAC_ID" = "null" ]; then
    BROAD_DAC_JSON=$(echo "$DAC_LIST" | jq '.[] | select(.name=="Broad DAC")')
  else
    BROAD_DAC_JSON=$(echo "$DAC_LIST" | jq ".[] | select(.dacId==$DAA_DAC_ID)")
  fi
  BROAD_DAC_ID=$(echo "$BROAD_DAC_JSON" | jq -r '.dacId')
  if [ -z "$BROAD_DAC_JSON" ]; then
    error "Broad DAC not found"
  fi
}

post_broad_daa() {
  BROAD_DAA_JSON=$(curl_post_file "$API/daa/dac/$BROAD_DAC_ID" "$FILE")
  BROAD_DAA_ID=$(echo "$BROAD_DAA_JSON" | jq -r '.daaId')
  echo "Created Broad DAA '$BROAD_DAA_ID' with DAC '$BROAD_DAC_ID'"
}

put_add_daa_to_dacs() {
  DAC_ID_LIST=$(echo "$DAC_LIST" | jq -r '.[].dacId')
  for DAC_ID in $DAC_ID_LIST; do
    curl_put "$API/daa/$BROAD_DAA_ID/dac/$DAC_ID" 1> /dev/null
  done
  echo "Added Broad DAA to all DACs"
}

get_library_card_users() {
  LIBRARY_CARD_JSON=$(curl_get "$API/libraryCards")
  LIBRARY_CARD_USERS=$(echo "$LIBRARY_CARD_JSON" | jq -r '. | map(select(.userId != null) | .userId) | unique | { users: . }')
}

post_bulk_add_users_to_daa() {
  curl_post "$API/daa/bulk/$BROAD_DAA_ID" "$LIBRARY_CARD_USERS"
  echo "Added Broad DAA to all library card users"
}

AUTH_TOKEN=$(gcloud auth print-access-token)

parse_cli_args "$@"
get_broad_daa
check_broad_daa
get_broad_dac
post_broad_daa
put_add_daa_to_dacs
get_library_card_users
post_bulk_add_users_to_daa
