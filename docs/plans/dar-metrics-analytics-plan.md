# DAR Metrics and Analytics Plan

## Status

In progress. Tickets 1 to 3 are done, and
[#3049](https://github.com/DataBiosphere/consent/pull/3049), which rewrites the metrics queries tickets
4 to 7 extend, has merged, so those can start. Product has answered the blocking questions (see
[Product Decisions](#product-decisions)), so nothing is waiting on product. Tracked in epic
[DT-4182](https://broadworkbench.atlassian.net/browse/DT-4182).

## Summary

DUOS has been asked to report thirteen Data Access Request (DAR) metrics on an internal admin
dashboard: decision outcomes, how decisions were made, approval turnaround, submission volume,
expiration and renewal, all as data over time.

All thirteen can be answered from data Consent already stores, most with full history (see
[Data Caveats](#data-caveats)). Most of the work is a reporting layer: admin endpoints in `consent`
and a dashboard in `duos-ui`. Two tickets fix existing problems the reporting would otherwise inherit,
and they run first. Renewal is the existing progress-report flow, so it needs reporting only.

## Objective

Give DUOS admins one view of how DARs move through the system: how many are submitted and by whom,
how many reach a DAC decision and what it was, how often RADAR decides instead of a chair, how long SO
and DAC approval take, and how many requests expire.

Endpoints take a required date range and return counts and summary statistics per day, week or
month, computed in SQL, plus paginated flat rows with a timestamp on each.

## Product Decisions

Answered by product on
[DT-3999](https://broadworkbench.atlassian.net/browse/DT-3999) (25 September 2026).

| # | Question | Decision | Applied in |
| --- | --- | --- | --- |
| 1 | What does "renewal" mean? | The existing progress-report flow. No new column. | Ticket 9 |
| 2 | Does a reopened decision still count as decided? | No. A reopen overwrites the prior decision, so the pair's latest election alone decides its state. | Tickets 5, 6 |
| 3 | How is a chair-canceled election counted? | As its own canceled outcome. | Tickets 5, 8 |
| 4 | When does a multi-dataset DAR count as decided? | Once every dataset is decided. Metrics will later move to dataset granularity, so per-pair rows stay the base. | Tickets 5, 6 |
| 5 | How do denials roll up? | Partial approval is its own category: approved, denied or mixed. | Ticket 5 |
| 6 | Do external collaborators count as researchers on a DAR? | No. They need their own approval and aren't approved with the DAR. | Ticket 4 |

Our reading of Decision 4, not product's: a canceled dataset is closed, so it doesn't hold a DAR
open or affect its outcome.

## Background

DAR state lives in four tables: `data_access_request` (the request, submission and SO-approval
timestamps), `dar_dataset` (datasets requested), `election` (one decision process per DAR-dataset
pair) and `vote` (what was decided and when).

`MetricsResource` has one dataset-scoped endpoint on `develop`; #3049 adds four more and rewrites
the metrics queries in `DataAccessRequestDAO`, so line references into that file will shift. The role
dashboards
(`DacDashboardResource`, `SigningOfficialDashboardResource`, `ResearcherDashboardResource`) answer
"what should this user do next", not "what has happened over time".

In scope: admin reporting endpoints, an admin dashboard in `duos-ui`, two fixes, and renewal
reporting. Out of scope: any change to how DAC voting, RADAR rules or SO approval behave.

## How the Data Works Today

Verified against `develop` at `bc9b9925`, before #3049.

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

Two other rankings exist. The researcher approvals page ranks by `create_date DESC, vote_id DESC`
(`ResearcherDashboardDAO.java:70-83`), and the study and dataset metrics queries use
`LAST_VALUE(v.vote) … ORDER BY v.create_date` with no tie-break (`DataAccessRequestDAO.java:81-84`,
`:136-138`, `:212-215`, and both queries #3049 adds). They usually agree, but can differ when a vote
is edited after a later one was created, so the admin report and the study page can disagree on
those pairs.

Reporting follows none of these. Under Decision 2 a reopen overwrites the prior decision, so the
pair's latest data-access election decides alone, chosen as
`findLastElectionByReferenceIdDatasetIdAndType` does (newest `create_date`), with `election_id DESC`
as a tie-break. Don't key on `archived = false`: it holds only because both reopen paths archive the
old elections before creating the new one. Its cast `FINAL` or
`RADAR_APPROVE` vote is the decision; with none cast, the pair is pending or canceled. The SO and
researcher dashboards keep the older cast vote, which is right for "does this researcher have access
now" and is left alone.

### Closeouts have two definitions

`DataAccessRequest.getIsCloseoutProgressReport` requires a child DAR with a non-empty
`closeoutSupplement.reasons`. Every SQL check counts any row with a `closeoutSupplement`
(`DataAccessRequestDAO.java:101`, `:160`, `:233`, `DatasetDAO.java:699`,
`ResearcherDashboardDAO.java:129`, `SigningOfficialDashboardDAO.java:94-100`, and #3049), without
checking `parent_id`. On the progress-report path they coincide, since `validateCloseoutSupplement`
rejects a supplement without reasons (`DataAccessRequest.java:405-424`); they differ only for a row
with a supplement and no `parent_id`, which SQL counts and Java doesn't. Reporting uses
`parent_id IS NOT NULL AND data->>'closeoutSupplement' IS NOT NULL`, which matches both in practice.

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

A canceled election is a pair that reached a DAC and closed without a decision; reporting counts it
as its own canceled outcome (Decision 3). Its `last_update` is overwritten if the election is later
archived by a reopen (`ElectionDAO.java:231`), so it is only a reliable cancellation time for
elections that were never archived.

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
the first `true` in production (May 2026), or the row is a progress report or closeout (`:349-355`
writes true only for non-closeout reports without pre-authorization, so closeouts stay NULL even
though they go to an SO).

So `requires_so_approval = false` matches nothing, and `DataAccessRequestMapper.java:57` maps NULL to
`false`, which hides this in Java. Use `requires_so_approval IS NOT TRUE AND parent_id IS NULL`,
limited to submissions from 20 May 2026, the first `true` in production
([DT-4183](https://broadworkbench.atlassian.net/browse/DT-4183)).

That cutoff applies to skip classification only. Closeout SO approvals could be recorded from June
2025 (`changelog-consent-2025-06-05-save-so-closeout-approval.xml`) and moved into
`approving_so_timestamp` by `changelog-consent-2026-06-18-consolidate-so-approval-fields.xml`, so
closeout approval times go back further than original-DAR ones. The first recorded approvals are
September 2025 for closeouts and June 2026 for original DARs.

### Expiration is computed

`expiresAt` is `submissionDate` plus 365 days (`models/DataAccessRequest.java:31`, `:154-163`). It is a
function of a stored timestamp and a constant, so expiry dates can be computed for all history.

Per DAR it is wrong for continuing access: a parent passes its 365 days while an approved progress
report keeps access going. The rule in use elsewhere is per collection and dataset: access runs 365
days from the newest submission approved on that dataset, and a pending progress report extends
nothing. A progress report can cover a subset of the parent's approved datasets
(`DataAccessRequestService.java:326-332`), so renewing one dataset needn't extend another. The
researcher dashboard applies this (`ResearcherDashboardDAO.java:111-132`) and drops closed-out
collections; the study page after #3049 (DT-4130) applies it and ends access on the closeout's filing
date. Ticket 7 uses the same per-pair rule, with the closeout date as #3049 has it.

This reads renewal as continuing review, where an approved progress report restarts the term, and
Decision 1 confirms it.

### Renewal is the progress-report chain

A renewal is an approved progress report: a `parent_id` child that isn't a closeout. `parent_id` is
UNIQUE (`changelog-consent-2025-05-16-disallow-pr-siblings.xml:5-6`), so each submission has at most
one child, and `createProgressReport` requires the datasets to be approved on the parent
(`DataAccessRequestService.java:318-331`). A progress report can cover a subset of the parent's
datasets, so a renewal is per collection and dataset, like expiry.

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
| 1 | DARs with a DAC decision vs. not | 5. Latest election per pair on original DARs; a DAR is decided once every pair is; canceled and archived DARs excluded; canceled elections their own outcome |
| 2 | Approved vs. denied | 5. `vote.vote` on the latest election's cast `FINAL`/`RADAR_APPROVE` vote; DARs roll up to approved, denied or mixed |
| 3 | Decided via RADAR | 5. `vote.type`. No production RADAR decisions yet |
| 4 | DARs per researcher | 4. `user_id`, `submission_date` |
| 5 | DARs per institution | 4. Snapshot from ticket 3 where present, current institution otherwise |
| 6 | Datasets per DAR | 4. `dar_dataset` |
| 7 | Researchers per DAR (PI, lab staff, internal collaborators) | 4. Per DAR, not distinct people |
| 8 | Submission to DAC decision | 6. `submission_date` to `vote.update_date`, per pair and per DAR (last pair decided) |
| 9 | Submission to SO approval, separating pre-auth skips | 7. `approving_so_timestamp`. Original DARs from June 2026, closeouts from September 2025; skips from May 2026 |
| 10 | DARs expired | 7. Per collection and dataset, from the newest approved submission; closeouts end access |
| 11 | DARs renewed | 9. Approved progress reports, closeouts excluded |
| 12 | Researchers submitting DARs | 4. Distinct `user_id` with a `submission_date` |
| 13 | Timestamped, over time | Every reporting ticket returns a timestamp per row |

## Data Caveats

These show on the endpoint docs and next to the affected dashboard figures.

| Caveat | Effect | Mitigation |
| --- | --- | --- |
| Institution is current, not historical, before ticket 3 | A researcher who changes employer takes their past DARs with them | Ticket 3, forward-only |
| Collaborators have no stable identity | Cross-DAR person counts are an email-join approximation | Only report per-DAR counts |
| `requires_so_approval` NULL has three meanings | Skips, rows before May 2026 and closeouts look the same | Use the predicate above |
| Short history | No RADAR decisions in production; SO approval on original DARs from June 2026, on closeouts from September 2025 | Don't chart metrics 3 and 9 yet; list metric 9's values |
| Low volume | A few original DARs a month | Default to quarterly buckets or cumulative counts |
| Election cancel time | Lost if the election was later archived | Report the cancellation, not its date, for archived elections |
| Election cancel actor | Not recorded; admin and chair cancellations are identical rows | Don't split by who canceled |

Deciding votes cast before March 2021 have a null `update_date`, so ticket 6 reports turnaround from
then; their outcomes still count in ticket 5.

## Scalability

The reporting queries are aggregations over full history, so they will get slower as data grows. For
now the volume is small and the endpoints are admin-only with low traffic, so
plain Postgres queries should be enough, provided every reporting endpoint takes a required date range,
computes counts and summaries in SQL over it, and paginates its row detail. Index the join and date
columns.

Ticket 1 measured this in production: every query it timed ran well under 100 ms from memory, so
Postgres is enough (figures and plans on
[DT-4183](https://broadworkbench.atlassian.net/browse/DT-4183)). If that changes, the options in rough
order of effort are: add indexes; precompute into a materialized view refreshed on a schedule; or
publish DAR lifecycle events to an Elasticsearch index, alongside the existing dataset index
(`ElasticSearchService`), and serve reporting from there. The endpoint contracts in tickets 4 to 7
don't depend on which backs them, so this can change later without touching the dashboard.

## Design Decisions

| Question | Decision |
| --- | --- |
| Record new lifecycle events? | No, except the institution snapshot. Everything else is already stored or computable from what is; copying it would create a second source that can disagree with the first. |
| Where do decisions come from? | `vote.vote`, `vote.type` and `vote.update_date`. Not `Election.finalVoteDate` or `election.final_access_vote`. |
| Store decision rollups on `data_access_request`? | No. A reopen would have to invalidate them, and product plans to move metrics to dataset granularity. Compute at query time. |
| Where do the endpoints go? | `MetricsResource`, with `@RolesAllowed(ADMIN)` on each new method. Its existing endpoint is `@PermitAll` at the method level, so the two coexist. Queries go in `MetricsService` and a new `DarMetricsDAO`. |
| One endpoint or several? | Several, one per metric family, so each is a reviewable change and releases independently. |
| Row granularity? | Volume (ticket 4) returns one row per original DAR. Decisions and turnaround (tickets 5 and 6) return one row per DAR-dataset pair on original DARs, because decisions are made per pair; DAR-level figures are rolled up from those rows in SQL. |
| Request shape? | Tickets 4 to 7 require `from` and `to` on `submission_date`, take an optional `bucket` (day, week, month, quarter), return per-bucket counts and summaries computed in SQL, and paginate row detail. |
| Personal data? | Responses carry IDs, counts, enums and timestamps only. No names, emails or eRA Commons IDs, which sit in the DAR `data` JSON (`Collaborator`). |

## Sequencing

1. **Tickets 1, 2 and 3 first (done).** Ticket 1 set the honest date ranges and checked query cost.
   Tickets 2 and 3 fixed existing problems: ticket 2 was a bug in queries existing code already calls,
   and ticket 3 is forward-only, so every week it waited was a week of institution history lost.
2. **Tickets 4 to 7 in parallel.** #3049 rewrote
   `MetricsResource`, `MetricsService` and the metrics queries these tickets extend. Each adds a
   method to those and to `DarMetricsDAO`; the first to merge creates the DAO. That is a merge
   conflict, not a dependency.
3. **Ticket 9** alongside them; it reuses ticket 7's per-pair access rule.
4. **Ticket 8** (`duos-ui`) once 4 to 7 and 9 are deployed.

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
- `EXPLAIN ANALYZE` timings for the latest-vote CTE over all pairs, the per-DAR volume query, and the
  existing study and dataset metrics queries.
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

- Liquibase changeset adding nullable `institution_id`, `institution_name` and
  `institution_snapshot_date` to `data_access_request`, with a `changelog-master.xml` include.
  `institution_id` references `institution` `ON DELETE SET NULL` so an admin can still delete an
  institution; the name keeps the history when that happens. The date is set on every submission,
  so a submission with no institution isn't mistaken for one from before the column existed.
- Write it in the existing submission transaction in `DataAccessRequestService`, next to
  `captureDatasetDaaSnapshots`.
- Record the institution the submission was validated against. Reporting reads the columns in SQL,
  so `DataAccessRequestMapper` and the DAR response are unchanged.
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

- Submission records the institution and the recorded date, including when there's no institution.
- Changing the user's institution leaves an existing DAR's value alone.
- Deleting an institution keeps the recorded name.

**Out of scope:** backfilling past DARs, which isn't recoverable. Reading the column is in ticket 4.

---

### Ticket 4 (DT-4186): DAR volume and composition reporting

**Type:** Story · **Size:** 5 · **Depends on:** ticket 3, #3049

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
- Fall back only when `institution_snapshot_date` is NULL. A recorded NULL institution reports as
  "No institution", not the user's current one.
- Report the institution's current name through `institution_id`, so an admin rename shows. Use the
  recorded name only when the id is NULL, meaning the institution was deleted after submission.
- Switch the study and dataset metrics queries, which read `users.institution_id` live, to the
  recorded institution of the submission they already display, falling back to live. They source a
  row from the latest qualifying submission and this endpoint from the original DAR, so the two can
  still differ after an employer change; each is right for its own submission.
- Collaborator counts are per DAR; don't expose a distinct-person count. External collaborators
  aren't counted (Decision 6).
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
- The study and dataset metrics queries report the recorded institution of the submission they show.
- Progress reports, closeouts and drafts aren't counted.
- No collaborator name, email or eRA Commons ID is serialised.

**Tests**

- DAO tests per aggregation, including a multi-dataset DAR and one with no datasets.
- A null institution; a recorded institution that differs from the user's current one.
- A recorded institution that was later deleted reports its recorded name.
- A renamed institution reports its current name.
- A progress report recorded with no institution doesn't fall back to the user's current one.
- PI, lab staff and internal collaborator counts reported separately.
- A DAR with an external collaborator doesn't count them.
- A collection with a progress report counts once.
- The serialised response contains no collaborator name or email.
- Admin and non-admin resource tests.

---

### Ticket 5 (DT-4187): DAC decision reporting

**Type:** Story · **Size:** 5 · **Depends on:** #3049

Report, per DAR-dataset pair and rolled up per DAR, whether a DAC decision exists, whether it
approved or denied, and whether RADAR or a chair made it (metrics 1, 2 and 3).

**Notes**

- Latest-election fragment in `DarMetricsDAO`, shared with ticket 6: per `(reference_id, dataset_id)`,
  the newest data-access election (`create_date DESC, election_id DESC`) and its cast
  `FINAL`/`RADAR_APPROVE` vote (`v.vote IS NOT NULL`, tie-break `vote_id DESC`). Earlier elections
  don't count (Decision 2; see
  [A DAR-dataset pair can have several elections](#a-dar-dataset-pair-can-have-several-elections)).
- Switch the study and dataset metrics queries from `LAST_VALUE … ORDER BY v.create_date` to the same
  fragment, so the study page and this report agree on every pair. This is visible: those queries
  list approved pairs only (`last_vote = TRUE`), so a reopened pair with no new vote stops counting
  as approved on that submission.
- Bound the ranking inside its subquery to elections on DARs in the requested range. A filter
  outside it can't be pushed in, so the window sorts every election and vote in the table (DT-4178).
- Original DARs only (`parent_id IS NULL AND submission_date IS NOT NULL`). Exclude canceled and
  archived DARs with `LOWER(data->>'status') IN ('canceled', 'archived')`, keeping NULL status.
- Each pair row carries `state`, from the latest election: `APPROVED` or `DENIED` if its vote is cast,
  `PENDING` if open with no cast vote, `CANCELED` if canceled with no cast vote, `NO_ELECTION` if none
  exists yet. Plus the deciding vote's type and `update_date`, null unless decided.
- Start from `dar_dataset` and left-join elections and votes. A pair waiting on SO approval has no
  election (`DarCollectionService.java:945`, `:1151`) and must still count as undecided.
- DAR rollup (Decisions 4 and 5), from the pair rows:
  - `PENDING` while any pair is `PENDING` or `NO_ELECTION`;
  - otherwise `CANCELED` if every pair is `CANCELED`;
  - otherwise decided: `APPROVED` if every decided pair approved, `DENIED` if every one denied,
    `MIXED` if both. Canceled pairs don't affect the outcome;
  - decided-via `RADAR`, `MANUAL` or `MIXED`, over the decided pairs;
  - decision date is the latest pair decision.
- Required date range, optional bucket, paginated rows at both levels; per-bucket counts by state
  and vote type in SQL.
- Read `vote.vote` and `vote.type`, not `election.final_access_vote`.

**Acceptance criteria**

- A pair that went denied, approved, denied reports denied.
- A pair decided then reopened with no new vote reports `PENDING`, with no decision timestamp.
- A pair decided, reopened, then canceled reports `CANCELED`.
- A pair whose election was canceled by a chair reports `CANCELED`, not pending.
- A pair with no election reports `NO_ELECTION` and counts as undecided.
- A multi-dataset DAR with one RADAR and one manual decision reports both pairs correctly and rolls
  up as decided via `MIXED`.
- A DAR with one approved and one denied pair rolls up `MIXED`; with one pending pair, `PENDING`.
- A DAR with one approved and one canceled pair rolls up `APPROVED`; with all pairs canceled,
  `CANCELED`.
- A DAR's decision date is its last pair's decision.
- A canceled DAR is in no count. An archived DAR and a progress report are excluded.
- Every row has the DAR's `submission_date`, plus a nullable decision timestamp.
- Other roles get 403.

**Tests**

- One DAO test per pair history: single decision, reopened and re-decided, reopened with no vote
  yet, election canceled, decided then reopened then canceled, no election yet.
- One per DAR rollup: all approved, all denied, mixed, one pending, approved plus canceled, all
  canceled.
- A multi-dataset DAR mixing RADAR and manual decisions.
- Canceled and archived DARs are excluded, whatever the status casing.
- An uncast vote doesn't win.
- A reopened pair with no new vote: this endpoint reports it pending, and the study metrics query
  no longer counts it approved.

---

### Ticket 6 (DT-4188): DAC decision turnaround reporting

**Type:** Story · **Size:** 3 · **Depends on:** #3049; shares the latest-election fragment with
ticket 5

Report time from submission to DAC decision (metric 8), `submission_date` to `vote.update_date` on the
deciding vote, per pair and per DAR.

**Notes**

- Whichever of tickets 5 and 6 lands first adds the latest-election fragment; the other reuses it.
- A reopened and re-decided pair measures to the new decision; a reopened pair with no new vote is
  undecided and excluded (Decision 2).
- A DAR measures to its last pair decision, and only once ticket 5's rollup calls it decided
  (Decision 4).
- Exclude deciding votes with a null `update_date` and return the excluded count. Don't fall back to
  `create_date`: that is when the election opened, so it would understate turnaround. A DAR with any
  such pair is excluded and counted too.
- Same population and bounded ranking as ticket 5 (original DARs, canceled and archived excluded,
  ranking limited to the range inside its subquery).
- Required date range, optional bucket, and a `level` of pair or DAR. Mean, median and mode computed
  in SQL per bucket; elapsed times paginated.

**Acceptance criteria**

- Turnaround for decided pairs and decided DARs within the requested range.
- A reopened and re-decided pair measures to the new decision.
- A DAR with one pair still pending is excluded; once decided, it measures to its last decision.
- Null-`update_date` pairs excluded and counted.
- Mean, median and mode returned per bucket at both levels.
- Other roles get 403.

**Tests**

- Known turnaround for a manual and a RADAR decision.
- A two-dataset DAR decided on different days reports the later day.
- A reopened and re-decided pair.
- Null `update_date` excluded and counted.
- Mean, median and mode against a fixed synthetic set.

---

### Ticket 7 (DT-4189): SO approval and expiration reporting

**Type:** Story · **Size:** 5 · **Depends on:** #3049

Report submission-to-SO-approval time, separate requests that skipped SO review through
pre-authorization, and count expired DARs (metrics 9 and 10).

**Notes**

- Skips: `requires_so_approval IS NOT TRUE AND parent_id IS NULL`, submitted from 20 May 2026. Never
  `= false`.
- Report progress reports and closeouts separately, identifying closeouts by the reporting definition
  (see [Closeouts](#closeouts-have-two-definitions)).
- Compute access end per collection and dataset in SQL, by the rule in
  [Expiration](#expiration-is-computed): the newest submission approved on that dataset + 365 days,
  or the closeout date if earlier. Return the end reason, `EXPIRED` or `CLOSED_OUT`; the study page
  shows both as "Expired". A collection counts as expired once every dataset's access has ended.
- Required date range, optional bucket, paginated rows; summaries in SQL.
- Note in the OpenAPI path spec that skip classification starts 20 May 2026 and approval times start
  June 2026 for original DARs and September 2025 for closeouts.

**Acceptance criteria**

- Turnaround for DARs with `approving_so_timestamp`, with per-request values and summary statistics.
- Skips are distinguishable from DARs still waiting on an SO.
- A closeout that went to an SO isn't counted as a skip.
- DARs submitted before 20 May 2026 aren't counted as skips.
- Expiry agrees with `EXPIRATION_DURATION_MILLIS` either side of the boundary.
- A parent older than 365 days with an approved progress report newer than 365 days isn't expired.
- A pending progress report doesn't extend access.
- A closeout ends access on its filing date, reported as `CLOSED_OUT`.
- Per collection and dataset, the access-end date matches the dataset metrics query.
- Renewing one dataset doesn't extend another dataset in the same collection.
- Every row has a timestamp. Other roles get 403.

**Tests**

- One per `requires_so_approval` state: true and approved, true and pending, NULL original, NULL
  closeout, NULL before 20 May 2026.
- `= false` returns nothing.
- Expiry either side of 365 days; a collection kept live by an approved progress report; one not
  kept live by a pending one; a closeout before 365 days.

---

### Ticket 8 (DT-4190): Admin DAR analytics dashboard

**Type:** Story · **Size:** 8, split by section if needed · **Depends on:** tickets 4 to 7 and 9
deployed · **Repo:** `duos-ui`

An admin page showing the decision funnel, turnaround distributions, volume, and expiration and
renewal over time.

**Notes**

- Build on `src/components/dashboard/{ConsoleDashboard,ConsoleDashboardGrid}.tsx`.
- Sections: decision funnel; DAC turnaround with mean, median and mode; SO approval times as a list
  of values; volume; expiration and renewal. Add the RADAR vs. manual split and an SO turnaround
  chart once [Data Caveats](#data-caveats) lifts the short-history limit on metrics 3 and 9.
- Every time series can be bucketed by day, week, month or quarter, defaulting to quarter.
- Show caveats next to the figures they affect: institution source, the SO history bounds, and
  ticket 6's excluded count.

**Acceptance criteria**

- The funnel counts DARs as pending, approved, denied, mixed or canceled, per ticket 5's rollup.
- Pair-level counts are available beside the DAR ones.
- Only admins can reach the page.
- Each section shows an empty state when it has no data.
- Caveats appear on the page, not only in docs.

**Tests**

- Component tests per section against fixtures, including empty states.
- A non-admin can't reach the page.

**Out of scope:** bulk export.

---

### Ticket 9 (DT-4191): DAR renewal reporting

**Type:** Story · **Size:** 2 · **Depends on:** #3049

Report renewals (metric 11). A renewal is an approved progress report (Decision 1), so this is
reporting only, with full history and no schema change.

**Notes**

- Count progress-report pairs (`parent_id IS NOT NULL`) approved under ticket 5's latest-election
  fragment, excluding closeouts by the reporting definition (see
  [Closeouts](#closeouts-have-two-definitions)) and canceled or archived DARs.
- Count per collection and dataset, dated by the progress report's `submission_date`, which is what
  ticket 7 restarts the term from. Carry the approving vote's `update_date` too.
- Required date range on `submission_date`, optional bucket, paginated rows.

**Acceptance criteria**

- Renewals are reportable over time.
- A closeout, a pending progress report and a denied one aren't renewals.
- A progress report covering one of two approved datasets counts one renewal.
- Every row has a timestamp. Other roles get 403.

**Tests**

- Approved, pending, denied and closeout progress reports.
- A progress report covering a subset of the parent's datasets.

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
| 8. Dashboard (`duos-ui`) | [DT-4190](https://broadworkbench.atlassian.net/browse/DT-4190) | all | 8 | 4–7, 9 |
| 9. Renewal | [DT-4191](https://broadworkbench.atlassian.net/browse/DT-4191) | 11 | 2 | shares a fragment with 5 |

## Test Matrix

Across the reporting tickets, cover at least:

- a pair that went denied, approved, denied;
- a reopened pair with no cast vote (pending) and one re-decided (the new decision);
- a multi-dataset DAR mixing RADAR and manual decisions;
- a RADAR-decided election in both election queries (ticket 2);
- a canceled or archived DAR (excluded) and a chair-canceled election (its own state);
- a collection with a progress report, counted once;
- each `requires_so_approval` state, and `= false` matching nothing;
- a deciding vote with a null `update_date`;
- expiry either side of 365 days, an approved vs. pending progress report, and a closeout;
- a DAR with no decision, some datasets decided, and all decided, including approved, denied and
  mixed outcomes;
- an approved progress report (a renewal) and a closeout (not one);
- no collaborator name or email in any response;
- admin and non-admin access to every endpoint.

Synthetic data only. No Mockito `lenient()` stubbing.

## Out of Scope

- Changes to DAC voting, RADAR rules, SO approval or cancellation behavior.
- Decision rollup columns on `data_access_request`.
- Bulk CSV or warehouse export; a follow-up if the dashboard isn't enough.
- Backfilling historical institution.
- Dataset-granularity metrics. Product wants them eventually; the per-pair rows here are the base.

## Definition of Done

- Admin endpoints return flat, timestamped rows for every metric.
- Decision outcome, source and turnaround can be queried over any range of recorded history, with
  excluded rows counted.
- SO turnaround separates pre-authorization skips from pending approvals and from progress reports.
- RADAR-decided elections appear in both election queries with a decision date.
- New DARs record their submitter's institution.
- The `duos-ui` dashboard shows funnel, turnaround, volume, expiration and renewal with caveats
  beside the figures.
- Apart from tickets 2 and 3, no existing DAR, voting, SO approval or cancellation behavior has changed.
