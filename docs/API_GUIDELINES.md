# API Guidelines

Use these rules when adding or changing endpoints in the Consent service.

## Endpoint Design

- Keep authenticated API endpoints under `/api` and only place intentionally public endpoints outside `/api`.
- Use nouns for resources and HTTP verbs for actions.
- Prefer plural collection routes (for example `/api/users`) and singular subresources only when semantically necessary.
- Keep path depth shallow and avoid action-heavy route names.

## Request and Response Conventions

- Use JSON request/response payloads.
- Reuse models from `org.broadinstitute.consent.http.models` when possible.
- Validate input at the boundary (resource layer) and return clear client-facing errors.
- Do not leak internal exceptions, SQL details, or stack traces in responses.

## Status Codes

- `200 OK`: successful read/update or idempotent operation.
- `201 Created`: successful creation.
- `204 No Content`: successful operation without body.
- `400 Bad Request`: malformed payload or validation failure.
- `401 Unauthorized`: caller is unauthenticated.
- `403 Forbidden`: caller lacks required permissions.
- `404 Not Found`: resource does not exist.
- `409 Conflict`: version/state conflict.
- `413 Content Too Large`: the request body exceeds an endpoint-specific size limit.
- `500 Internal Server Error`: unhandled server-side error.

## Security and Authorization

- Use `@RolesAllowed` or `@PermitAll` explicitly on endpoints.
- Apply least privilege and avoid broad role access by default.
- Keep authorization checks consistent between Resource and Service logic.

## Backward Compatibility

- Treat existing response fields as contract.
- Additive response changes are preferred over breaking removals/renames.
- If a breaking change is unavoidable, document rollout and migration notes in the same PR.

## Testing and Documentation Requirements

For API changes, include all of the following:

- Resource-level tests in `src/test/java`.
- Updated OpenAPI contract at `src/main/resources/assets/api-docs.yaml`.
- Relevant docs updates (`docs/ONBOARDING.md`, `docs/ARCHITECTURE.md`, or this file).
- Clear PR notes on behavior changes and compatibility impact.

---

Keep this file updated as new patterns emerge.
