#!/bin/bash
#
# Usage: scripts/seed-showcase-fixture.sh
#
# Loads the synthetic Data Showcase fixture (scripts/sql/showcase-fixture.sql) into the LOCAL
# database: the `localdb` container of the local stack. It adds five datasets, DUOS-990001 to
# DUOS-990005, for testing showcase request flows with the local lookup and sign-in (DT-4358). It
# only ever writes to that container, never to a shared environment. Running it again restores the
# fixture's values.
#
# Afterwards, run scripts/index-es.sh: the DUOS dataset page finds datasets through Elasticsearch.
#

set -euo pipefail
unset CDPATH

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
SQL="$SCRIPT_DIR/sql/showcase-fixture.sql"
CONTAINER="localdb"

# DOCKER_HOST, DOCKER_CONTEXT or the active context can point the docker CLI at another machine,
# where a container named localdb could hold a shared database. Each one that is set must name a
# local Unix socket. `docker context inspect` reads DOCKER_CONTEXT and the active context.
CONTEXT_ENDPOINT=$(docker context inspect --format '{{.Endpoints.docker.Host}}' 2> /dev/null || true)
for ENDPOINT in "${DOCKER_HOST:-unix://}" "$CONTEXT_ENDPOINT"; do
  if [[ "$ENDPOINT" != unix://* ]]; then
    echo "ERROR: the docker endpoint is '${ENDPOINT:-unknown}', not a local Unix socket. This script seeds the local database only." >&2
    exit 1
  fi
done

if [[ "$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2> /dev/null)" != "true" ]]; then
  echo "ERROR: the $CONTAINER container is not running. Start the local stack first." >&2
  exit 1
fi

# The container's own POSTGRES_USER and POSTGRES_DB, so no credential is on this command line.
# The SQL refuses to run without duos.showcase_fixture_target=localdb, which only this script sets.
docker exec -i -e PGOPTIONS='-c duos.showcase_fixture_target=localdb' "$CONTAINER" sh -c 'psql -v ON_ERROR_STOP=1 -q -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < "$SQL"

echo "Seeded the showcase fixture: DUOS-990001 to DUOS-990005."
echo "Next: scripts/index-es.sh, so the dataset pages can find the fixture."
