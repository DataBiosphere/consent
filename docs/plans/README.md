# Planning Documents

This directory contains implementation plans and design notes for planned or in-progress changes.

Use this directory for documents that describe a proposed path forward, migration strategy, or design decision before the work is implemented. Keep stable contributor guidance in the top-level `docs/` files.

## Plans

| Plan | Purpose |
| --- | --- |
| `data-showcase-delivery-plan.md` | Plan for Data Showcase ([DT-3904](https://broadworkbench.atlassian.net/browse/DT-3904)) across consent and duos-ui: showcases as typed code entries authored by PR (like `libraryVersions.ts`), a public catalog lookup in consent, and a six-section MVP. |
| `data-showcase-post-mvp-plan.md` | Detailed post-MVP epics (E6–E10) for the Data Showcase plan (DT-3904): ranked discovery and charts, subscriptions, RADAR automation, archive notices, and publications/impact. Each is re-planned before it starts. |
| `data-showcase-artifact-review.md` | Companion to the Data Showcase plan (DT-3904): what the supplied design artifact actually implements, its structure, accessibility gaps, and how each finding maps to plan stories. |
| `dar-metrics-analytics-plan.md` | Plan for reporting DAR metrics (DAC decision outcome, source and turnaround, SO approval turnaround, volume, expiration) from data Consent already persists, and surfacing them in an internal admin analytics dashboard in duos-ui. Records which of the thirteen requested metrics are feasible and what renewal depends on. |
| `data-use-primary-consistency-plan.md` | Plan for aligning dataset primary Data Use registration rules with automated matching while handling legacy records safely. |
| `vodar-plan.md` | Plan for VODAR (View Only Data Access Requests) across duos-ui and consent: a constrained DAR for viewing data without analysis or publication, RADAR auto-approved when the DAC opts in and otherwise sent to normal DAC review. |
| `dataset-registration-schema-migration-plan.md` | Plan for migrating dataset/study registration away from `dataset-registration-schema_v1.json` backend validation. Completed in #2978. |
| `elasticsearch-service-duos-ui-usage.md` | Plan for adding access control to the information in consent's Elasticsearch infrastructure. |
| `es-security-capability-record.md` | Ticket A-1 record: measured Elasticsearch security capabilities (DLS/FLS/API keys/`run_as`) per environment, and the resulting Epic D / Epic E decision. |
| `study-dataset-circular-reference-plan.md` | Plan for removing `Study.datasets` from the Java model, the OpenAPI contract, and duos-ui so the Study and Dataset schemas no longer reference each other, delivered as three stacked PRs. |
| `consent-role-scram-password-plan.md` | Runbook for moving the `consent` database role from an MD5 to a SCRAM-SHA-256 password hash, with the same password, so that the DUOS BFF (Node in FIPS mode) can log in. Uses `psql`'s `\password`, libpq `require_auth` checks, and a same-session rollback. Done in dev, staging and prod (2026-10). |
| `researcher-status-decoupling-plan.md` | Plan for introducing a persisted `users.researcher_status` flag so researcher eligibility no longer keys off Library Card presence, leaving the Library Card as the DAA pre-authorization container. |
| `dataset-daa-override-plan.md` | [DT-3876] Plan for letting a DAC chair give one dataset its own DAA (from the DAC's DAAs, or a new upload) in place of the DAC default, and for resolving each dataset's effective DAA in the DAR flow and the SO console. Covers consent and duos-ui. |
| `so-approval-exempt-datasets-plan.md` | [DT-4267] Plan for letting a DAC chair mark a dataset so that DARs on it do not need Signing Official approval. The SO decision at submit uses only non-exempt datasets. Covers consent and duos-ui. |
| `asset-data-use-plan.md` | Plan for giving Model and Workspace study assets ([DT-3810](https://broadworkbench.atlassian.net/browse/DT-3810)) the same access management and primary and secondary data use terms as a Dataset, across consent and duos-ui. Extends the existing `study_property` JSON entries through a new `AssetDataUse` module, with no new tables, in eight tickets. |
| `study-asset-records-plan.md` | [DT-4208] Plan for giving every study model, workspace and biospecimen a permanent row with a server-assigned numeric id: a one-time conversion of existing registrations, sync on every registration write, and retire-not-delete on removal. Seven tasks across consent and duos-ui. |
| `asset-identifiers-plan.md` | [DT-4209] Plan for one catalog of every requestable asset (dataset, model, workspace, biospecimen), each with a DUOS identifier: `DUOS-M-`, `DUOS-W-` and `DUOS-B-` numbered by the DT-4208 row id, dataset identifiers unchanged, backfills, a daily integrity check, and rollback. |
