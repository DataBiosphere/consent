#!/usr/bin/env bash
#
# Reports how far the DUOS/Consent infrastructure dependencies lag behind upstream.
#
# Reads the version pins from terra-helmfile, terraform-ap-deployments and Thelma,
# looks up the newest upstream release of each, and prints a Markdown table. It
# only reports; it changes nothing.
#
# USAGE: ./check-infra-deps.sh [--helmfile-dir DIR] [--tf-dir DIR]
#
# Without a directory, the repo is shallow-cloned into a temporary directory with
# your git credentials (both repos are private). Pass a directory to reuse an
# existing checkout; it is read as-is, so pull it first.
#
# Upstream versions come from `git ls-remote` (GitHub repos), the registry tag
# lists (gcr.io images) and https://endoflife.date (end-of-life dates).
#
# To add a check, write a check_* function that calls `finding` once per row and
# add it to CHECKS. Sticks to bash 3.2 and BSD tools so it runs on stock macOS.

set -euo pipefail

HELMFILE_REPO=https://github.com/broadinstitute/terra-helmfile.git
TF_REPO=https://github.com/broadinstitute/terraform-ap-deployments.git
THELMA_TOOL_PINS=https://raw.githubusercontent.com/broadinstitute/thelma/main/scripts/install-runtime-deps.sh

# Terraform roots and local modules that make up the DUOS/Consent infrastructure.
DUOS_TF_DIRS=(
  dsp-duos
  modules/api-services
  modules/cloudsql-postgres
  modules/duos
  modules/gke-service-with-postgres
  terra-monitoring/modules/consent
  terra-monitoring/modules/duos
)

# Charts whose rendered manifests are deployed for DUOS/Consent. Their local
# (file://) subcharts are scanned too.
DUOS_CHARTS=(consent duos)

# API versions that are deprecated or no longer served, one
# "deprecated|replacement|note" per line. Maintained by hand.
DEPRECATED_APIS="
external-secrets.io/v1beta1|external-secrets.io/v1|ESO v0.17 stops serving v1beta1; blocks upgrading the cluster's ESO
"

CHECKS=(
  check_terraform
  check_terraform_modules
  check_cloudsql_postgres
  check_bee_postgres
  check_cloud_sql_proxy
  check_httpd_proxy
  check_external_secrets
  check_deprecated_apis
  check_argocd
  check_thelma_tools
)

TAB=$'\t'
TODAY=$(date +%F)

# Helpers. A lookup that finds nothing fails with a message on stderr, so the
# check shows up as ⚪ instead of silently dropping out of the report. Assign
# lookups to a variable before use: a failure inside "$(...)" passed straight
# as an argument is ignored.

# The numeric part of a version: v1.12.1 -> 1.12.1, 2.1.2-buster -> 2.1.2, POSTGRES_16 -> 16.
core() {
  printf '%s\n' "$1" | grep -oE '[0-9]+(\.[0-9]+)*' | awk 'NR == 1'
}

# Newest `<prefix>X.Y.Z` tag of a GitHub repo, without the prefix. Skips pre-releases.
latest_git_tag() {
  local repo=$1 prefix=${2-v}
  git ls-remote --tags --refs "https://github.com/$repo.git" \
    | awk -v prefix="$prefix" '{
        tag = substr($2, length("refs/tags/") + 1)
        version = substr(tag, length(prefix) + 1)
        if (substr(tag, 1, length(prefix)) == prefix && version ~ /^[0-9]+\.[0-9]+\.[0-9]+$/) print version
      }' \
    | sort -V | tail -1 | grep . \
    || { echo "no ${prefix}X.Y.Z tags in $repo" >&2; return 1; }
}

# Newest X.Y.Z (or vX.Y.Z) tag of a public container image. Usage: latest_image_tag REGISTRY IMAGE
latest_image_tag() {
  curl -fsS "https://$1/v2/$2/tags/list" | jq -r '.tags[]' \
    | grep -E '^v?[0-9]+\.[0-9]+\.[0-9]+$' | sort -V | tail -1 | grep . \
    || { echo "no release tags for $1/$2" >&2; return 1; }
}

eol_cycles() {
  curl -fsS "https://endoflife.date/api/$1.json"
}

# Newest release cycle of a product, e.g. 18 for postgresql.
latest_cycle() {
  eol_cycles "$1" | jq -er '.[0].cycle'
}

# End-of-life date of the release cycle containing a version; empty if none is set.
eol_date() {
  local version
  version=$(core "$2")
  eol_cycles "$1" | jq -r --arg v "$version" '
    first(.[] | .cycle as $c | select($v == $c or ($v | startswith($c + ".")))) | .eol
    | if type == "string" then . else empty end'
}

# First `KEY: value` after the line matching ANCHOR, e.g. the version under `- name: argo-cd`.
# Usage: value_after FILE ANCHOR KEY
value_after() {
  awk -v anchor="$2" -v key="$3:" '
    found && $1 == key { gsub(/"/, "", $2); print $2; done = 1; exit }
    $0 ~ anchor { found = 1 }
    END { if (!done) exit 1 }' "$1" \
    || { echo "no $3 after /$2/ in $1" >&2; return 1; }
}

# Prints `value<TAB>path` for each match of PATTERN (grep -E) in the files named NAME
# (a find -name glob) under the PATHs in ROOT; value is the last word of the match,
# unquoted. Usage: pins ROOT PATTERN NAME PATH...
pins() {
  local root=$1 pattern=$2 name=$3 path matches
  shift 3
  for path in "$@"; do
    [ -e "$root/$path" ] || { echo "$root/$path not found" >&2; return 1; }
  done
  # grep exits non-zero for a batch of files without a match, so judge by the output.
  matches=$(cd "$root" && find "$@" -type f -name "$name" -exec grep -HoE -e "$pattern" {} +) || true
  [ -n "$matches" ] || { echo "no match for '$pattern' under $root" >&2; return 1; }
  printf '%s\n' "$matches" | awk '{
    i = index($0, ":"); match_ = substr($0, i + 1); sub(/"$/, "", match_)
    n = split(match_, words, /[" ]+/)
    print words[n] "\t" substr($0, 1, i - 1)
  }'
}

# Collapses sibling files into one entry, e.g. live/{dev,prod}.yaml.
compact() {
  sort -u | awk '
    {
      file = $0; sub(/.*\//, "", file)
      parent = substr($0, 1, length($0) - length(file))
      stem = file; ext = ""
      if (i = index(file, ".")) { stem = substr(file, 1, i - 1); ext = substr(file, i) }
      key = parent "|" ext
      if (!count[key]++) { order[++n] = key; parents[key] = parent; exts[key] = ext; stems[key] = stem }
      else stems[key] = stems[key] "," stem
    }
    END {
      for (j = 1; j <= n; j++) {
        k = order[j]
        printf "%s%s%s%s", (j > 1 ? ", " : ""), parents[k], (count[k] > 1 ? "{" stems[k] "}" : stems[k]), exts[k]
      }
      print ""
    }'
}

join_lines() {
  paste -sd, - | sed 's/,/, /g'
}

# Reads `value<TAB>item` lines and prints `value<TAB>items` once per distinct value,
# with the items combined by AGGREGATE (default: compact).
group_pins() {
  local aggregate=${1:-compact} pins value
  pins=$(cat)
  [ -n "$pins" ] || { echo "no pins found" >&2; return 1; }
  for value in $(printf '%s\n' "$pins" | cut -f1 | sort -u); do
    printf '%s\t%s\n' "$value" \
      "$(printf '%s\n' "$pins" | awk -F'\t' -v v="$value" '$1 == v { print $2 }' | $aggregate)"
  done
}

# 🔴 past end of life or a major version behind, 🟡 behind, 🟢 current.
# Usage: status CURRENT LATEST EOL
status() {
  local current latest
  current=$(core "$1") || true
  latest=$(core "$2") || true
  if [ -z "$current" ] || [ -z "$latest" ]; then
    echo ⚪
  elif [ -n "$3" ] && [[ ! "$3" > "$TODAY" ]]; then
    echo 🔴
  elif [ "$(printf '%s\n%s\n' "$latest" "$current" | sort -V | tail -1)" = "$current" ]; then
    echo 🟢
  elif [ "${current%%.*}" -lt "${latest%%.*}" ]; then
    echo 🔴
  else
    echo 🟡
  fi
}

# Prints one table row, prefixed with a sort key.
# Usage: finding NAME CURRENT LATEST EOL WHERE [NOTE] [STATUS]; STATUS defaults to `status`.
finding() {
  local icon severity
  icon=${7:-$(status "$2" "$3" "$4")}
  case $icon in
    🔴) severity=0 ;;
    🟡) severity=1 ;;
    ⚪) severity=2 ;;
    *) severity=3 ;;
  esac
  printf '%s\t| %s | %s | %s | %s | %s | %s | %s |\n' "$severity" "$icon" "$1" "$2" "$3" "$4" "$5" "${6-}"
}

# --- terraform-ap-deployments ------------------------------------------------

# Terraform version Atlantis runs for each DUOS/Consent project.
check_terraform() {
  local latest version names eol
  latest=v$(latest_git_tag hashicorp/terraform)
  awk '
    function emit() {
      if (version != "" && (dir ~ /^dsp-duos\// || dir == "terra-monitoring" \
          || workspace ~ /^databiosphere-(consent|duos-ui)$/)) {
        sub(/-(dev|staging|prod|tools)$/, "", name)
        print version "\t" name
      }
    }
    /^  - name: / { emit(); name = $3; dir = workspace = version = "" }
    /^    dir: / { dir = $2 }
    /^    workspace: / { workspace = $2 }
    /^    terraform_version: / { version = $2 }
    END { emit() }' "$TF/atlantis.yaml" \
    | sort -u | group_pins join_lines \
    | while IFS=$TAB read -r version names; do
        eol=$(eol_date terraform "$version")
        finding Terraform "$version" "$latest" "$eol" "atlantis.yaml ($names)"
      done
}

# Remote modules pinned with `?ref=<name>-<version>`.
check_terraform_modules() {
  local source paths repo module ref prefix latest
  pins "$TF" 'source *= *"[^"]*\?ref=[^"]*"' '*.tf' "${DUOS_TF_DIRS[@]}" \
    | group_pins \
    | while IFS=$TAB read -r source paths; do
        source=${source#git::}
        source=${source#https://}
        repo=$(printf '%s\n' "$source" | sed -nE 's#^github\.com/([^/]+/[^/.]+)(\.git)?//.*#\1#p')
        module=${source#*//}
        module=${module%%\?*}
        ref=${source##*\?ref=}
        prefix=$(printf '%s\n' "$ref" | sed -E 's/[0-9]+\.[0-9]+\.[0-9]+$//')
        if [ -z "$repo" ] || [ "$prefix" = "$ref" ]; then
          finding "Module $source" "?" "" "" "$paths" "unrecognized source" ⚪
          continue
        fi
        latest=$prefix$(latest_git_tag "$repo" "$prefix")
        finding "Module $repo//$module" "$ref" "$latest" "" "$paths"
      done
}

check_cloudsql_postgres() {
  local latest version paths eol
  latest=$(latest_cycle postgresql)
  pins "$TF" 'db_version *= *"POSTGRES_[0-9]+"' '*.tfvars' dsp-duos \
    | group_pins \
    | while IFS=$TAB read -r version paths; do
        eol=$(eol_date postgresql "$version")
        finding "Cloud SQL Postgres" "$version" "$latest" "$eol" "$paths" \
          "Cloud SQL can lag upstream; check it offers the latest major"
      done
}

# --- terra-helmfile ----------------------------------------------------------

check_bee_postgres() {
  local path=values/app/consent/bee.yaml.gotmpl major latest eol
  major=$(value_after "$HELMFILE/$path" '^postgres:' tag)
  latest=$(latest_cycle postgresql)
  eol=$(eol_date postgresql "$major")
  finding "BEE Postgres image" "$major" "$latest" "$eol" "$path"
}

check_cloud_sql_proxy() {
  local path=charts/foundation-lib/values.yaml tag latest note="" icon=""
  tag=$(value_after "$HELMFILE/$path" 'repo: .*cloud-sql-proxy' tag)
  latest=$(latest_git_tag GoogleCloudPlatform/cloud-sql-proxy)
  case $tag in
    *buster*)
      note="Debian buster is end of life; -bookworm tags also ship a shell for the sleep (DDO-1352)"
      icon=🔴
      ;;
  esac
  finding "Cloud SQL Proxy" "$tag" "$latest" "" "$path" "$note" "$icon"
}

check_httpd_proxy() {
  local chart paths version latest
  local places=()
  for chart in "${DUOS_CHARTS[@]}"; do
    places+=("charts/$chart/values.yaml" "values/app/$chart")
  done
  latest=$(latest_image_tag us.gcr.io broad-dsp-gcr-public/httpd-terra-proxy)
  pins "$HELMFILE" '^proxyImageVersion: *[^ #]+' '*' "${places[@]}" \
    | group_pins \
    | while IFS=$TAB read -r version paths; do
        finding "httpd-terra-proxy image" "$version" "$latest" "" "$paths"
      done
}

check_argocd() {
  local chart=charts/dsp-argocd/Chart.yaml values=charts/dsp-argocd/values.yaml
  local app app_latest eol chart_version chart_latest
  app=$(value_after "$HELMFILE/$values" '^  image:$' tag)
  app_latest=v$(latest_git_tag argoproj/argo-cd)
  eol=$(eol_date argo-cd "$app")
  chart_version=$(value_after "$HELMFILE/$chart" '- name: argo-cd$' version)
  chart_latest=$(latest_git_tag argoproj/argo-helm argo-cd-)
  finding ArgoCD "$app" "$app_latest" "$eol" "$values"
  finding "ArgoCD Helm chart" "$chart_version" "$chart_latest" "" "$chart"
}

check_external_secrets() {
  local chart=charts/dsp-external-secrets/Chart.yaml version latest
  version=$(value_after "$HELMFILE/$chart" '- name: external-secrets$' version)
  latest=$(latest_git_tag external-secrets/external-secrets helm-chart-)
  finding "External Secrets Operator chart" "$version" "$latest" "" "$chart"
}

check_deprecated_apis() {
  local chart api replacement note found paths
  local charts=("${DUOS_CHARTS[@]}") templates=()
  for chart in "${DUOS_CHARTS[@]}"; do
    charts+=($(sed -nE 's#.*repository: file://\.\./([^ ]+).*#\1#p' "$HELMFILE/charts/$chart/Chart.yaml"))
  done
  for chart in $(printf '%s\n' "${charts[@]}" | sort -u); do
    templates+=("charts/$chart/templates")
  done
  found=$(pins "$HELMFILE" '^apiVersion: *[^ ]+' '*' "${templates[@]}")
  while IFS='|' read -r api replacement note; do
    [ -n "$api" ] || continue
    paths=$(printf '%s\n' "$found" | awk -F'\t' -v api="$api" '$1 == api { print $2 }' | compact)
    [ -z "$paths" ] || finding "Kubernetes API" "$api" "$replacement" "" "$paths" "$note" 🔴
  done <<< "$DEPRECATED_APIS"
}

# --- Thelma (renders and deploys terra-helmfile) -----------------------------

check_thelma_tools() {
  local script variable repo product name pin latest eol
  script=$(curl -fsS "$THELMA_TOOL_PINS")
  while read -r variable repo product name; do
    pin=$(printf '%s\n' "$script" | sed -nE "s/^$variable='?v?([^' ]+)'?.*/\\1/p")
    [ -n "$pin" ] || { echo "no $variable in Thelma's tool pins" >&2; return 1; }
    latest=$(latest_git_tag "$repo")
    eol=""
    [ "$product" = - ] || eol=$(eol_date "$product" "$pin")
    finding "$name" "$pin" "$latest" "$eol" "thelma: scripts/install-runtime-deps.sh"
  done <<'EOF'
HELM_VERSION helm/helm - helm
HELMFILE_VERSION helmfile/helmfile - helmfile
KUBECTL_VERSION kubernetes/kubernetes kubernetes kubectl
ARGOCD_VERSION argoproj/argo-cd argo-cd argocd CLI
EOF
}

# --- main --------------------------------------------------------------------

HELMFILE_DIR=""
TF_DIR=""
while [ $# -gt 0 ]; do
  case $1 in
    --helmfile-dir) HELMFILE_DIR=$2; shift 2 ;;
    --tf-dir) TF_DIR=$2; shift 2 ;;
    -h | --help) sed -nE '3,/^[^#]/ s/^# ?//p' "$0"; exit 0 ;;
    *) echo "Unknown argument: $1 (see --help)" >&2; exit 2 ;;
  esac
done

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

# Prints the absolute path of the checkout, cloning URL unless DIR is given.
# Usage: checkout URL [DIR]
checkout() {
  if [ -n "$2" ]; then
    (cd "$2" && pwd)
    return
  fi
  local dest
  dest="$WORK/$(basename "$1" .git)"
  echo "Cloning $1..." >&2
  git clone --quiet --depth 1 "$1" "$dest" >&2
  echo "$dest"
}

describe() {
  git -C "$1" log -1 --format='%h (%cs)'
}

HELMFILE=$(checkout "$HELMFILE_REPO" "$HELMFILE_DIR")
TF=$(checkout "$TF_REPO" "$TF_DIR")

echo "Looking up upstream versions..." >&2
pids=()
for check in "${CHECKS[@]}"; do
  "$check" > "$WORK/$check.out" 2> "$WORK/$check.err" &
  pids+=($!)
done
i=0
for check in "${CHECKS[@]}"; do
  if ! wait "${pids[$i]}"; then
    finding "$check" "?" "" "" "" "check failed: $(tr '\n|' '  ' < "$WORK/$check.err")" ⚪ \
      >> "$WORK/$check.out"
  fi
  i=$((i + 1))
done

echo "# DUOS/Consent infrastructure dependencies, $TODAY"
echo
echo "terra-helmfile $(describe "$HELMFILE"), terraform-ap-deployments $(describe "$TF")"
echo
echo "🔴 end of life, deprecated, or a major version behind · 🟡 behind · 🟢 current · ⚪ unknown"
echo
echo "| | Dependency | Current | Latest | End of life | Where | Note |"
echo "|---|---|---|---|---|---|---|"
for check in "${CHECKS[@]}"; do
  cat "$WORK/$check.out"
done | sort -s -t "$TAB" -k1,1n | cut -f2-
