# Upgrade the Consent database from Postgres 16 to 18 (DT-3587)

[DT-3587](https://broadworkbench.atlassian.net/browse/DT-3587) upgrades the Consent Cloud SQL
instances in dev and staging from PostgreSQL 16 to 18.
[DT-4324](https://broadworkbench.atlassian.net/browse/DT-4324) does the same for prod. This plan
uses the Cloud SQL major version upgrade in place, started from the Google Cloud console. The
Terraform changes after the upgrade.

Dev and staging are on PostgreSQL 18. Prod is not. See "Prod (DT-4324)" for the work that
remains.

This document is the full record of the procedure. It contains no instance names. Each command
uses `<instance>` and `<project>`. The instance name is the `db_name` value in the Terraform
variables of the environment. It is also the `instance_name` value in the `consent-postgres-creds`
secret.

## Scope

Consent has no runtime code that depends on the Postgres version. The work is in four repos.

| Repo | Change | Status |
| --- | --- | --- |
| `consent` | The Postgres image for the local scripts. The image for the tests stays on 16 until DT-4324. | [#3282](https://github.com/DataBiosphere/consent/pull/3282). Tests: [#3297](https://github.com/DataBiosphere/consent/pull/3297), open until the prod upgrade. |
| `terra-helmfile` | The Postgres image for the Consent BEE (the on-demand test environment). | [#6534](https://github.com/broadinstitute/terra-helmfile/pull/6534) |
| `duos-ui` | The Postgres image for the local database. The image for the integration tests stays on 16 until DT-4324. | [#4153](https://github.com/DataBiosphere/duos-ui/pull/4153). Tests: [#4171](https://github.com/DataBiosphere/duos-ui/pull/4171), open until the prod upgrade. |
| `terraform-ap-deployments` | The `db_version` value for each environment. | After each upgrade |

The JDBC driver (pgjdbc 42.7.14) and Liquibase (4.33.0) need no change. The full test suite, which
includes the migration tests, passed on Postgres 18.7 with these versions.

## Services that stop during the upgrade

The instance is not available during the upgrade. Two services use it:

- Consent.
- The DUOS backend. Its Cloud SQL proxy connects to the same instance and database, with the same
  `consent-postgres-creds` secret.

## Compatibility review

The review used the "Migration" lists in the PostgreSQL 17 and 18 release notes. These items are
the ones that can apply to Consent.

| Area | Result |
| --- | --- |
| Extensions | The migrations create none. The instances set the `cloudsql.enable_pg_cron` flag to `on`, so `pg_cron` can be installed. In dev, `pg_cron` 1.6.7 is installed in the `postgres` database. Cloud SQL supports `pg_cron` on PostgreSQL 18. |
| Database flags | The Terraform sets eight flags: `cloudsql.enable_pg_cron` and seven `log_*` flags. The release notes for 17 and 18 remove none of them. |
| Functions in indexes (17) | PostgreSQL 17 uses a safe `search_path` for maintenance operations. This applies to user functions in expression indexes and materialized views. Consent has expression indexes and one stored generated column. They use only built-in functions and operators (`LOWER`, `TRIM`, `COALESCE`, `->`, `to_tsvector`). Consent has no materialized view. |
| Generated columns (18) | In PostgreSQL 18, a generated column is virtual unless it says `STORED`. The one generated column, `ontology_index.search_vector`, says `STORED`. |
| Full-text search (18) | PostgreSQL 18 uses the default collation provider of the cluster. Cloud SQL uses libc, so nothing changes. The generated column and `OntologyDAO` also name the `'english'` configuration. |
| Foreign key error code (18) | A violation of a `RESTRICT` foreign key now reports SQLSTATE `23001`, not `23503`. Consent code reads `23503`, but no changeset makes a `RESTRICT` foreign key. |
| `AFTER` triggers (18) | These triggers now run as the role that queued the event. Consent does not change role in a session. |
| Password hashing (18) | PostgreSQL 18 warns about MD5 passwords. The `consent` role already uses SCRAM-SHA-256. See `consent-role-scram-password-plan.md`. |
| DUOS backend | Its database code needs no change. It uses the `pg` driver 8.21.0 and reads and writes only the `user_sessions` table. Its SQL uses no feature that changes in 17 or 18, and it does not read SQLSTATE codes. The audit triggers on `user_sessions` are in the Consent changesets. |
| Data directory (18) | The official `postgres:18` image moved its data directory to `/var/lib/postgresql/18/docker`. This affects the local compose file. It does not affect the BEE chart, which sets `PGDATA` to `/data/pgdata`. |

## The tests stay on Postgres 16 until the prod upgrade

The Consent tests use the image in `DAOTestHelper.POSTGRES_IMAGE`. This image stays on Postgres 16
until DT-4324 upgrades prod. Thus the tests always run on the version that prod runs. SQL that
uses a feature from version 17 or 18 fails in the tests, and it cannot go to prod.

The cost is that the tests do not run on the version of dev and staging after their upgrade. SQL
that operates differently on 18 can pass the tests and fail in dev. Dev and staging find such a
problem before prod gets the change. The full test suite also passed one time on Postgres 18.7,
on 2026-11-13.

The integration tests of `duos-ui` follow the same rule. Their image in
`.github/workflows/integration-tests.yml` stays on Postgres 16.

Two pull requests set the test images to `postgres:18.7-alpine` for DT-4324:

- `consent`: [#3297](https://github.com/DataBiosphere/consent/pull/3297) changes
  `DAOTestHelper.POSTGRES_IMAGE`.
- `duos-ui`: [#4171](https://github.com/DataBiosphere/duos-ui/pull/4171) changes the image in
  `.github/workflows/integration-tests.yml`.

The CI checks of the two pull requests passed on 18.7 on 2026-11-18. Merge them when prod is on
PostgreSQL 18, and not before.

## Confirmed by test

These tests used Postgres 18.7 in containers or in a BEE. The rehearsal for dev in step 5 used a
Cloud SQL instance. See its results there.

- Consent: the full test suite passed with the test image set to 18.7 (6393 tests, which include
  the migration tests). This was one run. The test image in the repo stays on 16.
- Local Consent database: a dev dump loads into the compose database, and the data stays after
  a container recreate.
- BEE: a BEE that had Postgres 16 data started from the BEE branch. The export, backup, and
  import steps completed. A sign in to DUOS made a new user record in Consent.
- DUOS backend: the session schema applies, and the audit triggers on `user_sessions` fire. 42
  Playwright tests passed in one local run with the servers on Postgres 18. The test image in
  the repo stays on 16.
- DUOS backend with local Consent: the UI with the backend on, and a local Consent stack on
  Postgres 18, operates normally. A new row in `user_sessions` shows the session. The addition
  and removal of user roles work.
- CI on 18.7: the checks of consent #3297 and duos-ui #4171 passed with the test images set to
  18.7. For `duos-ui`, this includes the Playwright tests in CI.
- Dump versions: a `pg_dump` 18 dump of the session schema fails on Postgres 16.13 with
  `unrecognized configuration parameter "transaction_timeout"`. A `pg_dump` 16 dump loads on
  Postgres 18.7.

## Not confirmed

- The Cloud SQL documents do not state that 16 can go directly to 18. Pre-check 1 confirmed it
  for dev, for staging, and for prod.
- Consent did not connect directly to a Cloud SQL instance on Postgres 18. In the rehearsal for
  dev and for staging, Consent used an export of the upgraded clone. In the rehearsal for dev,
  the DUOS backend did connect to the clone.
- `scripts/export-db.sh` was not run against a Postgres 18 server. Step 7 does this. The
  rehearsal for dev used a Cloud SQL export, not this script.
- The BEE test did not compare row counts before and after the upgrade.
- The Playwright tests that need the automation accounts did not run locally on Postgres 18.
- The rollback procedure was not rehearsed.
- The note about the sync from Terra Data Repository (TDR) comes from the upgrade from 11 to 16
  in 2024. Make sure that it is still correct.

## Step 1: Consent pull request

1. Set the tag to `postgres:18.7-alpine` in `scripts/export-db.sh` and
   `scripts/templates/docker-compose.yaml`.
2. In the compose file, change the volume target from `/var/lib/postgresql/data` to
   `/var/lib/postgresql`.
3. Update `DEVNOTES.md`. A developer must run `docker volume rm consent_pgdata` one time, because
   the 18 image cannot start on a volume that has Postgres 16 data.
4. Update the image tag in `render-local-configs-plan.md`.
5. Do not change `POSTGRES_IMAGE` in `DAOTestHelper.java`. See "The tests stay on Postgres 16
   until the prod upgrade".
6. Run `./mvnw clean test`.
7. Start the local compose database with a dev dump. Make sure that the dump loads.

Merge this pull request before the dev upgrade. A `pg_dump` client refuses a server that is newer
than itself, so the 16 client fails after the upgrade. The 18 client can read a 16 server.

## Step 2: BEE pull request (terra-helmfile)

1. In `values/app/consent/bee.yaml.gotmpl`, set `postgres.imageConfig.tag` to `18`.
2. In the same file, set `postgres.upgrade.from.imageConfig.tag` to `16`. The value was `9.6`.
   The export step of the chart stops if its image version is different from the version of the
   data.
3. Render the consent chart for a BEE. Make sure that the export step uses `postgres:16`, and that
   the import step and the main container use `postgres:18`.
4. Start a new BEE. Make sure that the `consent-postgres` pod starts.

## Step 3: duos-ui pull request

The DUOS backend connects to the same database, and `duos-ui` has its own Postgres image pins.

1. Set the image to `postgres:18.7-alpine` for the `db` service in
   `docker-compose.override.yaml`. This service has no data volume, so the data directory change
   does not apply.
2. Update the text in `docs/plans/ai_chatbot_docs/ADR-001-model-backend.md` that names the image.
3. Add the image version to `DEVNOTES.md`.
4. Do not change the image in `.github/workflows/integration-tests.yml`. See "The tests stay on
   Postgres 16 until the prod upgrade".
5. Make sure that the integration tests pass.

Merge this pull request soon after the Consent pull request. The local database of `duos-ui` loads
a Consent dump. After step 1 merges, `scripts/export-db.sh` makes dumps with the `pg_dump` 18
client. Such a dump sets `transaction_timeout`, which Postgres 16 does not know, so the load into
a Postgres 16 container fails.

## Step 4: Pre-checks

Do these checks on each environment before its upgrade. For the SQL checks, connect with
`scripts/db-connect.sh <env>`. The script starts `psql` in the `consent` database. To change to a
different database in the same session, use `\c`:

```
\c postgres
```

1. Make sure that `POSTGRES_18` is an upgrade target:

   ```bash
   gcloud sql instances describe <instance> --project <project> \
     --format='value(upgradableDatabaseVersions[].name)'
   ```

2. Make sure that automated backups are on and that the instance has no read replicas. The
   Terraform for this instance sets no backup configuration and defines no replica.

   ```bash
   gcloud sql instances describe <instance> --project <project> \
     --format='yaml(settings.backupConfiguration.enabled,replicaNames)'
   ```

3. List the extensions in the `consent` database and in the `postgres` database. Expect `plpgsql`
   in each of them. `pg_cron` installs in one database only, which the `cron.database_name` flag
   names. The default is `postgres`, and the Terraform does not set this flag. Thus expect
   `pg_cron` in `postgres` and not in `consent`.

   ```sql
   SELECT extname, extversion FROM pg_extension;
   ```

4. If `pg_cron` is installed, record its jobs. Compare them after the upgrade. Run this query in
   the database that has the extension.

   ```sql
   SELECT jobid, schedule, command, database, username, active FROM cron.job;
   ```

   The `cron.job` table has row-level security, so a role sees only the jobs that it owns. No
   rows does not prove that no jobs exist. The sequence for the job IDs has no such limit:

   ```sql
   SELECT last_value, is_called FROM cron.jobid_seq;
   ```

   If `is_called` is `f`, no job was ever made on the instance. If it is `t`, run the first
   query again as the role that owns the jobs.

5. List the replication slots. The upgrade deletes logical slots. Expect no rows.

   ```sql
   SELECT slot_name, slot_type FROM pg_replication_slots;
   ```

6. Make sure that no view depends on `pg_stat_activity` or `pg_stat_replication`. The query reads
   only the current database, so run it in `consent` and in `postgres`. Expect no rows.

   ```sql
   SELECT schemaname, viewname FROM pg_views
   WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
     AND definition ~ 'pg_stat_(activity|replication)';
   ```

7. Make sure that the `postgres` and `template1` databases use the `en_US.UTF8` collation. The
   upgrade needs this.

   ```sql
   SELECT datname, datcollate FROM pg_database;
   ```

8. Make sure that the instance has no operation in progress. Expect an empty list.

   ```bash
   gcloud sql operations list --instance <instance> --project <project> \
     --filter='status!=DONE'
   ```

### Results

| Check | Dev, 2026-11-16 | Staging, 2026-11-18 | Prod, 2026-11-22 |
| --- | --- | --- | --- |
| 1. Upgrade target | Pass. The targets are `POSTGRES_17` and `POSTGRES_18`. | Pass. The same targets. | Pass. The same targets. |
| 2. Backups and replicas | Pass. Backups are on. The instance has no replicas. | Pass. The same result. | Pass. The same result. |
| 3. Extensions | Pass. `plpgsql` 1.0 in `consent`. `plpgsql` 1.0 and `pg_cron` 1.6.7 in `postgres`. | Pass. The same versions. | Pass. The same versions. |
| 4. `pg_cron` jobs | Pass. No job was ever made (`is_called` is `f`). | Pass. No job was ever made. | Pass. No job was ever made. |
| 5. Replication slots | Pass. No slots. | Pass. No slots. | Pass. No slots. |
| 6. Views on `pg_stat_*` | Pass. No rows in `consent` or in `postgres`. | Pass. No rows in the two databases. | Pass. No rows in the two databases. |
| 7. Collation | Pass. All databases use `en_US.UTF8`. | Pass. The same result. | Pass. The same result. |
| 8. Operations | Pass. No operation in progress. | Pass. | Pass. Do this check again immediately before the upgrade. |

The dev server, the staging server, and the prod server were on PostgreSQL 16.15.

The changesets for `user_sessions` describe an hourly `pg_cron` job and a weekly `pg_cron` job.
Dev, staging, and prod have neither job. This does not change the upgrade. It is a different problem.

## Step 5: Rehearsal on a clone

Do the rehearsal for each environment. It gives the expected downtime. The upgrade from 11 to 16
took 15 to 17 minutes.

1. Clone the instance:

   ```bash
   gcloud sql instances clone <instance> <instance>-pg18-rehearsal --project <project>
   ```

2. Upgrade the clone, and record the time:

   ```bash
   gcloud sql instances patch <instance>-pg18-rehearsal --project <project> \
     --database-version=POSTGRES_18
   ```

3. Connect to the clone. `scripts/db-connect.sh` and `scripts/export-db.sh` read the instance name
   from the secret, so they cannot connect to a clone. Start the proxy directly. The clone has the
   same users and passwords as the instance.

   ```bash
   cloud-sql-proxy --port 5433 <project>:us-central1:<instance>-pg18-rehearsal
   ```

   ```bash
   psql -h 127.0.0.1 -p 5433 -U <user> -d consent
   ```

4. In `psql`, do the SQL checks of step 7, and run `ANALYZE;`. The checks of step 7 that use a
   service do not apply, because no service connects to the clone.
5. Optional: do the tests with the services that "Tests with the services" describes.
6. Delete the clone:

   ```bash
   gcloud sql instances delete <instance>-pg18-rehearsal --project <project>
   ```

### Results

| Check | Dev, 2026-11-16 | Staging, 2026-11-18 | Prod, 2026-11-22 |
| --- | --- | --- | --- |
| Clone | Completed in 4 minutes 31 seconds. | Completed. The time is not recorded. | Completed in 4 minutes 20 seconds. |
| Upgrade of the clone | Completed in 13 minutes 24 seconds. | Completed in 13 minutes 44 seconds. | Completed in 13 minutes 32 seconds. |
| Version | PostgreSQL 18.6. | PostgreSQL 18.6. | PostgreSQL 18.6. |
| `ANALYZE` | Completed. It gave the 11 warnings that step 7 describes. | Completed, in Cloud SQL Studio, which shows no warnings. | Completed. |
| Extensions in `postgres` | `plpgsql` 1.0 and `pg_cron` 1.6.7, the same as before the upgrade. | The same as dev, and the same as before the upgrade. | The same as dev, and the same as before the upgrade. |

### Tests with the services

These tests are optional. They show that the services operate on a database that the Cloud SQL
upgrade made. Do them before you delete the clone. Test 2 needs the clone.

| Test | Database | Dev, 2026-11-17 | Staging, 2026-11-18 |
| --- | --- | --- | --- |
| 1. Local Consent | An export of the upgraded clone, in the local compose database. | Pass. Sign in works, and `GET /api/user/me` gives the user. The Consent log has no errors. | Pass. Sign in works, and `GET /api/user/me` gives the user. |
| 2. Local DUOS backend | The upgraded clone, through the Cloud SQL proxy. | Pass. Sign in works, and the UI shows an active session. | Not done |
| 3. Local DUOS backend with local Consent | The same export as test 1. | Pass. Sign in works, and the local database shows the sign in. | Not done |

For test 1, `scripts/export-db.sh` cannot connect to a clone, so make the file by one of these
methods. Load it as `DEVNOTES.md` describes.

- Export the `consent` database from Cloud SQL into a bucket, and download the file. Remove the
  lines at the end of the file that give rights to the Cloud SQL superuser, because the local
  database does not have that role. The test for dev used this method.
- Make a dump through the proxy with the `pg_dump` 18 client and the
  `--no-owner --no-privileges` options.

The file has real user data. Do not copy, share, or upload it.

For test 2, use the `docker-compose.cloudsql.yaml` mode of `duos-ui`. Set `CLOUDSQL_INSTANCE` in
`.env.local` to the connection name of the clone. The users and passwords are the same as on the
instance. Set the value back after the test. The sessions from this test go into the clone only.

For test 3, use the `docker-compose.consent.yaml` mode of `duos-ui` with the Consent stack of
test 1.

The upgrade time is the full time of the update operation. It includes the backup before the
upgrade (41 seconds) and the backup after it (61 seconds). For each upgrade, tell the Slack
channels to expect a downtime of approximately 15 minutes.

Cloud SQL gave version 18.6. The local image is 18.7. A `pg_dump` 18.7 client can read an 18.6
server, because the major version is the same.

## Step 6: Upgrade

Upgrade dev first. Upgrade staging after dev operates on Postgres 18 with no problem. Staging gets
the newest dev version each night, so do its upgrade during the work day.

### Before the upgrade

1. Tell these Slack channels. Give the environment, the start time, the expected downtime from the
   rehearsal, and the two services that stop.
   - `#dsp-data-team`
   - `#workbench-oncall`
   - `#workbench-release`
   - `#classic-terra-collaboration`
   - `#terra-devops-champions`
2. Make sure that no Consent or DUOS deploy starts during the upgrade. A deploy runs the Liquibase
   migration job, and the job fails when the database is not available. For dev, do not merge to
   `develop` during the upgrade.
3. Start immediately after the hourly sync from TDR completes. This was a lesson from the upgrade
   from 11 to 16.

### In the Google Cloud console

1. Open the instance.
2. Open **Backups**, and create a backup with a clear description.
3. Go back to **Overview**, and select **Edit**.
4. Open the upgrade page, and select PostgreSQL 18.
5. Start the upgrade.
6. Wait until the operation completes.

Cloud SQL also makes a backup immediately before the upgrade and a second backup after it.

### Terraform, after the upgrade

Do this on the same day. Until it merges, a Terraform plan for the environment shows a version
difference.

1. In `terraform-ap-deployments`, set `db_version` to `POSTGRES_18` in
   `dsp-duos/consent/tfvars/<env>.tfvars`. Open a pull request.
2. Comment `atlantis plan -p consent-<env>`. Autoplan is off for these projects.
3. Read the plan. It must show no change to the instance. If it shows a replacement or a version
   change, stop and find the cause.
4. Apply and merge.

Do not change `prod.tfvars` for DT-3587. Change it for DT-4324, after the prod upgrade.

## Step 7: After each upgrade

1. Make sure that the version is 18:

   ```sql
   SELECT version();
   ```

2. Run `ANALYZE;` in the `consent` database. The `consent` role is not a superuser, so
   PostgreSQL gives a warning for each shared system catalog, for example
   `permission denied to analyze "pg_authid", skipping it`. The rehearsal for dev gave 11 such
   warnings. They are not a problem. A warning for a table in the `consent` schema is a problem.
3. If `pg_cron` is installed, make sure that the `postgres` database still lists it, and compare
   `cron.job` with the record from pre-check 4.
4. Make sure that the Consent `/status` endpoint is healthy.
5. Make sure that the next deploy runs the Liquibase migration job without errors.
6. Do the smoke tests in DUOS: log in, list datasets, open a data access request, and search the
   ontology. The log in also tests the connection of the DUOS backend.
7. Run `./scripts/export-db.sh <env>`, and make sure that the dump is complete.
8. Tell the Slack channels that the upgrade is complete.
9. Monitor Query Insights for slower queries for some days.

## Prod (DT-4324)

Steps 1, 2, and 3 are complete, and prod does not need them again. Do steps 4 to 7 for prod, with
these differences.

| Step | Difference for prod |
| --- | --- |
| 4. Pre-checks | None. The results table has the results. |
| 5. Rehearsal | Do it on a clone of the prod instance. The results table has the results. The upgrade of the clone took 13 minutes 32 seconds, so expect a downtime of approximately 15 minutes. |
| 5. Tests with the services | The file from a prod clone has real user data from prod. The rule at the end of that section applies. |
| 6. Before the upgrade | Also tell the DUOS users about the downtime. Make sure that no release goes to prod during the upgrade. |
| 6. Terraform | Set `db_version` in `prod.tfvars`, and use the Atlantis project for prod. |
| 7. After the upgrade | Item 7 does not apply. `scripts/export-db.sh` does not export prod. |
| Rollback | The same procedure. It was not rehearsed, and data that the services write after the upgrade is lost. |

After prod is on PostgreSQL 18:

1. Merge [consent #3297](https://github.com/DataBiosphere/consent/pull/3297) and
   [duos-ui #4171](https://github.com/DataBiosphere/duos-ui/pull/4171).
2. Update the section "The tests stay on Postgres 16 until the prod upgrade" in this document.

## Rollback

A downgrade in place is not possible.

1. Restore the backup from before the upgrade into a new Postgres 16 instance.
2. Set `instance_name` in `consent-postgres-creds` to the new instance. Consent and the DUOS
   backend both read this secret. Restart the two services.
3. Make Terraform agree with the restored instance. Do these steps in `terraform-ap-deployments`,
   in `dsp-duos/consent`, with the workspace of the environment:
   1. In `<env>.tfvars`, set `db_name` to the new instance.
   2. In `<env>.tfvars`, make sure that `db_version` is `POSTGRES_16`. If the change from step 6
      is merged, set the value back.
   3. Remove the upgraded instance from the state. Do not plan or apply before this step: a new
      `db_name` with the old state gives a replacement, which deletes the upgraded instance. The
      address can also hold only one instance, so the import fails without this step:

      ```bash
      terraform state rm google_sql_database_instance.consent-db
      ```

   4. Import the restored instance:

      ```bash
      terraform import -var-file=tfvars/<env>.tfvars google_sql_database_instance.consent-db <project>/<new instance>
      ```

   5. Comment `atlantis plan -p consent-<env>`. The plan must show no change to the instance. If
      it shows a version change, do not apply: an apply upgrades the restored instance again.
4. Terraform does not manage the upgraded instance after step 3. Delete it in the console when you
   are sure that you do not need it.

Data that the services write after the upgrade is lost. The rehearsal in step 5 decreases this
risk.

## Alternative

Database Migration Service can replicate the instance into a new Postgres 18 instance, with almost
no downtime. It needs a new instance, a secret change, and more Terraform work. Use it only if the
rehearsal shows a downtime that is not acceptable.
