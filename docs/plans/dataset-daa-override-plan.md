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
| 2 | consent | Resolve the effective DAA in `DaaDAO` | ~250 lines | 1 |
| 3 | consent | Upload a DAA without making it the DAC default | ~150 lines | — |
| 4 | consent | Show override DAAs to SOs (`overrideDatasetIds`, dashboard counts) | ~200 lines | 1 |
| 5 | consent | Endpoints for chairs to set and clear the override | ~350 lines | 1, 2 |
| 6 | duos-ui | Use the effective DAA in the DAR and progress report forms | ~350 lines | 1 deployed |
| 7 | duos-ui | DAA picker on the DAC profile page | ~350 lines | 5 deployed, **6 deployed** |
| 8 | duos-ui | Upload a new DAA from the picker | ~150 lines | 3 deployed, 7 |
| 9 | duos-ui | Show override-only DAAs on the SO DAA assignment page | ~100 lines | 4 deployed |

Sizes include tests. Tasks 1 and 3 can start at once.

## Tasks

### Task 1: Store the override DAA on the dataset (consent)

Add the column and carry it through the model and the DAO layer.

- Liquibase changeset: add `dataset.override_daa_id BIGINT NULL`, with an FK to `data_access_agreement(daa_id)` (`ON DELETE SET NULL`) and an index.
- `Dataset.java`: add `overrideDaaId`. `DatasetMapper` and `DatasetReducer`: map the column (follow the `dac_approval` pattern).
- `DatasetDAO`:
  - Add `override_daa_id` to the six dataset select lists that already select `d.dac_approval`.
  - Add `updateDatasetDaaOverride(daaId, updateDate, updateUserId, datasetId)`. A `null` `daaId` clears the override.
- Clear the override when the DAC changes (decision 4). This touches `DatasetDAO.updateDatasetDacId` and `updateDatasetByDatasetId`, and the two dataset `UPDATE` statements in `DacServiceDAO` (DAC delete and externalize). In the two `DatasetDAO` methods, clear the override only when the DAC id really changes:

  ```sql
  override_daa_id =
      CASE WHEN dac_id IS DISTINCT FROM :dacId THEN NULL ELSE override_daa_id END,
  dac_id = :dacId
  ```

  PostgreSQL reads the old row in every `SET` expression, so the `CASE` sees the old `dac_id`.

**Tests:** new datasets have no override; the override can be set and cleared; the override appears in `findDatasetById`, `findDatasetsByIdList`, and `findDatasetsAssociatedWithDac`; a DAC change clears it; an unchanged DAC keeps it; DAC delete and externalize clear it.

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

**Tests:** for each query, cover these cases:
- A dataset with no override gets the DAC default.
- A dataset with an override gets the override.
- A mix of the two gets both DAAs.
- An override on a DAC that has no default still works.
- A dataset with neither gets no DAA.

The existing `DaaDAOTest` tests must pass unchanged.

### Task 3: Upload a DAA without making it the DAC default (consent)

- `DaaResource.createDaaForDac` (`POST /api/daa/dac/{dacId}`): add the query parameter `setAsDacDefault`, default `true`.
- Pass the parameter through `DaaService.createDaaWithFso` to `DaaServiceDAO.createDaaWithFso`, which calls `createDacDaaRelation` only when the value is `true`. The DAA still records the DAC as its `initial_dac_id`, so it counts as one of the DAC's DAAs (decision 1).
- OpenAPI: document the parameter.

**Tests:** with `false`, the DAA exists and is linked by `initial_dac_id` but not by `dac_daa`. With no parameter, the behavior is the same as today.

### Task 4: Show override DAAs to SOs (consent)

- `DataAccessAgreement`: add `overrideDatasetIds` (sorted list).
- `DaaDAO.mapOverrideDaaIdsToDatasetIds()`: a new query that maps each DAA to the datasets that use it as an override. It reuses `DaaDatasetReducer`.
- `DaaService.findAll` and `findById`: fill `overrideDatasetIds`, with an empty list when the DAA has no override datasets.
- `SigningOfficialDashboardDAO` and `AdminDashboardDAO`: the `assignable_daas` CTE also counts DAAs that are a dataset override in a DAC that is not deleted.
- OpenAPI: add `overrideDatasetIds` to `DataAccessAgreement.yaml`.

**Tests:** the new DAO query; the service fills the field (with and without overrides); both dashboards count an override-only DAA.

### Task 5: Endpoints for chairs to set and clear the override (consent)

- `PUT /api/dac/{dacId}/dataset/{datasetId}/daa/{daaId}` sets the override.
- `DELETE /api/dac/{dacId}/dataset/{datasetId}/daa` clears it.
- Both endpoints use role `CHAIRPERSON` and return the updated `Dataset`.

Implementation:
- `DacResource`: copy the pattern of `approveDataset` (`PUT /api/dac/{dacId}/dataset/{datasetId}`). Move its two checks into a private helper, `findDatasetForChair`, and use the helper in all three endpoints. The checks are "the dataset belongs to this DAC" and "the user is a chair of this DAC". Both return 404, as today.
- `DatasetService.updateDaaOverride(dataset, user, daaId)`:
  - For a non-null `daaId`, return 404 if the DAA does not exist.
  - Return 400 if the DAA does not belong to the dataset's DAC (decision 1).
  - Then update the row and return the fresh dataset.
  - No reindex is needed, because the search index does not hold the DAA.
- OpenAPI: the two path files, and `overrideDaaId` on `Dataset.yaml`.

**Tests:**
- Service: set, clear, a DAA of another DAC (400), an unknown DAA (404).
- Resource: success for both endpoints, wrong DAC, not a chair.
- The existing `approveDataset` tests after the helper refactor.

### Task 6: Use the effective DAA in the DAR and progress report forms (duos-ui)

- Types: `Dataset.overrideDaaId?: number | null`, `DAAObject.overrideDatasetIds?: number[]`.
- `src/libs/daaHelpers.ts`:
  - `findEffectiveDaa(dataset, daas)`: the override if it is set, else the DAA whose `dacs` contains the dataset's DAC. If the override is set but missing from `daas`, return `undefined`. Do not fall back to the default, because the backend requires the override.
  - `getRequiredDaaIds(datasets, daas)`: unique ids, ascending.
- Use the helpers in the four places that repeat the DAC-only rule:
  - `RequiredDAAs.tsx`: when an override DAA has no DAC name, show "—".
  - `DataAccessAgreements.tsx`: delete `getDisplayedDaaIds`.
  - `ProgressReportDataAccessAgreements.tsx`: delete the local `getRequiredDaaIds`.
  - `SelectableDatasets.tsx`: delete `buildDaaByDacId`. Keep the list of current DAAs in state and resolve the DAA for each dataset.
- **Bug fix that comes with this task:** `getDisplayedDaaIds` drops a DAA when its base file name is the same as another DAA's. With overrides, two required DAAs can easily share a name, and then the backend rejects the DAR ("All of the DAAs required were not acknowledged"). `getRequiredDaaIds` de-duplicates by id.

**Tests:**
- The helpers: default, override, missing override, no DAC.
- Two DAAs with the same file name are both kept.
- Each of the four components shows or sends the override DAA.

**Unusual step:** deploy this task before Task 7. If chairs can set overrides while the old form is live, the form sends the DAC default, and every DAR on an overridden dataset fails validation.

### Task 7: DAA picker on the DAC profile page (duos-ui)

- `DAC.setDatasetDaa(dacId, datasetId, daaId)` and `DAC.clearDatasetDaa(dacId, datasetId)` in `src/libs/ajax/DAC.ts`.
- `daaHelpers.getDacDaaOptions(daas, dacId)`: the DAC's DAAs (uploaded by it, or its default), oldest first.
- New component `DatasetDaaCell`:
  - A chair sees a select with these options:
    - "DAC default (<file name>)". This option clears the override, so the dataset follows the default if the default changes later.
    - Each of the DAC's other DAAs.
  - Everyone else sees the current DAA name as text.
  - If the save fails, show an error and keep the old value.
- `DacProfile.tsx`: load `DAA.getDaas()` with the DAC and its datasets, and add a "Data Access Agreement" column to the datasets grid. This needs some setup in the page:
  - a chair check: `user.roles` has `Chairperson` for this `dacId`. Do not include admins, because the backend allows chairs only.
  - a `handleDatasetChange` handler that merges the returned dataset into its row.
  - the full `dataset` object on each grid row.
  - the column list as a `useMemo`.

**Tests:**
- The ajax calls.
- The cell: read-only text for the default and for an override; picking a DAA sets it; picking the default clears it; a failed save keeps the old value.
- The page: the column is present, a chair can edit, a member cannot.

### Task 8: Upload a new DAA from the picker (duos-ui)

- `DAA.createDaa(file, dacId, { setAsDacDefault })`: when the value is `false`, add `?setAsDacDefault=false` to the URL.
- `DatasetDaaCell`: add the option "Upload a new DAA…". It opens the existing `UploadDaaModal`. On save, the cell does three things:
  1. It uploads the file with `setAsDacDefault: false`.
  2. It adds the new DAA to the page's list.
  3. It assigns the new DAA to the dataset.

**Tests:** the URL parameter; the upload does not make the DAA the default, and then assigns it to the dataset; a failed upload shows an error.

### Task 9: Show override-only DAAs on the SO DAA assignment page (duos-ui)

- `researcherViewHelpers.ts`:
  - Add `isAssignableDaa(daa)`. It is true when the DAA has DACs or override datasets.
  - `getDacName` labels an override-only DAA as "Dataset-specific (N datasets)".
- `DaaAssociationsPage.tsx`: filter with `isAssignableDaa`. Today the filter drops DAAs that have no DACs, and its comment says something wrong about such DAAs. Fix the comment too.

**Tests:** the helper, the label, and an override-only DAA that shows on the page.

## Edge cases to cover in review

1. Two required DAAs have the same file name. Expected: both show and both are sent (Task 6).
2. A dataset's override DAA is missing from the list that the UI loaded. Expected: the UI does not fall back to the DAC default (Task 6).
3. A DAA is an override only, so nothing lists it as a DAC default. Expected: SOs can see it and pre-authorize researchers for it (Tasks 4 and 9).
4. A dataset moves to another DAC. Expected: the override clears (Task 1).
5. A chair uploads a DAA for one dataset. Expected: the DAC default does not change (Tasks 3 and 8).

## Deploy order

- Deploy each consent PR before the duos-ui PRs that use it (see the "Depends on" column). The new JSON fields and the query parameter are additive, so the current UI works against the new backend.
- Deploy Task 6 before Task 7 (see Task 6).

## Overlap with DT-4267

The SO-approval-exempt plan changes some of the same code. Whichever branch merges second keeps both sides of each conflict:

- **consent:**
  - `changelog-master.xml`, `Dataset.java`, the dataset mappers, and the six `DatasetDAO` select lists: each plan adds one column.
  - The DAC-change `UPDATE` statements: each plan adds one `SET` item.
  - `Dataset.yaml`: each plan adds one property.
- **`DacResource.findDatasetForChair`:** both plans add the same helper. Keep one copy.
- **`DacProfile.tsx`:** both plans add the same setup (the chair check, `handleDatasetChange`, the row `dataset`, the `columns` memo). Keep one copy, and keep both columns (DAA, then SO Approval).
- **`DataAccessAgreements.tsx`:** this plan replaces `getDisplayedDaaIds` with `getRequiredDaaIds`. DT-4267 filters out exempt datasets before it calls that function. Keep the filter and call `getRequiredDaaIds`.
