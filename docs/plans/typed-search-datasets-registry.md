# Typed Search — Dataset View Registry

Ticket: [DT-3848](https://broadworkbench.atlassian.net/browse/DT-3848) (slice 2a of 6)

**Status: proposal (2026-11-25).** This document defines the `datasets` view of
[`typed-search-contract.md`](typed-search-contract.md) ("the contract"): its keys, its search
fields, the rules that the server adds for each scope, and its projections.
[`typed-search-examples-datasets.md`](typed-search-examples-datasets.md) has a request and a
response for each caller. "Design" refers to
[`elasticsearch-to-postgresql-plan.md`](elasticsearch-to-postgresql-plan.md).

The duos-ui sources are at duos-ui `origin/develop` (`7714385a`). The ES field names come from the
consent document builder (`ElasticSearchService`, `DatasetTerm`, `StudyTerm`, `DacTerm`,
`UserTerm`, `DataUseTerm`).

## 1. Keys

"Filter" gives the match of a filter on the key (contract section 4).

| Key | Holds | Filter | Facet | Sort | Today (ES) |
| --- | --- | --- | --- | --- | --- |
| `datasetName` | text | — | — | Yes | Sort on `datasetName.keyword` |
| `datasetIdentifier` | text | — | — | Yes | Sort on `datasetIdentifier.keyword` |
| `studyName` | text | — | — | Yes | Sort on `study.studyName.keyword` |
| `accessManagement` | text | One of | Yes | Yes | `term` on `accessManagement.keyword` |
| `dataUse` | list of codes | One of, without regard to case | Yes | — | `match` on `dataUse.primary.code` |
| `dataUseModifiers` | list of codes | One of, without regard to case | Yes | — | `match_phrase` on `dataUse.secondary.code` |
| `dataType` | list of text | One of, without regard to case | Yes | — | `match` on `study.dataTypes` |
| `dac` | text (DAC name) | One of, without regard to case | Yes | Yes | `match_phrase` on `dac.dacName` |
| `participantCount` | number | Range | — | Yes | `range` on `participantCount` |
| `soApprovalModel` | text | One of | — | — | `term` on `soApprovalModel.keyword` |
| `instantApproval` | boolean | One of (`true` or `false`) | — | — | `term` on `instantApprovalEligible` |

- A filter on a key that holds a list matches a dataset if one element is one of the values
  (design section 4.2). `dataUseModifiers` matches a full code: `RS-G` does not match `RS-PD`.
- `instantApproval`: a dataset with no value matches neither `true` nor `false`, as today.
- `dataUseModifiers` facets need `size` 50: the vocabulary has more than 10 codes
  (`datasetAsset.ts` `FILTER_AGGS`).
- The asset filter keys of the library (for example `publicationJournal`) are also keys of the
  `datasets` view. They narrow the studies of the datasets, as today. Slice 3 lists them.

## 2. Search fields

The `search` text matches the search fields of the library: the union of the search fields of
every asset type (`ALL_SEARCH_FIELDS`, `assets/index.ts`), as the Datasets tab and the Submissions
page do today. The dataset fields of that union are `datasetName`, `datasetIdentifier`,
`dataLocation`, `dataUse.primary.code`, `dataUse.secondary.code`, `study.description`,
`study.studyName`, `study.species`, `study.piName`, `study.dataCustodianEmail`, and
`study.dataTypes`. Slice 3 lists the fields that the study and asset types add (for example the DAC
name, the phenotype, the study tags, and the publication titles).

A match on a study or asset field returns each dataset of that study. The registry can remove a
field from the search of a scope or a role (contract section 4).

## 3. Server rules for each scope

The server adds these rules to each `datasets` query. The caller cannot remove them.

| Scope | Public visibility rule | Approved rule | Has a study | Callers |
| --- | --- | --- | --- | --- |
| `library` | Yes, for a user without a privileged role | Yes | Yes | Data Library, Datasets tab |
| `ownedByMe` | No: every role that can use the scope is exempt | No | Yes | Submissions, Datasets tab |
| `dacIds` | No | No | No | DAC Datasets, Manage DAC, DAC profile |
| `studyId` | No | No | Yes (by the scope) | Study page |
| `datasetIds`, `datasetIdentifiers` | No | No | No | DAR review, DUC addendum, dataset page |

- **Public visibility rule:** the study has `publicVisibility = true`, unless the user is a
  Chairperson, Data Submitter, Admin, or Signing Official (design section 7.4). The design gives
  the Submissions page "same as the Data Library"; the page does not send the rule today, and each
  role that can use `ownedByMe` is exempt, so the result is the same.
- **Approved rule:** a controlled dataset appears only with `dacApproval = true`
  (`APPROVED_CONTROLLED_SHOULD` in `datasetAsset.ts`).
- **Has a study:** every Data Library, Submissions, and study page query sends `exists: study`
  today (`useLibraryData.ts`, `useStudyDetailsData.ts`, design section 4.2). The v1 callers do not.
- Q6 can change the rules of the `studyId`, `datasetIds`, and `datasetIdentifiers` scopes (slice
  4).

## 4. Projections

`dataUse` is `{ "primary": [ { "code", "description" } ], "secondary": [ { "code", "description" } ] }`,
as in the ES document. duos-ui shows the description ("DS (disease)", tooltips), and DAR review
groups datasets by the full object.

The default projection of the `datasets` view depends on the scope (section 5).

| Projection | Fields | Callers |
| --- | --- | --- |
| `library` | `datasetId`, `datasetIdentifier`, `datasetName`, `study.studyId`, `study.studyName`, `accessManagement`, `dacApproval`, `requestLocation`, `soApprovalModel`, `participantCount`, `dacId`, `dac.dacName`, `dataLocation`, `url`, `instantApprovalEligible`, `dataUse` | Data Library Datasets tab, study page grid (`datasetColumns.tsx`, `DataLibrary.tsx`, `StudyDetails.tsx`) |
| `submission` | The `library` fields, and `createUserDisplayName`, `study.dataCustodianEmail`, `deletable` | Submissions, Datasets tab (`submissionColumns.tsx`) |
| `dacTable` | `datasetId`, `datasetIdentifier`, `datasetName`, `dacId`, `dac.dacName`, `dac.dacEmail`, `dacApproval`, `deletable`, `hasInstitutionCertification`, `dataUse`, `dataLocation`, `url`, `createUserDisplayName`, `submitter.displayName`, `study.studyId`, `study.studyName`, `study.phsId`, `study.description`, `study.species`, `study.piName`, `study.dataTypes`, `study.dataCustodianEmail`, `study.dataSubmitterEmail` | DAC Datasets: the grid (`datasetGridColumns.tsx`, `DACDatasetApprovalStatus.tsx`) and its search in the browser (`filterDatasetTerms` in `libs/utils.ts`) |
| `dataUse` | `datasetId`, `dacId`, `dac.dacId`, `dac.dacName`, `dataUse` | Manage DAC and DAC profile (`dacDatasetGrid.tsx`), DAR review and DUC addendum (`BucketUtils.ts`, `DatasetList.tsx`, `DucAddendum.tsx`) |
| `selection` | `datasetId`, `accessManagement`, `dacApproval`, `dacId` | Study page "select all" (`isRowSelectable` in `datasetAsset.ts`, and the DAC ids) |
| `detail` | `datasetId`, `datasetIdentifier`, `datasetName`, `accessManagement`, `requestLocation`, `dataLocation`, `url`, `participantCount`, `dacId`, `instantApprovalEligible`, `dataUse`, `study.studyName`, `study.description`, `study.phenotype`, `study.piName`, `study.dataCustodianEmail` | Dataset page (`DatasetStatistics.tsx`) |

## 5. Projections for each scope

A projection that this table does not permit for the scope, or for the role of the user, gets a 400.
This keeps names and email addresses in the responses of the pages that show them today:

- `dacTable` is for the DAC Datasets page, which only a Chairperson can open today
  (`AppRoutes.tsx`); a Member can use the DAC scope only with `dataUse`.
- `detail` is for the dataset page, which looks up one dataset; a list of two or more ids gets a
  400, so a caller cannot read these fields for many datasets in one request.

The table controls the fields that a response returns, not the fields that the `search` text
matches. The `library` search matches `study.piName` and `study.dataCustodianEmail` (section 2), so
a caller can test whether a name or an email belongs to a dataset from the results, as today. The
Studies tab shows both fields today. Slice 3 and DT-3844 decide the search fields of each scope.

| Scope | Permitted projections | Default |
| --- | --- | --- |
| `library` | `library` | `library` |
| `ownedByMe` | `library`, `submission` | `submission` |
| `dacIds` | `dacTable` (Chairperson and Admin only), `dataUse` | `dacTable` for a Chairperson or Admin, `dataUse` for a Member |
| `studyId` | `library`, `selection` | `library` |
| `datasetIds`, `datasetIdentifiers` | `dataUse`, and `detail` with a list of one id or identifier | `dataUse` |

**Personal data in each projection** (design section 3.1a):

| Projection | Personal data fields |
| --- | --- |
| `library`, `selection`, `dataUse` | None |
| `submission` | `createUserDisplayName`, `study.dataCustodianEmail` |
| `dacTable` | `createUserDisplayName`, `submitter.displayName`, `dac.dacEmail`, `study.piName`, `study.dataCustodianEmail`, `study.dataSubmitterEmail` |
| `detail` | `study.piName`, `study.dataCustodianEmail` |

Today each signed-in user can read every one of these fields through the ES endpoints. The table
above is the proposal for DT-3844, which enforces the field rules.
