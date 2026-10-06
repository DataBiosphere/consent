# Render Local Configs Plan

## Status

Proposed. This plan is for team review. No code exists yet. The team answered the first set of
questions. See [Decisions](#decisions). On 2026-10-01 the plan changed to follow the new `duos-ui`
Dev Container (DT-4206): the Dev Container renders all config itself, `render-configs.sh` does
the checks, and tests are kept to a minimum. A test render of the consent chart ran on 2026-09-24
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

`duos-ui` has four scripts in `scripts/`:

| Script | What it does | Needs |
|---|---|---|
| `render-configs.sh` | Writes `server.crt`, `server.key` and `ca-bundle.crt` from the `local-dev` namespace. Optional flags write `.env.local`, `public/config.json` and `site.conf`. It writes the certs to a staging folder, and moves them into place only after all three succeed. | VPN, `gcloud`, `kubectl`, `jq`, `openssl` |
| `render-site-conf.sh` | Renders `site.conf` from the duos chart template in `terra-helmfile`. It fails if helm syntax remains after the render. | `gh` |
| `setup-devcontainer.sh` | Runs as the Dev Container `postCreateCommand`. If a config file is missing (or always, with `--refresh`), it checks the tools and logins, then runs `render-configs.sh` with every write flag. It always exits 0, and prints the commands that fix a missing login. | `gcloud`, `kubectl`, `gh` in the container |
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
9. The Dev Container installs every tool, and renders all config inside the container.

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
| `scripts/render-configs.sh` | The entry point. It writes certs by default. Flags turn on the other files. It does the tool, login and file checks for all callers. |
| `scripts/render-chart-configs.sh` | Runs `helm template` and writes `consent.yaml`, `site.conf` and `oauth2.conf`. It also runs on its own, without the VPN. |
| `scripts/export-db.sh` | Writes a database dump. It also runs on its own. |
| `scripts/index-es.sh` | Fills the local `dataset` index from the database. It runs after compose starts. |
| `scripts/setup-devcontainer.sh` | Runs `render-configs.sh` when the Dev Container is created. It adds almost no logic of its own. |
| `.devcontainer/Dockerfile` | Adds `gcloud` and the GKE auth plugin to the Dev Container image. |
| `scripts/templates/local-values.yaml` | Helm values for local development. |
| `scripts/templates/docker-compose.yaml` | The compose template. |

All output goes to `config/`. The script creates the directory if it does not exist.

### Usage

```
Usage: scripts/render-configs.sh [OPTION]...
  --project PROJECT                 Google project for the dev cluster, for the certs and dev secrets.
                                    Defaults to broad-dsde-dev. The export never uses it.
  --helmfile_ref REF                The terra-helmfile branch or tag to render. Defaults to master.
  --helmfile_dir DIR                Render a local terra-helmfile checkout as it is, with changes that
                                    are not committed. Cannot be used with --helmfile_ref.
  --write_chart_configs true|false  Write consent.yaml, site.conf and oauth2.conf. Defaults to false.
  --write_compose true|false        Write docker-compose.yaml and .env. Defaults to false.
  --export_db true|false            Write a new database dump. Defaults to false.
  --db_env dev|staging              The environment to export. Defaults to dev. The script refuses prod.
  --fetch_sendgrid true|false       Put the dev SendGrid key in .env. Defaults to false.
  --all true|false                  Write the certs, the chart configs and the compose files. Also
                                    export a dump if config/ has no dump. Defaults to false.
  --only_missing true|false         Skip each step whose files are present and valid. Defaults to false.
  --help                            Show this help.
```

The first-time setup command, outside the Dev Container, is:

```bash
./scripts/render-configs.sh --all true
docker compose -f config/docker-compose.yaml -f config/docker-compose.override.yaml up -d
./scripts/index-es.sh
```

In the Dev Container, `setup-devcontainer.sh` runs the first command. See
[Dev Container](#dev-container).

`--all` never makes a second dump. A dump is large, and the old one stays valid. To make a new
dump, use `--export_db true`.

`index-es.sh` is a separate step because it needs a running stack. `render-configs.sh` runs before
compose starts.

After a cert rotation, run `./scripts/render-configs.sh` with no flags.

To test a `terra-helmfile` change with a local consent instance, render the branch and restart
compose:

```bash
./scripts/render-configs.sh --write_chart_configs true --helmfile_ref my-branch
```

To test changes that are not pushed yet, use `--helmfile_dir ~/develop/terra-helmfile`.

### Robust `render-configs.sh`

`render-configs.sh` does all of the checks, so each caller stays small. The `duos-ui`
`setup-devcontainer.sh` has its own file and login checks. In consent these checks go in
`render-configs.sh`. Then `setup-devcontainer.sh`, a developer on the host and a developer who
renders one file all get the same checks and messages.

| Feature | Behavior |
|---|---|
| Preflight | Before it writes a file, the script checks what the selected steps need: the tools, the active `gcloud` account, `gh auth status` (only to clone `terra-helmfile`), and the dev cluster (one `kubectl` call with `--request-timeout=15s`, only for steps that read the cluster). It prints every problem at once, each with the command that fixes it. Then it stops with exit code 2. |
| Exit codes | 0: each selected step succeeded or was skipped. 1: a step failed. 2: a tool, a login or the VPN is missing, and the developer must act. Callers act on the code, not on the text. |
| Staged writes | Each step writes to a staging folder in `config/`, and moves its files into place only after the step succeeds. A failed step leaves the old files unchanged. A trap removes the staging folder on each exit, also on Ctrl-C. This extends the `duos-ui` cert change to all files. |
| Independent steps | A failed step does not stop the other steps. Without the VPN, the chart configs still render, because they need only `gh` and `helm`. The certs, the `.env` values from the cluster and the export fail. |
| `--only_missing` | Skips a step when its files are present and valid. Valid means not empty, and for `server.crt`, not expiring in 14 days or less (`openssl x509 -checkend`). So a plain run also renews certs that expire soon. For the export, valid means that the dump that `CONSENT_DB_DUMP` names exists. |
| Summary | At the end, one line for each step: written, skipped (present) or failed. Then the next commands, for example `docker compose ... up -d` and `./scripts/index-es.sh`. |

The script reads only the lines that it needs from `config/.env` with `grep`. It never sources
`.env` and never prints a value from it.

### Certs

Copy `write_certs` from `duos-ui` without changes. It reads `tls.crt` and `tls.key` from the
`local-dev-cert` secret, and `ca.crt` from the `kube-root-ca.crt` configmap, in the `local-dev`
namespace of the `terra-dev` cluster.

### Chart Configs (`consent.yaml`, `site.conf`, `oauth2.conf`)

`scripts/render-chart-configs.sh` does these steps:

1. Get the `terra-helmfile` source:
   - With `--helmfile_dir`, use that checkout as it is. The script can run only read-only `git`
     commands in it: `git -C DIR rev-parse HEAD` and `git -C DIR status --porcelain`. It never
     runs a command that changes the checkout, such as `fetch`, `checkout`, `pull` or `stash`.
   - Otherwise, clone `broadinstitute/terra-helmfile` into a temp directory with
     `gh repo clone -- --depth 1 --branch REF`. `REF` is `--helmfile_ref`, or `master`.
   - Print the source and its commit, so the developer can see which config they run. For
     `--helmfile_dir`, add `(uncommitted changes)` when `git status --porcelain` has output.
     If the folder is not a Git checkout, print `commit unknown`, and continue.
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

Before step 1, the script checks `scripts/templates/local-values.yaml` for secrets. It fails if a
key name has `password`, `secret`, `token`, `apikey`, `api_key` or `dsn` in it, in any case. The
check uses `grep -iE` on the key names, so it needs no YAML tool. The error names the file, the
line and the key, and it says that secrets go only in `config/.env`. This check cannot find every
secret, for example a secret under a key with a neutral name. It catches the likely mistakes.

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

The project comes only from this table. `export-db.sh` has no `--project` option, and
`render-configs.sh` does not send its `--project` value to the export. Each `gcloud` and
`cloud-sql-proxy` call gives the project from the table explicitly (`--project`, and the full
instance connection name). So the gcloud default project and `CLOUDSDK_CORE_PROJECT` have no
effect. The script checks `--db_env` against the table before it reads a secret.

The steps:

1. Read the instance name, user and password from the `consent-postgres-creds` secret in Secret
   Manager in the project, as `scripts/db-connect.sh` does.
2. Create a temporary Docker network, for example `consent-export-$$`.
3. Start the proxy in a container on that network, from a pinned image
   (`gcr.io/cloud-sql-connectors/cloud-sql-proxy:2.18.0`). Give it the options as `CSQL_PROXY_*`
   environment variables:
   - `CSQL_PROXY_TOKEN`: from `gcloud auth print-access-token`. The script passes `-e
     CSQL_PROXY_TOKEN` with no value, so Docker copies the value from the script environment.
     The token is not in the command line or the process list.
   - `CSQL_PROXY_ADDRESS=0.0.0.0` and `CSQL_PROXY_PORT=5432`. The default address is `127.0.0.1`,
     which other containers cannot reach. `0.0.0.0` is the address inside the proxy container
     only. The container publishes no ports, so the host and the local network cannot reach it.
   - `CSQL_PROXY_INSTANCE_CONNECTION_NAME`: the full name, with the project from the table.
4. Wait until `pg_isready` from the `postgres:16.14-alpine` image connects to the proxy container
   by its name. Stop after 60 seconds.
5. Run `pg_dump --no-owner --no-privileges` from the `postgres:16.14-alpine` image, the same image
   as the local database, on the same network. Connect to the proxy container by its name, on
   port 5432. Pass the password with `-e PGPASSWORD`, in the same way as the token.
6. Compress the output with `gzip`. The Postgres image loads `.sql.gz` files on its own.
7. Write to a temp file, and move it into place only when `pg_dump` succeeds.
8. Delete the older dumps. See [Data Handling](#data-handling).
9. On exit, also when a step fails, remove the proxy container and the network.

Why run the proxy in a container: a proxy on the host listens on the host's `127.0.0.1`. How a
container reaches that address depends on the runtime:

| Runtime | Container to host `127.0.0.1` |
|---|---|
| Colima | Works. `host.docker.internal` goes to the host loopback. A test with a listener on `127.0.0.1` confirmed this. |
| Docker Desktop | Works through `host.docker.internal`. |
| Linux Docker Engine | Fails. `host-gateway` is the bridge address, and the proxy does not listen there. |
| Podman | Depends on the version and the network mode. |

To bind a host proxy to `0.0.0.0` would open the database to the local network. On a private
Docker network, the two containers find each other by name on each runtime. The developer also
does not need to install `cloud-sql-proxy`.

The access token expires after about one hour. The proxy uses the token only to connect, so a
dump that takes longer than one hour is a risk only if the proxy must connect again. The PR for
ticket 4 records the time of a full dev dump.

Why this format: the local Postgres container loads the dump with `psql` and `ON_ERROR_STOP=1`.
The dumps that developers use today have `Owner: -` on each object, so somebody made them with
`--no-owner`. Why not `gcloud sql export sql`? That command has no `--no-owner` option. Its output
has `OWNER TO` and `GRANT` statements for Cloud SQL roles, such as `cloudsqlsuperuser`, which do
not exist in local Postgres. The load then stops at the first error.

Why run `pg_dump` in the image: a `pg_dump` from a newer major version can write settings that
Postgres 16 does not know (for example `transaction_timeout` from version 17). The image version
always matches the local database.

#### Data Handling

A dev or staging dump can contain real user data, for example the names and email addresses of
people who use dev. The dump keeps this data unchanged. Consent finds users by email, so a local
stack with changed emails does not work for the developer who signs in.

The controls:

| Control | Rule |
|---|---|
| Location | Dumps go only in `config/`, which is not tracked (`/config/` is in `.gitignore`). |
| File mode | Each dump gets mode `600`, the same as the other files that hold secrets. |
| Retention | After a new dump moves into place, the script deletes all older dumps that it made (`consentdb-dev-*.sql.gz` and `consentdb-staging-*.sql.gz`). Only the new dump stays. A failed export deletes nothing. |
| Other dumps | The script never deletes a dump that it did not make, such as an old hand-made `.sql` file. It lists each one, and asks the developer to delete it. |
| Sharing | `DEVNOTES.md` says: do not copy, share or upload a dump. Make a new one with the script. |

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

### Dev Container

This section follows the `duos-ui` Dev Container (DT-4206). The Dev Container renders all config
itself. The developer connects the host to the non-split Broad VPN, opens the container, and logs
in once in the container terminal.

Consent needs more tools than `duos-ui`: `helm` for the chart render and Docker for the export.
The container gets each tool from the image or from a Dev Container feature:

| Tool | Source |
|---|---|
| Java 25, Maven | `ghcr.io/devcontainers/features/java:1` (as now) |
| Docker, Docker Compose | `ghcr.io/devcontainers/features/docker-in-docker:2` (as now). The export runs its proxy and `pg_dump` containers on this daemon. |
| `gcloud`, GKE auth plugin | `.devcontainer/Dockerfile`, from Google's apt repository, as in `duos-ui` |
| `kubectl`, `helm` | `ghcr.io/devcontainers/features/kubectl-helm-minikube:1`, with `minikube` set to `none`. `duos-ui` sets `helm` to `none`. Consent pins `helm` to `4.2.3`, the version of the test render. |
| `gh` | `ghcr.io/devcontainers/features/github-cli:1` |
| `jq`, `openssl`, `gzip`, `curl` | The base image. If the base image does not have one of them, the Dockerfile installs it. |

Changes to `.devcontainer/`:

| Change | Reason |
|---|---|
| Add `Dockerfile`: `FROM mcr.microsoft.com/devcontainers/java:25`, then `gcloud` and the GKE auth plugin | The same method as `duos-ui`. |
| `devcontainer.json`: `"build": {"dockerfile": "Dockerfile"}` in place of `image` | Uses the new image. |
| `devcontainer.json`: add the `github-cli` and `kubectl-helm-minikube` features, and update `devcontainer-lock.json` | The tools in the table. |
| `devcontainer.json`: `"postCreateCommand": "./scripts/setup-devcontainer.sh"` | The same hook as `duos-ui`. |
| `devcontainer.json`: `forwardPorts` 27443, 7777 and 9200 | For the container mode. Docker-in-Docker publishes ports inside the container only. |

`setup-devcontainer.sh` is small, because `render-configs.sh` does the checks:

| Call | Runs |
|---|---|
| `./scripts/setup-devcontainer.sh` | `render-configs.sh --all true --only_missing true` |
| `./scripts/setup-devcontainer.sh --refresh` | `render-configs.sh --all true` |

After the call, the script prints one message for the exit code:

| Exit code | Message |
|---|---|
| 0 | The summary from `render-configs.sh`, then the next commands. |
| 2 | "Run the commands above in this terminal, then run `./scripts/setup-devcontainer.sh` again." |
| Other | "`render-configs.sh` failed. Fix the cause above, then run the script again." |

The script always exits 0, so the container starts when a login is missing. It finds the repo root
from its own path, as the `duos-ui` script does.

On each new container, the developer logs in once in the container terminal:

```sh
gcloud auth login --no-launch-browser
gh auth login
./scripts/setup-devcontainer.sh
```

The `gh` account must be able to read `broadinstitute/terra-helmfile`. The logins stay in the
container, and a rebuild removes them, as in `duos-ui`. `--only_missing` treats a cert that
expires in 14 days or less as missing, so a plain run renews the certs. `--refresh` makes all
files again, except the dump.

The team uses two modes, and both must work:

| Mode | Where `docker compose up` runs | Where `index-es.sh` runs |
|---|---|---|
| Host | In a terminal on the host. The Dev Container renders the files, or the developer runs `render-configs.sh` on the host. | On the host |
| Container | In the Dev Container, with Docker-in-Docker | In the Dev Container |

The workspace bind mount makes the same `config/` files available to both modes. `index-es.sh`
runs where compose runs, because it calls the stack on `localhost`.

The container mode also needs Application Default Credentials (ADC) in the container, because the
compose template mounts the ADC file from `~/.config/gcloud`. The preflight warns, but does not
stop, when `--all` runs in the container and there is no ADC file. The warning gives the command:
`gcloud auth application-default login --no-launch-browser`. A host-mode developer uses the ADC
file on the host and can ignore the warning.

The `duos-ui` combined Dev Container (`.devcontainer/uber`) mounts consent, but it runs only the
`duos-ui` setup, and it sets `helm` to `none`. To render consent config there, that container needs
`helm` and a second `postCreateCommand` step. That is a `duos-ui` change, outside this plan.

### Secrets

| Secret | Today | Plan |
|---|---|---|
| Dev database password | In plain text in `consent.yaml` and `docker-compose.yaml` | Not needed. Local Postgres gets its own random password. |
| SendGrid API key | In plain text in `consent.yaml` | Placeholder by default. Fetch only with `--fetch_sendgrid true`. |
| Sentry DSN | In plain text in `docker-compose.yaml` | Removed. |
| `consent-dev` JSON key | `sqlproxy-service-account.json` | Removed. Use ADC. |

Tracked files hold no secret values. This rule applies to all files under `scripts/templates/`,
which includes `local-values.yaml` and `docker-compose.yaml`. A local secret goes only in
`config/.env`, which is not tracked (`/config/` is in `.gitignore`). The compose template refers
to each secret with a `${VAR}` reference, not a value.

The chart gives no reason to put a secret in `local-values.yaml`. `_consent.yaml.tpl` has fixed
placeholders for each secret (for example `password: foo`), and the `-Ddw.*` flags replace them.
A secret in the values file has no template key to fill.

The script never prints a secret value. All files that hold a secret get mode `600`.

### Tools

| Tool | Used for |
|---|---|
| `gcloud`, `kubectl` | Certs, dev secrets, Secret Manager |
| `helm` (v3.8 or later) | Chart render |
| `gh` | `terra-helmfile` clone at `--helmfile_ref`, if there is no `--helmfile_dir` |
| `cloud-sql-proxy` (v2) | `db-connect.sh` only. The export runs the proxy image. |
| `docker` | `pg_dump` and the local stack |
| `jq`, `openssl`, `gzip` | Secrets and files |
| `curl` | `index-es.sh` |

The preflight checks each tool that the selected steps need, and names any tool that is missing.
The Dev Container installs all of them. See [Dev Container](#dev-container).

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
4. **`consent.yaml` gets the dev values.** Examples: JSON logs, rate limiting on,
   `cacheExpireMinutes: 1` and the Twilio status URL for SendGrid.
5. **The vendored subcharts in a `terra-helmfile` checkout can be stale.** In the test checkout,
   `charts/consent/charts/` had `httpd-proxy-0.55.0`, but the source chart is at `0.57.0`. The
   old `liquibase-migration` package also had no `esolib`, so the render failed. The script must
   build the dependencies itself, in a temp copy.

## Tickets

1. Add `scripts/render-configs.sh` with the cert step and the base for the other steps: the
   preflight, the exit codes, staged writes, the step summary, `--all` and `--only_missing`. Add a
   "Render Configs" section to `DEVNOTES.md`. (`DEVNOTES.md` already links to this section, but
   the section does not exist.) Each later ticket adds one step to this base.
2. Add `scripts/render-chart-configs.sh`, `scripts/templates/local-values.yaml` and the
   `--write_chart_configs`, `--helmfile_ref` and `--helmfile_dir` flags.
3. Add `scripts/templates/docker-compose.yaml`, the `.env` step and the `--write_compose` flag.
4. Add `scripts/export-db.sh` and the `--export_db` and `--db_env` flags. Move `scripts/db-connect.sh` to
   `cloud-sql-proxy` v2 in the same change. `db-connect.sh` runs on the host and uses `psql`, so it
   keeps a host proxy on `127.0.0.1`.
5. Add `scripts/index-es.sh`.
6. Add `.devcontainer/Dockerfile` and `scripts/setup-devcontainer.sh`. In
   `.devcontainer/devcontainer.json`, add the build, the `github-cli` and `kubectl-helm-minikube`
   features, `postCreateCommand` and `forwardPorts`. Update `devcontainer-lock.json`.
7. Change each compose command in `DEVNOTES.md` to name both compose files. Replace the
   "Configure" section of `DEVNOTES.md`. It tells developers to copy
   `src/test/resources/consent-config.yml` by hand. Remove the steps for `wait-for-it.sh`, the
   JSON key, `sqlproxy.env` and `tcell_agent.config`.

Ticket 1 can start now. Tickets 2 to 4 can go in any order after ticket 1. Ticket 3 needs the
output of ticket 2 to test. Ticket 5 needs a running stack from ticket 3 and a dump from ticket 4.
Ticket 6 can start after ticket 1, with the cert step only. It gets each later step with no
change. Ticket 7 goes last. It also adds a Dev Container section to `DEVNOTES.md`, like the one in
`duos-ui`.

## Verification

Tests are kept to a minimum. The rendered files are the reference, so a difference from an old
local copy is not a failure. Each ticket runs the checks below that apply to it:

1. **Render:** each rendered file is the same as the matching configmap key from a direct
   `helm template` run. The only differences come from the
   [list of local changes](#all-local-changes).
2. **Dev Container, first start:** the setup script asks for the logins. After the logins, a second
   run writes all files. `docker compose ... up` starts the stack. `/status` and one authenticated
   `/api` request work through the proxy on port 27443. `index-es.sh` fills the index. Do this one
   time in each mode: container and host.
3. **No VPN:** with the VPN off, the setup script exits 0 and names the VPN. The chart configs
   render. The certs and `.env` stay as they were.
4. **Rerun:** a second run with `--only_missing true` changes nothing. A run with `--refresh` writes
   the certs again and keeps the `.env` values.
5. **Prod:** `--db_env prod` stops before the script reads a secret.

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
| How do we protect user data in a dump? | Handling controls only. No scrubbing, because local development needs the real user emails. The script can delete older dumps. | [Data Handling](#data-handling): mode `600`, only the newest dump stays, no sharing. |
| Where does the team run the local stack? | Both. Some developers run it fully in the Dev Container. Others run compose from a terminal on the host. | Ticket 6 supports both modes. `forwardPorts` is for the container mode. The host mode does not change. |
| Where does the config get rendered? | Inside the Dev Container, as in `duos-ui` (DT-4206). | The Dev Container installs `gcloud`, `kubectl`, `helm` and `gh`. `setup-devcontainer.sh` runs `render-configs.sh`. The host `~/.config/gcloud` mount is not needed. |
| How much testing? | A minimum. | Five checks in [Verification](#verification). |
| Where do the checks go? | In `render-configs.sh`, so that `setup-devcontainer.sh` has less to do. | The preflight, exit codes, staged writes, `--all` and `--only_missing` (ticket 1). |
| How does the local `dataset` index get filled? | A script calls the reindex API with a `gcloud auth print-access-token` token. | `scripts/index-es.sh` (ticket 5). |
| Which `terra-helmfile` commit does the script render? | `master` by default. The team often tests `terra-helmfile` changes with a local consent instance. | `--helmfile_ref` selects a branch. `--helmfile_dir` renders a local checkout with changes that are not committed. |
