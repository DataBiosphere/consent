# Elasticsearch to PostgreSQL Migration — Design

Ticket: [DT-4437](https://broadworkbench.atlassian.net/browse/DT-4437)

**Status: draft (2026-11-19). No decision is made.** Section 9 gives a draft recommendation.
Section 11 lists the questions that the team must answer before the recommendation is final.

**This part of the document has sections 1 to 3 and section 12.** Sections 4 to 11 are in the
pull requests that follow this one. References to those sections point to them.

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

## 12. Sources

- consent at `dfa588c47` and duos-ui at `84fe8af4`.
- Cloud SQL supported extensions:
  <https://docs.cloud.google.com/sql/docs/postgres/extensions>
- PostgreSQL `REFRESH MATERIALIZED VIEW`:
  <https://www.postgresql.org/docs/18/sql-refreshmaterializedview.html>
- ParadeDB documentation: <https://www.paradedb.com/docs/welcome/introduction.md>
- `sql2dsl` (wrong direction): <https://github.com/eolinker/sql2dsl>
