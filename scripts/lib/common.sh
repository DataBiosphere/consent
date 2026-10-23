#!/bin/bash
#
# Shared helpers for the local config scripts. Source this file; do not run it.
# Written for bash 3.2, the bash that macOS has in /bin/bash.
#

# The active gcloud account, or nothing.
gcloud_account() {
  gcloud auth list --filter=status:ACTIVE --format='value(account)' 2> /dev/null | head -1
}
