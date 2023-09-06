#!/bin/bash
#
# Connects to the consent database in an environment. You MUST have jq installed
# to be able to use this script.
#
# USAGE: ./db-connect.sh ENV
#   ENV must be one of dev, alpha, or staging
#

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
    printf "Usage: %s ${BLD}ENV${RST}\n  ${BLD}ENV${RST} must be one of dev, alpha, or staging\n" "$0"
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
    --help ) usage;;
    dev|alpha|staging ) ;;
    prod ) error "This script cannot be run against prod.";;
    * ) error "ENV must be one of dev, alpha, or staging";;
esac

VAULT_PATH="secret/dsde/firecloud/$1/consent/secrets/postgres/app_sql_user"
VAULT_JSON=$(vault read -format=json "$VAULT_PATH")

INSTANCE=$(echo "$VAULT_JSON" | jq -r .data.instance_name)
USERNAME=$(echo "$VAULT_JSON" | jq -r .data.username)
PASSWORD=$(echo "$VAULT_JSON" | jq -r .data.password)

INSTANCE="broad-dsde-$1:us-central1:$INSTANCE"

cloud_sql_proxy -instances="$INSTANCE"=tcp:5432 -dir=/tmp &
sleep 3
psql "postgresql://$USERNAME:$PASSWORD@localhost:5432/consent"

kill -- -"$$"
