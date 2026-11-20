# Elasticsearch to PostgreSQL Migration — Design

Ticket: [DT-4437](https://broadworkbench.atlassian.net/browse/DT-4437)

**Status: draft (2026-11-19). No decision is made.** Section 9 gives a draft recommendation.
Section 11 lists the questions that the team must answer before the recommendation is final.

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

The publication column of storage A, C1, and C2 needs a first fill too. The source is the same as
for the storage B table: the set of published datasets after the comparison of section 9.4, not
`indexed_date` without a check. `dataset.indexed_date` can be that column, but only after the
comparison corrected it.

Notes for storage B:

- The write must occur in the same transaction as the data change. Then the document cannot differ
  from the data after a failure, and `index_confirmed_date` is no longer necessary as a
  confirmation. But `AssetLookupDAO` reads that column to decide if a study or a dataset is in the
  library (section 3.5). Those two queries must change to the new publication state before the
  column goes. The switch in section 9.2 has a step for this.
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
  | In the transaction of the data change | Search is correct at commit only if one write runs at a time. With two writes at the same time, a refresh can use a snapshot from before the other write committed, and that change then stays out of the view (see the note below). Only one refresh can run at a time, so the writes wait for each other. A refresh without `CONCURRENTLY` also keeps its lock until the write transaction commits, so readers wait for the full write. The cost of each write also increases with the number of datasets. |
  | After the commit | The stale-on-failure case of today comes back. Storage B removes that case. |
  | On a schedule | A new dataset is absent from search until the next refresh. No scheduled job exists today (section 3.4). `pg_cron` is on the Cloud SQL list, but it adds a new part to operate. |

- **A refresh in the write transaction needs an explicit lock.** A reviewer checked the
  PostgreSQL 16 source: at the default isolation level, a refresh takes its snapshot before it
  waits for the lock on the view. `CONCURRENTLY` has the same behavior. The author did not check
  the source. Two possible corrections exist: take the lock on the view with `LOCK TABLE` before
  the refresh, or run the refresh at `SERIALIZABLE` with a retry. Each makes all writes run one
  after the other.
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
  has this meaning by design (section 3.5), but its values are not all correct today, because an
  index write sets it before ES accepts the document (section 4.1).
- A design that creates rows only on an admin call leaves each new registration out of search.
- Storage A needs that state in each search query as a filter.
- Storage C puts that filter in the view definition. Storage C2 shows a change to that state only
  after a refresh.
- Storage B must keep the rule that a row exists only for a published dataset. The first fill must
  obey it (section 4.1).
- A DAC approval, a DAC rule toggle, and the conversion of DAC datasets to external each force a
  write today, without a check that the dataset is published. Thus they publish a dataset again
  after an admin removed it. The team must decide if the new design keeps this behavior.
- The parity test must compare the set of published datasets before and after the switch
  (section 9.4).
- Only a removal writes an audit row today (`DEINDEXED`, through `deleteIndex`). The `INDEXED`
  action exists, but no publication path writes it: publication calls
  `DatasetDAO.updateDatasetsIndexedDate`, which writes no audit row. Thus the new design must add
  the audit of each publication, and the parity test cannot use `INDEXED` rows as a source.

Decision still open: keep the three paths, or give them names that do not say "index".

### 4.2 How ES functions map to PostgreSQL

| ES function | PostgreSQL equivalent | Parity risk |
| --- | --- | --- |
| `term`, `terms` on a keyword field with one value | `=` and `= ANY(...)` on a column or a `jsonb` path | Low |
| `term`, `terms` on a keyword field with a list of values (`study.data.tags`, `study.dataTypes`, `study.dataCustodianEmail`, dataset `cloud`) | `?` and `?|` on a `jsonb` array, `&&` on a typed array, or `EXISTS` on the elements. Not `=`. | **Medium.** ES matches if one element matches. `=` on the full value does not. The branded library scopes send `terms` on the study tags, so a study with two tags leaves its library if the map uses `=`. The field registry must record, for each field, if it holds a list. |
| `exists` | `IS NOT NULL`, or `?` for a `jsonb` key | Low. Each Data Library query sends `exists: study`, so a dataset without a study is not in the Data Library. |
| `bool` `should` with `minimum_should_match` | `OR`, in a group with parentheses | Low |
| Match on `_id` | `dataset_id = ANY(...)` | Low |
| `term` on an analyzed field (for example `accessManagement: "controlled"`) | `lower(col) = ...` | Low. ES matches the lowercase token. SQL must do the same. |
| `match`, `match_phrase` in filters | Equality that ignores case, or `@>` on `jsonb` | Medium. ES matches a token. SQL matches the full value, unless the design uses text search. |
| `range` on numbers and dates | `>=` and `<=` as two separate comparisons on typed values. Not `BETWEEN`. | Low. duos-ui sends ranges with only a minimum or only a maximum (`filterRegistry.ts`), so each comparison is optional. Dates in assets are strings and need a safe cast. |
| `must_not` `term`, as in the approved filter (`accessManagement` is not `controlled`) | `IS DISTINCT FROM`, or `<>` with an explicit `IS NULL` case | Medium. `must_not` also matches a dataset that has no `accessManagement` value. A SQL `<>` drops that dataset, so the counts decrease. |
| Filters on asset arrays | `jsonb_path_exists` or `EXISTS` on `jsonb_array_elements` | **Medium. The results change.** ES matches the full document. Two asset filters can thus match two different assets of one study. The asset tabs check each row again in the browser, but the study and dataset tabs and their counts keep that match. A SQL `EXISTS` on one array element removes it. The team must keep the two behaviors, or product must accept the change (section 11). |
| `multi_match` `phrase_prefix` across about 70 fields | One `tsvector` column with the `simple` configuration and a `tsquery` of the form `a <-> b:*` | **High.** See the notes below. |
| `terms` aggregation on a field with one value | `GROUP BY` with `count(*)` | Low. ES returns 10 buckets by default. SQL needs an explicit limit. |
| `terms` aggregation on a field with a list of values (`study.dataTypes`, `dataUse.primary.code`, `dataUse.secondary.code`) | Expand the elements with `unnest` or `jsonb_array_elements_text`, then `count(DISTINCT dataset_id)` for each element | **Medium.** ES puts each element in its own bucket and counts a document one time for each bucket. `GROUP BY` on the full list makes one bucket for each list, so `["WGS", "RNA"]` is in neither the WGS count nor the RNA count. |
| `filter` aggregation | `count(*) FILTER (WHERE ...)` | Low |
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

The design of the search box is not complete. These points are open:

- **How the fields go into the `tsvector`.** If the builder joins about 70 fields into one text, the
  operator `<->` can match the last word of one field and the first word of the next. ES
  `phrase_prefix` matches in one field only. A gap between the fields in the `tsvector` positions
  can prevent this. The design must say how.
- **How the user text becomes a `tsquery`.** `to_tsquery` gives an error on text such as `a & (`.
  The server must build the query from clean tokens, and it must not send the user text directly.
- **The existing sanitizer is not sufficient.** `OntologyDAO.sanitizeForTsQuery` removes each
  character that is not a letter, a digit, or a space. With it, `DUOS-000123` does not match as one
  identifier. The dataset search needs its own rule for identifiers, email addresses, and URLs.
- **`unaccent` in an index.** `unaccent()` is not `IMMUTABLE`, so a generated column or an index
  expression cannot call it directly. The generated column of the `ontology_index` precedent
  cannot get it in that way. It needs
  an immutable wrapper function, or a text search configuration that uses the `unaccent`
  dictionary.

Add to the parity test (section 9.4): a study with two tags in a branded library, a facet on a
field with a list of values, a phrase that crosses two fields, and a search for text with `tsquery`
operator characters.

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
| Draft effort | 8 to 10 person-weeks | 5 to 7 person-weeks. This number does not include the Epic E mediation that option 2 needs for server-owned visibility, so it is not equal in scope to the other two numbers. | 6 to 8 person-weeks |
| Goals and anti-goals (section 1) | Satisfies each one. | Fails two anti-goals without more work: consent becomes an ES emulator, and the visibility rules stay in the browser. | Satisfies each one. |
| Largest risk for the date | The quantity of duos-ui work | Parity of analyzer behavior and of response shapes | Design time for the request, and duos-ui capacity |
| Security today | Server owns each query | The browser still builds the visibility rules, unless mediation is added | Server owns each query and each visibility rule |
| Fine-grained access control later | Rules repeat in each endpoint | Needs Epic E mediation on a query language that the team does not control | One location for rules. Rules join live tables. |
| External API clients that send ES queries | Break | Continue to work, for the shapes that the translator accepts | Break |
| Work that later becomes waste | Low | **High.** The translator has no use after duos-ui moves to a typed request. | Low |

## 7. Security

### 7.1 Today

- The server sends client queries to ES with one shared credential.
- The server adds no filter. The browser builds each visibility rule.
- Any user who signs in can read each document in the index. The documents hold personal data
  (section 3.1a).
- The two search endpoints accept any ES query. Thus the queries that duos-ui sends are not the
  limit of what a caller can read today (section 3.3).
- The browser does not add each visibility rule to each request. The table in section 3.3 shows
  the rules of each caller. That table is the baseline for "no more data than today".
- The access control epics exist to correct this.

### 7.2 What changes when ES goes

| Item in the existing plans | After the move |
| --- | --- |
| Epic D (ES document-level and field-level security) | Not applicable. The license dependency goes. |
| Question OPEN-13 (does a different client read the data with the service credential?) | The question moves to PostgreSQL. It does not go away. The DUOS BFF already signs in with the `consent` database role. Its code reads only `user_sessions` today. But the `consent` role creates the search table and thus owns it, so the BFF connection has full rights on that table. Row-level security does not make a limit here, because the owner of a table is exempt by default. Give the BFF its own database role with grants for the session data only, and check those grants before the search table goes to production. Row-level security is then the equivalent of Epic D for a different reader of the search data. |
| Epic E-0 (server-owned aggregations) | Options 1 and 3 give this by design. Option 2 gives it only for the shapes that the translator accepts. |
| Epic E-1 (query mediation) | Options 1 and 3 give this by design. No client query language exists. Option 2 must still build it. |
| Epic E-2 (injected visibility filter) | A SQL `WHERE` clause that the server adds. It can join live tables. |
| Epic E-3 (response field allowlist) | The `SELECT` list, or a projection of the stored document. This protects the returned rows only. A facet key, a sort key, and a text match can also show the value of a protected field. Thus the field registry must say, for each field, which operations can use it: return, filter, facet, sort, and text search. The leak tests of the access contract (`ElasticSearchLeakDefensePocTest`) must move to the new API as negative tests. |
| Epic B (access metadata in each document) | Smaller. Access rules can join live tables such as DAC membership and institution. A rule change has effect immediately, without an index write. |
| Epic C (auth context service) | No change. Still necessary. |
| ES capability service, probes, and tests | Removed. |
| Access contract (`es-access-contract.md`) | Stays valid. Its dimension matrix and field classification do not depend on the mechanism. |

### 7.3 Fine-grained access control

- A PostgreSQL search can enforce a rule with a join to the live tables. The rule can use DAC
  membership, institution, an explicit list of users, or policy tags.
- The access contract forecasts rules of this type (its section G.4). In SQL each rule is one more
  predicate. There is no credential to create for each request.
- PostgreSQL row-level security is a possible second layer later. Consent uses one database role
  today, so row-level security needs a session variable for each request. This design does not
  include it.
- Hidden data must not change counts or facets. In SQL, the visibility predicate applies before each
  aggregate, so this condition holds by construction.

### 7.4 Rule for this migration

Decision 3 of the access contract says that a change must keep the authorization of today, unless
the team approves an expansion. The migration must obey this rule.

Options 1 and 3 move the visibility rules from the browser to the server. A user who changes the
request by hand can then no longer see hidden studies. This makes access tighter. The team must
approve it as a decision.

**Product approval is necessary for this change.** The source for the rules is the access contract
(`es-access-contract.md`, section A), which the Jira epics [DT-3816](https://broadworkbench.atlassian.net/browse/DT-3816) to [DT-3823](https://broadworkbench.atlassian.net/browse/DT-3823) use. Section 7.5
compares the contract with the table below. The two do not agree. Until product approves, the
server rule sets must be the rules in the table below, which are the rules of today.

The rules are not the same for each caller today. One rule for all callers hides data that some
pages show now. The server must keep one rule set for each scope:

| Caller | Public visibility rule | Approved rule | Other scope |
| --- | --- | --- | --- |
| Data Library | Yes, unless the user is Chairperson, Data Submitter, Admin, or Signing Official | Yes | Branded library, if one is selected |
| Submissions page | Same as the Data Library | No | Ownership by the user |
| Study page | No | No | One study |
| Dataset page | No | No | One dataset |
| DAC Datasets, Manage DAC, DAC Profile | No | No | DAC ids |
| DAR review and DUC addendum | No | No | Dataset ids |
| Researcher dashboard | Yes, with the same role exception | Yes | Ownership for the submission count |
| DAC dashboard | Public, or in a DAC of the user | Yes | None |
| Signing Official dashboard | No | No | Institution of the user |

A proposal to make these rules tighter or the same is a separate decision. The open items of the
access contract cover such changes.

**The approved rule applies to some results only.** The registry must give the predicate for each
result and each count:

| Result or count | Approved rule today |
| --- | --- |
| Dataset rows on the Datasets tab | Yes |
| Dataset count (`datasets_count`) in the Data Library and on the Researcher and DAC dashboards | Yes |
| Study rows, study count, and asset rows and counts | No. A public study that has only one controlled dataset without approval is still in these results. |
| Chairperson count of datasets in the DACs of the user | No. This count also has no public visibility rule. |
| Submission count and the Submissions page | No |
| Study page grid | No |

### 7.4a Authorization of each scope

The server must authorize the scope before it applies the rules of the scope. An id from the session
tells the server who the user is. It does not tell the server that the user can use a scope or read
a target.

The column "Today" shows that the two ES endpoints make none of these checks. The column "Draft
rule" comes from the role limits of the duos-ui pages. It is a proposal, and product must confirm it
with the visibility rule (question 6).

| Scope | Today | Draft rule: who can use it | Draft rule: permitted targets |
| --- | --- | --- | --- |
| Library, with or without a branded library | Each user who signs in | Each user who signs in | No target. The branded library key must be in the server list. The "my institution" library uses the institution from the session. |
| Owned by the user | Each user who signs in can send an ownership clause for a different user | Data Submitter, Chairperson, Admin | The user id and email come from the session only. |
| DAC | Each user who signs in can send each DAC id | Chairperson and Member for their DACs. Admin for each DAC. | Each DAC id in the request must be a DAC of the user, unless the user is an Admin. |
| One study | Each user who signs in can read each study | Open. Product must decide: each user who signs in (as today), or the contract rule (`canReadStudy`). | The study id. If the contract rule applies, the server tests it for that study. |
| One dataset, or a list of dataset ids | Each user who signs in can read each dataset | Open, with the same two choices. For the DAR pages, a possible rule is that the datasets must be in a DAR that the user can read. | Each dataset id. |
| Institution (Signing Official dashboard) | Server-built. The institution comes from the user. | Signing Official | The institution from the session only. |

Rules for each scope:

- A request for a scope that the user cannot use gets a 403 response, not an empty result.
- A request with a target id that the user cannot read gets a 403 or a 404 response. The team must
  select one and use it for each scope (question 13 in section 11). The existing check
  `DatasetService.verifyStudyVisibilityAccess` returns 404, so that a caller cannot learn that
  the study exists.
- If the study scope and the dataset scope stay open to each user who signs in, the public
  visibility rule controls discovery in the library only. It does not protect a study from a user
  who has the id. This is the behavior of today. The document must say so to product.

### 7.5 The source of the rules, and a conflict

**Sources**

- The Jira epics [DT-3816](https://broadworkbench.atlassian.net/browse/DT-3816) to
  [DT-3823](https://broadworkbench.atlassian.net/browse/DT-3823) are "Elasticsearch Document
  Visibility", Epics A to H. They are the Jira form of the plans in this directory.
- Ticket A-2 ([DT-3826](https://broadworkbench.atlassian.net/browse/DT-3826), Completed) is the
  access contract. Its section A is the statement of the rules.

**The rule in the access contract**

The contract takes its rule from `DatasetService` (`readBasis`, `canReadStudy`,
`isCreatorOrCustodian`). A user can read a dataset if one of these conditions is true:

1. The user is an Admin.
2. The study has `publicVisibility = true`.
3. The dataset has no study.
4. The user created the dataset.
5. The user created the study.
6. The email of the user is in the data custodian list of the study. The match is exact and is
   case-sensitive.

The contract gives no access for these items. Each is deferred until product approves it:

- DAC member or DAC chair (OPEN-3).
- Institution, policy tags, and explicit user lists (OPEN-5).
- `dacApproval`. The contract calls it a display attribute, not authorization (OPEN-4).

**Three rule sets exist today**

| Rule set | Where | Who sees a study that is not public |
| --- | --- | --- |
| Access contract | `DatasetService`, for the dataset endpoints | Admin, dataset creator, study creator, custodian |
| Role rule | duos-ui `isRestrictedToPublicVisibility`, and `DashboardSearchService` on the server | Chairperson, Data Submitter, Admin, Signing Official |
| None | Study page, Dataset page, DAC pages, DAR review (ES queries) | Each user who signs in |
| Role and institution | Signing Official dashboard | The endpoint permits only the Signing Official role. It returns counts only, for the institution of the user. |

**The conflict**

The table in section 7.4 records the role rule, because that is what the search pages do today.
The contract records the `DatasetService` rule. They give different results:

| User | Contract rule | Role rule in the Data Library |
| --- | --- | --- |
| Chairperson, Data Submitter, or Signing Official who is not the creator or a custodian | Does not see a study that is not public | Sees it |
| Researcher who is the creator or a custodian of a study that is not public | Sees it | Does not see it |
| Admin | Sees all | Sees all |

The contract and the epics do not mention the role rule. Ticket G-1 says that the browser filter
goes away when the server filter implements the contract rows. That change removes studies that are
not public from the library view of each Chairperson, Data Submitter, and Signing Official. The
contract does not record this as a change.

Thus "keep the authorization of today" has two possible meanings for search. Product must select one
(question 6). Until then, the server rule sets are the table in section 7.4.

**How the epics map to this migration**

| Epic | Jira | Effect of this migration |
| --- | --- | --- |
| A. Discovery and contract | [DT-3816](https://broadworkbench.atlassian.net/browse/DT-3816) | Done, except A-3 ([DT-3827](https://broadworkbench.atlassian.net/browse/DT-3827)). The contract stays valid. The ES capability record has no more use. |
| B. Index schema | [DT-3817](https://broadworkbench.atlassian.net/browse/DT-3817) | Mostly not necessary. Access rules join live tables, so no `accessPolicy` object and no index version migration. A custodian change has effect immediately. |
| C. Auth context service | [DT-3818](https://broadworkbench.atlassian.net/browse/DT-3818) | Still necessary. Tickets C-1 and C-2 give the caller context and the shared predicates that the SQL filter uses. |
| D. Native ES security | [DT-3819](https://broadworkbench.atlassian.net/browse/DT-3819) | Not applicable. |
| E. Query mediation | [DT-3820](https://broadworkbench.atlassian.net/browse/DT-3820) | E-1 and E-4 are not necessary with option 3, because no client query language exists. E-2 becomes a SQL `WHERE` clause. E-3 becomes the `SELECT` list. |
| F. API hardening | [DT-3821](https://broadworkbench.atlassian.net/browse/DT-3821) | F-2 and F-3 ([DT-3848](https://broadworkbench.atlassian.net/browse/DT-3848), [DT-3849](https://broadworkbench.atlassian.net/browse/DT-3849)) are the server-owned search API. They are "deferrable" in Jira. Option 3 is that API. This answers OPEN-11 with "yes". |
| G. Frontend alignment | [DT-3822](https://broadworkbench.atlassian.net/browse/DT-3822) | Phase 5 of this design does G-1 to G-4. |
| H. Rollout and docs | [DT-3823](https://broadworkbench.atlassian.net/browse/DT-3823) | The feature flag, the comparison of the two systems, and the switch in section 9 do the work of H-1 and H-3. H-2 (audit log) stays separate. |

Option 3 delivers a large part of Epics E, F, and G as a side effect. That work is already in the
backlog. This reduces the true additional cost of the migration.

## 8. What the team must remove or change

This list applies to each option.

**consent**

- `ElasticSearchService`: the client calls go. The document builder stays under storage B. Under
  storage A or storage C, SQL replaces it.
- `DashboardSearchService`: the three dashboard queries become SQL.
- `ElasticSearchCapabilityService` (1989 lines), its resource, its models, and its OpenAPI paths.
- `ElasticSearchHealthCheck`, and the `elastic-search` entry in `StatusResource`.
- `ElasticSearchSupport`, `ElasticSearchConfiguration`, and the Guice providers in `ConsentModule`.
- The `elasticSearch` configuration block, which includes the unused `indexName` key.
- The `elasticsearch-rest-client` dependency and the Testcontainers ES dependency.
- The Apache HttpComponents 4 pin, but only after a check of the dependency tree. `OidcService`
  imports `org.apache.http.client.utils.URLEncodedUtils`, and the comment on the pin in `pom.xml`
  also names `java-http-client`. The pin can thus be necessary after ES goes.
- About 30 test files. Seven Testcontainers classes go. The `ci` profile exclusion goes.
- `scripts/index-es.sh`, the `elastic` service in the compose template, and `ELASTIC_PASSWORD` in
  `render-configs.sh`.
- The other dependencies in section 3.7: `scripts/seed-showcase-fixture.sh`, the
  `-Ddw.elasticSearch.*` flags, the license endpoints, and the static method
  `buildMapFromPropertyValue`, which must move before `ElasticSearchService` goes.
- `DEVNOTES.md`, `docs/ONBOARDING.md`, and `docs/README.md`.
- `dataset.index_confirmed_date`, if the write joins the transaction. `AssetLookupDAO` and
  `AssetLookupService` read this column. They must change to the PostgreSQL publication state
  before the ES writes stop (step 7 of section 9.2), not at this cleanup. Only an ES write sets the
  column, so after step 7 a new publication does not set it.
- The index endpoints (`/api/dataset/index...`): see section 4.1a.

**duos-ui**

- `searchDatasetIndex` and `searchDatasetIndexV2` in `DataSet.ts`.
- `src/types/elastic.ts`.
- The query builders: `filterRegistry.ts`, the ten asset definitions, `libraryVersions.ts`,
  `useLibraryData.ts`, `useLibraryTabCounts.ts`, and `useStudyDetailsData.ts`. Option 2 leaves them.
- The four v1 callers.
- About 25 test files with ES queries or mock ES responses.
- The `elastic-search` key in the status types.

**Infrastructure**

- The `elasticSearch` values and secrets in terra-helmfile.
- The ES clusters for dev, staging, and production.

**Documents**

- Mark the three ES plans as superseded where this design replaces them.
- `study-asset-records-plan.md` proposes an ES mapping. That part is no longer necessary.

## 9. Draft recommendation

**Option 3 with storage B, delivered in phases.** Use `pg_trgm` from option 4 if the parity test
shows a need.

Reasons:

1. Option 2 costs almost the same as option 3 and leaves the team with an emulator and with the
   browser in control of visibility.
   Option 3 is also the server-owned search API that tickets F-2 and F-3 already plan (section 7.5).
2. Option 1 becomes option 3 as soon as the Data Library endpoint exists.
3. Option 3 gives the results of Epics E-0, E-1, and E-2 as a side effect.
4. Storage B keeps the documents identical, so a test can compare the two systems. Storage A and
   storage C change the field logic at the same time as the search engine (section 4.1).

### 9.1 Phases

| Phase | Work | Draft size | Repository |
| --- | --- | --- | --- |
| 1 | Search document table, write in the transaction, first fill | L | consent |
| 2 | Dashboard counts in SQL, with a server configuration switch between ES and PostgreSQL | M | consent |
| 3 | Generic search service: registry, `datasets` view, scopes for the four v1 callers | XL | consent |
| 4 | `studies` and `counts` views, asset filters, facets | XL | consent |
| 5 | duos-ui: v1 callers, then the Data Library builders and readers | XL to 2 XL | duos-ui |
| 6 | Parity test with generated data, the set of query pairs and their replay in the deployed environment, publication check, and the build of the ES index from the published set for rollback | L to XL | consent |
| 7 | Switch, observe, stop ES use, measure zero traffic, stop the clusters. Then remove the ES code and configuration. | L | all |

Phase 1 also moves the document builder reads and the row write into the transaction of each
data change (section 4.1). The size of phase 1 does not include that work yet, and it can be
larger than L.

Phases 1 and 2 have no planned effect on users and can start immediately. One effect is possible:
a failure of the row write in the transaction (see the risk in section 10). Phases 3 and 5 can run in parallel
after the team agrees on the request.

### 9.1a Does this fit the date?

Only one milestone must occur before the renewal: **ES has no reads and no writes, and the team
measured this.** That is phases 1 to 6 and steps 1 to 8 of the switch. The cluster can stop at
step 9. The removal of code, tests, documents, and configuration in step 10 can follow after the
date.

- The target for the milestone is 20 January 2027, one sprint before 1 February.
- The milestone is about 5 to 7 person-weeks by the draft sizes.
- About three work sprints remain, which is about 6 weeks.
- One person cannot do it by the target.
- Four people on the team can do duos-ui work, and all four can work on this migration.
- Four people for three work sprints is about 24 person-weeks. The milestone needs 5 to 7. Thus the
  quantity of work fits. But this comparison is not sufficient, because more people cannot make
  some steps shorter. See the calendar below.
- A possible division: two people in consent (phases 1 to 4 and 6) and two people in duos-ui
  (phase 5).
- The team must agree on the request (section 5.3) in the first sprint. This agreement is the item
  that lets the two repositories work in parallel.
- Product approval of the visibility rules is the other item on the critical path. Ask for it in the
  first sprint.

**Calendar.** These items occur in sequence, and each has a time that people cannot decrease:

| Item | Why it cannot run in parallel | Time |
| --- | --- | --- |
| Agreement on the request, and product approval of the visibility rules | Phases 3 to 5 need them | Not known. The plan asks for both in the sprint that ends 9 December. |
| Phase 3, then phase 4 | Phase 4 extends the service of phase 3 | About 2 to 3 weeks with two people |
| Phase 6 | The replay needs the complete service of phases 3 and 4 | About 1 to 2 weeks |
| Step 6 of the switch: observe one release | A fixed wait | One release cycle. The team must give its length. |
| Step 8 of the switch: measure zero ES traffic | A fixed wait | The agreed period. The team must give its length. |
| An external caller, if the access log finds one | The date of its change is not in the control of the team | Not known (section 9.5) |
| The holidays | Fewer work days in the sprint that ends 6 January | About one sprint |

A possible sequence, if each item starts on time:

| Sprint end | Work that must be complete |
| --- | --- |
| 9 December | Request agreed. Product approval. Phases 1 and 2. Phase 3 started. |
| 23 December | Phases 3 and 4. duos-ui work of phase 5 in progress. |
| 6 January | Phase 5. Phase 6. Steps 1 to 5 of the switch. |
| 20 January | Step 6 (observe one release), step 7, and step 8 (zero traffic). |

Conclusions:

- The last sprint must hold two waits: one release cycle and the zero-traffic period. If the sum
  of the two is more than two weeks, the milestone does not fit the target of 20 January.
- No sprint has spare time. A late approval, or transaction work in phase 1 that is larger than
  its size (section 9.1), moves the milestone after the target.
- Thus the margin is small, and the author cannot say that the fallback in section 9.3 is
  improbable. Ask about a short renewal now (question 7 in section 11).

These numbers are estimates. The team has not checked the sizes or the two waits.

### 9.2 Switch

1. Deploy the new table. Write to ES and to PostgreSQL.
2. Compare the three sets of published dataset ids and correct each difference (section 9.4). This
   is a gate. Do not continue with a difference that has no decision.
3. Fill the table from the agreed set. The fill reads the publication state again for each
   dataset, with the lock of a live write, so it cannot undo a removal that occurs during the fill
   (section 4.1). Then compare the table with the published set (second gate in section 9.4). Do
   not continue with a difference.
4. Release the new endpoint. The old endpoints stay. Set the dashboard switch to PostgreSQL in
   this step, one environment at a time. Start the replay of the query pairs (section 9.4).
5. Change duos-ui to the new endpoint behind a feature flag.
6. Observe one release. Compare results and database load. In this period:
   - Change `AssetLookupDAO` to the PostgreSQL publication state. Test a publication and a removal.
   - Measure the HTTP traffic on the two old search endpoints, by caller. ES traffic is not a
     sufficient measure, because step 7 stops it for all clients at the same time.
   - Do the second gate of section 9.4 again.
   - Build and test the tool that builds the ES index from the published set. The rollback after
     step 7 needs it, and `POST /api/dataset/index` cannot do this work (section 3.4).
7. Stop all ES use with configuration, not with a code removal. Do this step only when the old
   search endpoints get no traffic, or when each caller that remains has a decision (section 9.5):
   - Stop the ES writes.
   - Make the two old search endpoints return an error, because they read ES.
   - Stop the ES health check, or `/status` reports a degraded service after the cluster stops.
   - Disable the admin endpoints `/api/elasticSearch/capabilities` and `/api/elasticSearch/license`.
     They call ES. One admin call makes the measurement in step 8 wrong, and after step 9 they call
     a cluster that does not exist.
   - Confirm that the dashboards read PostgreSQL.
8. Measure zero ES traffic from consent in each environment for an agreed period.
9. Stop the ES clusters. **Steps 1 to 9 must be complete before the renewal.**
10. Remove the old endpoints, the ES code, the tests, and the configuration. This step can follow
    after the renewal.

Rollback:

- Until step 7: set the duos-ui flag off, and set the dashboard switch to ES. The duos-ui flag
  alone is not sufficient: the server computes the dashboard counts, so the flag cannot correct a
  wrong count or a high database load from those queries.
- From step 7 to step 9: the cluster still exists, but its documents are old. Do these steps in
  order:
  1. Start the ES writes again.
  2. Build the index from the published set in PostgreSQL. Do not use `POST /api/dataset/index`
     without a change: it writes all datasets, and it publishes datasets that an admin removed
     (section 4.1).
  3. Delete each ES document that is not in the published set.
  4. Compare the ids in ES with the published set. Open the old search endpoints only when the
     two sets agree.
- After step 9: no rollback to ES.

### 9.3 If duos-ui capacity is not available before the date

A narrow form of option 2 is the fallback. Translate only the shapes in section 3.3 and plan to
remove the translator later. The team must accept the waste in section 6.

### 9.4 Parity test and publication check

**Test data**

The repository rule is "Use synthetic test data only" (`AGENTS.md`). A database export can contain
user sessions and private submission data. Thus the parity test does not use a local export of
production data.

- **Tests in the repository** use a generated data set. The data set must contain:
  - Publication differences: a dataset with `indexed_date` and no document, and the reverse.
  - Each combination of roles, and users who are creators and custodians without a role.
  - Each asset type and each asset field that a filter uses.
  - Studies that are not public, datasets without a study, and controlled datasets without approval.
  - Identifiers, email addresses, and URLs in the text fields.
  - The quantity of production: about 10,000 datasets.
- **Real data** is compared in the deployed environment, without an export. The method is a
  replay of query pairs:
  - The old endpoints get raw ES JSON, and the new endpoint gets a typed request. No translator
    between the two exists, and the plan does not build one. Thus the server cannot send each
    live search to the two systems.
  - Phase 6 writes a fixed set of query pairs. Each pair is one raw ES query from section 3.3 and
    its typed equivalent. The set includes each caller, each filter, each facet, and each scope.
  - An admin operation sends each pair to the two systems, as a caller with each role, and
    records the differences in ids and counts, without the document content.
  - The replay starts at step 4 of the switch, when the PostgreSQL search exists. It runs again
    in step 6. It cannot include steps 1 to 3.
  - This is a smaller method than the shadow mode of ticket H-3
    ([DT-3856](https://broadworkbench.atlassian.net/browse/DT-3856)). It finds differences only
    for the pairs in the set, not for each query that a user sends.

**Publication check (gate for step 2 of the switch)**

In each environment, compare three sets of dataset ids:

1. The ids in the ES index.
2. The rows with `indexed_date`.
3. The rows with `index_confirmed_date`.

| Difference | Probable cause | Action before the fill |
| --- | --- | --- |
| `indexed_date` is set, no ES document, no confirmed date | ES rejected the write | Decide for each dataset: publish it (send it again and confirm) or clear `indexed_date`. |
| ES document, no `indexed_date` | A delete from ES did not complete, or an old document | Decide for each dataset: remove the document or set the date. |
| `indexed_date` is set, ES document, no confirmed date | The write occurred before `index_confirmed_date` existed, or the confirmation did not complete | Send the dataset again so that ES confirms it. |

The author did not check if datasets that were indexed before the `index_confirmed_date` changeset
have that column set. The comparison shows this.

The fill uses the agreed set only after the three sets agree, or after each difference has a
decision.

**Search table check (second gate, for steps 3 and 6 of the switch)**

The first gate compares three sets that exist before the fill. It cannot find a row that the fill
or a live write omitted or added. Thus a second comparison is necessary:

- Compare the dataset ids in the search table with the published set in PostgreSQL
  (`indexed_date`, after the first gate corrected it).
- A missing row and an extra row are each a defect. Correct the cause, not only the row.
- Do the comparison after the fill, and again before PostgreSQL reads start for users.
- Keep the comparison as an admin check that can run at any time after the switch.

**Negative tests**

- For each protected field of the access contract, test that a caller without access cannot get
  its value through a returned row, a facet, a sort, or a text match (section 7.2).
- Test that a removed dataset is absent from results and from counts at commit.

**Query comparison**

- Send the queries of section 3.3 to both systems.
- Compare document sets, counts, and facet values. Record each difference.
- Decide for each difference: correct it, or accept it and document it. The team accepts small
  differences in search box results if they are documented. Differences in filters, counts, and
  facets are not in that group and need a correction or a separate decision.

### 9.5 External API clients

The two search endpoints accept raw ES queries from each user who signs in. The team knows of no
caller other than duos-ui. External API users exist, and one external research partner is a
possible caller.

1. Read the access logs for `POST /api/dataset/search/index` and `/index/v2`. Find the requests that
   do not come from duos-ui. Do this in the first sprint, because the result can change the plan.
2. Ask the known external API users if they call these endpoints.
3. If no external caller exists: mark the two endpoints as deprecated in the OpenAPI document, then
   stop them at step 7 of the switch.
4. If an external caller exists:
   - Give the caller the new typed endpoint and a date for the removal.
   - Do not keep ES for one client.
   - A narrow translator for the query shapes of that client is the last choice. It brings back
     the maintenance cost of option 2.

An external caller gets the server-owned visibility rules too. A client that reads studies that are
not public through a raw query today can lose those results.

## 10. Risks

| Risk | Effect | Mitigation |
| --- | --- | --- |
| The date | A late finish causes a renewal for one more year | Start phases 1 and 2 now. Ask if a short renewal is possible. |
| Search box parity | Users find different results | The team accepts small differences that are documented. The parity test finds them. `pg_trgm` for identifiers. |
| Database load | Tab counts run on each filter change. The database connection limit is fixed. | The data is small (about 6,000 to 10,000 rows), so each query is short. Measure in the deployed environment and with generated data of the same quantity. Combine the counts and the page into one request. |
| Stale documents | Same as today under storage B | Accept for now. Move fields to live joins or to a standard view later (section 4.1). |
| Result order | Order without a sort changes | Define a default sort. |
| A failed search write stops the change of the user | Today an index failure causes a log entry, and the registration, the update, or the DAC rule toggle of the user still succeeds. With the row write in the transaction, an exception in the document builder or in the write rolls back that change. | The team must select one behavior (question 14 in section 11): fail the change, or catch the exception, write a log entry, and repair the row later. The second behavior brings back a stale row, so it needs the admin check of section 9.4. |
| Two repositories | Releases must agree | New endpoint beside the old ones. Feature flag in duos-ui. |
| External API clients | The team knows of no external caller of the search endpoints. But external API users exist, and one of them is a possible caller. Options 1 and 3 break a client that sends ES queries. | See section 9.5. |

## 11. Open questions

1. ~~What is the exact renewal date?~~ **Answered:** not known. February is the only guide. This
   plan uses 1 February 2027. A person who knows the contract can still confirm the date.
2. ~~What is the yearly ES cost?~~ **Answered:** the team knows the fee and expects an
   increase. This document does not record the amount. See section 1.3.
3. ~~How many datasets are in production?~~ **Answered:** about 6,000 in dev and in production, and
   more than 10,000 in the next two quarters. Open part: the number of studies.
4. ~~Can duos-ui changes occur in this period?~~ **Answered:** yes. Four people on the team can do
   duos-ui work, and all four can work on this migration.
5. ~~Does a client other than duos-ui call the two search endpoints?~~ **Answered:** the team knows
   of none. External API users exist, and one of them is a possible caller. Open part: the access log
   check and the answers from those users (section 9.5).
6. ~~Who approves server-owned visibility rules?~~ **Answered:** product must approve. Open parts
   for product (section 7.5):
   - Which rule applies to search for a study that is not public: the contract rule (Admin, creator,
     custodian) or the role rule (Chairperson, Data Submitter, Admin, Signing Official)?
   - Does a creator or custodian without one of those roles see the study in the Data Library?
   - Do the pages that have no rule today (Study page, Dataset page, DAC pages, DAR review, Signing
     Official dashboard) get the same rule as the Data Library?
   - Is the approved rule (`dacApproval`) a display filter only, as OPEN-4 proposes?
7. Is a short ES renewal, of months and not a year, possible as a fallback?
8. ~~Are small differences in search box results acceptable if the team documents them?~~
   **Answered:** yes.
9. ~~Do the admin index endpoints keep their paths?~~ **Answered:** admins use the single-dataset
   endpoints to publish a dataset and to remove it from the library, so those two operations stay in
   each storage type. See section 4.1a. Open part: the names of the paths.
10. Is a delay between a dataset change and its effect in search acceptable? If the answer is yes,
    how long? Storage C2 with a scheduled refresh is possible only with a yes (section 4.1).
11. How long does a search box query take on a standard view with 10,000 datasets? Storage C1 is
    possible only if this time is acceptable (section 4.1).
12. For product: two asset filters can match two different assets of one study today, and the
    study and dataset tabs then show that study. Must the new search keep this, or must the two
    filters match the same asset (section 4.2)?
13. Does a request for a target that the user cannot read get a 403 or a 404 response
    (section 7.4a)? An existing study check returns 404.
14. If the search row write fails in the transaction, does the change of the user fail, or does
    the server write a log entry and repair the row later (section 10)?

Questions 5, 7, 10, and 11 have no owner. Question 5 can change the plan and must have an answer in
the first sprint (section 9.5). The team must name one owner for each of these four questions.

## 12. Sources

- consent at `dfa588c47` and duos-ui at `84fe8af4`.
- Cloud SQL supported extensions:
  <https://docs.cloud.google.com/sql/docs/postgres/extensions>
- PostgreSQL `REFRESH MATERIALIZED VIEW`:
  <https://www.postgresql.org/docs/18/sql-refreshmaterializedview.html>
- ParadeDB documentation: <https://www.paradedb.com/docs/welcome/introduction.md>
- `sql2dsl` (wrong direction): <https://github.com/eolinker/sql2dsl>
