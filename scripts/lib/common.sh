#!/bin/bash
#
# Shared helpers for the local config scripts. Source this file; do not run it.
# Written for bash 3.2, the bash that macOS has in /bin/bash.
#

# Print the value of KEY from an env file, or nothing. One layer of surrounding quotes is
# removed, because the scripts write values in single quotes. The file is never sourced:
# config/.env holds secrets, and a value must not run as shell code.
env_get() {
  local file="$1" key="$2" val
  [[ -f "$file" ]] || return 0
  val=$(grep -E "^${key}=" "$file" | tail -1 | cut -d= -f2- || true)
  if [[ $val == \'*\' || $val == \"*\" ]]; then
    val=${val:1:${#val}-2}
  fi
  echo "$val"
}

# Set KEY to VALUE in an env file, and keep the other lines. The new file replaces the old one
# with a rename, so a failure leaves the old file as it was. The file gets mode 600.
env_set() {
  local file="$1" key="$2" value="$3" tmp
  if [[ "$value" == *"'"* ]]; then
    echo "ERROR: the value for $key has a single quote, which config/.env cannot hold." >&2
    return 1
  fi
  tmp=$(mktemp "$(dirname "$file")/.env.XXXXXX")
  chmod 600 "$tmp"
  if [[ -f "$file" ]]; then
    grep -vE "^${key}=" "$file" > "$tmp" || true
  fi
  printf "%s='%s'\n" "$key" "$value" >> "$tmp"
  mv "$tmp" "$file"
}

# Clone broadinstitute/terra-helmfile at REF into DEST, as duos-ui does: over HTTPS first, then
# over SSH. Other arguments go to `git clone`. GIT_TERMINAL_PROMPT=0 makes git fail instead of
# asking for a password, so a missing HTTPS credential falls through to SSH.
clone_helmfile() {
  local ref="$1" dest="$2"
  shift 2
  rm -rf "$dest"
  if GIT_TERMINAL_PROMPT=0 git clone --quiet --depth 1 --branch "$ref" "$@" \
    https://github.com/broadinstitute/terra-helmfile.git "$dest" 2> /dev/null; then
    return 0
  fi
  echo "Could not clone terra-helmfile over HTTPS. Trying SSH." >&2
  rm -rf "$dest"
  if GIT_TERMINAL_PROMPT=0 git clone --quiet --depth 1 --branch "$ref" "$@" \
    git@github.com:broadinstitute/terra-helmfile.git "$dest"; then
    return 0
  fi
  echo "ERROR: Could not clone terra-helmfile at $ref over HTTPS or SSH. Check that your git credentials or SSH key can read broadinstitute/terra-helmfile." >&2
  return 1
}

# Check that a flag value is true or false.
check_bool() {
  local flag="$1" value="$2"
  if [[ "$value" != "true" && "$value" != "false" ]]; then
    echo "ERROR: $flag must be true or false, not '$value'." >&2
    exit 1
  fi
}
