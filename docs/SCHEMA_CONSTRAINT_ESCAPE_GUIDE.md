# Breaking Free of Dataset Registration Schema Constraints

## Status

Proposed design guide for the dataset/study registration flow.

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

The schema already has intentional extension points:

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

The replacement design should also allow new or partner-specific metadata to be captured without changing a global JSON Schema for every product experiment, partner variation, or non-critical display field.

The design must preserve:

- Stable required fields needed to create studies and datasets.
- Existing authorization rules.
- Data-use governance rules.
- DAC assignment behavior.
- Existing API compatibility for registration clients.
- Search/indexing stability.

## Recommendation

Move from schema-file validation to application-owned validation:

1. Introduce explicit registration request DTOs for study and dataset creation/update.
2. Validate core required fields with Java/domain validators instead of `JsonSchemaUtil`.
3. Keep flexible namespaced `data` maps for metadata that should not be globally modeled.
4. Keep `dataset-registration-schema_v1.json` only during migration for existing clients and `/schemas/dataset-registration/v1`.
5. Deprecate the schema endpoint once clients no longer build forms or validation from it.

Use this rule of thumb:

| Need | Recommended path |
| --- | --- |
| Capture non-governance metadata | Put it in top-level `data` for study metadata or consent-group `data` for dataset metadata. |
| Render UI-only labels, choices, or partner-specific form metadata | Put it in `assets` if it is presentation/submission-context metadata and not dataset state. |
| Add a field used by backend workflows, search filters, permissions, matching, DAC automation, or reporting | Promote it to a first-class DTO/domain/property field. |
| Relax or change required core registration data | Change the DTO/domain validator contract and document it in OpenAPI. |
| Change data-use or access-management semantics | Do not use an escape hatch; use explicit domain changes and tests. |

## Proposed Extension Contract

Treat flexible metadata as namespaced JSON under `data`.

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
        "examplePartner": {
          "cohortLabel": "pilot-a",
          "collectionWave": 2
        }
      }
    }
  ],
  "data": {
    "examplePartner": {
      "externalStudyId": "EXT-123",
      "submissionProgram": "pilot"
    }
  }
}
```

Namespace rules:

- Use one top-level object key per product, partner, or feature namespace, for example `nhgri`, `anvil`, `examplePartner`, or `duosExperimental`.
- Do not place arbitrary extension keys directly inside `data`; group them under a namespace.
- Do not duplicate first-class schema fields inside `data` as an override mechanism.
- Do not store secrets, credentials, access tokens, or policy decisions in extension data.
- Prefer scalar, array, and object values that serialize cleanly with Gson.
- Keep extension data small enough to be practical as a row-level JSON property, not a document store.

## What This Escapes

This approach escapes logical construct churn, not the core DTO/domain contract.

Escaped safely:

- Partner-specific submission metadata.
- UI workflow hints that need to round-trip with a study or dataset.
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
| Flexible metadata under `data` | feature-specific validator only when that feature consumes the namespace |

The resource layer should only parse input, call validators/services, and map exceptions to HTTP responses. Business validation belongs below the resource layer.

### Adding Flexible Study Metadata

1. Add the namespaced object under top-level `data` in the registration payload.
2. Submit through `POST /api/dataset/v3` or `PUT /api/dataset/study/{studyId}`.
3. Read it back through `GET /api/dataset/study/registration/{studyId}`.
4. If a backend consumer needs the value, read the `StudyProperty` with key `data` and parse the namespaced sub-object.

No DTO, migration, or OpenAPI change is required if the field is purely flexible metadata.

### Adding Flexible Dataset Metadata

1. Add the namespaced object under `consentGroups[n].data`.
2. Submit through the registration create or study update endpoint.
3. Read it back through `GET /api/dataset/study/registration/{studyId}`.
4. If a backend consumer needs the value, read the dataset property with `schemaProperty = data` and parse the namespaced sub-object.

No DTO, migration, or OpenAPI change is required if the field is purely flexible metadata.

### Promoting Extension Data to a First-Class Field

Promote a flexible field when it becomes part of backend behavior or public API semantics.

Required work:

1. Add the field to the request DTO and response DTO.
2. Add validation to the create/update validator that owns the field.
3. Add conversion in `DatasetRegistrationService` for create/update persistence.
4. Add reconstruction in `SchemaFromStudy` or `ConsentGroupFromDataset`.
5. Update `src/main/resources/assets/api-docs.yaml` and related OpenAPI schema files.
6. Add tests for create, update, and registration reconstruction.
7. Update search indexing if the field is queryable or displayed in search results.
8. During migration, update `dataset-registration-schema_v1.json` only if existing schema-driven clients still require the field there.

## Validation Strategy

Keep validation ownership distinct:

- Create validation: protects the core registration shape and creation business rules.
- Edit validation: protects update-only rules that require existing study/dataset state.
- Extension validation: optional consumer-side validation inside a namespace, owned by the feature that reads that namespace.

For extension validation, avoid blocking core registration unless the extension field is required for a specific feature path. If a feature requires its own extension values, validate only that namespace and return feature-specific errors.

## Risks and Mitigations

| Risk | Mitigation |
| --- | --- |
| Extension data becomes an untyped dumping ground | Require namespaced keys and promote fields when backend behavior depends on them. |
| Clients treat extension data as validated contract | Document that `data` is flexible metadata and not globally schema-validated. |
| Search/reporting needs emerge later | Promote the field before using it for indexed queries or governance reports. |
| Conflicting partner keys | Namespacing prevents collisions. |
| Hidden policy changes bypass governance | Disallow data-use, DAC, authorization, and matching semantics in extension data. |
| OpenAPI drift | Update OpenAPI only when first-class fields or endpoint behavior change. |

## Decision Checklist

Before adding a new schema field, answer these questions:

- Is this required to create a valid study or dataset?
- Will backend authorization, matching, DAC automation, voting, indexing, or reporting use this value?
- Does the value need global validation, enum control, or typed API documentation?
- Will clients need to discover this field from OpenAPI or another typed contract?
- Would older clients behave incorrectly if they ignore the value?

If the answer is no to all, use namespaced `data`.

If any answer is yes, make it a first-class DTO/domain field.

## Recommended Outcome for This Ticket

Remove the backend dependency on `dataset-registration-schema_v1.json`, but do not make the entire registration object unstructured. Instead:

- Replace JSON Schema validation with explicit request DTOs and create/update validators.
- Preserve `dataset-registration-schema_v1.json` temporarily only for compatibility with schema-driven clients.
- Use top-level `data` for study-level flexible metadata.
- Use `consentGroups[n].data` for dataset-level flexible metadata.
- Keep `assets` limited to submission/UI asset metadata.
- Promote only durable backend semantics to first-class DTO/domain fields.
- Deprecate and remove the schema endpoint after clients no longer depend on it.
