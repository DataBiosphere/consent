# DAR Metrics and Analytics Plan

## Status

Proposed. Implementation can begin on six of the nine tickets immediately; the rest wait on four
product decisions listed in [Blocking Decisions](#blocking-decisions).

## Summary

DUOS is asked to report thirteen Data Access Request (DAR) metrics — decision outcomes, how decisions
were reached, approval turnaround, submission volume, expiration and renewal — as longitudinal data on
an internal admin dashboard.

Twelve of the thirteen can be answered from columns Consent already persists, across full history. The
work is therefore a reporting layer, not a change to how DUOS records anything: four read-only
endpoints in `consent`, one dashboard in `duos-ui`, and one defect fix. The thirteenth metric, renewal,
has no representation in DUOS at all and needs a product definition before it can be built.

Nine tickets, each independently deployable and scoped to a single reviewable change. Five can be
worked in parallel. The critical path to a usable dashboard is five tickets.

## Objective

Give DUOS administrators a single internal view of how Data Access Requests move through the system:
how many are submitted and by whom, how many reach a DAC decision and what that decision was, how
often RADAR decides instead of a chairperson, how long Signing Official (SO) and DAC approval take,
and how many requests expire.

Every figure must be returned as flat, timestamped rows so it can be bucketed by day, week or month
and fed directly into descriptive statistics — mean, median, mode — without reshaping on the client.

## Blocking Decisions

Four questions need answers from product. Only the first prevents work starting; the rest shape what
a later deliverable is allowed to claim, and two of them change numbers a reader of the dashboard
would act on. They are listed first because the cost of discovering them late is rework on endpoints
that have already shipped.

| # | Decision needed | What it gates | Cost of guessing |
| --- | --- | --- | --- |
| 1 | **What "renewal" means.** Is it the existing progress-report continuing-review flow, or a researcher submitting a fresh request after one expired? These are different flows and only the first exists today. | Ticket 8 entirely. Nothing can be built without it. | Total rework — the two readings need different schema. |
| 2 | **Whether a reopened decision still counts as decided.** When a decided DAR-dataset pair is reopened, the last cast vote stands but the live election has no vote. | Ticket 7. Tickets 4 and 5 return both readings until it is answered; the dashboard has to pick one. | The funnel silently over- or under-reports outstanding work, which is the number the dashboard exists to show. |
| 3 | **When a multi-dataset DAR counts as decided** — on the first dataset decision, or once all are in. | The deferred DAR-level rollup over tickets 4 and 5. Per-pair rows ship without it. | Turnaround averages shift materially for multi-dataset requests. |
| 4 | **Whether a DAR is denied if any dataset is denied, all are, or partial approval is its own bucket.** | The deferred DAR-level rollup over ticket 4. Per-pair rows ship without it. | Approval rate is wrong in a way no one can see from the chart. |

**None of these gates a reporting ticket's release.** Tickets 4 and 5 ship per-DAR-dataset rows, which
are correct under every reading; Decisions 3 and 4 shape only the DAR-level rollup, a later reduction
over those rows, and Decision 4 gates nothing else. Decision 2 is needed before ticket 7 presents a
single funnel figure, and until it is answered tickets 4 and 5 return both readings rather than
choosing one. Only Decision 1 blocks a ticket outright.

Answering any of 2 to 4 changes a query, not a schema.

A fifth question is smaller and engineering-answerable: whether "researchers on a DAR" should include
external collaborators, who unlike internal collaborators are not institution-validated at submission.
It affects the shape of ticket 3's response only.

## Background and Scope

DUOS records DAR state across four entities: `data_access_request` holds the request and its
submission and SO-approval timestamps, `dar_dataset` links it to the datasets requested, `election`
opens a decision process per DAR-dataset pair, and `vote` records what was decided and when.

Most of what the dashboard needs is already in those tables. What does not exist is a reporting
surface over them: the current `MetricsResource` exposes a single dataset-scoped endpoint, and the
role dashboards (`DacDashboardResource`, `SigningOfficialDashboardResource`,
`ResearcherDashboardResource`) answer "what should this user do next", not "what has happened over
time".

In scope: read-only reporting endpoints in `consent`, an admin dashboard page in `duos-ui`, one defect
fix, and two optional accuracy improvements.

Out of scope: any change to how DAC voting, RADAR rules or SO approval behave. This is an observation
layer over existing decision logic.

## Current Behavior

The facts below determine how each metric is queried. Each was verified against `develop`.

### Decisions live on votes, not on elections

A decision is a `vote` row whose `type` is `FINAL` (a chairperson) or `RADAR_APPROVE` (an automation
rule), with `vote.vote` carrying approve or deny. Casting it closes the election
(`service/dao/VoteServiceDAO.java:61-67`).

Two columns look like they hold the outcome and do not. `election.final_access_vote` has no production
writer — the only `updateElectionById` overload that sets it (`ElectionDAO.java:220-226`) is called
solely from tests. `Election.finalVoteDate` is not a column at all; it is derived in SQL, two different
ways, and neither sees RADAR (see [the RADAR gap](#radar-decisions-are-invisible-to-two-election-queries)).

Reporting reads `vote` directly.

### The decision timestamp is `vote.update_date`

`VoteServiceDAO.updateVotesWithValue:41-44` writes `update_date` when a vote is cast, for manual and
RADAR decisions alike. The only other two writes to the `vote` table — the reminder flag
(`VoteDAO.java:78`) and a rationale edit (`VoteDAO.java:113`) — deliberately leave it alone.

It is therefore a durable record of when a decision was made, with full history.

### A DAR-dataset pair accumulates elections

Reopening a decision archives the previous elections and opens a new one
(`service/dao/DarCollectionServiceDAO.java:85`), so one pair can carry a sequence: denied, then
approved, then denied again. `DataAccessRequestDAO.java:56-64` documents the reduction — rank the
pair's votes and take the most recent.

`SigningOfficialDashboardDAO.java:48-64` implements it as a pair of CTEs, `ranked_final_votes` feeding
`latest_final_votes`. Three details in that query matter and are easy to omit:

- it accepts both `final` and `radar_approve` vote types;
- it requires `v.vote IS NOT NULL`, so an uncast vote on a freshly reopened election cannot win the
  ranking and report a decision nobody made;
- it orders by `COALESCE(v.update_date, v.create_date) DESC, v.vote_id DESC`, making ties
  deterministic.

`ResearcherDashboardDAO.java:46-68` applies equivalent logic under different CTE names. Either is the
precedent to copy.

### RADAR decisions are invisible to two election queries

Both derivations of `final_vote_date` join the vote on `LOWER(v.type) = 'final'`, so a
`RADAR_APPROVE` vote never matches. They fail differently:

- `findElectionWithFinalVoteById` (`ElectionDAO.java:58-72`) uses an INNER JOIN (`:66`), so a
  RADAR-only election is **absent from the result entirely**;
- `findLastElectionsByReferenceIds` (`:130-160`) uses a LEFT JOIN (`:145`), so the election is returned
  with null vote fields.

`:62` compounds it by deriving the date from `v.create_date`, which is when the election opened rather
than when anyone voted. Ticket 2 fixes both.

### SO approval is timestamped; its requirement flag is not always written

`updateDarApprovalSO` sets `approving_so_id` and `approving_so_timestamp = now()`
(`DataAccessRequestDAO.java:521-524`) — a reliable event timestamp.

`requires_so_approval` is computed at submission by `requiresSOApproval`
(`DataAccessRequestService.java:898-905`) as an OR: a dataset carries the `REQUIRE_SO_DAR_APPROVAL`
automation rule, or the researcher is not pre-authorized for all required Data Access Agreements.

The column is nullable with no default
(`changelog-consent-2026-01-19-add-requires-so-approval-attribute.xml:6`) and is written **only when
true** (`DataAccessRequestService.java:298-300`). NULL therefore means three different things: approval
was skipped because the researcher was pre-authorized; the row predates the column (January 2026); or
the row is a progress report or closeout, which take a separate path
(`DataAccessRequestService.java:349-355` writes `true` only for non-closeout reports whose submitter
lacks pre-authorization, leaving closeouts NULL even though they do go to an SO).

Consequences for any query: `WHERE requires_so_approval = false` matches nothing under Postgres null
semantics and fails silently. `DataAccessRequestMapper.java:57` coerces the NULL to primitive `false`,
so the problem is invisible from the service layer. The usable predicate is
`requires_so_approval IS NOT TRUE AND parent_id IS NULL`, date-bounded.

### Expiration is derived, not stored

`expiresAt` is `submissionDate + EXPIRATION_DURATION_MILLIS` (365 days), recomputed whenever the object
is hydrated (`models/DataAccessRequest.java:31`, `:154-163`). Because it is a pure function of a
persisted timestamp and a constant, the date any DAR expired is computable retroactively and is stable
over time. Reporting computes it; nothing needs capturing.

### Cancellation is a JSON status

`DarStatus.CANCELED` is written into `data->>'status'` and read back by string comparison
(`resources/DarCollectionResource.java:301-305`). There is no cancellation timestamp. A canceled DAR is
neither decided nor awaiting a decision, so every funnel query must exclude it explicitly or it counts
as pending indefinitely.

### Renewal has no representation

The nearest artifact is the progress-report chain, and it cannot be reused: `parent_id` is UNIQUE
(`changelog-consent-2025-05-16-disallow-pr-siblings.xml:5-6`), so a DAR has at most one child, and it
already means continuing review — `createProgressReport` requires the datasets to be approved on the
parent (`DataAccessRequestService.java:318-331`). Renewal needs its own column and its own definition.

### Collaborators are validated but not identified

At submission, internal collaborators and lab staff are looked up by email, must resolve to a DUOS user
holding a library card, and must share the submitter's institution
(`DataAccessRequestService.java:617`, `:718`). External collaborators do not go through that check.

`Collaborator` is a record of name, email, title, era commons id and country stored in the DAR's JSON,
with no user-id foreign key. Counting collaborators per DAR is exact; identifying the same person
across DARs means joining on email after the fact.

### Institution is read live

`users.institution_id` is the only source. The DAR's own `institution` property is in
`DataAccessRequestData.DEPRECATED_PROPS` and is stripped at submission, so nothing records where a
researcher worked when they applied.

## Requirements → Behavior

| # | Requested metric | Where it lands |
| --- | --- | --- |
| 1 | DARs with a DAC decision vs. not | Ticket 4. Latest cast vote per DAR-dataset pair; canceled DARs excluded. Exact meaning set by Blocking Decision 2 |
| 2 | Decision results: approved vs. denied | Ticket 4. `vote.vote` on the latest cast `FINAL`/`RADAR_APPROVE` vote. DAR-level rollup by Blocking Decision 4 |
| 3 | DARs decided via RADAR rules | Ticket 4. `vote.type`. History from July 2025 |
| 4 | DARs submitted per researcher | Ticket 3. `user_id` + `submission_date` |
| 5 | DARs submitted per institution | Ticket 3, current institution only. Ticket 9 makes it historical |
| 6 | Datasets requested per DAR | Ticket 3. `dar_dataset` |
| 7 | Researchers per DAR (PI, lab staff, internal collaborators) | Ticket 3. Counts per DAR, not distinct people |
| 8 | Turnaround: submission → DAC decision | Ticket 5. `submission_date` to `vote.update_date` |
| 9 | Turnaround: submission → SO approval, delineating pre-auth skip | Ticket 6. `approving_so_timestamp`; skip via `IS NOT TRUE AND parent_id IS NULL`. History from January 2026 |
| 10 | DARs expired | Ticket 6. Computed from `submission_date` |
| 11 | DARs renewed | Ticket 8, conditional on Blocking Decision 1 |
| 12 | Number of researchers submitting DARs | Ticket 3. Distinct `user_id` where `submission_date` is not null |
| 13 | Timestamp everything, longitudinal | Every reporting ticket returns a timestamp per row; bucketing is a client concern |

## Data Accuracy Caveats

Four limits are properties of the data, not of the queries. Each needs stating on the endpoint and on
the dashboard, because a number that is quietly wrong is worse than one that is absent.

| Caveat | Effect | Remedy |
| --- | --- | --- |
| **Institution is current, not historical** | A researcher changing employer moves all their past DARs with them | Ticket 9, forward-only |
| **Collaborators have no durable identity** | Per-DAR counts are exact; distinct-person counts across DARs are an email-join approximation | State as approximate; do not present as a headcount |
| **`requires_so_approval` NULL is overloaded** | Skips, pre-column rows and closeouts are indistinguishable without extra predicates | `IS NOT TRUE AND parent_id IS NULL`, bounded to submissions after January 2026 |
| **Short history on two metrics** | RADAR exists from July 2025, SO columns from January 2026 | No year-over-year comparison on metrics 3 and 9 yet |

A fifth is a measurement rather than a known limit: deciding votes old enough to predate the current
vote-casting path may carry a null `update_date`, which would make their turnaround wrong rather than
missing. Ticket 1 quantifies it before anything is promised.

## Recommended Decisions

| Question | Decision |
| --- | --- |
| Capture new lifecycle events? | No. Every requested fact except renewal is already persisted or derivable. Re-recording them would duplicate facts that can then disagree with their source, and would make metrics forward-only that are currently answerable across full history. |
| Read decisions from where? | `vote.vote`, `vote.type` and `vote.update_date`, never `Election.finalVoteDate` or `election.final_access_vote`. |
| Denormalize decision rollups onto `data_access_request`? | No. The rollup rules are unresolved, and an election history would need invalidating whenever a later election supersedes an earlier one. Reduce at query time. |
| Extend `MetricsResource`? | No. It is `@PermitAll`; an admin analytics surface must not inherit that. New admin-scoped resource, same service and mapper patterns. |
| One endpoint or several? | Several, split by metric family. A single surface covering every metric would be too large to review in one pass, and would couple unrelated queries into one release. |
| Row granularity? | Per endpoint, not global. Volume and composition (ticket 3) returns one row per DAR, since that is the unit being counted. Decision and turnaround reporting (tickets 4 and 5) returns one row per DAR-dataset pair, because a decision is made per pair. DAR-level decision aggregates are reductions over the pair rows. Every row carries a timestamp either way. |

## Implementation Approach

### Sequencing

Ticket 1 is a measurement spike and should run first, because its answers set the honest date range
for tickets 5 and 6 and could change what the dashboard promises.

Tickets 2 to 6 are then independent of one another and can be worked in parallel — a fan-out, not a
stack. None of them is gated on a blocking decision; see [Blocking Decisions](#blocking-decisions) for
what each decision actually shapes.

Ticket 7 (`duos-ui`) consumes tickets 3 to 6 at runtime and follows them, and is the one place
Decision 2 must be settled, because a dashboard has to show one funnel number. Tickets 8 and 9 are
conditional and optional respectively.

The critical path to a usable dashboard is tickets 3 to 7. Ticket 1 should precede them but does not
block them, and ticket 2 can land at any point.

### Shared surface

Tickets 3 to 6 each add endpoints to a new admin-scoped `DarAnalyticsResource`, with a matching
service and DAO. Whichever merges first creates those three files; the others add a method to each.
That is a small textual conflict, not an ordering dependency — none depends on another's behavior.

Every endpoint ticket also touches, by convention and counted in its estimate:

- `ConsentApplication.java` — `env.jersey().register(...)`
- `ConsentModule.java` — a `@Provides @Singleton synchronized` provider
- `src/main/resources/assets/paths/<endpoint>.yaml`, and its entry in `api-docs.yaml`

### Sizing

Each ticket is scoped to one reviewable change against a comparable existing surface — the role
dashboards are the closest analogue, each being a resource, service, DAO and response model with its
tests. Where a metric family would have grown past that, it was split rather than carried.

Points below are relative estimates, not durations.

## Jira-Ready Tickets

### Ticket 1: Measure reporting data coverage in production

**Issue type:** Spike

**Suggested size:** 2 points

**Dependencies:** None

**Summary**

Establish how much history each metric actually has before any endpoint promises a trend line.

**Description**

Every metric in this plan is answerable in principle. What is unknown is how many rows exist to answer
it with. Two of the metrics have short histories by construction, and one depends on a timestamp that
may be absent on older rows.

Run counts against production and record them in this plan, so the dashboard's date ranges and the
product conversation are grounded in real volume.

**Implementation notes**

- Read-only queries. No schema or code changes.
- Query the `consent` schema via `pg_catalog` and `::regclass`, not `information_schema` with
  `table_schema = 'public'`.

**Acceptance criteria**

- Submitted DARs per month for the last 24 months is recorded.
- The count of DAR-dataset pairs with a closed data-access election is recorded.
- The count of `RADAR_APPROVE` votes, and the earliest, is recorded.
- The count of DARs with a non-null `approving_so_timestamp`, and the earliest, is recorded.
- The count of **cast** `FINAL`/`RADAR_APPROVE` votes with a null `update_date` is recorded, with the
  newest such vote's date. The count must require `v.vote IS NOT NULL` and be scoped to data-access
  elections: election creation inserts uncast `FINAL` votes with neither a value nor an update
  timestamp (`service/dao/DarCollectionServiceDAO.java:149`, `:181`), and counting those would measure
  pending work rather than missing history.
- The count of DARs with a null `requires_so_approval` split by `parent_id IS NULL`, so skips can be
  separated from progress reports.
- This plan is updated with the figures and with any metric the volume makes not worth charting.

**Tests**

None; this is a measurement.

**Out of scope**

Any change to application code or schema.

---

### Ticket 2: Report RADAR decisions from the election queries

**Issue type:** Story

**Suggested size:** 3 points

**Dependencies:** None

**Summary**

Fix two election queries that cannot see RADAR-approved decisions, and one that dates a decision from
when its election opened.

**Description**

Both derivations of `final_vote_date` in `ElectionDAO` restrict the vote join to type `final`, so a
`RADAR_APPROVE` vote never matches. `findElectionWithFinalVoteById` uses an INNER JOIN, so a
RADAR-decided election is missing from its results entirely; `findLastElectionsByReferenceIds` uses a
LEFT JOIN, so the election is returned with a null vote and null date. Separately, the first query
derives the date from `v.create_date`, which is when the election opened rather than when the vote was
cast.

This predates the analytics work and affects any consumer of `Election.finalVote`, which is why it is
separated from the reporting tickets.

**Implementation notes**

- Accept both `final` and `radar_approve` in the vote-type filter, matching
  `SigningOfficialDashboardDAO.java:48-64`.
- Derive the date from `COALESCE(v.update_date, v.create_date)` in both queries.
- The INNER JOIN change alters which elections are returned, not only which fields are populated.
  Identify what consumes both queries in `duos-ui` and confirm a newly appearing election does not
  change a count or list the UI depends on.

**Acceptance criteria**

- A RADAR-approved election is returned by `findElectionWithFinalVoteById`, having previously been
  absent.
- A RADAR-approved election returned by `findLastElectionsByReferenceIds` carries its vote value and a
  non-null decision date.
- Both queries date a manual decision from when the vote was cast, not when the election opened.
- Elections with no cast vote are unaffected.
- `duos-ui` consumers of both queries have been identified and confirmed unaffected, or updated.

**Tests**

- A test per query asserting the RADAR case, each failing before the fix and passing after.
- A regression test that a manual `FINAL` decision reports the vote-cast date.
- A test that an open election with an uncast vote is unchanged.

**Out of scope**

Any change to how votes are cast or elections are closed.

---

### Ticket 3: DAR volume and composition reporting

**Issue type:** Story

**Suggested size:** 5 points

**Dependencies:** None. Creates `DarAnalyticsResource` if tickets 4 to 6 have not

**Summary**

Expose submission volume and request composition: DARs per researcher and per institution, datasets
per DAR, researchers per DAR, and the number of distinct researchers submitting.

**Description**

Covers requested metrics 4, 5, 6, 7 and 12. All are aggregations over existing columns; no schema
change is involved.

This is the first of four endpoints on a new admin-scoped analytics resource. It establishes the
resource, its service and its DAO, which the other three extend.

**Implementation notes**

- New `resources/DarAnalyticsResource.java` annotated `@RolesAllowed(ADMIN)`, plus
  `service/DarAnalyticsService.java`, `db/DarAnalyticsDAO.java` and a mapper.
- Reads `data_access_request`, `dar_dataset`, `users`, `institution`, and the DAR `data` JSON for
  collaborators.
- Java records for the response DTOs.
- Prefer CTEs for the multi-table rollups; paginate rather than materialising unbounded results.
- Return one row per DAR, with a timestamp, so the client buckets without reshaping. This endpoint
  counts requests, so the DAR is the unit; tickets 4 and 5 return pair-level rows instead because a
  decision is made per dataset.
- Institution comes from `users.institution_id` and is the submitter's current institution. Record
  that in the OpenAPI path spec.
- Collaborator counts are per DAR. Do not expose a distinct-person count.

**Acceptance criteria**

- Each of metrics 4, 5, 6, 7 and 12 is retrievable by an admin.
- A non-admin receives 403.
- Every row carries a timestamp permitting day, week and month bucketing.
- A DAR with no collaborators reports zero rather than being omitted.
- A researcher with no institution is reported rather than dropped from the institution rollup.
- The path spec states that institution reflects the submitter's current employer.
- Results are paginated.

**Tests**

- DAO tests for each aggregation, including a DAR with multiple datasets and one with none.
- A test for a researcher with a null institution.
- A resource test for the admin and non-admin cases.
- A test that collaborator counts include PI, lab staff and internal collaborators separately.

**Out of scope**

Distinct-person collaborator counting. Historical institution attribution — see ticket 9.

---

### Ticket 4: DAC decision reporting

**Issue type:** Story

**Suggested size:** 5 points

**Dependencies:** None. Blocking Decisions 3 and 4 shape only the deferred DAR-level rollup

**Summary**

Expose, per DAR-dataset pair, whether a DAC decision exists, whether it approved or denied, and
whether RADAR or a chairperson made it.

**Description**

Covers requested metrics 1, 2 and 3. The decision is the most recent cast `FINAL` or `RADAR_APPROVE`
vote for the pair, since reopening archives the previous election and opens a new one.

Rows are returned per DAR-dataset pair rather than per DAR. That is what keeps the ticket independent
of Blocking Decisions 3 and 4: the DAR-level rollup becomes a later reduction over these rows rather
than a rewrite.

**Implementation notes**

- Copy the `ranked_final_votes`/`latest_final_votes` CTE pair from
  `SigningOfficialDashboardDAO.java:48-64`, partitioning by `(reference_id, dataset_id)`.
- Retain its `v.vote IS NOT NULL` filter. Without it an uncast vote on a reopened election wins the
  ranking and reports a decision that was never made.
- Retain its `vote_id` tie-break so ordering is deterministic.
- Exclude canceled DARs via `data->>'status'`, or they are counted as pending indefinitely.
- Read `vote.vote` for the outcome and `vote.type` for the source. Do not read
  `election.final_access_vote`, which no production code writes.
- Until Blocking Decision 2 is answered, report reopened pairs under both readings rather than
  choosing one silently.

**Acceptance criteria**

- A pair whose election history is denied, approved, then denied reports denied.
- A multi-dataset DAR with a RADAR decision on one dataset and a manual decision on another reports
  both, attributed correctly.
- A canceled DAR appears in neither the decided nor the pending count.
- A pair with an open election and no cast vote reports undecided, with a null decision timestamp
  rather than being omitted.
- A reopened pair is reported under both readings while Blocking Decision 2 is unanswered, and under
  the chosen reading once it is.
- Every row carries the DAR's `submission_date` as its cohort timestamp, so undecided pairs can still
  be bucketed by day, week or month, plus a nullable decision timestamp where a decision exists.
- A non-admin receives 403.

**Tests**

- A DAO test per election-history shape: single decision, reopened once, reopened with no vote yet.
- A test for a multi-dataset DAR mixing RADAR and manual decisions.
- A test that a canceled DAR is excluded.
- A test that an uncast vote does not win the ranking.

**Out of scope**

DAR-level rollups, pending Blocking Decisions 3 and 4. Turnaround statistics — see ticket 5.

---

### Ticket 5: DAC decision turnaround reporting

**Issue type:** Story

**Suggested size:** 3 points

**Dependencies:** None. Shares the deciding-vote selector with ticket 4 — see Implementation notes

**Summary**

Report elapsed time from submission to DAC decision, with mean, median and mode.

**Description**

Covers requested metric 8, measuring `data_access_request.submission_date` to `vote.update_date` on
the deciding vote. `update_date` is written when a vote is cast and is not touched by the reminder-flag
or rationale-edit paths, so it is a durable decision timestamp across full history.

**Implementation notes**

- The deciding-vote selection is shared with ticket 4. Whichever ticket is implemented first adds it
  to `db/DarAnalyticsDAO.java` as a named, reusable fragment; the second uses it rather than writing a
  parallel version. Neither ticket has to wait for the other, in the same way both share the resource
  scaffolding.
- Exclude deciding votes with a null `update_date` and report the excluded count alongside the
  statistics. Do not fall back to `create_date`: the election opened before the vote was cast, so
  substituting it reports a **shorter** turnaround than actually occurred, understating how long DAC
  review takes. Ticket 1 quantifies how many rows this affects.
- Surface mean, median and mode together; a single average hides the shape of this distribution.
- Return the per-row elapsed values as well as the summary, so the client can chart a distribution.

**Acceptance criteria**

- Turnaround is reported for decided DAR-dataset pairs with full history.
- Pairs with a null deciding-vote `update_date` are excluded and counted, not silently dated from
  `create_date`.
- Mean, median and mode are each returned.
- Undecided pairs are excluded from the statistics.
- Rows are bucketable by day, week and month.
- A non-admin receives 403.

**Tests**

- A test asserting a known turnaround for a manual decision and for a RADAR decision.
- A test that a null `update_date` row is excluded and counted.
- A test of mean, median and mode against a fixed, synthetic distribution.
- A test that an undecided pair contributes nothing.

**Out of scope**

SO approval turnaround — see ticket 6.

---

### Ticket 6: SO approval and expiration reporting

**Issue type:** Story

**Suggested size:** 5 points

**Dependencies:** None

**Summary**

Report submission-to-SO-approval turnaround, separate requests that skipped SO review through
pre-authorization, and count expired DARs.

**Description**

Covers requested metrics 9 and 10. SO approval is timestamped by `approving_so_timestamp`. Expiration
is computed as `submission_date + 365 days`, matching `EXPIRATION_DURATION_MILLIS`.

The pre-authorization skip needs care. `requires_so_approval` is nullable and written only when true,
so NULL means a skip, a row predating the column, or a progress report or closeout. All three must be
separated.

**Implementation notes**

- Identify the skip as `requires_so_approval IS NOT TRUE AND parent_id IS NULL`, bounded to DARs
  submitted after January 2026.
- Never write `requires_so_approval = false`. It matches nothing and fails silently.
- Report progress reports and closeouts separately, against their own approval flow, rather than
  folding them into submission figures.
- Compute expiry in SQL from `submission_date`; do not rely on the hydrated model.
- State the January 2026 lower bound in the OpenAPI path spec.

**Acceptance criteria**

- Turnaround is reported for DARs with a non-null `approving_so_timestamp`.
- Per-request elapsed values are returned alongside any summary statistics, so a distribution can be
  charted rather than only an average.
- Every row carries a timestamp permitting day, week and month bucketing, including the expiration
  series.
- DARs that skipped SO review are distinguishable from those still awaiting it.
- A closeout that went to an SO is not counted as a pre-authorization skip, and progress reports and
  closeouts are reported as their own figures rather than folded into submissions.
- DARs submitted before January 2026 are excluded from the skip figure rather than counted as skips.
- Expiry matches `EXPIRATION_DURATION_MILLIS` on both sides of the boundary.
- A non-admin receives 403.

**Tests**

- A test per `requires_so_approval` state: true and approved, true and pending, NULL on an original
  submission, NULL on a closeout, NULL on a pre-2026 row.
- A test asserting that `= false` returns nothing, guarding the predicate against regression.
- Expiry tests either side of 365 days.

**Out of scope**

DAC decision turnaround — see ticket 5.

---

### Ticket 7: Admin DAR analytics dashboard

**Issue type:** Story

**Suggested size:** 8 points, split by section if it grows past one reviewable change

**Dependencies:** Tickets 3 to 6 deployed, and Blocking Decision 2 answered. Repository: `duos-ui`

**Summary**

Add an internal admin analytics page presenting the decision funnel, turnaround distributions, volume
rollups and expiration over time.

**Description**

Consumes the four endpoints from tickets 3 to 6. Each section maps to one endpoint, so the page splits
along those seams if it needs to.

**Implementation notes**

- Build on `src/components/dashboard/{ConsoleDashboard,ConsoleDashboardGrid}.tsx` rather than a new
  layout system, and follow the `dataviz` skill's chart conventions.
- Sections: decision funnel with RADAR-versus-manual split; SO turnaround distribution with mean,
  median and mode shown beside it; volume rollups; expiration over time.
- Every time series filterable and bucketable by day, week or month.
- Surface the accuracy caveats in the UI where they apply — current-institution attribution on the
  institution chart, the January 2026 lower bound on SO turnaround, and any exclusion count returned
  by ticket 5.

**Acceptance criteria**

- Blocking Decision 2 is settled and the funnel presents one reading of a reopened decision, not two.
- An admin can reach the page; no other role can.
- Each section renders from its endpoint and shows an empty state rather than an error when a metric
  has no data.
- Turnaround sections display mean, median and mode.
- Date bucketing by day, week and month works on every time series.
- Caveats are visible next to the figures they qualify, not only in documentation.

**Tests**

- Component tests per section against fixture responses.
- A test for the empty state of each section.
- A test that a non-admin cannot reach the page.

**Out of scope**

Bulk export. Any metric not delivered by tickets 3 to 6.

---

### Ticket 8: Capture DAR renewal

**Issue type:** Conditional Story

**Suggested size:** 5 points, to be re-estimated once Blocking Decision 1 is answered

**Dependencies:** Blocking Decision 1. Cannot start without it

**Summary**

Record when a DAR renews an earlier one, under whichever definition product settles on.

**Description**

Covers requested metric 11. DUOS has no renewal concept today, and the progress-report chain cannot be
reused: `parent_id` is UNIQUE, so a DAR has at most one child, and it already means continuing review
of an active request rather than resubmission after expiry.

This is the only capture work in the plan, and the only metric that will be forward-only.

**Implementation notes**

- Add a distinct `renewed_from_dar_id` rather than overloading `parent_id`.
- Write it on whichever submission path product's definition identifies.
- If renewal resets the expiration clock, the derived-expiration decision in ticket 6 must be revisited,
  because expiry would no longer be a pure function of `submission_date`.

**Acceptance criteria**

- A renewed DAR records the DAR it renews.
- A DAR that is both progress-reported and renewed records both, independently.
- Renewal counts are reportable over time.
- Existing progress-report behavior is unchanged.
- Whether renewal affects expiration is decided and implemented.

**Tests**

- A test that a renewal links to its predecessor.
- A test that renewal and a progress report coexist on one DAR.
- A regression test that progress-report creation is unaffected.

**Out of scope**

Backfilling renewals from historical data, which is not inferable.

---

### Ticket 9: Snapshot institution at submission

**Issue type:** Story

**Suggested size:** 3 points

**Dependencies:** Capture half none; reporting half requires ticket 3

**Summary**

Record the submitter's institution on the DAR so historical attribution stops moving when a researcher
changes employer.

**Description**

Institution is currently read live from `users.institution_id`, so a researcher who moves takes every
past DAR with them and year-over-year institution reporting is unreliable.

Persisting it at submission fixes the capture side; the reporting query must then prefer the snapshot
and fall back to the live value for rows predating it. Both halves are needed — capturing alone leaves
ticket 3 still reporting current institution.

**Implementation notes**

- Add the column in a Liquibase changeset with a `changelog-master.xml` include.
- Write it inside the existing creation transaction in `DataAccessRequestService`, beside
  `captureDatasetDaaSnapshots`.
- Update `db/mapper/DataAccessRequestMapper.java`; DAR hydration is explicitly mapped, not automatic.
- Update `db/DarAnalyticsDAO.java` to prefer the snapshot and fall back to the live institution.
- Distinguish the two eras in the response rather than blending them, so a consumer can tell which
  rows are historically accurate.

**Acceptance criteria**

- A DAR submitted after this change records the submitter's institution at submission.
- The institution rollup uses the snapshot where present and the live value otherwise.
- The response distinguishes snapshotted rows from fallback rows.
- A researcher changing institution does not move their post-change DARs.
- Existing submission behavior is otherwise unchanged.

**Tests**

- A test that submission records the institution.
- A test that changing a user's institution does not alter an existing DAR's reported institution.
- A test that a pre-change DAR falls back to the live value and is marked as such.

**Out of scope**

Backfilling historical institutions, which is not recoverable.

---

### Ticket summary

| Ticket | Metrics | Size | Dependencies |
| --- | --- | --- | --- |
| 1. Measure reporting data coverage | — | 2 | none |
| 2. Report RADAR decisions from election queries | — | 3 | none |
| 3. Volume and composition reporting | 4, 5, 6, 7, 12 | 5 | none |
| 4. DAC decision reporting | 1, 2, 3 | 5 | none |
| 5. DAC decision turnaround reporting | 8 | 3 | none; shares a selector with ticket 4 |
| 6. SO approval and expiration reporting | 9, 10 | 5 | none |
| 7. Admin analytics dashboard (`duos-ui`) | all | 8 | tickets 3–6 deployed; Blocking Decision 2 |
| 8. Capture DAR renewal | 11 | 5 | Blocking Decision 1 |
| 9. Snapshot institution at submission | 5 (accuracy) | 3 | ticket 3 for the reporting half |

## Test Matrix

Across the reporting tickets, cover at minimum:

- an election history of denied, approved, then denied on one DAR-dataset pair;
- a reopened pair whose live election carries no cast vote;
- a multi-dataset DAR mixing a RADAR decision with a manual one;
- a RADAR-decided election, which ticket 2 makes visible to both election queries;
- a canceled DAR, counted as neither decided nor pending;
- each `requires_so_approval` state: true and approved, true and pending, NULL on an original
  submission, NULL on a closeout, NULL on a pre-2026 row;
- an assertion that `requires_so_approval = false` matches nothing;
- a deciding vote with a null `update_date`, excluded and counted rather than dated from `create_date`;
- expiry either side of the 365-day boundary;
- a DAR with no decision, a partial decision, and all datasets decided;
- admin and non-admin access to every endpoint.

Synthetic data only. No Mockito `lenient()` stubbing.

## Explicitly Out of Scope

- Any change to DAC voting, RADAR rule or SO-approval workflow semantics.
- Capturing lifecycle facts Consent already persists.
- Denormalized decision rollup columns on `data_access_request`.
- Bulk CSV or warehouse export. The dashboard is the delivery target; export is a fast-follow if it
  proves insufficient.
- A general-purpose audit framework across DUOS.
- Backfilling renewal or historical institution, neither of which is recoverable.

## Definition of Done

- Admin-scoped endpoints return flat, timestamped rows requiring no client-side reshaping before
  descriptive statistics.
- Decision outcome, decision source and decision turnaround are reportable across full history, with
  any excluded range reported rather than silently dropped.
- SO approval turnaround is reportable, with pre-authorization skips identified by a predicate that
  survives the nullable column and excludes progress reports.
- RADAR-decided elections are visible to both election queries and carry a decision date.
- `duos-ui` presents the funnel, turnaround, volume and expiration views, bucketable by day, week or
  month, with the accuracy caveats shown beside the figures they qualify.
- The four blocking decisions have been answered, and renewal is implemented or explicitly declined.
- Apart from ticket 2, no existing DAR, voting, SO-approval or cancellation behavior has changed.
