# Architecture

## Overview

Consent is a Dropwizard service with a layered design and explicit separation of HTTP, business logic, and persistence concerns.

## Core Layers

- **Resource layer** (`http/resources`): JAX-RS endpoints, request validation, auth annotations, HTTP status mapping.
- **Service layer** (`http/service`): domain workflows and orchestration.
- **DAO layer** (`http/db`): JDBI interfaces and SQL-backed data access.
- **Model layer** (`http/models`): request/response and domain data models.

Representative classes:

- Resource: `UserResource`
- Service: `UserService`
- DAO: `UserDAO`
- Application bootstrap: `ConsentApplication`

## Request Lifecycle (Example)

1. Request enters `UserResource` (for example `/api/user/me`).
2. Resource validates/authenticates, then delegates to `UserService`.
3. Service applies business rules and calls `UserDAO`.
4. DAO executes database operations through JDBI.
5. Service maps results to API-facing models.
6. Resource returns JSON response.

## Dependency Injection and Wiring

- Guice modules provide constructor-injected dependencies.
- Resource registration and app startup wiring happen in `ConsentApplication`.
- Prefer constructor injection over static/service-locator patterns.

## Persistence and Migrations

- PostgreSQL is the primary datastore.
- Liquibase changelogs manage schema evolution.
- Keep schema and DAO changes in the same PR when possible.

## Error and Security Model

- Security is expressed at endpoint level (`@RolesAllowed`, `@PermitAll`).
- Errors are normalized through existing exception/error response patterns (`ErrorResource` and related models).

## Architectural Guardrails

- Keep business logic out of resources.
- Avoid DAO calls directly from resources.
- Preserve existing package and naming conventions unless a broader refactor is planned.
- Update `docs/API_GUIDELINES.md` and OpenAPI docs when API behavior changes.
