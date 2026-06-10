# Dataset Registration Schema Migration Plan

## Status

Proposed implementation plan for migrating dataset/study registration away from backend JSON Schema validation.

## Context

Dataset and study registration currently use `dataset-registration-schema_v1.json` as the public contract for create and update flows. The schema gives clients a predictable form shape, validates required fields on create, and maps known fields into normalized `study`, `study_property`, `dataset`, and `dataset_property` records.

The ticket asks how to "escape from schema constraints" for datasets and studies. In practice there are two different constraint classes:

- Shape constraints: JSON type, required-field, enum, format, and conditional requirements in `src/main/resources/dataset-registration-schema_v1.json`.
- Logical constraints: product/business rules attached to known fields, such as AnVIL conditional requirements, immutable update fields, non-removable consent groups, and controlled data-use changes.

The long-term path is to stop using the JSON Schema file as the backend source of truth. The schema can remain temporarily as a compatibility artifact for existing clients, but backend validation should move into typed request models and explicit domain validators.

## Current Implementation

### Create Flow

`POST /api/dataset/v3` accepts multipart form data with a `dataset` JSON part.

Current behavior:

1. `DatasetResource.createDatasetRegistration` validates the payload with `JsonSchemaUtil.validateSchemaMessagesV1`.
2. `JsonSchemaUtil` loads `dataset-registration-schema_v1.json` and validates against JSON Schema draft 2019-09.
3. Valid JSON is deserialized into `DatasetRegistrationSchemaV1`.
4. `DatasetRegistrationService.createDatasetsFromRegistration` creates one study plus one dataset per consent group.
5. Known fields are converted into `StudyProperty` and `DatasetProperty` records.
6. Study-level `assets` and `data` are persisted as JSON `StudyProperty` values.
7. Consent-group-level `data` is persisted as a JSON `DatasetProperty` with schema property `data`.

### Study Update Flow

`PUT /api/dataset/study/{studyId}` accepts the same registration-shaped multipart payload.

Current behavior differs from create:

1. `StudyResource.updateStudyByRegistration` does not run direct JSON Schema validation.
2. `DatasetRegistrationSchemaV1UpdateValidator.deserializeRegistration` deserializes a registration object in an edit context.
3. Existing consent groups ignore protected fields such as data-use fields, DAC id, access management, and dataset identifier.
4. `DatasetRegistrationSchemaV1UpdateValidator.validate` applies manual business validation for uniqueness, immutability, consent group membership, and required edit fields.
5. `DatasetRegistrationService.updateStudyFromRegistration` writes study properties and dataset properties using the same conversion path as create.

### Existing Extension Fields

The current schema/model path already has intentional extension points that should be preserved in the DTO-based replacement:

- Top-level `assets`: object with `additionalProperties: true`.
- Top-level `data`: object with `additionalProperties: true`.
- Consent-group `data`: model-backed `Map<String, Object>` that round-trips through `DatasetProperty` as JSON. The consent-group schema is open by default because it does not set `additionalProperties: false`.

Observed persistence behavior:

- Top-level `assets` becomes a `StudyProperty` key named `assets` with type `Json`.
- Top-level `data` becomes a `StudyProperty` key named `data` with type `Json`.
- Consent-group `data` becomes a `DatasetProperty` with `propertyName = data`, `schemaProperty = data`, and type `Json`.
- Registration reconstruction reads those JSON properties back into `DatasetRegistrationSchemaV1` and `ConsentGroup`.

## Design Goal

Remove the backend dependency on `dataset-registration-schema_v1.json` while preserving safe creation and update of studies and datasets.

The replacement design should also preserve optional client-managed metadata without changing a global JSON Schema for every non-critical display or submission field.

The design must preserve:

- Stable required fields needed to create studies and datasets.
- Existing authorization rules.
- Data-use governance rules.
- DAC assignment behavior.
- Existing API compatibility for registration clients.
- Search/indexing stability.

Non-goals:

- Do not make registration a fully unstructured JSON blob.
- Do not bypass governance, matching, DAC, authorization, or voting rules.
- Do not remove `dataset-registration-schema_v1.json` until schema-driven clients are migrated.
- Do not add backend validation for optional `data` metadata; promote it to a first-class DTO/domain field if backend behavior depends on it.

## Recommendation

Move from schema-file validation to application-owned validation:

1. Introduce explicit registration request DTOs for study and dataset creation/update.
2. Validate core required fields with Java/domain validators instead of `JsonSchemaUtil`.
3. Keep flexible `data` maps for optional metadata that does not need backend validation.
4. Keep `dataset-registration-schema_v1.json` only during migration for existing clients and `/schemas/dataset-registration/v1`.
5. Deprecate the schema endpoint once clients no longer build forms or validation from it.

Use this rule of thumb:

| Need | Recommended path |
| --- | --- |
| Capture non-governance metadata | Put it in top-level `data` for study metadata or consent-group `data` for dataset metadata. |
| Render UI-only labels, choices, or form metadata | Put it in `assets` if it is presentation/submission-context metadata and not dataset state. |
| Add a field used by backend workflows, search filters, permissions, matching, DAC automation, or reporting | Promote it to a first-class DTO/domain/property field. |
| Relax or change required core registration data | Change the DTO/domain validator contract and document it in OpenAPI. |
| Change data-use or access-management semantics | Do not use an escape hatch; use explicit domain changes and tests. |

## Proposed Extension Contract

Treat `data` as optional client-managed metadata. The backend should preserve and round-trip it, but should not use it as an authoritative registration contract.

This metadata may be validated by frontend TypeScript types or other client-side form logic. Backend validation remains focused on first-class DTO/domain fields.

Example study-level extension:

```json
{
  "studyName": "Synthetic Study",
  "studyDescription": "Synthetic study description",
  "dataTypes": ["WGS"],
  "publicVisibility": true,
  "nihAnvilUse": "I am not NHGRI funded and do not plan to store data in AnVIL",
  "piName": "Synthetic PI",
  "consentGroups": [
    {
      "consentGroupName": "Synthetic Consent Group",
      "numberOfParticipants": 100,
      "dataAccessCommitteeId": 1,
      "generalResearchUse": true,
      "data": {
        "duosRegistration": {
          "workflowTag": "legacy-import",
          "internalNotes": "Created during registration migration dry run"
        }
      }
    }
  ],
  "data": {
    "duosRegistration": {
      "source": "manual-registration",
      "migrationBatch": "batch-1"
    }
  }
}
```

Metadata rules:

- Prefer grouping related metadata under a stable top-level object key, for example `duosRegistration`, `migration`, or `submissionWorkflow`.
- Do not duplicate first-class DTO/domain fields inside `data` as an override mechanism.
- Do not store secrets, credentials, access tokens, or policy decisions in extension data.
- Prefer scalar, array, and object values that serialize cleanly with Gson.
- Keep extension data small enough to be practical as a row-level JSON property, not a document store.
- If backend behavior starts depending on a value in `data`, promote that value to a first-class DTO/domain field.

## What This Escapes

This approach escapes logical construct churn, not the core DTO/domain contract.

Escaped safely:

- Optional client-managed submission metadata.
- UI metadata that needs to round-trip with a study or dataset.
- Experimental fields that are not used for governance, matching, authorization, or DAC decisions.
- Metadata that can be ignored by older clients without changing behavior.

Not escaped:

- Missing core required fields on create.
- Invalid enum values for first-class fields.
- Invalid email/date/URI formats for first-class fields.
- Data-use terms and matching semantics.
- DAC assignment and access-management decisions.
- Update immutability rules for existing consent groups.

## Implementation Guide

### Path Away From `dataset-registration-schema_v1.json`

The replacement architecture should make backend validation independent from the JSON Schema file.

Target flow for create:

1. `DatasetResource.createDatasetRegistration` parses the multipart `dataset` part into a `DatasetRegistrationRequest`.
2. A create validator checks required study fields, consent-group fields, data-use consistency, DAC references, dates, email formats, URLs, and mutually exclusive choices.
3. `DatasetRegistrationService.createDatasetsFromRegistration` receives a validated domain command instead of a schema-generated model.
4. Persistence still writes normalized study/dataset fields plus flexible `data` JSON properties.

Target flow for update:

1. `StudyResource.updateStudyByRegistration` parses the multipart `dataset` part into a `StudyRegistrationUpdateRequest`.
2. An update validator checks edit-specific rules against the existing study: name uniqueness, immutable submitter, no deletion of existing consent groups, protected data-use fields, and required editable study fields.
3. The service receives a validated update command.
4. Existing dataset/study reconstruction can continue to return a registration-shaped response until clients migrate.

Recommended classes:

- `DatasetRegistrationRequest`: request DTO for create.
- `ConsentGroupRegistrationRequest`: nested DTO for dataset/consent-group create.
- `StudyRegistrationUpdateRequest`: request DTO for study registration update.
- `DatasetRegistrationCreateValidator`: create-only required-field and domain validator.
- `DatasetRegistrationUpdateValidator`: update-only validator. This would replace the current `DatasetRegistrationSchemaV1UpdateValidator`.
- `DatasetRegistrationMapper`: maps request DTOs to `StudyInsert`, `DatasetInsert`, `StudyUpdate`, and `DatasetUpdate` commands.

The request DTOs should be owned by the API/service code, not generated from JSON Schema.

### Class Shape Examples

These examples are intentionally partial. They show ownership boundaries and expected shape, not every registration field.

Create request DTO:

```java
public record DatasetRegistrationRequest(
    String studyName,
    String studyDescription,
    List<String> dataTypes,
    Boolean publicVisibility,
    String nihAnvilUse,
    String piName,
    String piEmail,
    List<ConsentGroupRegistrationRequest> consentGroups,
    Map<String, Object> assets,
    Map<String, Object> data) {}
```

Consent-group request DTO:

```java
public record ConsentGroupRegistrationRequest(
    String consentGroupName,
    Integer numberOfParticipants,
    Integer dataAccessCommitteeId,
    String accessManagement,
    Boolean generalResearchUse,
    Boolean hmb,
    Boolean poa,
    List<String> diseaseSpecificUse,
    String otherPrimary,
    Map<String, Object> data) {}
```

Create validator:

```java
public class DatasetRegistrationCreateValidator {

  private final DacDAO dacDAO;

  public void validate(DatasetRegistrationRequest request) {
    requireNonBlank(request.studyName(), "Study Name is required");
    requireNonBlank(request.studyDescription(), "Study Description is required");
    requireNotEmpty(request.dataTypes(), "Data Types is required");
    requireNotNull(request.publicVisibility(), "Public Visibility is required");
    requireNonBlank(request.nihAnvilUse(), "NIH AnVIL Use is required");
    requireNonBlank(request.piName(), "Principal Investigator is required");
    requireNotEmpty(request.consentGroups(), "Consent Groups are required");

    request.consentGroups().forEach(this::validateConsentGroup);
  }

  private void validateConsentGroup(ConsentGroupRegistrationRequest group) {
    requireNonBlank(group.consentGroupName(), "Consent Group Name is required");
    requireNotNull(group.numberOfParticipants(), "# of Participants is required");
    validatePrimaryUse(group);

    if ("controlled".equals(group.accessManagement())) {
      requireNotNull(group.dataAccessCommitteeId(), "DAC is required");
      if (dacDAO.findById(group.dataAccessCommitteeId()) == null) {
        throw new BadRequestException("Could not find DAC");
      }
    }
  }

  private void validatePrimaryUse(ConsentGroupRegistrationRequest group) {
    long selectedPrimaryUses =
        Stream.of(
                Boolean.TRUE.equals(group.generalResearchUse()),
                Boolean.TRUE.equals(group.hmb()),
                Boolean.TRUE.equals(group.poa()),
                group.diseaseSpecificUse() != null && !group.diseaseSpecificUse().isEmpty(),
                group.otherPrimary() != null && !group.otherPrimary().isBlank())
            .filter(Boolean::booleanValue)
            .count();

    if (!"open".equals(group.accessManagement()) && selectedPrimaryUses != 1) {
      throw new BadRequestException("Exactly one primary data use is required");
    }
  }
}
```

Update validator:

```java
public class DatasetRegistrationUpdateValidator {

  private final DatasetService datasetService;

  public void validate(Study existingStudy, StudyRegistrationUpdateRequest request) {
    validateRequiredStudyFields(request);
    validateStudyName(existingStudy, request.studyName());
    validateConsentGroupMembership(existingStudy, request.consentGroups());
    validateNoExistingConsentGroupRemoval(existingStudy, request.consentGroups());
  }

  private void validateStudyName(Study existingStudy, String requestedName) {
    Set<String> studyNames = datasetService.findAllStudyNames();
    if (studyNames.contains(requestedName) && !requestedName.equals(existingStudy.getName())) {
      throw new BadRequestException("Invalid change to Study Name");
    }
  }

  private void validateNoExistingConsentGroupRemoval(
      Study existingStudy, List<ConsentGroupRegistrationUpdateRequest> groups) {
    Set<Integer> existingIds = SetUtils.emptyIfNull(existingStudy.getDatasetIds());
    Set<Integer> requestedIds =
        groups.stream()
            .map(ConsentGroupRegistrationUpdateRequest::datasetId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    if (!requestedIds.containsAll(existingIds)) {
      throw new BadRequestException("Invalid removal of Consent Groups");
    }
  }
}
```

Mapper from request DTO to service command:

```java
public class DatasetRegistrationMapper {

  public StudyInsert toStudyInsert(DatasetRegistrationRequest request, User user) {
    return new StudyInsert(
        request.studyName(),
        request.studyDescription(),
        request.dataTypes(),
        request.piName(),
        request.piEmail(),
        request.publicVisibility(),
        user.getUserId(),
        toStudyProperties(request),
        List.of());
  }

  public DatasetInsert toDatasetInsert(
      DatasetRegistrationRequest request,
      ConsentGroupRegistrationRequest group,
      User user) {
    return new DatasetInsert(
        group.consentGroupName(),
        group.dataAccessCommitteeId(),
        toDataUse(group),
        user.getUserId(),
        toDatasetProperties(group),
        List.of());
  }
}
```

Resource after the backend switch:

```java
public Response createDatasetRegistration(
    @Auth AuthUser authUser,
    FormDataMultiPart multipart,
    @FormDataParam("dataset") String json) {
  try {
    DatasetRegistrationRequest request =
        GsonUtil.getInstance().fromJson(json, DatasetRegistrationRequest.class);
    createValidator.validate(request);

    User user = userService.findUserByEmail(authUser.getEmail());
    Map<String, FormDataBodyPart> files = extractFilesFromMultiPart(multipart);
    List<Dataset> datasets =
        datasetRegistrationService.createDatasetsFromRegistration(request, user, files);

    return Response.created(studyUri(datasets)).entity(toResponse(datasets)).build();
  } catch (Exception e) {
    return createExceptionResponse(e);
  }
}
```

### Migration Phases

Phase 1: Mirror current behavior.

- Add request DTOs that represent the currently accepted registration payload.
- Add create/update validators that reproduce current JSON Schema and manual update checks.
- Add tests comparing old validation failures with new validator failures for representative payloads.
- Keep `JsonSchemaUtil.validateSchemaMessagesV1` active as a guardrail.

Phase 2: Run validators in parallel.

- In `POST /api/dataset/v3`, run the new create validator after existing JSON Schema validation.
- In `PUT /api/dataset/study/{studyId}`, run the new update validator in place of or alongside the current update validator.
- Log discrepancies where JSON Schema accepts but the new validator rejects, or vice versa.
- Do not change client behavior yet.

Phase 3: Switch backend authority.

- Make the new validators authoritative.
- Stop calling `JsonSchemaUtil.validateSchemaMessagesV1` in `DatasetResource.createDatasetRegistration`.
- Keep deserialization tolerant for unknown extension metadata under `data`.
- Keep the schema endpoint available for clients that still fetch it for form rendering.

Phase 4: Deprecate schema endpoint.

- Mark `/schemas/dataset-registration/v1` as deprecated in OpenAPI.
- Publish OpenAPI request schemas based on DTOs, not the old JSON Schema file.
- Coordinate client removal of schema-driven form validation.
- Remove `dataset-registration-schema_v1.json` only after no client depends on it.

Phase 5: Clean up old model coupling.

- Replace `DatasetRegistrationSchemaV1` usage in create/update service paths with request/domain command types.
- Keep a response DTO for `GET /api/dataset/study/registration/{studyId}` if clients still need registration-shaped data.
- Remove `JsonSchemaUtil` registration validation methods when no endpoint uses them.

### DUOS UI Findings

The current `duos-ui` codebase indicates:

- `src/routing/AppRoutes.tsx` routes `/data_submission_form` and `/study_update/:studyId` to `DataSubmissionFormV2`.
- The legacy `src/pages/data_submission/DataSubmissionForm.jsx` calls `DataSet.getRegistrationSchema()` and validates with `RegistrationValidation.js`.
- `src/libs/ajax/DataSet.js` and `src/libs/ajax/Schema.js` still expose clients for `/schemas/dataset-registration/v1`.
- `DataSubmissionFormV2` does not fetch `/schemas/dataset-registration/v1`; it uses TypeScript models and converts a `Study` into a registration-shaped multipart payload with `studyToDatasetSchemaSubmission`.
- Legacy Cypress tests under `cypress/component/DataSubmission` still depend on a copied `dataset-registration-schema_v1.json` fixture.

This means `/schemas/dataset-registration/v1` is tied to the legacy v1 data submission form and legacy tests. The active submission route is v2, but v2 still posts to the existing backend registration endpoints using a registration-shaped payload.

### Decisions Needed Before Implementation

Before implementing the migration, the team should choose from these options.

| Decision | Options | Recommended choice |
| --- | --- | --- |
| Schema endpoint compatibility | A. Keep `/schemas/dataset-registration/v1` indefinitely.<br>B. Mark it deprecated and remove after v1 data submission code/tests are removed.<br>C. Remove immediately. | B. `duos-ui` v2 no longer appears to fetch the schema, but legacy v1 form code and Cypress fixtures still reference it. Deprecate now, remove after those are deleted or confirmed unused. |
| Backend endpoint strategy | A. Change `POST /api/dataset/v3` and `PUT /api/dataset/study/{studyId}` in place.<br>B. Add new registration endpoints and migrate v2 UI to them. | A for first implementation. V2 UI already posts registration-shaped multipart payloads to these endpoints, so changing backend validation in place minimizes client churn. Add new endpoints only if the DTO shape intentionally diverges from the current payload. |
| Backend request shape | A. Keep accepting the current registration-shaped multipart `dataset` payload.<br>B. Replace it with a new DTO payload shape immediately. | A. Keep the wire payload stable while replacing backend validation internals. Rename internal Java types to DTO/domain commands, not the external payload yet. |
| Error response contract | A. Preserve current `400` behavior and user-facing validation message style.<br>B. Return a structured field-error response. | A initially. Preserve behavior while swapping validators, then consider structured errors as a separate API improvement. |
| File handling contract | A. Keep current multipart file field names.<br>B. Replace them with new typed file metadata. | A. Keep `alternativeDataSharingPlan` and `consentGroups[n].nihInstitutionalCertificationFile` unchanged to avoid UI and backend file-upload churn. |
| Registration-shaped response | A. Keep `GET /api/dataset/study/registration/{studyId}` returning registration-shaped data.<br>B. Replace with a new DTO response immediately. | A. V2 UI still benefits from registration-shaped reconstruction. Decouple create/update validation first; response cleanup can happen later. |
| OpenAPI ownership | A. Treat OpenAPI as the API contract after migration.<br>B. Keep JSON Schema as the form/API contract. | A. Once backend validation is DTO/domain-owned, OpenAPI should describe the supported request/response contract. JSON Schema should not remain the authoritative backend validation source. |
| Legacy duos-ui cleanup | A. Remove legacy v1 form code, schema ajax clients, and schema fixture tests before backend switch.<br>B. Switch backend first, then remove UI code. | A if feasible. Removing legacy callers first reduces compatibility risk and gives confidence to deprecate `/schemas/dataset-registration/v1`. |

### Where Validation Should Live After Migration

Validation should be split by ownership:

| Validation | Owner |
| --- | --- |
| Required create fields | `DatasetRegistrationCreateValidator` |
| Required update fields | `DatasetRegistrationUpdateValidator` |
| Study name uniqueness | update/create validator using `DatasetService` |
| DAC id existence | create/update validator using `DacDAO` or service facade |
| Consent-group add/remove rules | update validator using existing study state |
| Data-use consistency | domain validator, not flexible `data` |
| Email/date/URL parsing | DTO/domain validators |
| Optional metadata under `data` | no backend validation; preserve and round-trip only |

The resource layer should only parse input, call validators/services, and map exceptions to HTTP responses. Business validation belongs below the resource layer.

### Adding Optional Study Metadata

1. Add the optional metadata object under top-level `data` in the registration payload.
2. Submit through `POST /api/dataset/v3` or `PUT /api/dataset/study/{studyId}`.
3. Read it back through `GET /api/dataset/study/registration/{studyId}`.
4. Do not add backend validation for this metadata.

No DTO, migration, or OpenAPI change is required if the field is optional client-managed metadata.

### Adding Optional Dataset Metadata

1. Add the optional metadata object under `consentGroups[n].data`.
2. Submit through the registration create or study update endpoint.
3. Read it back through `GET /api/dataset/study/registration/{studyId}`.
4. Do not add backend validation for this metadata.

No DTO, migration, or OpenAPI change is required if the field is optional client-managed metadata.

### Promoting Optional Metadata to a First-Class Field

Promote an optional `data` field when it becomes part of backend behavior or public API semantics.

Required work:

1. Add the field to the request DTO and response DTO.
2. Add validation to the create/update validator that owns the field.
3. Add conversion in `DatasetRegistrationService` for create/update persistence.
4. Add reconstruction in the registration response mapper. During migration this may still be `SchemaFromStudy` or `ConsentGroupFromDataset`.
5. Update `src/main/resources/assets/api-docs.yaml` and related OpenAPI schema files.
6. Add tests for create, update, and registration reconstruction.
7. Update search indexing if the field is queryable or displayed in search results.
8. During migration, update `dataset-registration-schema_v1.json` only if existing schema-driven clients still require the field there.

## Validation Strategy

Keep validation ownership distinct:

- Create validation: protects the core registration shape and creation business rules.
- Edit validation: protects update-only rules that require existing study/dataset state.
- Optional `data` metadata: not backend-validated. Preserve it for frontend/client-managed workflows.

## Risks and Mitigations

| Risk | Mitigation |
| --- | --- |
| Optional `data` becomes an untyped dumping ground | Keep `data` optional and client-managed; promote fields when backend behavior depends on them. |
| Clients treat optional `data` as backend-validated contract | Document that `data` is preserved by the backend but not backend-validated. |
| Search/reporting needs emerge later | Promote the field before using it for indexed queries or governance reports. |
| Conflicting optional metadata keys | Prefer grouped metadata under stable top-level keys. |
| Hidden policy changes bypass governance | Disallow data-use, DAC, authorization, and matching semantics in extension data. |
| OpenAPI drift | Update OpenAPI only when first-class fields or endpoint behavior change. |

## Field Classification Checklist

Before adding a new registration field or metadata value, answer these questions:

- Is this required to create a valid study or dataset?
- Will backend authorization, matching, DAC automation, voting, indexing, or reporting use this value?
- Does the value need global validation, enum control, or typed API documentation?
- Will clients need to discover this field from OpenAPI or another typed contract?
- Would older clients behave incorrectly if they ignore the value?

If the answer is no to all, use optional `data`.

If any answer is yes, make it a first-class DTO/domain field.

During the migration window, also ask:

- Does an existing client still read `dataset-registration-schema_v1.json` to render or validate this field?
- If yes, mirror the field in the schema file temporarily as a compatibility artifact.
- If no, update only DTOs, validators, mappers, OpenAPI, and tests.
