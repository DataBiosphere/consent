# ElasticSearchService And duos-ui Usage Inventory

## Overview

This document inventories where `ElasticSearchService` is used in the Consent service and where
`duos-ui` depends on those ElasticSearch-backed behaviors.

Scope includes:

- Direct search/index/delete calls to `ElasticSearchService`
- Indirect indexing side effects triggered by Consent endpoint workflows
- `duos-ui` runtime callers, plus test/config/docs touchpoints

## ElasticSearchService API Surface (Consent)

Primary implementation:

- `src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java`

Key public methods used by other layers:

- `searchDatasets(String query)`
- `searchDatasetsStream(String query)`
- `indexDataset(Integer datasetId)`
- `indexDatasets(List<Integer> datasetIds)`
- `indexDatasetIds(List<Integer> datasetIds)`
- `indexStudy(Integer studyId)`
- `deleteIndex(Integer datasetId, Integer userId)`
- `synchronizeDatasetInESIndex(Dataset dataset, boolean force)`

## Consent Elements That Use ElasticSearch

### Resource Layer

#### DatasetResource

File:

- `src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java`

ElasticSearch-backed endpoints:

- `POST /api/dataset/search/index` -> `elasticSearchService.searchDatasets(query)`
- `POST /api/dataset/search/index/v2` -> `elasticSearchService.searchDatasetsStream(query)`
- `POST /api/dataset/index` -> `elasticSearchService.indexDatasetIds(datasetIds)`
- `POST /api/dataset/index/{datasetId}` -> `elasticSearchService.indexDataset(datasetId)`
- `DELETE /api/dataset/index/{datasetId}` -> `elasticSearchService.deleteIndex(datasetId, userId)`

ElasticSearch mutation side effects in non-search endpoints:

- `PATCH /api/dataset/{datasetId}` -> `elasticSearchService.synchronizeDatasetInESIndex(...)`
- `DELETE /api/dataset/{datasetId}` -> `elasticSearchService.deleteIndex(...)`

#### StudyResource

File:

- `src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java`

ElasticSearch-backed study flows:

- `PUT /api/dataset/study/{studyId}` -> `elasticSearchService.indexStudy(studyId)`
- `DELETE /api/dataset/study/{studyId}` -> per-dataset `elasticSearchService.deleteIndex(...)`

### Service Layer

#### DatasetRegistrationService

File:

- `src/main/java/org/broadinstitute/consent/http/service/DatasetRegistrationService.java`

ElasticSearch usage:

- Registration create flow -> `elasticSearchService.indexDatasets(createdDatasetIds)`
- Dataset update flow -> `elasticSearchService.synchronizeDatasetInESIndex(updatedDataset, false)`

#### DatasetService

File:

- `src/main/java/org/broadinstitute/consent/http/service/DatasetService.java`

ElasticSearch usage includes:

- Sync after dataset/data use updates (`synchronizeDatasetInESIndex`)
- Delete on dataset/study deletion (`deleteIndex`)
- Reindex operations (`indexDatasets`, `indexStudy`)

Representative call sites include dataset update/delete and study-related mutation paths.

#### DacService

File:

- `src/main/java/org/broadinstitute/consent/http/service/DacService.java`

ElasticSearch usage:

- DAC externalization flow reindexes modified datasets via
  `elasticSearchService.indexDatasets(modifiedDatasetIds)`.

### Configuration, Client Support, and Health

- `src/main/java/org/broadinstitute/consent/http/configurations/ElasticSearchConfiguration.java`
  - Defines ElasticSearch index/server/auth configuration.
- `src/main/java/org/broadinstitute/consent/http/service/ontology/ElasticSearchSupport.java`
  - Creates ES `RestClient` and helper metadata (cluster health path, JSON header).
- `src/main/java/org/broadinstitute/consent/http/health/ElasticSearchHealthCheck.java`
  - Performs cluster health checks against ElasticSearch.

## duos-ui Elements That Depend On ElasticSearch-Backed Consent Behavior

### API Wrapper

Primary client wrapper:

- `../duos-ui/src/libs/ajax/DataSet.js`

Methods tied to ElasticSearch-backed consent behavior:

- `searchDatasetIndex` -> `POST /api/dataset/search/index`
- `searchDatasetIndexV2` -> `POST /api/dataset/search/index/v2`
- `registerDataset` -> `POST /api/dataset/v3` (indirect index mutation)
- `updateDatasetV3` -> `PUT /api/dataset/v3/{datasetId}` (indirect sync)
- `updateStudy` -> `PUT /api/dataset/study/{studyId}` (indirect study reindex)
- `deleteDataset` -> `DELETE /api/dataset/{datasetId}` (indirect delete from index)

### Runtime duos-ui Call Sites

Search callers (v1/v2):

- `../duos-ui/src/hooks/useLibraryData.ts` (v2 search path)
- `../duos-ui/src/pages/DatasetSearch.jsx`
- `../duos-ui/src/components/data_search/DatasetSearchTable.jsx`
- `../duos-ui/src/pages/DACDatasets.jsx`
- `../duos-ui/src/pages/researcher_console/DatasetSubmissions.jsx`
- `../duos-ui/src/pages/DatasetStatistics.tsx`
- `../duos-ui/src/components/study_details/StudyDetails.tsx`
- `../duos-ui/src/utils/BucketUtils.ts`

Mutation callers that trigger index side effects:

- `../duos-ui/src/pages/data_submission/DataSubmissionForm.jsx` (`registerDataset`)
- `../duos-ui/src/pages/data_submission/v2/DataSubmissionFormV2.tsx`
  (`registerDataset`, `updateStudy`)
- `../duos-ui/src/components/data_update/DatasetUpdate.jsx` (`updateDatasetV3`)
- `../duos-ui/src/pages/researcher_console/DatasetSubmissionsTable.jsx` (`deleteDataset`)
- `../duos-ui/src/components/dac_dataset_table/DACDatasetApprovalStatus.jsx`
  (`deleteDataset`)

### Query/Type Dependencies

- `../duos-ui/src/types/elastic.ts` defines query and response typings used to construct
  ElasticSearch DSL payloads sent to consent search endpoints.

## Cross-Reference Matrix

| duos-ui element | Consent endpoint or service path | ElasticSearch behavior |
| --- | --- | --- |
| `DataSet.searchDatasetIndex(...)` | `POST /api/dataset/search/index` (`DatasetResource.searchDatasetIndex`) | `searchDatasets` |
| `DataSet.searchDatasetIndexV2(...)` | `POST /api/dataset/search/index/v2` (`DatasetResource.searchDatasetIndexStream`) | `searchDatasetsStream` |
| `DataSet.registerDataset(...)` | `POST /api/dataset/v3` -> `DatasetRegistrationService.createDatasetsFromRegistration` | `indexDatasets` |
| `DataSet.updateDatasetV3(...)` | `PUT /api/dataset/v3/{datasetId}` -> `DatasetRegistrationService.updateDataset` | `synchronizeDatasetInESIndex` |
| `DataSet.updateStudy(...)` | `PUT /api/dataset/study/{studyId}` (`StudyResource.updateStudyByRegistration`) | `indexStudy` |
| `DataSet.deleteDataset(...)` | `DELETE /api/dataset/{datasetId}` (`DatasetResource.delete`) | `deleteIndex` |

## Consent Endpoint Overlap Matrix (With Security)

This section documents endpoints that overlap with ElasticSearch-backed dataset information and
the security controls that govern those endpoints.

Legend:

- `Direct` overlap: endpoint directly calls `ElasticSearchService`.
- `Indirect` overlap: endpoint mutates data that is reindexed/deleted in ElasticSearch as a side
  effect.
- `None` overlap: endpoint does not read/write ElasticSearch-backed dataset index content.

### Read Overlap Endpoints

| Endpoint | Overlap | ElasticSearch linkage | Security controls | Allowed roles | Allowed action on overlapping info | Sources |
| --- | --- | --- | --- | --- | --- | --- |
| `POST /api/dataset/search/index` | Direct | `searchDatasets(query)` | `@PermitAll` + authenticated `@Auth DuosUser` | All authenticated roles | Read | **Endpoint:** [L425](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L425), [L428](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L428), [L430](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L430); **ES Linkage:** [L212](../src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java#L212) |
| `POST /api/dataset/search/index/v2` | Direct | `searchDatasetsStream(query)` | `@PermitAll` + authenticated `@Auth DuosUser` | All authenticated roles | Read | **Endpoint:** [L439](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L439), [L442](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L442), [L444](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L444); **ES Linkage:** [L230](../src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java#L230) |

### Write Overlap Endpoints

| Endpoint | Overlap | ElasticSearch linkage | Security controls | Allowed roles | Allowed action on overlapping info | Sources |
| --- | --- | --- | --- | --- | --- | --- |
| `POST /api/dataset/v3` | Indirect | `DatasetRegistrationService.createDatasetsFromRegistration` -> `indexDatasets(createdDatasetIds)` | `@RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})` | `ADMIN`, `CHAIRPERSON`, `DATASUBMITTER` | Write | Creator-only write; records must be associated with a valid DAC and Study. | **Endpoint:** [L98](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L98), [L99](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L99), [L105](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L105); **ES Linkage:** [L190](../src/main/java/org/broadinstitute/consent/http/service/DatasetRegistrationService.java#L190), [L224](../src/main/java/org/broadinstitute/consent/http/service/DatasetRegistrationService.java#L224) |
| `PUT /api/dataset/v3/{datasetId}` | Indirect | `DatasetRegistrationService.updateDataset` -> `synchronizeDatasetInESIndex(...)` | `@RolesAllowed({ADMIN, CHAIRPERSON})` | `ADMIN`, `CHAIRPERSON` | Write | ADMIN can update any dataset; CHAIRPERSON must have role in the dataset's associated DAC. | **Endpoint:** [L148](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L148), [L149](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L149), [L150](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L150); **ES Linkage:** [L268](../src/main/java/org/broadinstitute/consent/http/service/DatasetRegistrationService.java#L268), [L302](../src/main/java/org/broadinstitute/consent/http/service/DatasetRegistrationService.java#L302), [L349](../src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java#L349) |
| `PATCH /api/dataset/{datasetId}` | Indirect | `synchronizeDatasetInESIndex(patched, false)` | `@RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})` + non-admin must be creator or custodian | `ADMIN`, `CHAIRPERSON`, `DATASUBMITTER` (conditional) | Write | Non-admin must be dataset creator or data custodian (study custodian). ADMIN has unrestricted access. | **Endpoint:** [L182](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L182), [L183](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L183), [L184](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L184); **Security:** [L194](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L194), [L505](../src/main/java/org/broadinstitute/consent/http/service/DatasetService.java#L205); **ES Linkage:** [L349](../src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java#L349) |
| `DELETE /api/dataset/{datasetId}` | Indirect | `deleteIndex(datasetId, userId)` after DB delete | `@RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})` + `validateDatasetDacAccess` | `ADMIN`, `CHAIRPERSON`, `DATASUBMITTER` (conditional) | Write (delete) | ADMIN unrestricted; DATASUBMITTER must be creator; CHAIRPERSON must be chair of dataset's DAC. Dataset must not be in use (`deletable=true`). | **Endpoint:** [L359](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L359), [L360](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L360), [L361](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L361); **Security:** [L369](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L369), [L495](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L495); **ES Linkage:** [L166](../src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java#L166) |
| `PUT /api/dataset/study/{studyId}` | Indirect | `indexStudy(studyId)` | `@RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})` + `isCreatorCustodianOrAdmin` | `ADMIN`, `CHAIRPERSON`, `DATASUBMITTER` (conditional) | Write | Non-admin must be study creator, study data custodian, or ADMIN. Applies to all datasets in the study. | **Endpoint:** [L233](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L233), [L234](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L234), [L240](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L240); **Security:** [L248](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L248), [L242](../src/main/java/org/broadinstitute/consent/http/service/DatasetService.java#L242); **ES Linkage:** [L266](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L266), [L324](../src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java#L324) |
| `DELETE /api/dataset/study/{studyId}` | Indirect | per-dataset `deleteIndex(id, userId)` | `@RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})` + non-admin must be study creator | `ADMIN`, `CHAIRPERSON`, `DATASUBMITTER` (conditional) | Write (delete) | Non-admin must be study creator; all associated datasets must be deletable (not in use). | **Endpoint:** [L166](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L166), [L168](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L168), [L169](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L169); **Security:** [L179](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L179), [L180](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L180); **ES Linkage:** [L166](../src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java#L166) |
| `POST /api/dac/{dacId}/datasets/externalize` | Indirect | `DacService.convertDacDatasetsToExternal` -> `indexDatasets(modifiedDatasetIds)` | `@RolesAllowed({ADMIN})` | `ADMIN` | Write | ADMIN-only; converts specified datasets from the DAC to external datasets. Affected datasets are reindexed. | **Endpoint:** [L312](../src/main/java/org/broadinstitute/consent/http/resources/DacResource.java#L312), [L315](../src/main/java/org/broadinstitute/consent/http/resources/DacResource.java#L315), [L316](../src/main/java/org/broadinstitute/consent/http/resources/DacResource.java#L316); **ES Linkage:** [L332](../src/main/java/org/broadinstitute/consent/http/service/DacService.java#L332), [L349](../src/main/java/org/broadinstitute/consent/http/service/DacService.java#L349) |
| `POST /api/dataset/index` | Direct | `indexDatasetIds(datasetIds)` | `@RolesAllowed(ADMIN)` | `ADMIN` | Admin write | **Endpoint:** [L388](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L388), [L389](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L389), [L390](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L390); **ES Linkage:** [L416](../src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java#L416) |
| `POST /api/dataset/index/{datasetId}` | Direct | `indexDataset(datasetId)` | `@RolesAllowed(ADMIN)` | `ADMIN` | Admin write | **Endpoint:** [L401](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L401), [L402](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L402), [L403](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L403); **ES Linkage:** [L371](../src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java#L371) |
| `DELETE /api/dataset/index/{datasetId}` | Direct | `deleteIndex(datasetId, userId)` | `@RolesAllowed(ADMIN)` | `ADMIN` | Admin write (delete) | **Endpoint:** [L412](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L412), [L413](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L413), [L414](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L414); **ES Linkage:** [L166](../src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java#L166) |

## Consent API Family Coverage (Comprehensive Context)

This table shows all major Consent endpoint families and whether they overlap with
ElasticSearch-backed dataset index information.

| Endpoint family | Overlap with ElasticSearch-backed dataset info | Notes | Sources |
| --- | --- | --- | --- |
| Dataset search/index endpoints | Yes (Direct) | Search and index management endpoints call `ElasticSearchService` directly. | **Endpoint:** [L425](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L425), [L439](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L439); **ES Linkage:** [L212](../src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java#L212), [L230](../src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java#L230) |
| Dataset mutation endpoints | Yes (Indirect) | Create/update/patch/delete flows reindex or delete dataset documents in ES. | **Endpoint:** [L98](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L98), [L148](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L148), [L182](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L182), [L359](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L359); **ES Linkage:** [L224](../src/main/java/org/broadinstitute/consent/http/service/DatasetRegistrationService.java#L224), [L302](../src/main/java/org/broadinstitute/consent/http/service/DatasetRegistrationService.java#L302), [L349](../src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java#L349), [L166](../src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java#L166) |
| Study mutation endpoints | Yes (Indirect) | Study update/delete flows reindex or remove associated dataset documents. | **Endpoint:** [L233](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L233), [L166](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L166); **ES Linkage:** [L266](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L266), [L324](../src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java#L324), [L166](../src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java#L166) |
| DAC dataset externalization endpoint | Yes (Indirect) | Externalization flow reindexes modified datasets after conversion. | **Endpoint:** [L312](../src/main/java/org/broadinstitute/consent/http/resources/DacResource.java#L312), [L315](../src/main/java/org/broadinstitute/consent/http/resources/DacResource.java#L315); **ES Linkage:** [L332](../src/main/java/org/broadinstitute/consent/http/service/DacService.java#L332), [L349](../src/main/java/org/broadinstitute/consent/http/service/DacService.java#L349) |
| DAR endpoints | None | DAR data may reference datasets but does not directly read/write ES index via resource path. | **Resource:** [L68](../src/main/java/org/broadinstitute/consent/http/resources/DataAccessRequestResource.java#L68), [L69](../src/main/java/org/broadinstitute/consent/http/resources/DataAccessRequestResource.java#L69) |
| DAA endpoints | None | DAA workflows are authorization/governance records and not ES index operations. | **Resource:** [L47](../src/main/java/org/broadinstitute/consent/http/resources/DaaResource.java#L47), [L48](../src/main/java/org/broadinstitute/consent/http/resources/DaaResource.java#L48) |
| Vote endpoints | None | Voting lifecycle does not mutate/search dataset ES index directly. | **Resource:** [L31](../src/main/java/org/broadinstitute/consent/http/resources/VoteResource.java#L31), [L62](../src/main/java/org/broadinstitute/consent/http/resources/VoteResource.java#L62) |
| User endpoints | None | User profile/role operations do not directly invoke dataset ES index behavior. | **Resource:** [L59](../src/main/java/org/broadinstitute/consent/http/resources/UserResource.java#L59), [L91](../src/main/java/org/broadinstitute/consent/http/resources/UserResource.java#L91) |
| Draft endpoints | None | Draft submission state is not an ES dataset index operation. | **Resource:** [L42](../src/main/java/org/broadinstitute/consent/http/resources/DraftResource.java#L42), [L43](../src/main/java/org/broadinstitute/consent/http/resources/DraftResource.java#L43) |
| Institution endpoints | None | Institution administration does not overlap with dataset ES index behavior. | **Resource:** [L28](../src/main/java/org/broadinstitute/consent/http/resources/InstitutionResource.java#L28), [L29](../src/main/java/org/broadinstitute/consent/http/resources/InstitutionResource.java#L29) |

## Security Controls on Overlap Endpoints

Security enforcement for overlap endpoints is layered:

1. Authentication: `OAuthCustomAuthFilter` with polymorphic auth (`AuthUser`, `DuosUser`) is
  registered in `ConsentApplication`, and overlap endpoints require authenticated `@Auth` context
  ([ConsentApplication.java#L203](../src/main/java/org/broadinstitute/consent/http/ConsentApplication.java#L203), [ConsentApplication.java#L207](../src/main/java/org/broadinstitute/consent/http/ConsentApplication.java#L207), [ConsentApplication.java#L209](../src/main/java/org/broadinstitute/consent/http/ConsentApplication.java#L209)).
2. Role gate: `RolesAllowedDynamicFeature` enforces `@RolesAllowed` and `@PermitAll` declarations
  at the resource method level ([ConsentApplication.java#L219](../src/main/java/org/broadinstitute/consent/http/ConsentApplication.java#L219)).
3. Conditional authorization in code:
  - `PATCH /api/dataset/{datasetId}`: non-admins must be dataset creator or custodian.
  - `DELETE /api/dataset/{datasetId}`: `validateDatasetDacAccess` enforces creator and DAC-chair
    constraints for non-admins.
  - `PUT /api/dataset/study/{studyId}`: user must satisfy `isCreatorCustodianOrAdmin`.
  - `DELETE /api/dataset/study/{studyId}`: non-admin must be study creator; datasets must be
    deletable.
    Sources: [DatasetResource.java#L194](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L194), [DatasetResource.java#L495](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L495), [StudyResource.java#L248](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L248), [DatasetService.java#L242](../src/main/java/org/broadinstitute/consent/http/service/DatasetService.java#L242), [StudyResource.java#L179](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L179).
4. Visibility constraints on read paths outside search index endpoints use dataset/study
  public-visibility and creator/custodian/admin checks (`DatasetService.verifyPublicVisibilityAccess`)
  ([DatasetService.java#L150](../src/main/java/org/broadinstitute/consent/http/service/DatasetService.java#L150), [DatasetService.java#L176](../src/main/java/org/broadinstitute/consent/http/service/DatasetService.java#L176)).

## Role-Based Read/Write Categories for Overlapping Information

The matrix below categorizes what each role can do with information that overlaps with ES-backed
dataset index content.

| Role | Read overlapping information | Write overlapping information | Notes | Sources |
| --- | --- | --- | --- | --- |
| `ADMIN` | Yes | Yes | Unrestricted access to all overlap endpoints; can create, update, patch, delete datasets/studies; admin reindex and delete endpoints. | **Role:** [L13](../src/main/java/org/broadinstitute/consent/http/enumeration/UserRoles.java#L13); **Constraints:** None; full access; **Write:** [L389](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L389), [L402](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L402), [L413](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L413) |
| `CHAIRPERSON` | Yes | Conditional Yes | Can create/update/delete datasets and studies. Constraints: for delete, must be chairperson of the dataset/study's associated DAC. For create/update, must be associated with a DAC. | **Role:** [L11](../src/main/java/org/broadinstitute/consent/http/enumeration/UserRoles.java#L11); **Constraints:** DAC membership required; delete restricted to own DAC; **Write:** [L99](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L99), [L149](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L149), [L360](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L360), [L234](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L234), [L168](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L168) |
| `DATASUBMITTER` | Yes | Conditional Yes | Can create datasets/studies; can patch own datasets. Delete operations restricted to own creations. Study operations require creator/custodian role. | **Role:** [L16](../src/main/java/org/broadinstitute/consent/http/enumeration/UserRoles.java#L16); **Constraints:** Write requires creator ownership (delete) or no constraints (create/patch); **Write:** [L99](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L99), [L183](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L183), [L360](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L360), [L234](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L234), [L168](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L168) |
| `MEMBER` | Yes | No | Read-only access through authenticated search endpoints. No write permissions to any overlap endpoints. | **Role:** [L10](../src/main/java/org/broadinstitute/consent/http/enumeration/UserRoles.java#L10); **Constraints:** Read-only; search endpoints ([L428](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L428), [L442](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L442)) visible to all authenticated; **Read:** [L428](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L428), [L442](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L442) |
| `RESEARCHER` | Yes | No | Read-only access through authenticated search endpoints. No write permissions to any overlap endpoints. | **Role:** [L14](../src/main/java/org/broadinstitute/consent/http/enumeration/UserRoles.java#L14); **Constraints:** Read-only; search endpoints visible to all authenticated; **Read:** [L428](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L428), [L442](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L442) |
| `SIGNINGOFFICIAL` | Yes | No | Read-only access through authenticated search endpoints. No write permissions to any overlap endpoints. | **Role:** [L15](../src/main/java/org/broadinstitute/consent/http/enumeration/UserRoles.java#L15); **Constraints:** Read-only; search endpoints visible to all authenticated; **Read:** [L428](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L428), [L442](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L442) |
| `ALUMNI` | Yes | No | Read-only access through authenticated search endpoints. No write permissions to any overlap endpoints. | **Role:** [L12](../src/main/java/org/broadinstitute/consent/http/enumeration/UserRoles.java#L12); **Constraints:** Read-only; search endpoints visible to all authenticated; **Read:** [L428](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L428), [L442](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L442) |
| `ITDIRECTOR` | Yes | No | Read-only access through authenticated search endpoints. No write permissions to any overlap endpoints. | **Role:** [L17](../src/main/java/org/broadinstitute/consent/http/enumeration/UserRoles.java#L17); **Constraints:** Read-only; search endpoints visible to all authenticated; **Read:** [L428](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L428), [L442](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L442) |
| `SERVICE_ACCOUNT` | Yes (if authenticated as user context) | No | Read-only access through authenticated search endpoints if impersonating a user. No write permissions to any overlap endpoints. | **Role:** [L18](../src/main/java/org/broadinstitute/consent/http/enumeration/UserRoles.java#L18); **Constraints:** Read-only; requires user context; search endpoints require @Auth DuosUser; **Read:** [L428](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L428), [L442](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L442) |

## Additional Constraints and Rules

In addition to role-based access control, the following additional constraints apply to dataset and study
operations that overlap with ElasticSearch index content:

### Creator and Custodian Ownership

- **Dataset creator**: User who created the dataset via `POST /api/dataset/v3` or dataset registration flow.
- **Dataset custodian**: Designated data custodian(s) at the study level (stored as `dataCustodianEmail` property).
- **Study creator**: User who created the study; may differ from individual dataset creators within that study.
- **Applicability**:
  - `PATCH /api/dataset/{datasetId}`: Non-admin must be dataset creator or custodian.
  - `DELETE /api/dataset/{datasetId}`: DATASUBMITTER must be creator; CHAIRPERSON must be chair of the dataset's DAC.
  - `PUT /api/dataset/study/{studyId}`: Non-admin must be study creator or custodian.
  - `DELETE /api/dataset/study/{studyId}`: Non-admin must be study creator.
  - Sources: [DatasetService.java#L205](../src/main/java/org/broadinstitute/consent/http/service/DatasetService.java#L205), [StudyResource.java#L179](../src/main/java/org/broadinstitute/consent/http/resources/StudyResource.java#L179), [DatasetResource.java#L495](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L495).

### DAC (Data Access Committee) Membership and Association

- **DAC association**: Every dataset belongs to exactly one DAC; this is stored as `dacId` in the dataset record.
- **DAC chairperson requirement**: For operations that require `CHAIRPERSON` role, the user must be a chairperson
  of the specific DAC associated with the dataset or study being operated on.
- **DAC validation**: The `validateDatasetDacAccess` method [L495](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L495)
  enforces that non-admin CHAIRPERSON and DATASUBMITTER users have appropriate DAC roles.
- **Applicability**:
  - `PUT /api/dataset/v3/{datasetId}`: CHAIRPERSON must have role in the dataset's associated DAC.
  - `DELETE /api/dataset/{datasetId}`: CHAIRPERSON must be chair of dataset's DAC.
  - All CHAIRPERSON write operations implicitly require DAC membership ([L11](../src/main/java/org/broadinstitute/consent/http/enumeration/UserRoles.java#L11)).
  - Sources: [DacResource.java#L103](../src/main/java/org/broadinstitute/consent/http/resources/DacResource.java#L103), [DacService.java#L51](../src/main/java/org/broadinstitute/consent/http/service/DacService.java#L51).

### Dataset Deletability State

- **Deletable flag**: Datasets have a `deletable` boolean property; a dataset can only be deleted if `deletable=true`.
- **In-use protection**: A dataset is marked `deletable=false` when it is referenced by Data Access Requests (DARs)
  or other dependent objects, preventing accidental deletion of datasets in active use.
- **Applicability**:
  - `DELETE /api/dataset/{datasetId}`: Throws `BadRequestException` if dataset is not deletable.
  - `DELETE /api/dataset/study/{studyId}`: All associated datasets must be deletable.
  - Check before deletion: [DatasetResource.java#L365](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L365).
  - Sources: [DatasetResource.java#L361](../src/main/java/org/broadinstitute/consent/http/resources/DatasetResource.java#L361).

### Study-Level Visibility and Access

- **Public visibility flag**: Studies have a `publicVisibility` property that controls dataset visibility for read operations.
- **Study-based constraints**: When a dataset is associated with a study (non-null `studyId`), access may be
  gated by study-level permissions rather than dataset-level permissions alone.
- **Client-side filtering for search results**: The dataset search endpoints (`POST /api/dataset/search/index` and `/v2`) do **NOT** enforce 
  study-level visibility filtering at the server level. Instead, filtering is **delegated to the client** (duos-ui). This means:
  - ElasticSearch search results include all matching datasets regardless of `publicVisibility` status.
  - Datasets with `publicVisibility=false` are returned by the API but are filtered out by client-side logic in duos-ui.
  - **Security implication**: If client-side filtering is not present or bypassed, non-authorized users may observe non-public datasets via direct API calls.
  - Sources for client filtering: `../duos-ui/src/pages/DatasetSearch.jsx` (injects `'study.publicVisibility': true` into the query). `../duos-ui/cypress/component/DataLibrary.spec.tsx` is a Cypress component test that exercises this behavior, not a runtime source.
- **Server-side enforcement for writes and non-search reads**:
  - Study read checks on non-search endpoints use `verifyPublicVisibilityAccess` [L176](../src/main/java/org/broadinstitute/consent/http/service/DatasetService.java#L176), 
    which checks public visibility or creator/custodian status.
  - Study mutation endpoints (`PUT /api/dataset/study/{studyId}`, `DELETE /api/dataset/study/{studyId}`)
    apply study-level authorization to all contained datasets.
  - Sources: [DatasetService.java#L176](../src/main/java/org/broadinstitute/consent/http/service/DatasetService.java#L176), [DatasetService.java#L242](../src/main/java/org/broadinstitute/consent/http/service/DatasetService.java#L242).

### Institution and Library Card Rules

- **Institution enforcement**: The system enforces institution and library card rules for user registration
  via `userService.enforceInstitutionAndLibraryCardRules(email)` [AuthorizationHelper.java#L48](../src/main/java/org/broadinstitute/consent/http/authentication/AuthorizationHelper.java#L48).
- **Implication**: Users may be rejected or throttled based on institutional affiliation when accessing
  authenticated endpoints, including dataset search and write operations.
- **Applicability**: All overlap endpoints with `@Auth` authentication requirement.

### Dataset and Study Name Exposure

- **Unique naming requirement**: The system enforces uniqueness of dataset names and study names
  (see [DatasetService.java#L246](../src/main/java/org/broadinstitute/consent/http/service/DatasetService.java#L246) - `getDatasetByName`,
  and related DAO methods), which is an architectural requirement for dataset/study identification.
- **Data exposure consequence**: Because of this uniqueness requirement, dataset names and study names are
  necessarily **exposed in all API responses** that return dataset or study objects, including:
  - Search results from `/api/dataset/search/index` and `/v2` (contains `datasetName` field)
  - Dataset creation/update endpoints (POST/PUT `/api/dataset/v3`)
  - Study mutation endpoints (PUT/DELETE `/api/dataset/study/{studyId}`)
  - All indirect reindex operations
- **Indexed fields**: Both `datasetName` and `study.studyName` are indexed in ElasticSearch and are
  part of the searchable dataset index.
- **Security implication**: Dataset and study names may reveal sensitive information about data origin,
  research topic, or institutional affiliation. Names are queryable and visible to any authenticated user
  who can access the search endpoints, regardless of visibility flags.
- **Sources**: [DatasetService.java#L246](../src/main/java/org/broadinstitute/consent/http/service/DatasetService.java#L246),
  [ElasticSearchService.java#L212](../src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java#L212) (search returns documents containing dataset/study names from the index).

### Summary Table: Constraint Types by Endpoint

| Endpoint | Creator | Custodian | DAC Chair | DAC Member | Deletable | Study-Level |
| --- | --- | --- | --- | --- | --- | --- |
| `POST /api/dataset/v3` | ✓ creator | - | - | - | - | ✓ required |
| `PUT /api/dataset/v3/{id}` | - | - | ✓ required | - | - | - |
| `PATCH /api/dataset/{id}` | ✓ or custodian | ✓ or creator | - | - | - | - |
| `DELETE /api/dataset/{id}` | ✓ for DATASUBMITTER | - | ✓ for CHAIRPERSON | - | ✓ required | - |
| `PUT /api/dataset/study/{id}` | ✓ or custodian | ✓ or creator | - | - | - | ✓ applies to all |
| `DELETE /api/dataset/study/{id}` | ✓ required | - | - | - | ✓ all required | - |
| `POST /api/dac/{id}/datasets/externalize` | - | - | - | - | - | - |

## Indexed Elements/Properties Covered by the Dataset Index

The dataset index stores `DatasetTerm` documents with nested objects used by search and filtering.

### DatasetTerm fields

- `datasetId`
- `createUserId`
- `createUserDisplayName` (deprecated)
- `datasetIdentifier`
- `deletable`
- `datasetName`
- `participantCount`
- `dataUse` (`DataUseSummary`)
- `dataLocation`
- `url`
- `requestLocation`
- `dacId`
- `dacApproval`
- `accessManagement`
- `study` (`StudyTerm`)
- `submitter` (`UserTerm`)
- `updateUser` (`UserTerm`)
- `dac` (`DacTerm`)
- `hasInstitutionCertification`
- `data` (`Map<String, Object>`) for dynamic properties

### StudyTerm nested fields

- `description`
- `studyName`
- `studyId`
- `phsId`
- `phenotype`
- `species`
- `piName`
- `dataSubmitterEmail`
- `dataSubmitterId`
- `dataCustodianEmail`
- `publicVisibility`
- `throughBioId`
- `dataTypes`
- `assets` (`Map<String, Object>`; includes study asset collections)
- `data` (`Map<String, Object>`)
- `externalIdentifier`
- `externalIdentifierType`

### UserTerm and DacTerm nested fields

- `submitter` / `updateUser` (`UserTerm`): `userId`, `displayName`, `institution`
  (`InstitutionTerm`: `id`, `name`)
- `dac` (`DacTerm`): `dacId`, `dacName`, `dacEmail`

> **Access classification lives elsewhere.** Every path above is classified SEARCH-VISIBLE or
> INTERNAL in [`es-access-contract.md`](es-access-contract.md) §B, which is enumerated from the model
> classes and is the authoritative list. Adding a field to `DatasetTerm`, `StudyTerm`, `UserTerm`,
> `DacTerm`, or `InstitutionTerm` requires classifying it there in the same change — an unclassified
> field is dropped by the E-3 allowlist and omitted from the D-3 field grant.

## Test, Config, and Docs Touchpoints In duos-ui

Test files with stubs/intercepts tied to these paths:

- `../duos-ui/cypress/component/DataLibrary.spec.tsx` (intercepts `/api/dataset/search/index/v2`)
- `../duos-ui/cypress/component/DataSearch/dataset_search_table.spec.jsx`
- `../duos-ui/cypress/component/DacDatasetTable/dac_dataset_table.spec.jsx`
- `../duos-ui/cypress/component/Dataset/DatasetStatistics.spec.jsx`
- `../duos-ui/cypress/component/StudyDetails/StudyDetails.spec.tsx`
- `../duos-ui/cypress/component/utils/bucket_utils.spec.ts`
- `../duos-ui/cypress/component/DarCollectionTable/dar_dataset_table.spec.jsx`
- `../duos-ui/cypress/component/DarCollectionReview/dar_collection_review.spec.jsx`
- `../duos-ui/cypress/component/DucAddendum/DucAddendum.spec.tsx`
- `../duos-ui/test/pages/DatasetStatistics.spec.tsx`

Config and docs references:

- `../duos-ui/public/config.json` (`apiUrl` base for dataset endpoints)
- `../duos-ui/config/base_config.json` (`apiUrl` key)
- `../duos-ui/docs/data-library.md` (ElasticSearch filter behavior note)

## Scope Notes

- This is an inventory/reference document, not a migration plan.
- It includes both direct ElasticSearch calls and indirect indexing side effects that are
  observable through Consent endpoint behavior used by `duos-ui`.

## Access Controls Implementation Plan

Implement server-enforced dataset search authorization by making Elasticsearch aware of
caller-specific access policy, not just query syntax. **The path originally recommended** was native
Elasticsearch document-level security (DLS) and field-level security (FLS) with backend-generated
per-request search credentials or impersonation context, backed by explicit access metadata in indexed
dataset documents. Because the current API accepts arbitrary raw Elasticsearch DSL through a shared
service credential, the plan also included server-owned query mediation and response shaping while
preserving current endpoints. **A-2's proof of concept reversed the emphasis** — the mediation is the
enforcement and the native path is a layer over it — and left open whether that layer is built at all.
Read the two paragraphs below before the epics; the tickets themselves are written for either outcome.

**Revised after A-2's proof of concept.** Query mediation and response shaping were originally scoped
as a *fallback* for clusters without DLS/FLS. Measurement showed they are required on the native path
too: DLS does not isolate every index-wide statistic the query surface exposes, and a field outside an
FLS grant is not queryable, so the native grant must be wider than the response bundle and something
else must narrow the response. Epic E's E-0, E-1 and E-3 therefore ship in **both** configurations;
only E-2 and E-4 are fallback-specific. See the A-2 outcome.

**And that raises a question about the recommended path itself — OPEN-13.** If E-0/E-1/E-2/E-3 ship
on any path, what D-1…D-5 still uniquely buys is one thing: document filtering enforced by the
*cluster* rather than by E-2's server-injected clause, with both expressing the same predicate.
Contract §G works the comparison and recommends **deferring D-1…D-5 pending a deliberate answer**,
not cancelling them. The question to answer first is whether anything other than this endpoint will
ever query the index with the service's credentials — if so, Epic D is not redundant. Nothing below
assumes that answer: Epic D's tickets are written out in full, and the deferral is cheap to reverse.

Size key: **S** ≈ 1 day, **M** ≈ 2–3 days, **L** ≈ 4–5 days, **XL** ≈ 1 week+

### Epic Summary

| Epic | Name | Owner | Blocked by | Blocks |
| --- | --- | --- | --- | --- |
| A | Discovery & Contract | Infra + Backend | — | B, C, D, E |
| B | Index Schema & Indexing Pipeline | Backend | A-2, A-3 (not A-1) | D, E, F |
| C | Auth Context Service | Backend | A-2 (not A-1) | D, E |
| D | Native DLS/FLS Path *(**deferral proposed** — OPEN-13)* | Backend + Infra | A-1, B, C, **E-0/E-1/E-3**, **OPEN-13** | F, *only where built* |
| E | Query Mediation & Response Shaping | Backend | B, C | D, F |
| F | API Hardening | Backend | E, and D where licensed | G |
| G | Frontend Alignment | Frontend | F | — |
| H | Observability, Rollout & Docs | Backend + Infra | E, and D where licensed | — |

Epic D no longer stands alone, and Epic E is no longer conditional. The old "D **or** E" framing is
replaced throughout by "**E-0/E-1/E-3 always; D-1…D-5 additionally where licensed**" — see the A-2
outcome for the two measurements that force it.

**Epic D additionally carries an open question about whether to build it at all (OPEN-13).** It is
blocked on that answer as much as on the license, which is why the row above lists both. Read the
callout at the head of Epic D, and contract §G, before estimating or starting any D ticket. Every
other epic is unaffected either way.

---

### Epic A — Discovery & Contract

**Goal**: Establish facts about the Elasticsearch cluster's security capabilities, evaluate the
local developer configuration changes needed, and define the formal access contract that all later
epics are built on.

**Status**: A-0 closed. A-1 has local, control-cluster, and production measurements recorded in
[`es-security-capability-record.md`](es-security-capability-record.md); dev and staging remain. **A-1
no longer decides Epic D vs. E on its own** — it settles whether Epic D is *possible*, while OPEN-13
asks whether it is *wanted* given that E-0/E-1/E-2/E-3 ship regardless (contract §G). A licensed
cluster is now a necessary rather than a sufficient reason to build D. A-2 is delivered and complete as
[`es-access-contract.md`](es-access-contract.md) — every dimension and field is decided, and its
remaining OPEN items are proposed *changes* to current behavior, each with a preserve-today default,
so none of them blocks Epics B or C.

**Note on the blocking relationships**: only the *enforcement mechanism* (Epic D vs. E) is blocked on
A-1. The access contract is not, and was deliberately written to be mechanism-neutral — the rules
must be identical under native DLS/FLS and under the mediated fallback, or the fallback becomes a
hole. Epics B and C are blocked on A-2, not A-1. Nor is Epic E: it ships in every configuration, so
neither A-1's remaining measurements nor OPEN-13 gates the start of implementation work.

---

#### Ticket A-0 — Evaluate local developer Elasticsearch configuration changes

**Summary**: Determine what changes are required to the local developer Elasticsearch setup in
`config/docker-compose.yaml` to support development and testing of the security work across all
epics.

**Context**: The local developer environment (`config/docker-compose.yaml`) runs an Elasticsearch container with security explicitly disabled:
- `xpack.security.enabled=false`
- `xpack.security.transport.ssl.enabled=false`
- `discovery.type=single-node`

ES 9.x ships with X-Pack Security built in and fully supports DLS, FLS, and API keys when
security is enabled. The native DLS/FLS path (Epic D) requires security to be enabled for local
development and testing. The compatibility fallback (Epic E) operates entirely at the application
layer and requires no Elasticsearch configuration changes.

**Acceptance criteria**:
- Delta documented between local config (security disabled, single-node) and cloud
  production config (version, security settings, credential model).
- Decision made on local developer security strategy — options:
  - Option A: enable `xpack.security.enabled=true` by default in `docker-compose.yaml` for all
    developers, with a bootstrapped `ELASTIC_PASSWORD` env var and a matching `consent.yaml`
    snippet for local `authUser`/`authPassword`.
  - Option B: add a separate Docker Compose profile (e.g. `--profile security-dev`) that runs a
    security-enabled variant; the default profile stays as-is so developers not working on Epic D
    are not affected.
- For developers working only on Epic E (fallback): confirm no `docker-compose.yaml` changes are
  needed — document this explicitly.
- Developer onboarding notes updated to describe how to run the local ES with security enabled.

**Implementation notes**:
- When security is enabled in single-node mode, Elasticsearch auto-generates credentials on first
  boot unless `ELASTIC_PASSWORD` is set. Add `ELASTIC_PASSWORD: <local-dev-password>` to the
  `elastic` service environment block and add the matching `authUser: elastic` and
  `authPassword: <local-dev-password>` to `consent.yaml` (or a dev-only `consent-local.yaml`).
- Docker Compose profile example:
  ```yaml
  elastic:
    image: docker.elastic.co/elasticsearch/elasticsearch:<version>
    profiles: [default, security-dev]
    environment:
      - xpack.security.enabled=${ES_SECURITY_ENABLED:-false}
      - ELASTIC_PASSWORD=${ELASTIC_PASSWORD:-}
  ```
  Developers run `ES_SECURITY_ENABLED=true ELASTIC_PASSWORD=devpassword docker compose up` to
  enable security without changing the committed file.
- Confirm whether production/cloud uses the same ES major version (9.x) or a different one. DLS
  query syntax and API-key semantics are stable across 8.x and 9.x but differ from 7.x.
- The `xpack.security.transport.ssl.enabled=false` line is correct for single-node development
  even when `xpack.security.enabled=true` — transport SSL is only required for multi-node
  clusters.

**Dependencies**: None (can run in parallel with A-1; informs D-2 and D-5 test setup).
**Size**: S

##### A-0 Outcome

**Decision: Option B** — security is env-var gated in `config/docker-compose.yaml` rather than
unconditionally on. The recommended default *inside* that gate later moved from off to on, once the
capability endpoint made a secured local cluster useful beyond Epic D (DEVNOTES.md); since `/config/`
is not version-controlled (see the note at the end of this section), that default is per-developer
either way, and the gate is the part of the decision that carries. Findings below were verified
empirically against `elasticsearch:9.4.4` using throwaway containers
running the exact env block from `config/docker-compose.yaml`. (Originally established on 9.3.3 and
re-verified on 9.4.4 when the pin moved; every finding reproduced unchanged, including the exact
error strings. The only observed delta was the bundled Lucene version, 10.3.2 → 10.4.0.)

These probes are no longer a manual exercise: they are asserted by `ElasticSearchSecurityBaselineTest`,
`ElasticSearchBasicLicenseTest`, `ElasticSearchSecurityDisabledTest` and
`ElasticSearchDlsFlsEnforcementTest`. Requalifying a new Elasticsearch version means bumping
`ElasticSearchTestCluster.IMAGE` and running `./mvnw test -Dtest='ElasticSearch*Test'` — see
"Qualifying a new Elasticsearch version" in `src/test/java/org/broadinstitute/consent/integration/README.md`.

**Local vs. cloud delta**

| Dimension | Local (`config/docker-compose.yaml`) | Cloud / production |
| --- | --- | --- |
| Version | ES 9.4.4, `build_flavor: default`, Lucene 10.4.0 | **Unconfirmed — A-1 must establish this.** Not determinable from this repo; production config is rendered outside it. |
| Topology | `discovery.type=single-node` | Multi-node (historic dev hostnames `elasticsearch5a{1,2,3}.dsde-dev` are commented out in `config/consent.yaml`) |
| License | **basic**, self-generated (`GET /_license` → `"type": "basic"`) | Unconfirmed. Determines whether Epic D is possible at all — see below. |
| Security | `xpack.security.enabled=false` → all requests unauthenticated | Unconfirmed; `ElasticSearchConfiguration.cloudId` support implies Elastic Cloud is a supported deployment, which always has X-Pack Security |
| Transport SSL | `false` (correct for single-node) | Required for multi-node |
| HTTP protocol | plain `http`, port 9200 (`consent.yaml` has no `protocol`, so the `"http"` default applies) | Presumably `https`/443 via `protocol`/`port` config keys |
| Credential model | none by default; single shared `authUser`/`authPassword` when security is on | Single shared `authUser`/`authPassword` (`ElasticSearchSupport.createRestClient`), or `cloudId` |
| Client | `org.elasticsearch.client:elasticsearch-rest-client` 9.4.4 (pom.xml L872-876) | same |

**Blocking finding for Epic D — DLS/FLS requires a non-basic license**

The premise that "ES 9.x ships with X-Pack Security built in and fully supports DLS/FLS when
enabled" is only half right. Enabling security is necessary but not sufficient. Verified against
9.4.4:

- Basic license (the image default), security enabled: `PUT /_security/role` with a DLS `query`
  **or** an FLS `field_security` grant → HTTP 403,
  `current license is non-compliant for [field and document level security]`.
- `POST /_security/api_key` with an inline DLS/FLS role descriptor **succeeds** on basic — the
  license check is deferred. The subsequent search fails 403 with
  `indices_with_dls_or_fls: dataset`. It fails closed (no silent bypass), but a naive
  D-2 implementation would look correct at credential-creation time and only break at query time.
- After `POST /_license/start_trial?acknowledge=true` (30-day trial, Platinum-equivalent): DLS/FLS
  role creation succeeds, and a search with a DLS+FLS API key correctly returned only the
  `publicVisibility: true` document with `_source` reduced to the granted fields.
- Authentication, RBAC, and API keys themselves work on basic. Only the DLS/FLS grants do not.

Consequences:
- **A-1 must confirm the production license tier, not just the version and security state.** If
  production is basic, Epic D is not viable and Epic E becomes the only path — this is a stronger
  gate than A-1's current acceptance criteria capture.
- Local Epic D work runs on the 30-day trial. A trial can be started only once per major version per cluster
  (`GET /_license/trial_status`); after expiry the `consent_elastic` volume must be wiped to obtain another on the same major
  version, while a major-version upgrade can restore eligibility. Treat the reported trial status
  as authoritative. Document this in D-5's test setup — CI cannot rely on a long-lived trial.

**Verified compose behavior (both modes)**

The env block verified here was `xpack.security.enabled=${ES_SECURITY_ENABLED:-false}`,
`ELASTIC_PASSWORD=${ELASTIC_PASSWORD:-devpassword}`, and explicit
`xpack.security.http.ssl.enabled=false`. Both modes were exercised, so the table holds whichever way
the gate defaults — and the recommended default has since moved to `:-true` (DEVNOTES.md):

| Mode | Unauthenticated | Authenticated | HTTPS on 9200 |
| --- | --- | --- | --- |
| default (`ES_SECURITY_ENABLED` unset) | 200 | 200 (credentials tolerated) | not served |
| `ES_SECURITY_ENABLED=true` | 401 | 200 over plain `http` | not served |

`xpack.security.http.ssl.enabled=false` keeps the HTTP layer plain, so `protocol: http` in
`consent.yaml` needs no change and no TLS trust configuration is required locally.

`config/consent.yaml` now carries `authUser: elastic` / `authPassword: devpassword`
unconditionally rather than requiring a per-mode edit: the Apache HttpClient credentials provider
in `createRestClient` only sends credentials in response to a 401 challenge, which a
security-disabled cluster never issues. Verified: security-disabled ES returns 200 for a request
carrying basic-auth credentials.

**Epic E — explicitly no changes needed**

Epic E (`SearchQueryMediator`, mandatory filter injection, response field allowlist) runs entirely
in the application layer against the unauthenticated default cluster. Developers working only on
Epic E need **no** `docker-compose.yaml` change, no `ELASTIC_PASSWORD`, no trial license, and no
`consent.yaml` credentials. The same is true of Epics A, B, C, and G. This is why Option A
(security on by default for everyone) was rejected: it would impose credential setup and a
once-per-major-version trial license on the majority of the work for the benefit of one epic.

**Note: `config/` is not version-controlled**

`/config/` is git-ignored (`.gitignore` L149) and rendered by the Broad-internal
`firecloud-develop/configure.rb` (DEVNOTES.md "Render Configs"). The ticket's framing of
"committed defaults" does not apply — there is no committed compose file in this repo. The changes
above were applied to the local rendered copy, and the durable form of this work is the
`DEVNOTES.md` documentation plus, for other developers to pick it up by default, an equivalent
change to the `firecloud-develop` compose template. **Follow-up owner needed for the
`firecloud-develop` change** — it is outside this repository.

**Follow-ups this raises for other tickets**

- **A-1**: add license tier to the required written record; treat basic as a hard stop for Epic D.
- **D-2**: the ES rest client authenticates challenge-response, not preemptively. Verify that
  requests carrying an `Authorization: ApiKey` header (rather than the client-level credentials
  provider) behave as expected, and that the extra 401 round-trip per request is acceptable.
- **D-5**: integration tests need a security-enabled ES with a trial license. This is proven
  workable — see "Testcontainers harness for DLS/FLS" below. No CI workflow currently provisions
  Elasticsearch at all, but Testcontainers needs none.

##### Testcontainers harness for DLS/FLS (verified)

`org.testcontainers:elasticsearch` exists at 1.21.4, the same version as the existing
`org.testcontainers:postgresql` dependency, and `ElasticsearchContainer` can be started from a test
exactly as `ContainerTests` starts `PostgreSQLContainer`. Added to `pom.xml` (test scope), with the
harness landed as the D-5 foundation:

- `src/test/java/org/broadinstitute/consent/integration/ElasticSearchContainerTests.java` — abstract
  base class, mirroring `ContainerTests`. Starts a security-enabled single-node container in a static
  initializer, builds a client via the application's own `ElasticSearchSupport.createRestClient`,
  activates the trial license, and exposes helpers: `elasticSearchConfiguration()`, `restClient()`,
  `recreateIndex`, `indexDocument`, `createApiKey` (inline role descriptors, per D-2), and
  `searchAsApiKey`. It does not start the Dropwizard application, so it is independent of
  `ContainerTests`.
- `src/test/java/org/broadinstitute/consent/integration/ElasticSearchDlsFlsEnforcementTest.java` —
  reference usage and proof of the mechanism. 5 tests, ~13s: the application client authenticates
  against the secured cluster, the trial license is active, the privileged client sees all documents
  and fields, DLS hides the non-public document, and FLS strips ungranted fields from `_source`.

- `src/test/java/org/broadinstitute/consent/integration/ElasticSearchLeakDefensePocTest.java` — the
  end-to-end proof of concept for the access contract, with fixtures in
  `ElasticSearchAccessContractFixtures` and the enforcement modeled in
  `ElasticSearchAccessContractModel`. 254 tests, ~21s: a corpus of 26 exfiltration attempts and 7
  legitimate requests, run under four enforcement configurations. See the A-2 outcome below for what
  it measured; it is the reason D-3, E-0, E-1 and E-3 changed.

D-5 extends `ElasticSearchContainerTests` and swaps the literal role descriptor for the generated
DLS query and FLS grants from D-3. It should reuse the PoC's corpus rather than write a new one —
see D-5's revised criteria.

**Note for a version bump.** `ElasticSearchDlsFlsEnforcementTest` and the three security-baseline
classes assert that a *capability exists*; `ElasticSearchLeakDefensePocTest` pins *behaviors* of DLS
and FLS that the access contract's reasoning depends on — which aggregations DLS does and does not
isolate, and whether a field outside an FLS grant stays queryable. Those are the assertions most
likely to change silently across versions, so it must be run alongside the other four. Each carries a
failure message naming what to re-derive in `es-access-contract.md` if the answer changes.

Two defaults must be overridden, and this is the main trap:

- For image versions ≥ 8.0.0, `ElasticsearchContainer`'s constructor automatically calls
  `withPassword("changeme")` and `withCertPath("/usr/share/elasticsearch/config/certs/http_ca.crt")`
  — so out of the box the container serves **HTTPS with a self-signed CA**, and
  `getHttpHostAddress()` reflects that. `ElasticSearchSupport.createRestClient` builds its
  `RestClientBuilder` with no `SSLContext` hook, so it cannot trust that CA. The container exposes
  `caCertAsBytes()` / `createSslContextFromCa()` for tests that build their own client, but tests
  that exercise the application's client must instead pass
  `withEnv("xpack.security.http.ssl.enabled", "false")` to force plain `http`, matching the local
  compose setup. (If production turns out to be HTTPS with a private CA, `createRestClient` needs an
  `SSLContext` hook — a D-1/D-2 concern, not just a test concern.)
- `withPassword(...)` sets `ELASTIC_PASSWORD`; the trial license still has to be activated per
  container before any DLS/FLS call, because each fresh container starts on a self-generated basic
  license. That activation is an explicit step in the test that needs it, made through the admin
  endpoint (`POST /api/elasticSearch/license/trial?acknowledge=true`, via
  `ElasticSearchContainerTests.activateTrialLicense()`) rather than by the harness reaching for
  `POST /_license/start_trial` on every subclass's behalf — a trial can be started once per major version per cluster, and the
  classes that assert what a *basic* license refuses must not have it spent underneath them.

This also resolves the D-2 concern about challenge-response authentication: the shared-credential
client authenticated successfully against the secured cluster, and a per-request
`Authorization: ApiKey` header on a `Request` was honored alongside it.

---

#### Ticket A-1 — Confirm Elasticsearch cluster security capabilities

**Summary**: Determine whether the target cluster supports DLS, FLS, API keys, and `run_as`.

**Context**: `ElasticSearchSupport.createRestClient` (L17–46) authenticates with a single shared
`authUser`/`authPassword` credential from `ElasticSearchConfiguration` (L22–24). Before any
per-request credential work can begin, we need the cluster edition and security feature inventory.

**Acceptance criteria**:
- Written record of: Elasticsearch version, edition (OSS / Basic / Enterprise), X-Pack Security
  enabled, DLS/FLS availability, API-key support, and `run_as` privilege availability.
- Confirm that `org.elasticsearch.client:elasticsearch-rest-client` in the current POM is
  compatible with any required security API calls (API-key creation, `_security/api_key`).
- Decision documented: whether Epic D (native DLS/FLS) is added on top of Epic E, or Epic E ships
  alone. **Not a choice between them** — A-2's proof of concept established that E-0/E-1/E-3 are
  required either way, so what A-1 decides is Epic D's inclusion, not the enforcement strategy.

**Implementation notes**:
- Run `GET /_xpack` and `GET /_cluster/settings` against the cluster.
- `ElasticSearchConfiguration.cloudId` (L20) — if present, the cluster is Elastic Cloud, which
  always has X-Pack Security; skip edition check and go straight to feature confirmation.
- `ElasticSearchSupport` uses the low-level `RestClient`; API-key management requires calling
  `POST /_security/api_key` directly via `Request`/`Response` — confirm this is feasible.

**Dependencies**: None.
**Size**: S

---

#### Ticket A-2 — Define formal access contract

**Summary**: Enumerate all access dimensions and decide which are document-level vs. field-level.

**Context**: The current access model uses `DatasetService.verifyPublicVisibilityAccess` (L176),
`canReadStudy` (L196), and `isCreatorOrCustodian` (L209/L216) but these checks are not reflected
in the Elasticsearch index. This ticket defines the contract that shapes both the `accessPolicy`
nested object (Ticket B-1) and the auth context resolver (Ticket C-1).

**Deliverable**: [`es-access-contract.md`](es-access-contract.md) — **written**. The sections below
record what it settled and what it deliberately did not.

**Acceptance criteria** (all met except where noted):
- ✅ Completed matrix: for each dimension (`publicVisibility`, ADMIN bypass, dataset creator, study
  creator, custodian, DAC member/chair, institution allowlist, policy tags) record data source,
  enforcement level, persistent backing, **and whether the contract preserves current behavior or
  expands it** — contract §A. Fourteen dimensions; eight PRESERVE, six DEFER.
- ✅ Field-level security groupings decided — contract §B, classifying **every** indexed path from
  the model classes into SEARCH-VISIBLE or INTERNAL, with dynamic maps (`data`, `assets`) INTERNAL
  and no wildcard grants permitted — including for admins, who bypass document filtering but not
  field filtering.
- ✅ `publicVisibility` DLS semantics decided — contract Decision 1: restricted documents are
  **invisible**, not redacted. Redaction is also not expressible in native FLS; see below.
- ✅ `publicVisibility = NULL` resolved — contract §A.1. It first looked like an undecidable policy
  question because the two code paths read a null differently, but `study.public_visibility` is
  `NOT NULL` and the nulls that actually occur come from the summary query's `LEFT JOIN` — i.e. they
  are the "dataset has no study" case, which both paths already allow. Nothing was left for an owner
  to decide.

**Findings that change other tickets**:
- **A per-document `fieldAccessProfile` cannot drive native FLS.** Field grants live in the
  credential's index privileges and apply uniformly to every document a search request matches;
  nothing re-selects a grant per hit. B-2 is invalidated as written and B-1 must drop the field —
  contract Decision 2.
- **Four dimensions in B-1's `AccessPolicyTerm` grant no access today** (DAC membership/chair,
  institution, policy tags, principal allowlists). Populating them into a DLS filter would be a
  silent authorization expansion, so they are DEFERred pending OPEN-3/OPEN-5.
- **Dataset creator and study creator are distinct** (different columns, both privileged paths);
  B-1/B-3 must index both document-side IDs, and D-3/E-2 must compare the caller's one user ID
  against both.
- **Custodian matching is case-sensitive and trims only the stored side**, so `Alice@x.org` fails
  against a stored `alice@x.org` — contract §A.2, OPEN-6.

##### A-2 Outcome — proof of concept, and what measuring the design changed

The contract was written as an argument. It is now also exercised: `ElasticSearchLeakDefensePocTest`
runs a corpus of 45 exfiltration attempts and 7 legitimate requests against a real security-enabled
cluster on a trial license, under four configurations — today's endpoint, Epic D as originally
specified, Epic D with §F mediation, and Epic E. Enforcement is modeled in the test tree
(`ElasticSearchAccessContractModel`) because E-0/E-1/E-2/E-3 do not exist yet and D-3 was blocked;
everything else is real, including the API keys and their DLS/FLS role descriptors. Contract §1.1a
describes it in full.

**The design holds.** Today's endpoint leaks by every route tried; Epic D as originally specified
still leaks; the enforcement the contract now describes closes all of it, on both paths, with
identical results between them, while every legitimate request the product makes still returns data.

**Twelve statements in the contract were wrong or incomplete, and are corrected there.** Every one
of them changes a ticket in this document, so they are listed in full. Findings 1–6 came from
building and mutation-testing the proof of concept. **Findings 7–12 came from a systematic sweep** of
Elasticsearch's Query DSL and Search API references, Elastic's documented DLS/FLS limitations, and
the DLS CVE history — rather than from attacking the shapes someone happened to think of. Findings 9,
10 and 11 are not defects in any control this plan specifies; they are controls the plan was missing.
**Row 13 is not a correction at all**: it is a measured answer to the structural option OPEN-10 had
left open, and it is listed with the rest because it changes three tickets the same way they do.

| # | Finding | Tickets affected |
| --- | --- | --- |
| 1 | §1.1's headline examples do not reproduce on 9.4.4. `terms` with `min_doc_count: 0`, `cardinality`, `value_count` and `rare_terms` are all correctly isolated by DLS. **But `significant_terms` reports `bg_count` for the whole index, and `explain` reports index-wide `N`/`docFreq`** — so §1.1's conclusion stands on different evidence, and stripping `explain` is load-bearing. | E-0, E-1 |
| 2 | **OPEN-8 is resolved, restrictively.** A `term` query on a path outside the FLS grant matches **zero documents with no error**. It affects **three** paths, not the two §B.0a named — `study.publicVisibility` is in the same position, on the data library's main query path. | **D-3 (unblocked)**, E-3 |
| 3 | **A granted multi-field does not carry its `.keyword` subfield.** Sorting on `datasetName.keyword` with only `datasetName` granted returns *a* page, not the right one, silently. Contract §F.1's `.keyword` normalization rule conceals this by accepting a reference the grant will not serve. | D-3 |
| 4 | E-1's "add `fields` to the strip list" + "strip at every depth" **breaks the library search box and every highlighted column**: `fields` is a legitimate member of `multi_match`/`query_string`/`highlight`. | E-1 |
| 5 | §F.2's "filter the sort channel against the allowlist" is not implementable — sort values carry no field names. The channel must be **dropped**. | E-3 |
| 6 | Two enforcement controls could be deleted without any test noticing, found by deliberately weakening the model. Both now have assertions. One consequence is structural: §F.2's `aggregations.**` walk defends against *our own* enumeration drift (OPEN-9), not against callers. | E-3, B-6 |
| 7 | **A field-reference validator cannot be trusted over an open grammar.** Six shapes pass it untouched because their references are not where it looks: `query_string` (fielded terms in query *text*), a `terms` **lookup** (reads a field out of another document), `more_like_this` (searches by document **id**), `_script` sort, `knn`, and `pit` (replaces the index searched). The first of these is a working oracle over RESPONSE-INTERNAL values on the fallback path. E-1 needs a **closed shape allowlist**, and `pit` comes off its permitted list. | **E-1** |
| 8 | **§F.2's `buckets[*].key` row is not implementable.** A bucket key carries no field name, so E-3 cannot tell an internal value from a legitimate facet key and passes both through — measured. The check moves to **E-0**, which knows the field targets before it runs. | **E-0**, E-3 |
| 9 | **QUERYABLE implies readable.** A `range` binary search recovers a RESPONSE-INTERNAL field's exact value from `hits.total` in ten mediator-accepted requests. "QUERYABLE but RESPONSE-INTERNAL" is obscurity, not confidentiality — a decision to take rather than assume (contract §B.0b, OPEN-12), and a second argument for item 4.2. | **G-1**, 4.2 |
| 10 | **A second role descriptor on the API key unions away both DLS and FLS** — measured. D-2 must mint exactly one descriptor naming the index, and the key's owner must hold no separate privilege on it. Silent failure: every single-descriptor test still passes. | **D-2**, D-5 |
| 11 | **`copy_to` reaches around the FLS grant** and no request-side control can close it — the target is a legitimately allowlisted path. New mapping-shape constraints for B-1/B-3: no `copy_to` into a visible field, no `alias` fields, no mapping-level `runtime` fields. (Both alias directions measured **closed** on 9.5.1, and pinned so a version bump is caught.) | **B-1**, **B-3**, D-3 |
| 12 | **The request surface is larger than the request body.** `searchDatasets` sends **unmediated** caller DSL to `_validate/query` before the search runs; `searchDatasetsStream` returns Elasticsearch's body verbatim; and URL parameters (`q`, `_source_includes`, `search_type=dfs_query_then_fetch`, `scroll`, `routing`) are a channel the mediator never sees. All unstated invariants today. | **E-1 (3B.4)**, E-3 |
| 13 | **Physical index separation works as a security control and fails as a catalog** — measured by `ElasticSearchIndexSeparationTest` (contract §A.3). Term statistics stay **per index** under the default search type, so OPEN-10's residual really is closed by a split; but a byte-identical document scored **13.6× higher** in the small tier, so the merged ranking orders by tier rather than by content. Recommendation is unchanged — accept the residual, and prefer constant scoring if that ever changes — so this adds **no** implementation scope. What it adds is two version-bump probes, a migration caveat, and a *measured* consequence for `search_type`: `dfs_query_then_fetch` pools statistics across indices, which makes §F.2a's "forward no URL parameter" a control rather than hygiene. | **D-5**, B-5, **F-1**/E-4 |

**The structural consequence is the important one.** Findings 1 and 2 each independently make Epic D
depend on Epic E's components: on E-0/E-1 because DLS does not isolate index-wide statistics, and on
E-3 because the native path's FLS grant must be wider than the response bundle. Epic E is therefore
not a fallback that Epic D replaces — see the revised Epic E goal and Phase 3A/3B.

**Two further reasons the native path cannot be trusted alone, both from outside this project.**
First, several protections a DLS-based design would lean on are properties of DLS *specifically* —
Elastic documents that suggesters are ignored under DLS, that remote-call queries (`terms` lookup,
`geo_shape` indexed shapes, `percolate`) are refused, and that `multi_match` wildcard field lists are
rejected. **None of that exists on the fallback path**, which runs with privileged credentials and an
injected filter clause, so each is a hole there unless the mediator closes it. Second, document level
security has had repeated CVEs in which the native mechanism itself failed: CVE-2021-22135 (suggester
and profile API disclosing document existence under DLS/FLS), the pre-7.11.2 cross-cluster search
disclosure, and CVE-2024-12539 (a DLS bypass in 8.16.0–8.16.1). A design whose correctness depends on
DLS being bug-free has a single point of failure that has failed before — which is the same
conclusion findings 1 and 2 reach on independent grounds.

**Implementation notes**:
- `dataCustodianEmail` is parsed from the study property bag in `DatasetService.isCreatorOrCustodian`
  (L220–236), not a dedicated DB column. Contract §C records what that does and does not cost, and
  corrects A-3's "no new storage needed" framing.
- The Indexed Elements section of this document is incomplete (it omits `study.externalIdentifier`,
  `study.externalIdentifierType`, and `UserTerm.institution` sub-fields). Contract §B is enumerated
  from the model classes and supersedes it.

**Dependencies**: A-1 — **satisfied for this ticket's purposes.** A-1's outstanding dev/staging rows
choose the enforcement *mechanism* (Epic D vs. E); the contract is stated in mechanism-neutral terms
because it must be identical either way, so it was not held for them.
**Size**: S — **actual: L.** The exhaustive field classification and the behavior-preservation audit
were the bulk of it.

---

#### Ticket A-3 — Inventory storage gaps for new access dimensions

**Summary**: Identify which access dimensions lack persistent backing and decide where to store them.

**Context**: The `accessPolicy` nested object planned for `DatasetTerm` (B-1) needs a persistent
source for every field. Dimensions such as `allowedInstitutionIds`, `allowedPrincipalIds`, and
`policyTags` have no current DB-level representation.

**Acceptance criteria**:
- For each dimension not covered by existing Study/Dataset/User properties: decision on storage
  location (existing Study property bag, new DB column, new table, or external config).
- If new DB tables are required, an entity-relationship sketch and migration script outline.
- Decision on whether institution allowlists derive solely from `User.institutionId` or require a
  separate dataset-to-institution mapping table.

**Implementation notes**:
- `dacId` already exists as a column on `Dataset` — genuinely no new storage.
- `dataCustodianEmail` is **not** the same case, and the earlier "no new storage needed" framing was
  wrong. Persistent backing exists, but as an unstructured JSON array inside the `study_property`
  bag: no referential integrity (access is granted to a string, not a principal), no normalization,
  no index, and no defined behavior for malformed values. [`es-access-contract.md`](es-access-contract.md)
  §C records this in full. It does **not** block Epics B–E, because B-3 denormalizes custodian emails
  into `accessPolicy` at index time — but it does make reindex-on-custodian-change a correctness
  requirement (B-4), it leaves the non-search endpoints parsing the bag, and OPEN-6 (case
  normalization) has to be answered either way. Whether custodianship should become a first-class
  relation is this ticket's call to make.
- `allowedPrincipalIds` (explicit user allowlists) and `policyTags` (consent-code-based access
  tags) are the most likely to require new storage — but answer contract **OPEN-5** first: none of
  them corresponds to a current requirement, and if the answer is "not now," the storage question
  does not arise and B-1 should drop the fields.
- Institution allowlists cannot be derived from `User.institutionId` alone: there is **no
  dataset-to-institution mapping** of any kind today, so this dimension needs a storage decision
  before it can mean anything at all.

**Dependencies**: A-2.
**Size**: M

---

### Epic B — Index Schema & Indexing Pipeline

**Goal**: Add an `accessPolicy` nested object to every indexed dataset document and ensure all
reindex paths populate it correctly. Foundation for both Epic D (native DLS/FLS) and Epic E (query
mediation and response shaping).

**Blocked by**: A-2, A-3.
**Blocks**: D, E.

---

#### Ticket B-1 — Add `accessPolicy` nested object to `DatasetTerm`

**Summary**: Define `AccessPolicyTerm` and add it as a nested field on `DatasetTerm`.

**Context**: `DatasetTerm` is assembled in `ElasticSearchService.toDatasetTerm` (L429–515).
Currently it carries no structured access metadata; all visibility logic lives in application code.
This ticket adds the schema without populating it yet (population is B-3).

**Acceptance criteria**:
- New `AccessPolicyTerm` class. **Field set revised by the A-2 contract** — see
  [`es-access-contract.md`](es-access-contract.md) §D:
  - `publicVisibility: Boolean`
  - `hasStudy: Boolean` — carries contract §A row 5 (a dataset with no study is readable by
    everyone today). Required because the filter treats a null `publicVisibility` on a study-bearing
    document as *not* public, so "no study" cannot be expressed as an absent visibility.
  - `datasetCreatorUserId: Integer` — the dataset's creator (`dataset.create_user_id`)
  - `studyCreatorUserId: Integer` — the study's creator (`study.create_user_id`), a *different*
    privileged path; contract §A rows 6 and 7
  - `custodianEmails: List<String>` — trim surrounding whitespace on each stored value, matching
    today's `custodian.trim()` behavior, but preserve case (contract §A.2). Lowercasing or otherwise
    normalizing here would authorize case-mismatched custodians through search while the dataset
    endpoints still rejected them; OPEN-6 proposes fixing both paths together.
  - `dacId: Integer` — indexed for display/filtering parity, **not** consulted for authorization
    while contract rows 9–10 are DEFERred
  - ~~`creatorEmail`~~ — dropped; creator matching is by user ID (contract §A.2)
  - ~~`dacApproval`~~ — dropped; it is a display attribute, not authorization (contract row 11)
  - ~~`allowedInstitutionIds`, `allowedPrincipalIds`, `policyTags`~~ — **do not add** until OPEN-5
    establishes that they are requirements. None has storage or current behavior; shipping them
    unpopulated invites a later reader to treat them as enforcement.
  - ~~`fieldAccessProfile`~~ — **removed.** Native FLS cannot select a field grant per document
    (contract Decision 2).
- Class-level comment recording that **every** `accessPolicy` path is INTERNAL and must never appear
  in a field grant (contract §B.4).
- `DatasetTerm` gains an `accessPolicy: AccessPolicyTerm` field.
- Elasticsearch index mapping updated with `accessPolicy` as a `nested` (or `object`) type —
  confirm with A-1 outcome which is required for the DLS query approach.
- **Mapping-shape constraints** (A-2 finding 11, contract §B.5d). The mapping must contain:
  - **no `copy_to` targeting a RESPONSE-VISIBLE field.** `copy_to` duplicates a field's content into
    another field's index *at index time*, before any role is consulted, so granting the target
    grants the ability to search the source's values — measured open on 9.5.1. E-3 sees nothing to
    filter, because `copy_to` does not alter `_source`; the leak is an exact-value oracle, and
    A-2 finding 9 shows what a caller does with one. **No request-side control can close it** — the
    target is a legitimately allowlisted path and the caller's query is the one the search box
    issues. The mapping is the only place the control fits.
  - **no `alias` fields.** Both directions measured *closed* on 9.5.1 (FLS resolves an alias to its
    concrete field and applies the grant there, and granting an alias grants nothing), so this is a
    forward-looking constraint rather than a live hole — but Elastic documents that FLS "should not
    be set on alias fields", and an alias is a second QUERYABLE-looking name for a concrete field.
  - **no mapping-level `runtime` fields.** E-1 strips caller-supplied `runtime_mappings`; a runtime
    field defined in the *mapping* is not caller-supplied, is not stripped, and its script can read
    `params._source` wholesale.
- **The Elasticsearch metadata fields are classified explicitly, not left implicit** (contract §B.5d).
  FLS **always** permits `_id`, `_index`, `_routing`, `_type`, `_parent`, `_timestamp`, `_ttl` and
  `_size`, whatever the grant says — so no FLS grant this plan writes can restrict them, and they are
  outside §B's field tables entirely. Today nothing leaks: `_id` is the dataset ID, which is
  RESPONSE-VISIBLE anyway. **That is a fact about the current indexing scheme, not a control.** Record
  each metadata field's classification alongside the `accessPolicy` schema so the next indexing change
  is made against a stated position, and note that E-3's retained-key set — not FLS — is what decides
  which of them reach the caller.
- **Nothing sensitive is routed through `_routing`** (contract §B.5d). A custom routing key is
  readable at the hit level and unrestrictable by any grant, so routing on, say, a creator ID or a
  custodian email would publish it. The current scheme uses the default (`_id`-derived) routing;
  keep it, and treat any future custom routing key as a §B classification decision.
- Existing `DatasetTerm` serialization tests still pass.

**Implementation notes**:
- Add `AccessPolicyTerm.java` alongside `DatasetTerm.java` in the model/elasticsearch package.
- Update the index mapping JSON (or the programmatic mapping builder in `ElasticSearchService`) to
  include the `accessPolicy` block. Leave all fields null/empty for now — B-3 populates them.
- `nested` type is required if DLS queries need to target individual sub-fields of `accessPolicy`
  in isolation; `object` suffices if DLS queries treat the whole block as a flat filter context.
  Decide after A-1.

**Dependencies**: A-2, A-3.
**Size**: M

---

#### Ticket B-2 — ~~Add `fieldAccessProfile` marker to `DatasetTerm`~~ — **CANCELLED by A-2**

**Do not implement.** The mechanism this ticket specifies does not exist in Elasticsearch.

It assumed the auth context could read a profile marker off each document and request a matching
field grant. Field grants are declared in the credential's index privileges and are evaluated when
the request is authorized, then applied uniformly to every document that privilege matches; there is
no stage at which the cluster inspects a hit and re-selects a grant for it.
([Elastic: controlling access at document and field level](https://www.elastic.co/docs/deploy-manage/users-roles/cluster-or-deployment-auth/controlling-access-at-document-field-level))

The obvious replacement — a request-wide privileged bundle selected from the caller — is also
unsafe. Creator and custodian privilege is document-scoped, while the same DLS request also returns
unrelated public datasets. A privileged request-wide grant would therefore expose privileged fields
from those unrelated documents. See [`es-access-contract.md`](es-access-contract.md) Decision 2.

**Replacement work**: D-3 and E-3 apply the single SEARCH-VISIBLE allowlist to every caller,
including ADMIN. C-1 derives document-visibility context only; it derives no field bundle. No
separate ticket is needed.

**Size**: — (removed from the plan)

---

#### Ticket B-3 — Populate `accessPolicy` in `toDatasetTerm` / `toStudyTerm`

**Summary**: Update `ElasticSearchService.toDatasetTerm` (L429) and `toStudyTerm` (L245) to read
all `accessPolicy` fields from `Dataset`, `Study`, and `User` objects.

**Context**: `ElasticSearchService` already injects `datasetDAO`, `userDAO`, `dacDAO`, `studyDAO`,
and `institutionDAO` (L58–66). All data needed for `accessPolicy` is reachable — this ticket wires
the mapping.

**Acceptance criteria**:
- `accessPolicy.publicVisibility` ← `dataset.getStudy().getPublicVisibility()` (null-safe). The
  column is `NOT NULL`, so a study-bearing dataset always has a real value; the filter treats an
  unexpected null as *not* public (contract §A.1).
- `accessPolicy.hasStudy` ← `dataset.getStudyId() != null`. This is what carries contract §A row 5
  — a dataset with no study is readable by everyone today, and that must not be expressed as a null
  `publicVisibility`.
- **Mapping-shape constraints** (A-2 finding 11, contract §B.5d): the dataset index mapping must
  contain **no `copy_to` targeting a RESPONSE-VISIBLE field, no `alias` fields, and no mapping-level
  `runtime` fields**. `copy_to` duplicates a field's content into another field's index at index
  time, before any role is consulted, so granting the target grants the ability to *search* the
  source's values — measured on 9.5.1. E-3 sees nothing to filter, because `copy_to` does not alter
  `_source`; the leak is an exact-value oracle, and finding 9 shows what a caller does with one.
  **The mediator cannot close this**: the `copy_to` target is a legitimately allowlisted path and the
  caller's query is the one the search box issues. The mapping is the only place the control fits.
- **The index request carries no custom `_routing` key** (contract §B.5d). FLS always permits
  `_routing` whatever the grant says, so a routing key derived from a creator ID or custodian email
  would be readable at the hit level with no grant able to restrict it. Keep the default `_id`-derived
  routing; B-1 classifies the metadata fields, and E-3's retained-key set is what keeps them out of
  responses.
- Contract test: assert the constraints above **against the live index mapping**, not against
  `DatasetTerm` — the mapping is what Elasticsearch enforces against, and contract §B.5c already
  established that §B's field tables are not a sufficient source for anything the grant depends on.
- `accessPolicy.datasetCreatorUserId` ← `dataset.getCreateUserId()`.
- `accessPolicy.studyCreatorUserId` ← `dataset.getStudy().getCreateUserId()` — a separate privileged
  path from the dataset creator (contract §A rows 6/7), not a duplicate of it.
- `accessPolicy.custodianEmails` ← parsed from the study property bag as in
  `DatasetService.isCreatorOrCustodian` (L220–236), preserving that method's exact matching
  semantics — apply the same `trim()` on the stored side and **no** case normalization (contract
  §A.2). Malformed or non-array property values must not throw out of indexing.
- `accessPolicy.dacId` ← `dataset.getDacId()`.
- ~~`creatorEmail`, `dacApproval`, `fieldAccessProfile`, `allowedInstitutionIds`~~ — not populated;
  see the revised B-1 field set.
- **A dataset with no study must remain readable by everyone** (contract §A row 5 — that is current
  behavior). Do not default a null study to `publicVisibility=false`; that would hide datasets that
  are visible today. Represent "no study" explicitly so the DLS filter can match it.
- Unit test: null study → no NPE, and the resulting document is readable by a caller with no
  relationship to it.

**Implementation notes**:
- `isCreatorOrCustodian` in `DatasetService` (L224–238) parses custodian email from
  `study.getProperties()`. Do not call `DatasetService` from `ElasticSearchService` to avoid a
  circular dependency — extract a private helper `parseCustodianEmails(Study study)` in
  `ElasticSearchService` instead.
- Guard all `study` accesses — datasets created outside the registration flow may have a null
  study reference.

**Dependencies**: B-1. A-3 is not required for the current field set; its speculative dimensions are
deferred pending OPEN-5.
**Size**: M

---

#### Ticket B-4 — Verify all reindex trigger paths emit current `accessPolicy`

**Summary**: Audit every code path that calls `indexDatasets`, `indexDataset`, `indexStudy`, or
`synchronizeDatasetInESIndex` and confirm each passes a fully-populated `Dataset` to
`toDatasetTerm`.

**Context**: Reindex trigger paths:
- `DatasetRegistrationService.createDatasetsFromRegistration` (L224) → `indexDatasets`
- `DatasetRegistrationService.updateDataset` (L302) → `synchronizeDatasetInESIndex`
- `DatasetResource.patchDataset` → `synchronizeDatasetInESIndex`
- `StudyResource.updateStudyByRegistration` (L266) → `indexStudy`
- `DacService.convertDacDatasetsToExternal` (L332/L349) → `indexDatasets`
- Admin endpoints `POST /api/dataset/index` and `POST /api/dataset/index/{datasetId}`

**Acceptance criteria**:
- Each path verified: `toDatasetTerm` at index time has access to a dataset with `study` and
  `createUserId` populated. If not, the path is updated to load required associations first.
- `synchronizeDatasetInESIndex` (L349–359) confirmed to load the dataset fresh from DB before
  calling `toDatasetTerm`, not relying on a potentially stale in-memory object.
- Integration test: create a dataset with `publicVisibility=false`, `PATCH` it, verify the
  reindexed document's `accessPolicy.publicVisibility` is `false`.

**Implementation notes**:
- `indexStudy` (L324) iterates over all datasets in the study — confirm each fetched `Dataset`
  object includes its `Study` and properties.
- `DacService.convertDacDatasetsToExternal` calls `indexDatasets(ids)` with IDs only;
  `indexDatasets` re-fetches from DB, so it should be fine — but verify the fetch includes the
  study association.

**Dependencies**: B-3.
**Size**: M

---

#### Ticket B-5 — Design and implement index version migration strategy

**Summary**: Plan and execute the Elasticsearch index migration required to add the `accessPolicy`
`nested` field without downtime: new index, alias cutover, background reindex.

**Context**: Adding a `nested` field type requires reindexing all documents — `PUT /_mapping`
cannot add nested types to a live index. The active index is identified by
`ElasticSearchConfiguration.datasetIndexName` (L14), exposed as `indexKey` in
`ElasticSearchService` (L68).

**Acceptance criteria**:
- Migration runbook: new index name convention (e.g. `datasets-v2`), alias (`datasets`) pointing
  to the active index, cutover steps, rollback procedure (alias swap back to `v1` within minutes).
- Reindex script or admin endpoint: creates the new index with the updated mapping, runs
  `POST /_reindex`, atomically swaps the alias.
- Application reads/writes using the aliased name from config; no hard-coded index names in code.

**Implementation notes**:
- Clarify which config field (`indexName` L10 vs. `datasetIndexName` L14) `ElasticSearchService`
  uses as `indexKey` — there are two fields; only one should be active.
- Confirm that dataset documents use `datasetId` as the ES document `_id` — this makes the reindex
  idempotent.
- Decide whether the reindex script lives as a one-off admin script, a Flyway migration, or a new
  `POST /api/dataset/index/migrate` admin endpoint.
- **If the two-index split of contract §A.3 is ever adopted, this ticket grows** (A-2 finding 13). It
  is *not* in scope now — §A.3 recommends against it, and the runbook above assumes one index — but
  the split moves a document between indices whenever `publicVisibility` or the study link changes,
  which makes those updates a delete-and-index rather than an update. That is the same trigger set
  B-4 already reindexes on, so the cost is in this migration strategy rather than in the pipeline.

**Dependencies**: B-3.
**Size**: M

---

#### Ticket B-6 — Unit-test `accessPolicy` population in `ElasticSearchServiceTest`

**Summary**: Add comprehensive unit tests for `accessPolicy` field population in `toDatasetTerm`
and `toStudyTerm` before any auth enforcement code is written against them.

**Acceptance criteria**:
- `publicVisibility` flows through for `true` / `false`.
- A dataset with no study indexes `hasStudy=false` and remains readable by an unrelated caller
  (contract §A row 5) — the case a null `publicVisibility` would otherwise have to carry.
- `custodianEmails` populated when study has `dataCustodianEmail` property.
- `custodianEmails` is empty (not null) when study has no custodian property.
- `custodianEmails` preserves case: `" Alice@X.org "` in the property bag indexes as `Alice@X.org`
  (trimmed, not lowercased), so search authorizes exactly the callers `DatasetService` does today.
- Malformed `dataCustodianEmail` (not a JSON array, unparseable) does not throw out of indexing.
- Null study → no NPE, and the document remains readable by an unrelated caller (contract row 5).
- `dacId`, `datasetCreatorUserId`, `studyCreatorUserId` flow through — with a case where the dataset
  creator and study creator are **different users**, since conflating them is the likely bug.
- No `accessPolicy` field is present in the SEARCH-VISIBLE projection (contract §B.4).
- **Contract test over the live index mapping**, not the model classes (A-2 finding 11, contract
  §B.5d): assert no `copy_to` into a RESPONSE-VISIBLE field, no `alias` fields, and no mapping-level
  `runtime` fields. The mapping is what Elasticsearch enforces against, and contract §B.5c already
  established that §B's field tables are not a sufficient source for anything the grant depends on.
  This is the mapping-shape counterpart to the field-level drift test OPEN-9 asks for.

**Implementation notes**:
- Mirror the existing mock-heavy pattern in `ElasticSearchServiceTest` — mock all DAO calls.
- The mapping assertion needs the real mapping, so it belongs with the integration tests rather than
  in the mock-based class — fetch `GET /<index>/_mapping` and walk it.

**Dependencies**: B-3.
**Size**: M

---

### Epic C — Auth Context Service

**Goal**: Build a service that resolves an authenticated `DuosUser` into a structured caller
context for use by both the native DLS path and the compatibility fallback.

**Blocked by**: A-2.
**Blocks**: D, E.

---

#### Ticket C-1 — Create `DatasetSearchAuthContext` resolver

**Summary**: Implement an immutable record class and resolver service that maps a `DuosUser` to
all access dimensions needed for Elasticsearch authorization.

**Context**: Access checks are currently scattered across `DatasetService.verifyPublicVisibilityAccess`
(L176), `canReadStudy` (L196), and `isCreatorOrCustodian` (L209/L216), each evaluated against
a single dataset per call. The resolver pre-builds a caller context evaluated once per request,
not once per document.

**Acceptance criteria**:
- `DatasetSearchAuthContext` (record or immutable class) with:
  - `Integer userId` — matches **both** the dataset creator and the study creator dimensions, which
    are distinct columns and distinct privileged paths ([`es-access-contract.md`](es-access-contract.md)
    §A rows 6 and 7); the filter must test both, not one
  - `String userEmail` — passed through **unnormalized**, for the exact custodian matching the
    dataset endpoints do today (contract §A.2)
  - `boolean isAdmin`
  - **No field-bundle field.** Search serves one bundle to every caller (contract Decision 2), so
    there is nothing per-caller to derive. A per-caller `privileged` bundle was considered and
    rejected: creator/custodian privilege is document-scoped, and a caller privileged on one dataset
    also receives every public dataset through the same DLS filter, so a request-wide privileged
    grant would project privileged fields out of unrelated documents.
  - **No institution, DAC-membership/chair, principal-allowlist, or policy-tag fields.** None is
    consumed by current read authorization; resolving speculative context adds queries and invites a
    later implementation to feed it into DLS accidentally. Add a field only with the signed-off
    requirement that consumes it (contract rows 9–14, OPEN-3/OPEN-5).
- `DatasetSearchAuthContextResolver` service: accepts a `DuosUser`, returns a
  `DatasetSearchAuthContext`.
- `isAdmin` is `true` when user has `UserRoles.ADMIN` (L13 in `UserRoles.java`).
- No DAC lookup is performed; DAC membership and chair status grant no search read access today.

**Implementation notes**:
- `DuosUser.getRoles()` returns the role set; check for `UserRoles.ADMIN`.
- Keep the resolver stateless and derive the context entirely from the supplied `DuosUser`; the
  current contract requires no DAO lookup.
- Do not inject `DacDAO` or `DacService` until OPEN-3 is approved. The current resolver needs no
  DAC dependency.

**Dependencies**: A-2.
**Size**: M

---

#### Ticket C-2 — Normalize policy evaluation into shared helpers

**Summary**: Extract the shared read-access predicates from `DatasetService` into a policy
evaluator usable by both search mediation (Epic E) and native DLS role generation (Epic D).

**Context**: `DatasetService.verifyPublicVisibilityAccess` (L176), `canReadStudy` (L196), and
`isCreatorOrCustodian` (L209/L216) encode the same access logic used by non-search endpoints. The
compatibility fallback (E-2) must express equivalent logic as Elasticsearch query filters —
normalizing the Java predicates first makes it easier to write the ES query equivalent and prevents
the two code paths from diverging.

**Acceptance criteria**:
- `DatasetAccessPolicy` utility class with pure functions:
  - `canRead(DatasetSearchAuthContext ctx, AccessPolicyTerm policy)` → boolean
  - `isCreator(DatasetSearchAuthContext ctx, AccessPolicyTerm policy)` → boolean
  - `isCustodian(DatasetSearchAuthContext ctx, AccessPolicyTerm policy)` → boolean
- `DatasetService` existing method signatures unchanged; delegating internally to these helpers
  is optional but encouraged for single-source-of-truth.
- Unit tests confirm parity: `DatasetAccessPolicy.canRead` produces the same result as
  `DatasetService.verifyPublicVisibilityAccess` for all role/visibility combinations.

**Implementation notes**:
- The helpers accept the pre-resolved `AccessPolicyTerm` (from B-3) rather than the raw `Study`
  object — this makes them work against both live dataset objects and indexed terms.
- Do not break the `DatasetService` public API during this refactor.

**Dependencies**: C-1, B-3.
**Size**: S

---

#### Ticket C-3 — Unit-test auth context for all role/dimension combinations

**Summary**: Parameterized unit tests covering every role and dimension combination for
`DatasetSearchAuthContext` and `DatasetAccessPolicy`.

**Acceptance criteria**:
- Positive matrix: `ADMIN`, public reader (`RESEARCHER`/`MEMBER`/`SIGNINGOFFICIAL`), dataset
  creator, study creator, and study custodian.
- Negative matrix: `CHAIRPERSON` or `MEMBER` with a DAC relationship but no creator/custodian
  relationship, and users sharing an institution with the submitter. These remain ordinary public
  readers; DAC and institution do not feed the auth context or policy.
- Each applicable combination tested for `canRead`, `isCreator`, and `isCustodian`.
- Edge cases: null study, dataset with no DAC, custodian email list empty, user with multiple
  roles.

**Implementation notes**:
- Use `@ParameterizedTest` with a method source building `DatasetSearchAuthContext` +
  `AccessPolicyTerm` pairs with expected `canRead` outcomes.
- Assert that `DatasetSearchAuthContextResolver` has no DAC or institution DAO dependency.

**Dependencies**: C-1, C-2.
**Size**: M

---

### Epic D — Native Elasticsearch DLS/FLS Path

**Goal**: Implement per-request Elasticsearch credentials (API keys with inline role descriptors
or `run_as` headers) that enforce DLS and FLS natively at the cluster level.

**Blocked by**: A-1 (cluster capability confirmed), B-3 (`accessPolicy` indexed), C-1 (auth
context available), **E-0 + E-1** (contract §1.1 — DLS does not deliver Decision 1 on its own),
**E-3** (contract §B.5c — the native FLS grant is wider than the response bundle, so the response
filter is what closes the difference).
**Blocks**: F.
**Condition**: Proceed only if A-1 confirms DLS/FLS support.

**Epic D is not sufficient on its own, and this is not a matter of degree.** Native DLS/FLS is a
component of the enforcement, not the whole of it. Two measurements from A-2's proof of concept
establish it independently: DLS leaves `significant_terms` and `explain` reporting index-wide
statistics, and FLS makes a non-granted path unqueryable so the grant has to include paths that must
not be returned. Shipping D-1…D-5 without E-0/E-1/E-3 produces a system that filters documents and
still leaks, and whose "My Data Submissions" and library visibility filters silently return nothing.

> ### ⚠ Do not start this epic without reading contract §G / **OPEN-13**
>
> Those corrections were made one ticket at a time and left an unasked question: **once
> E-0/E-1/E-2/E-3 ship — and they must, on any path — what does D-1…D-5 still buy?** Contract §G
> works the comparison. The short version: one thing, document filtering enforced by the *cluster*
> rather than by E-2's server-injected clause — and both express the **same predicate**, written once
> in the PoC model and fed to both paths.
>
> Deferring D-1…D-5 would remove the five tickets below, the `securityMode` switch, dual-mode wiring
> in D-4/E-4, the two-path parity burden, contract §B.5c's widened FLS grant, §B.7a's
> role-descriptor constraint, §B.5d's alias resolution, and the Platinum/Enterprise dependency —
> while Epics B, C and E ship unchanged. The forecast of finer per-document rules (rows 9–14)
> sharpens it: a growing predicate costs a bigger `bool` on the E-2 path and a bigger inline role
> descriptor **minted per request** on this one.
>
> **The question to answer first** (contract §G.3): DLS binds to the *credential*, E-2 binds to *our
> endpoint*. If anything other than this endpoint will ever query the index with the service's
> credentials — a BI tool, a notebook, a support engineer, a future service — then Epic D is **not**
> redundant and the deferral is wrong. Answer that before estimating any ticket below.
>
> **Recommended default: defer, do not cancel.** Nothing depends on D, D-3's predicate builder is
> shared and stays either way, and the deferral is cheap to reverse.

---

#### Ticket D-1 — Extend `ElasticSearchConfiguration` with security-mode settings

**Summary**: Add `securityMode`, privileged service-account credential fields, and the single search
field allowlist to `ElasticSearchConfiguration`.

**Context**: `ElasticSearchConfiguration` currently holds a single shared `authUser`/`authPassword`
(L22–24). The native path requires either a privileged account for API-key generation or a
`run_as`-capable service account.

**Acceptance criteria**:
- New fields:
  - `String securityMode` — `"none"`, `"fallback"`, `"shadow"`, or `"native-dls"`.
  - `String serviceAccountUser` / `String serviceAccountPassword` — may reuse `authUser`/
    `authPassword` if the same account has sufficient privilege.
  - `List<String> searchVisibleFields` — the one bundle's **literal** allowed paths, transcribed from
    [`es-access-contract.md`](es-access-contract.md) §B. Not glob patterns: contract §B.5 forbids
    wildcards, because `data`, `study.data`, and `study.assets` are dynamic maps and a
    `study.*`-style grant would publish whatever a future registration schema puts in them.
- Application starts cleanly with `securityMode: none` (legacy behavior unchanged).
- Startup validation: if `securityMode` is `"native-dls"` and `serviceAccountUser` is blank,
  throw with a descriptive error.

**Implementation notes**:
- Use Dropwizard `@JsonProperty` / `@NotNull` pattern consistent with existing fields.
- `searchVisibleFields` defaults to the complete SEARCH-VISIBLE list from contract §B. There is no
  public/privileged split and no caller-specific override.
- Document new keys in the config YAML schema or `docs/`.

**Dependencies**: A-1, A-2.
**Size**: S

---

#### Ticket D-2 — Per-request credential construction in `ElasticSearchSupport`

**Summary**: Add a method to `ElasticSearchSupport` that constructs a per-request `Request` object
carrying either an API-key header or `es-security-runas-user` header derived from
`DatasetSearchAuthContext`.

**Context**: `ElasticSearchSupport.createRestClient` (L17–46) builds a single shared `RestClient`
at startup. Per-request credentials are passed as HTTP headers on individual `Request` objects
using the existing low-level `RestClient` — no new client instance is needed per request.

**Acceptance criteria**:
- `ElasticSearchSupport.buildSecuredRequest(String endpoint, HttpEntity body,
  DatasetSearchAuthContext ctx, ElasticSearchConfiguration config)` → `Request` with the
  appropriate per-request auth header.
- API-key approach: call `POST /_security/api_key` with an inline role descriptor containing the
  DLS query from D-3 and FLS field list; embed the resulting key as `Authorization: ApiKey <base64>`.
- `run_as` approach (simpler fallback within D): set header
  `es-security-runas-user: <ctx.userId>` on a service-account-authenticated request.
- Unit test: given `isAdmin=true`, generated credential grants unrestricted **document** access but
  uses the same SEARCH-VISIBLE FLS grant as every other caller.
- Unit test: given non-admin context, credential includes DLS query and FLS field list.
- **The minted key names the dataset index in exactly one role descriptor** (A-2 finding 10, contract
  §B.7a). Multiple descriptors **union** their DLS queries and FLS grants rather than intersecting
  them, so a second descriptor granting plain `read` on the same index removes document filtering
  entirely — measured on 9.5.1, silently, with no error and an ordinary-looking response.
- **The key's owning user holds no separate index privilege on the dataset index.** An API key's
  effective permissions are the intersection of its descriptors with the owner's, so a superuser or
  broadly-privileged owner narrows nothing and the key's own restriction is all that acts.
- Unit test: a key minted with a second, unrestricted descriptor returns **every** document — asserted
  on the key the service actually mints, not on the descriptor the builder returns. Every existing
  single-descriptor test passes against a two-descriptor key, which is why this needs its own case.

**Implementation notes**:
- API keys have a TTL — set to ≤ 5 minutes. Avoid generating one per document; one per request is
  correct.
- `run_as` is simpler but requires pre-provisioned ES users for every DUOS user — less dynamic.
  API-key approach is more self-contained. Confirm feasibility with A-1 outcome.

**Dependencies**: D-1, C-1.
**Size**: L

---

#### Ticket D-3 — DLS query and FLS field-grant generator

**Summary**: Build `DlsQueryBuilder` and `FlsGrantBuilder` that translate `DatasetSearchAuthContext`
into an Elasticsearch DLS query string and an FLS field-grant list.

**Context**: The DLS query expresses the contract's document-visibility rules
([`es-access-contract.md`](es-access-contract.md) §A rows 1–3, 5–8). The FLS grant is **constant** —
one bundle for every caller including admins (contract Decision 2), so `FlsGrantBuilder` takes no
caller input at all beyond validating that it was asked for the one bundle that exists.

**Acceptance criteria**:
- `DlsQueryBuilder.buildForContext(DatasetSearchAuthContext ctx)` → JSON string.
  - ADMIN → `{"match_all": {}}`.
  - Non-admin → `bool` with `minimum_should_match: 1` over exactly these clauses:
    `publicVisibility` true; **dataset has no study**; dataset creator; study creator; custodian.
  - **No DAC clause, no institution clause, no policy-tag clause.** Contract rows 9–14 are DEFERred:
    none of them grants dataset read access today, and adding them here is an authorization
    expansion pending OPEN-3/OPEN-5.
- `FlsGrantBuilder.build()` → `List<String>`. **Revised by measurement — this is no longer the
  SEARCH-VISIBLE list** (contract §B.5c). It is:
  - the RESPONSE-VISIBLE paths from contract §B, **plus**
  - every QUERYABLE path, including the three that are RESPONSE-INTERNAL — `createUserId`,
    `study.dataSubmitterId`, `study.publicVisibility` — because a path outside the grant matches
    nothing (OPEN-8, resolved restrictively), **plus**
  - the `.keyword` subfield of every granted multi-field the product sorts or exact-matches on
    (`datasetName.keyword`, `study.studyName.keyword`), because granting `datasetName` does not grant
    its subfield and a sort on it then returns the wrong page silently.
  - **Same list for admins** — no `["*"]`. An admin wildcard would serve `accessPolicy.*` and the
    dynamic property maps, contradicting contract §B.4/§B.5/§B.7. ADMIN is a document-visibility
    bypass, not a projection bypass.
  - No wildcard or `except` form anywhere in the grant (contract §B.5), which is what makes the
    subfield enumeration real work: `datasetName*` would solve it and is forbidden.
- **E-3's response filter must run on this path**, since the grant now returns three paths that must
  not reach the caller. The grant governs what the *search* can resolve; the response filter governs
  what the *caller* receives. Native FLS cannot do both (contract §B.5c).
  - **Generated from the live mapping, and resolving any `alias` to its concrete field name before
    emitting it** (A-2 finding 11, contract §B.5d). Elastic documents that FLS "should not be set on
    alias fields — to secure a concrete field, its field name must be used directly"; granting an
    alias was measured to grant *nothing* on 9.5.1, so an alias in the grant fails closed and hides a
    field the product needs. B-1 forbids alias fields outright, which makes this a second line of
    defence rather than the control.
  - **Depends on B-1's mapping-shape constraints holding.** A `copy_to` into any granted field makes
    the grant meaningless for the copied field's content, and nothing in this builder can detect
    that — measured open on 9.5.1 (contract §B.5d).
- Unit tests per dimension, plus negative tests: a DAC member who is not creator/custodian does
  **not** match a non-public dataset; an admin grant contains no `accessPolicy` path; the grant
  contains a `.keyword` entry for every multi-field in the mapping that any sort target resolves to.
- **Note what the grant cannot buy** (A-2 finding 9, contract §B.0b, OPEN-12): granting the three
  QUERYABLE-but-RESPONSE-INTERNAL paths and stripping them in E-3 hides their *values from the
  response*, not from the caller. A `range` binary search recovers such a value exactly from
  `hits.total` in ten mediator-accepted requests. That is accepted for two user IDs and a boolean —
  but it is a decision (OPEN-12), and this builder should not be described as making those fields
  confidential.

**Implementation notes**:
- DLS query for a non-admin caller:
  ```json
  {"bool": {"should": [
    {"term": {"accessPolicy.publicVisibility": true}},
    {"term": {"accessPolicy.hasStudy": false}},
    {"term": {"accessPolicy.datasetCreatorUserId": 42}},
    {"term": {"accessPolicy.studyCreatorUserId": 42}},
    {"terms": {"accessPolicy.custodianEmails": ["user@example.com"]}}
  ], "minimum_should_match": 1}}
  ```
- `accessPolicy.hasStudy: false` is what keeps the currently-public "dataset with no study" case
  readable (contract §A row 5). Without it that case is silently denied — it cannot be carried by a
  null `publicVisibility`, because the filter treats a null on a study-bearing document as *not*
  public (contract §A.1).
- Both creator clauses are required and they are different columns; matching only one denies
  legitimate access to the other kind of creator.
- Custodian matching is **exact** — `keyword` term match, no lowercase normalizer on the field, and
  the caller email passed through unnormalized (contract §A.2). This preserves today's
  case-sensitive behavior; changing it is OPEN-6 and must move both paths at once.
- If `accessPolicy` is mapped as `nested`, terms must be wrapped in a `nested` query — confirm
  with B-1 mapping decision.
- **Generate the grant from the index mapping, not from contract §B's tables.** The tables enumerate
  logical paths and contain no subfields, so a grant generated from them compiles, passes review, and
  breaks sorting. This is OPEN-9's drift problem in a second place, and the second argument for
  generating rather than hand-maintaining.
- Build the role descriptor as a JSON tree and let the serializer escape the DLS query into the
  `query` string, rather than hand-escaping it. Hand-escaping is where these descriptors go wrong, and
  a mis-escaped DLS query can fail open. The PoC's `roleDescriptors` does it this way for that reason.
- **OPEN-8 no longer blocks this ticket.** It was resolved by measurement — see the A-2 outcome. The
  answer is restrictive, which is why the grant above is wider than originally specified.

**Dependencies**: D-2, C-2, B-1, **E-0 + E-1** (contract §1.1), **E-3** (contract §B.5c).
**Size**: L → **L/XL.** The generator itself is unchanged in size; the mapping-derived subfield
enumeration and the E-3 dependency are the additions.

---

#### Ticket D-4 — Wire per-request credentials into `searchDatasets` / `searchDatasetsStream`

**Summary**: Update `ElasticSearchService.searchDatasets` (L212) and `searchDatasetsStream` (L230)
to use per-request DLS credentials when `securityMode` is `"native-dls"`.

**Acceptance criteria**:
- Overloads (or updated signatures): `searchDatasets(String query, DatasetSearchAuthContext ctx)`
  and `searchDatasetsStream(String query, DatasetSearchAuthContext ctx)`.
- When `securityMode == "native-dls"`: obtain per-request credential from D-2, execute search
  with that credential.
- When `securityMode != "native-dls"`: fall through to existing behavior unchanged.
- `DatasetResource` callers at L425 and L439 updated to pass `duosUser` → resolved
  `DatasetSearchAuthContext`.
- Integration test: non-admin cannot retrieve a `publicVisibility=false` dataset via the search
  endpoint.
- **No caller-supplied URL parameter, index name or path reaches Elasticsearch** (A-2 finding 12,
  contract §F.2a). `_search` accepts `q` (a full Lucene query string), `_source_includes`,
  `docvalue_fields`, `explain`, `sort`, `search_type=dfs_query_then_fetch` (which scores from
  *global* term statistics), `scroll`, `routing` and `preference` — none of which the mediator sees,
  because the mediator reads the body. This holds in today's code by construction; make it an
  asserted invariant rather than an accident.
- **`validateQuery` must not see unmediated DSL.** `searchDatasets` calls it on the **raw** caller
  string today — regex-mangled, then sent to `_validate/query` — before building the search request,
  so whatever the mediator would refuse has already reached the cluster once. Run it on the mediated
  body or delete it; a server-built query does not need caller-DSL validation.

**Implementation notes**:
- Both endpoint methods already have `@Auth DuosUser duosUser` as a parameter (L428, L442) — the
  user is available, just not currently forwarded.
- Keep the existing parameterless signatures to avoid breaking admin-only callers that do not have
  user context; add overloads with `ctx`.

**Dependencies**: D-3, D-2.
**Size**: M

---

#### Ticket D-5 — Integration tests for DLS and FLS enforcement

**Summary**: Integration tests against a security-enabled Elasticsearch instance validating
document filtering and field omission.

> **Mostly written already.** A-2's proof of concept (`ElasticSearchLeakDefensePocTest`) is this
> ticket's corpus, running against a real cluster with the enforcement modeled in the test tree. This
> ticket's work is **substituting the real components for the model**, not authoring a new suite. Do
> not start over: the attack corpus, the leak-marker scheme, the caller fixtures and the two-path
> parity assertion all transfer unchanged, and the record of which assertions are load-bearing
> (contract §1.1a) is not cheap to reconstruct.

**Acceptance criteria**:
- Replace `ElasticSearchAccessContractModel`'s four modeled components with `DlsQueryBuilder`,
  `FlsGrantBuilder`, `AggregationVocabulary`, `SearchQueryMediator` and `ResponseFieldFilter`. The
  test class should need no changes beyond the call sites.
- **Keep the `UNMEDIATED` and `NATIVE_UNMEDIATED` configurations.** They are what prove the corpus can
  still detect a leak, and without them every "defended" assertion can pass vacuously. Deleting them
  because the code is now correct is the single most likely way to lose this suite's value.
- Keep the two-path parity assertion. Contract §1.1 warns that the enforcement paths drifting apart is
  how the fallback becomes a hole, and drift is invisible in whichever environment runs the other path.
  Both findings 2 and 3 first surfaced as parity failures.
- Seed test data via `ElasticSearchService.indexDataset` rather than the fixtures' literal documents,
  so the same code path as production populates `accessPolicy` (B-3) — this is the one substantive
  addition to the PoC, and it is what makes the suite a test of the pipeline rather than of the index.
- Retain the Elasticsearch-behavior probes as regression tests against a version bump:
  `min_doc_count: 0` isolation, `significant_terms` `bg_count`, `explain` statistics, FLS
  queryability of a non-granted path, `copy_to` reaching around the grant, and both alias directions.
- **Retain `ElasticSearchIndexSeparationTest`'s two scoring probes too** (A-2 finding 13, contract
  §A.3), even though the split it measures is not being built: that term statistics stay **per index**
  under the default `query_then_fetch`, and that `dfs_query_then_fetch` **pools** them. The second is
  what makes F-1's "forward no caller URL parameter" a control rather than hygiene, so it is a
  regression test for a shipping requirement, not only for a deferred option. It needs no license and
  shares the same container. If the pooling control ever stops pooling, the separation probe starts
  passing for the wrong reason — read that test's failure message before adjusting either.
- **Assert the role-descriptor count on the key the service actually mints** (A-2 finding 10,
  contract §B.7a), not on the descriptor `FlsGrantBuilder`/`DlsQueryBuilder` return. Descriptors
  **union** rather than intersect, so a second descriptor naming the index removes document filtering
  entirely — and every existing single-descriptor test passes against a two-descriptor key, which is
  exactly why this needs its own case. Assert the key's owning user holds no separate index privilege
  either.
- **Assert B-1's mapping-shape constraints against the live mapping** (A-2 finding 11, contract
  §B.5d). A `copy_to` into a granted field defeats the grant with no request to refuse and nothing in
  the response to filter; there is no other place this can be caught.
- **Keep the two "control removed" tests.** The closed shape allowlist and the aggregation-vocabulary
  check leak nothing the marker scan can see — one discloses through a hit count over documents the
  caller is authorized for, the other through a bucket key indistinguishable from a legitimate facet
  — so they are asserted by running the mediator with the control switched off and checking what it
  was holding. Any new control whose absence the corpus cannot detect needs the same treatment.
- **Keep the two DSL-surface sweeps.** They walk every clause in the Query DSL reference and every
  top-level member of the Search API reference, asserting each unsupported clause is refused by name
  and each unsupported member is stripped or refused. On a version bump, diff both lists against the
  references rather than trusting them.

**Implementation notes**:
- Extends `ElasticSearchContainerTests`; the image is pinned once, in `ElasticSearchTestCluster`.
- Detect leaks by scanning the whole serialized response, not by walking named paths. Contract §F.2
  requires unrecognized channels to fail closed, and a test that inspects `hits.hits[*]._source` shares
  the exact blind spot it exists to catch — that is how the §B.5b hole survived review in the first
  place.
- **Mutation-test the suite once it is real.** Two of the four deliberate weakenings tried against the
  model were not caught (contract §1.1a), and both gaps were in controls that looked obviously
  necessary. Re-run that exercise against the production components: remove `aggs` from the strip list,
  drop E-2's filter, narrow E-3 to `hits._source`, retain the `sort` channel.

**Dependencies**: D-4.
**Size**: L → **M.** The corpus exists; this is substitution plus the `indexDataset` seeding path.

---

### Epic E — Compatibility Fallback

**Goal**: Server-owned aggregations, query mediation, and response field allowlisting.

**Renamed from "Compatibility Fallback", because it is not one.** E-0, E-1 and E-3 are required in
every configuration, licensed or not — the A-2 proof of concept measured Epic D to be insufficient
without them (contract §1.1, §B.5c). Only **E-2** (the injected authorization filter, which DLS
replaces) and **E-4** (wiring for the unlicensed path) are fallback-specific. The epic ships whole
where DLS/FLS is unavailable, and ships minus E-2 alongside Epic D where it is available.

The ticket IDs are unchanged deliberately: `es-access-contract.md` references E-0/E-1/E-2/E-3 by
number throughout, and renumbering them to reflect the regrouping would invalidate every one of those
references for no gain.

**Blocked by**: B-3 (`accessPolicy` indexed), C-1 (auth context available).
**Blocks**: D (via E-0/E-1/E-3), F.

---

#### Ticket E-0 — Server-owned aggregation vocabulary

**Summary**: Build the aggregations server-side from a closed, named vocabulary selected by
`(tab, filters, page, size, sort)`, so callers never send `aggs` at all.

**Context**: Contract §F.1. Caller-supplied aggregations are the one surface where neither DLS nor a
field allowlist closes the leak — `significant_terms` on a perfectly legitimate field such as
`study.piName` reports `bg_count`, the document count of the entire index, because it compares against
an index-wide background set by design. Validating shapes means maintaining a denylist against an open
grammar that grows with every Elasticsearch release.

> **Measured, and the example changed (A-2 finding 1).** This ticket originally led with `terms` +
> `min_doc_count: 0`, on Elastic's documented warning. That does **not** reproduce on 9.4.4 — DLS
> isolates it correctly, as it does `shard_min_doc_count: 0`, an `include` regex, `_key` ordering,
> `rare_terms`, the `global` aggregation, `cardinality`, `value_count` and the `_terms_enum` API. What
> does leak past DLS is `significant_terms`' `bg_count` and `explain`'s `_explanation`, which reports
> index-wide `N` and `docFreq`. The justification for this ticket is unchanged and the denylist
> argument is now stronger, not weaker: two of the three shapes that denylist would have named have
> swapped status against the version we actually run, in the direction nobody would have re-checked.

The vocabulary is already closed in practice. Every aggregation the product issues is one of three
shapes, all in `duos-ui/src/components/data_library/assets/`:

1. **`FILTER_AGGS`** (`datasetAsset.ts:18-22`) — four fixed `terms` facets on `accessManagement`,
   `dataUse.primary.code`, `study.dataTypes`, `dac.dacName`. No parameters.
2. **`STUDIES_AGG`** (`definition.ts:42-56`) — `terms(study.studyId, size 10000)` +
   `top_hits(size 1)`. Shared unchanged by **nine** study-asset tabs; the asset type selects which
   `study.assets.*` leaves the `top_hits` `_source` requests.
3. **studyAsset composite** (`studyAsset.ts:42-72`) — `cardinality(study.studyId)`, a `composite`
   page over `study.studyId`, plus `top_hits`, `value_count(datasetId)`, `sum(participantCount)`,
   `terms(datasetId)`, and the filter facets.

**Acceptance criteria**:
- `AggregationVocabulary.build(AssetType tab, FilterState filters, PaginationState page, SortState sort)`
  → the `aggs` node, covering all three shapes above.
- Shape 2's `top_hits` `_source` is the **enumerated** `study.assets.<type>.*` leaf list for the
  requested tab (contract §B.5a) — never `["study.*"]`, never a wildcard.
- No parameter of any shape is caller-settable beyond the four listed: no `min_doc_count`, no
  aggregation type, no `background_filter`, no field target.
- Unit test: each of the eleven asset tabs produces an `aggs` node equivalent to what
  `duos-ui/develop` sends today, so the migration is behavior-preserving.
- Unit test: a caller-supplied `min_doc_count`, `significant_terms`, or arbitrary field target cannot
  reach the built aggregation by any input to `build(...)`.
- Unit test: the studies tab's `composite` paging returns the same page boundaries as the current
  client-built query.
- **`AggregationVocabulary.validate(aggs)`, run before every execution and over every entry at build
  time** (A-2 finding 6, contract §F.2). Every `terms` `field` and every `top_hits` `_source` leaf
  must be **RESPONSE-VISIBLE** — not merely QUERYABLE, since both become response content — every
  aggregation type must be one of the three shapes, and every parameter must be one that shape may
  set. A `top_hits` with no explicit `_source` is refused, because it returns whole documents.
- Unit test: a vocabulary entry whose `terms` `field` is `accessPolicy.custodianEmails`, and one
  whose `top_hits` `_source` names `study.assets.models.internalCheckpointUri`, are both refused
  before execution. Both are what OPEN-9's predicted drift looks like at the point where it does
  damage.

> **Why this validator lives here rather than in E-3 (A-2 finding 6).** Contract §F.2 originally
> assigned `aggregations.**.buckets[*].key` to E-3, to be "projected defensively" against
> RESPONSE-VISIBLE. That is not implementable. A bucket key is a bare *value* arriving with no field
> name attached and is structurally identical to the `accessManagement` keys the filter panel is
> built from, so nothing in the response distinguishes a legitimate facet key from
> `custodian@example.org` — the same argument §F.2 already accepts for the `sort` channel. Measured:
> with E-3 running, a drifted `terms` aggregation on `accessPolicy.custodianEmails` returns the
> emails as keys. The request side is where the field target is still known, and this ticket owns it.

**Implementation notes**:
- Nine of eleven tabs share shape 2, so the duos-ui side of this concentrates in `definition.ts` and
  `useLibraryData.ts` rather than spreading across all eleven asset files.
- This is the first increment of deferred plan item 4.2 (server-owned search API taking business
  parameters) — see contract §F.1a and OPEN-11. Building it here is not throwaway scaffolding; the
  remaining increment is the `query` clause.
- Ship behind the same `securityMode` switch as the rest of Epic E, but note the dependency direction:
  D-3 needs this too (contract §1.1), so it cannot be fallback-gated permanently.

**Dependencies**: C-1, B-3 *(for the §B.5a leaf enumeration)*.
**Blocks**: E-1, D-3.
**Size**: L

---

#### Ticket E-1 — Build `SearchQueryMediator` with DSL sanitization

**Summary**: Implement `SearchQueryMediator` that strips unsafe surfaces from client-provided ES
DSL before execution.

**Context**: `DatasetResource.searchDatasetIndex` (L425) currently passes the client request body
directly to `ElasticSearchService.searchDatasets` with no sanitization. A client can include
`_source` overrides, `script_fields`, `explain`, or `profile` to extract information about
documents it should not see.

> **Revised by contract §F.1 and §B.5b.** The original criteria stripped *top-level* keys and
> preserved `aggs`/`sort`/`highlight` wholesale. Both halves are unsafe: the client's own
> `STUDIES_AGG` carries a nested `_source` wildcard inside `aggs.*.top_hits` that a root-level
> `remove()` does not touch, and a preserved `aggs`/`sort`/`highlight` can target a
> RESPONSE-INTERNAL path.
>
> `aggs` is now **stripped rather than validated** — see the new **E-0**, which rebuilds the three
> aggregation shapes the product actually uses on the server. Contract §F.1 explains why validating
> caller aggregations is not a defensible position: the field allowlist cannot close count-and-
> existence leaks that ride on legitimately queryable fields, which leaves a three-item denylist of
> aggregation shapes doing the security work.
>
> This ticket is also **no longer fallback-only** — contract §1.1 makes query mediation a prerequisite
> of the native path too, so Epic D depends on it.

**Acceptance criteria**:
- `SearchQueryMediator.sanitize(String clientDsl)` → `String`:
  - Removes these keys **at every depth**, not only at the root: `aggs`, `aggregations`, `_source`,
    `docvalue_fields`, `script`, `script_fields`, `stored_fields`, `explain`, `profile`,
    `seq_no_primary_term`, `version`, `runtime_mappings`, `collapse`, `inner_hits`, `rescore`,
    `suggest`, `indices_boost`.
  - **`fields` is removed conditionally, not at every depth** — see the correction below. It is a
    response channel at request level and inside `top_hits`/`inner_hits`, but the clause's own field
    list inside `multi_match` and `highlight`. Strip it in the first role; keep and validate it in
    the second. (`query_string`, `simple_query_string` and `combined_fields` also carry a legitimate
    `fields` member, but the shape allowlist below refuses all three outright, so their `fields`
    never reaches validation. Keep the two lists separate anyway: stripping runs first and has to be
    readable without the allowlist.)
  - **Accepts a closed allowlist of shapes and refuses everything else** — request members, query
    clause types, and the members and value shapes each clause may carry. An earlier version of this
    criterion said "structurally permits `query`, `sort`, `size`, `from`, `search_after`, `pit`,
    `highlight` — but every field reference inside them is validated", which is not sufficient and
    which put `pit` on the permitted list. Measured (contract §F.1 rule 0), six shapes pass the
    field-reference validator untouched because their references are not where it looks:
    `query_string` (fielded terms inside the query *text*), a `terms` **lookup** (`path` names a
    field read out of another document, which no filter bounds), `more_like_this` (`like` takes
    document **ids**), `_script` sort, `knn` (a request-level `field`), and `pit` (replaces the index
    being searched). `highlight_query`, `matched_fields` and `function_score` are the same problem in
    a nested query.
  - Supported request members are `query`, `sort`, `highlight`, `from`, `size`, `search_after`,
    `track_total_hits`. Supported query clauses are `match_all`, `match_none`, `bool`, `term`,
    `terms`, `match`, `match_phrase`, `range`, `exists`, `multi_match` — the product's entire query
    surface. **Refuse a `multi_match`/`simple_query_string`/`combined_fields` with no `fields`
    member**: it falls back to `index.query.default_field`, which defaults to `*`, so a clause with
    no field reference at all searches every field in the mapping.
  - Note the ordering with the strip list: **strip first, then refuse.** Stripping is the
    compatibility layer, so duos-ui's current body (which sends `_source` and `aggs`) is accepted
    rather than rejected on its first call; the allowlist is the security layer over whatever is
    left.
  - Aggregations are **not** validated and forwarded; they are stripped here and rebuilt by E-0 from
    a closed server-owned vocabulary (contract §F.1).
  - Result is valid JSON.
- `SearchQueryMediator.validateFieldReferences(String dsl, Set<String> queryableFields)`:
  - Collects every field reference at every depth from the surfaces that remain caller-controlled:
    query clause targets, `sort` keys, `highlight.fields` keys, and the `fields` entries of the
    `multi_match` family.
  - **Rejects** (does not silently drop) a request referencing a path outside the QUERYABLE
    allowlist — dropping a `filter` clause broadens the query and dropping a `sort` changes paging.
  - Normalizes before matching: resolves `.keyword` suffixes and strips `field^boost`. **Refuses
    wildcards** rather than expanding them — with the server owning `aggs` and `_source`, no
    legitimate caller reference contains one.
  - **Resolving `.keyword` here obliges D-3's grant to serve the subfield.** Accepting
    `datasetName.keyword` because it normalizes to an allowlisted `datasetName` is only sound if the
    FLS grant actually contains `datasetName.keyword`, and by default it does not (contract §B.5c).
    The two must change together or sorting silently returns the wrong page.
  - **No aggregation-shape validation.** Contract §F.1 rejects that approach: a denylist of
    `min_doc_count: 0` / `significant_terms` / `significant_text` fails open on the next aggregation
    type, and a field allowlist cannot close count-and-existence leaks that ride on legitimately
    QUERYABLE fields. Aggregations are stripped and rebuilt server-side by **E-0** instead.
- Unit test: sanitized DSL contains none of the stripped keys **including inside a nested
  `aggs.*.top_hits`** — use the real `STUDIES_AGG` shape as the fixture.
- Unit test: a caller-supplied `aggs` block is removed entirely, whatever it contains.
- Unit test: a `sort` on `study.throughBioId` is rejected.
- Unit test: DSL with only an allowlisted `query` and `size` is returned unchanged.
- Unit test: `{"term": {"createUserId": 42}}` is **accepted** — it is RESPONSE-INTERNAL but
  QUERYABLE, and "My Data Submissions" depends on it (contract §B.0a).
- Unit test: `{"multi_match": {"query": "x", "fields": ["datasetName", "study.studyName^2"]}}` is
  **accepted with its `fields` intact**, and `{"multi_match": {"query": "x", "fields": ["*"]}}` is
  **rejected**. The first fails if `fields` is stripped unconditionally; the second only passes if it
  was not, because there is nothing left to reject once it has been removed.
- Unit test: `{"highlight": {"fields": {"datasetName": {}}}}` survives sanitization with its `fields`
  intact, and the same shape naming `study.throughBioId` is rejected.

**Implementation notes**:
- A single recursive `JsonNode` walk, not `((ObjectNode) root).remove(...)`. The original note is
  the source of the nested-`_source` hole; do not reinstate it.
- **The walk must carry its parent key**, because the `fields` rule is context-dependent. A strip list
  that only knows the current key cannot tell `$.fields` from `$.query.multi_match.fields`.
- **Correction from A-2's proof of concept (finding 4).** The earlier revision of this ticket said to
  add `fields` to the strip list *and* to strip at every depth. Those two instructions together break
  the library search box — a stripped `multi_match.fields` silently widens the search to every field
  in the mapping rather than erroring — and disable every highlighted column. They also delete the
  `["*"]` reference the validator needs in order to refuse it, so the sanitizer ends up quietly
  permitting the thing it was added to block. Contract §F.1 rule 1 has the corrected rule.
- The QUERYABLE allowlist and the RESPONSE-VISIBLE allowlist are **different sets** (contract §B).
  Keep them as two named constants so a future edit cannot conflate them.
- Keep sanitization/validation separate from auth filter injection (E-2) for independent testability.

**Dependencies**: C-1, E-0 *(E-0 owns the aggregations this ticket strips)*.
**Blocks**: D-3 (contract §1.1), E-2, E-4.
**Size**: M *(unchanged — E-0 absorbs the aggregation work, so this ticket stays a strip list, a
shape allowlist and a field-reference check; contract §F.3. The allowlist added by A-2 finding 7 is
smaller than the walk it protects, since the product's whole query surface is nine clauses. Its cost
is policy rather than code: adding a tenth clause becomes a review question.)*

---

#### Ticket E-2 — Inject mandatory authorization filter into `SearchQueryMediator`

**Summary**: Extend `SearchQueryMediator` to wrap the sanitized client query in a `bool.must`
alongside a server-built access policy filter derived from `DatasetSearchAuthContext`.

**Acceptance criteria**:
- `SearchQueryMediator.mediate(String clientDsl, DatasetSearchAuthContext ctx)` → `String` produces:
  ```json
  {"query": {"bool": {"must": [<sanitized client query>, <access policy filter>]}}, ...}
  ```
- ADMIN context → access policy filter is `{"match_all": {}}`.
- Non-admin → access policy filter equivalent to DLS query from D-3 (same logic, different
  execution path — reuse `DlsQueryBuilder.buildForContext` if D-3 has shipped, otherwise implement
  a standalone `AccessFilterBuilder` and unify later).
- Unit tests for each enforced access dimension: public reader, dataset creator, study creator, and
  custodian. Add a negative DAC-member/chair case proving that DAC relationship alone does not add a
  clause or grant a restricted document.
- Negative test: crafted client query attempting to retrieve `publicVisibility=false` documents is
  blocked by the injected filter.

**Implementation notes**:
- Jackson `ObjectNode` manipulation: extract the client `query` node, build the `bool.must`
  wrapper, replace `root.query` with the wrapped version.
- The `accessPolicy` fields used in the filter must exist in the index (B-3 required as a
  prerequisite).

**Dependencies**: E-1, C-2, B-3.
**Size**: L

---

#### Ticket E-3 — Server-managed field allowlist applied to search responses

**Summary**: Strip every field outside the single SEARCH-VISIBLE allowlist from Elasticsearch
response documents, for every caller.

**Context**: The server must remove restricted fields from response documents before returning them.

> **Revised by contract §F.2 and §B.5b.** Filtering `hits.hits[*]._source` is not sufficient.
> A `top_hits` sub-aggregation returns whole `_source` documents at
> `aggregations.**.hits.hits[*]._source`, and that is the **primary** response channel for nine of
> the data library's eleven tabs — so as originally specified this ticket returned complete,
> unfiltered `study` objects to every caller. `sort`, `highlight`, and `fields` leak per-hit values
> the same way.
>
> **Revised again by contract §B.5c — this ticket is no longer fallback-only.** It runs on the native
> path too. OPEN-8 resolved restrictively: a path outside the FLS grant is not queryable, so D-3's
> grant has to include the three QUERYABLE-but-RESPONSE-INTERNAL paths, and something other than FLS
> must then keep them out of the response. That something is this filter. The grant governs what the
> *search* can resolve; this ticket governs what the *caller* receives. **Epic D depends on it.**

**Acceptance criteria**:
- `ResponseFieldFilter.apply(String responseJson, List<String> responseVisibleFields)` → `String`
  retains only RESPONSE-VISIBLE paths from contract §B, dropping everything else including
  unrecognized paths, across **every** channel:
  - `hits.hits[*]._source`
  - `aggregations.**.hits.hits[*]._source` — `top_hits` at arbitrary nesting depth
  - `aggregations.**.buckets[*].key` — **out of scope, moved to E-0** (A-2 finding 8). A `terms`
    bucket key *is* a field value, but it arrives with no field name attached and is structurally
    identical to the `accessManagement` keys the filter panel needs, so no allowlist can tell them
    apart — the same reason `hits.hits[*].sort` must be dropped rather than filtered. Measured: with
    this filter running, a drifted `terms` aggregation on `accessPolicy.custodianEmails` returns the
    emails as keys. E-0 validates its own vocabulary's field targets before executing them instead.
  - `hits.hits[*].highlight` and `hits.hits[*].fields` — **projected**, not dropped: both are keyed by
    field path, so they can be filtered, and dropping them wholesale would disable highlighting on
    `datasetName`, which the catalog uses.
  - `hits.hits[*].sort` — **dropped entirely, not filtered** (contract §F.2, A-2 finding 5). Sort
    values are a positional array carrying no field names, so no allowlist applies to them. It matters
    because the two §B axes overlap here: `createUserId` is QUERYABLE and RESPONSE-INTERNAL, E-1
    therefore accepts it as a sort key, and this channel then echoes its value once per hit. Nothing is
    lost — no duos-ui caller reads sort values.
  - `hits.hits[*].inner_hits.**._source`
  - **ADMIN is filtered identically** — no bypass. Admins see every *document* (DLS `match_all`),
    not every *field*; contract §B.7.
- **Applies to `searchDatasetsStream` as well** (A-2 finding 12, contract §F.2a). The v2 endpoint
  returns Elasticsearch's response body **verbatim** as an `InputStream` — no `hits` extraction, and
  not even the `validateQuery` call that v1 makes — so today it is the *less* protected of the two
  endpoints, not the more. That makes this a streaming filter rather than a response-object filter,
  which is a real difference in the work and the reason for the size bump below.
- Unit test: `accessPolicy` is absent from the response for **every** caller, admin included
  (contract §B.4) — asserted on the `top_hits` channel as well as `hits._source`.
- Unit test: `data` and `study.data` are absent for every caller (contract §B.5).
- Unit test: `study.assets.*` **is** present, limited to the enumerated leaves the eleven
  `AssetDefinition`s declare (contract §B.5a). This is the regression test for the misclassification
  that would otherwise have emptied nine library tabs.
- Unit test: `requestLocation`, `deletable`, and `submitter.displayName` are **present** — all three
  are live UI dependencies (contract §B.0a).
- Unit test: `createUserId` and `study.dataSubmitterId` are **absent** from the response while
  remaining accepted in a query (paired with E-1's test).
- Unit test: a real `STUDIES_AGG`-shaped response has its nested `_source` filtered.
- Unit test: a path not present in §B — simulating a newly added model field — is dropped rather
  than passed through, in every channel above.
- Unit test: `hits.hits[*].sort` is absent from the filtered response after a sort on `createUserId`,
  which E-1 legitimately accepts.
- Unit test: an aggregation whose **server-built** `top_hits` `_source` names a non-RESPONSE-VISIBLE
  leaf still has it stripped. This is the OPEN-9 drift case, and it is the only test in this ticket
  that fails if the `aggregations.**` walk is removed — see the implementation note below.

**Implementation notes**:
- Walk `hits.hits[*]._source` as `Map<String, Object>` and apply a recursive **allowlist** filter:
  retain only paths present in the bundle, drop everything else — including paths the filter does not
  recognize. A denylist, or a glob like `"study.*"`, fails open on every field added later; contract
  §B.5 forbids both, because `study.data` / `study.assets` / `data` are dynamic maps whose keys are
  populated wholesale from property bags.
- There is one bundle for all callers (contract Decision 2) — it comes neither from the document
  (cancelled B-2) nor from the caller. Admins included; see contract §B.7.
- `accessPolicy.*` must be stripped from every response regardless of bundle (contract §B.4). The
  fallback path retrieves whole `_source` objects, so this is the ticket where an enforcement-input
  field would otherwise be handed back to the caller.
- Bundle definitions must be generated from, or checked against, contract §B — the same source D-3
  builds its FLS grant from. Two hand-maintained lists will diverge, and the divergence will only be
  visible in whichever environment runs the other path. Note that D-3's grant is a **superset** of this
  ticket's allowlist (contract §B.5c), so "the same list" is no longer literally true — derive both
  from §B, but do not assume they are equal.
- **Recurse structurally, not by known path.** `aggregations` nests arbitrarily and its keys are
  caller-named. The cleanest form is a single rule — *any object carrying a `_source` is a hit,
  wherever it occurs* — which covers `top_hits` and `inner_hits` through the same code path as ordinary
  hits, and needs no list of channels to look in.
- **Fail closed by retention, not by removal.** Keep a named set of hit-level keys (`_id`, `_score`,
  `_source`, `highlight`, `fields`) and drop everything else, rather than enumerating channels to
  remove. That way a response channel added by a future Elasticsearch version is dropped by default —
  which is what contract §F.2 asks for and is not achievable with a removal list.
- **That retained set is also the only control over the Elasticsearch metadata fields** (contract
  §B.5d). FLS *always* permits `_id`, `_index`, `_routing`, `_type`, `_parent`, `_timestamp`, `_ttl`
  and `_size` regardless of the grant, so D-3's grant cannot restrict them and B-1 classifies them
  instead. Retaining only `_id` — which is the dataset ID and RESPONSE-VISIBLE — drops the rest,
  including `_routing` and `_index`, and it does so on the native path as well as the fallback. Adding
  a metadata field to the retained set is therefore an exposure decision, not a convenience one.
- **What the `aggregations.**` walk is actually for (A-2 finding 6).** Once E-0 makes aggregations
  server-owned, callers cannot reach that channel, and removing the walk changes no result for any
  caller-supplied request — this was measured by deleting it and watching the PoC stay green. Keep it
  anyway: what it defends against is **us**. Contract §B.5a puts the `study.assets.*` leaf enumeration
  in duos-ui's asset definitions and OPEN-9 warns the backend copy will drift; a drifted copy makes the
  *server* ask Elasticsearch for an internal leaf, where neither E-1's strip list nor its field
  validator is involved and the caller has done nothing wrong. This projection is the only control
  left. The unit test above is that scenario, and it exists so the walk cannot be simplified away as
  dead weight once E-0 lands.

**Dependencies**: E-2, D-1.
**Blocks**: **D-3** (contract §B.5c).
**Size**: M → **L.** Multi-channel recursion, plus running on both paths rather than one, plus a streaming variant for the v2 endpoint (A-2 finding 12). Partly offset: `buckets[*].key` moves out of scope to E-0 (A-2 finding 8).

---

#### Ticket E-4 — Wire `SearchQueryMediator` into `ElasticSearchService`

**Summary**: Wire the mediator (E-1/E-2) and response filter (E-3) into `searchDatasets` and
`searchDatasetsStream` when `securityMode` is `"fallback"`.

**Acceptance criteria**:
- When `securityMode == "fallback"`:
  - Client DSL processed by `SearchQueryMediator.mediate(clientDsl, ctx)` before ES submission.
  - Response processed by `ResponseFieldFilter.apply(...)` before returning to caller.
- When `securityMode == "none"`: existing behavior unchanged.
- Both `searchDatasets` (L212) and `searchDatasetsStream` (L230) updated.
- `DatasetResource` callers at L425 and L439 updated to pass `duosUser` → resolved
  `DatasetSearchAuthContext`.
- **Mediation is the first thing either method does, and nothing downstream sees the raw string**
  (A-2 finding 12, contract §F.2a). `searchDatasets` currently calls `validateQuery(query)` on the
  **unmediated** caller DSL — stripping `sort`/`size`/`from` with three regular expressions, then
  sending the result to `_validate/query` — *before* it builds the search request. On that path,
  whatever the mediator would refuse has already reached the cluster once. Run validation on the
  mediated body or **delete it**: a server-built query does not need caller-DSL validation, and the
  regex stripping is its own hazard.
- **No caller-supplied URL parameter, index name or path is forwarded**, with a regression test.
  `_search` accepts `q`, `_source_includes`, `docvalue_fields`, `explain`, `sort`,
  `search_type=dfs_query_then_fetch`, `scroll`, `routing` and `preference` — a channel the mediator
  never sees, because it reads the body. True by construction today; make it asserted. **`search_type`
  is the measured one** (A-2 finding 13, contract §A.3): `dfs_query_then_fetch` pools term statistics
  across every index targeted, so a caller who could set it widens the population its scores are drawn
  from. That makes this criterion a control with a demonstrated consequence rather than hygiene.
- **`searchDatasetsStream` gets the response filter too.** It currently returns Elasticsearch's body
  **verbatim** as an `InputStream`, with no `hits` extraction and without even the `validateQuery`
  call — so it is the *less* protected of the two endpoints today, not the more.

**Implementation notes**:
- `searchDatasetsStream` returns an `InputStream` — apply the field filter by reading the stream,
  filtering the JSON, then re-wrapping as an `InputStream` before returning; or convert to
  `String` internally and stream the result. Note this makes E-3 a streaming filter rather than a
  response-object filter, which is a real difference in the work (contract §F.2a).
- Keep the `securityMode` switch as a simple if-else in `ElasticSearchService`.
- The same ordering requirement applies to **D-4** on the native path: per-request credentials do not
  help if unmediated DSL has already been sent under the service's own credentials.

**Dependencies**: E-2, E-3.
**Size**: S → M *(the ordering fix and the stream filter are both new scope from A-2 finding 12)*

---

#### Ticket E-5 — Unit-test `SearchQueryMediator` and `ResponseFieldFilter`

**Summary**: Comprehensive unit tests covering all access dimensions and edge cases for the
mediator and response filter.

**Acceptance criteria**:
- `SearchQueryMediator` tests: DSL passthrough for ADMIN, correct `bool.must` injection for each
  non-admin role/dimension.
- `ResponseFieldFilter` tests: the same allowlist is applied to every caller including ADMIN;
  nested SEARCH-VISIBLE fields such as `study.dataCustodianEmail` are retained; INTERNAL fields and
  unknown fields are removed; null/missing source fields are not errors.
- Negative test: client DSL with injected `_source` override does not expose restricted fields
  after full mediation pipeline.
- Negative test: a `fields` list inside `multi_match` survives sanitization while a root-level `fields`
  does not (E-1's context-dependent rule — A-2 finding 4).
- Negative test: `hits.hits[*].sort` is absent from a filtered response (E-3 — A-2 finding 5).
- Negative test: a `query_string` clause is **refused**, not validated — its field references live in
  the clause's own query text where no walk reaches them (A-2 finding 7).
- Negative test: a `terms` clause whose value is a **lookup** (`{"index":…,"id":…,"path":…}`) is
  refused. The clause is supported and its field reference validates fine; only the *value shape* is
  dangerous, so a test that checks field names alone passes while the hole is open.
- Negative test: a `multi_match` with no `fields` member is refused — it falls back to
  `index.query.default_field` (`*`) and searches every field in the mapping.
- **Sweep tests**: every clause in Elasticsearch's Query DSL reference that is not on the supported
  list is refused by name, and every top-level Search API body member that is not supported is either
  stripped or refused. These are what make "closed allowlist" checkable rather than a count of the
  examples someone thought of; keep them diffed against the references on a version bump.
- Negative test: an aggregation vocabulary entry whose `terms` `field` is not RESPONSE-VISIBLE is
  refused **before execution** (E-0 — A-2 finding 8). Bucket keys cannot be filtered afterwards.

**Implementation notes**:
- Use `JSONAssert` or Jackson-based assertions for comparing query structure.
- Test the full pipeline end-to-end: `mediate(...)` → mock response JSON → `apply(...)` →
  assert final field set.
- **These are the unit-level companions to D-5, not a substitute for it.** The mediator and response
  filter can both be unit-tested into a state that passes every assertion here and still leaks, because
  what leaks is a channel nobody thought to assert on. The whole-response scan against a real cluster
  is the check that catches those; keep both.

**Dependencies**: E-4.
**Size**: M

---

### Epic F — API Hardening

**Goal**: Harden the existing search endpoints to always route through the secured/mediated path,
and design the long-term server-owned search API.

**Blocked by**: Epic E live (E-0/E-1/E-3 at minimum), and Epic D additionally where the cluster is
licensed. Not "either one" — see the A-2 outcome.
**Blocks**: G.

---

#### Ticket F-1 — Harden search endpoints to always pass caller context

**Summary**: Update `DatasetResource.searchDatasetIndex` (L425) and `searchDatasetIndexStream`
(L439) to always forward `duosUser` into the secured search path; remove the parameterless
passthrough.

**Context**: Both methods already receive `@Auth DuosUser duosUser` (L428, L442) but do not
currently forward it to the service call (L430, L444). This is the wiring ticket.

**Acceptance criteria**:
- Both endpoint methods pass `duosUser` → resolved `DatasetSearchAuthContext` to the service.
- No remaining code path calls the ES service without caller context.
- Regression tests: non-admin users see only authorized datasets; admin users see all.
- **The request surface stays the JSON body, and that becomes an asserted invariant** (A-2 finding
  12, contract §F.2a). Every control in contract §F assumes caller influence arrives as a body sent
  to a server-built path. That is true today and nothing enforces it. Assert that the endpoints:
  - accept **no caller-supplied URL query parameter** — `_search` takes `q` (a full Lucene query
    string), `_source_includes`, `_source_excludes`, `docvalue_fields`, `stored_fields`, `explain`,
    `version`, `seq_no_primary_term`, `sort`, `search_type=dfs_query_then_fetch`, `scroll`, `routing`
    and `preference`; the mediator sees none of them. **`search_type` is measured, not theoretical**
    (A-2 finding 13, contract §A.3): `dfs_query_then_fetch` pools term statistics across every index
    targeted, so a caller able to set it scores from a wider population than the one it may read;
  - accept **no caller-supplied index, alias or path** — a caller-chosen target would apply the DLS
    query and injected filter to the wrong data;
  - reach Elasticsearch **only** through the mediated search call. `_count`, `_msearch`,
    `_field_caps`, `_terms_enum`, `_mget`, `_termvectors`, `_explain`, `_validate/query`,
    `_async_search`, `_pit`, `_scroll`, `_sql` and `_esql` all read the same index and none is
    mediated. `_validate/query` is not hypothetical — see E-4.

**Implementation notes**:
- Inject `DatasetSearchAuthContextResolver` into `DatasetResource` via Dropwizard constructor
  injection.
- The resolver call is cheap (pre-built context) — resolving once per request at the resource
  layer is the right place.

**Dependencies**: D-4 or E-4.
**Size**: S

---

#### Ticket F-2 — Design server-owned v3 search API (deferrable)

**Summary**: Define a server-owned search API that accepts structured business parameters instead
of raw Elasticsearch DSL.

**Acceptance criteria**:
- API contract: request body schema (text query string, typed facet filters, page/size, sort),
  response schema (shaped `DatasetTerm` list + total count + facet counts).
- No raw Elasticsearch DSL in request or response.
- Design document / ADR capturing field mapping, facet aggregation strategy, sort field allowlist.

**Implementation notes**:
- The existing `elastic.ts` types construct DSL queries for `accessManagement`, `dataUse`,
  `dataType`, `dac` facets (see `DatasetSearchTable.jsx:L100–141`). These become the typed filter
  model in the v3 request schema.
- Server builds: full-text query across `datasetName`, `study.studyName`, `study.description`,
  `study.piName`; terms filters per facet; pagination; access policy filter injected server-side.

**Dependencies**: F-1.
**Size**: L (deferrable)

---

#### Ticket F-3 — Expose v3 search endpoint with backward compatibility (deferrable)

**Summary**: Implement `POST /api/dataset/search/index/v3` and add deprecation notices to v1/v2.

**Acceptance criteria**:
- v3 endpoint responds with shaped dataset list for valid business-parameter requests.
- v1 and v2 continue to function; return a `Deprecation` response header pointing to v3.
- duos-ui integration test verifies v3 returns the same datasets as v2 for equivalent queries.

**Dependencies**: F-2.
**Size**: M (deferrable)

---

### Epic G — Frontend Alignment

**Goal**: Remove client-side visibility filtering from duos-ui, update all search callers to
trust server-filtered results, and align Cypress tests with the server-authoritative model.

**Blocked by**: F-1 (or E-4 live behind feature flag).
**Blocks**: Nothing.

---

#### Ticket G-1 — Remove client-side `publicVisibility` and `dacApproval` filtering

**Summary**: Remove the `'study.publicVisibility': true` term injected in `DatasetSearch.jsx`
(L34) and any equivalent `dacApproval` filter injection in other search callers.

**Context**: Once the server enforces these filters (F-1), client-side duplication becomes a
source of drift — and may under-return results for callers who should see private datasets (e.g.
a creator searching for their own `publicVisibility=false` study).

**Acceptance criteria**:
- `DatasetSearch.jsx` no longer injects `'study.publicVisibility': true`.
- All other search callers (`DatasetSearchTable.jsx`, `BucketUtils.ts`, etc.) audited and any
  equivalent access-policy filter terms removed.
- Cypress tests updated to not assert that these terms appear in outgoing request bodies.
- Manual test: an admin user can see `publicVisibility=false` datasets that were previously hidden
  client-side; verify the server now controls visibility.
- **Once this lands, `study.publicVisibility` leaves the QUERYABLE set**, which removes one of the
  three paths OPEN-12 is about (A-2 finding 9, contract §B.0b). While a field is QUERYABLE its value
  is recoverable by a `range` binary search over `hits.total` regardless of what the response filter
  strips, so narrowing QUERYABLE is the *only* thing that actually closes it. Update the QUERYABLE
  allowlist in the same change — leaving it in costs nothing operationally and keeps a live oracle
  open.

**Implementation notes**:
- Grep for `publicVisibility` and `dacApproval` across `../duos-ui/src/` before making changes
  to catch all callers.
- Coordinate on timing: do not remove the client-side filter until the corresponding server-side
  filter is confirmed live behind `ElasticSearchConfiguration.securityMode`.

**Dependencies**: F-1 live (or E-4 live behind feature flag).
**Size**: S

---

#### Ticket G-2 — Audit and update all duos-ui search callers

**Summary**: Review all eight search callers for assumptions about unfiltered server results and
update `elastic.ts` types to reflect the server-authoritative model.

**Context**: Search callers: `useLibraryData.ts`, `DatasetSearch.jsx`, `DatasetSearchTable.jsx`,
`DACDatasets.jsx`, `DatasetSubmissions.jsx`, `DatasetStatistics.tsx`, `StudyDetails.tsx`,
`BucketUtils.ts`. Any that inject access-policy-related filters or post-filter results on
access-policy grounds should be cleaned up.

**Acceptance criteria**:
- Each caller reviewed; any access-policy DSL filter that the server now handles is removed.
- No caller sends `_source`, `docvalue_fields`, or `script_fields` (stripped by E-1 in any case,
  but clean up at source).
- `elastic.ts` updated: remove any type fields that correspond to now-server-managed request
  surfaces.
- All callers compile and Cypress tests pass.

**Implementation notes**:
- `BucketUtils.ts:L337` — inspect the query for any visibility-related terms.
- `DACDatasets.jsx:L52` — document any DAC filtering it performs. The server does **not** grant read
  access from DAC membership or chair status; contract rows 9–10 defer that expansion.

**Dependencies**: G-1, F-1.
**Size**: M

---

#### Ticket G-3 — Update Cypress tests to reflect server-authoritative results

**Summary**: Update all Cypress component tests that intercept `/api/dataset/search/index/v2` to
use mock responses that reflect post-enforcement server output, and remove scenarios testing
now-removed client-side filtering.

**Context**: Tests under `cypress/component/` mock API responses that currently include
`publicVisibility=false` documents to verify client-side filtering. After the server enforces
access policy, those mock responses should represent what the server would actually return.

**Acceptance criteria**:
- Tests no longer assert that `publicVisibility=false` documents injected into mocked responses
  are hidden by client logic.
- Mock responses updated to represent only authorized documents (as the server would return).
- `DataLibrary.spec.tsx:L868–875` — `dacApproval: true` assertion in outgoing query construction
  removed or updated per G-1 outcome.
- All updated tests pass in CI.

**Implementation notes**:
- These are component tests (not E2E) — they test rendering against mocked HTTP. Update mock
  response bodies; assertions about rendered content can remain where the expected data is still
  correct.
- `cypress/component/DataSearch/dataset_search_table.spec.jsx` tests the `DatasetSearchTable`
  component directly — update both mock data and assertions.

**Dependencies**: G-2.
**Size**: M

---

#### Ticket G-4 — Update `DatasetSearchTable.jsx` for server-authoritative search

**Summary**: Remove any access-policy filter terms from `DatasetSearchTable.jsx` and align it
fully with the server-authoritative search flow.

**Context**: `DatasetSearchTable.jsx` is the active dataset search table implementation (at
`src/components/data_search/DatasetSearchTable.jsx`). It constructs and sends ES queries (L230).

**Acceptance criteria**:
- No access-policy filter terms injected by `DatasetSearchTable.jsx` (the L100–141 filter block
  handles UI-selected facets — `accessManagement`, `dataUse`, `dataType`, `dac` — which are
  user-selected filters and should be retained; only access-policy terms should be removed).
- Cypress test in `cypress/component/DataSearch/dataset_search_table.spec.jsx` updated.

**Dependencies**: G-2.
**Size**: M

---

### Epic H — Observability, Rollout, and Documentation

**Goal**: Feature flags for safe staged rollout, audit logging for access enforcement events,
shadow-mode comparison tooling, and documentation updates.

**Blocked by**: E live, and D additionally where licensed.
**Blocks**: Nothing (runs in parallel with G).

---

#### Ticket H-1 — Validate security-mode feature flags and startup behavior

**Summary**: Ensure `ElasticSearchConfiguration.securityMode` controls all modes cleanly and
the application fails fast on invalid configuration.

**Acceptance criteria**:
- `securityMode: none` → legacy behavior, no mediator, no per-request credentials.
- `securityMode: fallback` → `SearchQueryMediator` active; no per-request ES credentials.
- `securityMode: native-dls` → per-request DLS credentials active; mediator bypassed.
- `securityMode: shadow` → see H-3.
- Invalid value → application fails to start with a descriptive error message.
- `ElasticSearchHealthCheck` reports current `securityMode` in its health check output.

**Implementation notes**:
- Validate via a custom Dropwizard `@ValidSecurityMode` annotation or a startup lifecycle hook
  that checks `ElasticSearchConfiguration.securityMode` against an allowed-values set.
- Document new config keys in `docs/` or the config YAML schema.

**Dependencies**: D-1.
**Size**: S

---

#### Ticket H-2 — Audit logging and metrics for access enforcement

**Summary**: Add structured logging and metrics for: filtered-result queries, denied-field
accesses, reindex completion events, and credential creation failures.

**Acceptance criteria**:
- Log entry per search request: `userId`, `securityMode`, `resultCount`, `durationMs`. No PII
  (no email addresses, no query text).
- Log entry per reindex operation: `datasetId`, `triggeredBy`, `accessPolicyPopulated`, `durationMs`.
- Metrics: `dataset_search_filtered_total` (counter, by `securityMode`),
  `dataset_reindex_duration_seconds` (histogram).

**Implementation notes**:
- Use the existing Dropwizard `MetricRegistry` for metrics and SLF4J for structured log entries.
- `filteredCount` (documents removed by access policy vs. raw ES count) is only estimable in
  shadow mode (H-3) — omit from non-shadow log entries or approximate.

**Dependencies**: E-4 or D-4.
**Size**: M

---

#### Ticket H-3 — Staged rollout: shadow mode and cutover criteria

**Summary**: Run secured and legacy result counts in parallel (shadow mode) before full cutover;
define success criteria and rollback trigger.

**Context**: Shadow mode executes both the mediated/secured query and the current unmediated query
per request, compares result counts, and logs the difference — measuring enforcement impact before
it affects users.

**Acceptance criteria**:
- `securityMode: shadow` (H-1) — primary response is the unmediated (legacy) result; secured
  result count computed and difference logged.
- Success criteria documented: e.g. "secured count ≤ 5% below legacy for 48 hours with no
  false-positive reports from test accounts."
- Rollback trigger: if secured count diverges by > 10% from legacy, alert fires and `securityMode`
  can be toggled back to `"none"` via config without code deployment.

**Implementation notes**:
- Shadow mode requires two ES requests per search — acceptable during rollout, not for production
  steady-state. Log shadow comparisons at `DEBUG` with a dedicated logger (e.g.
  `es.shadow.search`) so they can be sampled via log-level config without flooding production.

**Dependencies**: H-2.
**Size**: S

---

#### Ticket H-4 — Update documentation for server-authoritative enforcement model

**Summary**: Update the inventory and plans documents to reflect the post-enforcement architecture.

**Acceptance criteria**:
- "Study-Level Visibility and Access" section updated: remove the security-implication note about
  client-side bypass; add description of server-enforced DLS/FLS or query mediation.
- Cross-reference matrix updated if any endpoint behavior changed.
- This plans document updated with final implementation decisions (path taken, what was deferred).

**Dependencies**: G-1, H-1.
**Size**: S

---

### Critical Path

```
A-1 → A-2 → A-3
               ↓                      ↓
    B-1→B-3→B-4→B-5→B-6         C-1→C-2→C-3
                 ↓                      ↓
       E-0→E-1→E-2→E-3→E-4  (always; E-2/E-4 fallback-specific if D is built)
                 ↓
       D-1→D-2→D-3→D-4→D-5  (additionally, where licensed AND OPEN-13 says build)
                          ↓
                       F-1 → [F-2 → F-3 (deferrable)]
                          ↓
              G-1→G-2→G-3→G-4    H-1→H-2→H-3→H-4
```

Rough total (native DLS/FLS path): ~14–18 backend-engineer weeks; ~3–4 frontend-engineer weeks;
~1–2 infra-engineer weeks. **Epic E can no longer be deferred when the cluster supports DLS/FLS** —
E-0, E-1 and E-3 are prerequisites of Epic D, not alternatives to it (see the A-2 outcome). What a
supporting cluster defers is only **E-2** and **E-4**, plus F-2/F-3 as before. The native-path total is
therefore closer to the combined figure than the original framing implied.

**If OPEN-13 resolves as contract §G recommends, D-1…D-5 leave the critical path entirely** and the
Epic E chain runs whole — E-2 and E-4 stop being fallback-specific, F-1 depends on E alone, and the
backend total drops by Epic D's five tickets (S + L + L/XL + M + M) plus the two-path parity work
folded into the estimates above. That is the larger of the two branches, so treat ~14–18 weeks as the
build-D figure rather than the expected one until the question is answered.

### Phase 0 — Pre-requisites and Contract

*Blocks all native-security work.*

| Task | Size | Owner |
| --- | --- | --- |
| 0.1 Confirm target Elasticsearch edition, DLS/FLS availability, API-key/run-as support, and operational model for per-request credentials | S | Infra + Backend |
| 0.2 Define formal access contract — **done and unblocked**: [`es-access-contract.md`](es-access-contract.md). Remaining OPEN items are proposed behavior changes, each defaulting to preserve-today, so Phase 1 can start | S → L | Backend (policy lead) |
| 0.3 Inventory storage gaps: determine which new dimensions (institution allowlists, explicit user/group lists, policy tags) lack persistent backing and decide whether they go in existing Study/Dataset properties, new DB tables, or external config | M | Backend + DB |

### Phase 1 — Index Schema and Indexing Pipeline

*~Parallel with Phase 0 once contract is defined. Steps 1.1→1.3 are sequential; 1.4–1.6 run in parallel after 1.3. Cancelled step 1.2 is retained below only as a decision record.*

| Task | Size | Owner |
| --- | --- | --- |
| 1.1 Add `accessPolicy` nested object to `DatasetTerm` carrying the DLS-needed fields the contract authorizes: `publicVisibility`, `hasStudy`, `datasetCreatorUserId`, `studyCreatorUserId`, `custodianEmails`, `dacId`. The speculative allowlist/tag fields are held back pending OPEN-5 | M | Backend |
| ~~1.2 Add field-access profile marker to `DatasetTerm` to control FLS~~ — **cancelled**: ES cannot select a field grant per document, and a request-wide caller-specific privileged bundle leaks fields from unrelated public documents. Search uses one allowlist for every caller (contract Decision 2) | — | — |
| 1.3 Update `ElasticSearchService.toDatasetTerm` and `toStudyTerm` to populate all new `accessPolicy` fields from Dataset/Study/User data | M | Backend (depends on 1.1 and 0.3) |
| 1.4 Update all reindex trigger paths (dataset registration, dataset update, study update, DAC externalization, explicit reindex endpoint) to ensure `accessPolicy` is always current | M | Backend (depends on 1.3) |
| 1.5 Design versioned index migration: new index name + Elasticsearch alias cutover + full background reindex strategy; write the reindex script/job | M | Backend + Infra (depends on 1.3) |
| 1.6 Update `ElasticSearchServiceTest` for new `accessPolicy` field population: add coverage per access dimension | M | Backend (depends on 1.3) |

### Phase 2 — Auth Context Service

*Parallel with Phase 1; needed by both native (3A) and fallback (3B) paths.*

| Task | Size | Owner |
| --- | --- | --- |
| 2.1 Create `DatasetSearchAuthContext` (or similar) with only the currently enforced inputs: userId, unnormalized email, and global roles. Do not resolve DAC, institution, allowlist, or policy-tag context until a signed-off requirement consumes it | M | Backend |
| 2.2 Normalize existing Consent read rules from `DatasetService.verifyPublicVisibilityAccess` / `canReadStudy` / `isCreatorOrCustodian` into a shared policy evaluator usable by both search mediation and native DLS role generation; avoid duplicating the logic | S | Backend (depends on 2.1) |
| 2.3 Unit-test auth context and policy evaluation for ADMIN, public reader, dataset creator, study creator, and custodian; add negative cases proving DAC membership/chair status and institution alone grant no access | M | Backend (depends on 2.1, 2.2) |

### Phase 3A — Native Elasticsearch DLS/FLS Path

*Depends on Phase 0.1 confirming cluster support, **and on 3B.1/3B.3 plus the server-owned aggregation
vocabulary** — see the A-2 outcome. 3A is a superset of the shared mediation work, not a substitute for
it, so it can no longer run "parallel with 3B during evaluation": the shared parts land first either
way.*

***Do not start this phase before OPEN-13 is answered.** Contract §G recommends deferring D-1…D-5 —
this whole phase — because 3B delivers the same predicate through E-2's injected filter. See the
callout at the head of Epic D. Phase 3B is unaffected and should proceed regardless.*

| Task | Size | Owner |
| --- | --- | --- |
| 3A.1 Extend `ElasticSearchConfiguration` with security-mode flag, impersonation/API-key settings, and the single SEARCH-VISIBLE field allowlist | S | Backend + Infra |
| 3A.2 Update `ElasticSearchSupport` to support per-request credential construction: either generate API keys with inline role descriptors or set run-as headers from a privileged service account. **The key must name the dataset index in exactly one role descriptor, and its owning user must hold no separate index privilege on it** — descriptors *union* their DLS queries and FLS grants rather than intersecting, so a second one silently removes document filtering (contract §B.7a, A-2 finding 10) | L | Backend (depends on 2.1, 3A.1) |
| 3A.3 Build role/query descriptor generator that translates `DatasetSearchAuthContext` into Elasticsearch DLS query (wrapping index's `accessPolicy` fields) and FLS field-grant list. **The grant is wider than the response allowlist** — it adds the QUERYABLE-but-internal paths and the `.keyword` subfields of sorted multi-fields, and must be generated from the index mapping rather than from the contract's field tables (contract §B.5c), resolving any `alias` to its concrete field name (contract §B.5d) | L | Backend (depends on 2.2, 3A.2, 3B.0, 3B.1, 3B.3) |
| 3A.4 Wire per-request credentials into `ElasticSearchService.searchDatasets` and `searchDatasetsStream` so they use the secured client rather than the shared service credential. **Mediation runs first** — `searchDatasets` currently sends raw caller DSL to `_validate/query` before the search, so per-request credentials do not help if unmediated input has already gone out under the service's own credential (contract §F.2a, A-2 finding 12) | M | Backend (depends on 3A.3) |
| 3A.5 Substitute the real components into the existing end-to-end harness (`ElasticSearchLeakDefensePocTest`) and seed through `indexDataset`; keep the unmediated configurations, the two-path parity assertion, the two "control removed" tests and the two DSL-surface sweeps, and re-run the mutation exercise against production code. **Add: role-descriptor count on the minted key, and B-1's mapping-shape constraints against the live mapping** — both failures are silent and neither is visible to a test written against a builder's return value (A-2 findings 10 and 11) | L → M | Backend + QA (depends on 3A.4) |

### Phase 3B — Query Mediation and Response Shaping

*Formerly "Compatibility Fallback". **3B.0, 3B.1 and 3B.3 are required in every configuration** and
must land before 3A.3; only 3B.2 and 3B.4 are specific to the unlicensed path. Ships whole where
DLS/FLS is unavailable.*

| Task | Size | Owner |
| --- | --- | --- |
| **3B.0 Build the server-owned aggregation vocabulary**: the three shapes the product actually issues, selected by `(tab, filters, page, size, sort)`, so callers never send `aggs`. Required on both paths — DLS does not isolate `significant_terms`' index-wide background count (contract §F.1). **Also validate the vocabulary itself before executing it** — `terms` field targets and `top_hits` `_source` leaves against RESPONSE-VISIBLE — since bucket keys cannot be filtered on the response side (A-2 finding 8, contract §F.2) | L | Backend |
| 3B.1 Build `SearchQueryMediator` that accepts client DSL, strips unsafe response-shaping surfaces at **every depth** (`_source`, `aggs`, `docvalue_fields`, `script_fields`, `explain`, `profile`, `runtime_mappings`, `collapse`, `inner_hits`), then **refuses any request member, query clause or clause value shape outside a closed allowlist** (A-2 finding 7 — `query_string`, `terms` lookup, `more_like_this`, `_script` sort, `knn` and `pit` all defeat a field-reference walk), validates the remaining field references against the QUERYABLE allowlist, and wraps the client query inside a server-built `bool` filter. Note `fields` is stripped as a response channel but **kept and validated** inside `multi_match`/`highlight` | M | Backend (depends on 3B.0) |
| 3B.2 Add mandatory authorization filter injection to `SearchQueryMediator` using `DatasetSearchAuthContext`: emit a `must` bool clause enforcing publicVisibility / no-study / dataset-creator / study-creator / custodian as terms queries against indexed `accessPolicy` fields. **No DAC or institution clause** — contract rows 9–12 are DEFERred and adding them expands authorization | L | Backend (depends on 2.1, 3B.1) |
| 3B.3a Apply the response filter to `searchDatasetsStream` as well — the v2 endpoint returns Elasticsearch's body verbatim as an `InputStream`, so this is a streaming filter rather than a response-object filter (A-2 finding 12) | M | Backend (depends on 3B.3) |
| 3B.3 Add the server-managed field allowlist (contract §B, one bundle for all callers) to **every response channel** — `hits._source`, `aggregations.**` `top_hits`, `highlight`, `fields`, `inner_hits` — dropping the `sort` array outright and leaving bucket keys to 3B.0, which is the only place they can be checked (A-2 finding 8); strip internal fields server-side, not in the client, and apply it to admins too. **Required on the native path as well** (contract §B.5c) | M → L | Backend (depends on 3B.1) |
| 3B.4 Wire `SearchQueryMediator` into both `searchDatasets` and `searchDatasetsStream` in `ElasticSearchService`, **as the first thing either does** — today `searchDatasets` sends the raw caller DSL to `_validate/query` before building the search request, so unmediated input already reaches the cluster once (A-2 finding 12). Run validation on the mediated body or delete it. Forward no caller-supplied URL parameter, index name or path, and regression-test that | S → M | Backend (depends on 3B.2, 3B.3) |
| 3B.5 Unit-test `SearchQueryMediator` for each enforced dimension, add negative DAC/institution cases, and confirm INTERNAL fields are absent from responses rather than blanked on the client side | M | Backend (depends on 3B.4) |

### Phase 4 — API Hardening and Long-term Contract

*Depends on 3B being live, and on 3A additionally where the cluster is licensed.*

| Task | Size | Owner |
| --- | --- | --- |
| 4.1 Harden existing `/api/dataset/search/index` and `/api/dataset/search/index/v2` in `DatasetResource` to always pass `duosUser` into the mediated/secured search path; remove the current passthrough-to-raw-ES behavior | S | Backend (depends on 3B, and 3A where licensed) |
| 4.2 Design server-owned search API that accepts business parameters instead of raw Elasticsearch DSL (filters, pagination, sort, text query) and returns shaped response; scope as v3 endpoint. **3B.0 is its first increment** — the `aggs` half is already being built, so this is finishing the `query` half, and doing so retires the mediator, the field validator and the response-channel walker entirely (contract §F.1a, OPEN-11). **A-2 finding 9 is a second, independent argument**: while callers author DSL, a QUERYABLE field's value is recoverable by binary search over `hits.total`, so "QUERYABLE but RESPONSE-INTERNAL" cannot hold. Deriving "My Data Submissions" and `restrictToPublicVisibility` from the caller's identity server-side closes that, and retires OPEN-8, contract §B.5c's widened grant and E-3's presence on the native path with it (contract §B.0b, OPEN-12) | L | Backend *(recommend promoting from deferrable)* |
| 4.3 Update `DatasetResource` to expose the v3 search endpoint and maintain backward compatibility for v1/v2 during migration | M | Backend (depends on 4.2) |

### Phase 5 — Frontend Alignment

*Depends on 4.1 or 3B live. Step 5.1 can ship as soon as backend enforcement lands.*

| Task | Size | Owner |
| --- | --- | --- |
| 5.1 Identify and remove client-side `publicVisibility` and `dacApproval` filtering from `datasetAsset.ts`; replace with trust in server-filtered results | S | Frontend |
| 5.2 Audit all duos-ui search callers for hard-coded DSL filters that assume server returns unfiltered sets; update callers and types in `elastic.ts` | M | Frontend (depends on 4.1 or 3B being live) |
| 5.3 Update Cypress component tests and unit tests that intercept `/api/dataset/search/index/v2` to reflect server-authoritative result sets; remove mocked private-dataset scenarios that were testing client filtering | M | Frontend (depends on 5.2) |
| 5.4 Update `DatasetSearchTable.jsx` (`src/components/data_search/DatasetSearchTable.jsx`) to remove any remaining client-side visibility/approval filters and align it with the server-authoritative search methodology (same as 5.1/5.2 scope). Note: `src/components/DataSearch/dataset_search_table.tsx` does not exist in the codebase; `DatasetSearchTable.jsx` is the active implementation. | M | Frontend |

### Phase 6 — Observability, Rollout, and Documentation

| Task | Size | Owner |
| --- | --- | --- |
| 6.1 Add feature flags to `ElasticSearchConfiguration` for native DLS/FLS mode, compatibility fallback mode, and strict field filtering; ensure the application starts cleanly in each mode | S | Backend + Infra |
| 6.2 Add audit logging and metrics for: filtered-result queries, denied-field accesses, credential creation failures, and reindex completion events | M | Backend |
| 6.3 Staged rollout plan: run secured and legacy result counts in parallel (shadow mode) before full cutover; define success criteria and rollback trigger | S | Backend + Infra |
| 6.4 Update this document's security and overlap sections to reflect the new server-authoritative enforcement model | S | Backend |

### Critical Path

Phase 0.1 → Phase 0.2/0.3 → Phase 1.1–1.3 / Phase 2.1–2.2 (parallel) → Phase 3B.0/3B.1/3B.3 →
Phase 3A (where licensed, **and only if OPEN-13 says build**) → Phase 4.1 → Phase 5.1 → Phase 6.1–6.3

Rough total (native DLS/FLS path): ~14–18 backend-engineer weeks end-to-end across phases;
~3–4 frontend-engineer weeks; ~1–2 infra-engineer weeks for cluster security setup and rollout
support. **Only 3B.2, 3B.4 and 4.3 can be deferred if the cluster unambiguously supports DLS/FLS** —
3B.0, 3B.1 and 3B.3 cannot, and 4.2 is now recommended rather than deferred. The earlier claim that
"Phases 3B and 4.2/4.3 can be deferred" was wrong, and it was the estimate most affected by the A-2
proof of concept. **If OPEN-13 resolves toward deferring Epic D, nothing in 3B is deferrable at all**
— 3B.2 and 3B.4 become the enforcement rather than the fallback, and Phase 3A drops out of the total.

### Decisions

- **Recommended target architecture**: server-owned aggregations, query mediation and response-channel
  field allowlisting over indexed `accessPolicy` metadata, with the authorization predicate injected
  into every query. Native Elasticsearch DLS/FLS — backend-generated per-request auth context carrying
  that same predicate — sits **on top of** that as defence in depth where licensed.
- **Whether to build the native layer at all is open (OPEN-13).** This bullet used to read "native
  Elasticsearch DLS/FLS now", which A-2 overtook: measurement moved statistics isolation, field
  projection and query-surface control off the native path, leaving Epic D with one unique
  contribution — enforcement bound to the *credential* rather than to our endpoint. Contract §G
  recommends **deferring D-1…D-5 pending a deliberate answer**, not cancelling them. The question that
  settles it: will anything other than this endpoint ever query the index with the service's
  credentials? If yes, Epic D is not redundant and the deferral is wrong.
- **Required in every configuration**: server-owned aggregations, query mediation, and response-channel
  field allowlisting. Originally scoped as a fallback for clusters without DLS/FLS; A-2's proof of
  concept measured Epic D to be insufficient without them, on two independent grounds (contract §1.1,
  §B.5c). Native DLS/FLS is a component of the enforcement, not the whole of it.
- **Every Elasticsearch behavior this plan relies on is measured, not cited.**
  `ElasticSearchLeakDefensePocTest` exercises the design end-to-end against a real cluster, and three
  of the five behavioral claims the contract originally made turned out to be wrong. Any future claim
  about what DLS or FLS does should be added to that suite rather than to a document.
- **Included scope**: `publicVisibility`, creator, and custodian enforcement on the server side;
  explicit inventory and deferral of institution, DAC, principal-allowlist, and policy-tag access;
  streaming endpoint behavior, reindex strategy, testing, and duos-ui alignment. Deferred dimensions
  become enforcement scope only after their OPEN decision is approved.
- **Excluded scope**: redesign of non-search Consent endpoints, unrelated UI behavior changes, and
  implementation of arbitrary policy-authoring UX unless policy storage gaps force a minimal
  admin/data-model addition.
- **Critical assumption**: the target Elasticsearch environment supports the native security
  features needed for DLS/FLS. If not, Epic D is skipped and Epic E ships whole — but E-0/E-1/E-3 are
  the first implementation milestone either way, so this assumption no longer gates the start of work,
  only Epic D's inclusion. **And it is no longer the only thing that gates it**: OPEN-13 asks whether
  Epic D is worth building even where the license allows it, so a "yes" on this assumption is a
  necessary rather than a sufficient reason to proceed with Epic D.

### Further Considerations

1. **Search API direction**: Option A — harden existing raw-DSL endpoints first for compatibility.
   Option B — add a server-owned search API in parallel and migrate clients over time.
   Recommendation: do both, but treat the server-owned API as the long-term destination.
2. **Field-level policy granularity**: the current contract deliberately has one SEARCH-VISIBLE
   bundle for every caller. Role/profile-based or policy-tag-driven field exposure would be
   document-scoped for creators and custodians and therefore cannot be implemented safely by native
   FLS in a single search. If that requirement appears, reopen Decision 2 and use application-owned
   per-document projection rather than adding another FLS profile.
3. **Institution restrictions source of truth**: Option A — user institution alone. Option B —
   institution plus library-card or other status-derived qualifiers. Recommendation: separate
   identity context from eligibility state so document policy remains stable even if login checks
   change.