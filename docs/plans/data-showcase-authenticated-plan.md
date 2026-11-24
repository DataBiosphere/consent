# Data Showcase: putting the catalog lookup behind sign-in

Ticket: [DT-4499](https://broadworkbench.atlassian.net/browse/DT-4499)

**Status: draft (2026-11-24). No decision is made.** Section 6 gives a recommendation. The choice
between the options in section 4 belongs to the product owner.

Related documents:

- [`data-showcase-delivery-plan.md`](data-showcase-delivery-plan.md): the MVP plan. Its section 3,
  "Public contract and security boundary", is the design this document reconsiders.
- [`data-showcase-contracts.md`](data-showcase-contracts.md): the S2.1 field list and the open gates.
- [`data-showcase-post-mvp-plan.md`](data-showcase-post-mvp-plan.md): epics E6 to E10.
- [`elasticsearch-service-duos-ui-usage.md`](elasticsearch-service-duos-ui-usage.md): why the Data
  Library search is not a safe base for this work (section 3).

## 1. Question and short answer

The ticket asks what becomes possible if the showcase reads its data through the authenticated
`/api` endpoints instead of the anonymous `POST /showcase/datasets`.

Short answer:

- **It removes the gate that blocks launch.** Every field the anonymous lookup returns is already
  returned to any signed-in user for a public study. An authenticated lookup with the same fields
  shows a signed-in user nothing new, so it does not need the S2.1 field approval that holds
  [DT-4466](https://broadworkbench.atlassian.net/browse/DT-4466).
- **It retires most of the anonymous-only machinery**: the public rate limit, the BFF public proxy
  route, the `/showcase/*` CORS and CSP allowances and the separate lookup URL settings.
- **It enables per-user features** that an anonymous page cannot have: access and request state on
  dataset cards, subscriptions tied to an account, and catalog asset records on resource cards.
- **The cost is reach.** A visitor without a DUOS account cannot see live dataset cards.

The recommendation (section 6) keeps the showcase pages public and shows the dataset shelves and
live counts only to signed-in visitors.

## 2. Where the showcase stands (2026-11-24)

The MVP is built but not live:

- **Done:** S1.1 to S1.3, S2.1, S2.2 (with the rate limit, the BFF proxy and the signed client IP),
  S3.1 to S3.4, S4.1 and S4.2. Six section types render: `masthead`, `hero`,
  `latest-data-releases`, `open-access-datasets`, `featured-tools` and `featured-workspaces`.
- **Off:** Consent's lookup is deployed but switched off (`showcase.publicLookupEnabled`, default
  `false`). DT-4466, which turns it on, has been blocked since 2026-11-13 on the S2.1 gate in the
  contracts record: the field list, the free-text `species` decision and the `scope` vocabulary need
  approval from the catalog lead, the data stewards and AppSec. Until then every dataset shelf shows
  "These datasets can't be loaded right now".
- **Blocked behind it:** the S4.3 health check
  ([DT-4359](https://broadworkbench.atlassian.net/browse/DT-4359)) and live-card previews.
- **Not started:** E5 (flagship and partner configs, metrics, launch). duos-ui's
  `src/showcases/registry.json` has no entries; only end-to-end fixtures exist.

## 3. What a signed-in user can already see

`OAuthCustomAuthFilter` requires a signed-in user for every `api/` path. These existing endpoints
return dataset and study data to any signed-in user, with no role:

| Endpoint | Visibility rule | What it returns |
| --- | --- | --- |
| `GET /api/dataset/v2/{id}`, `GET /api/dataset/registration/{identifier}`, `GET /api/dataset/batch` | `DatasetService.verifyPublicVisibilityAccess`: public study, or the user is an admin, the creator or a custodian | The full `Dataset`, with its `Study`: description, PI name and email, data types, every dataset and study property (including `species` and `numberOfParticipants`), the DAC id and `dacApproval` |
| `GET /api/dataset/study/{id}` and `/{id}/datasets` | `DatasetService.canReadStudy` (same rule) | The full `Study` and its datasets |
| `GET /api/asset-catalog/{identifier}` | `AssetLookupService`: a readable study or dataset | The asset's catalog fields |
| `GET /api/dar/v2` | The caller's own requests | Each request with its datasets and state |

The nine fields of the S2.1 projection (`ShowcasePublicField`) are a subset of what the first row
returns for a public study. The showcase eligibility rule is narrower than `verifyPublicVisibilityAccess`:
it also needs open or external access, or DAC approval with a live DAC, and it grants no admin,
creator or custodian exception. So an authenticated lookup that keeps both the projection and the
eligibility rule cannot show a signed-in user anything they cannot already read.

**Not a safe base: the Data Library search.** `POST /api/dataset/search/index` and `/v2` pass the
caller's Elasticsearch query to the index unchanged. The `study.publicVisibility` filter is added by
duos-ui, not by Consent (`elasticsearch-service-duos-ui-usage.md`, "Client-side filtering for search
results"). The access-control epics in that plan fix this. Until they land, nothing in this plan
reads from the search endpoints, and the post-MVP rule "never the authenticated Elasticsearch search"
(E6) still holds.

## 4. Options

| | A. Public (current plan) | B. Public pages, signed-in data | C. Signed-in only |
| --- | --- | --- | --- |
| Showcase routes | Public | Public | Inside `Authenticated` |
| Hero, copy, tools, workspaces | Everyone | Everyone | Signed-in users |
| Dataset shelves and hero counts | Everyone | Signed-in users; others see a sign-in prompt | Signed-in users |
| Consent lookup | `POST /showcase/datasets`, anonymous | `POST /api/showcase/datasets` | `POST /api/showcase/datasets` |
| Launch gate | DT-4466 field approval | AppSec confirms section 3 | AppSec confirms section 3 |
| Reach | Anyone with the link | Anyone sees the program; datasets need an account | Account needed for everything |

Signing in to DUOS for the first time creates an account and asks the user to accept the terms of
service, so in B and C "signed in" means "has a DUOS account", not only "has a Google or Microsoft
identity".

## 5. Opportunities and costs

### Opportunities (B and C)

- **Launch without the field approval.** AppSec confirms that the authenticated lookup returns a
  subset of section 3. That is a smaller question than approving an anonymous exposure, and the
  `species` decision can wait for option A.
- **Wider cards without new approval.** Fields that are already visible to signed-in users, such as
  the study description or data types, could be added to the projection. Each one is still a product
  choice, and the projection stays an allowlist.
- **Less to run.** The anonymous-only pieces in section 7 can be retired, and the existing per-user
  `RateLimitFilter` applies to the new route.
- **Personal card state.** A card can say "You have access", "Request in progress" or "Request
  access", from the user's own requests (`GET /api/dar/v2`). This also gives E8's instant-approval
  shelf a per-user answer instead of a generic one.
- **Subscriptions tied to the account (E7).** S7.1 no longer has to design anonymous enrollment,
  double opt-in or public token routes for the first release: a signed-in user subscribes, and the
  existing account email is the channel.
- **Catalog asset records on resource cards.** The contracts record rejected reusing
  `DUOS-W-` and `DUOS-M-` records in S3.4 because "every route that returns asset fields needs a
  signed-in user". With a signed-in page, `GET /api/asset-catalog/{identifier}` can supply registered
  workspaces and AI models (the `ai-models` section, R2).
- **Identified analytics (S5.2).** Showcase events can follow a user from a card to a request, not
  only an anonymous id.

### Costs and risks

- **Reach.** The source epic describes a public page for a program. In B, a visitor without an
  account sees the program but not its datasets; in C, they see nothing. Partners may judge this
  differently.
- **Production-data previews.** S4.2 previews a hidden showcase on staging against production data
  through the anonymous lookup. An authenticated lookup would need the reviewer to be signed in to
  production from a staging page, which the BFF session and the legacy token do not support. Previews
  would use staging data instead, so they show the layout but not the production cards.
- **Sign-in friction.** Each dataset shelf needs a clear sign-in prompt that returns the visitor to
  the same showcase.
- **Popularity is still private.** Signed-in users are not trusted readers of request counts, so the
  E6 rules (a minimum request count, no totals or trends) stay.
- **Work already done.** Option B keeps the anonymous code but leaves it off; removing it is a
  separate choice (section 7).

## 6. Recommendation

Choose **option B**:

- It unblocks launch now, on an AppSec confirmation instead of a field approval.
- It keeps the public program page that partners can link to.
- It leaves option A open: the anonymous lookup stays in the code, switched off, and DT-4466 can
  still turn it on once the field approval lands.

Option C gives up the public page for no further gain in exposure. Choose it only if the product
owner wants the program pages themselves to be account-only.

Decisions for the product owner (Jonathan Lawson):

1. Option A, B or C.
2. For B: whether DT-4466 continues in parallel, or is closed until a partner asks for anonymous
   datasets.
3. For B: what a signed-out visitor sees in the hero in place of the live counts (no counts, or
   counts written in the config).

## 7. What changes

### Consent

| Item | B or C |
| --- | --- |
| `POST /api/showcase/datasets` | **New.** `@PermitAll` with `@Auth`, the same request contract and `ShowcaseCatalogService`, the same projection, eligibility and `unavailable` result. OpenAPI path under `/api`. |
| `RateLimitFilter` | Applies to the new route per user, where `rateLimit.enabled` is on. Check each environment's value in terra-helmfile before launch. |
| `PublicShowcaseCatalogResource`, `PublicRateLimitFilter`, `ShowcaseConfiguration`, the `rateLimit.showcase` block | Kept, switched off. Remove only if option A is dropped. |
| `SignedClientIpVerifier`, `ClientIpResolver` | Kept. The support form uses them too ([DT-4496](https://broadworkbench.atlassian.net/browse/DT-4496)). |
| `/showcase/*` CORS (`showcaseAllowedOrigins`) | Kept with the public route, or removed with it. |

The request caps (body size, identifier count, ranking limit) stay on the new route: a signed-in
caller can still send a large request.

### duos-ui

| Item | B or C |
| --- | --- |
| `ShowcaseCatalog.ts` | Calls the new route through `fetchAdapter`, like other `/api` calls: the BFF API proxy in BFF mode, `apiUrl` in legacy mode. |
| Dataset shelves and hero counts | B: signed-out visitors see a sign-in prompt instead of the lookup. `ShowcasePage` already reads `useUserIsLogged()`. |
| Showcase routes | B: unchanged. C: move `/data` and `/showcase/:key` inside `Authenticated`, which keeps the URL through sign-in. |
| Sign-in return | The prompt signs in with the showcase URL as the target, as `SignInButton` does with `redirectTo`. |
| `showcaseHandoffsEnabled` | Always on once the lookup and the handoffs use the same Consent. The production-data preview case (D19) goes away. |
| BFF `POST /public/showcase/datasets`, legacy `showcaseApiUrl`, `DUOS_SHOWCASE_API_URL`, the legacy CSP entry | Unused. Remove only if option A is dropped. |
| S4.2 preview procedure, `docs/data-showcase-lookup.md` | Previews use staging data. |

## 8. Proposed stories (option B)

1. **Authenticated lookup (consent).** Add `POST /api/showcase/datasets` with the existing service
   and tests for the contract, the eligibility rule, the field list and the per-user rate limit.
   Record the AppSec confirmation in the contracts record's "Open gates".
2. **Signed-in shelves (duos-ui).** Switch the client to the new route, show the sign-in prompt on
   dataset shelves and hero counts for signed-out visitors, and return them to the showcase after
   sign-in. Playwright: signed out and signed in, BFF and legacy.
3. **Previews (duos-ui).** Change the S4.2 procedure and the lookup docs to staging data, and remove
   the production-data preview notice.
4. **Launch (E5).** Unchanged, except that the flagship's hero counts follow decision 3.

Follow-on stories, each planned when it starts: personal card state, account subscriptions (E7),
asset records on resource cards, and removal of the anonymous route if option A is dropped.

## 9. Open questions

| Question | Owner |
| --- | --- |
| Option A, B or C, and the hero counts for signed-out visitors | Product owner |
| Does an authenticated lookup with the S2.1 fields need any approval beyond confirming section 3? | AppSec |
| Is `rateLimit.enabled` on in staging and production? | Consent lead |
| Do partners accept that their datasets need a DUOS account to view? | Product owner, partners |
