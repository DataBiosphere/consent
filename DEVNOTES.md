# Local Development

Implementation plans and migration design notes live in [docs/plans](docs/plans). Check that directory before starting larger feature, migration, or refactor work.

* Maven 3.9
* Java 25
* Dropwizard Docs: http://www.dropwizard.io/

### Check out repository:

```bash
git clone git@github.com:broadinstitute/consent.git
```

### Build, test

```bash
cd consent
mvn clean test package 
```

Tests require docker or another OCI runtime to spin up postgres and http servers that run against
localhost. Ensure that your test environment supports that.  Command line docker, podman, and 
Docker Desktop have all worked to run these tests.  Podman was observed to perform better when running tests,
however docker is the tool of choice for other developers and the CI/CD environment.

#### Docker

Consent docker images are stored in GCR: `gcr.io/broad-dsp-gcr-public/consent`

```
# build the docker image
docker build . -t consent
```

This image can then be run with the proper configuration files provided.

### Render Configs

Specific to internal Broad systems:

```bash
APP_NAME=consent ENV=local OUTPUT_DIR=config ../firecloud-develop/configure.rb
```

Otherwise, use `src/test/resources/consent-config.yml` as a template to
create your own environment-specific configuration.

### Spin up application:

Specific to internal Broad systems:

```bash
mvn clean compile
docker-compose -p consent -f config/docker-compose.yaml up
```

Or, if not using docker:

```bash
java -jar /path/to/consent.jar server /path/to/config/file
```

Visit local swagger page: https://local.dsde-dev.broadinstitute.org:27443

### Debugging

Port 7777 is open in the configured docker compose.
Set up a remote debug configuration pointing to `local.dsde-dev.broadinstitute.org`
and the defaults should be correct.

### Developing with a local Elastic Search instance:

`config/docker-compose.yaml` already ships an `elastic` service, and `config/consent.yaml` already
points at it (`servers: [elastic]`), so a normal `docker-compose up` gives you a local cluster with
no edits. I suggest changing the default bucket location so uploaded ontology files do not
interfere with other dev environments.

#### Running with X-Pack Security enabled

Security is **on by default**, which is what the security work (`GET /api/elasticSearch/capabilities`
and native DLS/FLS) needs. Two things make that work, and they have to agree:

* `ELASTIC_PASSWORD` in the compose `elastic` service bootstraps the `elastic` superuser.
* `authUser` / `authPassword` in `config/consent.yaml` are the credentials Consent sends. They are
  harmless when security is off — the ES client only sends credentials in response to a 401
  challenge, which a security-disabled cluster never issues.

DLS and FLS are Platinum features, so the compose file self-generates a 30-day **trial** license via
`xpack.license.self_generated.type`. That setting only applies the first time a cluster forms. If
your `elastic` data volume predates it, the cluster keeps its `basic` license and DLS/FLS come back
`LICENSE_BLOCKED`; activate the trial once, by hand:

```bash
curl -u elastic:devpassword -XPOST 'localhost:9200/_license/start_trial?acknowledge=true'
curl -s -u elastic:devpassword localhost:9200/_license   # expect "type": "trial"
```

Note that transport SSL stays disabled. ES logs a bootstrap warning about it, which is expected and
correct here — transport SSL is only required for multi-node clusters.

To get the old security-disabled cluster back for a run, without editing the committed file:

```bash
ES_SECURITY_ENABLED=false docker-compose -p consent -f config/docker-compose.yaml up
```

Work that lives entirely at the application layer needs no security and is unaffected either way.

## How To...

### How to add a new email

DUOS uses SendGrid for sending emails and FreeMarker for expanding email templates before 
passing them to SendGrid. To add a new email, follow these steps:
1. 
2. Create the HTML template and add it to [src/main/resources/freemarker](src/main/resources/freemarker).
2. Add a new entry to [EmailType](src/main/java/org/broadinstitute/consent/http/enumeration/EmailType.java) that references that file.
3. Add a new [MailMessage](src/main/java/org/broadinstitute/consent/http/mail/message/MailMessage.java) subclass that references this EmailType and implements methods to provide data for the freemarker template and database operation
4. Add a method to [EmailService](src/main/java/org/broadinstitute/consent/http/service/EmailService.java) that creates this message and passes it to sendMessage().


### How to scan with OWASP
Consent code includes a maven plugin for the OWASP dependency check tool.  Three environment
variables are needed to use this.  You will need the dotNet 8 SDK to generate a report.

Install the dotNet 8 SDK with:

```$ brew install --cask dotnet-sdk@8```

Register at the National Vulnerability Database for an API key here:
https://nvd.nist.gov/developers/request-an-api-key

Assign the key value they issue you as environment variable NVD_API_KEY

e.g.

```$ export NVD_API_KEY=<your api key here>```

Register at the OSS Index for an API key:
https://ossindex.sonatype.org/

Assign the username you used (email address) to the environment variable OSS_INDEX_USERNAME

e.g.

```$ export OSS_INDEX_USERNAME=<your username>```

Assign the API Token (visible on the User Settings page) to the environment variable OSS_INDEX_PASSWORD

e.g.

```$ export OSS_INDEX_PASSWORD=<your API token>```

Run the dependency checker:
```$ mvn org.owasp:dependency-check-maven:check``` 

## Integration Testing

Integration tests live in `src/test/java/**/integration/` and are run as part
of the standard `mvn test` lifecycle — no special profile, external server, or
manual Postgres setup is required.

#### How they work

Each test class extends `ContainerTests`, which uses a JUnit 5
`DropwizardAppExtension` to boot the full application in-process against the
config at `src/test/resources/consent-ci.yaml`. A WireMock server on port 9999
stands in for all external services (Sam, ECM, GCS, etc.).

Database seeding is performed programmatically in `ContainerTests.seedDatabase()`
via typed DAO calls (`@BeforeAll`). The seed data is fully synthetic and
idempotent. To add new baseline rows, extend the relevant `seed*` helper method
inside `ContainerTests`.

#### Database

`ContainerTests` starts its own [Testcontainers](https://www.testcontainers.org/)
`PostgreSQLContainer` in a static initializer and passes the container's
coordinates directly to `DropwizardAppExtension` via `ConfigOverride`. The
hardcoded coordinates in `consent-ci.yaml` are never reached at runtime. No
local Postgres is needed in any environment.

#### How they run in CI

The GitHub Actions workflow at `.github/workflows/coverage.yaml` runs
`mvn clean test` on every push/PR to `develop`, which exercises unit and
integration tests together via Testcontainers — no additional CI configuration
is needed.

#### Running integration tests locally

**Integration tests only:**

```bash
mvn clean test -Dtest="org.broadinstitute.consent.integration.**"
```

**All tests** (unit + integration together, as CI does):

```bash
mvn clean test
```

**From the IDE:** run or debug any test class in the `integration` package
directly — `DAOTestHelper` activates automatically and provides the database.
