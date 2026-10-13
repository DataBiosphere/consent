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
| 2 | consent | Leave exempt datasets out of the SO decision | ~200 lines | 1 |
| 3 | consent | Endpoint for chairs to set the flag | ~300 lines | 1 |
| 4 | consent | `NOT_REQUIRED` value in the search index | ~80 lines | 1 |
| 5 | duos-ui | Exemption toggle on the DAC profile page | ~350 lines | 3 deployed |
| 6 | duos-ui | DAR form hint and SO queue test | ~80 lines | 1 deployed |
| 7 | duos-ui | Data Library chip and filter | ~120 lines | 4 deployed |

Sizes include tests. Tasks 2, 3, and 4 can go in any order after Task 1. Tasks 6 and 7 are small and can share one PR.

## Tasks

### Task 1: Store the exemption flag on the dataset (consent)

Add the column and carry it through the model and the DAO layer.

- Liquibase changeset: add `dataset.so_approval_exempt BOOLEAN NOT NULL DEFAULT FALSE`.
- `Dataset.java`: add `soApprovalExempt`. `DatasetMapper` and `DatasetReducer`: map the column (follow the `dac_approval` pattern).
- `DatasetDAO`:
  - Add `so_approval_exempt` to the six dataset select lists that already select `d.dac_approval`.
  - Add `updateDatasetSoApprovalExempt(exempt, updateDate, updateUserId, datasetId)`.
  - Add `findSoApprovalExemptDatasetIds(datasetIds)`.
- Reset the flag when the DAC changes (decision 4). This touches `DatasetDAO.updateDatasetDacId` and `updateDatasetByDatasetId`, and the two dataset `UPDATE` statements in `DacServiceDAO` (DAC delete and externalize). In the two `DatasetDAO` methods, reset the flag only when the DAC id really changes:

  ```sql
  so_approval_exempt =
      CASE WHEN dac_id IS DISTINCT FROM :dacId THEN FALSE ELSE so_approval_exempt END,
  dac_id = :dacId
  ```

  PostgreSQL reads the old row in every `SET` expression, so the `CASE` sees the old `dac_id`.

**Tests:** new datasets are not exempt; the flag can be set and cleared; the filter query returns only exempt ids; a DAC change resets the flag; an unchanged DAC keeps it; DAC delete and externalize reset it.

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

- Apply the same filter to the pre-authorization check in `createProgressReport`.
- `isUserPreAuthorizedForAllDaas` returns `true` for an empty list, with no query.
- Do not change `hasAcknowledgedRequiredDaas`. Researchers still acknowledge every DAA (decision 3).

**Tests:**
- All datasets exempt, the researcher has no pre-authorization, and the DAC has the SO rule: no SO approval, and RADAR runs.
- Mixed, where only the exempt dataset's DAC has the rule: no SO approval.
- Mixed, where a non-exempt dataset's DAC has the rule: SO approval.
- A progress report on exempt datasets only: no SO approval.

Existing tests keep passing with no change, because an unstubbed `findSoApprovalExemptDatasetIds` returns an empty list.

### Task 3: Endpoint for chairs to set the flag (consent)

`PUT /api/dac/{dacId}/dataset/{datasetId}/so-approval-exemption`, body `{"exempt": true|false}`, role `CHAIRPERSON`. It returns the updated `Dataset`.

- `DacResource`: copy the pattern of `approveDataset` (`PUT /api/dac/{dacId}/dataset/{datasetId}`). Move its two checks into a private helper, `findDatasetForChair`, and use the helper in both endpoints. The checks are "the dataset belongs to this DAC" and "the user is a chair of this DAC". Both return 404, as today.
- New request model `DatasetSoApprovalExemption` (copy `DatasetApproval`). Return 400 if the body is empty or `exempt` is missing.
- `DatasetService.updateSoApprovalExemption(dataset, user, exempt)`: update the row, reindex the dataset (as `approveDataset` does), and return the fresh dataset.
- OpenAPI: a new path file, the request schema, and `soApprovalExempt` on `Dataset.yaml`.

**Tests:** service (update and reindex), resource (success, wrong DAC, not a chair, bad body), and the existing `approveDataset` tests after the helper refactor.

### Task 4: `NOT_REQUIRED` value in the search index (consent)

- Add `NOT_REQUIRED` to `SoApprovalModel`.
- In `ElasticSearchService`, when a dataset is exempt, set `soApprovalModel` to `NOT_REQUIRED` before the rule-based check. Datasets with no DAC keep `PRE_AUTHORIZED`. Such a dataset cannot be exempt, because Task 1 resets the flag with the DAC.
- Add the value to the `soApprovalModel` enum in `DatasetSearch.yaml`.

**Tests:** an exempt dataset maps to `NOT_REQUIRED`, also when its DAC has the SO rule.

**Unusual step:** none. No dataset is exempt at deploy time, so no reindex is needed. Task 3 reindexes each dataset when a chair changes its flag.

### Task 5: Exemption toggle on the DAC profile page (duos-ui)

- Types: `Dataset.soApprovalExempt?: boolean`. Add `'NOT_REQUIRED'` to `DatasetTerm.soApprovalModel`.
- `DAC.updateSoApprovalExemption(dacId, datasetId, exempt)` in `src/libs/ajax/DAC.ts`.
- New component `DatasetSoExemptionCell`:
  - A chair sees a switch labeled "Required" or "Not required", with a tooltip that explains the effect and says that it applies to new requests only.
  - Everyone else sees the text only.
  - If the save fails, show an error and leave the switch in its old position.
- `DacProfile.tsx`: add an "SO Approval" column to the datasets grid. This needs some setup in the page:
  - a chair check: `user.roles` has `Chairperson` for this `dacId`. Do not include admins, because the backend allows chairs only.
  - a `handleDatasetChange` handler that merges the returned dataset into its row.
  - the full `dataset` object on each grid row.
  - the column list as a `useMemo`.

**Tests:** the cell (read-only text, a toggle that saves, a failed save that reverts) and the page (the column is present, a chair can edit, a member cannot).

### Task 6: DAR form hint and SO queue test (duos-ui)

- `DataAccessAgreements.tsx` shows "Your Institutional Signing Official must approve you…" when the researcher lacks a required DAA. Compute that list from the non-exempt datasets only. The DAA table and the acknowledgment do not change.
- `SigningOfficialDarApprovals.spec.tsx`: add a test that a collection with `requiresSOApproval: false` does not show. The page already filters on that flag. This test documents the acceptance criterion and guards it.

**Tests:** the hint is hidden when only exempt datasets need the DAA, and shown when a non-exempt dataset also needs it.

### Task 7: Data Library chip and filter (duos-ui)

- Add `'not-required'` to `SoApprovalModel` in `src/types/library.ts`, and map `NOT_REQUIRED` to it in `DatasetUtils.getSoApprovalModelByDatasetId`.
- `datasetColumns.tsx`: show the chip "SO Approval Not Required" with a neutral color and a tooltip. Replace the two-way `isPerRequestApproval` branches with one lookup table, keyed by model.
- `useLibraryPageState.ts`: add the filter option `NOT_REQUIRED`.
- Check that `SoApprovalReminderBanner` shows no banner for `'not-required'`.

**Tests:** the mapping, the chip, and the banner.

## Edge cases to cover in review

1. Every dataset is exempt, but the researcher has no pre-authorization and the DAC has the SO rule. Expected: no SO approval, and RADAR runs (Task 2).
2. A DAR mixes exempt and non-exempt datasets. Only the non-exempt datasets count, in both directions (Task 2).
3. A progress report covers exempt datasets only. Expected: no SO approval (Task 2).
4. A dataset moves to another DAC. Expected: the flag resets (Task 1).
5. A chair sets the flag while a DAR on that dataset waits for SO approval. Expected: the DAR keeps waiting (decision 2). Confirm this with product before release.

## Deploy order

Deploy the consent PRs before the duos-ui PRs that use them (see the "Depends on" column). The new JSON field and enum value are additive, so the current UI works against the new backend.

## Overlap with DT-3876

The per-dataset DAA plan changes some of the same code. Whichever branch merges second keeps both sides of each conflict:

- **consent:**
  - `changelog-master.xml`, `Dataset.java`, the dataset mappers, and the six `DatasetDAO` select lists: each plan adds one column.
  - The DAC-change `UPDATE` statements: each plan adds one `SET` item.
  - `Dataset.yaml`: each plan adds one property.
- **`DacResource.findDatasetForChair`:** both plans add the same helper. Keep one copy.
- **`DacProfile.tsx`:** both plans add the same setup (the chair check, `handleDatasetChange`, the row `dataset`, the `columns` memo). Keep one copy, and keep both columns (DAA, then SO Approval).
- **`DataAccessAgreements.tsx`:** DT-3876 replaces `getDisplayedDaaIds` with `getRequiredDaaIds`. Apply this plan's exempt-dataset filter to whichever function is there.
