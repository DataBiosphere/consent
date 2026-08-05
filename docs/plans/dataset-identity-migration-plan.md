# Dataset Identity Migration

## Goal

Use `dataset.dataset_id` for match persistence and relational joins while keeping the canonical
`DUOS-######` alias as the public dataset identifier. Allocate new aliases from a PostgreSQL
sequence and enforce uniqueness in the database.

## Preflight Audit

Before production rollout, run the following read-only checks. The Liquibase migration repeats the
blocking checks and halts rather than guessing when the data is ambiguous.

```sql
SELECT dataset_id, alias
FROM dataset
WHERE alias IS NULL OR alias < 0;

SELECT alias, COUNT(*)
FROM dataset
GROUP BY alias
HAVING COUNT(*) > 1;

SELECT m.match_id, m.purpose, m.consent
FROM match_entity m
LEFT JOIN dataset d
  ON m.consent = 'DUOS-' || LPAD(d.alias::text, 6, '0')
WHERE d.dataset_id IS NULL;

SELECT COUNT(*) AS pre_migration_total,
       COUNT(d.dataset_id) AS exact_matches,
       COUNT(*) - COUNT(d.dataset_id) AS unresolved
FROM match_entity m
LEFT JOIN dataset d
  ON m.consent = 'DUOS-' || LPAD(d.alias::text, 6, '0');
```

Any unresolved match rows must be explicitly corrected or isolated before the final constraint
change. The Data Use classification sample associated with the preceding ticket does not contain
dataset aliases or match identifiers, so it cannot resolve an unmatched alias.

## Rollout

The bounded compatibility phase runs during application startup, before the new application code
serves traffic:

1. Verify aliases are non-null, non-negative, and unique.
2. Create `dataset_alias_seq` above the audited maximum, make it the alias default, and add database
   uniqueness and non-null constraints.
3. Add nullable `match_entity.dataset_id` with its foreign key and index.
4. Backfill only exact canonical alias matches.
5. Halt if any match is unresolved or if `(purpose, dataset_id)` would be duplicated.
6. Make `dataset_id` non-null, replace the old uniqueness constraint, and remove `consent`.

The DAO change is deployed with the migration. Match writes use `dataset_id`; match reads join to
`dataset` only to derive the existing public `consent` value.

## Alias Lookup Inventory

- `DatasetService` identifier lookup and `TDRService` identifier lookup remain public API/integration
  boundaries and may translate a `DUOS-######` identifier to an alias.
- Dataset and DAC response projections may format aliases for user-facing output.
- Match persistence and election joins use only `dataset_id` after this migration.

## Rollback

Liquibase rollback recreates and backfills `match_entity.consent` from the referenced dataset,
restores `(purpose, consent)` uniqueness, removes `dataset_id`, removes alias constraints/default,
and drops the alias sequence. Because aliases remain immutable public identifiers throughout the
rollout, the reconstructed legacy values are compatible with the previous application version.

