# Elasticsearch to PostgreSQL Migration — Design

Ticket: [DT-4437](https://broadworkbench.atlassian.net/browse/DT-4437)

**Status: draft (2026-11-19). No decision is made.** Section 9 gives a draft recommendation.
Section 11 lists the questions that the team must answer before the recommendation is final.

**This part of the document has sections 1 to 6 and section 12.** Sections 7 to 11 are in the
pull request that follows this one. References to those sections point to it.

Related documents:

- [`elasticsearch-service-duos-ui-usage.md`](elasticsearch-service-duos-ui-usage.md): the usage
  inventory and the access control epics (A to H).
- [`es-access-contract.md`](es-access-contract.md): the access contract (Ticket A-2).
- [`es-security-capability-record.md`](es-security-capability-record.md): the measured security
  capabilities of the clusters (Ticket A-1).

## 1. Purpose

Consent pays for an Elasticsearch (ES) cluster. This document shows what the team must do to remove
ES and to supply the same search functions from PostgreSQL.

The ticket gives four options:

1. Write everything again to use PostgreSQL.
2. Translate ES queries to PostgreSQL queries.
3. Build general facilities for generic search services (a hybrid of 1 and 2).
4. Find libraries that can help with option 2.

The ticket asks for four criteria: maintenance, scope of work, time to deliver, and security for
future requirements such as fine-grained access control.

### 1.1 Goals

- Remove all ES use from consent, from duos-ui, and from the deployment.
- Keep the search functions that users have today.
- Complete the work one sprint before the ES renewal in February 2027.
- Do not make future access control more difficult.

### 1.2 Non-goals

- New search functions.
- The full access control program (Epics B to H). This design must agree with that program, but it
  does not deliver it.
- A cheaper ES tier or a self-hosted ES cluster. The ticket asks for removal. If the date is at
  risk, a short renewal is a fallback (see section 10).

### 1.3 Why do this work

- ES has a recurring yearly fee, and the team expects the fee to increase.
- The effort for the milestone is about 5 to 7 person-weeks (section 9.1a). That effort probably
  costs more than one year of the fee. The team must check this.
- Thus the fee of one year is not the full reason. The other reasons are:
  - The fee recurs each year. The effort occurs one time.
  - One less system to operate, to secure, and to keep compliant.
  - No separate index that can differ from the database.
  - Server-owned queries, which the access control program needs in any case (section 7).

### 1.4 Anti-goals

- A design that makes consent an ES emulator that the team must maintain for years.
- A design that keeps the visibility rules in the browser.
- A change that shows a user more data than the user can see today.

## 2. Constraints

| Constraint | Detail |
| --- | --- |
| Date | Renewal is in February 2027. The exact date is not known. This plan uses 1 February 2027, the earliest possible date. The work must be complete one sprint before. |
| Time | The current sprint ends 2026-11-25. If sprints are two weeks, four sprints end before the target: they end on 9 December, 23 December, 6 January, and 20 January. The holidays remove about one. Thus about three work sprints remain. The sprint length is an assumption. |
| People | Four people on the team can do duos-ui work. All four can work on this migration. |
| Cost | ES has a recurring yearly fee. The team expects the fee to increase. |
| Compliance | A compliance requirement applies to the hosting of each data store, and it is a cause of the ES cost. The PostgreSQL databases already satisfy it. A replacement must not add a new system that needs a compliance approval. |
| Data size | About 6,000 datasets in dev and in production. The team expects more than 10,000 in the next two quarters. The count will only increase. |
| Approval | Product must approve server-owned visibility rules (section 7.4). |
| Database host | The databases are Cloud SQL instances. Cloud SQL permits only the extensions on its supported list. |
| Database version | The move from PostgreSQL 16 to 18 is in progress (`postgres-18-upgrade-plan.md`). The tests stay on PostgreSQL 16 until the production upgrade. Thus SQL that needs version 17 or later fails in the tests and cannot go to production before that upgrade. |
| Connections | The production database has a fixed connection limit. All search load moves to this database. |
| Two repositories | The browser builds the ES queries. Thus most options change duos-ui too. |
| Existing contract | `es-access-contract.md` is neutral between the two ES mechanisms, Epic D and Epic E. It does not name PostgreSQL. Its dimension matrix and its field classification do not depend on ES, so this plan applies them to a PostgreSQL search too. |

## 3. Current state

### 3.1 Summary

- There is one ES index, `dataset`. It holds one document for each published dataset, not for
  each dataset. A dataset is in the index after an index write, and it leaves the index after an
  admin delete (section 3.5).
- `ElasticSearchService.toDatasetTerm` builds each document from PostgreSQL data.
- ES holds no data of its own. PostgreSQL is the source for every field. But Java code computes
  some fields, so they are not a direct read of a column:
  - `soApprovalModel` and `instantApprovalEligible`, from the automation rules of the DAC.
  - `dataUse`, from the data use translation rules.
  - `study.assets`, from the study properties.
  - duos-ui filters on `soApprovalModel.keyword`. This is important for a storage option that
    builds the fields in SQL.
- The index has no explicit mapping. It uses dynamic mapping.
- duos-ui builds raw ES query JSON in the browser.
- Two endpoints send that JSON to ES without change.
- Those two endpoints authenticate the caller. They do no authorization, and they add no
  visibility filter.
- The server builds its own search queries in one place only: `DashboardSearchService`. Other
  server code also sends requests to ES:
  - `ElasticSearchService` sends `_delete_by_query` for a removal and `_validate/query` before a
    v1 search.
  - `ElasticSearchCapabilityService` sends `_search` and `_count` probes.

### 3.1a Personal data in the documents

The documents hold personal data. Each signed-in user can read all of it through the two search
endpoints, because the server adds no filter.

| Field | Content |
| --- | --- |
| `study.dataSubmitterEmail` | Email address of the study creator |
| `study.dataCustodianEmail` | Email addresses of the data custodians |
| `dac.dacEmail` | Email address of the DAC |
| `createUserDisplayName`, `submitter.displayName`, `updateUser.displayName` | Names of the users who created, submitted, and changed the dataset |
| `submitter` and `updateUser` | User objects, which include the institution |
| `study.piName` | Name of the principal investigator |
| `data` and `study.data` | Free-form maps from the registration. Their content has no fixed list of keys. |

`es-access-contract.md` classifies each field (its section B). It records that the catalog already
shows PI names, custodian emails, submitter emails, and DAC emails to each signed-in user, and that
duos-ui fails if the custodian email is absent. Thus the classification of these fields is a
product decision, not only a technical one.

Each storage option and each response projection in the later sections must say which of these
fields it stores, which it returns, and to whom.

### 3.2 Read paths

| Path | Caller | What it does |
| --- | --- | --- |
| `POST /api/dataset/search/index` (v1) | duos-ui, 4 call sites | Sends the client query to `_validate/query`, then to `_search`. Returns the list of `_source` documents. |
| `POST /api/dataset/search/index/v2` | duos-ui Data Library, Submissions page, Study page | Sends the client query to `_search`. Returns the raw ES response, with `hits` and `aggregations`. |
| `DashboardSearchService` | Researcher, DAC, and Signing Official dashboards | Builds aggregation queries on the server. Returns counts. |
| `ElasticSearchCapabilityResource` | Admin only | Probes the security functions of the cluster. The `/capabilities` operations create and delete API keys and roles in the cluster. `GET /license` and `POST /license/trial` read the license and start a trial. Not a search path. The four operations and their OpenAPI files are part of the removal. |
| `ElasticSearchHealthCheck` | `/status` | Reads the cluster health. A failure sets `degraded`, not `ok=false`. |

Both search endpoints use `@PermitAll`. All duos-ui routes that call them are behind sign-in.

Both search endpoints reject a request where `from + size` is larger than 10000
(`ElasticSearchService.MAX_RESULT_WINDOW`). The replacement must keep this limit or change it on
purpose.

The public showcase catalog (`PublicShowcaseCatalogResource`) does not use ES. It reads PostgreSQL
through `DatasetDAO`.

### 3.3 The queries that duos-ui sends

This set of queries is the scope of the search functions that the replacement must supply. duos-ui
commit `84fe8af4` is the source.

The set is closed only in the browser. Both endpoints accept any ES query from any signed-in user.
Thus this list is not the limit of what a caller can send today, and the security sections must
not use it as that limit.

**The v1 callers (four simple queries)**

| Page | Query | Fields read |
| --- | --- | --- |
| DAC Datasets (`DACDatasets.tsx`) | `terms dacId`, size 10000 | Most dataset fields. Search occurs in the browser. |
| Manage DAC and DAC Profile (`dacDatasetGrid.tsx`) | `terms dacId`, size 10000 | `datasetId` and `dataUse` only |
| Dataset page (`DatasetStatistics.tsx`) | `match_phrase` on `datasetId` or `datasetIdentifier` | One document |
| DAR review and DUC addendum (`BucketUtils.ts`) | `terms _id`, size 10000 | `dataUse`, `dac`, `dacId` |

**The v2 callers (the Data Library engine)**

| Request | Shape |
| --- | --- |
| Datasets tab | Paged hits with `from`, `size`, and one `sort` field. Five `terms` facets. |
| Studies tab | `size: 0`. A `cardinality` total. Five `terms` facets. One aggregation on `study.studyId` with `top_hits`, `value_count`, `sum`, and three `terms` sub-aggregations. The form of that aggregation depends on the sort: |
| Studies tab, sort by a metric | A `terms` aggregation with `order` by a sub-aggregation and with `shard_size`. |
| Studies tab, sort by name | A `composite` aggregation with two sources: the study name and the study id. |
| Studies tab, no sort | A `composite` aggregation with one source: the study id. |
| Tab counts | `size: 0`. A `cardinality` of `study.studyId` for the Studies count. A `filter` aggregation for the Datasets count. A `terms` aggregation on `study.studyId` (size 10000) with `top_hits` of `study.*`. Sent on each search or filter change. |
| Eight asset tabs | No request of their own. They read the tab counts response and filter rows in the browser. |
| Option corpus | As tab counts, with `_source` limited to `study.assets.*`. |
| Facet metadata | The Datasets request with `size: 0` and no filters. Held for 24 hours. |
| Study page grid | Paged hits for one `study.studyId`, with `top_hits` and `sum`. |
| Study page, all datasets | Two requests with `size` equal to the total number of datasets in the study: one for the ids that a user can select, and one for the DAC ids. They fail if a study has more than 10000 datasets. |

**Clause types in use**

- `bool` with `must`, `should`, `must_not`, `filter`, and `minimum_should_match`.
- `term`, `terms`, `match`, `match_phrase`, `exists`, and `range`.
- `multi_match` of type `phrase_prefix` across about 70 fields. This is the search box.
- Matches on the `_index` and `_id` metadata fields.

**Aggregation types in use**

`terms`, `composite` (without `after`), `top_hits`, `cardinality`, `value_count`, `sum`, and `filter`.

**Shapes that the server builds (`DashboardSearchService`)**

`match_all`, `match_none`, a `global` aggregation, and `track_total_hits`. These are not in the
duos-ui set, but the replacement of the dashboard counts must supply the same results.

**Not in use**

Highlight, fuzzy match, wildcard, regular expression, `query_string`, scripts, `nested`,
`search_after`, and score functions.

**Visibility rules that the browser builds**

- `study.publicVisibility: true` for users without a privileged role.
- The approved rule: not controlled, or controlled with `dacApproval: true`.
- Ownership for the Submissions page: `createUserId`, `study.dataSubmitterId`, or
  `study.dataCustodianEmail`.
- DAC scope by `dacId`.
- Branded library scope by tags or by the institution of the submitter.

A user can remove these clauses from the request. The server does not put them back.

The browser does not add each rule to each request. This table is the baseline for the anti-goal
"no more data than today" (section 1.4):

| Caller | `study.publicVisibility` | Approved rule | Scope clause |
| --- | --- | --- | --- |
| Data Library, Datasets tab | Yes, for a user without a privileged role | Yes | Library scope |
| Data Library, Studies tab and asset tabs | Yes, for a user without a privileged role | **No** | Library scope |
| Data Library, tab counts | Yes, for a user without a privileged role | Only in the Datasets count | Library scope |
| Submissions page | **No** | **No** (`showAllControlled: true`) | Ownership |
| Study page | **No** | **No** (`showAllControlled: true`) | One study id |
| The four v1 callers | **No** | **No** | `dacId`, a dataset id, or a list of ids |

A server rule that applies each rule to each caller changes the results of the callers with a
"No". The later sections must decide this for each caller.

### 3.4 Write paths

- Twelve service call sites write to the index. Each follows a dataset, study, or DAC change:
  seven in `DatasetService`, three in `DatasetRegistrationService`, one in `DacService`, and one in
  `DACAutomationRuleService`.
- One resource call also writes: `PATCH` of a dataset in `DatasetResource`, for Admin, Chairperson,
  and Data Submitter.
- Three admin endpoints also write to the index.
- All writes occur on the request thread, with one exception. A DAC rule toggle writes on an executor.
- There is no scheduled job, no retry queue, and no outbox.
- Most callers catch an index failure, write a log entry, and continue. The document then becomes stale.
- Changes to a DAC name, a user name, an institution name, or an ontology label cause no index write.
  Documents that contain those values become stale.
- These fields also become stale:
  - `dac.dacEmail`, after a DAC update.
  - The submitter email, after an admin redacts the user.
  - `deletable`, which depends on DAR use. The submission of a DAR causes no index write.
  - `hasInstitutionCertification`, after the cleanup task deletes the file.
- `POST /api/dataset/index` builds the full index again. Admins use it. It has two defects that
  the migration must not copy:
  - It writes each row of `dataset` (`findAllDatasetIds`), and it sets `indexed_date` and
    `index_confirmed_date` for each. Thus it publishes again each dataset that an admin removed
    with `DELETE /api/dataset/index/{datasetId}`. This is in conflict with the meaning "published
    in the library" (section 3.5).
  - It does not remove a document that has no dataset. For that reason `scripts/index-es.sh`
    deletes the index before it calls the endpoint.
  - The first fill of a replacement and a rollback to ES must each use the set of published
    datasets, not all datasets.

### 3.5 Index dates in PostgreSQL

Two dataset columns record the index state:

- `dataset.indexed_date` means that the dataset must be in the index.
- `dataset.index_confirmed_date` means that ES accepted the document.

`AssetLookupDAO` reads `index_confirmed_date` to decide if a study or a dataset is in the library.
Thus "indexed" has a product meaning: **published in the library**. That meaning must stay after ES
goes.

### 3.6 Precedents in this repository

| Precedent | Where | Why it helps |
| --- | --- | --- |
| Ontology search moved from ES to PostgreSQL | `ontology_index` table, `OntologyDAO.findByQuery`, [DT-2530](https://broadworkbench.atlassian.net/browse/DT-2530) and [DT-2875](https://broadworkbench.atlassian.net/browse/DT-2875) | A generated `tsvector` column, a GIN index, and a prefix `tsquery`. The same pattern applies here. |
| Study card fields in SQL | `StudyRecommendationDAO` | SQL already derives participant count, access management, data use codes, and asset counts in the way that ES indexes them. |
| GIN indexes on `jsonb` | `data_access_request.data` | The team already operates this index type. |

### 3.7 Other dependencies on ES

The removal must include each of these:

- `scripts/index-es.sh`, and `scripts/seed-showcase-fixture.sh`, which tells the user to run it.
- The `elastic` service and the `-Ddw.elasticSearch.*` flags in
  `scripts/templates/docker-compose.yaml`.
- `ElasticSearchConfiguration`, which includes the unused `indexName` key.
- The ES client dependency in `pom.xml`.
- The static method `ElasticSearchService.buildMapFromPropertyValue`. `ConsentGroupFromDataset`
  calls it, and that class has no other relation to ES. The method must move before the class goes.
- The capability endpoints and their `elasticSearch*.yaml` OpenAPI files (section 3.2).

## 4. Two separate decisions

The four options in the ticket are about the **query interface**: what the browser sends and what
the server accepts. A second decision is about **storage**: where the search data lives in
PostgreSQL. The two decisions are almost independent.

### 4.1 Storage decision

Storage C is a database view. It has two variants. C1 is a standard view. C2 is a materialized
view. A peer review of this draft added storage C.

| | A. Live joins | B. Search document table | C1. Standard view | C2. Materialized view |
| --- | --- | --- | --- | --- |
| Design | Each search joins `dataset`, `study`, `dataset_property`, `study_property`, `dac`, `users`, and `institution`. | One row for each dataset in a new table. The row holds the document as `jsonb`, a `tsvector` column, and typed columns for the common filters. | A named view holds the joins of storage A. Each search reads the view. PostgreSQL runs the joins on each query. | The same view definition, but PostgreSQL stores the result. A refresh builds the full result again. |
| Document builder | Written again as SQL. | `toDatasetTerm` stays. Its output goes to the table and not to ES. | Written again as SQL, in the view definition. | Written again as SQL, in the view definition. |
| Write triggers | None. Data is always current. | The current triggers stay: twelve service call sites and one resource call (section 3.4). The write joins the database transaction. | None. Data is always current. | A refresh after each change, or a refresh on a schedule. No write for one row exists. |
| Stale data | None. | The same stale cases as today (section 3.4), unless live joins or a view replace them later. | None. | All fields are stale between two refreshes. A refresh also corrects the name changes that storage B leaves stale. |
| Indexes for search | On the base tables only. | Yes. A GIN index on the `tsvector` column. | None on the view. Each query computes the search text again. | Yes. A GIN index on the `tsvector` column. |
| Query difficulty | High. Properties are key and value rows. Assets are JSON in `study_property`. | Low to medium. `jsonb` operators and normal columns. | High one time, in the view definition. Low in each search query. | High one time, in the view definition. Low in each search query. |
| Parity risk | High. All field logic changes at one time. | Low. The documents are identical to the ES documents. | High, as in storage A. | High, as in storage A. |
| "Published" meaning | Needs a new explicit column. | A row in the table means published. `indexed_date` keeps its meaning. | Needs the explicit column. The view definition filters on it. | Needs the explicit column. The change shows in search only after a refresh. |
| First fill | Not necessary. | An admin endpoint call after deploy. | Not necessary. | The Liquibase changeset fills the view. No admin call is necessary. |
| Change to the document shape | Change the queries. | Change `toDatasetTerm`, then build all rows again. | A changeset replaces the view. | A changeset must drop and create the view. `CREATE OR REPLACE` does not exist for a materialized view. |

**Draft position: storage B.** It changes the smallest quantity of logic, and it makes a comparison
with ES possible document by document. Storage A or storage C1 can replace B later, one field at a
time.

Data size: the table holds about 6,000 rows now and more than 10,000 soon. This is small for
PostgreSQL. A full scan of the table is fast at this size, so most filters do not need an index.
The `tsvector` column gets a GIN index. Measure before the team adds more indexes. The size does
not change the choice between storage A and storage B. The size helps storage C1 and storage C2:
the joins and a full refresh are small at this size. But only a measurement can show that they are
fast enough (see the notes for storage C).

Personal data (section 3.1a): storage B and storage C2 store a copy of each personal data field.
Storage A and storage C1 store no copy. In each storage type, the response projection decides which
fields a caller gets, not the storage (section 7.2). A stored copy must also change when an admin
redacts a user, or the copy keeps the old value (section 3.4).

Notes for storage B:

- The write must occur in the same transaction as the data change. Then the document cannot differ
  from the data after a failure, and `index_confirmed_date` is no longer necessary.
- The current index calls do not give this. The team must change them:
  - Registration calls the index after the database write is complete
    (`DatasetRegistrationService`). The team must check each of the other triggers.
  - A DAC rule toggle writes on an executor, after the commit (`DACAutomationRuleService`).
  - The document builder reads through its own DAO objects. Those reads and the row write must use
    the transaction of the data change, or the row can contain data from before the change.
  - A DAC rule toggle writes one row for each dataset of the DAC. In the transaction, that work
    occurs before the commit. Measure it for the largest DAC. If it is too slow, the toggle needs a
    different design, for example one SQL statement that changes only the rule fields.
  - This work is part of phase 1. The draft size of phase 1 does not include it yet.
- Access rules must not come from the stored document. They must join the live tables (section 7).
- The first fill of the table needs the Java document builder. Thus the fill is an admin endpoint
  call after deploy, not a Liquibase changeset.
- The fill must not use all datasets. `POST /api/dataset/index` uses `findAllDatasetIds` today and
  writes every dataset. An unchanged call publishes datasets that are not in the library, and it
  changes the answers of `AssetLookupDAO`.
- `indexed_date IS NOT NULL` is not a safe source for the fill without a check.
  `indexDatasetTerms` sets `indexed_date` before it sends the document to ES. If ES rejects the
  first write, the date stays, but the dataset is not in search and `index_confirmed_date` is null.
  A fill from `indexed_date` publishes that dataset for the first time.
- Thus the three sets must agree before the fill: the ids in ES, the rows with `indexed_date`, and
  the rows with `index_confirmed_date`. Section 9.4 makes this comparison a gate for the switch.
- The fill runs while live writes occur. An admin can remove a dataset after the comparison and
  before the fill gets to that dataset. A fill from a fixed list of ids then creates the row again.
  Thus the fill must read the publication state again for each dataset, in the transaction that
  writes the row, and with the same row lock as a live write. If the dataset is not published at
  that moment, the fill writes no row.

Notes for storage C. The PostgreSQL and Cloud SQL facts come from the documents in section 12. The
conclusions are the opinion of the author:

- **C1 is storage A with a name.** It needs the same SQL as storage A and it has the same parity
  risk. Its gain is one definition that the search service, the dashboards, and a person with
  `psql` can all read.
- **C1 has no index of its own.** The search box needs a `tsvector` over about 70 fields. C1
  computes it across the joins for each dataset on each query. The tab counts run on each filter
  change. This can be fast enough at 10,000 rows. Measure it before the team selects C1.
- **C2 cannot change one row.** `REFRESH MATERIALIZED VIEW` replaces the full contents. A
  registration, an admin publish, and an admin removal (section 4.1a) each need a full refresh
  before search shows the change.
- **Each time to refresh C2 has a cost:**

  | Time of the refresh | Effect |
  | --- | --- |
  | In the transaction of the data change | Search is correct at commit. But only one refresh can run at a time, so the writes wait for each other. The cost of each write also increases with the number of datasets. |
  | After the commit | The stale-on-failure case of today comes back. Storage B removes that case. |
  | On a schedule | A new dataset is absent from search until the next refresh. No scheduled job exists today (section 3.4). `pg_cron` is on the Cloud SQL list, but it adds a new part to operate. |

- **A refresh of C2 can block readers.** `REFRESH ... CONCURRENTLY` does not block them. It needs
  a unique index on a plain column, for example `dataset_id`, and a view that already has data.
- **A removal must not wait for a refresh.** If a refresh is late or fails, a dataset that an admin
  removed stays in a C2 view. Thus each search on C2 must join the live publication column, for
  the results and for each count. The removal then has effect at commit. A new publication can
  wait for the refresh. A removal cannot.
- **`pg_ivm` does not solve this for the team.** The extension updates a materialized view row by
  row, and Cloud SQL supports it. But the Cloud SQL document says that it is not compatible with an
  in-place major version upgrade or with a data export. The team does upgrades in place
  ([DT-3587](https://broadworkbench.atlassian.net/browse/DT-3587)).
- **The data use summary is the hardest field.** `TranslationUtil.translateSummary` builds it in
  Java from a list of rules. `StudyRecommendationDAO` derives the primary codes in SQL, and its
  comment records one known difference from the index. A view must also derive the secondary codes
  and each description, because duos-ui reads them (`BucketUtils.ts`, `DataUseUtils.tsx`).
- **"Published" needs the explicit column in C1 and in C2,** as in storage A. The publication
  check in section 9.4 then applies to that column.
- **C1 and C2 make the comparison with ES weaker.** Under storage B the same Java code builds the
  ES document and the PostgreSQL document. Under storage A or C the comparison tests new field
  logic and a new search engine at the same time.
- **The repository has no database view today.** Storage C adds a new type of object to the
  changesets and to the test setup.
- **Access rules do not come from a C2 view,** for the same reason as storage B. They join the
  live tables (section 7).

A combination is possible later, and it does not change the draft position. A standard view can
join the storage B table with `dac`, `users`, and `institution` to give the current names. That
view removes the stale name cases of section 3.4, and `toDatasetTerm` stays.

### 4.1a The admin index endpoints

Three admin endpoints exist today: build all documents, build one document, and remove one document.

| | A. Live joins, and C1. Standard view | B. Search document table | C2. Materialized view |
| --- | --- | --- | --- |
| Build one document (`POST /api/dataset/index/{datasetId}`) | **Necessary as "publish".** It sets an explicit column. No document exists. | **Necessary as "publish".** It also builds one row again if a dataset is in doubt. | **Necessary as "publish".** It sets the column. Search shows the dataset after the next refresh. |
| Remove one document (`DELETE /api/dataset/index/{datasetId}`) | **Necessary as "remove from the library".** It clears the column. | **Necessary as "remove from the library".** It deletes the row. | **Necessary as "remove from the library".** It clears the column. The view contains the dataset until the next refresh, so search must also join the live column (section 4.1). |
| Build all documents (`POST /api/dataset/index`) | Not necessary. | See the two notes below. | Useful as "refresh the view now". |

Two notes for storage B. They are the opinion of the author:

- The bulk build has two necessary uses. The first is the first fill of the table (section 4.1). The
  second is a change to the document shape: if `toDatasetTerm` gets a new field, each row needs a
  new build. A start-up task or a one-time job can do the same work, but a bulk operation of some
  type must exist.
- The single-dataset endpoints do more than repair. **Confirmed:** admins use `POST` to publish a
  dataset in the library and `DELETE` to remove it. Thus these two operations must stay in each
  storage type.

Publication has three types of event today. The design must keep them separate:

| Event | Cause today | Rule for the new design |
| --- | --- | --- |
| First publication, automatic | Dataset registration. A dataset that a study update adds. DAC approval of a dataset. Conversion of DAC datasets to external. A DAC rule toggle, for each dataset of the DAC. | Creates the row (storage B) or sets the column (storage A and storage C). No admin call is necessary. |
| First publication, by an admin | `POST /api/dataset/index/{datasetId}` | Same result as the automatic event. |
| Refresh | Each other dataset, study, or data use change | Changes a row only if the dataset is already published. Must not publish. |
| Removal | `DELETE /api/dataset/index/{datasetId}`, dataset delete, study delete | Deletes the row or clears the column. |

Consequences:

- "Published in the library" is a state that the design must store explicitly. Most datasets get it
  automatically at registration. Admins can also set it and clear it. `dataset.indexed_date`
  already has this meaning (section 3.5).
- A design that creates rows only on an admin call leaves each new registration out of search.
- Storage A needs that state in each search query as a filter.
- Storage C puts that filter in the view definition. Storage C2 shows a change to that state only
  after a refresh.
- Storage B must keep the rule that a row exists only for a published dataset. The first fill must
  obey it (section 4.1).
- A DAC approval and a DAC rule toggle force a write today. Thus they publish a dataset again after
  an admin removed it. The team must decide if the new design keeps this behavior.
- The parity test must compare the set of published datasets before and after the switch
  (section 9.4).
- The audit rows `INDEXED` and `DEINDEXED` record these operations today. Keep them.

Decision still open: keep the three paths, or give them names that do not say "index".

### 4.2 How ES functions map to PostgreSQL

| ES function | PostgreSQL equivalent | Parity risk |
| --- | --- | --- |
| `term`, `terms` on keyword values | `=` and `= ANY(...)` on a column or a `jsonb` path | Low |
| `term` on an analyzed field (for example `accessManagement: "controlled"`) | `lower(col) = ...` | Low. ES matches the lowercase token. SQL must do the same. |
| `match`, `match_phrase` in filters | Equality that ignores case, or `@>` on `jsonb` | Medium. ES matches a token. SQL matches the full value, unless the design uses text search. |
| `range` on numbers and dates | `>=` and `<=` as two separate comparisons on typed values. Not `BETWEEN`. | Low. duos-ui sends ranges with only a minimum or only a maximum (`filterRegistry.ts`), so each comparison is optional. Dates in assets are strings and need a safe cast. |
| `must_not` `term`, as in the approved filter (`accessManagement` is not `controlled`) | `IS DISTINCT FROM`, or `<>` with an explicit `IS NULL` case | Medium. `must_not` also matches a dataset that has no `accessManagement` value. A SQL `<>` drops that dataset, so the counts decrease. |
| Filters on asset arrays | `jsonb_path_exists` or `EXISTS` on `jsonb_array_elements` | **Medium. The results change.** ES matches the full document. Two asset filters can thus match two different assets of one study. The asset tabs check each row again in the browser, but the study and dataset tabs and their counts keep that match. A SQL `EXISTS` on one array element removes it. The team must keep the two behaviors, or product must accept the change (section 11). |
| `multi_match` `phrase_prefix` across about 70 fields | One `tsvector` column with the `simple` configuration and a `tsquery` of the form `a <-> b:*` | **High.** See the notes below. |
| `terms` aggregation | `GROUP BY` with `count(*)` | Low. ES returns 10 buckets by default. SQL needs an explicit limit. |
| `cardinality` | `count(DISTINCT ...)` | Low. SQL is exact. ES is approximate. |
| `composite` and `terms` on `study.studyId` with `top_hits` | `GROUP BY study_id`, or a query on the `study` table | Low in SQL. High if the server must emulate the ES response shape. |
| `sum`, `value_count` | `sum()`, `count()` | Low |
| Default order by `_score` | `ts_rank`, or an explicit default sort | Medium. Result order changes when there is no sort. |
| Limit of 10000 results | None | The ES limit goes away. The new service must set its own limits. See the notes below. |

Notes on limits. The search service uses the same database as all other consent traffic, so each
request must have a limit:

- A maximum page size, and a maximum offset or a keyset page.
- A maximum number of ids in a scope, and a maximum number of filters and of filter values.
- A maximum number of facets in one request, and a maximum number of buckets in one facet.
- A statement timeout on each search query.
- The callers that send `size: 10000` today (section 3.3) need a decision: a larger limit for
  those scopes, or pages.

The parity test (section 9.4) must include a range with only a minimum, a range with only a
maximum, a dataset with no `accessManagement` value, and a study where two asset filters match two
different assets.

Notes on the search box:

- ES splits text with its standard analyzer. PostgreSQL text search splits text with a different
  parser. Identifiers such as `DUOS-000123`, email addresses, and URLs give different tokens.
- The `english` configuration stems words. ES does not stem here. Use the `simple` configuration.
- The `pg_trgm` extension can add substring match for identifiers and email addresses.
- A side-by-side comparison must measure the differences (section 9.4).

## 5. The options

### 5.1 Option 1 — Purpose-built PostgreSQL endpoints

Each page gets a typed endpoint. Examples: datasets for a DAC, one dataset by identifier, library
datasets, library studies, library counts. The server builds all SQL. duos-ui stops building queries
and calls the new endpoints.

- **For:** The simplest server code for each endpoint. The smallest attack surface. Each endpoint
  has clear authorization.
- **Against:** The largest change in duos-ui. About twelve endpoints. The Data Library has many
  filters, so its endpoint becomes a generic search endpoint in practice. That result is option 3.

### 5.2 Option 2 — Translate ES queries to SQL on the server

The endpoints and duos-ui do not change. The server parses the ES query JSON, translates the closed
set of clauses in section 3.3 to SQL, and builds a response in the ES shape.

- **For:** No change in duos-ui. One repository. A fast switch with a configuration flag.
- **Against:**
  - Consent must emulate the ES response shapes, which include nested `top_hits` in aggregation
    buckets. This is the most expensive part.
  - The contract between browser and server stays the ES query language. New search functions need
    new translator work.
  - The browser continues to build the visibility rules. The server must find those clauses and
    replace them, which is the mediation work of Epic E.
  - Analyzer behavior does not translate exactly (section 4.2).
- **Note:** A translator rejects each shape that it does not know. This closes some leak routes in
  the access contract, for example `significant_terms` and `explain`.

### 5.3 Option 3 — A generic search service with a typed request (hybrid)

One new endpoint accepts a small typed request that the team owns. It is not ES query language.

```json
{
  "view": "datasets | studies | counts",
  "scope": { "library": "broad", "ownedByMe": false, "dacIds": [], "studyId": null, "datasetIds": [] },
  "search": "free text",
  "filters": [ { "key": "dataType", "values": ["WGS"] }, { "key": "participantCount", "gte": 100 } ],
  "facets": ["dac", "dataType"],
  "sort": { "key": "datasetName", "order": "asc" },
  "page": { "offset": 0, "limit": 25 }
}
```

A field registry on the server maps each key to a SQL expression, a type, and the operations that it
permits. The server builds the SQL from the registry. The server adds the visibility rules for the
`scope` of the request (section 7.4). The server reads the user id, the DAC ids, and the institution
of the user from the session.

The request selects the scope, and some scopes have weaker visibility rules than the Data Library.
Thus each scope needs its own authorization: a necessary role, and a check of each target id.
Section 7.4a defines this. Without it, a user can select a weak scope to avoid the Data Library
rule.

- **For:**
  - duos-ui already has registries (`filterRegistry.ts`, the asset definitions,
    `libraryVersions.ts`). Each entry changes from an ES clause to a key. The structure stays.
  - The request has a closed vocabulary. This is the result that Epics E-0 and E-1 specify.
  - A new filter is one registry entry on each side.
  - The four v1 callers and the dashboards use the same service through `scope`.
  - The response is plain rows and counts. No ES shapes.
- **Against:** Both repositories change. The team must design the request with care. More initial
  design than option 2.

### 5.4 Option 4 — Libraries

No library does the work of option 2. The search found no maintained library that translates ES
query JSON to PostgreSQL SQL. The libraries found translate in the other direction (SQL to ES).

| Candidate | What it is | Usable here |
| --- | --- | --- |
| PostgreSQL full-text search (`tsvector`, GIN) | Built in | Yes. Already in use for the ontology. |
| `pg_trgm`, `unaccent`, `btree_gin` | Substring match, accent removal, mixed indexes | Yes. On the Cloud SQL supported list. Not in use in consent today. |
| `pg_bigm` | Two-character index for substring match | Yes, behind a Cloud SQL flag. Probably not necessary. |
| `pg_textsearch` | BM25 rank | Only on PostgreSQL 17 or later on Cloud SQL. Not necessary for parity. |
| ParadeDB `pg_search` | BM25 search. Accepts ES JSON for aggregations only. | **No.** Not on the Cloud SQL list. It needs a separate server with logical replication, which replaces one search system with a different one. That server also needs a compliance approval. |
| ZomboDB | Index type that stores data in ES | No. It needs ES. |
| `rum`, PGroonga | Other index types | No. Not on the Cloud SQL list. |
| Hibernate Search | Search mapping for Java | No. Its backends are Lucene and ES. Not checked in this research. |
| jOOQ | Type-safe SQL builder for Java | Possible, for dynamic SQL in option 2 or 3. New dependency. JDBI with a small internal builder is the alternative. Not checked in this research. |
| SQL-to-ES translators (for example `sql2dsl`, `elasticsearch-sql`) | Wrong direction | No |

Run `SELECT * FROM pg_available_extensions` on a real instance before the team relies on an
extension.

**Result:** Option 4 does not make option 2 cheaper. Libraries help with search quality
(`pg_trgm`) and with SQL construction (jOOQ) in each option.

## 6. Comparison

Size key from the existing plan: **S** is about 1 day, **M** is 2 to 3 days, **L** is 4 to 5 days,
**XL** is 1 week or more. All sizes are draft estimates by the author. The team must check them.

| Criterion | Option 1: purpose-built | Option 2: translator | Option 3: generic service |
| --- | --- | --- | --- |
| Maintenance | Medium. Many endpoints, each simple. | **High.** The team owns an ES emulator. The browser stays tied to ES query language. | **Low.** One registry on each side. |
| Scope in consent | About 12 endpoints, storage, dashboards, removal | Parser, clause translator, aggregation emulator, storage, dashboards, removal | One service and registry, storage, dashboards, removal |
| Scope in duos-ui | **Large.** Each caller changes. | **None.** | Medium to large. The query builders and response readers change. The structure stays. |
| Draft effort | 8 to 10 person-weeks | 5 to 7 person-weeks, plus Epic E mediation later | 6 to 8 person-weeks |
| Largest risk for the date | The quantity of duos-ui work | Parity of analyzer behavior and of response shapes | Design time for the request, and duos-ui capacity |
| Security today | Server owns each query | The browser still builds the visibility rules, unless mediation is added | Server owns each query and each visibility rule |
| Fine-grained access control later | Rules repeat in each endpoint | Needs Epic E mediation on a query language that the team does not control | One location for rules. Rules join live tables. |
| External API clients that send ES queries | Break | Continue to work, for the shapes that the translator accepts | Break |
| Work that later becomes waste | Low | **High.** The translator has no use after duos-ui moves to a typed request. | Low |


## 12. Sources

- consent at `dfa588c47` and duos-ui at `84fe8af4`.
- Cloud SQL supported extensions:
  <https://docs.cloud.google.com/sql/docs/postgres/extensions>
- PostgreSQL `REFRESH MATERIALIZED VIEW`:
  <https://www.postgresql.org/docs/18/sql-refreshmaterializedview.html>
- ParadeDB documentation: <https://www.paradedb.com/docs/welcome/introduction.md>
- `sql2dsl` (wrong direction): <https://github.com/eolinker/sql2dsl>
