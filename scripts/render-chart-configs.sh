#!/bin/bash
#
# Renders consent.yaml, site.conf and oauth2.conf from the consent chart in terra-helmfile, with
# the dev values and scripts/templates/local-values.yaml. The rendered files are the reference
# for local config: do not edit them. Put a local change in local-values.yaml, with its reason.
#
# The script also sets PROXY_IMAGE in the .env file of the output folder to the proxy image of the
# rendered deployment, so the local proxy is the same as the dev proxy.
#
# You MUST have helm and git installed. Without --helmfile_dir, you MUST also have git read
# access to broadinstitute/terra-helmfile, over HTTPS or SSH. This script does not need the VPN.
#
# render-configs.sh calls this script. It also runs on its own.
#

set -eu
set -o pipefail
# With CDPATH, `cd DIR` can print DIR, and then $(cd DIR && pwd) has the path two times.
unset CDPATH

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"
LOCAL_VALUES="$SCRIPT_DIR/templates/local-values.yaml"

HELMFILE_REF="master"
HELMFILE_DIR=""
OUTPUT_DIR="$REPO_ROOT/config"

usage() {
  cat << EOF
Usage: $0 [OPTION]...
Render consent.yaml, site.conf and oauth2.conf from the consent chart in terra-helmfile, and
set PROXY_IMAGE in .env.

  --helmfile_ref REF   The terra-helmfile branch or tag to render. Defaults to master.
  --helmfile_dir DIR   Render a local terra-helmfile checkout as it is, with changes that are
                       not committed. Cannot be used with --helmfile_ref.
  --output_dir DIR     Where to write the files. Defaults to config/ in the repo root.
  --help               Show this help.
EOF
  exit 0
}

error() {
  echo "ERROR: $1" >&2
  exit 1
}

parse_cli_args() {
  local ref_set=false
  while [[ $# -gt 0 ]]; do
    [[ "$1" == "--help" || $# -ge 2 ]] || error "Option $1 needs a value. Try --help to see a list of all options."
    case "$1" in
      --helmfile_ref)
        HELMFILE_REF=$2
        ref_set=true
        shift 2
        ;;
      --helmfile_dir)
        HELMFILE_DIR=$2
        shift 2
        ;;
      --output_dir)
        OUTPUT_DIR=$2
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
  if [[ "$ref_set" == "true" && -n "$HELMFILE_DIR" ]]; then
    error "Use --helmfile_ref or --helmfile_dir, not both."
  fi
}

# Secrets go only in config/.env. A key name like these in the tracked values file is almost
# always a mistake. This check cannot find a secret under a key with a neutral name.
check_local_values() {
  local hits
  hits=$(grep -inE '^[[:space:]]*[A-Za-z0-9_.-]*(password|secret|token|apikey|api_key|dsn)[A-Za-z0-9_.-]*[[:space:]]*:' "$LOCAL_VALUES" || true)
  if [[ -n "$hits" ]]; then
    echo "ERROR: $LOCAL_VALUES has a key that looks like a secret:" >&2
    echo "$hits" | sed -E 's/^([0-9]+):[[:space:]]*([^:]+):.*/  line \1: \2/' >&2
    echo "This file is tracked. Put secrets only in config/.env." >&2
    exit 1
  fi
}

# Sets SOURCE_DIR to a terra-helmfile tree, and prints where it came from.
get_source() {
  if [[ -n "$HELMFILE_DIR" ]]; then
    [[ -d "$HELMFILE_DIR/charts/consent" ]] || error "$HELMFILE_DIR is not a terra-helmfile checkout."
    SOURCE_DIR=$(cd "$HELMFILE_DIR" && pwd)
    # Read-only git commands only. Never fetch, checkout, pull or stash in this checkout.
    local commit="commit unknown" dirty=""
    if git -C "$SOURCE_DIR" rev-parse HEAD > /dev/null 2>&1; then
      commit="commit $(git -C "$SOURCE_DIR" rev-parse --short HEAD)"
      if [[ -n "$(git -C "$SOURCE_DIR" status --porcelain 2> /dev/null)" ]]; then
        dirty=" (uncommitted changes)"
      fi
    fi
    echo "terra-helmfile: $SOURCE_DIR, $commit$dirty"
  else
    SOURCE_DIR="$WORK_DIR/terra-helmfile"
    clone_helmfile "$HELMFILE_REF" "$SOURCE_DIR" || exit 1
    echo "terra-helmfile: $HELMFILE_REF, commit $(git -C "$SOURCE_DIR" rev-parse --short HEAD)"
  fi
}

# helm packages each file:// dependency with the charts/ folder it has at that time, and the
# vendored packages in a checkout can be stale. So build the dependencies of each dependency
# first, then the consent chart, all in a copy.
build_dependencies() {
  cp -R "$SOURCE_DIR/charts" "$WORK_DIR/charts"
  local consent="$WORK_DIR/charts/consent" dep
  for dep in $(grep -E 'repository: *"?file://\.\./' "$consent/Chart.yaml" | sed -E 's#.*file://\.\./([^"]+)"?.*#\1#'); do
    if grep -qE '^dependencies:' "$WORK_DIR/charts/$dep/Chart.yaml"; then
      rm -rf "$WORK_DIR/charts/$dep/charts"
      helm dependency update --skip-refresh "$WORK_DIR/charts/$dep" > /dev/null \
        || error "helm could not build the dependencies of the $dep chart."
    fi
  done
  rm -rf "$consent/charts"
  helm dependency update --skip-refresh "$consent" > /dev/null \
    || error "helm could not build the dependencies of the consent chart."
}

# Helmfile renders the .gotmpl values files with the environment. Only these two fields are in
# use. Any other template syntax means a change that this script must learn about.
render_values() {
  local src="$1" dest="$2"
  sed -e 's/{{ *\.Values\.Environment\.Name *}}/dev/g' \
      -e 's/{{ *\.Values\.Destination\.ConfigBase *}}/live/g' "$src" > "$dest"
  if grep -q '{{' "$dest"; then
    error "$src has template syntax that this script cannot render. Update scripts/render-chart-configs.sh."
  fi
}

# Print the block value of one key of a rendered ConfigMap. helm writes each key at two spaces
# and its block content at four spaces. A line at zero or two spaces ends the block.
extract_key() {
  local key="$1" file="$2"
  awk -v k="  $key: " '
    index($0, k) == 1 { on = 1; next }
    on && (/^[^ ]/ || /^  [^ ]/) { on = 0 }
    on { sub(/^    /, ""); print }
  ' "$file"
}

# The value layers of the consent release in dev, in the order of helmfile.yaml in terra-helmfile.
# Each layer can be a .yaml.gotmpl file, a .yaml file, or both. Like helmfile, skip a missing file.
VALUE_LAYERS=(
  values/app/global
  values/app/global/live
  values/app/global/live/dev
  charts/consent/values/live
  charts/consent/values/live/dev
  values/app/consent
  values/app/consent/live
  values/app/consent/live/dev
)

render() {
  local values="$WORK_DIR/values" layer n=0 value_args=()
  mkdir -p "$values"
  [[ -f "$SOURCE_DIR/values/app/consent/live/dev.yaml" ]] \
    || error "terra-helmfile has no values/app/consent/live/dev.yaml. The layout changed. Update scripts/render-chart-configs.sh."
  for layer in "${VALUE_LAYERS[@]}"; do
    if [[ -f "$SOURCE_DIR/$layer.yaml.gotmpl" ]]; then
      n=$((n + 1))
      render_values "$SOURCE_DIR/$layer.yaml.gotmpl" "$values/$n.yaml"
      value_args+=(-f "$values/$n.yaml")
    fi
    if [[ -f "$SOURCE_DIR/$layer.yaml" ]]; then
      value_args+=(-f "$SOURCE_DIR/$layer.yaml")
    fi
  done

  # The local values go last.
  helm template consent "$WORK_DIR/charts/consent" \
    "${value_args[@]}" \
    -f "$LOCAL_VALUES" \
    --show-only templates/configmap.yaml \
    --show-only templates/proxy-configmap.yaml \
    --show-only templates/deployment.yaml > "$WORK_DIR/rendered.yaml" \
    || error "helm template failed for the consent chart."

  local stage="$STAGE_DIR"
  extract_key consent.yaml "$WORK_DIR/rendered.yaml" > "$stage/consent.yaml"
  extract_key oauth2-config "$WORK_DIR/rendered.yaml" > "$stage/oauth2.conf"
  extract_key apache-httpd-proxy-config "$WORK_DIR/rendered.yaml" > "$stage/site.conf"

  local f
  for f in consent.yaml oauth2.conf site.conf; do
    [[ -s "$stage/$f" ]] || error "The render made an empty $f. The chart layout changed. Update scripts/render-chart-configs.sh."
    if grep -q '{{' "$stage/$f"; then
      error "The rendered $f has template syntax. Update scripts/render-chart-configs.sh."
    fi
  done
  grep -q 'ProxyPass' "$stage/site.conf" || error "The rendered site.conf has no ProxyPass. Update scripts/render-chart-configs.sh."
  # A terra-helmfile ref from before 2026-10-27 does not have the liquibaseRunOnStartup chart value.
  if ! grep -qE '^  runOnStartup: true' "$stage/consent.yaml"; then
    echo "WARNING: The rendered consent.yaml does not run the Liquibase changesets at startup. This terra-helmfile ref has no liquibaseRunOnStartup chart value. Start the app with -Ddw.liquibase.runOnStartup=true." >&2
  fi

  # The image of the consent-proxy container in the rendered deployment, with all value overrides.
  local proxy_image
  proxy_image=$(awk '
    /^ *- name: consent-proxy *$/ { on = 1; next }
    on && /^ *image:/ { gsub(/"/, ""); print $2; exit }
  ' "$WORK_DIR/rendered.yaml")
  [[ -n "$proxy_image" ]] || error "The rendered deployment has no consent-proxy image. Update scripts/render-chart-configs.sh."

  # Move the files into place only after all three are good.
  move_into_place "$stage" "$OUTPUT_DIR" consent.yaml oauth2.conf site.conf
  env_set "$OUTPUT_DIR/.env" PROXY_IMAGE "$proxy_image"
  echo "Wrote consent.yaml, site.conf and oauth2.conf to $OUTPUT_DIR, and set PROXY_IMAGE to $proxy_image"
}

parse_cli_args "$@"
command -v helm > /dev/null || error "Rendering the chart needs helm. See https://helm.sh/docs/intro/install/"
command -v git > /dev/null || error "Rendering the chart needs git."
check_local_values

# config/ holds secrets. Mode 700 keeps other users on this machine out of it. The script sets the
# mode only on config/ and on a folder that it makes, not on an existing folder such as ~.
if [[ ! -d "$OUTPUT_DIR" || "$OUTPUT_DIR" == "$REPO_ROOT/config" ]]; then
  mkdir -p "$OUTPUT_DIR" || error "Could not make $OUTPUT_DIR."
  chmod 700 "$OUTPUT_DIR"
fi

# WORK_DIR holds the clone and the build. STAGE_DIR is in the output folder, so the move into place
# is a rename on the same file system. The trap removes both on each exit, also on Ctrl-C.
WORK_DIR=$(mktemp -d "${TMPDIR:-/tmp}/render-chart-configs.XXXXXX")
STAGE_DIR=$(mktemp -d "$OUTPUT_DIR/.render-chart.XXXXXX")
trap 'rm -rf "$WORK_DIR" "$STAGE_DIR"' EXIT
trap 'exit 130' INT TERM

get_source
build_dependencies
render
