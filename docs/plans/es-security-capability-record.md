# Elasticsearch Security Capability Record — Ticket A-1

Written record of the Elasticsearch security feature inventory for the three Consent
environments, plus the Epic D / Epic E decision that depends on it.

Companion to
[`elasticsearch-service-duos-ui-usage.md`](elasticsearch-service-duos-ui-usage.md) (Ticket A-1).

## How this record is produced

One tool: [`/api/elasticSearch/capabilities`](../../src/main/java/org/broadinstitute/consent/http/resources/ElasticSearchCapabilityResource.java),
which reports the full inventory for whichever cluster a deployment is pointed at — inferred on `GET`,
proven on `POST`. All it needs is an Admin token for that environment.

The two methods are the two modes. `GET` creates nothing; `POST` runs the write probes. The split is
deliberate rather than a query flag: minting credentials on a cluster is a side effect, and a URL
that does it on `GET` is one a prefetcher, a monitoring crawler, or a shared bookmark can fire
without anyone deciding to.

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
curl -s -X POST -H "Authorization: Bearer $(gcloud auth print-access-token)" \
    'https://<env-host>/api/elasticSearch/capabilities' \
    | tee "es-capability-<env>-$(date +%F).json" | jq
```

To measure a cluster no deployment points at — a new environment, or a throwaway container used as a
control — point a local Consent deployment's `elasticSearch` configuration block at it and call the
endpoint against that.

### Running the capability endpoint

```shell
# GET: read-only. Safe anywhere, but DLS/FLS/API-key verdicts are inferred from the license tier.
curl -s -H "Authorization: Bearer $(gcloud auth print-access-token)" \
    https://<env-host>/api/elasticSearch/capabilities | jq

# POST: proven instead of inferred — creates and tears down a short-lived key and role.
curl -s -X POST -H "Authorization: Bearer $(gcloud auth print-access-token)" \
    'https://<env-host>/api/elasticSearch/capabilities' | jq

# Optionally probe run_as against a specific username rather than the credential's own principal
# (accepted by both methods)
curl -s -H "Authorization: Bearer $(gcloud auth print-access-token)" \
    'https://<env-host>/api/elasticSearch/capabilities?runAsUser=some-user' | jq
```

**Read-only mode (`GET`)** creates, modifies, and deletes nothing. That safety is what costs certainty:
DLS, FLS, and API-key support cannot be proven without writing, so they come back as
`INFERRED_SUPPORTED` / `LICENSE_BLOCKED` reasoned from the license tier and the cluster's
`xpack.security.dls_fls.enabled` setting. Only `run_as` (a header on a read-only request) and X-Pack
Security itself are observed.

**Write-probe mode (`POST`)** mints a short-lived API key and authenticates as it, creates a role carrying
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
  constraint Epic D has to work within. If it holds neither `manage_security` nor a key-minting
  privilege (`manage_own_api_key` or `manage_api_key`), the write probes cannot run and the report
  says so explicitly rather than reading their refusal as a verdict against the native path (see the
  decision table below).
- **`security_settings`** — filtered to the dozen or so values that gate a capability, out of the
  ~50 defaults a cluster reports. `xpack.security.dls_fls.enabled` is the one to read alongside the
  license: set to `false` it switches DLS and FLS off cluster-wide whatever the tier entitles the
  cluster to, and the report treats it as decisive.

One verdict distinction to keep straight in a write-probe run: `SUPPORTED` for DLS or FLS means the
filters were *enforced* end to end, while `INFERRED_SUPPORTED` means the cluster accepted them and
the enforcement check did not complete — an empty or unreadable index, or no usable probe key. A
cluster can store a DLS query and ignore it at search time, so acceptance is not enforcement, and
`notes` says which of the two you are reading.

**Scope: these are X-Pack probes, so they measure Elasticsearch only.** The endpoint identifies the
distribution from `GET /` and is otherwise scoped to Elasticsearch deployments; every environment
in the inventory below runs Elasticsearch.

## Environment inventory

### Local (rendered `config/docker-compose.yaml`) — measured 2026-07-29 with the write probes

Ticket A-0 is closed. Be precise about what that does and does not mean for anyone else's machine:
`/config/` is git-ignored (`.gitignore` L149) and rendered per developer by the Broad-internal
`firecloud-develop`, so **nothing in this repository sets any Elasticsearch default** — there is no
committed compose file for a change to land in. The cluster measured below is a local rendered copy,
edited to set `xpack.security.enabled` to **true** (overridable per-run with
`ES_SECURITY_ENABLED=false`) and to self-generate a **trial** license. The durable form of that
change is the `firecloud-develop` compose template, which is outside this repo and still needs an
owner — see the A-0 outcome in
[`elasticsearch-service-duos-ui-usage.md`](elasticsearch-service-duos-ui-usage.md). Until it lands,
each developer applies these settings themselves; see the notice below.

The endpoint has now been run against that local cluster in write-probe mode, so every row below is
an observation rather than an inference — this is the first environment where all five capabilities
came back `SUPPORTED`:

| Capability | Verdict | Evidence |
| --- | --- | --- |
| Elasticsearch version | 9.4.4 | `GET /` → `version.number` |
| Distribution | elasticsearch | `GET /` → `version.distribution` |
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

A local cluster does **not** pick these settings up on its own, and pulling this branch will not put
them anywhere: `config/docker-compose.yaml` is git-ignored and rendered per developer, so the file you
actually start ES with is not in this repository at all. Each developer has to enable these settings
in their own rendered copy before the endpoint will report anything like the table above — and a local
cluster that lags behind produces `UNAVAILABLE` / `LICENSE_BLOCKED` verdicts that read like findings
when they are only local drift.

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

| Capability | Basic, `GET` | Basic, `POST` | Trial, `GET` | Trial, `POST` |
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

> Call `POST /api/elasticSearch/capabilities` against dev with an Admin token and
> summarise the verdicts here — verdicts only, with the raw report attached to the ticket (see the
> note under `production` below). Production has since been measured and came back clean, so the
> write probes are no longer unproven in a *deployed* environment; what dev and staging now add is
> the other two thirds of the decision rule, and — if either runs a narrower credential than
> production's — the first reading of what a real least-privilege shared credential can do.

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

### `production` — measured 2026-08-05 with the write probes

Measured out of the order this document recommends: the write probes were run against production
before dev or staging. The run came back clean — five `SUPPORTED` verdicts, `write_probes_run: true`,
and no teardown failure in `notes` — so nothing was harmed by taking it first, but the sequencing
advice above stands for the environments still to be measured.

Production is an **Elastic Cloud** deployment on an **enterprise** license, and every capability was
observed rather than inferred:

| Capability | Verdict | Evidence |
| --- | --- | --- |
| Elasticsearch version | 9.x — one minor behind the pinned client, same major | `GET /` → `version.number` |
| Distribution | elasticsearch | `GET /` → `version.distribution` |
| Edition / license | Elastic Cloud, `enterprise`, `status: active` | `elastic_cloud: true`; `GET /_license` |
| X-Pack Security enabled | **`SUPPORTED`** | `GET /_xpack` 200; `GET /_security/_authenticate` 200 |
| DLS | **`SUPPORTED` — enforced, not merely accepted** | a `match_none` DLS key returned **none** of the documents the shared credential can see from `GET /dataset/_search` |
| FLS | **`SUPPORTED` — enforced** | a key granting only `datasetIdentifier` returned documents carrying only that field |
| API keys | **`SUPPORTED`** | key created, authenticated, invalidated |
| `run_as` | **`SUPPORTED`** (self-impersonation only — see below) | the `es-security-runas-user` header was honoured and the request resolved to the named principal |
| Credential privileges | the deployment's credential holds the key-minting privilege Epic D needs; the full block is on the ticket, not here | `POST /_security/user/_has_privileges` |
| `dataset` index | non-empty | so the `match_none` DLS result means enforcement rather than an empty index |
| `elasticsearch-rest-client` (POM) | 9.4.4 against a cluster one minor older — same major | `rest_client_compatibility` could not read the client version in the deployed jar at the time of this run; since fixed (see below) |
| Recommendation | Epic D viable here, **observed** | probe role and keys carrying DLS/FLS descriptors accepted *and* enforced |

Teardown behaved as documented: three short-lived keys and one probe role were created under the
`duos-capability-probe` / `duos_dlsfls_probe` names and removed again, with no teardown failure
reported in `notes`.

> **Environment specifics are deliberately not in this file, because this repository is public.**
> The full report — the principal the deployment authenticates as, its roles, the complete
> `cluster_privileges` block, and the `security_settings` dump including the cluster's audit setting
> — is attached to **DT-3826**, where access is already scoped. What is kept here is what the Epic D
> / Epic E decision rule actually consumes: whether security is on, whether DLS and FLS are licensed
> *and enforced*, and whether the deployment can mint keys. When filling in the rows for dev and
> staging below, summarise the verdicts the same way and attach the raw report to the ticket rather
> than pasting it here.

Four things in this run are worth carrying forward as their own findings.

**Epic D's per-request key minting is not privilege-blocked in production.** The note further down
predicted the opposite — that the shared `authUser` almost certainly does not hold a key-minting
grant — and production contradicts it in the permissive direction, so there is no privilege to request
from infra before Epic D can proceed there. Two caveats travel with that. The run says nothing about
what a *least-privilege* credential could do, exactly as the local superuser run said nothing about
the deployed ones; and the breadth of what that credential does hold is worth reviewing on its own
terms, independent of this work — raised on DT-3826. Epic D itself needs only `manage_own_api_key`,
which is the grant to ask for if that credential is ever narrowed.

**The `run_as` evidence is self-impersonation.** The probe resolved the credential's own principal to
itself, which shows the cluster accepts and honours the header but not that it will resolve a
*different* principal. If Epic D's design leans on `run_as` rather than on per-request keys, re-run
with `?runAsUser=<some-other-user>` to settle it; the DLS/FLS verdicts do not depend on this.

**Do not assume cluster-side audit logging.** Under Epic D the per-request API key *is* the
access-control decision, so on a cluster with auditing off nothing on the cluster side records which
key read what. Whatever audit trail the access contract needs must therefore come from the Consent
side unless auditing is known to be enabled on that deployment — read
`xpack.security.audit.enabled` out of the report per environment rather than assuming either way, and
treat enabling it as an infra ask.

**Elastic Cloud reserves some cluster settings to Elastic's operators.** That gates none of DLS, FLS,
API keys, or `run_as` — all four were observed working — but it does mean any future infra ask
(enabling audit, for instance) is a support request rather than a settings change.

#### Why `rest_client_compatibility` came back indeterminate — since fixed

The field reported that it "could not determine the client or cluster version at runtime," which reads
like a gap but was not a finding about the cluster. The cluster version was known (it is in the
report's own `version` field); the *client* version was not, because
`ElasticSearchCapabilityService` read it from
`RestClient.class.getPackage().getImplementationVersion()`, and the shade plugin strips dependency
manifests (`META-INF/MANIFEST*`, `**/pom.properties`) when assembling the deployable uber jar. Locally,
where the client keeps its own jar, the same code reported the 9.4.4 match — so the field would have
stayed indeterminate in *every* deployed environment while working fine everywhere it did not matter.

Fixed: the version now falls back to `elasticsearch.rest.client.version` in `mvn.properties`, the
build-time property file `properties-maven-plugin` already generates from the pom (the same mechanism
`SwaggerResource` uses), with the dependency version promoted to a pom property so it lands there.
The package lookup is still tried first, since it reports the jar actually loaded rather than the one
the build pinned. Verified against the built `consent.jar`: the package version is null there,
reproducing the production symptom exactly, and the fallback resolves 9.4.4. **A re-run against
production should now name the client version rather than declining to.**

The substantive question it would have answered is settled anyway: the pinned 9.4.4 client ran against
a cluster one minor older, a gap within the same major and the same shape as the 9.3.3 control-cluster
run recorded below, and every security call in this run succeeded.

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
(removed in 8.x) nor the new typed Java API client is required.

`ElasticSearchCapabilityService` is the standing demonstration of that conclusion, which is why no
separate feasibility test is kept: it drives the same security APIs from inside the application
through the injected `RestClient`, so a successful response from the endpoint in any environment is
itself evidence that the transport reaches `/_security` there. The report says as much in its
`rest_client_compatibility` field.

## Decision

**Pending** — blocked on the `dev` and `staging` rows above; `production` is measured. The decision rule,
fixed in advance so the measurement determines the outcome:

| Measured state of the deployed clusters | Decision |
| --- | --- |
| Security enabled and DLS/FLS licensed in all three | **Epic D** (native DLS/FLS). Epic E only if a rollout-safety fallback is wanted. |
| Security enabled, license lacks DLS/FLS | **Epic E**, and raise the Platinum/Enterprise upgrade as a separate infra decision before committing to Epic D. |
| Security disabled anywhere | **Epic E** now; Epic D stays blocked on infra enabling X-Pack Security in that environment. |
| Environments disagree | **Both** — Epic E as the portable path, Epic D where licensed. The access contract from Ticket A-2 must be identical either way, so the enforcement layer stays swappable. |
| Write probes refused for lack of privileges | **Not a decision.** The probes measured the credential, not the cluster; fall back to the license reading and treat the missing `manage_security` / `manage_own_api_key` grant as its own prerequisite for Epic D. |
| DLS/FLS accepted but **not enforced** | **Epic E**, and treat it as a defect report to infra: a filter that is accepted and silently ignored is worse than one that is refused, and Epic D cannot be built on it. |
| DLS/FLS accepted, enforcement **not checked** (`INFERRED_SUPPORTED` from a write-probe run) | **Not a decision.** Acceptance is not enforcement. Fix what the notes say stopped the check — usually an empty or unreadable dataset index — and re-run the write probes before recording anything. |
| DLS/FLS licensed but `xpack.security.dls_fls.enabled=false` | **Epic E** until the setting is enabled. The license entitles the cluster to the feature; the setting switches it off cluster-wide, so it is an infra change, not a license one. |

Known so far: **local and production both fall in the first row** — security enabled, DLS and FLS
licensed *and* observed enforced. Production is the one that counts, since it is a deployed cluster
measured through its own deployment's credential; local closed Ticket A-0 but decides nothing on its
own.

That leaves the decision genuinely pending rather than merely unrecorded. The rule turns on all three
deployed clusters, and two of them are unmeasured — if dev or staging is on a lower license tier, or
has `dls_fls.enabled` off, the outcome is the "environments disagree" row (**both** epics, Epic E as
the portable path) rather than a clean Epic D. Production being on Elastic Cloud enterprise makes
that a real possibility rather than a formality: it is the environment most likely to carry the
strongest license, so the others cannot be assumed to match it.

One caveat that no further measurement will remove: production's credential is broadly privileged, so
its write probes have the same limitation local's did. They prove what the *cluster* licenses and
enforces — which is what the decision rule asks — but not what a least-privilege service credential
could do there. If that credential is ever narrowed, Epic D's minimum grant is `manage_own_api_key`.

## Notes for whoever runs this against the deployed clusters

- The shared `authUser` may or may not hold `manage_security` or `manage_api_key` — this was expected
  to be the binding constraint, and in production it turned out not to be. Do not carry that forward
  as an assumption about dev or staging; measure each, and keep the per-environment specifics on the
  ticket rather than in this file.
  The report's `cluster_privileges` block says exactly which it has, via
  `POST /_security/user/_has_privileges`. If it lacks them, that is itself a finding: Epic D's
  per-request key minting goes through `POST /_security/api_key`, which needs at minimum
  `manage_own_api_key` — the narrowest grant that authorises it, and so the one to ask infra for.
- `grant_api_key` is **not** a substitute. It authorises `POST /_security/api_key/grant`, which mints
  a key on behalf of another user from that user's own credentials — a different endpoint and a
  different design, and not the one these probes or Epic D use. The report's privilege check is
  scoped to the endpoint actually called, so a credential holding only `grant_api_key` is reported as
  unable to mint rather than predicted to work and then refused.
- Because the endpoint authenticates as the deployment's own configured credential, that block *is*
  the shared credential's privileges — there is no way to accidentally record an admin's instead,
  which is what Epic D actually has to work with at runtime. When the credential holds neither
  `manage_own_api_key` nor `manage_api_key`, API keys come back `NOT_PERMITTED` rather than
  supported: the distinction between "the cluster can" and "we can."
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
