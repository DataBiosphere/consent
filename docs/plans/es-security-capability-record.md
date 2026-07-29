# Elasticsearch Security Capability Record — Ticket A-1

Written record of the Elasticsearch security feature inventory for the three Consent
environments, plus the Epic D / Epic E decision that depends on it.

Companion to
[`elasticsearch-service-duos-ui-usage.md`](elasticsearch-service-duos-ui-usage.md) (Ticket A-1).

## How this record is produced

One tool: [`GET /api/elasticSearch/capabilities`](../../src/main/java/org/broadinstitute/consent/http/resources/ElasticSearchCapabilityResource.java),
which reports the full inventory for whichever cluster a deployment is pointed at — inferred, or with
`writeProbes=true` proven. All it needs is an Admin token for that environment.

Each environment already runs its own Consent deployment holding its own cluster credential, so that
token yields the per-environment record without anyone obtaining cluster network access or a copy of a
secret. Nothing is read from a secret store; the endpoint uses the credential its own deployment is
already configured with.

Two earlier tools did the same job from outside the application and have been removed: a
`scripts/es-security-audit.sh` that reimplemented the whole verdict matrix in untested bash, and an
`ElasticSearchSecurityProbeTest` that drove the security APIs through the production
`ElasticSearchSupport.createRestClient` path but was inert unless `ES_PROBE_URL` was set, so it never
ran in CI and never guarded anything. `ElasticSearchCapabilityService` supersedes both: it makes the
same calls from inside the application, its verdict logic is unit-tested, and a successful response
from it in any environment is itself the client-compatibility evidence the probe test was written to
supply. Two implementations that can disagree about something as consequential as "is DLS enforced
here" are worse than one that is tested.

To capture a report file, redirect the endpoint's JSON:

```shell
curl -s -H "Authorization: Bearer $(gcloud auth print-access-token)" \
    'https://<env-host>/api/elasticSearch/capabilities?writeProbes=true' \
    | tee "es-capability-<env>-$(date +%F).json" | jq
```

To measure a cluster no deployment points at — a new environment, or a throwaway container used as a
control — point a local Consent deployment's `elasticSearch` configuration block at it and call the
endpoint against that.

### Running the capability endpoint

```shell
# Read-only. Safe anywhere, but DLS/FLS/API-key verdicts are inferred from the license tier.
curl -s -H "Authorization: Bearer $(gcloud auth print-access-token)" \
    https://<env-host>/api/elasticSearch/capabilities | jq

# Proven instead of inferred: creates and tears down a short-lived key and role.
curl -s -H "Authorization: Bearer $(gcloud auth print-access-token)" \
    'https://<env-host>/api/elasticSearch/capabilities?writeProbes=true' | jq

# Optionally probe run_as against a specific username rather than the credential's own principal
curl -s -H "Authorization: Bearer $(gcloud auth print-access-token)" \
    'https://<env-host>/api/elasticSearch/capabilities?runAsUser=some-user' | jq
```

**Read-only mode** creates, modifies, and deletes nothing. That safety is what costs certainty: DLS,
FLS, and API-key support cannot be proven without writing, so they come back as `INFERRED_SUPPORTED`
/ `LICENSE_BLOCKED` reasoned from the license tier. Only `run_as` (a header on a read-only request)
and X-Pack Security itself are observed.

**`writeProbes=true`** mints a short-lived API key and authenticates as it, creates a role carrying
both a DLS query and an FLS grant, then uses keys whose `role_descriptors` carry those filters
against the real dataset index to check the cluster *enforces* them: a `match_none` DLS key must
return zero of the documents the shared credential can see, and a key granting one field must return
only that field. That distinction is the whole point — a Basic-licensed cluster accepts a key
carrying a DLS descriptor at creation and fails only later at search time, which no license
inference can tell you and which this probe reproduces exactly. Everything created is namespaced
`duos-capability-probe-*` / `duos_dlsfls_probe_*`, expires within 10 minutes regardless, and is torn
down before the response returns; a teardown that fails is reported in `notes` rather than left for
you to find in the logs.

Three fields carry most of the interpretive weight:

- **`write_probes_run`** — read this first. It tells you whether the DLS/FLS/API-key verdicts below
  are observations or inferences.
- **`cluster_privileges`** — what the deployment's *own* shared credential may do, which is the
  constraint Epic D has to work within. If it holds neither `manage_security` nor `manage_api_key`,
  the write probes cannot run and the report says so explicitly rather than reading their refusal as
  a verdict against the native path (see the decision table below).
- **`security_settings`** — filtered to the dozen or so values that gate a capability, out of the
  ~50 defaults a cluster reports.

## Environment inventory

### Local (`config/docker-compose.yaml`) — measured 2026-07-29 with `writeProbes=true`

Ticket A-0 is closed: the compose file now sets `xpack.security.enabled` to **true** by default
(overridable per-run with `ES_SECURITY_ENABLED=false`) and self-generates a **trial** license, so the
security features are exercisable locally as shipped. The endpoint has now been run against the
running local cluster in write-probe mode, so every row below is an observation rather than an
inference — this is the first environment where all five capabilities came back `SUPPORTED`:

| Capability | Verdict | Evidence |
| --- | --- | --- |
| Elasticsearch version | 9.4.4 | `GET /` → `version.number` |
| Distribution | elasticsearch (not OpenSearch) | `GET /` → `version.distribution` |
| Edition / license | Trial (Platinum-equivalent), `status: active`, expires 2026-08-28 | `GET /_license` → `type: trial` |
| X-Pack Security enabled | **`SUPPORTED`** | `GET /_xpack` 200; `GET /_security/_authenticate` 200 |
| DLS | **`SUPPORTED` — enforced, not merely accepted** | a `match_none` DLS key returned **0 of 1158** documents from `GET /dataset/_search` |
| FLS | **`SUPPORTED` — enforced** | a key granting only `datasetIdentifier` returned documents carrying only that field |
| API keys | **`SUPPORTED`** | key created (`POST /_security/api_key` 200), authenticated as `elastic`, invalidated |
| `run_as` | **`SUPPORTED`** | `es-security-runas-user: elastic` honoured, request resolved to `elastic` |
| Credential privileges | all six probed privileges true: `manage_security`, `manage_api_key`, `grant_api_key`, `manage_own_api_key`, `read_security`, `monitor` | `POST /_security/user/_has_privileges` as `elastic` (`superuser`) |
| Relevant cluster settings | `dls_fls.enabled=true`, `authc.api_key.enabled=true`, `authc.run_as.enabled=true`; `audit.enabled=false`, `authc.token.enabled=false`, both SSL layers off | `security_settings` in the report |
| `dataset` index | 1158 docs | non-empty, so the `match_none` DLS result means enforcement rather than an empty index |
| `elasticsearch-rest-client` (POM) | 9.4.4 | `rest_client_compatibility`: matches cluster major 9 |
| Recommendation | Epic D viable here, **observed** | probe role and keys carrying DLS/FLS descriptors accepted *and* enforced |

Teardown behaved as documented: three short-lived keys and one probe role were created under the
`duos-capability-probe` / `duos_dlsfls_probe` names and removed again, with no teardown failure
reported in `notes`.

One caveat survives the measurement: the trial license is 30 days. After expiry the cluster silently
drops to `basic` and DLS/FLS revert to `LICENSE_BLOCKED` — worth recognising as a license expiry
rather than reading as a regression in the feature work.

Unlike the deployed environments, the local credential is the `elastic` superuser, so it holds the
`manage_security` / `manage_api_key` grants the write probes need. That makes local the one place the
probes are guaranteed *not* to be inconclusive — useful for exercising the probe path itself, and a
reminder that this clean local run says nothing about whether the shared credential in dev, staging,
or production can do the same. Read it as evidence that the *probe path and the cluster features*
work, not as a preview of the deployed rows.

#### Notice: developers must update their own local configuration

A local cluster does **not** pick these settings up on its own. `config/docker-compose.yaml` is
committed, but most people carry local edits to it (the bucket location, ports, memory limits) or run
a copy of their own, so a pull of this branch will not necessarily put these settings into the file
you actually start ES with. Each developer has to enable them in their own compose file before the
endpoint will report anything like the table above — and a local cluster that lags behind produces
`UNAVAILABLE` / `LICENSE_BLOCKED` verdicts that read like findings when they are only local drift.

What has to be true in your `config/docker-compose.yaml` (and any personal copy or override file you
run instead of it):

- `xpack.security.enabled=${ES_SECURITY_ENABLED:-true}` — without this the `/_security` API is absent
  and every security verdict follows from that one fact.
- `xpack.license.self_generated.type=${ES_LICENSE_TYPE:-trial}` — a `basic` license leaves DLS and
  FLS `LICENSE_BLOCKED`, so Epic D cannot be developed against.
- `ELASTIC_PASSWORD=${ELASTIC_PASSWORD:-devpassword}`, matching `authUser` / `authPassword` in
  `config/consent.yaml` — otherwise the deployment cannot authenticate at all.
- `xpack.security.transport.ssl.enabled=false` and `xpack.security.http.ssl.enabled=false` — keeps the
  HTTP layer on plain `http` so consent's `protocol: http` client keeps working with security on.
- The image at `docker.elastic.co/elasticsearch/elasticsearch:9.4.4`, which is the version measured
  above and the version of the pinned REST client.

Two things that trip people up, both consequences of state that outlives a compose edit:

- The self-generated license type only takes effect **the first time a cluster forms**. On an existing
  `elastic` volume that already registered a `basic` license, editing the compose file changes
  nothing; activate the trial once by hand:

  ```shell
  curl -u elastic:devpassword -XPOST 'localhost:9200/_license/start_trial?acknowledge=true'
  ```

  Or discard the volume and let the cluster form fresh.
- `api_key.enabled`, `run_as.enabled`, and `dls_fls.enabled` are cluster defaults and need no compose
  entry; if the report shows any of them false, something in your local setup has explicitly disabled
  it. `audit.enabled=false` and `authc.token.enabled=false` are expected and gate nothing this work
  needs.

DEVNOTES.md ("Developing with a local Elastic Search instance") carries the full workflow, including
getting the old security-disabled cluster back for a run with `ES_SECURITY_ENABLED=false` — which
remains fine for Epics A–C and E, since none of them need security.

### Control clusters (ES 9.3.3 and 9.4.4, security enabled) — measured 2026-07-28, 9.4.4 added 2026-07-29

Not a Consent environment. A throwaway container was run under both license tiers to
establish what each tier permits, so the deployed-environment results below can be read
against a known baseline — and so the verdict logic is validated in both directions rather
than only against a security-disabled cluster.

The exercise was run twice, on **9.3.3** and again on **9.4.4** — the latter being both the local
cluster's version and the version of the REST client in `pom.xml`, so the client is now known to work
against a same-version cluster and not only across a minor-version gap. Every verdict below was
identical on the two versions, in both license tiers and in both endpoint modes; the table therefore
records one set of results rather than two.

| Capability | Basic license | Trial (Platinum-equivalent) license |
| --- | --- | --- |
| X-Pack Security enabled | true | true |
| API keys | **supported** — created, authenticated, invalidated | **supported** |
| `run_as` | **supported** — header honoured, resolved to target user | **supported** |
| DLS | **blocked by license** — role with `indices[].query` rejected 403 | **proven end-to-end** — `match_none` API key returned 0 of 2 docs |
| FLS | **blocked by license** — role with `field_security` rejected 403 | **supported** — role accepted |

The operative finding: **API keys and `run_as` are Basic-tier features; DLS and FLS are not.**
Epic D therefore has a license dependency that Epic E does not, and a Basic-licensed cluster
will accept an API key carrying a DLS role descriptor at creation time and only fail at
search time with a 403 — a failure mode worth designing around.

#### The endpoint was validated against these same clusters

The endpoint was run against the control clusters in both modes and under both license tiers, on both
9.3.3 and 9.4.4. Its read-only inferences agree with the tier-by-tier measurements above, and its own
write probes independently reproduce them — so the verdicts have been checked rather than trusted:

| Capability | Basic, read-only | Basic, `writeProbes` | Trial, read-only | Trial, `writeProbes` |
| --- | --- | --- | --- | --- |
| X-Pack Security | `SUPPORTED` | `SUPPORTED` | `SUPPORTED` | `SUPPORTED` |
| API keys | `INFERRED_SUPPORTED` | `SUPPORTED` — created, authenticated, invalidated | `INFERRED_SUPPORTED` | `SUPPORTED` |
| DLS | `LICENSE_BLOCKED` | `LICENSE_BLOCKED` — key accepted, 403 at search | `INFERRED_SUPPORTED` | `SUPPORTED` — 0 of 2 docs through a `match_none` key |
| FLS | `LICENSE_BLOCKED` | `LICENSE_BLOCKED` — as above | `INFERRED_SUPPORTED` | `SUPPORTED` — only the granted field returned |
| `run_as` | `SUPPORTED` | `SUPPORTED` | `SUPPORTED` | `SUPPORTED` |
| Recommendation | Epic E | Epic E | Epic D | Epic D, observed |

Two things worth recording from that exercise. First, the Basic write-probe run reproduced the exact
failure mode this document warns about: the API key carrying a DLS role descriptor was **accepted at
creation** and failed only at search time with `current license is non-compliant`. Read-only
inference and write probes reach the same verdict there by different routes, which is the strongest
form of agreement available. Second, teardown was verified from the cluster side, not just trusted:
after the runs, `GET /_security/role/duos_dlsfls_probe_*` returned `{}` and every
`duos-capability-probe-*` key showed `invalidated: true`. Both held on 9.4.4 as well, including the
Basic-tier accepted-then-403-at-search behaviour — so that failure mode is not an artefact of one
minor version.

#### Running the probes as a least-privilege credential

Because the deployed environments' shared credential is unlikely to hold `manage_security`, the
probes were also run as a purpose-built user with only `monitor` plus `read` on the indices — the
shape the real service credential is expected to have. All six probed cluster privileges came back
false except `monitor`, every write was refused, and the report's conclusion is the important part:

> Inconclusive from the write probes: this deployment's credential is not permitted to create a role
> or an API key, so the DLS and FLS verdicts describe the credential rather than the cluster (see
> `cluster_privileges`). Re-run with a credential holding `manage_security` and `manage_api_key` to
> settle it. On the license alone: Epic D … is viable on this cluster …

That distinction matters more than it looks: a privilege refusal tells you nothing about whether the
cluster licenses DLS, so it must not be recorded as a verdict against the native path. The endpoint
falls back to the license reading and says which of the two you are looking at.

### `dev` — not yet measured

> Call `GET /api/elasticSearch/capabilities?writeProbes=true` against dev with an Admin token and
> paste the findings here. Dev is the right place to run the write probes first in a *deployed*
> environment — teardown has been confirmed on the control clusters and locally, so what dev adds is
> confirmation under a real shared credential rather than a superuser one.

| Capability | Verdict | Evidence |
| --- | --- | --- |
| Elasticsearch version | | |
| Distribution | | |
| Edition / license | | |
| X-Pack Security enabled | | |
| DLS | | |
| FLS | | |
| API keys | | |
| `run_as` | | |

### `staging` — not yet measured

| Capability | Verdict | Evidence |
| --- | --- | --- |
| Elasticsearch version | | |
| Distribution | | |
| Edition / license | | |
| X-Pack Security enabled | | |
| DLS | | |
| FLS | | |
| API keys | | |
| `run_as` | | |

### `production` — not yet measured

Call the endpoint read-only first — in that mode it creates nothing, so it cannot leave anything
behind on the production cluster. Only add `writeProbes=true` after the same call has been run in dev
and staging and the teardown has been confirmed there; the probes are designed to be safe in
production (namespaced, 10-minute expiry, torn down in a `finally`, failures reported in `notes`),
but production is not the place to find out.

If production's shared credential turns out to lack `manage_security`, the write probes there will be
inconclusive by construction. That is a finding to record rather than a problem to work around: it
means Epic D's per-request key minting needs a privilege grant before it can work in production at
all.

| Capability | Verdict | Evidence |
| --- | --- | --- |
| Elasticsearch version | | |
| Distribution | | |
| Edition / license | | |
| X-Pack Security enabled | | |
| DLS | | |
| FLS | | |
| API keys | | |
| `run_as` | | |

## REST client compatibility — resolved

`org.elasticsearch.client:elasticsearch-rest-client` **9.4.4** (`pom.xml:865-869`) is
compatible with every security API call the plan requires. Verified, not assumed: against live
security-enabled **9.3.3 and 9.4.4** clusters, a client built through the production
`ElasticSearchSupport.createRestClient` path successfully:

1. issued `GET /_security/_authenticate` and `GET /_xpack`;
2. issued `POST /_security/api_key` carrying a `role_descriptors` block with both a DLS
   `query` and an `field_security` grant;
3. authenticated a second `RestClient` as that API key;
4. invalidated the key via `DELETE /_security/api_key`.

The 9.4.4 run is the more direct evidence of the two, since it pairs the pinned client with a cluster
of its own version; the 9.3.3 run additionally shows the transport tolerates a minor-version gap
between client and cluster, which is what a deployed environment on an older minor would present.

No dependency change is needed. The low-level `RestClient` is a version-agnostic HTTP
transport with no typed request model, so security endpoints are reached with
`RestClient.performRequest(Request)` and a JSON entity — neither the high-level REST client
(removed in 8.x) nor the new typed Java API client is required. The one caveat is that this
holds for Elasticsearch; against OpenSearch there is no `POST /_security/api_key` at all, and
the endpoint flags that case explicitly.

`ElasticSearchCapabilityService` is the standing demonstration of that conclusion, which is why no
separate feasibility test is kept: it drives the same security APIs from inside the application
through the injected `RestClient`, so a successful response from the endpoint in any environment is
itself evidence that the transport reaches `/_security` there. The report says as much in its
`rest_client_compatibility` field.

## Decision

**Pending** — blocked on the `dev`, `staging`, and `production` rows above. The decision rule,
fixed in advance so the measurement determines the outcome:

| Measured state of the deployed clusters | Decision |
| --- | --- |
| Security enabled and DLS/FLS licensed in all three | **Epic D** (native DLS/FLS). Epic E only if a rollout-safety fallback is wanted. |
| Security enabled, license lacks DLS/FLS | **Epic E**, and raise the Platinum/Enterprise upgrade as a separate infra decision before committing to Epic D. |
| Security disabled anywhere | **Epic E** now; Epic D stays blocked on infra enabling X-Pack Security in that environment. |
| Environments disagree | **Both** — Epic E as the portable path, Epic D where licensed. The access contract from Ticket A-2 must be identical either way, so the enforcement layer stays swappable. |
| Write probes refused for lack of privileges | **Not a decision.** The probes measured the credential, not the cluster; fall back to the license reading and treat the missing `manage_security` / `manage_api_key` grant as its own prerequisite for Epic D. |
| DLS/FLS accepted but **not enforced** | **Epic E**, and treat it as a defect report to infra: a filter that is accepted and silently ignored is worse than one that is refused, and Epic D cannot be built on it. |

Known so far: the local environment now falls in the **first** row — security enabled, DLS and FLS
licensed *and* observed enforced — which closes Ticket A-0 but decides nothing on its own. The rule
above is about the deployed clusters, and local's superuser credential is not the shared credential
any of them use.

## Notes for whoever runs this against the deployed clusters

- The shared `authUser` almost certainly does **not** hold `manage_security` or `manage_api_key`.
  The report's `cluster_privileges` block says exactly which it has, via
  `POST /_security/user/_has_privileges`. If it lacks them, that is itself a finding: Epic D's
  per-request key minting needs at minimum `grant_api_key` (preferred, since it mints keys on
  behalf of a user without full `manage_api_key`).
- Because the endpoint authenticates as the deployment's own configured credential, that block *is*
  the shared credential's privileges — there is no way to accidentally record an admin's instead,
  which is what Epic D actually has to work with at runtime. When the credential holds neither
  `manage_api_key` nor `grant_api_key`, API keys come back `NOT_PERMITTED` rather than supported:
  the distinction between "the cluster can" and "we can."
- The end-to-end DLS check needs a non-empty index. The endpoint uses the configured
  `datasetIndexName` automatically, and says so explicitly when that index is empty or unreadable
  rather than reporting a false pass — an empty index makes a `match_none` key return zero documents
  for the wrong reason.
- Everything the write probes create is namespaced `duos-capability-probe-*` /
  `duos_dlsfls_probe_*` and expires in 10 minutes. Teardown is in a `finally` block, and any
  teardown failure is reported in the response `notes` rather than left in the server log.
- Every probe key carries a `role_descriptors` block, including the plain round-trip key, whose
  descriptor grants nothing at all. A key created without one would instead inherit a snapshot of
  the deployment credential's own permissions.
- Both DLS and FLS are checked for *enforcement*, not just acceptance: a `match_none` key must return
  zero documents, and a key granting one field must return only that field. Acceptance alone would
  pass on a cluster that stores the descriptor and ignores it.
