# Study/Dataset Circular Reference Plan

## Status

Proposed. Every code reference below was verified against `consent` `develop` at `75193914` and
`duos-ui` `develop` at `b11e26ee`. The plan was reviewed independently with the Codex CLI in
read-only mode against both repositories; each of its material findings was re-checked against
the code before being folded in (see [Review findings](#review-findings)).

Ticket: DT-3723.

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

Only `GET /api/dataset/study/{studyId}` populates a non-empty `datasets` list, through
`DatasetService.getStudyWithDatasetsById`. However, `Study.getDatasets()` returns `Set.of()` when
the field is null, so every serialized `Study` emits `datasets: []`: the study PATCH (including its
304 body), the registration PUT, the conversion PUT, the custodians PUT, and the `study` nested
inside every `Dataset` response. Removal is therefore a property removal on every Study payload,
not just the loss of populated data on one endpoint.

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

### Latent bug: study-update emails never fire

`DatasetServiceDAO.updateStudy` returns `studyDAO.findStudyById(...)`, and `assembleStudy` fills
only `datasetIds`. So `createdDatasetsFromUpdatedStudy` always sees an empty set and
`sendDatasetSubmittedEmails` never runs on a study update. `DatasetRegistrationServiceTest` (the
test around line 424) asserts that the emails go out; it passes only because it mocks
`getDatasets()`. This plan restores the intended behavior as part of rewriting the method.

### Deletion order

`DatasetService.deleteStudy` deletes the Elasticsearch document for every dataset id **before**
calling `DatasetServiceDAO.deleteStudy`. Today the deletable guard lives in the resource, ahead of
both. Any move of the guard must keep it ahead of the index deletion.

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

Gate: PR 1 deployed everywhere -> PR 2 deployed and observed -> PR 3.

**Open question (decision).** Do generated clients of the OpenAPI spec exist outside duos-ui? The
only evidence is the legacy-`operationId` note in `docs/ai/prompts/openapi-spec.md`, which keeps
old ids "for backward compatibility with generated clients". If they exist, someone must own the
deprecation window between PR 1 and PR 3 and set its length.

### PR 1: consent, additive `[DT-3723][1/3]`

- `schemas/Study.yaml`: mark `datasets` `deprecated: true`; point the description at
  `datasetIds` and the new endpoint.
- `DatasetService`: add `findStudyDatasets(User, Study)` wrapping the existing
  `findDatasetsByIds(user, study.getDatasetIds())`, which applies `verifyPublicVisibilityAccess`.
- Add `GET /api/dataset/study/{studyId}/datasets` on `StudyResource`, operationId
  `apiDatasetStudyStudyIdDatasetsGet`, new file under `assets/paths/`. It must call
  `requireReadableStudy` first and return 404 for an unreadable study; it must never return `[]`
  for a study the caller cannot see.
- Alternative with no backend change: the UI calls `/api/dataset/batch` with the study's
  `datasetIds` and short-circuits on an empty list. Semantics match for the edit form (see the
  access rule above).

### PR 2: duos-ui `[DT-3723][2/3]`

- `src/libs/ajax/Study.ts`: add `getDatasets(studyId)` for the new endpoint (or reuse
  `DataSet.getDatasetsByIds`).
- `DataSubmissionFormV2.onLoadFormData`: fetch the study and its datasets in parallel and pass
  both to `buildConsentGroupsFromStudy(study, datasets)`. `onUpdateStudyError` reuses the loader
  and inherits the change.
- `v2-common-functions.tsx`: change `buildConsentGroupsFromStudy` to take the datasets
  explicitly.
- Remove `datasets` from both `Study` interfaces: the required one in `src/types/model.ts` and
  the optional one in `v2-models.tsx`. Removing the required field also fixes the nested
  `Dataset.study` mocks.
- Tests that build a `Study` with `datasets` and need updating:
  `test/pages/data_submission/v2/v2-common-functions.spec.ts`, `data-bag-handling.spec.ts`,
  `DataSubmissionFormV2.modes.spec.tsx`, `DataSubmissionFormV2.spec.tsx` (the mocked function
  signature), `test/libs/ajax/DataSet.spec.ts`, and the shared-type mocks in
  `DacProfile.spec.tsx`, `BucketUtils.spec.ts`, `VotingHistory.spec.tsx`, `DAC.spec.ts`, and
  `ProgressReportApplication.spec.tsx`. Add a unit test for the new
  `buildConsentGroupsFromStudy` signature.

### PR 3: consent, removal `[DT-3723][3/3]`

- `Study.java`: delete the `datasets` field, `addDatasets`, and `getDatasets`.
  `schemas/Study.yaml`: delete the property.
- `DatasetService`: remove `getStudyWithDatasetsById` and its three cases in
  `DatasetServiceTest`. Resource paths use `findStudy` plus `findStudyDatasets` from PR 1.
- `StudyResource`: `getStudyById` returns the study alone. `getRegistrationFromStudy` and
  `updateStudyByRegistration` fetch the list and pass it to
  `DatasetRegistrationSchemaV1Builder.build(study, datasets)` and to a new `List<Dataset>`
  parameter on `StudyUpdateRequestValidator` for the consent-group rename check.
- Deletion, reordered: authorize -> load **all** datasets unfiltered through
  `datasetDAO.findDatasetsByIdList(study.getDatasetIds())` -> enforce all-deletable (400) ->
  delete in the database -> delete from Elasticsearch. The guard moves out of the resource into
  `DatasetService.deleteStudy`, ahead of the index deletion, and runs against the full list rather
  than the caller-visible subset. `DatasetServiceDAO.deleteStudy` takes the list (or loads it).
  If the transaction is meant to be atomic, attach the DAOs to the same handle and test rollback;
  today the on-demand DAOs run outside the manually managed handle.
- Emails: `DatasetServiceDAO.updateStudy` currently returns `Study`, which is the PUT response
  body. Change it to return a record `StudyUpdateResult(Study study, List<Integer>
  insertedDatasetIds)`, collecting the ids that `executeInsertDatasetWithFiles` already returns.
  This is race-free and needs no extra study read, unlike diffing `datasetIds` before and after.
  `updateStudyFromRegistration` keeps returning the `Study` for the response and loads exactly the
  inserted datasets after commit for `sendDatasetSubmittedEmails`. Dataset removal is already
  blocked by `validateConsentGroupRemoval`, so inserts are the only new datasets.
  **Behavior change (decision):** DAC chairs start receiving "dataset submitted" emails on study
  update, as the existing test already expects.
- `ElasticSearchService` and `VoteService`: no change.

## Test Matrix

### consent

- `StudyResourceTest`: all eight `getDatasets`/`addDatasets` call sites (lines 224, 351, 424, 451,
  467, 500, 516, 586 at the pinned revision).
- `DatasetRegistrationServiceTest`: the update-email block around line 424 and the
  `createdDatasetsFromUpdatedStudy` block around line 557.
- `StudyUpdateRequestValidatorTest`: the rename-check cases around line 243 and the helper at
  line 471.
- `DatasetServiceDAOTest`: the `deleteStudy` case around line 1001 (Testcontainers; runs locally).
- `DatasetServiceTest`: `getStudyWithDatasetsById` cases and `deleteStudy` delegation around
  line 733.
- `DatasetResourceTest`: `createMockStudy` builds the literal Java cycle
  (`dataset.setStudy(study); study.addDatasets(...)`); replace it with a helper that returns the
  study and dataset as a pair. Used at lines 952, 975, and 1493.
- New: email cases (no inserts -> no email; N inserts -> one notification per new dataset per
  chair; existing datasets only -> none; DAC with no chairs -> no failure). A delete-order case
  proving a study with an in-use dataset leaves the index untouched. A contract test that a
  serialized `Study` has no `datasets` key.
- OpenAPI: the Maven `validate` phase runs the `openapi-generator-maven-plugin`
  `validate-openapi-spec` execution. Optionally add a schema-graph cycle assertion that excludes
  the known `Dac`/`DataAccessAgreement` cycle.

### duos-ui

- The spec files listed under PR 2, plus a new unit test for `buildConsentGroupsFromStudy(study,
  datasets)`.

## Review findings

The plan was reviewed with the Codex CLI. These findings changed it and were each verified against
the code:

- Every Study payload serializes `datasets: []`, not only the study GET.
- The shared `Study` type in `src/types/model.ts` declares `datasets` as required.
- `DatasetService.deleteStudy` removes Elasticsearch documents before the DAO runs, so the
  deletable guard cannot simply move into the DAO.
- Collecting inserted ids from the DAO is safer than a before/after `datasetIds` diff.
- `Dac.yaml` and `DataAccessAgreement.yaml` form a cycle that survives this change.
- The batch endpoint is functionally sufficient for the edit form because study readers can read
  every dataset in the study; the dedicated endpoint is a convenience, not a correctness fix.
- The email fix restores behavior an existing test already asserts.

## Definition of Done

- `Study.yaml` no longer references `Dataset.yaml`; the spec validator passes.
- `Study.java` has no dataset collection; no production or test code calls `addDatasets` or
  `Study.getDatasets`.
- The data-submission edit form rebuilds consent groups from a separate dataset fetch.
- Neither duos-ui `Study` interface declares `datasets`.
- Study deletion rejects an in-use dataset before touching the search index.
- A study update that inserts consent groups emails the DAC chairs, with tests.
