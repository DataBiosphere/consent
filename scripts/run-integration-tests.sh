#!/usr/bin/env bash
# =============================================================================
# run-integration-tests.sh
#
# Runs the integration-test suite locally using the same services, config, and
# seed data that the GitHub Actions workflow uses.
#
# USAGE
#   ./scripts/run-integration-tests.sh [OPTIONS]
#
# OPTIONS
#   --skip-build        Skip `mvn clean package`; use an existing target/ jar.
#   --sql-file <path>   Path (relative to repo root) to a SQL seed file.
#                       Defaults to .github/config/seed-ci.sql.
#   --base-url <url>    Override the baseUrl passed to integration tests.
#                       Defaults to http://localhost:8080/
#   -h, --help          Print this message and exit.
#
# REQUIREMENTS
#   docker, mvn, java, psql  must all be on PATH.
# =============================================================================

set -euo pipefail

# ── Defaults ─────────────────────────────────────────────────────────────────
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SKIP_BUILD=false
SQL_FILE="${REPO_ROOT}/.github/config/seed-ci.sql"
BASE_URL="http://localhost:8080/"
APP_LOG="/tmp/consent-app.log"
GCS_STUB="/tmp/ci-gcs-account.json"
APP_PID=""

POSTGRES_CONTAINER="consent-ci-postgres"
ELASTIC_CONTAINER="consent-ci-elastic"
DB_NAME="consent"
DB_USER="consent"
DB_PASS="ci-password"

# ── Argument parsing ──────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-build)   SKIP_BUILD=true; shift ;;
    --sql-file)     SQL_FILE="${REPO_ROOT}/$2"; shift 2 ;;
    --base-url)     BASE_URL="$2"; shift 2 ;;
    -h|--help)
      sed -n '/^# USAGE/,/^# REQUIREMENTS/p' "$0" | sed 's/^# \{0,2\}//'
      exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

# ── Cleanup on exit ───────────────────────────────────────────────────────────
cleanup() {
  echo ""
  echo "── Cleanup ──────────────────────────────────────────────────────────"
  if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" 2>/dev/null; then
    echo "Stopping application (PID $APP_PID)..."
    kill "$APP_PID" || true
  fi
  echo "Stopping containers..."
  docker rm -f "$POSTGRES_CONTAINER" "$ELASTIC_CONTAINER" 2>/dev/null || true
  echo "Done."
}
trap cleanup EXIT

cd "$REPO_ROOT"

echo "══════════════════════════════════════════════════════════════════════"
echo " Consent Integration Tests – local run"
echo "══════════════════════════════════════════════════════════════════════"

# ── 1. Build ──────────────────────────────────────────────────────────────────
if [[ "$SKIP_BUILD" == "true" ]]; then
  echo "── Step 1/7: Build (skipped) ─────────────────────────────────────────"
else
  echo "── Step 1/7: Build ───────────────────────────────────────────────────"
  mvn clean package -Dmaven.test.skip=true --batch-mode --no-transfer-progress
fi

# ── 2. Start PostgreSQL ───────────────────────────────────────────────────────
echo "── Step 2/7: Start PostgreSQL ────────────────────────────────────────"
docker rm -f "$POSTGRES_CONTAINER" 2>/dev/null || true
docker run -d \
  --name "$POSTGRES_CONTAINER" \
  -e POSTGRES_DB="$DB_NAME" \
  -e POSTGRES_USER="$DB_USER" \
  -e POSTGRES_PASSWORD="$DB_PASS" \
  -p 5432:5432 \
  postgres:16-alpine

echo -n "  Waiting for PostgreSQL..."
for i in $(seq 1 30); do
  if docker exec "$POSTGRES_CONTAINER" pg_isready -U "$DB_USER" -d "$DB_NAME" &>/dev/null; then
    echo " ready."
    break
  fi
  echo -n "."
  sleep 2
  if [[ $i -eq 30 ]]; then
    echo " timed out." >&2; exit 1
  fi
done

# ── 3. Start Elasticsearch ────────────────────────────────────────────────────
echo "── Step 3/7: Start Elasticsearch ────────────────────────────────────"
docker rm -f "$ELASTIC_CONTAINER" 2>/dev/null || true
docker run -d \
  --name "$ELASTIC_CONTAINER" \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "cluster.routing.allocation.disk.threshold_enabled=false" \
  -p 9200:9200 \
  elasticsearch:9.3.0

echo -n "  Waiting for Elasticsearch..."
for i in $(seq 1 30); do
  if curl -sf "http://localhost:9200/_cluster/health?wait_for_status=yellow&timeout=5s" &>/dev/null; then
    echo " ready."
    break
  fi
  echo -n "."
  sleep 5
  if [[ $i -eq 30 ]]; then
    echo " timed out." >&2; exit 1
  fi
done

# ── 4. Write stub GCS credentials ─────────────────────────────────────────────
echo "── Step 4/7: Write stub GCS service-account ──────────────────────────"
cat > "$GCS_STUB" <<'EOF'
{
  "type": "service_account",
  "project_id": "ci-project",
  "private_key_id": "ci-key-id",
  "private_key": "",
  "client_email": "ci@ci-project.iam.gserviceaccount.com",
  "client_id": "000000000000000000000",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token"
}
EOF

# ── 5. Start application ──────────────────────────────────────────────────────
echo "── Step 5/7: Start application ───────────────────────────────────────"
echo "  Log: $APP_LOG"
java \
  -classpath "target/classes:$(find target/lib -name '*.jar' | tr '\n' ':')" \
  org.broadinstitute.consent.http.ConsentApplication \
  server .github/config/consent-ci.yaml \
  > "$APP_LOG" 2>&1 &
APP_PID=$!

echo -n "  Waiting for application..."
for i in $(seq 1 36); do
  if curl -sf "http://localhost:8080/status" &>/dev/null; then
    echo " ready (~$((i * 5))s)."
    break
  fi
  echo -n "."
  sleep 5
  if [[ $i -eq 36 ]]; then
    echo " timed out." >&2
    echo "=== application log ===" >&2
    cat "$APP_LOG" >&2
    exit 1
  fi
done

# ── 6. Seed database ──────────────────────────────────────────────────────────
# Runs after the app starts so that Liquibase has already created the full
# schema before any INSERT statements are executed.
echo "── Step 6/7: Seed database ───────────────────────────────────────────"
if [[ -f "$SQL_FILE" ]]; then
  echo "  Using: $SQL_FILE"
  PGPASSWORD="$DB_PASS" psql -h localhost -U "$DB_USER" -d "$DB_NAME" -f "$SQL_FILE"
else
  echo "  No seed file found at '$SQL_FILE'; skipping (Liquibase will init schema)."
fi

# ── 7. Run integration tests ──────────────────────────────────────────────────
echo "── Step 7/7: Run integration tests ──────────────────────────────────"
echo "  baseUrl: $BASE_URL"
mvn test -P integration-tests \
  -DbaseUrl="$BASE_URL" \
  --batch-mode

