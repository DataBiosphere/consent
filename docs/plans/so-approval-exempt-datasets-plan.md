# SO-Approval-Exempt Datasets Plan

## Status

Proposed. Ticket: [DT-4267](https://broadworkbench.atlassian.net/browse/DT-4267).

Related plan: `dataset-daa-override-plan.md` ([DT-3876](https://broadworkbench.atlassian.net/browse/DT-3876)) changes some of the same files. The two plans do not depend on each other. See [Overlap with DT-3876](#overlap-with-dt-3876).

## Overview

A DAC chair can mark a dataset as **SO-approval exempt**. A DAR on exempt datasets only does not need Signing Official (SO) approval. It skips the SO approval queue and goes straight to DAC review (or to RADAR, if the DAC uses it).

### How SO approval works today

At DAR submit, `DataAccessRequestService.requiresSOApproval` decides whether the DAR needs SO approval. The answer is yes if either condition is true:

- a dataset's DAC has the `REQUIRE_SO_DAR_APPROVAL` automation rule, or
- the researcher's library card does not hold every DAA that the datasets need.

The service stores the answer in `data_access_request.requires_so_approval`. Every later step reads that one flag: the SO approval queue, the SO "Approve" action, the DAC "Open" action, auto-open of elections, RADAR, and the notification emails. Progress reports use the DAA part of the same check.

Note: the ticket says that the requester's institution drives SO approval. The code does not use the institution for this decision. It uses only the two conditions above.

### The change

1. Add `dataset.so_approval_exempt` (boolean, default `false`).
2. At DAR and progress-report submit, run the existing check over the **non-exempt** datasets only. If every dataset is exempt, the DAR does not need SO approval.
3. Give DAC chairs an endpoint and a toggle on the DAC profile page to set the flag.
4. Show the exemption to researchers (DAR form) and in the Data Library.

The stored flag stays the single gate, so the SO queue and the DAC workflow need no change.

SOs still get the informational "DAR submitted" email (`notifySigningOfficialsOfDARSubmission`) for a DAR on exempt datasets. That email goes out for every DAR today, including DARs that need no SO approval, and this plan does not change it. Only the "approval needed" email (`notifySpecificSigningOfficialOfApprovalNeeded`) depends on the flag.

## Decisions

| # | Question | Decision | Source |
|---|---|---|---|
| 1 | A DAR has exempt and non-exempt datasets. Is SO approval partial? | No. The SO approves the whole DAR. Only the non-exempt datasets decide whether SO approval is needed. | Ticket answer |
| 2 | Is the exemption retroactive? | No. The decision is made and stored at submit. A DAR that already waits for SO approval keeps waiting after a chair sets the flag. | **Confirm with product** |
| 3 | Do exempt datasets still need DAA acknowledgment? | Yes. The exemption removes only the SO step. | This plan |
| 4 | What happens when a dataset moves to another DAC, or its DAC is deleted or externalized? | The flag resets to `false`. The exemption is a decision of the old DAC. | This plan |
| 5 | Who can set the flag? | Chairpersons of the dataset's DAC, the same rule as dataset approval. | This plan |
| 6 | Where do chairs set it? | The "Datasets Managed by this DAC" grid on the DAC profile page. The `/dac_datasets` grid reads the search index and stays read-only. | This plan |
| 7 | How does the Data Library show it? | A new search-index value, `SoApprovalModel.NOT_REQUIRED`, with the chip "SO Approval Not Required". | This plan |

## Task summary

| # | Repo | Task | Size (est.) | Depends on |
|---|---|---|---|---|
| 1 | consent | Store the exemption flag on the dataset | ~300 lines | — |
| 2 | consent | Leave exempt datasets out of the SO decision | ~250 lines | 1 |
| 3 | consent | `NOT_REQUIRED` value in the search index, and a reindex after DAC delete | ~150 lines | 1 |
| 4 | consent | Endpoint for chairs to set the flag | ~300 lines | 2 and 3 deployed; decision 2 confirmed |
| 5 | duos-ui | Exemption toggle on the DAC profile page | ~350 lines | 4 deployed |
| 6 | duos-ui | DAR form hint and SO queue test | ~100 lines | 1 deployed |
| 7 | duos-ui | Data Library chip, filter, and banner | ~150 lines | 3 deployed |

Sizes include tests. Tasks 2 and 3 can go in any order after Task 1. Tasks 6 and 7 are small and can share one PR.

**The endpoint (Task 4) is the only way to set the flag, so it ships after everything that reads the flag.** Until Task 4 deploys, no dataset can be exempt. When it deploys, the SO decision (Task 2) and the search index (Task 3) already handle exempt datasets. A chair never sees a dataset marked "Exempt" while new DARs on it still go to the SO queue, and no catch-up reindex is needed.

## Tasks

### Task 1: Store the exemption flag on the dataset (consent)

Add the column and carry it through the model and the DAO layer.

- Liquibase changeset: add `dataset.so_approval_exempt BOOLEAN NOT NULL DEFAULT FALSE`.
- `Dataset.java`: add `soApprovalExempt`. `DatasetMapper` and `DatasetReducer`: map the column (follow the `dac_approval` pattern).
- `DatasetDAO`:
  - Add `so_approval_exempt` to the six dataset select lists that already select `d.dac_approval`.
  - Add `updateDatasetSoApprovalExempt(exempt, updateDate, updateUserId, datasetId, dacId)`. It returns the number of updated rows. Its `WHERE` clause also checks the DAC, so that a write from a stale DAC changes nothing (see Task 4):

    ```sql
    WHERE dataset_id = :datasetId AND dac_id = :dacId
    ```

  - Add `findSoApprovalExemptDatasetIds(datasetIds)`.
- OpenAPI: add `soApprovalExempt` to `Dataset.yaml`. Dataset responses carry the field as soon as this task deploys, and Task 6 reads it before Task 4 ships.
- Reset the flag when the DAC changes (decision 4). This touches `DatasetDAO.updateDatasetDacId` and `updateDatasetByDatasetId`, and the two dataset `UPDATE` statements in `DacServiceDAO` (DAC delete and externalize). In the two `DatasetDAO` methods, reset the flag only when the DAC id really changes:

  ```sql
  so_approval_exempt =
      CASE WHEN dac_id IS DISTINCT FROM :dacId THEN FALSE ELSE so_approval_exempt END,
  dac_id = :dacId
  ```

  PostgreSQL reads the old row in every `SET` expression, so the `CASE` sees the old `dac_id`.

**Tests:**
- New datasets are not exempt.
- The flag can be set and cleared.
- An update with the wrong DAC id changes no row and returns 0.
- The filter query returns only exempt ids.
- A DAC change resets the flag. An unchanged DAC keeps it.
- DAC delete and externalize reset the flag.
- A change to the flag does not change `requires_so_approval` on a DAR that was already submitted (decision 2).

### Task 2: Leave exempt datasets out of the SO decision (consent)

In `DataAccessRequestService`, run the existing SO check over the non-exempt datasets only. This is the core of the ticket:

```java
private boolean requiresSOApproval(User user, List<Integer> datasetIds) {
  List<Integer> subjectIds = findDatasetIdsSubjectToSOApproval(datasetIds); // drops exempt ids
  if (subjectIds.isEmpty()) {
    return false;
  }
  return !datasetDAO.filterDatasetIdsByAutomationRuleType(subjectIds, REQUIRE_SO_DAR_APPROVAL).isEmpty()
      || !isUserPreAuthorizedForAllDaas(user, subjectIds);
}
```

- Apply the same filter to the pre-authorization check in `createProgressReport`. A progress report reads the flag when it is submitted, not when its parent DAR was submitted.
- `isUserPreAuthorizedForAllDaas` returns `true` for an empty list, with no query.
- Do not change `hasAcknowledgedRequiredDaas`. Researchers still acknowledge every DAA, including the DAAs of exempt datasets (decision 3).

**Tests.** The two conditions (the DAC rule and DAA pre-authorization) each need their own cases, with the other condition held off:

| Datasets | DAC rule | Researcher's pre-authorization | Expected |
|---|---|---|---|
| All exempt | on | none | No SO approval, and RADAR runs |
| Exempt (rule on) + non-exempt (rule off) | mixed | has every DAA | No SO approval |
| Exempt (rule off) + non-exempt (rule on) | mixed | has every DAA | SO approval |
| Exempt (DAA X) + non-exempt (DAA Y) | off | has Y, not X | No SO approval |
| Exempt (DAA X) + non-exempt (DAA Y) | off | has X, not Y | SO approval |
| Exempt and non-exempt that share DAA X | off | not X | SO approval (the non-exempt dataset needs X) |

Also test:
- A DAR on exempt datasets only still fails validation if a DAA is not acknowledged.
- A progress report on exempt datasets only needs no SO approval.
- A progress report uses the flag at its own submit time. Its parent was submitted when the dataset was not exempt, and the progress report is submitted after the flag is set: it needs no SO approval.

Existing tests keep passing with no change, because an unstubbed `findSoApprovalExemptDatasetIds` returns an empty list.

### Task 3: `NOT_REQUIRED` value in the search index (consent)

- Add `NOT_REQUIRED` to `SoApprovalModel`.
- In `ElasticSearchService.toDatasetTerm`, today one branch sets two fields together when the DAC rules resolve: `soApprovalModel` and `instantApprovalEligible`. Keep them independent:
  - `soApprovalModel`: if the dataset is exempt, set `NOT_REQUIRED`. The flag alone decides this, so set it also when the rules could not be resolved (`enabledRulesByDacId == null`). Otherwise keep the current rule-based value.
  - `instantApprovalEligible`: no change. It still comes from the DAC rules, and it stays unset when the rules could not be resolved. Exempt datasets still go through RADAR (decision 1), so they keep this value.
  - Datasets with no DAC keep `PRE_AUTHORIZED`. Such a dataset cannot be exempt, because Task 1 resets the flag with the DAC.
- Add the value to the `soApprovalModel` enum in `DatasetSearch.yaml`.
- Reindex the datasets of a deleted DAC. Task 1 resets the flag in the database when a DAC is deleted, but `DacService.deleteDac` does not reindex today, so an exempt dataset would keep `NOT_REQUIRED` in the Data Library. Copy the pattern of the externalize flow (`DacService`, around L326): read the DAC's dataset ids before the delete, and call `indexDatasets` for them after the delete succeeds. Log a failed reindex; do not fail the delete. This also fixes the other DAC fields that go stale in the index after a delete today.

**Tests:**
- An exempt dataset maps to `NOT_REQUIRED`, also when its DAC has the SO rule.
- An exempt dataset has the same `instantApprovalEligible` value as a non-exempt dataset with the same DAC rules and data use.
- When the rules cannot be resolved, an exempt dataset still maps to `NOT_REQUIRED`, and `instantApprovalEligible` stays unset.
- `deleteDac` reindexes the DAC's datasets after the delete, and a failed reindex does not fail the delete.

No reindex is needed at deploy. No dataset can be exempt until Task 4 deploys, and Task 4 reindexes each dataset when a chair changes its flag. For a reindex that fails at that time, see Task 4.

### Task 4: Endpoint for chairs to set the flag (consent)

`PUT /api/dac/{dacId}/dataset/{datasetId}/so-approval-exemption`, body `{"exempt": true|false}`, role `CHAIRPERSON`. It returns the updated `Dataset`.

- `DacResource`: copy the pattern of `approveDataset` (`PUT /api/dac/{dacId}/dataset/{datasetId}`). Move its two checks into a private helper, `findDatasetForChair`, and use the helper in both endpoints. The checks are "the dataset belongs to this DAC" and "the user is a chair of this DAC". Both return 404, as today.
- New request model `DatasetSoApprovalExemption` (copy `DatasetApproval`). Return 400 if the body is empty or `exempt` is missing.
- `DatasetService.updateSoApprovalExemption(dataset, user, exempt)`:
  1. Update the row with the DAC id from the request.
  2. If no row changed, the dataset moved to another DAC after the resource checked it. An admin or a chair can move a dataset (for example, through `PUT /api/dataset/v3/{datasetId}`). Throw `ConsentConflictException` (409) and do not reindex.
  3. Otherwise, reindex the dataset and return the fresh dataset. Use `elasticSearchService.synchronizeDatasetInESIndex(dataset, true)`, not the `indexDatasets` call that `approveDataset` makes. `synchronizeDatasetInESIndex` closes the response and logs a non-success status as well as an `IOException`. A failed reindex does not fail the request, because the database update is already done.
- OpenAPI: a new path file, the request schema, and the 409 response.

A failed reindex leaves only the Data Library chip and filter stale. The SO decision (Task 2) reads the database, not the index, so DARs are not affected. This is the same best-effort behavior as the reindex after a DAC rule toggle (`DACAutomationRuleService.reindexDatasetsForRuleChange`), which changes the same index field. To repair a stale entry, an admin calls `POST /api/dataset/index/{datasetId}`. The log warning names the dataset id. consent has no durable retry for index writes today. If the team wants one, it belongs in its own ticket, for every index write.

**Tests:**
- Service: update and reindex; a stale DAC (the DAO returns 0) gives 409 with no reindex, both to set and to clear the flag; a failed reindex still returns the updated dataset.
- Resource: success, wrong DAC, not a chair, bad body.
- The existing `approveDataset` tests after the helper refactor.

**Release gate:** confirm decision 2 (not retroactive) with product before this task deploys. This task is the first that lets a dataset become exempt.

### Task 5: Exemption toggle on the DAC profile page (duos-ui)

- Types: `Dataset.soApprovalExempt?: boolean`. Add `'NOT_REQUIRED'` to `DatasetTerm.soApprovalModel`.
- `DAC.updateSoApprovalExemption(dacId, datasetId, exempt)` in `src/libs/ajax/DAC.ts`.
- New component `DatasetSoExemptionCell`:
  - The cell shows the exemption state, not the outcome of a DAR. A non-exempt dataset does not always need SO approval: the normal check can still skip it (no DAC rule, and the researcher is pre-authorized). So the labels are "Exempt" and "Not exempt", never "Required" or "Not required".
  - A chair sees a switch labeled "SO approval exempt", with the state next to it. A tooltip explains the effect and says that it applies to new requests only.
  - Everyone else sees the text "Exempt" or "Not exempt".
  - If the save fails, show an error and leave the switch in its old position. On a 409, also tell the chair to reload the page, because the dataset moved to another DAC.
- `DacProfile.tsx`: add an "SO Approval" column to the datasets grid. This needs some setup in the page:
  - a chair check: `user.roles` has `Chairperson` for this `dacId`. Do not include admins, because the backend allows chairs only.
  - a `handleDatasetChange` handler that merges the returned dataset into its row.
  - the full `dataset` object on each grid row.
  - the column list as a `useMemo`.

**Tests:** the cell (the "Exempt" and "Not exempt" text, a toggle that saves, a failed save that reverts, the 409 message) and the page (the column is present, a chair can edit, a member cannot).

### Task 6: DAR form hint and SO queue test (duos-ui)

`DataAccessAgreements.tsx` computes two lists from the selected datasets. Only one of them changes:

| List | Use | Change |
|---|---|---|
| The required DAA ids (`getDisplayedDaaIds`, sent through `onDaaIdsChange`) | The DAA table, and the DAAs that the DAR acknowledges | **None.** It keeps every dataset, exempt or not. The backend rejects a DAR that does not acknowledge every DAA. |
| `missingDaaIds` | The hint about SO approval | Compute it from the non-exempt datasets only. |

Do not put the exemption filter inside `getDisplayedDaaIds`. Filter the datasets only where `missingDaaIds` is computed.

The hint today says "…must approve you under **each** data access agreement above…". With a mixed selection, the table also lists DAAs that only exempt datasets need, so "each … above" is no longer true. Change the hint to name the DAAs in `missingDaaIds`: "Your Institutional Signing Official must approve you under these data access agreements before a data access committee can review your request: <file names>."

`SigningOfficialDarApprovals.spec.tsx`: the page keeps a collection when `requiresSOApproval` is true **or** its actions include `Review_Progress_Report` (closeout review). Add two tests, and do not change the filter:
- A collection with `requiresSOApproval: false` and no `Review_Progress_Report` action does not show. This documents the acceptance criterion.
- A collection with `requiresSOApproval: false` and the `Review_Progress_Report` action still shows. This guards the closeout review workflow.

**Tests:**
- The hint is hidden when only exempt datasets need the DAA, and shown when a non-exempt dataset also needs it.
- With a mixed selection, the hint names only the DAAs that non-exempt datasets need.
- `onDaaIdsChange` still receives the DAAs of exempt datasets.

### Task 7: Data Library chip, filter, and banner (duos-ui)

- Add `'not-required'` to `SoApprovalModel` in `src/types/library.ts`, and map `NOT_REQUIRED` to it in `DatasetUtils.getSoApprovalModelByDatasetId`.
- `datasetColumns.tsx`: show the chip "SO Approval Not Required" with a neutral color and a tooltip. Replace the two-way `isPerRequestApproval` branches with one lookup table, keyed by model.
- `useLibraryPageState.ts`: add the filter option `NOT_REQUIRED`.
- `SoApprovalReminderBanner` is a page-level notice. Today it gets only `showsPerDatasetIndicator`, and it always says that DACs require SO approval. That statement is no longer always true. Change it as follows:
  - New wording: "Most data access committees require a Signing Official to approve either the researcher or each request before they review it." With the per-dataset indicator, add: "Each dataset below shows which approval model applies, including datasets that need no Signing Official approval."
  - Add the prop `allResultsSoApprovalNotRequired`. `LibraryPageShell` sets it to `true` when the result list is not empty and every model in `soApprovalModelByDatasetId` is `'not-required'`. When it is `true`, the shell does not render the banner.
  - For mixed results, or when the models are unknown, render the banner with the new wording.

**Tests:**
- The mapping and the chip.
- The banner shows the new wording, with and without the per-dataset sentence.
- The shell hides the banner when every result is `'not-required'`.
- The shell shows the banner for mixed results, for an empty result list, and when there is no model map.

## Edge cases to cover in review

1. Every dataset is exempt, but the researcher has no pre-authorization and the DAC has the SO rule. Expected: no SO approval, and RADAR runs (Task 2).
2. A DAR mixes exempt and non-exempt datasets. Only the non-exempt datasets count, for the DAC rule and for DAA pre-authorization separately. A DAA that an exempt and a non-exempt dataset share still counts (Task 2).
3. Exempt datasets still need DAA acknowledgment, on the server and in the form (Tasks 2 and 6).
4. A progress report uses the flag at its own submit time (Task 2).
5. A dataset moves to another DAC, also in the middle of a chair's save. Expected: the flag resets, and a stale write gets 409 (Tasks 1 and 4).
6. A chair sets the flag while a DAR on that dataset waits for SO approval. Expected: the DAR keeps waiting (decision 2, Task 1 test).

## Deploy order

1. Task 1, then Tasks 2 and 3 (consent). Task 6 can deploy any time after Task 1. Task 7 can deploy any time after Task 3.
2. Confirm decision 2 with product.
3. Task 4 (consent). This is the first change that lets a dataset become exempt.
4. Task 5 (duos-ui).

The new JSON field and enum value are additive, so the current UI works against the new backend at every step.

## Overlap with DT-3876

The per-dataset DAA plan changes some of the same code. Whichever branch merges second keeps both sides of each conflict:

- **consent:**
  - `changelog-master.xml`, `Dataset.java`, the dataset mappers, and the six `DatasetDAO` select lists: each plan adds one column.
  - The DAC-change `UPDATE` statements: each plan adds one `SET` item.
  - `Dataset.yaml`: each plan adds one property.
- **`DacResource.findDatasetForChair`:** both plans add the same helper. Keep one copy.
- **`DacProfile.tsx`:** both plans add the same setup (the chair check, `handleDatasetChange`, the row `dataset`, the `columns` memo). Keep one copy, and keep both columns (DAA, then SO Approval).
- **`DataAccessAgreements.tsx`:** DT-3876 replaces `getDisplayedDaaIds` with `getRequiredDaaIds`. Keep the exemption filter out of that function, because its result is the list of DAAs that the DAR acknowledges. Apply the filter only where `missingDaaIds` is computed: `getRequiredDaaIds(datasets.filter(d => !d.soApprovalExempt), daas)`.
