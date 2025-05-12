#!/bin/bash
#
# Connects to the consent database in an environment. You MUST have jq installed
# to be able to use this script.
#
# USAGE: ./db-connect.sh ENV
#   ENV must be one of dev, staging, or prod
#
# NOTE: This does not support bee deployments. You can connect to a bee deployment database
# by running these commands, where <pshapiro-test> is the name of your bee:
#
# Connect to the cluster and port-forward the postgres pod to the local port 5432.
# $ gcloud container clusters get-credentials terra-qa-bees --zone us-central1-a --project broad-dsde-qa
#
# $ kubectl port-forward --namespace terra-pshapiro-test $(kubectl get pod --namespace terra-pshapiro-test --selector="selectorLabel=consent-postgres" --output jsonpath='{.items[0].metadata.name}') 5432:5432 &
#
# Print out the postgres password. The user is always "consent".
# $ kubectl get secret consent-db-creds-eso --namespace terra-pshapiro-test -o json | jq -r .data.password
#
# At this point, you can use psql or an IDE to connect to the database. When you're done, don't
# forget to kill the port-forward process.

set -eu
set -o pipefail

# check if colors are supported in the terminal
check_color_support() {
    NCOLORS=$(tput colors)
    if [ "$NCOLORS" -ge 8 ]; then
        BLD="$(tput bold)"
        RED="$(tput setaf 1)"
        RST="$(tput sgr0)"
    else
        BLD=""
        RED=""
        RST=""
    fi
}

# print out usage to stdout
usage() {
    printf "Usage: %s ${BLD}ENV${RST}\n  ${BLD}ENV${RST} must be one of dev, staging, or prod\n" "$0"
    exit 0
}

# print out error with help message to stderr and exit
error() {
    printf "${RED}ERROR: %s${RST}\n\nTry ${BLD}%s --help${RST} to see a list of all options.\n" "$1" "$0" >&2
    exit 1
}

# print out error to stderr and exit
abort() {
    printf "${RED}ABORT: %s${RST}\n" "$1" >&2
    exit 1
}

# ensure that jq is installed
check_jq_installed() {
    if ! jq --version 1>/dev/null 2>&1; then
        abort "jq v1.6 or above is required; install jq to continue"
    fi
}

# ensure that gcloud and components are installed
check_gcloud_installed() {
    if ! gcloud --version 1>/dev/null 2>&1; then
        abort "gcloud is required; install google-cloud-sdk to continue"
    fi
    if ! cloud_sql_proxy --version 1>/dev/null 2>&1; then
        gcloud components install -q cloud_sql_proxy
    fi
}

check_color_support

check_jq_installed
check_gcloud_installed

if [ -z "${1+:}" ]; then
    usage
fi

case $1 in
    --help) usage;;
    dev)
      PROJECT=broad-dsde-dev
      ;;
    staging)
      PROJECT=broad-dsde-staging
      ;;
    prod)
      PROJECT=broad-dsde-prod
      ;;
    *) error "ENV must be one of dev, staging, or prod";;
esac

PG_JSON=$(gcloud --project $PROJECT secrets versions access latest --secret=consent-postgres-creds)

INSTANCE=$(echo "$PG_JSON" | jq -r .instance_name)
USERNAME=$(echo "$PG_JSON" | jq -r .username)
PASSWORD=$(echo "$PG_JSON" | jq -r .password)

INSTANCE="broad-dsde-$1:us-central1:$INSTANCE"

cloud_sql_proxy -instances="$INSTANCE"=tcp:5432 -dir=/tmp &
sleep 3
psql "postgresql://$USERNAME:$PASSWORD@localhost:5432/consent"

kill -- -"$$"
