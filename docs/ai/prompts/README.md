# AI Prompt Library (Consent)

Use these prompts as templates for codebase-aware requests.

## Before You Run a Prompt

Include these inputs so responses are actionable:

- Goal statement (what should change and why)
- Target files (paths in this repository)
- Existing classes to mirror (for example `UserResource`, `InstitutionService`)
- Constraints (no API breaks, no new frameworks, test scope)
- Definition of done (tests, docs, OpenAPI updates)

## Expected Output Quality

A good prompt result should include:

- A short implementation plan
- Concrete file-by-file changes
- Tests to add/update
- Risks or compatibility notes
- Follow-up verification commands

## Locator Matrix (Where to Look First)

| Task type | Start here first | Then check |
| --- | --- | --- |
| `api-endpoint.md` | `src/main/java/org/broadinstitute/consent/http/resources/` | `src/main/java/org/broadinstitute/consent/http/service/`, `src/main/java/org/broadinstitute/consent/http/db/`, `src/main/resources/assets/api-docs.yaml`, `src/test/java/org/broadinstitute/consent/http/resources/` |
| `feature-development.md` | `src/main/java/org/broadinstitute/consent/http/resources/`, `src/main/java/org/broadinstitute/consent/http/service/` | `src/main/java/org/broadinstitute/consent/http/db/`, `src/main/java/org/broadinstitute/consent/http/models/`, `src/test/java/`, `src/main/resources/assets/api-docs.yaml` |
| `bugfix.md` | Failing test or relevant resource test in `src/test/java/` | Matching runtime classes in `src/main/java/org/broadinstitute/consent/http/resources/`, `src/main/java/org/broadinstitute/consent/http/service/`, `src/main/java/org/broadinstitute/consent/http/db/` |
| `refactor.md` | In-scope classes under `src/main/java/org/broadinstitute/consent/http/` | Neighboring tests in `src/test/java/`, API contract references in `src/main/resources/assets/api-docs.yaml`, guidance in `docs/API_GUIDELINES.md` |
| `openapi-spec.md` | `src/main/resources/assets/paths/`, `src/main/resources/assets/api-docs.yaml` | `src/main/resources/assets/api-docs.yaml` |

## Available Prompt Templates

- `api-endpoint.md`
- `feature-development.md`
- `bugfix.md`
- `refactor.md`
- `openapi-spec.md`
