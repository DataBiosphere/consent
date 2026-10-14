# DAR Lifecycle: Draft, DAR, Progress Reports, Closeout

Read this before changing anything that creates, reads, or filters `data_access_request` rows,
elections, votes, or approval/grant queries. Each rule below exists because an earlier change got
it wrong.

## The flow

```text
(Draft) ──► Original DAR ─────────────────────────────────────────► Closeout
                    └──► Progress report ──► … (0 or more reports) ──┘
```

- **Drafts are optional.** `createDataAccessRequest` submits an existing draft or inserts a new
  DAR directly.
- **Progress reports are optional.** A researcher can close out straight from the original DAR
  (draft → submitted DAR → closeout), without filing a progress report first.
- **A closeout is also optional.** A collection with no closeout keeps each grant until that
  grant's term ends (8760 hours from the approved DAR's own `submission_date`) and then simply
  expires.
- Never assume a closeout's parent is a progress report, or that a collection contains a
  non-closeout progress report.

## The four states

All four live in the same `data_access_request` table. Tell them apart by columns, plus one JSON
field: `data.closeoutSupplement` is the only thing that separates a closeout from an ordinary
progress report. The only reliable draft marker is `submission_date IS NULL`: a draft can carry a
`collection_id`.

| State | `collection_id` | `parent_id` | `submission_date` | Created by |
| --- | --- | --- | --- | --- |
| Draft | `NULL`, or set (a canceled collection reset to draft) | `NULL` | `NULL` | `DataAccessRequestService.insertDraftDataAccessRequest` |
| Original DAR | set | `NULL` | set | `DataAccessRequestService.createDataAccessRequest` |
| Progress report | set | set (parent DAR id) | set at insert; resubmit can null it | `DataAccessRequestDAO.insertProgressReport` |
| Closeout | set | set (the original DAR or a progress report) | set, always (resubmit is rejected) | a progress report whose `data.closeoutSupplement` has reasons |

### 1. Draft

- Created by `DataAccessRequestService.insertDraftDataAccessRequest`; edited through
  `updateByReferenceId`, which rejects non-drafts (`SubmittedDARCannotBeEditedException`).
- A new draft has no collection. Resubmitting a canceled collection
  (`DarCollectionService.updateCollectionToDraftStatus`) turns its DARs back into drafts: it nulls
  `submission_date` but keeps `collection_id`, and `createDataAccessRequest` later reuses that
  collection. So never test `collection_id IS NULL` to find drafts.
- No elections, no grants. Drafts never count toward approvals, dashboards, or closeout logic.
- **Only an original DAR is *created* as a draft.** Progress reports are inserted already
  submitted, but resubmit nulls `submission_date` on **every** DAR in the collection, so a
  progress report in a resubmitted collection is a draft too (with `parent_id` set). Resubmit
  needs a canceled DAR in the collection, so this mostly affects pre-2022 collections with a
  canceled sibling original. A closed-out collection can't be resubmitted (409, under the
  collection lock), so a closeout never loses its `submission_date`.

### 2. Original DAR (submission)

- `createDataAccessRequest` validates (`validateDar`). In one transaction it then creates a
  `dar_collection` (code `DAR-<n>`) or reuses the draft's, and sets `submission_date`:
  `updateDraftToSubmittedForCollection` when submitting an existing draft, or
  `insertDataAccessRequest` when there is no draft. It also syncs `dar_dataset`, snapshots DAAs,
  and flags `requires_so_approval` when needed.
- Every write to `submission_date`: those two, `insertProgressReport` (`now()`),
  `updateDataByReferenceId`, which resubmit calls with `NULL`, and
  `DataAccessRequestServiceDAO.updateByReferenceId`, which saves draft edits (only drafts reach
  it, so it writes `NULL` back).
- Elections are per (DAR reference id, dataset). A chair opens them
  (`DarCollectionService.createElectionsForDarCollection`), or auto-open DACs and DAC automation
  rules open them (`createElectionsForNewDarCollection`, `DACAutomationRuleService`).
- A grant is a (DAR, dataset) pair whose most recent `final` or `radar_approve` vote on a
  DataAccess election approved it. Access runs 8760 hours from **that DAR's own**
  `submission_date` (`DataAccessRequestDAO.findApprovedDARsByDatasetId`,
  `DatasetDAO.getApprovedDatasets`). Newer submissions in the collection don't extend it unless
  they are approved themselves.
- Pre-2022 collections can hold **several original DARs** (one per dataset). Queries that pick
  "the latest DAR" must also read the non-canceled sibling originals; see `DacDashboardDAO` and
  `DarCollectionSummaryDAO`.
- `validateDar` rejects a `closeoutSupplement`. Original DARs cannot close anything out.

### 3. Progress report (renewal, optional)

- Optional: a collection can go straight from the original DAR to a closeout. A collection can
  have any number of reports, including none.
- A report renews access only once its own DataAccess election approves it: the new grant runs
  8760 hours from the report's `submission_date`, for the datasets approved on it. A pending or
  denied report renews nothing, and the earlier grants keep their original expiry.
- `POST /api/dar/v2/progress_report/{parentReferenceId}` →
  `DataAccessRequest.populateProgressReportFromJsonString` (copies the parent's data and overlays
  the report fields) → `DataAccessRequestService.createProgressReport`.
- Preconditions. Closeouts go through the same endpoint; they share the ones not marked
  "ordinary reports only":
  - The caller owns the parent.
  - The parent is not a draft.
  - The collection has no submitted closeout. Checked before validation and again under
    `lockCollection`.
  - Ordinary reports only: the parent has no open DataAccess elections. `postProgressReport`
    checks this before uploading documents, and `insertProgressReport` checks it again under the
    lock. Every election-opening path takes the same lock. RP elections don't block. A closeout
    is not blocked by open elections; it cancels them (see below).
  - Ordinary reports only: the datasets are a subset of the parent's datasets and are currently
    approved (`findDatasetApprovalsByDar`). A closeout's datasets are set by the service instead.
- `insertProgressReport` sets `parent_id`, `collection_id` and `submission_date = now()` in the
  INSERT. There is no draft step and no later "submit". Only resubmit nulls it afterwards (see
  [Draft](#1-draft)); don't write other fixtures or code that do.
- A non-closeout report starts a new review cycle: `processNewDarCollection` opens elections for
  auto-open DACs and notifies.

### 4. Closeout (terminal)

- **No interim progress report is needed.** A closeout is filed through the progress-report
  endpoint against a submitted DAR in the collection: the original DAR when no report exists, or
  the latest report otherwise (duos-ui uses the newest DAR). So `parent_id` may point at the
  original DAR.
- A closeout is a progress report with a `closeoutSupplement` (non-empty `reasons` plus a
  `signingOfficialId` at the submitter's institution). The model check is
  `DataAccessRequest.getIsCloseoutProgressReport()`: it requires `parent_id` and reasons.
- On submission, the closeout:
  - **Covers every dataset in the collection.** The service replaces the client's selection with
    `findDatasetIdsByCollectionId`, which is the union over all submitted DARs. That includes
    datasets that earlier progress reports dropped. Closeouts filed before this rule may list
    only the datasets the researcher kept selected. The list can be **empty** if every dataset
    in the collection was converted to external; see
    [Datasets converted to external](#datasets-converted-to-external-dt-3184).
  - **Ends every grant in the collection immediately.** It does not wait for SO approval or DAC
    acknowledgement. `approveDataAccessRequestCloseout` only records the SO and notifies chairs.
  - **Cancels every open election in the collection**, of any type (DataAccess and RP) and on
    any DAR, not just the parent (`ElectionDAO.cancelOpenElectionsByCollectionId`, under the
    collection lock). Votes are rejected after a closeout, so an election left open could never
    be decided. The elections are set to `Canceled`; like a chair cancel, this doesn't archive
    them (it leaves `archived` as it was), so the collection still shows them.
  - Skips dataset-registration approval checks, dataset-dependent collaboration/ethics documents,
    DAA acknowledgements, and election creation.
- After it, the collection is closed. All of these are rejected:
  - Votes and rationale updates (409).
  - Opening or reopening elections (409). Automated (RADAR) approval skips the dataset instead
    of failing, so approvals already made for other datasets still notify.
  - Chair cancellation (409).
  - Researcher cancellation (409).
  - Resubmitting the collection as a draft (409).
  - Signing official approval of a DAR in the collection (409).
  - Another progress report (400).
- New-DAR processing (`createElectionsForNewDarCollection`, `sendNewDARCollectionMessage`) runs
  after the submission commits and acts on the collection's most recent DAR. It does nothing
  once the collection holds a closeout, because a closeout committed in between would otherwise
  be that DAR and get elections and new-request emails.
- Every role sees the collection as `COMPLETE` (`DarCollectionService.applyCloseoutStatus`).
  Member and chair views offer no vote actions, and the DAC dashboard excludes it from
  "awaiting my vote" for chairs and members.
- The duos-ui closeout form shows the full collection dataset list with removal disabled.

## Datasets converted to external (DT-3184)

DT-3184 (PR #2874, merged 2026-04-27) added a way to convert a DAC's controlled datasets to
external access, for when the DAC stops managing access in DUOS. Whenever that conversion has run,
DARs can have **fewer `dar_dataset` rows than they were submitted with, or none at all**. Any
environment where the conversion has been run can contain such DARs.

`DacServiceDAO` runs the conversion. The request (`DacDatasetExternalizationRequest`) has flags,
so not every step runs every time. For each converted dataset it **always**:

- sets `dataset.dac_id`, `dac_approval` and `dac_approval_date` to NULL, and sets the dataset's
  `accessManagement` property to `external`. Controlled datasets are converted; open-access ones
  only when `convertOpenAccessDatasets` is true (default false).

Then, depending on the flags:

- `revokeApprovedAccess` (default **true**):
  - **deletes every `dar_dataset` row for the dataset**, on drafts, original DARs, progress
    reports and closeouts alike;
  - appends to each affected DAR's `admin_dar_notes`: "On `<timestamp>` the following datasets
    were removed administratively from this request because the responsible Data Access
    Committee no longer manages access using DUOS. `DUOS-…`".
- `cancelOpenElections` (default **true**): cancels and archives open **DataAccess** elections on
  the dataset. It leaves RP elections alone, so open RP elections can remain on those DARs with
  nothing to close them.
- `dryRun` (default false) only reports what would change.

So a missing `dar_dataset` row with the admin note means `revokeApprovedAccess` ran. If it was
turned off, the dataset is external but the DARs still list it.

What this means for any change you make:

- **A missing `dar_dataset` row is not necessarily corruption.** The removal is deliberate, so
  don't "repair" it by backfilling `dar_dataset`: that would bring back datasets DUOS no longer
  manages. Look for the admin note before calling a gap a bug.
- **Don't rebuild a DAR's datasets from `data.datasetIds`.** The JSON keeps the pre-conversion
  list. On progress reports and closeouts it is also a copy of the **parent's** list
  (`populateProgressReportFromJsonString` copies the parent's data), not the report's own
  datasets. The admin note is the accurate record of which datasets a DAR lost.
- **Inner joins to `dar_dataset` hide affected collections.** Every `DarCollectionSummaryDAO`
  list and by-ID query, and the dashboard `getCounts` queries, inner-join `dar_dataset`. So a
  collection whose latest DAR lost all its datasets vanishes from the admin, researcher and SO
  lists and from the dashboard totals. `DarCollectionDAO`'s collection-detail queries left-join
  and still find them. The DAC chair/member views can't show them at all, because no DAC manages
  those datasets.
- **Collection-wide dataset lists can be empty.** `findDatasetIdsByCollectionId` reads
  `dar_dataset`, so a closeout on a fully converted collection gets no datasets and fails
  "At least one dataset is required". Any new code that derives datasets from a collection must
  handle an empty result deliberately.
- Converted datasets have `dac_id IS NULL`. Queries that start from a DAC, or join
  `dataset.dac_id`, drop them as well.

## SQL rules for closeout predicates

Write a "this collection is closed out" predicate exactly like this:

```sql
submission_date IS NOT NULL
AND parent_id IS NOT NULL
AND data ->> 'closeoutSupplement' IS NOT NULL
```

- `parent_id IS NOT NULL` is the key condition. Without it, a stray supplement on an original DAR
  would close the collection.
- `submission_date IS NOT NULL` is required: resubmit can turn progress reports back into
  drafts (see [Draft](#1-draft)), and a draft must never close anything.
- Two equivalent spellings are in use: `data ->> 'closeoutSupplement' IS NOT NULL`, and
  `data -> 'closeoutSupplement' IS NOT NULL AND data -> 'closeoutSupplement' != 'null'::jsonb`
  (`SigningOfficialDashboardDAO`). `->>` already returns NULL for a JSON `null`. Prefer the first
  in new queries.
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

Closeout submission, election creation/cancellation, resubmit, signing official approval, and
vote/rationale updates serialize on the collection row. Only the checks re-run under the lock are serialized, so a new
precondition that must hold at commit belongs after `lockCollection`, not only in the resource.

1. Open a transaction (`dataAccessRequestDAO.inTransaction(...)` or
   `DataAccessRequestServiceDAO.inTransaction`).
2. `lockCollection(collectionId)` (or `lockCollectionsForReferenceIds`, which locks in a stable
   order).
3. Check `hasSubmittedCloseout` **after** taking the lock. `CollectionLocks.lockOpenCollection`
   does steps 2 and 3 and throws 409; use it. Progress reports do their own check because they
   return 400. Don't add a second check before the lock: it can't be relied on, and it repeats
   the query.
4. Mutate, then commit. The lock is held until commit.

- Don't catch a SQL error inside the locked callback and carry on: Postgres has already aborted
  the transaction, so the commit silently rolls back. Let it propagate out of `inTransaction`,
  then handle it (as `DACAutomationRuleService.openElectionAndApprove` does).
- A deadlock or lock timeout while waiting for the lock (SQL states `40P01`, `55P03`, `40001`)
  is a retryable conflict: `VoteService` returns 409 for it, not 400.

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
| Modelling "draft closeouts" (a closeout with `submission_date` NULL) in code or fixtures | Closeouts are never drafts: they are inserted submitted and resubmit is rejected once one exists | Test "supplement on an original DAR never ends grants" instead |
| Assuming a closeout always follows an ordinary progress report | Researchers can close out straight from the original DAR, with no progress report in between | Handle a closeout whose parent is the original DAR; test that path as well as closeout-after-report |
| Test fixtures inserting closeouts with `insertDataAccessRequest` (no parent) | Doesn't match how the app creates closeouts, so it passes the wrong predicates | File closeouts with `insertProgressReport` on an approved parent |
| Trusting the client's dataset list for a closeout | Clients could omit datasets to skip validation or keep access | Expand to `findDatasetIdsByCollectionId` before validation and again under the lock |
| Special-casing closeout status for one role only | Other roles showed `IN_PROCESS` or `SUBMITTED` for a closed collection | Apply closeout status in every role processor |
| Excluding closeouts from only the chair branch of the DAC dashboard | Members saw a vote count with nothing to vote on | Gate the whole "awaiting my vote" count on `NOT has_closeout` |
| Checking for a closeout outside the transaction | TOCTOU: a closeout could commit between the check and the write | Lock, re-check, mutate in one transaction |
| Treating DARs with no `dar_dataset` rows as corrupt and planning a backfill from `data.datasetIds` | The DT-3184 conversion removed those rows on purpose, and on progress reports the JSON is the parent's list | Check `admin_dar_notes` for the removal note; fix the queries or behavior that mishandle the gap, not the data |
| Comparing React props by identity in duos-ui `ProgressReportApplication` | The parent passes a freshly `merge()`d `dar` each render, which wiped the researcher's dataset removals | Key resets on dataset ids plus closeout mode |

## Verification checklist

- DAO and migration tests run locally (Testcontainers + Docker). Run them; don't defer to CI. Run
  DAO suites one at a time: they all bind port 8180.
- For grant or approval queries, test three cases: no closeout, a real closeout (progress report
  with a parent), and a supplement on an original DAR (must not close anything).
- Check real data shapes in `localdb` (schema `consent`) before tightening a predicate. Confirm which
  dump it's loaded from first (`docker inspect localdb` shows the mounted `config/consentdb-*.sql`).
- For collection or dataset queries, also test a DAR whose datasets were all converted to external
  (no `dar_dataset` rows, `dataset.dac_id` NULL). It should still behave sensibly.
- Sonar flags `throws` clauses left behind after you change a signature, multi-call
  `assertThrows` lambdas (S5778), and locals that hide fields (S1117). Re-check the callers' tests
  when you change a `throws` clause.
