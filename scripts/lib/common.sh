#!/bin/bash
#
# Shared helpers for the local config scripts. Source this file; do not run it.
# Written for bash 3.2, the bash that macOS has in /bin/bash.
#

# Clone broadinstitute/terra-helmfile at REF into DEST, as duos-ui does: over HTTPS first, then
# over SSH. Other arguments go to `git clone`. GIT_TERMINAL_PROMPT=0 makes git fail instead of
# asking for a password, so a missing HTTPS credential falls through to SSH.
clone_helmfile() {
  local ref="$1" dest="$2"
  shift 2
  local https_error
  rm -rf "$dest"
  if https_error=$(GIT_TERMINAL_PROMPT=0 git clone --quiet --depth 1 --branch "$ref" "$@" \
    https://github.com/broadinstitute/terra-helmfile.git "$dest" 2>&1); then
    return 0
  fi
  echo "Could not clone terra-helmfile over HTTPS. Trying SSH." >&2
  rm -rf "$dest"
  if GIT_TERMINAL_PROMPT=0 git clone --quiet --depth 1 --branch "$ref" "$@" \
    git@github.com:broadinstitute/terra-helmfile.git "$dest"; then
    return 0
  fi
  # The SSH error is above. Show the HTTPS error too, because it can be the useful one, for
  # example when REF does not exist.
  echo "The HTTPS clone said:" >&2
  local line
  while IFS= read -r line; do
    echo "  $line" >&2
  done <<< "$https_error"
  echo "ERROR: Could not clone terra-helmfile at $ref over HTTPS or SSH. Check that $ref exists, and that your git credentials or SSH key can read broadinstitute/terra-helmfile." >&2
  return 1
}

# Move FILES from STAGE into DEST. STAGE must be on the same file system as DEST, so that each move
# is a rename. A rename into a folder that the script can write fails only if the target is a
# folder. So check all the targets first: then a step does not stop with only some files moved.
# Before the move, copy each old file that changes to FILE.<time>.bak, so that a hand-made file
# is never lost. Each run that changes a file makes a new backup, and keeps the old backups.
move_into_place() {
  local stage="$1" dest="$2" f stamp
  shift 2
  for f in "$@"; do
    if [[ -d "$dest/$f" ]]; then
      echo "ERROR: $dest/$f is a folder. Remove it, then run the script again." >&2
      exit 1
    fi
  done
  stamp=$(date +%Y%m%d-%H%M%S)
  for f in "$@"; do
    if [[ -f "$dest/$f" ]] && ! cmp -s "$stage/$f" "$dest/$f"; then
      cp -p "$dest/$f" "$dest/$f.$stamp.bak"
    fi
  done
  for f in "$@"; do
    mv "$stage/$f" "$dest/$f"
  done
}

# Check that a flag value is true or false.
check_bool() {
  local flag="$1" value="$2"
  if [[ "$value" != "true" && "$value" != "false" ]]; then
    echo "ERROR: $flag must be true or false, not '$value'." >&2
    exit 1
  fi
}
