# Typed Search — Dataset View Examples

Ticket: [DT-3848](https://broadworkbench.atlassian.net/browse/DT-3848) (slice 2b of 6)

**Status: proposal (2026-11-25).** These examples follow
[`typed-search-contract.md`](typed-search-contract.md) ("the contract"). Section numbers with
"contract" refer to it, and "design" refers to
[`elasticsearch-to-postgresql-plan.md`](elasticsearch-to-postgresql-plan.md).

This document has one example for each caller that reads dataset rows today.
[`typed-search-datasets-registry.md`](typed-search-datasets-registry.md) ("the registry") defines
the keys, the search fields, the server rules for each scope, and the projections of the
`datasets` view. Slice 3 covers the `studies`, `assets`, and `counts` views. Slice 4 covers both outcomes of the open product decisions. Slice 5 turns each
example here into a JSON fixture for the consent and duos-ui contract tests.

The values in the examples are illustrative. The field names, the shapes, and the rules are the
contract.

## 1. Examples

Each example names the caller, the request, and the response. The server adds the rules of the
scope (registry section 3). A request without `projection` gets the default projection of its scope
(registry section 5). The results are shortened: `total` can be larger than the rows shown.

### 1.1 Data Library, Datasets tab

A Researcher searches the full library, with two filters, two facets, and a sort.

```json
{
  "view": "datasets",
  "scope": { "library": null },
  "search": "sequencing",
  "filters": [
    { "key": "dataType", "values": ["WGS"] },
    { "key": "participantCount", "gte": 100 }
  ],
  "facets": [ { "key": "dac" }, { "key": "accessManagement" } ],
  "sort": { "key": "datasetName", "order": "asc" },
  "page": { "offset": 0, "limit": 25 }
}
```

```json
{
  "view": "datasets",
  "total": 42,
  "page": { "offset": 0, "limit": 25 },
  "results": [
    {
      "datasetId": 101,
      "datasetIdentifier": "DUOS-000101",
      "datasetName": "Adult Sequencing Cohort",
      "study": { "studyId": 7, "studyName": "Heart Study" },
      "accessManagement": "controlled",
      "dacApproval": true,
      "requestLocation": "DUOS",
      "soApprovalModel": "PER_REQUEST",
      "participantCount": 1200,
      "dacId": 3,
      "dac": { "dacName": "Broad DAC" },
      "dataLocation": "AnVIL Workspace",
      "url": "https://example.org/workspace",
      "instantApprovalEligible": false,
      "dataUse": { "primary": [ { "code": "GRU", "description": "General research use" } ], "secondary": [] }
    }
  ],
  "facets": {
    "dac": [ { "value": "Broad DAC", "count": 30 }, { "value": "NHGRI DAC", "count": 12 } ],
    "accessManagement": [ { "value": "controlled", "count": 35 }, { "value": "open", "count": 7 } ]
  }
}
```

The `library` scope adds the public visibility rule (the user has no privileged role), the approved
rule, and "has a study". The facets count the datasets that match the search, the filters, and
these rules.

### 1.2 Facet metadata

duos-ui loads the facet values once, without filters, and holds them for 24 hours (design section
3.3).

```json
{
  "view": "datasets",
  "scope": { "library": null },
  "facets": [
    { "key": "accessManagement" },
    { "key": "dataUse" },
    { "key": "dataUseModifiers", "size": 50 },
    { "key": "dataType" },
    { "key": "dac" }
  ],
  "page": { "offset": 0, "limit": 0 }
}
```

```json
{
  "view": "datasets",
  "total": 5873,
  "page": { "offset": 0, "limit": 0 },
  "results": [],
  "facets": {
    "accessManagement": [ { "value": "controlled", "count": 5100 }, { "value": "open", "count": 773 } ],
    "dataUse": [ { "value": "GRU", "count": 2400 }, { "value": "HMB", "count": 1900 } ],
    "dataUseModifiers": [ { "value": "NPU", "count": 800 }, { "value": "RS-G", "count": 12 } ],
    "dataType": [ { "value": "WGS", "count": 1500 }, { "value": "RNA-seq", "count": 600 } ],
    "dac": [ { "value": "Broad DAC", "count": 3000 } ]
  }
}
```

A dataset with the data types `["WGS", "RNA-seq"]` counts one time in each of the two buckets.

### 1.3 Branded library

The NHGRI library. The server list maps the key to its rules (tags on the study and on the
dataset, `libraryVersions.ts`).

```json
{
  "view": "datasets",
  "scope": { "library": "nhgri" },
  "page": { "offset": 0, "limit": 25 }
}
```

The response has the shape of example 1.1. A dataset with two tags is in the library if one tag
matches (design section 4.2). An unknown key, for example `{ "library": "nhgrii" }`, gets a 400.

### 1.4 Submissions, Datasets tab

A Data Submitter lists the datasets that they own.

```json
{
  "view": "datasets",
  "scope": { "ownedByMe": true },
  "sort": { "key": "datasetName", "order": "asc" },
  "page": { "offset": 0, "limit": 25 }
}
```

The rows are those the user owns, from the session: the user created the dataset, or submitted the
study, or is a data custodian of the study (pending product decision Q15). The approved rule does
not apply, so controlled datasets without DAC approval appear (`showAllControlled` today). The
default projection is `submission`: the `library` fields with `createUserDisplayName`,
`study.dataCustodianEmail`, and `deletable`. A Researcher without the Data Submitter, Chairperson,
or Admin role gets a 403.

### 1.5 DAC Datasets

A Chairperson lists the datasets of their two DACs.

```json
{
  "view": "datasets",
  "scope": { "dacIds": [3, 8] },
  "projection": "dacTable",
  "sort": { "key": "datasetIdentifier", "order": "asc" },
  "page": { "offset": 0, "limit": 10000 }
}
```

```json
{
  "view": "datasets",
  "total": 312,
  "page": { "offset": 0, "limit": 10000 },
  "results": [
    {
      "datasetId": 101,
      "datasetIdentifier": "DUOS-000101",
      "datasetName": "Adult Sequencing Cohort",
      "dacId": 3,
      "dac": { "dacName": "Broad DAC", "dacEmail": "dac@example.org" },
      "dacApproval": true,
      "deletable": false,
      "hasInstitutionCertification": true,
      "dataUse": { "primary": [ { "code": "HMB", "description": "Health/medical/biomedical research" } ], "secondary": [] },
      "dataLocation": "AnVIL Workspace",
      "url": "https://example.org/workspace",
      "createUserDisplayName": "Dana Submitter",
      "submitter": { "displayName": "Dana Submitter" },
      "study": {
        "studyId": 7,
        "studyName": "Heart Study",
        "phsId": "phs000001",
        "description": "...",
        "species": "Human",
        "piName": "Pat Investigator",
        "dataTypes": ["WGS"],
        "dataCustodianEmail": ["custodian@example.org"],
        "dataSubmitterEmail": "submitter@example.org"
      }
    }
  ]
}
```

The page searches the rows in the browser (`filterDatasetTerms`), so the request has no `search`,
and `dacTable` carries each field that search reads. A DAC id that is not a DAC of the user gets a 403 or
a 404, pending product decision Q13, unless the user is an Admin. A Member gets `dataUse` by
default, and a 400 for `dacTable` (registry section 5).

### 1.6 Manage DAC and DAC profile

```json
{
  "view": "datasets",
  "scope": { "dacIds": [3] },
  "projection": "dataUse",
  "page": { "offset": 0, "limit": 10000 }
}
```

```json
{
  "view": "datasets",
  "total": 120,
  "page": { "offset": 0, "limit": 10000 },
  "results": [
    {
      "datasetId": 101,
      "dacId": 3,
      "dac": { "dacId": 3, "dacName": "Broad DAC" },
      "dataUse": {
        "primary": [ { "code": "HMB", "description": "Health/medical/biomedical research" } ],
        "secondary": [ { "code": "NPU", "description": "Not-for-profit use only" } ]
      }
    }
  ]
}
```

### 1.7 DAR review and DUC addendum

The datasets of a data access request, by id.

```json
{
  "view": "datasets",
  "scope": { "datasetIds": [101, 102, 205] },
  "projection": "dataUse",
  "page": { "offset": 0, "limit": 10000 }
}
```

The response has the shape of example 1.6, with one row for each id that exists. Who can use this
scope is pending product decision Q6 (slice 4). More than 10,000 ids get a 400; the caller sends
them in batches of 10,000 (contract section 6).

### 1.8 Dataset page

The dataset page looks the dataset up by its identifier.

```json
{
  "view": "datasets",
  "scope": { "datasetIdentifiers": ["DUOS-000101"] },
  "projection": "detail",
  "page": { "offset": 0, "limit": 1 }
}
```

```json
{
  "view": "datasets",
  "total": 1,
  "page": { "offset": 0, "limit": 1 },
  "results": [
    {
      "datasetId": 101,
      "datasetIdentifier": "DUOS-000101",
      "datasetName": "Adult Sequencing Cohort",
      "accessManagement": "controlled",
      "requestLocation": "DUOS",
      "dataLocation": "AnVIL Workspace",
      "url": "https://example.org/workspace",
      "participantCount": 1200,
      "dacId": 3,
      "instantApprovalEligible": false,
      "dataUse": { "primary": [ { "code": "GRU", "description": "General research use" } ], "secondary": [] },
      "study": {
        "studyName": "Heart Study",
        "description": "...",
        "phenotype": "Cardiovascular",
        "piName": "Pat Investigator",
        "dataCustodianEmail": ["custodian@example.org"]
      }
    }
  ]
}
```

`duos-000101` matches the same dataset: an identifier matches without regard to case (contract
section 3). An identifier that matches no dataset returns `total` 0 and no rows.

The page also opens from links of the form `/dataset/DUOS-D101` (`DatasetList.tsx`), which name the
dataset id, not the identifier (`DatasetStatistics.tsx`). For these, the page sends
`"scope": { "datasetIds": [101] }` with the same projection.

### 1.9 Study page grid

The datasets of one study, one page at a time.

```json
{
  "view": "datasets",
  "scope": { "studyId": 7 },
  "sort": { "key": "datasetName", "order": "asc" },
  "page": { "offset": 0, "limit": 25 }
}
```

The response has the shape of example 1.1. Who can use this scope is pending product decision Q6.
Today the same ES request also returns the study details (`top_hits` on `study.*`) and the total
participants (`sum`) that the page header shows (`useStudyDetailsData.ts`, `StudyDetails.tsx`).
The page reads them from a `studies` view request with the same scope; slice 3 has the example.

### 1.10 Study page selection

The study page asks for every dataset of the study, to know which ones the user can select. Today
it sends two requests with a size equal to the dataset count; one request now returns both the
selection fields and the DAC ids.

```json
{
  "view": "datasets",
  "scope": { "studyId": 7 },
  "projection": "selection",
  "page": { "offset": 0, "limit": 10000 }
}
```

```json
{
  "view": "datasets",
  "total": 12500,
  "page": { "offset": 0, "limit": 10000 },
  "results": [
    { "datasetId": 101, "accessManagement": "controlled", "dacApproval": true, "dacId": 3 }
  ]
}
```

`total` is larger than the page, so the caller sends the next page:

```json
{
  "view": "datasets",
  "scope": { "studyId": 7 },
  "projection": "selection",
  "page": { "offset": 10000, "limit": 10000 }
}
```

Today this request fails for a study with more than 10,000 datasets (design section 3.3).

## 2. Error examples

| Request | Response |
| --- | --- |
| `"filters": [ { "key": "dataTyp", "values": ["WGS"] } ]` | 400, `{ "message": "Unknown filter key: dataTyp", "code": 400 }` |
| `"sort": { "key": "dataUse", "order": "asc" }` | 400: `dataUse` is not a sort key of the view |
| `"filters": [ { "key": "participantCount", "values": ["100"] } ]` | 400: `participantCount` permits only a range |
| `"page": { "offset": 0, "limit": 10001 }` | 400: more than 10,000 rows in one page |
| `"assetType": "models"` with `"view": "datasets"` | 400: the `datasets` view does not accept `assetType` |
| A Researcher, not a DAC member, sends `"scope": { "dacIds": [3] }` | 403: the user cannot use the DAC scope for DAC 3 |
| `"scope": { "ownedByMe": true, "dacIds": [3] }` | 400: the scope has two keys |
| `"scope": { "library": null }`, `"projection": "dacTable"` | 400: the `library` scope does not permit `dacTable` (registry section 5) |
