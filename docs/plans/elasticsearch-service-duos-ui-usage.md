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

### UserTerm and DacTerm nested fields

- `submitter` / `updateUser` (`UserTerm`): `userId`, `displayName`, `institution`
- `dac` (`DacTerm`): `dacId`, `dacName`, `dacEmail`

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
caller-specific access policy, not just query syntax. The recommended path is native Elasticsearch
document-level security (DLS) and field-level security (FLS) with backend-generated per-request
search credentials or impersonation context, backed by explicit access metadata in indexed dataset
documents. Because the current API accepts arbitrary raw Elasticsearch DSL through a shared service
credential, the plan also includes a compatibility fallback: server-owned query rewriting and
response shaping while preserving current endpoints.

Size key: **S** ≈ 1 day, **M** ≈ 2–3 days, **L** ≈ 4–5 days, **XL** ≈ 1 week+

### Epic Summary

| Epic | Name | Owner | Blocked by | Blocks |
| --- | --- | --- | --- | --- |
| A | Discovery & Contract | Infra + Backend | — | B, C, D, E |
| B | Index Schema & Indexing Pipeline | Backend | A | D, E, F |
| C | Auth Context Service | Backend | A | D, E |
| D | Native DLS/FLS Path | Backend + Infra | A, B, C | F |
| E | Compatibility Fallback | Backend | B, C | F |
| F | API Hardening | Backend | D or E | G |
| G | Frontend Alignment | Frontend | F | — |
| H | Observability, Rollout & Docs | Backend + Infra | D or E | — |

---

### Epic A — Discovery & Contract

**Goal**: Establish facts about the Elasticsearch cluster's security capabilities, evaluate the
local developer configuration changes needed, and define the formal access contract that all later
epics are built on. All other epics are blocked on A-1.

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
- Decision documented: native DLS/FLS path (Epic D), compatibility fallback (Epic E), or both.

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

**Acceptance criteria**:
- Completed matrix: for each dimension (`publicVisibility`, ADMIN bypass, creator, custodian, DAC
  member/chair, institution allowlist, policy tags) record: data source, enforcement level (DLS
  filter vs. FLS field bundle), and whether persistent backing currently exists.
- Field-level security groupings decided: e.g. `"public"` profile grants `datasetName`,
  `datasetId`, `study.studyName`; `"privileged"` additionally grants `study.dataCustodianEmail`,
  `study.dataSubmitterEmail`.
- `publicVisibility` DLS semantics decided: invisible to non-privileged callers, or visible with
  field redaction?

**Implementation notes**:
- Start from the `StudyTerm` fields listed in the Indexed Elements section of this document. Flag
  every field containing PII or internal-only data.
- `dataCustodianEmail` is currently parsed from the study property bag in
  `DatasetService.isCreatorOrCustodian` (L224–238), not a dedicated DB column — note this as a
  storage gap candidate.

**Dependencies**: A-1.
**Size**: S

---

#### Ticket A-0 — Evaluate local developer Elasticsearch configuration changes

**Summary**: Determine what changes are required to the local developer Elasticsearch setup in
`config/docker-compose.yaml` to support development and testing of the security work across all
epics.

**Context**: The local developer environment (`config/docker-compose.yaml`) runs
`docker.elastic.co/elasticsearch/elasticsearch:9.3.3` with security explicitly disabled:
- `xpack.security.enabled=false`
- `xpack.security.transport.ssl.enabled=false`
- `discovery.type=single-node`

ES 9.x ships with X-Pack Security built in and fully supports DLS, FLS, and API keys when
security is enabled. The native DLS/FLS path (Epic D) requires security to be enabled for local
development and testing. The compatibility fallback (Epic E) operates entirely at the application
layer and requires no Elasticsearch configuration changes.

**Acceptance criteria**:
- Delta documented between local config (security disabled, 9.3.3, single-node) and cloud
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
    image: docker.elastic.co/elasticsearch/elasticsearch:9.3.3
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
- `dacId` already exists on `Dataset`; `dataCustodianEmail` already exists in the study property
  bag — no new storage needed for those dimensions.
- `allowedPrincipalIds` (explicit user allowlists) and `policyTags` (consent-code-based access
  tags) are the most likely to require new storage.

**Dependencies**: A-2.
**Size**: M

---

### Epic B — Index Schema & Indexing Pipeline

**Goal**: Add an `accessPolicy` nested object to every indexed dataset document and ensure all
reindex paths populate it correctly. Foundation for both Epic D (native DLS/FLS) and Epic E
(fallback).

**Blocked by**: A-2, A-3.
**Blocks**: D, E.

---

#### Ticket B-1 — Add `accessPolicy` nested object to `DatasetTerm`

**Summary**: Define `AccessPolicyTerm` and add it as a nested field on `DatasetTerm`.

**Context**: `DatasetTerm` is assembled in `ElasticSearchService.toDatasetTerm` (L429–515).
Currently it carries no structured access metadata; all visibility logic lives in application code.
This ticket adds the schema without populating it yet (population is B-3).

**Acceptance criteria**:
- New `AccessPolicyTerm` class with fields:
  - `publicVisibility: boolean`
  - `creatorUserId: Integer`
  - `creatorEmail: String`
  - `custodianEmails: List<String>`
  - `dacId: Integer`
  - `dacApproval: Boolean`
  - `allowedInstitutionIds: List<Integer>`
  - `allowedPrincipalIds: List<Integer>`
  - `policyTags: List<String>`
  - `fieldAccessProfile: String` — `"public"` or `"privileged"`
- `DatasetTerm` gains an `accessPolicy: AccessPolicyTerm` field.
- Elasticsearch index mapping updated with `accessPolicy` as a `nested` (or `object`) type —
  confirm with A-1 outcome which is required for the DLS query approach.
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

#### Ticket B-2 — Add `fieldAccessProfile` marker to `DatasetTerm`

**Summary**: Add a `fieldAccessProfile` string field (inside `AccessPolicyTerm`) to signal which
FLS field bundle applies to the document.

**Context**: FLS in Elasticsearch requires knowing which fields each document grants to which
caller profile. A profile marker on the document allows the auth context (C-1) to request a
field-grant list matched to the document's declared profile, without enumerating fields per
document in the query path.

**Acceptance criteria**:
- `AccessPolicyTerm.fieldAccessProfile` present (this may overlap with B-1; keep as a separate
  deliverable to track separately).
- Valid values: `"public"` (`publicVisibility=true`), `"privileged"` (`publicVisibility=false` or
  restricted fields present).
- Unit test: `fieldAccessProfile` is `"public"` for a dataset whose study has
  `publicVisibility=true`, and `"privileged"` for `publicVisibility=false`.

**Dependencies**: B-1.
**Size**: S

---

#### Ticket B-3 — Populate `accessPolicy` in `toDatasetTerm` / `toStudyTerm`

**Summary**: Update `ElasticSearchService.toDatasetTerm` (L429) and `toStudyTerm` (L245) to read
all `accessPolicy` fields from `Dataset`, `Study`, and `User` objects.

**Context**: `ElasticSearchService` already injects `datasetDAO`, `userDAO`, `dacDAO`, `studyDAO`,
and `institutionDAO` (L58–66). All data needed for `accessPolicy` is reachable — this ticket wires
the mapping.

**Acceptance criteria**:
- `accessPolicy.publicVisibility` ← `dataset.getStudy().getPublicVisibility()` (null-safe).
- `accessPolicy.creatorUserId` ← `dataset.getCreateUserId()`.
- `accessPolicy.creatorEmail` ← `userDAO.getUserById(createUserId).getEmail()`.
- `accessPolicy.custodianEmails` ← parsed from study property bag using the same logic as
  `DatasetService.isCreatorOrCustodian` (L224–238).
- `accessPolicy.dacId` ← `dataset.getDacId()`.
- `accessPolicy.dacApproval` ← `dataset.getDacApproval()`.
- `accessPolicy.fieldAccessProfile` ← `"public"` if `publicVisibility=true`, else `"privileged"`.
- `allowedInstitutionIds` ← per outcome of A-3; empty list if not yet implemented.
- Unit test: null study → `accessPolicy.publicVisibility` defaults to `false`, no NPE.

**Implementation notes**:
- `isCreatorOrCustodian` in `DatasetService` (L224–238) parses custodian email from
  `study.getProperties()`. Do not call `DatasetService` from `ElasticSearchService` to avoid a
  circular dependency — extract a private helper `parseCustodianEmails(Study study)` in
  `ElasticSearchService` instead.
- Guard all `study` accesses — datasets created outside the registration flow may have a null
  study reference.

**Dependencies**: B-1, B-2, A-3.
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

**Dependencies**: B-3.
**Size**: M

---

#### Ticket B-6 — Unit-test `accessPolicy` population in `ElasticSearchServiceTest`

**Summary**: Add comprehensive unit tests for `accessPolicy` field population in `toDatasetTerm`
and `toStudyTerm` before any auth enforcement code is written against them.

**Acceptance criteria**:
- `publicVisibility=true` → `fieldAccessProfile == "public"`.
- `publicVisibility=false` → `fieldAccessProfile == "privileged"`.
- `custodianEmails` populated when study has `dataCustodianEmail` property.
- `custodianEmails` is empty (not null) when study has no custodian property.
- Null study → `publicVisibility` defaults to `false`, no NPE.
- `dacId`, `dacApproval`, `creatorUserId`, `creatorEmail` flow through from dataset and user DAO.

**Implementation notes**:
- Mirror the existing mock-heavy pattern in `ElasticSearchServiceTest` — mock all DAO calls.
- Use `@ParameterizedTest` for the `fieldAccessProfile` cases.

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
  - `Integer userId`
  - `String userEmail`
  - `Integer institutionId`
  - `boolean isAdmin`
  - `Set<Integer> dacMemberships` — all DAC IDs the user belongs to
  - `Set<Integer> dacChairScopes` — DAC IDs where the user is chair
  - `List<String> policyTagGrants` — initially empty, placeholder for future policy-tag grants
- `DatasetSearchAuthContextResolver` service: accepts a `DuosUser`, returns a
  `DatasetSearchAuthContext`.
- `isAdmin` is `true` when user has `UserRoles.ADMIN` (L13 in `UserRoles.java`).
- `dacMemberships` loaded from `DacDAO` — do not pull in `DacService` as a dependency to keep the
  graph flat.

**Implementation notes**:
- `DuosUser.getRoles()` returns the role set; check for `UserRoles.ADMIN`.
- Keep the resolver stateless; all DB calls happen eagerly in the constructor/factory, not lazily.
- `DacService` (L51) already resolves DAC memberships — use `dacDAO` directly to avoid
  introducing a circular dependency through `DacService`.

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
- Test matrix: `ADMIN`, public reader (`RESEARCHER`/`MEMBER`/`SIGNINGOFFICIAL`), dataset creator,
  study custodian, DAC chair for the dataset's DAC, DAC member (not chair),
  institution-restricted user, user with no matching institution.
- Each combination tested for `canRead`, `isCreator`, `isCustodian`.
- Edge cases: null study, dataset with no DAC, custodian email list empty, user with multiple
  roles.

**Implementation notes**:
- Use `@ParameterizedTest` with a method source building `DatasetSearchAuthContext` +
  `AccessPolicyTerm` pairs with expected `canRead` outcomes.
- Mock `DacDAO` in `DatasetSearchAuthContextResolver` tests to control DAC membership data.

**Dependencies**: C-1, C-2.
**Size**: M

---

### Epic D — Native Elasticsearch DLS/FLS Path

**Goal**: Implement per-request Elasticsearch credentials (API keys with inline role descriptors
or `run_as` headers) that enforce DLS and FLS natively at the cluster level.

**Blocked by**: A-1 (cluster capability confirmed), B-3 (`accessPolicy` indexed), C-1 (auth
context available).
**Blocks**: F.
**Condition**: Proceed only if A-1 confirms DLS/FLS support.

---

#### Ticket D-1 — Extend `ElasticSearchConfiguration` with security-mode settings

**Summary**: Add `securityMode`, privileged service-account credential fields, and
`fieldAccessProfiles` to `ElasticSearchConfiguration`.

**Context**: `ElasticSearchConfiguration` currently holds a single shared `authUser`/`authPassword`
(L22–24). The native path requires either a privileged account for API-key generation or a
`run_as`-capable service account.

**Acceptance criteria**:
- New fields:
  - `String securityMode` — `"none"`, `"fallback"`, `"shadow"`, or `"native-dls"`.
  - `String serviceAccountUser` / `String serviceAccountPassword` — may reuse `authUser`/
    `authPassword` if the same account has sufficient privilege.
  - `Map<String, List<String>> fieldAccessProfiles` — maps profile name to list of allowed field
    glob patterns (e.g. `"public"` → `["datasetId", "datasetName", "study.studyName", ...]`).
- Application starts cleanly with `securityMode: none` (legacy behavior unchanged).
- Startup validation: if `securityMode` is `"native-dls"` and `serviceAccountUser` is blank,
  throw with a descriptive error.

**Implementation notes**:
- Use Dropwizard `@JsonProperty` / `@NotNull` pattern consistent with existing fields.
- `fieldAccessProfiles` default should include at minimum `"public"` and `"privileged"` entries
  reflecting the field lists decided in A-2.
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
- Unit test: given `isAdmin=true`, generated credential grants unrestricted access.
- Unit test: given non-admin context, credential includes DLS query and FLS field list.

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

**Context**: The DLS query must express: return documents where `accessPolicy.publicVisibility=true`
OR `accessPolicy.creatorUserId = <userId>` OR `accessPolicy.custodianEmails` contains `<email>`
OR `accessPolicy.dacId` is in `<dacMemberships>`. ADMIN bypasses all filters.

**Acceptance criteria**:
- `DlsQueryBuilder.buildForContext(DatasetSearchAuthContext ctx)` → JSON string.
  - ADMIN → `{"match_all": {}}`.
  - Non-admin → `bool` with `should` clauses for `publicVisibility`, creator, custodian, DAC
    membership; `minimum_should_match: 1`.
- `FlsGrantBuilder.buildForContext(DatasetSearchAuthContext ctx,
  Map<String, List<String>> profiles)` → `List<String>` field patterns.
  - ADMIN → `["*"]`.
  - Non-admin → field list from the caller's applicable profile.
- Unit tests for each access dimension and combinations.

**Implementation notes**:
- Example DLS query for non-admin:
  ```json
  {"bool": {"should": [
    {"term": {"accessPolicy.publicVisibility": true}},
    {"term": {"accessPolicy.creatorUserId": 42}},
    {"terms": {"accessPolicy.custodianEmails": ["user@example.com"]}},
    {"terms": {"accessPolicy.dacId": [1, 3]}}
  ], "minimum_should_match": 1}}
  ```
- If `accessPolicy` is mapped as `nested`, terms must be wrapped in a `nested` query — confirm
  with B-1 mapping decision.

**Dependencies**: D-2, C-2, B-1.
**Size**: L

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

**Acceptance criteria**:
- Setup: security-enabled ES (Docker/Testcontainers), two datasets: one `publicVisibility=true`,
  one `publicVisibility=false`.
- Test: non-admin search → `publicVisibility=false` document absent from results.
- Test: dataset creator search → sees own `publicVisibility=false` document.
- Test: ADMIN → sees all documents.
- Test: FLS — non-privileged caller's response does not contain `study.dataCustodianEmail`.
- Test: ADMIN response contains all fields.

**Implementation notes**:
- Use `testcontainers` with `docker.elastic.co/elasticsearch/elasticsearch:8.x` and
  `xpack.security.enabled=true`.
- Seed test data via `ElasticSearchService.indexDataset` (not direct ES API) to exercise the same
  code path as production and ensure `accessPolicy` is populated.

**Dependencies**: D-4.
**Size**: L

---

### Epic E — Compatibility Fallback

**Goal**: Server-side query mediation and field allowlisting that enforce access policy without
native Elasticsearch DLS/FLS. Ships independently of Epic D and becomes the primary Phase 3
delivery if the cluster lacks DLS/FLS support.

**Blocked by**: B-3 (`accessPolicy` indexed), C-1 (auth context available).
**Blocks**: F.

---

#### Ticket E-1 — Build `SearchQueryMediator` with DSL sanitization

**Summary**: Implement `SearchQueryMediator` that strips unsafe surfaces from client-provided ES
DSL before execution.

**Context**: `DatasetResource.searchDatasetIndex` (L425) currently passes the client request body
directly to `ElasticSearchService.searchDatasets` with no sanitization. A client can include
`_source` overrides, `script_fields`, `explain`, or `profile` to extract information about
documents it should not see.

**Acceptance criteria**:
- `SearchQueryMediator.sanitize(String clientDsl)` → `String`:
  - Removes top-level keys: `_source`, `docvalue_fields`, `script_fields`, `stored_fields`,
    `explain`, `profile`, `seq_no_primary_term`, `version`.
  - Preserves: `query`, `aggs`/`aggregations`, `sort`, `size`, `from`, `search_after`, `pit`,
    `highlight`.
  - Result is valid JSON.
- Unit test: sanitized DSL contains none of the stripped keys.
- Unit test: DSL with only `query` and `size` is returned unchanged.

**Implementation notes**:
- Parse using Jackson `ObjectNode`: `((ObjectNode) root).remove(List.of("_source", "explain",
  ...))` — clean and avoids manual JSON manipulation.
- Keep sanitization separate from auth filter injection (E-2) for independent testability.

**Dependencies**: C-1.
**Size**: M

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
- Unit tests for each access dimension: public reader, creator, custodian, DAC member.
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

**Summary**: Strip fields from Elasticsearch response documents that the caller is not permitted
to see, based on their `fieldAccessProfile`.

**Context**: The fallback path cannot rely on native FLS. The server must remove restricted fields
from hit `_source` objects before returning the response.

**Acceptance criteria**:
- `ResponseFieldFilter.applyProfile(String responseJson, String callerProfile,
  Map<String, List<String>> profileDefs)` → `String`:
  - For each hit in `hits.hits[*]._source`, removes fields not in the caller's profile grant list.
  - ADMIN profile → no fields removed.
  - `"public"` profile → only fields in the `"public"` grant list retained.
- Unit test: response with `study.dataCustodianEmail` has that field stripped for `"public"`
  profile caller.
- Unit test: ADMIN caller receives the full document.

**Implementation notes**:
- Walk `hits.hits[*]._source` as `Map<String, Object>` and apply a recursive filter against the
  profile's allowed field glob patterns (e.g. `"study.*"` allows all `study` sub-fields).
- The caller's profile derives from `DatasetSearchAuthContext.isAdmin` → `"admin"`, otherwise
  use the document's `accessPolicy.fieldAccessProfile` or a per-caller override from config.
- The `profileDefs` map comes from `ElasticSearchConfiguration.fieldAccessProfiles` (D-1).

**Dependencies**: E-2, D-1.
**Size**: M

---

#### Ticket E-4 — Wire `SearchQueryMediator` into `ElasticSearchService`

**Summary**: Wire the mediator (E-1/E-2) and response filter (E-3) into `searchDatasets` and
`searchDatasetsStream` when `securityMode` is `"fallback"`.

**Acceptance criteria**:
- When `securityMode == "fallback"`:
  - Client DSL processed by `SearchQueryMediator.mediate(clientDsl, ctx)` before ES submission.
  - Response processed by `ResponseFieldFilter.applyProfile(...)` before returning to caller.
- When `securityMode == "none"`: existing behavior unchanged.
- Both `searchDatasets` (L212) and `searchDatasetsStream` (L230) updated.
- `DatasetResource` callers at L425 and L439 updated to pass `duosUser` → resolved
  `DatasetSearchAuthContext`.

**Implementation notes**:
- `searchDatasetsStream` returns an `InputStream` — apply the field filter by reading the stream,
  filtering the JSON, then re-wrapping as an `InputStream` before returning; or convert to
  `String` internally and stream the result.
- Keep the `securityMode` switch as a simple if-else in `ElasticSearchService`.

**Dependencies**: E-2, E-3.
**Size**: S

---

#### Ticket E-5 — Unit-test `SearchQueryMediator` and `ResponseFieldFilter`

**Summary**: Comprehensive unit tests covering all access dimensions and edge cases for the
mediator and response filter.

**Acceptance criteria**:
- `SearchQueryMediator` tests: DSL passthrough for ADMIN, correct `bool.must` injection for each
  non-admin role/dimension.
- `ResponseFieldFilter` tests: field stripping per profile, ADMIN bypass, nested field handling
  (`study.dataCustodianEmail`), null/missing source fields are not errors.
- Negative test: client DSL with injected `_source` override does not expose restricted fields
  after full mediation pipeline.

**Implementation notes**:
- Use `JSONAssert` or Jackson-based assertions for comparing query structure.
- Test the full pipeline end-to-end: `mediate(...)` → mock response JSON → `applyProfile(...)` →
  assert final field set.

**Dependencies**: E-4.
**Size**: M

---

### Epic F — API Hardening

**Goal**: Harden the existing search endpoints to always route through the secured/mediated path,
and design the long-term server-owned search API.

**Blocked by**: Epic D or E (at least one live).
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
- `DACDatasets.jsx:L52` — verify it does not filter by DAC membership client-side (the server
  handles this via auth context for CHAIRPERSON callers after F-1).

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

**Blocked by**: D or E live.
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
    B-1→B-2→B-3→B-4→B-5→B-6    C-1→C-2→C-3
                 ↓                      ↓
          D-1→D-2→D-3→D-4→D-5  (or  E-1→E-2→E-3→E-4→E-5)
                          ↓
                       F-1 → [F-2 → F-3 (deferrable)]
                          ↓
              G-1→G-2→G-3→G-4    H-1→H-2→H-3→H-4
```

Rough total (native DLS/FLS path): ~14–18 backend-engineer weeks; ~3–4 frontend-engineer weeks;
~1–2 infra-engineer weeks. Epics E, F-2, and F-3 can be deferred if the cluster unambiguously
supports DLS/FLS.

### Phase 0 — Pre-requisites and Contract

*Blocks all native-security work.*

| Task | Size | Owner |
| --- | --- | --- |
| 0.1 Confirm target Elasticsearch edition, DLS/FLS availability, API-key/run-as support, and operational model for per-request credentials | S | Infra + Backend |
| 0.2 Define formal access contract: enumerate all dimensions (publicVisibility, ADMIN, creator, custodian, DAC, institution, allowlist, policy tags) and decide which are document-level vs. field-level | S | Backend (policy lead) |
| 0.3 Inventory storage gaps: determine which new dimensions (institution allowlists, explicit user/group lists, policy tags) lack persistent backing and decide whether they go in existing Study/Dataset properties, new DB tables, or external config | M | Backend + DB |

### Phase 1 — Index Schema and Indexing Pipeline

*~Parallel with Phase 0 once contract is defined. Steps 1.1→1.2→1.3 are sequential; 1.4–1.6 parallel after 1.3.*

| Task | Size | Owner |
| --- | --- | --- |
| 1.1 Add `accessPolicy` nested object to `DatasetTerm` carrying all DLS-needed fields: `publicVisibility`, `creatorUserId`, `creatorEmail`, `custodianEmails`, `datasetCreatorUserId`, `dacId`, `dacApproval`, `allowedInstitutionIds`, `allowedPrincipalIds`, `policyTags` | M | Backend |
| 1.2 Add field-access profile marker to `DatasetTerm` to control FLS (e.g. `fieldAccessProfile: "public" \| "privileged"`) | S | Backend (depends on 1.1) |
| 1.3 Update `ElasticSearchService.toDatasetTerm` and `toStudyTerm` to populate all new `accessPolicy` fields from Dataset/Study/User data | M | Backend (depends on 1.1 and 0.3) |
| 1.4 Update all reindex trigger paths (dataset registration, dataset update, study update, DAC externalization, explicit reindex endpoint) to ensure `accessPolicy` is always current | M | Backend (depends on 1.3) |
| 1.5 Design versioned index migration: new index name + Elasticsearch alias cutover + full background reindex strategy; write the reindex script/job | M | Backend + Infra (depends on 1.3) |
| 1.6 Update `ElasticSearchServiceTest` for new `accessPolicy` field population: add coverage per access dimension | M | Backend (depends on 1.3) |

### Phase 2 — Auth Context Service

*Parallel with Phase 1; needed by both native (3A) and fallback (3B) paths.*

| Task | Size | Owner |
| --- | --- | --- |
| 2.1 Create `DatasetSearchAuthContext` (or similar) that resolves an authenticated `DuosUser` into: userId, email, institutionId, global roles, DAC memberships by dacId, DAC chair scopes, and any policy-tag grants | M | Backend |
| 2.2 Normalize existing Consent read rules from `DatasetService.verifyPublicVisibilityAccess` / `canReadStudy` / `isCreatorOrCustodian` into a shared policy evaluator usable by both search mediation and native DLS role generation; avoid duplicating the logic | S | Backend (depends on 2.1) |
| 2.3 Unit-test auth context for each role/dimension combination: ADMIN, public reader, creator, custodian, DAC chair, institution-restricted, explicit allowlist | M | Backend (depends on 2.1, 2.2) |

### Phase 3A — Native Elasticsearch DLS/FLS Path

*Depends on Phase 0.1 confirming cluster support. Run parallel with 3B during evaluation.*

| Task | Size | Owner |
| --- | --- | --- |
| 3A.1 Extend `ElasticSearchConfiguration` with security-mode flag, impersonation/API-key settings, and field-security profile definitions | S | Backend + Infra |
| 3A.2 Update `ElasticSearchSupport` to support per-request credential construction: either generate API keys with inline role descriptors or set run-as headers from a privileged service account | L | Backend (depends on 2.1, 3A.1) |
| 3A.3 Build role/query descriptor generator that translates `DatasetSearchAuthContext` into Elasticsearch DLS query (wrapping index's `accessPolicy` fields) and FLS field-grant list | L | Backend (depends on 2.2, 3A.2) |
| 3A.4 Wire per-request credentials into `ElasticSearchService.searchDatasets` and `searchDatasetsStream` so they use the secured client rather than the shared service credential | M | Backend (depends on 3A.3) |
| 3A.5 Add integration tests against a security-enabled Elasticsearch instance to validate DLS and FLS enforcement: document filtering, field omission, and admin bypass | L | Backend + QA (depends on 3A.4) |

### Phase 3B — Compatibility Fallback

*Parallel with 3A; becomes the primary Phase 3 if cluster cannot support DLS/FLS. Can ship independently.*

| Task | Size | Owner |
| --- | --- | --- |
| 3B.1 Build `SearchQueryMediator` that accepts client DSL, strips unsafe response-shaping surfaces (`_source`, `docvalue_fields`, `script_fields`, `explain`, `profile`), and wraps the client query inside a server-built `bool` filter | M | Backend |
| 3B.2 Add mandatory authorization filter injection to `SearchQueryMediator` using `DatasetSearchAuthContext`: emit a `must` bool clause enforcing publicVisibility/creator/custodian/DAC/institution constraints as Elasticsearch terms/bool queries against indexed `accessPolicy` fields | L | Backend (depends on 2.1, 3B.1) |
| 3B.3 Add server-managed field allowlist per caller profile applied to search responses; strip sensitive fields server-side, not in the client | M | Backend (depends on 3B.2) |
| 3B.4 Wire `SearchQueryMediator` into both `searchDatasets` and `searchDatasetsStream` in `ElasticSearchService` | S | Backend (depends on 3B.2, 3B.3) |
| 3B.5 Unit-test `SearchQueryMediator` for each access dimension and confirm sensitive fields are absent from responses, not blanked on the client side | M | Backend (depends on 3B.4) |

### Phase 4 — API Hardening and Long-term Contract

*Depends on 3A or 3B being live.*

| Task | Size | Owner |
| --- | --- | --- |
| 4.1 Harden existing `/api/dataset/search/index` and `/api/dataset/search/index/v2` in `DatasetResource` to always pass `duosUser` into the mediated/secured search path; remove the current passthrough-to-raw-ES behavior | S | Backend (depends on 3A or 3B) |
| 4.2 Design server-owned search API that accepts business parameters instead of raw Elasticsearch DSL (filters, pagination, sort, text query) and returns shaped response; scope as v3 endpoint | L | Backend *(deferrable)* |
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

Phase 0.1 → Phase 0.2/0.3 → Phase 1.1–1.3 / Phase 2.1–2.2 (parallel) → Phase 3A or 3B →
Phase 4.1 → Phase 5.1 → Phase 6.1–6.3

Rough total (native DLS/FLS path): ~14–18 backend-engineer weeks end-to-end across phases;
~3–4 frontend-engineer weeks; ~1–2 infra-engineer weeks for cluster security setup and rollout
support. Phases 3B and 4.2/4.3 can be deferred if the cluster unambiguously supports DLS/FLS.

### Decisions

- **Recommended target architecture**: native Elasticsearch DLS/FLS now, backed by indexed
  `accessPolicy` metadata and backend-generated per-request Elasticsearch auth context.
- **Required fallback**: server-owned query mediation and field allowlisting if native cluster
  capabilities or rollout timing block immediate DLS/FLS adoption.
- **Included scope**: `publicVisibility` enforcement on the server side, explicit allow lists,
  institution restrictions, DAC-scoped access, policy tags/system-defined criteria, streaming
  endpoint behavior, reindex strategy, testing, and duos-ui alignment.
- **Excluded scope**: redesign of non-search Consent endpoints, unrelated UI behavior changes, and
  implementation of arbitrary policy-authoring UX unless policy storage gaps force a minimal
  admin/data-model addition.
- **Critical assumption**: the target Elasticsearch environment supports the native security
  features needed for DLS/FLS; if not, the fallback path becomes the first implementation
  milestone.

### Further Considerations

1. **Search API direction**: Option A — harden existing raw-DSL endpoints first for compatibility.
   Option B — add a server-owned search API in parallel and migrate clients over time.
   Recommendation: do both, but treat the server-owned API as the long-term destination.
2. **Field-level policy granularity**: Option A — role/profile-based field bundles (e.g.
   `public-reader` vs `privileged-reader`). Option B — fully policy-tag-driven per-document field
   exposure. Recommendation: start with profile-based bundles to control complexity, then evolve to
   tag-driven rules if required.
3. **Institution restrictions source of truth**: Option A — user institution alone. Option B —
   institution plus library-card or other status-derived qualifiers. Recommendation: separate
   identity context from eligibility state so document policy remains stable even if login checks
   change.