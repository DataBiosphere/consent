# DUOS Asset Identifiers Plan

## Status

Proposed. Ticket: [DT-4209](https://broadworkbench.atlassian.net/browse/DT-4209), the second story of the Stable Asset Identity epic ([DT-4207](https://broadworkbench.atlassian.net/browse/DT-4207)). It builds on `study-asset-records-plan.md` ([DT-4208](https://broadworkbench.atlassian.net/browse/DT-4208)) and refers to that plan's tasks as "DT-4208 Task N".

Code references were checked against `consent` `develop` at `b9edc7c5` and `duos-ui` at `e203f95d`. DT-4208 Decision 1 (biospecimen granularity) must be answered before Task 1 starts.

## Overview

Every requestable asset (dataset, model, workspace, biospecimen) gets one entry in a new `asset_catalog` table, with a DUOS identifier that is unique within its type. Existing dataset identifiers stay exactly as they are. An identifier keeps resolving after its asset is retired or deleted. A daily check reports entries whose asset vanished without going through the app, and assets that have no entry.

### How identifiers work today

- Only datasets have one. `Dataset.parseAliasToIdentifier` renders `dataset.alias` as `DUOS-` plus six zero-padded digits, and `parseIdentifierToAlias` parses it back, case-sensitively. `dataset_alias_seq` allocates the alias as the column default (`changelog-consent-2026-09-17-dataset-alias-default.xml`), which also honours an alias supplied explicitly. `dataset_alias_unique` and `dataset_alias_valid_integer` enforce uniqueness and a 0 to 2147483647 range. Alias 0 (`DUOS-000000`) is a valid legacy value. No application code updates an alias.
- `DatasetServiceDAO.executeInsertDatasetWithFiles` is the only dataset insert. `DatasetServiceDAO.deleteDataset` hard-deletes the row. `deleteStudy` calls `deleteDataset` for each of the study's datasets, and each of those commits on its own, then deletes the study in a final transaction. DT-4208 Task 1 makes that final delete cascade to the study's asset rows, retired ones included.
- After DT-4208, each model, workspace and biospecimen has a row in `study_model`, `study_workspace` or `study_biospecimen`, with a server-assigned serial id that the registration JSON carries as `modelId`, `workspaceId` or `biospecimenId`. An asset removed from a registration is retired (`retired_date`), and an id is never reassigned.
- `StudyAssets.findAssetList` serves the asset endpoints (`StudyAssetService`), the registration GET (`SchemaFromStudy`) and, through `assemble`, the search index (`ElasticSearchService.toStudyTerm`).
- duos-ui already uses two other `DUOS-` forms. `DUOS-S<studyId>` links to a study (`StudyDetails.tsx`, `NotFound.tsx`), and `DUOS-D<datasetId>` links to a dataset by its database id (`DatasetList.tsx`, `DatasetStatistics.getMatchPhrase`). `NotFound` matches case-sensitively and sends any other `/DUOS-…` path to `/dataset/<identifier>`.
- `datasetIdentifier` is a Data Library search field (`datasetAsset.ts`, `studyAsset.ts`). `MailSendSearch` accepts `DUOS-<digits>` in any case.

### The change

1. Add `asset_catalog`, one row per asset of any type, holding its type, its number and a reference to the asset.
2. Every dataset insert and every new asset row from DT-4208's sync adds an entry in the same transaction. Deleting a dataset or a study stamps its entries removed, creating any that are missing first.
3. Backfill entries for existing datasets, keeping each alias, and for every row DT-4208 created.
4. Run a daily integrity check that reports orphaned entries and assets without an entry.
5. Return each asset's identifier from the asset endpoints and the search index, and resolve any identifier, live, retired or removed, through a lookup endpoint. Showing and searching it in duos-ui waits on Decision 2.

## Decisions

| # | Question | Decision | Source |
|---|---|---|---|
| 1 | Identifier format and prefixes | `DUOS-M-000045` (model), `DUOS-W-000012` (workspace), `DUOS-B-000301` (biospecimen): six digits zero-padded, more when the number outgrows them, as datasets do. Datasets keep `DUOS-000123` with no letter. `D` and `S` are never used as type letters, because `DUOS-D…` and `DUOS-S…` already mean a dataset database id and a study id in duos-ui. | **Confirm with Jonathan Lawson** (ticket Q1) |
| 2 | Are the identifiers shown to users, and searchable? | Proposed: yes, as dataset identifiers are. The API, index and lookup return them either way (Task 6); duos-ui display and search (Task 7) waits on the answer. | **Confirm with Jonathan Lawson** (ticket Q2) |
| 3 | Can an identifier be reused after its asset is removed? | No. Model, workspace and biospecimen numbers are serial row ids, which are never reissued. Dataset numbers come from `dataset_alias_seq`, which never goes back; only an ad hoc insert with an explicit alias could reuse a deleted dataset's number, and the check reports that collision (Task 5). The entry outlives its asset, so the identifier resolves, as retired or removed (Task 6). | This plan (ticket Q3); confirm with Jonathan Lawson |
| 4 | Will publications, presentations, IP and funding stay non-requestable? | Planned as yes. If one becomes requestable, it takes a new type letter and a column here, with no change to existing identifiers. | **Jonathan Lawson** (ticket Q4) |
| 5 | How often does the orphan check run, and who is alerted? | Proposed: once a day, with findings sent to Sentry, which the dev team already watches. Who else is alerted is open. | **Jonathan Lawson** (ticket Q5) |
| 6 | Where does a model, workspace or biospecimen number come from? | Its DT-4208 row id. It is already permanent, server-assigned, increasing within its type and never reused, so `modelId` 45 is `DUOS-M-000045`. | This plan |
| 7 | Where does a dataset number come from? | `dataset.alias`, unchanged. The catalog copies it at insert; nothing in the app changes an alias afterwards. | This plan (ticket: existing identifiers unchanged) |
| 8 | What happens to an entry when its asset goes away? | A retired asset row keeps its entry, which still points at it. Deleting a dataset or a study stamps `asset_removed_date` on its entries in the transaction that deletes the rows, and the foreign key sets the reference to null. An entry whose reference is null without that stamp is an orphan. | This plan |
| 9 | Is biospecimen granularity settled? | No. DT-4208 Decision 1 decides whether `DUOS-B-…` names a vial or a collection. This plan assumes a vial; a later collection level would take its own letter. | DT-4208 Decision 1 |
| 10 | Numbers are increasing but have gaps. Is that acceptable? | Proposed: yes. A rolled-back registration still consumes its sequence values, and a retired-and-recreated asset takes a new number, so `DUOS-M-000046` may not exist. Dataset aliases already behave this way. Gap-free numbering would need a counter row that serializes every asset write. | **Confirm with Jonathan Lawson** (ticket: "numbered sequentially") |

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
| 5 | consent | Daily integrity check and repair | ~350 lines | 4 run |
| 6 | consent | Return and resolve identifiers | ~400 lines | 4 run |
| 7 | duos-ui | Show, search and link identifiers | ~250 lines | 6 deployed; Decision 2 |

Sizes include tests. **Each backfill deploys in a later release than its write path**, because Liquibase runs while old pods are still serving: an asset created by an old pod after the backfill ran would otherwise have no entry. An asset deleted before its backfill keeps its identifier, because the delete paths create the missing entry before stamping it (Task 1).

## Tasks

### Task 1: Catalog table, DAO, identifier format, and the dataset write and delete paths (consent)

- Liquibase changeset `changelog-consent-<date>-asset-catalog.xml` creates `asset_catalog`:
  - `catalog_id SERIAL PRIMARY KEY`, `asset_type TEXT NOT NULL` checked against `DATASET`, `MODEL`, `WORKSPACE`, `BIOSPECIMEN`, `asset_number INTEGER NOT NULL` checked `>= 0`, `create_date`, nullable `asset_removed_date`.
  - `dataset_id`, `model_id`, `workspace_id`, `biospecimen_id`, each a nullable foreign key to its table `ON DELETE SET NULL`, each with a partial unique index where not null.
  - A unique index on `(asset_type, asset_number)`, a check that at most one reference is set, and a check that a set reference matches `asset_type`.
- `AssetType` enum carrying each type's letter (none for datasets). `AssetIdentifier.format(type, number)` and `AssetIdentifier.parse(text)`; `parse` ignores case, as `MailSendSearch` does, and rejects `DUOS-D…` and `DUOS-S…`. `Dataset.parseAliasToIdentifier` delegates to `format`, so dataset output is byte-for-byte the same.
- `AssetCatalogDAO` (one contiguous text block per statement):
  - `insertDatasetEntry(datasetId)` reading the alias from the row, and `insertAssetEntry(type, rowId)` with the number set to the row id.
  - `ensureDatasetEntry(datasetId)` and `ensureStudyEntries(studyId)`, which insert any missing entry with `ON CONFLICT DO NOTHING`, covering every asset row of the study, live and retired.
  - `markRemovedByDataset(datasetId)` and `markRemovedByStudy(studyId)`, the latter over every asset row of the study, live and retired.
  - `findByTypeAndNumber`.
- `executeInsertDatasetWithFiles` calls `insertDatasetEntry` after the insert, on the same handle.
- `deleteDataset` calls `ensureDatasetEntry` and `markRemovedByDataset` just before `deleteDatasetById`, inside its transaction.
- `deleteStudy` calls `ensureStudyEntries` and `markRemovedByStudy` after the dataset loop, just before `deleteStudyByStudyId`, inside that final transaction. Stamping any earlier would mark live assets removed if a dataset delete in the loop fails, because each one commits on its own.

**Tests** (`AssetCatalogDAOTest`, `AssetIdentifierTest`, `DatasetServiceDAOTest`):
- `format` and `parse` round-trip every type, including alias 0 and a seven-digit number. `parse` accepts lower case and rejects `DUOS-D12`, `DUOS-S12`, an unknown letter and a blank.
- `Dataset.getDatasetIdentifier` is unchanged for a sample of aliases.
- A duplicate `(asset_type, asset_number)`, two references on one entry, or a reference that does not match the type is rejected.
- Registering a dataset adds one entry whose number is the alias. Deleting it stamps the entry and nulls `dataset_id`.
- Deleting a study stamps its dataset entries and the entries of its live and retired asset rows.
- Deleting a dataset or study whose entries do not exist yet creates and stamps them.
- A study delete that fails in the dataset loop leaves the study's asset entries unstamped.
- A failure inserting the entry rolls back the dataset insert.

### Task 2: Backfill dataset entries (consent)

- Changeset `changelog-consent-<date>-asset-catalog-datasets.xml` inserts an entry for every dataset that has none, numbered by its alias, with `ON CONFLICT DO NOTHING`. It locks `dataset` in `SHARE ROW EXCLUSIVE` mode first, so no insert lands between the read and the write. It is idempotent.
- A dataset skipped because its alias already belongs to another entry is a collision, which only an ad hoc insert can cause. The commit includes a report query that lists any; record its output, and the dataset and entry counts, in the PR.

**Tests** (`AssetCatalogDatasetsMigrationTest`, reading the SQL from the shipped changeset, as `StudyAssetsMigrationTest` does): every dataset gets one entry with its alias, alias 0 included; a dataset that already has an entry is left alone; a dataset whose alias belongs to a removed entry is skipped and listed; a second run changes nothing.

### Task 3: Add catalog entries from the asset sync (consent)

- `StudyAssetRecordSync` (DT-4208 Task 3) calls `insertAssetEntry` for each row it inserts, on the same handle. Updating or retiring a row does not touch its entry.
- The sync also removes any `duosIdentifier` key from incoming entries before storing them, so an echoed identifier never reaches the JSON.
- It ships in the same release as DT-4208 Task 3, or later. If later, Task 4 covers the rows created in between.

**Tests** (`DatasetServiceDAOTest`): a create gives every new asset row one entry numbered by its row id; an update that keeps a row adds no entry; a retired row keeps its entry; an incoming `duosIdentifier` is not stored; a sync failure rolls back the entries with everything else.

### Task 4: Backfill asset entries (consent)

- Changeset `changelog-consent-<date>-asset-catalog-assets.xml` inserts an entry for every `study_model`, `study_workspace` and `study_biospecimen` row with none, retired rows included, numbered by the row id. It locks the three tables in `SHARE ROW EXCLUSIVE` mode and is idempotent.
- It runs after DT-4208 Task 6 has run in production, so the conversion's rows are covered.

**Tests** (`AssetCatalogAssetsMigrationTest`): live and retired rows each get one entry with their row id; rows that already have one are left alone; a second run changes nothing.

### Task 5: Daily integrity check and repair (consent)

- `AssetCatalogCheck` finds:
  1. Orphans: a null reference with no `asset_removed_date`, which means the asset was deleted outside the app's delete paths. The check stamps `asset_removed_date`, so each orphan is reported once.
  2. Missing entries: a dataset or asset row with no entry. Reported every day until repaired.
  3. Collisions: a missing entry whose number already belongs to another entry of its type.
- Scheduling: a Dropwizard `Managed` executor on each pod wakes hourly. In one transaction it takes `pg_try_advisory_xact_lock`, and proceeds only if the `last_run` in a one-row `asset_catalog_check_run` table is more than 23 hours old, updating it in the same transaction. The lock is released at commit or rollback, so a failed run cannot leave it held, and the check runs once a day across all pods.
- Any finding is logged through `ConsentLogger.logException`, which reaches Sentry.
- Admin endpoints, documented in an OpenAPI path spec:
  - `GET /api/admin/asset-catalog/check` returns the three lists without changing anything.
  - `POST /api/admin/asset-catalog/repair` inserts the missing entries, whose numbers follow from the alias or row id, and returns what it inserted and the collisions it skipped. It never changes an existing entry.

**Tests** (`AssetCatalogCheckTest`, `AssetCatalogResourceTest`): each finding type is reported; an orphan is stamped and not reported again; a second run within 23 hours does nothing; a run that throws leaves no lock held; the GET changes nothing; repair inserts only missing entries and skips collisions; both endpoints are admin-only.

### Task 6: Return and resolve identifiers (consent)

- New `StudyAssetIdentifiers.decorate`, applied by `StudyAssetService.getAssetsByType` and by `ElasticSearchService.toStudyTerm` to what `StudyAssets.assemble` returns. It adds `duosIdentifier` to each model, workspace and biospecimen entry whose id is a whole number from 1 to 2147483647, formatted by `AssetIdentifier`. `StudyAssets` itself is unchanged, so the registration GET (`SchemaFromStudy`) carries no identifier and nothing is stored in the JSON.
- New `GET /api/asset-catalog/{identifier}` parses the identifier and returns its type, identifier and status: `LIVE`, `RETIRED` (the asset row has `retired_date`) or `REMOVED` (`asset_removed_date` is set). For a live or retired asset it also returns the asset id, and the study id when the caller can read the study, using the gate `StudyAssetService` uses. An unknown or unparseable identifier returns 404.
- Add `duosIdentifier` to `AiModel.yaml`, `Workspace.yaml` and `Biospecimen.yaml` as a read-only field, and the lookup in its own path spec. Both changes are additive, so no API-users notice is needed.
- Add `study.assets.models.duosIdentifier`, `…workspaces.duosIdentifier` and `…biospecimens.duosIdentifier` to the enumerated paths in `es-access-contract.md` §B.5a, then reindex through `POST /api/dataset/index`.

**Tests** (`StudyAssetIdentifiersTest`, `AssetCatalogResourceTest`, `ElasticSearchServiceTest`, `DatasetResourceTest`):
- An in-range integer id gets its identifier. A string id, a missing id, 0, and a 13-digit `Date.now()` id get none.
- The registration GET carries no `duosIdentifier`.
- The lookup returns each status, hides the study id from a caller who cannot read the study, and returns 404 for unknown, malformed, `DUOS-D…` and `DUOS-S…` identifiers.

### Task 7: Show, search and link identifiers (duos-ui)

Only if Decision 2 is yes.

- The model, workspace and biospecimen tabs show `duosIdentifier`, and their `searchFields` include it, as `datasetAsset.ts` does with `datasetIdentifier`.
- `NotFound` matches `DUOS-` prefixes ignoring case. For `/DUOS-M-…`, `/DUOS-W-…` and `/DUOS-B-…` it calls the lookup: a live asset opens its Data Library tab filtered to the identifier, and a retired or removed one shows that status instead of "page not found".

**Tests** (vitest): each tab renders and searches the identifier; each prefix routes to its tab in upper and lower case; a retired and a removed identifier show their status; `DUOS-000123`, `DUOS-D12` and `DUOS-S12` still route as today.

## Edge cases to cover in review

- A dataset created by an old pod while Task 1 is rolling out has no entry until Task 2 runs; the same holds for assets and Tasks 3 and 4. The check reports any that remain. If one is deleted first, the delete path creates and stamps its entry.
- A stale form that retires and recreates a model (DT-4208 edge cases) gives it a new identifier; the old one resolves as retired.
- A dataset or study deleted by ad hoc SQL leaves an orphan, which the check reports once.
- An ad hoc dataset insert with an explicit alias that a deleted dataset used gets no entry; the check and repair report it as a collision.
- An identifier with a letter passed to `Dataset.parseIdentifierToAlias` (e.g. the dataset registration GET) still throws `IllegalArgumentException`, as any malformed identifier does today.
- `asset_number` is an `INTEGER`, matching the alias range check and the serial ids.

## Rollback

Keep `asset_catalog` in every case. It is the only record of removed assets' identifiers.

- **Tasks 1 and 3:** revert the code. Datasets and assets created while it is reverted get no entry, and ones deleted while it is reverted lose theirs. After redeploying, run the repair, or, before Task 5 exists, re-run the Task 2 or Task 4 SQL by hand; both are idempotent.
- **Tasks 2 and 4:** the backfills only insert. To undo one, delete the entries it created (by `create_date`) whose reference is still set and whose `asset_removed_date` is null. Never delete a stamped entry: it may be the only record of a deleted asset's identifier.
- **DT-4208 data restore:** once Task 1 has shipped, DT-4208's conversion-window backup must include `asset_catalog`, in the same `pg_dump` run, and a restore must restore all five tables together. Restoring the asset tables alone would null the catalog's references, and the check would then stamp live assets as removed. Task 4 has not run at that point, so the restored catalog is consistent with the restored rows.
- **Task 5:** revert the code. Orphans stamped meanwhile keep their stamp, which is correct.
- **Task 6:** removing `duosIdentifier` or the lookup is a breaking change once it has shipped, so prefer a fix forward. If it must be reverted, follow `CONTRIBUTING.md` "Breaking API changes", then reindex.
- **Task 7:** revert; it reads only what Task 6 returns.

## Out of scope (follow-up)

- Letting requests target models, workspaces or biospecimens. A future request table references `asset_catalog`.
- Making the asset rows the source of truth (DT-4208 Decision 7).
- Detail pages for models, workspaces and biospecimens.

## Deploy order

1. DT-4208 Tasks 1 and 2 deploy.
2. Task 1 deploys.
3. Task 2 deploys.
4. DT-4208 Tasks 3 and 4 deploy, with Task 3 here.
5. DT-4208 Task 5 deploys, and DT-4208 Task 6 runs in its maintenance window, backing up `asset_catalog` with the other tables.
6. Task 4 deploys.
7. Tasks 5 and 6 deploy.
8. Task 7 deploys, if Decision 2 is yes.

## Overlap with DT-4208

- Task 1 adds foreign keys to the three DT-4208 tables, so it cannot merge before DT-4208 Task 1.
- Task 3 changes `StudyAssetRecordSync`. If DT-4208 Task 3 is still open, add the entry insert and the `duosIdentifier` strip to that PR instead.
- DT-4208's "Restoring data" step names four tables. Once Task 1 here has shipped, it is five; see [Rollback](#rollback).
- Task 6 decorates only integer ids, so it is safe before DT-4208 Task 7 drops the position-based fallback.
