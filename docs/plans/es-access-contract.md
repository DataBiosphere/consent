# Elasticsearch Access Contract — Ticket A-2

The formal access contract for dataset search: every access dimension, what it is allowed to do,
and which fields each class of caller may see. Companion to
[`elasticsearch-service-duos-ui-usage.md`](elasticsearch-service-duos-ui-usage.md) (the ticket plan)
and [`es-security-capability-record.md`](es-security-capability-record.md) (Ticket A-1, what the
clusters can enforce).

This is the document B-1 (`accessPolicy` schema), C-1 (auth context resolver), D-3 (DLS/FLS
generator), and E-2/E-3 (fallback filter and allowlist) are implemented against. Where it says
**DECISION**, the question is settled and downstream tickets may rely on it. Where it says **OPEN**,
it needs a named owner's sign-off and is listed in §E — those are policy choices, not engineering
ones, and this document deliberately does not invent them.

## Why this is not blocked on the rest of A-1

A-1 has production measured and dev/staging outstanding. Those rows choose the *enforcement
mechanism* — native DLS/FLS (Epic D) where licensed, mediated queries and response filtering
(Epic E) where not. They do not change the contract. The whole point of writing this separately is
that the answer to "who may see what" must be identical under either mechanism, or the two
enforcement paths will drift apart and the fallback will become a hole. So this contract is stated
in mechanism-neutral terms and each rule is annotated with how it lands in both.

## DECISION 1 — Restricted documents are invisible, not redacted

A caller who is not authorized for a dataset does not see a redacted version of it. The document is
absent from their results, and absent from the total hit count.

The alternative — return every document and blank the restricted fields — was rejected for three
reasons:

1. **It leaks by counting.** A result total, a facet count, or an aggregation bucket that includes
   documents the caller may not read tells them those datasets exist and how many there are.
2. **It cannot be expressed in native FLS.** See Decision 2: field grants do not vary per document,
   so "visible but redacted for restricted documents only" is not a thing a single search can do.
3. **It changes current behavior.** Today `DatasetService.verifyPublicVisibilityAccess` drops
   unauthorized datasets from the list entirely (`DatasetService.java:147-171`). Invisibility
   preserves that; redaction would be a new, more permissive posture adopted by accident.

Landing in each mechanism: **Epic D** — a DLS `query` in the role descriptor, so the cluster filters
before scoring and counting. **Epic E** — a mandatory `filter` clause injected into the query's
boolean context, which must be non-removable by the caller-supplied DSL (E-2's sanitization is what
makes that true).

## DECISION 2 — Search serves one field bundle to every caller

Two designs are ruled out first, because both were in the plan and both are unsound.

### A per-document FLS marker cannot work

`fieldAccessProfile` as specified in B-1/B-2 — a per-document marker naming the FLS bundle that
applies to that document — **cannot work, and must be removed from the design.**

Elasticsearch field-level security is declared in a role descriptor's index privileges:

```json
{"indices": [{"names": ["dataset"], "privileges": ["read"],
              "field_security": {"grant": ["datasetName", "study.studyName"]}}]}
```

The grant is bound to the *index privilege*, evaluated when the request is authorized, and applied
uniformly to every document that privilege matches. There is no point in the search lifecycle at
which the cluster reads a field off a hit and re-selects a field grant for it. A per-document marker
therefore has nothing to bind to.
([Elastic: controlling access at document and field level](https://www.elastic.co/docs/deploy-manage/users-roles/cluster-or-deployment-auth/controlling-access-at-document-field-level))

### Why a per-*caller* privileged bundle does not rescue it either

The obvious repair — keep one bundle per request, but pick `privileged` when the caller is a creator
or custodian of *something* — is also wrong, and it fails in a way worth spelling out because it is
easy to talk yourself into.

Creator and custodian privilege is **document-scoped**. The DLS filter is a disjunction: a custodian
of restricted dataset X receives X *and* every public dataset Y, because `publicVisibility = true` is
one of the `should` clauses. A request-wide `privileged` grant would then project privileged fields
out of all those unrelated public Y documents — datasets the caller has no relationship to
whatsoever. Being privileged on one document would buy privileged fields on every document in the
result set.

So the constraint is real and has no clever workaround: **native FLS cannot express document-scoped
field access.** Anything that needs it must be built somewhere else:

| Approach | Verdict |
| --- | --- |
| Separate indices per profile | Does not even apply. The profile here is caller-*relative* ("am I the custodian of this one?"), and an index split can only encode document-absolute properties. |
| Two searches — privileged-scoped and public-scoped — merged server-side | Technically possible; breaks scoring, paging, and aggregations across the merged set. |
| Application-owned projection per document after retrieval | Works, and is genuinely document-scoped. It is what Epic E does, and it is the upgrade path if the requirement ever appears. Doing it in the native path too would forfeit Epic D's reason to exist. |

**What we do instead: DECISION 2 — search serves one field bundle to every caller.**

The field grant does not vary at all — not by document, not by caller, not for admins. DLS varies
(who sees which documents); FLS does not. That is exactly the shape native FLS can enforce
correctly, so there is nothing left to get wrong.

This is only a sound decision if no field genuinely needs document-scoped exposure through search,
so that was checked against the consumers rather than assumed (§B.0). It holds: every field the
catalog publishes through search is published to *all* authenticated callers today — duos-ui's
dataset table renders PI name, custodian emails, and data location unconditionally, and its
client-side filter matches on submitter and DAC emails. There is no existing privileged-in-search
tier to preserve. The fields that are genuinely internal are consumed by nobody through search and
simply leave the projection.

Consequently the classification in §B has **two** tiers, not three: served by search, or not served
by search. Privileged, document-scoped data continues to reach privileged callers through the
per-dataset endpoints, which already do document-scoped checks in `DatasetService` and are not
affected by this contract.

**If a future requirement does need a privileged field in search results**, this decision has to be
reopened, not worked around: the answer is application projection (Epic E's mechanism, applied to
the native path as well), and the cost is that Epic D stops being sufficient on its own.

## DECISION 3 — The contract preserves today's authorization; every expansion is explicit

The default for every dimension is **PRESERVE**: reproduce what the application does today, exactly,
including the parts that look accidental. Dimensions that would *grant new access* (DAC membership,
institution, policy tags) are **DEFER**red — schema may be reserved for them, but no enforcement
path may consult them until they are signed off in §E.

This is the rule that keeps a security refactor from becoming a silent authorization change. The
matrix below states it per dimension so no implementer has to infer it.

---

## §A — Access dimension matrix

"Current effect" is what the application does **today**, from
`DatasetService.verifyPublicVisibilityAccess` / `canReadStudy` / `isCreatorOrCustodian`
(`DatasetService.java:147-237`). "Level" is document (who sees the dataset at all) or field (which
bundle they get). Under Decision 2 the field bundle is constant, so **every row's Level is
Document** — no dimension varies field access. The column is kept to make that explicit rather than
implied.

| # | Dimension | Source of truth | Current effect on read | Level | Persistent backing | Contract |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | ADMIN role | `user_role` → `UserRoles.ADMIN` | Full bypass: every dataset, no filtering | Document **only** | Yes — `user_role` table | **PRESERVE.** DLS filter is `match_all`. Field grant is the same as everyone else's — admin is not a projection bypass (§B.7). |
| 2 | Study `publicVisibility = TRUE` | `study.public_visibility` | Readable by any authenticated caller | Document | Yes — column | **PRESERVE.** |
| 3 | Study `publicVisibility = FALSE` | `study.public_visibility` | Readable only via #1, #6, #7, or #8 | Document | Yes — column | **PRESERVE.** |
| 4 | Study `publicVisibility = NULL` | `study.public_visibility` | **Unreachable** — the column is `NOT NULL`; the NULL seen in summary rows is the LEFT JOIN for #5 (§A.1) | Document | n/a | **RESOLVED.** Filter treats a null on a study-bearing document as not-public (fails closed). |
| 5 | Dataset has no study (`studyId IS NULL`) | `dataset.study_id` | Readable by everyone — "can't verify visibility, so return the dataset" | Document | Yes — column | **PRESERVE**, and see OPEN-2: this is fail-open. |
| 6 | Dataset creator | `dataset.create_user_id` = `user.user_id` | Readable | Document | Yes — column | **PRESERVE.** Distinct from #7. |
| 7 | Study creator | `study.create_user_id` = `user.user_id` | Readable | Document | Yes — column | **PRESERVE.** Distinct from #6. |
| 8 | Study data custodian | `study_property['dataCustodianEmail']` (JSON array) contains `user.email` | Readable | Document | **Partial — see §C.** Unstructured JSON in a property bag. | **PRESERVE**, with the matching rules in §A.2 made explicit. |
| 9 | DAC membership | `dac_user` via `DacDAO` | **No dataset read access today** | — | Yes — table | **DEFER.** Reserve `accessPolicy.dacId`; no enforcement path reads it. OPEN-3. |
| 10 | DAC chair | `dac_user` role | **No dataset read access today** | — | Yes — table | **DEFER.** OPEN-3. |
| 11 | `dacApproval` | `dataset.dac_approval` | **No read effect today** — a display/filter attribute the UI applies client-side (G-1) | — | Yes — column | **DEFER.** Indexing it as an `accessPolicy` field invites it to be read as authorization; it is not. OPEN-4. |
| 12 | Institution | `user.institution_id`; no dataset-side counterpart | **No dataset read access today** | — | User side only; **no dataset-to-institution mapping exists** | **DEFER.** Needs A-3 storage decision before it can mean anything. OPEN-5. |
| 13 | Policy tags | — | **Does not exist** | — | **None** | **DEFER.** OPEN-5. |
| 14 | Explicit principal allowlist | — | **Does not exist** | — | **None** | **DEFER.** OPEN-5. |

Rows 9–14 are the ones the review flagged: B-1's `AccessPolicyTerm` lists them as fields alongside
the dimensions that do carry authorization today, with nothing recording that four of them grant
nothing. Reserving schema for them is fine. Wiring them into a DLS filter without sign-off would
grant DAC members, institution peers, and tag holders read access to non-public datasets that they
do not have today.

### §A.1 — `publicVisibility = NULL` — RESOLVED by the schema, not by policy

This looked like an unresolvable policy question and was briefly recorded as one. It is not: the
database settles it, and the apparent disagreement between the two code paths is unreachable.

**`study.public_visibility` is `NOT NULL`** (`changelog-consent-2023-04-20-create-study.xml:22-24`,
never relaxed by a later changeset). A persisted study cannot have a null visibility, so
`canReadStudy` — which only ever runs against a loaded `Study` — cannot observe one.

The NULL that *does* occur is in the summary list, and it comes from a join, not from a study:

```sql
FROM dataset d LEFT JOIN study s ON s.study_id = d.study_id   -- DatasetDAO.java:119-129
```

`summary.public_visibility()` is NULL exactly when the dataset has **no study** — which is §A row 5,
a case both paths already agree on. Trace it: `Boolean.TRUE.equals(null)` is false, the creator
checks fail for a stranger, `summary.study_id() != null` is false, and control reaches the final
`else`, which adds the dataset. Readable. The single-dataset path returns the dataset for the same
reason (`studyId == null` → return). **The two paths agree.**

**DECISION**: the contract has one filter, and it is the two-clause form already implied by rows 2–5:
`publicVisibility` is true, *or* the dataset has no study, *or* the caller is privileged on it. A
null or missing `publicVisibility` on a document that *does* have a study is unreachable, and the
filter must therefore treat it as **not public** — the unreachable branch fails closed rather than
open. B-3's "no study" marker (§D) is what keeps row 5 working under that rule, rather than leaning
on a null to mean "visible."

This unblocks B-3, D-3, E-2, and G-1, which were waiting on a decision that did not need to be made.

<details>
<summary>The divergence that prompted the question, kept for the record</summary>

The two paths do read a null differently, and if the `NOT NULL` constraint were ever dropped they
would diverge in production. C-2 should normalize them while it is consolidating the predicates.</details>

**Single-dataset path** — `canReadStudy` (`DatasetService.java:193-205`):

```java
if (!Boolean.FALSE.equals(study.getPublicVisibility())) {
  return true;   // NULL is readable by anyone
}
```

**Summary-list path** — `verifyPublicVisibilityAccess(List<DatasetStudySummary>, User)`
(`DatasetService.java:147-171`):

```java
if (Boolean.TRUE.equals(summary.public_visibility())) {   // NULL is NOT public here
  ...
} else if (...creator checks...) {
} else if (summary.study_id() != null) {
  // falls through to a creator/custodian check, which a stranger fails
}
```

Were a null ever to reach these, a caller who is neither creator nor custodian **could** read the
study through `findMinimalDatasetByIdentifier` and **could not** see it in a dataset summary list.
The `NOT NULL` constraint is the only thing preventing that, which is a thin guarantee to leave
implicit — hence the normalization ask above.

### §A.2 — Identity matching rules

These are the details that decide whether a re-implementation actually reproduces current behavior.
All four are **PRESERVE**, meaning the DLS filter and the fallback must match today's semantics
exactly — but three of them are also latent defects, flagged as OPEN-6.

| Rule | Current behavior | Note |
| --- | --- | --- |
| Creator matching (#6, #7) | Integer user ID equality | Sound. |
| Custodian matching (#8) | String equality on email: `user.getEmail().equals(custodian.trim())` | **Case-sensitive**, and only the *stored* value is trimmed — the user's email is not. `Alice@x.org` does not match a stored `alice@x.org`. |
| Custodian property parse | `Gson.fromJson(prop.getValue().toString(), List<String>)` | Throws on malformed JSON; no defined behavior for a non-array value. |
| Missing custodian property | `Optional.empty()` → not a custodian | Correct and safe. |

**DECISION: the index preserves today's exact matching.** B-3 stores custodian emails with only the
`trim()` the current code applies to the stored side, and C-1 compares the caller's email with
`equals`, case-sensitively. A case-mismatched email that does not authorize today must not authorize
through search either.

This is deliberate, and it means shipping a known defect on purpose. Normalizing here would newly
authorize case-mismatched custodians — a real authorization expansion, arrived at as a side effect of
an indexing decision, and applying to the search path only while the non-search endpoints kept
rejecting the same user. That asymmetry is worse than the defect. **OPEN-6** proposes fixing it
properly: normalize in `DatasetService` and in the index together, under C-2's shared predicates, as
one reviewed change with parity across both paths.

Note also that ES `keyword` fields match exactly by default, so exact matching is what the index does
without any special handling; normalization would be the thing requiring extra work, not the reverse.

---

## §B — Field classification

**Two tiers**, per Decision 2. Every indexed path gets exactly one; there is no unclassified residue,
because a field that nobody classified is a field that leaks by default.

- **SEARCH-VISIBLE** — in the one bundle search serves. Every authenticated caller receives it,
  including admins, and no caller receives more. (All search callers are authenticated:
  `DatasetResource.java:442-462` is `@PermitAll` with `@Auth DuosUser`, so this is "any logged-in
  user," not "anonymous.")
- **INTERNAL** — never served through search, to anyone, admins included. Present in the index only
  because enforcement needs it, or because it is indexed and nothing consumes it.

There is no third, privileged tier: Decision 2 establishes that native FLS cannot scope fields per
document, and §B.0 establishes that nothing needs it.

### §B.0 — How these were classified, and what it means to move one

The tier is **not** a judgment about how sensitive a field looks. It is a record of what the product
publishes today, checked against the consumers:

- `duos-ui/src/components/data_search/DatasetSearchTableConstants.tsx` — the catalog table renders
  `study.piName`, `study.dataCustodianEmail`, `dataset.dataLocation`, and `dataset.url` in
  unconditional columns, for every caller.
- `duos-ui/src/libs/utils.ts:498-522` — the client-side search filter additionally matches on
  `study.dataSubmitterEmail`, `dac.dacEmail`, and `createUserDisplayName`.

So PI names, custodian emails, submitter emails, and DAC emails are **already public to every
authenticated user** in the DUOS catalog. Classifying them as privileged would not have protected
them; it would have broken the product — `dataCustodianEmail.join(', ')` in the table is not
null-guarded and throws outright if the field is stripped. This is the check that turned a plausible
three-tier design into a wrong one, and it is why the two tiers below are grounded in consumers
rather than in intuition about PII.

Two directions of movement, with different costs:

- **INTERNAL → SEARCH-VISIBLE** is an exposure change. It needs review here first.
- **SEARCH-VISIBLE → INTERNAL** is a tightening, and therefore a *product* change plus a duos-ui
  change: something on screen today would disappear, and an unguarded consumer may throw. Do not do
  it as part of the enforcement work. **OPEN-7** is where that conversation belongs.

Every path below is a **literal Elasticsearch field path**, because that is what an FLS `grant` and
the E-3 allowlist consume. Object fields are enumerated to their leaves — `dataUse.primary` is an
array of objects, so `dataUse.primary.code` and `dataUse.primary.description` are the grantable
paths and `dataUse.primary[]` is not a usable path at all.

### §B.1 — `DatasetTerm` (root)

| Path | Tier | Note |
| --- | --- | --- |
| `datasetId` | SEARCH-VISIBLE | |
| `datasetIdentifier` | SEARCH-VISIBLE | |
| `datasetName` | SEARCH-VISIBLE | |
| `participantCount` | SEARCH-VISIBLE | Rendered and summed in the table. |
| `dataUse.primary.code` | SEARCH-VISIBLE | Filtered on client-side (`utils.ts:500`). |
| `dataUse.primary.description` | SEARCH-VISIBLE | |
| `dataUse.secondary.code` | SEARCH-VISIBLE | |
| `dataUse.secondary.description` | SEARCH-VISIBLE | |
| `accessManagement` | SEARCH-VISIBLE | Drives selectability and the access-type column. |
| `dacId` | SEARCH-VISIBLE | |
| `dacApproval` | SEARCH-VISIBLE | Display attribute, not authorization (§A row 11). |
| `hasInstitutionCertification` | SEARCH-VISIBLE | |
| `dataLocation` | SEARCH-VISIBLE | Rendered in the data-location column. |
| `url` | SEARCH-VISIBLE | Rendered as the data-location link; client-side filter matches it. |
| `createUserDisplayName` (deprecated) | SEARCH-VISIBLE | Client-side filter matches it. Deprecated in favour of `submitter.displayName`; if it is removed from the index, remove the consumer in the same change. |
| `requestLocation` | **INTERNAL** | Indexed, no consumer found in duos-ui. |
| `deletable` | **INTERNAL** | Lifecycle state; discloses whether a dataset is in use. No consumer. |
| `createUserId` | **INTERNAL** | Internal user ID. No consumer. |
| `data.*` | **INTERNAL** | Dynamic map — §B.5. |

### §B.2 — `study` (`StudyTerm`)

| Path | Tier | Note |
| --- | --- | --- |
| `study.studyId` | SEARCH-VISIBLE | Used as a row key. |
| `study.studyName` | SEARCH-VISIBLE | |
| `study.description` | SEARCH-VISIBLE | Client-side filter matches it. |
| `study.phsId` | SEARCH-VISIBLE | dbGaP accession. |
| `study.phenotype` | SEARCH-VISIBLE | |
| `study.species` | SEARCH-VISIBLE | Client-side filter matches it. |
| `study.dataTypes` | SEARCH-VISIBLE | Array of keywords; the path is grantable as-is. |
| `study.piName` | SEARCH-VISIBLE | **Rendered in an unconditional "PI" column.** Published today. |
| `study.dataCustodianEmail` | SEARCH-VISIBLE | **Rendered in an unconditional "Data Custodian" column**, and the consumer is not null-guarded. Also an authorization input — that does not make it secret, but it does mean B-4 must reindex on change. |
| `study.dataSubmitterEmail` | SEARCH-VISIBLE | Client-side filter matches it (`utils.ts:505`). |
| `study.externalIdentifier` | SEARCH-VISIBLE | |
| `study.externalIdentifierType` | SEARCH-VISIBLE | |
| `study.dataSubmitterId` | **INTERNAL** | Internal user ID. No consumer. |
| `study.throughBioId` | **INTERNAL** | Internal cross-system identifier. No consumer. |
| `study.publicVisibility` | **INTERNAL** | Under Decision 1 a caller only receives documents they may read, so it carries no information they need. G-1 removes the client-side filtering that reads it — sequence accordingly. |
| `study.assets.*` | **INTERNAL** | Dynamic map — §B.5. |
| `study.data.*` | **INTERNAL** | Dynamic map — §B.5. |

### §B.3 — `submitter` / `updateUser` (`UserTerm`) and `dac` (`DacTerm`)

| Path | Tier | Note |
| --- | --- | --- |
| `dac.dacId` | SEARCH-VISIBLE | |
| `dac.dacName` | SEARCH-VISIBLE | Client-side filter matches it. |
| `dac.dacEmail` | SEARCH-VISIBLE | Client-side filter matches it. Usually a shared mailbox. |
| `submitter.userId` | **INTERNAL** | Internal user ID. |
| `submitter.displayName` | **INTERNAL** | No consumer — the table uses the deprecated `createUserDisplayName` instead. Promote if a consumer appears; do not grant it speculatively. |
| `submitter.institution.id` | **INTERNAL** | |
| `submitter.institution.name` | **INTERNAL** | |
| `updateUser.userId` | **INTERNAL** | |
| `updateUser.displayName` | **INTERNAL** | |
| `updateUser.institution.id` | **INTERNAL** | |
| `updateUser.institution.name` | **INTERNAL** | |

`updateUser` has no consumer at all and discloses who last edited a dataset. B-1 should consider
dropping it from the index rather than indexing it and then filtering it out of every response.

### §B.4 — `accessPolicy` (added by B-1)

**Every path under `accessPolicy.*` is INTERNAL, without exception, for every caller including
admins.** It is enforcement input: creator IDs, custodian emails, and whatever allowlists §E
eventually authorizes. Granting any of it would hand back identity data, and an allowlist field would
additionally disclose *who else* has access.

This is stated as a rule rather than a list because B-1's field set is expected to grow. In the
fallback path (E-3) it matters most: that path retrieves whole `_source` objects, so `accessPolicy`
is returned by Elasticsearch and must be stripped by the server.

### §B.5 — Dynamic maps are INTERNAL, and grants may not contain wildcards

`DatasetTerm.data`, `StudyTerm.data`, and `StudyTerm.assets` are `Map<String, Object>` populated
wholesale from property-bag values (`ElasticSearchService.java:294-297, 514-517`). Their key sets are
not fixed, not validated, and not reviewable at design time — a new registration schema field lands
in them with no code change here.

**Rule: dynamic maps are INTERNAL, and no field grant may contain a wildcard.** A grant of `data.*`
publishes whatever a future schema puts there, which is the precise failure this classification
exists to prevent. Individual keys may be promoted only by being enumerated explicitly —
`data.someKnownKey` — after review of what populates them.

Two consequences for implementation:

- **The grant list is an allowlist of literal paths.** ES FLS supports `grant` with `except`
  patterns; do not use `{"grant": ["*"], "except": [...]}`, which fails open on every field added
  later. Enumerate. This applies to the admin grant too — see §B.7.
- **Epic E must apply the same rule** in `ResponseFieldFilter` (E-3): filter by allowlist, dropping
  unrecognized paths, rather than by denylist.

### §B.6 — Keep this list in step with the model classes

§B is enumerated from `DatasetTerm.java`, `StudyTerm.java`, `UserTerm.java`, `DacTerm.java`,
`InstitutionTerm.java`, `DataUseSummary.java`, and `DataUseTerm.java`. The "Indexed Elements" section
of the plan document was incomplete (it omitted `study.externalIdentifier`,
`study.externalIdentifierType`, and the `UserTerm.institution` sub-fields); it has been corrected, but
§B supersedes it either way.

**Whoever adds a field to any of those classes must classify it here in the same change.** An
unclassified field is dropped by the E-3 allowlist and omitted from the D-3 grant — it will appear to
"not work" rather than to leak, which is the safe failure but an confusing one if this rule is not
known. A test asserting that every serialized path appears in §B's list would enforce it mechanically
and is worth adding in B-6.

### §B.7 — Admins get the same bundle, not a wildcard

Admins bypass **document** filtering (§A row 1: DLS is `match_all`). They do not bypass field
filtering. The D-3 FLS grant and the E-3 response filter must use the same SEARCH-VISIBLE list for
admins as for everyone else.

A wildcard admin grant would serve `accessPolicy.*` and both dynamic maps to admin callers, directly
contradicting §B.4 and §B.5 — and it would do so through the same UI, which has no use for them.
Admin tooling that genuinely needs internal fields should use the admin endpoints, which are outside
this contract. "Admin" is a document-visibility bypass, not a projection bypass.

---

## §C — Storage gaps, and a correction to A-3

A-3 currently states that `dataCustodianEmail` needs "no new storage." That is true in the narrow
sense and misleading in the way that matters, and A-2 flagged it as a storage-gap candidate for a
reason. Both are recorded here as the contract's position:

**Persistent backing exists, but not in a queryable, constrained form.** The value lives in
`study_property` under the key `dataCustodianEmail`, typed `PropertyType.Json`, holding a JSON array
of strings (`DatasetService.java:213-237`; written at `DatasetService.java:573-582`). That means:

- **No referential integrity.** A custodian email need not correspond to any DUOS user. Access is
  granted to a string, not to a principal.
- **No constraint or normalization.** Case, whitespace, and duplicates are whatever was written;
  §A.2 records the asymmetric trim and case-sensitive comparison that result.
- **No index.** Answering "which studies is this user custodian of" means parsing every property
  bag, which is why the current code can only ask it one study at a time.
- **No defined behavior for malformed content.** A non-array or unparseable value throws from Gson
  inside an authorization check.

For Epics B–E this is workable, because B-3 denormalizes custodian emails into `accessPolicy` at
index time and the search path reads the index rather than the property bag. **The gap does not
block this work.** It stays a gap for the non-search endpoints, which keep parsing the bag, and it
makes reindex-on-custodian-change a correctness requirement rather than a nicety (B-4).

Decisions that remain open on it are OPEN-6 (normalization) and, separately from this contract,
whether custodianship should become a first-class relation. The latter is A-3's call, not A-2's;
what A-2 requires is that A-3 stop describing it as a solved case.

### Other dimensions' storage, restated

| Dimension | Storage today | Needed for contract |
| --- | --- | --- |
| `publicVisibility` | `study.public_visibility` column | Nothing new. |
| Dataset creator | `dataset.create_user_id` column | Nothing new. |
| Study creator | `study.create_user_id` column | Nothing new. |
| `dacId` | `dataset.dac_id` column | Nothing new. |
| DAC membership / chair | `dac_user` table | Nothing new — but DEFERred (row 9/10), so nothing consumes it yet. |
| Institution allowlist | **User side only.** No dataset-to-institution mapping exists. | Blocked: A-3 must decide mapping table vs. derivation before OPEN-5 can be answered. |
| Policy tags | **None.** | Blocked on A-3 and OPEN-5. |
| Explicit principal allowlist | **None.** | Blocked on A-3 and OPEN-5. |

---

## §D — What downstream tickets must change

| Ticket | Required change |
| --- | --- |
| **B-1** | Drop `fieldAccessProfile` (Decision 2). Omit `allowedInstitutionIds`, `allowedPrincipalIds`, `policyTags` until OPEN-5 says they are requirements. Comment on the class that every field is INTERNAL (§B.4). Custodian emails are trimmed on the stored side but preserve case (§A.2); do not lowercase or otherwise normalize them. |
| **B-2** | **Cancelled.** It specifies a per-document FLS marker, which Elasticsearch cannot honour, and the per-caller repair leaks across documents. Deriving a bundle is no longer needed at all: there is one bundle (Decision 2). |
| **B-3** | Populate rows 1–3 and 5–8. Write an explicit **no-study marker** (`accessPolicy.hasStudy`, §A.1) so row 5 does not depend on a null meaning "visible." Do not normalize custodian emails. |
| **B-4** | Custodian changes must trigger reindex — see §C; the index is now the authorization source for search. |
| **C-1** | Carry only caller-side inputs: the caller's user ID, email **unnormalized** for exact matching, and global roles. Dataset/study creator IDs remain distinct document-side `accessPolicy` fields (rows 6/7); the filter compares the one caller ID with both. Do **not** resolve DAC, institution, allowlist, or policy-tag context while rows 9–14 are DEFERred. **No field-bundle field**: the bundle is constant (Decision 2). |
| **C-2** | Normalize the two `verifyPublicVisibilityAccess` overloads so their null handling cannot diverge if the `NOT NULL` constraint is ever relaxed (§A.1). |
| **D-3** | DLS filter from rows 1–3, 5–8 — plus the no-study clause. **No DAC clause** (row 9 is DEFERred). FLS grant = the single SEARCH-VISIBLE list from §B, literal paths, no wildcards, **including for admins** (§B.7). |
| **E-2 / E-3** | Same filter, same allowlist, same source document. E-3 must strip `accessPolicy.*` and both dynamic maps from every response including admin responses, and must filter by allowlist rather than denylist. |
| **G-1** | Client-side `publicVisibility`/`dacApproval` filtering is removed once the server filter implements rows 2–5 identically. `study.publicVisibility` is INTERNAL (§B.2), so the client stops receiving it — sequence the two changes together. |
| **G-2 / G-4** | Verify no duos-ui consumer reads an INTERNAL field. §B.0 records the audit done here; G-2 is where it is confirmed against the whole app rather than the search components. |

---

## §E — Open decisions requiring sign-off

These are policy, not engineering. **None of them blocks implementation** — every one has a stated
default that preserves current behavior, so Epics B and C can proceed while they are answered. What
they block is *changing* behavior in the direction each describes.

| ID | Decision | Blocks | Default if unanswered |
| --- | --- | --- | --- |
| **OPEN-2** | Should a dataset with no study remain readable by everyone (row 5)? It is fail-open today. | Nothing — PRESERVE is implementable now | PRESERVE (readable), as the contract states — but worth a conscious confirmation rather than inheritance. |
| **OPEN-3** | Should DAC members and chairs gain read access to non-public datasets in their DAC? They have none today. | Rows 9–10 | DEFER. |
| **OPEN-4** | Is `dacApproval` ever authorization, or purely display? | Row 11, G-1 | Display only. |
| **OPEN-5** | Do institution allowlists, policy tags, and explicit principal allowlists exist as requirements at all? All three are speculative, none has storage, and B-1 reserved fields for them. | Rows 12–14, A-3 | DEFER — and if the answer is "not now," drop them from `AccessPolicyTerm` rather than shipping unpopulated fields that read as enforcement. |
| **OPEN-6** | Fix the custodian email case-sensitivity defect (§A.2)? The contract preserves today's exact matching, so this is a proposal, not a blocker. If approved it must land in `DatasetService` and the index **together**, via C-2 — normalizing only the search path would authorize a user through search whom the dataset endpoints still reject. | Nothing | Keep exact matching; fix as a separate reviewed change. |
| **OPEN-7** | Should the catalog **stop** publishing `study.piName`, `study.dataCustodianEmail`, `study.dataSubmitterEmail`, and `dac.dacEmail` to all authenticated callers? They are on screen today (§B.0), so this contract keeps them SEARCH-VISIBLE. Restricting them is a product decision, and needs a duos-ui change in the same release — the table's `dataCustodianEmail.join(', ')` throws if the field is absent. | Nothing — this is a proposed tightening, not a gap | Keep publishing. Changing it is product scope, not enforcement scope. |

---

## Status

**Complete and unblocked.**

Every dimension in §A and every path in §B is decided, and no OPEN item stands between this contract
and B-1/B-3/C-1. That is a change from the first draft, which recorded `publicVisibility = NULL` as
an unanswerable policy question: the schema answers it (§A.1), and checking rather than escalating
turned the last blocker into a fact.

What the OPEN items in §E now represent is proposed *changes* — expanding access (OPEN-3, OPEN-5),
tightening it (OPEN-7), or fixing a defect (OPEN-6) — each with a default that preserves what the
application does today. They can be answered on their own schedule.

Two things this contract does **not** cover, deliberately:

- **Document-scoped field access.** Decision 2 establishes that native FLS cannot do it and that
  nothing currently needs it. If that changes, this document is where the change starts, and the
  answer is application projection rather than a cleverer FLS grant.
- **The non-search endpoints.** `DatasetService` keeps its own document-scoped checks and its own
  richer projections. This contract governs the search index only; C-2 is what keeps the two from
  drifting apart.
