# Data Showcase Behind Sign-in Plan

## Status

Proposed. Ticket: [DT-4499](https://broadworkbench.atlassian.net/browse/DT-4499).

Related plans: [`data-showcase-delivery-plan.md`](data-showcase-delivery-plan.md) (the MVP, built
around an anonymous lookup) and [`data-showcase-contracts.md`](data-showcase-contracts.md) (the S2.1
field list and its approval gate).

## Overview

Showcase pages move behind sign-in, and their catalog lookup moves to the authenticated `/api`. A
signed-in user then sees only catalog data they can already read, so the launch no longer waits on
the AppSec and data-steward approval of an anonymous exposure.

### How the showcase reads data today

- `/data` and `/showcase/:key` render for anyone. Their dataset shelves and hero counts call
  Consent's anonymous `POST /showcase/datasets`, through the BFF public proxy
  (`/public/showcase/datasets`) or directly with CORS in legacy mode.
- The lookup returns the nine S2.1 fields (`ShowcasePublicField`) for eligible datasets: a public
  study, and open or external access or DAC approval with a live DAC.
- The route is built but switched off (`showcase.publicLookupEnabled`).
  [DT-4466](https://broadworkbench.atlassian.net/browse/DT-4466), which turns it on, has been blocked
  since 2026-11-13 on the S2.1 gate: the field list, the free-text `species` and the `scope`
  vocabulary need approval from AppSec and the data stewards for anonymous callers. Every dataset
  shelf shows "These datasets can't be loaded right now", and the S4.3 health check
  ([DT-4359](https://broadworkbench.atlassian.net/browse/DT-4359)) is blocked behind it.

### The change

- **Consent:** `POST /api/showcase/datasets` with the same request contract, service, projection and
  eligibility rule, for any signed-in user.
- **duos-ui:** the showcase routes go inside `Authenticated`, and the catalog client calls the new
  route with the user's credentials.

The anonymous route, its rate limit and its CORS and proxy settings stay in the code, switched off.

## Decisions

### Signed-in users see nothing new

Self-registration gives a user the Researcher role. For a public study, such a user can already
read every S2.1 value:

| Field | Already readable through |
| --- | --- |
| `identifier`, `title`, `accessManagement`, `consentCodes`, `participantCount`, `accessUrl`, `species` | `GET /api/dataset/v2/{id}` (the full `Dataset` and `Study`, visibility by `DatasetService.verifyPublicVisibilityAccess`) |
| `dacName` | The dataset's `dacId` and `GET /api/dac` (Researcher role allowed) |
| `availableDate` | Open and external: the dataset's `createDate`. Controlled: the day of `dac_approval_date`, which no endpoint returns but the daily NewStudyDigest email discloses. Every user with email turned on gets the public studies whose datasets were DAC-approved in the previous 24 hours (`DatasetDAO.getRecentDacApprovedDatasetStudyIds`, `UserDAO.allEmailReceivingThinlyPopulatedUsers`). |

The showcase eligibility rule is narrower than the readers above: it needs a public study and grants
no admin, creator or custodian exception. So the authenticated route keeps the S2.1 projection and
eligibility rule unchanged, and needs no new field approval. The S2.1 gate stays with DT-4466, for
anonymous callers only.

### Reuse the showcase lookup, not the Data Library search

The new route calls `ShowcaseCatalogService` and the same DAO queries. It does not read from
`POST /api/dataset/search/index`, which passes the caller's Elasticsearch query through and leaves
the visibility filter to duos-ui (`elasticsearch-service-duos-ui-usage.md`).

### Fully behind sign-in, or "sign in to see more"

This plan's tasks assume the pages are fully behind sign-in, the intent of the ticket. The
alternative keeps the pages public and shows the dataset shelves and hero counts only after sign-in.

| | Fully behind sign-in | Sign in to see more |
| --- | --- | --- |
| Launch gate | None: signed-in users see nothing new | None: same route and fields |
| Who sees a showcase | DUOS account holders | Anyone sees the program, copy, tools and workspaces; datasets need an account |
| Partner links | Land on the sign-in page, then the showcase | Land on the showcase, with a prompt on each dataset shelf |
| Sign-in prompt | One, from the existing `Authenticated` redirect, which keeps the URL | A new state on each dataset shelf and in the hero, returning to the same showcase |
| Hero counts for signed-out visitors | Not applicable | Hidden, or replaced by copy in the config |
| duos-ui work | Move two routes; no new UI | Task 2 plus the prompt state, its copy and Playwright cases for both states |
| Handoff labels ("Sign-in required") | Always signed in, so the labels can go | Kept |

Choosing "sign in to see more" changes Task 2 only.

### Keep the client's own error handling

`ShowcaseCatalog.ts` keeps plain `fetch` with its own 400 and 429 handling and `Retry-After`. Moving
it to `fetchAdapter` would report each 429 as an error. `RateLimitFilter` is one budget per user
across all of `api/`, so a busy user can see 429s on a page with several shelves.

## What this opens up

- **Launch on the current code**, without the DT-4466 approval.
- **Per-user card state**: "Approved" from `GET /api/user/me/researcher/datasets`, "Request in
  progress" from `GET /api/collections/role/researcher/summary`.
- **E7 subscriptions tied to the account**, without anonymous enrollment.
- **Identified analytics** for S5.2: every showcase event has a user.

Each is its own story when it is picked up; none is in this plan's tasks.

## Task summary

| Task | Repo | Depends on |
| --- | --- | --- |
| 1. Authenticated lookup route | consent | — |
| 2. Showcase routes behind sign-in and the authenticated client | duos-ui | 1 |
| 3. Previews and the release check | duos-ui | 1, 2 |
| 4. Health check credential | consent, duos-ui | 1 |

## Tasks

### Task 1: Authenticated lookup route (consent)

- Add `POST /api/showcase/datasets`: `@PermitAll`, `@Auth`, the same body parsing, request caps,
  service call, `unavailable` result and `no-store` response as `PublicShowcaseCatalogResource`.
  Share the code rather than copy it.
- OpenAPI path under `/api`, with the 401 and the 429.
- Tests: contract, eligibility, the exact S2.1 keys, 401 without a user, and the per-user rate limit.
- Before launch, check that `rateLimit.enabled` is on in each environment (terra-helmfile).

### Task 2: Showcase routes behind sign-in and the authenticated client (duos-ui)

- Move `/data` and `/showcase/:key` inside `Authenticated` in `AppRoutes.tsx`.
- `ShowcaseCatalog.ts` calls `/api/showcase/datasets` with the user's credentials: the BFF API proxy
  with a CSRF token in BFF mode, `apiUrl` and the bearer token in legacy mode. It keeps plain
  `fetch`, so it adds those itself (`getCsrfToken`, `Config.authOpts`) and keeps its own error
  handling.
- `showcaseHandoffsEnabled`: the BFF (`server/src/config.ts`) and the legacy client
  (`src/libs/config.ts`) turn handoffs on only when the lookup origin equals `apiUrl`'s. The lookup
  is now on `apiUrl`, so handoffs are always on: remove the derivation and the production-data
  preview notice (D19).
- Remove the "Sign-in required" handoff labels, which no longer apply.
- Playwright: a signed-out visit redirects to sign-in and back; a signed-in visit shows cards. BFF
  and legacy.

### Task 3: Previews and the release check (duos-ui)

S4.2 previews a hidden showcase on staging with production cards through the anonymous lookup. That
no longer works: a staging page has no production session, configs list production identifiers,
and the delivery plan forbids resolving those on staging, where the same alias names a different
dataset. Production does not render `hidden` showcases.

- Staging previews show the layout and the non-dataset sections; dataset shelves show their
  unavailable state.
- Add a script that resolves a showcase config's identifiers and computed shelves through the
  production `/api/showcase/datasets` with the reviewer's token, and reports any `unavailable`
  identifier. The S4.1 guide adds it to the showcase PR checklist, and S5.3's release check uses it.
- Update `docs/data-showcase.md`, `docs/data-showcase-lookup.md` and the S4.2 acceptance check.

### Task 4: Health check credential (consent, duos-ui)

S4.3's scheduled check called the anonymous lookup. Give it a DUOS service-account user, as the
email notifier has (`SERVICE_ACCOUNT` role); `@PermitAll` on the new route already admits it. Store
the credential as a GitHub Actions secret. DT-4359 is then no longer blocked by DT-4466.
