# Feature Development Prompt Template

Use this when implementing a new cross-layer feature.

## What to Provide

- User/problem statement
- Scope boundaries (in scope and out of scope)
- Existing classes to mirror
- Data model or schema impacts
- Required tests and documentation updates

## Required Constraints

- Keep the Dropwizard + Guice + JDBI architecture intact.
- Prefer incremental changes to broad rewrites.
- Keep public API behavior backward compatible unless explicitly approved.
- Include tests and docs for behavior changes.
- Run Spotless formatting for changed files before finalizing.
- Favor Java records for new immutable DTO/view models; use classes when framework or persistence mapping requires them.
- For potentially large DB-backed results, prefer pagination/streaming/projections and avoid unbounded in-memory collections.
- For PostgreSQL DAO SQL spanning multiple datasets/tables, prefer CTEs (`WITH`) when they simplify logic or reduce repeated work.
- Resolve SonarQube issues in touched code and do not introduce new blocker/critical issues.
- Use only synthetic test data; do not use real or realistic PII (emails, IDs, names, tokens, or secrets).
- Do not use Mockito lenient stubbing (`lenient()`); keep tests strict-stubbing compliant.

### CTE Example (Use vs Not)

```text
Use CTE: gather matching dataset IDs, then update related rows in another table in one query.
Do not force CTE: a simple single-table lookup/filter is usually clearer as a direct SELECT.
```

## Copy/Paste Prompt

```text
Implement a new feature in the Consent service using existing architecture patterns.

Feature goal:
<describe user/business goal>

Scope:
- In scope: <list>
- Out of scope: <list>

Mirror existing patterns from:
- Resource: <class>
- Service: <class>
- DAO: <class>

Expected files to touch:
- <list files>

Acceptance criteria:
- <criterion 1>
- <criterion 2>
- <criterion 3>

Constraints:
- Keep Resource -> Service -> DAO layering
- No new framework dependencies
- Preserve backward compatibility unless stated otherwise
- Add/update tests and docs as needed
- Run Spotless formatting for changed files before finalizing
- Favor Java records for new immutable DTO/view models; use classes when framework or persistence mapping requires them
- For potentially large DB-backed results, prefer pagination/streaming/projections and avoid unbounded in-memory collections
- For PostgreSQL DAO SQL spanning multiple datasets/tables, prefer CTEs (`WITH`) when they simplify logic or reduce repeated work
- Resolve SonarQube issues in touched code and do not introduce new blocker/critical issues
- Use only synthetic test data; do not use real or realistic PII (emails, IDs, names, tokens, or secrets)
- Do not use Mockito lenient stubbing (`lenient()`); keep tests strict-stubbing compliant

Output format:
1) short implementation plan
2) proposed file changes
3) test plan
4) risk/compatibility notes
```

## Usage Example

```text
Implement a new feature in the Consent service using existing architecture patterns.

Feature goal:
Support searching users by partial display name for admin tools.

Scope:
- In scope: query endpoint, service filtering logic, DAO query support
- Out of scope: UI changes, role model redesign

Mirror existing patterns from:
- Resource: UserResource
- Service: UserService
- DAO: UserDAO

Expected files to touch:
- src/main/java/org/broadinstitute/consent/http/resources/UserResource.java
- src/main/java/org/broadinstitute/consent/http/service/UserService.java
- src/main/java/org/broadinstitute/consent/http/db/UserDAO.java
- src/test/java/org/broadinstitute/consent/http/resources/UserResourceTest.java
- src/main/resources/assets/api-docs.yaml

Acceptance criteria:
- Admins can search users by partial display name
- Empty query returns validation error
- Existing user endpoints are unaffected

Constraints:
- Keep Resource -> Service -> DAO layering
- No new framework dependencies
- Preserve backward compatibility unless stated otherwise
- Add/update tests and docs as needed
- Run Spotless formatting for changed files before finalizing
- Favor Java records for new immutable DTO/view models; use classes when framework or persistence mapping requires them
- For potentially large DB-backed results, prefer pagination/streaming/projections and avoid unbounded in-memory collections
- For PostgreSQL DAO SQL spanning multiple datasets/tables, prefer CTEs (`WITH`) when they simplify logic or reduce repeated work
- Resolve SonarQube issues in touched code and do not introduce new blocker/critical issues
- Use only synthetic test data; do not use real or realistic PII (emails, IDs, names, tokens, or secrets)
- Do not use Mockito lenient stubbing (`lenient()`); keep tests strict-stubbing compliant

Output format:
1) short implementation plan
2) proposed file changes
3) test plan
4) risk/compatibility notes
```
