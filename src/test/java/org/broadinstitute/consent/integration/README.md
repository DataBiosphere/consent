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

## Running via Maven

```shell
mvn clean test -Dtest="org.broadinstitute.consent.integration.**"
```

## Running from the IDE

No extra configuration is needed. The container starts automatically when the test class is
loaded.
