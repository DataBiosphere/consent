# Integration Testing

Provides a mechanism for running simple smoke tests against a fully running application stack.
The intention here is to keep this layer as slim as possible to provide a minimum sense of
confidence in application stability.

These tests exercise authenticated HTTP endpoints against a live `DropwizardAppExtension`-managed
application and a real database. They are **not** isolated unit tests.

## How the database is provided

[`ContainerTests`](ContainerTests.java) starts a Testcontainers `PostgreSQLContainer` in a
static initializer — before `DropwizardAppExtension` is constructed — and passes the
container's coordinates directly as `ConfigOverride` entries:

```java
ConfigOverride.config("database.url",             POSTGRES.getJdbcUrl()),
ConfigOverride.config("database.user",            POSTGRES.getUsername()),
ConfigOverride.config("database.password",        POSTGRES.getPassword()),
ConfigOverride.config("database.driverClass",     POSTGRES.getDriverClassName()),
ConfigOverride.config("server.applicationConnectors[0].port", "0"),
ConfigOverride.config("server.adminConnectors[0].port", "0"),
ConfigOverride.config("database.validationQuery", POSTGRES.getTestQueryString())
```

This means:

- **No local Postgres installation is needed** — the container is started automatically.
- **No CI database provisioning is needed** — the same container is used in CI.
- Dropwizard binds to ephemeral test ports, and tests use the `serviceUrl(...)` helper in
  [`ContainerTests`](ContainerTests.java) to build URLs for the active service instance.
- The hardcoded coordinates in `consent-ci.yaml` are never reached at runtime; they serve
  only as documentation of the expected schema.
- Testcontainers registers a JVM shutdown hook (via Ryuk) that stops the container when the
  test JVM exits — no manual teardown is required.

## How a security-enabled Elasticsearch cluster is provided

[`ElasticSearchContainerTests`](ElasticSearchContainerTests.java) is a separate base class for tests
that need Elasticsearch document- and field-level security (DLS/FLS). It starts a Testcontainers
`ElasticsearchContainer` in a static initializer and builds a client through the application's own
`ElasticSearchSupport.createRestClient`. Extend it directly; it is independent of `ContainerTests`
and does not start the Dropwizard application.

Two things are easy to get wrong here:

- **DLS/FLS needs a non-basic license, and getting one is an explicit step.** Each fresh container
  self-generates a *basic* license, and a DLS query or FLS field grant is rejected under it with
  HTTP 403 `current license is non-compliant for [field and document level security]`. A class that
  needs the trial calls `activateTrialLicense()` from its own `@BeforeAll`; the base class does not
  do it. That call goes through
  [`ElasticSearchAdminEndpoints`](ElasticSearchAdminEndpoints.java) to
  `POST /api/elasticSearch/license/trial`, the same admin endpoint an operator uses against a
  deployed cluster, so the activation path is itself covered rather than bypassed.

  It reads as ceremony until you notice what the alternative hides: a trial can be started only once
  per cluster and cannot be reverted, and doing it in the shared harness put that change in the setup
  of every subclass — including `ElasticSearchBasicLicenseTest`, whose entire subject is what a
  *basic* license refuses. Repeating the call is safe (the second caller gets `ALREADY_LICENSED`),
  which is what makes per-class activation practical on a cluster shared per JVM.

  Note that `POST /_security/api_key` accepts a DLS/FLS role descriptor even on a basic license — the
  rejection only surfaces on the search request.
- **"Container started" is earlier than "license readable."** Testcontainers considers the container
  ready once the HTTP layer answers on port 9200, but the self-generated basic license reaches the
  cluster state a beat later, and until it does `GET /_license` returns `404` with an empty body.
  A class that reads or changes the license must call
  `ElasticSearchTestCluster.awaitLicense(client)` first — the base class does, which is also what lets
  `activateTrialLicense()` read the tier before deciding anything. The window is about a second on an
  idle machine and wider during a full test run, where three of these containers boot alongside the
  Postgres one — which is why this surfaced as a full-suite-only failure.
- **The container defaults to HTTPS.** For image versions 8.0.0 and above,
  `ElasticsearchContainer` automatically applies `withPassword("changeme")` and `withCertPath(...)`,
  serving the HTTP layer over TLS with a self-signed CA. `ElasticSearchSupport.createRestClient` has
  no `SSLContext` hook and cannot trust that CA, so the base class forces
  `xpack.security.http.ssl.enabled=false` to match `config/docker-compose.yaml`. Tests needing TLS
  must build their own client from the container's `caCertAsBytes()` / `createSslContextFromCa()`.

The cluster is shared across subclasses in the same JVM, so use `recreateIndex(...)` for a
known-empty index rather than assuming an empty cluster.
[`ElasticSearchDlsFlsEnforcementTest`](ElasticSearchDlsFlsEnforcementTest.java) is the reference
usage.

## Qualifying a new Elasticsearch version

The security behavior Epic D depends on is not guaranteed by the version number — it depends on the
distribution, the license tier, and how the cluster fails when a grant it cannot enforce is used.
Four test classes assert all of it, so upgrading Elasticsearch is a one-line change plus a test run:

1. Change `IMAGE` in [`ElasticSearchTestCluster`](ElasticSearchTestCluster.java). It is the **only**
   Elasticsearch version pin in the test tree.
2. Run the suite:

   ```shell
   ./mvnw test -Dgroups=elasticsearch
   ```

3. Update `config/docker-compose.yaml`, the `elasticsearch-rest-client` version in `pom.xml`, and
   the version references in `DEVNOTES.md` to match.

| Class | Cluster state | What it protects |
| --- | --- | --- |
| [`ElasticSearchSecurityBaselineTest`](ElasticSearchSecurityBaselineTest.java) | secured, trial license | Reported version matches the pin; `build_flavor` is `default` (the OSS flavor has no X-Pack at all); X-Pack security is available and enabled; DLS, FLS and `run_as` roles are accepted; the trial is one-shot; the port serves plain `http` and not TLS |
| [`ElasticSearchBasicLicenseTest`](ElasticSearchBasicLicenseTest.java) | secured, **basic** license | A basic license refuses DLS/FLS roles with a 403, plain RBAC still works, an API key carrying a DLS/FLS descriptor is still *accepted*, and the search it is used for **fails closed** rather than returning unrestricted documents |
| [`ElasticSearchSecurityDisabledTest`](ElasticSearchSecurityDisabledTest.java) | security off | The compose default still answers unauthenticated, still tolerates configured credentials (which is why `consent.yaml` can carry them unconditionally), and serves no TLS |
| [`ElasticSearchDlsFlsEnforcementTest`](ElasticSearchDlsFlsEnforcementTest.java) | secured, trial license | DLS actually hides non-public documents and FLS actually strips ungranted fields |

The two license tiers exercise **the same role and role-descriptor payloads**, shared from
`ElasticSearchTestCluster`, so the only difference between "rejected" and "permitted" is the license.

The single most important assertion is
`ElasticSearchBasicLicenseTest.searchWithDlsFlsApiKeyFailsClosedUnderBasicLicense`. Elasticsearch
accepts a DLS/FLS API key on a basic license and only rejects it at search time; a future version
that instead ignored the unenforceable restriction would return every document to a caller expected
to see a subset. That test asserts the 403 and that no document contents appear in the response.

Cost: three containers (~35s), because the license tier and the security mode are per-cluster
properties — the trial cannot be un-started, and `xpack.security.enabled` cannot be toggled at
runtime. Classes that only need a secured cluster with a trial license should extend
`ElasticSearchContainerTests` and add no container of their own.

## Which of these run in CI

The Postgres-backed `ContainerTests` subclasses run in CI as part of the ordinary `mvn test`.

The four Elasticsearch classes do not. They carry the JUnit tag `elasticsearch`, and the `ci`
profile in `pom.xml` — activated by the `CI=true` GitHub Actions sets in every job — passes that tag
to surefire's `excludedGroups`. They start up to three Elasticsearch containers and exist to qualify
an Elasticsearch version or the Epic D security behavior, which is a deliberate act, not something
every PR should pay for. The tag sits on `ElasticSearchContainerTests` as well, so a new subclass
inherits the exclusion.

Locally, and in the IDE, nothing is excluded — `excludedTestGroups` defaults to empty. To exclude
them locally as CI does, or to include them in CI, override the property on the command line, which
takes precedence over the profile:

```shell
mvn clean test -DexcludedTestGroups=elasticsearch   # skip them, as CI does
mvn clean test -DexcludedTestGroups=                # run everything, even with CI set
```

## Running via Maven

```shell
# every integration test, Postgres- and Elasticsearch-backed alike
mvn clean test -Dtest="org.broadinstitute.consent.integration.**"

# only the Elasticsearch classes
mvn clean test -Dgroups=elasticsearch
```

Prefer `-Dgroups=elasticsearch` over a name pattern such as `-Dtest='ElasticSearch*Test'`: the
pattern also picks up the unrelated `ElasticSearchServiceTest` and `ElasticSearchSupportTest` unit
tests, and it silently matches nothing new when a class is added under a different name.

## Running from the IDE

No extra configuration is needed. The container starts automatically when the test class is
loaded.
