# Data Use Terms for Models and Workspaces

Oct 13, 2026 · Gregory Rushton · Ticket: [DT-3810](https://broadworkbench.atlassian.net/browse/DT-3810)

The request adds Access Management and Primary and Secondary Data Use Terms to the Model and Workspace study assets, so that they match the Dataset schema. Today, researchers and Signing Officials who browse the Data Library cannot see the access restrictions or the permitted research uses for a model or a workspace. In consent, the work stores these fields, exposes them through the API, and sends them to Elasticsearch. In duos-ui, the work adds the fields to the `AiModel` and `Workspace` types and shows them in the Data Library with the existing Dataset components. Other asset types, such as Biospecimen, and the authoring of Data Use Terms for Datasets are out of scope.

## Current state

Consent stores the Model and Workspace assets of a study as JSON lists, one `study_property` row for each asset type. A 2026-09-02 migration moved each list into its own row. The entries have no data use and no structured access management.

| Aspect | Dataset | Model | Workspace |
| --- | --- | --- | --- |
| Storage in consent | `dataset` table row | JSON list in `study_property`, key `models` | JSON list in `study_property`, key `workspaces` |
| Data use | `DataUse` JSON in `dataset.data_use` | None | None |
| Access management | `dataset_property` row, enum open / controlled / external | None | Free-text `access` string |
| Validation | `StudyRegistrationRequestValidator` | None (stored as sent) | None (stored as sent) |
| API | Dataset and study endpoints | `GET /api/dataset/study/{id}/assets/models` | `GET /api/dataset/study/{id}/assets/workspaces` |
| Elasticsearch | One document per dataset; `dataUse` holds a computed `DataUseSummary` | Untyped map under `study.assets.models` in each dataset document | Untyped map under `study.assets.workspaces` in each dataset document |
| Data Library UI | Data use chip column, access column, filters | Columns for format, license, cloud, tags | Columns for platform, tools, cloud, free-text access |
| Registration form | Data use editor per consent group | `AiModelList` | `WorkspaceList` |

Key code in consent: `StudyAssets.java`, `StudyAssetResource.java`, `ElasticSearchService.toStudyTerm`, `OntologyService.translateDataUseSummary`, `ConsentGroup.AccessManagement`. Key code in duos-ui: `src/types/model.ts` (`AiModel`, `Workspace`), `columns/modelColumns.tsx`, `columns/workspaceColumns.tsx`, `dataUseGridColumn.tsx`, `accessManagementDisplay.ts`.

## Reference architecture

Each Model and each Workspace entry gets two new fields. The fields use the same types that a Dataset uses. Consent stores the raw `DataUse`, and computes the `DataUseSummary` only when it indexes, as it does for a Dataset.

### Stored shape of one asset entry

```json
{
  "modelId": "...",
  "name": "...",
  "accessManagement": "open | controlled | external",
  "dataUse": { "generalUse": false, "hmbResearch": true, "nonProfitUse": true, "publicationResults": true }
}
```

- `dataUse` has the same JSON shape as `dataset.data_use` (the `DataUse` class). Primary terms (GRU, HMB, DS, POA) and secondary terms (NPU, PUB, COL, and others) both live in this one object.
- `accessManagement` takes the values of the existing `AccessManagement` enum.
- All other fields of the entry stay as they are today.

### Data flow

1. The submitter enters data use and access management for each model or workspace in the study registration form.
2. duos-ui sends the lists as the top-level `models` and `workspaces` registration fields.
3. `StudyRegistrationRequestValidator` (study create) and `StudyUpdateRequestValidator` (study edit) validate the two new fields on each entry. It rejects an unknown enum value or a `DataUse` that fails `DataUsePrimaryValidator`.
4. Consent stores each list in its `study_property` row, as today. No schema change is necessary.
5. `ElasticSearchService.toStudyTerm` reads each entry. If the entry has a `dataUse`, it adds a `dataUseSummary` that `OntologyService.translateDataUseSummary` computes.
6. The bulk index call writes the enriched entries under `study.assets.models` and `study.assets.workspaces` in each dataset document.
7. The study asset endpoints return the stored entries, with the same computed summary.
8. The Data Library and the study details page show a data use chip and an access management pill for each row. They reuse the Dataset components.

### Indexed shape of one asset entry

```json
{
  "modelId": "...",
  "accessManagement": "controlled",
  "dataUse": { "hmbResearch": true, "nonProfitUse": true },
  "dataUseSummary": {
    "primary": [ { "code": "HMB", "description": "..." } ],
    "secondary": [ { "code": "NPU", "description": "..." } ]
  }
}
```

The UI type for a Dataset row calls the summary `dataUse`. For assets, the index keeps the raw `dataUse` and adds `dataUseSummary`, so that a round trip through the registration form does not lose the raw object. The UI maps `dataUseSummary` to the `dataUse` field that the shared components expect.

## Design decisions

Decided: option A. We keep the JSON lists in `study_property` and add the two fields to each entry. We discarded option B, because it gives the same result for the user at about twice the cost.

| Option | What changes | Approx. lines | Benefit | Cost |
| --- | --- | --- | --- | --- |
| A. Extend the JSON entries (chosen) | Validation, index enrichment, OpenAPI, UI | 2,600 | No schema change, no data migration, no change to the read and write path of `StudyAssets` | Data use is not queryable in SQL |
| B. New `ai_model` and `workspace` tables (discarded) | Two tables, DAOs, mappers, a data migration from `study_property`, a rewrite of `StudyAssets` reads and writes, plus all of option A | 5,000 or more | Data use is queryable in SQL; asset IDs are real keys | Undoes the 2026-09-02 promotion work; a second migration of live data |

Other decisions in the plan:

- **Store `DataUse`, compute `DataUseSummary`.** This matches the Dataset path. Ontology labels for disease codes stay current after each reindex.
- **Reuse the enum.** Use `ConsentGroup.AccessManagement` where it is now. Do not move it, because 38 files use it. Do not make a second enum.
- **Validate only the new keys.** Do not parse a whole entry into a Java class and write it back. `StudyAssets` keeps unknown client fields on purpose, and a Gson round trip drops them.
- **No DAC rule for assets.** Models and workspaces are not requestable in the Data Library (`isRowSelectable` is false). Access management on an asset is information only, so `validateDacRequirement` does not apply.
- **Keep `Workspace.access` for now.** It stays as a deprecated free-text field until the team decides how to treat existing values. See the open questions.
- **Authoring is in scope.** The request puts authoring out of scope only for Datasets. To make the fields "storable", submitters need a form to enter them, so ticket 5 is necessary.
- **Model data use is entered by hand.** A model does not get its data use from its `trainedOnDatasets`. The submitter enters it in the registration form.

### Module design for asset data use

We recommend one new module, `AssetDataUse`, next to `StudyAssets`. It has one pure write entry point and one read entry point. We compared three designs, and this design gives the most leverage for the smallest interface.

```java
public final class AssetDataUse {
  // Write side: pure and static. Validates, and removes dataUseSummary.
  public static Prepared prepare(String assetKey, List<Object> entries);
  public record Prepared(List<Object> stored, List<String> violations) {}

  // Read side: injected singleton. Adds a fresh dataUseSummary to each entry.
  public AssetDataUse(TranslationUtil translator);
  public Map<String, Object> withSummaries(Map<String, Object> assetsByType);
  public List<Object> withSummaries(String assetKey, List<Object> entries);
}
```

| Design | Interface | Approx. lines (main + tests) | Result |
| --- | --- | --- | --- |
| 1. Small module, `AssetDataUse` | `prepare` and `withSummaries` | 350 | Recommended |
| 2. Registry of field rules, `StudyAssetEntries` plus `AssetEntryField` | 4 methods, 2 factories, 1 extension interface | 610 | Too general. Only one field is derived. The `pure()` and `withSummaries()` factories let a caller silently get no summary. |
| 3. Put the logic inside `StudyAssets` | Existing methods, plus `entryViolations` | 440 | Callers cannot forget the summary, but a pure model class gets a database dependency, and no caller can read the raw list. |

Rules for the module:

- Only `models` and `workspaces` change. Other keys and non-map entries pass through.
- Unknown keys in an entry stay, in their order. No input is changed.
- `stored` never contains `dataUseSummary`. `withSummaries` removes any stored summary before it adds a fresh one.
- A missing `dataUse` or `accessManagement` is valid. On update, an entry whose access management and data use pair is already stored is not re-checked; each stored pair excuses one sent entry, matched by pair rather than identity. `violations` never throws. It returns messages such as `models[2].accessManagement: must be one of open, controlled, external`.
- A malformed stored `dataUse` gets no summary and a `logWarn`. An ontology database error propagates, as it does for datasets.
- `withSummaries` memoises within one call, so identical `dataUse` values cost one ontology lookup.
- Other asset types can use the module later. To add a type, add its key to the list of asset types that the module handles. The callers do not change. Each new type still needs its own UI work, and Biospecimen first needs a decision on its free-text `optionalDataUse` field.

Callers:

| Caller | Call |
| --- | --- |
| `StudyRegistrationRequestValidator` | `prepare(...).violations()` |
| `StudyUpdateRequestValidator.validate` | `prepare(...).violations()` |
| `RegistrationRequestMapper.promotedAsset` | `prepare(...).stored()` |
| `ElasticSearchService.toStudyTerm` | `withSummaries(assemble(...))` |
| `StudyAssetService` | `withSummaries(key, findAssetList(...))` |
| `SchemaFromStudy` (registration GET) | `withSummaries(key, findAssetList(...))` |

The registration GET (`SchemaFromStudy`) also reads these lists. It is also the source of the summaries that clients send back. `DatasetRegistrationSchemaV1Builder` and `SchemaFromStudy` are made with `new` in `DatasetResource` and `StudyResource`, so ticket 4 must pass the injected module to them. The module depends on `TranslationUtil`, which `OntologyService.translateDataUseSummary` calls. Tests use the Testcontainers database in `DAOTestHelper` with `ontology_index` rows, and need no port.

## Tickets

The plan has 8 tickets: 4 in consent and 4 in duos-ui. Each estimate includes tests. Tickets 1 and 2 can start at the same time, and the backend can ship before the UI.

| # | Repo | Title | Scope | Approx. lines | Depends on |
| --- | --- | --- | --- | --- | --- |
| 1 | consent | Validate data use and access management on model and workspace entries | Add the write side of `AssetDataUse` (`prepare`). It validates `dataUse` and `accessManagement` on each `models` and `workspaces` entry and removes `dataUseSummary`. Add one shared `collectAssetViolations(registration)` method to `StudyRegistrationRequestValidator`. Call it from `collectStudyViolations` (study create) and from `StudyUpdateRequestValidator.validate` (study edit), which does not call the base collectors. Validate the list that `StudyAssets.promotedValue` returns, which is the list that persistence stores. Call `prepare` from `RegistrationRequestMapper.promotedAsset`. Use `ConsentGroup.AccessManagement` where it is now. Test bad model and workspace fields on create and on update, including entries that come from the legacy `assets.models` and `assets.workspaces`. | 350 | None |
| 2 | consent | Document the new asset fields in OpenAPI | Add a reusable schema for the `DataUse` object, moved out of `paths/datasetByIdDataUse.yaml`. Add a `DataUseSummary` schema that uses the existing term schema in `DataUse.yaml`. Add `dataUse`, `accessManagement` and `dataUseSummary` to `AiModel.yaml` and `Workspace.yaml`. | 250 | None |
| 3 | consent | Index the data use summary for models and workspaces | Add the read side of `AssetDataUse` (`withSummaries`) and its `@Provides @Singleton` provider in `ConsentModule`. Call it from `ElasticSearchService.toStudyTerm`, so that each entry that has a `dataUse` gets a `dataUseSummary`. Check the `dataset` index mapping and update it if it is not dynamic. | 300 | 1 |
| 4 | consent | Return the data use summary from the study asset endpoints | Share the enrichment from ticket 3 with `StudyAssetService`, so that `/assets/models` and `/assets/workspaces` return the same shape as the index. Also use it in SchemaFromStudy, which the registration GET uses, and pass the module through DatasetRegistrationSchemaV1Builder. | 300 | 3 |
| 5 | duos-ui | Enter data use and access management for models and workspaces | Add the new fields to the `AiModel` and `Workspace` types. Add an access management select and the existing data use editor to the `AiModelList` and `WorkspaceList` forms. If the diff goes above 500 lines, split it into one ticket for each asset type. | 500 | 1, 2 |
| 6 | duos-ui | Show data use and access management in the Data Library | Add the fields to the Elasticsearch and library row types. Make `DATA_USE_GRID_COLUMN` generic. Add the data use and access columns to `modelColumns.tsx` and `workspaceColumns.tsx`, which also updates the study details tables. | 400 | 3, 4 |
| 7 | duos-ui | Filter models and workspaces by data use and access management | Add access management and data use filters to the Models and Workspaces tabs in `dataLibraryFilterConfig.ts` and the client-side `matchesModelFilters` and `matchesWorkspaceFilters`. | 350 | 6 |
| 8 | both | Retire the free-text workspace access field | Apply the team decision on `Workspace.access`: map, hide or remove the old values. Remove the old column and filter. | 150 | 6, team decision |

Total: about 2,600 lines.

## Risks and open questions

The team must answer questions 1 and 2 before ticket 5 starts.

1. **Existing `Workspace.access` values.** Do we map them to the enum, show them read-only, or drop them? Free text cannot map to the enum without a manual review.
2. **Secondary terms for models.** The `DataUse` object includes `aiLlmUse`. Product must confirm which secondary terms make sense for a model and for a workspace.
3. **Index mapping.** Consent code does not set an explicit mapping for the `dataset` index. If the index is dynamic, the new fields need no change. If not, ticket 3 must update the mapping and run a reindex.
4. **Index size.** Each dataset document repeats its study's assets. A study with many datasets and many models stores each summary many times. The extra size is small, but we must confirm it on a large study.
