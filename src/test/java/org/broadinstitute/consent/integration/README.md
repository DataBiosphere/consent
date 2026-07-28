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
`ElasticsearchContainer` in a static initializer, builds a client through the application's own
`ElasticSearchSupport.createRestClient`, and activates the trial license. Extend it directly; it is
independent of `ContainerTests` and does not start the Dropwizard application.

Two things are easy to get wrong here:

- **DLS/FLS needs a non-basic license.** Each fresh container self-generates a *basic* license, and
  a DLS query or FLS field grant is rejected under it with HTTP 403 `current license is
  non-compliant for [field and document level security]`. The base class activates the 30-day trial
  in its static initializer. Note that `POST /_security/api_key` accepts a DLS/FLS role descriptor
  even on a basic license — the rejection only surfaces on the search request.
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
   ./mvnw test -Dtest='ElasticSearch*Test'
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

## Running via Maven

```shell
mvn clean test -Dtest="org.broadinstitute.consent.integration.**"
```

## Running from the IDE

No extra configuration is needed. The container starts automatically when the test class is
loaded.
