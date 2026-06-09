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
  - Sources for client filtering: `../duos-ui/cypress/component/DataLibrary.spec.tsx`, `../duos-ui/src/components/DataSearch/dataset_search_table.tsx` (implements filter).
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
  (see [DatasetService.java#L205](../src/main/java/org/broadinstitute/consent/http/service/DatasetService.java#L205) - `getDatasetByName`, 
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
- **Sources**: [DatasetService.java#L205](../src/main/java/org/broadinstitute/consent/http/service/DatasetService.java#L205), 
  [ElasticSearchService.java#L212](../src/main/java/org/broadinstitute/consent/http/service/ElasticSearchService.java#L212) (searchDatasets indexes names).

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

  ## Access Controls Implementation Plan