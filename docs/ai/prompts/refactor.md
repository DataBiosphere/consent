# Refactor Prompt Template

Use this for behavior-preserving cleanup (readability, maintainability, duplication reduction).

## What to Provide

- Refactor goal and non-goals
- Files/classes in scope
- Current pain points (duplication, complexity, naming)
- Test coverage expectations

## Required Constraints

- Preserve external behavior and API contracts.
- Keep Resource -> Service -> DAO layering.
- Do not introduce new frameworks unless explicitly approved.
- Make changes in small, reviewable steps.

## Copy/Paste Prompt

```text
Refactor selected Consent service code without changing behavior.

Goal:
<describe maintainability/readability goal>

In scope:
- <class/file>
- <class/file>

Out of scope:
- API contract changes
- new dependencies/frameworks

Constraints:
- Preserve behavior and endpoint contracts
- Keep existing architecture and DI style
- Update tests only as needed for maintainability or determinism

Output format:
1) refactor plan in small steps
2) file-by-file changes
3) behavior safety checks
4) verification commands
```

## Usage Example

```text
Refactor selected Consent service code without changing behavior.

Goal:
Reduce duplication in dataset permission checks while keeping all endpoint behavior unchanged.

In scope:
- src/main/java/org/broadinstitute/consent/http/service/DatasetService.java
- src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java

Out of scope:
- API contract changes
- new dependencies/frameworks

Constraints:
- Preserve behavior and endpoint contracts
- Keep existing architecture and DI style
- Update tests only as needed for maintainability or determinism

Output format:
1) refactor plan in small steps
2) file-by-file changes
3) behavior safety checks
4) verification commands
```

