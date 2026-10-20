# DUOS Asset Identifiers Plan

## Status

Proposed. Ticket: [DT-4209](https://broadworkbench.atlassian.net/browse/DT-4209), the second story of the Stable Asset Identity epic ([DT-4207](https://broadworkbench.atlassian.net/browse/DT-4207)). It builds on `study-asset-records-plan.md` ([DT-4208](https://broadworkbench.atlassian.net/browse/DT-4208)) and refers to that plan's tasks as "DT-4208 Task N".

Code references were checked against `consent` `develop` at `b9edc7c5` and `duos-ui` at `e203f95d`. Decision 1 (the identifier format) must be confirmed before Task 1 starts.

## Overview

Every requestable asset (dataset, model, workspace, biospecimen) gets one entry in a new `asset_catalog` table, with a DUOS identifier that is unique within its type. Existing dataset identifiers stay exactly as they are. An identifier keeps resolving after its asset is retired or deleted. An admin check reports entries whose asset vanished without going through the app, and assets that have no entry, and an admin repair fixes what it can.

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
4. Provide admin check and repair endpoints for orphaned entries and assets without an entry.
5. Return each asset's identifier from the asset endpoints and the search index, and resolve any identifier, live, retired or removed, through a lookup endpoint. Showing and searching it in duos-ui waits on Decision 2.

## Decisions

| # | Question | Decision | Source |
|---|---|---|---|
| 1 | Identifier format and prefixes | `DUOS-M-000045` (model), `DUOS-W-000012` (workspace), `DUOS-B-000301` (biospecimen): six digits zero-padded, more when the number outgrows them, as datasets do. Datasets keep `DUOS-000123` with no letter. `D` and `S` are never used as type letters, because `DUOS-D…` and `DUOS-S…` already mean a dataset database id and a study id in duos-ui. | **Confirm with Jonathan Lawson** (ticket Q1) |
| 2 | Are the identifiers shown to users, and searchable? | Proposed: yes, as dataset identifiers are. The API, index and lookup return them either way (Task 7); duos-ui display and search (Task 8) waits on the answer. | **Confirm with Jonathan Lawson** (ticket Q2) |
| 3 | Can an identifier be reused after its asset is removed? | No. Model, workspace and biospecimen numbers are serial row ids, which are never reissued. Dataset numbers come from `dataset_alias_seq`, which never goes back; only an ad hoc insert with an explicit alias could reuse a deleted dataset's number. That dataset gets no entry, and the collision is logged, and the check reports it (Tasks 2 and 6) rather than failing the registration. The entry outlives its asset, so the identifier resolves, as retired or removed (Task 7). An ad hoc alias reuse is the one way to break this: the identifier then names the new dataset until someone fixes the alias by hand, and the check reports it. | This plan (ticket Q3); confirm with Jonathan Lawson |
| 4 | Will publications, presentations, IP and funding stay non-requestable? | Planned as yes. If one becomes requestable, it takes a new type letter and a column here, with no change to existing identifiers. | **Jonathan Lawson** (ticket Q4) |
| 5 | How often does the orphan check run, and who is alerted? | Proposed: on demand, through admin check and repair endpoints, as `POST /api/dataset/index` works today; consent has no scheduled jobs. Drift comes only from ad hoc SQL, a restore, or old pods during a rollout or revert, so whoever does one of those calls the check afterwards. Nothing alerts on its own; a schedule can wrap the same check later if one is needed. | **Jonathan Lawson** (ticket Q5) |
| 6 | Where does a model, workspace or biospecimen number come from? | Its DT-4208 row id. It is already permanent, server-assigned, increasing within its type and never reused, so `modelId` 45 is `DUOS-M-000045`. | This plan |
| 7 | Where does a dataset number come from? | `dataset.alias`, unchanged. The catalog copies it at insert. Nothing in the app changes an alias afterwards, and the check reports an entry whose number no longer matches. | This plan (ticket: existing identifiers unchanged) |
| 8 | What happens to an entry when its asset goes away? | A retired asset row keeps its entry, which still points at it. Deleting a dataset or a study stamps `asset_removed_date` on its entries in the transaction that deletes the rows, and the foreign key sets the reference to null. An entry whose reference is null without that stamp is an orphan. | This plan |
| 9 | Does `DUOS-B-…` name a vial or a collection? | A vial or sample, since DT-4208 Decision 1 settled one row per vial. A later collection level would take its own letter. | DT-4208 Decision 1 |
| 10 | Numbers are increasing but have gaps. Is that acceptable? | Yes. A rolled-back registration still consumes its sequence values, and a retired-and-recreated asset takes a new number, so `DUOS-M-000046` may not exist. Dataset aliases already behave this way. Gap-free numbering would need a counter row that serializes every asset write. | Jonathan Lawson, 2026-10-19 |
| 11 | What is the canonical form? | `AssetIdentifier.format`: at least six digits, zero-padded. `parse` accepts only that exact form, as the dataset endpoints do today (`DatasetService.findDatasetByIdentifier` rejects `DUOS-5` for `DUOS-000005`), so one asset has exactly one identifier. duos-ui upper-cases a pasted identifier before routing. | This plan |

### Alternatives considered

- **A per-type number sequence, separate from the row id**, as datasets separate `alias` from `dataset_id`. Each asset would carry a second number, and the identifier would need a JSON write-back or a join on every read. The row id already has every property a public number needs. The split exists for datasets only because the alias predates them.
- **A view over the four tables instead of a table.** A deleted dataset would vanish from it, so its identifier could no longer resolve, and a future request table could not reference it.
- **Giving datasets a `D` letter.** That breaks the ticket's "unchanged" rule, and `DUOS-D…` already means a database id.
- **Database triggers that write and stamp entries.** They would also cover old pods and ad hoc SQL, but the repo removed the alias trigger on 2026-09-17 in favour of explicit writes, and a trigger hides a write from the DAO and its tests. The remaining gaps are small, and the check reports them.
- **`ON DELETE RESTRICT` instead of `SET NULL`.** Every delete path, ad hoc script and test cleanup that removes a dataset or study would have to clear the catalog first. With `SET NULL`, a delete outside the app's own paths leaves an orphan for the check to find, which is what the ticket asks for.

## Task summary

| # | Repo | Task | Size (est.) | Depends on |
|---|---|---|---|---|
| 1 | consent | Create the catalog table, DAO and identifier format | ~250 lines | DT-4208 Task 1; Decision 1 |
| 2 | consent | Write dataset entries on insert and delete, and backfill existing datasets | ~250 lines | 1 |
| 3 | consent | Catch up dataset entries | ~100 lines | 2 deployed |
| 4 | consent | Add catalog entries from the asset sync | ~200 lines | 2; ships with or after DT-4208 Task 3 |
| 5 | consent | Backfill asset entries | ~150 lines | 4 deployed; DT-4208 Task 6 run in production |
| 6 | consent | Add the integrity check and repair endpoints | ~200 lines | 5 run |
| 7 | consent | Return and resolve identifiers | ~400 lines | 5 run |
| 8 | duos-ui | Show, search and link identifiers | ~250 lines | 7 deployed; Decision 2 |

Sizes include tests. Tasks are numbered in deploy order. **Each backfill deploys in a later release than its write path**, because Liquibase runs while old pods are still serving: an asset created by an old pod after the backfill ran would otherwise have no entry. An asset deleted before its backfill keeps its identifier, because the delete paths create the missing entry before stamping it (Task 2). Task 2's changeset also runs the dataset backfill once, so every dataset that exists before new pods serve has an entry; Task 3 repeats it for datasets that old pods created during the rollout. Task 1 can deploy on its own: a dataset deleted before Task 2 has no entry, as one deleted before this plan does.

## Tasks

### Task 1: Create the catalog table, DAO and identifier format (consent)

Add the storage and the identifier format. Nothing writes to the catalog yet.

- Liquibase changeset `changelog-consent-<date>-asset-catalog.xml` creates `asset_catalog`:
  - `catalog_id SERIAL PRIMARY KEY`, `asset_type TEXT NOT NULL` checked against `DATASET`, `MODEL`, `WORKSPACE`, `BIOSPECIMEN`, `asset_number INTEGER NOT NULL` checked `>= 0`, `create_date TIMESTAMPTZ NOT NULL DEFAULT now()`, nullable `asset_removed_date`.
  - `dataset_id`, `model_id`, `workspace_id`, `biospecimen_id`, each a nullable foreign key to its table `ON DELETE SET NULL`, each with a partial unique index where not null.
  - A unique index on `(asset_type, asset_number)`, a check that at most one reference is set, a check that a set reference matches `asset_type`, and a check that a set `model_id`, `workspace_id` or `biospecimen_id` equals `asset_number` (Decision 6).
- `AssetType` enum carrying each type's letter (none for datasets). `AssetIdentifier.format(type, number)` and `AssetIdentifier.parse(text)`; `parse` accepts only the canonical form (Decision 11), with a number from 0 to 2147483647, and rejects `DUOS-D…` and `DUOS-S…`. `Dataset.parseAliasToIdentifier` delegates to `format`, so dataset output is byte-for-byte the same. `parseIdentifierToAlias` and its callers are unchanged: it accepts any padding, and `DatasetService` rejects a non-canonical identifier afterwards by comparing it with the formatted alias. `AssetIdentifier.parse` enforces the canonical form itself.
- `AssetCatalogDAO` (one contiguous text block per statement), attached with `handle.attach` wherever it joins an existing transaction:
  - `insertDatasetEntry(datasetId)` reading the alias from the row, and `insertAssetEntry(type, rowId)` with the number set to the row id.
  - `ensureDatasetEntry(datasetId)` and `ensureStudyEntries(studyId)`, which insert any missing entry with `ON CONFLICT DO NOTHING`, covering every asset row of the study, live and retired. A dataset skipped by the conflict is a collision and is logged.
  - `markRemovedByDataset(datasetId)` and `markRemovedByStudy(studyId)`, the latter over every asset row of the study, live and retired.
  - `findByTypeAndNumber`.

**Tests** (`AssetCatalogDAOTest`, `AssetIdentifierTest`):
- `format` and `parse` round-trip every type, including alias 0 and a seven-digit number.
- `parse` rejects lower case, other padding (`DUOS-M-45`), `DUOS-D12`, `DUOS-S12`, an unknown letter, a sign, a number above 2147483647 and a blank.
- `Dataset.getDatasetIdentifier` is unchanged for a sample of aliases.
- A duplicate `(asset_type, asset_number)`, two references on one entry, or a reference that does not match the type is rejected.
- Each DAO method inserts, ensures, stamps and finds as described, and `ensureStudyEntries` covers retired rows.

### Task 2: Write dataset entries on insert and delete, and backfill existing datasets (consent)

Wire the dataset write and delete paths to the catalog, and backfill every existing dataset.

- Changeset `changelog-consent-<date>-asset-catalog-dataset-backfill.xml` runs the dataset backfill from a `sqlFile` that Task 3's changeset also references, lock statements included, so the two runs are identical, and so that an old pod deleting an existing dataset during the rollout leaves an orphan that repair stamps, rather than an identifier with no record. The commit includes a report query that lists any dataset skipped as a collision; record its output, and the dataset and entry counts, in the PR.
- `executeInsertDatasetWithFiles` calls `insertDatasetEntry` after the insert, on the same handle. It uses `ON CONFLICT DO NOTHING` and logs a collision through `ConsentLogger.logException`, so an alias that an ad hoc insert reused never fails a registration.
- `deleteDataset` calls `ensureDatasetEntry` and `markRemovedByDataset` just before `deleteDatasetById`, inside its transaction.
- `deleteStudy` starts its final transaction with `SELECT study_id FROM study WHERE study_id = :studyId FOR UPDATE`, then calls `ensureStudyEntries` and `markRemovedByStudy`, then `deleteStudyByStudyId`. Stamping before the dataset loop would mark live assets removed if a dataset delete in the loop fails, because each one commits on its own. Taking the row lock first makes a concurrent registration update commit before the stamp, so its new rows are stamped too instead of surfacing as false orphans.

**Tests** (`DatasetServiceDAOTest`):
- Registering a dataset adds one entry whose number is the alias. Deleting it stamps the entry and nulls `dataset_id`.
- Deleting a study stamps its dataset entries and the entries of its live and retired asset rows.
- Deleting a dataset or study whose entries do not exist yet creates and stamps them.
- A study delete that fails in the dataset loop leaves the study's asset entries unstamped.
- A study delete racing a registration update that adds a model stamps the new model's entry.
- A registration whose alias collides with an existing entry succeeds, gets no entry, and logs the collision.
- A failure inserting the entry rolls back the dataset insert.

### Task 3: Catch up dataset entries (consent)

Cover the datasets that old pods created while Task 2 was rolling out.

- Changeset `changelog-consent-<date>-asset-catalog-datasets.xml` repeats Task 2's backfill. It inserts an entry for every dataset that has none, numbered by its alias, with `ON CONFLICT DO NOTHING`. It sets a short `lock_timeout`, then locks `dataset` in `SHARE ROW EXCLUSIVE` mode, so no insert lands between the read and the write and a busy table fails the deploy fast instead of queueing writes behind it. It is idempotent, and inserts everything in one transaction, so every entry it creates shares that transaction's `now()` as its `create_date`.
- A dataset skipped because its alias already belongs to another entry is a collision, which only an ad hoc insert can cause. Task 2's report query lists any; record its output, and the dataset and entry counts, in the PR.

**Tests** (`AssetCatalogDatasetsMigrationTest`, reading the SQL from the shipped changeset, as `StudyAssetsMigrationTest` does):
- Every dataset gets one entry with its alias, alias 0 included.
- A dataset that already has an entry is left alone.
- A dataset whose alias belongs to a removed entry is skipped and listed.
- A second run changes nothing.

### Task 4: Add catalog entries from the asset sync (consent)

Give every new model, workspace and biospecimen row its entry as it is created.

- `StudyAssetRecordSync` (DT-4208 Task 3) calls `insertAssetEntry` for each row it inserts, on the same handle. Updating or retiring a row does not touch its entry.
- The sync's write-back also removes any `duosIdentifier` key from each entry, so an identifier a client echoes does not survive the write. It is the one exception to DT-4208's rule that the write-back changes only the id key.
- It ships in the same release as DT-4208 Task 3, or later. If later, Task 5 covers the rows created in between.

**Tests** (`DatasetServiceDAOTest`):
- A create gives every new asset row one entry numbered by its row id.
- An update that keeps a row adds no entry, and a retired row keeps its entry.
- An incoming `duosIdentifier` is not stored.
- A sync failure rolls back the entries with everything else.

### Task 5: Backfill asset entries (consent)

Give every asset row created before Task 4 its entry.

- Changeset `changelog-consent-<date>-asset-catalog-assets.xml` inserts an entry for every `study_model`, `study_workspace` and `study_biospecimen` row with none, retired rows included, numbered by the row id, with `ON CONFLICT DO NOTHING`. A row skipped because an entry with a null reference holds its number is a relinkable finding (Task 6); the commit includes a report query that lists any. It sets a short `lock_timeout` and locks the three tables in `SHARE ROW EXCLUSIVE` mode in one `LOCK TABLE` statement, in the order the sync writes them (models, workspaces, biospecimens), and inserts everything in one transaction, so its entries share that transaction's `now()`. A lock conflict, or a deadlock with a live write, aborts one side: either the deploy is retried or one write fails, and no identifier is wrong. It is idempotent.
- It runs after DT-4208 Task 6 has run in production, so the conversion's rows are covered.

**Tests** (`AssetCatalogAssetsMigrationTest`):
- Live and retired rows each get one entry with their row id.
- Rows that already have one are left alone.
- A second run changes nothing.

### Task 6: Add the integrity check and repair endpoints (consent)

Let an admin find and fix catalog drift on demand.

- `AssetCatalogCheck` finds:
  1. Orphans: a null reference with no `asset_removed_date`, which means the asset was deleted outside the app's delete paths.
  2. Missing entries: a dataset or asset row with no entry, whose number is free.
  3. Collisions: a dataset with no entry whose alias already belongs to another entry. Reported only here, not also as missing. Only an ad hoc insert can cause one, and repair cannot fix it.
  4. Mismatches: a dataset entry whose number differs from its dataset's current alias, which only an ad hoc `UPDATE` can cause.
  5. Relinkable: a model, workspace or biospecimen row with no entry whose number belongs to an entry of its type with a null reference, which a restore done without the catalog leaves behind. Repair relinks it. Such an entry is reported here, not also as an orphan.
- Nothing runs on a schedule and no new table is needed. The check runs when an admin calls it, after ad hoc SQL or a restore, as `POST /api/dataset/index` does for the search index.
- Admin endpoints, documented in an OpenAPI path spec:
  - `GET /api/admin/asset-catalog/check` returns every current finding of the five kinds without changing anything.
  - `POST /api/admin/asset-catalog/repair` inserts the missing entries, whose numbers follow from the alias or row id. For a model, workspace or biospecimen whose number belongs to an entry with a null reference, which is what a partial restore leaves behind, it relinks that entry and clears any stamp instead; the number is the row id, so the row is the same asset. It never relinks a dataset entry: the alias could belong to a different dataset, so that case stays a collision. Then it stamps `asset_removed_date` on the remaining orphans. It runs in one transaction and inserts with `ON CONFLICT DO NOTHING`, so a concurrent repair or registration is skipped rather than failing it. It returns what it inserted, relinked, stamped and skipped. It never changes an entry that still references an asset.

**Tests** (`AssetCatalogCheckTest`, `AssetCatalogResourceTest`):
- Each finding type is reported, and the GET changes nothing.
- The GET reports an unstamped model entry with a null reference whose row exists as relinkable only, not as an orphan.
- Repair stamps an orphan, which is then no longer reported.
- Repair relinks a model entry whose reference a restore nulled, rather than stamping it, and relinks a stamped model entry whose row is back.
- Repair leaves a stamped dataset entry whose alias a new dataset reused.
- Repair inserts only missing entries and skips collisions, and a second repair changes nothing.
- Both endpoints are admin-only.

### Task 7: Return and resolve identifiers (consent)

Expose each asset's identifier, and resolve any identifier back to its asset.

- New `StudyAssetIdentifiers.decorate`, applied by `StudyAssetService.getAssetsByType` to the list `StudyAssets.findAssetList` returns, and by `ElasticSearchService.toStudyTerm` to the map `StudyAssets.assemble` returns. It adds `duosIdentifier` to each model, workspace and biospecimen entry whose id is a whole number from 1 to 2147483647, formatted by `AssetIdentifier`. `StudyAssets` itself is unchanged, so the registration GET (`SchemaFromStudy`) carries no identifier and nothing is stored in the JSON.
- New `GET /api/asset-catalog/{identifier}`, for any authenticated user. It parses the identifier, finds the entry, and returns the canonical identifier, type and status: `LIVE`, `RETIRED` (the asset row has `retired_date`) or `REMOVED` (`asset_removed_date` is set, or the reference is null and repair has not stamped it yet).
  - An existing dataset, model, workspace or biospecimen row with that number wins over the entry: if the entry is missing, or has no reference while a row holds the number (a repair not yet run, or an alias collision), the lookup answers from the row, as live or retired, since that row is what shows the identifier.
  - A live or retired asset is returned, with its asset id and study id, only when the caller can read it; otherwise the response is 404, as for an unknown identifier, so private studies cannot be probed. A model, workspace or biospecimen uses the study gate `StudyAssetService` uses. A dataset uses the read rule of the dataset GET (`DatasetService.findDatasetById(user, …)`), which also covers a dataset with no study.
  - A removed asset returns only its type and status, to any authenticated user. That reveals only that the number was once used, which the numbering already implies.
  - An unparseable identifier returns 404.
- Add `duosIdentifier` to `AiModel.yaml`, `Workspace.yaml` and `Biospecimen.yaml` as a read-only field, and the lookup in its own path spec. Both changes are additive, so no API-users notice is needed.
- Add `study.assets.models.duosIdentifier`, `…workspaces.duosIdentifier` and `…biospecimens.duosIdentifier` to the enumerated paths in `es-access-contract.md` §B.5a, then reindex through `POST /api/dataset/index`.

**Tests** (`StudyAssetIdentifiersTest`, `AssetCatalogResourceTest`, `ElasticSearchServiceTest`, `DatasetResourceTest`):
- An in-range integer id gets its identifier. A string id, a missing id, 0, and a 13-digit `Date.now()` id get none.
- The registration GET carries no `duosIdentifier`.
- The lookup returns each status, and returns 404 for a live asset in a study the caller cannot read.
- It resolves a dataset with no study, and a live and a retired asset with no entry.
- It answers REMOVED for an unstamped orphan, and LIVE for a dataset that reused a removed entry's alias.
- It returns 404 for `DUOS-M-45`, and for unknown, malformed, `DUOS-D…` and `DUOS-S…` identifiers.

### Task 8: Show, search and link identifiers (duos-ui)

Only if Decision 2 is yes. Show the identifiers in the Data Library and route pasted identifiers to their asset.

- The model, workspace and biospecimen tabs show `duosIdentifier`, and their `searchFields` include it, as `datasetAsset.ts` does with `datasetIdentifier`.
- `NotFound` upper-cases the identifier before matching, so the case-sensitive checks downstream (`DatasetStatistics.getMatchPhrase`, the `DUOS-S` replace) see the canonical form. For `/DUOS-M-…`, `/DUOS-W-…` and `/DUOS-B-…` it calls the lookup: a live asset opens its Data Library tab filtered to the identifier, and a retired or removed one shows that status instead of "page not found".

**Tests** (vitest):
- Each tab renders and searches the identifier.
- Each prefix routes to its tab in upper and lower case.
- A retired and a removed identifier show their status.
- `DUOS-000123`, `DUOS-D12` and `DUOS-S12` still route as today, and their lower-case forms, which show "page not found" today, now route the same way.

## Edge cases to cover in review

- A dataset created by an old pod while Task 2 is rolling out has no entry until Task 3 runs; the same holds for assets and Tasks 4 and 5. The check reports any that remain. If one is deleted first, the delete path creates and stamps its entry.
- A stale form that retires and recreates a model (DT-4208 edge cases) gives it a new identifier; the old one resolves as retired.
- A dataset or study deleted by ad hoc SQL leaves an orphan, which the check reports until repair stamps it.
- An ad hoc dataset insert with an explicit alias that a deleted dataset used gets no entry; the check and repair report it as a collision. If that dataset is later deleted, the collision is logged, and its identifier has no record of its own.
- An old pod deleting a dataset during Task 2's rollout leaves an orphan, which repair stamps, because the changeset backfilled every existing dataset. Only a dataset that an old pod both creates and deletes during the rollout loses its identifier.
- An identifier with a letter passed to `Dataset.parseIdentifierToAlias` (e.g. the dataset registration GET) still throws `IllegalArgumentException`, as any malformed identifier does today.
- `asset_number` is an `INTEGER`, matching the alias range check and the serial ids.

## Staging verification

Before each of these tasks deploys to production, restore a production snapshot to staging, deploy the task there, and check:

- **Task 1:** the Data Library and the dataset endpoints show the same dataset identifiers as before, for a sample of studies.
- **Task 2:** every dataset that the report query does not list as a collision has one entry whose number is its alias.
- **Task 5:** every model, workspace and biospecimen row that the report query does not list as relinkable has one entry whose number is its row id.
- **Tasks 6 and 7:** `GET /api/admin/asset-catalog/check` reports nothing unexplained.

Zero unexplained findings is the gate for production.

## Rollback

Keep `asset_catalog` in every case. It is the only record of removed assets' identifiers.

- **Task 1:** revert the code. Nothing writes to the table until Task 2.
- **Tasks 2 and 4:** revert the code, and prefer a fix forward. Datasets and assets created while it is reverted get no entry; one that is also deleted before the redeploy loses its identifier, and one deleted with an entry leaves an orphan for repair to stamp. After redeploying, run the repair, or, before Task 6 exists, re-run the Task 3 or Task 5 SQL by hand; both are idempotent.
- **Backfills (Tasks 2, 3 and 5):** they only insert. To undo one, delete the entries whose `create_date` equals that backfill's transaction timestamp, and whose reference is still set and whose `asset_removed_date` is null. Never delete a stamped entry: it may be the only record of a deleted asset's identifier.
- **DT-4208 data restore:** once Task 1 has shipped, DT-4208's conversion-window backup must include `asset_catalog` in the same `pg_dump` run, and the window's freeze must cover dataset deletes too. A restore touches only the catalog's `MODEL`, `WORKSPACE` and `BIOSPECIMEN` rows, in one transaction: delete those rows, delete the asset tables' rows, reload the asset tables, then reload those rows from the dump. Never `TRUNCATE … CASCADE`: it empties the whole catalog, dataset entries and removal records included. `DATASET` entries are left alone. Restoring the asset tables without their catalog rows would null the references. Repair relinks them by row id, but the restore should not depend on it.
- **Task 6:** revert the code. Entries that repair inserted, relinked or stamped stay, which is correct.
- **Task 7:** removing `duosIdentifier` or the lookup is a breaking change once it has shipped, so prefer a fix forward. If it must be reverted, follow `CONTRIBUTING.md` "Breaking API changes", then reindex.
- **Task 8:** revert; it reads only what Task 7 returns.

## Out of scope (follow-up)

- Letting requests target models, workspaces or biospecimens. A future request table references `asset_catalog`.
- Making the asset rows the source of truth (DT-4208 Decision 7).
- Detail pages for models, workspaces and biospecimens.

## Deploy order

1. DT-4208 Tasks 1 and 2 deploy.
2. Task 1 deploys.
3. Task 2 deploys.
4. Task 3 deploys.
5. DT-4208 Tasks 3 and 4 deploy, with Task 4 here.
6. DT-4208 Task 5 deploys, and DT-4208 Task 6 runs in its maintenance window, backing up `asset_catalog` with the other tables.
7. Task 5 deploys.
8. Tasks 6 and 7 deploy. Then call `GET /api/admin/asset-catalog/check`, and `POST /api/admin/asset-catalog/repair` if it reports anything.
9. Task 8 deploys, if Decision 2 is yes.

## Overlap with DT-4208

- Task 1 adds foreign keys to the three DT-4208 tables, so it cannot merge before DT-4208 Task 1.
- Task 4 changes `StudyAssetRecordSync`. If DT-4208 Task 3 is still open, add the entry insert and the `duosIdentifier` strip to that PR instead.
- DT-4208's "Restoring data" step names four tables. Once Task 1 here has shipped, it is five; see [Rollback](#rollback).
- Task 7 decorates only integer ids, so it is safe before DT-4208 Task 7 drops the position-based fallback.
