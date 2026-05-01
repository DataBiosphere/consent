# Local Development

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

Update the compose file to include a new section for an ES instance:

```
es:
  image: docker.elastic.co/elasticsearch/elasticsearch:5.5.0
  ports:
    - "9200:9200"
  volumes:
    - ../data:/usr/share/elasticsearch/data
  environment:
    transport.host: 127.0.0.1
    xpack.security.enabled: "false"
    http.host: 0.0.0.0
```

Add a line to the `app` section to link to that:

```
  links:
    - es:es
```

Finally, update the servers in consent.conf to point to this instance:

```
elasticSearch:
  servers:
    - es
  indexName: local-ontology    
  datasetIndexName: datasetIName
```

Consent will now point to a local ES instance.
I also suggest changing the default bucket location so uploaded
ontology files do not interfere with other dev environments.

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

Integration tests live in `src/test/java/**/integration/` and are run with the
`integration-tests` Maven profile.  They make real HTTP calls against a running
instance of the application, so a live server and its backing services must be
available before the tests execute.

#### How they run in CI

The GitHub Actions workflow at `.github/workflows/integration-tests.yaml`
handles everything automatically on every push/PR to `develop`:

1. **PostgreSQL 16** and **Elasticsearch 9** are started as service containers.
2. The application jar is built with `mvn clean package`.
3. A SQL seed file is loaded into Postgres to provide baseline test data
   (see [SQL seed file priority](#sql-seed-file-priority) below).
4. The application is started with the CI config at
   `.github/config/consent-ci.yaml` and the workflow waits for `GET /status`
   to return 200.
5. `mvn test -P integration-tests -DbaseUrl=http://localhost:8080/` is run.
6. Test reports are uploaded as a workflow artifact (`integration-test-reports`)
   and the application log as `app-log`.

#### SQL seed file priority

The workflow resolves which SQL file populates the database using this
precedence (highest to lowest):

| Priority | Source |
|---|---|
| 1 | `sql-file` input on a manual `workflow_dispatch` trigger |
| 2 | `DB_SEED_SQL_FILE` repository/environment variable (set in **Settings → Variables → Actions**) |
| 3 | `.github/config/seed-ci.sql` — the default synthetic seed checked into the repo |
| 4 | *(nothing)* — Liquibase initialises a clean schema only |

The seed file at `.github/config/seed-ci.sql` contains one synthetic user per
application role, a test institution, and a test DAC.  It is idempotent and
safe to run repeatedly.  Add new rows in the clearly marked sections at the
bottom of that file; follow the `ON CONFLICT DO NOTHING` / `WHERE NOT EXISTS`
pattern already used there.

#### Running integration tests locally

A convenience script mirrors the CI workflow exactly:

```bash
# Full run: build → start services → seed DB → start app → test → cleanup
./scripts/run-integration-tests.sh

# Skip Maven build when the jar is already up-to-date
./scripts/run-integration-tests.sh --skip-build

# Use a different SQL seed file (e.g. a recent DB dump)
./scripts/run-integration-tests.sh --sql-file config/consent-recent.sql

# Run the tests against an already-running environment instead
./scripts/run-integration-tests.sh --base-url https://consent.dsde-dev.broadinstitute.org/
```

The script requires `docker`, `mvn`, `java`, and `psql` on your `PATH`.
On exit (pass or fail) it automatically stops the application process and
removes the Docker containers it started, so nothing is left running.

To run the tests manually against an existing environment, pass a custom base
URL directly:

```bash
mvn test -P integration-tests -DbaseUrl=https://consent.dsde-dev.broadinstitute.org/
```
