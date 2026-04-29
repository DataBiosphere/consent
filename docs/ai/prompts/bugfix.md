# Bugfix Prompt Template

Use this when fixing an incorrect behavior, error response, or failing test.

## What to Provide

- Bug description and observed impact
- How to reproduce (request, input, or failing test)
- Expected behavior
- Relevant logs/stack trace snippets
- Candidate files/classes involved

## Required Constraints

- Reproduce first with a test when possible.
- Fix the smallest responsible scope (Resource, Service, or DAO).
- Reuse existing exception and error response patterns.
- Avoid behavior changes outside the bug scope.

## Copy/Paste Prompt

```text
Fix a bug in the Consent service.

Bug summary:
<describe incorrect behavior>

Reproduction:
- Request/test: <details>
- Current output: <details>
- Expected output: <details>

Likely files:
- <file path>
- <file path>

Constraints:
- Add or update a test that reproduces the issue
- Keep existing API contract unless the bug is contract-related
- Reuse existing error handling patterns

Output format:
1) root-cause analysis
2) file-by-file fix plan
3) tests added/updated
4) verification commands
```

## Usage Example

```text
Fix a bug in the Consent service.

Bug summary:
Role assignment endpoint returns 500 instead of 400 when role name is invalid.

Reproduction:
- Request/test: POST /api/user/roles with unsupported role string
- Current output: 500 with generic error
- Expected output: 400 with validation message

Likely files:
- src/main/java/org/broadinstitute/consent/http/resources/UserResource.java
- src/main/java/org/broadinstitute/consent/http/service/UserService.java
- src/test/java/org/broadinstitute/consent/http/resources/UserResourceTest.java

Constraints:
- Add or update a test that reproduces the issue
- Keep existing API contract unless the bug is contract-related
- Reuse existing error handling patterns

Output format:
1) root-cause analysis
2) file-by-file fix plan
3) tests added/updated
4) verification commands
```

