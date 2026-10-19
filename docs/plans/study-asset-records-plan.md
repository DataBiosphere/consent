# Study Asset Records Plan

## Status

Proposed. Ticket: [DT-4208](https://broadworkbench.atlassian.net/browse/DT-4208), the first story of the Stable Asset Identity epic ([DT-4207](https://broadworkbench.atlassian.net/browse/DT-4207)). It blocks [DT-4209](https://broadworkbench.atlassian.net/browse/DT-4209) (DUOS identifiers for all requestable assets).

Related plan: `asset-data-use-plan.md` ([DT-3810](https://broadworkbench.atlassian.net/browse/DT-3810), tickets under [DT-4297](https://broadworkbench.atlassian.net/browse/DT-4297)) changes some of the same files. See [Overlap with DT-3810](#overlap-with-dt-3810).

Code references were checked against `consent` `develop` at `b9edc7c5` and `duos-ui` at `e203f95d`. Decision 1 must be answered before Task 1 starts.

## Overview

Every model, workspace and biospecimen that a study registers gets its own permanent row with a server-assigned numeric id. A one-time conversion creates rows for existing registrations, and every registration write keeps the rows in sync. The registration stays the source of truth, and users see no change.

### How assets are stored today

- Each asset type is one JSON array in a `study_property` row, keyed `models`, `workspaces` or `biospecimens`. The 2026-09-02 changeset (`changelog-consent-2026-09-02-study-assets.xml`) promoted them out of the legacy `assets` object. `StudyAssets` reads and assembles them. No asset has a row of its own.
- Ids are client-generated strings. `WorkspaceAddEdit.tsx` uses `crypto.randomUUID()` with a `Date.now()` fallback. `AiModelAddEdit.tsx` starts `modelId` as `''`. Biospecimens cannot be edited in the UI; `StudyAssetManagement.tsx` tells submitters to contact the DUOS team, so they arrive with whatever id that path supplies.
- When an id is missing, the Data Library invents `${studyId}-${index}` (`modelAsset.ts`, `workspaceAsset.ts`, `biospecimenAsset.ts`). That id changes whenever a submitter reorders the list.
- Only registration writes touch these keys. `DatasetRegistrationService.createDatasetsFromRegistration` and `updateStudyFromRegistration` call `DatasetServiceDAO.insertDatasetRegistration` and `updateStudy`, which store `RegistrationRequestMapper.toStudyProperties` in one transaction; on update, `executeUpdateStudyReplaceProps` deletes and reinserts every study property. `patchStudy` (`StudyPatch`) and the dataset-to-study conversion in `DatasetService` never write the asset keys.
- Reads go through `StudyAssets.findAssetList` (`/api/dataset/study/{studyId}/assets/models` and `/workspaces`), `SchemaFromStudy` (registration GET) and `StudyAssets.assemble` (`ElasticSearchService.toStudyTerm`). There is no biospecimen endpoint; the Data Library reads biospecimens from the index only.
- `Biospecimen.yaml` already stores post-mortem interval as `{value, unit}`, which the Data Library range filter reads.

### The change

1. Add `study_model`, `study_workspace` and `study_biospecimen` tables with a serial primary key.
2. On every registration write, match each incoming asset to its row by id, update or insert it, retire rows whose asset was removed, and write the assigned ids back into the registration JSON.
3. Convert existing registrations once: create a row for every asset and write its id into the JSON.
4. duos-ui stops generating ids, sends the server's id back on edit, and drops the position fallback.
5. The OpenAPI id fields become `integer`.

Nothing reads the tables yet. They give DT-4209 a stable key to hang DUOS identifiers on.

## Decisions

| # | Question | Decision | Source |
|---|---|---|---|
| 1 | Biospecimen granularity: one row per vial, or a collection-level grouping? | One row per vial, with room for a later `study_biospecimen_collection` parent. Also ask whether biobanks supply a sample id that could be a unique key. | **Confirm with Jonathan Lawson** (ticket Q1, required) |
| 2 | How do we tell an edited asset from a removed-and-replaced one? | The server-assigned id round-trips. An entry that comes back with its id is the same asset. See [Sync key](#sync-key). | This plan (ticket Q2) |
| 3 | Delete or retire a removed asset? What about requests? | Retire (`retired_date`), never delete, so DT-4209's catalog can still resolve it. Requests cannot target these assets yet; DT-4209 and later epics own that. | This plan (ticket Q3) |
| 4 | Missing or malformed asset data during conversion | Never fail the run. A non-object element is skipped; a field of the wrong type is stored as null. Both keep their original JSON and are listed in the conversion report. | This plan (ticket Q4) |
| 5 | How many assets exist in production? | Run the Task 3 count query against production before Task 3 merges, and record the numbers in its PR. | **Kevin Marete** (ticket Q5) |
| 6 | Do exports, bookmarks or links rely on today's position-based ids? | Nothing in `consent` or `duos-ui` does. External use is unknown. | **Confirm with Jonathan Lawson** and support (ticket Q6) |
| 7 | When do the rows become authoritative, and who owns it? | Deferred until after DT-4209. | **Jonathan Lawson** (ticket Q7) |
| 8 | How does the DUOS team add biospecimens, and can that path send ids back? | Whatever tool they use must send each biospecimen's id back on edit, or every edit retires and recreates the rows. | **Confirm with Jonathan Lawson** before Task 5 |
| 9 | One generic asset table, or one per type? | One per type. The biospecimen fields share almost nothing with the others, and DT-4209 adds the cross-type catalog on top. | This plan |
| 10 | Where does the conversion run? | A Liquibase changeset in SQL, following the 2026-09-02 promotion, so it runs once per environment with the deploy and needs no admin step. | This plan |

### Sync key

The server assigns the key. Alternatives considered and rejected:

- **Client-generated UUID as sync key, numeric key shown.** Two identifiers per asset, uniqueness depends on the client, and the existing `Date.now()` fallback can collide.
- **Server-assigned UUID.** Breaks the ticket's "numeric everywhere" requirement. DT-4209's `DUOS-M-000045` identifiers already give a stable human-facing form.
- **Natural key from content** (name + url, or donor + type + date). An edit to a key field reads as remove-and-add, which is the ambiguity Decision 2 removes.
- **Replace all rows on every write.** Ids change on every edit, so there is no stable identity.

## Task summary

| # | Repo | Task | Size (est.) | Depends on |
|---|---|---|---|---|
| 1 | consent | Create the asset record tables and DAO | ~350 lines | Decision 1 |
| 2 | consent | Sync asset records on registration create and update | ~450 lines | 1; DT-4298 merged |
| 3 | consent | Convert existing registrations | ~350 lines | 2 deployed; Decision 5 |
| 4 | duos-ui | Use server-assigned asset ids | ~250 lines | 2 deployed; DT-4302 merged |
| 5 | consent | Make the asset id fields `integer` in OpenAPI | ~100 lines | 3 run in production; 4 deployed; DT-4299 merged; Decision 8; API-users notice sent |

Sizes include tests. **Task 2 deploys before Task 3 runs**, so no registration write can land between the conversion and the sync and leave an asset without a row.

## Tasks

### Task 1: Create the asset record tables and DAO (consent)

Add the storage. Nothing calls it yet.

- Liquibase changeset `changelog-consent-<date>-study-asset-records.xml`, included at the end of `changelog-master.xml`, creates three tables:
  - `study_model`: `model_id SERIAL PRIMARY KEY`, `name`, `description`, `url`, `cloud TEXT[]`, `format`, `license`, `trained_on_datasets TEXT[]`, `maintainer JSONB`, `tags TEXT[]`, `access_management`, `data_use JSONB`.
  - `study_workspace`: `workspace_id SERIAL PRIMARY KEY`, `name`, `platform`, `url`, `cloud TEXT[]`, `description`, `tools TEXT[]`, `access`, `tags TEXT[]`, `access_management`, `data_use JSONB`.
  - `study_biospecimen`: `biospecimen_id SERIAL PRIMARY KEY`, `donor_id`, `specimen_type`, `preservation_method`, `preservation_details`, `date_of_collection`, `post_mortem_interval_value NUMERIC`, `post_mortem_interval_unit`, `sex`, `age NUMERIC`, `race`, `country_of_origin`, `extracted_diagnoses TEXT[]`, `pathology`, `organization`, `source_site`, `optional_data_use`.
  - Every table also has `study_id INTEGER NOT NULL` with a foreign key to `study(study_id)` `ON DELETE CASCADE` (as `study_comment` does), `legacy_key TEXT`, `create_date`, `update_date`, a nullable `retired_date`, an index on `study_id`, and a unique index on `(study_id, legacy_key)` where `legacy_key` is not null.
- Columns mirror `AiModel.yaml`, `Workspace.yaml` and `Biospecimen.yaml`. Enum fields are text, because models and workspaces are stored as sent today and a new enum value must not need a migration. `access_management` and `data_use` stay null until DT-3810 ships.
- `StudyAssetRecordDAO` (Jdbi SQL object, one contiguous text block per statement), per type:
  - `insert…` returning the new id.
  - `update…(id, studyId, …)` returning the updated row count. Its `WHERE` checks `study_id` and `retired_date IS NULL`, so an id from another study, or a retired row, changes nothing.
  - `findLiveIds(studyId)` and `findIdByLegacyKey(studyId, legacyKey)`.
  - `retireExcept(studyId, keepIds)`, which sets `retired_date` on live rows not in `keepIds`.
- Row records and mappers for the three types, following the existing mapper pattern.

**Tests** (`StudyAssetRecordDAOTest`, extends `DAOTestHelper`):
- Insert, update and retire work for each type.
- An update with another study's id, or a retired row's id, changes no row and returns 0.
- `legacy_key` is unique per study, and the same key is allowed in two studies.
- Deleting the study removes its rows.
- Arrays, `maintainer`, `data_use` and post-mortem value and unit round-trip.

### Task 2: Sync asset records on registration create and update (consent)

This is the core of the ticket.

- New `StudyAssetRecordSync`, a plain class like the composites in `service/dao`. `DatasetServiceDAO.insertDatasetRegistration` calls it after `executeInsertStudy`, and `updateStudy` calls it after `executeUpdateStudyReplaceProps`, with the same handle. A failure rolls back the whole registration write, as a dataset failure does.
- For each type, it reads the list that the write just stored and applies this table to each entry:

  | Incoming entry | Action |
  | --- | --- |
  | Integer id (or a string of digits) matching a live row of this study | Update the row |
  | Any id matching a row's `legacy_key` | Update that row. Covers a form opened before the conversion |
  | No id, or an id matching nothing | Insert a row, keeping a non-integer id as `legacy_key` |
  | Live row with no entry left | Retire the row |

- It mirrors what the write stored. When the write stores no property for a type, every live row of that type is retired; the update path deletes and reinserts all properties, so an absent list is a removed list.
- It then sets each entry's id key to the row id and updates the `study_property` value. It works on the entries as parsed by `GsonUtil` and changes only the id key. It never maps an entry to a typed class, which would drop unknown client keys.
- A field of the wrong type for its column (e.g. a non-numeric `age`) is stored as null and logged. The JSON keeps the original.
- `indexStudy` already runs after both writes, so the index picks up the ids with no new call. `SchemaFromStudy` and the asset endpoints read the JSON, so they return the ids with no change.

**Tests** (`DatasetServiceDAOTest`, and `DatasetRegistrationServiceTest` where a service seam is needed):
- Create assigns an id to every entry and writes it into the stored JSON.
- Update with ids updates rows in place. Reordering the list keeps every id.
- Removing an entry retires its row. Sending an empty list, or no list, retires all rows of that type.
- An entry without an id, or with another study's id, gets a new row.
- A UUID matching a `legacy_key` updates that row.
- Two entries with the same id: the first updates, the second gets a new row.
- Unknown client keys survive the write-back.
- A sync failure rolls back the study and dataset writes.

### Task 3: Convert existing registrations (consent)

- Before merge, run the count query in production and put the numbers in the PR (Decision 5):

  ```sql
  WITH asset_rows AS MATERIALIZED (
    SELECT sp.study_id, sp.key, sp.value
    FROM study_property sp
    WHERE sp.key IN ('models', 'workspaces', 'biospecimens')
  )
  SELECT key,
         count(DISTINCT study_id) AS studies,
         sum(CASE WHEN jsonb_typeof(value::jsonb) = 'array' THEN jsonb_array_length(value::jsonb) END) AS assets
  FROM asset_rows
  GROUP BY key;
  ```

- Liquibase changeset `changelog-consent-<date>-study-asset-records-convert.xml`, using the `MATERIALIZED` CTE guards of the 2026-09-02 promotion. For each type, over `jsonb_array_elements … WITH ORDINALITY`:
  1. Skip any element that is not a JSON object.
  2. Skip any element whose id is an integer matching a row of its study. This makes a manual re-run, e.g. after a partial restore, change nothing.
  3. Otherwise insert a row. `legacy_key` is the element's current id when that id is unique within the study's list, and `position-<n>` otherwise.
  4. Rebuild the array in the same order, setting each converted element's id with `jsonb_set` from the row matched on `legacy_key`. Every other key is kept.
- Lists still inside the legacy `assets` object are not converted. The 2026-09-02 promotion moved every array out, and only non-array values remain there.
- After deploy, reindex through the admin `POST /api/dataset/index`, since each dataset document embeds its study's assets. The id keys keep their names, so the enumerated `study.assets.*` paths in `es-access-contract.md` §B.5a are unchanged, and numeric values index into the existing string mapping.
- Commit a report query beside the changeset: per type, the elements in the JSON, rows created, and elements skipped with study id and position. Run it before and after in each environment.
- Run [Staging verification](#staging-verification) before the production deploy.

**Tests** (`StudyAssetRecordsMigrationTest`, following `StudyAssetsMigrationTest`, which reads the SQL out of the shipped changeset and runs it against seeded rows):
- Every object element gets a row, and its id is written into the JSON.
- Element order and unknown keys are preserved.
- Duplicate or empty legacy ids fall back to `position-<n>`.
- Non-object elements and non-array values are left untouched and counted.
- A second run changes nothing.
- Studies with no asset properties are untouched.

### Task 4: Use server-assigned asset ids (duos-ui)

- `src/types/model.ts`: `modelId`, `workspaceId` and `biospecimenId` become `number`, optional before the first save.
- `AiModelAddEdit.tsx` and `WorkspaceAddEdit.tsx` stop generating ids. A new entry is sent without one; an edited entry keeps the id it was loaded with.
- `AiModelList` and `WorkspaceList` key rows by `getItemKey`. An unsaved entry has no id, so give it a client-only key that is never sent.
- `studyToDatasetSchemaSubmission` sends ids back unchanged, including biospecimens, which `BiospecimenList` passes through read-only.
- `modelAsset.ts`, `workspaceAsset.ts` and `biospecimenAsset.ts` drop the `${studyId}-${index}` fallback. This waits for Task 3 to run in production.

**Tests** (vitest):
- Editing an entry keeps its id. A new entry is submitted without an id.
- Two unsaved entries have distinct row keys.
- Data Library rows use the numeric id.

### Task 5: Make the asset id fields `integer` in OpenAPI (consent)

- `AiModel.yaml`, `Workspace.yaml` and `Biospecimen.yaml`: the id fields become `integer`, described as server-assigned. The description notes that a string is still accepted on input.
- This is a breaking response change. Follow `CONTRIBUTING.md` "Breaking API changes": check in with Comms, and send the notice to `api-users@firecloud.org` a few days before the release.
- Confirm that the DUOS team's biospecimen path sends ids back (Decision 8).

**Tests:** the OpenAPI bundle builds, and the existing registration round-trip tests pass with integer ids.

## Edge cases to cover in review

- A form opened before the conversion submits UUIDs. The `legacy_key` match updates the rows rather than retiring and recreating them.
- A `Date.now()` id is a 13-digit number, outside the integer range, so it can never be read as a row id. It goes to the `legacy_key` match.
- An id copied from another study never updates that study's row. It creates a new row in the study being written.
- A study is deleted: its rows go with it, by cascade.
- Two concurrent edits of one study: the last write wins, as it does for every other study property today. The losing write's new rows are retired by the winning write.
- The write-back must not touch any key except the id, including keys the backend does not know.

## Staging verification

Before the production run of Task 3, restore a production snapshot to staging, run the conversion, and:

- compare each row field by field with the element it came from;
- compare the Data Library's model, workspace and biospecimen tabs for a sample of studies, before and after;
- check that the report's skipped elements are all genuinely malformed.

Zero unexplained differences is the gate for production.

## Rollback

The tables are additive and nothing reads them. Rolling back means reverting Task 2 and dropping the tables. Ids left in the registration JSON do no harm, because the old code treats ids as opaque strings. Take a `study_property` backup before the production run of Task 3, so the original JSON can be restored if needed.

## Out of scope (follow-up)

- Making the rows the source of truth instead of the registration (Decision 7).
- DUOS identifiers and the cross-type catalog (DT-4209).
- Governance, search or request changes.
- Data use on biospecimens. DT-3810 leaves it out until its free-text `optionalDataUse` field is decided.

## Deploy order

1. DT-4298 and DT-4299 (DT-3810 validation and OpenAPI) merge first. They are smaller and need no migration, and Task 2 then copies their fields from the start.
2. Task 1, then Task 2, deploy.
3. Staging verification, then Task 3 deploys and the reindex runs.
4. Task 4 deploys. Its fallback removal waits for step 3 in production.
5. The API-users notice goes out, then Task 5 releases.

## Overlap with DT-3810

`asset-data-use-plan.md` adds `dataUse` and `accessManagement` to model and workspace entries, and it rejected new tables for that purpose. That decision still holds. The tables here exist for identity, the registration JSON stays the source of truth, and DT-3810's `AssetDataUse` module keeps validating and enriching the JSON entries unchanged. This plan follows two of its rules:

- The model and workspace tables copy `dataUse` and `accessManagement` as stored. The computed `dataUseSummary` is never stored; `AssetDataUse.prepare` strips it before persistence.
- Entries are never round-tripped through a typed class. The id write-back changes only the id key.

Three DT-4297 tickets touch the same files as this plan:

| DT-3810 ticket | Shared files | Task here |
|---|---|---|
| DT-4298: validate model and workspace entries | `RegistrationRequestMapper.promotedAsset`, the registration write path | Task 2 |
| DT-4299: OpenAPI fields | `AiModel.yaml`, `Workspace.yaml` | Task 5 |
| DT-4302: registration form fields | `AiModelList`, `WorkspaceList`, their add/edit forms | Task 4 |
