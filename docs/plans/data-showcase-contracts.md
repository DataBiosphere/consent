# Data Showcase: S1.1 contracts and decisions

Record for [DT-4272](https://broadworkbench.atlassian.net/browse/DT-4272) (S1.1). Decisions were
recorded on 2026-10-19 unless dated otherwise. The type config (S1.2) and catalog lookup (S2.1,
S2.2) build on this record. It amends the [delivery plan](data-showcase-delivery-plan.md); where
the two differ, this record wins.

## Sources and precedence

Conflicts are resolved in this order:

1. Product decisions: 2026-10-07 (code-defined showcases authored by PR, no admin UI), 2026-10-08
   (Postgres-backed lookup), and the rows marked "Decision" below.
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
| The sub-nav lists visible content sections only; masthead and hero have no anchor (as in the artifact's 21 anchors) | Decision; overrides original Story 3's "every section" |
| Six-section MVP: `masthead`, `hero`, `latest-data-releases`, `open-access-datasets`, `featured-tools`, `featured-workspaces` | Decision (delivery plan section 2) |
| Configs list canonical production `DUOS-` identifiers; previews read production with DUOS handoffs disabled | Decision (delivery plan section 3) |
| Search submits to `/datalibrary` | Current DT-3904 Story 3 |
| Hero stats are live: studies, datasets, tools; a stat that is 0 is hidden | Original Story 4; decision (MVP, hiding) |
| No data-size stat: original Story 4's "total data size" is replaced by a dataset count, because DUOS has no trusted byte-size source | Decision |
| Study and dataset counts use the requestable tier (the rows a shelf can show) within the hero's `scope`; no scope, as on the flagship, means the whole public catalog | Decision |
| Tools count the items in enabled `featured-tools`, `new-workflow-tools`, `notebooks` and `ai-models`; workspaces are not tools | Decision |
| Hero onboarding links to the workspaces shelf; a same-page link is hidden when its target section isn't visible | Original Story 18 |
| Carousel: manual controls, keyboard access, reduced motion | Original Story 4; artifact review finding 2 |
| Rankings, `stats` counts and charts are computed live on each request, not precomputed by a scheduled rollup job; caching or precomputation is added only if the ranking-cost experiment shows a need. Live results meet original Story 2's freshness goal | Decision; overrides original Story 2's rollup job, cache and invalidation path |
| `latest-data-releases` is computed newest-first | Original Story 5 (release date descending) |
| Its date: `dac_approval_date` for controlled datasets, `create_date` for open or external ones (the NewStudyDigest columns); no time window | Decision |
| "New in DUOS" badge for datasets at most 30 days old (UTC dates) | Original Story 5 (badge); decision (window, label) |
| Workflow items carry an optional `addedDate` for the same 30-day "New" badge | Original Story 15 |
| `largest-cohorts` ranks and labels participant counts, not samples | Decision; replaces original Story 7's sample count |
| `open-access-datasets` is curated or computed, chosen per showcase; open access only; card links straight to access | Decision; original Story 8 |
| Item fields are Story 29's forms, plus the workflow `addedDate`; the artifact's extra fields (PI, versions, clone counts, groupings) are excluded | Current Story 29; decision |
| Analysis Apps allow one featured card | Original Story 14 |
| Impact: an embed, or a periodically refreshed snapshot of through.bio data flagged as not live; attribution either way | Original Story 21 |
| Conference: dates with time zone, location, description; live registration link or alert signup | Original Story 22; Story 29 |
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

Current Story 13 adds a flagship-only full-catalog chart scope. This record is the correction;
DT-3904 itself is not edited.

## Fixtures and lookup examples

duos-ui `test/fixtures/showcase/contracts.ts`, with `contracts.test.ts`
([duos-ui#4029](https://github.com/DataBiosphere/duos-ui/pull/4029)), contains the following. S1.2
moves the types to `src/types/showcase.ts`; the fixture configs and examples stay as test data.

- `SECTION_KEYS`: all 23 keys in fixed order. The test checks that every config lists them in that
  order, enables only MVP sections and uses canonical identifiers.
- Provisional section types, the section-to-ranking mapping, a flagship config and a partner config.
- Lookup request and response examples for the `identifiers`, `ranking`, `all` and `stats` modes,
  plus 400 rejections for a field the mode doesn't accept and a non-canonical identifier.

Contract rules the fixtures fix:

- **Identifiers:** `DUOS-` plus the alias zero-padded to six digits, with no extra padding. Configs
  use this form and consent echoes it unchanged, so the page can match results to config order.
  The lookup rejects any other form, such as `DUOS-123`, with 400.
- **Dates:** `YYYY-MM-DD` in UTC; date ranges include both ends.
- **`stats` mode** (new to S2.2): request `{ "mode": "stats", "scope"?: {...} }`, response
  `{ "studyCount", "datasetCount" }`. It counts requestable datasets within the scope and their
  distinct studies, follows the same body, scope and no-store rules as the other modes, and rejects
  other fields with 400.

Field names, nullability and the `scope` vocabulary stay provisional until S2.1.

## Open gates

| Gate | Owner | Blocks |
| --- | --- | --- |
| Design review: desktop and mobile shelves, carousel controls, keyboard order, logo slots, error states, enabled versus visible sections. Not yet held. | Designer | S3.1–S3.4 |
| Rebranded artifact supplied; artifact review updated with its hash | Product owner | — |
| S2.1 field names, nullability and `scope` vocabulary | Consent catalog lead, data stewards, AppSec | S2.2 |
| Each owner confirms they can trace every behavior above to its source (acceptance criterion 1) | Product owner, designer, consent lead, duos-ui lead | Closing DT-4272 |
