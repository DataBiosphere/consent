# DUOS Asset Identifiers Plan

## Status

Proposed. Ticket: [DT-4209](https://broadworkbench.atlassian.net/browse/DT-4209), the second story of the Stable Asset Identity epic ([DT-4207](https://broadworkbench.atlassian.net/browse/DT-4207)). It builds on `study-asset-records-plan.md` ([DT-4208](https://broadworkbench.atlassian.net/browse/DT-4208)) and refers to that plan's tasks as "DT-4208 Task N".

Code references were checked against `consent` `develop` at `b9edc7c5` and `duos-ui` at `e203f95d`. DT-4208 Decision 1 (biospecimen granularity) must be answered before Task 1 starts.

## Overview

Every requestable asset (dataset, model, workspace, biospecimen) gets one entry in a new `asset_catalog` table, with a DUOS identifier that is unique within its type and never reused. Existing dataset identifiers stay exactly as they are. A daily check reports entries whose asset no longer exists, and assets that have no entry.

### How identifiers work today

- Only datasets have one. `Dataset.parseAliasToIdentifier` renders `dataset.alias` as `DUOS-` plus six zero-padded digits, and `parseIdentifierToAlias` parses it back, case-sensitively. `dataset_alias_seq` allocates the alias as the column default (`changelog-consent-2026-09-17-dataset-alias-default.xml`), and `dataset_alias_unique` and `dataset_alias_valid_integer` enforce uniqueness and a 0 to 2147483647 range. Alias 0 (`DUOS-000000`) is a valid legacy value.
- `DatasetServiceDAO.executeInsertDatasetWithFiles` is the only dataset insert. `DatasetServiceDAO.deleteDataset` hard-deletes the row, and `deleteStudy` calls it for each of the study's datasets before deleting the study, which DT-4208 Task 1 makes cascade to the study's asset rows.
- After DT-4208, each model, workspace and biospecimen has a row in `study_model`, `study_workspace` or `study_biospecimen`, with a server-assigned serial id that the registration JSON carries as `modelId`, `workspaceId` or `biospecimenId`. Removed assets are retired (`retired_date`), never deleted, and an id is never reassigned.
- duos-ui already uses two other `DUOS-` forms. `DUOS-S<studyId>` links to a study (`StudyDetails.tsx`, `NotFound.tsx`), and `DUOS-D<datasetId>` links to a dataset by its database id (`DatasetList.tsx`, `DatasetStatistics.getMatchPhrase`). `NotFound` sends any other `/DUOS-…` path to `/dataset/<identifier>`.
- `datasetIdentifier` is a Data Library search field (`datasetAsset.ts`, `studyAsset.ts`). `MailSendSearch` accepts `DUOS-<digits>` in any case.

### The change

1. Add `asset_catalog`, one row per asset of any type, holding its type, its number and a reference to the asset.
2. Every dataset insert and every new asset row from DT-4208's sync adds an entry in the same transaction. Deleting a dataset or a study marks its entries removed.
3. Backfill entries for existing datasets, keeping each alias, and for every row DT-4208 created.
4. Run a daily integrity check that reports orphaned entries, assets without an entry, and dataset entries whose number no longer matches the alias.
5. Return each asset's identifier from the asset endpoints and the search index. Showing and searching it in duos-ui waits on Decision 2.

## Decisions

| # | Question | Decision | Source |
|---|---|---|---|
| 1 | Identifier format and prefixes | `DUOS-M-000045` (model), `DUOS-W-000012` (workspace), `DUOS-B-000301` (biospecimen): six digits zero-padded, more when the number outgrows them, as datasets do. Datasets keep `DUOS-000123` with no letter. `D` and `S` are never used as type letters, because `DUOS-D…` and `DUOS-S…` already mean a dataset database id and a study id in duos-ui. | **Confirm with Jonathan Lawson** (ticket Q1) |
| 2 | Are the identifiers shown to users, and searchable? | Proposed: yes, as dataset identifiers are. The API and index return them either way (Task 6); duos-ui display and search (Task 7) waits on the answer. | **Confirm with Jonathan Lawson** (ticket Q2) |
| 3 | Can an identifier be reused after its asset is removed? | Never. Numbers come from sequences that never go back, the entry outlives its asset, and `(asset_type, asset_number)` is unique. A removed asset's identifier still resolves, as removed. | This plan (ticket Q3); confirm with Jonathan Lawson |
| 4 | Will publications, presentations, IP and funding stay non-requestable? | Planned as yes. If one becomes requestable, it takes a new type letter and a column here, with no change to existing identifiers. | **Jonathan Lawson** (ticket Q4) |
| 5 | How often does the orphan check run, and who is alerted? | Proposed: daily, with findings sent to Sentry, which the dev team already watches. Who else is alerted is open. | **Jonathan Lawson** (ticket Q5) |
| 6 | Where does a model, workspace or biospecimen number come from? | Its DT-4208 row id. It is already permanent, server-assigned, sequential within its type and never reused, so `modelId` 45 is `DUOS-M-000045`. | This plan |
| 7 | Where does a dataset number come from? | `dataset.alias`, unchanged. The catalog copies it, and the integrity check flags any entry whose number differs. | This plan (ticket: existing identifiers unchanged) |
| 8 | What happens to an entry when its asset goes away? | A retired asset row keeps its entry, which still points at it. A deleted dataset or study has its entries stamped `asset_removed_date` in the same transaction, and the foreign key sets the reference to null. An entry whose reference is null without that stamp is an orphan. | This plan |
| 9 | Is biospecimen granularity settled? | No. DT-4208 Decision 1 decides whether `DUOS-B-…` names a vial or a collection. This plan assumes a vial; a later collection level would take its own letter. | DT-4208 Decision 1 |

### Alternatives considered

- **A per-type number sequence, separate from the row id**, as datasets separate `alias` from `dataset_id`. Each asset would carry a second number, and the identifier would need a JSON write-back or a join on every read. The row id already has every property a public number needs. The split exists for datasets only because the alias predates them.
- **A view over the four tables instead of a table.** A deleted dataset would vanish from it, so its identifier could no longer resolve, and a future request table could not reference it.
- **Giving datasets a `D` letter.** That breaks the ticket's "unchanged" rule, and `DUOS-D…` already means a database id.
- **`ON DELETE RESTRICT` instead of `SET NULL`.** Every delete path, ad hoc script and test cleanup that removes a dataset or study would have to clear the catalog first. With `SET NULL`, a delete outside the app's own paths leaves an orphan for the check to find, which is what the ticket asks for.

## Task summary

| # | Repo | Task | Size (est.) | Depends on |
|---|---|---|---|---|
| 1 | consent | Catalog table, DAO, identifier format, and the dataset write and delete paths | ~400 lines | DT-4208 Task 1; DT-4208 Decision 1 |
| 2 | consent | Backfill dataset entries | ~150 lines | 1 deployed |
| 3 | consent | Add catalog entries from the asset sync | ~200 lines | 1; ships with or after DT-4208 Task 3 |
| 4 | consent | Backfill asset entries | ~150 lines | 3 deployed; DT-4208 Task 6 run in production |
| 5 | consent | Daily integrity check | ~350 lines | 4 run |
| 6 | consent | Return identifiers from the asset endpoints and the index | ~200 lines | DT-4208 Task 6 run |
| 7 | duos-ui | Show, search and link identifiers | ~250 lines | 6 deployed; Decision 2 |

Sizes include tests. **Each backfill deploys in a later release than its write path**, because Liquibase runs while old pods are still serving: an asset created by an old pod after the backfill ran would otherwise have no entry.

## Tasks

### Task 1: Catalog table, DAO, identifier format, and the dataset write and delete paths (consent)

- Liquibase changeset `changelog-consent-<date>-asset-catalog.xml` creates `asset_catalog`:
  - `catalog_id SERIAL PRIMARY KEY`, `asset_type TEXT NOT NULL` checked against `DATASET`, `MODEL`, `WORKSPACE`, `BIOSPECIMEN`, `asset_number INTEGER NOT NULL` checked `>= 0`, `create_date`, nullable `asset_removed_date`.
  - `dataset_id`, `model_id`, `workspace_id`, `biospecimen_id`, each a nullable foreign key to its table `ON DELETE SET NULL`, each with a partial unique index where not null.
  - A unique index on `(asset_type, asset_number)`, a check that at most one reference is set, and a check that a set reference matches `asset_type`.
- `AssetType` enum carrying each type's letter (none for datasets). `AssetIdentifier.format(type, number)` and `AssetIdentifier.parse(text)`; `parse` ignores case, as `MailSendSearch` does, and rejects `DUOS-D…` and `DUOS-S…`. `Dataset.parseAliasToIdentifier` delegates to `format`, so dataset output is byte-for-byte the same.
- `AssetCatalogDAO` (one contiguous text block per statement): `insertDatasetEntry(datasetId)` reading the alias from the row, `insertAssetEntry(type, rowId)` with the number set to the row id, `markRemovedByDataset(datasetId)`, `markRemovedByStudy(studyId)` over the study's asset rows, and `findByTypeAndNumber`.
- `executeInsertDatasetWithFiles` calls `insertDatasetEntry` after the insert, on the same handle. `deleteDataset` calls `markRemovedByDataset` before the delete. `deleteStudy` calls `markRemovedByStudy` before deleting the study.

**Tests** (`AssetCatalogDAOTest`, `AssetIdentifierTest`, `DatasetServiceDAOTest`):
- `format` and `parse` round-trip every type, including alias 0 and a seven-digit number. `parse` accepts lower case and rejects `DUOS-D12`, `DUOS-S12`, an unknown letter and a blank.
- `Dataset.getDatasetIdentifier` is unchanged for a sample of aliases.
- A duplicate `(asset_type, asset_number)`, two references on one entry, or a reference that does not match the type is rejected.
- Registering a dataset adds one entry whose number is the alias. Deleting it stamps the entry and nulls `dataset_id`. Deleting a study stamps its dataset and asset entries.
- A failure inserting the entry rolls back the dataset insert.

### Task 2: Backfill dataset entries (consent)

- Changeset `changelog-consent-<date>-asset-catalog-datasets.xml` inserts an entry for every dataset that has none, numbered by its alias. It locks `dataset` in `SHARE ROW EXCLUSIVE` mode first, so no insert lands between the read and the write. It is idempotent, and its precondition halts if any two datasets share an alias.
- Record the dataset count and the entry count before and after in the PR.

**Tests** (`AssetCatalogDatasetsMigrationTest`, reading the SQL from the shipped changeset, as `StudyAssetsMigrationTest` does): every dataset gets one entry with its alias, alias 0 included; a dataset that already has an entry is left alone; a second run changes nothing.

### Task 3: Add catalog entries from the asset sync (consent)

- `StudyAssetRecordSync` (DT-4208 Task 3) calls `insertAssetEntry` for each row it inserts, on the same handle. Updating or retiring a row does not touch its entry.
- It ships in the same release as DT-4208 Task 3, or later. If later, Task 4 covers the rows created in between.

**Tests** (`DatasetServiceDAOTest`): a create gives every new asset row one entry numbered by its row id; an update that keeps a row adds no entry; a retired row keeps its entry; a sync failure rolls back the entries with everything else.

### Task 4: Backfill asset entries (consent)

- Changeset `changelog-consent-<date>-asset-catalog-assets.xml` inserts an entry for every `study_model`, `study_workspace` and `study_biospecimen` row with none, retired rows included, numbered by the row id. It locks the three tables in `SHARE ROW EXCLUSIVE` mode and is idempotent.
- It runs after DT-4208 Task 6 has run in production, so the conversion's rows are covered.

**Tests** (`AssetCatalogAssetsMigrationTest`): live and retired rows each get one entry with their row id; rows that already have one are left alone; a second run changes nothing.

### Task 5: Daily integrity check (consent)

- `AssetCatalogCheck` runs three read-only queries:
  1. Orphans: a null reference with no `asset_removed_date`. The check stamps `asset_removed_date` so each orphan is reported once.
  2. Missing entries: a dataset or asset row with no entry.
  3. Mismatches: a dataset entry whose number differs from the alias.
- It runs once a day from a Dropwizard `Managed` scheduled executor. A `pg_try_advisory_lock` makes sure only one pod runs it. Any finding is logged through `ConsentLogger.logException`, which reaches Sentry.
- An admin endpoint, `GET /api/admin/asset-catalog/check`, returns the same report on demand. With `repair=true` it inserts the missing entries, whose numbers follow from the alias or row id. It never changes an existing entry. Document it in an OpenAPI path spec.

**Tests** (`AssetCatalogCheckTest`, `AssetCatalogResourceTest`): each finding type is reported; an orphan is stamped and not reported again; repair inserts only missing entries; a second pod that cannot take the lock does nothing; the endpoint is admin-only.

### Task 6: Return identifiers from the asset endpoints and the index (consent)

- `StudyAssets.assemble` and `findAssetList` add `duosIdentifier` to each model, workspace and biospecimen entry whose id is an integer, formatted by `AssetIdentifier`. Nothing is stored in the JSON, and `SchemaFromStudy` leaves it out, so a registration form never echoes it back. After DT-4208 Task 6, every converted entry's id is a row id, which is why this task waits for it.
- Add `duosIdentifier` to `AiModel.yaml`, `Workspace.yaml` and `Biospecimen.yaml` as a read-only field. The change is additive, so no API-users notice is needed.
- Add `study.assets.models.duosIdentifier`, `…workspaces.duosIdentifier` and `…biospecimens.duosIdentifier` to the enumerated paths in `es-access-contract.md` §B.5a, then reindex through `POST /api/dataset/index`.

**Tests** (`StudyAssetsTest`, `ElasticSearchServiceTest`): an integer id gets its identifier; a string id or a missing id gets none; the registration GET carries no `duosIdentifier`.

### Task 7: Show, search and link identifiers (duos-ui)

Only if Decision 2 is yes.

- The model, workspace and biospecimen tabs show `duosIdentifier`, and their `searchFields` include it, as `datasetAsset.ts` does with `datasetIdentifier`.
- `NotFound` routes `/DUOS-M-…`, `/DUOS-W-…` and `/DUOS-B-…` to the Data Library tab for that type, filtered to the identifier, instead of to `/dataset/`.

**Tests** (vitest): each tab renders and searches the identifier; each prefix routes to its tab; `DUOS-000123`, `DUOS-D12` and `DUOS-S12` still route as today.

## Edge cases to cover in review

- A dataset created by an old pod while Task 1 is rolling out has no entry until Task 2 runs; the same holds for assets and Tasks 3 and 4. The check reports any that remain.
- A stale form that retires and recreates a model (DT-4208 edge cases) gives it a new identifier; the old one still resolves, to a retired asset.
- A dataset or study deleted by ad hoc SQL leaves an orphan, which the check reports once.
- An identifier with a letter passed to `Dataset.parseIdentifierToAlias` (e.g. the dataset registration GET) still throws `IllegalArgumentException`, as any malformed identifier does today.
- `asset_number` is an `INTEGER`, matching the alias range check and the serial ids.

## Rollback

Keep `asset_catalog` in every case. It is the only record of removed assets' identifiers, and nothing outside this plan reads it.

- **Tasks 1 and 3:** revert the code. Datasets and assets created while it is reverted get no entry. After redeploying, run the check with `repair=true`, or, before Task 5 exists, re-run the Task 2 or Task 4 SQL by hand; both are idempotent.
- **Tasks 2 and 4:** the backfills only insert. To undo, delete the entries they created by `create_date`; nothing references them yet.
- **Task 5:** revert the code. Orphans stamped meanwhile keep their stamp, which is correct.
- **Task 6:** removing `duosIdentifier` from responses is a breaking change once it has shipped, so prefer a fix forward. If it must be reverted, follow `CONTRIBUTING.md` "Breaking API changes", then reindex.
- **Task 7:** revert; it reads only what Task 6 returns.

## Out of scope (follow-up)

- Letting requests target models, workspaces or biospecimens. A future request table references `asset_catalog`.
- Making the asset rows the source of truth (DT-4208 Decision 7).
- Detail pages for models, workspaces and biospecimens.

## Deploy order

1. DT-4208 Task 1 deploys.
2. Task 1 deploys.
3. Task 2 deploys.
4. DT-4208 Tasks 3 and 4 deploy, with Task 3 here.
5. DT-4208 Tasks 5 and 6 deploy, and Task 6 there runs.
6. Task 4 deploys.
7. Task 5 and Task 6 deploy.
8. Task 7 deploys, if Decision 2 is yes.

## Overlap with DT-4208

- Task 1 adds foreign keys to the three DT-4208 tables, so it cannot merge before DT-4208 Task 1.
- Task 3 changes `StudyAssetRecordSync`. If DT-4208 Task 3 is still open, add the entry insert to that PR instead.
- Task 6 reads only integer ids, so it is safe before DT-4208 Task 7 drops the position-based fallback.
