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
- `outcome`: in `open-questions/` only. It maps each question that the example depends on (Q6, Q12,
  Q13, Q15) to `A` (today, except for Q13), `B` (the change), or `AB` (the same with both). A test
  runs the fixtures whose answers product approves.
- `response`: `status` and `body`. An error body is `{ "message", "code" }` with `code` equal to the
  status. Messages and values are illustrative; `total` can be larger than the rows shown.
- `serverCheck`: the reason for an error that the tests cannot detect from the request alone, for
  example a DAC that is not a DAC of the user. Each 404, and each 403 that no role rule explains,
  needs one.

The folders hold the views: `datasets/`, `studies/`, `counts/`, `assets/`, and `open-questions/`.
`registry/` holds the key lists, and `self-test/` the requests that the request check must reject;
neither is a fixture.
