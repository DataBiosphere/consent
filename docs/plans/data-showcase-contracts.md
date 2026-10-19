# Data Showcase: S1.1 contracts and decisions

Record for [DT-4272](https://broadworkbench.atlassian.net/browse/DT-4272) (S1.1). Decisions were
recorded on 2026-10-19 unless dated otherwise. The type config (S1.2) and catalog lookup (S2.1,
S2.2) build on this record. It amends the [delivery plan](data-showcase-delivery-plan.md); where
the two differ, this record wins.

## Sources and precedence

Conflicts are resolved in this order:

1. Product decisions: 2026-10-07 (code-defined showcases authored by PR, no admin UI) and
   2026-10-08 (Postgres-backed lookup), as recorded at the top of the delivery plan.
2. The original DT-3904 criteria, recovered from Jira change history:
   [original criteria](data-showcase-original-criteria.md).
3. The current DT-3904 description's "[NEW]" and "Updated" text, where it doesn't conflict with
   1 or 2.
4. The design artifact, for structure, layout and visuals only. It is being rebuilt with the same
   structure and features under names DUOS is licensed to use, so the
   [artifact review](data-showcase-artifact-review.md) still applies. Fixtures use no artifact names.

The delivery plan's proposals apply where none of these sources decides.

## Where each behavior comes from

| Behavior | Source |
| --- | --- |
| Showcases are code configs reviewed by PR; no admin console, drafts or no-deploy editing (replaces Stories 24–30 and Story 23's "without a code deploy") | Decision 2026-10-07 |
| Public lookup is a Postgres-backed consent endpoint | Decision 2026-10-08 |
| 23 registry keys in fixed order | Current DT-3904 table; three keys renamed (below) |
| Only content sections have anchors (21 of 23); navigation is built from visible navigable sections | Artifact; delivery plan section 3 |
| Six-section MVP: `masthead`, `hero`, `latest-data-releases`, `open-access-datasets`, `featured-tools`, `featured-workspaces` | Delivery plan section 2; approved |
| Configs list production `DUOS-` identifiers; previews read production with DUOS handoffs disabled | Delivery plan section 3; approved |
| Search submits to `/datalibrary` | Current DT-3904 Story 3 |
| Hero stats are live: studies, datasets, tools | Original Story 4; MVP decision |
| Study and dataset counts cover the showcase's scope (flagship: the whole public catalog); tools = enabled `featured-tools` and `featured-workspaces` items | Decision |
| Carousel: manual controls, keyboard access, reduced motion | Original Story 4; artifact review findings 2 and 4 |
| `latest-data-releases` is computed newest-first | Original Story 5 (release date descending) |
| Its date is the NewStudyDigest date: `dac_approval_date` for controlled datasets, `create_date` for open or external ones; no time window | Decision |
| "New in DUOS" badge for datasets at most 30 days old | Original Story 5 (badge); decision (window, label) |
| `largest-cohorts` ranks and labels participant counts, not samples | Decision; replaces original Story 7's sample count |
| `open-access-datasets` is curated or computed, chosen per showcase; open access only; card links straight to access | Decision; original Story 8 |
| Item fields are Story 29's forms only; the artifact's extra fields (PI, versions, clone counts, groupings) are excluded | Current Story 29; decision |
| Analysis Apps allow one featured card | Original Story 14 |
| Impact: an embed, or a static snapshot labeled as not live, with attribution | Original Story 21 |
| Conference registration: live link or alert signup | Original Story 22 |
| Workshops: public signup and a private-training request path; the artifact's multi-session enrollment is not required | Original Story 23 |
| Shelf, grid, featured-card and panel layouts | Artifact |
| Breakpoints, theme presets, typography | Artifact as evidence; design sets values in S1.3 |

## Registry key renames

| Was | Now |
| --- | --- |
| `anvil-impact` | `program-impact` |
| `anvil-workshops` | `training-workshops` |
| `anvil-scholars` | `scholars-program` |

No code uses these keys yet, so nothing needs retiring.

## Story 29 correction

The current DT-3904 Story 29 table runs two rows together ("destination URL.At a Glance (8)").
The intended rows are:

| Section type | Item form |
| --- | --- |
| Research areas and initiatives (16–17) | Label, description, icon, destination URL. |
| At a Glance (8) | Choose which charts to show. Charts compute from the showcase's curated datasets. |

This record is the correction. DT-3904 itself is not edited.

## Fixtures and lookup examples

[`data-showcase-fixtures.ts`](data-showcase-fixtures.ts) (also in duos-ui at
`test/fixtures/showcase/contracts.ts`, with a test) contains:

- `SECTION_KEYS`: all 23 keys in fixed order. The test checks that every config lists them in that
  order and enables only MVP sections.
- Provisional section types, a flagship config and a partner config.
- Lookup request and response examples for the `identifiers`, `ranking`, `all` and `stats` modes,
  plus a 400 rejection.

The `stats` mode is new to S2.2. Request: `{ "mode": "stats", "scope"?: {...} }`. Response:
`{ "studyCount", "datasetCount" }`. It counts the datasets that pass S2.1 eligibility within the
scope, and their distinct studies. Without a scope it counts the whole public catalog. It has the
same body, scope and no-store rules as the other modes and rejects other fields with 400. Field
names and the `scope` vocabulary stay provisional until S2.1.

## Open gates

| Gate | Owner | Blocks |
| --- | --- | --- |
| Design review: desktop and mobile shelves, carousel controls, keyboard order, logo slots, error states, enabled versus visible sections. Not yet held. | Designer | S3.1–S3.4 |
| Rebranded artifact supplied; artifact review updated with its hash | Product owner | — |
| S2.1 field names, nullability and `scope` vocabulary | Consent catalog lead, data stewards, AppSec | S2.2 |
| Sign-off on this record | Product owner, designer, consent lead, duos-ui lead | S1.2, S2.1 |
