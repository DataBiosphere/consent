# Planning Documents

This directory contains implementation plans and design notes for planned or in-progress changes.

Use this directory for documents that describe a proposed path forward, migration strategy, or design decision before the work is implemented. Keep stable contributor guidance in the top-level `docs/` files.

## Plans

| Plan | Purpose |
| --- | --- |
| `data-use-primary-consistency-plan.md` | Plan for aligning dataset primary Data Use registration rules with automated matching while handling legacy records safely. |
| `vodar-plan.md` | Plan for VODAR (View Only Data Access Requests) across duos-ui and consent: a constrained DAR for viewing data without analysis or publication, RADAR auto-approved when the DAC opts in and otherwise sent to normal DAC review. |
| `dataset-registration-schema-migration-plan.md` | Plan for migrating dataset/study registration away from `dataset-registration-schema_v1.json` backend validation. |
| `elasticsearch-service-duos-ui-usage.md` | Plan for adding access control to the information in consent's Elasticsearch infrastructure. |
