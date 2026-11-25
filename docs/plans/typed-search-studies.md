# Typed Search — Study View

Ticket: [DT-3848](https://broadworkbench.atlassian.net/browse/DT-3848) (slice 3a of 6)

**Status: proposal (2026-11-25).** This document defines the `studies` view of
[`typed-search-contract.md`](typed-search-contract.md) ("the contract"): its keys, its search
fields, the rules that the server adds for each scope, its projections, and an example for each
caller. "Design" refers to [`elasticsearch-to-postgresql-plan.md`](elasticsearch-to-postgresql-plan.md).
[`typed-search-datasets-registry.md`](typed-search-datasets-registry.md) ("the dataset registry")
defines the dataset keys that this view shares.

The duos-ui sources are at duos-ui `origin/develop` (`c52f9246`): `studyAsset.ts`,
`StudyCardGrid.tsx`, `StudyCard.tsx`, and `studyColumns.tsx` in `components/data_library/`, and
`useStudyDetailsData.ts` and `StudyDetails.tsx` for the study page.

## 1. What a study row is

A study row aggregates the datasets of the study that match the request: the rules of the scope,
the `search` text, and the filters. A study with no matching dataset is not a row. This is what the
Studies tab does today: it sends dataset documents to an ES aggregation by `study.studyId`
(`buildStudiesAgg`), so the count, the participant total, the dataset ids, and the chips of a study
cover the matching datasets only.

`total` is the number of study rows. ES counts them with `cardinality`, which is approximate; the
server count is exact (contract section 7).

## 2. Keys

**Filters.** The `studies` view accepts the filter keys of the `datasets` view (dataset registry
section 1) and the row filter keys of each asset type
([`typed-search-assets-registry.md`](typed-search-assets-registry.md) section 4). A dataset key
keeps the datasets that match. An asset key keeps the studies with one asset of its type that
matches. duos-ui sends every active filter of every tab on each request today
(`buildActiveFilterClauses`), so a `soApprovalModel` filter set on the Datasets tab also narrows the
Studies tab, as today.

**Facets.** The facet keys of the `datasets` view: `accessManagement`, `dataUse`,
`dataUseModifiers`, `dataType`, and `dac`. A facet counts the matching datasets, as the ES facets
of the Studies request count dataset documents today (contract section 7). No caller reads these
facets today; the server accepts them so that the Studies tab can show counts later.

**Sort keys** (contract section 5):

| Key | Sorts by | Today (ES) |
| --- | --- | --- |
| `studyName` | `study.studyName`, as text, with regard to case, as ES sorts the keyword today | `composite` source on `study.studyName.keyword` |
| `totalParticipants` | The sum of `participantCount` of the matching datasets | `terms` order on the `sum` |
| `datasetCount` | The number of matching datasets | `terms` order on the `value_count` |
| `phenotype` | `study.phenotype` | Ignored: default order |
| `species` | `study.species` | Ignored: default order |
| `piName` | `study.piName` | Ignored: default order |
| `dataCustodianEmail` | `study.dataCustodianEmail`, a list: lowest value ascending, highest descending | Ignored: default order |

Without `sort`, and as the tie-breaker, the order is `studyId` ascending.

## 3. Search fields

The `search` text matches the search fields of the library (`ALL_SEARCH_FIELDS`), as on each tab
today. The fields that the study type adds to the union (`studyAsset.searchFields`) beyond the
dataset fields of the dataset registry section 2 are `dac.dacName`, `study.phenotype`, and
`study.data.tags`. The asset fields of the union are in the asset registry, section 6.

A match on a dataset field keeps that dataset. A match on a study or asset field keeps each dataset
of the study.

## 4. Server rules for each scope

The `studies` view accepts three scopes. Another scope gets a 400: no caller sends it today.

| Scope | Public visibility rule | Approved rule | Has a study | Callers |
| --- | --- | --- | --- | --- |
| `library` | Yes, for a user without a privileged role | **No** | Yes | Data Library, Studies tab |
| `ownedByMe` | No: every role that can use the scope is exempt | No | Yes | Submissions, Studies tab |
| `studyId` | No | No | Yes | Study page header |

- The public visibility rule and the "has a study" rule are those of the dataset registry, section
  3.
- **No approved rule.** The Studies request does not take the approved rule today
  (`studyAsset.buildQuery` accepts no options). A controlled dataset without DAC approval thus counts
  in the dataset count, the participant total, and the dataset ids of its study, but is not a row of
  the Datasets tab. The `counts` view keeps the same difference
  ([`typed-search-counts.md`](typed-search-counts.md)).
- The `assets` view uses the rules of the `library` and `ownedByMe` rows of this table.
- Q6 can change the rules of the `studyId` scope (slice 4).

## 5. Projections

| Projection | Fields | Callers |
| --- | --- | --- |
| `library` | `studyId`, `studyName`, `studyDescription`, `piName`, `species`, `phenotype`, `dataTypes`, `dataUseCodes`, `accessTypes`, `datasetCount`, `totalParticipants`, `datasetIds`, `modelCount`, `workspaceCount` | Data Library cards (`StudyCard.tsx`), study selection (`studyAsset.ts`) |
| `submission` | The `library` fields, and `dataCustodianEmail` | Submissions table (`studyColumns.tsx`), which shows the Data Custodian column |
| `header` | `studyId`, `studyName`, `description`, `dataTypes`, `piName`, `phenotype`, `species`, `dataCustodianEmail`, `datasetCount`, `totalParticipants` | Study page header (`StudyDetails.tsx`) |

| Scope | Permitted projections | Default |
| --- | --- | --- |
| `library` | `library` | `library` |
| `ownedByMe` | `library`, `submission` | `submission` |
| `studyId` | `header` | `header` |

The fields of the `submission` projection are those of the study row that duos-ui builds today
(`StudyAggregation`, `types/library.ts`). The `library` projection leaves out `dataCustodianEmail`:
the cards do not show it (`useStudyDetailsData.ts` notes this), so the library scope does not
return the emails of every study in bulk. This follows the rule of the dataset registry, section 5.

- `studyName`, `studyDescription` (from `study.description`), `piName`, `species`, `phenotype`,
  `dataCustodianEmail` (a list, `submission` only), and `dataTypes` (a list) come from the study.
- `datasetCount`, `totalParticipants`, and `datasetIds` cover the matching datasets. `datasetIds`
  drives the study checkbox, which selects every dataset of the study. A row has 10,000 ids at
  most, as ES returns today (`terms` size 10,000), so the size of a row has a limit.
- `dataUseCodes` and `accessTypes` are the distinct `dataUse.primary` codes and `accessManagement`
  values of the matching datasets, by dataset count descending, then by value. ES returns 10 of each
  at most (`terms` size 10), and the server keeps that limit.
- `modelCount` and `workspaceCount` count the models and workspaces of the study that have a record
  id, as the asset rows do (asset registry section 1). They do not depend on the asset filters.

The `header` projection replaces the two aggregations that the study page adds to its dataset
request today (`study_details`, `total_participants` in `useStudyDetailsData.ts`). Its
`datasetCount` replaces the hits total that the page shows as the dataset count. The page reads the
dataset rows with a separate `datasets` request (dataset examples, example 1.9).

**Personal data** (design section 3.1a): `piName` in each projection, and `dataCustodianEmail` in
`submission` and `header`. The cards show the PI name, and the Submissions table and the study page
show both. Today each signed-in user reads them through the ES endpoints. The `search` text still
matches `study.piName` and `study.dataCustodianEmail` in each scope, as today (dataset registry,
section 5). DT-3844 enforces the field rules and decides the search fields of each scope.

## 6. Examples

The values are illustrative. Slice 5 turns each example into a JSON fixture.

### 6.1 Data Library, Studies tab

```json
{
  "view": "studies",
  "scope": { "library": null },
  "search": "heart",
  "filters": [ { "key": "dataType", "values": ["WGS"] } ],
  "sort": { "key": "totalParticipants", "order": "desc" },
  "page": { "offset": 0, "limit": 12 }
}
```

```json
{
  "view": "studies",
  "total": 18,
  "page": { "offset": 0, "limit": 12 },
  "results": [
    {
      "studyId": 7,
      "studyName": "Heart Study",
      "studyDescription": "A cohort study of heart disease.",
      "piName": "Example PI",
      "species": "Human",
      "phenotype": "Cardiovascular disease",
      "dataTypes": ["WGS", "RNA-seq"],
      "dataUseCodes": ["GRU", "HMB"],
      "accessTypes": ["controlled", "open"],
      "datasetCount": 3,
      "totalParticipants": 4200,
      "datasetIds": [101, 102, 105],
      "modelCount": 1,
      "workspaceCount": 0
    }
  ]
}
```

The study has more datasets, but three match the search, the filter, and the scope rules. Today
the browser takes the page from the first 10,000 study buckets; the server pages, and the card grid
can show a page after study 10,000.

### 6.2 Submissions, Studies table

A Data Submitter sorts their studies by PI name, a sort that ES ignores today (contract section 5).

```json
{
  "view": "studies",
  "scope": { "ownedByMe": true },
  "sort": { "key": "piName", "order": "asc" },
  "page": { "offset": 0, "limit": 25 }
}
```

The response has the shape of example 6.1, with `dataCustodianEmail` (the default projection of
the scope is `submission`). A study without a PI name comes last.

### 6.3 Study page header

```json
{
  "view": "studies",
  "scope": { "studyId": 42 },
  "page": { "offset": 0, "limit": 1 }
}
```

```json
{
  "view": "studies",
  "total": 1,
  "page": { "offset": 0, "limit": 1 },
  "results": [
    {
      "studyId": 42,
      "studyName": "Kidney Study",
      "description": "A study of kidney function.",
      "dataTypes": ["WES"],
      "piName": "Example PI",
      "phenotype": "Chronic kidney disease",
      "species": "Human",
      "dataCustodianEmail": ["custodian@example.org"],
      "datasetCount": 12,
      "totalParticipants": 9800
    }
  ]
}
```

A study without datasets has no row: `total` is 0 and `results` is empty. The page also reads the
study with `Study.getById`, as today, and uses it for the name, description, data types, and PI name
that the header lacks.

### 6.4 Errors

| Request | Status | `message` (illustrative) |
| --- | --- | --- |
| `"sort": { "key": "datasetName" }` | 400 | `Unknown sort key for studies: datasetName` |
| `"scope": { "dacIds": [3] }` | 400 | `Scope not accepted by view studies: dacIds` |
| `"projection": "header"` with the `library` scope | 400 | `Projection not permitted for scope library: header` |
| `"facets": [ { "key": "modelLicense" } ]` | 400 | `Unknown facet key for studies: modelLicense` |
