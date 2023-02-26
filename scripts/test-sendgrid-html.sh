#!/bin/bash
#
# Check that a sendgrid template renders correctly in an email.
#
# USAGE: ./test-sendgrid-html.sh EMAIL HTML
#   EMAIL must specify your email address
#   HTML  must specify the html template to use
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
    printf "Usage: %s ${BLD}EMAIL${RST} ${BLD}HTML${RST}\n  ${BLD}EMAIL${RST} your email address\n  ${BLD}HTML${RST}  the html template name to use\n" "$0"
    exit 0
}

# print out error with help message to stderr and exit
error() {
    printf "${RED}ERROR: %s${RST}\n\nTry ${BLD}%s --help${RST} to see a list of all options.\n" "$1" "$0" >&2
    exit 1
}

check_color_support

# if arguments are missing, print out usage
if [ -z "${1+:}" ] || [ -z "${2+:}" ]; then
    usage
fi

# check if first argument is an email address
case $1 in
    --help ) usage;;
    *@* ) ;;
    * ) error "Invalid email address";;
esac
case $2 in
    *.html ) ;;
    * ) error "Invalid html template";;
esac

# go to parent directory
cd "$(dirname "$PWD")"

# convert template into payload for json
TEMPLATE=$(find src/main/resources/freemarker -name "$2")
if ! echo "${TEMPLATE}" | grep . ; then
    error "Template not found"
fi
PAYLOAD=$(tr '\n' '    ' < "${TEMPLATE}" | sed -e 's/    //g' -e 's/"/\\"/g')

# create full json payload
FROM=$(grep googleAccount config/consent.yaml | cut -d ':' -f2 | sed 's/ //')
JSON=$(cat <<- EOF
{
    "personalizations": [
        {
            "to": [
                {
                    "email": "$1"
                }
            ],
            "subject": "DUOS template: $2"
        }
    ],
    "content": [
        {
            "type": "text/html",
            "value": "$PAYLOAD"
        }
    ],
    "from": {
        "email": "$FROM",
        "name": "DUOS"
    },
    "reply_to": {
        "email": "$1"
    }
}
EOF
)

# send payload using sendgrid api
API_KEY=$(grep sendGridApiKey config/consent.yaml | cut -d ':' -f2 | sed 's/ //')
curl --request POST \
    --url https://api.sendgrid.com/v3/mail/send \
    --header "Authorization: Bearer ${API_KEY}" \
    --header "Content-Type: application/json" \
    --data "$JSON"
