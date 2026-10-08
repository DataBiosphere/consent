#!/bin/bash
# Shared helpers. Source this file: . scripts/scram-password/lib.sh

# parse_creds JSON
# Reads the credentials JSON {"username": "...", "password": "..."} and sets PW and U.
# Returns 1 with a message on stderr, and sets nothing useful, unless the password is a
# non-empty string that a shell variable keeps exactly.
#
# Two traps that this guards against:
#   - jq -j prints a missing or null .password as the text "null", which is not empty.
#   - $(...) drops trailing newlines, so a password that ends in a newline would change.
# PW and U are the results. The script that sources this file reads them.
# shellcheck disable=SC2034
parse_creds() {
  local json=$1 raw_len kept_len
  PW=""; U=""
  PW=$(printf '%s' "$json" | jq -j 'if (.password | type) == "string" then .password else empty end') || return 1
  U=$(printf '%s' "$json" | jq -r 'if (.username | type) == "string" then .username else empty end') || return 1
  if [ -z "$PW" ]; then
    echo "the credentials have no non-empty string password" >&2
    PW=""; return 1
  fi
  raw_len=$(printf '%s' "$json" | jq -j '.password' | wc -c | tr -d ' ')
  kept_len=$(printf '%s' "$PW" | wc -c | tr -d ' ')
  if [ "$raw_len" != "$kept_len" ]; then
    echo "the password ends with a newline, which a shell variable cannot keep" >&2
    PW=""; return 1
  fi
  return 0
}
