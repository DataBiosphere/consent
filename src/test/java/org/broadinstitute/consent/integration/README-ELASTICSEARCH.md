# Running the Elasticsearch tests

How to run the `ElasticSearch*` classes in this package, and what their failures mean.
[`README.md`](README.md) covers what they assert and why; this file is the operational half.

Everything here is tagged `elasticsearch`. The exclusion is *conditional*, and the condition is the
thing to know:

- **Locally, they run by default.** The `excludedTestGroups` property defaults to empty, so a plain
  `./mvnw test` includes them.
- **In CI they do not.** GitHub Actions sets `CI=true`, which activates the `ci` profile in `pom.xml`
  and sets `excludedTestGroups=elasticsearch`.

So `-DexcludedTestGroups=` is needed only when `CI=true` is set in your environment — which is true of
CI jobs and of some devcontainers. It is harmless otherwise, and the examples below include it so they
work in both cases.

## Prerequisites

- A running Docker daemon (`docker info` should succeed).
- Network access on first run: the image is ~700 MB and is pulled once, then cached.
- ~2 GB free RAM per container. A full run starts **three** Elasticsearch containers — one shared by
  every subclass of `ElasticSearchContainerTests`, one for `ElasticSearchBasicLicenseTest`, and one
  with security disabled for `ElasticSearchSecurityDisabledTest` — plus the Postgres container from
  `ContainerTests` if you run the whole suite.

## Commands

Run one class — usually what you want, and the fastest loop:

```bash
./mvnw test -Dtest=ElasticSearchLeakDefensePocTest -DexcludedTestGroups=
```

Run every Elasticsearch test:

```bash
./mvnw test -Dgroups=elasticsearch -DexcludedTestGroups=
```

Run a single test method:

```bash
./mvnw test -Dtest='ElasticSearchLeakDefensePocTest#open8_aPathOutsideTheFlsGrantIsNotQueryable' \
  -DexcludedTestGroups=
```

Skip them locally the way CI does:

```bash
./mvnw clean test -DexcludedTestGroups=elasticsearch
```

Prefer `-Dgroups=elasticsearch` over a name pattern such as `-Dtest='ElasticSearch*Test'`: the
pattern also picks up the unrelated `ElasticSearchServiceTest` and `ElasticSearchSupportTest` unit
tests, and it silently matches nothing new when a class is added under a different name.

Two things that are easy to get wrong:

- **`-DexcludedTestGroups=` (empty value) when `CI=true` is set.** Without it the run silently reports
  `Tests run: 0` and succeeds, which reads as a pass. A command-line property overrides the profile;
  that is the whole reason the override exists.
- **Use the `test` phase, not `surefire:test`.** Invoking the plugin goal directly skips JaCoCo's
  `prepare-agent`, which leaves `@{argLine}` unresolved and the forked JVM dies with
  `Error: could not open '{argLine}'`. That failure looks like a container problem and is not one.

## From the IDE

No extra configuration is needed, and nothing is excluded — the container starts automatically when
the test class is loaded.

## The leak-defense proof of concept

`ElasticSearchLeakDefensePocTest` runs 45 exfiltration attempts and 7 legitimate requests against a
real cluster under four enforcement configurations — today's endpoint, Epic D as originally
specified, Epic D with query mediation, and Epic E. It is the source of the measurements recorded in
`docs/plans/es-access-contract.md` §1.1a.

The enforcement it tests is modeled in `ElasticSearchAccessContractModel` rather than implemented in
`src/main`, because tickets E-0/E-1/E-2/E-3 do not exist yet. Everything else is real: real
documents, real caller DSL, real API keys carrying real DLS/FLS role descriptors. When those tickets
land, D-5 substitutes the production components for the model — **it does not write a new suite.**
Its fixtures — documents, callers, allowlists and the leak-marker scheme — live in
`ElasticSearchAccessContractFixtures`.

Three things to know before changing it:

- **`UNMEDIATED` and `NATIVE_UNMEDIATED` must stay.** They assert that the attacks still work against
  an unprotected endpoint. Delete them and every "defended" assertion can pass vacuously.
- **Leak detection scans the whole serialized response**, not named paths. That is deliberate: the
  contract requires unrecognized response channels to fail closed, and a test that inspected
  `hits.hits[*]._source` would share the exact blind spot it exists to catch.
- **Mutation-test it after any change to the model.** Two of four deliberate weakenings were *not*
  caught on the first attempt, both in controls that looked obviously necessary. The four to try:
  remove `aggs` from the strip list, make `injectsAuthorizationFilter()` return `false`, narrow
  `filterResponse` to `hits.hits` only, and add `sort` to `RETAINED_HIT_KEYS`. All four must fail.
- **Coverage of the DSL is by enumeration, not by inspiration.** Two table-driven sweeps —
  `everyUnsupportedQueryClauseIsRefusedByName` and `noUnsupportedRequestMemberReachesElasticsearch` —
  walk every clause in Elasticsearch's Query DSL reference and every top-level member of the Search
  API reference. On a version bump, diff those two lists against the references rather than trusting
  them; a clause added in a later version is on neither list, which is the whole reason the control
  is an allowlist.
- **Four measurements are about the cluster and the mapping rather than the mediator**, and they fail
  in ways no request-side test would notice: `copyToAndFieldAliasBothReachAroundTheFlsGrant` (a
  `copy_to` reaches around the FLS grant), `aSecondRoleDescriptorOnTheApiKeyUnionsAwayDocumentAndFieldSecurity`
  (role descriptors union, they do not intersect),
  `aQueryableButResponseInternalFieldIsRecoverableByBinarySearch` (QUERYABLE implies readable), and
  the OPEN-8 pair. Read their failure messages rather than adjusting them — each names what to
  re-derive in `es-access-contract.md` if the answer has changed.
- **Two controls cannot be mutation-tested this way, and have bespoke assertions instead.** The
  closed shape allowlist (`SUPPORTED_QUERY_CLAUSES`) and the aggregation-vocabulary check leak
  nothing `assertNoLeak` can see: one discloses through a *hit count* over documents the caller is
  authorized for, the other through a bucket key that looks exactly like a legitimate facet. Weaken
  either and the whole corpus still passes. The two tests in section 7 cover them by running the
  mediator with the control switched off — `mediateWithoutRequestValidation` — and asserting what it
  was holding. Any new control whose absence the corpus cannot detect needs the same treatment; if
  you add one and every test still passes, that is the signal, not the reassurance.

## Index separation and score statistics

`ElasticSearchIndexSeparationTest` measures the two facts contract §A.3 rests on, for the two-index
split recorded there as OPEN-10's structural remedy. It shares the same container and needs no
license — these are scoring measurements, not security enforcement.

Its corpus is two single-shard indices with deliberately opposite document frequencies for one term
and a **byte-identical document in both**. The twin is the whole design: same text, same query, so
any score difference is a difference in the statistics it was scored against and nothing else. One
shard per index means "per shard" and "per index" cannot be confused.

Results, both recorded in §A.3: term statistics **stay per index** across a multi-index search under
the default `query_then_fetch` (the twin scores identically alone and merged, to exact equality),
`dfs_query_then_fetch` **pools** them, and the merged ranking is distorted **13.6×** for identical
content. One number is printed rather than asserted — the exact ratio is a property of this corpus,
so pinning it would fail on any scoring change without telling anyone anything.

If the `dfs_query_then_fetch` control ever stops pooling statistics, `addingARestrictedIndexToThe...`
starts passing for the wrong reason: it would then pass whether or not separation is real. Read that
test's failure message before adjusting either.

For a version bump it matters differently from the other four classes, which assert that a
*capability* exists. This one pins *behaviors* of DLS and FLS that the access contract's reasoning
depends on — which aggregations DLS isolates, whether a non-granted field stays queryable, whether a
granted multi-field carries its `.keyword` subfield. Those are the assertions most likely to change
quietly across versions, so read their failure messages rather than adjusting them. Each one names
what to re-derive in `es-access-contract.md` if the answer has changed.

## Troubleshooting

**`Error: could not open '{argLine}'`** — you ran `surefire:test` instead of the `test` phase. See
above.

**`Tests run: 0`, build succeeds** — `CI=true` is set in your environment, so the `ci` profile is
excluding the `elasticsearch` tag. Add `-DexcludedTestGroups=`. This is the failure mode most likely to
be mistaken for a pass.

**`this cluster's trial is spent`** — a trial can be started once per major version per cluster. Every
test here uses a fresh container, so this means the container was reused; stop the leftover container
(`docker ps` / `docker rm -f`) and re-run.

**`no license after 60000ms`** — the container came up but never published its self-generated license.
Usually memory pressure from several containers at once. Run one class at a time.

**TLS or certificate errors** — the containers are forced onto plain `http`, because
`ElasticSearchSupport.createRestClient` has no `SSLContext` hook and cannot trust the image's
self-signed CA. A test that needs TLS must build its own client from the container's
`caCertAsBytes()`. `ElasticSearchContainerTests`' javadoc has the full explanation.

**Containers left running after a crash** — Testcontainers reaps them via Ryuk at JVM exit. If the JVM
was killed, `docker ps | grep elasticsearch` and remove them by hand.
