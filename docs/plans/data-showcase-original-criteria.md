# Data Showcase: original source criteria (DT-3904)

Recorded for [DT-4272](https://broadworkbench.atlassian.net/browse/DT-4272) (S1.1). Where the
current DT-3904 description says "Unchanged" or "Original acceptance criteria stand", this text is
the authoritative source (product decision, 2026-10-19).

- **Source:** the DT-3904 description as saved on 2026-08-18 23:02 (America/New_York), taken from
  the Jira change history (the `fromString` of that description change). Not taken from the
  artifact HTML.
- **Conversion:** Jira wiki markup converted mechanically to Markdown. The text is otherwise
  verbatim. Omitted: the epic summary, background, goals, metrics, design reference, open questions
  and sequencing sections, and a stray chat prompt between the epic and the stories.
- **Corrected references** (product decision, 2026-10-19). The text below is left as written:
  - The 22 sections listed below became 23 in the current description, which splits Research
    Areas (`research-area`) from Research Initiatives (`research-initiative`).
  - Story 1's "archive-lifecycle state (see Story 8)" means Story 11.
  - Story 1's "chart rollups in Story 7" and Story 2's "frontend chart components (Story 9)" mean
    Story 13.

### In Scope — Page Sections

Each maps to a distinct functional area; sizing/estimation should treat these as candidate Stories under this Epic.

| # | Section | Functionality |
| --- | --- | --- |
|1|**Masthead / global search**|Global search across studies, datasets, accession IDs; sign-in; persistent sub-nav with anchor links to every section below|
|2|**Hero**|Program stats (studies, data size, tools count), rotating announcements carousel, primary CTAs (search data library, search tools library), "New to AnVIL" onboarding panel (free cloud credits / AnVIL Lite signup, workshop signup)|
|3|**Latest Data Releases**|Chronological shelf of newly released datasets; email opt-in for release notifications|
|4|**Most Requested Datasets**|Ranked shelf by request volume; shows reviewing DAC per dataset|
|5|**Largest Cohorts**|Shelf ranked by sample count|
|6|**Open Access Datasets**|No-request-needed datasets|
|7|**Instant Approval — Controlled Access Data**|Datasets whose DAC has pre-cleared automatic approval when a submitted Research Use Statement matches approved DUO terms; shows median approval time, approval method, consent-code tooltip (GRU/HMB/NPU/etc.), "Request now" CTA|
|8|**Datasets at a Glance (Analytics)**|Bar charts (by disease area, by data type) and a donut chart breaking down the full catalog by access type (open / instant-approval / DAC-reviewed controlled), each segment/label clickable through to filtered results|
|9|**Datasets Moving to Archive**|Proactive warning strip + shelf of datasets on a countdown to cold storage, each showing archive date, post-archive retrieval SLA (24–48 hrs), size, and computed total retrieval fee; CTA to request/download before the cutover|
|10|**Analysis Apps (Featured Tools)**|Integrated analysis platforms, feature-card treatment for a hero tool|
|11|**Workflows**|New/updated workflow tools (variant callers, QC pipelines, etc.)|
|12|**Notebooks**|Curated notebook templates (Jupyter/Python, RStudio/Bioconductor, GATK, Scanpy, PLINK)|
|13|**AI Models**|Catalog of ML models usable against AnVIL data (Enformer, AlphaMissense, Nucleotide Transformer, DeepVariant, ESM-2)|
|14|**Featured Workspaces**|Example/starter Terra-style workspaces|
|15|**Latest Published Science**|Recent publications using AnVIL data/tools; submission prompt for researchers to self-report publications|
|16|**Research Areas / Research Initiatives**|Browsable groupings by disease/research domain and by named consortium initiative|
|17|**AnVIL Impact (Clinical Impact Portfolio)**|Embedded/linked visualization (via through.bio integration) showing a citation tree from AnVIL-funded publications through follow-on research to clinical trials, with a summary stat banner|
|18|**Community Conference**|Save-the-date panel with registration-alert signup|
|19|**Where Else**|Cross-links to related external resources/programs|
|20|**Workshops**|Public + private/in-house training offerings|
|21|**Scholars Program**|Program overview/enrollment entry point|
|22|**Tech & Policy Interns**|Program overview/enrollment entry point|

### Key Functional Requirements

1. **DUO-based automated approval engine**: system must evaluate a submitted Research Use Statement against a dataset's approved Data Use Ontology terms and auto-approve without human DAC review when they match; must log method ("Automated DUO match") and time-to-approval for audit/reporting.
1. **Storage lifecycle awareness**: datasets must carry an archive-eligibility state machine (hot → scheduled-to-archive → archived) with a computed archive date, and cost/latency metadata (retrieval fee per GB, 24–48 hr SLA) surfaced ahead of the transition.
1. **Unified catalog metadata model**: a single dataset record must support the attributes used across shelves — accession ID, consent code(s) (GRU/HMB/NPU/etc.), reviewing DAC, disease area, data type, sample count, size, release date — since the same dataset can appear in multiple shelves (e.g., "Most Requested" and "Instant Approval").
1. **Notification subscriptions**: email capture for (a) new-release alerts and (b) conference-registration alerts, with institutional-email pattern expectations (`you@institution.edu`).
1. **Analytics rollups**: aggregate counts by disease area, by data type, and by access-type (open/instant/DAC-reviewed) must be computed/cached for the "at a glance" charts; each chart segment links through to a filtered dataset list.
1. **External data/impact integration**: the "AnVIL Impact" panel sources data from through.bio (or equivalent) — needs a defined integration/embedding approach (iframe, API pull, or static export) and attribution.
1. **Global search**: single search box spanning studies, datasets, and accession IDs.

### Out of Scope (for this Epic)

* The actual DAC manual-review workflow/UI (assumed to already exist; this Epic only adds the automated pre-clearance path in front of it)
* Terra workspace execution environment itself (only the "featured workspaces" discovery shelf is in scope)
* through.bio's underlying analytics platform (only the embedding/linking of its output)
* Payment/billing flow for archive retrieval fees (surfacing the fee is in scope; charging is not)

### Foundational

#### Story 1 — Unified Dataset Catalog Data Model

**As a** platform engineer, **I need** a single dataset record schema that carries all attributes referenced across the new homepage, **so that** the same dataset can appear correctly in multiple shelves (e.g., a dataset can be both "Most Requested" and "Instant Approval") without duplicated or inconsistent data.

**Acceptance Criteria**

* Dataset record includes: accession ID, title, description, disease area(s), data type(s), sample count, size (TB/GB), release date, reviewing DAC, consent code(s) (GRU/HMB/NPU/etc.) with human-readable tooltip text, access type (open / instant-approval / DAC-reviewed), and archive-lifecycle state (see Story 8).
* Schema supports a dataset belonging to multiple disease areas and data types (for chart rollups in Story 7).
* Existing DUOS/AnVIL dataset records are backfilled/migrated into the new schema without data loss.
* Schema changes are documented and reviewed by data stewards before rollout.

**Notes**: This is a prerequisite for nearly every other story below — sequence it first.

#### Story 2 — Catalog Aggregation & Analytics Rollup Service

**As a** portal, **I need** precomputed aggregate counts by disease area, data type, and access type, **so that** the "at a glance" charts and shelf rankings ("Most Requested," "Largest Cohorts") render quickly without expensive live queries.

**Acceptance Criteria**

* A scheduled or event-driven job recomputes rollups when datasets are added, updated, or change access-type/lifecycle state.
* Rollups expose: count and % by access type (open/instant/DAC-reviewed), count by disease area, count by data type, ranked lists for "most requested" and "largest cohorts."
* Rollup data is cached with a defined freshness SLA (e.g., ≤15 min staleness) and a manual refresh/invalidation path.
* API/service exposes these rollups in a form the frontend chart components (Story 9) can consume directly.

### Global Navigation & Hero

#### Story 3 — Masthead, Global Search, and Section Navigation

**As a** researcher, **I want** to search across studies, datasets, and accession IDs from anywhere on the page and jump directly to any section, **so that** I can quickly find what I'm looking for without scrolling through the full page.

**Acceptance Criteria**

* Persistent header includes a search input that queries across study names, dataset titles, and accession IDs, returning results ranked by relevance.
* Search is accessible via keyboard (Enter to submit, proper `aria-label`s).
* Sub-navigation bar lists all page sections as anchor links; clicking scrolls smoothly to the corresponding section.
* Sign-In CTA is present and routes to the existing auth flow.
* Layout is responsive down to tablet width; sub-nav either wraps or becomes a scrollable strip on narrow viewports.

#### Story 4 — Hero Section: Program Stats, Announcements, and Onboarding Panel

**As a** first-time or returning visitor, **I want** to immediately see program scale, current announcements, and a clear path to get started, **so that** I understand what the platform offers and how to begin.

**Acceptance Criteria**

* Stat strip displays live counts (studies, total data size, accessible tools) sourced from the catalog service, not hardcoded.
* Announcements carousel supports multiple entries, auto-advances or is manually navigable (prev/next controls), and each slide links to a full announcement/release/registration page.
* "New to AnVIL" panel presents the free-credits program (e.g., AnVIL Lite) and workshop signup as distinct CTAs.
* Primary CTAs ("Search the Data Library," "Search the Tools Library") route to the correct filtered search views.
* Section is keyboard-navigable and carousel respects `prefers-reduced-motion`.

### Discovery Shelves (Datasets)

#### Story 5 — Latest Data Releases Shelf + Release Notification Signup

**As a** researcher, **I want** to see newly released datasets and optionally get emailed when new ones drop, **so that** I don't have to keep manually checking for releases relevant to my research.

**Acceptance Criteria**

* Shelf shows datasets ordered by release date descending, with a "New" badge for recent entries (define recency window, e.g., ≤30 days).
* "See all releases" link routes to a full, filterable release list.
* Email capture form validates a well-formed email, submits to the notification subscription service, and shows a success/error state.
* Subscribers receive a real email when a new dataset is published (define channel: transactional email service).

#### Story 6 — Most Requested Datasets Shelf

**As a** researcher, **I want** to see which datasets are most in-demand, **so that** I can gauge relevance/quality signal from other researchers' behavior.

**Acceptance Criteria**

* Shelf ranks datasets by request volume over a defined rolling window (e.g., trailing 90 days) — window is configurable, not hardcoded.
* Each card shows the reviewing DAC.
* Ranking updates on the cadence defined by the rollup service (Story 2).

#### Story 7 — Largest Cohorts Shelf

**As a** researcher, **I want** to browse datasets by cohort/sample size, **so that** I can find datasets with sufficient statistical power for my study design.

**Acceptance Criteria**

* Shelf ranks datasets by sample count descending.
* Sample counts are pulled from the catalog record (Story 1) and match what's shown on the dataset's own detail page.

#### Story 8 — Open Access Datasets Shelf

**As a** researcher, **I want** a dedicated shelf of datasets that require no access request, **so that** I can start working immediately without any approval wait.

**Acceptance Criteria**

* Only datasets flagged `access type = open` in the catalog appear here.
* Card CTA goes straight to download/access rather than a request flow.

### Instant Approval (Automated DUO Matching)

#### Story 9 — Automated DUO Matching Engine (Backend)

**As a** DAC / platform, **I want** access requests automatically evaluated against a dataset's approved Data Use Ontology terms, **so that** clearly-compliant requests are approved instantly without consuming manual reviewer time.

**Acceptance Criteria**

* Given a submitted Research Use Statement and a dataset's approved DUO terms, the engine returns one of: **auto-approved**, **needs manual review**, or **auto-rejected** (if applicable), with a machine-readable reason code.
* Every auto-approval is logged with: timestamp, matched DUO terms, time-to-decision, and an immutable audit trail record (for compliance/reporting).
* Ambiguous or partial matches route to the existing manual DAC review queue rather than defaulting to approval.
* A dataset/DAC admin can mark specific datasets/tiers as eligible for automated approval (opt-in per dataset, not global default).
* Engine decision time is measured and reportable (feeds the "Median approval time" stat shown in Story 10).
* Security/compliance review sign-off obtained before enabling auto-approval in production, given this bypasses human DAC review.

**Open question carried from Epic**: confirm fallback behavior on ambiguous matches with data governance stakeholders before implementation.

#### Story 10 — Instant Approval Shelf (Frontend)

**As a** researcher, **I want** to see which controlled-access datasets I can get approved for near-instantly, **so that** I can prioritize requests that won't block my work.

**Acceptance Criteria**

* Shelf displays only datasets enabled for automated approval (Story 9).
* Each card shows: median approval time, approval method ("Automated DUO match"), consent code with hover/tap tooltip explaining the code, reviewing entity, and sample count.
* Info tooltip on the section header explains in plain language how instant approval works (matching submitted Research Use Statement to approved DUO terms).
* "Request now" CTA submits directly into the request flow pre-flagged for automated evaluation, and the requester sees a real-time status update (approved / pending manual review) rather than a generic "submitted" message.

### Storage Lifecycle & Archive

#### Story 11 — Dataset Storage Lifecycle State Machine (Backend)

**As a** platform, **I need** datasets to move through defined storage states (hot → scheduled-to-archive → archived) with a computed archive date, **so that** the frontend can proactively warn researchers before access gets slower and more expensive.

**Acceptance Criteria**

* State machine states and transition rules are defined and documented (what triggers "scheduled-to-archive," e.g., request-volume threshold + time since last access).
* Each dataset in `scheduled-to-archive` exposes: archive date, size, and a computed total retrieval cost (size × per-GB retrieval rate from the storage vendor/policy table).
* Retrieval SLA (24–48 hrs) is configurable, not hardcoded, in case the underlying storage tier changes.
* Transition to `archived` is reflected in the catalog and removes the dataset from "hot" shelves/instant paths as applicable.

**Open question carried from Epic**: confirm source of truth for per-GB retrieval pricing (storage vendor API vs. internal policy table).

#### Story 12 — Datasets Moving to Archive Shelf

**As a** researcher, **I want** advance warning that a dataset I care about is about to become slower/costlier to retrieve, **so that** I can request or download it while it's still in hot storage.

**Acceptance Criteria**

* Shelf shows a persistent warning strip explaining the retrieval slowdown/cost once archived.
* Each card shows: days-until-archive countdown badge, archive date, retrieval SLA, size, and computed total retrieval cost.
* CTA text differs appropriately by dataset state (e.g., "Request before archive" vs. "Download before archive" for datasets the user already has access to).
* Shelf only shows datasets in the `scheduled-to-archive` state (Story 11), ordered by soonest-to-archive first.

### Analytics

#### Story 13 — "Datasets at a Glance" Analytics Panel

**As a** researcher or program stakeholder, **I want** a visual breakdown of the catalog by disease area, data type, and access type, **so that** I can quickly understand what's available and how access is distributed.

**Acceptance Criteria**

* Bar charts render disease-area and data-type counts sourced from the rollup service (Story 2); each bar label links to a pre-filtered dataset list.
* Donut chart shows % split across open / instant-approval / DAC-reviewed access types, with a center total-dataset count; each legend row is clickable through to a filtered view.
* Charts are accessible: proper `aria-label`/`role="img"` summaries describing the data (not just visual bars/arcs), and values are readable via screen reader.
* Chart values update on the rollup cadence and never silently go stale beyond the defined SLA.

### Tools & Compute Ecosystem Shelves

#### Story 14 — Analysis Apps (Featured Tools) Shelf

**As a** researcher, **I want** to discover integrated analysis platforms available on AnVIL, **so that** I know what compute tools I can use without leaving the platform.

**Acceptance Criteria**

* Shelf supports a featured "hero" tool treatment distinct from standard cards.
* Each tool links to its own detail/launch page.

#### Story 15 — Workflows Shelf

**As a** researcher, **I want** to see new and updated analysis workflows, **so that** I can find pipelines relevant to my data type without building my own.

**Acceptance Criteria**

* Shelf lists workflow tools with title, description, and relevant tags (e.g., data type it operates on).
* Newly added/updated workflows are indicated (e.g., "New" badge) consistent with the pattern used in Story 5.

#### Story 16 — Notebooks Shelf

**As a** researcher, **I want** to browse curated analysis notebook templates, **so that** I can start from a working example instead of a blank environment.

**Acceptance Criteria**

* Shelf lists notebook templates across supported environments (Jupyter/Python, RStudio/Bioconductor, etc.) with a one-click "open in workspace" or equivalent action.

#### Story 17 — AI Models Shelf

**As a** researcher, **I want** to discover ML models that can be run against AnVIL data, **so that** I can apply state-of-the-art models without sourcing/hosting them myself.

**Acceptance Criteria**

* Shelf lists available models with a short description of what each predicts/produces and a link to documentation/launch.

#### Story 18 — Featured Workspaces Shelf

**As a** new researcher, **I want** example starter workspaces, **so that** I have a concrete, working starting point for common analyses.

**Acceptance Criteria**

* Shelf lists workspace templates (e.g., WGS QC, GWAS, single-cell RNA-seq) each linking to a clonable/launchable workspace.
* Cross-linked from the Hero's "New to AnVIL" panel (Story 4).

### Research & Impact

#### Story 19 — Latest Published Science Shelf + Publication Submission

**As a** researcher, **I want** to see recent publications that used AnVIL data/tools, and be able to submit my own, **so that** the platform's scientific output stays visible and current.

**Acceptance Criteria**

* Shelf lists recent publications with citation, brief description, and a link out.
* A visible prompt lets researchers self-report a publication that used AnVIL data/tools; submission enters a review/moderation queue before appearing publicly.

#### Story 20 — Research Areas & Research Initiatives Browsing

**As a** researcher, **I want** to browse datasets/tools grouped by disease/research domain and by named consortium initiative, **so that** I can find everything relevant to my field in one place.

**Acceptance Criteria**

* Two distinct groupings are browsable: by research/disease area, and by named initiative (e.g., TOPMed, eMERGE).
* Selecting a group routes to a filtered view showing all associated datasets, tools, and workspaces (not datasets only).

#### Story 21 — AnVIL Impact Panel (through.bio Integration)

**As a** program stakeholder, **I want** to show how AnVIL-funded publications have led to downstream clinical trials, **so that** the platform's real-world impact is visible to visitors and funders.

**Acceptance Criteria**

* Panel embeds or links to the through.bio Clinical Impact Portfolio view with correct attribution ("Data courtesy of through.bio").
* Summary stat banner (e.g., "N publications contributed to N clinical trials for N diseases") is sourced from through.bio data, not hardcoded copy.
* If a live embed isn't feasible for this release, a periodically-refreshed static/cached snapshot is acceptable as an interim solution — flag explicitly as a known limitation rather than presenting it as live.
* Data-sharing/API agreement with through.bio is confirmed before integration work begins.

### Community & Education

#### Story 22 — Community Conference Panel + Registration Alert

**As a** prospective attendee, **I want** to see conference details and get notified when registration opens, **so that** I don't miss the signup window.

**Acceptance Criteria**

* Panel shows location/date and description; registration CTA is either a live link (if open) or an email-alert signup (if not yet open), matching current state.
* Email subscribers are notified via the same notification service used in Story 5.

#### Story 23 — Education & Community Program Panels (Where Else, Workshops, Scholars, Tech & Policy Interns)

**As a** prospective participant, **I want** to learn about AnVIL's outreach programs and cross-linked external resources, **so that** I can find training or program opportunities beyond direct data access.

**Acceptance Criteria**

* Each of the four panels (Where Else / Workshops / Scholars / Tech & Policy Interns) renders its own content block with a description and a clear enrollment/contact CTA.
* Workshops panel includes both public workshop signup and a request path for private/in-house training.
* Content in each panel is managed independently (e.g., via CMS or config) so program teams can update copy without a code deploy.
