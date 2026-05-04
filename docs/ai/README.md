# AI Prompt System for Consent Dropwizard Codebase

These prompts are repository-specific and designed to produce changes that align with existing Consent patterns.

## Prompt Catalog

| Prompt | Use when you need to... |
| --- | --- |
| `feature-development.md` | Implement a new capability across Resource, Service, and DAO layers |
| `api-endpoint.md` | Add or modify an HTTP endpoint and keep API contracts consistent |
| `bugfix.md` | Diagnose and fix a production or test failure |
| `refactor.md` | Improve readability/maintainability without changing behavior |

## Recommended Workflow

1. Choose one prompt based on task type.
2. Fill in concrete repository context (class names, package paths, test files).
3. Ask for a small implementation plan first.
4. Apply changes incrementally with tests after each step.
5. Verify docs/OpenAPI updates for endpoint behavior changes.

## Quality Rules for AI-Assisted Changes

- Reference real project classes and packages (for example `UserResource`, `UserService`, `UserDAO`).
- Reuse existing patterns before introducing new abstractions.
- Prefer minimal, reviewable diffs.
- For PostgreSQL DAO SQL touching multiple datasets/tables, prefer CTEs (`WITH`) when they make queries clearer and avoid repeated scans/work.
- Require tests for behavior changes.
- If a prompt output conflicts with repository conventions, repository conventions win.

## Where Prompts Live

- `docs/ai/prompts/README.md`
- `docs/ai/prompts/*.md`
