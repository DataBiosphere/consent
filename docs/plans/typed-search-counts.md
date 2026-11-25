# Typed Search — Counts View

Ticket: [DT-3848](https://broadworkbench.atlassian.net/browse/DT-3848) (slice 3b of 6)

**Status: proposal (2026-11-25).** This document defines the `counts` view of
[`typed-search-contract.md`](typed-search-contract.md) ("the contract"): the names of its totals,
the rule of each total, and an example for each caller. "Design" refers to
[`elasticsearch-to-postgresql-plan.md`](elasticsearch-to-postgresql-plan.md).

The `counts` view gives the numbers on the tab badges of the Data Library and the Submissions page.
Today duos-ui computes them from one ES request (`useLibraryTabCounts.ts`, `libraryCounts.ts`, at
duos-ui `origin/develop` `c52f9246`). That request also carries the study documents that the asset
tabs read their rows from; with the contract, the asset tabs send their own `assets` requests
instead ([`typed-search-examples-assets.md`](typed-search-examples-assets.md)).

## 1. Request

The view accepts `view`, `scope`, `search`, and `filters` (contract section 2). Another field gets a
400. It accepts the `library` and `ownedByMe` scopes, as the `assets` view does; another scope gets
a 400.

The tab counts do not depend on the tab, the page, or the sort today (the ES request key is the
library, the filters, and the search text). The caller sends the same `search` and `filters` as the
requests of the tabs.

## 2. Response

```json
{
  "view": "counts",
  "totals": {
    "studies": 18,
    "datasets": 42,
    "models": 3,
    "workspaces": 2,
    "clinical_trials": 4,
    "biospecimens": 120,
    "publications": 14,
    "presentations": 6,
    "intellectual_properties": 1,
    "funding_resources": 5
  }
}
```

`totals` has the ten names below, always all ten. The names are the tab keys of duos-ui
(`AssetType`, `types/library.ts`), so the client puts each total on its tab without a map. Each
total is exact.

| Name | Equals | Today (ES) |
| --- | --- | --- |
| `studies` | `total` of the `studies` view with the same scope, search, and filters | `total_studies`, a `cardinality` on `study.studyId`: approximate, no approved rule |
| `datasets` | `total` of the `datasets` view with the same scope, search, and filters | `datasets_count`, a `filter` aggregation: the approved rule in the Data Library, every dataset in Submissions |
| `models`, `workspaces`, `clinical_trials`, `biospecimens`, `publications`, `presentations`, `intellectual_properties`, `funding_resources` | `total` of the `assets` view for that `assetType` with the same scope, search, and filters | The browser builds the rows of up to 10,000 study buckets, applies the row filters of the type, and counts the rows |

- **Same rules as the views.** Each total uses the rules of its view: the scope rules, the search
  fields, and the match of each filter key. A badge thus equals the `total` of its tab.
- **Studies and datasets.** The `studies` total has no approved rule; the `datasets` total in the
  library has it ([`typed-search-studies.md`](typed-search-studies.md) section 4). A study whose
  only datasets are controlled and not approved counts on the Studies tab and adds nothing to the
  Datasets tab, as today.
- **Asset totals.** The row filter keys of a type apply to the rows of that type. Each other filter
  narrows the studies (contract section 7). A model or workspace without a record id is not counted,
  as it is not a row ([`typed-search-assets-registry.md`](typed-search-assets-registry.md) section
  1).
- **No 10,000 limit.** Today the asset counts cover the assets of the first 10,000 study buckets.
  The server counts every row.
- **Funding resources in Submissions.** The Submissions page has no Funding Resources tab. The
  response still has `funding_resources`, as `computeTabCounts` computes it today; the page does not
  show it.

## 3. The dashboards count differently

The Researcher, DAC, and Signing Official dashboards keep their own server endpoints (contract
section 3), and their numbers do not change. They differ from the badges:

| Dashboard total (`DashboardSearchService`) | Difference from the `counts` view |
| --- | --- |
| Data Library models and workspaces | Counts every entry of the study lists, with or without a record id. The `counts` view counts rows. |
| Data Submissions total | One number: the studies, the datasets, and seven asset types (no funding), with no search or filter. |
| DAC datasets, Institution library | Scopes that `/api/search` does not have (contract section 3). |

The plan for the dashboards is in design section 7.4.

## 4. Examples

The values are illustrative. Slice 5 turns each example into a JSON fixture.

### 4.1 Data Library, no search

```json
{
  "view": "counts",
  "scope": { "library": null }
}
```

The response has the shape of section 2. A user without a privileged role gets the totals with the
public visibility rule.

### 4.2 Data Library, with search and filters

```json
{
  "view": "counts",
  "scope": { "library": "nhgri" },
  "search": "cardiac",
  "filters": [
    { "key": "dataType", "values": ["WGS"] },
    { "key": "modelLicense", "values": ["mit"] }
  ]
}
```

```json
{
  "view": "counts",
  "totals": {
    "studies": 4,
    "datasets": 9,
    "models": 2,
    "workspaces": 1,
    "clinical_trials": 0,
    "biospecimens": 30,
    "publications": 5,
    "presentations": 0,
    "intellectual_properties": 0,
    "funding_resources": 2
  }
}
```

- `modelLicense` is a row filter for `models`: the total counts the models with a license that
  contains "mit".
- For each other total, `modelLicense` narrows the studies: a study counts if it has a model whose
  license contains "mit". `workspaces` counts every workspace of these studies.
- `dataType` and the search narrow the studies and the datasets of every total.

### 4.3 Submissions

```json
{
  "view": "counts",
  "scope": { "ownedByMe": true },
  "search": "kidney"
}
```

The response has the shape of section 2. The `ownedByMe` scope adds the ownership rule and no
approved rule, so `datasets` counts every dataset of the user that matches.

### 4.4 Errors

| Request | Status | `message` (illustrative) |
| --- | --- | --- |
| `"page": { "offset": 0, "limit": 0 }` | 400 | `Field not accepted by view counts: page` |
| `"facets": [ { "key": "dac" } ]` | 400 | `Field not accepted by view counts: facets` |
| `"scope": { "studyId": 42 }` | 400 | `Scope not accepted by view counts: studyId` |
| `"assetType": "models"` | 400 | `Field not accepted by view counts: assetType` |
