# Typed Search — Asset View Examples

Ticket: [DT-3848](https://broadworkbench.atlassian.net/browse/DT-3848) (slice 3d of 6)

**Status: proposal (2026-11-25).** These examples follow
[`typed-search-contract.md`](typed-search-contract.md) ("the contract").
[`typed-search-assets-registry.md`](typed-search-assets-registry.md) ("the registry") defines the
asset types, the row fields, the sort keys, the filter keys, the facet keys, and the search fields
of the `assets` view. "Design" refers to
[`elasticsearch-to-postgresql-plan.md`](elasticsearch-to-postgresql-plan.md).

Today the asset tabs of the Data Library and the Submissions page send no request of their own:
they read up to 10,000 study documents from the tab counts response and build the rows in the
browser (`useLibraryPageState.ts`). With the `assets` view, each tab sends the requests below. Slice
5 turns each example into a JSON fixture for the consent and duos-ui contract tests.

The values are illustrative. The field names, the shapes, and the rules are the contract. The
results are shortened: `total` can be larger than the rows shown.

## 1. Examples

### 1.1 Data Library, AI Models tab

A Researcher searches the full library, with a row filter (`modelLicense`), a study filter
(`dataType`), and a sort.

```json
{
  "view": "assets",
  "assetType": "models",
  "scope": { "library": null },
  "search": "cardiac",
  "filters": [
    { "key": "modelLicense", "values": ["mit"] },
    { "key": "dataType", "values": ["WGS"] }
  ],
  "sort": { "key": "name", "order": "asc" },
  "page": { "offset": 0, "limit": 25 }
}
```

```json
{
  "view": "assets",
  "total": 3,
  "page": { "offset": 0, "limit": 25 },
  "results": [
    {
      "studyId": 7,
      "studyName": "Heart Study",
      "assetKey": "id:45",
      "modelId": 45,
      "duosIdentifier": "DUOS-M-000045",
      "name": "Arrhythmia Classifier",
      "description": "Classifies ECG traces.",
      "url": "https://example.org/models/45",
      "format": "ONNX",
      "license": "MIT",
      "cloud": ["GCP"],
      "trainedOnDatasets": ["DUOS-000101"],
      "maintainer": { "name": "Example Maintainer", "email": "maintainer@example.org" },
      "tags": ["cardiology"]
    }
  ]
}
```

- The study must have one dataset that passes the scope rules of the `studies` view, has the data
  type `WGS`, and matches "cardiac" on one of the search fields (registry sections 1 and 6). The
  search can match the dataset, its study, or any asset of the study, as today.
- `modelLicense` "mit" is a "contains" filter on the row: it keeps "MIT" and "MIT-0".
- A model of the same study with the license "Apache-2.0" is not a row. A model without a
  `modelId` is not a row (registry section 1).
- `total` counts the model rows, not the studies.

### 1.2 Option lists of the AI Models tab

The tab loads its option lists with a separate request. It leaves out the filters on its own facet
keys (`modelLicense` here) and keeps the other filters (registry section 5).

```json
{
  "view": "assets",
  "assetType": "models",
  "scope": { "library": null },
  "search": "cardiac",
  "filters": [ { "key": "dataType", "values": ["WGS"] } ],
  "facets": [
    { "key": "modelFormat", "size": 10000 },
    { "key": "modelLicense", "size": 10000 },
    { "key": "modelCloud", "size": 10000 },
    { "key": "modelTags", "size": 10000 }
  ],
  "page": { "offset": 0, "limit": 0 }
}
```

```json
{
  "view": "assets",
  "total": 9,
  "page": { "offset": 0, "limit": 0 },
  "results": [],
  "facets": {
    "modelFormat": [ { "value": "ONNX", "count": 5 }, { "value": "PyTorch", "count": 4 } ],
    "modelLicense": [ { "value": "MIT", "count": 6 }, { "value": "Apache-2.0", "count": 3 } ],
    "modelCloud": [ { "value": "GCP", "count": 8 }, { "value": "AWS", "count": 2 } ],
    "modelTags": [ { "value": "cardiology", "count": 7 } ]
  }
}
```

A model with the clouds `["GCP", "AWS"]` counts one time in each of the two buckets. duos-ui shows
the values and can now show the counts.

### 1.3 Publications tab, a type with position keys

A date range, a "one of" row filter, and a sort on the published date.

```json
{
  "view": "assets",
  "assetType": "publications",
  "scope": { "library": null },
  "filters": [
    { "key": "publicationJournal", "values": ["Nature Genetics"] },
    { "key": "publicationPublishedDate", "gte": "2020-01-01", "lte": "2024-12-31" }
  ],
  "sort": { "key": "publishedDate", "order": "desc" },
  "page": { "offset": 0, "limit": 25 }
}
```

```json
{
  "view": "assets",
  "total": 14,
  "page": { "offset": 0, "limit": 25 },
  "results": [
    {
      "studyId": 7,
      "studyName": "Heart Study",
      "assetKey": "pos:1",
      "publicationId": "PUB-2",
      "title": "Variants in cardiac genes",
      "pubmedId": "12345678",
      "publishedDate": "2023-05-02",
      "authors": [ { "name": "Example Author", "orcId": "0000-0000-0000-0000" } ],
      "citation": true,
      "datasetCitation": "DUOS-000101",
      "journal": "Nature Genetics",
      "doi": "10.0000/example",
      "url": "https://example.org/pub",
      "access": "Open",
      "tags": ["cardiology"]
    },
    {
      "studyId": 9,
      "studyName": "Lung Study",
      "assetKey": "pos:0",
      "title": "Airway sequencing",
      "publishedDate": "2021-11-30",
      "authors": [ { "name": "Another Author" } ],
      "journal": "Nature Genetics"
    }
  ]
}
```

- `pos:1` is the second entry of the publications of study 7. The row also has `publicationId`
  when the entry has one; the key does not use it.
- "Nature genetics" would not match: "one of" compares with regard to case.
- A publication without a `publishedDate` is not a row of this result.
- The sort covers every row of the result, not only the page (a change from today).

### 1.4 Clinical Trials tab, a range on two fields

```json
{
  "view": "assets",
  "assetType": "clinical_trials",
  "scope": { "library": null },
  "filters": [
    { "key": "clinicalTrialStatus", "values": ["Recruiting", "Completed"] },
    { "key": "clinicalTrialDates", "gte": "2019-01-01", "lte": "2025-12-31" }
  ],
  "page": { "offset": 0, "limit": 25 }
}
```

The response has the shape of example 1.3, with the clinical trial fields (registry section 2). A
trial matches if its status is one of the two values (the stored values of `ClinicalTrial.yaml`; "one
of" compares with regard to case), it starts on or after 2019-01-01, and it ends on or before
2025-12-31. Without a sort, the rows are in `studyId`, then `assetKey` order.

### 1.5 Biospecimens tab

```json
{
  "view": "assets",
  "assetType": "biospecimens",
  "scope": { "library": null },
  "filters": [
    { "key": "biospecimenType", "values": ["BLOOD"] },
    { "key": "biospecimenPostMortemIntervalUnit", "values": ["HOURS"] },
    { "key": "biospecimenPostMortemInterval", "lte": 24 },
    { "key": "participantCount", "gte": 100 }
  ],
  "sort": { "key": "dateOfCollection", "order": "asc" },
  "page": { "offset": 0, "limit": 25 }
}
```

```json
{
  "view": "assets",
  "total": 120,
  "page": { "offset": 0, "limit": 25 },
  "results": [
    {
      "studyId": 11,
      "studyName": "Tissue Study",
      "assetKey": "id:301",
      "biospecimenId": 301,
      "sampleId": "S-0001",
      "duosIdentifier": "DUOS-B-000301",
      "donorId": "D-17",
      "specimenType": "BLOOD",
      "preservationMethod": "FRESH_FROZEN",
      "dateOfCollection": "2018-03-14",
      "postMortemInterval": { "value": 6, "unit": "HOURS" },
      "sex": "FEMALE",
      "age": 54,
      "race": "Example",
      "countryOfOrigin": "Example",
      "extractedDiagnoses": ["Example"],
      "organization": "Example Biobank"
    }
  ]
}
```

- The row returns the donor fields, as today (registry section 7).
- `participantCount` is a study filter: the study must have a dataset with at least 100
  participants.
- A biospecimen without a `postMortemInterval` is not a row of this result (registry section 4).

### 1.6 Submissions, Presentations tab

A Data Submitter lists the presentations of the studies that they own.

```json
{
  "view": "assets",
  "assetType": "presentations",
  "scope": { "ownedByMe": true },
  "filters": [ { "key": "presentationFormat", "values": ["Poster"] } ],
  "page": { "offset": 0, "limit": 25 }
}
```

The response has the shape of example 1.3, with the presentation fields. The scope adds the
ownership rule of the `studies` view and no public visibility rule.

### 1.7 A filter of another asset type

On the AI Models tab, a `publicationJournal` filter that the user set on the Publications tab
narrows the studies.

```json
{
  "view": "assets",
  "assetType": "models",
  "scope": { "library": null },
  "filters": [ { "key": "publicationJournal", "values": ["Nature Genetics"] } ],
  "page": { "offset": 0, "limit": 25 }
}
```

The result has every model row of each study with a publication in that journal. The filter does
not apply to the model rows.

### 1.8 A page after row 10,000

```json
{
  "view": "assets",
  "assetType": "biospecimens",
  "scope": { "library": null },
  "page": { "offset": 10000, "limit": 100 }
}
```

The response has rows 10,001 to 10,100 and the full `total`. Today the tabs see the assets of 10,000
studies at most.

## 2. Errors

Each error has the consent error body (contract section 9).

| Request | Status | `message` (illustrative) |
| --- | --- | --- |
| `"view": "assets"` without `assetType` | 400 | `Missing field: assetType` |
| `"assetType": "datasets"` | 400 | `Unknown asset type: datasets` |
| `"view": "studies"` with `assetType` | 400 | `Field not accepted by view studies: assetType` |
| `"scope": { "dacIds": [3] }` with the `assets` view | 400 | `Scope not accepted by view assets: dacIds` |
| `facets: [ { "key": "publicationJournal" } ]` with `"assetType": "models"` | 400 | `Unknown facet key for models: publicationJournal` |
| `sort: { "key": "cloud" }` with `"assetType": "models"` | 400 | `Unknown sort key for models: cloud` |
| `{ "key": "biospecimenCollectionDate", "values": ["2020-01-01"] }` | 400 | `Filter needs a range: biospecimenCollectionDate` |
| `{ "key": "fundingDate", "gte": "2024-01-01", "lte": "2020-01-01" }` | 400 | `Range gte is greater than lte: fundingDate` |
| `"projection": "library"` with the `assets` view | 400 | `Unknown projection for models: library` |
