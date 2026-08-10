# Data Use Primary Consistency Plan

## Status

Proposed.

## Objective

Align dataset primary Data Use validation and automated matching around one definition in Consent,
while preserving the current UX rule and handling legacy records deliberately.

## Background and Scope

The originating PR comment confirms that the current UX and
`StudyRegistrationRequestValidator.validateDataUseConsistency` behavior are correct: a new dataset
has either open access with no controlled-access primary, or exactly one primary Data Use for
controlled/external access.

The inconsistency is historical drift:

- registration rules moved from previous UI logic into Consent and enforce a single primary;
- matching rules moved from Ontology into Consent and still accommodate overlapping primary fields;
- the two implementations do not share a canonical definition; and
- `DataUse.other` is not explicitly evaluated by `DataUseMatcherV4`.

This work must not loosen registration to permit multiple primaries. It should make single-primary
the rule for current writes and make legacy matching behavior explicit.

The relevant primary categories are:

- General Research Use (GRU)
- Health/Medical/Biomedical use (HMB)
- Populations, Origins, or Ancestry use (POA)
- non-empty Disease-Specific use (DS)
- nonblank Other primary text

OPEN, CONTROLLED, and EXTERNAL are access-management values, not `DataUse` categories.

## Current Behavior

### Registration

The validator counts `accessManagement: open` together with GRU, HMB, POA, DS, and Other and
requires exactly one selection. This runs for all consent groups during registration create and for
new consent groups added during study update. Existing consent groups are not revalidated because
that update path does not replace their persisted `DataUse`.

Two admin paths can replace `DataUse` without the same invariant:

- `PUT /api/dataset/{id}/datause`
- dataset-to-study conversion when its payload contains `dataUse`

### Matching

`DataUseMatcherV4` checks GRU, HMB, POA, and DS fields directly. Multi-primary behavior is therefore
resolved by branch order. Some branches also encode the valid permission hierarchy, so they cannot
simply be removed after introducing single-primary validation.

Persisted matches identify their dataset through `match_entity.consent`, a text value populated from
the formatted dataset alias (`DUOS-######`). This couples an internal relationship to a presentation
identifier, prevents a database foreign key to `dataset`, and forces consumers to reproduce alias
formatting in joins. Alias allocation is also implemented as `MAX(alias) + 1` without a database
uniqueness constraint, which is unsafe for concurrent creation. These are separate scalability and
referential-integrity concerns from primary Data Use consistency, but the audit must expose them and
the follow-up in Ticket 5 must replace this design.

Other-only datasets are an important explicit case. Because `DataUse.other` is not checked by the
matcher, the result currently depends on the research purpose and may be APPROVE, DENY, or ABSTAIN.
The proposed behavior is an explicit ABSTAIN because free-text Other cannot be matched safely.

Match results are persisted with an algorithm version. A matcher or dataset change does not update
existing match rows automatically.

### duos-ui

Consent will own the canonical dataset classification, but duos-ui independently implements related
behavior and does not consume the Consent classifier as a shared library:

- `ConsentGroupAddEdit` enforces primary mutual exclusivity through its radio widget and reset
  handler, while its validation only asserts that at least one primary is present for non-open data.
- `selectedPrimaryGroup` and `processDefinedLimitations` use first-match precedence rather than
  detecting noncanonical multiple-primary values.
- `BucketUtils` reimplements Other/modifier abstention and uses an `algorithmVersion === 'v3'` gate
  to decide when persisted match results can be trusted by the DAC voting UI.
- `processOtherInDataUse` classifies both primary and secondary Other as modifiers for display.
- the Library grid tolerates multiple primary terms by rendering two chips plus an overflow count.

These behaviors are valid compatibility mechanisms in some contexts, but they can drift when
Consent adds the canonical classifier and a new matcher version. Ticket 6 must reconcile the UI's
classification, abstention, error handling, and tests with the final Consent semantics. The UI may
retain defensive display of legacy multi-primary data when that choice is explicit and tested.

### Legacy data

Historical migrations contain DS+POA and `other`+recognized-primary combinations. The model now
distinguishes `DataUse.other` from `DataUse.secondaryOther`, but older values may not reliably
reflect that distinction. Legacy records must be measured before matching or migration behavior is
changed globally.

## Recommended Decisions

| Question | Decision |
| --- | --- |
| Should registration permit multiple primaries? | No. Preserve the current UX-aligned single-primary rule. |
| Does EXTERNAL require a primary use? | Yes. External access does not require a DUOS DAC, but it still requires one primary Data Use. |
| What does `DataUse.other` mean for current registrations? | Other primary. `secondaryOther` remains the separate secondary field. |
| What should matching do with Other only or no primary? | ABSTAIN with a specific manual-review rationale. |
| What should matching do with multiple primaries? | ABSTAIN with a specific manual-review rationale for every combination, including legacy combinations. |
| Should legacy records be normalized automatically? | Only where the audit produces a deterministic, domain-approved mapping. Never select the first populated field. |
| How should datasets be referenced internally? | By `dataset.dataset_id` foreign keys. Keep the formatted alias as a public/display identifier only, allocated by a database sequence and protected by a unique constraint. |

## Implementation Approach

Use a small domain classifier shared by validation and matching:

- `SINGLE(category)`
- `NONE`
- `MULTIPLE(categories)`

For a `DataUse` object, a Boolean primary counts only when `true`, DS counts only when non-empty,
and Other counts only when nonblank. Secondary fields do not count. A null model value is treated as
unsupported/`NONE` at the matcher boundary.

Keep access-management policy outside the `DataUse` classifier:

- OPEN requires zero controlled-access primaries.
- CONTROLLED requires exactly one primary.
- EXTERNAL requires exactly one primary and no DUOS DAC.
- Missing access management preserves its current controlled-equivalent behavior.

The classifier applies to dataset restrictions, not research-purpose `DataUse`, which may combine a
purpose with valid modifiers.

## Jira-Ready Tickets

There are three core Consent tickets, one conditional ticket, one required architecture follow-up,
and one required duos-ui alignment ticket. The conditional ticket should be created only when the
audit shows that Data Use normalization or persisted-match reprocessing is necessary. Ticket 5
addresses alias scalability independently and must not be folded into the matcher behavior change.
Ticket 6 closes the cross-repository consistency gap after Tickets 2 and 3 finalize the contract.

### Ticket 1: Audit persisted dataset primary Data Use shapes

**Issue type:** Spike

**Suggested size:** 3 points

**Summary**

Audit persisted dataset Data Use shapes and approve a disposition for legacy zero-, Other-, and
multi-primary records.

**Description**

Measure existing records before changing matcher behavior. Report counts for null/empty/unparseable,
NONE, each SINGLE category, and each observed MULTIPLE combination. Cross-tabulate noncanonical
records by access management and determine whether they are referenced by DARs or persisted matches.

For records containing `other`, an authorized domain owner should review a small sample without
copying raw text or dataset identifiers into Jira.

**Implementation notes**

The following read-only query is a starting point for retrieving the required inputs. Run it only in
an approved restricted environment because `data_use` can contain free text. Do not attach its raw
output to Jira.

```sql
WITH access_management AS (
  SELECT
    dataset_id,
    STRING_AGG(DISTINCT LOWER(property_value), ',' ORDER BY LOWER(property_value))
      FILTER (WHERE schema_property = 'accessManagement') AS access_management
  FROM dataset_property
  GROUP BY dataset_id
),
dar_usage AS (
  SELECT dataset_id, COUNT(DISTINCT reference_id) AS dar_count
  FROM dar_dataset
  GROUP BY dataset_id
),
match_usage AS (
  -- Transitional reconciliation of the current text reference. Do not copy this
  -- alias-derived join into application code; Ticket 5 replaces it with dataset_id.
  SELECT d.dataset_id, COUNT(*) AS match_count
  FROM match_entity m
  JOIN dataset d
    ON m.consent =
      'DUOS-' || LPAD(d.alias::text, GREATEST(6, LENGTH(d.alias::text)), '0')
  GROUP BY d.dataset_id
)
SELECT
  d.dataset_id,
  'DUOS-' || LPAD(d.alias::text, GREATEST(6, LENGTH(d.alias::text)), '0')
    AS dataset_identifier,
  COALESCE(am.access_management, 'missing') AS access_management,
  d.data_use,
  COALESCE(du.dar_count, 0) AS dar_count,
  COALESCE(mu.match_count, 0) AS match_count
FROM dataset d
LEFT JOIN access_management am ON am.dataset_id = d.dataset_id
LEFT JOIN dar_usage du ON du.dataset_id = d.dataset_id
LEFT JOIN match_usage mu ON mu.dataset_id = d.dataset_id
ORDER BY d.dataset_id;
```

Parse and classify `data_use` outside SQL so invalid JSON can be counted without causing a JSON cast
failure. The audit utility must not log the raw JSON. Classification rules:

- null database value → `NULL`
- empty string → `EMPTY`
- parsing failure → `UNPARSEABLE`
- parsed value with zero primary categories → `NONE`
- one category → `SINGLE(category)`
- more than one → `MULTIPLE(sorted categories)`

The `match_usage` CTE deliberately isolates the legacy alias-derived join needed to reconcile the
current schema. It is not the target design. Before relying on match counts, separately report null,
noncanonical, duplicate-mapping, and unmatched `match_entity.consent` values; do not silently omit
them. Ticket 5 backfills and validates a real `dataset_id` relationship before application reads or
writes stop using the legacy text column.

**Acceptance criteria**

- A redacted table reports count and percentage for every observed classification.
- Results are cross-tabulated by OPEN, CONTROLLED, EXTERNAL, and missing access management.
- The audit records how “active dataset” was defined; if all persisted datasets are used, that is
  stated explicitly.
- Redacted counts identify how many noncanonical datasets have DAR or persisted-match references.
- A reconciliation count identifies match rows whose current `consent` text maps to zero or more
  than one dataset; mapped plus unresolved rows equals the total persisted-match count.
- A domain owner classifies sampled historical `other` values as primary, secondary, mixed, or
  indeterminate.
- Every observed noncanonical shape receives one proposed disposition: ABSTAIN/manual review,
  deterministic normalization, or curator-assisted normalization. Every MULTIPLE shape ABSTAINS
  unless it is normalized before matching.
- The need for Ticket 4 is decided and recorded.
- No raw Other text, Data Use JSON, or production dataset identifiers are committed or copied into
  Jira/general logs.

**Out of scope**

- Updating production data.
- Changing matcher decisions.
- Reopening elections or changing historical votes.

---

### Ticket 2: Add canonical primary classification and enforce it on Data Use writes

**Issue type:** Story

**Suggested size:** 5 points

**Dependencies:** May begin with Ticket 1; incorporate audit findings before rollout

**Summary**

Add a shared dataset primary Data Use classifier and enforce the current UX rule on supported write
paths.

**Description**

Replace the hand-counted registration definition with a reusable classifier for GRU, HMB, POA, DS,
and OTHER. Use the same vocabulary in registration validation and at the service boundary for admin
Data Use replacement and dataset-to-study conversion.

This is a refactor toward one source of truth, not a change to payloads accepted by the current UX.

**Implementation notes**

- Prefer an enum plus an immutable classification result.
- Keep access management outside the `DataUse` classifier.
- Validate before translation, persistence, audit insertion, or Elasticsearch synchronization.
- Preserve create/update validation scope for existing consent groups.
- Reject invalid JSON/JSON `null` without echoing the submitted payload.
- Remove the existing raw JSON echo from `DataUseParser` parse-failure logging.
- Update OpenAPI descriptions for the admin update and conversion endpoints.

**Acceptance criteria**

- The classifier covers null, NONE, every SINGLE category, and representative MULTIPLE values.
- OPEN with no controlled primary is valid; OPEN with a controlled primary is invalid.
- CONTROLLED, EXTERNAL, and missing access management require exactly one primary.
- Registration create and new consent groups added during update preserve current UX behavior.
- Admin Data Use replacement and dataset-to-study conversion enforce the same rule.
- Existing consent-group update behavior is unchanged.
- Invalid writes return 400 and cause no database, audit, translation, Elasticsearch, match, or vote
  changes.
- Valid writes preserve existing audit, translation, and Elasticsearch behavior.
- Client-facing validation message wording and the newline-joined response format consumed by
  duos-ui remain stable unless an intentional contract change is approved and coordinated with
  duos-ui.
- Tests remain strict-stubbing compliant; no Mockito `lenient()` stubbing is introduced.

**Tests**

- Unit tests for classifier inputs and category combinations.
- Parameterized access-management × primary-shape validator tests.
- Resource/service tests for registration, admin update, and conversion paths.
- Regression tests that rejected JSON is not included in errors/logs.
- Contract tests for the newline-joined validation response format consumed by duos-ui.

**Out of scope**

- Changing matcher decisions.
- Migrating existing records.
- Applying the dataset invariant to research-purpose Data Use.

---

### Ticket 3: Make legacy and unsupported matcher behavior explicit

**Issue type:** Story

**Suggested size:** 5 points

**Dependencies:** Tickets 1 and 2

**Summary**

Use canonical primary classification at the matching boundary and explicitly handle Other, missing,
and legacy multi-primary dataset Data Use.

**Description**

Classify the dataset before invoking the existing V4 cases. Preserve the existing matcher for
`SINGLE(GRU/HMB/POA/DS)`. All MULTIPLE combinations ABSTAIN, including historical combinations;
Ticket 1 determines whether any records should be normalized before they reach matching.

Do not remove valid hierarchy branches from `matchDiseases`, `matchHMB`, or `matchMDS`; they remain
necessary for single-primary dataset-to-purpose matching.

**Required behavior**

| Dataset classification | Behavior |
| --- | --- |
| `SINGLE(GRU/HMB/POA/DS)` | Delegate to existing V4 behavior. |
| `SINGLE(OTHER)` | ABSTAIN with an Other/manual-review rationale. |
| `NONE` or null | ABSTAIN with a missing/unsupported-primary rationale. |
| Any `MULTIPLE` | ABSTAIN with a multiple-primary/manual-review rationale. |

**Implementation notes**

- A small V5 wrapper around unchanged V4 cases is preferable if persisted results need a distinct
  algorithm version.
- Ensure a preflight ABSTAIN cannot be overridden by result aggregation.
- Review DAC automation rules while applying the classifier. In particular, GRU+HMB must not satisfy
  a GRU-only or HMB-only automatic-approval rule. Include the guard here if affected combinations
  exist; otherwise record why no automation change is needed.
- Do not include Other text in rationales or logs.
- Record whether persisted matches need Ticket 4 reprocessing.

**Acceptance criteria**

- Other-only and NONE/null datasets ABSTAIN for GRU, HMB, POA, DS, and MDS purposes.
- Every MULTIPLE combination ABSTAINS, including each observed legacy combination.
- Existing single-primary results and rationales remain unchanged.
- `secondaryOther` alone does not classify as Other primary.
- New abstain cases have specific, user-appropriate rationales.
- The stored algorithm version distinguishes changed semantics when persisted rows are affected.
- DAC automation cannot auto-approve a noncanonical shape, or the audit/test evidence demonstrates
  that no applicable rule can do so.

**Tests**

- Parameterized dataset-classification × research-purpose matrix.
- Existing V4 matcher regression suite.
- Characterization tests showing that every observed legacy MULTIPLE combination ABSTAINS.
- DAC automation regression for GRU+HMB and other observed noncanonical shapes.

**Out of scope**

- Rewriting the valid GRU/HMB/POA/DS matching hierarchy.
- Automatically interpreting Other free text.
- Updating persisted dataset or match records.

---

### Ticket 4: Normalize legacy records and reprocess affected matches, if required

**Issue type:** Conditional Story

**Suggested size:** Estimate after Ticket 1

**Dependencies:** Tickets 1–3

**Create only when**

The audit identifies records with an approved normalization mapping, or persisted match rows that
must be recomputed under the explicit matcher policy.

**Summary**

Normalize approved legacy Data Use records and recompute affected persisted matches.

**Implementation notes**

- Never choose a primary from branch order or free-text keywords.
- Use a deterministic Liquibase migration only for mappings approved for every affected record;
  otherwise use a controlled curator-assisted process.
- Preserve recoverable original values and approved audit evidence.
- Keep `translated_data_use` and Elasticsearch synchronized with migrated JSON.
- Select affected DAR purposes through `dar_dataset` and use restartable, failure-isolated match
  reprocessing.
- Do not rewrite elections, final votes, or historical automation votes.

**Acceptance criteria**

- Only approved records are changed, using synthetic-data-tested mappings.
- The migration is idempotent or safely restartable.
- Pre/post classification counts reconcile with updated records.
- `translated_data_use`, dataset audit evidence, and Elasticsearch are consistent after migration.
- Affected persisted matches are recomputed under the approved algorithm version.
- Reprocessing reports processed, skipped, failed, and retried purpose counts.
- APPROVE/DENY/ABSTAIN changes are reviewed, especially approvals becoming abstentions.
- No raw Other text or production identifiers appear in general logs or Jira.
- Rollback/correction instructions and operational ownership are documented.

**Close as unnecessary when**

No active record needs normalization and no persisted match needs reprocessing.

---

### Ticket 5: Replace alias-derived internal dataset references

**Issue type:** Technical Story

**Suggested size:** 8 points

**Dependencies:** Ticket 1 supplies reconciliation evidence; implementation may otherwise proceed
independently of Tickets 2–4

**Summary**

Reference datasets by foreign key in persisted matches and make public alias allocation
concurrency-safe.

**Description**

Stop using the formatted dataset alias as the internal identity of a dataset. Add
`match_entity.dataset_id` referencing `dataset.dataset_id`, use that key for match persistence and
joins, and derive `DUOS-######` only when producing API or user-facing output.

Preserve aliases as public identifiers, but allocate them with a database sequence/identity rather
than `MAX(alias) + 1` and enforce uniqueness in the database. This ticket does not replace aliases
in external contracts and does not make the internal numeric `dataset_id` public.

**Implementation notes**

- Audit null, duplicate, noncanonical, and conflicting aliases before adding constraints.
- Add a nullable `match_entity.dataset_id`, its foreign key, and an index first.
- Backfill only exact, unambiguous legacy mappings. Quarantine or explicitly resolve unmatched and
  ambiguous `match_entity.consent` values; never guess from numeric parsing alone.
- Change match creation to pass the selected dataset's `dataset_id`. Join to `dataset` when the
  public identifier is needed in a response.
- During rollout, use an explicitly bounded compatibility phase (for example, dual-write plus
  comparison metrics), then make `dataset_id` non-null and replace the current
  `(purpose, consent)` uniqueness rule with `(purpose, dataset_id)`.
- Rename or remove `match_entity.consent` after all consumers are migrated; do not leave two
  authoritative dataset identities indefinitely.
- Initialize the alias sequence above the audited maximum, add a unique constraint, and replace the
  DAO's `MAX(alias) + 1` allocation. Treat sequence gaps as valid.
- Inventory other alias-based internal lookups separately. Migrate relational ownership and joins
  to `dataset_id`; keep lookup by public alias only at API/integration boundaries.

**Acceptance criteria**

- Every active match row has exactly one valid `dataset_id` foreign key, or an approved exception is
  documented and isolated before the non-null constraint is applied.
- Match inserts and internal joins do not format, parse, or compare `DUOS-` strings.
- Existing APIs continue returning the same canonical public dataset identifiers.
- Concurrent dataset creation cannot allocate duplicate aliases, and the database enforces alias
  uniqueness.
- Match uniqueness is enforced by `(purpose, dataset_id)`.
- Backfill totals reconcile: migrated plus explicitly unresolved rows equals the pre-migration row
  count.
- Rollout and rollback tests cover legacy reads, dual-write comparison (if used), constraint
  validation, and public identifier compatibility.

**Out of scope**

- Replacing the `DUOS-######` public identifier contract.
- Reusing deleted aliases or requiring gapless alias numbering.
- Changing Data Use classification or matcher decisions.

---

### Ticket 6: Align duos-ui with the canonical Data Use classification

**Issue type:** Story

**Suggested size:** 5 points

**Dependencies:** Tickets 2 and 3

**Summary**

Update duos-ui's parallel Data Use classification and DAC-voting abstain logic to match the
canonical Consent definition, and close the UI test gaps around the exactly-one-primary invariant.

**Description**

duos-ui does not consume Consent's classifier as a shared library. Several UI paths reimplement
primary selection, first-match classification, and abstention behavior. Reconcile those paths with
the classifier vocabulary, MULTIPLE policy, and algorithm version delivered by Consent so future
drift is detected in tests rather than production.

**Implementation notes**

- Update `BucketUtils` (`isOther`, `shouldAbstain`, `AbstainDataUseCodes`, and the current
  `algorithmVersion === 'v3'` gate) and its `CollectionAlgorithmDecision` consumer to recognize the
  algorithm version from Ticket 3 and reflect its ABSTAIN dispositions for Other-only, NONE/null,
  and MULTIPLE datasets.
- Reconcile the hardcoded precedence in `selectedPrimaryGroup` and `processDefinedLimitations` with
  the canonical classifier. If display ordering intentionally differs from classification, name and
  test that distinction rather than treating the first category as canonical.
- Decide whether to correct `processOtherInDataUse` so primary Other is displayed as a permission
  while secondary Other remains a modifier, or create a separately owned follow-up ticket.
- Decide whether the Library grid should retain its multi-primary overflow rendering for audited
  legacy records. Do not remove defensive rendering before Ticket 1 and any Ticket 4 migration are
  resolved.
- Confirm whether a curator-assisted Ticket 4 process needs operator UI. Current duos-ui code does
  not call the admin Data Use replacement or dataset-to-study conversion endpoints, so a backend-only
  process is acceptable only when explicitly approved.
- Preserve handling of Consent's newline-joined validation error response on registration create
  and update unless both repositories coordinate an intentional contract change.

**Acceptance criteria**

- The DAC voting UI correctly reflects ABSTAIN for Other-only, NONE/null, and every MULTIPLE shape
  under the algorithm version introduced by Ticket 3.
- A stale or unrecognized `algorithmVersion` is handled explicitly and is neither silently trusted
  nor silently hidden.
- A test exercises `ConsentGroupAddEdit` primary mutual exclusivity: selecting a new primary clears
  the previous selection.
- Tests simulate a backend 400 Data Use consistency violation on registration create and update and
  verify that every newline-delimited violation is rendered.
- Any intentional UI-versus-Consent ordering or legacy-display difference is documented and tested.
- The primary Other display classification has either been corrected or captured in an accepted,
  owned follow-up ticket.
- The need for an operator UI for Ticket 4 is decided and recorded.

**Out of scope**

- Redesigning the duos-ui Data Use forms beyond consistency work.
- Building an operator UI unless the Ticket 4 decision requires it.

## Test Matrix

At minimum, cover:

- OPEN with no primary and OPEN combined with a primary;
- CONTROLLED, EXTERNAL, and missing access management with zero, one, and multiple primaries;
- each SINGLE category;
- blank Other and empty DS;
- Other only, NONE/null, and representative MULTIPLE datasets against GRU, HMB, POA, DS, and MDS
  purposes;
- every observed legacy MULTIPLE combination; and
- admin update and conversion paths.

Assert both match result and rationale. Existing single-primary matching expectations remain the
regression source of truth.

## Explicitly Out of Scope

- Allowing new multi-primary datasets.
- Validating that DS terms resolve in the ontology beyond the existing non-empty-list rule.
- Applying dataset primary rules to research purposes.
- Rewriting the established valid matching hierarchy.
- Automatically classifying free-text Other restrictions.
- Reopening elections or changing historical final votes.

## Definition of Done

- Consent has one tested dataset primary classification definition.
- All supported Data Use write paths enforce the current UX-aligned rule.
- Matcher behavior is explicit for SINGLE, Other, NONE/null, and MULTIPLE values.
- Valid single-primary matching behavior remains unchanged.
- Legacy data has an approved compatibility, manual-review, or normalization disposition.
- Conditional migration/reprocessing is completed or explicitly unnecessary.
- Matches reference datasets through an enforced `dataset_id` foreign key, and aliases are uniquely,
  concurrency-safely allocated for presentation use.
- duos-ui's Data Use classification, validation-error rendering, and DAC-voting abstain logic have
  been reviewed against the canonical Consent definition and either updated to match or explicitly
  signed off as an intentional, tested difference.
- Relevant tests pass and API documentation is updated where behavior changes.
