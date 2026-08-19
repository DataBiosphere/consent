# Researcher Status Decoupling Plan

## Status

Proposed. Supersedes the initial draft design; revised after a repository-wide verification pass
that found four unclassified Library Card gates, a scope boundary error, and several factual
mistakes in the draft.

Revised a second time after an adversarial review of this plan found fifteen defects in it, all of
which were confirmed against source and are now resolved in the body below. See
[Review-Driven Revisions](#review-driven-revisions) for the finding-to-section map.

## Changes from the Ticket

Reviewers who have already read the ticket only need this section to know how the plan differs. The
plan is a superset of the ticket's objectives; nothing the ticket asked for was dropped, but the
following was added, corrected, narrowed, or deliberately made less prescriptive.

**Added — gates the ticket did not classify.** The ticket swapped four Java gates only. Four
SQL-level Library Card gates (`DatasetDAO.getApprovedDatasets`, `ResearcherDashboardDAO`,
`SigningOfficialDashboardDAO`, `ElectionDAO`) plus `TDRService` and `PassportService` also gate
access on card presence. Without them an active card-less researcher would still be denied approved
data, defeating the ticket's own objective, and a deactivated researcher would keep dashboard,
election, TDR, and Passport access. See [Current Behavior](#current-behavior).

**Added — audit table and Passport provenance.** The ticket specified a bare column plus backfill.
Passport visas assert *when* a researcher was vouched for and *by whom*, which today is read from
Library Card creation; once status is independent, that provenance needs its own source. Hence
`researcher_status_audit`, the seeded backfill rows, and the deterministic latest-activation query.

**Added — three-phase feature-flagged rollout.** The ticket implied a single coordinated ship. Because
the old UI's deactivate arm deletes a Library Card while the new backend reads status, any deployment
skew would let one surface silently disagree with the other, so backend and UI gates flip together
behind a flag. See [Rollout and Compatibility](#rollout-and-compatibility).

**Added — endpoint contract and self-activation guard.** The ticket sketched the endpoint body only.
The plan pins 400/403/404 handling, institution scoping, idempotency, and stripping `researcherStatus`
from generic user-update payloads so a researcher cannot activate themselves.

**Corrected — mapper change is required.** The ticket asserted no mapper work was needed because JDBI
bean-mapping would pass the column through. `UserWithRolesMapper` sets fields explicitly
(`setEmailPreference`, `setEraCommonsId`), so it must set `researcherStatus` too.

**Narrowed — deactivation does not revoke already-granted external access.** Deactivation removes the
researcher from the TDR approved-user list and withholds Passport visas going forward, but does not
claw back access already granted in TDR/Terra. The Signing Official table's existing copy ("suspend
their access to any data approved by a DAC") therefore overstates the immediate effect for
already-provisioned data. Revocation is listed under
[Explicitly Out of Scope](#explicitly-out-of-scope).

**Genericized — UI work is specified by behavior, not by line number.** The ticket named exact files
and lines (`ResearcherInfo` loading tri-state, the Library Card support link, the three access-button
tooltips, the `ResearcherStatus.tsx` state split, per-spec-file fixture edits, a `checkIsAdmin`
helper on `UserResource`). Those are all still in scope and are reached through the classification
sweep in [Library Card Reference Disposition](#library-card-reference-disposition); they are not
re-listed by line because the line numbers drift and the sweep is the guarantee of completeness.

**Unchanged from the ticket.** Collaborator Library Card validation, the DAA bulk-assignment
eligibility pool, "no renames in this change", and leaving the contractual Library Card Agreement
text and PDF filenames alone all carry over exactly as the ticket specified.

**Added after review — four items in neither the ticket nor the first revision.** Activation when
registration links a pre-issued Library Card; a bidirectional status/card reconciliation run in the
Phase-3 flip window; an admin researcher-status control on `AdminEditUser`, because after Phase 3
deleting a Library Card no longer deactivates anyone; and alignment of the status endpoint's Signing
Official scoping and failure code with the sibling SO-scoped endpoint on the same resource. Each is
marked as a decision in its own section so a reviewer can veto it.

## Review-Driven Revisions

Every row was confirmed against source before the plan was amended. Reviewers who read the first
revision only need this table plus the sections it points at.

| # | Defect in the previous revision | Resolved in |
| --- | --- | --- |
| 1 | `UserServiceDAO.createUser` links an SO's pre-issued card to a newly registered user; neither the backfill nor any card path set status, so that researcher stayed permanently inactive | [Library Card creation, DAA assignment, and registration](#library-card-creation-daa-assignment-and-registration) |
| 2 | Status was backfilled once at Phase 1 while card CRUD stayed authoritative until Phase 3, so every card issued or deleted in that window produced exactly the skew the rollout forbids | [Rollout and Compatibility](#rollout-and-compatibility) |
| 3 | Replacing `hasLibraryCard` with status would NPE in `needsLibraryCardRemovedForUser`, which dereferences the card inside that guard — for precisely the active card-less users enforcement must cover | [Institution and domain enforcement](#institution-and-domain-enforcement) |
| 4 | The admin `Manage Library Cards` delete button was unclassified; after Phase 3 it would silently stop deactivating | [duos-ui Changes](#duos-ui-changes) |
| 5 | The prescribed duos-ui sweep regex could not match snake_case `library_cards`, missing the file that defines the route the Definition of Done requires proving unchanged | [Library Card Reference Disposition](#library-card-reference-disposition) |
| 6 | The prescribed consent sweep regex missed the camelCase OpenAPI surface, DI wiring, `lc_daa` joins, and all of `src/test` | [Library Card Reference Disposition](#library-card-reference-disposition) |
| 7 | The `so`/`system` provenance mapping was described as the "issuer" and attached to `asserted()`; it actually lives in the visa `by` claim, and `ResearcherStatus.by()` hardcodes `so` | [Passport](#passport) |
| 8 | `researcherStatus` is already taken on the consent API by `SigningOfficialDashboardSummary.ResearcherStatus`, and the SO-dashboard service/DTO layer was never classified | [Names already in use](#names-already-in-use) |
| 9 | `ResearcherStatusRequiredException` had no status code, message, or superclass, and `Resource.DISPATCH` is an exact-class map — an unregistered subclass returns 500 | [Status endpoint, exception, and service](#status-endpoint-exception-and-service) |
| 10 | Defining activation provenance as the latest `new_status = true` row without requiring current status makes a deactivated user's Affiliation-and-Role visa assert a revoked SO vouch | [Passport](#passport) |
| 11 | "Strip `researcherStatus` from generic user-update JSON" misdescribed the mechanism: `UserUpdateFields` is a typed allowlist, so the guard is *not adding* the field | [Migration, model, and audit](#migration-model-and-audit) |
| 12 | One of the three enforcement removal branches is a bare DAO call with no transaction, so the required transactional status write had nowhere to attach | [Institution and domain enforcement](#institution-and-domain-enforcement) |
| 13 | The endpoint's SO scoping and 403 contradicted `signingOfficialMeetsRequirements` on the same resource, which requires the SO's own institution and returns 400 | [Status endpoint, exception, and service](#status-endpoint-exception-and-service) |
| 14 | The rollout invented a "release coordinator" and never named the DB-backed feature-flag mechanism consent already ships, so no ticket defined the flag key | [Rollout and Compatibility](#rollout-and-compatibility) |
| 15 | `SigningOfficialDashboardDAO` has a second, independent `library_card` gate (`researchers_approved`) that no table classified, and the cited line number was wrong | [Current Behavior](#current-behavior) |

## Objective

Introduce a persisted `users.researcher_status` boolean that independently controls researcher
eligibility, and reduce the Library Card to what it already is structurally: a container for DAA
pre-authorization.

After this change:

- an active researcher may have no Library Card;
- an inactive researcher may retain a Library Card and DAA assignments;
- every authorization-sensitive check reads persisted status at request time rather than inferring
  eligibility from Library Card presence.

## Scope and Repository Boundary

This change spans two repositories:

| Repository | Path | Stack |
| --- | --- | --- |
| `consent` | `/workspaces/consent` | Java 25, Dropwizard, Jdbi, Liquibase, Maven wrapper (`./mvnw`) |
| `duos-ui` | `/workspaces/duos-ui` | React 19, TypeScript, Vite, pnpm; Vitest unit/browser tests, Playwright e2e |

`consent` holds no frontend source (no `package.json`, no JS/TSX). All user-interface work lives in
`duos-ui` and is specified in [duos-ui Changes](#duos-ui-changes).

Deployment uses the three feature-flag phases in [Rollout and Compatibility](#rollout-and-compatibility).
The backend and UI gates switch together; a consent-first behavior change is not safe on its own.

## Core Model

```mermaid
flowchart LR
  Status[users.researcher_status] --> DAR[New DAR draft, submission, update]
  Status --> ERA[ERA credential validation]
  Status --> Dataset[Approved-dataset APIs and dashboards]
  Status --> TDR[TDR approved-user list]
  Status --> Passport[Researcher Status and Controlled Access Grants visas]
  Status --> Elections[Card-holding-user election filtering]
  LibraryCard --> Collab[Collaborator DAA pre-authorization]
  LibraryCard --> Eligible[Existing DAA bulk-assignment pool]
  Reg[Registration links a pre-issued card] --> Status

  LibraryCard[Library Card] --> DAAs[DAA assignments]
  DAAs --> SO[SO-preauthorization evaluation]
  DAAs --> Ack[DAA acknowledgement validation]
```

```mermaid
stateDiagram-v2
  [*] --> Inactive: new user, no pre-issued card / migration default
  [*] --> Active: registration links a pre-issued Library Card
  Inactive --> Active: SO or admin activates
  Active --> Inactive: SO or admin deactivates
  Active --> Inactive: institution/domain enforcement

  note right of Inactive
    Manual deactivation retains
    Library Card and DAA assignments.
    The one-time Phase-3 reconciliation
    is the only other writer.
  end note
```

## Current Behavior

Verified against `develop` at `e20fe76c2`. Line references are current as of that commit.

### Library Card reads that this change touches

Java-level reads. The first eight are eligibility gates; the last three are not gates but derive
behavior from card presence and must be handled anyway:

| Location | Behavior |
| --- | --- |
| `service/DataAccessRequestService.java:182` | DAR draft creation; throws `LibraryCardRequiredException` |
| `service/DataAccessRequestService.java:525` | `validateCommonDarAndProgressReportElements`; throws `NIHComplianceRuleException` |
| `resources/DataAccessRequestResource.java:634` | `checkAuthorizedUpdateUser`; throws `LibraryCardRequiredException` |
| `service/UserService.java:387` | `validateActiveERACredentials`; throws `LibraryCardRequiredException` |
| `service/DataAccessRequestService.java:723` | `findCollaboratorsWithoutLibraryCards`; registration + card check, no DAA inspection |
| `service/UserService.java:242-243` | `findAllUsersWithInstitutionAndLibraryCard`, the DAA bulk "eligible users" pool |
| `service/TDRService.java:85` | `libraryCardDAO.findByUserEmails` defines the TDR approved-user list |
| `service/feature/InstitutionAndLibraryCardEnforcement.java:200` | `hasLibraryCard`, the trigger for all enforcement removal branches. **It is not substitutable:** `needsLibraryCardRemovedForUser:167` dereferences `user.getLibraryCard().getCreateUserId()` inside that guard. |
| `service/dao/UserServiceDAO.java:56-66` | `createUser` links a Library Card pre-issued by email (`user_id` NULL) to the newly registered user, inside the registration transaction. This is today's onboarding activation path and is invisible to a card-presence backfill. |
| `service/passport/ResearcherStatus.java:26,46` | `asserted()` reads the Library Card create date; `by()` hardcodes `so` |
| `service/passport/AffiliationAndRole.java:28,53` | `asserted()` reads the Library Card create date; `by()` returns `so` when a card exists and `system` otherwise |

SQL-level gates. **These were absent from the draft design and are the reason an active card-less
researcher would otherwise be denied access:**

| Location | Behavior |
| --- | --- |
| `db/DatasetDAO.java:619` | `getApprovedDatasets` — `INNER JOIN library_card` |
| `db/ResearcherDashboardDAO.java:125` | `EXISTS (SELECT 1 FROM library_card …)` |
| `db/SigningOfficialDashboardDAO.java:12,18` | `(lc.id IS NOT NULL) AS active` in the `institution_users` CTE (line 12), fed by the `LEFT JOIN library_card` at line 18 → `activeResearchers` / `inactiveResearchers` |
| `db/ElectionDAO.java:83` | `findElectionsWithCardHoldingUsersByElectionIds` — `INNER JOIN library_card` |

`DatasetDAO.getApprovedDatasets` is the single query behind three surfaces the draft treated as
independent: `GET /api/user/me/researcher/datasets` (`resources/UserResource.java:523` →
`service/DatasetService.java:459`) and the Passport Controlled Access Grants visas
(`service/passport/PassportService.java:50`).

`SigningOfficialDashboardDAO` contains a **second, independent** `library_card` gate that is not a
status gate and must not be touched: `researchers_approved` (lines 117-121) joins
`institution_users → library_card → lc_daa → assignable_daas` and feeds
`daaAssociations.researchersApproved`. It is genuine DAA pre-authorization counting. Changing the
`active` projection must not remove or re-scope the `lc` alias this second aggregate depends on.

### Names already in use

Three names collide with the obvious choice and must be disambiguated before implementation:

- `models/SigningOfficialDashboardSummary.java:7` already exposes `researcherStatus`, an object —
  `record ResearcherStatus(long active, long inactive)` — required by
  `assets/schemas/SigningOfficialDashboardSummary.yaml`. Adding a boolean `User.researcherStatus`
  is safe in Java but collides in generated clients and docs, so both must be documented; the
  dashboard record's name and shape stay as they are.
- `service/passport/ResearcherStatus.java` is a visa claim type, unrelated to either.
- The SO dashboard response is shaped by `SigningOfficialDashboardService` and
  `SigningOfficialDashboardSummary`, not by the DAO alone. Both are in scope for the `active` /
  `inactive` source change even though only the DAO holds SQL.

## Implementation Design

### Migration, model, and audit

Create a registered date-named Liquibase changeset with strictly ordered change sets:

1. Create `researcher_status_audit`, its foreign keys to `users`, a source check constraint for
   `SIGNING_OFFICIAL`, `ADMIN`, and `INSTITUTION_ENFORCEMENT`, and index
   `(user_id, action_date DESC, researcher_status_audit_id DESC)`.
2. Add `users.researcher_status BOOLEAN NOT NULL DEFAULT false`.
3. Backfill `true` for users with a Library Card.
4. Seed a `false → true` audit row for every backfilled user, using the Library Card creation time,
   creator, and `SIGNING_OFFICIAL` source.

The table must exist before any audit seed. Audit rows are transition-only.

Audit `source` keeps exactly the three values `SIGNING_OFFICIAL`, `ADMIN`, and
`INSTITUTION_ENFORCEMENT`. Rows written by the backfill seed, by registration-time card linkage, and
by the Phase-3 reconciliation all use `SIGNING_OFFICIAL`, because each records an action a Signing
Official actually took through the Library Card UI. Do not add a fourth source value; do not let the
check constraint be discovered by a failing insert.

Add `researcherStatus` to `User`, `User.yaml`, all relevant User/DAR projections, and
`UserWithRolesMapper` — the mapper sets fields explicitly (`setEmailPreference`, `setEraCommonsId`)
and will not pick the column up on its own.

**Self-activation guard.** Both `PUT /api/user/{id}` and the `@PermitAll`
`PUT /api/user` (`updateSelf`) deserialize through `gson.fromJson(json, UserUpdateFields.class)`, a
typed allowlist of `displayName`, `emailPreference`, `userRoleIds`, `eraCommonsId`, `daaAcceptance`,
and `userData`. An unknown `researcherStatus` key is therefore already dropped and there is nothing
to strip. The guard is a prohibition, not an addition: **do not add `researcherStatus` to
`UserUpdateFields`**, or `updateSelf` becomes a self-activation hole. duos-ui's `User.ts` `unset()`
list needs no new entry for the same reason.

### Status endpoint, exception, and service

Add `PUT /api/user/{userId}/researcherStatus`, restricted to ADMIN and SIGNINGOFFICIAL, with exactly
`{ "researcherStatus": boolean }`. Reject malformed, missing, or non-boolean values with 400; return
404 for unknown users; and return the updated user on success. Idempotent writes return 200 without an
audit row.

**Signing Official scoping — decision, previously specified as 403.** `api/user` already carries an
SO-scoped endpoint, `UserResource.signingOfficialMeetsRequirements:302-309`, which requires the
*acting* SO to have a non-null `institutionId`, permits a *target* whose `institutionId` is null, and
returns **400** on any failure. To avoid two contradictory SO-scoping rules and two failure codes on
one resource, this endpoint reuses that predicate's shape and returns **400**, superseding the 403 in
the previous revision. It diverges from the sibling in one respect, deliberately: the target's
institution must match the acting SO's, rather than being allowed to be null. Activating a user with
no institution would immediately be undone by
[institution and domain enforcement](#institution-and-domain-enforcement), so permitting it would
only produce a confusing round trip. A reviewer who prefers strict parity with the sibling endpoint
should say so before ticket 3.

**Exception contract.** `ResearcherStatusRequiredException extends UnprocessableEntityException` with
a `MESSAGE` telling the researcher their status is inactive and to contact their Signing Official —
not to obtain a Library Card. `Resource.createExceptionResponse:252` does `DISPATCH.get(e.getClass())`
on a `HashMap` with no superclass walk, so an unregistered subclass of `UnprocessableEntityException`
falls through to `Response.serverError()` and returns **500** with the message leaked. Registering the
new class in `DISPATCH` for **422** is therefore load-bearing, not bookkeeping, and needs its own test.
duos-ui keys an element on `id="libraryCardRequired"` (`ResearcherInfo.tsx:129`); if that id is renamed
with the copy, its specs move in the same commit.

Implement persistence-backed `UserService.isActiveResearcher` and `requireActiveResearcher`, plus a
transactional status transition helper that writes the audit row with the appropriate source. Add the
OpenAPI path and register it in `api-docs.yaml`.

### Authorization Gate Changes

Behind the Phase-3 backend flag, replace Library Card eligibility conditions with persisted status for
DAR draft creation, authorized update, ERA credential validation, the shared
`validateCommonDarAndProgressReportElements` condition (retaining `NIHComplianceRuleException`), the
approved-dataset API, researcher dashboard, Signing Official active/inactive dashboard projection,
election filtering, TDR lookup, and Passport. `DatasetDAO` remains the shared source for the
approved-dataset API and Passport grants. Institution enforcement is a status *writer*, not a simple
condition swap; its `hasLibraryCard` trigger stays as it is, for the reasons in
[Institution and domain enforcement](#institution-and-domain-enforcement).

Collaborator validation remains its existing registered-user and Library Card validation. DAA bulk
assignment retains its existing Library Card-based eligibility pool. These are deliberately excluded
from the status substitutions. Retain genuine DAA joins and all Library Card APIs, entities, tables,
routes, page/component files, and identifiers.

`isUserPreAuthorizedForAllDaas` must return false for a missing Library Card so active card-less users
fall back to the existing SO-approval flow instead of throwing.

### Passport

- Reload persisted status before issuing visas. Inactive researchers receive Affiliation-and-Role only;
  withhold Researcher Status and Controlled Access Grants visas.
- Passport activation provenance applies **only while the user is currently active**, and is exactly
  the latest audit row for the user with `new_status = true`, ordered by
  `action_date DESC, researcher_status_audit_id DESC`.
- Two distinct visa fields are involved, and the previous revision conflated them. `source()` returns
  `PassportService.ISS` and does not change. The `so` / `system` value is the **`by()`** claim:
  `ResearcherStatus.by():46` currently hardcodes `VisaBy.SO`, and `AffiliationAndRole.by():53`
  currently branches on `getLibraryCard() == null`. Both must instead derive from the activation row:
  `SIGNING_OFFICIAL` maps to `so`; `ADMIN` and `INSTITUTION_ENFORCEMENT` map to `system`. Editing only
  `asserted()` leaves the mapping inert.
- `ResearcherStatus.asserted():26` and `AffiliationAndRole.asserted():28` use the activation timestamp
  instead of `LibraryCard::getCreateDate`.
- For a currently **inactive** user, Affiliation-and-Role is still issued but must not assert a
  revoked vouch: use `by = system` and the user creation timestamp, never the timestamp of an
  activation that has since been reversed. The same fallback applies to an active user with no
  activation row at all.

### Institution and domain enforcement

Enforcement writes status; it does not read it as a gate. Three constraints, all of which the previous
revision got wrong or left open:

- **`hasLibraryCard` stays.** It guards `needsLibraryCardRemovedForUser:165-181`, which dereferences
  `user.getLibraryCard().getCreateUserId()` to find the issuing SO's institution. Substituting
  persisted status there would NPE on exactly the active card-less users this plan creates. Card
  removal remains keyed on card presence.
- **Status deactivation keys on the institution/domain mismatch itself,** independently of card
  presence, so an active card-less user whose domain no longer matches is deactivated by the
  asynchronous sweep. That code path must not dereference the Library Card.
- **Both removal branches become transactional.** `handleUserWithInstitutionInMap:154` and
  `dropLCAndInstitutionForUser:212` already route through
  `UserServiceDAO.updateInstitutionAndClearLibraryCardForUser`, which is a `jdbi.useTransaction`
  block — extend it to write status and the audit row inside that transaction. The branch at
  `handleUserWithInstitutionInMap:158` is a bare `libraryCardDAO.deleteAllLibraryCardsByUser(userId)`
  with no transaction at all; it needs a new `UserServiceDAO` method, e.g.
  `clearLibraryCardAndDeactivateResearcherTxn(userId)`, so the card delete and the status/audit write
  cannot half-apply. Deactivation-only (no card to remove) needs the same treatment via the
  transactional transition helper.

Audit rows here use source `INSTITUTION_ENFORCEMENT` and remain transition-only. Existing Library
Card-removal behavior is otherwise unchanged.

### Library Card creation, DAA assignment, and registration

`LibraryCardService.createLibraryCard`, `DaaServiceDAO` bulk assignment, and the DAA resource retain
their current Library Card creation behavior. They must not write `researcher_status` or create a
researcher-status audit row. Assignment can therefore create a card for an inactive user without
reactivating that user.

**Registration is the one carve-out — decision.** `UserServiceDAO.createUser:56-66` looks up a
Library Card previously issued against the user's email (`user_id` NULL) and links it to the new
`user_id` inside the registration transaction. That is how a researcher whose SO pre-issued a card
becomes able to submit today. A card-presence backfill cannot reach those rows, and a blanket "card
paths never write status" rule would leave every such researcher permanently inactive after Phase 3.
So when — and only when — `createUser` finds and links a pre-issued card, it also sets
`researcher_status = true` and writes a `false → true` audit row with source `SIGNING_OFFICIAL`, in
that same transaction. The alternative considered was requiring the SO to re-toggle after the
researcher registers; it was rejected because it silently breaks an onboarding flow that works today
and gives the SO no signal that action is needed.

## Library Card Reference Disposition

Every existing Library Card reference must be classified before implementation:

| Reference class | Disposition |
| --- | --- |
| DAR draft/update, ERA, shared DAR/progress-report validator | Switch to status in Phase 3; the shared validator keeps `NIHComplianceRuleException`. |
| `DatasetDAO`, researcher and SO dashboards, `ElectionDAO`, TDR, Passport, institution enforcement | Apply the status changes described above, behind the backend Phase-3 flag. |
| Collaborator validation and DAA bulk assignment eligibility | Unchanged Library Card behavior. |
| DAA assignment, acknowledgement, `LibraryCardService`, resource, DAO, models, API, entity, and table | Unchanged DAA-container behavior. |
| `isUserPreAuthorizedForAllDaas` | Retain the DAA comparison and add only a null-card guard. |
| User and DAR projections | Continue hydrating Library Card data and add `researcherStatus`. |
| `service/dao/UserServiceDAO.createUser` | Card linkage is retained **and** activates the user with an audit row — the single card-to-status coupling that remains. |
| `SigningOfficialDashboardDAO.researchers_approved` (lines 117-121) | Unchanged DAA pre-authorization counting; keep the `lc` alias the aggregate depends on. |
| `SigningOfficialDashboardService`, `SigningOfficialDashboardSummary` | In scope for the `active` / `inactive` source change; record name and shape unchanged. |
| `UserUpdateFields` | Unchanged, deliberately. `researcherStatus` must never be added to it. |
| `AdminManageLC.tsx`, `LibraryCardTable.tsx:137` | Stays Library Card management (DAA pre-authorization). Copy must stop reading as deactivation, and admins get a status control on `AdminEditUser` instead. |
| `service/passport/ResearcherStatus.by()`, `AffiliationAndRole.by()` | Switch from card presence to audit-derived provenance; `source()` unchanged. |
| `LibraryCardRequiredException` | All three throw sites move to `ResearcherStatusRequiredException`, leaving the class unused. Keep it and its 422 registration through Phases 1-2 so a flag rollback still returns 422, then delete the class, its import, and its registration in the Phase 3 cleanup. |

Re-run this classification sweep in both repositories before implementation and classify any new hit:

```text
# consent: case-insensitive so camelCase, PascalCase and snake_case all match; resources cover
# api-docs.yaml and the schemas; src/test is included because the Test Matrix depends on it.
rg -ni "librarycard|library_card|lc_daa|librarycardrequired" src/main src/test

# duos-ui: no glob filter — the route-defining file contains only the snake_case literal
# (signingOfficialConsoleRoutes.ts), and .css/.json hits were previously invisible.
rg -ni "librarycard|library_card" src test
```

The previous revision's commands could not reproduce its own verification pass. The consent regex was
case-sensitive and scoped to `src/main`, so it missed the `/api/libraryCards*` OpenAPI paths and
`LibraryCard.yaml` in `assets/api-docs.yaml`, `LibraryCardService` wiring in `ConsentModule`,
`User.setLibraryCard`, the `lc_daa` joins, and every test. The duos-ui regex could not match
`library_cards`, so it returned nothing for
`pages/signing_official_console/signingOfficialConsoleRoutes.ts` — the file that defines the very
route the Definition of Done requires proving unchanged.

## duos-ui Changes

### Types, status display, and toggle

- Add `researcherStatus` to `DuosUser`, and add `User.setResearcherStatus(userId, researcherStatus)`
  for `PUT /api/user/{userId}/researcherStatus`. Do not add the field to any generic user-update
  payload type; the backend allowlist already drops it, and the status endpoint is the only writer.
- The existing Signing Official `library_cards` route, page file, component, action identifiers, and
  Library Card implementation names remain unchanged. Its displayed switch state and copy use
  `researcherStatus`.
- In Phase 3 both existing toggle arms call the status endpoint and update the local status value;
  neither invokes Library Card creation or deletion. Deactivation leaves the row, its Library Card,
  and DAA assignments intact. Remove Library Card Agreement copy from the activation confirmation;
  retain it for actual DAA assignment.
- Researcher profile, access buttons, DAR UI, voting restriction, controlled-access-grants handling,
  and admin status display use `researcherStatus`. Keep Library Card/DAA display and management where
  it is genuinely pre-authorization behavior. Update copy to distinguish active researcher status
  from DAA pre-authorization.
- Do not rename the `library_cards` route or introduce redirects, file/component renames, frontend
  type renames, Library Card API/entity/table renames, or other identifier renames.

### Admin surfaces

The SO console is not the only place a Library Card is deleted. `pages/AdminManageLC.tsx` renders
`components/library_card_table/LibraryCardTable.tsx`, whose delete button calls
`LibraryCardAPI.deleteLibraryCard(id)` (`:137`). After Phase 3 that call no longer deactivates
anyone, so an admin would delete a card, watch the row disappear, and leave the researcher with full
DAR, approved-dataset, and Passport access.

- Keep the delete behavior — under the new model it correctly removes DAA pre-authorization only —
  and change the surrounding copy so it cannot be read as deactivation.
- **Decision, new scope:** give admins a researcher-status control on `pages/AdminEditUser.tsx`
  (route `/admin_edit_user/:userId`), the existing per-user admin surface for roles and institution.
  The endpoint already permits ADMIN, and `AdminManageLC` is a card-oriented list, not a user editor.
  Without this, Phase 3 removes the only admin deactivation path in the product.

### Session freshness

After an operator changes their own status, refresh stored current-user state. Client state is a hint;
backend gates reload persisted status.

## Rollout and Compatibility

Use the feature-flag mechanism consent already ships: `FeatureFlagService` / `FeatureFlagDAO` /
`FeatureFlag`, exposed by `PublicFeatureFlagResource` at `GET /feature/{key}` and consumed by duos-ui
through `libs/ajax/FeatureFlag.ts getFeatureFlag` (precedent: `NHGRI_RESTRICTED_DAC`). Use a single
key, `RESEARCHER_STATUS_GATING`. Because both sides read the same database row, the flip is already
atomic and no release-coordination machinery is needed. Note that `/feature` is `@PermitAll`, so the
key and its value are publicly readable; that leaks only the existence of the feature, which is
acceptable here. No deployment window may expose a Library Card operation as a researcher-status
operation:

```mermaid
sequenceDiagram
  participant C as Consent
  participant U as duos-ui
  participant F as Feature flag

  C->>C: Deploy schema, endpoint, projections, audit support
  Note over C: Existing Library Card gates remain active
  U->>U: Deploy status-capable UI with toggle disabled
  F->>C: Enable researcher-status backend gates
  F->>U: Enable status toggle and status-based UI gates
```

1. **Phase 1 — compatibility backend.** Deploy the audit table then status column/backfill/audit seed,
   endpoint, projections, Passport support, and compatibility fields. The current Library Card
   eligibility gates remain authoritative. The new endpoint is available but no UI exposes a
   status-changing action.
2. **Phase 2 — disabled UI.** Deploy duos-ui status-capable code behind a disabled feature flag.
   The existing Library Card CRUD toggle remains authoritative while disabled.
3. **Phase 3 — coordinated enablement.** Reconcile status against card presence, then flip
   `RESEARCHER_STATUS_GATING` on, enabling the backend gates and the UI status toggle together. The old
   UI must no longer be able to call Library Card deletion as a status action.

**Bidirectional reconciliation at the flip (required).** The Phase-1 backfill is a point-in-time
snapshot, and Library Card CRUD stays authoritative through Phases 1-2 while card paths are forbidden
from writing status. Every card issued in that window leaves status `false`, and every card deleted in
that window leaves status `true` — the first silently strips access at the flip, the second silently
preserves access for someone an SO already deactivated. So the flip window must run a reconciliation
that sets status **in both directions**. The two populations to reconcile are:

```sql
-- to activate: status false but a card exists (card issued during Phases 1-2)
SELECT u.user_id FROM users u
WHERE u.researcher_status = false
  AND EXISTS (SELECT 1 FROM library_card lc WHERE lc.user_id = u.user_id);

-- to deactivate: status true but no card (card deleted during Phases 1-2)
SELECT u.user_id FROM users u
WHERE u.researcher_status = true
  AND NOT EXISTS (SELECT 1 FROM library_card lc WHERE lc.user_id = u.user_id);
```

These are selection queries, not the write. Reconciliation **must not** `UPDATE users` directly, or it
produces unaudited transitions in violation of the audit invariant: apply each flip through the same
transactional status transition helper the endpoint uses, so the status write and its transition-only
audit row (source `SIGNING_OFFICIAL` — each records a card action an SO took through the old UI) commit
together. Alternatively, do it in SQL with `WITH flipped AS (UPDATE users … RETURNING user_id) INSERT
INTO researcher_status_audit …` in a single transaction. Either way it is idempotent, re-runnable, and
runs in the same window as the flag flip so nothing lands between reconciliation and enablement.

**Phase 1-2 writes through the endpoint are provisional.** The endpoint is live from Phase 1 with no UI
exposing it, and the deactivating half of the reconciliation will revert any card-less activation made
in that window. Treat manual endpoint use before Phase 3 as testing only, and have the reconciliation
log every user it flips so an unexpected revert is visible rather than silent.

## Jira-Ready Tickets

| # | Ticket | Depends on |
| --- | --- | --- |
| 1 | Audit table first, then status column, card backfill, and audit seed | — |
| 2 | Model, projections, JSON write guard, schema | 1 |
| 3 | Transactional status transition helper and status endpoint/OpenAPI | 1, 2 |
| 4 | Status-gate flag, Java/SQL gates, Passport, TDR, and institution enforcement | 2, 3 |
| 5 | Preserve collaborator and DAA-bulk Library Card behavior; add null-card preauthorization guard | 2 |
| 6 | duos-ui status transport and feature-flagged status UI, including the `AdminEditUser` status control and `AdminManageLC` copy | 2, 3 released |
| 7 | Bidirectional reconciliation script, `RESEARCHER_STATUS_GATING` flag row, coordinated Phase-3 enablement, and regression verification | 4, 6 |
| 8 | Registration-time activation in `UserServiceDAO.createUser` | 1, 2 |
| 9 | Transactional enforcement branches (new `UserServiceDAO` method) and card-independent status deactivation | 3 |
| 10 | Passport `by()` / `asserted()` provenance from the audit table, including the inactive-user fallback | 1, 2 |

## Test Matrix

Migration and audit:

- prove `researcher_status_audit` exists, including FKs, source constraint, and the
  `(user_id, action_date DESC, researcher_status_audit_id DESC)` index, before audit seeding;
- prove carded users backfill active, card-less users remain inactive, and seeds are correct;
- prove Passport selects the latest `new_status = true` audit row, including tied timestamps, and
  applies the specified `by` value and no-audit fallback;
- prove the reconciliation is bidirectional and idempotent: a card issued during Phases 1-2 ends
  active, a card deleted during Phases 1-2 ends inactive, re-running writes no further audit rows,
  and every row it writes satisfies the `source` check constraint;
- prove registration with a pre-issued card ends with the card linked, status active, and one
  `false → true` audit row, all committed in the same transaction — and that registration with no
  pre-issued card leaves the user inactive.

Status and authorization:

- endpoint authorization, validation, institution scope (acting SO without an institution, target in
  another institution, target with no institution, admin unrestricted), the 400 failure code,
  idempotency, transaction rollback, and transition-only audit writes;
- `ResearcherStatusRequiredException` returns 422 with its own message through
  `Resource.createExceptionResponse` — a registration miss must fail this test, not surface as 500;
- `PUT /api/user` (`updateSelf`) with a `researcherStatus` key in the body leaves status unchanged;
- all listed Java and SQL status gates, including inactive users receiving the existing
  `NIHComplianceRuleException` outcome through the shared validator for DARs, progress reports, and
  closeouts;
- inactive Passport users retain Affiliation-and-Role only; active users receive the withheld visas.

DAA and Library Card regression:

- creating a Library Card or assigning a DAA does not activate an inactive researcher and creates no
  activation audit row;
- status toggle is the only manual activation/deactivation path; toggling preserves Library Cards and
  DAA assignments;
- collaborator checks remain Library Card-based and the existing DAA bulk-assignment eligibility
  behavior remains unchanged;
- an active card-less user falls back to SO approval without a null-pointer failure;
- the enforcement sweep handles an active card-less user with a mismatched domain: status is
  deactivated, no Library Card is dereferenced, and no `NullPointerException` occurs;
- the untransactioned enforcement branch is covered: a failure between card removal and the status
  write leaves neither applied;
- `SigningOfficialDashboardSummary.daaAssociations.researchersApproved` is unchanged by the `active` /
  `inactive` source change;
- an admin deleting a Library Card does not change researcher status, and the `AdminEditUser` control
  does.

Rollout and UI:

- Phase 1 preserves old UI Library Card behavior while backend Library Card gates remain authoritative;
- Phase 2 hides/disables status changes; Phase 3 enables backend and UI status gates together and
  verifies neither toggle arm creates or deletes a Library Card;
- UI status surfaces use `researcherStatus`, while Library Card/DAA surfaces remain pre-authorization
  behavior;
- prove no route, component, frontend type, Library Card API, entity, table, or identifier rename is
  introduced; specifically `/signing_official_console/library_cards` remains the route.

Run the relevant consent Maven tests, duos-ui type check and Vitest/browser tests, and a cross-repository
smoke test covering Phase 1, Phase 3 deactivation/reactivation, and DAA assignment after deactivation.

## Explicitly Out of Scope

- Revoking already-granted external access (TDR, Terra) on deactivation.
- Status-change notification emails.
- A researcher-status history endpoint. The audit table supports one later.
- Renaming the Library Card implementation to `DaaAuthorization`. A later rename-only effort may do
  this; contractual Library Card Agreement text and PDF filenames stay unchanged regardless.

## Definition of Done

- The ordered audit migration, backfill, and seed are complete and status changes are audited only on
  transitions.
- Researcher eligibility gates use persisted status only after coordinated Phase 3 enablement; Phase 1
  retains the existing Library Card gates.
- Collaborator checks and DAA bulk-assignment eligibility retain their existing Library Card behavior.
- DAA assignment never changes researcher status; explicit status endpoint actions are the manual
  activation/deactivation path.
- Passport uses the precise latest-activation audit query and preserves only Affiliation-and-Role for
  inactive researchers.
- Phase 3 toggles preserve Library Cards and DAAs, and no status action calls the Library Card deletion
  flow.
- Registration with a pre-issued Library Card yields an active researcher; no other card path writes
  status.
- The Phase-3 flip is preceded by a bidirectional reconciliation, and `RESEARCHER_STATUS_GATING` is the
  single flag both repositories read.
- Enforcement branches write status and their audit row transactionally, and no code path dereferences
  a Library Card that may be absent.
- Admins retain a deactivation path (`AdminEditUser`), and `AdminManageLC` no longer reads as
  deactivation.
- The `library_cards` route and Library Card implementation names remain unchanged; only displayed
  status behavior and copy change.
