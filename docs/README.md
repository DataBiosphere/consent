# Consent Service Documentation

This directory is the canonical home for developer documentation for the Consent Dropwizard service.

## Start Here

If you are new to the repository, read in this order:

1. `ONBOARDING.md`
2. `ARCHITECTURE.md`
3. `API_GUIDELINES.md`
4. `../CONTRIBUTING.md`

## Documentation Map

| Document | Audience | Purpose |
| --- | --- | --- |
| `ONBOARDING.md` | New contributors | Local setup, run commands, and first tasks |
| `ARCHITECTURE.md` | Contributors and reviewers | High-level system design and request flow |
| `API_GUIDELINES.md` | API authors and reviewers | API conventions, validation, and compatibility rules |
| `ELECTION_OPERATIONS.md` | Support and operations | Ownership and escalation path for exceptional DAR election cleanup |
| `plans/README.md` | Contributors and reviewers | Index of active implementation plans and design notes |
| `ai/README.md` | Developers using AI tooling | Prompt usage model and best practices |
| `ai/prompts/*.md` | Developers using AI tooling | Task-specific prompt templates and examples |

## Documentation Standards

- Keep examples grounded in existing classes and packages.
- Keep implementation plans and migration designs under `docs/plans/`.
- Prefer short sections and checklists over long prose.
- When behavior changes, update docs in the same PR as code changes.
- If guidance is obsolete, remove or mark it clearly as historical.
