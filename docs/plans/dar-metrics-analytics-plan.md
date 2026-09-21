# DAR Metrics and Analytics Plan

## Status

Proposed. No code written. This plan is the deliverable of DT-3999 ("Investigate: DUOS Admin
Metrics & Dashboard V1"); the implementation tickets it proposes are not yet filed.

Five open questions block parts of the design. Three of them (2, 3, 4 below) are deliberately
engineered around rather than waited on — see [Recommended Decisions](#recommended-decisions).

## Objective

Capture Data Access Request (DAR) lifecycle facts — DAC decision outcomes, RADAR versus manual
decisions, Signing Official (SO) approval turnaround, submission volume, dataset counts, and
expiration and renewal — as timestamped, append-only records, and expose them as flat rows that
drop straight into descriptive-statistics tooling without reshaping. Surface them in an internal
admin analytics dashboard in `duos-ui`.

The constraint that shapes everything below: DUOS derives most DAR state at read time. That is
correct for driving current UI state and wrong for longitudinal reporting, because a derived value
answers "what is true now", not "what was true then".

## Feasibility of the Requested Metrics

The ticket reads as though most of these metrics need new plumbing. They do not. Ten of the thirteen
can be answered from data Consent already stores; what is missing is a reporting query, not a
capture subsystem. The table below is the short answer to "what is possible".

Verdicts are based on reading the code, not on querying production. Whether enough rows exist to
make a given trend chart worth drawing is a separate question — see
[Before Promising Charts](#before-promising-charts).

| # | Requested metric | Verdict | Why |
| --- | --- | --- | --- |
| 1 | DARs with a DAC decision vs. not | Available now | Latest election per DAR-dataset pair; a closed election is a decided one |
| 2 | Decision results: approved vs. denied | Available now per dataset; DAR-level rollup needs a product decision | `vote.vote` on the `FINAL`/`RADAR_APPROVE` vote already carries it, and `DataAccessRequestDAO`'s `LAST_VALUE` queries already read it. Nothing needs persisting — see Open Question 3 |
| 3 | DARs decided via RADAR rules | Available now | `VoteType.RADAR_APPROVE`, in place since July 2025 |
| 4 | DARs submitted per researcher | Available now | `user_id` + `submission_date` |
| 5 | DARs submitted per institution | Available now — for *current* institution only | Caveat A |
| 6 | Datasets requested per DAR | Available now | `dar_dataset` |
| 7 | Researchers per DAR (PI, lab staff, internal collaborators) | Available now as counts | Caveat B |
| 8 | Turnaround: submission → DAC decision | **Going forward only** | No trustworthy decision timestamp exists. See Caveat C |
| 9 | Turnaround: submission → SO approval, delineating pre-auth skip | Available now | `approving_so_timestamp` is a real event timestamp; `requires_so_approval = false` identifies the pre-auth skip. History runs from January 2026 |
| 10 | DARs expired | Available now, full history | Deterministic from `submission_date`; no capture needed |
| 11 | DARs renewed | **Not possible today** | No renewal concept exists in DUOS. Needs a product definition first, then new capture, then forward-only data. See Open Question 1 |
| 12 | Number of researchers submitting DARs | Available now | Distinct `user_id` where `submission_date` is not null |
| 13 | Timestamp everything, longitudinal | Partially | Submission, SO approval and expiry have accurate history; decisions do not, per metric 8 |

### Caveat A — institution attribution is current, not historical

Institution is read live from `users.institution_id`. The DAR's own `institution` property is in
`DataAccessRequestData.DEPRECATED_PROPS` and is stripped, so nothing records the researcher's
institution *at the time they submitted*. A researcher who changes institution silently moves all of
their past DARs to the new one.

"DARs per institution" is therefore answerable as "DARs by where the submitter works now", which is
fine for a current-workload view and wrong for a year-over-year trend. Fixing it means snapshotting
the institution at submission — cheap to add, forward-only once added.

### Caveat B — collaborator counts are self-reported free text

`Collaborator` is a record of name, email, title, era commons id and country, stored in the DAR's
JSON. Collaborators are not linked to DUOS user accounts. Counting them per DAR is reliable;
deduplicating a person across DARs, or verifying they exist, is not. Metrics of the form "how many
distinct researchers appear as collaborators" should not be promised.

### Caveat C — why decision turnaround is the one genuinely hard metric

This is the metric worth spending the PO's attention on, because it is the one the ticket most
underestimates. DUOS does not durably record when a DAC decision was made:

- there is no `final_vote_date` column; it is derived in SQL, two different ways;
- both derivations read the vote's `create_date`, which is when the election *opened*, not when the
  chairperson voted — and `updateVotesWithValue` writes only `update_date`;
- both derivations join on vote type `final`, so RADAR-approved elections return null.

So historical decision turnaround cannot be computed even approximately: for manual votes the
available timestamp measures the wrong thing, and for RADAR votes there is none. This is the reason
the plan adds an event table at all. Once emission is in place the metric is exact — but it starts
from the deploy date, with no backfill possible.

### Before promising charts

Everything above is a statement about what the schema *can* answer. Before committing to specific
dashboard visuals, run counts against production for: submitted DARs per month over the last two
years; how many carry a closed data-access election; how many have a `RADAR_APPROVE` vote; and how
many have a non-null `approving_so_timestamp`. A year-over-year chart needs enough rows per bucket to
be meaningful, and metrics 3 and 9 have only fourteen and eight months of history respectively.

## Background and Scope

In scope: a DAR-scoped event table in Consent, emission at existing transition points, a reporting
endpoint, and an admin dashboard page in `duos-ui`.

Out of scope, and stated up front because the ticket's plan drifts toward them: any change to DAC
voting, RADAR rule, or SO-approval workflow semantics. This is a read-only observation layer over
existing decision logic. See [Explicitly Out of Scope](#explicitly-out-of-scope).

## Current Behavior

Each item below was verified against the code on the `develop` branch at the time of writing.

### Decision outcomes are not persisted at the DAR level

`DacDashboardSummary.DarRequests` says so directly: "There is deliberately no denied count because
DUOS has no denied collection status"
(`src/main/java/org/broadinstitute/consent/http/models/DacDashboardSummary.java:8-13`). Denial exists
only as a `false` `vote` row whose election is then closed.

Note that `election.final_access_vote` is *not* the source, contrary to the ticket. The only
`updateElectionById` overload that writes that column (`ElectionDAO.java:220-226`) has no production
callers — only tests. `updateVotesWithValue` sets `election.status` and nothing else
(`service/dao/VoteServiceDAO.java:45-46`, `:63-66`). In current data the approve/deny signal lives
solely in `vote.vote` on the `FINAL` or `RADAR_APPROVE` vote, which is what
`DataAccessRequestDAO`'s `LAST_VALUE` queries already read. Any report built on `final_access_vote`
would read a column that is populated for legacy rows only.

### A DAR-dataset pair can have many elections, and only the last one counts

`DataAccessRequestDAO` documents the rule at `:56-64` and `:190-201`: election 1 may be denied,
election 2 approved, election 3 denied again, and `LAST_VALUE` over the partition selects the most
recent. Any decision record must therefore be keyed by `election_id`, not by `(dar, dataset)`, and
"the current decision" is a query-time reduction over that history — not a column to overwrite.

### RADAR and manual decisions already converge on one chokepoint

`DACAutomationRuleService.openElectionAndApprove` creates an election and a single
`VoteType.RADAR_APPROVE` vote, then calls the same `voteServiceDAO.updateVotesWithValue` a manual
chair vote uses (`DACAutomationRuleService.java:299-329`). That method closes the election for both
types in one branch (`service/dao/VoteServiceDAO.java:61-67`), inside an existing transaction.

This is better than the ticket's two-hook proposal: one emission point inside that transaction
covers both paths, and `vote.getType()` distinguishes them.

### `Election.finalVoteDate` is derived, not stored — and is null for RADAR

There is no `final_vote_date` column. `ElectionDAO` computes it two different ways: as
`v.create_date` at `:62`, and as `COALESCE(v.update_date, v.create_date)` at `:134-138`. Both join
the vote on `LOWER(v.type) = 'final'` for `dataaccess` elections (`:66-68`, `:145-148`), so a
`RADAR_APPROVE` vote does not match and a RADAR-decided election yields a null final vote and null
date from `findLastElectionsByReferenceIds`.

Two consequences. First, the ticket's claim that a per-election decision timestamp "is stored" is
wrong; nothing durable records when a decision was made. Second, `v.create_date` is when the vote
row was created — when the election opened — not when the chair voted, and `updateVotesWithValue`
writes only `update_date`. The nearest thing DUOS has to a decision timestamp today is therefore
either wrong or absent depending on which query you ask.

This is the strongest argument in the plan for an event table: the fact is not merely unaggregated,
it is unrecorded.

### SO approval is fully modeled, including the pre-authorization skip

`updateDarApprovalSO` sets `approving_so_id` and `approving_so_timestamp = now()`
(`DataAccessRequestDAO.java:521-524`) — a real, durable event timestamp. `requires_so_approval` is
persisted at submission inside the creation transaction
(`DataAccessRequestService.java:248`, `:298-300`).

The flag is an OR of two causes (`DataAccessRequestService.java:898-905`): a dataset carries the
`REQUIRE_SO_DAR_APPROVAL` automation rule, *or* the user is not pre-authorized for all required DAAs.
Because it is an OR, `requires_so_approval = false` implies both that no dataset required a rule and
that the user was pre-authorized — so the stored boolean *does* identify the pre-authorization skip
metric 9 asks for. Only the converse is ambiguous: when the flag is true, the persisted data does not
say which of the two causes applied. No requested metric asks for that, so it needs no new column.

Note also that the method is `requiresSOApproval`; the ticket's `flagIfSOApprovalIsNeeded` does not
exist.

### Expiration is deterministic, so it does not need an event

`expiresAt` is `submissionDate + EXPIRATION_DURATION_MILLIS` (365 days), recomputed on hydration
(`models/DataAccessRequest.java:31`, `:154-163`).

The ticket argues this "loses the point-in-time fact" and that monthly counts "silently change as
time passes". That reasoning does not hold: expiration is a pure function of a persisted
`submission_date` and a compile-time constant, so the date a DAR expired is computable retroactively
and is stable. What changes over time is which DARs are *currently* expired, which is the correct
behavior for a "current state" field and irrelevant to a historical chart.

An `EXPIRED` event would add cost and no information. It becomes necessary only if expiration stops
being deterministic — for example if renewal resets the clock, which is Open Question 1.

For completeness, the hook the ticket proposed is also not what it describes.
`sendExpirationNotices` is not a scheduled job; it runs when an external caller hits
`EmailNotifierResource` (`:50`, `:64-70`). Its query is idempotent only because it left-joins
`email_entity` and excludes DARs already emailed (`DataAccessRequestDAO.java:256-259`), and it
excludes everything submitted before `MINIMUM_SUBMITTED_DATE_FOR_DAR_EXPIRATIONS`
(`DataAccessRequestService.java:78`). Emitting from there would produce events timestamped when the
mailer ran, not when the DAR expired, and would silently omit older DARs.

### `parent_id` cannot carry renewals

`parent_id` has a unique constraint — `uk_parent_id`, added by
`changelog-consent-2025-05-16-disallow-pr-siblings.xml:5-6` — so a DAR has at most one child. It
already means "continuing review": `createProgressReport` requires the datasets to be approved on the
parent (`DataAccessRequestService.java:318-331`).

Overloading it for renewal is therefore not just semantically muddy, it is structurally impossible
for a DAR that is both progress-reported and renewed, or renewed twice. Renewal needs its own
column.

### Cancellation is a JSON status, not a column

`DarStatus.CANCELED` is written into `data->>'status'` and read back by string comparison
(`resources/DarCollectionResource.java:301-305`). There is no cancellation timestamp.

### Existing precedent to follow

`dar_dataset_daa_snapshot` (`changelog-consent-2026-04-28-dar-dataset-daa-snapshot.xml`) is a
point-in-time capture table written inside the DAR creation transaction by
`captureDatasetDaaSnapshots` (`DataAccessRequestService.java:296-297`, `:906`). It is the closest
structural precedent for what this plan adds and should be mirrored: not-null FKs, a `captured_at`
timestamp, a uniqueness constraint that makes re-emission a no-op, and an index per FK.
`LibraryCardDaaAudit` and `rules/DACAutomationRuleAudit` are the append-only audit-record precedent.

### The existing metrics surface is narrower than the ticket states

`MetricsResource` exposes exactly one endpoint, `GET /api/metrics/dar-summaries/{datasetId}`, and it
is annotated `@PermitAll`. The `/study/{studyId}` endpoint the ticket refers to does not exist.

`@PermitAll` matters: an admin analytics surface must not inherit it. Extending this resource means
mixing authorization levels on one path prefix, so the new endpoints belong on a separate
admin-scoped resource even though they reuse the service and mapper patterns.

## Findings That Change the Ticket's Plan

| # | Ticket's position | Finding | Effect |
| --- | --- | --- | --- |
| 1 | Emit `DAC_DECISION` from two hooks (`VoteService` and `DACAutomationRuleService`) | Both already funnel through `VoteServiceDAO.updateVotesWithValue`, which closes the election in one branch | One hook, inside an existing transaction |
| 2 | Per-election `finalVoteDate` is stored | It is derived, computed inconsistently across two queries, and null for RADAR | Strengthens the case for the event table; also a candidate defect to file separately |
| 2b | `Election.finalAccessVote` holds the outcome | No production code writes that column; `vote.vote` is the live signal | Read votes, not `final_access_vote` |
| 3 | RADAR events have no human actor | The vote is inserted with `rule.enabledByUserId()` | `actor_user_id` is non-null for RADAR; add `decided_via` rather than inferring from a null actor |
| 4 | Persist an `EXPIRED` event | Expiration is a deterministic function of `submission_date` | Drop the event; compute expiration in the query |
| 5 | `requiresSOApproval` only needs exposing | Correct — and because it is an OR, `false` already identifies the pre-auth skip | No new column; expose it |
| 6 | Renewal may reuse `parentId` | `parent_id` is unique per DAR | Renewal needs a distinct column |
| 7 | Add rollup columns to `data_access_request` (plan step 2) | Rollup rules are the unresolved product questions, and a DAR-dataset pair has an election *history* | Do not denormalize; reduce at query time |
| 8 | Extend `MetricsResource` | It is `@PermitAll` | New admin-scoped resource, shared patterns |

## Recommended Decisions

1. **Record decisions per election, not per DAR.** Open Questions 2 and 3 (first-versus-last
   decision date; any-versus-all denial) then stop blocking the schema: both become query-time
   reductions that can be changed without a migration once product answers. This is the single
   decision that unblocks implementation.
2. **Do not denormalize rollups onto `data_access_request`.** Beyond the above, the election history
   means a rollup column would need invalidating whenever a later election supersedes an earlier
   one — reconciliation work for no read benefit at DUOS's data volume.
3. **Drop the `EXPIRED` event.** Compute `submission_date + 365 days` in the reporting query.
   Revisit only if renewal resets expiry.
4. **Defer `RENEWED` entirely** until Open Question 1 is answered, but reserve
   `renewed_from_dar_id` rather than planning to overload `parent_id`.
5. **Put the new endpoints on an admin-scoped resource**, reusing the `MetricsService` and mapper
   patterns rather than the `MetricsResource` path prefix.

## Design

### `dar_metrics_event`

Append-only. No update or delete path.

```
dar_metrics_event(
  event_id        bigserial primary key,
  dar_id          bigint      not null references data_access_request(id),
  event_type      text        not null,   -- SUBMITTED | DAC_DECISION | SO_APPROVED | CANCELED
  event_timestamp timestamp   not null,
  actor_user_id   bigint      null references users(user_id),
  election_id     bigint      null references election(election_id),
  dataset_id      bigint      null references dataset(dataset_id),
  decision_result text        null,       -- APPROVED | DENIED
  decided_via     text        null        -- MANUAL | RADAR
)
```

Constraints and indexes, mirroring `dar_dataset_daa_snapshot`:

- unique `(dar_id, event_type, election_id)`, so re-emission on a retried transaction is a no-op
  rather than a duplicate row;
- index on `dar_id`, on `event_timestamp`, and on `(event_type, event_timestamp)` for the
  time-bucketed queries;
- `election_id`, `dataset_id`, `decision_result`, and `decided_via` are set only on `DAC_DECISION`.

No `metadata` JSONB column. The ticket proposed one "for anything not worth its own column"; a typed
column per fact is what makes these rows droppable into statistics tooling, which is the stated
requirement, and an untyped escape hatch is how that guarantee erodes.

`RENEWED` is absent by decision 4. `EXPIRED` is absent by decision 3. `SO_NOT_REQUIRED_PREAUTH` is
absent because it is not an event — it is a property of the submission, handled below.

### Emission points

| Event | Site | Transaction |
| --- | --- | --- |
| `SUBMITTED` | `DataAccessRequestService.createDataAccessRequest` | existing `dataAccessRequestServiceDAO.inTransaction`, beside `captureDatasetDaaSnapshots` |
| `DAC_DECISION` | `VoteServiceDAO.updateVotesWithValue`, in the existing `FINAL || RADAR_APPROVE` branch | existing `jdbi.useTransaction` |
| `SO_APPROVED` | the `updateDarApprovalSO` call site | must be wrapped; the DAO call is currently standalone |
| `CANCELED` | the collection cancellation path behind `DarCollectionResource` | to be confirmed during implementation |

Every emission is inside the transaction of the state change it records. Emission failure rolls the
operation back; it is not logged and swallowed. A metrics row that silently goes missing produces a
chart that is quietly wrong, which is worse than a failed request — and for the three sites that
already have a transaction, this costs nothing.

## Implementation Approach

Three PRs, drained bottom-up. This plan document ships in PR 1 rather than as its own PR.

## Jira-Ready Tickets

### Ticket 1: Capture DAR lifecycle events

Add the `dar_metrics_event` table via one Liquibase changeset plus the `changelog-master.xml`
include; add the model, DAO, and mapper following `LibraryCardDaaAudit`;
emit at the four sites above.

Done when: every listed transition writes exactly one row inside the transaction of the state change;
re-running a decided election adds no duplicate; `decided_via` is `RADAR` exactly when the vote type
is `RADAR_APPROVE`; existing submission, voting, SO-approval, and cancellation behavior is unchanged
and existing tests in the touched classes pass untouched.

### Ticket 2: DAR analytics reporting endpoints

A new admin-scoped resource returning one flat row per DAR: submission timestamp, decision timestamp
and result and `decided_via` reduced from the election history, SO approval timestamp with
`requires_so_approval`, computed expiry, dataset count, researcher counts
broken out by PI, lab staff, and internal collaborator, institution, and submitter. Plus rollups by
researcher and by institution for metrics 4, 5, and 12.

This is where Open Questions 2 and 3 get encoded, as a documented reduction over
`dar_metrics_event` rows. Prefer CTEs for the multi-table reduction; paginate.

Record endpoint behavior in the OpenAPI path spec under `src/main/resources/assets/paths/`.

### Ticket 3: Admin DAR analytics dashboard (`duos-ui`)

A new page on the existing `ConsoleDashboard`/`ConsoleDashboardGrid` framework: decision funnel,
RADAR versus manual split, SO turnaround distribution with mean, median, and mode surfaced beside
it, volume rollups, and date bucketing by day, week, or month. Separate repository, so a separate
ticket.

### Ticket 4 (conditional): Renewal capture

Blocked on Open Question 1. Adds `renewed_from_dar_id` and a `RENEWED` event, and revisits decision
3 if renewal resets the expiration clock.

### Candidate defect (file separately)

`findLastElectionsByReferenceIds` returns a null final vote and null final vote date for
RADAR-approved elections, because the vote join is restricted to `LOWER(v.type) = 'final'`. Confirm
with a test before filing. This is pre-existing and not caused by this work, but it will distort any
report built on `Election.finalVoteDate`, which is a second reason for this plan not to build on it.

## Test Matrix

At minimum, cover:

- one event per transition for each of the four event types;
- a manual `FINAL` approval and denial, and a `RADAR_APPROVE` approval, asserting `decision_result`
  and `decided_via`;
- an election history on one DAR-dataset pair — denied, approved, denied — asserting three rows and
  the correct query-time reduction;
- a multi-dataset DAR mixing a RADAR decision on one dataset with a manual decision on another;
- re-running `updateVotesWithValue` on an already-decided election, asserting no duplicate row;
- a rolled-back transaction at each emission site, asserting no orphan event row;
- the reporting query against a DAR with no decision, a partial decision, and all datasets decided.

Synthetic data only. No Mockito `lenient()` stubbing.

## Risks

- **Emission inside existing transactions lengthens them.** The three reusing transactions add one
  insert each; the SO-approval site gains a transaction it does not have today. Low, but it touches
  the voting write path, which is the most sensitive code in the service.
- **`SO_APPROVED` wrapping changes error behavior** at a site that currently cannot fail on a
  metrics concern. Review carefully.
- **The `CANCELED` path is the least verified** of the four; cancellation is a JSON status with no
  timestamp today, and the emission site is confirmed only as far as `DarCollectionResource`.
- **No backfill means charts start empty** and stay partial for a year, since a DAR's full lifecycle
  spans the 365-day expiry. See Open Question 5.

## Open Questions

1. **Renewal.** Is it the existing progress-report continuing-review flow, or a researcher
   resubmitting after expiry — a flow that does not exist today? Blocks Ticket 4, and decision 3 if
   renewal resets expiry.
2. **Decision timestamp rollup.** For a multi-dataset DAR, is it decided on the first dataset
   decision or the last? Needed for Ticket 2; does not block Ticket 1.
3. **Denial rollup.** Is a DAR denied if any dataset is denied, or only if all are, with partial
   approval as a third bucket? Needed for Ticket 2; does not block Ticket 1.
4. **Researcher count.** Does "researchers on a DAR" deliberately exclude external collaborators, or
   should those be a separate metric?
5. **Backfill.** Going forward only, or backfill from existing history? Submission dates and SO
   timestamps backfill cleanly. Decisions do not: `v.create_date` is when the election opened, not
   when the vote was cast, so a backfilled decision timestamp would be wrong rather than missing. If
   backfill is wanted, `SUBMITTED` and `SO_APPROVED` are safe and `DAC_DECISION` should be either
   omitted or explicitly flagged as approximate.

## Explicitly Out of Scope

- Any change to DAC voting, RADAR rule, or SO-approval workflow semantics.
- Denormalized decision rollup columns on `data_access_request`.
- Bulk CSV or warehouse export. The dashboard is the agreed delivery target; export is a fast-follow
  if the dashboard proves insufficient.
- A general-purpose, entity-agnostic audit framework across DUOS. This adds one DAR-scoped table
  following existing precedent.
- Fixing the derived `finalVoteDate` behavior, which is filed separately.

## Definition of Done

- Consent persists, per election, whether a DAR was approved or denied, when, and whether by RADAR or
  a chairperson — none of which is durably recorded today.
- SO approval turnaround is reportable, with pre-authorization skips distinguishable from DARs that
  went through SO review.
- An admin-scoped endpoint returns flat rows requiring no client-side reshaping before descriptive
  statistics.
- `duos-ui` surfaces the funnel, turnaround, volume, and expiration views, bucketable by day, week,
  or month.
- Renewal and backfill have an approved disposition, implemented or explicitly declined.
- No existing DAR, voting, SO-approval, or cancellation behavior has changed.
