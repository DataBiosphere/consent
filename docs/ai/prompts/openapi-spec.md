# OpenAPI Spec Prompt Template

Use this when adding, editing, or reviewing OpenAPI path definitions in either:

- `src/main/resources/assets/paths/` — per-path YAML files referenced via `$ref`
- `src/main/resources/assets/api-docs.yaml` — inline operation blocks not yet extracted to a path file

---

## `operationId` Naming Convention

All **new** `operationId` values must follow the path-derived camelCase pattern:

```
api<PathSegments><HttpVerb>
```

Rules:

| Part | Rule | Example |
|---|---|---|
| Prefix | Always start with `api` | `api…` |
| Path segments | Concatenate each segment in PascalCase; drop slashes and underscores | `/draft/v1/` → `DraftV1` |
| Path parameters | Use the parameter name in PascalCase | `{draftUUID}` → `DraftUUID`, `{fileId}` → `FileId` |
| Version segments | Include as-is in PascalCase | `v2` → `V2` |
| HTTP verb | Append the verb in PascalCase at the end | GET → `Get`, POST → `Post`, DELETE → `Delete`, PUT → `Put`, PATCH → `Patch` |

### Examples

| Route | Method | `operationId` |
|---|---|---|
| `/api/draft/v1/{draftUUID}/attachments/{fileId}` | GET | `apiDraftV1DraftUUIDAttachmentsFileIdGet` |
| `/api/draft/v1/{draftUUID}/attachments/{fileId}` | DELETE | `apiDraftV1DraftUUIDAttachmentsFileIdDelete` |
| `/api/dar/v2/progress_report/{parentReferenceId}` | POST | `apiDarV2ProgressReportParentReferenceIdPost` |
| `/api/sam/register/self/info` | GET | `apiSamRegisterSelfInfoGet` |
| `/api/user/{userId}` | PUT | `apiUserUserIdPut` |
| `/api/dataset/v2/{id}` | GET | `apiDatasetV2IdGet` |
| `/api/dac/{dacId}/rules/{ruleId}/toggle` | PUT | `apiDacDacIdRulesRuleIdTogglePut` |

> **Legacy names** (e.g. `findDacById`, `deleteDac`, `approveCollection`) exist in older
> files and must not be used as models for new work. They are retained only for backward
> compatibility with generated clients.

---

## YAML Structure Rules

1. `operationId` **must be a direct peer** of `summary`, `description`, `tags`, and
   `parameters`. It must **never** be indented under `summary:` or any other block scalar.
2. Recommended field order inside an operation object:

   ```yaml
   get:          # or post / put / delete / patch
     operationId: apiExampleGet
     summary: Short one-line description
     description: |
       Longer description if needed.
     tags:
       - TagName
     parameters: ...
     requestBody: ...   # POST/PUT only
     responses: ...
   ```

3. Each `summary` for a given HTTP verb must accurately reflect **that verb's** action.
   Do not copy-paste the `summary`/`description` from one verb block to another.

---

## Checklist When Writing or Reviewing a Path YAML File

- [ ] `operationId` follows the `api<PathSegments><HttpVerb>` pattern (or the unauthenticated `<pathSegments><HttpVerb>` form for non-`/api` routes).
- [ ] `operationId` is unique across **all** files in `src/main/resources/assets/paths/` **and** all inline operations in `src/main/resources/assets/api-docs.yaml`.
- [ ] `operationId` is a direct map key — not nested inside a block scalar.
- [ ] `summary` and `description` match the operation's actual HTTP verb and behavior.
- [ ] `deprecated: true` operations include a pointer to the replacement route in `summary` or `description`.
- [ ] The path file is referenced in `src/main/resources/assets/api-docs.yaml`.

---

## Copy/Paste Prompt

```text
Update (or add) an operation in the Consent OpenAPI spec.

Goal:
<describe the endpoint change>

Route: <HTTP verb> <full path>
Location: src/main/resources/assets/paths/<filename>.yaml
  OR inline in src/main/resources/assets/api-docs.yaml (if the operation is not yet in a path file)

operationId to use:
<derive from the path-derived camelCase rule: api<PathSegments><HttpVerb>> for authenticated routes under /api
<derive from the path-derived camelCase rule: <PathSegments><HttpVerb>> for unauthenticated routes outside /api

Files to update:
- src/main/resources/assets/paths/<filename>.yaml  (preferred; extract from api-docs.yaml if currently inline)
- src/main/resources/assets/api-docs.yaml  (if new path reference or inline operation)

Constraints:
- operationId must follow the api<PathSegments><HttpVerb> pattern if under /api (for authenticated routes).
- operationId must follow the <PathSegments><HttpVerb> pattern when not served under /api (for unauthenticated routes).
- operationId must be unique across paths/ files AND inline operations in api-docs.yaml.
- operationId must be a direct peer of summary/description/tags — not nested inside a block scalar.
- summary and description must correspond to this verb's actual behavior (do not copy from another verb block).
- Deprecated operations must reference the replacement route.
- Do not introduce a new operationId style (no verb-first legacy names).
```

