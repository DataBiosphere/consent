# Agent Guidance Index

This repository uses multiple instruction entry points.

## For GitHub Copilot (GitHub UI / PR review)

- Primary file: `.github/copilot-instructions.md`

## For Repo Conventions and Implementation Details

- `docs/API_GUIDELINES.md`
- `docs/ARCHITECTURE.md`
- `docs/ONBOARDING.md`
- `docs/plans/README.md`
- `docs/ai/prompts/README.md`
- `docs/ai/prompts/api-endpoint.md`
- `docs/ai/prompts/feature-development.md`
- `docs/ai/prompts/bugfix.md`
- `docs/ai/prompts/refactor.md`
- `CONTRIBUTING.md`

## Key Rules (Quick Reference)

- Keep `Resource -> Service -> DAO` layering.
- `/api` routes are authenticated (proxy/OAUTH-decorated); non-`/api` routes are unauthenticated.
- Update `src/main/resources/assets/api-docs.yaml` when API contracts change.
- Add/update tests for behavior changes.
- Put new implementation plans and migration design docs under `docs/plans/`.
- Avoid Mockito lenient stubbing (`lenient()`); keep tests strict-stubbing compliant.
- Use synthetic test data only.
