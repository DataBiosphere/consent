# DAR Lifecycle: Draft → DAR → Progress Report → Closeout

Read this before changing anything that creates, reads, or filters `data_access_request` rows,
elections, votes, or approval/grant queries. Each rule below exists because an earlier change got
it wrong.

## The four states

All four live in the same `data_access_request` table. Tell them apart by columns, not by JSON.

| State | `collection_id` | `parent_id` | `submission_date` | Created by |
| --- | --- | --- | --- | --- |
| Draft | `NULL` | `NULL` | `NULL` | `DataAccessRequestDAO.insertDraftDataAccessRequest` |
| Original DAR | set | `NULL` | set | `DataAccessRequestService.createDataAccessRequest` |
| Progress report | set | set (parent DAR id) | set, always | `DataAccessRequestDAO.insertProgressReport` |
| Closeout | set | set | set, always | a progress report whose `data.closeoutSupplement` has reasons |

### 1. Draft

- Created by `DataAccessRequestService.insertDraftDataAccessRequest`; edited through
  `updateByReferenceId`, which rejects non-drafts (`SubmittedDARCannotBeEditedException`).
- No collection, no elections, no grants. Drafts never count toward approvals, dashboards, or
  closeout logic.
- **Only an original DAR can be a draft. A progress report is never a draft.**

### 2. Original DAR (submission)

- `createDataAccessRequest` validates (`validateDar`). In one transaction it then creates a
  `dar_collection` (code `DAR-<n>`) or reuses the draft's, and sets `submission_date` through
  `updateDraftToSubmittedForCollection`. It also syncs `dar_dataset`, snapshots DAAs, and flags
  `requires_so_approval` when needed.
- Elections are per (DAR reference id, dataset). A chair opens them
  (`DarCollectionService.createElectionsForDarCollection`), or auto-open DACs and DAC automation
  rules open them (`createElectionsForNewDarCollection`, `DACAutomationRuleService`).
- A grant is an approved FINAL vote on a DataAccess election. Access runs 8760 hours from the
  newest submission in the collection, original or progress report.
- Pre-2022 collections can hold **several original DARs** (one per dataset). Queries that pick
  "the latest DAR" must also read the non-canceled sibling originals; see `DacDashboardDAO` and
  `DarCollectionSummaryDAO`.
- `validateDar` rejects a `closeoutSupplement`. Original DARs cannot close anything out.

### 3. Progress report (renewal)

- `POST /api/dar/v2/progress_report/{parentReferenceId}` →
  `DataAccessRequest.populateProgressReportFromJsonString` (copies the parent's data and overlays
  the report fields) → `DataAccessRequestService.createProgressReport`.
- Preconditions:
  - The caller owns the parent.
  - The parent is not a draft.
  - The parent has no open DataAccess elections.
  - The collection has no submitted closeout.
  - The datasets are a subset of the parent's datasets and are currently approved
    (`findDatasetApprovalsByDar`).
- `insertProgressReport` sets `parent_id`, `collection_id` and `submission_date = now()` in the
  INSERT. There is no draft step and no later "submit". Don't write fixtures or code that
  null out a progress report's `submission_date`.
- A non-closeout report starts a new review cycle: `processNewDarCollection` opens elections for
  auto-open DACs and notifies.

### 4. Closeout (terminal)

- A closeout is a progress report with a `closeoutSupplement` (non-empty `reasons` plus a
  `signingOfficialId` at the submitter's institution). The model check is
  `DataAccessRequest.getIsCloseoutProgressReport()`: it requires `parent_id` and reasons.
- On submission, the closeout:
  - **Covers every dataset in the collection.** The service replaces the client's selection with
    `findDatasetIdsByCollectionId`, which is the union over all submitted DARs. That includes
    datasets that earlier progress reports dropped.
  - **Ends every grant in the collection immediately.** It does not wait for SO approval or DAC
    acknowledgement. `approveDataAccessRequestCloseout` only records the SO and notifies chairs.
  - Skips dataset-registration approval checks, dataset-dependent collaboration/ethics documents,
    DAA acknowledgements, and election creation.
- After it, the collection is closed. All of these are rejected:
  - Votes and rationale updates (409).
  - Opening or reopening elections (409).
  - Chair cancellation (409).
  - Another progress report (400).
- Every role sees the collection as `COMPLETE` (`DarCollectionService.applyCloseoutStatus`).
  Member and chair views offer no vote actions, and the DAC dashboard excludes it from
  "awaiting my vote" for chairs and members.
- The duos-ui closeout form shows the full collection dataset list with removal disabled.

## SQL rules for closeout predicates

Write a "this collection is closed out" predicate exactly like this:

```sql
submission_date IS NOT NULL
AND parent_id IS NOT NULL
AND data ->> 'closeoutSupplement' IS NOT NULL
```

- `parent_id IS NOT NULL` is the key condition. Without it, a stray supplement on an original DAR
  would close the collection.
- `submission_date IS NOT NULL` is redundant for progress reports, but keep it so drafts can
  never match.
- A closeout applies to the **whole collection**: compare on `collection_id`, not on the
  closeout's own `dar_dataset` rows.
- When a query **projects** the supplement (rather than filtering on it), gate the column on
  the parent: `CASE WHEN dar.parent_id IS NOT NULL THEN dar.data ->> 'closeoutSupplement' END`.
  Callers such as `DarCollectionService` treat a non-null `closeout` as "closed out".
- Every query that detects a closeout applies this rule. Keep it that way:
  - Grants and approvals: `DataAccessRequestDAO` (`hasSubmittedCloseout*`,
    `findApprovedDARsByDatasetId`, `findDatasetApprovalsByDar`, the `closeouts` CTEs),
    `DatasetDAO.getApprovedDatasets`, `ResearcherDashboardDAO`.
  - Collection summaries and dashboards: `DarCollectionSummaryDAO` (the `closeout` column),
    `DacDashboardDAO` (`has_closeout`), `SigningOfficialDashboardDAO` (`needs_so`,
    `awaiting_so`).
  - Metrics: `DarMetricsDAO` (`EXPIRATIONS`, the SO-approval `kind`, `RENEWALS`).
- When you add a query that detects a closeout, apply the rule. Test it with a real closeout
  (a progress report with a parent) **and** with a supplement on an original DAR, which must not
  close anything.

## Concurrency

Closeout submission, election creation/cancellation, and vote/rationale updates serialize on the
collection row:

1. Open a transaction (`dataAccessRequestDAO.inTransaction(...)` or
   `DataAccessRequestServiceDAO.inTransaction`).
2. `lockCollection(collectionId)` (or `lockCollectionsForReferenceIds`, which locks in a stable
   order).
3. Re-check `hasSubmittedCloseout` **after** taking the lock. A preflight check outside the lock
   is fine for fast failure, but never rely on it alone.
4. Mutate, then commit. The lock is held until commit.

JDBI 3 (3.52.x) binds one handle per thread: `Jdbi.withHandle` reuses the thread's `handleScope`,
and `Handle.inTransaction` joins an open transaction. So on-demand DAOs and nested
`jdbi.useTransaction` calls inside that callback share the same connection and transaction. Code
review bots have repeatedly claimed this opens a second pooled connection; it does not.
`CollectionCloseoutTransactionTest` proves it. Consequences:

- Never call `handle.commit()` or `setAutoCommit(false)` inside a helper that might run nested.
  It would commit the caller's transaction early and drop the lock.
- Do the locked work on the same thread. Executors and async callbacks get a new handle.

## Mistakes that have already happened

| Mistake | Why it's wrong | Correct approach |
| --- | --- | --- |
| Treating any DAR with `closeoutSupplement` as a closeout | Originals can't close out; a stray JSON field closed whole collections | Require `parent_id IS NOT NULL`; `validateDar` rejects the field |
| Modelling "draft closeouts" (a progress report with `submission_date` NULL) in code or fixtures | Progress reports are never drafts | Test "supplement on an original DAR never ends grants" instead |
| Test fixtures inserting closeouts with `insertDataAccessRequest` (no parent) | Doesn't match production, so it passes the wrong predicates | File closeouts with `insertProgressReport` on an approved parent |
| Trusting the client's dataset list for a closeout | Clients could omit datasets to skip validation or keep access | Expand to `findDatasetIdsByCollectionId` before validation and again under the lock |
| Special-casing closeout status for one role only | Other roles showed `IN_PROCESS` or `SUBMITTED` for a closed collection | Apply closeout status in every role processor |
| Excluding closeouts from only the chair branch of the DAC dashboard | Members saw a vote count with nothing to vote on | Gate the whole "awaiting my vote" count on `NOT has_closeout` |
| Checking for a closeout outside the transaction | TOCTOU: a closeout could commit between the check and the write | Lock, re-check, mutate in one transaction |
| Comparing React props by identity in duos-ui `ProgressReportApplication` | The parent passes a freshly `merge()`d `dar` each render, which wiped the researcher's dataset removals | Key resets on dataset ids plus closeout mode |

## Verification checklist

- DAO and migration tests run locally (Testcontainers + Docker). Run them; don't defer to CI. Run
  DAO suites one at a time: they all bind port 8180.
- For grant or approval queries, test three cases: no closeout, a real closeout (progress report
  with a parent), and a supplement on an original DAR (must not close anything).
- Check real data shapes against the `localdb` prod dump (schema `consent`) before tightening a
  predicate.
- Sonar flags `throws` clauses left behind after you change a signature, multi-call
  `assertThrows` lambdas (S5778), and locals that hide fields (S1117). Re-check the callers' tests
  when you change a `throws` clause.
