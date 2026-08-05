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
  ON m.consent = 'DUOS-'
    || REPEAT('0', GREATEST(0, 6 - LENGTH(d.alias::text)))
    || d.alias::text
WHERE d.dataset_id IS NULL;

SELECT COUNT(*) AS pre_migration_total,
       COUNT(d.dataset_id) AS exact_matches,
       COUNT(*) - COUNT(d.dataset_id) AS unresolved
FROM match_entity m
LEFT JOIN dataset d
  ON m.consent = 'DUOS-'
    || REPEAT('0', GREATEST(0, 6 - LENGTH(d.alias::text)))
    || d.alias::text;
```

Any unresolved match rows must be explicitly corrected or isolated before the final constraint
change. The Data Use classification sample associated with the preceding ticket does not contain
dataset aliases or match identifiers, so it cannot resolve an unmatched alias.

## Rollout

This migration uses an expand/migrate/contract rollout so old and new instances can safely overlap.

### Compatibility release (this change)

1. Verify aliases are non-null, non-negative, and unique.
2. Lock dataset writes while initializing `dataset_alias_seq` above the audited maximum.
3. Install an alias-allocation trigger that overrides both legacy `MAX(alias) + 1` values and
   omitted aliases with the next sequence value. Add database uniqueness and non-null constraints.
4. Add nullable `match_entity.dataset_id` with a cascading foreign key and index. Match rationales
   also cascade when their owning match is removed.
5. Backfill only exact canonical alias matches.
6. Install a compatibility trigger that populates `dataset_id` for legacy match writes that provide
   only `consent`.
7. Halt if any match is unresolved or if `(purpose, dataset_id)` would be duplicated, then enforce
   non-null and `(purpose, dataset_id)` uniqueness.
8. Keep the legacy `consent` column and `(purpose, consent)` constraint for old instances. New match
   writes populate both identities during this bounded phase.

Dataset deletion cascades to its matches and their rationales, preserving the supported deletion
workflow without leaving records that refer to a deleted dataset.

### Contract release (required follow-up)

After every environment is running the compatibility release and reconciliation still reports zero
unresolved or conflicting rows:

1. Remove the match compatibility trigger.
2. Remove the legacy `(purpose, consent)` constraint and `match_entity.consent` column.
3. Replace the temporary alias-allocation trigger with `dataset_alias_seq` as the column default.

The contract changes must be delivered in a later release; they must not run while an older
application instance can still serve traffic.

## Alias Lookup Inventory

- `DatasetService` identifier lookup and `TDRService` identifier lookup remain public API/integration
  boundaries and may translate a `DUOS-######` identifier to an alias.
- Dataset and DAC response projections may format aliases for user-facing output.
- Match persistence and election joins use `dataset_id`; the compatibility trigger translates only
  writes from older application instances.

## Rollback

Liquibase rollback removes the compatibility triggers and new constraints, restores the original
match-rationale foreign key behavior, removes `dataset_id`, removes the alias constraints, and drops
the alias sequence. The legacy `consent` column and its uniqueness constraint remain present
throughout this release, so rollback does not need to reconstruct public identifiers.
