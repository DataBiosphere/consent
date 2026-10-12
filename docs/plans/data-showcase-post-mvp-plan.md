# Data Showcase: post-MVP epics (E6–E10)

Companion to the [delivery plan](data-showcase-delivery-plan.md) for
[DT-3904](https://broadworkbench.atlassian.net/browse/DT-3904). The delivery plan details the MVP
(E1–E5) and keeps a short outline of each epic below. This document holds the detailed post-MVP
stories, moved here unchanged on 2026-10-09 so the MVP plan is easier to read. Each epic is
re-planned before it starts, and this document is its starting point.

Section and story references (section 3, S2.2, section 8 and so on) point to the delivery plan
unless they name a story in this document. The delivery plan's conventions apply here too: backend
file shorthand `http/...` means `src/main/java/org/broadinstitute/consent/http/...`, and all fixtures
are synthetic.

## Subscription registry synchronization (E7)

Code remains the content source of truth. E7 adds a versioned JSON manifest generated from the
validated duos-ui configs and retired-keys list, plus a Consent registry snapshot for subscription
workers. A new partner needs a config PR and deployment synchronization, not a Consent code release.

- **Manifest contract:** include schema version, target environment, frontend release identifier,
  content hash, and entries containing stable key, `live`/`hidden`/`retired` state, enabled topics
  and topic membership definitions. Retirement is represented by tombstones from the retired-keys
  list; it is not a third routable frontend status. Include only subscription-relevant public
  metadata, with no executable config or arbitrary query fragments.
- **Membership:** an editorial source contributes its configured dataset identifiers. A computed
  source contributes its fixed scope and section eligibility predicate plus pins/exclusions.
  Consent evaluates membership against authoritative catalog data, independently of ranking order
  and display `limit`; a top-N cutoff must not silently discard notification recipients. Pins must
  satisfy scope/eligibility, exclusions win, and the union is deduplicated. Program owners approve
  each topic's source sections and scope explicitly; unrelated shelves do not imply subscription
  membership. Visibility is rechecked before sending. Archive topics additionally use E9 criteria.
- **Delivery and authority:** S7.1 adds an authenticated `/api` synchronization contract restricted
  to the deployment identity, preserving Resource → Service → DAO. The release pipeline stages a
  validated immutable snapshot and pauses enrollment/sends before switching frontend releases,
  then activates the snapshot and resumes only after that frontend release is serving. Use an
  expected-active revision and monotonic activation revision to reject stale/concurrent updates;
  retries are idempotent. Rollback explicitly reactivates a prior compatible snapshot under a new
  activation revision, while retained retirement tombstones prevent key reuse.
- **Failure and retirement:** pause new enrollment and sends during release/snapshot mismatch or
  failed synchronization; never fall back to accepting arbitrary keys. Monitor the active release
  and snapshot revision, define a bounded freshness interval in S7.1, and fail closed when that
  check expires. Hidden, removed, retired or topic-disabled entries cannot enroll or deliver;
  missing entries in a replacement snapshot are inactive. Recheck registry state before each send,
  including queued jobs. Unsubscribe remains available during outages and after retirement.
- **Ownership:** the frontend release owner owns manifest generation/activation and rollback;
  the notifications owner owns snapshot validation, reconciliation and delivery gating. Agree
  protocol fixtures, failure recovery and freshness before enabling any S7 signup UI.

## Epics and stories

### E6 — Ranked discovery, charts and the remaining catalog/resource shelves (post-MVP)

**Outcome:** ranked shelves stay current without hand curation and visitors see richer discovery without new access
decisions. **Owner:** catalog/analytics backend owner + frontend discovery owner.
**Source:** 2, 6, 7, 13, 15–17, 20 and the corresponding config schemas. **Dependencies:** E1–E5 interfaces.

#### S6.1 — Public ranking modes

- **Backend:** add `most-requested` and `largest-cohort` to the S2.2 `ranking` mode through a bounded
  `ShowcaseRankingService`/DAO; add `instant-eligible` and `archive-soonest` when E8/E9 data exist.
  Rankings return identifiers only and always run after eligibility and scope filters.
- **Backend — most requested:** reuse DAR metrics definitions, excluding draft DARs, with an
  explicit rolling window, renewal and multi-dataset counting policy. Because popularity reveals
  request activity: require a minimum request count before a dataset can rank, never return counts,
  and never rank on requests to ineligible datasets. Agree the threshold with privacy owners.
- **Backend — performance:** start with bounded indexed queries computed per request; add short-lived
  caching or materialized rollups only if the ranking-cost experiment or production load justifies
  it. Deterministic ties.
- **Acceptance/tests:** fixed-clock data verifies time windows, duplicate requests, the minimum-count
  threshold, eligibility before ranking, scope filters, pins/exclusions, sparse counts, null
  participant values and tie order. No response contains request counts or requester identity.
- **Dependencies/PR boundary:** S2.2 + metrics-owner and privacy agreement; one PR per ranking.

#### S6.2 — Most requested and largest cohorts

- **Backend/frontend:** add config schemas, CI guard rules and renderers for `most-requested-datasets`
  and `largest-cohorts` as computed shelves on the S6.1 rankings, using shared dataset cards. Show live reviewing DAC
  and the correctly labeled cohort measure. Define cohort eligibility when the catalog has only
  participant counts; do not equate samples with participants without sign-off.
- **Backend/frontend:** omit the artifact's lifetime request totals, request-volume sparklines and
  quarterly volume changes under S6.1's no-public-request-count policy. Do not add these to config
  or the projection. Any future volume display requires a separately approved privacy-policy
  revision and updated contract/tests; provenance alone is insufficient. Optional median review
  duration requires an approved suppression threshold, source, denominator, clock, window and
  freshness, with sample counts kept internal. Sub-cohort/phenotype displays likewise require
  known provenance. Unknown metrics are hidden, never filled from sample copy.
- **Acceptance/tests:** pins and exclusions apply on top of the ranking; metadata changes stay
  live. Tests cover invalidated datasets, empty public shelves, rolling-window explanation and no
  public disclosure of requester identities or request volumes for any dataset. Contract and
  renderer tests reject request-total/trend fields and verify suppression of sparse timing metrics.
- **Dependencies/PR boundary:** S6.1; deliver each shelf with its config schema and API fixtures.

#### S6.3 — Curated-set charts and computed hero statistics

- **Frontend/backend:** for a curated set, aggregate client-side over the deduplicated union of
  eligible datasets already returned by the S2.2 lookup, which is bounded and needs no new endpoint.
  Flagship-only full-catalog mode is also computed client-side, from the S2.2 `all` mode, so no
  aggregate endpoint is needed if the measured all-eligible response fits one page load. If it does
  not, fall back to a server-side aggregation built from a fixed list over the same projection.
  Never use the authenticated Elasticsearch search for this: it requires sign-in, returns full
  index documents and lets the caller define aggregations. Explicitly define
  chart categories, missing-value buckets and whether counts are datasets or participants; never
  sum participants across datasets and label them unique people without deduplication evidence.
- **Frontend:** initially support the observed disease-area and data-type bars plus access-mode
  donut, with an explicit multi-label/category policy. Account for external/unknown access modes;
  do not force them into the three illustrated slices. Compute unique studies by study identity,
  tool totals from defined resource scope, and bytes only from compatible authoritative measures.
  The mockup's repeated 1,842 value is not evidence that study and dataset counts are equal.
- **Frontend:** implement `datasets-at-a-glance`, chart selection in config, hero computed/manual mode,
  bar/donut visualizations and filter click-through using existing library query encoding. Provide
  text/table equivalents, keyboard-operable filters and labels independent of color.
- **Acceptance/tests:** repeated IDs count once, hidden/deleted records contribute nothing, the CI
  guard rejects full-catalog mode outside the flagship, empty charts are honest and aggregates agree with rendered
  membership. Test the heading/filter/text-equivalent corrections in the artifact review; earlier
  Jira accessibility criteria still require recovery in S1.1.
- **Dependencies/PR boundary:** E2 `all` mode; chart fields (disease area, data types) must first be
  approved and exposed in the S2.1 projection; query/aggregate tests before chart components.

#### S6.4 — Workflows, notebooks and AI models

- **Backend/frontend:** add `new-workflow-tools`, `notebooks` and `ai-models` as distinct registry
  types using E3's resource-card primitives and config schemas. Validate configured names/descriptions/tags/
  thumbnails/launch/docs URLs; add appropriate user-facing resource labels and empty states.
- **Backend/frontend:** add optional typed version, runtime/platform, notebook environment and
  model parameter-count fields where approved content exists. Usage-in-workspaces and added-date
  badges require defined sources/meaning. Keep these out of generic hand-entered analytics and
  omit unavailable fields rather than making external telemetry a release dependency.
- **Acceptance/tests:** each type independently supports config validation, item order, enabled/empty
  semantics, safe launch links and thumbnail alt handling. Test all three entries even if they
  share implementation. No execution, credential exchange or model hosting is implied by a card.
- **Dependencies/PR boundary:** S3.4/S1.2; one small PR per entry or a tightly scoped shared-family PR.

#### S6.5 — Research areas and initiatives

- **Backend/frontend:** add `research-area` and `research-initiative` with label, description,
  approved icon/image and destination URL config schemas. Keep destinations explicit until the
  scoped-library decision is resolved; display only approved groupings.
- **Backend/frontend:** preserve research-area grids and initiative groups within the fixed section.
  Add group ID/title, acronym/full name and optional dated availability badge to initiative items;
  the export has four groups and thirteen cards, not one flat shelf. Group/item ordering does not
  authorize section reordering. Avoid hardcoded group counts in editable eyebrow copy.
- **Acceptance/tests:** group cards navigate correctly, internal filter links survive authentication,
  unsafe destinations fail both validators and public empty/disabled groups have no anchor.
- **Dependencies/PR boundary:** E1/E3 patterns; independent of rollup implementation.

### E7 — Program subscriptions, community and education (post-MVP)

**Outcome:** showcase-specific opt-ins result in managed delivery, not dead signup forms.
**Owner:** notifications backend owner, frontend owner and communications/privacy stakeholders.
**Source:** 5, 22, 23 and shared notification requirement. **Dependencies:** E1–E5; archive notices
depend on E9 authoritative lifecycle events.

#### S7.1 — Subscription preferences and enrollment lifecycle

- **Backend:** design `ShowcaseSubscription` storage keyed by showcase key (immutable and never
  reused, enforced by the S1.2 CI guard), topic, subscriber identity/channel and consent state, with
  timestamps. Implement the subscription registry synchronization contract above and persisted registry
  snapshots. Validate key syntax, then require an active `live` entry and enabled topic in the
  current synchronized snapshot; reject unknown, hidden, retired and removed keys. A matching key
  pattern alone never authorizes enrollment. Reuse existing mail infrastructure where suitable,
  but add explicit subscription/verification/unsubscribe contracts.
  Resolve anonymous-versus-signed-in enrollment, double opt-in, retention and abuse limits before
  implementation. Public token actions need narrow non-`/api` routes; authenticated preferences
  remain under `/api`. Verification/unsubscribe tokens are opaque, expiring and stored safely.
- **Frontend:** add accessible signup/confirmation/preferences/unsubscribe states. Avoid exposing
  whether an email already exists. State exactly which program and topic is being subscribed to.
  Generate the manifest in the release pipeline and keep enrollment unavailable until its release
  revision is active in Consent. Environment-local tests synchronize only synthetic configs.
- **Acceptance/tests:** deduplicate enrollment, isolate two showcases, reject unknown/hidden/
  retired keys and disabled topics, verify token expiry/replay, suppress unsubscribed recipients
  and exercise rate limits. Test synchronization authorization, malformed manifests, environment
  mismatch, stale/concurrent activation, retries, deployment failure, rollback and freshness expiry.
  Removal/retirement stops enrollment and queued sends; unsubscribe still works during an outage.
  Never put email addresses into analytics.
- **Dependencies/PR boundary:** communications decisions + E1 showcase keys; agree manifest fixtures,
  then registry storage/sync API, release integration and subscription API, then UI flows. Update
  the showcase guide and release runbook with synchronization, retirement and recovery procedures.

#### S7.2 — Reliable release and registration-alert delivery

- **Existing pipeline first:** Consent already sends a `NEW_STUDY_DIGEST` email
  (`EmailService.getRecentStudyInfoForDigestMessage`) built from the same “new in DUOS” signals
  the release shelf uses. Decide before building whether showcase release alerts extend that digest
  (for example a per-showcase topic filter) or run beside it. Record the event definition,
  audiences, preferences and how a user subscribed to both avoids duplicate notices. Do not run two
  “new data” definitions side by side.
- **Backend:** implement source events, audience resolution, durable job/outbox state, retry/backoff,
  per-event/per-recipient idempotency and delivery observability using existing SendGrid/mail hooks.
  Resolve release membership from the active synchronized manifest using that contract's editorial/
  computed membership rules. Record the manifest revision used for audience resolution; recheck
  current membership, public eligibility, topic/registry state and unsubscribe before sending.
  Scope changes do not replay old release events automatically. Deduplicate a dataset that appears
  in multiple shelves and specify cross-showcase duplicate-email preference. Operationally
  reconcile failed deliveries.
- **Frontend:** enable release signup only after end-to-end delivery exists; display meaningful
  pending/success/failure states and link to preference management. Track opt-in and verified opt-in
  distinctly; provider acceptance is not proof of inbox delivery.
- **Acceptance/tests:** synthetic provider failures/retries, unsubscribe-before-send, duplicate events,
  showcase retirement, schedule cancellation and topic isolation produce correct outcomes. Test
  editorial membership, computed scope with pins/exclusions, datasets outside the visible top N,
  changed visibility/scope before send, manifest mismatch and queued jobs after retirement.
  QA verifies delivery through a test sink, never real researchers. Registration sends on approved
  state change.
- **Dependencies/PR boundary:** S7.1 registry synchronization + the release date decided in S1.1; worker/outbox
  and UI activation separately.

#### S7.3 — Conference and education section family

- **Backend/frontend:** implement `community-conference`, `where-else`, `anvil-workshops`,
  `anvil-scholars`, `tech-policy-interns` with typed config schemas and CI guard rules. Conference carries date/
  timezone/location/description, optional past-event history and open-registration versus alert-signup
  state. Where Else supports linked event tiles with optional date/location, reflecting the export;
  a generic-links-only variant needs explicit product agreement. Program panels support Markdown
  without raw HTML, CTA links and structured lead/audience/deadline/focus facts. Workshops support
  upcoming/recent sessions and a private-training CTA without exposing private material. Preserve
  season/year-only dates without inventing exact timestamps.
- **Acceptance/tests:** explicit timezones render consistently; expired events do not claim upcoming
  registration. Alert signup requires S7.1/S7.2; inline workshop enrollment requires S7.4, while
  safe provider registration links do not. Copy-only panels
  render through type-specific emptiness rules; test Markdown/link safety and each registry key.
- **Dependencies/PR boundary:** E1/E3; static panels can precede subscription delivery with signup disabled.

#### S7.4 — Workshop registration strategy and conditional enrollment integration

- **Decision:** the export lets visitors select several sessions and submit one email address.
  This is distinct from subscribing to an alert about registration opening. Prefer a real provider
  registration URL per session initially; record the resulting interaction difference with product.
  Implement native multi-session signup only if that behavior is required for release.
- **Backend, if native signup is chosen:** agree provider contracts for stable session IDs,
  enrollment, capacity/closed sessions, identity/consent, cancellation and outcomes. Add a narrowly
  scoped resource/service/adapter with idempotency, rate limiting, verified recipient handling and
  per-session results. Subscription state is not evidence of enrollment. Reuse delivery plumbing
  where appropriate without treating an email-send success as successful registration.
- **Frontend:** for the preferred link variant, render accessible session cards/links with clear
  provider handoff. For native signup, implement labeled multi-select, validation, pending state,
  partial-success/retry feedback and confirmation/cancellation instructions. Never show the export's
  checkbox form unless its submission path works end to end.
- **Acceptance/tests:** verify correct session destinations for the link variant. Native integration
  additionally tests duplicate submissions, one closed session among several, provider outage,
  invalid/expired identity verification and accurate per-session status without duplicate enrollment.
- **Dependencies/PR boundary:** S7.3 plus program/provider decision; native path additionally uses
  S7.1/S7.2 consent/delivery capabilities. Scope and estimate the adapter after provider validation;
  this is a conditional post-MVP story, not a new MVP prerequisite.

### E8 — Governed DUO/RADAR automation and instant-approval discovery (post-MVP)

**Outcome:** eligible requests receive auditable existing-system grants, while ambiguous/ineligible
requests continue through manual review. **Owner:** consent/RADAR lead, DAC governance and frontend
DAR owner. **Source:** 9–10 and instant metrics/rankings. **Dependencies:** governance can start
at R0; production activation waits for the missing original criteria and explicit rule approval.

#### S8.1 — Ratify eligibility, matching and measurement policy

- **Implement:** inventory existing RADAR rules and matching results against the intended DUO
  behavior. Specify which DACs/datasets opt in, rule/ontology versions, ambiguous/unknown terms,
  requester/SO/DAA eligibility, country constraints, revocations and multi-dataset outcomes.
  The mockup's research-use-statement tooltip does not specify a free-text or LLM-based matcher;
  preserve structured, deterministic rule evaluation unless separately justified and approved.
  Coordinate primary-data-use and VODAR work without conflating their scopes. Define whether the
  approval target measures submission-to-decision or eligible-after-SO-to-decision; report both
  where meaningful. “Under 5–10 minutes” is a target, not an unconditional UI promise.
- **Frontend:** design conditional eligibility copy and status states before exposing “instant.”
- **Acceptance/tests:** DAC/product owners approve synthetic positive/negative/ambiguous scenarios
  and fallback behavior. No new rule treats ontology compatibility alone as authority to grant access.
- **Alternative exit:** if existing RADAR rules satisfy all agreed launch cases, record that result
  and scope S8.2 to proven audit/idempotency/integration gaps; do not build a new matcher just because
  the source calls this an “engine.” S8.3/S8.4 still validate discovery claims and rollout metrics.
- **Dependencies/PR boundary:** recovered source criteria; policy/test fixtures before rule coding.

#### S8.2 — Extend the existing decision path safely

- **Backend:** extend `DACAutomationRuleService`, rule implementations and matching adapters only
  where S8.1 shows gaps. Keep existing election/vote/notification semantics and approval consumers.
  Re-evaluate authoritative dataset restrictions, DAC opt-in, request and SO prerequisites at
  decision time; a showcase field or stale search badge cannot authorize access.
- **Backend:** persist rule/version, evaluated input identifiers, outcome/reason and decision
  timestamps for audit. Make retries/concurrent triggers idempotent per DAR/dataset decision and
  integrate notifications after committed decisions. Unknown/ambiguous/error outcomes produce no
  grant and fall back to existing review with observable reasons.
- **Acceptance/tests:** synthetic positive/negative/ambiguous matches; changed consent/DAC opt-in,
  canceled/reopened requests, SO pending, mixed datasets, duplicate execution and transaction
  failure. Assert no duplicate vote/grant/email and no regressions to ordinary manual review or
  approved-user endpoints. Keep a kill switch independent of showcasing.
- **Dependencies/PR boundary:** S8.1; evaluator/rule tests, then persisted-decision integration.

#### S8.3 — Instant shelf, request status and ranking

- **Backend:** expose current dataset-level automation eligibility and privacy-safe median timing
  with window/freshness and an indication when timing is unavailable for insufficient samples.
  Sample counts remain internal under S6.1's no-public-request-count policy. Eligibility is not a
  user-specific guaranteed outcome.
  Add the `instant-eligible` ranking to S2.2; request status remains authenticated and scoped to its owner.
- **Frontend:** implement `instant-approval` renderer/config schema, consent tooltip and “Request now”
  handoff into the existing DAR. Show pending SO/automation/manual/approved/failed states through
  the existing request UI; specify polling/backoff/cancellation or reuse its current mechanism after
  source review. Suppress misleading median values for insufficient samples.
- **Acceptance/tests:** disable a DAC rule while a card is open; submitting does not bypass the
  updated policy. Test authorization of status reads, partial multi-dataset outcomes, live shelf
  invalidation and correct actor-specific messaging without request-state leakage to public pages.
- **Dependencies/PR boundary:** S8.2 + E2/E6 ranking interface; API projection then UI integration.

#### S8.4 — Shadow evaluation and controlled rollout

- **Implement:** run new matching behavior in evaluation-only mode against synthetic/staging cases
  and an approved evaluation process before enabling grants. DACs opt in to a pilot; compare expected
  outcomes, false-approval prevention and p50/p95 timing, including SO delays. Track automated versus
  manual outcomes from votes, not clicks, using agreed reporting definitions.
- **Acceptance/tests:** kill-switch drill stops new automated grants while manual routing remains
  available. Audit, alerting and operational owner are in place. Disabling a rule does not silently
  revoke existing grants; any revocation follows the established authorized workflow.
- **Dependencies/PR boundary:** S8.1–S8.3; activation is a distinct release gate, not bundled with MVP.

### E9 — Storage lifecycle awareness and advance notices (post-MVP)

**Outcome:** researchers receive credible warning before retrieval becomes slower or costlier.
**Owner:** data-storage integration owner, consent backend, notifications and frontend owners.
**Source:** 11–12. **Dependencies:** authoritative provider contract; E7 for actual notification
delivery. Storage movement, access grants and billing remain separate concerns.

#### S9.1 — Authoritative lifecycle and retrieval contract

- **Implement:** identify who supplies lifecycle state, scheduled archive time, byte size, fee/
  currency/basis and retrieval SLA. Define ownership, refresh frequency, cancellation/rescheduling,
  timezone and provider event ordering. Proposed states are ACTIVE, ARCHIVE_SCHEDULED, ARCHIVED and
  RESTORING, with retrieval requests modeled separately if the provider requires them. Reconcile
  with omitted original criteria before finalizing transitions. A local countdown is not proof
  that the provider moved data.
- **Contract:** the artifact mixes a per-GB banner with total-dollar card values. Define whether
  totals are provider quotes or estimates, decimal/binary byte units, currency, fee exclusions and
  quote expiry. Do not derive a production tariff from the illustrated amounts or use its generic
  24–48-hour SLA for every provider. A hot-storage claim does not waive DUOS access approval.
- **Frontend:** approve exact messaging for estimates, unknown price/SLA, stale status and a
  request made before the deadline. Do not promise a pre-archive access request prevents archival
  unless the storage owner explicitly supports that guarantee.
- **Acceptance/tests:** storage/product owners sign off on transition/event examples, notice lead
  times and the source of retrieval requests needed for the success metric. Unknown fees stay
  unknown; the platform does not collect payment.
- **Dependencies/PR boundary:** external contract before lifecycle schema and promises.

#### S9.2 — Lifecycle ingestion and reconciliation

- **Backend:** add normalized lifecycle metadata, provider event/version identifiers and observed/
  effective timestamps. Implement authenticated provider ingestion or controlled scheduled polling,
  allowed transitions, idempotent processing and reconciliation. Expose current safe fields through
  E2 projection. Repeated/out-of-order events cannot regress newer state or trigger duplicate notices.
- **Frontend/admin:** show source/freshness/errors in catalog administration as appropriate. Showcase
  configs select a computed archive shelf through ranking/scope/pins/exclusions, but the schema has
  no fields that could override provider lifecycle/fees.
- **Acceptance/tests:** scheduled→rescheduled/canceled, scheduled→archived, restore and provider
  outage paths use synthetic clocks/events. A stale feed is visibly degraded and does not silently
  invent a state. No worker moves data or alters grants as a side effect.
- **Dependencies/PR boundary:** S9.1; metadata + importer + reconciliation tests together.

#### S9.3 — Archive shelf, warning and notice workflow

- **Backend:** add the `archive-soonest` ranking and section eligibility excluding already
  archived records. Use S7's delivery mechanism for lead-time notices, deduplication, correction
  after rescheduling and suppression after unsubscribe/archive cancellation. Agree the audience:
  interested subscribers, approved researchers, pending requesters, or a defined combination.
- **Frontend:** implement `datasets-moving-to-archive` with warning strip, UTC-based countdown,
  size, fee/currency/basis, SLA/freshness and appropriate request/retrieval CTA. Its config requires
  `ranking: 'archive-soonest'`, with optional fixed-vocabulary `scope`, capped `limit`, `pin` and
  `exclude`; it has no editorial `items` list. Lifecycle state, archive time, size, fees and SLA
  come only from the E9 projection. The CI guard rejects hand-entered provider values/countdowns
  and any other ranking. Pins must satisfy scope and archive eligibility; exclusions win.
- **Acceptance/tests:** timezone/deadline boundary, missing estimates, provider outage, canceled
  schedule and already-archived records yield truthful output. Test scope, capped limits,
  deterministic archive-time ordering, eligible/ineligible pins, exclusions and invalid configs.
  Notice membership uses the synchronized S7 manifest independently of the shelf's display limit.
  Notice timing is verified with a test sink. If the backend cannot confirm future eligibility,
  hide or label the claim rather than
  showing a negative countdown as though archival were still upcoming.
- **Dependencies/PR boundary:** S9.2 + S7.2; shelf may ship before notifications only with that limited
  behavior explicitly released and no claims that notice delivery is complete.

#### S9.4 — Pilot and measure archive outcomes

- **Implement:** pilot with a cooperative storage provider, verify operational contacts and failed
  ingestion/delivery alerts. Define a comparable pre/post archive retrieval cohort and baseline;
  distinguish retrieval requests from dataset access requests and provider completion from clicks.
- **Acceptance/tests:** demonstrate notice receipt before a real scheduled transition under an
  approved pilot, reconcile observed provider state, and exercise stale-feed recovery. If retrieval
  outcome data is unavailable, mark the reduction metric unmeasurable rather than claiming success.
- **Dependencies/PR boundary:** S9.1–S9.3; separate activation gate from automated approval rollout.

### E10 — Publications, impact and legacy showcase migration (post-MVP)

**Outcome:** verified program impact and curated science become discoverable; legacy tiles retire
only when replacement workflows are proven. **Owner:** content/product, frontend/backend and impact
partner owner. **Source:** 19, 21, follow-up portion of 31. **Dependencies:** E1–E5; agreement for embed.

#### S10.1 — Curated publications

- **Backend/frontend:** implement `latest-published-science` with citation, summary, destination and
  year, per-showcase labels/links and self-report prompt setting. Add the config schema, CI guard
  rules and renderer together. Approved curated publications can launch before submission moderation;
  keep the prompt off until S10.2 is available.
- **Backend/frontend:** for reference parity, support optional journal, authors, month/date,
  cited-dataset IDs and related-workspace item/link alongside the common fields. Resolve catalog
  labels live and suppress related CTAs whose target is unavailable; do not copy sample citations
  or imply the artifact's workspace associations have been scientifically verified.
- **Acceptance/tests:** bounded citation/year validation, safe links, item ordering, no raw HTML,
  accessible cards and empty/disabled behavior. Publication presence alone does not substantiate
  a claimed clinical or program outcome.
- **Dependencies/PR boundary:** E1/E3; independently releasable registry entry.

#### S10.2 — Self-report submission and moderation (conditional)

- **Decision:** an external form link (for example a program-owned form) is the default; approved
  submissions reach the page through an ordinary config PR. Build the native path below only if
  product needs in-DUOS submission tracking.
- **Backend, if native:** inspect for an existing moderation capability; if absent, add an authenticated
  publication-submission resource/service/DAO with stable showcase attribution, submitter/audit,
  pending/approved/rejected states and admin-only review. Establish abuse/duplicate policy. Approval
  makes an item available for curation; it never changes a live page by itself.
- **Frontend, if native:** add submission form, feedback and admin moderation list/detail. Approved
  submissions are copied into a showcase config by PR.
- **Acceptance/tests:** submission/review permissions, duplicate submissions, rejection/resubmission
  and two-showcase isolation. Approved content stays off the page until a config PR ships it.
  Synthetic citation/identity fixtures only.
- **Dependencies/PR boundary:** S10.1 plus moderation owner; submission API then review/curation UI.

#### S10.3 — through.bio impact presentation and agreement gate

- **Preferred strategy:** implement `anvil-impact` as approved summary/stat content, attribution,
  optional approved static image and a real external portfolio link. The inspected artifact uses
  a static SVG illustration, not a live iframe. This option preserves its presentation intent with
  less integration work, but changes the ticket's embed-specific criterion and needs product agreement.
- **Backend/frontend:** define typed display mode (`summary` or `embed`), content provenance and
  refresh ownership. Implement only the agreed initial mode and reject unsupported settings. If
  live embedding is selected, add an approved URL/provider allowlist, frame sandbox, CSP, lazy
  loading, accessible frame title and fallback link/message. Never fetch arbitrary URLs server-side
  or accept executable HTML/SVG in config; static images are bundled assets reviewed in the PR.
- **Acceptance/tests:** off by default; enablement requires recorded program agreement/content
  approval. For summary mode, test factual approved content, attribution, image alt and real safe
  links without iframe requests. Embed mode additionally tests rejected origins, blocked/slow
  frames and usability without third-party cookies. Do not port the illustrative clinical graph
  as factual evidence or treat a config flag as the external agreement itself.
- **Dependencies/PR boundary:** per-program agreement + S1.1 mode decision; release independently
  of science. A live embed is a separate follow-up slice if summary mode launches first.

#### S10.4 — Retire featured-library maintenance after parity

- **Implement:** showcases and featured libraries are now the same code pattern, so a showcase
  entry can carry the `libraryVersions.ts` key whose query scopes its “browse all” link. Inventory
  every featured tile; map it to a showcase or document why it remains. Preserve existing
  `/datalibrary/:query` bookmarks/search semantics. Migrate entry links incrementally, verify
  equivalent destinations, and merge `DATA-LIBRARY.md` into the showcase guide where they overlap.
- **Acceptance/tests:** no removal until product approves parity and usage evidence for affected
  partners. Test old links and authenticated search scopes, maintain a reversible link switch, and
  avoid deleting query definitions still used elsewhere. Prepare a distinct Jira ticket after MVP;
  no automatic retirement triggered by the first flagship publication.
- **Dependencies/PR boundary:** proven E5 launch + partner-by-partner parity, not simply E1–E4 complete.
