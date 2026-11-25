# Typed Search Endpoint — Contract

Ticket: [DT-3848](https://broadworkbench.atlassian.net/browse/DT-3848) (backlog D2, epic
[DT-4500](https://broadworkbench.atlassian.net/browse/DT-4500))

**Status: proposal (2026-11-25).** The team must confirm this contract. The server work is
[DT-3849](https://broadworkbench.atlassian.net/browse/DT-3849) (A1) and the client work is
[DT-4512](https://broadworkbench.atlassian.net/browse/DT-4512) (U1). Both write contract tests from
the examples that follow this document.

Related documents:

- [`elasticsearch-to-postgresql-plan.md`](elasticsearch-to-postgresql-plan.md): the design. This
  contract is option 3 of that design (section 5.3). Section numbers with "design" refer to it.
- [`es-access-contract.md`](es-access-contract.md): the access rules for each field.

This document defines the endpoint, the request, the response envelope, the limits, and the errors.
The examples for each view come in separate documents (DT-3848 slices 2 to 4). Each example must
obey this contract.

## 1. Endpoint

`POST /api/search`

- One endpoint for the four views: `datasets`, `studies`, `assets`, and `counts`. The `view` field
  of the request selects the result shape.
- Request and response bodies are `application/json`.
- The caller must be signed in. The endpoint takes the user from the bearer token, as the other
  consent endpoints do. "Session" below means that signed-in user. A request body cannot name a
  user, an email address, a DAC membership, or an institution.
- `POST`, not `GET`, because the request has lists of ids and filters that do not fit in a URL.
  The request reads data only and changes nothing.

## 2. Request

```json
{
  "view": "datasets",
  "scope": { "library": "broad" },
  "search": "sequencing",
  "filters": [
    { "key": "dataType", "values": ["WGS"] },
    { "key": "participantCount", "gte": 100 }
  ],
  "facets": [ { "key": "dac" }, { "key": "dataUseModifiers", "size": 50 } ],
  "sort": { "key": "datasetName", "order": "asc" },
  "page": { "offset": 0, "limit": 25 },
  "projection": "library"
}
```

| Field | Type | Required | Rule |
| --- | --- | --- | --- |
| `view` | string | Yes | `datasets`, `studies`, `assets`, or `counts`. |
| `assetType` | string | With `assets` only | The asset type of the rows, from the server list of the asset types that the Data Library tabs show. The examples documents list the keys. Required for the `assets` view; with another view it gets a 400. |
| `scope` | object | Yes | Exactly one scope (section 3). |
| `search` | string | No | The search box text. Empty or absent means no text match. |
| `filters` | array | No | Each element has a `key`, and either `values` or a range (section 4). |
| `facets` | array | No | Facet distributions to return. Each element has a `key` and an optional `size`. A key appears at most once. |
| `sort` | object | No | `key`, and `order` (`asc` or `desc`, default `asc`). Section 5 gives the default sort. |
| `page` | object | No | `offset` (0 or more, no upper limit) and `limit` (0 to 10,000). Defaults: `offset` 0, `limit` 25. `limit` 0 returns `total` and `facets` without rows, as the facet metadata request needs. |
| `projection` | string | No | The field set of each result row. The examples documents name the projections of each view. Each view has a default. |

The `counts` view accepts only `view`, `scope`, `search`, and `filters`. The `datasets` and
`studies` views accept each field except `assetType`. The `assets` view accepts each field.

**The `assets` view.** Each result row is one asset of one study, of the requested `assetType`: one
entry of that type in the asset list of the study. The eight asset tabs of the Data Library and the
seven asset tabs of the Submissions page each send their own request, so the server pages, sorts,
filters, and counts the rows of one type. Today the tabs have no request of their own: they read up
to 10,000 study documents from the tab counts response and filter the rows in the browser (design
section 3.3). An asset row has the visibility of its study, with the rules of the `studies` view
(design section 7.4). Each asset type has its own projection, sort keys, row filter keys, and facet
keys. Slice 3 lists them with its examples.

- **Study filters and row filters.** A filter whose key is a row filter key of the requested asset
  type applies to the asset row. Each other filter key of the library (dataset filters, study
  filters, and the row filter keys of the other asset types) applies to the study, as today: it
  narrows the studies whose assets the view returns. A study has rows in the result if one of its
  datasets matches the rules of the scope, the `search` text, and the dataset filters, all on that
  one dataset, as one ES document does today, and if it passes the asset filters of the other
  types. The `search` text uses the search fields of the library (`assets/index.ts`).
- **The option corpus.** The options of a tab are the facets of the `assets` view. The caller sends
  them in a separate request with `page.limit` 0, without the filters on the tab's own facet keys
  and with every other filter (the row filters with fixed options of the tab included), as today
  (`useLibraryPageState.ts`). Facets add counts, which the tabs do not show today. A facet has
  10,000 values at most (section 6); today the option list has no limit.

The server rejects a request with a field that this table does not name, or with a field that the
view does not accept (400). It does not ignore the field. Consent parses request bodies with Gson,
which ignores unknown fields, does not tell `null` from an absent field, and keeps the last of two
fields with the same name. The endpoint must parse the body as the showcase lookup does
(`ShowcaseJsonBody`, `ShowcaseRequestFields.rejectUnrecognized`): it checks the keys of each JSON
object, and refuses a repeated name, a byte order mark, and nesting deeper than the request needs
(400).

## 3. Scopes

The `scope` object has exactly one key. Its value must be `true`, a non-empty list, a positive id,
or a library key (for `library`, `null` selects the full library). `{}`, two keys, `false`, an
empty list, or a key that this table does not name gets a 400.

The server adds the visibility rules of the scope (design section 7.4) to each query. The caller
cannot remove them, and the request has no field for them.

| Scope | Request | Who can use it (design section 7.4a) | Target check |
| --- | --- | --- | --- |
| Library | `{ "library": "<key>" }`, or `{ "library": null }` for the full library | Each signed-in user | The key must be in the server list of branded libraries. The "my institution" library uses the institution from the session. For a user with no institution it returns no rows, as today (duos-ui sends institution id 0). |
| Owned by the user | `{ "ownedByMe": true }`. With the `assets` view, for the asset tabs of the Submissions page. | Data Submitter, Chairperson, Admin | None. The user id and email come from the session. A dataset is owned if the user created it, submitted its study, or is a data custodian of its study: the email of the user equals one of the custodian emails (Q15). |
| DAC | `{ "dacIds": [1, 2] }` | Chairperson and Member for their DACs, Admin for each DAC | Each DAC id must be a DAC of the user. Otherwise 404. An Admin sees each DAC: an id that does not exist adds no rows. |
| One study | `{ "studyId": 42 }` | Each signed-in user | The study must be visible to the user (below). Otherwise 404. In the `datasets` view, a user who sees only some datasets of the study (the dataset creator grant) gets those rows, and 404 only if they see none. |
| Dataset ids | `{ "datasetIds": [10, 11] }` | Each signed-in user | Each dataset must be visible to the user (below). Otherwise the whole request gets 404. |
| Dataset identifiers | `{ "datasetIdentifiers": ["DUOS-000123"] }` | As dataset ids | As dataset ids. For the dataset page, which looks a dataset up by its identifier (`DatasetStatistics.tsx`). An identifier matches the full identifier without regard to case, as the ES `match_phrase` does today. |

**Visibility of a study that is not public (Q6).** A study with `publicVisibility` not `true`, its
datasets, and its assets are visible to a user who:

- has the Chairperson, Data Submitter, Admin, or Signing Official role, in each scope, as today in
  the Data Library;
- is a Member of the DAC of a dataset of the study, in the study scope and the two dataset scopes
  (the study page and DAR review), for the datasets of that DAC; the library stays as today for
  a Member;
- created the study, or is one of its data custodians: the email of the user equals one of the
  custodian emails after trimming, with regard to case (`DatasetService.isCreatorOrCustodian`);
- created the dataset, for that dataset only: the study row and the asset rows of the study stay
  hidden from that user;
- in the two dataset scopes only, is the researcher of a submitted DAR that has the dataset
  (the DAR pages and the DUC addendum of a submitted DAR). A draft or a canceled DAR gives no
  grant: the draft endpoint accepts any dataset id.

A dataset without a study stays visible to each user, as `DatasetService.readBasis` has it today.
The rule applies to the library, the study scope, the two dataset scopes, the `assets` view, and
the `counts` view. The DAC scope keeps its own rule, and the owned-by-the-user scope is exempt, as
today. An Admin sees everything. For any other user, a target that the user cannot see gets 404,
the same status and body as a target that does not exist (Q13), so a hidden study cannot be told
from a missing one. duos-ui shows a 404 on the dataset page as an error today; DT-4512 maps it to
"not found". The decisions are in DT-3848 (comment of 2026-11-25) and
[`typed-search-examples-open-questions.md`](typed-search-examples-open-questions.md).

The `assets` view accepts the library scope and the owned-by-the-user scope. Another scope with
`assets` gets a 400.

The design (section 7.4a) also names an institution scope for the Signing Official dashboard. The
request has no institution scope, because the dashboards do not call `/api/search`:

The dashboards (design section 7.4, the Researcher, DAC, and Signing Official rows) keep their own
server endpoints. They call the search service inside the server and do not send a request to
`/api/search`. The DAC dashboard rule ("public, or in a DAC of the user") is thus not a request
scope.

## 4. Filters

- `key` must be a key of the server field registry for the view. The examples documents list the
  keys. An unknown key gets a 400.
- The registry gives each field the operations that it permits, and the scopes and roles that can
  filter, sort, facet, search, or project it (design section 7.2). A key that the scope or the role
  of the user cannot use gets a 400.
- The `search` text matches only the fields that the registry permits the scope and the role to
  search. The caller cannot select the fields.
- `values` is a list of 1 or more values. A row matches if its value is one of the list ("any of"). For a field that
  holds a list, a row matches if one element of the list is one of the values (design section 4.2).
- A range has `gte`, `lte`, or both. A range with one end only is permitted. `gte` greater than
  `lte` gets a 400. An empty `values` list gets a 400.
- A range on a text key, or `values` on a key that permits only a range, gets a 400.
- Filters combine with AND. Values in one filter combine with OR.
- In the `datasets` and `studies` views, two filters on asset fields can match two different
  assets of one study, as today (Q12, decided to keep this).
- In the `assets` view, the row filters of the requested type apply to the asset row, so they all
  must match the same asset. The study filters narrow the studies (section 2).
- The registry gives each text key its match: "one of" (equal to one of the values) or "contains"
  (contains one of the values, without regard to case). Each asset row filter key keeps the match
  of the browser check of today: "contains" where the asset definition uses `includesIgnoreCase`,
  and "one of" where it compares with `includes` (for example the clinical trial status and the
  biospecimen type). The asset registry (slice 3) lists the match of each key.

## 5. Sort and page

**Sort keys.** The examples documents list the sort keys of each view. They are the sorts that
duos-ui sends today:

- `datasets` view: `datasetName`, `studyName`, `accessManagement`, `dac`, `datasetIdentifier`
  (`datasetAsset.ts` `SORT_FIELD_MAP`), and `participantCount`, which that map passes through.
- `studies` view: `studyName`, `totalParticipants`, `datasetCount` (`StudyCardGrid.tsx`
  `SORT_OPTIONS`), and `phenotype`, `species`, `piName`, `dataCustodianEmail`, which the Studies
  table marks as sortable (`studyColumns.tsx`).
- `assets` view: the sort keys of each asset type, which the examples documents list.

Another sort key gets a 400.

Today ES ignores a sort on `phenotype`, `species`, `piName`, or `dataCustodianEmail` and returns the
default order (`studyAsset.ts`). The new endpoint sorts by them. This is a change of behavior, so
the parity test expects a different order for these four keys. A key that holds a list
(`dataCustodianEmail`) sorts by its lowest value in ascending order and by its highest value in
descending order, as ES does for a field with several values.

**Text order.** Each sort on a text key, and the value order of the facet buckets, compares the
text by its bytes, with regard to case (`"Zebra"` before `"apple"`), as ES sorts a `keyword` field
today. The server uses the `"C"` collation of PostgreSQL for these comparisons; the default
collation of the database orders text in another way. Numbers and dates sort as numbers and dates.

**Default sort.** Without `sort`:

- `datasets` view: by `datasetId`, ascending.
- `studies` view: by `studyId`, ascending. This is the order of the ES `composite` aggregation that
  the Studies tab sends today without a sort (design section 3.3).
- `assets` view: by `studyId`, then `assetKey`, ascending. `assetKey` is a key that the server
  gives each asset row. For models, workspaces, and biospecimens it is `id:` and the record id of
  the asset (DT-4208); an entry of these types without a record id is not a row, as today. For the
  other types it is `pos:` and the position of the entry in the asset list of the study. A position
  key changes when a submitter changes the order of the list. `assetKey` sorts by its number
  (`pos:2` before `pos:10`), so the `pos:` rows of a study keep the order of its list, as the browser
  shows them today. The `id:` rows sort by record id, which can differ from the list order that the
  browser shows today; this is a change of behavior.

ES orders unsorted dataset results by score, or by index order without a search. Neither order is
the same in the two systems. The parity test (design section 9.4) thus sends an explicit sort in
each query pair, or compares the full result without a page.

**Tie-breaker.** The server always adds the id, ascending, as the last sort key: `datasetId` for
the `datasets` view, `studyId` for the `studies` view, and `studyId` then `assetKey` for the
`assets` view. For the `studies` view this is the order that ES gives
to ties today (the `composite` second source and the `terms` key order). For the `datasets` view,
ES breaks ties by its internal document order, so the parity test adds `datasetId` ascending to
the ES side of each pair, or compares the full result. Two requests with the same body
thus return the same order, and a page never repeats or skips a row while the data does not change.

**Nulls.** A row without a value for the sort key comes last in both orders. This differs from
today for the study name sort: the ES `composite` aggregation leaves out a study without a name.

**Page.** `offset` is the number of rows to skip. `limit` is the number of rows to return. The
response gives `total`, the number of rows that match without the page.

## 6. Limits

No current caller loses a request that works now, and each kind of work in a request has a limit.
Some limits come from ES; the others are limits that this service chooses. Each limit is a
server configuration value with the default below.

| Limit | Default | Error | Source |
| --- | --- | --- | --- |
| `page.limit`, the rows in one page, in every view | 10,000 | 400 | ES `max_result_window`. `page.offset` has no limit, so a caller pages through a result of any size. ES stops at 10,000 rows; this endpoint does not. |
| Ids or identifiers in a scope list | 10,000 | 400 | The 10,000 rows that the v1 callers request today |
| Filters in one request | 1,024 | 400 | ES boolean clause limit (`indices.query.bool.max_clause_count`, 1,024 in older ES) |
| Filter values in one request, all filters together | 1,024 | 400 | As above. ES 9 does not count the values of one `terms` query as clauses, so this limit is stricter than ES; no current caller sends more. |
| `search` length, and the length of each filter value | 1,000 characters | 400 | |
| `facets` entries | One for each facet key of the view, each key once | 400 | |
| `facets[].size` | 10 by default, 10,000 at most | 400 | ES `terms` aggregation default size. ES 9 permits up to 65,536 buckets (`search.max_buckets`); 10,000 is the decision for this contract. |
| Request body | 1,048,576 bytes (1 MiB) | 413 | A size limit on this endpoint, with the existing `RequestTooLargeExceptionMapper`. Consent has no general body limit, so A1 must add it. |
| Request time | 30 seconds for all the queries of one request | 504 | Section 8 |

**Callers that request 10,000 rows.** Today the v1 callers send:

- DAC datasets, Manage DAC, and DAC profile: a few DAC ids, with `size: 10000` rows
  (`DACDatasets.tsx`, `dacDatasetGrid.tsx`).
- The dataset page: one identifier, without a size (ES returns at most 10 rows).
- DAR review and the DUC addendum: a list of dataset ids, with `size: 10000` (`BucketUtils.ts`).
- The study page: two requests for every dataset of a study, with `pageSize` equal to the dataset
  count (`useStudyDetailsData.ts`).

Each sends one request with `page.limit` up to 10,000, as today. A caller with more than 10,000 ids
splits them into requests of 10,000 ids or fewer and joins the results. A caller that needs more
than 10,000 rows sends the next page with a larger `offset`. The study page requests for a study
with more than 10,000 datasets thus page instead of failing as they do today (design section 3.3).

## 7. Response

The `datasets`, `studies`, and `assets` views return the page that the server used:

```json
{
  "view": "datasets",
  "total": 1234,
  "page": { "offset": 0, "limit": 25 },
  "results": [
    {
      "datasetId": 10,
      "datasetName": "...",
      "dataUse": { "primary": [ { "code": "GRU", "description": "..." } ], "secondary": [] }
    }
  ],
  "facets": {
    "dac": [ { "value": "Broad DAC", "count": 812 } ]
  }
}
```

- `results` holds plain rows with the fields of the projection. The response has no ES shapes:
  no `hits`, `_source`, `aggregations`, or `buckets`.
- A field without a value is absent from the row: the row has no `null`, and no empty text or
  list in place of a missing value. The one exception is `dataUse`, which is always present in a
  projection that has it, as `{ "primary": [...], "secondary": [...] }` with empty lists when the
  dataset has no codes, so that callers can compare it as a whole (DAR review groups datasets by
  `dataUse`, `BucketUtils.ts`).
- `facets` has one entry for each requested facet key. Each entry is a list of `value` and `count`,
  by count descending, then by value ascending. In the `datasets` and `studies` views, a facet counts
  datasets, as the ES facets do today (the Studies tab facets also count dataset documents). In the
  `assets` view, a facet counts asset rows. A row with a list of values counts one time in each
  bucket of its values (design section 4.2). A row with no value for the facet field is in no
  bucket, as with an ES `terms` aggregation. A facet never has a `null` bucket.
- Slices 2 and 3 name the projections that replace the study details and total participants that
  the study page reads with its dataset rows today.
- The `counts` view returns `view` and named totals, without `page`, `results`, or `facets`: the
  dataset and study counts of the tabs, and one total for each asset type, for the tab badges. Each
  asset total counts the rows of its type with the rule of the `assets` view: the row filters of the
  type apply to the rows, and the other filters to the studies. Slice 3 defines the names.
- `total`, `count`, and each total are exact. ES `cardinality` is approximate. The parity test must
  accept this difference.

## 8. Timeout

- The queries of one request (rows, total, facets) share one budget of 30 seconds. Each query runs
  with a PostgreSQL `statement_timeout` equal to the time left in the budget. This is the only
  timer, so one status reaches the caller.
- The server maps a query cancelled by the timeout (SQLSTATE `57014`) to 504. Without this mapping,
  `Resource.createExceptionResponse` returns 500.

## 9. Errors

The errors that the endpoint returns use the existing consent error body:

```json
{ "message": "Unknown filter key: dataTyp", "code": 400 }
```

| Status | When |
| --- | --- |
| 400 | The body is not valid JSON, a required field is missing (`view`, `scope`, or `assetType` with the `assets` view), a field is unknown or not accepted by the view, a value has the wrong type, a key is not in the registry or not permitted for the scope or role, the scope is not valid (section 3), or a limit of section 6 is exceeded. |
| 401 | The caller is not signed in. The consent authentication filter answers before the endpoint, with a plain text body, not the JSON body. |
| 403 | The user cannot use the requested scope (design section 7.4a). The server returns 403, not an empty result. |
| 429 | The consent rate limit for `/api/` (`RateLimitFilter`). The response has a `Retry-After` header and no body. |
| 404 | A target id of the scope (a DAC, a study, a dataset) that the user cannot read or that does not exist, with the same body for both (Q13). Not for an Admin, who sees each target. |
| 413 | The body is larger than 1,048,576 bytes. |
| 504 | The queries of the request took more than 30 seconds. |
| 500 | Any other server failure. The body has a fixed message, `Search failed`. |

- **Order of the checks.** The server parses the body and checks the `scope` first; a body that
  is not JSON, or a scope that is not valid or that the view does not accept (section 3, and the
  documents of each view), gets a 400. If the roles of the user cannot use the scope, the response
  is 403, even when the request breaks another rule. Then the server checks the rest of the request
  (400), and then the target ids of the scope (404).
  A user who cannot use a scope thus learns nothing about the other rules from its responses.
- The `message` names the field or the limit. A key from the request is cut to 64 characters in
  the message, as `ShowcaseLookupException` does. The message never repeats the `search` text or a
  filter value.
- The endpoint maps each exception itself and does not use `Resource.createExceptionResponse`.
  That method logs the exception message at WARN, and for a JDBI error the message holds the SQL
  and its bound values, which include the search text. For an exception class that it does not
  know, it also puts the exception message in the 500 body. The endpoint logs the exception class
  and the request view and scope kind only.

## 10. Product decisions

The design (section 11) left four questions to product. They were decided on 2026-11-25 (DT-3848
comment), and this contract follows the answers:

| Question | Answer | Part of the contract |
| --- | --- | --- |
| Q6: who sees a study that is not public | Today's rules, and the study is hidden from researchers in each scope, with the grants of section 3 | Section 3 |
| Q12: can two asset filters match two different assets of one study | Yes, as today | Section 4 |
| Q13: 403 or 404 for a target that the user cannot read | 404, the same as a target that does not exist | Section 9 |
| Q15: does the ownership rule match the data custodian email | Yes: the rule is corrected | Section 3 |
