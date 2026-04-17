# Onboarding Guide

This guide helps new contributors run the Consent service locally and make a safe first change.

## Prerequisites

From repository configuration:

- Java 25 (`pom.xml`)
- Maven 3.9+
- Docker or another OCI runtime for integration tests and local compose workflows
- PostgreSQL (only if not using the provided compose stack)

## Quick Start (Docker Compose)

Use this path for the most production-like local setup.

```bash
mvn clean compile
docker-compose -p consent -f config/docker-compose.yaml up
```

### Verify the service

- Swagger UI (compose path): `https://local.dsde-dev.broadinstitute.org:27443`
- OpenAPI endpoint: `https://local.dsde-dev.broadinstitute.org:27443/api-docs/openapi.yaml`

If needed, map the local host in your `/etc/hosts` file:

```text
127.0.0.1 local.dsde-dev.broadinstitute.org
```

## Alternative Run (Direct JAR)

Use this when debugging outside containers.

```bash
mvn clean package
java -jar target/consent-*.jar server config/consent.yaml
```

- Swagger UI (direct path): `http://localhost:8080/`
- OpenAPI endpoint: `http://localhost:8080/api-docs/openapi.yaml`

## Run Tests

```bash
mvn clean test
```

Note: tests use containerized dependencies (per `DEVNOTES.md`), so make sure your OCI runtime is available.

## First Contribution Checklist

- [ ] Read `docs/ARCHITECTURE.md`
- [ ] Read `docs/API_GUIDELINES.md`
- [ ] Run the service locally (compose or direct)
- [ ] Run `mvn clean test`
- [ ] Make a small change and include test updates
- [ ] Update docs if behavior or API contracts changed

## Adding a New Endpoint

- Follow the Resource -> Service -> DAO pattern.
- Reuse existing resources as reference (`UserResource`, `InstitutionResource`).
- Register new resources in `ConsentApplication`.
- Add resource/service tests in `src/test/java`.
- Update `src/main/resources/assets/api-docs.yaml` for contract changes.

## AI-Assisted Development

- Start at `docs/ai/README.md`
- Use task-specific prompts in `docs/ai/prompts/`
