# API Endpoint Prompt Template

Use this when adding or modifying HTTP endpoints.

## What to Provide

- Endpoint goal and business behavior
- Target route(s) and HTTP method(s)
- Existing classes to mirror (for example `UserResource`, `InstitutionResource`)
- Files expected to change
- Constraints (backward compatibility, security rules, no new frameworks)

## Required Constraints

- Route auth boundary: paths under `/api` are authenticated by the proxy and decorated with OAUTH context; non-`/api` paths are unauthenticated.
- Preserve Resource -> Service -> DAO separation.
- Reuse existing auth/error handling patterns (`@RolesAllowed`, `ErrorResource` patterns).
- For PostgreSQL DAO SQL that joins multiple datasets/tables or performs multi-table actions, prefer CTEs (`WITH`) when they improve clarity and reduce repeated work.
- Update OpenAPI docs and tests for contract changes.
- Do not use Mockito lenient stubbing (`lenient()`); keep tests strict-stubbing compliant.

## Copy/Paste Prompt

```text
Implement a new API endpoint in the Consent service.

Goal:
<describe behavior>

API shape:
- Method: <GET|POST|PUT|DELETE>
- Route: </api/...>
- Request body: <model or none>
- Response: <model and status codes>

Reuse patterns from:
- Resource: <existing resource class>
- Service: <existing service class>
- DAO: <existing dao class>

Files to update:
- <resource file>
- <service file>
- <dao file or existing dao>
- src/main/resources/assets/api-docs.yaml
- <test files>

Constraints:
- Keep endpoint conventions from docs/API_GUIDELINES.md
- Put authenticated endpoints under `/api`; only place intentionally public endpoints outside `/api`
- Do not introduce new framework dependencies
- For PostgreSQL DAO SQL that joins multiple datasets/tables or performs multi-table actions, prefer CTEs (`WITH`) when they improve clarity and reduce repeated work
- Add or update tests in src/test/java
- Do not use Mockito lenient stubbing (`lenient()`); keep tests strict-stubbing compliant

Output format:
1) short plan
2) file-by-file diff summary
3) tests added/updated
4) follow-up verification commands
```

## Usage Example

```text
Implement a new API endpoint in the Consent service.

Goal:
Allow admins to list institutions with optional name filtering.

API shape:
- Method: GET
- Route: /api/institutions
- Request body: none
- Response: 200 with a list of InstitutionSummary models

Reuse patterns from:
- Resource: InstitutionResource
- Service: InstitutionService
- DAO: InstitutionDAO

Files to update:
- src/main/java/org/broadinstitute/consent/http/resources/InstitutionResource.java
- src/main/java/org/broadinstitute/consent/http/service/InstitutionService.java
- src/main/java/org/broadinstitute/consent/http/db/InstitutionDAO.java
- src/main/resources/assets/api-docs.yaml
- src/test/java/org/broadinstitute/consent/http/resources/InstitutionResourceTest.java

Constraints:
- Keep endpoint conventions from docs/API_GUIDELINES.md
- Put authenticated endpoints under `/api`; only place intentionally public endpoints outside `/api`
- Do not introduce new framework dependencies
- For PostgreSQL DAO SQL that joins multiple datasets/tables or performs multi-table actions, prefer CTEs (`WITH`) when they improve clarity and reduce repeated work
- Add or update tests in src/test/java
- Do not use Mockito lenient stubbing (`lenient()`); keep tests strict-stubbing compliant

Output format:
1) short plan
2) file-by-file diff summary
3) tests added/updated
4) follow-up verification commands
```
