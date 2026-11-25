# Typed search fixtures

Ticket: [DT-3848](https://broadworkbench.atlassian.net/browse/DT-3848). Each file is one example
of `POST /api/search` from the typed search documents in `docs/plans/` (`typed-search-*.md`). The
server tests (DT-3849) and the client tests (DT-4512) read these files. duos-ui copies them, with
the consent commit that it copied from.

`TypedSearchFixtureFormatTest` checks the format of each file. `TypedSearchRequestTest` and
`TypedSearchResponseTest` check the request and the response against the contract and the key
lists in `registry/`, which stand in for the server registry until DT-3849 builds it.

## Format

```json
{
  "example": "datasets 1.1, Data Library Datasets tab",
  "source": "docs/plans/typed-search-examples-datasets.md#11-data-library-datasets-tab",
  "user": { "roles": ["RESEARCHER"] },
  "request": { "view": "datasets", "scope": { "library": null } },
  "response": {
    "status": 200,
    "body": { "view": "datasets", "total": 0, "page": { "offset": 0, "limit": 25 }, "results": [] }
  }
}
```

- `user.roles`: `UserRoles` names. `user.relation` (optional): how the user relates to the data.
- Each fixture stands alone: an id in one fixture says nothing about the same id in another.
- `response`: `status` and `body`. An error body is `{ "message", "code" }` with `code` equal to the
  status. Messages and values are illustrative; `total` can be larger than the rows shown.
- `serverCheck`: the reason for an error that the tests cannot detect from the request alone, for
  example a DAC that is not a DAC of the user. Each 404, and each 403 that no role rule explains,
  needs one.

The folders hold the views: `datasets/`, `studies/`, `counts/`, and `assets/`. The fixtures named
`q6-`, `q12-`, `q13-`, and `q15-` show the product decisions (`typed-search-examples-open-questions.md`).
`registry/` holds the key lists, and `self-test/` the requests that the request check must reject;
neither is a fixture.
