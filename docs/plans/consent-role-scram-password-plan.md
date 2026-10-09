# Consent Role SCRAM Password Plan

## Status

In progress. Dev is done ([DT-4237](https://broadworkbench.atlassian.net/browse/DT-4237), 2026-10-08).
Staging is done ([DT-4244](https://broadworkbench.atlassian.net/browse/DT-4244), 2026-10-09).
Prod ([DT-4245](https://broadworkbench.atlassian.net/browse/DT-4245)) follows, with the procedure below.
See [Results](#results).

## Summary

The DUOS server runs Node in FIPS mode, which does not allow MD5. The `consent` role stores its
password as an MD5 hash, because the databases moved in place from Postgres 9 to 11 to 16, and an
in-place upgrade keeps old hashes. The server then asks DUOS for an MD5 login, and the `pg` library
fails with `Unrecognized algorithm name`.

The fix stores a SCRAM-SHA-256 hash of the **same** password. The password does not change, so no
secret rotation is needed. Consent and its Liquibase job use the `pgjdbc` driver, which supports SCRAM.
Terraform does not manage this role's password, so a later `terraform apply` does not undo the change.

The change uses `psql`'s `\password` command. The `psql` client hashes the password, and only the
verifier reaches the server. The plaintext never appears in a statement or in the server logs.

## What You Need

- `gcloud`, with `gcloud auth application-default login` done, and the Cloud SQL Client role on the project
- `docker`, for the Cloud SQL Auth Proxy
- `psql` with libpq 16 or later, for the `require_auth` checks (for example Postgres.app on macOS)
- the VPN, for the `kubectl` steps

This plan uses placeholders: `<env>` (dev, staging or prod), `<project>` (GCP project), `<instance>` (Cloud SQL instance of the
consent database), `<namespace>` (Kubernetes namespace), `<consent deployment>`, and
`<duos deployment>` with `<duos container>`. The secret `consent-postgres-creds` holds the user, the
password and the instance name (`jq -r .instance_name`). It has no `db` field: the database is `consent`.

## Procedure

Do one environment at a time. For prod, first take a backup:
`gcloud sql backups create --instance=<instance> --project=<project>`.

1. **Find the clients of the role.** A client with libpq below 10 or `pgjdbc` below 42.2 cannot do SCRAM.
   List the logins of the role in the last 30 days. The log shows each client's `application_name`, not its
   driver version. Use the name to find each client, then check that client's driver version yourself.
   Many clients send no `application_name`. On dev, most logins (the consent app) had none.

   ```shell
   gcloud logging read 'resource.type="cloudsql_database"
     AND resource.labels.database_id="<project>:<instance>"
     AND textPayload:"connection authorized: user=consent "' \
     --project <project> --freshness=30d --format='value(textPayload)' \
     | sed -E 's/^.*connection authorized: //; s/ SSL.*$//' | sort | uniq -c | sort -rn
   ```

   This needs the `log_connections` flag on the instance. The consent instances set it. Empty output means
   that the flag is off or that the read failed, because the role logs in all the time. Do not add `--limit`.

2. **Start the proxy** on your laptop, bound to loopback only. Read the instance's connection name first,
   so that the region is not a guess:

   ```shell
   CONNECTION_NAME=$(gcloud sql instances describe <instance> --project <project> --format='value(connectionName)')
   docker run -d --name scram-proxy -p 127.0.0.1:5434:5432 \
     -e GOOGLE_APPLICATION_CREDENTIALS=/secrets/adc.json \
     -v "$HOME/.config/gcloud/application_default_credentials.json:/secrets/adc.json:ro" \
     gcr.io/cloud-sql-connectors/cloud-sql-proxy:2.14.0-alpine \
     --address 0.0.0.0 --port 5432 "$CONNECTION_NAME"
   ```

3. **Load the password, and check that the role is on MD5.** In terminal A:

   ```shell
   export PGPASSWORD="$(gcloud --project <project> secrets versions access latest --secret=consent-postgres-creds | jq -r .password)"
   CONN="host=127.0.0.1 port=5434 dbname=consent user=consent sslmode=disable"
   psql -X "$CONN require_auth=md5" -tAc "select 'login ok'"
   ```

   It must print `login ok`. If the server asks for SCRAM instead, the role is already fixed. Stop.

4. **Open a session and keep it open** until step 6 passes. It is your way back.

   ```shell
   psql -X "$CONN"
   ```

   In that session, check the hash setting. It must print `scram-sha-256` (the Postgres 16 default):

   ```sql
   SHOW password_encryption;
   ```

5. **Change the hash.** Copy the password from the secret to the clipboard, for example on macOS:
   `gcloud --project <project> secrets versions access latest --secret=consent-postgres-creds | jq -j .password | pbcopy`.
   In the open session, run the command below and paste the password at both prompts:

   ```sql
   \password consent
   ```

   `psql` asks twice, so it catches two different entries, but not the same wrong value twice. Step 6
   catches that.

6. **Check the result** from terminal B. A new terminal does not have the variables from terminal A,
   so run the `export PGPASSWORD=…` and `CONN=…` lines from step 3 there first. Then:

   ```shell
   psql -X "$CONN require_auth=scram-sha-256" -tAc "select 'login ok'"
   ```

   - `login ok`: the change is complete. Close the session in terminal A (`\q`).
   - Any failure: roll back in the open session, paste the same password twice, and repeat step 3:

     ```sql
     SET password_encryption = 'md5';
     \password consent
     ```

   If the session in terminal A is lost and no login works, an admin must set the password on the
   instance's **Users** page in the Cloud console, to the value in the secret.

7. **Restart consent, and test DUOS.** Open connections stay valid, so a restart is the real test:

   ```shell
   kubectl -n <namespace> rollout restart deployment/<consent deployment>
   kubectl -n <namespace> rollout status deployment/<consent deployment> --timeout=540s
   curl -s https://consent.dsde-<env>.broadinstitute.org/status | jq '.systems.postgresql.healthy'
   kubectl -n <namespace> exec deploy/<duos deployment> -c <duos container> -- sh -c \
     'P=$(ls -d /usr/src/app/server/node_modules/.pnpm/pg@*/node_modules/pg | head -1); node -e "
     const {Client}=require(\"$P\");
     const c=new Client({host:process.env.DUOS_DB_HOST,port:5432,database:process.env.DUOS_DB_NAME,user:process.env.DUOS_DB_USER,password:process.env.DUOS_DB_PASSWORD,ssl:false});
     c.on(\"error\",e=>console.log(\"EVT\",e.message));
     c.connect().then(()=>c.query(\"select current_user\")).then(r=>{console.log(\"OK\",r.rows[0]);return c.end()}).catch(e=>{console.log(\"ERR\",e.message);process.exit(1)})"'
   ```

   `postgresql` must be `true`, and the DUOS test must print `OK`. Before the change, the DUOS test printed
   `Unrecognized algorithm name`. Run the DUOS test once before step 4 too, as a baseline.

   If `kubectl` hangs, your kubeconfig may hold an old address for the cluster. To avoid changing your
   current context, write fresh credentials to a separate file and pass it with `--kubeconfig`:
   `KUBECONFIG=/tmp/kube-<env> gcloud container clusters get-credentials <cluster> --zone <zone> --project <project>`.
   `gcloud container clusters list --project <project>` shows `<cluster>` and `<zone>`.

8. **Check the logs.** `\password` sends only the verifier, and the instances log no statements. This command
   must print nothing:

   ```shell
   gcloud logging read 'resource.type="cloudsql_database"
     AND resource.labels.database_id="<project>:<instance>"
     AND textPayload:"ALTER"' --project <project> --freshness=3h --format='value(textPayload)'
   ```

9. **Clean up:** `docker rm -f scram-proxy`, `unset PGPASSWORD`, and clear the clipboard.

## Results

| Date | Environment | Result |
|---|---|---|
| 2026-10-08 | dev | MD5 to SCRAM. The DUOS pod test printed `OK`. Consent restarted with no login errors. `GET /api/user/me` worked. The Cloud SQL logs held no password or hash. |
| 2026-10-09 | staging | MD5 to SCRAM ([DT-4244](https://broadworkbench.atlassian.net/browse/DT-4244)). No rollback was needed. The DUOS pod test changed from `Unrecognized algorithm name` to `OK`. Consent restarted with no login errors. `GET /api/user/me` worked, and sign-in to DUOS worked. The Cloud SQL logs held no `ALTER` text, no password and no verifier. |
| | prod | Not done. [DT-4245](https://broadworkbench.atlassian.net/browse/DT-4245) |

The `\password` flow was also tested on a local Postgres 16 with `log_statement=all`. Only
`ALTER USER "consent" PASSWORD 'SCRAM-SHA-256$…'` (and `'md5…'` for the rollback) reached the server,
with no copy of the plaintext password.
