# Per-Dataset DAA Override Plan

## Status

Proposed. Ticket: [DT-3876](https://broadworkbench.atlassian.net/browse/DT-3876).

Related plan: `so-approval-exempt-datasets-plan.md` ([DT-4267](https://broadworkbench.atlassian.net/browse/DT-4267)) changes some of the same files. The two plans do not depend on each other. See [Overlap with DT-4267](#overlap-with-dt-4267).

## Overview

A DAC chair can give one dataset a Data Access Agreement (DAA) that is different from the DAC's default DAA. The chair can pick one of the DAC's DAAs or upload a new one. A DAR on that dataset then requires the dataset's DAA, not the DAC default.

### How DAAs apply to datasets today

A DAC has at most one default DAA, stored in `dac_daa` (`UNIQUE(dac_id)`). A DAC can also own other DAAs that it uploaded (`data_access_agreement.initial_dac_id`), but only the default applies to datasets.

consent resolves "the DAA for a dataset" as `dataset.dac_id → dac_daa.daa_id` in four `DaaDAO` queries:
- `findDaaIdsByDatasetIds`
- `mapDaaIdsToDatasetIds`
- `findCurrentDaaMappingsByDatasetIds`
- `findByDarReferenceId`

These queries drive the following:
- DAR validation (`hasAcknowledgedRequiredDaas` requires an exact match between the required and the acknowledged DAAs).
- The SO pre-authorization check.
- The per-DAR DAA snapshot.
- `POST /api/daa/datasets`.

duos-ui repeats the same rule in four places. Each one takes the first DAA whose `dacs` list contains `dataset.dacId`.

### The change

1. Add `dataset.override_daa_id` (nullable FK to `data_access_agreement`). `NULL` means "use the DAC default".
2. Change the four `DaaDAO` queries to resolve the **effective DAA**: `COALESCE(dataset.override_daa_id, dac_daa.daa_id)`. Every caller gets per-dataset behavior with no change of its own.
3. Give DAC chairs endpoints and a picker on the DAC profile page to set or clear the override, and to upload a DAA for one dataset without changing the DAC default.
4. Replace the four DAC-only lookups in duos-ui with one helper that uses the override first.
5. Make override-only DAAs visible to SOs, so that SOs can pre-authorize researchers for them.

## Decisions

| # | Question | Decision | Source |
|---|---|---|---|
| 1 | Which DAAs can a chair assign to a dataset? | Only the DAC's DAAs: its default (`dac_daa`), or a DAA that it uploaded (`initial_dac_id`). This is the existing rule in `DaaService.isDaaLinkedToDac`. | Ticket answer |
| 2 | Where is the override stored? | New column `dataset.override_daa_id`, JSON field `overrideDaaId`. The name says "override", so that no reader takes `null` to mean "no DAA". | This plan |
| 3 | How does a chair upload a DAA for one dataset? | Today `POST /api/daa/dac/{dacId}` always makes the upload the DAC default. Add the query parameter `setAsDacDefault` (default `true`). The dataset upload sends `false`. | This plan |
| 4 | What happens when a dataset moves to another DAC, or its DAC is deleted or externalized? | The override clears. It is a decision of the old DAC. | This plan |
| 5 | Who can set the override? | Chairpersons of the dataset's DAC, the same rule as dataset approval. | This plan |
| 6 | Where do chairs set it? | The "Datasets Managed by this DAC" grid on the DAC profile page. The `/dac_datasets` grid reads the search index and stays read-only. | This plan |
| 7 | The ticket says "a specific DAA for the dataset, model, or workspace". | DUOS has no "model" or "workspace" entity. This plan covers datasets only. | **Confirm with product** |
| 8 | An override-only DAA has an empty `dacs` list, because `dacs` lists only the DACs that use the DAA as their default. How do SOs see it? | Add `overrideDatasetIds` to the DAA JSON. The SO DAA assignment page and the dashboard counts treat a DAA as assignable when it has DACs or override datasets. | This plan |

## Task summary

| # | Repo | Task | Size (est.) | Depends on |
|---|---|---|---|---|
| 1 | consent | Store the override DAA on the dataset | ~300 lines | — |
| 2 | consent | Resolve the effective DAA in `DaaDAO` | ~300 lines | 1 |
| 3 | consent | Upload a DAA without making it the DAC default | ~150 lines | — |
| 4 | consent | Show override DAAs to SOs (`overrideDatasetIds`, dashboard counts) | ~200 lines | 1 |
| 5 | consent | Endpoints for chairs to set and clear the override | ~350 lines | 2 and 4; **6 and 9 deployed**; decision 7 confirmed |
| 6 | duos-ui | Use the effective DAA in the DAR and progress report forms | ~350 lines | 1 deployed |
| 7 | duos-ui | DAA picker on the DAC profile page | ~350 lines | 5 deployed |
| 8 | duos-ui | Upload a new DAA from the picker | ~150 lines | 3 deployed, 7 |
| 9 | duos-ui | Show override-only DAAs on the SO DAA assignment page | ~100 lines | 4 deployed |

Sizes include tests. Tasks 1 and 3 can start at once.

**The endpoints (Task 5) are the only way to write an override, so they ship after everything that reads overrides.** The API, not the picker, is the gate: a client can call the API without the picker. Until Task 5 deploys, no dataset can have an override. When it deploys, these parts already handle overrides:
- the server queries (Task 2),
- the DAR and progress report forms (Task 6),
- the SO DAA assignment page (Task 9), so that SOs can pre-authorize researchers for an override-only DAA.

## Tasks

### Task 1: Store the override DAA on the dataset (consent)

Add the column and carry it through the model and the DAO layer.

- Liquibase changeset: add `dataset.override_daa_id BIGINT NULL`, with an FK to `data_access_agreement(daa_id)` (`ON DELETE SET NULL`) and an index.
- `Dataset.java`: add `overrideDaaId`. `DatasetMapper` and `DatasetReducer`: map the column (follow the `dac_approval` pattern).
- `DatasetDAO`:
  - Add `override_daa_id` to the six dataset select lists that already select `d.dac_approval`.
  - Add `updateDatasetDaaOverride(daaId, updateDate, updateUserId, datasetId, dacId)`. A `null` `daaId` clears the override. It returns the number of updated rows. Its `WHERE` clause also checks the DAC, so that a write from a stale DAC changes nothing (see Task 5):

    ```sql
    WHERE dataset_id = :datasetId AND dac_id = :dacId
    ```

- OpenAPI: add `overrideDaaId` to `Dataset.yaml`. Dataset responses carry the field as soon as this task deploys, and Task 6 reads it before Task 5 ships.
- Clear the override when the DAC changes (decision 4). This touches `DatasetDAO.updateDatasetDacId` and `updateDatasetByDatasetId`, and the two dataset `UPDATE` statements in `DacServiceDAO` (DAC delete and externalize). In the two `DatasetDAO` methods, clear the override only when the DAC id really changes:

  ```sql
  override_daa_id =
      CASE WHEN dac_id IS DISTINCT FROM :dacId THEN NULL ELSE override_daa_id END,
  dac_id = :dacId
  ```

  PostgreSQL reads the old row in every `SET` expression, so the `CASE` sees the old `dac_id`.

**Tests:**
- New datasets have no override.
- The override can be set and cleared.
- An update with the wrong DAC id changes no row and returns 0, both to set and to clear.
- The override appears in `findDatasetById`, `findDatasetsByIdList`, and `findDatasetsAssociatedWithDac`.
- A DAC change clears the override. An unchanged DAC keeps it.
- DAC delete and externalize clear the override.

### Task 2: Resolve the effective DAA in `DaaDAO` (consent)

Change the four queries listed in the overview to start from the dataset and take the override first:

```sql
SELECT DISTINCT COALESCE(d.override_daa_id, dac_daa.daa_id) AS daa_id
FROM dataset d
LEFT JOIN dac_daa ON dac_daa.dac_id = d.dac_id
WHERE d.dataset_id IN (<datasetIds>)
  AND COALESCE(d.override_daa_id, dac_daa.daa_id) IS NOT NULL
```

- `findByDarReferenceId` keeps its column list. Its `dacs` value stays "the DAC of the DAR's dataset", which is what the old joins returned.
- Leave `findDaaDatasetIdsByUserId` alone. Its only caller, `DatasetService.enforceDAARestrictions`, has no callers.

This task changes behavior for DAR validation, the SO pre-authorization check, and the DAR snapshots. No dataset has an override until Task 5 ships, so the change is safe to deploy early. Reviewers should still read it as a behavior change.

**Tests.** First, for each query, cover these cases:
- A dataset with no override gets the DAC default.
- A dataset with an override gets the override.
- A mix of the two gets both DAAs.
- An override on a DAC that has no default still works.
- A dataset with neither gets no DAA.

The existing `DaaDAOTest` tests must pass unchanged.

Then cover each workflow that reads these queries. consent has no integration test that submits a DAR, so use DAO tests (`DAOTestHelper`, real SQL) that call the same queries as the service:

| Workflow | Service code | Query | Test |
|---|---|---|---|
| DAA acknowledgment | `hasAcknowledgedRequiredDaas` | `findDaaIdsByDatasetIds` | For a dataset with an override, the required set is `{override}`, not `{default}`. |
| SO pre-authorization | `isUserPreAuthorizedForAllDaas` | `findDaaIdsByDatasetIds` | A library card with only the default DAA does not cover a dataset with an override. A card with the override DAA does. |
| DAR snapshot | `captureDatasetDaaSnapshots` | `findCurrentDaaMappingsByDatasetIds`, then `insertDarDatasetDaaSnapshots` | The snapshot records the override DAA. |
| Snapshot stability | — | `findDatasetDaaSnapshotsByReferenceId` | After a snapshot is captured, set, change, and clear the override. The snapshot does not change. |

### Task 3: Upload a DAA without making it the DAC default (consent)

- `DaaResource.createDaaForDac` (`POST /api/daa/dac/{dacId}`): add the query parameter `setAsDacDefault`, default `true`.
- Pass the parameter through `DaaService.createDaaWithFso` to `DaaServiceDAO.createDaaWithFso`, which calls `createDacDaaRelation` only when the value is `true`. The DAA still records the DAC as its `initial_dac_id`, so it counts as one of the DAC's DAAs (decision 1).
- OpenAPI: document the parameter.

**Tests:** with `false`, the DAA exists and is linked by `initial_dac_id` but not by `dac_daa`. With no parameter, the behavior is the same as today.

### Task 4: Show override DAAs to SOs (consent)

- `DataAccessAgreement`: add `overrideDatasetIds` (sorted list).
- Two new `DaaDAO` queries:
  - `mapOverrideDaaIdsToDatasetIds()`: maps each DAA to the datasets that use it as an override. It reuses `DaaDatasetReducer`. Only `findAll` uses it.
  - `findOverrideDatasetIdsByDaaId(daaId)`: the override datasets of one DAA, `ORDER BY dataset_id`. `findById` uses it, so that a single read does not load every override. This matters because `findById` runs in loops, for example in `DaaService.findDAAsInJsonArray`.
- `DaaService.findAll` and `findById`: fill `overrideDatasetIds`, sorted, with an empty list when the DAA has no override datasets.
- `SigningOfficialDashboardDAO` and `AdminDashboardDAO`: the `assignable_daas` CTE also counts DAAs that are a dataset override in a DAC that is not deleted.
- OpenAPI: add `overrideDatasetIds` to `DataAccessAgreement.yaml`.

**Tests:** both new DAO queries, including the sort order; `findAll` and `findById` fill the field (with and without overrides), and `findById` does not call the bulk query; both dashboards count an override-only DAA.

### Task 5: Endpoints for chairs to set and clear the override (consent)

- `PUT /api/dac/{dacId}/dataset/{datasetId}/daa/{daaId}` sets the override.
- `DELETE /api/dac/{dacId}/dataset/{datasetId}/daa` clears it.
- Both endpoints use role `CHAIRPERSON` and return the updated `Dataset`.

Implementation:
- `DacResource`: copy the pattern of `approveDataset` (`PUT /api/dac/{dacId}/dataset/{datasetId}`). Move its two checks into a private helper, `findDatasetForChair`, and use the helper in all three endpoints. The checks are "the dataset belongs to this DAC" and "the user is a chair of this DAC". Both return 404, as today.
- `DatasetService.updateDaaOverride(dataset, user, daaId)`:
  1. For a non-null `daaId`, return 404 if the DAA does not exist.
  2. Return 400 if the DAA does not belong to the dataset's DAC (decision 1).
  3. Update the row with the DAC id from the request.
  4. If no row changed, the dataset moved to another DAC after the resource checked it. An admin or a chair can move a dataset (for example, through `PUT /api/dataset/v3/{datasetId}`). Throw `ConsentConflictException` (409).
  5. Otherwise, return the fresh dataset. No reindex is needed, because the search index does not hold the DAA.
- OpenAPI: the two path files and the 409 response.

**Tests:**
- Service: set; clear; a DAA of another DAC (400); an unknown DAA (404); a stale DAC (the DAO returns 0) gives 409, both to set and to clear.
- Resource: success for both endpoints, wrong DAC, not a chair.
- The existing `approveDataset` tests after the helper refactor.

**Release gate:** confirm decision 7 (datasets only) with product before this task deploys. This task is the first that lets a dataset have an override.

### Task 6: Use the effective DAA in the DAR and progress report forms (duos-ui)

- Types: `Dataset.overrideDaaId?: number | null`.
- `src/libs/daaHelpers.ts`:
  - `findEffectiveDaa(dataset, daas)`: the override if it is set, else the DAA whose `dacs` contains the dataset's DAC. If the override is set but missing from `daas`, return `undefined`. Do not fall back to the default, because the backend requires the override.
  - `getRequiredDaaIds(datasets, daas)`: unique ids, ascending.
  - `findDatasetsWithUnresolvedOverride(datasets, daas)`: the datasets whose override is set but missing from `daas`.
- **Unresolved override.** `getRequiredDaaIds` cannot include a DAA that it cannot find, so a submit would fail the backend's exact-set check. `GET /api/daa` returns every DAA, so this happens only when the list is stale or did not load. When `findDatasetsWithUnresolvedOverride` is not empty:
  - `DataAccessAgreements.tsx` shows an error that names the datasets and asks the researcher to reload the page, and disables the Attest button. Save Draft stays available.
  - `ProgressReportDataAccessAgreements.tsx` shows the same error, and the progress report form does not submit.
- Use the helpers in the four places that repeat the DAC-only rule:
  - `RequiredDAAs.tsx`: when an override DAA has no DAC name, show "—".
  - `DataAccessAgreements.tsx`: delete `getDisplayedDaaIds`.
  - `ProgressReportDataAccessAgreements.tsx`: delete the local `getRequiredDaaIds`.
  - `SelectableDatasets.tsx` has two branches. Change only the editing branch: delete `buildDaaByDacId`, keep the list of current DAAs in state, and resolve each dataset with `findEffectiveDaa`. Do not change the submitted-DAR branch (`disabled`). It shows the DAA from the DAR snapshot (`DAR.getDatasetDaaSnapshots`), which is the DAA the researcher acknowledged. A later override must not change what a submitted DAR shows.
- **Bug fix that comes with this task:** `getDisplayedDaaIds` drops a DAA when its base file name is the same as another DAA's. With overrides, two required DAAs can easily share a name, and then the backend rejects the DAR ("All of the DAAs required were not acknowledged"). `getRequiredDaaIds` de-duplicates by id.

**Tests:**
- The helpers: default, override, missing override, no DAC.
- Two DAAs with the same file name are both kept.
- Each of the four components shows or sends the override DAA.
- `SelectableDatasets` for a submitted DAR still shows the snapshot DAA, also when the dataset now has a different override.
- With an unresolved override, the DAR form shows the error and disables Attest, and the progress report form shows the error and does not submit.

This task must deploy before Task 5 (see the task summary). If overrides could exist while the old form is live, the form would send the DAC default, and every DAR on an overridden dataset would fail validation.

### Task 7: DAA picker on the DAC profile page (duos-ui)

- `DAC.setDatasetDaa(dacId, datasetId, daaId)` and `DAC.clearDatasetDaa(dacId, datasetId)` in `src/libs/ajax/DAC.ts`.
- `daaHelpers.getDacDaaOptions(daas, dacId)`: the DAC's DAAs (uploaded by it, or its default), oldest first.
- New component `DatasetDaaCell`. It must keep two states apart that have different future behavior:

  | State | Stored value | When the DAC default changes later |
  |---|---|---|
  | Follow the DAC default | `overrideDaaId = null` | The dataset uses the new default. |
  | Pinned to a DAA | `overrideDaaId = <id>` | The dataset keeps the pinned DAA. |

  - A chair sees a select with these options:
    - "Follow the DAC default (<file name>)". This option clears the override.
    - Every one of the DAC's DAAs, **including the current default**. Each one pins the dataset to that DAA. Mark the current default with "(current DAC default)" after its name.
  - The selected option comes from `overrideDaaId`, never from a comparison with the default. A dataset pinned to the current default shows the pinned option, not "Follow the DAC default".
  - If `overrideDaaId` is set but the DAA is missing from the list, show it as "DAA-<id>" and keep it selected.
  - Everyone else sees the current DAA name as text, with "(DAC default)" when the dataset follows the default.
  - If the save fails, show an error and keep the old value. On a 409, also tell the chair to reload the page, because the dataset moved to another DAC.
- `DacProfile.tsx`: load `DAA.getDaas()` with the DAC and its datasets, and add a "Data Access Agreement" column to the datasets grid. This needs some setup in the page:
  - a chair check: `user.roles` has `Chairperson` for this `dacId`. Do not include admins, because the backend allows chairs only.
  - a `handleDatasetChange` handler that merges the returned dataset into its row.
  - the full `dataset` object on each grid row.
  - the column list as a `useMemo`.

**Tests:**
- The ajax calls.
- The cell:
  - read-only text for the default and for an override.
  - picking a DAA pins it; picking "Follow the DAC default" clears it.
  - a dataset pinned to the current default shows the pinned option selected, and the chair can switch it to "Follow the DAC default".
  - after the page reloads with a new DAC default, a pinned dataset still shows its pinned DAA, and a following dataset shows the new default.
  - a failed save keeps the old value; a 409 shows the reload message.
- The page: the column is present, a chair can edit, a member cannot.

### Task 8: Upload a new DAA from the picker (duos-ui)

- `DAA.createDaa(file, dacId, { setAsDacDefault })`: when the value is `false`, add `?setAsDacDefault=false` to the URL.
- `DatasetDaaCell`: add the option "Upload a new DAA…". It opens the existing `UploadDaaModal`. On save, the cell does three things:
  1. It uploads the file with `setAsDacDefault: false`.
  2. It adds the new DAA to the page's list.
  3. It pins the dataset to the new DAA.

**Tests:** the URL parameter; the upload does not make the DAA the default, and then pins it to the dataset; a failed upload shows an error.

### Task 9: Show override-only DAAs on the SO DAA assignment page (duos-ui)

- Types: `DAAObject.overrideDatasetIds?: number[]`. This task is the only one that reads the field, so it adds the type.
- `researcherViewHelpers.ts`:
  - Add `isAssignableDaa(daa)`. It is true when the DAA has DACs or override datasets.
  - `getDacName` labels an override-only DAA as "Dataset-specific (N datasets)".
- `DaaAssociationsPage.tsx`: filter with `isAssignableDaa`. Today the filter drops DAAs that have no DACs, and its comment says that "a DAA is always associated with the DAC that uploaded it". That is not true: `dacs` comes only from `dac_daa`, the DAC default link. Fix the comment too.

**Tests:** the helper, the label, and an override-only DAA that shows on the page.

This task must deploy before Task 5. Otherwise a chair could require an override-only DAA that SOs cannot find, so no researcher could be pre-authorized for it.

## Edge cases to cover in review

1. Two required DAAs have the same file name. Expected: both show and both are sent (Task 6).
2. A dataset's override DAA is missing from the list that the UI loaded. Expected: the UI does not fall back to the DAC default, and it blocks the submit with an error that asks the researcher to reload (Task 6).
3. A DAA is an override only, so nothing lists it as a DAC default. Expected: SOs can see it and pre-authorize researchers for it (Tasks 4 and 9).
4. A dataset moves to another DAC, also in the middle of a chair's save. Expected: the override clears, and a stale write gets 409 (Tasks 1 and 5).
5. A dataset is pinned to the DAA that is also the DAC default. Expected: it stays pinned when the default changes (Task 7).
6. An override changes after a DAR was submitted. Expected: the DAR's snapshot does not change (Task 2).
7. A chair uploads a DAA for one dataset. Expected: the DAC default does not change (Tasks 3 and 8).

## Out of scope (follow-up)

`DaaDAO.findByDarReferenceId` (the DAAs of a DAR, used by `DataAccessRequestResource`) returns the *current* DAAs of the DAR's datasets, not the DAAs that the researcher acknowledged at submit. This is already true today when a DAC changes its default DAA. Overrides make it happen more often. The `dar_dataset_daa_snapshot` table already holds the acknowledged DAAs, so a follow-up ticket can read from it. This plan changes the query only so that it resolves overrides (Task 2).

## Deploy order

1. Tasks 1–4 (consent). Task 3 can deploy at any point.
2. Tasks 6 and 9 (duos-ui).
3. Confirm decision 7 with product.
4. Task 5 (consent). This is the first change that lets a dataset have an override.
5. Task 7, then Task 8 (duos-ui).

The new JSON fields and the query parameter are additive, so the current UI works against the new backend at every step.

## Overlap with DT-4267

The SO-approval-exempt plan changes some of the same code. Whichever branch merges second keeps both sides of each conflict:

- **consent:**
  - `changelog-master.xml`, `Dataset.java`, the dataset mappers, and the six `DatasetDAO` select lists: each plan adds one column.
  - The DAC-change `UPDATE` statements: each plan adds one `SET` item.
  - `Dataset.yaml`: each plan adds one property.
- **`DacResource.findDatasetForChair`:** both plans add the same helper. Keep one copy.
- **`DacProfile.tsx`:** both plans add the same setup (the chair check, `handleDatasetChange`, the row `dataset`, the `columns` memo). Keep one copy, and keep both columns (DAA, then SO Approval).
- **`DataAccessAgreements.tsx`:** this plan replaces `getDisplayedDaaIds` with `getRequiredDaaIds`. DT-4267 filters out exempt datasets only where `missingDaaIds` is computed. Keep that filter out of `getRequiredDaaIds`, because its result is the list of DAAs that the DAR acknowledges.
