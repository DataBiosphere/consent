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
| 3 | Delete or retire a removed asset? What about requests? | Retire (`retired_date`), never delete, so DT-4209's catalog can still resolve it. A retired row is never revived. The one exception is deleting the study, which deletes its rows by cascade; DT-4209's study delete path marks their catalog entries removed first. Requests cannot target these assets yet; DT-4209 and later epics own that. | This plan (ticket Q3) |
| 4 | Missing or malformed asset data during conversion | Never fail the run. A non-object element is skipped; a field of the wrong type is stored as null. Both keep their original JSON; the report query counts non-object elements. | This plan (ticket Q4) |
| 5 | How many assets exist in production? | Run the Task 6 inventory query against production before Task 3 deploys, to set the list cap, and again before the Task 6 run in each environment; record the numbers in each PR. | **Kevin Marete** (ticket Q5) |
| 6 | Do exports, bookmarks or links rely on today's position-based ids? | No. The ids are used only inside duos-ui, and nothing in `consent` or `duos-ui` relies on their position. | Kevin Marete, 2026-10-19 (ticket Q6) |
| 7 | When do the rows become authoritative, and who owns it? | Deferred until after DT-4209. | **Jonathan Lawson** (ticket Q7) |
| 8 | How are biospecimens added and edited, and can that path send ids back? | Like every other asset type, through the registration form and the API. The form has no biospecimen editor yet (`BiospecimenAddEdit` is a placeholder that redirects to the Data Library), so a follow-up ticket adds one, sending ids back as Task 5 does for models and workspaces. API callers send ids back as the OpenAPI contract describes. | Jonathan Lawson, 2026-10-19 |
| 9 | One generic asset table, or one per type? | One per type. The biospecimen fields share almost nothing with the others, and DT-4209 adds the cross-type catalog on top. | This plan |
| 10 | Where does the conversion run? | An admin runs `syncStudy` over every study with DT-4378's bulk call, once per environment after Task 3 deploys. It was first planned as a SQL Liquibase changeset, which would have been a second implementation of every Task 3 rule. Each study syncs in its own transaction under the study row lock, so registration writes need no freeze for correctness; the run still takes a backup first (see [Rollback](#rollback)). | Kevin Marete, 2026-10-30 |
| 11 | When do API responses change type? | When Task 3 deploys, the asset endpoints and registration GET start returning integer ids for every study written from then on, and all studies after Task 6. The API-users notice goes out before Task 3's release and covers both steps and the list cap. Task 4 documents `integer` or `string` in that release; Task 6 documents that responses hold an integer. | This plan, per `CONTRIBUTING.md` "Breaking API changes" |
| 12 | Where does a biospecimen's own id go once `biospecimenId` holds the row id? | Into `sampleId`. Biospecimens arrive with whatever id the DUOS-team path supplies, which may be a real sample id, and the Data Library shows it in its "Biospecimen ID" column. When an entry's `sampleId` is absent, null or blank, the sync and the conversion copy its original `biospecimenId` into `sampleId` as a JSON string, if that id is a non-blank string or a whole number (written as plain digits) and is not in row-id form. An entry that claims its row with no `sampleId` key keeps the row's `sample_id`; an explicit blank `sampleId` clears it (the write path drops null values, so null reads as absent). A biospecimen whose `biospecimenId` is in row-id form but claims no row of the study, and that has no `sampleId`, is rejected with a 400 asking for the sample id in `sampleId`, so a numeric sample id is never silently replaced. The deploy check proves no client id is in row-id form before Task 3; from then on the OpenAPI contract and the API-users notice say `biospecimenId` is server-assigned and a sample id goes in `sampleId`. The column and search read `sampleId`. | Kevin Marete, 2026-10-20 (review) |

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
| 2 | duos-ui | Search and show the biospecimen `sampleId` | ~80 lines | id and `sampleId` fields mapped and §B.5a updated (Task 3's first steps) |
| 3 | consent | Sync asset records on registration create and update | ~450 lines | 1; 2 deployed; DT-4323 deployed; DT-4298 merged; Decisions 5 and 11; short-id count is zero |
| 4 | consent | Document the asset id fields as `integer` or `string` in OpenAPI | ~100 lines | Ships in the same release as 3; DT-4299 merged |
| 5 | duos-ui | Send server-assigned asset ids | ~250 lines | 3 deployed; DT-4302 merged |
| 6 | consent | Convert existing registrations | ~100 lines, plus the run | 3 and DT-4378 deployed; Decision 5 |
| 7 | duos-ui | Drop the position-based id fallback | ~100 lines | 6 run in production |

Sizes include tests. Tasks are numbered in deploy order. **Task 2 deploys before Task 3**, because Task 3 starts replacing the `biospecimenId` the Data Library shows with a row id, and moves the original into `sampleId`. **The id and `sampleId` fields are mapped explicitly before Task 2 deploys** (see Task 3), because until Task 6 runs the index holds numeric and string ids side by side. **Task 3 deploys before Task 6 runs**, so no registration write can land after the conversion and leave an asset without a row. **[DT-4323](https://broadworkbench.atlassian.net/browse/DT-4323) deploys before Task 3**: the duos-ui edit form loads only the legacy `assets` property, so every UI edit submits no asset lists, and Task 3 would retire every row of the study.

## Tasks

### Task 1: Create the asset record tables and DAO (consent)

Add the storage. Nothing calls it yet.

- Liquibase changeset `changelog-consent-<date>-study-asset-records.xml`, included at the end of `changelog-master.xml`, creates three tables:
  - `study_model`: `model_id SERIAL PRIMARY KEY`, `name`, `description`, `url`, `cloud TEXT[]`, `format`, `license`, `trained_on_datasets TEXT[]`, `maintainer JSONB`, `tags TEXT[]`, `access_management`, `data_use JSONB`.
  - `study_workspace`: `workspace_id SERIAL PRIMARY KEY`, `name`, `platform`, `url`, `cloud TEXT[]`, `description`, `tools TEXT[]`, `access`, `tags TEXT[]`, `access_management`, `data_use JSONB`.
  - `study_biospecimen`: `biospecimen_id SERIAL PRIMARY KEY`, `sample_id`, `donor_id`, `specimen_type`, `preservation_method`, `preservation_details`, `date_of_collection`, `post_mortem_interval_value NUMERIC`, `post_mortem_interval_unit`, `sex`, `age NUMERIC`, `race`, `country_of_origin`, `extracted_diagnoses TEXT[]`, `pathology`, `organization`, `source_site`, `optional_data_use`.
  - Every table also has `study_id INTEGER NOT NULL` with a foreign key to `study(study_id)` `ON DELETE CASCADE` (as `study_comment` does), `legacy_key TEXT`, `create_date`, `update_date`, a nullable `retired_date`, an index on `study_id`, and a unique index on `(study_id, legacy_key)` where `legacy_key` is not null. The unique index covers retired rows too, so a legacy id is never reused.
- Columns mirror `AiModel.yaml`, `Workspace.yaml` and `Biospecimen.yaml`. Enum fields are text, because models and workspaces are stored as sent today and a new enum value must not need a migration. `access_management` and `data_use` stay null until DT-3810 ships.
- `StudyAssetRecordDAO` (Jdbi SQL object, one contiguous text block per statement), per type:
  - `insert…`, an `@SqlBatch` with `@GetGeneratedKeys` returning the new ids in input order.
  - `update…(id, studyId, …)`, an `@SqlBatch` returning each row's updated count. Its `WHERE` checks `study_id` and `retired_date IS NULL`, so an id from another study, or a retired row, changes nothing.
  - `findLive(studyId)` returning id and `legacy_key`, and `findUsedLegacyKeys(studyId)` over live and retired rows.
  - `retireExcept(studyId, keepIds)`, which sets `retired_date` on live rows not in `keepIds`.
- The same changeset creates the row-id form test (defined in Task 3) as an immutable SQL function:

  ```sql
  CREATE FUNCTION asset_id_in_row_form(id jsonb) RETURNS boolean
  LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE jsonb_typeof(id)
      WHEN 'string' THEN CASE WHEN id #>> '{}' ~ '^[1-9][0-9]{0,9}$'
                              THEN (id #>> '{}')::bigint <= 2147483647
                              ELSE false END
      WHEN 'number' THEN id::numeric BETWEEN 1 AND 2147483647 AND id::numeric = trunc(id::numeric)
      ELSE false
    END
  $$;
  ```
- Row records and mappers for the three types, following the existing mapper pattern.

**Tests** (`StudyAssetRecordDAOTest`, extends `DAOTestHelper`):
- Insert, update and retire work for each type.
- An update with another study's id, or a retired row's id, changes no row and returns 0.
- `legacy_key` is unique per study, including against retired rows, and the same key is allowed in two studies.
- Deleting the study removes its rows.
- Arrays, `maintainer`, `data_use` and post-mortem value and unit round-trip.
- `asset_id_in_row_form` is true for `7`, `7.0`, `"7"` and `"2147483647"`, and false for `"007"`, `"00000000001"`, `7.5`, `2147483648`, `"3000000000"`, a UUID, `""` and null.

### Task 2: Search and show the biospecimen `sampleId` (duos-ui)

- `biospecimenAsset.ts` replaces `study.assets.biospecimens.biospecimenId` with `study.assets.biospecimens.sampleId` in `searchFields`, which feed the shared `ALL_SEARCH_FIELDS` that every tab's `phrase_prefix` query uses. Once Task 3 writes row ids, a prefix search on `biospecimenId` would match unrelated studies by serial id. Until a study is next written or Task 6 runs, its biospecimens have no `sampleId`, so their original ids are not searchable in that window; this is accepted.
- `Biospecimen` in `src/types/model.ts` gains `sampleId?: string`. `transformResponse` sets each row's `sampleId` from the indexed `sampleId`, falling back to the indexed (not synthesized) `biospecimenId` when it is not in row-id form; Task 7 removes the fallback. The "Biospecimen ID" column's `field` becomes `sampleId`, so sorting and the quick filter use the value shown.

**Tests** (vitest): `ALL_SEARCH_FIELDS` includes `sampleId` and not `biospecimenId`, and a row built by `transformResponse` shows `sampleId` when present, a string or numeric original `biospecimenId` otherwise, and nothing for a row id or a missing id.

### Task 3: Sync asset records on registration create and update (consent)

This is the core of the ticket.

- Before deploy, count production elements whose own id field (`modelId` in `models`, and so on) is in row-id form (defined below the table). No server ids exist yet, and the UI has only ever produced UUIDs, 13-digit `Date.now()` values and empty strings, so the expected count is zero. If it is not, stop and decide: once this task assigns row ids, a stale form holding such an id would update whichever asset has that row id. The query reads promoted lists and lists still inside a legacy `assets` object, and filters each value's type in a `MATERIALIZED` CTE before any cast or `jsonb_each` call:

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
    SELECT lists.key, e.element,
           CASE lists.key WHEN 'models' THEN 'modelId' WHEN 'workspaces' THEN 'workspaceId' ELSE 'biospecimenId' END AS id_key
    FROM lists, jsonb_array_elements(lists.value) AS e(element)
  )
  SELECT key, count(*) AS short_ids
  FROM elements
  WHERE jsonb_typeof(element) = 'object'
    AND asset_id_in_row_form(element -> id_key)
  GROUP BY key;
  ```

- Before Task 2 deploys, map the three id fields and `sampleId` explicitly in every environment, and add `study.assets.biospecimens.sampleId` to the enumerated paths in `es-access-contract.md` §B.5a so D-3/E-3 serve it. The `dataset` index has no explicit mapping, and from this task until Task 6 runs it holds numeric ids for studies written since and string ids for the rest. If a new or rebuilt index first sees a numeric id, it maps the field as a number, and every dataset document whose study still has a UUID id fails to index. Add `study.assets.models.modelId`, `study.assets.workspaces.workspaceId`, `study.assets.biospecimens.biospecimenId` and `study.assets.biospecimens.sampleId` as `text` with a `keyword` subfield, the type dynamic mapping already gives a string, so existing queries behave the same:
  - in an index template matching the `dataset` index, so a rebuilt index gets it;
  - with `PUT dataset/_mapping` on each live index where a field is absent. Where a field is already numeric, the mapping cannot be changed in place: recreate the index from the template and reindex first.
  - Record each environment's mapping before and after in the PR.

- New `StudyAssetRecordSync`, a plain class like the composites in `service/dao`. `DatasetServiceDAO.insertDatasetRegistration` calls it after `executeInsertStudy`, and `updateStudy` calls it after `executeUpdateStudyReplaceProps`, with the same handle. A failure rolls back the whole registration write, as a dataset failure does.
- On update, two concurrent edits of one study already run one after the other: `executeUpdateStudy` updates the `study` row before replacing the properties, and the second edit waits on that row lock until the first commits. The sync runs after that update in the same transaction, so it needs no lock of its own. A test pins the ordering, because moving the sync before the study update would let one edit's retire miss the other's uncommitted inserts and leave rows live with no entry.
- For each type, it loads the study's live rows and used legacy keys, then reads the list the write just stored and applies this table to each entry in order. Each row can be claimed by at most one entry per write.

  | Incoming entry | Action |
  | --- | --- |
  | Id is in row-id form and matches an unclaimed live row of this study | Update and claim that row |
  | Id is any other non-blank value and matches the `legacy_key` of an unclaimed live row | Update and claim that row. Covers a form opened before the conversion |
  | Anything else: no id, a blank id, a claimed or retired row's id, another study's id, or a number out of range | Insert a row (a biospecimen in row-id form with no `sampleId` is rejected instead; Decision 12). `legacy_key` is the id when it is non-blank, not in row-id form, not repeated in the incoming list, unused by the study's rows, at most 255 code points (`char_length`), and free of NUL and unpaired surrogates, and null otherwise; Task 6 applies the same rule |
  | Live row not claimed by any entry | Retire the row |

  An id is in **row-id form** when it is a JSON number with a whole value from 1 to 2147483647, or a string matching `^[1-9][0-9]{0,9}$` whose value is at most 2147483647. Task 1's changeset creates it as the SQL function `asset_id_in_row_form(jsonb)`, which the deploy check and Task 6's report query call; Task 3's Java helper is tested against the same cases.

- It mirrors what the write stored. When the write stores no property for a type, every live row of that type is retired; the update path deletes and reinserts all properties, so an absent list is a removed list.
- Before overwriting a biospecimen's id, it applies Decision 12's copy rule, which also covers an entry matched by `legacy_key` from a stale form; an entry that claims its row by row id with no `sampleId` key gets the row's `sample_id` written back. A client-sent numeric `sampleId` is written back as a string of its exact JSON text, and the result is stored in `sample_id`.
- It then sets each entry's id key to its row id and updates the `study_property` value. It edits the stored list as a Gson `JsonElement` tree (`JsonParser.parseString`), whose numbers keep their exact text, so no other value is rounded; copied numbers use `getAsBigDecimal().toBigIntegerExact()`, matching Task 6's `trunc(…)::text`. It changes only the id key, plus `sampleId` as above. It never maps an entry to a typed class, which would drop unknown client keys. A non-object element is left as it is and gets no row.
- A field of the wrong type for its column (e.g. a non-numeric `age`) is stored as null and logged. The JSON keeps the original.
- Inserts and updates run as `@SqlBatch` statements and the retire as one `retireExcept`, not one statement per entry, so a large biospecimen list (Decision 1) holds the study row lock briefly. A shared check on `StudyRegistrationRequestValidator`, called from `collectStudyViolations` on create and from `StudyUpdateRequestValidator.validate` on update, rejects a list longer than a cap with a 400. It checks the effective list from `StudyAssets.promotedValue`, so a list sent inside `assets`, as duos-ui sends them, is capped too. The cap is one number for all three types: at least 1,000, and at least ten times the longest production list of any type in the Decision 5 inventory run before this task deploys, so no existing study becomes uneditable.
- The search index picks up the ids with no new call: create already runs `indexDatasets` (`DatasetRegistrationService`), and update runs `indexStudy` (`StudyResource`). `SchemaFromStudy` and the asset endpoints read the JSON, so they return the ids with no change.
- `StudyAssetRecordSync` exposes `syncStudy(handle, studyId)`, which locks the study row with `SELECT … FROM study WHERE study_id = ? FOR UPDATE`, then picks each type's source with `StudyAssets.findAssetList`'s precedence (the promoted key ignoring case, else the legacy `assets` key) and parses that property's raw value with `JsonParser.parseString`, so the copy, the write-back and any move out of `assets` work on the same exact-number tree as a registration write, and applies the same rules. A list found only in the legacy `assets` object, with no property of that key in any case, is first moved to its own property and removed from `assets`, as a registration write does, so the ids are written back where later reads find them. If a promoted property exists but is not an array, that type is skipped and the study logged, as Task 6 does. The lock serializes it with registration writes, which update that row first; without it a write committing between the read and the write-back would be reverted. [Rollback](#rollback) uses it to reconcile studies edited while the sync was off.

**Tests** (`DatasetServiceDAOTest`, and `DatasetRegistrationServiceTest` where a service seam is needed):
- Create assigns an id to every entry and writes it into the stored JSON.
- Two models with `modelId: ''` get two rows and two different ids.
- Update with ids updates rows in place. Reordering the list keeps every id.
- Removing an entry retires its row. Sending an empty list, or no list, retires all rows of that type.
- An entry with another study's id, a retired row's id, or a 13-digit number gets a new row.
- Two entries with the same id, integer or UUID: the first claims the row, the second gets a new row with a null `legacy_key`.
- A UUID matching a live row's `legacy_key` updates that row.
- Unknown client keys and non-object elements survive the write-back.
- A biospecimen sent with a sample id keeps it in `sampleId` and `sample_id`, including one matched by `legacy_key` from a stale form; a numeric one is stored as a string; a non-blank `sampleId` is never overwritten, a blank one is filled from a copyable original id, and an explicit blank on a row-id match clears `sample_id`, sent through the registration mapper on update; an id in row-id form (matched, duplicated, retired or from another study) is never copied.
- Two new entries sharing a UUID both insert, with a null `legacy_key`.
- `"007"` and `7.5` are not in row-id form and get new rows; `7` and `"7"` match row 7.
- A list over the cap is rejected with a 400 on create and on update, at the top level and inside `assets`.
- `syncStudy` on a study whose biospecimens are only in the legacy `assets` object moves them to their own property with ids, and a second run changes nothing. Beside a non-array `biospecimens` property, it changes neither value.
- An entry sent back by row id without `sampleId` keeps the row's sample id. `1700000000000`, `12345678901234567890` and `"S-1"` are copied as `"1700000000000"`, `"12345678901234567890"` and `"S-1"`; `12345678` (row-id form) and `12.5` are not. An unknown key holding `12345678901234567890` is unchanged after the write-back and after `syncStudy`, including on a legacy-only list. A new biospecimen with `biospecimenId: "12345"` and no `sampleId` is rejected with a 400.
- A sync failure rolls back the study and dataset writes.
- Two concurrent updates of one study leave exactly one live row per entry of the last write.
- A dataset document whose study has a UUID id and one whose study has an integer id both index against the explicit mapping.
- `syncStudy` on a study whose JSON gained, lost and changed entries outside the sync inserts, retires and updates to match. On an unconverted study it gives every entry a row, as Task 6 would. A registration update that runs during `syncStudy` waits for it and is not reverted.

### Task 4: Document the asset id fields as `integer` or `string` in OpenAPI (consent)

- `AiModel.yaml`, `Workspace.yaml` and `Biospecimen.yaml`: the id fields become `oneOf: [integer, string]`, described as server-assigned. `Biospecimen.yaml` adds `sampleId` (string, the submitter's identifier for the sample). The registration's `models`, `workspaces` and `biospecimens` list fields get `maxItems` set to the Task 3 cap, and the `assets` description says the cap applies to its lists too. Studies not written since Task 3 still return their client strings until Task 6 converts them, so the contract cannot narrow yet. Task 6 documents that responses hold an integer; input keeps accepting a string.
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

The conversion runs `syncStudy` (Task 3) over every study with the bulk call from [DT-4378](https://broadworkbench.atlassian.net/browse/DT-4378), `POST /api/dataset/study/assets/sync` with no `since`, called again with each summary's `nextAfterStudyId` until it is absent (Decision 10). Every rule a registration write applies, including the copy into `sampleId` and the `legacy_key` rule, applies to existing studies with no second implementation to keep in step.

- Before the maintenance window in each environment, run the inventory query, record the numbers in the PR (Decision 5), and make the hand fixes below; run it again inside the window to confirm nothing changed. It matches keys ignoring case, as `StudyAssets` does, and casts only values that pass `pg_input_is_valid`, as `StudyRecommendationDAO` does:

  ```sql
  WITH asset_rows AS MATERIALIZED (
    SELECT sp.study_id, sp.key AS stored_key, lower(sp.key) AS key, sp.value::jsonb AS value
    FROM study_property sp
    WHERE lower(sp.key) IN ('models', 'workspaces', 'biospecimens', 'assets')
      AND pg_input_is_valid(sp.value, 'jsonb')
  ),
  legacy_objects AS MATERIALIZED (
    SELECT study_id, value FROM asset_rows
    WHERE key = 'assets' AND jsonb_typeof(value) = 'object'
  ),
  legacy_lists AS MATERIALIZED (
    SELECT legacy_objects.study_id, lower(o.k) AS key, o.v AS value
    FROM legacy_objects, jsonb_each(legacy_objects.value) AS o(k, v)
  ),
  lists AS MATERIALIZED (
    SELECT study_id, key, value, false AS legacy, stored_key <> key AS miscased FROM asset_rows
    WHERE key <> 'assets' AND jsonb_typeof(value) = 'array'
    UNION ALL
    SELECT study_id, key, value, true, false FROM legacy_lists
    WHERE key IN ('models', 'workspaces', 'biospecimens') AND jsonb_typeof(value) = 'array'
      AND value <> '[]'::jsonb
  ),
  non_array_promoted AS MATERIALIZED (
    SELECT DISTINCT sp.study_id, lower(sp.key) AS key
    FROM study_property sp
    WHERE lower(sp.key) IN ('models', 'workspaces', 'biospecimens')
      AND CASE WHEN pg_input_is_valid(sp.value, 'jsonb') THEN jsonb_typeof(sp.value::jsonb) <> 'array' ELSE true END
  )
  SELECT t.key, count(DISTINCT lists.study_id) AS studies,
         coalesce(sum(jsonb_array_length(lists.value)), 0) AS assets,
         max(jsonb_array_length(lists.value)) AS longest_list,
         count(lists.key) FILTER (WHERE legacy) AS legacy_lists,
         count(lists.key) FILTER (WHERE miscased) AS miscased_lists,
         count(lists.key) FILTER (WHERE legacy AND n.study_id IS NOT NULL) AS blocked_legacy_lists,
         (SELECT count(*) FROM non_array_promoted np WHERE np.key = t.key) AS non_array_promoted
  FROM (VALUES ('models'), ('workspaces'), ('biospecimens')) AS t(key)
  LEFT JOIN lists ON lists.key = t.key
  LEFT JOIN non_array_promoted n ON n.study_id = lists.study_id AND n.key = lists.key
  GROUP BY t.key;
  ```

  `syncStudy` moves a list found only in the legacy `assets` object to its own property, and syncs a miscased property (`Models`) in place, since reads match keys ignoring case. A legacy list beside a property of its own type is never read, so it stays as it is. Two things it cannot settle, so fix them by hand before the run: `blocked_legacy_lists` (a legacy list beside a promoted property that is not a valid JSON array, which reads fall back from) and `non_array_promoted`. `syncStudy` skips both types of such a study and logs them (Decision 4), and an unconverted list would break Task 7. Also check for duplicate asset properties; any row is a study whose reads pick one copy in no fixed order, so fix each by hand until the query returns nothing:

  ```sql
  SELECT study_id, lower(key) AS key, count(*)
  FROM study_property
  WHERE lower(key) IN ('models', 'workspaces', 'biospecimens', 'assets')
  GROUP BY study_id, lower(key)
  HAVING count(*) > 1;
  ```

- Run the report query before and after the bulk call. Per type it counts the object entries, the entries whose id names a live row of their study, the non-object elements, and the live rows no entry names. After the run, `converted` equals `entries` and `live_rows_without_entry` is zero:

  ```sql
  WITH asset_rows AS MATERIALIZED (
    SELECT sp.study_id, lower(sp.key) AS key, sp.value::jsonb AS value
    FROM study_property sp
    WHERE lower(sp.key) IN ('models', 'workspaces', 'biospecimens', 'assets')
      AND pg_input_is_valid(sp.value, 'jsonb')
  ),
  legacy_objects AS MATERIALIZED (
    SELECT study_id, value FROM asset_rows
    WHERE key = 'assets' AND jsonb_typeof(value) = 'object'
  ),
  legacy_lists AS MATERIALIZED (
    SELECT legacy_objects.study_id, lower(o.k) AS key, o.v AS value
    FROM legacy_objects, jsonb_each(legacy_objects.value) AS o(k, v)
  ),
  lists AS MATERIALIZED (
    SELECT study_id, key, value FROM asset_rows
    WHERE key <> 'assets' AND jsonb_typeof(value) = 'array'
    UNION ALL
    SELECT study_id, key, value FROM legacy_lists
    WHERE key IN ('models', 'workspaces', 'biospecimens') AND jsonb_typeof(value) = 'array'
      -- Reads, and so the sync, ignore a legacy list once its type has a property of its own.
      AND NOT EXISTS (
        SELECT 1 FROM study_property sp
        WHERE sp.study_id = legacy_lists.study_id AND lower(sp.key) = legacy_lists.key
      )
  ),
  elements AS MATERIALIZED (
    SELECT lists.study_id, lists.key, e.element,
           CASE lists.key WHEN 'models' THEN 'modelId' WHEN 'workspaces' THEN 'workspaceId' ELSE 'biospecimenId' END AS id_key
    FROM lists, jsonb_array_elements(lists.value) AS e(element)
  ),
  entries AS MATERIALIZED (
    SELECT study_id, key, jsonb_typeof(element) = 'object' AS is_object,
           CASE WHEN jsonb_typeof(element) = 'object' AND asset_id_in_row_form(element -> id_key)
                THEN (element ->> id_key)::numeric::integer END AS row_id
    FROM elements
  ),
  live AS MATERIALIZED (
    SELECT 'models' AS key, study_id, model_id AS row_id FROM study_model WHERE retired_date IS NULL
    UNION ALL
    SELECT 'workspaces', study_id, workspace_id FROM study_workspace WHERE retired_date IS NULL
    UNION ALL
    SELECT 'biospecimens', study_id, biospecimen_id FROM study_biospecimen WHERE retired_date IS NULL
  )
  SELECT t.key,
         (SELECT count(*) FROM entries e WHERE e.key = t.key AND e.is_object) AS entries,
         (SELECT count(*) FROM entries e
          WHERE e.key = t.key AND e.is_object
            AND EXISTS (SELECT 1 FROM live l WHERE l.key = e.key AND l.study_id = e.study_id AND l.row_id = e.row_id)) AS converted,
         (SELECT count(*) FROM entries e WHERE e.key = t.key AND NOT e.is_object) AS non_objects,
         (SELECT count(*) FROM live l WHERE l.key = t.key) AS live_rows,
         (SELECT count(*) FROM live l
          WHERE l.key = t.key
            AND NOT EXISTS (SELECT 1 FROM entries e WHERE e.key = l.key AND e.study_id = l.study_id AND e.row_id = l.row_id)) AS live_rows_without_entry
  FROM (VALUES ('models'), ('workspaces'), ('biospecimens')) AS t(key);
  ```

- The bulk call commits each study in its own transaction under the study row lock, which registration writes take first, so a write during the run waits for that study's sync and then syncs itself (Task 3). It streams one result per study and a summary, up to `limit` studies per call. A study that fails is reported and the rest continue: a 400 is a biospecimen in row-id form with no `sampleId`, fixed by hand and re-run with `POST /api/dataset/study/{studyId}/assets/sync`. Each synced study's datasets that are already in the index are reindexed by the same call, so no separate reindex is needed.
- The PR changes the three OpenAPI id fields from Task 4 to say that responses hold an integer, so it merges after the production run, once the report shows every entry converted. Their type stays `integer` or `string`, because the schemas also describe requests, and a form opened before the conversion still sends the old id, which `legacy_key` matches.
- Run [Staging verification](#staging-verification) before the production run.

**Tests** (`StudyAssetRecordSyncTest`, through `DatasetServiceDAO.syncStudyAssets` over JSON stored without the registration sync, as existing studies were):
- Every object element gets a row and its id in the JSON; element order and unknown keys are preserved.
- Empty, duplicate, UUID and 13-digit ids all get rows; every copy of a duplicated id, and blanks, get a null `legacy_key`.
- A list under a differently cased key inside `assets` moves to its own property and converts; a top-level `Models` property converts in place.
- A biospecimen's original string or numeric id is copied to `sampleId` as a string.
- Non-object elements are left in place with no row; a non-array or malformed promoted value is left untouched.
- A second run changes nothing; a study with no asset properties gets no rows.

### Task 7: Drop the position-based id fallback (duos-ui)

- `modelAsset.ts`, `workspaceAsset.ts` and `biospecimenAsset.ts` drop the `${studyId}-${index}` fallback, and the "Biospecimen ID" column drops its `biospecimenId` fallback (Task 2).
- `src/types/model.ts` narrows the three id fields from Task 5 to `number`, since every converted entry now has a row id.
- The conversion leaves non-object elements in place without an id, so the three transforms skip any element that is not an object rather than rendering a row with an undefined id.

**Tests** (vitest): rows use the numeric id, the type check passes with the narrowed fields, and a non-object element in the index response renders no row, for all three types.

## Edge cases to cover in review

- A form opened before the conversion submits UUIDs. The `legacy_key` match updates those rows. Models had empty ids, so they have no `legacy_key`: a stale form retires and recreates them, which costs their ids but loses no data.
- A stale form holding a pre-conversion id in row-id form would match another asset's row id. The UI has never produced one, and Task 3 does not deploy if production holds any.
- A `Date.now()` id is a 13-digit number, outside the integer range. It is not in row-id form, so neither Task 3 nor Task 6 matches or casts it.
- An id copied from another study, or from a retired row, never updates that row. It creates a new row in the study being written.
- A study is deleted: its rows go with it, by cascade (Decision 3).
- Two concurrent edits of one study run one after the other, because the update writes the study row before the sync runs. The last write wins, as it does for every other study property today.
- A registration write during the conversion waits on its study's row lock, then runs Task 3's sync against converted data.
- The write-back must not touch any key except the id and a biospecimen's `sampleId` per Decision 12, including keys the backend does not know.

## Staging verification

Before the production run of Task 6, restore a production snapshot to staging, run the conversion, and:

- check that the report query shows every entry converted and no live row without an entry, and that the bulk call's summary shows no failures;
- compare each row field by field with the element it came from;
- compare the Data Library's model, workspace and biospecimen tabs for a sample of studies, before and after, including a search on every tab;
- read the `dataset` index mapping for `study.assets.*Id` in each environment, and confirm it, and `study.assets.biospecimens.sampleId`, is the explicit `text` mapping added before Task 2;
- check that the report's `non_objects` and every value the sync logged as skipped are genuinely malformed.

Zero unexplained differences is the gate for production.

## Rollback

Keep the tables in every case. They hold the only mapping from assigned ids to assets, and dropping them would lose it if the work is redeployed.

- **Before Task 5 deploys:** revert Tasks 3 and 4 together. Ids already written into the JSON do no harm, because the old code treats ids as opaque strings.
- **After Task 5 deploys:** revert Task 5 (and Task 7, if it shipped) first, so the UI generates ids and has its fallback again, then revert Tasks 3 and 4.
- **Index mapping:** the explicit `text` mapping added before Task 2 (including `sampleId`) accepts both a reverted UI's UUIDs and integer ids, so a rollback needs no index change. Confirm at rollback that each environment still has it, and that a workspace created from the reverted UI indexes.
- **Restoring data:** the Task 6 run happens in a maintenance window with every study write frozen (registration create and update, `patchStudyById`, `deleteStudyById`, `PUT /api/dataset/study/{studyId}/custodians`, `PUT /api/dataset/study/convert/{datasetIdentifier}`, which can create a study, and dataset deletes through `DELETE /api/dataset/{datasetId}`, since DT-4209's catalog references datasets too), drained before the backup and held until the post-conversion report is checked. The run needs no freeze for correctness (Decision 10); the freeze keeps the backup restorable. At its start, back up `study_property` and the three asset tables in one consistent snapshot (one `pg_dump` run). A restore is valid only inside that window, because no study has been created, edited or deleted since the backup. Restore the four tables together or not at all; restoring the JSON alone leaves rows that no longer match it. Once DT-4209's catalog has shipped, the same `pg_dump` run includes `asset_catalog`, and the restore follows `asset-identifiers-plan.md` Rollback, since deleting the asset rows nulls the catalog's references. After the window closes, recover with `syncStudy` over the current JSON instead of restoring. The snapshot predates the conversion, so after a restore, and a fix for whatever made it necessary, run `POST /api/dataset/study/assets/sync` with no `since` again, following each summary's `nextAfterStudyId` until it is absent, which runs `syncStudy` for every study and reindexes each study's indexed datasets, before Task 7 is redeployed.
- **Redeploying after a rollback:** registrations created or edited while Task 3 was reverted changed their JSON without touching the rows, and may hold client ids in row-id form again. Before redeploying, re-run Task 3's short-id query, and move any biospecimen hit into `sampleId` by hand (other hits as Task 3 says); repeat it before Task 6 if Task 6 has not run. Then redeploy Tasks 3 and 4, then run `POST /api/dataset/study/assets/sync?since=<rollback time>`, following `nextAfterStudyId` as above, which resyncs and reindexes every study whose `create_date` or `update_date` is at or after it (`insertStudy` leaves `update_date` null, so `update_date` alone misses new studies).
- **A study that fails a resync:** the bulk call reports it and continues. A 400 is a biospecimen whose id is in row-id form with no `sampleId`: move its sample id into `sampleId` by hand, then re-run that study with `POST /api/dataset/study/{studyId}/assets/sync`.

## Out of scope (follow-up)

- Making the rows the source of truth instead of the registration (Decision 7).
- DUOS identifiers and the cross-type catalog (DT-4209).
- Governance, search or request changes.
- Adding and editing biospecimens in the registration form (Decision 8).
- Data use on biospecimens. DT-3810 leaves it out until its free-text `optionalDataUse` field is decided.

## Deploy order

1. DT-4298 and DT-4299 (DT-3810 validation and OpenAPI) merge first. They are smaller and need no migration, and Task 3 then copies their fields from the start.
2. The Decision 5 inventory runs in production to set the list cap. Then the Comms check-in and the API-users notice, which states the cap, go out.
3. The explicit index mapping and the §B.5a entry from Task 3 land. Then Task 1, Task 2 and DT-4323 deploy.
4. Tasks 3 and 4 deploy in one release.
5. Task 5 deploys.
6. DT-4378 deploys. The inventory and its hand fixes, then staging verification. Then, in a maintenance window with every study write frozen, the inventory again, the backup, the bulk resync (which reindexes) and the report check. Task 6's PR, which carries the OpenAPI text, merges after the run.
7. Task 7 deploys.

## Overlap with DT-3810

`asset-data-use-plan.md` adds `dataUse` and `accessManagement` to model and workspace entries, and it rejected new tables for that purpose. That decision still holds. The tables here exist for identity, the registration JSON stays the source of truth, and DT-3810's `AssetDataUse` module keeps validating and enriching the JSON entries unchanged. This plan follows two of its rules:

- The model and workspace tables copy `dataUse` and `accessManagement` as stored. The computed `dataUseSummary` is never stored; `AssetDataUse.prepare` strips it before persistence.
- Entries are never round-tripped through a typed class. The id write-back changes only the id key, and a biospecimen's `sampleId` per Decision 12.

Three DT-4297 tickets touch the same files as this plan:

| DT-3810 ticket | Shared files | Task here |
|---|---|---|
| DT-4298: validate model and workspace entries | `RegistrationRequestMapper.promotedAsset`, the registration write path | Task 3 |
| DT-4299: OpenAPI fields | `AiModel.yaml`, `Workspace.yaml` | Task 4 |
| DT-4302: registration form fields | `AiModelList`, `WorkspaceList`, their add/edit forms | Task 5 |
