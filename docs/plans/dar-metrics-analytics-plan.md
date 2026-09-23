# DAR Metrics and Analytics Plan

## Status

Proposed. Six of the nine tickets can start now, and ticket 4 follows ticket 3. Ticket 8 needs
product answers on reopened and canceled decisions, and ticket 9 needs a definition of renewal. See
[Blocking Decisions](#blocking-decisions). Tracked in epic
[DT-4182](https://broadworkbench.atlassian.net/browse/DT-4182).

## Summary

DUOS has been asked to report thirteen Data Access Request (DAR) metrics on an internal admin
dashboard: decision outcomes, how decisions were made, approval turnaround, submission volume,
expiration and renewal, all as data over time.

Twelve of the thirteen can be answered from data Consent already stores, most with full history (see
[Data Caveats](#data-caveats)). Most of the work is a reporting layer: admin endpoints in `consent`
and a dashboard in `duos-ui`. Two tickets fix existing problems the reporting would otherwise inherit,
and they run first. Renewal needs a product definition before anything can be built.

## Objective

Give DUOS admins one view of how DARs move through the system: how many are submitted and by whom,
how many reach a DAC decision and what it was, how often RADAR decides instead of a chair, how long SO
and DAC approval take, and how many requests expire.

Endpoints take a required date range and return counts and summary statistics per day, week or
month, computed in SQL, plus paginated flat rows with a timestamp on each.

## Blocking Decisions

These need answers from product.

| # | Question | Gates |
| --- | --- | --- |
| 1 | **What does "renewal" mean?** The existing progress-report (continuing review) flow, or a researcher submitting a new request after one expired? Only the first exists today; it needs only reporting, while the second needs a new column. | Ticket 9 |
| 2 | **Does a reopened decision still count as decided?** When a decided DAR-dataset pair is reopened, the last cast vote stands but the new election has no vote. | Ticket 8's funnel. Tickets 5 and 6 carry both readings (`decision` and `state`) until answered |
| 3 | **How should a chair-canceled election be counted?** A canceled election is closed without a decision. It could be undecided, its own "canceled" outcome, or excluded. See [Cancellation](#cancellation-happens-at-two-levels). | Ticket 8's funnel. Ticket 5 reports it as its own state until answered |
| 4 | **When does a multi-dataset DAR count as decided?** On the first dataset decision, or once all are in? | A DAR-level rollup, deferred. Per-pair rows ship without it |
| 5 | **Is a DAR denied if any dataset is denied, if all are, or is partial approval its own bucket?** | Same deferred rollup |

Decisions 2 to 5 change queries, not schema, so the reporting tickets can ship per-pair rows now and
apply the answers later.

One smaller product question: should "researchers on a DAR" include external collaborators, who are
not institution-validated at submission? It doesn't block anything; it only affects ticket 4's
response.

## Background

DAR state lives in four tables: `data_access_request` (the request, submission and SO-approval
timestamps), `dar_dataset` (datasets requested), `election` (one decision process per DAR-dataset
pair) and `vote` (what was decided and when).

`MetricsResource` has one dataset-scoped endpoint today. The role dashboards
(`DacDashboardResource`, `SigningOfficialDashboardResource`, `ResearcherDashboardResource`) answer
"what should this user do next", not "what has happened over time".

In scope: admin reporting endpoints, an admin dashboard in `duos-ui`, two fixes, and renewal if product
defines it. Out of scope: any change to how DAC voting, RADAR rules or SO approval behave.

## How the Data Works Today

Verified against `develop`.

### Decisions are votes

A decision is a `vote` row with `type` `FINAL` (a chair) or `RADAR_APPROVE` (an automation rule), and
`vote.vote` holding approve or deny. Casting it closes the election
(`service/dao/VoteServiceDAO.java:61-67`).

`election.final_access_vote` looks like the outcome but has no production writer; the only
`updateElectionById` overload that sets it (`ElectionDAO.java:220-226`) is called only from tests.
`Election.finalVoteDate` is not a column; it is derived in SQL in two places, and neither handles
RADAR (see [below](#radar-decisions-are-missing-from-two-election-queries)). Reporting should read
`vote`.

### The decision timestamp is `vote.update_date`

`VoteServiceDAO.updateVotesWithValue:41-44` sets `update_date` when a vote is cast, manual or RADAR.
The other two writes to `vote`, the reminder flag (`VoteDAO.java:78`) and a rationale edit
(`VoteDAO.java:113`), leave it alone, so it records when the decision was made.

### A DAR-dataset pair can have several elections

Reopening a decision archives the previous elections and opens a new one
(`service/dao/DarCollectionServiceDAO.java:85`), so a pair can go denied, approved, denied.
`DataAccessRequestDAO.java:56-64` describes the rule: take the most recent vote.

`SigningOfficialDashboardDAO.java:48-64` implements it with two CTEs, `ranked_final_votes` and
`latest_final_votes`. Copy three details from it:

- accept both `final` and `radar_approve`;
- require `v.vote IS NOT NULL`, so an uncast vote on a reopened election doesn't win;
- order by `COALESCE(v.update_date, v.create_date) DESC, v.vote_id DESC` so ties are deterministic.

`ResearcherDashboardDAO.java:46-68` does the same under different names.

### RADAR decisions are missing from two election queries

Both derivations of `final_vote_date` join on `LOWER(v.type) = 'final'`, so a `RADAR_APPROVE` vote
never matches:

- `findElectionWithFinalVoteById` (`ElectionDAO.java:58-72`) uses an INNER JOIN (`:66`), so a
  RADAR-decided election is dropped from the result;
- `findLastElectionsByReferenceIds` (`:130-160`) uses a LEFT JOIN (`:145`), so it comes back with null
  vote fields.

`:62` also takes the date from `v.create_date`, which is when the election opened, not when anyone
voted. Ticket 2 fixes both.

### Cancellation happens at two levels

- **DAR cancellation.** A researcher cancels their own collection before any election exists
  (`DarCollectionService.cancelDarCollectionAsResearcher`, which refuses once elections are present).
  It writes `"Canceled"` into `data->>'status'` (`DataAccessRequestDAO.cancelByReferenceIds`) and
  records no timestamp.
- **Election cancellation.** A chair cancels open elections on a collection after the DAC has opened
  them (`cancelDarCollectionElectionsAsChair`). It sets `election.status = 'Canceled'` and
  `last_update` (`ElectionDAO.java:52`); the DAR itself is untouched. Only chairs can do this now
  (`cancelDarCollectionByRole` rejects other roles). Admins could historically, through the same
  `cancelElectionsForReferenceIds` helper, and the election records no actor, so the two can't be
  told apart.

A canceled DAR never reaches a DAC, so reporting excludes it, and archived DARs too, as the SO and
researcher dashboards do (`SigningOfficialDashboardDAO.java:46`, `DataAccessRequestDAO.java:52`).
Status is stored as `Canceled`/`Archived` (`DarStatus`), so compare with `LOWER()`.

A canceled election is a pair that reached a DAC and closed without a decision; how to count it is
Blocking Decision 3. Its `last_update` is overwritten if the election is later archived by a reopen
(`ElectionDAO.java:231`), so it is only a reliable cancellation time for elections that were never
archived.

### Progress reports are DAR rows

A progress report or closeout is a `data_access_request` row with a `parent_id` in the same
collection, submitted with `submission_date = now()` (`DataAccessRequestDAO.java:481`), and elections
open on it like any DAR (`DarCollectionServiceDAO.java:65`). Drafts have a null `submission_date`.
Counting original DARs means `parent_id IS NULL AND submission_date IS NOT NULL`; without it, one
collection counts once per submission.

### SO approval

`updateDarApprovalSO` sets `approving_so_id` and `approving_so_timestamp = now()`
(`DataAccessRequestDAO.java:521-524`).

`requires_so_approval` is set at submission by `requiresSOApproval`
(`DataAccessRequestService.java:898-905`): true if a dataset has the `REQUIRE_SO_DAR_APPROVAL` rule or
the researcher isn't pre-authorized for all required DAAs. The column is nullable with no default
(`changelog-consent-2026-01-19-add-requires-so-approval-attribute.xml:6`) and is only written when
true (`DataAccessRequestService.java:298-300`). NULL can mean the SO step was skipped, the row predates
January 2026, or the row is a progress report or closeout (`:349-355` writes true only for
non-closeout reports without pre-authorization, so closeouts stay NULL even though they go to an SO).

So `requires_so_approval = false` matches nothing, and `DataAccessRequestMapper.java:57` maps NULL to
`false`, which hides this in Java. Use `requires_so_approval IS NOT TRUE AND parent_id IS NULL`,
limited to submissions after January 2026.

That cutoff applies to skip classification only. Closeout SO approvals were recorded from June 2025
(`changelog-consent-2025-06-05-save-so-closeout-approval.xml`) and moved into `approving_so_timestamp`
by `changelog-consent-2026-06-18-consolidate-so-approval-fields.xml`, so closeout approval times go
back further than original-DAR ones.

### Expiration is computed

`expiresAt` is `submissionDate` plus 365 days (`models/DataAccessRequest.java:31`, `:154-163`). It is a
function of a stored timestamp and a constant, so expiry dates can be computed for all history.

Per DAR it is wrong for continuing access: a parent passes its 365 days while a progress report keeps
access going. The researcher dashboard dates expiry from the collection's latest submission
(`ResearcherDashboardDAO.java:117`), and ticket 7 does the same.

### Renewal has no explicit representation

If renewal means continuing review, the progress-report chain already records it. If it means a new
request after expiry, that chain can't be reused: `parent_id` is UNIQUE
(`changelog-consent-2025-05-16-disallow-pr-siblings.xml:5-6`), so a DAR has at most one child, and it
already means continuing review; `createProgressReport` requires the datasets to be approved on the
parent (`DataAccessRequestService.java:318-331`).

### Collaborators have no user id

Internal collaborators and lab staff are looked up by email at submission and must be DUOS users with
a library card at the submitter's institution (`DataAccessRequestService.java:617`, `:718`). External
collaborators aren't checked. `Collaborator` is stored in the DAR JSON as name, email, title, eRA
Commons id and country, with no user id. Counting collaborators per DAR is exact; matching the same
person across DARs means joining on email.

### Institution is read live

`users.institution_id` is the only source. The DAR's own `institution` property is in
`DataAccessRequestData.DEPRECATED_PROPS` and is dropped at submission, left over from when user details
were stored on the DAR before moving to the user. Nothing records where a researcher worked when they
applied. Ticket 3 fixes this going forward.

Admins can delete an institution (`InstitutionService.java:79-85`), and `users.institution_id` is
`ON DELETE SET NULL` (`changelog-consent-70.0.xml`), so the live value can also vanish. Original DARs
can't be submitted without an institution (the PI/SO/IT email check,
`DataAccessRequestService.java:691-708`), but progress reports only check collaborators (`:476`), so
one can be submitted with none.

## Metrics to Tickets

| # | Requested metric | Ticket and source |
| --- | --- | --- |
| 1 | DARs with a DAC decision vs. not | 5. Latest cast vote per pair on original DARs; canceled and archived DARs excluded; canceled elections per Decision 3 |
| 2 | Approved vs. denied | 5. `vote.vote` on the latest cast `FINAL`/`RADAR_APPROVE` vote |
| 3 | Decided via RADAR | 5. `vote.type`. History from July 2025 |
| 4 | DARs per researcher | 4. `user_id`, `submission_date` |
| 5 | DARs per institution | 4. Snapshot from ticket 3 where present, current institution otherwise |
| 6 | Datasets per DAR | 4. `dar_dataset` |
| 7 | Researchers per DAR (PI, lab staff, internal collaborators) | 4. Per DAR, not distinct people |
| 8 | Submission to DAC decision | 6. `submission_date` to `vote.update_date` |
| 9 | Submission to SO approval, separating pre-auth skips | 7. `approving_so_timestamp`. Original DARs from January 2026, closeouts from June 2025 |
| 10 | DARs expired | 7. Computed from the collection's latest `submission_date` |
| 11 | DARs renewed | 9, once Decision 1 is answered |
| 12 | Researchers submitting DARs | 4. Distinct `user_id` with a `submission_date` |
| 13 | Timestamped, over time | Every reporting ticket returns a timestamp per row |

## Data Caveats

These show on the endpoint docs and next to the affected dashboard figures.

| Caveat | Effect | Mitigation |
| --- | --- | --- |
| Institution is current, not historical, before ticket 3 | A researcher who changes employer takes their past DARs with them | Ticket 3, forward-only |
| Collaborators have no stable identity | Cross-DAR person counts are an email-join approximation | Only report per-DAR counts |
| `requires_so_approval` NULL has three meanings | Skips, pre-2026 rows and closeouts look the same | Use the predicate above |
| Short history | RADAR from July 2025; SO approval on original DARs from January 2026, on closeouts from June 2025 | No year-over-year on metrics 3 and 9 yet |
| Election cancel time | Lost if the election was later archived | Report the cancellation, not its date, for archived elections |
| Election cancel actor | Not recorded; admin and chair cancellations are identical rows | Don't split by who canceled |

Old deciding votes may also have a null `update_date`. Ticket 1 measures how many.

## Scalability

The reporting queries are aggregations over full history, so they will get slower as data grows. For
now the volume is small (ticket 1 records it) and the endpoints are admin-only with low traffic, so
plain Postgres queries should be enough, provided every reporting endpoint takes a required date range,
computes counts and summaries in SQL over it, and paginates its row detail. Index the join and date
columns.

Ticket 1 also records query plans and timings for the heaviest queries against production-sized data.
If they are too slow, the options in rough order of effort are: add indexes; precompute into a
materialized view refreshed on a schedule; or publish DAR lifecycle events to an Elasticsearch index,
alongside the existing dataset index (`ElasticSearchService`), and serve reporting from there. The
endpoint contracts in tickets 4 to 7 don't depend on which backs them, so this can change later without
touching the dashboard.

## Design Decisions

| Question | Decision |
| --- | --- |
| Record new lifecycle events? | No, except the institution snapshot, and renewal if it means a new flow. Everything else is already stored or computable from what is; copying it would create a second source that can disagree with the first. |
| Where do decisions come from? | `vote.vote`, `vote.type` and `vote.update_date`. Not `Election.finalVoteDate` or `election.final_access_vote`. |
| Store decision rollups on `data_access_request`? | No. The rollup rules aren't settled, and a reopen would have to invalidate them. Compute at query time. |
| Where do the endpoints go? | `MetricsResource`, with `@RolesAllowed(ADMIN)` on each new method. Its existing endpoint is `@PermitAll` at the method level, so the two coexist. Queries go in `MetricsService` and a new `DarMetricsDAO`. |
| One endpoint or several? | Several, one per metric family, so each is a reviewable change and releases independently. |
| Row granularity? | Volume (ticket 4) returns one row per original DAR. Decisions and turnaround (tickets 5 and 6) return one row per DAR-dataset pair on original DARs, because decisions are made per pair; DAR-level figures are computed from those rows. |
| Request shape? | Tickets 4 to 7 require `from` and `to` on `submission_date`, take an optional `bucket` (day, week, month), return per-bucket counts and summaries computed in SQL, and paginate row detail. |
| Personal data? | Responses carry IDs, counts, enums and timestamps only. No names, emails or eRA Commons IDs, which sit in the DAR `data` JSON (`Collaborator`). |

## Sequencing

1. **Tickets 1, 2 and 3 first.** Ticket 1 sets the honest date ranges and checks query cost. Tickets 2
   and 3 fix existing problems: ticket 2 is a bug in queries existing code already calls, and ticket 3 is
   forward-only, so every week it waits is a week of institution history lost.
2. **Tickets 4 to 7 in parallel** (ticket 4 after ticket 3). They each add a method to `MetricsResource`, `MetricsService` and
   `DarMetricsDAO`; the first to merge creates the DAO. That is a merge conflict, not a dependency.
3. **Ticket 8** (`duos-ui`) once 4 to 7 are deployed and Decisions 2 and 3 are answered.
4. **Ticket 9** if and when product defines renewal.

Each endpoint ticket also touches `ConsentModule.java` if it adds a provider, and adds its
`src/main/resources/assets/paths/<endpoint>.yaml` with an entry in `api-docs.yaml`.

Points are relative estimates.

## Tickets

### Ticket 1 (DT-4183): Measure reporting data coverage and query cost

**Type:** Spike · **Size:** 2 · **Depends on:** nothing

Find out how much history each metric has and how expensive the queries are before promising a trend
line or choosing a backing store.

**Notes**

- Read-only. No code or schema changes.
- Query the `consent` schema via `pg_catalog` and `::regclass`, not `information_schema` with
  `table_schema = 'public'`.

**Acceptance criteria**

- Submitted DARs per month for the last 24 months.
- DAR-dataset pairs with a closed data-access election.
- `RADAR_APPROVE` votes: count and earliest.
- DARs with `approving_so_timestamp`: count and earliest.
- Cast `FINAL`/`RADAR_APPROVE` votes on data-access elections with a null `update_date`, and the newest
  such vote. Require `v.vote IS NOT NULL`: election creation inserts uncast `FINAL` votes
  (`service/dao/DarCollectionServiceDAO.java:149`, `:181`), which would count pending work instead.
- DARs with null `requires_so_approval`, split by `parent_id IS NULL`.
- Canceled elections, split by archived and not.
- `EXPLAIN ANALYZE` timings for the latest-vote CTE over all pairs and for the per-DAR volume query.
- Figures and query plans recorded on DT-4183, not in this repo, which is public. This plan
  links to them and records only which metrics aren't worth charting and whether Postgres is enough
  (see [Scalability](#scalability)).

---

### Ticket 2 (DT-4184): Include RADAR decisions in the election queries

**Type:** Story · **Size:** 3 · **Depends on:** nothing

Fix the two `ElectionDAO` queries that miss RADAR-approved decisions, and the one that dates a decision
from when its election opened.

`findElectionWithFinalVoteById` drops RADAR-decided elections (INNER JOIN on type `final`).
`findLastElectionsByReferenceIds` returns them with a null vote and date (LEFT JOIN). The first also
uses `v.create_date` as the decision date. The only production callers are
`DataAccessRequestService.sendReminderMessage` (`:872`) and the researcher-cancel check in
`DarCollectionService` (`:830`).

**Notes**

- Accept `final` and `radar_approve`, as `SigningOfficialDashboardDAO.java:48-64` does.
- Use `COALESCE(v.update_date, v.create_date)` for the date in both, matching
  `findLastElectionsByReferenceIds`. `Election.finalVoteDate` is for display; reporting reads
  `vote.update_date` directly (ticket 6).
- Every chair gets an uncast `FINAL` vote (`DarCollectionServiceDAO.java:149`), so a multi-chair
  election has several rows. In `findElectionWithFinalVoteById`, drop `DISTINCT` and order before
  `LIMIT 1`: `ORDER BY v.vote IS NULL, v.update_date DESC NULLS LAST, v.vote_id DESC`.
- Fixing the INNER JOIN changes which elections are returned. Confirm both callers behave correctly
  when a RADAR-decided election is returned.

**Acceptance criteria**

- `findElectionWithFinalVoteById` returns RADAR-approved elections.
- `findLastElectionsByReferenceIds` returns RADAR-approved elections with their vote and a decision
  date.
- Both date a manual decision from when the vote was cast.
- Elections with no cast vote are unchanged.
- A multi-chair election with one cast `FINAL` vote returns the cast vote.
- A reminder for a vote on a RADAR-decided election no longer fails to find the election.
- The researcher-cancel check still refuses once any election exists.

**Tests**

- One RADAR test per query that fails before the fix.
- A manual `FINAL` decision reports the vote-cast date.
- An open election with an uncast vote is unchanged.
- A two-chair election with one cast vote returns that vote.

---

### Ticket 3 (DT-4185): Record institution at submission

**Type:** Story · **Size:** 2 · **Depends on:** nothing

Store the submitter's institution on the DAR when it is submitted, so institution reporting stops
changing when a researcher changes employer.

**Notes**

- Liquibase changeset adding nullable `institution_id` and `institution_name` to
  `data_access_request`, with a `changelog-master.xml` include. `institution_id` references
  `institution` `ON DELETE SET NULL` so an admin can still delete an institution; the name keeps the
  history when that happens.
- Write it in the existing submission transaction in `DataAccessRequestService`, next to
  `captureDatasetDaaSnapshots`.
- Update `db/mapper/DataAccessRequestMapper.java`; DAR hydration is mapped by hand.
- Progress reports and closeouts record the institution at their own submission, NULL if the
  submitter has none.

**Acceptance criteria**

- A newly submitted DAR records the submitter's institution id and name.
- Changing the user's institution afterwards doesn't change the DAR's recorded institution.
- An original DAR without an institution is still rejected, as it is today. A progress report
  without one is accepted, as today, and records NULL.
- Deleting the institution nulls the id and keeps the name.
- Existing submission behavior is otherwise unchanged.

**Tests**

- Submission records the institution.
- Changing the user's institution leaves an existing DAR's value alone.
- Deleting an institution keeps the recorded name.

**Out of scope:** backfilling past DARs, which isn't recoverable. Reporting on the column is in ticket 4.

---

### Ticket 4 (DT-4186): DAR volume and composition reporting

**Type:** Story · **Size:** 5 · **Depends on:** ticket 3

Report DARs per researcher and per institution, datasets per DAR, researchers per DAR, and distinct
researchers submitting (metrics 4, 5, 6, 7 and 12).

**Notes**

- Admin-only method on `MetricsResource`, backed by `MetricsService` and `DarMetricsDAO`. Response DTOs
  as Java records.
- One row per original DAR (`parent_id IS NULL AND submission_date IS NOT NULL`), excluding canceled
  and archived, with its `submission_date`.
- Institution: the DAR's recorded institution from ticket 3, falling back to `users.institution_id`
  for DARs submitted before it. Include a flag saying which, and note it in the OpenAPI path spec. A
  DAR with no institution either way reports null and groups as "No institution".
- Collaborator counts are per DAR; don't expose a distinct-person count.
- Response fields are limited to: reference id, collection id, submitter user id, institution id,
  name and source, `submission_date`, dataset count, and PI, lab staff and internal collaborator
  counts. Nothing else from the DAR `data` JSON.
- Required date range, optional bucket, paginated rows (see [Design Decisions](#design-decisions)).

**Acceptance criteria**

- Metrics 4, 5, 6, 7 and 12 are available to an admin; other roles get 403.
- Every row has a timestamp.
- A DAR with no collaborators reports zero.
- A researcher with no institution is included, not dropped.
- Each row says whether its institution was recorded at submission or read live.
- Progress reports, closeouts and drafts aren't counted.
- No collaborator name, email or eRA Commons ID is serialised.

**Tests**

- DAO tests per aggregation, including a multi-dataset DAR and one with no datasets.
- A null institution; a recorded institution that differs from the user's current one.
- PI, lab staff and internal collaborator counts reported separately.
- A collection with a progress report counts once.
- The serialised response contains no collaborator name or email.
- Admin and non-admin resource tests.

---

### Ticket 5 (DT-4187): DAC decision reporting

**Type:** Story · **Size:** 5 · **Depends on:** nothing

Report, per DAR-dataset pair, whether a DAC decision exists, whether it approved or denied, and
whether RADAR or a chair made it (metrics 1, 2 and 3).

**Notes**

- Reuse the `ranked_final_votes`/`latest_final_votes` pattern from
  `SigningOfficialDashboardDAO.java:48-64`, partitioned by `(reference_id, dataset_id)`, keeping the
  `v.vote IS NOT NULL` filter and the `vote_id` tie-break. Put it in `DarMetricsDAO` as a reusable
  fragment so ticket 6 can share it.
- Original DARs only (`parent_id IS NULL AND submission_date IS NOT NULL`). Exclude canceled and
  archived DARs with `LOWER(data->>'status') IN ('canceled', 'archived')`, keeping NULL status.
- Each row carries two fields, one per reading of Decision 2:
  - `decision`: approve, deny or null, from the latest cast vote across all the pair's elections;
  - `state`: from the latest election only, `DECIDED` if its vote is cast, `PENDING` if open with no
    cast vote, `CANCELED` if canceled with no cast vote, `NO_ELECTION` if none exists yet.
- Start from `dar_dataset` and left-join elections and votes. A pair waiting on SO approval has no
  election (`DarCollectionService.java:945`, `:1151`) and must still count as undecided.
- A pair decided, reopened, then canceled reports its old `decision` and `state = CANCELED`.
- Required date range, optional bucket, paginated rows; per-bucket counts by `state`, `decision` and
  vote type in SQL.
- Read `vote.vote` and `vote.type`, not `election.final_access_vote`.

**Acceptance criteria**

- A pair that went denied, approved, denied reports denied.
- A multi-dataset DAR with one RADAR and one manual decision reports both correctly.
- A canceled DAR is in neither the decided nor the pending count.
- A pair whose election was canceled by a chair reports `CANCELED`, not pending.
- A pair decided then reopened reports its old `decision` with `state = PENDING`.
- An archived DAR and a progress report are excluded.
- A pair with no election reports `NO_ELECTION` and counts as undecided.
- A pair with an open election and no cast vote ever reports `state = PENDING`, a null `decision`
  and a null decision timestamp.
- Every row has the DAR's `submission_date`, plus a nullable decision timestamp.
- Other roles get 403.

**Tests**

- One DAO test per history: single decision, reopened once, reopened with no vote yet, election
  canceled, decided then reopened then canceled, no election yet.
- A multi-dataset DAR mixing RADAR and manual decisions.
- Canceled and archived DARs are excluded, whatever the status casing.
- An uncast vote doesn't win.

**Out of scope:** DAR-level rollups (Decisions 4 and 5).

---

### Ticket 6 (DT-4188): DAC decision turnaround reporting

**Type:** Story · **Size:** 3 · **Depends on:** nothing; shares the latest-vote fragment with ticket 5

Report time from submission to DAC decision (metric 8), `submission_date` to `vote.update_date` on the
deciding vote.

**Notes**

- Whichever of tickets 5 and 6 lands first adds the latest-vote fragment; the other reuses it.
- Exclude deciding votes with a null `update_date` and return the excluded count. Don't fall back to
  `create_date`: that is when the election opened, so it would understate turnaround.
- Same population as ticket 5 (original DARs, canceled and archived excluded). Each row carries
  ticket 5's `state`, so the dashboard can apply either reading of Decision 2.
- Required date range, optional bucket, and an optional `state` filter applied before aggregation, so
  summaries match either reading. Mean, median and mode computed in SQL per bucket; per-pair elapsed
  times paginated.

**Acceptance criteria**

- Turnaround for decided pairs within the requested range.
- Null-`update_date` pairs excluded and counted.
- Filtering by `state` changes the summaries, not only the rows.
- Mean, median and mode returned per bucket; pairs with no cast vote excluded.
- Other roles get 403.

**Tests**

- Known turnaround for a manual and a RADAR decision.
- Null `update_date` excluded and counted.
- Mean, median and mode against a fixed synthetic set.

---

### Ticket 7 (DT-4189): SO approval and expiration reporting

**Type:** Story · **Size:** 5 · **Depends on:** nothing

Report submission-to-SO-approval time, separate requests that skipped SO review through
pre-authorization, and count expired DARs (metrics 9 and 10).

**Notes**

- Skips: `requires_so_approval IS NOT TRUE AND parent_id IS NULL`, submitted after January 2026. Never
  `= false`.
- Report progress reports and closeouts separately.
- Compute expiry per collection in SQL as its latest `submission_date` + 365 days, matching
  `ResearcherDashboardDAO.java:117`. A closed-out collection is reported as closed, not expired.
- Required date range, optional bucket, paginated rows; summaries in SQL.
- Note in the OpenAPI path spec that skip classification starts January 2026 and closeout approval
  times start June 2025.

**Acceptance criteria**

- Turnaround for DARs with `approving_so_timestamp`, with per-request values and summary statistics.
- Skips are distinguishable from DARs still waiting on an SO.
- A closeout that went to an SO isn't counted as a skip.
- Pre-January 2026 DARs aren't counted as skips.
- Expiry agrees with `EXPIRATION_DURATION_MILLIS` either side of the boundary.
- A parent older than 365 days with a progress report newer than 365 days isn't expired.
- Every row has a timestamp. Other roles get 403.

**Tests**

- One per `requires_so_approval` state: true and approved, true and pending, NULL original, NULL
  closeout, NULL pre-2026.
- `= false` returns nothing.
- Expiry either side of 365 days, and a collection kept live by a progress report.

---

### Ticket 8 (DT-4190): Admin DAR analytics dashboard

**Type:** Story · **Size:** 8, split by section if needed · **Depends on:** tickets 4 to 7 deployed;
Decisions 2 and 3 answered · **Repo:** `duos-ui`

An admin page showing the decision funnel, turnaround distributions, volume and expiration over time.

**Notes**

- Build on `src/components/dashboard/{ConsoleDashboard,ConsoleDashboardGrid}.tsx`.
- Sections: decision funnel with RADAR vs. manual; DAC and SO turnaround with mean, median and mode;
  volume; expiration.
- Every time series can be bucketed by day, week or month.
- Show caveats next to the figures they affect: institution source, the SO history bounds, and
  ticket 6's excluded count.

**Acceptance criteria**

- The funnel shows one reading of reopened and canceled decisions, per Decisions 2 and 3.
- Only admins can reach the page.
- Each section shows an empty state when it has no data.
- Caveats appear on the page, not only in docs.

**Tests**

- Component tests per section against fixtures, including empty states.
- A non-admin can't reach the page.

**Out of scope:** bulk export.

---

### Ticket 9 (DT-4191): Record DAR renewal

**Type:** Story, conditional · **Size:** 5, re-estimate after Decision 1 · **Depends on:** Decision 1

Report renewals under product's definition (metric 11).

**Notes**

- If renewal means the existing progress-report flow, this is reporting only: count submitted
  `parent_id` children, excluding closeouts (a non-empty `closeoutSupplement.reasons`, as in
  `DataAccessRequest.getIsCloseoutProgressReport`), with full history. No schema change.
- If it means a new request after expiry, add `renewed_from_dar_id` rather than reusing `parent_id`.
  That is forward-only.
- Ticket 7 already dates expiry from a collection's latest submission. If renewal means a new
  request, decide whether it extends the original's expiry.

**Acceptance criteria**

- Renewals are reportable over time.
- Progress-report behavior is unchanged.
- Under the progress-report reading: closeouts are not counted as renewals.
- Under the new-request reading: a renewed DAR records the DAR it renews, and renewal and a progress
  report can coexist on one DAR.

**Out of scope:** backfilling a new renewal link, which isn't possible.

---

### Summary

| Ticket | Jira | Metrics | Size | Depends on |
| --- | --- | --- | --- | --- |
| 1. Data coverage and query cost | [DT-4183](https://broadworkbench.atlassian.net/browse/DT-4183) | — | 2 | — |
| 2. RADAR in election queries | [DT-4184](https://broadworkbench.atlassian.net/browse/DT-4184) | — | 3 | — |
| 3. Record institution at submission | [DT-4185](https://broadworkbench.atlassian.net/browse/DT-4185) | 5 (accuracy) | 2 | — |
| 4. Volume and composition | [DT-4186](https://broadworkbench.atlassian.net/browse/DT-4186) | 4, 5, 6, 7, 12 | 5 | 3 |
| 5. DAC decisions | [DT-4187](https://broadworkbench.atlassian.net/browse/DT-4187) | 1, 2, 3 | 5 | — |
| 6. DAC turnaround | [DT-4188](https://broadworkbench.atlassian.net/browse/DT-4188) | 8 | 3 | shares a fragment with 5 |
| 7. SO approval and expiration | [DT-4189](https://broadworkbench.atlassian.net/browse/DT-4189) | 9, 10 | 5 | — |
| 8. Dashboard (`duos-ui`) | [DT-4190](https://broadworkbench.atlassian.net/browse/DT-4190) | all except 11 | 8 | 4–7; Decisions 2, 3 |
| 9. Renewal | [DT-4191](https://broadworkbench.atlassian.net/browse/DT-4191) | 11 | 5 | Decision 1 |

## Test Matrix

Across the reporting tickets, cover at least:

- a pair that went denied, approved, denied;
- a reopened pair with no cast vote;
- a multi-dataset DAR mixing RADAR and manual decisions;
- a RADAR-decided election in both election queries (ticket 2);
- a canceled or archived DAR (excluded) and a chair-canceled election (its own state);
- a collection with a progress report, counted once;
- each `requires_so_approval` state, and `= false` matching nothing;
- a deciding vote with a null `update_date`;
- expiry either side of 365 days, and a collection kept live by a progress report;
- a DAR with no decision, some datasets decided, and all decided;
- no collaborator name or email in any response;
- admin and non-admin access to every endpoint.

Synthetic data only. No Mockito `lenient()` stubbing.

## Out of Scope

- Changes to DAC voting, RADAR rules, SO approval or cancellation behavior.
- Decision rollup columns on `data_access_request`.
- Bulk CSV or warehouse export; a follow-up if the dashboard isn't enough.
- Backfilling a new renewal link or historical institution.

## Definition of Done

- Admin endpoints return flat, timestamped rows for every metric except renewal.
- Decision outcome, source and turnaround can be queried over any range of recorded history, with
  excluded rows counted.
- SO turnaround separates pre-authorization skips from pending approvals and from progress reports.
- RADAR-decided elections appear in both election queries with a decision date.
- New DARs record their submitter's institution.
- The `duos-ui` dashboard shows funnel, turnaround, volume and expiration with caveats beside the
  figures.
- Blocking decisions answered, and renewal built or explicitly declined.
- Apart from tickets 2 and 3, no existing DAR, voting, SO approval or cancellation behavior has changed.
