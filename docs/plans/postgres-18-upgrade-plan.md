# Upgrade the Consent database from Postgres 16 to 18 (DT-3587)

[DT-3587](https://broadworkbench.atlassian.net/browse/DT-3587) upgrades the Consent Cloud SQL
instances in dev and staging from PostgreSQL 16 to 18.
[DT-4324](https://broadworkbench.atlassian.net/browse/DT-4324) does the same for prod. This plan
uses the Cloud SQL major version upgrade in place, the same procedure as the
[upgrade from 11 to 16](https://broadworkbench.atlassian.net/wiki/spaces/dt/pages/3319627778).

This document contains no instance names. Each command uses `<instance>` and `<project>`. The
instance name is the `db_name` value in the Terraform variables of the environment. It is also the
`instance_name` value in the `consent-postgres-creds` secret.

## Scope

Consent has no runtime code that depends on the Postgres version. The work is in three repos.

| Repo | Change |
| --- | --- |
| `consent` | The Postgres image for tests and for the local scripts. |
| `terra-helmfile` | The Postgres image for the Consent BEE (the on-demand test environment). |
| `terraform-ap-deployments` | The `db_version` value for each environment. |

The JDBC driver (pgjdbc 42.7.14) and Liquibase (4.33.0) need no change.

## Compatibility review

| Area | Result |
| --- | --- |
| Extensions | The migrations create none. The instances set the `cloudsql.enable_pg_cron` flag to `on`, so `pg_cron` can be installed. Cloud SQL supports `pg_cron` on PostgreSQL 18. |
| Functions and triggers | The trigger functions are plain plpgsql. PostgreSQL 17 restricts `search_path` for maintenance operations. This applies only to functions in expression indexes or materialized views. Consent has neither. |
| Full-text search | PostgreSQL 18 uses the default collation provider of the cluster. Cloud SQL uses libc, so nothing changes. `OntologyDAO` also names the `'english'` configuration. |
| Password hashing | PostgreSQL 18 warns about MD5 passwords. The `consent` role already uses SCRAM-SHA-256. See `consent-role-scram-password-plan.md`. |
| Data directory | The official `postgres:18` image moved its data directory to `/var/lib/postgresql/18/docker`. This affects the local compose file. It does not affect the BEE chart, which sets `PGDATA` to `/data/pgdata`. |

## Not confirmed

- The Cloud SQL documents do not state that 16 can go directly to 18. Pre-check 1 confirms it for
  each instance.
- The BEE upgrade path for a BEE that has Postgres 16 data is not tested. See step 2.

## Step 1: Consent pull request

1. Set `POSTGRES_IMAGE` to `postgres:18.7-alpine` in `DAOTestHelper.java`. The DAO tests, the
   migration tests, and the integration tests all use this value.
2. Set the same tag in `scripts/export-db.sh` and `scripts/templates/docker-compose.yaml`.
3. In the compose file, change the volume target from `/var/lib/postgresql/data` to
   `/var/lib/postgresql`.
4. Update `DEVNOTES.md`. A developer must run `docker volume rm consent_pgdata` one time, because
   the 18 image cannot start on a volume that has Postgres 16 data.
5. Update the image tag in `render-local-configs-plan.md`.
6. Run `mvn clean test`.
7. Run a fresh `docker compose up` with a dev dump. Make sure that the dump loads.

Merge this pull request before the dev upgrade. The `pg_dump` 16 client refuses an 18 server. The
`pg_dump` 18 client can read a 16 server.

## Step 2: BEE pull request (terra-helmfile)

1. In `values/app/consent/bee.yaml.gotmpl`, set `postgres.imageConfig.tag` to `18`.
2. In the same file, set `postgres.upgrade.from.imageConfig.tag` to `16`. The value is `9.6`
   today. The export step of the chart stops if its image version is different from the version
   of the data.
3. Render the consent chart for a BEE. Make sure that the two image tags are correct.
4. Start a new BEE. Make sure that the `consent-postgres` pod starts.

## Step 3: Pre-checks

Do these checks on each environment before its upgrade.

1. Make sure that `POSTGRES_18` is an upgrade target:

   ```bash
   gcloud sql instances describe <instance> --project <project> \
     --format='value(upgradableDatabaseVersions[].name)'
   ```

2. List the extensions in each database, `postgres` included. Expect `plpgsql` and possibly
   `pg_cron`.

   ```sql
   SELECT extname, extversion FROM pg_extension;
   ```

3. List the replication slots. The upgrade deletes logical slots.

   ```sql
   SELECT * FROM pg_replication_slots;
   ```

4. Make sure that the instance has no read replicas. The Terraform defines none.
5. Make sure that no view depends on `pg_stat_activity` or `pg_stat_replication`.
6. Make sure that automated backups are on.

## Step 4: Rehearsal on a clone

1. Clone the instance:

   ```bash
   gcloud sql instances clone <instance> <instance>-pg18-rehearsal --project <project>
   ```

2. Upgrade the clone, and record the time. The upgrade from 11 to 16 took 15 to 17 minutes.

   ```bash
   gcloud sql instances patch <instance>-pg18-rehearsal --project <project> \
     --database-version=POSTGRES_18
   ```

3. Export the clone with the scripts from step 1. Load the dump locally and do the smoke tests
   in step 6.
4. Delete the clone.

## Step 5: Upgrade

Upgrade dev first, then staging.

1. Tell the Slack channels that the Confluence page lists. Include the expected downtime.
2. Create a manual backup with a clear description.
3. Start immediately after the hourly sync from Terra Data Repository (TDR) completes.
4. In `terraform-ap-deployments`, set `db_version` to `POSTGRES_18` in
   `dsp-duos/consent/tfvars/<env>.tfvars`. Open a pull request.
5. Comment `atlantis plan -p consent-<env>`. Autoplan is off for these projects.
6. Read the plan. It must show an update in place. If it shows a replacement, stop.
7. Comment `atlantis apply -p consent-<env>`.

The instance is not available during the upgrade, so Consent API calls fail until it completes.

If you upgrade through the console or `gcloud`, merge the Terraform change immediately after. If
you do not, the next apply shows a version difference.

Do not change `prod.tfvars` for DT-3587.

## Step 6: After each upgrade

1. Make sure that `SELECT version();` shows 18.
2. Run `ANALYZE;`.
3. Make sure that the `/status` endpoint is healthy.
4. Make sure that the next deploy runs the Liquibase migration job without errors.
5. Do the smoke tests: log in, list datasets, open a data access request, and search the ontology.
6. Monitor Query Insights for slower queries for some days.

## Rollback

A downgrade in place is not possible.

1. Restore the backup from before the upgrade into a new Postgres 16 instance.
2. Set `instance_name` in `consent-postgres-creds` to the new instance, then restart Consent.
3. Update the Terraform to agree.

Data that Consent writes after the upgrade is lost. The rehearsal in step 4 decreases this risk.

## Alternative

Database Migration Service can replicate the instance into a new Postgres 18 instance, with almost
no downtime. It needs a new instance, a secret change, and more Terraform work. Use it only if the
rehearsal shows a downtime that is not acceptable.
