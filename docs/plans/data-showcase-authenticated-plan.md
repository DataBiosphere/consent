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

- **It shrinks the gate that blocks launch.** Eight of the nine S2.1 fields are already readable by
  a signed-in researcher for a public study. The ninth, `availableDate`, is new for controlled
  datasets: it is the DAC approval date, which no endpoint returns today. Behind sign-in, the S2.1
  approval that holds [DT-4466](https://broadworkbench.atlassian.net/browse/DT-4466) narrows to that
  one field and to how the fields are presented.
- **It makes the anonymous-only machinery removable**, if the public lookup is later dropped: the
  public rate limit, the BFF public proxy route, the `/showcase/*` CORS and CSP allowances and the
  separate lookup URL settings.
- **The cost is reach.** A visitor without a DUOS account cannot see live dataset cards.

Per-user features (access state on cards, account subscriptions, identified analytics) are possible
under every option: a public page can still read the signed-in user's own data. They are listed in
section 5 because they get easier, not because they need sign-in.

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

`OAuthCustomAuthFilter` requires a signed-in user for every `api/` path. Self-registration gives a
new user the Researcher role. These existing endpoints return dataset, study and DAC data to such a
user:

| Endpoint | Visibility rule | What it returns |
| --- | --- | --- |
| `GET /api/dataset/v2/{id}` | `DatasetService.verifyPublicVisibilityAccess`: public study, or the user is an admin, the creator or a custodian. A dataset with no study is readable by any signed-in user. | The full `Dataset` with its `Study`: description, PI name and email, data types, every dataset and study property (including `species` and `numberOfParticipants`), `dacId` and `dacApproval` |
| `GET /api/dataset/study/{id}` and `/{id}/datasets` | `DatasetService.canReadStudy` (same rule) | The full `Study` and its datasets |
| `GET /api/dataset/batch` | Same rule, per dataset | Datasets without their study |
| `GET /api/dataset/registration/{identifier}` | Same rule | The registration form (`DatasetRegistrationSchemaV1`), not the `Dataset` |
| `GET /api/dac` | Roles: admin, member, chair, researcher | Every DAC, with its name |

Against the S2.1 projection (`ShowcasePublicField`):

| Field | Already readable by a signed-in researcher? |
| --- | --- |
| `identifier`, `title`, `accessManagement`, `consentCodes`, `participantCount`, `accessUrl`, `species` | Yes, from `GET /api/dataset/v2/{id}` |
| `dacName` | Yes, by joining the dataset's `dacId` to `GET /api/dac` |
| `availableDate` | Open and external datasets: yes (`createDate`). **Controlled datasets: no.** It is `dataset.dac_approval_date`, which only the showcase queries and the NewStudyDigest email read. The newest ranking orders by it, so the order also reveals it. |

The showcase eligibility rule is narrower than `verifyPublicVisibilityAccess`. It needs a public
study, so datasets without a study are excluded. It also needs open or external access, or DAC
approval with a live DAC, and it grants no admin, creator or custodian exception.

A signed-in user is not a trusted reader. Moving the lookup behind sign-in limits who can call it;
it does not make new fields safe to return.

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
| Launch gate | DT-4466: all S2.1 fields, `species`, `scope` | The controlled-dataset `availableDate`, and the stewards' presentation questions | Same as B |
| Reach | Anyone with the link | Anyone sees the program; datasets need an account | Account needed for everything |

Signing in to DUOS for the first time creates an account and asks the user to accept the terms of
service, so in B and C "signed in" means "has a DUOS account", not only "has a Google or Microsoft
identity".

## 5. Opportunities and costs

### Opportunities that need sign-in (B and C)

- **A smaller launch gate.** AppSec and the data stewards decide on one new field instead of an
  anonymous exposure: show the DAC approval date of controlled datasets to signed-in users, or use a
  different date for them. The stewards' presentation questions, such as how to show the free-text
  `species`, still apply. The `scope` vocabulary is a DAC name, which researchers can already read.
- **Removable machinery.** If option A is later dropped, the anonymous-only pieces in section 7 can
  be removed, and the existing per-user `RateLimitFilter` covers the route. While A stays possible,
  B keeps them, switched off.

### Opportunities under any option, easier when signed in

- **Personal card state.** A card can say "Approved" from the researcher's approved datasets
  (`GET /api/user/me/researcher/datasets`), or "Request in progress" from their own requests
  (`GET /api/collections/role/researcher/summary`). `GET /api/dar/v2` is not a source: it returns an
  empty list to a researcher. This also gives E8's
  instant-approval shelf a per-user answer. Under option A the page would fetch this separately for
  signed-in visitors; under B the shelf already knows the user.
- **Subscriptions tied to the account (E7).** A signed-in subscriber removes anonymous enrollment
  from S7.1's first release. S7.1 still needs its decisions on verification and on unsubscribing,
  including unsubscribing from an email link while signed out.
- **Identified analytics (S5.2).** S5.2 plans to carry attribution through sign-in. Signed-in shelves mean every dataset-card event has a user.

### Not an opportunity

- **Catalog asset records on resource cards.** `GET /api/asset-catalog/{identifier}` returns only
  the identifier, type, status, `listed` flag and ids, not a name or URL. Sign-in removes one of the
  four reasons the contracts record gives for not reusing `DUOS-W-` and `DUOS-M-` records in S3.4
  ("no public source"). The others remain: no record for apps, editorial picks that are not
  registered assets, and no thumbnail or docs URL.

### Costs and risks

- **Reach.** The source epic describes a public page for a program. In B, a visitor without an
  account sees the program but not its datasets; in C, they see nothing. Partners may judge this
  differently.
- **Production-data previews and release qualification.** S4.2 previews a hidden showcase on
  staging against production data through the anonymous lookup, and S5.3 qualifies launch that way.
  An authenticated lookup would need the reviewer to be signed in to production from a staging
  page, which neither the BFF session nor the legacy token supports. Staging cannot stand in: configs
  list production identifiers, aliases come from a per-environment sequence, and the delivery plan
  forbids passing production aliases to a staging backend, because they would resolve to unrelated
  staging datasets. So a staging preview would show the layout with no dataset cards, and S4.2's
  acceptance check, "a reviewer can see a pending showcase with real live cards before merge", would
  change. Production does not render `hidden` showcases (`isShowcaseHiddenPreviewEnabled` is off for
  `prod`), so nothing replaces that check today. The candidates are in section 9.
- **The health check.** S4.3's scheduled check calls the anonymous production lookup without a
  session. Under B it needs its own credential (a service account) or stays blocked.
- **Sign-in friction.** Each dataset shelf needs a clear sign-in prompt that returns the visitor to
  the same showcase.
- **Popularity is still private.** Signed-in users are not trusted readers of request counts, so the
  E6 rules (a minimum request count, no totals or trends) stay.
- **One projection, two routes.** The new route reuses `ShowcasePublicField`. Adding a field to it
  for signed-in users would also add it to the anonymous route if DT-4466 turns that on. A wider
  signed-in card needs its own projection, or approval for both routes.

## 6. Recommendation

Choose **option B**:

- It unblocks launch on a decision about one field instead of the whole anonymous exposure.
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
| `POST /api/showcase/datasets` | **New.** `@PermitAll` with `@Auth`, the same request contract and `ShowcaseCatalogService`, the same projection, eligibility rule and `unavailable` result. OpenAPI path under `/api`. |
| Request caps (body size, identifier count, ranking limit) | Kept on the new route: a signed-in caller can still send a large request. |
| `RateLimitFilter` | Applies to the new route where `rateLimit.enabled` is on. It is one budget per user across all of `api/`. Check each environment's value in terra-helmfile before launch. |
| `PublicShowcaseCatalogResource`, `ShowcaseConfiguration` | Kept, with the resource unregistered (`showcase.publicLookupEnabled` off). |
| `PublicRateLimitFilter`, the `rateLimit.showcase` block, `SignedClientIpVerifier`, `ClientIpResolver` | Kept. The filter is registered whatever the switch says and still limits `showcase/*` by IP. Remove only if option A is dropped, and keep the signed client IP if another anonymous route uses it by then ([DT-4496](https://broadworkbench.atlassian.net/browse/DT-4496) signs support requests in duos-ui, but no Consent route other than `showcase/*` reads the signed client IP yet). |

### terra-helmfile

| Item | B or C |
| --- | --- |
| `proxy.showcaseAllowedOrigins` (Consent's `/showcase/*` CORS) and the chart switch `showcasePublicLookupEnabled` | Unchanged while A stays possible. |
| `DUOS_SHOWCASE_API_URL`, `showcaseApiUrl` | Unused by the shelves; removed only if A is dropped. |

### duos-ui

| Item | B or C |
| --- | --- |
| `ShowcaseCatalog.ts` | Calls the new route with the user's credentials: the BFF API proxy in BFF mode, `apiUrl` in legacy mode. It keeps its own handling of 400 and 429 rather than `fetchAdapter`'s, which signs the user out on a 401 and reports every error status. |
| Dataset shelves and hero counts | B: signed-out visitors see a sign-in prompt instead of the lookup. `ShowcasePage` already reads `useUserIsLogged()`. |
| Showcase routes | B: unchanged. C: move `/data` and `/showcase/:key` inside `Authenticated`, which keeps the URL through sign-in. |
| Sign-in return | The prompt signs in with the showcase URL as the target, as `SignInButton` does with `redirectTo`. |
| `showcaseHandoffsEnabled` | Today the BFF (`server/src/config.ts`) and the legacy client (`src/libs/config.ts`) turn handoffs on only when the lookup URL's origin equals `apiUrl`'s. With the lookup on `apiUrl`, both derivations change so handoffs follow the authenticated route. The production-data preview case (D19) goes away. |
| BFF `POST /public/showcase/datasets`, the legacy CSP entry | Unused. Remove only if A is dropped. |
| S4.2 preview procedure, `docs/data-showcase-lookup.md` | Staging previews show no dataset cards; the replacement check follows the section 9 decision. |

## 8. Proposed stories (option B)

1. **Authenticated lookup (consent).** Add `POST /api/showcase/datasets` with the existing service,
   and tests for the contract, the eligibility rule, the field list and the rate limit. Record the
   `availableDate` decision and the stewards' presentation decisions in the contracts record's "Open
   gates".
2. **Signed-in shelves (duos-ui).** Switch the client to the new route with its own error handling,
   show the sign-in prompt on dataset shelves and hero counts for signed-out visitors, return them
   to the showcase after sign-in, and change the handoffs derivation. Playwright: signed out and
   signed in, BFF and legacy.
3. **Previews and release qualification (duos-ui).** Change the S4.2 procedure, its acceptance
   check and the lookup docs: staging previews show the layout without dataset cards. Add the
   replacement check chosen in section 9, use it for S5.3's release qualification too, and remove
   the production-data preview notice.
4. **Health check (S4.3).** Give the scheduled check a service-account credential for the new route,
   or keep it blocked with DT-4466.
5. **Launch (E5).** Unchanged apart from story 3, and the flagship's hero counts follow decision 3.

Follow-on stories, each planned when it starts: personal card state, account subscriptions (E7) and
removal of the anonymous route if option A is dropped.

## 9. Open questions

| Question | Owner |
| --- | --- |
| Option A, B or C, and the hero counts for signed-out visitors | Product owner |
| May signed-in users see the DAC approval date of controlled datasets, or should those cards use another date? | AppSec, data stewards |
| How should the free-text `species` be shown (as is, truncated, or not at all)? | Data stewards |
| Is `rateLimit.enabled` on in staging and production? | Consent lead |
| Does the health check get a service-account credential? | Consent lead, duos-ui lead |
| How is a hidden showcase's real cards checked before launch: render `hidden` entries on production for admins only; a script that resolves the config's identifiers through the new route with a reviewer's credential (identifiers, not layout); or check the live page right after release, before announcing it? | Product owner, duos-ui lead |
| Do partners accept that their datasets need a DUOS account to view? | Product owner, partners |
