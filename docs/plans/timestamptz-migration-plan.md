# Change Consent date columns to timestamptz (DT-4452)

[DT-4452](https://broadworkbench.atlassian.net/browse/DT-4452) is part of epic
[DT-2885](https://broadworkbench.atlassian.net/browse/DT-2885). It changes each Consent column that
holds a point in time from `timestamp without time zone` to `timestamp with time zone`
(`timestamptz`).

## Why

A `timestamp` value is a clock time with no zone. pgjdbc writes a Java date into it as a clock time
in the JVM default zone, and `NOW()` writes it as a clock time in the session zone. If the two
zones are different, one column holds values from two zones. A `timestamptz` value is one exact
moment, so the zones do not change what a write stores.

## Delivery: two stacked PRs

Consent runs its migrations as an Argo Job before the new pods start. During a rollout, the old
pods read the new schema. Two reads fail on a `timestamptz` column, so they change first.

| PR | Branch | Change |
| --- | --- | --- |
| A | `DT-4452-showcase-date` | `ShowcaseDatasetSummaryMapper` reads `available_date` as `OffsetDateTime`, not `LocalDateTime`. `DACAutomationRuleAuditMapper` reads `action_date` with `getTimestamp`, not `Timestamp.valueOf(getString(...))`. Both reads work for both column types. |
| B | `DT-4452-timestamptz` | `changelog-consent-2026-11-09-timestamptz.xml`: one changeset for each table. |

Deploy PR A to every environment before you merge PR B.

## Columns

On 2026-11-09, a scratch database built from all changesets had 53 `timestamp without time zone`
columns and no `date` columns. PR B changes 50 columns in 30 tables:

| Table | Columns |
| --- | --- |
| `acknowledgement` | `first_acknowledged`, `last_acknowledged` |
| `daa_audit` | `action_date` |
| `dac` | `create_date`, `delete_date`, `update_date` |
| `dac_audit` | `action_date` |
| `dac_rule_audit` | `action_date` |
| `dac_rule_settings` | `activation_date` |
| `dar_collection` | `create_date`, `update_date` |
| `dar_dataset_daa_snapshot` | `captured_at` |
| `data_access_agreement` | `create_date`, `update_date` |
| `data_access_request` | `approving_so_timestamp`, `create_date`, `institution_snapshot_date`, `submission_date`, `update_date` |
| `dataset` | `create_date`, `dac_approval_date` |
| `dataset_audit` | `modification_date` |
| `dataset_audit_property` | `modificationdate` |
| `dataset_authorized_readers` | `created_date` |
| `dataset_property` | `create_date` |
| `draft` | `create_date`, `update_date` |
| `election` | `create_date`, `last_update` |
| `email_entity` | `date_sent` |
| `feature_flag` | `create_date`, `update_date` |
| `feature_flag_audit` | `action_date` |
| `file_storage_object` | `create_date`, `delete_date`, `update_date` |
| `institution` | `create_date`, `update_date` |
| `lc_daa_audit` | `action_date` |
| `library_card` | `create_date`, `update_date` |
| `match_entity` | `create_date` |
| `ontology_index` | `create_date`, `update_date` |
| `study` | `create_date`, `update_date` |
| `user_redaction_audit` | `action_date` |
| `users` | `create_date` |
| `vote` | `create_date`, `update_date` |

The other 3 columns belong to the DUOS BFF session store: `user_sessions.expire`,
`user_session_audit.created_at` and `user_session_audit.ended_at`. The BFF writes them, not
Consent Java, so this change does not include them. The BFF owners can convert them separately.

## How the changesets work

- Each changeset converts one table with one `ALTER TABLE` statement. The table locks one time.
- `vote` must change both columns in one statement. `idx_vote_cast_date` indexes
  `COALESCE(update_date, create_date)`, and that expression fails while only one column is
  `timestamptz`.
- `SET LOCAL TimeZone = 'UTC'` reads each stored value as a UTC time. In UTC, PostgreSQL 12 and
  later keeps the table data and rebuilds only the indexes on the changed columns. A scratch run
  confirmed that the table file numbers did not change.
- `SET LOCAL lock_timeout = '10s'` stops the deploy if a table is busy. One changeset for each
  table means that a retry does not repeat the tables that are done.
- A `sqlCheck` precondition on each changeset counts the expected `timestamp` columns of its table.
  If the deployed schema is different, the deploy halts (`onFail="HALT"`).
- The rollback converts back to `timestamp without time zone`, also in UTC. A scratch round trip
  gave back the same values.

## Checks before you merge PR B

1. **Compare the deployed schemas.** The deployed schema can be different from the changesets.
   Run this query against the dev, staging and prod exports. Compare the result with the 53
   columns above.

    ```sql
    SELECT c.table_name, c.column_name, c.data_type
    FROM information_schema.columns c
    JOIN information_schema.tables t USING (table_schema, table_name)
    WHERE c.table_schema = 'public'
      AND t.table_type = 'BASE TABLE'
      AND c.data_type IN ('timestamp without time zone', 'timestamp with time zone', 'date')
      AND c.table_name NOT LIKE 'databasechangelog%'
    ORDER BY c.data_type, c.table_name, c.column_name;
    ```

2. **Confirm that the stored values are UTC.** The conversion is correct only if the Consent JVM
   and Cloud SQL wrote the values in UTC. An export does not keep the server settings, so check
   these in each environment:
    - The Cloud SQL `timezone` flag (the default is UTC).
    - The `TZ` variable and any `-Duser.timezone` for the Consent pods and the migration Job in
      terra-helmfile, and the default zone of the `broad-dsp-gcr-public/base/jre` image.

## Behavior changes after PR B

- **Tests.** A local test JVM can use a zone other than UTC, while the test database uses UTC.
  Before, a Java date and `NOW()` in the same `timestamp` column could be some hours apart. After,
  they agree. A test that expected the old difference must change.
- **SQL that depends on the session zone.** `date_trunc` and casts to `date` on a `timestamptz`
  value use the session zone. pgjdbc sets the session zone to the JVM zone. In UTC, the results do
  not change.
- **`java.sql.Date` reads.** `getDate` on a `timestamptz` column gives the date in the JVM zone. In
  UTC, the results do not change. DT-4451 changes the `vote.update_date` read.
