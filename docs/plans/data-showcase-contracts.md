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

S2.1 sets the field names, nullability and `scope` vocabulary below. Data stewards and AppSec still
have to approve them (open gates).

## S2.1 catalog projection

Record for [DT-4274](https://broadworkbench.atlassian.net/browse/DT-4274). `ShowcasePublicField`
defines the fields, `ShowcaseDatasetSummary` returns them in this order, and
`DatasetDAO.findShowcaseSummaries` reads them.

| Field | Source | Type | Null | Rule |
| --- | --- | --- | --- | --- |
| `identifier` | `dataset.alias` | string | no | `DUOS-` plus the alias padded to six digits |
| `title` | `dataset.name` | string | no | |
| `accessManagement` | `dataset_property` `accessManagement`, then legacy `consentGroup.accessManagement` | `open`, `controlled`, `external` | yes | Keys match ignoring case; values are trimmed and lower-cased; unrecognized values are skipped; null when no value is recognized |
| `dacName` | `dac.name` through `dataset.dac_id` | string | yes | |
| `consentCodes` | `dataset.data_use` | string[] | no | Codes only (`GRU`, `HMB`, `DS`, `OTHER`, ...), in `TranslationUtil.translateSummary` order; free-text values and disease labels are omitted |
| `participantCount` | `dataset_property` `numberOfParticipants` | integer (participants) | yes | Trimmed and parsed as an integer; null if missing, non-integer or negative |
| `availableDate` | `dataset.create_date` (open, external) or `dataset.dac_approval_date` (others) | `YYYY-MM-DD` | yes | Date part of the stored timestamp, not shifted by the reader's JVM time zone. The columns have no time zone, so the date is UTC only if the writer ran in UTC |
| `accessUrl` | `dataset_property` `url`, then legacy `consentGroup.url` | string | yes | Open and external only; absolute `http`/`https` with a host and no user info; null otherwise |
| `species` | `study_property` `species` | string | yes | Trimmed; null if blank. Free text with no length limit; needs the steward decision below |

Responses serialize the summary with `GsonUtil.getNullSerializingInstance()`, so a null field is
present as `null`. When a property has several rows, the row with the lowest id is used. Older
`# of participants` rows with no `schema_property` are not read, so their count is null. The query reads only
`dataset_property.schema_property IN (numberOfParticipants, url, consentGroup.url)` and
`study_property.key IN (species)`, plus the two access-management keys used for eligibility.

Omitted: study description and phenotype (free text), PI name, data types, file formats, data size
(no trusted byte source), `translated_data_use` (embeds free text).

Open decision: `species` is free text in the registration schema and study template, so it could
hold an email or a long note. Data stewards decide whether to approve it as is, truncate it, or omit
it.

### S2.2 requirements

- Write the response entity with `GsonUtil.getNullSerializingInstance()`. The default
  `JerseyGsonProvider` uses `GsonUtil.getInstance()`, which drops null fields. Add a resource-level
  test that the serialized keys equal `ShowcasePublicField.jsonNames()`.
- `DatasetDAO.findShowcaseSummaries` has no limit on the number of aliases. Enforce the
  200-identifier cap before calling it.

### Eligibility

A dataset is returned only if all of these hold:

1. It has a study, and `study.public_visibility = true` (the column is `NOT NULL`).
2. `accessManagement` is `open` or `external`, or `dataset.dac_approval = true`. Controlled datasets,
   and datasets without a recognized access value, need DAC approval.

Admin, creator and custodian exceptions do not apply. Deleted datasets are absent because they are
hard-deleted. Eligibility inputs are not returned.

### `scope` vocabulary

`ShowcaseScopeKey`:

| Key | Column | Values |
| --- | --- | --- |
| `dac` | `dac.name` | DAC names, exact match |

The values for one key are alternatives (any one can match). Multiple keys must all match.

## Open gates

| Gate | Owner | Blocks |
| --- | --- | --- |
| Design review: desktop and mobile shelves, carousel controls, keyboard order, logo slots, error states, enabled versus visible sections. Not yet held. | Designer | S3.1–S3.4 |
| Rebranded artifact supplied; artifact review updated with its hash | Product owner | — |
| S2.1 field definitions (including the `species` free-text decision) and `scope` vocabulary approved | Consent catalog lead, data stewards, AppSec | S2.2 |
| Each owner confirms they can trace every behavior above to its source (acceptance criterion 1) | Product owner, designer, consent lead, duos-ui lead | Closing DT-4272 |
