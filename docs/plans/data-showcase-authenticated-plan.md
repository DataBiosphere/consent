# Data Showcase Behind Sign-in Plan

## Status

Proposed. Ticket: [DT-4499](https://broadworkbench.atlassian.net/browse/DT-4499).

Related plans: [`data-showcase-delivery-plan.md`](data-showcase-delivery-plan.md) (the MVP, built
around an anonymous lookup) and [`data-showcase-contracts.md`](data-showcase-contracts.md) (the S2.1
field list and its approval gate).

## Overview

Showcase pages move behind sign-in, and their catalog lookup moves to the authenticated `/api`. A
signed-in user then sees only catalog data they can already read, so the launch no longer waits on
the AppSec and data-steward approval of an anonymous exposure. A signed-in page also makes later
per-user features possible, such as access state on dataset cards; none is in this plan.

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
  ([DT-4359](https://broadworkbench.atlassian.net/browse/DT-4359), duos-ui #4164) needs the lookup on.

### The change

- **Consent:** `POST /api/showcase/datasets` with the same request contract, service, projection and
  eligibility rule, for the roles that can read DAC names. Its one difference is the date, below.
- **duos-ui:** the showcase routes go inside `Authenticated`, and the catalog client calls the new
  route as the signed-in user.

The anonymous route, its rate limit and its CORS and proxy settings stay in the code, switched off.

## Decisions

### Signed-in users see nothing new

The route allows the roles of `GET /api/dac`: admin, member, chair and researcher.
Self-registration gives a user the Researcher role. For a public study, these users can already
read these S2.1 values:

| Field | Already readable through |
| --- | --- |
| `identifier`, `title`, `accessManagement`, `consentCodes`, `participantCount`, `accessUrl`, `species` | `GET /api/dataset/v2/{id}` (the full `Dataset` and `Study`, visibility by `DatasetService.verifyPublicVisibilityAccess`) |
| `dacName` | The dataset's `dacId` and `GET /api/dac` |
| `availableDate` | Only as the dataset's `createDate` (below) |

The anonymous route dates a controlled dataset by `dac_approval_date`, which no endpoint returns,
and its newest ranking orders by that date. The authenticated route therefore uses `create_date`
for every dataset, for both the field and the ranking. The cost: a controlled dataset that waited
for DAC approval is dated by its creation, so it can sort lower in "Latest data releases" and miss
the 30-day "New in DUOS" badge. Eligibility still needs the approval, so no unapproved dataset
appears.

The showcase eligibility rule is narrower than the readers above: it needs a public study and grants
no admin, creator or custodian exception. With the date changed and the roles matched, the
authenticated route needs no new field approval. The S2.1 gate stays with DT-4466, for anonymous
callers only. A user with none of the four roles, such as a signing official who is not also a
researcher, sees the showcase page but gets 403 from the lookup, so no dataset cards or counts.

### Reuse the showcase lookup, not the Data Library search

The new route calls `ShowcaseCatalogService` and the same DAO queries, with a bound switch for the
date. It does not read from
`POST /api/dataset/search/index`, which passes the caller's Elasticsearch query through and leaves
the visibility filter to duos-ui (`elasticsearch-service-duos-ui-usage.md`).

### Call the route through `fetchAdapter`

The client moves from plain `fetch` with `credentials: 'omit'` to `fetchAdapter`, passing
`Config.authOpts()` as other `/api` callers do. `fetchAdapter` drops that header in BFF mode, where it
sends the session cookie and CSRF token instead, retries once on a CSRF rejection and signs the user
out on a 401. It gains an option that leaves 400 and 429 to the caller, without error reporting and
with `Retry-After`. A 429 is normal here: `RateLimitFilter` is
one budget per user across all of `api/`, and a page makes one lookup per shelf.

### Previews show layout only outside production

Showcase configs list production identifiers, and the same `DUOS-` alias names a different dataset
in another environment. On local, dev and staging, entries from `src/showcases/registry.json`
render without catalog lookups (no shelves, no hero counts) and with dataset handoffs turned off.
The end-to-end fixture entries, which use each environment's own synthetic datasets, keep both.
This replaces `showcaseHandoffsEnabled` and the production-data preview (D19).

### Fully behind sign-in, or "sign in to see more"

This plan's tasks assume the pages are fully behind sign-in, the intent of the ticket. The
alternative keeps the pages public and shows the dataset shelves and hero counts only after sign-in.

| | Fully behind sign-in | Sign in to see more |
| --- | --- | --- |
| Launch gate | None: signed-in users see nothing new | None: same route and fields |
| Who sees a showcase | DUOS account holders | Anyone sees the program, copy, tools and workspaces; datasets need an account |
| Partner links | Land on the sign-in page, then the showcase | Land on the showcase, with a prompt on each dataset shelf |
| Sign-in prompt | One, from the existing `Authenticated` redirect, which keeps the path and query but not a `#section` anchor | A new state on each dataset shelf and in the hero, returning to the same showcase |
| Hero counts for signed-out visitors | Not applicable | Hidden, or replaced by copy in the config |
| duos-ui work | Move two routes; no new UI | Task 2 plus the prompt state, its copy and Playwright cases for both states |
| Handoff labels ("Sign-in required") | Always signed in, so the labels can go | Kept |

Choosing "sign in to see more" changes Task 2 only.

## Task summary

| Task | Repo | Depends on |
| --- | --- | --- |
| 1. Authenticated lookup route | consent | — |
| 2. Showcase routes behind sign-in and the authenticated client | duos-ui | 1 |
| 3. Layout-only previews outside production | duos-ui, consent docs | 2, 4 |
| 4. Health check on the authenticated route | consent, duos-ui | 1, duos-ui #4164 |

## Tasks

### Task 1: Authenticated lookup route (consent)

- Add `POST /api/showcase/datasets`: `@RolesAllowed({ADMIN, MEMBER, CHAIRPERSON, RESEARCHER})`,
  `@Auth`, the same body parsing, request caps, service call, `unavailable` result and `no-store`
  response as `PublicShowcaseCatalogResource`. Share the code rather than copy it.
- Date every dataset by `create_date` on this route: a bound `:datedByCreation` switch in the
  existing `CASE` of `findShowcaseSummaries` and `findShowcaseNewestAliases`, like their
  `:openAccessOnly` switch, so each query stays one text block. The anonymous route keeps its S2.1
  date.
- OpenAPI path under `/api`, with the 401, the 403, the 429 and its own `availableDate` description.
- Tests: contract, eligibility, the exact S2.1 keys, the `create_date` dating and ranking of a
  controlled dataset, 401 without a user, 403 for a user without the roles, and the per-user rate
  limit.
- Before launch, check that `rateLimit.enabled` is on in each environment (terra-helmfile).

### Task 2: Showcase routes behind sign-in and the authenticated client (duos-ui)

- Move `/data` and `/showcase/:key` inside `Authenticated` in `AppRoutes.tsx`.
- `ShowcaseCatalog.ts` calls `/api/showcase/datasets` through `fetchAdapter` with
  `Config.authOpts()`, and the new option for 400 and 429.
- Remove the "Sign-in required" handoff labels, which no longer apply.
- Playwright: a signed-out visit redirects to sign-in and back; a signed-in visit shows cards. BFF
  and legacy.

### Task 3: Layout-only previews outside production (duos-ui)

- On local, dev and staging, render `registry.json` entries without catalog lookups and with dataset
  handoffs off; keep both for the end-to-end fixture entries.
- Remove `showcaseHandoffsEnabled` from the BFF (`server/src/config.ts`) and the client
  (`src/libs/config.ts`), and the production-data preview notice.
- No one sees a pending showcase with live cards before it goes live. A reviewer checks its
  identifiers with the Task 4 health check, run locally against production with their own
  credentials; the S4.1 guide adds that step to the showcase PR checklist.
- Update the S4.2 procedure and acceptance check, S5.1 and S5.3 in `data-showcase-delivery-plan.md`,
  D19 in `data-showcase-contracts.md`, and `docs/data-showcase.md` and `docs/data-showcase-lookup.md`
  in duos-ui.

### Task 4: Health check on the authenticated route (consent, duos-ui)

duos-ui #4164 runs the S4.3 check daily, on showcase PRs and by hand, against the anonymous lookup.

- Point it at `/api/showcase/datasets` with a dedicated DUOS user that has only the Researcher
  role. A shared service account would also open the endpoints its role allows.
- Give the credential only to the scheduled and manual runs on `develop`, through Workload Identity
  Federation or an environment-scoped secret. Pull-request runs execute the PR's own script, so they
  run without it and skip the identifier lookups.
- DT-4359 is then no longer blocked by DT-4466.
