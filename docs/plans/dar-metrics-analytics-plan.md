# DAR Metrics and Analytics Plan

## Status

Proposed. No code written. This plan is the deliverable of DT-3999 ("Investigate: DUOS Admin
Metrics & Dashboard V1"); the implementation tickets it proposes are not yet filed.

The headline conclusion changed during review: **this work needs no new data capture.** Twelve of the
thirteen requested metrics are answerable from columns Consent already persists, and the thirteenth
(renewal) needs a product decision before any schema. An earlier draft proposed an event table on the
premise that DAC decision timestamps were not recorded; that premise was wrong. See
[Findings That Change the Ticket's Plan](#findings-that-change-the-tickets-plan).

## Objective

Report a defined set of Data Access Request (DAR) metrics — DAC decision outcomes, RADAR versus
manual decisions, Signing Official (SO) approval turnaround, submission volume, dataset counts, and
expiration and renewal — as flat, timestamped rows that drop into descriptive-statistics tooling
without reshaping, and surface them in an internal admin dashboard in `duos-ui`.

## Feasibility of the Requested Metrics

The ticket reads as though most of these metrics need new plumbing. They do not. Twelve of thirteen
are answerable from data Consent already stores; what is missing is a reporting query, not a capture
subsystem.

This distinction is the whole scoping story: **reporting work is fast and covers history
retroactively. Capture work is slow and only ever starts from the deploy date.** Only renewal falls
in the second bucket.

Verdicts are based on reading the code, not on querying production. Whether enough rows exist to make
a given chart worth drawing is separate — see [Before Promising Charts](#before-promising-charts).

| # | Requested metric | Verdict | Source |
| --- | --- | --- | --- |
| 1 | DARs with a DAC decision vs. not | Available now, once "decided" is defined | Latest election per DAR-dataset pair. Reopening changes the answer — Open Question 6 |
| 2 | Decision results: approved vs. denied | Available now per dataset; DAR-level rollup needs a product decision | `vote.vote` on the latest `FINAL`/`RADAR_APPROVE` vote. See Open Question 3 |
| 3 | DARs decided via RADAR rules | Available now | `VoteType.RADAR_APPROVE`, in place since July 2025 |
| 4 | DARs submitted per researcher | Available now | `user_id` + `submission_date` |
| 5 | DARs submitted per institution | Available now — for *current* institution only | Caveat A |
| 6 | Datasets requested per DAR | Available now | `dar_dataset` |
| 7 | Researchers per DAR (PI, lab staff, internal collaborators) | Available now as counts | Caveat B |
| 8 | Turnaround: submission → DAC decision | Available now | `vote.update_date` on the deciding vote. Caveat C |
| 9 | Turnaround: submission → SO approval, delineating pre-auth skip | Available now | `approving_so_timestamp`; skip identified by `requires_so_approval IS NOT TRUE`. Caveat D |
| 10 | DARs expired | Available now, full history | Deterministic from `submission_date`; no capture needed |
| 11 | DARs renewed | **Not possible today** | No renewal concept exists. Needs a product definition, then new capture, then forward-only data. Open Question 1 |
| 12 | Number of researchers submitting DARs | Available now | Distinct `user_id` where `submission_date` is not null |
| 13 | Timestamp everything, longitudinal | Available now | Every row above carries a timestamp; bucketing is a query concern |

### Caveat A — institution attribution is current, not historical

Institution is read live from `users.institution_id`. The DAR's own `institution` property is in
`DataAccessRequestData.DEPRECATED_PROPS` and is stripped, so nothing records the researcher's
institution *at the time they submitted*. A researcher who changes institution silently moves all
their past DARs with them.

"DARs per institution" is answerable as "DARs by where the submitter works now" — fine for a current
workload view, wrong for a year-over-year trend. Fixing it means snapshotting at submission, which is
cheap but forward-only. See ticket 8.

### Caveat B — collaborators are validated but not durably identified

Internal collaborators and lab staff are not unverified free text: at submission each is looked up by
email, must resolve to a DUOS user holding a library card, and must share the submitter's institution
(`DataAccessRequestService.java:617`, `:718`). External collaborators do not go through that check.

What is missing is a persisted identity. `Collaborator` is a record of name, email, title, era commons
id and country stored in the DAR's JSON, with no user-id foreign key. Counting collaborators per DAR
is reliable. Counting *distinct people* across DARs means joining on email after the fact, which is
workable but will drift as people change address, so it needs stating as an approximation rather than
being quietly presented as a headcount.

### Caveat C — the decision timestamp exists, but not where the ticket looked

`vote.update_date` is written when a vote is cast, by `VoteServiceDAO.updateVotesWithValue:41-44`,
for manual and RADAR decisions alike. The two other writes to the `vote` table — the reminder flag
(`VoteDAO.java:78`) and the rationale edit (`VoteDAO.java:113`) — deliberately leave it alone. It is
therefore a durable decision timestamp with full history, not an approximation.

What is unreliable is `Election.finalVoteDate`, which is derived in SQL two different ways: as
`v.create_date` (`ElectionDAO.java:62`), which is when the election opened, and as
`COALESCE(v.update_date, v.create_date)` (`ElectionDAO.java:134-138`), which is correct. Both restrict
the vote join to type `final`, so neither sees a RADAR decision — and they fail differently. The first
uses an INNER JOIN (`:66`), so a RADAR-only election returns **no row at all**; the second uses a LEFT
JOIN (`:145`), so it returns the election with null vote fields.

The conclusion for this plan: read `vote.update_date` directly and do not build on
`Election.finalVoteDate`. `SigningOfficialDashboardDAO.java:48-64` already does exactly this — its
`ranked_final_votes`/`latest_final_votes` CTEs filter `LOWER(v.type) IN ('final', 'radar_approve')`,
require `v.vote IS NOT NULL` so uncast votes cannot win, and rank by
`COALESCE(v.update_date, v.create_date) DESC, v.vote_id DESC`. `ResearcherDashboardDAO.java:46-68`
applies equivalent logic under different names — `final_votes` feeding a `latest_final_votes` that
ranks in an inline subquery. Either is the precedent to copy.

One thing to confirm against production rather than assume: rows old enough to predate the current
vote-casting path may carry a null `update_date`, in which case their decision time falls back to
`create_date` and is wrong. Measure how many before promising full-history turnaround.

### Caveat D — the SO approval flag is null when approval was skipped

`requires_so_approval` is nullable with no default
(`changelog-consent-2026-01-19-add-requires-so-approval-attribute.xml:6`), and
`DataAccessRequestService.java:298-300` writes it **only when true**. A DAR that skipped SO approval
therefore holds NULL, not `false`.

This is a live trap for the reporting query. `WHERE requires_so_approval = false` returns **zero
rows** under Postgres null semantics, so the pre-authorization skip count would silently come out
empty. The correct predicate is `IS NOT TRUE`.

Java code does not see this, because `DataAccessRequestMapper.java:57` coerces the null to primitive
`false` — which is also why the flag looks trustworthy from the model layer and is not.

NULL has two further meanings, so `IS NOT TRUE` on its own over-counts skips:

- **Rows predating the column** (January 2026). Bound the report to DARs submitted after it existed.
- **Progress reports and closeouts**, which split three ways
  (`DataAccessRequestService.java:349-355`). A non-closeout progress report whose submitter is not
  pre-authorized gets `true`; one whose submitter is pre-authorized is left NULL; a **closeout is left
  NULL unconditionally**, even though closeouts do go to an SO and approval writes the same
  `approving_so_timestamp` through `approveDataAccessRequestCloseout` (`:414-417`). So a NULL on a
  child row can mean a genuine skip or a closeout awaiting review, and the two are not separable from
  this column.

The usable predicate is therefore `requires_so_approval IS NOT TRUE` **restricted to original
submissions** — `parent_id IS NULL` — and bounded by date. Progress reports and closeouts need
counting separately, against their own approval flow, rather than folding into the submission
figures.

### Before promising charts

Everything above states what the schema *can* answer. Before committing to specific visuals, run
counts against production for: submitted DARs per month over the last two years; how many carry a
closed data-access election; how many have a `RADAR_APPROVE` vote; how many have a non-null
`approving_so_timestamp`; and how many deciding votes have a null `update_date` (Caveat C). RADAR has
existed only since July 2025 and the SO columns since January 2026, so year-over-year comparisons on
metrics 3 and 9 are not available yet regardless of what is built.

## Background and Scope

In scope: reporting endpoints in Consent over existing data, one defect fix, and an admin dashboard
page in `duos-ui`.

Out of scope, stated up front because the ticket's plan drifts toward it: any change to DAC voting,
RADAR rule, or SO-approval workflow semantics. This is a read-only reporting layer.

## Current Behavior

Each item was verified against the code on `develop` at the time of writing.

### Decision outcomes are not rolled up to the DAR

`DacDashboardSummary.DarRequests` says so directly: "There is deliberately no denied count because
DUOS has no denied collection status" (`models/DacDashboardSummary.java:8-13`). Denial exists as a
`false` `vote` row on a closed election, never aggregated to the DAR.

Note that `election.final_access_vote` is *not* the source, contrary to the ticket. The only
`updateElectionById` overload writing that column (`ElectionDAO.java:220-226`) has no production
callers — only tests. `updateVotesWithValue` sets `election.status` and nothing else
(`service/dao/VoteServiceDAO.java:45-46`, `:63-66`). The live signal is `vote.vote`.

### A DAR-dataset pair can have many elections, and only the last one counts

`DataAccessRequestDAO` documents the rule at `:56-64` and `:190-201`: election 1 may be denied,
election 2 approved, election 3 denied again, and `LAST_VALUE` over the partition selects the most
recent. Reporting must therefore reduce over an election history rather than assume one decision per
pair. Reopening a decision creates a *new* election rather than re-voting an old one
(`service/dao/DarCollectionServiceDAO.java:85`).

### RADAR and manual decisions share one write path

`DACAutomationRuleService.openElectionAndApprove` creates an election and a single
`VoteType.RADAR_APPROVE` vote, then calls the same `voteServiceDAO.updateVotesWithValue` a manual
chair vote uses (`DACAutomationRuleService.java:299-329`). That method closes the election for both
types in one branch (`service/dao/VoteServiceDAO.java:61-67`).

RADAR votes are attributed to `rule.enabledByUserId()` — the admin who enabled the rule — so a null
actor does not distinguish them; the vote type does.

### SO approval is well modeled, with one null trap

`updateDarApprovalSO` sets `approving_so_id` and `approving_so_timestamp = now()`
(`DataAccessRequestDAO.java:521-524`) — a real event timestamp. `requires_so_approval` is computed by
`requiresSOApproval` (`DataAccessRequestService.java:898-905`) as an OR of two causes: a dataset
carries the `REQUIRE_SO_DAR_APPROVAL` automation rule, or the user is not pre-authorized for all
required DAAs.

The OR means a *computed* `false` does identify the pre-authorization skip. The *stored* column does
not, because it is only written when true — see Caveat D. Note also that the method is
`requiresSOApproval`; the ticket's `flagIfSOApprovalIsNeeded` does not exist.

### Expiration is deterministic

`expiresAt` is `submissionDate + EXPIRATION_DURATION_MILLIS` (365 days), recomputed on hydration
(`models/DataAccessRequest.java:31`, `:154-163`).

The ticket argues this "loses the point-in-time fact" and that monthly counts "silently change as
time passes". That does not hold: expiration is a pure function of a persisted `submission_date` and
a compile-time constant, so the date a DAR expired is computable retroactively and is stable. What
changes is which DARs are *currently* expired, which is correct for a current-state field and
irrelevant to a historical chart. No capture is needed.

### `parent_id` cannot carry renewals

`parent_id` is UNIQUE (`uk_parent_id`, from
`changelog-consent-2025-05-16-disallow-pr-siblings.xml:5-6`), so a DAR has at most one child, and it
already means continuing review: `createProgressReport` requires the datasets to be approved on the
parent (`DataAccessRequestService.java:318-331`). Renewal needs its own column.

### Cancellation is a JSON status, not a column

`DarStatus.CANCELED` is written into `data->>'status'` and read back by string comparison
(`resources/DarCollectionResource.java:301-305`). There is no cancellation timestamp.

Cancellation is not a requested metric, but it matters for reporting: a canceled DAR is neither
decided nor awaiting a decision, so the decision funnel must exclude it explicitly or count it as
pending forever.

### The existing metrics surface is narrower than the ticket states

`MetricsResource` exposes one endpoint, `GET /api/metrics/dar-summaries/{datasetId}`, annotated
`@PermitAll`. The `/study/{studyId}` endpoint the ticket refers to does not exist.

`@PermitAll` matters: an admin analytics surface must not inherit it, so the new endpoints belong on
a separate admin-scoped resource even though they reuse the service and mapper patterns.

## Findings That Change the Ticket's Plan

| # | Ticket's position | Finding | Effect |
| --- | --- | --- | --- |
| 1 | Decision timestamps are not recorded, so an event table is required | `vote.update_date` is written when a vote is cast, for manual and RADAR alike, and nothing else overwrites it | **No capture layer at all.** This removes the event table, its emission hook, and the forward-only constraint |
| 2 | `Election.finalVoteDate` is stored | It is derived two ways, one of them wrong, and null for RADAR | Read `vote.update_date`; the null-for-RADAR half is ticket 1 |
| 3 | `Election.finalAccessVote` holds the outcome | No production code writes that column | Read `vote.vote` |
| 4 | Persist an `EXPIRED` event | Expiration is deterministic from `submission_date` | Compute it |
| 5 | `requiresSOApproval` only needs exposing | True of the computed value; the stored column is NULL when approval was skipped | Query with `IS NOT TRUE`, not `= false` |
| 6 | Renewal may reuse `parentId` | `parent_id` is UNIQUE per DAR | Renewal needs a distinct column |
| 7 | Add rollup columns to `data_access_request` | Rollup rules are unresolved product questions, and a pair has an election *history* | Do not denormalize; reduce at query time |
| 8 | Extend `MetricsResource` | It is `@PermitAll` | New admin-scoped resource |

## Recommended Decisions

1. **Capture nothing.** Every requested fact except renewal is already persisted or derivable from
   what is. Re-recording them would duplicate facts that can then disagree with their source, and
   would make metrics forward-only that are currently answerable across full history.
2. **Read `vote.update_date` and `vote.vote` directly**, not `Election.finalVoteDate` or
   `election.final_access_vote`. Follow the `ranked_final_votes`/`latest_final_votes` CTEs in
   `SigningOfficialDashboardDAO.java:48-64`, which already handle RADAR, uncast votes and recency
   correctly.
3. **Reduce over the election history at query time**, so Open Questions 2 and 3 (first-versus-last
   decision date; any-versus-all denial) do not block implementation and can change without a
   migration.
4. **Defer renewal** until Open Question 1 is answered, and reserve `renewed_from_dar_id` rather than
   overloading `parent_id`.
5. **Put the new endpoints on an admin-scoped resource**, reusing `MetricsService` and mapper
   patterns rather than the `MetricsResource` path prefix.
6. **Split the work by metric family, not by layer.** With no shared schema, the reporting tickets
   can be built in parallel — see [Implementation Approach](#implementation-approach).

## Design

There is no new table and no new column in the core of this plan. The design work is in the queries.

### Selecting the current decision

For each DAR-dataset pair, rank that pair's `FINAL` and `RADAR_APPROVE` votes by
`COALESCE(update_date, create_date)` descending, tie-broken by `vote_id`, and take the first. That
row yields all three decision facts at once:

- `vote.vote` — approved or denied;
- `vote.type` — RADAR or manual;
- `vote.update_date` — when it was decided.

`SigningOfficialDashboardDAO.java:48-64` is this query already, written for the SO console, and
`ResearcherDashboardDAO.java:46-68` applies equivalent logic under different CTE names. Reuse that
shape rather than inventing another — note in particular its `v.vote IS NOT NULL` filter, without
which an uncast vote on a reopened election wins the ranking and reports a decision that was never
made.

Partition by `(reference_id, dataset_id)`. Omitting `dataset_id` yields one election per DAR and is
valid only for existence checks — `ElectionDAO.findLastElectionsByReferenceIds` carries a comment
saying exactly that.

### Reopened elections change the answer

Reopening a decision archives the previous elections and creates a new one
(`service/dao/DarCollectionServiceDAO.java:85`). So a DAR-dataset pair that was approved and then
reopened has an archived election carrying a cast vote and a live election carrying none.

The two plausible readings disagree:

- **Latest election** — the pair is *undecided*, because the live election has no cast vote.
- **Latest cast vote** — the pair is *approved*, because the archived vote is the most recent one
  actually cast.

The precedent CTE gives the second reading: it filters `v.vote IS NOT NULL`, so it skips the live
uncast vote and reports the old decision. That is right for the SO console, which is asking "what was
decided", and it is not obviously right for a funnel asking "what is still awaiting a decision".

This is a product-visible choice, not an implementation detail, so it is Open Question 6 rather than
a decision taken here. Until it is settled, report the two counts separately rather than picking one
silently.

### Shape of the returned rows

One row per DAR-dataset pair, each carrying its own timestamp so the client can bucket by day, week
or month without reshaping. DAR-level aggregates are a reduction over those rows, applied once Open
Questions 2 and 3 are answered.

Prefer CTEs for the multi-table reductions, per the repo's PostgreSQL convention, and paginate rather
than materialising unbounded result sets.

### Predicates that are easy to get wrong

- SO approval skipped: `requires_so_approval IS NOT TRUE`, never `= false` (Caveat D).
- Canceled DARs: excluded via `data->>'status'`, or they count as pending forever.
- Expired: `submission_date + INTERVAL '365 days'`, matching
  `DataAccessRequest.EXPIRATION_DURATION_MILLIS`.
- Decision turnaround: bound to DARs submitted after January 2026 if the SO columns are involved, and
  check for null `update_date` on old votes (Caveat C).

## Implementation Approach

### Shape of the work

Because nothing in the core needs capturing, tickets 2 to 5 are read-only reporting surfaces over
existing data with no schema between them, so there is no ordering constraint — a **fan-out, not a
stack**. Nothing here requires the DT-3942 pattern of five PRs rebasing onto each other.

Three tickets sit outside that core and do write: ticket 1 changes what two existing queries return,
and tickets 7 and 8 add columns. All three are independent of the reporting tickets and of each
other.

One qualification, so "standalone" is not oversold: tickets 2 to 5 all add endpoints to the same new
resource, service and DAO, so they overlap textually. Whichever lands first creates those files and
the others add a method each. That is a merge conflict of a few lines, not a dependency — none needs
another's behavior, and they can be reviewed in any order.

### Sizing basis

Budget is 500 insertions per PR. Estimates are calibrated against comparable merged work in this
repository rather than guessed:

- `DacDashboardResource` + service + DAO + summary model is 239 lines of main code and 498 of test —
  737 total for one read-only reporting surface. A single "DAR analytics" PR covering every reporting
  metric would be roughly three times the budget, which is why reporting is split by metric family.
- Recent merged PRs run 434–612 insertions (`[DT-3942][2/5]` 508, `[3/5]` 538, `[4/5]` 612,
  `[DT-3940][1/4]` 434).

Estimates are insertions, split main/test. Treat them as ±25%.

Every endpoint ticket also touches, by convention, and counted in the estimates:

- `ConsentApplication.java` — `env.jersey().register(...)`
- `ConsentModule.java` — a `@Provides @Singleton synchronized` provider
- `src/main/resources/assets/paths/<endpoint>.yaml` and its entry in `api-docs.yaml`

## Jira-Ready Tickets

### Ticket 1: Fix RADAR-approved elections reporting no final vote

**Depends on: nothing.** Size: ~120 (20 main / 100 test).

A pre-existing defect. Both `final_vote_date` derivations join the vote on `LOWER(v.type) = 'final'`,
so a `RADAR_APPROVE` vote never matches. They fail differently, and the difference matters:

- `findElectionWithFinalVoteById` (`ElectionDAO.java:58-72`) uses an **INNER JOIN** (`:66`), so a
  RADAR-only election is **absent from the result**, not merely missing its vote. Fixing it changes
  which elections appear, not just which fields are populated.
- `findLastElectionsByReferenceIds` (`:130-160`) uses a **LEFT JOIN** (`:145`), so it returns the
  election with null vote fields. It also derives the date correctly once the type filter is fixed.
- `:62` additionally derives the date from `create_date`, the election-open time, rather than
  `update_date`.

Impacted: `db/ElectionDAO.java`, `src/test/java/org/broadinstitute/consent/http/db/ElectionDAOTest.java`.

Confirm each with a failing test first. This is the only reporting ticket that alters existing read
behavior, and the INNER JOIN change is the riskier half — an election appearing where none did before
can change counts and list contents, so check what consumes both queries in `duos-ui` before shipping.

Done when: a RADAR-approved election is returned by both queries, carries its vote and a decision date
taken from `update_date`, and a test fails without the fix.

### Ticket 2: DAR volume and composition reporting

Metrics 4, 5, 6, 7, 12. **Depends on: nothing.** Size: ~450 (220 main / 230 test).

Aggregation over existing columns; no schema change.

Impacted: new `resources/DarAnalyticsResource.java`, `service/DarAnalyticsService.java`,
`db/DarAnalyticsDAO.java` + mapper, record DTOs under `models/`. Reads `data_access_request`,
`dar_dataset`, `users`, `institution`, and the DAR `data` JSON for collaborators.

Record Caveats A and B in the path spec: institution is the submitter's current institution, and
collaborator counts are per-DAR rather than distinct people.

### Ticket 3: DAC decision reporting

Metrics 1, 2, 3. **Depends on: nothing.** Size: ~480 (215 main / 265 test).

Whether each DAR-dataset pair has a decision, whether it was approved or denied, and whether RADAR or
a chairperson decided. Ship **one row per DAR-dataset pair**, which is what keeps this independent of
Open Questions 2 and 3; the DAR-level rollup is a later small addition, not a rewrite.

Impacted: `db/DarAnalyticsDAO.java` + mapper, `service/DarAnalyticsService.java`,
`resources/DarAnalyticsResource.java`, record DTOs. Reads `election`, `vote`, `dar_dataset`.

Copy the `ranked_final_votes`/`latest_final_votes` CTEs from `SigningOfficialDashboardDAO.java:48-64`,
including the `v.vote IS NOT NULL` filter; partition by `(reference_id, dataset_id)`. Exclude canceled
DARs via `data->>'status'`, and settle the reopened-election question below before writing the
funnel.

Done when: a pair with a denied-then-approved-then-denied election history reports denied, a
multi-dataset DAR mixing RADAR and manual decisions reports both, and a canceled DAR appears in
neither the decided nor the pending count.

### Ticket 4: Decision turnaround reporting

Metric 8. **Depends on: nothing.** Size: ~300 (130 main / 170 test).

Submission-to-decision turnaround from `submission_date` to `vote.update_date` on the deciding vote,
with mean, median and mode. Full history, no capture — this is the metric the ticket believed
impossible.

Impacted: `db/DarAnalyticsDAO.java`, `service/DarAnalyticsService.java`,
`resources/DarAnalyticsResource.java`.

Quantify null `update_date` on old deciding votes first (Caveat C) and either exclude those rows or
report them as a known-unknown; do not silently fall back to `create_date`, which would report the
election-open date as the decision date.

### Ticket 5: SO approval and expiration reporting

Metrics 9, 10. **Depends on: nothing.** Size: ~350 (160 main / 190 test).

Impacted: `db/DarAnalyticsDAO.java` + mapper, `service/DarAnalyticsService.java`,
`resources/DarAnalyticsResource.java`, record DTOs. Reads `approving_so_id`,
`approving_so_timestamp`, `requires_so_approval`, `submission_date`.

Identify the pre-authorization skip as `requires_so_approval IS NOT TRUE AND parent_id IS NULL`,
bounded to DARs submitted after the column existed (January 2026). All three conditions are needed:
the column is NULL when approval was skipped, NULL on closeouts and on pre-authorized progress
reports, and NULL on everything written before January 2026 (Caveat D). A test asserting that
`= false` returns nothing is worth keeping as a guard against the predicate regressing.

Done when: turnaround is reported for approved DARs, skips are distinguishable from pending, a
closeout that went to an SO is not counted as a skip, and expiry matches
`EXPIRATION_DURATION_MILLIS`.

### Ticket 6: Admin DAR analytics dashboard (`duos-ui`)

**Depends on tickets 2 to 5 being deployed, at runtime.** Separate repository. Size: ~500–600, split
by section.

Decision funnel, RADAR-versus-manual split, SO turnaround distribution with mean/median/mode beside
it, volume rollups, and day/week/month bucketing. Built on the existing
`src/components/dashboard/{ConsoleDashboard,ConsoleDashboardGrid}.tsx` framework and the `dataviz`
skill's chart conventions. Each section consumes one endpoint, so the split seams already exist.

### Ticket 7 (conditional): Renewal capture

Metric 11. **Blocked on Open Question 1.** Size: ~350.

The only capture work needed for a *requested metric*; ticket 8 also writes, but for accuracy rather
than a new metric. Adds `renewed_from_dar_id` — not a reuse of `parent_id`. Revisit the decision to
derive expiration if renewal resets the clock.

Impacted: a changeset, `models/DataAccessRequest.java`, `db/DataAccessRequestDAO.java`, and whichever
resubmission path product defines.

### Ticket 8 (optional): Snapshot institution at submission

Fixes Caveat A. **Capture half depends on nothing; the reporting half depends on ticket 2.** Size:
~280 (110 main / 170 test), splittable along that seam if the dependency is inconvenient.

Persist the submitter's institution on the DAR at submission so historical attribution stops moving
when a researcher changes employer, **and consume it in the reporting query** — capturing the column
alone leaves ticket 2 still reporting current institution, so the caveat would survive its own fix.

Impacted: a changeset adding the column; `service/DataAccessRequestService.java` (inside the existing
creation transaction, beside `captureDatasetDaaSnapshots`); `models/DataAccessRequest.java`;
`db/DataAccessRequestDAO.java`; `db/mapper/DataAccessRequestMapper.java`, since DAR hydration is
explicitly mapped rather than automatic (`:16`); and `db/DarAnalyticsDAO.java` to prefer the snapshot
and fall back to the live institution for rows predating it.

Forward-only, and the fallback means the two eras have to be distinguishable in the output rather than
silently blended. Worth filing only if year-over-year institution reporting matters to the PO.

### Summary

| Ticket | Metrics | Size | Depends on |
| --- | --- | --- | --- |
| 1. Fix RADAR final-vote defect | — | ~120 | nothing |
| 2. Volume and composition reporting | 4, 5, 6, 7, 12 | ~450 | nothing |
| 3. DAC decision reporting | 1, 2, 3 | ~480 | nothing |
| 4. Decision turnaround reporting | 8 | ~300 | nothing |
| 5. SO approval and expiration reporting | 9, 10 | ~350 | nothing |
| 6. `duos-ui` dashboard | all | ~500–600, split | 2–5 deployed (runtime) |
| 7. Renewal capture | 11 | ~350 | Open Question 1 |
| 8. Snapshot institution at submission | 5 (accuracy) | ~280 | ticket 2, for the reporting half |

Tickets 1 to 5 can all be worked in parallel; none blocks another. Ticket 8's capture half is equally
independent, but its reporting half modifies the institution query ticket 2 introduces, so it either
follows ticket 2 or splits in two.

Metric 13 is not a ticket: every reporting ticket returns a timestamp per row, which is what makes
bucketing a client concern.

## Test Matrix

At minimum, cover:

- a DAR-dataset pair with a denied-then-approved-then-denied election history, asserting the current
  decision is denied;
- a multi-dataset DAR mixing a RADAR decision on one dataset with a manual decision on another;
- a RADAR-decided election reporting a decision date, which fails before ticket 1;
- a canceled DAR counted as neither decided nor pending;
- a DAR with `requires_so_approval` NULL, asserting it is reported as a skip and that `= false` would
  have missed it;
- a deciding vote with a null `update_date`, asserting it is excluded rather than dated from
  `create_date`;
- expiry against `EXPIRATION_DURATION_MILLIS` on either side of the boundary;
- the reporting query against a DAR with no decision, a partial decision, and all datasets decided.

Synthetic data only. No Mockito `lenient()` stubbing.

## Risks

- **Reporting accuracy is bounded by the caveats, not the queries.** Institution attribution moves
  with the researcher, collaborators have no durable identity, and two metrics have under fifteen
  months of history. These are presentation risks: a number that is quietly wrong on a dashboard is
  worse than one that is absent.
- **Ticket 1 changes existing read behavior.** The reporting tickets add read-only surfaces; that one
  alters what two existing queries return for RADAR elections — including making elections appear that
  previously did not — so `duos-ui` consumers need checking.
- **The `requires_so_approval` null trap fails silently.** A query using `= false` returns an empty
  result rather than an error, and the Java model coerces the null away, so the mistake is invisible
  from the service layer. This is why it has a named test above.
- **Tickets 7 and 8 add columns**, and both are optional or blocked. They are the only schema changes
  in the plan.

## Open Questions

1. **Renewal.** Is it the existing progress-report continuing-review flow, or a researcher
   resubmitting after expiry — a flow that does not exist today? Blocks ticket 7, and the decision to
   derive expiration if renewal resets the clock.
2. **Decision timestamp rollup.** For a multi-dataset DAR, is it decided on the first dataset
   decision or the last? Needed for DAR-level rollups in tickets 3 and 4; does not block per-dataset
   rows.
3. **Denial rollup.** Is a DAR denied if any dataset is denied, only if all are, or is partial
   approval a third bucket? Same scope as question 2.
4. **Researcher count.** Does "researchers on a DAR" deliberately exclude external collaborators, or
   should those be a separate metric? Note that externals are not institution-validated at submission
   while internals are (Caveat B).
5. **History coverage.** Not a product question but a measurement: how many deciding votes carry a
   null `update_date`, and how many DARs predate the SO columns? The answers set the honest starting
   date for metrics 8 and 9. Run before promising charts.
6. **Reopened elections.** When a decided DAR-dataset pair is reopened, is it "decided" (its last cast
   vote stands) or "awaiting a decision" (its live election has no vote)? This changes the headline
   funnel number. The existing dashboards already chose the first reading deliberately —
   `ResearcherDashboardDAO.java:56-57` carries the comment "for a re-opened election the most recently
   edited vote decides" — but they are answering "what was decided", not "what is outstanding", so the
   choice should not be inherited without asking. Needed for tickets 3 and 4.

## Explicitly Out of Scope

- Any change to DAC voting, RADAR rule, or SO-approval workflow semantics.
- Denormalized decision rollup columns on `data_access_request`.
- Re-recording facts Consent already persists.
- Bulk CSV or warehouse export. The dashboard is the agreed delivery target; export is a fast-follow
  if the dashboard proves insufficient.
- A general-purpose audit framework across DUOS.

## Definition of Done

- Admin-scoped endpoints return flat, timestamped rows requiring no client-side reshaping before
  descriptive statistics.
- Decision outcome, decision source and decision turnaround are reportable across full history, with
  any excluded range stated rather than silently dropped.
- SO approval turnaround is reportable, with pre-authorization skips identified by a predicate that
  survives the nullable column.
- `duos-ui` surfaces the funnel, turnaround, volume and expiration views, bucketable by day, week or
  month.
- RADAR-decided elections report their vote and decision date, with a test that fails without the fix.
- Renewal has an approved disposition, implemented or explicitly declined.
- Apart from ticket 1, no existing DAR, voting, SO-approval or cancellation behavior has changed.
