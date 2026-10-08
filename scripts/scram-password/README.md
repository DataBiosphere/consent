# Set the consent database role to a SCRAM password

Tickets: [DT-4237](https://broadworkbench.atlassian.net/browse/DT-4237) (dev and these scripts),
[DT-4244](https://broadworkbench.atlassian.net/browse/DT-4244) (staging),
[DT-4245](https://broadworkbench.atlassian.net/browse/DT-4245) (prod)

## Why

The DUOS server runs Node in FIPS mode. FIPS mode does not allow MD5.
The `consent` role in the Cloud SQL instances stores its password as an MD5 hash.
The databases began on Postgres 9 and moved in place to 11 and then to 16.
An in-place upgrade keeps old MD5 hashes. Postgres 16 itself defaults to SCRAM.

An MD5 role makes the server ask for an MD5 login. The `pg` library then fails with:

```text
Unrecognized algorithm name
Connection terminated unexpectedly
```

Nothing shows this failure while the BFF is off, because no request queries the database.
When the BFF is on, every login fails.

The fix is to store a SCRAM-SHA-256 hash of the **same** password. The password does not change.
You do not rotate the secret. Consent uses the `pgjdbc` driver, which supports SCRAM.
The Liquibase job runs from the consent image with the same driver.

The Terraform for the consent database does not manage this role or its password.
A later `terraform apply` does not undo the change.

## Files

| File | Purpose |
|---|---|
| `apply-scram.sh` | Runs the change. It checks the result and rolls back on failure. |
| `scram-hash` | Reads a password on stdin. Prints its SCRAM verifier. Refuses any output that does not have the exact shape of a verifier (a 16-byte salt and two 32-byte keys in canonical Base64). |
| `check-logs.sh` | Searches the Cloud SQL logs for the password and for hash text. Prints counts only. |
| `lib.sh` | Shared check of the credentials JSON. A null, missing or empty password stops the scripts. |
| `md5-verifier.py` | Prints the MD5 verifier of a role from its password on stdin. Used for the rollback. |
| `probe-auth.py` | Asks the server which login method it wants. It sends no password. |
| `node-connect-test.js` | Tests a login with the same `pg` library and FIPS Node as DUOS. |

## What you need

- `gcloud` with access to the project, and `gcloud auth application-default login` done
- the Cloud SQL Client role on the project
- `jq`, `python3`, `docker`, and `psql` version 10 or later
- to build `pg-scram` (next section): the GitHub CLI `gh` with a login (`gh auth login`), `base64`, `shasum`,
  a C compiler (`cc`), and the libpq headers and library, with `pg_config` on the `PATH`
  (for example Postgres.app on macOS, or the `libpq-dev` package on Debian)
- for steps that use `kubectl`: the VPN
- the `pg-scram` binary (next section). `scram-hash` finds it in `scripts/scram-password`, or in `PG_SCRAM_BIN`, or on the `PATH`.

## Build pg-scram

[`fboulnois/pg-scram`](https://github.com/fboulnois/pg-scram) is a program of about 30 lines.
It calls libpq `PQencryptPasswordConn` to make the SCRAM verifier. It needs no database.
The project has not changed since 2022. Build it from the pinned commit.
Run the commands from the repository root. They write `main.c` and `pg-scram` into
`scripts/scram-password`, where the `.gitignore` of that directory ignores them. Do not commit them.

```shell
COMMIT=68a8861af6112588db2190bd625394c92e1585ec
gh api "repos/fboulnois/pg-scram/contents/main.c?ref=$COMMIT" --jq .content | base64 --decode > scripts/scram-password/main.c
shasum -a 256 scripts/scram-password/main.c   # must print d98bde7c1d80bb0df159dd278f46211423db8e16f432ed317191b50fe80de08d
cc -I"$(pg_config --includedir)" scripts/scram-password/main.c -o scripts/scram-password/pg-scram -L"$(pg_config --libdir)" -lpq
```

On macOS with Postgres.app, add `/Applications/Postgres.app/Contents/Versions/latest/bin` to the `PATH` first.

If libpq fails, `pg-scram` prints `(null)` and exits 0. Postgres would then store `(null)` as the password.
Always use `scram-hash`. It checks that the output is a real verifier.

## Environments

Do the procedure for dev, then staging, then prod. This README uses these placeholders:

| Placeholder | Meaning |
|---|---|
| `<project>` | The GCP project of the environment |
| `<instance>` | The Cloud SQL instance of the consent database |
| `<namespace>` | The Kubernetes namespace of the environment |
| `<consent deployment>` | The Kubernetes deployment that runs consent |
| `<duos deployment>`, `<duos container>` | The deployment and the container that run the DUOS server |

The connection name is `<project>:us-central1:<instance>`.
The secret `consent-postgres-creds` in each project holds the user, the password, and the instance name.
Read the instance name from the secret. This README does not list instance names.

```shell
gcloud --project <project> secrets versions access latest --secret=consent-postgres-creds | jq -r .instance_name
```

The secret has no `db` field. The database name is `consent`.

## Procedure

Do these steps for one environment at a time. Do dev first.

1. **Find old clients.** An old client (libpq below 10, or `pgjdbc` below 42.2) cannot do SCRAM.
   List the clients that logged in as `consent` in the last 30 days:

   ```shell
   (
     set -euo pipefail
     LOGINS=$(gcloud logging read 'resource.type="cloudsql_database"
       AND resource.labels.database_id="<project>:<instance>"
       AND logName="projects/<project>/logs/cloudsql.googleapis.com%2Fpostgres.log"
       AND textPayload:"connection authorized: user=consent "' \
       --project <project> --freshness=30d --format='value(textPayload)')
     [ -n "$LOGINS" ] || { echo "no logins found: the log read failed"; exit 1; }
     printf '%s\n' "$LOGINS" | sed -E 's/^.*connection authorized: //; s/ SSL.*$//' | sort | uniq -c | sort -rn
   )
   ```

   The block stops with an error if the read fails, so a failed read cannot look like an empty list of clients.
   The consent role logs in all the time, so an empty result means that something is wrong.
   The command has no `--limit`, so it reads every login in the window. Do not add one: a cap can hide an
   old client that then breaks after the change.
   The Cloud SQL logs show `host=[local]` for every connection, so you cannot map a client to a pod.
   Most logins have no `application_name`. Look for a named tool that you do not expect.

2. **Prod only: take an on-demand backup.**

   ```shell
   gcloud sql backups create --instance=<instance> --project=<project>
   ```

3. **Start a Cloud SQL Auth Proxy on your laptop.** It binds to loopback only.

   ```shell
   docker run -d --name scram-proxy -p 127.0.0.1:5434:5432 \
     -e GOOGLE_APPLICATION_CREDENTIALS=/secrets/adc.json \
     -v "$HOME/.config/gcloud/application_default_credentials.json:/secrets/adc.json:ro" \
     gcr.io/cloud-sql-connectors/cloud-sql-proxy:2.14.0-alpine \
     --address 0.0.0.0 --port 5432 <project>:us-central1:<instance>
   ```

4. **Probe.** The result must be MD5. If it is SCRAM, stop. The role is already fixed.

   ```shell
   python3 scripts/scram-password/probe-auth.py 127.0.0.1 5434 consent consent
   # authType 5: MD5
   ```

5. **Run the change.** The script reads the password from Secret Manager.
   The password stays in shell variables and pipes.

   ```shell
   bash scripts/scram-password/apply-scram.sh <project> 5434
   ```

   The script does these things:

   1. It stops if the probe is not MD5.
   2. It opens one `psql` session and keeps it open.
   3. It sets `ALTER ROLE consent PASSWORD '<SCRAM verifier>'`. The server sees only the hash.
      If the script does not see the confirmation in time, it prints a warning and goes on to step 4.
      The change may still apply. After this point, the script does not stop before step 4. A signal is the
      one exception (see item 6).
   4. It probes again (must be 10) and makes a new login with the same password.
      Both steps have a time limit (the probe 10 s, the login 15 s). A stalled step counts as a failure.
   5. If either check fails, it uses the open session to put the MD5 verifier back.
      It queues a marker behind the rollback on the same session and waits for it, so that the confirmation comes
      after both `ALTER` commands ran. A first `ALTER` that is still blocked cannot make the probe look clean.
      Then it confirms with the probe (must be 5). It exits with code 2.
   6. If the script stops after step 3 and before step 4 ends (Ctrl+C, a TERM or HUP signal, or an error),
      it also puts the MD5 verifier back before it exits. Bash handles a signal when the current step ends,
      so a signal can wait for the time limit of that step.

   Exit codes: 0 success, 1 stopped before any change, 2 verification failed and rollback ran,
   129, 130 or 143 interrupted (HUP, INT or TERM). After an interrupt, read the output and probe the role.

6. **Test the DUOS pod.** The result must be `OK {"current_user":"consent"}`.

   ```shell
   kubectl -n <namespace> exec -i deploy/<duos deployment> -c <duos container> -- sh -c \
     'node - "$(ls -d /usr/src/app/server/node_modules/.pnpm/pg@*/node_modules/pg | head -1)"' \
     < scripts/scram-password/node-connect-test.js
   ```

7. **Restart consent.** Open connections stay valid. A restart makes every connection a new one.

   ```shell
   kubectl -n <namespace> rollout restart deployment/<consent deployment>
   kubectl -n <namespace> rollout status deployment/<consent deployment> --timeout=540s
   curl -s https://consent.dsde-<env>.broadinstitute.org/status | jq -r '.systems | to_entries[] | "\(.key): \(.value.healthy)"'
   ```

   `postgresql` must be healthy. The `degraded` flag can come from `sendgrid`, which does not use the database.
   Check that the new pods log no `password authentication`, `SCRAM`, `FATAL` or `SQLException` lines.
   The Liquibase job runs at the next consent deploy. It uses the same driver.

8. **Check the Cloud SQL logs for the password.** The script searches for the real value and prints only counts.

   ```shell
   scripts/scram-password/check-logs.sh <project> <instance>
   ```

   The script decodes every JSON string before it matches, so a password with a quote or a backslash cannot
   hide in JSON escapes. It works in a private temporary directory and touches no file in your current directory.
   It checks for the plaintext password, any `SCRAM-SHA-256$` verifier, any `md5` verifier, and `ALTER ROLE` text.
   Every count must be 0. The script stops with an error if a read fails, so a failed read cannot look like
   a clean result. By default it reads the last 3 hours. Pass a third argument (for example `6h`) to change that.

   The change is not logged, because the instances set no `log_statement`.
   If a statement fails, `log_min_error_statement = error` logs its text. The text holds only the hash.

9. **Clean up.**

   ```shell
   docker rm -f scram-proxy
   ```

## Roll back by hand

`apply-scram.sh` rolls back by itself. To roll back later, set the MD5 verifier.
The verifier is `md5` followed by the MD5 of the password and the role name. Postgres 16 still accepts it.
The old hash cannot be read, and you do not need it. The verifier comes from the same password.
The block checks the password (a non-empty string, with no trailing newline) and the verifier before it runs the `ALTER`. Postgres treats a string that is not
a verifier as a plaintext password, so a failed helper must never reach the `ALTER`.
Use `psql -X`, so a local `~/.psqlrc` (for example `\set AUTOCOMMIT off`) cannot change the result.

```shell
(
  set -euo pipefail
  . scripts/scram-password/lib.sh
  parse_creds "$(gcloud --project <project> secrets versions access latest --secret=consent-postgres-creds)" \
    || { echo "bad password in the secret - nothing changed"; exit 1; }
  MD5H=$(printf '%s' "$PW" | python3 scripts/scram-password/md5-verifier.py consent)
  [[ "$MD5H" =~ ^md5[0-9a-f]{32}$ ]] || { echo "bad MD5 verifier - nothing changed"; exit 1; }
  printf "ALTER ROLE consent PASSWORD '%s';\n" "$MD5H" \
    | PGPASSWORD=$PW psql -X -v ON_ERROR_STOP=1 -f - "host=127.0.0.1 port=5434 dbname=consent user=consent sslmode=disable"
)
```

## Rehearse first

**On a local container.** The script reads the credentials from `CREDS_CMD` when you set it.
Start a `postgres:16` container with `POSTGRES_HOST_AUTH_METHOD=md5`. Create an MD5 role with
`SET password_encryption='md5'; CREATE ROLE consent LOGIN PASSWORD '<test password>'`.
Then run these commands from the repository root:

```shell
CREDS_CMD='printf "{\"username\":\"consent\",\"password\":\"<test password>\"}"' \
  bash scripts/scram-password/apply-scram.sh test <local port>
SCRAM_TEST_FORCE_FAIL=1 CREDS_CMD=... bash scripts/scram-password/apply-scram.sh test <local port>   # tests the rollback
ALTER_CONFIRM_TRIES=0 CREDS_CMD=... bash scripts/scram-password/apply-scram.sh test <local port>   # tests a missing confirmation
VERIFY_TIMEOUT=3 PSQL=<a psql wrapper that sleeps on the login> CREDS_CMD=... \
  bash scripts/scram-password/apply-scram.sh test <local port>                                   # tests a stalled login
```

**On a clone of the instance.** A clone copies the roles and their hashes.

```shell
gcloud sql instances clone <instance> <instance>-scram-test --project <project>
# test, then delete it. Turn deletion protection off first:
gcloud sql instances patch <instance>-scram-test --no-deletion-protection --project <project>
gcloud sql instances delete <instance>-scram-test --project <project>
```

The clone inherits deletion protection. A deleted instance name cannot be reused for about a week.
A clone of prod holds prod data. Use a backup for prod instead.

## Known problems

- The secret has no `db` field. Use the database name `consent`.
- The Bash tool of some agents runs zsh. It has no `PIPESTATUS`. Capture exit codes directly.
- Docker Desktop cannot mount a file from `/private/tmp`. Pass the script on stdin, as in step 6.
- Do not pass the password as `docker run -e DUOS_DB_PASSWORD=$PW`. It shows in the process list.
  Run `export DUOS_DB_PASSWORD` and then `docker run -e DUOS_DB_PASSWORD`.
- Cloud SQL may deny reads of `pg_authid`. Use `probe-auth.py` to see the stored hash type.
- `gcloud sql instances patch` has no `--update-labels` option in the tested version.
- A local DUOS stack against a database needs `https://` redirect URIs. With `http://` the sign-in lands on the deployed site.

## Results

| Date | Environment | Result |
|---|---|---|
| 2026-10-08 | dev | Probe 5 to 10. The connect test in the DUOS pod passed. Consent restarted with no login errors. `GET /api/user/me` worked. The Cloud SQL logs had no password or hash. |
| | staging | not done. Tracked in [DT-4244](https://broadworkbench.atlassian.net/browse/DT-4244). |
| | prod | not done. Tracked in [DT-4245](https://broadworkbench.atlassian.net/browse/DT-4245). |
