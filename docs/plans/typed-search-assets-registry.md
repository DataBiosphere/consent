# Typed Search — Asset View Registry

Ticket: [DT-3848](https://broadworkbench.atlassian.net/browse/DT-3848) (slice 3c of 6)

**Status: proposal (2026-11-25).** This document defines the `assets` view of
[`typed-search-contract.md`](typed-search-contract.md) ("the contract"): its asset types, the key
of each row, the row fields, the sort keys, the row filter keys, the facet keys, the search fields,
and the personal data in the rows. [`typed-search-examples-assets.md`](typed-search-examples-assets.md)
has a request and a response for each asset tab. "Design" refers to
[`elasticsearch-to-postgresql-plan.md`](elasticsearch-to-postgresql-plan.md).

The duos-ui sources are at duos-ui `origin/develop` (`c52f9246`). The asset definitions are in
`components/data_library/assets/` (`<type>Asset.ts`), the columns in
`components/data_library/columns/`, and the filters in `filterRegistry.ts` and
`dataLibraryFilterConfig.ts`. On the consent side, `ElasticSearchService.toStudyTerm` puts the
asset lists of a study in `study.assets` (`StudyAssets.assemble`, `StudyAssetIdentifiers.decorate`).

## 1. Asset types

`assetType` is one of the eight values below (duos-ui `AssetType`, `types/library.ts`). Another
value gets a 400.

| `assetType` | Tab | Source list | `assetKey` | Data Library | Submissions |
| --- | --- | --- | --- | --- | --- |
| `models` | AI Models | `study.assets.models` | `id:<modelId>` | Yes | Yes |
| `workspaces` | Workspaces | `study.assets.workspaces` | `id:<workspaceId>` | Yes | Yes |
| `biospecimens` | Biospecimens | `study.assets.biospecimens` | `id:<biospecimenId>` | Yes | Yes |
| `clinical_trials` | Clinical Trials | `study.assets.clinicalTrials` | `pos:<n>` | Yes | Yes |
| `publications` | Publications | `study.assets.publications` | `pos:<n>` | Yes | Yes |
| `presentations` | Presentations | `study.assets.presentations` | `pos:<n>` | Yes | Yes |
| `intellectual_properties` | Intellectual Property | `study.assets.intellectualProperties` | `pos:<n>` | Yes | Yes |
| `funding_resources` | Funding Resources | `study.assets.funding` | `pos:<n>` | Yes | No |

- **Rows.** Each entry of the source list is one row (contract section 2). For `models`,
  `workspaces`, and `biospecimens`, an entry without an integer record id (DT-4208) is not a row,
  as today (`withRowIds` in `definition.ts`). Every entry of the other types is a row.
- **`assetKey`.** `id:` and the record id, or `pos:` and the position of the entry in the source
  list, from 0 (contract section 5). The key compares as text. Some entries of the `pos:` types carry their own id
  (`clinicalTrialId`, `publicationId`, `presentationId`, `ipId`, `fundingId`), which duos-ui uses as
  the row id when it is present. The key does not use them: the field is optional, so one type would
  mix both forms. The row returns the field (section 2).
- **Scopes.** The `library` and `ownedByMe` scopes (contract section 3). An asset row is in the
  result if its study has at least one dataset that matches, by itself, the rules of the scope, the
  `search` text, and the dataset filters, as one ES document does today (`useLibraryData.ts`), and
  if the study passes the asset study filters (section 4). The rules of each scope are those of the `studies` view
  ([`typed-search-studies.md`](typed-search-studies.md)): no approved rule, as the asset tabs have
  today. A Submissions request for `funding_resources` is valid; the page does not send it.

## 2. Row fields

Each type has one projection, `row`, which is the default. Another `projection` value gets a 400.
Every row has `studyId`, `studyName` (`study.studyName`), and `assetKey`. The other fields are
those that duos-ui reads into the row today, with the names of the source entry:

| `assetType` | Fields |
| --- | --- |
| `models` | `modelId`, `duosIdentifier`, `name`, `description`, `url`, `format`, `license`, `cloud`, `trainedOnDatasets`, `maintainer` (`name`, `email`), `tags` |
| `workspaces` | `workspaceId`, `duosIdentifier`, `name`, `platform`, `url`, `description`, `tools`, `cloud`, `access`, `tags` |
| `biospecimens` | `biospecimenId`, `sampleId`, `duosIdentifier`, `donorId`, `specimenType`, `preservationMethod`, `preservationDetails`, `dateOfCollection`, `postMortemInterval` (`value`, `unit`), `sex`, `age`, `race`, `countryOfOrigin`, `extractedDiagnoses`, `pathology`, `organization`, `sourceSite`, `optionalDataUse` |
| `clinical_trials` | `clinicalTrialId`, `title`, `registry`, `identifier`, `status`, `sponsor`, `startDate`, `endDate`, `interventionType`, `description`, `phase`, `url`, `tags` |
| `publications` | `publicationId`, `title`, `pubmedId`, `publishedDate`, `authors` (`name`, `orcId`), `bibliographicCitation`, `datasetCitation`, `citation`, `journal`, `doi`, `url`, `access`, `tags` |
| `presentations` | `presentationId`, `title`, `date`, `url`, `authors`, `datasetCitation`, `citation`, `presenter` (`name`, `email`), `event`, `location`, `format`, `access`, `tags` |
| `intellectual_properties` | `ipId`, `type`, `title`, `assignee`, `patentNumber`, `filingDate`, `status`, `url`, `contact`, `tags` |
| `funding_resources` | `fundingId`, `funderName`, `funderProgram`, `grantNumber`, `projectTitle`, `startDate`, `endDate`, `url`, `tags` |

- **Values.** The server returns the stored value. A field without a value is absent from the row.
  The browser changes some values today, and keeps doing so after it reads the response: it trims
  text, wraps one text value of a list field (`cloud`, `tools`, `tags`, `trainedOnDatasets`) in a
  list (`toStringArray`), adds `authorNames` from `authors[].name`, reads a missing `citation` as
  `false`, and fills a missing biospecimen `age` with 0 and a missing `postMortemInterval` with
  `{ "value": 0, "unit": "HOURS" }`. The filters (section 4) do not use these fills.
- **Dates** are stored as text. The schemas do not fix a format; the registration form writes
  `YYYY-MM-DD`, and the examples use that form. A date filter value must be `YYYY-MM-DD` (400
  otherwise). A stored value that is not a valid `YYYY-MM-DD` date matches no date range and sorts
  last; the server does not fail on it (design section 4.2, "safe cast").
- `duosIdentifier` is the identifier that consent adds to models, workspaces, and biospecimens
  (`DUOS-M-000045`, DT-4209).

## 3. Sort keys

The sort keys of each type are the fields of the columns that the asset table marks as sortable
today. A column with a list of values (chips) is not sortable and has no sort key.

| `assetType` | Sort keys |
| --- | --- |
| `models` | `name`, `studyName`, `duosIdentifier`, `format`, `license`, `maintainerName` |
| `workspaces` | `name`, `studyName`, `duosIdentifier`, `platform`, `description`, `access` |
| `biospecimens` | `studyName`, `duosIdentifier`, `sampleId`, `specimenType`, `donorId`, `dateOfCollection` |
| `clinical_trials` | `title`, `studyName`, `identifier`, `status`, `phase`, `interventionType`, `sponsor`, `startDate` |
| `publications` | `title`, `studyName`, `journal`, `publishedDate`, `pubmedId`, `doi`, `access`, `citation` |
| `presentations` | `title`, `studyName`, `event`, `date`, `location`, `format`, `access`, `citation` |
| `intellectual_properties` | `title`, `studyName`, `type`, `patentNumber`, `assignee`, `status`, `filingDate`, `contact` |
| `funding_resources` | `studyName`, `fundingId`, `funderName`, `funderProgram`, `grantNumber`, `projectTitle`, `startDate` |

- `maintainerName` sorts by `maintainer.name`. The Maintainer column shows that name.
- Text sorts without regard to case. Dates sort as dates. `citation` sorts `false` before `true`.
- Without `sort`, and as the tie-breaker, the order is `studyId`, then `assetKey` (contract section
  5).
- **A change from today.** The asset tables sort in the browser (`sortingMode: 'client'`), so a sort
  orders only the rows of the current page. The server sorts every row of the result before it takes
  the page.

## 4. Filter keys

A filter key of the requested type is a **row filter**: it applies to each row, so all row filters
must match the same asset (contract section 4). The dataset keys of
[`typed-search-datasets-registry.md`](typed-search-datasets-registry.md) section 1 apply to the
datasets (section 1, "Scopes"). The row filter keys of the other asset types are **study filters**:
each keeps a study with one asset of the type of the key that matches. The match of a key is the
same in both uses, except for the keys with two fields (below).

| `assetType` | Key | Row field | Match |
| --- | --- | --- | --- |
| `models` | `modelFormat` | `format` | Contains |
| | `modelLicense` | `license` | Contains |
| | `modelCloud` | `cloud` (list) | Contains |
| | `modelTags` | `tags` (list) | Contains |
| `workspaces` | `workspaceTools` | `tools` (list) | Contains |
| | `workspacePlatform` | `platform` | Contains |
| | `workspaceCloud` | `cloud` (list) | Contains |
| | `workspaceAccess` | `access` | Contains |
| `biospecimens` | `biospecimenType` | `specimenType` | One of |
| | `biospecimenPostMortemIntervalUnit` | `postMortemInterval.unit` | One of |
| | `biospecimenPostMortemInterval` | `postMortemInterval.value` | Range (number) |
| | `biospecimenCollectionDate` | `dateOfCollection` | Range (date) |
| | `biospecimenDataUse` | `optionalDataUse` | Contains |
| `clinical_trials` | `clinicalTrialStatus` | `status` | One of |
| | `clinicalTrialPhase` | `phase` | One of |
| | `clinicalTrialInterventionType` | `interventionType` | Contains |
| | `clinicalTrialRegistry` | `registry` | Contains |
| | `clinicalTrialDates` | `startDate` and `endDate` | Range (date), two fields |
| `publications` | `publicationJournal` | `journal` | One of |
| | `publicationAccess` | `access` | One of |
| | `publicationPublishedDate` | `publishedDate` | Range (date) |
| `presentations` | `presentationEvent` | `event` | One of |
| | `presentationFormat` | `format` | One of |
| | `presentationAccess` | `access` | One of |
| | `presentationDate` | `date` | Range (date) |
| `intellectual_properties` | `ipType` | `type` | One of |
| | `ipStatus` | `status` | One of |
| | `ipFiledDate` | `filingDate` | Range (date) |
| `funding_resources` | `fundingFunderName` | `funderName` | One of |
| | `fundingDate` | `startDate` and `endDate` | Range (date), two fields |

- **Contains** keeps a row whose value contains one of the filter values, without regard to case.
  For a list field, one element must contain it. **One of** keeps a row whose value is equal to one
  of the filter values, with regard to case, as the browser compares today (`includes`).
- The server trims a text value before it compares, buckets, or sorts it, as the browser trims the
  row values today. A facet value thus finds its own rows again with "one of".
- A list field (`cloud`, `tools`, `tags`, `trainedOnDatasets`) that holds one text value, as older
  registrations do (`definition.ts`), counts as a list of that one value for filters and facets.
  The row returns the stored value.
- **Range (date)** compares ISO dates. A row without a date does not match a range that has the
  bound for that date.
- **Two fields:** `gte` applies to `startDate` and `lte` to `endDate`. As a row filter, a trial
  that starts on or after `gte` and ends on or before `lte` matches. As a study filter, the two
  bounds can match two different assets of the study, as the two ES range clauses do today
  (`filterRegistry.ts`). Q12 can change this (slice 4).
- `participantCount` on the Biospecimens tab is the dataset key: a study filter, as today.
- An inverted range (`gte` after `lte`) gets a 400 (contract section 4). duos-ui treats it as no
  filter today (`isInvertedDateRange`), so it leaves the filter out of the request.

**Changes from today.** The search engine matches a study today, and the browser checks each row
again. The server uses only the match of the table, which changes these results:

- A biospecimen without a `postMortemInterval` passes a range that includes 0 today, because the
  browser fills it with 0 hours. The server does not match it.
- A biospecimen without a `postMortemInterval` passes the unit filter `HOURS` today, because of the
  same fill. The server does not match it.
- A biospecimen without a `dateOfCollection` passes a range with an upper bound only today
  (`dateOfCollection || ''`). The server does not match it.
- On the tab of its own type, a "contains" key must today pass the ES `match_phrase` (whole words)
  and the browser check (part of a word). `modelLicense` "MI" thus returns no row today for a study
  whose only license is "MIT". The server uses only "contains", so it returns that model.
- As a study filter (a key of another type), a key has no row check today: the study matches by the
  search clause alone. That clause is `match_phrase` (words, without regard to case) for the model,
  workspace, clinical trial, and biospecimen keys, and `term` on the `.keyword` field for the other
  keys. The server uses the match of the table, so a study filter can keep other studies than
  today: `modelLicense` "MI" keeps a study with an "MIT" license, and `clinicalTrialStatus`
  "recruiting" does not keep a study whose trial has "Recruiting".

## 5. Facet keys

The facets of a type are its option lists (contract section 2, "The option corpus"). duos-ui builds
these lists from the rows today (`useLibraryPageState.ts`). Each facet counts asset rows. A value is
trimmed, and a blank value is in no bucket.

| `assetType` | Facet keys (the row field) |
| --- | --- |
| `models` | `modelFormat` (`format`), `modelLicense` (`license`), `modelCloud` (`cloud`), `modelTags` (`tags`) |
| `workspaces` | `workspaceTools` (`tools`), `workspacePlatform` (`platform`), `workspaceCloud` (`cloud`), `workspaceAccess` (`access`) |
| `biospecimens` | `biospecimenDataUse` (`optionalDataUse`) |
| `clinical_trials` | `clinicalTrialRegistry` (`registry`) |
| `publications` | `publicationJournal` (`journal`), `publicationAccess` (`access`) |
| `presentations` | `presentationEvent` (`event`), `presentationFormat` (`format`), `presentationAccess` (`access`) |
| `intellectual_properties` | `ipType` (`type`), `ipStatus` (`status`) |
| `funding_resources` | `fundingFunderName` (`funderName`) |

- A facet key of another type, or a key of this type that the table does not name, gets a 400.
- The other row filter keys keep their fixed option lists in duos-ui: `clinicalTrialStatus`,
  `clinicalTrialPhase`, `clinicalTrialInterventionType`, `biospecimenType`, and
  `biospecimenPostMortemIntervalUnit`.
- The option request leaves out the filters on the facet keys of the tab and keeps every other
  filter, the row filters with fixed options included, as `CORPUS_DERIVED_OPTION_KEYS` does today.
- duos-ui shows every option today. It sends `size` 10,000, the limit of contract section 6, to get
  every value.

## 6. Search fields

The `search` text matches the search fields of the library: the union of the search fields of
every asset type (`ALL_SEARCH_FIELDS`, `assets/index.ts`), for each tab, as today. A match on any of
these fields keeps the study, and the view returns every row of the study that passes the row
filters (contract section 2).

The union has the dataset fields of
[`typed-search-datasets-registry.md`](typed-search-datasets-registry.md) section 2, the study fields
of [`typed-search-studies.md`](typed-search-studies.md), and these asset fields:

| `assetType` | Search fields, in `study.assets.<source list>` |
| --- | --- |
| `models` | `name`, `format`, `license`, `cloud`, `tags`, `description`, `url`, `trainedOnDatasets`, `maintainer.name`, `maintainer.email`, `duosIdentifier` |
| `workspaces` | `name`, `platform`, `description`, `tools`, `cloud`, `tags`, `access`, `url`, `duosIdentifier` |
| `biospecimens` | `sampleId`, `specimenType`, `donorId`, `duosIdentifier` |
| `clinical_trials` | `title`, `sponsor`, `identifier`, `registry`, `tags`, `interventionType`, `phase`, `status` |
| `publications` | `title`, `journal`, `doi`, `pubmedId`, `authors.name`, `tags` |
| `presentations` | `title`, `event`, `location`, `authors`, `presenter.name`, `format`, `tags` |
| `intellectual_properties` | `title`, `type`, `assignee`, `patentNumber`, `status`, `contact`, `tags` |
| `funding_resources` | `fundingId`, `funderName`, `funderProgram`, `grantNumber`, `projectTitle`, `url`, `tags` |

Today the search is a `phrase_prefix` match: the last word of the text can be the start of a word.
Design section 4.2 gives the PostgreSQL match.

## 7. Personal data

Each signed-in user can read these fields today through the ES endpoints, and the `row` projections
return them, as decided for this migration (2026-11-25). Design section 3.1a does not list them yet.
DT-3844, which enforces the field rules, decides whether a projection leaves them out.

| `assetType` | Personal data in the row | Also a search field | Shown today |
| --- | --- | --- | --- |
| `models` | `maintainer.name`, `maintainer.email` | Both | Name in the column, email in its tooltip |
| `biospecimens` | `donorId`, `sex`, `age`, `race`, `countryOfOrigin`, `extractedDiagnoses` | `donorId` | `donorId` only |
| `publications` | `authors[].name`, `authors[].orcId` | `authors.name` | Names |
| `presentations` | `authors`, `presenter.name`, `presenter.email` | `authors`, `presenter.name` | `presenter.name` |
| `intellectual_properties` | `contact`, `assignee` | Both | Both |

The `workspaces`, `clinical_trials`, and `funding_resources` rows have no field that the asset
schemas (`src/main/resources/assets/schemas/`) describe as personal data.
