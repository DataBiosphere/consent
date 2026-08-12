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
#### Running with X-Pack Security enabled

`config/` is not version-controlled — it is rendered per developer (see "Render Configs" above) — so
none of this arrives with a `git pull`; it has to go into your own `config/docker-compose.yaml`. The
example stanza below turns security **on**, which is what the security work
(`GET /api/elasticSearch/capabilities` and native DLS/FLS) needs, and keeps a
`${ES_SECURITY_ENABLED:-true}` gate so you can drop back to an unauthenticated cluster for a single
run. Two things make it work, and they have to agree:

* `ELASTIC_PASSWORD` in the compose `elastic` service bootstraps the `elastic` superuser.
* `authUser` / `authPassword` in `config/consent.yaml` are the credentials Consent sends. They are
  harmless when security is off — the ES client only sends credentials in response to a 401
  challenge, which a security-disabled cluster never issues.

DLS and FLS are Platinum features, so the stanza self-generates a 30-day **trial** license via
`xpack.license.self_generated.type`. That setting only applies the first time a cluster forms. If
your `elastic` data volume predates it, the cluster keeps its `basic` license and DLS/FLS come back
`LICENSE_BLOCKED`. Activating the trial is a separate, deliberate step — never something a
capability probe does on your behalf — and Consent exposes it as an admin endpoint, so it works the
same way here as it does against a deployed cluster:

```bash
# What tier is this cluster on, and does it still have a trial to spend?
curl -s -X GET  'localhost:8000/api/elasticSearch/license'
# Start it. acknowledge=true is required: one trial per cluster, and it cannot be reverted.
curl -s -X POST 'localhost:8000/api/elasticSearch/license/trial?acknowledge=true'
```

Both need an Admin bearer token. Straight at the cluster works too, and is the quicker path when
Consent is not running:

```bash
curl -u elastic:devpassword -XPOST 'localhost:9200/_license/start_trial?acknowledge=true'
curl -s -u elastic:devpassword localhost:9200/_license   # expect "type": "trial"
```

Note that transport SSL stays disabled. ES logs a bootstrap warning about it, which is expected and
correct here — transport SSL is only required for multi-node clusters.

To get an unauthenticated cluster back for a run, without editing your compose file again:

```bash
ES_SECURITY_ENABLED=false docker-compose -p consent -f config/docker-compose.yaml up
```

An example docker-compose stanza for elastic:
 ```
 elastic:
    image: docker.elastic.co/elasticsearch/elasticsearch:9.5.1
    ports:
      - "9200:9200"
    container_name: elastic
    volumes:
      - elastic:/usr/share/elasticsearch/data
    deploy:
      resources:
        limits:
          memory: 4gb
    environment:
      - "ES_JAVA_OPTS=-Xms2g -Xmx2g"
      # X-Pack Security is ON here: the capability endpoint and Epic D (native DLS/FLS) need it.
      # Epics A-C and E (application-layer fallback) do not, so to run one session unauthenticated:
      #
      #   ES_SECURITY_ENABLED=false docker-compose -p consent -f config/docker-compose.yaml up
      #
      # See DEVNOTES.md ("Developing with a local Elastic Search instance") for the full workflow.
      - xpack.security.enabled=${ES_SECURITY_ENABLED:-true}
      # DLS/FLS is a Platinum feature, so self-generate a trial rather than the default basic
      # license. Only applies the first time a cluster forms — on an existing `elastic` volume,
      # activate it by hand instead, once per major version per cluster, either through Consent's
      # admin endpoint or straight at the cluster:
      #
      #   curl -X POST 'localhost:8000/api/elasticSearch/license/trial?acknowledge=true'
      #   curl -u elastic:devpassword -XPOST 'localhost:9200/_license/start_trial?acknowledge=true'
      - xpack.license.self_generated.type=${ES_LICENSE_TYPE:-trial}
      # Bootstraps the `elastic` superuser password when security is on; ignored when it is off.
      # Must match authUser/authPassword in consent.yaml.
      - ELASTIC_PASSWORD=${ELASTIC_PASSWORD:-devpassword}
      # Correct in both modes: transport SSL is only required for multi-node clusters.
      - xpack.security.transport.ssl.enabled=false
      # Keep the HTTP layer on plain http so consent's `protocol: http` client keeps working.
      - xpack.security.http.ssl.enabled=false
```
I also suggest changing the default bucket location so uploaded
ontology files do not interfere with other dev environments.

#### Which work actually needs security enabled

Only the capability endpoint and native Elasticsearch document- and field-level security (DLS/FLS)
require it. Application-layer authorization work does not — it never touches Elasticsearch security —
so if you are not working on those, running with `ES_SECURITY_ENABLED=false` is fine.

#### Enabling DLS/FLS locally (trial license required)

Enabling security is necessary but **not sufficient** for DLS/FLS. The Docker image self-generates
a **basic** license, and DLS/FLS is a Platinum/Enterprise feature. With a basic license, creating a
role or API key that carries a DLS `query` or an FLS `field_security` grant fails closed with
HTTP 403 `current license is non-compliant for [field and document level security]`. Note that
`POST /_security/api_key` *accepts* such a role descriptor at creation time — the rejection happens
later, on the search request.

Activate the 30-day trial license once the secured cluster is up, either through Consent's admin
endpoint or against the cluster directly:

```bash
# Through Consent (Admin bearer token required); reports what it changed, and refuses without
# acknowledge=true because the trial can be started once per major version per cluster.
curl -s -X POST 'localhost:8000/api/elasticSearch/license/trial?acknowledge=true'

# Or straight at the cluster.
curl -u elastic:devpassword -XPOST 'localhost:9200/_license/start_trial?acknowledge=true'
curl -u elastic:devpassword 'localhost:9200/_license'   # => "type": "trial"
```

Either way it is a step you take, not one that happens while you are asking a different question:
`GET`/`POST /api/elasticSearch/capabilities` reports the license tier and never changes it, and the
container tests activate the trial from the `@BeforeAll` of the classes that need it rather than from
their shared harness.

Caveats:

- A trial can be started **once per major version per cluster** (`GET /api/elasticSearch/license` reports eligibility
  as `trial_available`, as does `GET /_license/trial_status` on the cluster).
  After 30 days the license reverts to basic and DLS/FLS stops working. For another trial on
  the same major version, wipe the cluster's data volume; after a major-version upgrade, check
  trial_available because the cluster may be eligible again: `docker-compose -p consent -f config/docker-compose.yaml down` then
  `docker volume rm consent_elastic` (this deletes local indices — they must be re-indexed).
- Authentication, role-based access control, and API keys all work fine on the basic license. Only
  the DLS/FLS grants require trial/Platinum.

#### Upgrading the local Elastic Search version

Everything described above — the two security modes, the license gating, and that an unenforceable
DLS/FLS grant fails closed instead of returning unrestricted data — is asserted by tests, so an
upgrade does not need to be re-verified by hand:

```bash
# 1. bump ElasticSearchTestCluster.IMAGE (the only version pin in the test tree)
./mvnw test -Dgroups=elasticsearch
# 2. then match it here: config/docker-compose.yaml, pom.xml (elasticsearch-rest-client), this file
```

These four classes carry the JUnit tag `elasticsearch` and are **not** run by CI — see
"How they run in CI" below. Running them is a deliberate step when changing the Elasticsearch
version or the Epic D security work, not something a build does for you.

See "Qualifying a new Elasticsearch version" in
[the integration test README](src/test/java/org/broadinstitute/consent/integration/README.md).

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

The exception is the Elasticsearch container tests. They are tagged
`elasticsearch`, and the `ci` profile in `pom.xml` — activated by the `CI=true`
that GitHub Actions sets in every job — feeds that tag to surefire's
`excludedGroups`, so they do not run in CI. They start up to three Elasticsearch
containers and exist to qualify a local Elasticsearch version rather than to
gate the build; see "Upgrading the local Elastic Search version" above.

To run them in CI anyway (a workflow edit, or a `workflow_dispatch` job), pass an
empty value on the command line, which overrides the profile:

```bash
./mvnw clean jacoco:prepare-agent test jacoco:report -DexcludedTestGroups=
```

#### Running integration tests locally

**Integration tests only:**

```bash
mvn clean test -Dtest="org.broadinstitute.consent.integration.**"
```

**All tests** (unit + integration together, as CI does):

```bash
mvn clean test
```

Note that locally this *also* runs the Elasticsearch container tests, which CI
skips. To match CI exactly, add `-DexcludedTestGroups=elasticsearch`.

**From the IDE:** run or debug any test class in the `integration` package
directly — `DAOTestHelper` activates automatically and provides the database.
