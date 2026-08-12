# Elasticsearch Access Contract — Ticket A-2

The formal access contract for dataset search: every access dimension, what it is allowed to do,
and which fields each class of caller may see. Companion to
[`elasticsearch-service-duos-ui-usage.md`](elasticsearch-service-duos-ui-usage.md) (the ticket plan)
and [`es-security-capability-record.md`](es-security-capability-record.md) (Ticket A-1, what the
clusters can enforce).

This is the document B-1 (`accessPolicy` schema), C-1 (auth context resolver), D-3 (DLS/FLS
generator), and E-0/E-1/E-2/E-3 (aggregation vocabulary, mediator, filter and allowlist) are
implemented against. Where it says **DECISION**, the question is settled and downstream tickets may
rely on it. Where it says **OPEN**, it needs a named owner's sign-off and is listed in §E — those are
policy choices, not engineering ones, and this document deliberately does not invent them.

**Every claim this contract makes about how Elasticsearch behaves is now measured, not cited.** The
design is exercised end-to-end by `ElasticSearchLeakDefensePocTest` against a real cluster; §1.1a
describes what it does and lists the four statements in this document that its measurements changed.
Where a section says "measured", that test is the source, and it will fail if a version bump changes
the answer.

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
which documents a query and its aggregations can match. **Epic E** — a mandatory `filter` clause
injected into the query's boolean context, which must be non-removable by the caller-supplied DSL
(E-2's sanitization is what makes that true).

### §1.1 — Scope of the invisibility guarantee: DLS alone does not deliver it

An earlier draft of this decision claimed that restricted documents "cannot contribute to counts,
facets, or aggregations" and that DLS filters before scoring and counting. **That claim was too
strong for the native path, and is corrected here.** Elastic documents the limitation explicitly:

> While document-level security prevents users from viewing restricted documents, it's still
> possible to write search requests that return aggregate information about the entire index.

and, on the same page, that a caller so restricted

> could still learn about field names and terms that only exist in inaccessible documents, and count
> how many inaccessible documents contain a given term.

([Elastic: controlling access at document and field level](https://www.elastic.co/docs/deploy-manage/users-roles/cluster-or-deployment-auth/controlling-access-at-document-field-level))

DLS does filter the hit set and ordinary aggregation buckets. What it does *not* isolate is
index-wide term and document statistics. **Which surfaces actually expose them has now been
measured on our own cluster rather than inferred from the documentation above** (§1.1a), and the
answer is narrower than this section first claimed — but not empty:

| Surface | Leaks past DLS on 9.4.4? |
| --- | --- |
| `terms` with `min_doc_count: 0` | **No.** Returns only terms present in readable documents. Nor with `shard_min_doc_count: 0`, an `include: ".*"` regex, `_key` ordering, `rare_terms`, or wrapped in a `global` aggregation. |
| `cardinality` / `value_count` / `sum` | **No.** Computed over matching documents only. |
| `_terms_enum` API | **No.** Returns an empty term list. |
| `significant_terms` | **Yes.** `bg_count` reports the document count of the entire index. |
| `explain` | **Yes.** `_explanation` reports index-wide `N` ("total number of documents with field") and `n` ("number of documents containing term"). |
| `profile` | Refused by the cluster outright once DLS is active. **But not on the fallback path**, which runs privileged with no DLS, so E-1 must still strip it. |

The two that do leak are sufficient on their own: a caller authorized for two of five documents
reads the size of the whole index from `bg_count`, and index-wide term frequencies from
`_explanation`, in a single ordinary search request. The conclusion below is therefore unchanged —
only the evidence for it is, and the two examples this section used to lead with (`min_doc_count: 0`
and the `cardinality` magnitude leak) have been removed because they do not reproduce.

This correction matters beyond tidiness. A security argument resting on a stale example invites the
next reader to re-derive it, find that it does not reproduce, and conclude the mediator is
unnecessary — which would take D-3's dependency on E-0/E-1 with it.

**DECISION: the invisibility guarantee is a property of document filtering *plus* query mediation,
and therefore both enforcement paths need both halves.** Query mediation may not be an Epic E
speciality. Specifically:

- **The guarantee this contract makes** is that a restricted document contributes no *hit*, no
  `hits.total`, and no bucket to any aggregation the server issues. It is scoped to the server-owned
  query surface, not to arbitrary caller DSL.
- **What makes that true** is §F: the server constructs every aggregation from a closed vocabulary
  (§F.1), and validates every remaining caller field reference against the QUERYABLE allowlist. Both
  apply on the native path (Epic D) exactly as on the fallback path (Epic E) — native DLS/FLS is a
  necessary component of Epic D, not a sufficient one.
- **Why the aggregation half must be server-owned rather than validated** is that a field allowlist
  closes value leaks but not count-and-existence leaks: those ride on fields the caller may
  legitimately aggregate, so only control of the aggregation *shape* closes them. §F.1 has the
  argument.
- **What this contract does not claim** is that a caller holding an arbitrary-DSL search endpoint
  cannot infer the existence or count of documents they may not read. Under unrestricted DSL that
  inference is possible on *both* paths, and no field grant or DLS query closes it. The residual after
  §F — index-wide term statistics visible through relevance scores — is recorded as OPEN-10 rather than
  claimed as closed.

This is the correction that moves `SearchQueryMediator` from "the fallback's compensating control"
to "a shared prerequisite of the contract." See §F and the revised D-3/E-1/E-3 rows in §D — and
§B.5c, which adds a second, independent reason Epic D cannot ship on its own components alone.

### §1.1a — How this was measured, and what else the measurement changed

Every Elasticsearch behavior this contract relies on is now exercised by
`ElasticSearchLeakDefensePocTest` (`src/test/java/org/broadinstitute/consent/integration/`), an
end-to-end proof of concept that runs a corpus of 26 exfiltration attempts and 7 legitimate requests
against a real cluster on a trial license, under four configurations:

| Mode | What it is | Measured result |
| --- | --- | --- |
| `UNMEDIATED` | today's `DatasetResource.searchDatasetIndex` | leaks by every route tried |
| `NATIVE_UNMEDIATED` | Epic D as originally specified — DLS/FLS, caller DSL passed through | still leaks index-wide statistics (§1.1) |
| `NATIVE` | Epic D plus §F mediation | no leak |
| `FALLBACK` | Epic E — mediation, injected filter, response filter | no leak, and identical results to `NATIVE` |

The enforcement itself is modeled in `ElasticSearchAccessContractModel` rather than implemented in
`src/main`, because E-0/E-1/E-2/E-3 do not exist yet and D-3 was blocked. Everything else is real:
real documents, real caller DSL, real API keys carrying real DLS/FLS role descriptors. It is a proof
of the design, not of an implementation — but the attacks either get through or they do not, and no
part of that verdict depends on a stub.

Three properties of the test are worth knowing, because they are what make its results usable as
evidence here:

- **It asserts that today's endpoint leaks.** Every attack is marked with whether it must succeed
  against `UNMEDIATED`, and a single test checks all of them. A "defended" assertion cannot pass
  vacuously because the attack quietly stopped working.
- **Leak detection scans the whole serialized response**, not named paths. §F.2 requires
  unrecognized channels to fail closed, and a test that inspected `hits.hits[*]._source` would share
  the exact blind spot it exists to catch. Statistical disclosure, which carries no marker, is
  checked structurally alongside it.
- **It was mutation-tested.** Four deliberate weakenings of the model were introduced to check the
  corpus notices: removing `aggs` from the strip list, dropping E-2's injected filter, narrowing E-3
  to `hits._source` only, and retaining the `sort` channel. The first two were caught immediately;
  **the last two were not**, and the assertions that close them were added as a result. That is also
  how §F.2's `aggregations.**` requirement came to be understood as defending against our own drift
  rather than against a caller — see §F.2.

Four measurements changed this document. §1.1 above is the first. The others are folded into the
sections they affect, and are collected here so a reader can find them:

| Finding | Lands in |
| --- | --- |
| §1.1's headline examples do not reproduce; `significant_terms` and `explain` do | §1.1, OPEN-10 |
| **OPEN-8 resolves restrictively**, and affects three paths rather than two | §B.0a, §B.5c, §D, OPEN-8 |
| A granted multi-field does not carry its `.keyword` subfield, and the failure is silent | §B.5c, §F.1, OPEN-9 |
| E-1's `fields` strip rule and §F.2's `sort` rule are not implementable as written | §F.1, §F.2 |

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

Classification runs on **two independent axes**, because a field can be needed by a caller without
being shown to one. An earlier draft used a single axis and, as a direct result, marked five fields
INTERNAL that the product depends on today (§B.0a).

| Axis | Values | Enforced by |
| --- | --- | --- |
| **Response** — may the value reach the caller? | RESPONSE-VISIBLE / RESPONSE-INTERNAL | D-3 FLS grant; E-3 response filter (**all channels**, §F.2) |
| **Query** — may caller DSL reference the path? | QUERYABLE / NON-QUERYABLE | §F.1 query-field allowlist, on **both** paths |

The axes are genuinely independent, and three of the four combinations are in real use:

- **RESPONSE-VISIBLE + QUERYABLE** — the ordinary catalog field. `datasetName`, `study.piName`.
- **RESPONSE-INTERNAL + QUERYABLE** — the caller must be able to *filter* on it but must never
  *receive* it. `createUserId` and `study.dataSubmitterId` are exactly this: duos-ui's "My Data
  Submissions" builds a `term` query on both and renders neither (§B.0a). A single-axis design cannot
  express this case, and forcing it either leaks the field or breaks the feature.
- **RESPONSE-VISIBLE + NON-QUERYABLE** — rendered, but not a legitimate filter or sort target.
  Reduces the inference surface of §1.1 without costing the product anything.
- **RESPONSE-INTERNAL + NON-QUERYABLE** — true internals. Everything under `accessPolicy.*` (§B.4).

All search callers are authenticated: `DatasetResource.java:442-462` is `@PermitAll` with
`@Auth DuosUser`, so RESPONSE-VISIBLE means "any logged-in user," not "anonymous."

There is still no *privileged* response tier: Decision 2 establishes that native FLS cannot scope
fields per document, and nothing needs it. Decision 2 is unaffected by the second axis — there is one
response bundle and one query allowlist, both constant across callers.

**Notation.** The tables below give the response axis in the **Tier** column (SEARCH-VISIBLE is
retained as a synonym for RESPONSE-VISIBLE, since D-3's grant consumes exactly that set) and the
query axis in a **Query** column. A blank Query cell means NON-QUERYABLE.

### §B.0 — How these were classified, and what it means to move one

The tier is **not** a judgment about how sensitive a field looks. It is a record of what the product
publishes today, checked against the consumers.

**Every caller of the search endpoint must be audited, not just the components that look like
search.** The complete caller set in `duos-ui` (`git grep -l searchDatasetIndex`) is:

| Caller | Reads |
| --- | --- |
| `src/hooks/useLibraryData.ts` | the whole data library, all 11 asset tabs |
| `src/hooks/useLibraryTabCounts.ts` | tab count badges |
| `src/hooks/useStudyDetailsData.ts` | study detail panel |
| `src/pages/DACDatasets.tsx` | DAC datasets table |
| `src/pages/DatasetStatistics.tsx` | dataset detail page |
| `src/utils/BucketUtils.ts` | DAR bucket construction |
| `src/libs/ajax/DataSet.ts` | the client itself |

Consumers confirmed as published to every authenticated caller today:

- `src/components/data_search/DatasetSearchTableConstants.tsx` — the catalog table renders
  `study.piName`, `study.dataCustodianEmail`, `dataset.dataLocation`, and `dataset.url` in
  unconditional columns, for every caller.
- `src/libs/utils.ts:498-522` — the client-side search filter additionally matches on
  `study.dataSubmitterEmail`, `dac.dacEmail`, and `createUserDisplayName`.

So PI names, custodian emails, submitter emails, and DAC emails are **already public to every
authenticated user** in the DUOS catalog. Classifying them as privileged would not have protected
them; it would have broken the product — `dataCustodianEmail.join(', ')` in the table is not
null-guarded and throws outright if the field is stripped. This is the check that turned a plausible
three-tier design into a wrong one, and it is why the tiers below are grounded in consumers rather
than in intuition about PII.

### §B.0a — Correction: five fields were misclassified as unconsumed

The first draft's audit covered only `data_search/DatasetSearchTableConstants.tsx` and
`libs/utils.ts` — two files, both belonging to the **older** search UI. It missed
`src/components/data_library/**` entirely, and `data_library` is where most of the current catalog
lives. Five paths were recorded as "no consumer found" and marked INTERNAL while being in active use
on `duos-ui/develop`. Generating an FLS grant or an E-3 allowlist from that table would have broken
each of the flows below.

| Path | First draft | Actual consumer on `duos-ui/develop` | Corrected |
| --- | --- | --- | --- |
| `requestLocation` | INTERNAL, "no consumer" | `data_library/columns/datasetColumns.tsx:92` renders it as the **"Request Path"** column; `pages/DatasetStatistics.tsx:266-269` renders it as a link, from a `searchDatasetIndex` response | RESPONSE-VISIBLE |
| `deletable` | INTERNAL, "no consumer" | `data_library/columns/submissionColumns.tsx:88` gates the **delete action**; `dac_dataset_table/DACDatasetApprovalStatus.tsx:54` branches on it | RESPONSE-VISIBLE |
| `createUserId` | INTERNAL, "no consumer" | `researcher_console/DatasetSubmissions.tsx:46` — `{term: {createUserId: userId}}`, one of the `should` clauses that **defines "My Data Submissions"** | RESPONSE-INTERNAL, **QUERYABLE** |
| `study.dataSubmitterId` | INTERNAL, "no consumer" | `researcher_console/DatasetSubmissions.tsx:47` — `{term: {'study.dataSubmitterId': userId}}`, same query | RESPONSE-INTERNAL, **QUERYABLE** |
| `submitter.displayName` | INTERNAL, "no consumer — promote if a consumer appears" | `dac_dataset_table/DACDatasetTableCellData.tsx:75` — `dataset.submitter?.displayName ?? ''`, rendered in the DAC datasets table, fed by `DACDatasets.tsx:55` `searchDatasetIndex` | RESPONSE-VISIBLE |

Two structural lessons, both now rules:

1. **The query axis exists because of rows 3 and 4.** `createUserId` and `study.dataSubmitterId` must
   stay filterable and must not be returned. Under native FLS a path outside the grant is not
   searchable at all — **measured, see OPEN-8** — so putting these outside the grant silently empties
   "My Data Submissions" rather than failing loudly. The remedy is §B.5c: grant them and strip them
   in the response filter.
2. **`study.publicVisibility` is QUERYABLE too.** `hooks/useLibraryData.ts:44` emits
   `{term: {'study.publicVisibility': true}}` when `restrictToPublicVisibility` is set. §B.2 keeps it
   RESPONSE-INTERNAL, but it cannot be made NON-QUERYABLE until G-1 removes that clause — so G-1
   sequences before the query allowlist tightens, not merely alongside it.

   **This is a third path in the same situation as rows 3 and 4, and it was missed.** The two lessons
   above were written as if the QUERYABLE-but-RESPONSE-INTERNAL set had two members; it has three,
   and the third is on the data library's main query path rather than on one console screen. Every
   consequence of lesson 1 applies to it identically: left outside the FLS grant, the
   `restrictToPublicVisibility` clause matches nothing and the library silently returns an empty
   catalog. §B.5c states the rule over the whole set rather than per path, so a fourth member cannot
   be missed the same way.

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
| `requestLocation` | SEARCH-VISIBLE | **Corrected — §B.0a.** Rendered as the "Request Path" column (`datasetColumns.tsx:92`) and on the dataset detail page (`DatasetStatistics.tsx:266`). |
| `deletable` | SEARCH-VISIBLE | **Corrected — §B.0a.** Gates the delete action (`submissionColumns.tsx:88`) and the DAC approval status branch. Discloses whether a dataset is in use; that exposure exists today, and removing it is OPEN-7 scope, not enforcement scope. |
| `createUserId` | **RESPONSE-INTERNAL** — **QUERYABLE** | **Corrected — §B.0a.** Internal user ID, never rendered, but `DatasetSubmissions.tsx:46` filters on it to build "My Data Submissions". Must be **in the FLS grant** and stripped by the response filter — a non-granted path is unmatchable (§B.5c, OPEN-8). |
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
| `study.dataSubmitterId` | **RESPONSE-INTERNAL** — **QUERYABLE** | **Corrected — §B.0a.** Never rendered, but `DatasetSubmissions.tsx:47` filters on it for "My Data Submissions". Must be **in the FLS grant** and stripped by the response filter (§B.5c, OPEN-8). |
| `study.throughBioId` | **INTERNAL** | Internal cross-system identifier. No consumer found in any of the seven search callers. |
| `study.publicVisibility` | **RESPONSE-INTERNAL** — **QUERYABLE until G-1** | Under Decision 1 a caller only receives documents they may read, so the *value* carries no information they need. But `useLibraryData.ts:44` emits `{term: {'study.publicVisibility': true}}`, so the *path* must stay queryable until G-1 lands (§B.0a lesson 2). Like the two rows above it must therefore be **in the FLS grant** and stripped by the response filter (§B.5c) — the third member of that set, and the one §B.0a originally missed. |
| `study.assets.*` | **SEARCH-VISIBLE — enumerated, see §B.5a** | **Corrected — §B.5a.** Not unconsumed: nine of the data library's eleven tabs are built entirely from this subtree. |
| `study.data.*` | **INTERNAL** | Dynamic map — §B.5. |

### §B.3 — `submitter` / `updateUser` (`UserTerm`) and `dac` (`DacTerm`)

| Path | Tier | Note |
| --- | --- | --- |
| `dac.dacId` | SEARCH-VISIBLE | |
| `dac.dacName` | SEARCH-VISIBLE | Client-side filter matches it. |
| `dac.dacEmail` | SEARCH-VISIBLE | Client-side filter matches it. Usually a shared mailbox. |
| `submitter.userId` | **INTERNAL** | Internal user ID. |
| `submitter.displayName` | SEARCH-VISIBLE | **Corrected — §B.0a.** `DACDatasetTableCellData.tsx:75` renders it in the DAC datasets table, fed by `searchDatasetIndex`. The first draft's "no consumer, do not grant speculatively" was wrong on the facts. |
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

### §B.5a — Correction: `study.assets.*` is the data library's primary data path

The rule above is sound for `data.*` and `study.data.*`. Applied to `study.assets.*` it was based on a
false premise — that the subtree has no consumer — and would take out most of the catalog.

**Nine of the data library's eleven asset tabs read nothing else.** Each asset definition in
`duos-ui/src/components/data_library/assets/` declares `searchFields` and a `buildQuery` over
`study.assets.<type>.*`: `modelAsset` queries `study.assets.models.{name,format,license,tags,
description,url,trainedOnDatasets,maintainer.name,maintainer.email}`, and `workspaceAsset`,
`publicationAsset`, `biospecimenAsset`, `clinicalTrialAsset`, `presentationAsset`,
`intellectualPropertyAsset`, and `fundingResourceAsset` do the same for their own subtrees. These
paths are both **queried** (folded into `ALL_SEARCH_FIELDS`, which every tab's `multi_match` and every
tab-count badge uses) and **rendered** (`transformResponse` maps them onto grid rows).

So `study.assets.*` is RESPONSE-VISIBLE and QUERYABLE in fact. What remains true is the *reason* the
first draft was nervous: it is a dynamic map, so it cannot be granted with a wildcard.

**DECISION: `study.assets.*` is served, but only through enumerated paths, and the enumeration is
owned by the asset definitions.** Concretely:

- D-3's grant and E-3's allowlist enumerate the leaf paths each asset definition actually uses. The
  eleven `AssetDefinition`s are the authoritative list; §B.6's mechanical check must cover them.
- A new key appearing under `study.assets.*` from a registration-schema change is **not** served until
  it is added to an asset definition and to the enumeration. That is the fail-closed behavior §B.5
  wanted, and it is achievable without a wildcard.
- `study.data.*` and `data.*` stay INTERNAL. Neither has a consumer among the seven search callers,
  and the §B.5 rule applies to them unchanged.

**This is the one place where enumeration is a real cost**, so it needs stating plainly: the asset
subtrees are wide, they change with the registration schema, and a hand-maintained copy in the
backend will drift from `duos-ui`. Generating the allowlist from a shared schema — or, failing that,
a contract test that fails when an `AssetDefinition` references an unenumerated path — is the only
version of this that stays correct. **OPEN-9.**

### §B.5b — The client sends a nested `_source` wildcard, and E-1 does not strip it

`STUDIES_AGG` (`data_library/assets/definition.ts:42-56`) is the shared aggregation behind every
study-asset tab:

```json
{"terms": {"field": "study.studyId", "size": 10000},
 "aggs": {"study_details": {"top_hits": {"size": 1, "_source": ["study.*"]}}}}
```

Three things follow, and each is a hole in the Epic E design as currently specified:

1. **E-1's strip list is top-level only.** Its implementation note is
   `((ObjectNode) root).remove(List.of("_source", ...))`, which removes `$._source` and leaves
   `$.aggs.studies.aggs.study_details.top_hits._source` untouched. A caller-supplied `_source` inside
   a `top_hits` sub-aggregation survives sanitization intact, and can be changed to `["*"]` or
   `["accessPolicy.*"]`.
2. **E-3 does not filter this channel.** It filters `hits.hits[*]._source`. A `top_hits`
   sub-aggregation returns whole `_source` documents at
   `aggregations.**.hits.hits[*]._source` — a different path, unfiltered, and the *primary* response
   channel for nine tabs. As specified, E-3 returns the complete `study` object there, including
   `study.publicVisibility`, `study.dataSubmitterId`, `study.throughBioId`, and `study.data.*`.
3. **The `study.*` wildcard is the product's own request.** The client asks for a wildcard subtree,
   so "no wildcards" cannot be enforced by rejecting the request without breaking the library. The
   server must instead *narrow* it: treat a caller `_source` as a request, intersect it with the
   allowlist, and project the result.

**DECISION: `_source`, and every other field-bearing key, must be handled at every depth, not just at
the root.** The mediator walks the DSL tree; the response filter walks every channel. §F states both
requirements, and §D revises E-1/E-3 accordingly. Note that Epic D is *not* exposed to hole 2 — native
FLS applies to `_source` retrieval wherever it happens, including inside `top_hits` — which is
precisely why the fallback path cannot be specified as "the same minus FLS."

### §B.5c — The FLS grant is not the RESPONSE-VISIBLE list, and §B is not enough to generate it

Two measurements (§1.1a) establish that the grant D-3 specifies — "the RESPONSE-VISIBLE list from §B,
literal paths, no wildcards" — cannot be shipped as-is. Both come from the same Elasticsearch
behavior, stated here because the contract elsewhere assumes the opposite: **a field outside an FLS
grant does not exist as far as the search is concerned, not merely as far as the response is.**

**1. Every QUERYABLE path must be in the grant.** A `term` query on a non-granted path matches
nothing. Not an error — zero hits, which is indistinguishable from a legitimately empty result. The
three QUERYABLE-but-RESPONSE-INTERNAL paths (§B.0a) are therefore unusable on the native path unless
granted, which takes out "My Data Submissions" and the library's `restrictToPublicVisibility` filter.

**2. A granted multi-field does not carry its `.keyword` subfield.** `datasetName` is `text` with a
`keyword` subfield, which is what the catalog sorts on. Granting `datasetName` does not grant
`datasetName.keyword` — they are separate fields to FLS. A sort on the subfield then resolves against
nothing and Elasticsearch returns *a* page, just not the one requested, with no error. This is worse
than the first case: wrong pagination is not obviously wrong.

Case 2 also puts the grant in direct conflict with §F.1's third normalization rule. The mediator
resolves `datasetName.keyword` to `datasetName`, finds it on the allowlist, and accepts the sort; FLS
then declines to serve the field the sort actually names. **The allowlist and the grant disagree about
what a subfield is**, and the disagreement produces silently wrong results rather than a refusal.

**DECISION: the native path's FLS grant is RESPONSE-VISIBLE ∪ QUERYABLE ∪ the `.keyword` subfields of
every granted multi-field, and E-3's response filter runs on the native path as well.** The grant
governs what the *search* can resolve; the response filter governs what the *caller* receives. Only
splitting the two responsibilities honours both §B axes at once — native FLS alone cannot, which is
the substance of OPEN-8, and this is the remedy OPEN-8 itself named as its fallback.

Three consequences, none of them local:

- **Epic D is not "Epic E minus the response filter."** It needs E-3. Combined with §1.1's finding
  that it also needs E-0 and E-1, the plan's "D **or** E" framing is wrong for a second, independent
  reason (§F.3 already asks for that correction; this strengthens it).
- **§B's tables are not a sufficient source for the grant.** They enumerate logical paths; the grant
  needs mapping-level field names, including subfields that appear nowhere in §B. A grant generated
  from these tables would compile, pass review, and break sorting. §B.5's no-wildcard rule is what
  makes this non-trivial: `datasetName*` would solve it and is forbidden.
- **This is OPEN-9's drift problem in a second place**, and a second argument for generating the
  grant from the mapping rather than maintaining it against the classification tables by hand.

Decision 2 is unaffected: there is still exactly one grant and one response bundle, constant across
callers, including admins. What changes is that the grant is wider than the bundle, and the response
filter — not FLS — is what closes the gap.

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
| **D-3** | DLS filter from rows 1–3, 5–8 — plus the no-study clause. **No DAC clause** (row 9 is DEFERred). FLS grant is literal paths, no wildcards, **the same for admins** (§B.7), **with `study.assets.*` leaves enumerated** (§B.5a). **Revised twice by measurement (§1.1a):** (a) DLS/FLS alone does not deliver Decision 1, so D-3 depends on E-0 and E-1 and must not ship without them (§1.1); (b) the grant is **not** the RESPONSE-VISIBLE list — OPEN-8 is resolved restrictively, so the grant is RESPONSE-VISIBLE ∪ QUERYABLE ∪ the `.keyword` subfields of granted multi-fields, **and E-3 runs on this path too** (§B.5c). Generate the grant from the mapping, not from §B's tables: they do not contain the subfields. |
| **E-1** | **Revised.** Strip the unsafe keys at **every depth**, not just the root — a nested `_source` inside `aggs.*.top_hits` currently survives (§B.5b). Add `runtime_mappings`, `collapse`, `inner_hits`, `rescore`, `suggest`, `docvalue_fields` and `stored_fields` to the strip list. Do not simply "preserve `aggs`/`sort`/`highlight`" — pass each through the §F.1 field-reference validator. **Correction:** `fields` may **not** be stripped unconditionally at every depth. It is a response channel at request level but the clause's own field list inside `multi_match`/`query_string`/`highlight`; stripping it there silently widens the search to all fields, silently disables highlighting, and hides the `["*"]` reference the validator needs to refuse (§F.1 rule 1). Strip it as a channel, keep and validate it as a field list. |
| **E-2 / E-3** | Same filter, same allowlist, same source document. **E-3 revised:** filtering `hits.hits[*]._source` is not sufficient. It must handle **every response channel** (§F.2) — `aggregations.**` including `top_hits` `_source`, `hit.fields`, `hit.highlight`, `hit.sort`, and `inner_hits` — for every caller including admins, by allowlist rather than denylist. As specified, nine data-library tabs receive whole unfiltered `study` objects through the aggregation channel. **Two further revisions:** `hit.sort` must be **dropped**, not filtered — it carries values with no field names, so no allowlist applies (§F.2); and **E-3 is required on the native path as well**, because §B.5c widens the FLS grant beyond RESPONSE-VISIBLE and the response filter is what closes the difference. E-3 is therefore not fallback-only. |
| **D-5 / E-5 / B-6** | The end-to-end proof of concept (§1.1a) already exists as `ElasticSearchLeakDefensePocTest`, with the enforcement modeled in `ElasticSearchAccessContractModel`. These tickets should replace the model with the real components rather than write a new corpus: the attack corpus, the marker scheme, the caller fixtures and the parity assertion are all reusable as-is, and the mutation results in §1.1a record which assertions are load-bearing. Keep the `NATIVE_UNMEDIATED` configuration — it is what proves the corpus can still detect a leak. |
| **G-1** | Client-side `publicVisibility`/`dacApproval` filtering is removed once the server filter implements rows 2–5 identically. `study.publicVisibility` is RESPONSE-INTERNAL (§B.2), so the client stops receiving it — sequence the two changes together. **Also:** `useLibraryData.ts:44` sends it as a `term` clause, so G-1 must land before `study.publicVisibility` becomes NON-QUERYABLE (§B.0a). |
| **G-2 / G-4** | Verify no duos-ui consumer reads a RESPONSE-INTERNAL field or filters on a NON-QUERYABLE one. §B.0's caller table is the scope; §B.0a is a record of what a partial audit cost. The audit must cover `data_library/**` and the eleven `AssetDefinition`s, not only `data_search/**`. |

---

## §E — Open decisions requiring sign-off

These are policy, not engineering. **None of them blocks implementation** — every one has a stated
default that preserves current behavior, so Epics B and C can proceed while they are answered. What
they block is *changing* behavior in the direction each describes.

Two entries are exceptions to "policy, not engineering" and are kept in this table only so their IDs
resolve. **OPEN-8 is resolved**: it was a fact about Elasticsearch, it has been measured, and the row
now records the answer and where it lands. **OPEN-9** is an engineering sync problem rather than a
sign-off, and it is the one item on this list that has grown rather than shrunk.

| ID | Decision | Blocks | Default if unanswered |
| --- | --- | --- | --- |
| **OPEN-2** | Should a dataset with no study remain readable by everyone (row 5)? It is fail-open today. | Nothing — PRESERVE is implementable now | PRESERVE (readable), as the contract states — but worth a conscious confirmation rather than inheritance. |
| **OPEN-3** | Should DAC members and chairs gain read access to non-public datasets in their DAC? They have none today. | Rows 9–10 | DEFER. |
| **OPEN-4** | Is `dacApproval` ever authorization, or purely display? | Row 11, G-1 | Display only. |
| **OPEN-5** | Do institution allowlists, policy tags, and explicit principal allowlists exist as requirements at all? All three are speculative, none has storage, and B-1 reserved fields for them. | Rows 12–14, A-3 | DEFER — and if the answer is "not now," drop them from `AccessPolicyTerm` rather than shipping unpopulated fields that read as enforcement. |
| **OPEN-6** | Fix the custodian email case-sensitivity defect (§A.2)? The contract preserves today's exact matching, so this is a proposal, not a blocker. If approved it must land in `DatasetService` and the index **together**, via C-2 — normalizing only the search path would authorize a user through search whom the dataset endpoints still reject. | Nothing | Keep exact matching; fix as a separate reviewed change. |
| **OPEN-7** | Should the catalog **stop** publishing `study.piName`, `study.dataCustodianEmail`, `study.dataSubmitterEmail`, and `dac.dacEmail` to all authenticated callers? They are on screen today (§B.0), so this contract keeps them SEARCH-VISIBLE. Restricting them is a product decision, and needs a duos-ui change in the same release — the table's `dataCustodianEmail.join(', ')` throws if the field is absent. | Nothing — this is a proposed tightening, not a gap | Keep publishing. Changing it is product scope, not enforcement scope. |
| **OPEN-8** | **RESOLVED by measurement — restrictively. No longer blocks D-3.** The question was whether a path outside the FLS `grant` remains usable as a `term` query target. It does not: a `term` query on a non-granted path matches **zero documents, with no error**, which is indistinguishable from a legitimately empty result. Measured on 9.4.4 by `ElasticSearchLeakDefensePocTest.open8_aPathOutsideTheFlsGrantIsNotQueryable`, with a wider-grant control leg proving the result is FLS and not a faulty probe. Consequences: the two §B axes cannot both be honoured by native FLS alone; the remedy is the one this item already named as its fallback — **grant the QUERYABLE paths and strip them in the response filter on the native path too** (§B.5c); and the affected set is **three** paths, not two — `study.publicVisibility` is in the same position and §B.0a had missed it. | Nothing — D-3 is unblocked, with §B.5c's grant | n/a — settled by measurement. |
| **OPEN-9** | How is the `study.assets.*` leaf enumeration (§B.5a) kept in sync between the backend allowlist and duos-ui's eleven `AssetDefinition`s? A hand-maintained copy will drift, and drift is invisible in whichever environment runs the other path. Options: generate both from a shared schema, or add a contract test that fails when an `AssetDefinition` references an unenumerated path. **Strengthened by two findings.** First, drift is now the *primary* justification for §F.2's `aggregations.**` walk: with server-owned aggregations a caller cannot reach that channel, so what the walk defends against is a drifted server-side enumeration asking Elasticsearch for an internal leaf (§F.2). Second, §B.5c adds a second thing that cannot be generated from §B's tables — the `.keyword` subfields of granted multi-fields — which makes "generate from a shared schema/mapping" the answer for the FLS grant as a whole, not just for the asset subtrees. | Nothing — but the drift is a live defect risk for D-3/E-3 | Add the contract test in B-6; treat generation from the mapping as the durable fix, now for the whole grant. |
| **OPEN-10** | Accept the residual scoring-statistics leak (§F.1)? Relevance scores derive from index-wide term frequencies, so a caller can infer weakly about documents DLS hides even with server-owned aggregations. Closing it means running the text search in `filter` context with constant scoring, which costs relevance ranking on the library search box. **Now measured, and the direct reads are worse than "weak inference" suggested:** `_explanation` reports index-wide `N` and `docFreq` verbatim, and `significant_terms`' `bg_count` reports the whole index's document count — both straight past DLS (§1.1). Those are closed by §F.1 (strip `explain`, server-own `aggs`), not by DLS, which is what makes stripping them load-bearing rather than tidy. What remains genuinely residual after §F.1 is only the inference from `_score` values. | Nothing | Accept and document the residual — but the residual is now specifically "inference from score values", not the direct reads, which are closed. Revisit only if the threat model changes. |
| **OPEN-11** | Promote plan item 4.2 (server-owned search API taking business parameters) from "deferrable" to the roadmap? §F.1a argues the aggregation half should be done now regardless, and that finishing the `query` half retires §F entirely. | Nothing — §F.1 is implementable without an answer | Do the `aggs` half now as E-0; schedule the `query` half deliberately rather than leaving it indefinitely deferred. |

---

## §F — Query mediation and response-channel filtering

New section. §1.1 establishes that document filtering alone does not deliver Decision 1, and §B.5b
establishes that filtering `hits.hits[*]._source` alone does not deliver §B's classification. Both
gaps are the same shape: enforcement was specified against one channel while the caller controls
several. **These requirements apply to Epic D and Epic E identically.** Epic D's native DLS/FLS
replaces §F.2 for `_source` projection only; everything else in §F is required on both paths.

### §F.1 — The server owns aggregations outright; callers do not send them

An earlier version of this section proposed validating caller-supplied aggregation DSL: walk the tree,
check field references against the QUERYABLE allowlist, and forbid `min_doc_count: 0`,
`significant_terms`, and `significant_text`. **That design is rejected, and it is worth being precise
about why, because it looked reasonable.**

**A field allowlist does not close the aggregation leak.** It closes *value* leaks — an aggregation on
`accessPolicy.custodianEmails` is refused because the path is not QUERYABLE. It does nothing about
*count and existence* leaks, because those ride on fields the caller is perfectly entitled to
aggregate. `significant_terms` on `study.piName` reports `bg_count` — the document count of the whole
index, including everything DLS hid — and `study.piName` is QUERYABLE and RESPONSE-VISIBLE, so every
field check passes. The leak is a property of the *aggregation shape*, not of the field. (Measured, and
deliberately not the example this paragraph used to give: `min_doc_count: 0` was the illustration here,
and it does not reproduce on 9.4.4 — see §1.1. The argument is unchanged, but it now rests on a route
that demonstrably works.)

Which leaves shape restrictions doing the actual security work — and a list of three forbidden shapes
is a **denylist**, with exactly the failure mode this contract rejects everywhere else (§B.5, §F.2):
it fails open on the next aggregation type, the next parameter, or the next ES version. Enumerating
leak techniques against an open grammar is not a defensible long-term position, and "we validated the
DSL" reads as stronger than it is. The denylist that was proposed makes the point by itself: two of its
three entries — `min_doc_count: 0` and `significant_terms` — turn out to have swapped status against
the version we actually run, in the direction nobody would have checked.

**DECISION: aggregations are constructed by the server from a closed, named vocabulary. Caller
requests name a facet or a tab; they do not carry `aggs`.** `aggs`/`aggregations` joins the strip
list — removed at every depth, like `_source`.

This is sound because the vocabulary is *already* closed in practice. Every aggregation the product
issues lives in `duos-ui/src/components/data_library/assets/` and is one of three shapes:

| # | Shape | Used by | Parameters |
| --- | --- | --- | --- |
| 1 | `FILTER_AGGS` — four fixed `terms` facets on `accessManagement`, `dataUse.primary.code`, `study.dataTypes`, `dac.dacName` (`datasetAsset.ts:18-22`) | the datasets tab, filter panel counts | none |
| 2 | `STUDIES_AGG` — `terms(study.studyId)` + `top_hits(size 1)` (`definition.ts:42-56`) | **nine** study-asset tabs, unchanged between them | asset type (selects the `_source` leaves) |
| 3 | studyAsset's composite — `cardinality(study.studyId)`, `composite` paging over `study.studyId`, plus `top_hits`, `value_count(datasetId)`, `sum(participantCount)`, `terms(datasetId)` (`studyAsset.ts:42-72`) | the studies tab | page, page size |

Three shapes, parameterized by nothing more than `(tab, filters, page, size, sort)`. No caller needs to
author aggregation DSL, and none of the three admits a caller-chosen `min_doc_count`,
`background_filter`, or aggregation type. The leak is closed **by construction** rather than by
detection: there is no caller-supplied shape left to inspect.

What remains for the mediator is genuinely small, and is field-level only:

1. **Strip at every depth, but context-aware**: `aggs`/`aggregations`, `_source`, and the rest of §D's
   E-1 list. The nested-`_source` hole of §B.5b disappears with them — the server emits `top_hits` and
   therefore sets its own `_source`.

   **`fields` is the exception, and "strip `fields` at every depth" is wrong.** E-1's revised criteria
   say both "add `fields` to the strip list" and "strip at every depth"; taken together those two
   instructions break the product, in the quiet direction. `fields` is a response channel at request
   level, but inside `multi_match`, `query_string`, `simple_query_string`, `combined_fields` and
   `highlight` it is the clause's own list of field paths — which the library search box and every
   highlighted column use. Stripping it there does not fail loudly: it silently widens the search to
   every field in the mapping, silently disables highlighting, and removes the very reference rule 2
   needs in order to refuse `"fields": ["*"]`. The rule is therefore: **strip `fields` where it is a
   response channel, keep it where it names query targets, and validate it there.**
2. **Validate the remaining field references** — query clause targets, `sort` keys,
   `highlight.fields` keys, `multi_match`-family `fields` entries — against the QUERYABLE allowlist,
   and **reject** rather than drop, since a dropped `filter` broadens a query and a dropped `sort`
   changes paging. This is what keeps `{"term": {"createUserId": 42}}` working (§B.0a) while refusing a
   `term` on `accessPolicy.*`.
3. **Normalize before matching**: resolve `.keyword` suffixes, strip `field^boost`, and refuse
   wildcards outright instead of expanding them — with the server owning `_source` and `aggs`, no
   legitimate caller reference contains one.

   **Resolving `.keyword` here creates an obligation on the FLS grant.** Accepting
   `datasetName.keyword` because it normalizes to an allowlisted `datasetName` is only sound if the
   grant actually serves `datasetName.keyword`, and by default it does not — see §B.5c. The
   normalization rule and the grant must be changed together or sorting silently returns the wrong
   page.

**Scoring statistics remain a residual leak** and this contract does not claim otherwise: relevance
scores are computed from index-wide term frequencies, so a determined caller can still infer weakly
from score values. Stripping `explain` and `profile` removes the *direct* read, and §1.1's measurements
show that is not a formality — `_explanation` reports index-wide `N` and `docFreq` outright, and
`significant_terms`' `bg_count` reports the size of the whole index, both straight past DLS. Those two
are closed by §F.1 (`aggs` is server-owned, `explain` is stripped) rather than by DLS. Closing the
remaining *inference* would require constant scoring (`filter` context only), which costs relevance
ranking on the text search box. **OPEN-10** records that trade rather than pretending it is settled.

### §F.1a — The simplification this points at

Shapes 1–3 above are parameterized by `(tab, filters, page, size, sort)`. That is a business-parameter
API, and it is already in the plan as deferred item 4.2 ("server-owned search API that accepts
business parameters instead of raw Elasticsearch DSL",
`elasticsearch-service-duos-ui-usage.md:1870`). §F.1 is that item applied to `aggs` alone.

Applying it to the `query` clause as well would retire this entire section: with no caller DSL at all
there is no field-reference validator, no strip list, no normalization, and no §F.2 response-channel
walker, because the server would know every field it asked for. The mediator exists only to make an
arbitrary-DSL endpoint safe; the endpoint is the thing generating the work.

The cost is a duos-ui rewrite — eleven `AssetDefinition`s and `filterRegistry` currently build DSL
client-side — and a v3 endpoint alongside v2 during migration. That is real, and it is why item 4.2 was
deferred. But the ordering matters: **doing `aggs` now (§F.1) is both the largest security win and the
first increment of 4.2**, and it makes the eventual migration smaller rather than larger. Recommend
promoting 4.2 from "deferrable" to the roadmap on that basis. **OPEN-11.**

### §F.2 — Filter every response channel, not just `hits._source`

Elasticsearch returns field values through more channels than one. Each must be handled — and the
handling is not the same for all of them, because not every channel names the field it is carrying:

| Channel | Path | Handling | Why |
| --- | --- | --- | --- |
| Hit source | `hits.hits[*]._source` | **Project** against RESPONSE-VISIBLE | The channel E-3 already covers. |
| **Aggregation `top_hits`** | `aggregations.**.hits.hits[*]._source` | **Project** | Whole documents, at arbitrary nesting depth. **The primary channel for nine data-library tabs** (§B.5b). |
| **Aggregation buckets** | `aggregations.**.buckets[*].key` | Project defensively | A `terms` bucket key *is* the field value — an agg on `accessPolicy.custodianEmails` returns the emails as keys. Prevented in §F.1; filter here too. |
| **Sort values** | `hits.hits[*].sort` | **Drop entirely** — see below | Sorting on a RESPONSE-INTERNAL path echoes its value per hit. |
| **Highlight** | `hits.hits[*].highlight` | **Project** (keyed by field path) | Returns matched snippets of the highlighted field's content. |
| **`fields`** | `hits.hits[*].fields` | **Project** (keyed by field path) | The `fields` request parameter — not in E-1's original strip list. |
| **Inner hits** | `hits.hits[*].inner_hits.**._source` | **Project** | Same exposure as `top_hits`, different path. |

**The sort channel cannot be filtered against an allowlist, and must be dropped.** This section
previously said to filter it "against the RESPONSE-VISIBLE allowlist" like every other channel, which
is not implementable: `hits.hits[*].sort` is a positional array of *values* carrying no field names,
so there is nothing for an allowlist to match on. It matters because the two §B axes overlap exactly
here — `createUserId`, `study.dataSubmitterId` and `study.publicVisibility` are QUERYABLE and
RESPONSE-INTERNAL (§B.0a), so §F.1's validator accepts any of them as a sort key and the sort channel
then echoes the value once per hit. Two implementations close it and there is no third: drop the array
in E-3, or validate `sort` keys against RESPONSE-VISIBLE rather than QUERYABLE in the mediator.
**Dropping is preferred** — it needs no second allowlist in the mediator and it fails closed for sort
keys nobody anticipated. Note that the product does not read sort values, so nothing is lost.

Three implementation rules, the first two mirroring §B.5's allowlist principle:

- **Recurse structurally, not by known path.** `aggregations` nests arbitrarily and its keys are
  caller-named, so a filter keyed on expected paths misses renamed or more deeply nested aggs. Walk
  for the *shapes* (`hits.hits[*]._source`, `buckets[*].key`) wherever they occur. In practice the
  cleanest form of this is a single rule — *any object carrying a `_source` is a hit, wherever it
  occurs* — which covers `top_hits` and `inner_hits` by the same code path as ordinary hits.
- **Unrecognized channels fail closed.** If a future ES version adds a response channel, the filter
  should drop what it does not recognize rather than pass it through. This is the response-side
  counterpart to §B.6's rule about unclassified fields. The way to get this for free is to retain a
  named set of hit-level keys and drop the rest, rather than enumerating channels to remove.
- **Project the path-keyed channels rather than dropping them.** `highlight` and `fields` are keyed by
  field path, so they *can* be filtered, and should be: dropping them wholesale would also disable
  highlighting on `datasetName`, which the catalog uses. An enforcement design that closes every
  channel by deleting it passes every attack and is unshippable.

**On what the `aggregations.**` walk is actually defending against.** Once §F.1 makes aggregations
server-owned, callers cannot reach this channel at all — and removing the walk changes no result for
any caller-supplied attack (§1.1a, mutation 3). That is not an argument for dropping it. What it
defends against is *us*: §B.5a puts the `study.assets.*` leaf enumeration in duos-ui's asset
definitions, and OPEN-9 warns the backend copy will drift. A drifted enumeration makes the **server**
ask Elasticsearch for an internal leaf, where neither the strip list nor the field validator is
involved and the caller has done nothing wrong. The response-side projection is the only control left.
Keep the walk, and understand it as protecting the boundary against our own mistake rather than
against an attacker.

### §F.3 — What this costs

Stating it plainly, because the estimates in the plan predate all of this. Note that §F.1's closed
vocabulary makes E-1 *smaller* than the validator design it replaced — the aggregation-shape validator
disappears, and with it the recursive walk over caller `aggs`:

- **E-1** loses the arbitrary-`aggs` validator and gains a depth-aware strip list plus a
  field-reference check over `query`/`sort`/`highlight` only. **M, unchanged** — the security
  improvement is free relative to the original estimate.
- **New ticket E-0 — server-owned aggregation vocabulary.** The three shapes of §F.1, built server-side
  and selected by `(tab, filters, page, size, sort)`. **L.** This is net new work and the real cost of
  the section, but it is the first increment of plan item 4.2 rather than throwaway scaffolding
  (§F.1a).
- **E-3 grows** from a single-path `_source` filter to a multi-channel recursive response filter.
  **M → L.** Its `aggregations.**` channel stays in scope even with server-owned aggs — not as defence
  in depth against callers, who can no longer reach it, but because a drifted server-side enumeration
  can (§F.2).
- **D-3 gains a dependency** on E-0, E-1 **and E-3**, and is no longer "just" role-descriptor
  generation. The E-3 dependency is the one added by measurement (§B.5c): the native path's FLS grant
  has to be wider than the response bundle, so something has to narrow the response, and native FLS is
  not it.
- **E-3 is no longer fallback-only either.** The original framing had exactly one shared component (the
  mediator); it now has three, which is most of Epic E.
- **The mediator is no longer fallback-only**, so it moves out of Epic E's exclusive ownership. Either
  hoist E-0/E-1/E-3 into a shared epic, or have Epic D depend on Epic E's components — the plan's
  current "D **or** E" framing (`elasticsearch-service-duos-ui-usage.md:1795`) no longer holds and
  should be corrected to "E-0/E-1/E-3 always, D-1..D-5 additionally where licensed." With E-3 now
  shared as well, hoisting is the better of the two: what is left in Epic E after E-0/E-1/E-3 move out
  is only E-2 and E-4.
- **The proof of concept is already written** (§1.1a), so D-5/E-5's cost is replacing a model with real
  components rather than authoring a corpus. Net **reduction** against the plan's estimate for both.
- **duos-ui** stops sending `aggs` and instead names a tab and filters. Nine of eleven tabs share
  shape 2, so the change is concentrated in `definition.ts` and `useLibraryData.ts` rather than spread
  across all eleven asset files.

---

## Status

**§A complete. §B corrected and complete. §F revised and exercised end-to-end. No blockers: OPEN-8
is resolved by measurement, so D-3 is unblocked — with a wider FLS grant than it specified.**

The design is now backed by a running proof of concept rather than by reasoning alone
(§1.1a): `ElasticSearchLeakDefensePocTest` runs 26 exfiltration attempts and 7 legitimate requests
against a real cluster under four enforcement configurations, and demonstrates that today's endpoint
leaks by every route tried, that Epic D as originally specified still leaks, and that the enforcement
this contract now describes closes all of it while the product keeps working. That test is the reason
four statements in this document changed; each is marked where it lands.

This replaces the previous "complete and unblocked," which was wrong in three ways that review
caught, and then in two more that only running it revealed. Recording them here rather than quietly
editing them out, because most are process findings and the process is what let them through:

1. **Decision 1 over-promised.** It asserted that restricted documents cannot contribute to counts or
   aggregations. Elastic documents the opposite for DLS. Corrected in §1.1: the guarantee holds only
   over a mediated query surface, which makes query mediation a prerequisite of *both* enforcement
   paths rather than a fallback-only control. **Sharpened again by measurement:** the two examples
   §1.1 originally led with do not reproduce on 9.4.4, while `significant_terms` and `explain` do. The
   conclusion survived; the evidence did not.
2. **The §B audit was scoped to the wrong files.** It read two files in `data_search/**` and concluded
   five fields had no consumers. All five are live in `data_library/**`, which it never opened
   (§B.0a) — and `study.assets.*`, dismissed as an unconsumed dynamic map, turns out to be the
   backbone of nine of the library's eleven tabs (§B.5a). The lesson is in §B.0: the audit scope is
   *every caller of the search endpoint*, and that set is enumerable in one `git grep`.
3. **Enforcement was specified per-channel where the caller controls many channels.** E-1 stripped
   top-level keys only; E-3 filtered `hits._source` only. The client's own `STUDIES_AGG` sends a
   nested `_source` wildcard that defeats both (§B.5b). §F now states the requirement for the request
   and response sides together.

A fourth correction is internal to this revision and worth keeping visible, because it is the same
mistake as finding 1 in a new place. §F.1 initially proposed *validating* caller-supplied aggregation
DSL — field allowlist plus a denylist of `min_doc_count: 0`, `significant_terms`, and
`significant_text`. That would not have delivered the guarantee either: count-and-existence leaks ride
on legitimately queryable fields, so the field allowlist is irrelevant to them and a three-item shape
denylist was doing all the real work, against an open grammar that grows every ES release. §F.1 now has
the server construct aggregations from a closed three-shape vocabulary (new ticket **E-0**), which
closes the class by construction. It is also *less* code than the validator it replaced, and the first
increment of a plan item that was already written down (§F.1a, OPEN-11).

Two further corrections came out of building the proof of concept, and they belong on this list
because neither was findable by reading:

5. **Two enforcement rules were not implementable as written.** E-1's "add `fields` to the strip list"
   plus "strip at every depth" breaks the library search box and every highlighted column, because
   `fields` is a legitimate member of `multi_match`/`query_string`/`highlight` (§F.1 rule 1). §F.2's
   "filter the sort channel against the RESPONSE-VISIBLE allowlist" cannot be done at all, because
   sort values carry no field names (§F.2). Both were written by analogy with the channels either side
   of them, and both would have been implemented, reviewed, and shipped.
6. **The FLS grant was specified as the RESPONSE-VISIBLE list, which does not work.** A non-granted
   path is unmatchable (OPEN-8) and a granted multi-field does not carry its `.keyword` subfield
   (§B.5c). The second is the more dangerous: it produces wrong pagination rather than an error, and
   §F.1's `.keyword` normalization rule actively conceals it by accepting a reference the grant will
   not serve.

Findings 2 and 3 share a root cause worth naming: **the contract was written against the model classes
and inferred its consumers, rather than reading them.** §B.6's mechanical check (every serialized path
must appear in §B) would not have caught any of this, because the defect was in the direction the
check does not run — a path *classified* but classified wrong. OPEN-9's contract test is the version
that runs in the useful direction.

Findings 1, 5 and 6 share a different one, and it is the reason the proof of concept was worth
building: **the contract asserted behaviors of Elasticsearch, and of its own rules, that nothing
executed.** Three of the five were wrong. Two more were caught only by deliberately weakening the
model to see whether the test noticed — it did not, twice (§1.1a) — which is worth remembering the
next time a control looks obviously necessary. Every Elasticsearch behavior this document now relies on
has a named test, and each of those tests carries a failure message saying what to re-derive here if a
version bump changes the answer.

What is safe to build on now: §A's dimension matrix is unchanged by all of this and B-1/B-3/C-1 remain
unblocked. §B's corrected classification is usable, with the §B.5a enumeration and OPEN-9 sync
mechanism outstanding. D-3 is unblocked, on §B.5c's grant rather than its own. §F has been exercised
but not reviewed, and it still materially changes E-1/E-3's estimates (§F.3) — and now D-3's, which
gains a dependency on E-3.

The remaining §E items other than OPEN-9 are proposed *changes* — expanding access (OPEN-3, OPEN-5),
tightening it (OPEN-7), or fixing a defect (OPEN-6) — each with a default that preserves what the
application does today. They can be answered on their own schedule. None of them blocks anything.

Two things this contract does **not** cover, deliberately:

- **Document-scoped field access.** Decision 2 establishes that native FLS cannot do it and that
  nothing currently needs it. If that changes, this document is where the change starts, and the
  answer is application projection rather than a cleverer FLS grant.
- **The non-search endpoints.** `DatasetService` keeps its own document-scoped checks and its own
  richer projections. This contract governs the search index only; C-2 is what keeps the two from
  drifting apart.
