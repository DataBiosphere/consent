# Render Local Configs Plan

## Status

Proposed. This plan is for team review. No code exists yet. The team answered the first set of
questions. See [Decisions](#decisions). A test render of the consent chart ran on 2026-09-24
against a local `terra-helmfile` checkout at `ebe889ffb`. See
[Test Render Findings](#test-render-findings).

## Summary

Each developer builds the untracked `config/` directory by hand. The copies drift from each other
and from the deployed dev environment. Some copies hold live secrets that nobody rotates.

This plan adds `scripts/render-configs.sh`, based on the script of the same name in `duos-ui`. The
script writes the files that `config/docker-compose.yaml` needs to start a local stack:

| Output | Source |
|---|---|
| `consent.yaml` | `charts/consent/templates/_consent.yaml.tpl` in `terra-helmfile`, rendered with `helm template` |
| `site.conf`, `oauth2.conf` | `charts/consent/templates/proxy-configmap.yaml` in `terra-helmfile`, rendered with `helm template` |
| Certs | The `local-dev-cert` secret in the dev cluster, same as `duos-ui` |
| `docker-compose.yaml` | A template in this repo |
| Database dump | The dev Cloud SQL instance, through `pg_dump` |

## The duos-ui Model

`duos-ui` has three scripts in `scripts/`:

| Script | What it does | Needs |
|---|---|---|
| `render-configs.sh` | Writes `server.crt`, `server.key` and `ca-bundle.crt` from the `local-dev` namespace. Optional flags write `.env.local`, `public/config.json` and `site.conf`. | VPN, `gcloud`, `kubectl`, `jq`, `openssl` |
| `render-site-conf.sh` | Renders `site.conf` from the duos chart template in `terra-helmfile`. It fails if helm syntax remains after the render. | `gh` |
| `render-accounts.sh` | Writes test service account keys to a gitignored env file. It writes to a temp file first, so a failure keeps the old file. | `gcloud`, `jq` |

The patterns that we copy:

1. The default run writes only the certs, because the certs rotate every 3 months.
2. Flags (`--write_x true|false`) turn on each other file.
3. The script runs the same from the repo root or from `scripts/`.
4. The script keeps values from the old file, and it backs up the old file first.
5. Secret files get mode `600`.
6. Deployed config comes from its real source in `terra-helmfile`. Nobody copies it from a bucket
   or edits it by hand.
7. The script fails with a clear message when a tool, the VPN or an access right is missing.
8. The script fails if template syntax (`{{`) remains in any output.

### The Rendered Files Are Authoritative

The files that `terra-helmfile` renders are the reference for `consent.yaml`, `site.conf` and
`oauth2.conf`. Existing local copies are out of date, and they are not a reference. When a local
copy and a rendered file are different, the rendered file is correct.

The plan permits a local change to a rendered file only when the local stack cannot run without
it. Each local change must:

1. Go in `scripts/templates/local-values.yaml` or in a `-Ddw.*` flag in the compose template. The
   script never edits a rendered file after the render.
2. Have a comment that gives the reason.
3. Be in the [list of local changes](#all-local-changes). A reviewer can then see all of the
   differences from dev in one place.

If the local stack needs a change to the deployed config, the change goes in `terra-helmfile`,
not in this repo.

One difference from duos-ui: the duos `site.conf` comes from one plain template file, so `duos-ui` fetches it
with `gh` and removes the `define` lines. The consent proxy config calls the shared `httpd-proxy`
library chart, and `consent.yaml` uses helm values. So consent must run `helm template` on the
full chart.

## Current `config/` Contents

| File | Used by | Plan |
|---|---|---|
| `server.crt`, `server.key`, `ca-bundle.crt` | proxy | Render from the `local-dev-cert` secret. |
| `consent.yaml` | app | Render from `_consent.yaml.tpl`. |
| `site.conf`, `oauth2.conf` | proxy | Render from `proxy-configmap.yaml`. |
| `docker-compose.yaml` | compose | Copy from a tracked template. |
| `consentdb-*.sql` | local Postgres | Export from the dev Cloud SQL instance. |
| `wait-for-it.sh` | app | Remove. Use a Postgres healthcheck in compose. |
| `sqlproxy-service-account.json` | app (`googleStore`) | Remove. Use Application Default Credentials (ADC), per `DEVNOTES.md`. |
| `sqlproxy.env` | Nothing | Remove. |
| `tcell_agent.config` | Nothing | Remove. |
| `data/` | local Elasticsearch | Replace with a named Docker volume. |
| `README.md` | People | Move the content into `DEVNOTES.md`. |

## Design

### Script Layout

| File | Purpose |
|---|---|
| `scripts/render-configs.sh` | The entry point. It writes certs by default. Flags turn on the other files. |
| `scripts/render-chart-configs.sh` | Runs `helm template` and writes `consent.yaml`, `site.conf` and `oauth2.conf`. It also runs on its own, without the VPN. |
| `scripts/export-db.sh` | Writes a database dump. It also runs on its own. |
| `scripts/index-es.sh` | Fills the local `dataset` index from the database. It runs after compose starts. |
| `scripts/templates/local-values.yaml` | Helm values for local development. |
| `scripts/templates/docker-compose.yaml` | The compose template. |

All output goes to `config/`. The script creates the directory if it does not exist.

### Usage

```
Usage: scripts/render-configs.sh [OPTION]...
  --project PROJECT                 Google project for the dev cluster. Defaults to broad-dsde-dev.
  --helmfile_ref REF                The terra-helmfile branch or tag to render. Defaults to master.
  --helmfile_dir DIR                Render a local terra-helmfile checkout as it is, with changes that
                                    are not committed. Cannot be used with --helmfile_ref.
  --write_chart_configs true|false  Write consent.yaml, site.conf and oauth2.conf. Defaults to false.
  --write_compose true|false        Write docker-compose.yaml and .env. Defaults to false.
  --export_db true|false            Write a new database dump. Defaults to false.
  --db_env dev|staging              The environment to export. Defaults to dev. The script refuses prod.
  --fetch_sendgrid true|false       Put the dev SendGrid key in .env. Defaults to false.
  --help                            Show this help.
```

The first-time setup command is:

```bash
./scripts/render-configs.sh --write_chart_configs true --write_compose true --export_db true
docker compose -f config/docker-compose.yaml -f config/docker-compose.override.yaml up -d
./scripts/index-es.sh
```

`index-es.sh` is a separate step because it needs a running stack. `render-configs.sh` runs before
compose starts.

After a cert rotation, run `./scripts/render-configs.sh` with no flags.

To test a `terra-helmfile` change with a local consent instance, render the branch and restart
compose:

```bash
./scripts/render-configs.sh --write_chart_configs true --helmfile_ref my-branch
```

To test changes that are not pushed yet, use `--helmfile_dir ~/develop/terra-helmfile`.

### Certs

Copy `write_certs` from `duos-ui` without changes. It reads `tls.crt` and `tls.key` from the
`local-dev-cert` secret, and `ca.crt` from the `kube-root-ca.crt` configmap, in the `local-dev`
namespace of the `terra-dev` cluster.

### Chart Configs (`consent.yaml`, `site.conf`, `oauth2.conf`)

`scripts/render-chart-configs.sh` does these steps:

1. Get the `terra-helmfile` source:
   - With `--helmfile_dir`, use that checkout as it is. The script does not run `git` in it.
   - Otherwise, clone `broadinstitute/terra-helmfile` into a temp directory with
     `gh repo clone -- --depth 1 --branch REF`. `REF` is `--helmfile_ref`, or `master`.
   - Print the source and its commit, so the developer can see which config they run.
2. Copy `charts/` into a temp directory. The script never changes the developer's checkout.
3. Build the chart dependencies from the bottom up: first `liquibase-migration` (for `esolib`),
   then `consent`. Use `helm dependency update --skip-refresh`.
4. Render the helmfile values templates. Replace `{{ .Values.Environment.Name }}` with `dev` and
   `{{ .Values.Destination.ConfigBase }}` with `live`. Fail if `{{` remains.
5. Run `helm template` with the values in the same order as helmfile:
   1. `values/app/global.yaml.gotmpl`
   2. `values/app/global/live.yaml.gotmpl`
   3. `values/app/global/live/dev.yaml`
   4. `values/app/consent/live.yaml.gotmpl`
   5. `values/app/consent/live/dev.yaml`
   6. `scripts/templates/local-values.yaml` (from this repo, always last)
6. Use `--show-only templates/configmap.yaml --show-only templates/proxy-configmap.yaml`.
7. Write the `consent.yaml`, `oauth2-config` and `apache-httpd-proxy-config` keys to
   `config/consent.yaml`, `config/oauth2.conf` and `config/site.conf`, with no changes.
8. Fail if any output has `{{`.

The rendered `site.conf` sends traffic to `localhost:8080`. In a pod, the proxy and the app share
one network, so `localhost` is the app. Local compose does the same: the proxy service uses
`network_mode: service:app`. The script then does not need to change `site.conf`.

The local values file has only two values:

```yaml
# The dev Elasticsearch hosts do not exist on a laptop. Use the compose `elastic` service.
elasticSearchServers:
  - elastic
# One local instance. The chart uses this value for rateLimit.podCount.
replicas: 1
```

All other values come from dev. This includes `rateLimit.enabled: true`, the database pool size,
the JSON log layouts and `ServerName localhost`.

### Values that the Template Does Not Control

The template has placeholder values for some keys, for example `database.user: foo`. The deployed
pod replaces them with Dropwizard `-Ddw.*` flags in `deployment.yaml`. Local compose uses the same
method, with values from `config/.env`. These flags are the only other local changes.

| Key | Local flag | Reason |
|---|---|---|
| `database.user` | `-Ddw.database.user=consent` | Placeholder in the template. Dev also replaces it. |
| `database.password` | `-Ddw.database.password=${POSTGRES_PASSWORD}` | Placeholder in the template. Dev also replaces it. |
| `mailConfiguration.sendGridApiKey` | `-Ddw.mailConfiguration.sendGridApiKey=${SENDGRID_API_KEY}` | Placeholder in the template. Dev also replaces it. |
| `googleStore.password` | `-Ddw.googleStore.password=` | Dev mounts a Yale key. Local uses ADC, per `DEVNOTES.md`. A blank value makes `GCSService` use ADC. |
| `elasticSearch.authUser`, `authPassword` | Match `ELASTIC_PASSWORD` in compose | Dev sets these with flags for Elastic Cloud. |

The team runs the app only with compose, so these flags are only in the compose template.
`DEVNOTES.md` does not need a separate list for other run methods.

### All Local Changes

This is the full list of differences between the local stack and dev:

| Where | Change |
|---|---|
| `local-values.yaml` | `elasticSearchServers`, `replicas` |
| `-Ddw.*` flags | Database user and password, SendGrid key, `googleStore.password`, Elasticsearch credentials |
| Compose | No Sentry DSN. No Cloud SQL proxy: the database is a local Postgres container. |

A pull request that adds a row to this table must give the reason in the PR.

### `docker-compose.yaml` and `.env`

The script copies `scripts/templates/docker-compose.yaml` and writes `config/.env` with mode `600`.
Compose reads `.env` on its own. The template follows the deployment in `terra-helmfile` where it
can:

| Item | Value | Reason |
|---|---|---|
| Proxy image version | `proxyImageVersion` from `values/app/consent/live/dev.yaml` | The same proxy as dev. |
| Proxy network | `network_mode: service:app` | The same network as the pod. The rendered `site.conf` works with no change. |
| Proxy ports | `27080:80` and `27443:443`, published on the `app` service | A service with `network_mode: service:app` cannot publish its own ports. |
| Proxy environment | `B2C_APPLICATION_ID` from `.env` | This is the only variable that the deployment gives the proxy. The rendered `site.conf` uses it. |
| App flags | The `-Ddw.*` flags above. No `-Dsentry.dsn`. | Local errors must not go to the shared Sentry project. They make false alerts. |
| ADC | Mount the ADC file and set `GOOGLE_APPLICATION_CREDENTIALS` | Replaces the JSON key. See `DEVNOTES.md`. |
| Start order | A Postgres healthcheck and `depends_on: condition: service_healthy` | Replaces `wait-for-it.sh`. |
| Database dump | `CONSENT_DB_DUMP` from `.env` | The export step sets it. |
| Elasticsearch | The stanza from `DEVNOTES.md`, with a named volume | One copy of the ES setup, with X-Pack security on. |

`.env` holds `POSTGRES_PASSWORD`, `SENDGRID_API_KEY`, `ELASTIC_PASSWORD`, `B2C_APPLICATION_ID` and
`CONSENT_DB_DUMP`. The script keeps each value from an old `.env`.

| Value | New value when there is no old value |
|---|---|
| `POSTGRES_PASSWORD` | Random, from `openssl rand`. Local Postgres does not need the dev password. |
| `SENDGRID_API_KEY` | A placeholder. With `--fetch_sendgrid true`, the key from `consent-secrets` in `terra-dev`. |
| `ELASTIC_PASSWORD` | `devpassword`, per `DEVNOTES.md`. |
| `B2C_APPLICATION_ID` | From the `consent-proxy-b2c-secrets` secret in `terra-dev`. |

A developer who needs other compose changes for their own machine can put them in
`config/docker-compose.override.yaml`. This file must not change the rendered config files.

Compose loads an override file automatically only when the command has no `-f` option. The
commands in this plan and in `DEVNOTES.md` use `-f`, so each command must name both files:

```bash
docker compose -f config/docker-compose.yaml -f config/docker-compose.override.yaml up -d
```

Compose stops with an error if a `-f` file does not exist. So `--write_compose` writes an empty
override file (`services: {}`) when there is no override file. The script never changes an
override file that exists. A test with `docker compose config` confirmed both points: with one
`-f`, Compose ignores the override, and the empty file is valid.

### Database Export

`scripts/export-db.sh` writes `config/consentdb-ENV-YYYY-MM-DD.sql.gz` and sets `CONSENT_DB_DUMP`
in `.env`. `ENV` comes from `--db_env`:

| `--db_env` | Project | Result |
|---|---|---|
| `dev` (default) | `broad-dsde-dev` | Export. |
| `staging` | `broad-dsde-staging` | Export. |
| `prod` | None | The script stops with an error before it reads a secret. There is no flag that overrides this. |
| Any other value | None | The script stops with an error. |

The steps:

1. Read the instance name, user and password from the `consent-postgres-creds` secret in Secret
   Manager in the project, as `scripts/db-connect.sh` does.
2. Start `cloud-sql-proxy` (v2) on port 5433. Port 5432 belongs to the local Postgres container.
3. Run `pg_dump --no-owner --no-privileges` from the `postgres:16.14-alpine` image, the same image
   as the local database. Connect to the proxy at `host.docker.internal:5433`.
4. Compress the output with `gzip`. The Postgres image loads `.sql.gz` files on its own.
5. Stop the proxy on exit, also when a step fails.
6. Write to a temp file, and move it into place only when `pg_dump` succeeds.

Why this format: the local Postgres container loads the dump with `psql` and `ON_ERROR_STOP=1`.
The dumps that developers use today have `Owner: -` on each object, so somebody made them with
`--no-owner`. Why not `gcloud sql export sql`? That command has no `--no-owner` option. Its output
has `OWNER TO` and `GRANT` statements for Cloud SQL roles, such as `cloudsqlsuperuser`, which do
not exist in local Postgres. The load then stops at the first error.

Why run `pg_dump` in the image: a `pg_dump` from a newer major version can write settings that
Postgres 16 does not know (for example `transaction_timeout` from version 17). The image version
always matches the local database.

A staging dump changes only the data. The rendered config stays dev, so the app still uses dev
B2C, Sam and the dev bucket.

### Elasticsearch Index

Consent reads and writes one index, `dataset` (`elasticSearch.datasetIndexName`). The ontology
search does not use Elasticsearch. It uses the `ontology_index` table in Postgres, so the database
dump already has the ontology terms.

The admin endpoint `POST /api/dataset/index` builds the `dataset` index from the database. It sends
all datasets to Elasticsearch with the `_bulk` API. The first `_bulk` call creates the index with
the default dynamic mapping. That mapping adds the `.keyword` subfields that the queries use, for
example `study.studyName.keyword`.

The index must match the database. An old index can return dataset IDs that are not in a new dump.
So fill the index again after each new dump.

```
Usage: scripts/index-es.sh [OPTION]...
  --reset true|false   Delete the local dataset index before the reindex. Defaults to true.
  --help               Show this help.
```

The script does these steps:

1. Get a token with `gcloud auth print-access-token`. The token stays in a shell variable. The
   script sends the header to `curl` on stdin (`-H @-`), so the token is not in the process list
   and the script never prints it.
2. Wait for `/status` on `https://local.dsde-dev.broadinstitute.org:27443`, for up to 5 minutes.
3. If `--reset true`, send `DELETE /dataset` to `localhost:9200`. Use `ELASTIC_PASSWORD` from
   `config/.env`. A 404 is not an error, because a new volume has no index.
4. Send `POST /api/dataset/index` through the proxy with the token. Write the streamed response to
   a temp file.
5. Check the response. The endpoint streams its result, so the HTTP status is 200 even when
   indexing fails. The script fails in each of these conditions:
   - The HTTP status is not 2xx.
   - The body is empty, or it is not valid JSON.
   - The body has `Error indexing datasets`. `indexDatasetIds()` writes this text when it catches
     an `IOException`, for example when Elasticsearch returns a status that is not 200.
   - The `_bulk` response has `"errors": true`. Elasticsearch returns 200 when some documents
     fail, for example because of a mapping conflict. `performRequest()` checks only the status,
     and returns the body unchanged. So a failure of one document passes the other checks.
6. If `errors` is true, show each failed item with `jq`: the dataset ID (`_id`), `status`,
   `error.type` and `error.reason`. Then fail.
7. Show the document count from `GET /dataset/_count`.

See the [Elasticsearch bulk API](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-bulk)
for the response format.

The token must belong to a user who has the Admin role in the loaded database. With a dev dump,
this is the developer's dev DUOS account. If the call returns 401 or 403, the script says which
account `gcloud` used and that this account needs the Admin role.

### Secrets

| Secret | Today | Plan |
|---|---|---|
| Dev database password | In plain text in `consent.yaml` and `docker-compose.yaml` | Not needed. Local Postgres gets its own random password. |
| SendGrid API key | In plain text in `consent.yaml` | Placeholder by default. Fetch only with `--fetch_sendgrid true`. |
| Sentry DSN | In plain text in `docker-compose.yaml` | Removed. |
| `consent-dev` JSON key | `sqlproxy-service-account.json` | Removed. Use ADC. |

The script never prints a secret value. All files that hold a secret get mode `600`.

### Tools

| Tool | Used for |
|---|---|
| `gcloud`, `kubectl` | Certs, dev secrets, Secret Manager |
| `helm` (v3.8 or later) | Chart render |
| `gh` | `terra-helmfile` clone at `--helmfile_ref`, if there is no `--helmfile_dir` |
| `cloud-sql-proxy` (v2) | Database export |
| `docker` | `pg_dump` and the local stack |
| `jq`, `openssl`, `gzip` | Secrets and files |
| `curl` | `index-es.sh` |

The script checks each tool at the start and names any tool that is missing.

## Test Render Findings

A test render with the steps above works. Existing local copies are out of date, so the render is
the reference. These findings show what changes for a developer who moves to the rendered files:

1. **`oauth2.conf` does not change.** The rendered file matches the typical local copy, without
   the comment lines.
2. **`site.conf` gets three items from dev:**
   - `Cache-control: no-store` on all responses.
   - An audience allow list on `/api` (a `RequireAny` block). It checks the token `aud` claim.
   - An `oidc_claim_expires_at` request header (CTM-384).
3. **The proxy needs `B2C_APPLICATION_ID`.** The allow list reads this variable when httpd starts.
   The deployed pod gets it from `consent-proxy-b2c-secrets`. If compose does not set it, the
   proxy rejects valid tokens.
4. **Local `/mcp` routing stops.** The chart has no `/mcp` location. The `/mcp` block exists only
   in local copies, so the rendered `site.conf` does not have it. To use `/mcp` locally, add the
   location to the chart in `terra-helmfile` first.
5. **`consent.yaml` gets the dev values.** Examples: JSON logs, rate limiting on,
   `cacheExpireMinutes: 1` and the Twilio status URL for SendGrid. Keys that only local copies
   have, such as `nih.denyEmailPatterns`, go away.
6. **The vendored subcharts in a `terra-helmfile` checkout can be stale.** In the test checkout,
   `charts/consent/charts/` had `httpd-proxy-0.55.0`, but the source chart is at `0.57.0`. The
   old `liquibase-migration` package also had no `esolib`, so the render failed. The script must
   build the dependencies itself, in a temp copy.

## Tickets

1. Add `scripts/render-configs.sh` with the cert step only. Add a "Render Configs" section to
   `DEVNOTES.md`. (`DEVNOTES.md` already links to this section, but the section does not exist.)
2. Add `scripts/render-chart-configs.sh`, `scripts/templates/local-values.yaml` and the
   `--write_chart_configs`, `--helmfile_ref` and `--helmfile_dir` flags.
3. Add `scripts/templates/docker-compose.yaml`, the `.env` step and the `--write_compose` flag.
4. Add `scripts/export-db.sh` and the `--export_db` and `--db_env` flags. Move `scripts/db-connect.sh` to
   `cloud-sql-proxy` v2 in the same change, so both scripts use one proxy version.
5. Add `scripts/index-es.sh`.
6. Change each compose command in `DEVNOTES.md` to name both compose files. Replace the
   "Configure" section of `DEVNOTES.md`. It tells developers to copy
   `src/test/resources/consent-config.yml` by hand. Remove the steps for `wait-for-it.sh`, the
   JSON key, `sqlproxy.env` and `tcell_agent.config`.

Ticket 1 can start now. Tickets 2 to 4 can go in any order after ticket 1. Ticket 3 needs the
output of ticket 2 to test. Ticket 5 needs a running stack from ticket 3 and a dump from ticket 4.
Ticket 6 goes last.

The `/mcp` proxy location is not part of this plan. If the team wants it, it is a separate
`terra-helmfile` change. The next render then includes it.

## Verification

The rendered files are the reference, so a difference from an old local copy is not a failure.
For each ticket:

1. Run the script with the output set to a temp directory.
2. Make sure that each rendered file is the same as the matching configmap key from a direct
   `helm template` run. The only differences must come from the
   [list of local changes](#all-local-changes).
3. Run `docker compose -f config/docker-compose.yaml -f config/docker-compose.override.yaml up`.
4. Get `/status` and the Swagger page through the proxy on port 27443.
5. Send one authenticated `/api` request with a dev B2C token. This test checks the audience
   allow list and `B2C_APPLICATION_ID`.
6. Make sure that the port-80 redirect and `/oauth2callback` work with the rendered
   `ServerName localhost`. If they do not work, add `proxy.serverName` to the local values and to
   the list of local changes.
7. Run the script a second time. Make sure that it keeps the `.env` values and makes `.bak` files.
8. For ticket 2: render with `--helmfile_ref` set to a test branch, and with `--helmfile_dir` set to
   a checkout that has a change that is not committed. Make sure that each change is in the output
   and that the script prints the source and commit.
9. For ticket 4: start compose with a new dev dump and a fresh container. Make sure that the load
   has no errors and that Liquibase reports no pending changes. Do the same with a staging dump.
10. For ticket 4: run with `--db_env prod`. Make sure that the script stops before it reads a
    secret or starts the proxy.
11. For ticket 5: run the script on a new volume and again on a full index. Make sure that the
    document count is the same as the number of datasets in the database both times.
12. For ticket 5: make one document fail, for example with a local index that has a conflicting
    mapping for one field. Make sure that the script fails and shows the failed dataset IDs.
13. For ticket 3: put a changed value in `config/docker-compose.override.yaml`. Make sure that
    `docker compose ... config` shows it, and that `--write_compose` does not change the file.
14. For ticket 5: compare `GET /dataset/_mapping` on local with the dev index once. Record any
    difference in the PR.

## Alternatives

| Option | For | Against |
|---|---|---|
| `helm template` from `terra-helmfile` (**recommended**) | Uses the authoritative templates. Works without the VPN. Shows a change on `master` at once. | Needs `helm` and a dependency build. The script must copy the helmfile values order. |
| Read the deployed configmaps with `kubectl` | The exact config that dev runs. No `helm`. | Needs the VPN. Shows a change only after ArgoCD syncs dev. |
| Track all config files in this repo | Simple. Works offline. | Drifts from dev, which is the problem that this plan fixes. |
| Change `localhost` to `app` in `site.conf` after the render | No `network_mode` in compose. | The script edits a rendered file. This breaks the rule that the render is the reference. |
| `gcloud sql export sql` for the dump | Runs on the server. No local proxy. | No `--no-owner`. Needs a bucket and export rights. The output needs a filter before it loads. |

## Decisions

The team answered the open questions from the first review:

| Question | Decision | Effect on the plan |
|---|---|---|
| Which environments can the export script use? | Dev by default. Staging with an option. Never prod. | `--db_env dev\|staging`. The script refuses `prod`, and no flag overrides this. |
| Does anyone use `sqlproxy.env` or `tcell_agent.config`? | No. | The script does not write them. Ticket 5 removes them from the docs. |
| Does anyone run the app outside compose? | No. The team runs the app with compose from a terminal. | The `-Ddw.*` flags are only in the compose template. No IntelliJ steps. |
| How does the local `dataset` index get filled? | A script calls the reindex API with a `gcloud auth print-access-token` token. | `scripts/index-es.sh` (ticket 5). |
| Which `terra-helmfile` commit does the script render? | `master` by default. The team often tests `terra-helmfile` changes with a local consent instance. | `--helmfile_ref` selects a branch. `--helmfile_dir` renders a local checkout with changes that are not committed. |
