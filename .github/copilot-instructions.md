# Copilot Instructions for Consent

These instructions apply to code suggestions and PR review in this repository.

## Architecture and Boundaries

- Keep the existing Dropwizard + Guice + JDBI architecture.
- Preserve `Resource -> Service -> DAO` separation.
- Prefer incremental changes to broad rewrites.
- Reuse existing auth and error handling patterns (`@RolesAllowed`, `Resource.createExceptionResponse`, `org.broadinstitute.consent.http.mappers` `ExceptionMapper`s).

## API and Auth Rules

- Auth boundary is route-based:
  - Paths under `/api` are authenticated through the proxy Auth section and include OAUTH-decorated request information.
  - Non-`/api` paths are unauthenticated and must not assume OAUTH context.
- Put authenticated endpoints under `/api`.
- Keep public API behavior backward compatible unless explicitly approved.
- If API contracts change, update OpenAPI spec in `src/main/resources/assets/api-docs.yaml`.

## Quality and Performance Expectations

- Run Spotless formatting for changed files.
- Resolve SonarQube issues in touched code; do not introduce new blocker/critical issues.
- For large DB-backed results, prefer pagination/streaming/projections and avoid unbounded in-memory collections.
- For PostgreSQL DAO queries that span multiple datasets or act on multiple tables, prefer CTEs (`WITH`) when they simplify logic and reduce repeated work.
- Favor Java records for new immutable DTO/view models when framework/persistence mapping allows.

## Testing and Test Data

- Add or update tests for behavior changes in `src/test/java`.
- Use synthetic test data only; do not include real or realistic PII, tokens, or secrets.
- Do not use Mockito lenient stubbing (`lenient()`); prefer strict stubbing and remove unused/mismatched stubs.

## Source of Truth

- `docs/API_GUIDELINES.md`
- `docs/ARCHITECTURE.md`
- `docs/ONBOARDING.md`
- `docs/ai/prompts/`
- `CONTRIBUTING.md`

