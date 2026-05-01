# Smoke Testing

Provides a mechanism for running simple smoke tests against a fully running application stack.
The intention here is to keep this layer as slim as possible to provide a minimum sense of
confidence in application stability.

These tests exercise authenticated HTTP endpoints against a live `DropwizardAppExtension`-managed
application and a real database. They are **not** isolated unit tests.

## How the database is provided

The integration tests work differently depending on the environment they run in.

### Local development — no Postgres setup required

[`DAOTestHelper`](../../db/DAOTestHelper.java) is registered as a JUnit
`TestExecutionListener` via `META-INF/services`. When any test plan starts, it launches a
Testcontainers `PostgreSQLContainer` and calls Dropwizard's `ConfigOverride`, which writes
`dw.database.*` **JVM system properties**. Dropwizard's configuration layer applies `dw.*`
system properties as overrides on every subsequent application start — so
`DropwizardAppExtension` silently connects to the Testcontainers Postgres instead of whatever
URL is in `consent-ci.yaml`.

Result: no local Postgres is needed; `DAOTestHelper` wires everything transparently.

### CI — real Postgres at `localhost:5432`

The CI pipeline provisions a Postgres instance matching the coordinates in `consent-ci.yaml`:

| Setting  | Value              |
|----------|--------------------|
| Host     | `localhost:5432`   |
| Database | `consent`          |
| User     | `consent`          |
| Password | `ci-password`      |

In CI, `-DenableTestContainers=false` **must** be passed. Without it, `DAOTestHelper` would
start Testcontainers and overwrite the real CI database coordinates with `test`/`test`
credentials, causing the tests to run against the wrong database.

## Running via Maven

**Local:**

```shell
mvn clean test -Dtest="org.broadinstitute.consent.integration.**"
```

**CI:**

```shell
mvn clean test -DenableTestContainers=false -Dtest="org.broadinstitute.consent.integration.**"
```

## Running from the IDE

No extra configuration is needed for local IDE runs. `DAOTestHelper` activates automatically
and provides the database.
