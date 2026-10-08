# Data Showcase artifact review

Reviewed 2026-10-07. Companion to the [delivery plan](data-showcase-delivery-plan.md).
This record distinguishes observed mockup behavior from proposed production requirements and scope
decisions. It does not certify the mockup's sample data or external service claims.
The request-metrics disposition was aligned with the delivery plan's disclosure policy on 2026-10-08;
the recorded artifact observations are unchanged.

## Source and method

- Canonical reference: [Claude artifact](https://claude.ai/code/artifact/0ce96e5e-57b4-4943-9682-7c9538c98176).
  The sharing query parameter is omitted from repository documentation.
- The user supplied the HTML export after authenticated CLI and shared-link retrieval failed.
  Reviewed local file: `/tmp/duos-showcase-artifact.UPjpPL/artifact.html`, 543,573 bytes, 3,510
  newline characters. This is a temporary source location, not a durable repository asset.
- SHA-256 of the exact pasted file, including its initial paste-instruction comment:
  `1356b52733e99b48ac51b963db4ed3b49531a8fef803bda7369395acdf293ce0`.
- HTML title: `DUOS — Data Use Oversight System`. No authoritative artifact version or modification
  timestamp was supplied; dates within sample content are not version metadata.
- Inspected HTML/CSS/JavaScript, extracted a DOM inventory with scripts disabled, then rendered the
  local file in Chromium with external HTTP(S) requests blocked. Checked 1440×1000 light/dark and
  390×844 light viewports with reduced motion enabled. Inspected screenshots of the page top,
  analysis apps and impact panel. None of these renders produced a JavaScript page error.
- This is a source/design review with limited browser checks, not a full WCAG audit, provider
  integration test, or live rendering of the remotely hosted artifact. Source line numbers below
  refer to the pasted file identified by the hash.

## What the mockup actually implements

The export is a standalone HTML page with embedded fonts/images, inline SVG illustrations and one
inline script. Its script implements announcement rotation, horizontal shelf scrolling and a
decorative hero canvas. There are no application API calls, admin pages, persistence, matching
engine, storage integration, request-status updates, subscription delivery or live impact embed.

The DOM contains 106 links; 82 are `href="#"`, and the remaining 24 are internal fragment links.
There are no external link destinations. All three email forms use `onsubmit="return false;"`
and have no action. The search input/button has no submission handler. Open-data download and
archive actions are styled spans, not functioning links. Several resource cards similarly lack an
action. These controls communicate intended user journeys; they are not completed integrations.

The footer says the studies, figures and citations are illustrative rather than live. The impact
panel separately labels itself a static preview and its publication/trial names illustrative.
Neither actual-looking names nor embedded logos make claims about approval timing, costs, security
certification, free credits, training, or program deadlines suitable for production without review.

## Structure and visual template

| Observation | Evidence | Implementation implication |
| --- | --- | --- |
| Masthead, hero, then 21 content sections in the ticket's order | Lines 1577–3389; `.subsection-nav` has 21 links | All 23 registry types are represented visually, but only the 21 content sections have matching anchor IDs. `masthead` and `hero` are classes without those IDs. |
| Sticky two-row masthead, DUOS/AnVIL co-branding, search and sign-in | `.masthead`, `.masthead-inner`, `.subsection-nav` | Keep global identity and partner branding explicit; generate navigation from visible navigable content sections, not every enabled registry key. |
| Wide, restrained shelf layout | `.wrap` max-width 1400px; 356px `.card`; horizontal overflow/scroll snapping | Use one accessible shelf primitive; preserve intended horizontal scrolling without page-level clipping. Do not implement every section as a horizontal shelf. |
| Three hero columns on desktop | Intro/stats/CTAs, onboarding panel, announcement panel; `.hero-inner` collapses at 980px | Preserve the hierarchy with a responsive grid. Stats stack below 480px; search moves below branding below 560px. |
| Distinct app, research and event grids | App grid 3→1 columns at 900px; research area 2→1 at 760px; initiatives 3→2→1 at 900/600px; Where Else 4→2→1 at 900/520px | Shared primitives can have fixed per-type variants; a universal card-strip renderer would lose important visual structure. |
| Two-column conference, workshop and program panels | Collapse at 760px; facts/history remain separate from description | Typed settings/forms must support structured information where visual fidelity matters. |
| Serif headings, sans-serif body, monospaced labels/figures | Fraunces, IBM Plex Sans, IBM Plex Mono; blue/gray palette and subdued borders | Use template-owned typography and accessible theme tokens. Custom per-partner fonts are not necessary to reproduce the design. |
| System light/dark modes | `prefers-color-scheme` plus `data-theme` overrides | Theme/contrast QA needs both supported modes; no visible mode switch or theme persistence is implemented. |
| Decorative animated hero | `heroCanvas`, marked `aria-hidden`; requestAnimationFrame loop with reduced-motion still frame | A static CSS/SVG decoration is a simpler proposed MVP equivalent. Animation is not needed for discovery or publishing. |
| Footer outside the section registry | Documentation, DACs, policies, support, GitHub and attribution | Reuse the app footer or provide approved fixed links. Do not create a 24th editable section implicitly. |

The section-key sequence matches the delivery plan's full registry table. Source comments number
the content shelves 1–21, excluding masthead/hero; they are not the ticket's story numbers or its
23-type registry positions.

## Requirements and schema differences

| Area | Observed mockup detail | Proposed disposition and story |
| --- | --- | --- |
| Labels | Short navigation labels and uppercase eyebrow text differ from section titles | Provide registry default `navLabel`, optional override and optional eyebrow copy; do not derive all labels from the full title. S1.1/S1.2/S3.1. |
| Hero statistics | Shows a study count, data size and accessible-tool count; the same illustrative 1,842 appears as a dataset total in the donut | Define each metric's entity, units and scope. Dataset-union aggregation alone cannot compute a tool total, and study count is not dataset count. S2.1/S3.3/S6.3. |
| Dataset fields | Latest releases show PI, data types and samples; one sample card displays both GRU and HMB | Use authoritative, public-safe projections. Do not seed conflicting primary-use terms or manually copied metadata. Participants and samples remain distinct. S2.1/S3.3. |
| Most requested | Quarter-over-quarter sparkline/change, median review time and lifetime request count in addition to rank/DAC | The ticket's rolling-window ranking is not the same metric as lifetime totals. S6.1/S6.2 omit public request totals and volume trends; adding them requires a separate privacy-policy revision, not merely a known source. Optional median timing needs approved suppression, window and freshness, with sample counts kept internal. |
| Largest cohorts | Explicitly ranks participants and shows sub-cohort count and phenotype focus | The ticket calls for sample-count prefills. Resolve the semantic conflict in favor of accurately labeled available measures; do not fabricate sample counts or sub-cohort aggregation. S2.1/S6.2. |
| Open data | Format and size plus a download label | Use an actual approved download/provider link; label it accurately if it opens a provider page rather than downloading bytes. S2.1/S3.3. |
| Instant access | Tooltip describes research-use-statement/DUO matching; cards claim automated matching and medians below 5 or 10 minutes | This does not establish free-text/LLM matching, supported DUO semantics or measured timing. Validate existing RADAR coverage and preserve all eligibility gates. S8.1–S8.4. |
| Archive | Absolute dates and fixed 9/14/21/30-day badges; generic 24–48-hour retrieval claim; per-GB banner but total-dollar card values | Countdown is static, not a clock/state machine. Require provider timestamps, unit/currency/basis, total-estimate rules and unknown/stale states. No universal retrieval promise or implied access approval. S9.1–S9.3. |
| Apps and resources | Full-width featured Terra card, other app grid, workflow versions/runtime/workspace usage, notebook environments, model parameter counts | Keep shared typed cards with optional type-specific fields; treat usage as sourced metrics, not invented editorial numbers. Avoid building an execution environment. S3.4/S6.4. |
| Workspaces | Difficulty, estimated time, clone count and “Uses” dataset/tool relationships | Add optional bounded descriptive fields and typed relationships. Clone count requires a trusted source or omission; do not imply a statistics integration already exists. S3.4. |
| Publications | Journal, authors, month/year, abstract, cited dataset tags and optional related-workspace CTA | Citation/year alone cannot express full visual parity. Add structured optional fields and safe related links; hide links to absent/disabled destinations. S10.1. |
| Research initiatives | Thirteen initiative cards in four named groups, with acronym/full name and upcoming badges | A flat label/URL list loses the hierarchy. Add within-section grouping and optional status labels; section order remains fixed. S6.5. |
| Conference | Upcoming event plus past-conference timeline | Add optional history items and explicit timezone/date precision; do not invent exact dates for season-only information. S7.3. |
| Where Else | Nine upcoming event/conference tiles with dates and locations | This differs from a generic external-resource-links panel. Support typed event metadata and real links, or obtain approval for the simpler links-only variant. S7.3. |
| Workshops | Select multiple upcoming sessions, enter email, view recent sessions and request private training | This is registration, not merely a release/registration-opening alert subscription. Prefer provider registration links initially; native multi-session signup is conditional S7.4 with its own contract. |
| Scholars/interns | Program lead, eligible audience, application deadline and focus-area facts beside copy/CTA | Add bounded structured facts to the panel schema; confirm content and actual destinations. S7.3. |
| Impact | Inline SVG publication-to-outcome graph, attribution, static stat banner and an external-intent CTA; zero iframes | An approved static summary/image plus real provider link is closer to this export and simpler than a mandatory live embed. A live embed remains an explicit richer option. S10.3. |
| Showcase management | No admin/editor/draft/publish implementation in the export | Those requirements came from the Jira text. On 2026-10-07 product removed the no-deploy requirement; showcases are code-defined configs authored by PR. E1/E4. |

All extensions above are proposed typed fields/behaviors requiring product refinement; displaying
sample text is not an acceptance criterion to implement an unbounded metadata system. Start with
the required common fields, hide unknown optional fields and keep measured values out of manual
configs unless explicitly identified as editorial estimates with provenance.

## Accessibility and responsive findings

Observed useful features include focus outlines, email/search accessible labels, button-based
consent chips, labeled arrow controls, a donut SVG accessible label and partial reduced-motion
handling. These do not establish WCAG AA compliance. Specific implementation work is needed:

1. The page has one `h1` and no `h2`–`h6`; visual section/card headings are divs. Add a meaningful
   heading hierarchy, landmarks and a skip link without changing the visual appearance.
2. Announcement rotation advances every five seconds. It pauses on mouse hover and suppresses
   autoplay for reduced motion, but has no explicit pause control or focus pause. Inactive slides
   use opacity/pointer-events, leaving three invisible slide links at `tabIndex=0` with no inert
   or aria-hidden ancestor in the browser check. Production must remove inactive controls from
   keyboard navigation and the accessibility tree, and provide pause/focus behavior.
3. CSS pseudo-element tooltips expose `data-tooltip` visually on hover/focus but have no explicit
   described-by association or dismissal interaction. Use an accessible tooltip primitive and
   avoid clipping tooltips within horizontally scrolling shelves.
4. Shelf arrows always request smooth scrolling, including when reduced motion is enabled. They
   have generic repeated names and no observed boundary-disabled state. Give each shelf named
   controls, appropriate disabled states and motion-aware behavior; keep touch/keyboard access.
5. At a 390px viewport, measured body scroll width is 428px despite `overflow-x:hidden`. The hero
   stacks, but the page still has overflowing content. Fix sizing/wrapping instead of hiding the
   excess; maintain intended scrolling inside shelves and the navigation strip. Include 320px,
   390px, tablet, desktop, long copy and zoom in implementation QA.
6. The sticky masthead has no `scroll-margin`/`scroll-padding` compensation in the source. Add an
   anchor offset so destination headings are not obscured after navigation.
7. Charts include visible counts/legend and an SVG label, but their filter links are placeholders.
   Provide complete text/table equivalents and real keyboard-operable filter links. The illustrated
   three-way access breakdown also needs a policy for external/unknown catalog access types.
8. Verify all supported light/dark foreground/background combinations, including faint metadata and
   text on accent buttons. Do not assume the mockup palette passes contrast because a theme exists.

These findings extend S3.1–S3.3, S6.3 and S5.3 acceptance checks. They are corrections needed to
deliver the requested quality bar, not reasons to discard the visual template.

## Recommended effect on delivery strategy

Keep the six-section MVP and the separation of publishing, discovery, automation and archival
epics. The artifact offers no backend evidence that would justify coupling these releases.

- Prefer a template composed of shelf, grid, featured-card and structured-panel primitives with
  typed config schemas. Preserve the different fixed layouts; avoid both 23 bespoke implementations and
  a general page builder.
- Prefer existing RADAR coverage plus accurate eligibility/status display before new matching
  rules. The tooltip is not evidence that a new engine or free-text semantic matcher is necessary.
- Prefer approved impact summary/stat content plus a provider link for the first impact release.
  Make iframe integration conditional on demonstrated value and agreement. This changes the ticket's
  embed-specific criterion while preserving the export's static presentation intent.
- Prefer real provider registration links over a new workshop registration service initially.
  Native multi-session enrollment preserves more of the mockup interaction but introduces stable
  session IDs, provider enrollment outcomes, duplicate/partial-failure handling and cancellation.
  Record this as an explicit choice in S7.4, not a hidden expansion of notification subscriptions.
- Prefer a static hero decoration and template-owned fonts. Preserve branding and layout without
  introducing animation lifecycle or per-program font configuration into MVP.
- Maintain truthful content and source provenance. Do not ship faux links, assumed retrieval rates,
  illustrative consent combinations, free-service promises or invented scientific impact metrics.

The export resolves the artifact-content blocker. It does **not** supply the earlier Jira criteria
referenced as “unchanged,” source-of-truth decisions for catalog/metrics, program content approval,
or provider/DAC agreements. Those remain explicit gates in the delivery plan.
