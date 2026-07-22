# VODAR (View Only Data Access Request) Plan

## Status

Proposed.

## Objective

Add a "View Only Data Access Request" (VODAR) path across `duos-ui` and `consent` so a researcher can
request access to *view* a dataset — to decide whether to later request full analysis access — without
committing to analyze or publish. A VODAR is a constrained DAR: the applicant selects one primary
"view only" data-use option, attests a fixed Research Use Statement (RUS), and answers the ethical
questions. When the dataset's DAC has opted in via a RADAR rule, the VODAR is auto-approved with no
manual DAC vote; otherwise it proceeds through the normal DAC review flow like any other DAR. The VODAR
rule is an auto-approval accelerator, not a precondition for submitting a VODAR.

This plan covers the minimum viable feature. Several product requirements are explicitly deferred to a
Future Directions section because they require net-new infrastructure (timed access revocation, active
auth-list push) rather than extensions of existing behavior.

## Background and Scope

A VODAR is not a new entity. It is a `DataAccessRequest` whose data-use payload declares view-only
intent and whose approval is automated. The work therefore extends the three existing surfaces this
plan documents, rather than introducing a parallel request type.

### Terminology

- **VODAR flag** — a new boolean on the DAR data-use payload (`vodar`) meaning "view only, no analysis,
  no publication."
- **VODAR boilerplate** — the fixed RUS text a VODAR must carry, defined once and validated on both
  sides.
- **RADAR** — Rule Automated DAR Approval. The existing DAC-opt-in auto-approval mechanism
  (`dac_automation_rules` + `dac_rule_settings` + `RuleImplementationInterface`).
- **Ethical questions** — for VODAR purposes, *all* of the Section 3 / Step 3
  (`ResearchPurposeStatement.tsx`) questions: the study-characteristic radios and the ethics-concern
  radios. If any Section 3 question is answered Yes, the request cannot be submitted or auto-approved as
  a VODAR (see the decision table for the exact field list).

## Current Behavior

### duos-ui — DAR application form

The form lives under `src/pages/dar_application/`. Root orchestrator
`DataAccessRequestApplication.tsx` holds a single `formData` object (`DarFormData`) and passes
`formFieldChange` / `batchFormFieldChange` down to step components. Relevant steps:

- **Step 2 — `DataAccessRequest.tsx`**: datasets, the RUS textarea (`rus`, lines ~194-221, required,
  `defaultValue={formData.rus}`), the primary-purpose radio cascade (`diseases` → `hmb` → `poa` →
  `methods` → `otherText`, lines ~223-324, mutual exclusion handled in `primaryChange`, lines
  ~131-157), the non-technical summary (`nonTechRus`), and conditional IRB doc upload.
- **Step 3 — `ResearchPurposeStatement.tsx`**: study-characteristic radios (`aiLlmUse`, `controls`,
  `population`, `forProfit`, `oneGender`+`gender`, `pediatric`, `vulnerablePopulation`) rendered via
  the local `ResearchPurposeRow` helper, and the ethics radios under "Does this research involve the
  study of:" (`illegalBehavior`, `sexualDiseases`, `psychiatricTraits`, `notHealth`,
  `stigmatizedDiseases`). `addiction` and `populationMigration` exist in state/model but are not
  currently rendered.

Client validation is in `src/utils/darFormUtils.ts`: `validateDARFormData` → `calcDarErrors`
(Step 2, including the primary-purpose cascade and `rus`) and `calcRusErrors` (Step 3, requiring every
field in `requiredRusFields`). Submit runs through `attemptSubmit` →`submitDARFormData` →
`DAR.postDar` (`src/libs/ajax/DAR.ts`) → `POST /api/dar/v2`. The data model is
`src/types/model.ts` (`CombinedDataAccessRequest`, `DataAccessRequestData`).

### consent — DAR submission and validation

`DataAccessRequestResource.createDataAccessRequest` (`@POST api/dar/v2`) →
`DataAccessRequestService.createDataAccessRequest`, whose first step is `validateDar`. That validation
covers identity, institution, DAAs, datasets-approved, and country of operation. **It does not validate
the research-purpose / data-use content of the payload** (methods, hmb, ethics booleans, rus, etc.) —
those are accepted as-is into `DataAccessRequestData` (`data`). The only payload-content check today is
ontology-entry filtering.

The data-use booleans and `rus`/`nonTechRus` live on
`DataAccessRequestData` (`models/DataAccessRequestData.java`). `UseRestrictionConverter.parseDataUsePurpose`
translates them into a `DataUse` for matching. The ethics booleans (`illegalBehavior`, `addiction`,
`sexualDiseases`, `stigmatizedDiseases`, `vulnerablePopulation`, `populationMigration`,
`psychiatricTraits`, `notHealth`) also drive `DataAccessRequest.requiresManualReview()`.

### consent — RADAR and approval

Rules are catalogued in `dac_automation_rules` and activated per DAC in `dac_rule_settings` (chair
toggles via `PUT api/dac/{dacId}/rules/{ruleId}/toggle`). `DACAutomationRuleType` enumerates rule
types; each has a `RuleImplementationInterface` implementation registered in `Rules.implementationList`
whose `compare(dataset, dar)` returns whether to auto-approve. Shared predicate helpers on the
interface (`hasNoModifiers`, `secondaryConditionChecks`, `requestIsOnlyHMB`, `requestHasDiseases`)
reject datasets with modifiers or DARs with any concern/characteristic flag set.

On DAR submit (when `!requiresSOApproval`), `DACAutomationRuleService.triggerDACRuleSettings` loads the
DAR and, per dataset, applies each rule the dataset's DAC has activated. `applyRule` auto-approves only
if `compare(...) == true && !CountryValidator.containsBannedCountry(dar)`; it then calls
`openElectionAndApprove`, which creates a DATA_ACCESS/OPEN election plus a **single `RADAR_APPROVE`
vote owned by the chair who enabled the rule**, sets that vote TRUE, and closes the election. Rule
failures are caught and logged; submit still succeeds.

Approval is derived, not stored: `DataAccessRequestDAO.findApprovedDARsByDatasetId` treats a DAR as
approved when the most recent `FINAL` or `RADAR_APPROVE` vote is TRUE (with a submission-date > 1-year
filter). This is a lightweight election-with-one-vote path, not a truly election-less approval.

### consent — auth lists and expiration

Auth/access lists are **pull-based**. There is no outbound push to Sam groups or auth domains on
approval. External consumers query approved users via `TDRService.getApprovedUsersForDataset`
(`DatasetResource` `/{identifier}/approvedUsers`, `TDRResource`), which is backed by the same
`findApprovedDARsByDatasetId` FINAL/RADAR_APPROVE query. A RADAR-approved VODAR therefore appears in
that query automatically.

Expiration today is **notification + query filter only**, with no state change: `DataAccessRequest`
computes `expired`/`expiresAt` from a 365-day `EXPIRATION_DURATION_MILLIS`; `findApprovedDARsByDatasetId`
drops approvals older than one year; `sendDARExpirationNotices` emails at 11-month/1-year intervals.
There is no mechanism that revokes an approval or flips vote/election state on a timer.

## Requirements → Behavior

| Requirement | Where it lands |
| --- | --- |
| VODAR is the first/primary data-use option: "requesting access for viewing only, will not analyze nor publish". | New `vodar` boolean, rendered first in the Step 2 data-use series. The option is always available, for any dataset selection. |
| If VODAR = Yes, disable other data-use radios (do not display, do not require); same validation in backend. | UI hides/skips the Step 2 primary-purpose cascade; server rejects a VODAR that carries any primary-purpose selection. Section 3 is not disabled — it stays as the approval gate. |
| When checked, input standard text into the RUS field. | UI writes the VODAR boilerplate into `rus`. |
| Disable editing of the text. | RUS textarea becomes read-only while VODAR = Yes. |
| Add a RADAR rule that lets DACs auto-approve VODAR DARs. | New `DACAutomationRuleType.VODAR_V1` + rule implementation + Liquibase seed. |
| Rule: IF RUS = VODAR (checkbox + boilerplate) AND no ethical question is Yes, THEN approve. | Rule `compare(dataset, dar)` = `vodar == true && rus == boilerplate && noSection3Yes`. |
| Do not allow submission of a VODAR if any ethical (Step 3) question is Yes. | Client validation blocks submit; server `validateDar` rejects. "Ethical questions" = all of Section 3. |

Future Directions from the requirements (deferred — see final section): disable lab staff/collaborators,
active auth-list push, auto-expire after 12 hours, suppress progress reports, discrete application to a
subset of datasets, and truly election-less approvals.

## Recommended Decisions

| Question | Decision |
| --- | --- |
| What is the new field named? | A single boolean `vodar` on `DataAccessRequestData` / `CombinedDataAccessRequest`, defaulting absent/false. Keep the same name in both repos for traceability. |
| Exactly which inputs count as "other data-use radios" that VODAR disables? | The Step 2 primary-purpose cascade only: `diseases`, `hmb`, `poa`, `methods`, `other`/`otherText`, and `ontologies`. NOT the Section 3 questions (those remain and gate approval), NOT datasets, NOT researcher info, NOT DAAs. |
| Exactly which fields count as "ethical (Step 3) questions"? | *All* Section 3 (`ResearchPurposeStatement.tsx`) questions — the study-characteristic radios (`aiLlmUse`, `controls`, `population`, `forProfit`, `oneGender`+`gender`, `pediatric`, `vulnerablePopulation`) and the ethics-concern radios (`illegalBehavior`, `sexualDiseases`, `psychiatricTraits`, `notHealth`, `stigmatizedDiseases`). If any is Yes, the request cannot be submitted or auto-approved as a VODAR. Also include the two model-only concern booleans (`addiction`, `populationMigration`) in the backend guard for defense in depth, even though they are not currently rendered in the UI. |
| Under VODAR, are the Section 3 questions hidden or auto-defaulted? | No. They remain visible and required, so the applicant must explicitly answer each. Any Yes blocks submission (client and server) and prevents auto-approval. This keeps VODAR a genuine "I am only viewing, none of these apply" attestation rather than a silent default. |
| Does the dataset's own Data Use restrict VODAR approval? | No. View-only access is granted per-dataset by the DAC enabling the VODAR rule; the dataset's `DataUse` modifiers do not block it. This differs from GRU/HMB rules, which gate on dataset Data Use. The existing banned-country gate still applies. |
| When is the VODAR option available in the form? | Always. Any researcher can select VODAR for any dataset selection. Whether it is then auto-approved depends on the DAC having activated `VODAR_V1`; if not, the VODAR is submitted as a normal pending DAR for manual DAC review. The option is not gated on eligibility. |
| Is the RUS boilerplate a single source of truth? | Yes. Define it once, version it, and have the UI populate it and the backend validate `rus` equals it exactly (after trim/normalization). Drift between the two constants is a defect. |
| How is a VODAR approved for MVP? | When the DAC has activated `VODAR_V1`, reuse the existing RADAR path: one OPEN election + one `RADAR_APPROVE` vote. When it has not, the VODAR proceeds through the normal DAC election/vote like any other DAR. Truly election-less approval is a Future Direction, not MVP. |
| Does a VODAR flow through Signing-Official approval? | Yes, same as any DAR for MVP: RADAR only runs when `!requiresSOApproval`. Disabling lab staff/collaborators (which drives SO need) is deferred. |
| Do VODAR approvals reach auth lists? | Yes, automatically, via the existing pull-based `approvedUsers` query (the `RADAR_APPROVE` vote is honored today). No push integration is added in MVP. |
| How is `vodar` represented durably? | As a first-class, indexable column on the DAR (populated from the payload at write time by a single writer), not only a field inside the `data` JSON. Almost every future goal — 12h expiry sweeps, view-only scope, metrics, DAC oversight, election-less approval — must *query* for VODARs; a JSON-only flag forces a later schema migration and JSON backfill. Add the column in Ticket 1. Keep one writer so the column and payload cannot drift. |
| What identifies a request as a VODAR? | The `vodar` flag/column (the checkbox), not the boilerplate text. Boilerplate equality is a submission-time integrity check only. All downstream identification (expiry, metrics, scope) keys off the flag, so the boilerplate can be versioned without stranding historical approvals. |
| Can a submitted VODAR ever be left for manual DAC voting? | Yes. Auto-approval requires the DAC to have activated `VODAR_V1`; when it has not, the VODAR is submitted as a normal pending DAR and the DAC votes on it manually, exactly like any other DAR. The server does not reject a well-formed VODAR for being non-auto-approvable. The banned-country gate still blocks auto-approval as it does today. |
| When does a VODAR's approval clock start, and how does 12h expiry work? | Anchor expiry to the approval time (for auto-approval, ≈ submission date). The recommended mechanism is a scope-aware query-time filter (mirroring the existing 1-year filter in `findApprovedDARsByDatasetId`, gated on the `vodar` marker) rather than a state-mutating timer, so no new revocation state is needed for pull consumers. Implementation is deferred, but the anchor and mechanism are decided now so MVP does not foreclose them; the queryable marker is the prerequisite. |
| What happens to a VODAR that requires Signing-Official approval? | Same as any DAR: it waits in the SO queue until the SO acts, after which RADAR runs (auto-approving if the DAC has `VODAR_V1` on) or the VODAR proceeds to normal DAC review. No special re-check is needed, because a non-auto-approvable VODAR is allowed to fall through to manual review. The future "disable lab staff/collaborators" work is what reduces how often a VODAR needs SO approval. |
| How do DACs see VODAR auto-approvals? | Auto-approvals are recorded durably (leveraging the queryable `vodar` marker) so a DAC has oversight of what was approved under its rule, alongside the existing rule-toggle audit (`dac_rule_audit`) and the compliance log. |

## Implementation Approach

1. **One data-use flag, one boilerplate constant.** Add `vodar` to the DAR data model in both repos and
   a single canonical VODAR RUS string per repo, kept in sync and covered by a contract test. In
   `consent`, promote `vodar` to a first-class, indexable column populated from the payload by a single
   writer — not only a field in the `data` JSON — so the future goals can query for VODARs without a
   later migration. A request's VODAR identity is the flag, not the boilerplate text.
2. **UI enforces the constrained shape; backend independently enforces the same shape.** The UI hides
   the other data-use inputs, auto-fills and locks `rus`, and blocks submit on any ethical Yes. The
   backend does not trust the client: `validateDar` re-checks the VODAR shape (flag + boilerplate + no
   Step 2 primary purpose + no Section 3 Yes) and rejects violations with 400 before persistence,
   translation, or matching. A well-formed VODAR is always accepted; whether it is then auto-approved or
   sent to normal DAC review depends only on the RADAR rule.
3. **RADAR rule keys off the DAR, not the dataset Data Use.** A new `VODAR_V1` rule implementation
   approves when the DAR is a valid VODAR (flag + boilerplate) and carries no ethical Yes, independent
   of the dataset's `DataUse`. It composes with the existing banned-country gate and per-DAC activation.
4. **The VODAR option is always available; auto-approval is opt-in.** The form offers VODAR for any
   dataset selection. When a selected dataset's DAC has activated `VODAR_V1`, the RADAR rule
   auto-approves; otherwise the VODAR is submitted as a normal pending DAR for the DAC to vote on. No
   eligibility gate, and no narrow-to-eligible prompt, is needed.
5. **Reuse the existing approval, auth-list, and notification plumbing.** No new approval state, no new
   auth-list push, and no timed revocation in MVP. Those are isolated Future Directions. Auto-approvals
   are recorded durably via the `vodar` marker so DACs retain oversight and the expiry clock has a query
   surface.

## Refactoring and Risk

Two small refactors should be done as part of this work; a third is a larger strategic change to be
aware of but not to attempt now. The through-line risk is touching shared code paths
(`RuleImplementationInterface`, the approval-derivation query, the DAR form), so two mitigations apply
across the board.

1. **Extract one canonical VODAR predicate (do this).** A single `isCleanVodar(DataAccessRequestData)`
   on the backend (flag + boilerplate + no Step 2 primary purpose + no Section 3 Yes), mirrored once on
   the client, instead of writing the definition three times (client validation, server `validateDar`,
   RADAR `compare`). *Benefit:* the VODAR invariant gates data access, so a single definition keeps the
   three enforcement points from drifting and tests target one surface. *Risk:* low — new code, no change
   to existing behavior.
2. **Clarify `RuleImplementationInterface.secondaryConditionChecks` (do this, carefully).** Extract a
   named `hasNoResearchPurposeConcerns(...)` shared by the matcher, the existing GRU/HMB rules, and
   VODAR, rather than reusing a method that bundles several concerns. *Benefit:* removes ambiguity about
   which fields gate VODAR and gives one place to maintain. *Risk:* medium — this touches code the
   existing RADAR rules depend on; guard it with the characterization tests below before refactoring.
3. **Strategic, not now: replace derive-approval-from-votes with an explicit approval projection.** The
   reason several future goals are hard — election-less approval, 12h expiry, downstream scope, metrics —
   is that "approved" is recomputed from the vote/election join in `findApprovedDARsByDatasetId` on every
   read. An explicit approval record (`dar + dataset + scope + approvedAt + source`) would make all four
   straightforward. *Benefit:* the highest-leverage long-term simplification. *Risk:* high — it is
   cross-cutting (`TDRService`, `DatasetResource`, `DarCollectionSummary`, matching, the approved-users
   contract) and prone to scope creep. The MVP's queryable `vodar` column is the deliberate down-payment:
   it delivers the query surface those goals need without committing to the full approval-model rewrite.

**Cross-cutting mitigations.**

- **Additive, not destructive schema.** The `vodar` column is nullable/defaulted and populated at write,
  so nothing about how DARs persist is rewritten.
- **Characterization tests first.** Pin the existing RADAR rules (GRU/HMB/DS) and the approved-users
  query with characterization tests before extracting any shared helper, so an unintended behavior change
  is caught rather than shipped.

The net effect of refactors 1–2 is that the plan's own correctness claim — the client and server VODAR
definitions agree — becomes structural rather than maintained by hand, which is the right posture for an
invariant that gates data access.

## Communication and Notification Touchpoints

A VODAR approval is not only a state change — several audiences must learn it happened, and, critically,
must be able to tell a *view-only* grant apart from a full analysis approval. Today "approved" is a
scope-less binary derived from a TRUE `FINAL`/`RADAR_APPROVE` vote, so a VODAR surfaces on approved
lists indistinguishable from a standard approval. That is convenient (it already appears) but carries a
risk (see the note on downstream systems below).

This section covers DUOS-internal / user-facing communication only. Modifying the outward-facing
approved-users / TDR APIs to carry a view-only scope is **explicitly out of scope** here (see below).

**Cross-cutting requirement — approval scope labeling (DUOS-internal surfaces).** Where DUOS itself
presents an approval to a person, distinguish a view-only grant from a full approval, derived from the
DAR's `vodar` flag (equivalently, a `RADAR_APPROVE` vote on a VODAR). This applies to the researcher's
own views, DAC oversight, compliance logging, and metrics — not to the external consumer APIs.

Touchpoints, by audience:

- **Researcher (applicant).** Send a VODAR-specific approval message rather than the generic
  DAR-approved email — it must convey the scope (view-only, no analysis, no publication), the short
  lifetime once auto-expiry lands, and how to escalate to a full analysis DAR. The researcher's DAR
  console status and the voting-history view must render the automated approval clearly and labeled
  view-only, not as an empty/confusing election.
- **DAC (chair + members).** A VODAR whose DAC has activated `VODAR_V1` is auto-approved and needs no
  manual vote; a VODAR whose DAC has not is a normal pending DAR the DAC votes on like any other. Where a
  VODAR reaches the manual queue, present it clearly as a view-only request (derived from the `vodar`
  marker) so voters understand the reduced scope. For auto-approvals, provide oversight visibility that
  they occurred under the rule the DAC enabled, via a durable record keyed off the `vodar` marker
  (alongside the `dac_rule_audit` trail). Auto-approvals should not be invisible to the DAC.
- **Signing Official / institution.** SO approval already gates whether RADAR runs; consider whether the
  institution needs a distinct post-approval record for view-only access.
- **Compliance and metrics.** `ComplianceLogger.logRadarApproval` already fires on the RADAR path;
  ensure the entry carries the view-only classification. `DarMetricsSummary` and DAR dashboards should
  count VODARs and RADAR auto-approvals distinctly from standard approvals.

**Downstream access / auth systems (TDR / Terra / Sam) — out of scope.** A VODAR approval already
flows into the pull-based approved-users query (`findApprovedDARsByDatasetId` →
`getApprovedUsersForDataset`), so downstream consumers see the user with no extra work. This plan does
**not** modify those APIs to carry a view-only scope. The known consequence is that a consumer cannot,
today, distinguish a view-only user from a fully approved one and could over-grant analysis access.
Adding a scope to the consumer contract — and propagating revocation for the 12-hour auto-expire — is
deferred to the auto-expire Future Direction, where it must be designed together with the consuming
platforms.

MVP scope for this section: the VODAR-specific approval message and correct researcher-facing status /
voting-history display. DAC oversight summaries, institutional records, distinct metrics/compliance
classification, and any change to the downstream consumer APIs are fast-follow / deferred items.

## Jira-Ready Tickets

Six core tickets: three in `consent`, two in `duos-ui`, and one cross-repo verification ticket. Ticket
1 is foundational and unblocks the rest. Tickets 2 and 3 (backend validation and the RADAR rule) can
proceed in parallel after Ticket 1 and share one seam that should be built once and reused: the "clean
VODAR" predicate. Ticket 4 (UI form) depends on Ticket 1 for the field/boilerplate contract. Ticket 5
(UI rule toggle) depends on Ticket 3. Ticket 6 closes the loop.

### Ticket 1: Add the VODAR data-use flag and canonical boilerplate to the DAR model

**Issue type:** Story

**Suggested size:** 3 points

**Summary**

Introduce a `vodar` boolean and a single canonical VODAR RUS boilerplate string on the DAR data model
in both repositories, with no behavior change yet.

**Description**

Add `vodar` to `DataAccessRequestData` (`consent`) and to `DataAccessRequestData` /
`CombinedDataAccessRequest` (`duos-ui`, `src/types/model.ts`), defaulting to absent/false so existing
DARs and drafts deserialize unchanged. Define the canonical VODAR RUS boilerplate as a single named
constant in each repo. This ticket only establishes the contract; validation, UI behavior, and the
RADAR rule arrive in later tickets.

**Implementation notes**

- `consent`: add the field with the same Gson `@SerializedName`/alias conventions as neighboring
  booleans; confirm `DataAccessRequestMapper`/`DataAccessRequestReducer` round-trip it.
- Promote `vodar` to a first-class, indexable column on the DAR (a boolean defaulting false) via a
  Liquibase changeset, with an index. The `data` JSON remains the submission source of truth; the column
  is the query surface for the future goals (expiry sweeps, scope, metrics, DAC oversight). Populate the
  column from the payload through a **single write path** so the column and JSON cannot drift — never
  two independently-writable copies. Backfill existing DARs to `false`.
- A request's VODAR identity is the `vodar` flag/column, not the boilerplate text. Everything downstream
  that needs to find VODARs keys off the flag; boilerplate equality is only a submission-time integrity
  check (Ticket 2). This keeps the boilerplate versionable without stranding historical approvals.
- Decide and document the ownership of the boilerplate string. The frontend populates it; the backend
  must validate against the identical value. Normalize on comparison (trim, collapse internal
  whitespace/newlines) and pin the exact expected form in a test fixture shared conceptually by both
  repos.
- Do not translate `vodar` into `DataUse` in `UseRestrictionConverter` unless Ticket 3's rule needs it;
  the rule reads the DAR payload directly.

**Acceptance criteria**

- `vodar` serializes/deserializes on new and legacy payloads without breaking existing DARs or drafts.
- A queryable, indexed `vodar` column exists, is populated from the payload by a single writer, and is
  backfilled `false` for existing DARs; the column and JSON never diverge.
- A single boilerplate constant exists per repo; no duplicated literals.
- A test asserts the two repos' boilerplate strings are byte-for-byte equal after normalization
  (contract/fixture test).
- No change to submission, validation, matching, or UI behavior yet.

**Out of scope**

- Validation, UI rendering, and the RADAR rule.

---

### Ticket 2: Enforce the VODAR invariant server-side on DAR submission

**Issue type:** Story

**Suggested size:** 5 points

**Dependencies:** Ticket 1

**Summary**

Validate the VODAR shape in `DataAccessRequestService.validateDar` so the backend rejects malformed
VODARs regardless of client behavior. A well-formed VODAR is always accepted; whether it auto-approves or
goes to normal DAC review is left to the RADAR rule.

**Description**

When `data.vodar == true`, enforce: (a) `rus` equals the canonical boilerplate; (b) no other data-use
input is selected — the Step 2 primary-purpose fields (`diseases`, `hmb`, `poa`, `methods`, `other`,
`otherText`, `ontologies`) are all empty/false; and (c) no Section 3 question is Yes — the full Section 3
set (`aiLlmUse`, `controls`, `population`, `forProfit`, `oneGender`/`gender`, `pediatric`,
`vulnerablePopulation`, `illegalBehavior`, `sexualDiseases`, `psychiatricTraits`, `notHealth`,
`stigmatizedDiseases`, plus the model-only `addiction`, `populationMigration`) is empty/false. Reject
violations with 400 before persistence, audit, translation, Elasticsearch sync, and match/rule
processing.

This validates only the *shape* of a VODAR, not whether it can auto-approve. A well-formed VODAR whose
dataset's DAC has not activated `VODAR_V1` is accepted and proceeds through the normal DAC review flow
like any other pending DAR — DACs may vote on VODARs manually. The RADAR rule (Ticket 3), not this
validator, decides auto-approval.

**Implementation notes**

- Add a focused `validateVodar(DataAccessRequest)` invoked from `validateDar`, guarded by
  `data.vodar == true`; a non-VODAR request is unaffected.
- Return a clear, newline-joined message consistent with the existing validation-response format the
  duos-ui form already parses.
- Do not echo `rus` free text or the full payload in error messages or logs.
- Confirm the invariant runs for both create-from-draft and direct submit paths and does not fire on
  draft save (drafts may be incomplete).
- Prefer normalizing the "no other data-use selected" check into one reusable predicate so Ticket 3's
  rule and this validator share the definition of "clean VODAR."

**Acceptance criteria**

- A VODAR with the correct boilerplate, no other data-use selections, and no ethical Yes is accepted.
- A VODAR with a mismatched/edited `rus` is rejected.
- A VODAR carrying any Step 2 primary-purpose selection is rejected.
- A VODAR with any Section 3 question Yes is rejected (each field, including the model-only ones).
- A well-formed VODAR whose dataset's DAC has not activated `VODAR_V1` is accepted and proceeds as a
  normal pending DAR for manual DAC review — it is not rejected for being non-auto-approvable.
- Non-VODAR submissions are unaffected.
- Rejected submissions cause no database, audit, translation, Elasticsearch, match, election, or vote
  changes.
- Error messages contain no raw RUS text or payload dump.
- Tests are strict-stubbing compliant (no Mockito `lenient()`).

**Tests**

- Parameterized validator tests over the boilerplate match, each disabled Step 2 primary-purpose field,
  and each Section 3 field.
- A test that a well-formed VODAR whose DAC has not enabled `VODAR_V1` is accepted and left as a normal
  pending DAR (not rejected).
- Resource/service tests for create-from-draft and direct submit.
- Regression that draft save does not trigger the VODAR invariant.
- Contract test for the newline-joined error format consumed by duos-ui.

**Out of scope**

- Auto-approval (Ticket 3).
- UI behavior (Ticket 4).

---

### Ticket 3: Add the VODAR RADAR auto-approval rule

**Issue type:** Story

**Suggested size:** 5 points

**Dependencies:** Ticket 1 (and the shared "clean VODAR" predicate from Ticket 2)

**Summary**

Add a `VODAR_V1` DAC automation rule that auto-approves a valid VODAR when the dataset's DAC has enabled
it and no ethical question is Yes.

**Description**

Extend RADAR with a rule whose `compare(dataset, dar)` returns true when the DAR is a valid VODAR
(`vodar == true` and `rus` equals the boilerplate) and no Section 3 question is Yes — independent of the
dataset's `DataUse`. Reuse the existing activation, banned-country gate, election, and `RADAR_APPROVE`
vote plumbing so a VODAR is approved the same way other RADAR rules approve, with no manual DAC vote.

**Implementation notes**

- Add `DACAutomationRuleType.VODAR_V1`; add `ViewOnlyDataAccessV1 implements RuleImplementationInterface`
  and register it in `Rules.implementationList`.
- `compare` must NOT call `hasNoModifiers` against the dataset (VODAR ignores dataset Data Use) and must
  NOT require GRU/HMB/disease shape. It should reuse the "clean VODAR + no Section 3 Yes" predicate.
  Note the existing `secondaryConditionChecks` helper already rejects most Section 3 concern flags on
  the DAR; confirm it covers the full Section 3 set (including `aiLlmUse`, `controls`, `population`,
  `forProfit`, `oneGender`, `pediatric`) and extend the shared predicate rather than duplicating it.
- Add a Liquibase changeset inserting the `dac_automation_rules` row (state `AVAILABLE`, a clear
  description), following `changelog-consent-2025-11-24-add-open-dar-rule.xml` /
  `changelog-consent-2026-01-13-add-rule-so-dar-approval.xml`.
- Auto-approval remains gated by `applyRule`'s `!containsBannedCountry(dar)` check and by per-DAC
  activation via `dac_rule_settings`; do not weaken either.
- Confirm behavior when a DAR spans multiple datasets whose DACs differ: only datasets whose DAC has
  VODAR_V1 enabled are auto-approved, matching current per-dataset rule semantics.
- Compliance logging (`ComplianceLogger.logRadarApproval`) and researcher notification
  (`sendDatasetApprovalNotifications`) already fire on the RADAR path; verify they trigger for VODAR.
- Send a VODAR-specific approval message instead of the generic DAR-approved email: it must convey the
  view-only scope (no analysis, no publication), the short lifetime once auto-expiry lands, and how to
  escalate to a full analysis DAR. Add a distinct message/`EmailType` variant rather than overloading
  the standard approval copy. Ensure the compliance log entry carries the view-only classification. This
  is the DUOS-internal side only; the downstream approved-users/TDR API is not modified (see the
  Communication and Notification Touchpoints section).
- Identify a VODAR by the `vodar` flag/column, not by matching `rus` text, everywhere except the
  submission-time integrity check — this keeps boilerplate versioning safe and gives the expiry clock a
  query surface.
- Record the auto-approval durably (leveraging the `vodar` marker) so DACs have oversight of what was
  approved under their rule, alongside the compliance log and the rule-toggle audit.

**Acceptance criteria**

- With VODAR_V1 enabled for a dataset's DAC, a valid VODAR is auto-approved (OPEN election + TRUE
  `RADAR_APPROVE` vote) and appears in `findApprovedDARsByDatasetId`.
- With VODAR_V1 not enabled, the same VODAR is not auto-approved.
- A VODAR with any Section 3 question Yes, or a mismatched boilerplate, is not auto-approved even when
  the rule is enabled (defense in depth alongside Ticket 2).
- A banned-country VODAR is not auto-approved.
- The rule's decision is independent of the dataset's `DataUse` modifiers.
- On auto-approval the researcher receives the VODAR-specific message (view-only scope, escalation
  path), not the generic DAR-approved email, and the compliance log records the view-only classification.
- Existing GRU/HMB/DS rules and their tests are unchanged.

**Tests**

- Rule-implementation unit tests: valid VODAR, each Section 3 question Yes, boilerplate mismatch, banned
  country, dataset-with-modifiers (still approves).
- `DACAutomationRuleService` tests for enabled vs not-enabled DAC and multi-dataset/multi-DAC DARs.
- A notification test asserting the VODAR-specific message (not the generic approval) is sent on
  auto-approval.
- Liquibase seed/migration test that the rule row exists and is `AVAILABLE`.

**Out of scope**

- Truly election-less approval, timed expiration (Future Directions).

---

### Ticket 4: Add the VODAR data-use option and RUS lock in the DAR form

**Issue type:** Story

**Suggested size:** 3 points

**Dependencies:** Ticket 1

**Summary**

Render VODAR as the first data-use option in Step 2; when selected, hide/skip the Step 2 primary-purpose
cascade, auto-fill and lock the RUS, keep all Section 3 questions visible and required, and block
submission when any Section 3 question is Yes.

**Description**

In `DataAccessRequest.tsx`, add a VODAR yes/no as the first item in the data-use series — always
available, for any dataset selection — with the exact label: "I am requesting access to the data for
viewing only, I will not analyze nor publish results on the data." When Yes:

- hide and do not require the Step 2 primary-purpose cascade (`diseases` → `hmb` → `poa` → `methods` →
  `otherText`/`other` and `ontologies`), and clear any previously selected values via
  `batchFormFieldChange`, consistent with the existing `primaryChange` mutual-exclusion pattern;
- write the VODAR boilerplate constant into `rus` and render the RUS textarea read-only;
- keep the entire Section 3 (`ResearchPurposeStatement.tsx`) step visible and required. Section 3 is the
  approval gate, not a disabled area: the applicant must still answer every question, and every answer
  must be No.

Update `darFormUtils.ts` so that when `vodar` is true: the hidden Step 2 primary-purpose fields are not
required; `rus` is not user-editable but still validated as present and equal to the boilerplate;
Section 3 remains fully required; and submission fails with a clear, tab-routed error if any Section 3
question is answered Yes.

**Implementation notes**

- Reuse `formFieldChange`/`batchFormFieldChange` and the `ResearchPurposeRow`/`ScrollableTabs` patterns
  already in the form; do not fork a new form component.
- When VODAR is toggled off, restore normal editing: clear the boilerplate from `rus` (or leave it for
  the user to edit — confirm with product) and re-enable the hidden Step 2 primary-purpose inputs.
- `requiredRusFields` (all of Section 3) stays required under VODAR — do not relax it. Only the hidden
  Step 2 primary-purpose fields become non-required.
- The Section 3 "any Yes" block is a validation gate, not a silent disable; surface a specific message
  and route to the Section 3 tab via the existing `getErrorTabId` mechanism.
- Persisting a VODAR draft must round-trip the `vodar` flag and boilerplate via the draft endpoints.
- Optionally inform the user that a VODAR whose DAC has not enabled auto-approval will go to normal DAC
  review; this is advisory copy only and must not block submission.
- Reflect the view-only scope in the researcher-facing status and the read-only voting-history view
  (`VotingHistoryOverview`) so an auto-approved VODAR reads as "view-only, auto-approved" rather than an
  empty election. Confirm the exact status/collection-summary components before finalizing copy; this is
  DUOS-internal display only and does not touch the downstream approved-users/TDR API.

**Acceptance criteria**

- The VODAR option is always available in the data-use series, regardless of the selected datasets'
  DACs, and appears first with the exact required label.
- Selecting VODAR hides and clears the Step 2 primary-purpose inputs and they are not required.
- Selecting VODAR fills `rus` with the boilerplate and makes it read-only.
- All Section 3 questions remain visible and required under VODAR.
- Submitting a VODAR with any Section 3 question Yes is blocked client-side with a tab-routed error.
- Deselecting VODAR restores the normal form.
- An auto-approved VODAR reads as view-only in the researcher's status and voting-history views.
- A VODAR draft saves and reloads with the flag and boilerplate intact.
- The submitted payload matches what Ticket 2 accepts (verified against a running/mocked backend).

**Tests**

- Component tests: VODAR toggle hides/clears the Step 2 primary-purpose inputs; RUS is populated and
  read-only; all of Section 3 remains required.
- Validation tests in `darFormUtils` for the VODAR branch, including the Section 3 "any Yes" block and
  the relaxed Step 2 primary-purpose requirements.
- A submit test asserting the payload shape sent to `DAR.postDar`.

**Out of scope**

- The DAC rule-management UI (Ticket 5).
- Per-dataset partial VODAR within one request (Future Direction).

---

### Ticket 5: Expose the VODAR RADAR rule in the DAC rule-management UI

**Issue type:** Story

**Suggested size:** 3 points

**Dependencies:** Ticket 3

**Summary**

Let a DAC chair enable/disable the VODAR_V1 rule for their DAC from duos-ui, with the standard audit
trail.

**Description**

Surface VODAR_V1 in the DAC automation-rule management surface so chairs can toggle it via the existing
`GET api/dac/{dacId}/rules` and `PUT api/dac/{dacId}/rules/{ruleId}/toggle` endpoints, with the rule's
description explaining that enabling it auto-approves view-only requests for that DAC's datasets.

**Implementation notes**

- Confirm the current duos-ui DAC rule-management component and reuse it; VODAR_V1 should appear
  automatically once seeded (Ticket 3), so this ticket is primarily copy, ordering, and any per-rule
  UI affordances plus tests. If no such management UI exists yet, split that discovery out and re-scope.
- Ensure the rule description communicates the view-only, no-DAC-vote semantics clearly to chairs.

**Acceptance criteria**

- A chair can enable and disable VODAR_V1 for their DAC; the toggle persists and audits.
- The rule is presented with an accurate, chair-facing description.
- Non-chairs cannot toggle it (enforced by the existing endpoint authorization).

**Out of scope**

- Discrete per-dataset (subset) activation (Future Directions).

---

### Ticket 6: Cross-repo VODAR verification and end-to-end test

**Issue type:** Story

**Suggested size:** 3 points

**Dependencies:** Tickets 2, 3, 4, 5

**Summary**

Verify the full VODAR path end to end: a researcher submits a valid VODAR against a VODAR-enabled DAC's
dataset and is auto-approved with no manual vote and appears in the approved-users query.

**Description**

Add an end-to-end test (Cypress/Playwright per duos-ui conventions) plus a backend integration test
covering: VODAR selection auto-fills and locks the RUS; the Step 2 primary-purpose inputs are hidden;
any Section 3 question Yes blocks submission on both client and server; a clean VODAR is auto-approved
via RADAR when the DAC has the rule enabled; and the approved user surfaces through `approvedUsers`.
Include the case where the DAC has not enabled the rule: the VODAR is still submitted and lands as a
normal pending DAR for manual DAC review (no auto-approval, not rejected).

**Acceptance criteria**

- E2E happy path passes: submit → RADAR approve → appears in approved users.
- E2E rule-disabled path: a VODAR whose dataset's DAC has not enabled `VODAR_V1` is submitted, is not
  auto-approved, and lands as a normal pending DAR available for manual DAC voting.
- E2E negative paths pass: Section 3 Yes blocked on client and server; edited RUS rejected.
- The boilerplate contract test (Ticket 1) is part of CI in both repos.

**Out of scope**

- Future Directions items.

## Future Directions (deferred, not in MVP)

These are stated product requirements that require net-new infrastructure or design and are
intentionally out of the MVP scope above. Each should become its own ticket once MVP lands.

- **Auto-expire after 12 hours.** Current expiration is notification-only over a 1-year window with no
  state revocation and no auth-list removal. Decisions already fixed by this plan: anchor the 12h window
  to approval time (≈ submission for auto-approval), and prefer a scope-aware **query-time filter**
  (mirroring the existing 1-year filter in `findApprovedDARsByDatasetId`, gated on the `vodar` marker)
  over a state-mutating timer job — no new revocation state is needed for pull consumers. The queryable
  `vodar` marker (Ticket 1) is the prerequisite. Active revocation is only required once a push channel
  exists (below); that combined design is the largest deferred item.
- **View-only scope on the downstream approved-users / TDR API.** MVP does not modify the outward
  consumer contract, so consumers cannot distinguish a view-only user from a fully approved one and
  could over-grant analysis access. Adding a scope to that API must be designed with the consuming
  platforms and is coupled to the auto-expire revocation channel above.
- **Add approved users to auth lists (active push).** Today access is pull-based, so VODAR approvals are
  already visible to consumers via `approvedUsers`. An active push to Sam groups / auth domains does not
  exist and would be net-new; only needed if a consumer cannot pull.
- **Disable lab staff and collaborators for VODAR.** Removes/greys the personnel sections for VODARs;
  interacts with the SO-approval gate (`requiresSOApproval`) that determines whether RADAR runs.
- **Pre-submission auto-approval hint.** An optional per-dataset lookup (does the DAC have `VODAR_V1`
  active?) could tell the researcher, before submitting, whether their VODAR will auto-approve or go to
  DAC review. Purely a UX affordance — not required, since a VODAR is valid either way.
- **Discrete application of VODAR to a subset of datasets.** Two related capabilities: per-dataset rule
  activation finer than the current per-DAC `dac_rule_settings` model, and true per-dataset view-only
  within a single request. The latter would let a mixed DAR be view-only for some datasets and analysis
  for the rest in one request, but it requires dataset-level (not DAR-level) data use, which the current
  model does not support.
- **Election-less approvals.** MVP reuses the one-vote election; a truly election-less approval is an
  architectural change to how approval state is represented and derived.
- **Progress reports not possible/necessary for VODARs.** Suppress the progress-report path for VODARs.

## Explicitly Out of Scope

- Any timed access revocation or auth-list push in the MVP (see Future Directions).
- Changing how non-VODAR DARs are validated, matched, or approved.
- Changing the existing GRU/HMB/DS RADAR rules or the banned-country gate.
- Introducing a new request entity distinct from `DataAccessRequest`.

## Definition of Done

- A `vodar` flag and a single canonical VODAR RUS boilerplate exist in both repos, kept in sync by a
  contract test.
- `vodar` is a queryable, indexed column populated by a single writer (not only a JSON field), and a
  request's VODAR identity keys off the flag, not the boilerplate text.
- A well-formed VODAR is always accepted; when its dataset's DAC has not enabled `VODAR_V1` it proceeds
  to normal DAC review (DACs may vote on VODARs manually), and when the DAC has, it is auto-approved.
- The 12h-expiry anchor (approval time) and mechanism (scope-aware query-time filter on the `vodar`
  marker) are recorded so the deferred implementation is unblocked.
- The DAR form renders VODAR as the first data-use option (always available), hides/clears the Step 2
  primary-purpose inputs when selected, auto-fills and locks the RUS, keeps all Section 3 questions
  required, and blocks submission on any Section 3 Yes.
- The backend independently enforces the same VODAR shape invariant on submit and rejects malformed
  VODARs with 400 and no side effects.
- A `VODAR_V1` RADAR rule auto-approves valid VODARs for DACs that enable it, independent of dataset
  Data Use, gated by the banned-country check, with no manual DAC vote.
- DAC chairs can enable/disable VODAR_V1 from duos-ui with audit.
- VODAR approvals surface through the existing pull-based approved-users query.
- End-to-end and unit/integration tests cover the happy path, the ethical-Yes and edited-RUS negative
  paths, and the rule-disabled fall-through to manual DAC review, and pass in CI.
- Deferred requirements are captured as Future Directions tickets.

## Appendix: SWOT Analysis

### Strengths (internal, positive)

- **Extends rather than forks.** VODAR is a constrained `DataAccessRequest` reusing RADAR, elections,
  `approvedUsers`, and notifications — small surface area, less to build and maintain, faster to ship.
- **Defense-in-depth on the invariant.** Client and server independently enforce the VODAR shape, and
  the server does not trust the client — the right posture for something that gates data access.
- **Code-grounded and actionable.** Tickets reference real classes, fields, files, and endpoints, so
  implementation discovery risk is low and sizing is mostly credible.
- **Disciplined MVP/Future boundary.** Deferred items each carry a rationale; letting non-auto-approvable
  VODARs fall through to normal DAC review keeps the MVP simple rather than building eligibility gating
  that isn't required.
- **Cheap forward-compatibility.** The queryable `vodar` column, decided expiry anchor/mechanism, and
  shared predicates de-risk future goals without premature build.
- **Correctness made structural.** Refactors 1–2 turn "client and server agree" from a hand-maintained
  hope into a single shared definition.

### Weaknesses (internal, negative)

- **View-only is not technically enforced in MVP.** The headline promise (view only, no analysis or
  publication) is an attestation — the downstream TDR/auth API is deliberately unchanged, so a view-only
  user is indistinguishable from a full-access one to the data plane. The core value is not enforced
  where the data actually lives.
- **Strict "all of Section 3 = No"** may push legitimate view-only requesters (e.g. population studies)
  off the fast path; this product rule has not been validated against real requester patterns.
- **Unverified UI surfaces.** Tickets 4/5 rest on assumptions (the DAC rule-management UI exists; the
  status/voting-history components) that were not traced — soft sizing there.
- **Two-repo boilerplate coupling.** A contract test mitigates drift, but two source-of-truth constants
  still exist.
- **No rollout/enablement strategy.** No feature flag, pilot, or rollback plan for turning VODAR on.
- **Conversion loop unmeasured.** The VODAR → full-DAR linkage (the product's premise) is not captured,
  so success cannot be measured.

### Opportunities (external, positive)

- **Drives broader RADAR adoption.** A low-risk, opt-in auto-approval is an easy "yes" for DACs and
  normalizes automation across DUOS.
- **Generalizable "approval scope."** The marker plus an eventual approval projection could scope all
  approvals, paving the way for tiered access (view / analyze / publish) platform-wide.
- **Positions DUOS for scope-aware access control** that Terra/TDR could adopt downstream.
- **Reduces reviewer burden** on low-risk requests — a quantifiable operational win freeing DAC capacity
  for substantive reviews.
- **Free test-coverage uplift** on legacy RADAR/matching code from the characterization-tests-first
  mitigation.

### Threats (external, negative)

- **Downstream dependency for the headline features.** True view-only enforcement and 12h auto-expire
  require Terra/TDR/Sam coordination outside these repos — timelines not controlled by this plan.
- **Governance acceptance is external.** Election-less, attestation-only view access to controlled data
  is a novel access class; IRBs/DACs/data-governance bodies may not accept it, potentially forcing manual
  review anyway. The product-owner paper argues the case, but institutional buy-in is not guaranteed.
- **Live data-governance risk during the enforcement gap.** For as long as MVP runs before downstream
  scope lands, consumers can over-grant analysis access to view-only users — a real production exposure.
- **Regression blast radius.** Refactoring `secondaryConditionChecks` and (later) approval derivation
  touches code that decides existing GRU/HMB approvals and the TDR-consumed approved-users contract — a
  bug there affects production access, not just VODAR.
- **In-flight collision.** Recent work on Data Use primary consistency and the AuthUser → DuosUser
  migration touches overlapping surfaces (matching, DAR, approvals) — merge and coordination risk.
- **Cross-repo release ordering.** duos-ui and consent deploy independently; the boilerplate and
  payload/validation contracts must ship in a compatible order or submissions break.

### Synthesis

The dominant tension: the plan is strong on internal engineering rigor but its headline value
proposition depends on enforcement it defers to external teams (Weaknesses → Threats). The single most
consequential decision is therefore framing, not code. Either pull the downstream scope/enforcement
forward if "view only" must be technically real at launch, or explicitly reframe MVP as
"attestation-based view access, enforcement to follow" so stakeholders and governance are not surprised.
The secondary strategic risk is regression in shared approval code, well-mitigated by the
characterization-tests-first rule but worth flagging to reviewers.
