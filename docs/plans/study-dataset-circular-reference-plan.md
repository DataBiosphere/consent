# Study/Dataset Circular Reference Plan

## Status

Proposed. Every code reference below was verified against `consent` `develop` at `75193914` and
`duos-ui` `develop` at `b11e26ee`. The plan was reviewed independently with the Codex CLI in
read-only mode against both repositories; each of its material findings was re-checked against
the code before being folded in (see [Review findings](#review-findings)). A second review
corrected several of those findings; the corrections are recorded in the same section.

Ticket: DT-3723, split into one ticket per PR:

| PR | Ticket | Repo | Branch |
| --- | --- | --- | --- |
| 1 of 3 | DT-4225 | consent | `otchet-dt-4225-study-datasets-endpoint` |
| 2 of 3 | DT-4226 | duos-ui | `otchet-dt-4226-study-datasets-fetch` |
| 3 of 3 | DT-4227 | consent | `otchet-dt-4227-remove-study-datasets`, based on the PR 1 branch |

## Objective

Break the OpenAPI `$ref` cycle between `schemas/Dataset.yaml` (`study` -> `Study.yaml`) and
`schemas/Study.yaml` (`datasets[]` -> `Dataset.yaml`) by removing `Study.datasets` from the Java
model, the API contract, and duos-ui. `Study.datasetIds` already exists and carries the same
membership; callers that need the full dataset objects look them up by id.

Circular `$ref`s are legal in OpenAPI 3.1, but code generators, validators, and serializers handle
them poorly. `Study.datasets` is the side to cut: a study embedding full datasets that each
re-embed the study is the direction that recurses without bound.

## Current Behavior

### What the API serializes today

Only `GET /api/dataset/study/{studyId}` includes a `datasets` list, which
`DatasetService.getStudyWithDatasetsById` fills. Responses are written by `JerseyGsonProvider`
through `GsonUtil.getInstance()`, which reads fields rather than getters and does not serialize
nulls. The field stays null everywhere else, so the study PATCH, the registration PUT, the
conversion PUT, the custodians PUT, and the `study` nested inside `Dataset` responses omit the key
entirely. `Study.getDatasets()` returning `Set.of()` for a null field affects Java callers only,
never the wire. The removal therefore changes the study GET response and nothing else on the wire.

The runtime payload is already acyclic. `DatasetDAO.assembleDataset` attaches a study that carries
only `datasetIds`, so `Dataset.study` never re-embeds datasets. Only the schema is cyclic.

### Backend consumers of `Study.datasets`

| Location | Use |
| --- | --- |
| `StudyResource.getStudyById` | Returns the study with datasets populated. |
| `StudyResource.deleteStudyById` | Checks every dataset is deletable before deleting. |
| `StudyResource.getRegistrationFromStudy` | Passes the list to `DatasetRegistrationSchemaV1Builder.build(study, datasets)`. |
| `StudyResource.updateStudyByRegistration` | Passes the study to the validator below. |
| `StudyUpdateRequestValidator.validateConsentGroupNameChanges` | Compares submitted consent-group names against stored dataset names. |
| `DatasetServiceDAO.deleteStudy` | Iterates the datasets to delete each one. |
| `DatasetRegistrationService.createdDatasetsFromUpdatedStudy` | Finds datasets created by a study update, for DAC chair emails. |
| `DatasetService.getStudyWithDatasetsById` | The only place that populates the field. |

`ElasticSearchService` and `VoteService` read only `Dataset.study`. `StudyTerm` has no dataset
collection. No custom Gson or Jackson adapter depends on the field, and nothing deserializes JSON
directly into `Study` (registration updates deserialize into `StudyUpdateRequest`; conversion uses
`StudyConversion`). `DatasetRegistrationSchemaV1Builder`, `ConsentGroupFromDataset`, and
`SchemaFromStudy` already take datasets as an explicit list or do not read them.

### duos-ui consumers

One runtime reader: `buildConsentGroupsFromStudy` in
`src/pages/data_submission/v2/v2-common-functions.tsx` reads `study.datasets` to rebuild the
consent groups on the data-submission edit form, called from
`DataSubmissionFormV2.onLoadFormData`. The error-reload path in `onUpdateStudyError` goes through
the same loader.

Two type declarations: `Study` in `src/types/model.ts` declares `datasets: Dataset[]` as
**required**, and `Dataset.study` points at that type; `Study` in
`src/pages/data_submission/v2/v2-models.tsx` declares it optional.

The study details page reads its datasets from Elasticsearch through `useStudyDetailsData` and is
unaffected. No Cypress or JSON fixtures carry `study.datasets`.

### Study-update emails have never been sent

`DatasetServiceDAO.updateStudy` returns `studyDAO.findStudyById(...)`, and `assembleStudy` fills
only `datasetIds`. So `createdDatasetsFromUpdatedStudy` always sees an empty set, and
`sendDatasetSubmittedEmails` is called with an empty list and sends nothing on a study update.
This has been true since the emails were added in #2210 (`5de8c31f`): the study reducer then also
filled only dataset ids, and with `getDatasets()` returning null the method threw after the update
had committed.

`testStudyUpdateNewDatasetEmails` in `DatasetRegistrationServiceTest` does not show otherwise. It
verifies only that `sendDatasetSubmittedEmails(any())` is called, which happens on every update
even with an empty list, so it would pass without its `getDatasets()` mock. Sending these emails
is therefore new behavior for DAC chairs, not restored behavior.

### Deletion order

`DatasetService.deleteStudy` deletes the Elasticsearch document for every dataset id **before**
calling `DatasetServiceDAO.deleteStudy`, and an index connection error stops the request with a
500 before any rows are deleted. Today the deletable guard lives in the resource, ahead of both.
Any move of the guard must keep it ahead of the index deletion. That is the only constraint: the
index-then-rows order itself does not need to change.

`DatasetServiceDAO.deleteStudy` calls `deleteDataset` once per dataset, and each call opens its
own handle and commits. A failure partway through a study therefore keeps the datasets already
deleted, while the study and the remaining datasets stay. The outer handle's rollback does not
undo the inner commits. This plan does not change that, but pins it with a test.

### Remaining schema cycles

After removing `Study -> Dataset`, the chain `DarCollection -> Dataset -> Study` is acyclic. One
unrelated cycle survives: `Dac.yaml` -> `DataAccessAgreement.yaml` -> `Dac.yaml`. It is out of
scope here and is noted so nobody expects the schema graph to be cycle-free after this work.

## Access rule that makes the batch endpoint viable

`DatasetService.readBasis` admits admins, the dataset creator, and anyone `canReadStudy` admits:
the study creator, its custodians, and everyone when the study is publicly visible. Anyone who can
open a private study on the edit form can therefore read every dataset in it, so the existing
`GET /api/dataset/batch?ids=` (which returns 404 if any requested id is filtered out) would not
fail for the edit form. A dedicated endpoint is still recommended below, but for parallel fetching,
URL length, and a single membership snapshot, not for correctness.

## Rollout

Three PRs, each gated on the previous deploy. The UI reads `study.datasets` today, so removing the
field before the UI ships would empty every consent group on the edit form.

Gates, in order:

1. PR 1 deployed to every environment.
2. PR 2 deployed and observed: the data-submission edit form loads existing studies with their
   consent groups.
3. PR 3 released.

PR 3 removes the `datasets` property from the `GET /api/dataset/study/{studyId}` response. The
breaking-change notice to API users that `CONTRIBUTING.md` describes was considered and confirmed
not required for this change.

### PR 1: consent, additive `[DT-4225][1/3]`

- `schemas/Study.yaml`: mark `datasets` `deprecated: true`; point the description at
  `datasetIds` and the new endpoint.
- `DatasetService`: add `findStudyDatasets(User, Study)` wrapping the existing
  `findDatasetsByIds(user, new ArrayList<>(study.getDatasetIds()))`, which applies
  `verifyPublicVisibilityAccess`. `findDatasetsByIds` takes a `List<Integer>` and
  `Study.getDatasetIds()` returns a `Set<Integer>`, so the ids are copied into a list, as
  `getStudyWithDatasetsById` already does. A study with no dataset ids returns an empty list
  without a query.
- Add `GET /api/dataset/study/{studyId}/datasets` on `StudyResource`, operationId
  `apiDatasetStudyStudyIdDatasetsGet`, new file under `assets/paths/`. Load the study with
  `DatasetService.findStudyByIdForRead`, which reads through `StudyDAO.findStudyById` and so fills
  `datasetIds`, and returns 404 for a study the caller cannot read. Do not use
  `DatasetService.requireReadableStudy`: it loads with `findStudyDetailsById`, which leaves
  `datasetIds` empty, so `findStudyDatasets` would return `[]` for a readable study. The endpoint
  must never return `[]` for a study the caller cannot see. `findStudyDatasets` documents this
  precondition.
- Alternative with no backend change: the UI calls `/api/dataset/batch` with the study's
  `datasetIds` and short-circuits on an empty list. Semantics match for the edit form (see the
  access rule above).

### PR 2: duos-ui `[DT-4226][2/3]`

- `src/libs/ajax/Study.ts`: add `getDatasets(studyId)` for the new endpoint (or reuse
  `DataSet.getDatasetsByIds`).
- `DataSubmissionFormV2.onLoadFormData`: fetch the study and its datasets in parallel and pass
  the datasets to the consent-group builder. `onUpdateStudyError` reuses the loader and inherits
  the change.
- `v2-common-functions.tsx`: `buildConsentGroupsFromStudy(study)` becomes
  `buildConsentGroupsFromDatasets(datasets)`. It never read the study itself, so it takes only
  the datasets.
- Remove `datasets` from both `Study` interfaces: the required one in `src/types/model.ts` and
  the optional one in `v2-models.tsx`. Removing the required field also fixes the nested
  `Dataset.study` mocks.
- Tests that build a `Study` with `datasets` and need updating:
  `test/pages/data_submission/v2/v2-common-functions.spec.ts`, `data-bag-handling.spec.ts`,
  `DataSubmissionFormV2.modes.spec.tsx`, `DataSubmissionFormV2.spec.tsx` (the mocked function
  signature), `test/libs/ajax/DataSet.spec.ts`, and the shared-type mocks in
  `DacProfile.spec.tsx`, `BucketUtils.spec.ts`, `VotingHistory.spec.tsx`, `DAC.spec.ts`, and
  `ProgressReportApplication.spec.tsx`, plus `DatasetUtils.spec.ts`, which the type check found. Add
  unit tests for `buildConsentGroupsFromDatasets` and for the new fetch.

### PR 3: consent, removal `[DT-4227][3/3]`

- `Study.java`: delete the `datasets` field, `addDatasets`, and `getDatasets`.
  `schemas/Study.yaml`: delete the property.
- `DatasetService`: remove `getStudyWithDatasetsById` and its three cases in
  `DatasetServiceTest`. Resource paths use `findStudy` plus `findStudyDatasets` from PR 1.
- `StudyResource`: `getStudyById` returns the study alone. `getRegistrationFromStudy` and
  `updateStudyByRegistration` fetch the list and pass it to
  `DatasetRegistrationSchemaV1Builder.build(study, datasets)` and to a new `List<Dataset>`
  parameter on `StudyUpdateRequestValidator` for the consent-group rename check.
- Deletion: authorize -> load **all** datasets unfiltered through
  `datasetDAO.findDatasetsByIdList(study.getDatasetIds())` -> enforce all-deletable (400) ->
  delete from Elasticsearch -> delete in the database. The guard moves out of the resource into
  `DatasetService.deleteStudy`, ahead of the index deletion. The index-then-rows order and the
  500 on an index connection error are unchanged. Loading unfiltered is not a behavior change:
  only admins and the study creator pass the delete ownership check, and both can read every
  dataset in the study. `DatasetServiceDAO.deleteStudy` takes the list. Its per-dataset commits
  are unchanged and pinned by a test; making the delete atomic is out of scope.
- Emails: `DatasetServiceDAO.updateStudy` currently returns `Study`, which is the PUT response
  body. Change it to return a record `StudyUpdateResult(Study study, List<Integer>
  insertedDatasetIds)`, collecting the ids that `executeInsertDatasetWithFiles` already returns.
  This is race-free and needs no extra study read, unlike diffing `datasetIds` before and after.
  `updateStudyFromRegistration` keeps returning the `Study` for the response and loads exactly the
  inserted datasets after commit for `sendDatasetSubmittedEmails`. Dataset removal is already
  blocked by `validateConsentGroupRemoval`, so inserts are the only new datasets.
  **New behavior (decision, approved):** DAC chairs start receiving "dataset submitted" emails
  when a study update adds consent groups. They have never received them since #2210.
- `ElasticSearchService` and `VoteService`: no change.

## Test Matrix

### consent

- `StudyResourceTest`: all eight `getDatasets`/`addDatasets` call sites (lines 224, 351, 424, 451,
  467, 500, 516, 586 at the pinned revision).
- `DatasetRegistrationServiceTest`: all nine `Study.getDatasets()` stubs, each of which stops
  compiling when the method is removed. Lines 434, 572 and 589 are the update-email block and the
  `createdDatasetsFromUpdatedStudy` cases, which are rewritten. Lines 1070, 1097, 1141, 1181, 1216
  and 1239 pair a mocked `updateStudy` return with an empty `getDatasets()`; they return a
  `StudyUpdateResult` with no inserted ids instead.
- `StudyUpdateRequestValidatorTest`: the rename-check cases around line 243 and the helper at
  line 471.
- `DatasetServiceDAOTest`: the `deleteStudy` case around line 1001 (Testcontainers; runs locally).
- `DatasetServiceTest`: `getStudyWithDatasetsById` cases and `deleteStudy` delegation around
  line 733. The delete cases cover: index entries removed before the rows, an in-use dataset
  rejected with neither the index nor the rows touched, a study with no datasets, and an index
  connection error stopping the delete before the rows.
- `DatasetResourceTest`: `createMockStudy` builds the literal Java cycle
  (`dataset.setStudy(study); study.addDatasets(...)`); replace it with a helper that returns the
  dataset carrying its study. Used at lines 952, 975, and 1493.
- `DatasetServiceDAOTest`, new: a delete that fails partway through a study. A match row on the
  second dataset blocks its delete through the `match_entity` foreign key, which the deletable
  flag does not check. The test asserts the first dataset is gone and the second dataset and the
  study remain.
- New: email cases that assert on what is sent, not only that the sender is called. An update
  with inserts emails about exactly the inserted datasets; one with none sends nothing. The
  existing per-chair cases cover the DAC side. A contract test that a serialized `Study` has no
  `datasets` key and that a dataset's nested study does not embed it again.
- OpenAPI: the Maven `validate` phase runs the `openapi-generator-maven-plugin`
  `validate-openapi-spec` execution. Optionally add a schema-graph cycle assertion that excludes
  the known `Dac`/`DataAccessAgreement` cycle.

### duos-ui

- The spec files listed under PR 2, plus unit tests for `buildConsentGroupsFromDatasets` and the
  new datasets fetch.

## Review findings

The plan was reviewed with the Codex CLI. These findings changed it and were each verified against
the code:

- ~~Every Study payload serializes `datasets: []`, not only the study GET.~~ Corrected by the
  second review: Gson reads fields and drops nulls, so only the study GET includes the key.
- The shared `Study` type in `src/types/model.ts` declares `datasets` as required.
- `DatasetService.deleteStudy` removes Elasticsearch documents before the DAO runs, so the
  deletable guard cannot simply move into the DAO.
- Collecting inserted ids from the DAO is safer than a before/after `datasetIds` diff.
- `Dac.yaml` and `DataAccessAgreement.yaml` form a cycle that survives this change.
- The batch endpoint is functionally sufficient for the edit form because study readers can read
  every dataset in the study; the dedicated endpoint is a convenience, not a correctness fix.
- ~~The email fix restores behavior an existing test already asserts.~~ Corrected by the second
  review: the test asserts only that the sender is called, and the emails have never been sent
  on update since #2210, so this is new behavior.

The second review also found:

- `docs/plans/README.md` did not list this plan.
- PR 3 had reordered deletion to rows first, then index, and logged index failures instead of
  failing. Only the guard's position mattered, so the original order is restored.
- The test matrix had no case for a delete that fails partway through a study.
- PR 1 named `requireReadableStudy` as the loader, which would have returned `[]` for a readable
  study. The plan now names `findStudyByIdForRead`.

A Copilot review of the plan PR then found:

- The `findStudyDatasets` sketch passed the `Set` from `getDatasetIds()` to a method taking a
  `List`, which does not compile. The plan now copies the ids into a list.
- The test matrix listed two of the nine `Study.getDatasets()` stubs in
  `DatasetRegistrationServiceTest`. It now lists all nine.
- Removing a response property is a breaking API change, and `CONTRIBUTING.md` describes Comms
  coordination and an api-users notice before release. This was considered and confirmed not
  required for this change, so it is not a gate.

## Definition of Done

- `Study.yaml` no longer references `Dataset.yaml`; the spec validator passes.
- `Study.java` has no dataset collection; no production or test code calls `addDatasets` or
  `Study.getDatasets`.
- The data-submission edit form rebuilds consent groups from a separate dataset fetch.
- Neither duos-ui `Study` interface declares `datasets`.
- Study deletion rejects an in-use dataset before touching the search index, and otherwise keeps
  its index-then-rows order.
- A test pins what a delete failing partway through a study leaves behind.
- A study update that inserts consent groups emails the DAC chairs, with tests.
