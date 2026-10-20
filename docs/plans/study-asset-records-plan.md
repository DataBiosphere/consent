# Study Asset Records Plan

## Status

Proposed. Ticket: [DT-4208](https://broadworkbench.atlassian.net/browse/DT-4208), the first story of the Stable Asset Identity epic ([DT-4207](https://broadworkbench.atlassian.net/browse/DT-4207)). It blocks [DT-4209](https://broadworkbench.atlassian.net/browse/DT-4209) (DUOS identifiers for all requestable assets).

Related plan: `asset-data-use-plan.md` ([DT-3810](https://broadworkbench.atlassian.net/browse/DT-3810), tickets under [DT-4297](https://broadworkbench.atlassian.net/browse/DT-4297)) changes some of the same files. See [Overlap with DT-3810](#overlap-with-dt-3810).

Code references were checked against `consent` `develop` at `b9edc7c5` and `duos-ui` at `e203f95d`.

## Overview

Every model, workspace and biospecimen that a study registers gets its own permanent row with a server-assigned numeric id. A one-time conversion creates rows for existing registrations, and every registration write keeps the rows in sync. The registration stays the source of truth, and users see no change.

### How assets are stored today

- Each asset type is one JSON array in a `study_property` row, keyed `models`, `workspaces` or `biospecimens`. The 2026-09-02 changeset (`changelog-consent-2026-09-02-study-assets.xml`) promoted them out of the legacy `assets` object, matching key names exactly. `StudyAssets` reads them, and falls back to the legacy object, ignoring case, so a list left under `Models` or an `Assets` row is still served (`StudyRecommendationDAO` documents the same). No asset has a row of its own.
- Ids are client-generated strings. `WorkspaceAddEdit.tsx` uses `crypto.randomUUID()` with a `Date.now()` fallback. `AiModelAddEdit.tsx` sets `modelId: ''` and never assigns it, so every model saved from the UI has an empty id. Biospecimens cannot be edited in the UI; `StudyAssetManagement.tsx` tells submitters to contact the DUOS team, so they arrive with whatever id that path supplies.
- When an id is missing, the Data Library invents `${studyId}-${index}` (`modelAsset.ts`, `workspaceAsset.ts`, `biospecimenAsset.ts`). That id changes whenever a submitter reorders the list. `biospecimenAsset.ts` also puts `study.assets.biospecimens.biospecimenId` in the shared search fields that every tab's `phrase_prefix` query uses.
- Only registration writes touch these keys. `DatasetRegistrationService.createDatasetsFromRegistration` and `updateStudyFromRegistration` call `DatasetServiceDAO.insertDatasetRegistration` and `updateStudy`, which store `RegistrationRequestMapper.toStudyProperties` in one transaction; on update, `executeUpdateStudyReplaceProps` deletes and reinserts every study property. Neither takes a lock on the study. `patchStudy` (`StudyPatch`) and the dataset-to-study conversion in `DatasetService` never write the asset keys.
- Reads go through `StudyAssets.findAssetList` (`/api/dataset/study/{studyId}/assets/models` and `/workspaces`), `SchemaFromStudy` (registration GET) and `StudyAssets.assemble` (`ElasticSearchService.toStudyTerm`). There is no biospecimen endpoint; the Data Library reads biospecimens from the index only. The index has no explicit mapping, so `study.assets.*` field types come from the first document indexed.
- `Biospecimen.yaml` already stores post-mortem interval as `{value, unit}`, which the Data Library range filter reads.

### The change

1. Add `study_model`, `study_workspace` and `study_biospecimen` tables with a serial primary key.
2. On every registration write, match each incoming asset to its row by id, update or insert it, retire rows whose asset was removed, and write the assigned ids back into the registration JSON.
3. Convert existing registrations once: create a row for every asset and write its id into the JSON.
4. duos-ui stops generating ids, sends the server's id back on edit, and then drops the position fallback.
5. The OpenAPI id fields allow `integer` or `string` when the sync ships, after API users are told, and become `integer` once the conversion has run.

Nothing reads the tables yet. They give DT-4209 a stable key to hang DUOS identifiers on.

## Decisions

| # | Question | Decision | Source |
|---|---|---|---|
| 1 | Biospecimen granularity: one row per vial, or a collection-level grouping? | One row per vial or sample. A later `study_biospecimen_collection` parent stays possible. | Jonathan Lawson, 2026-10-19 (ticket Q1) |
| 2 | How do we tell an edited asset from a removed-and-replaced one? | The server-assigned id round-trips. An entry that comes back with its id is the same asset. See [Sync key](#sync-key). | This plan (ticket Q2) |
| 3 | Delete or retire a removed asset? What about requests? | Retire (`retired_date`), never delete, so DT-4209's catalog can still resolve it. A retired row is never revived. Requests cannot target these assets yet; DT-4209 and later epics own that. | This plan (ticket Q3) |
| 4 | Missing or malformed asset data during conversion | Never fail the run. A non-object element is skipped; a field of the wrong type is stored as null. Both keep their original JSON and are listed in the conversion report. | This plan (ticket Q4) |
| 5 | How many assets exist in production? | Run the Task 6 inventory query against production before Task 6 merges, and record the numbers in its PR. | **Kevin Marete** (ticket Q5) |
| 6 | Do exports, bookmarks or links rely on today's position-based ids? | No. The ids are used only inside duos-ui, and nothing in `consent` or `duos-ui` relies on their position. | Kevin Marete, 2026-10-19 (ticket Q6) |
| 7 | When do the rows become authoritative, and who owns it? | Deferred until after DT-4209. | **Jonathan Lawson** (ticket Q7) |
| 8 | How are biospecimens added and edited, and can that path send ids back? | Like every other asset type, through the registration form and the API. The form has no biospecimen editor yet (`BiospecimenAddEdit` is a placeholder that redirects to the Data Library), so a follow-up ticket adds one, sending ids back as Task 5 does for models and workspaces. API callers send ids back as the OpenAPI contract describes. | Jonathan Lawson, 2026-10-19 |
| 9 | One generic asset table, or one per type? | One per type. The biospecimen fields share almost nothing with the others, and DT-4209 adds the cross-type catalog on top. | This plan |
| 10 | Where does the conversion run? | A Liquibase changeset in SQL, following the 2026-09-02 promotion, so it runs once per environment with the deploy and needs no admin step. It runs in the write-frozen maintenance window described under [Rollback](#rollback); it also locks `study_property` for its duration, so a write the freeze misses waits for the conversion instead of racing it. | This plan |
| 11 | When do API responses change type? | When Task 3 deploys, the asset endpoints and registration GET start returning integer ids for every study written from then on, and all studies after Task 6. The API-users notice goes out before Task 3's release and covers both steps. Task 4 documents `integer` or `string` in that release; Task 6 narrows it to `integer`. | This plan, per `CONTRIBUTING.md` "Breaking API changes" |

### Sync key

The server assigns the key. Alternatives considered and rejected:

- **Client-generated UUID as sync key, numeric key shown.** Two identifiers per asset, uniqueness depends on the client, and models already all share the empty id.
- **Server-assigned UUID.** Breaks the ticket's "numeric everywhere" requirement. DT-4209's `DUOS-M-000045` identifiers already give a stable human-facing form.
- **Natural key from content** (name + url, or donor + type + date). An edit to a key field reads as remove-and-add, which is the ambiguity Decision 2 removes.
- **Replace all rows on every write.** Ids change on every edit, so there is no stable identity.

## Task summary

| # | Repo | Task | Size (est.) | Depends on |
|---|---|---|---|---|
| 1 | consent | Create the asset record tables and DAO | ~350 lines | — |
| 2 | duos-ui | Remove `biospecimenId` from the Data Library search fields | ~30 lines | — |
| 3 | consent | Sync asset records on registration create and update | ~450 lines | 1; 2 deployed; DT-4298 merged; Decision 11; short-id count is zero; id fields mapped in every environment |
| 4 | consent | Document the asset id fields as `integer` or `string` in OpenAPI | ~100 lines | Ships in the same release as 3; DT-4299 merged |
| 5 | duos-ui | Send server-assigned asset ids | ~250 lines | 3 deployed; DT-4302 merged |
| 6 | consent | Convert existing registrations | ~400 lines | 3 deployed; Decision 5 |
| 7 | duos-ui | Drop the position-based id fallback | ~100 lines | 6 run in production |

Sizes include tests. Tasks are numbered in deploy order. **Task 2 deploys before Task 3**, because Task 3 starts indexing numeric biospecimen ids and a numeric field in the shared search query fails. **The id fields are mapped explicitly before Task 3 deploys** (see Task 3), because until Task 6 runs the index holds numeric and string ids side by side. **Task 3 deploys before Task 6 runs**, so no registration write can land after the conversion and leave an asset without a row.

## Tasks

### Task 1: Create the asset record tables and DAO (consent)

Add the storage. Nothing calls it yet.

- Liquibase changeset `changelog-consent-<date>-study-asset-records.xml`, included at the end of `changelog-master.xml`, creates three tables:
  - `study_model`: `model_id SERIAL PRIMARY KEY`, `name`, `description`, `url`, `cloud TEXT[]`, `format`, `license`, `trained_on_datasets TEXT[]`, `maintainer JSONB`, `tags TEXT[]`, `access_management`, `data_use JSONB`.
  - `study_workspace`: `workspace_id SERIAL PRIMARY KEY`, `name`, `platform`, `url`, `cloud TEXT[]`, `description`, `tools TEXT[]`, `access`, `tags TEXT[]`, `access_management`, `data_use JSONB`.
  - `study_biospecimen`: `biospecimen_id SERIAL PRIMARY KEY`, `donor_id`, `specimen_type`, `preservation_method`, `preservation_details`, `date_of_collection`, `post_mortem_interval_value NUMERIC`, `post_mortem_interval_unit`, `sex`, `age NUMERIC`, `race`, `country_of_origin`, `extracted_diagnoses TEXT[]`, `pathology`, `organization`, `source_site`, `optional_data_use`.
  - Every table also has `study_id INTEGER NOT NULL` with a foreign key to `study(study_id)` `ON DELETE CASCADE` (as `study_comment` does), `legacy_key TEXT`, `create_date`, `update_date`, a nullable `retired_date`, an index on `study_id`, and a unique index on `(study_id, legacy_key)` where `legacy_key` is not null. The unique index covers retired rows too, so a legacy id is never reused.
- Columns mirror `AiModel.yaml`, `Workspace.yaml` and `Biospecimen.yaml`. Enum fields are text, because models and workspaces are stored as sent today and a new enum value must not need a migration. `access_management` and `data_use` stay null until DT-3810 ships.
- `StudyAssetRecordDAO` (Jdbi SQL object, one contiguous text block per statement), per type:
  - `insert…` returning the new id.
  - `update…(id, studyId, …)` returning the updated row count. Its `WHERE` checks `study_id` and `retired_date IS NULL`, so an id from another study, or a retired row, changes nothing.
  - `findLive(studyId)` returning id and `legacy_key`, and `findUsedLegacyKeys(studyId)` over live and retired rows.
  - `retireExcept(studyId, keepIds)`, which sets `retired_date` on live rows not in `keepIds`.
- Row records and mappers for the three types, following the existing mapper pattern.

**Tests** (`StudyAssetRecordDAOTest`, extends `DAOTestHelper`):
- Insert, update and retire work for each type.
- An update with another study's id, or a retired row's id, changes no row and returns 0.
- `legacy_key` is unique per study, including against retired rows, and the same key is allowed in two studies.
- Deleting the study removes its rows.
- Arrays, `maintainer`, `data_use` and post-mortem value and unit round-trip.

### Task 2: Remove `biospecimenId` from the Data Library search fields (duos-ui)

- `biospecimenAsset.ts` drops `study.assets.biospecimens.biospecimenId` from `searchFields`, which feed the shared `ALL_SEARCH_FIELDS` that every tab's `phrase_prefix` query uses. Searching by an internal id is not a user feature. Once Task 3 indexes numeric ids, a fresh or never-populated field is mapped as a number, and a `phrase_prefix` query on it fails.

**Tests** (vitest): `ALL_SEARCH_FIELDS` no longer includes `biospecimenId`, and the biospecimen tab still searches its other fields.

### Task 3: Sync asset records on registration create and update (consent)

This is the core of the ticket.

- Before deploy, count production elements whose `modelId`, `workspaceId` or `biospecimenId` is 1 to 10 digits. No server ids exist yet, and the UI has only ever produced UUIDs, 13-digit `Date.now()` values and empty strings, so the expected count is zero. If it is not, stop and decide: once this task assigns row ids, a stale form holding such an id would update whichever asset has that row id. The query reads promoted lists and lists still inside a legacy `assets` object, and filters each value's type in a `MATERIALIZED` CTE before any cast or `jsonb_each` call:

  ```sql
  WITH asset_rows AS MATERIALIZED (
    SELECT lower(sp.key) AS key, sp.value::jsonb AS value
    FROM study_property sp
    WHERE lower(sp.key) IN ('models', 'workspaces', 'biospecimens', 'assets')
      AND pg_input_is_valid(sp.value, 'jsonb')
  ),
  legacy_objects AS MATERIALIZED (
    SELECT value FROM asset_rows
    WHERE key = 'assets' AND jsonb_typeof(value) = 'object'
  ),
  legacy_lists AS MATERIALIZED (
    SELECT lower(o.k) AS key, o.v AS value
    FROM legacy_objects, jsonb_each(legacy_objects.value) AS o(k, v)
  ),
  lists AS MATERIALIZED (
    SELECT key, value FROM asset_rows
    WHERE key <> 'assets' AND jsonb_typeof(value) = 'array'
    UNION ALL
    SELECT key, value FROM legacy_lists
    WHERE key IN ('models', 'workspaces', 'biospecimens') AND jsonb_typeof(value) = 'array'
  ),
  elements AS MATERIALIZED (
    SELECT lists.key, e.element
    FROM lists, jsonb_array_elements(lists.value) AS e(element)
  )
  SELECT key, count(*) AS short_ids
  FROM elements
  WHERE jsonb_typeof(element) = 'object'
    AND coalesce(element ->> 'modelId', element ->> 'workspaceId', element ->> 'biospecimenId') ~ '^[0-9]{1,10}$'
  GROUP BY key;
  ```

- Also before deploy, map the three id fields explicitly in every environment. The `dataset` index has no explicit mapping, and from this task until Task 6 runs it holds numeric ids for studies written since and string ids for the rest. If a new or rebuilt index first sees a numeric id, it maps the field as a number, and every dataset document whose study still has a UUID id fails to index. Add `study.assets.models.modelId`, `study.assets.workspaces.workspaceId` and `study.assets.biospecimens.biospecimenId` as `text` with a `keyword` subfield, the type dynamic mapping already gives a string, so existing queries behave the same:
  - in an index template matching the `dataset` index, so a rebuilt index gets it;
  - with `PUT dataset/_mapping` on each live index where a field is absent. Where a field is already numeric, the mapping cannot be changed in place: recreate the index from the template and reindex first.
  - Record each environment's mapping before and after in the PR.

- New `StudyAssetRecordSync`, a plain class like the composites in `service/dao`. `DatasetServiceDAO.insertDatasetRegistration` calls it after `executeInsertStudy`, and `updateStudy` calls it after `executeUpdateStudyReplaceProps`, with the same handle. A failure rolls back the whole registration write, as a dataset failure does.
- On update, two concurrent edits of one study already run one after the other: `executeUpdateStudy` updates the `study` row before replacing the properties, and the second edit waits on that row lock until the first commits. The sync runs after that update in the same transaction, so it needs no lock of its own. A test pins the ordering, because moving the sync before the study update would let one edit's retire miss the other's uncommitted inserts and leave rows live with no entry.
- For each type, it loads the study's live rows and used legacy keys, then reads the list the write just stored and applies this table to each entry in order. Each row can be claimed by at most one entry per write.

  | Incoming entry | Action |
  | --- | --- |
  | Id is a whole number from 1 to 2147483647 (as a JSON number or a digit string) and matches an unclaimed live row of this study | Update and claim that row |
  | Id is any other non-blank value and matches the `legacy_key` of an unclaimed live row | Update and claim that row. Covers a form opened before the conversion |
  | Anything else: no id, a blank id, a claimed or retired row's id, another study's id, or a number out of range | Insert a row. Keep a non-blank, non-integer id as `legacy_key` only if the study has never used it; otherwise `legacy_key` is null |
  | Live row not claimed by any entry | Retire the row |

- It mirrors what the write stored. When the write stores no property for a type, every live row of that type is retired; the update path deletes and reinserts all properties, so an absent list is a removed list.
- It then sets each entry's id key to its row id and updates the `study_property` value. It works on the entries as parsed by `GsonUtil` and changes only the id key. It never maps an entry to a typed class, which would drop unknown client keys. A non-object element is left as it is and gets no row.
- A field of the wrong type for its column (e.g. a non-numeric `age`) is stored as null and logged. The JSON keeps the original.
- The search index picks up the ids with no new call: create already runs `indexDatasets` (`DatasetRegistrationService`), and update runs `indexStudy` (`StudyResource`). `SchemaFromStudy` and the asset endpoints read the JSON, so they return the ids with no change.
- `StudyAssetRecordSync` exposes `syncStudy(handle, studyId)`, which reads the stored lists and applies the same rules. [Rollback](#rollback) uses it to reconcile studies edited while the sync was off.

**Tests** (`DatasetServiceDAOTest`, and `DatasetRegistrationServiceTest` where a service seam is needed):
- Create assigns an id to every entry and writes it into the stored JSON.
- Two models with `modelId: ''` get two rows and two different ids.
- Update with ids updates rows in place. Reordering the list keeps every id.
- Removing an entry retires its row. Sending an empty list, or no list, retires all rows of that type.
- An entry with another study's id, a retired row's id, or a 13-digit number gets a new row.
- Two entries with the same id, integer or UUID: the first claims the row, the second gets a new row with a null `legacy_key`.
- A UUID matching a live row's `legacy_key` updates that row.
- Unknown client keys and non-object elements survive the write-back.
- A sync failure rolls back the study and dataset writes.
- Two concurrent updates of one study leave exactly one live row per entry of the last write.
- A dataset document whose study has a UUID id and one whose study has an integer id both index against the explicit mapping.
- `syncStudy` on a study whose JSON gained, lost and changed entries outside the sync inserts, retires and updates to match. On an unconverted study it gives every entry a row, as Task 6 would.

### Task 4: Document the asset id fields as `integer` or `string` in OpenAPI (consent)

- `AiModel.yaml`, `Workspace.yaml` and `Biospecimen.yaml`: the id fields become `oneOf: [integer, string]`, described as server-assigned. Studies not written since Task 3 still return their client strings until Task 6 converts them, so the contract cannot narrow yet. Task 6 narrows it to `integer`; input keeps accepting a string.
- Ships in the same release as Task 3, which is when responses change (Decision 11). Before that release, check in with Comms and send the notice to `api-users@firecloud.org`, as `CONTRIBUTING.md` "Breaking API changes" requires.

**Tests:** the OpenAPI bundle builds, and the existing registration round-trip tests pass with integer and string ids.

### Task 5: Send server-assigned asset ids (duos-ui)

- `src/types/model.ts`: `modelId`, `workspaceId` and `biospecimenId` become `number | string`, optional before the first save. Studies not written since Task 3 still return their client strings until Task 6 converts them; Task 7 narrows the type to `number`.
- `AiModelAddEdit.tsx` and `WorkspaceAddEdit.tsx` stop generating ids. A new entry is sent without one; an edited entry keeps the id it was loaded with.
- `AiModelList`, `WorkspaceList` and `BiospecimenList` key rows by `getItemKey`. An entry with no id yet (unsaved, or a biospecimen loaded before the conversion) gets a client-only key that is never sent.
- `studyToDatasetSchemaSubmission` sends ids back unchanged, including biospecimens, which `BiospecimenList` passes through read-only.

**Tests** (vitest):
- Editing an entry keeps its id. A new entry is submitted without an id.
- Two unsaved entries have distinct row keys, in all three lists.

### Task 6: Convert existing registrations (consent)

- Before merge, run the inventory query in production and put the numbers in the PR (Decision 5). It matches keys ignoring case, as `StudyAssets` does, and casts only values that pass `pg_input_is_valid`, as `StudyRecommendationDAO` does:

  ```sql
  WITH asset_rows AS MATERIALIZED (
    SELECT sp.study_id, lower(sp.key) AS key, sp.value
    FROM study_property sp
    WHERE lower(sp.key) IN ('models', 'workspaces', 'biospecimens', 'assets')
      AND pg_input_is_valid(sp.value, 'jsonb')
  )
  SELECT key, count(DISTINCT study_id) AS studies,
         sum(CASE WHEN jsonb_typeof(value::jsonb) = 'array' THEN jsonb_array_length(value::jsonb) END) AS assets,
         count(*) FILTER (WHERE jsonb_typeof(value::jsonb) = 'object') AS legacy_objects
  FROM asset_rows
  GROUP BY key;
  ```

  If any list is still under a differently cased key or inside a legacy `assets` object, the changeset first promotes it with the 2026-09-02 SQL extended to ignore case, keeping that changeset's precedence (an existing promoted property wins). If none is, record that in the PR and leave the step out.
- Liquibase changeset `changelog-consent-<date>-study-asset-records-convert.xml`. It starts with `LOCK TABLE study_property IN SHARE ROW EXCLUSIVE MODE` (Decision 10) and uses `MATERIALIZED` CTEs that filter on key, `pg_input_is_valid` and `jsonb_typeof = 'array'` before any cast or `jsonb_array_elements` call. For each type, over `jsonb_array_elements … WITH ORDINALITY`:
  1. Skip any element that is not a JSON object.
  2. Read the element's id as text only. Never cast it: a UUID or a 13-digit `Date.now()` id would abort the changeset.
  3. If the id is all digits, at most 10 characters, and equals `row_id::text` of a live row of its study that no earlier element in the list has claimed, keep it and claim the row. This covers studies already written by Task 3 and makes a re-run change nothing. A client id that only looks like a row id cannot match, because a study's JSON holds row ids only after Task 3 or this changeset rewrote it.
  4. Otherwise, take a new id with `nextval` in a `MATERIALIZED` CTE keyed by study, type and position, and insert the row with that id. `legacy_key` is the element's id when it is non-blank, unique within the study's list, and unused by the study's rows, and null otherwise. No synthetic keys are generated, so nothing can collide with a client id.
  5. Rebuild the array in the same order from that CTE, setting each element's id with `jsonb_set`. Every other key is kept.
- After deploy, reindex, since each dataset document embeds its study's assets. Reindex only the datasets already in the index (`indexed_date IS NOT NULL`), through `POST /api/dataset/index/{datasetId}` for each: the bulk `POST /api/dataset/index` indexes every dataset and so re-adds any an admin removed with `DELETE /api/dataset/index/{datasetId}`. The id keys keep their names, so the enumerated `study.assets.*` paths in `es-access-contract.md` §B.5a are unchanged.
- Commit a report query beside the changeset: per type, the elements in the JSON, rows created, ids kept, and elements skipped with study id and position. Run it before and after in each environment.
- In the same PR, narrow the three OpenAPI id fields from Task 4 to `integer` for responses, since every converted entry now has a row id.
- Run [Staging verification](#staging-verification) before the production deploy.

**Tests** (`StudyAssetRecordsMigrationTest`, following `StudyAssetsMigrationTest`, which reads the SQL out of the shipped changeset and runs it against seeded rows):
- Every object element gets a row, and its id is written into the JSON.
- Element order and unknown keys are preserved.
- Empty, duplicate, UUID and 13-digit ids all convert without error; duplicates and blanks get a null `legacy_key`.
- A client id that looks like an integer but matches no live row of its study gets a new row. Two elements with the same row id: the first keeps it, the second gets a new row.
- A study already written by Task 3 keeps every id.
- Non-object elements, non-array values and malformed JSON are left untouched and counted.
- A second run changes nothing.
- A list under a differently cased key is promoted and converted.
- Studies with no asset properties are untouched.

### Task 7: Drop the position-based id fallback (duos-ui)

- `modelAsset.ts`, `workspaceAsset.ts` and `biospecimenAsset.ts` drop the `${studyId}-${index}` fallback.
- `src/types/model.ts` narrows the three id fields from Task 5 to `number`, since every converted entry now has a row id.
- The conversion leaves non-object elements in place without an id, so the three transforms skip any element that is not an object rather than rendering a row with an undefined id.

**Tests** (vitest): rows use the numeric id, the type check passes with the narrowed fields, and a non-object element in the index response renders no row, for all three types.

## Edge cases to cover in review

- A form opened before the conversion submits UUIDs. The `legacy_key` match updates those rows. Models had empty ids, so they have no `legacy_key`: a stale form retires and recreates them, which costs their ids but loses no data.
- A stale form holding a pre-conversion id of 1 to 10 digits would match another asset's row id. The UI has never produced one, and Task 3 does not deploy if production holds any.
- A `Date.now()` id is a 13-digit number, outside the integer range. Task 3 checks the range before matching, and Task 6 never casts it.
- An id copied from another study, or from a retired row, never updates that row. It creates a new row in the study being written.
- A study is deleted: its rows go with it, by cascade.
- Two concurrent edits of one study run one after the other, because the update writes the study row before the sync runs. The last write wins, as it does for every other study property today.
- A registration write during the conversion waits on the `study_property` lock, then runs Task 3's sync against converted data.
- The write-back must not touch any key except the id, including keys the backend does not know.

## Staging verification

Before the production run of Task 6, restore a production snapshot to staging, run the conversion, and:

- compare each row field by field with the element it came from;
- compare the Data Library's model, workspace and biospecimen tabs for a sample of studies, before and after, including a search on every tab;
- read the `dataset` index mapping for `study.assets.*Id` in each environment, and confirm it is the explicit `text` mapping added before Task 3;
- check that the report's skipped elements are all genuinely malformed.

Zero unexplained differences is the gate for production.

## Rollback

Keep the tables in every case. They hold the only mapping from assigned ids to assets, and dropping them would lose it if the work is redeployed.

- **Before Task 5 deploys:** revert Tasks 3 and 4 together. Ids already written into the JSON do no harm, because the old code treats ids as opaque strings.
- **After Task 5 deploys:** revert Task 5 (and Task 7, if it shipped) first, so the UI generates ids and has its fallback again, then revert Tasks 3 and 4.
- **Index mapping:** the explicit `text` mapping added before Task 3 accepts both a reverted UI's UUIDs and integer ids, so a rollback needs no index change. Confirm at rollback that each environment still has it, and that a workspace created from the reverted UI indexes.
- **Restoring data:** the deploy that carries Task 6 runs in a maintenance window with every study write frozen (registration create and update, `patchStudyById`, `deleteStudyById`, `PUT /api/dataset/study/{studyId}/custodians`, `PUT /api/dataset/study/convert/{datasetIdentifier}`, which can create a study, and dataset deletes through `DELETE /api/dataset/{datasetId}`, since DT-4209's catalog references datasets too), drained before the backup and held until the post-conversion report is checked. At its start, back up `study_property` and the three asset tables in one consistent snapshot (one `pg_dump` run), since Liquibase runs the conversion with that deploy. A restore is valid only inside that window, because no study has been created, edited or deleted since the backup. Restore the four tables together or not at all; restoring the JSON alone leaves rows that no longer match it. Once DT-4209's catalog has shipped, the same `pg_dump` run includes `asset_catalog`, and the restore follows `asset-identifiers-plan.md` Rollback, since deleting the asset rows nulls the catalog's references. After the window closes, recover with `syncStudy` over the current JSON instead of restoring. The snapshot predates the conversion, and Liquibase will not run the changeset again, so after a restore run `StudyAssetRecordSync.syncStudy` for every study, then reindex as in Task 6, before Task 7 is redeployed.
- **Redeploying after a rollback:** registrations created or edited while Task 3 was reverted changed their JSON without touching the rows. Redeploy Tasks 3 and 4, then run `syncStudy` for every study whose `create_date` or `update_date` is after the rollback (`insertStudy` leaves `update_date` null, so `update_date` alone misses new studies), and reindex.

## Out of scope (follow-up)

- Making the rows the source of truth instead of the registration (Decision 7).
- DUOS identifiers and the cross-type catalog (DT-4209).
- Governance, search or request changes.
- Adding and editing biospecimens in the registration form (Decision 8).
- Data use on biospecimens. DT-3810 leaves it out until its free-text `optionalDataUse` field is decided.

## Deploy order

1. DT-4298 and DT-4299 (DT-3810 validation and OpenAPI) merge first. They are smaller and need no migration, and Task 3 then copies their fields from the start.
2. Comms check-in and the API-users notice go out.
3. Task 1 and Task 2 deploy.
4. Tasks 3 and 4 deploy in one release.
5. Task 5 deploys.
6. Staging verification. Then, in a maintenance window with every study write frozen, the backup, the deploy carrying Task 6, the report check and the reindex.
7. Task 7 deploys.

## Overlap with DT-3810

`asset-data-use-plan.md` adds `dataUse` and `accessManagement` to model and workspace entries, and it rejected new tables for that purpose. That decision still holds. The tables here exist for identity, the registration JSON stays the source of truth, and DT-3810's `AssetDataUse` module keeps validating and enriching the JSON entries unchanged. This plan follows two of its rules:

- The model and workspace tables copy `dataUse` and `accessManagement` as stored. The computed `dataUseSummary` is never stored; `AssetDataUse.prepare` strips it before persistence.
- Entries are never round-tripped through a typed class. The id write-back changes only the id key.

Three DT-4297 tickets touch the same files as this plan:

| DT-3810 ticket | Shared files | Task here |
|---|---|---|
| DT-4298: validate model and workspace entries | `RegistrationRequestMapper.promotedAsset`, the registration write path | Task 3 |
| DT-4299: OpenAPI fields | `AiModel.yaml`, `Workspace.yaml` | Task 4 |
| DT-4302: registration form fields | `AiModelList`, `WorkspaceList`, their add/edit forms | Task 5 |
