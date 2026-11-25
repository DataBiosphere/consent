# Typed Search — Product Decisions and Their Examples

Ticket: [DT-3848](https://broadworkbench.atlassian.net/browse/DT-3848) (slice 4 of 6)

**Status: decided (2026-11-25).** The design left four questions to product (design section 11:
Q6, Q12, Q13, Q15). They were decided on 2026-11-25 (DT-3848 comment), and
[`typed-search-contract.md`](typed-search-contract.md) ("the contract", section 10) follows the
answers. This document records each answer, what it changes from today, and an example for each
case, so the server (DT-3849) and client (DT-4512) teams test the same behavior. "Design" refers to
[`elasticsearch-to-postgresql-plan.md`](elasticsearch-to-postgresql-plan.md). The values are
illustrative. Slice 5 turns each example into a JSON fixture in the folder of its view.

| Question | Answer | Contract |
| --- | --- | --- |
| Q6: who sees a study that is not public | Today's rules, and the study is hidden from researchers in each scope, with the grants of section 1 | Section 3 |
| Q12: can two asset filters match two different assets of one study | Yes, as today | Section 4 |
| Q13: 403 or 404 for a target that the user cannot read | 404, the same as a target that does not exist | Section 9 |
| Q15: does the ownership rule match the data custodian email | Yes: the rule is corrected | Section 3 |

## 1. Q6: a study that is not public

Design sections 7.4, 7.4a, and 7.5. Today the Data Library hides a study that is not public from
each user without the Chairperson, Data Submitter, Admin, or Signing Official role, but the study
page, the dataset page, and the dataset id requests have no rule: each signed-in user who has the id
reads the study.

**The rule.** A study with `publicVisibility` not `true`, its datasets, and its assets are visible
to a user who:

| Grant | Scopes |
| --- | --- |
| Has the Chairperson, Data Submitter, Admin, or Signing Official role | Each scope, as today in the Data Library |
| Is a Member of the DAC of a dataset of the study, for the datasets of that DAC | The study scope and the two dataset scopes (the study page, DAR review); not the library |
| Created the study, or is one of its data custodians (the email equals a custodian email after trimming, with regard to case) | Each scope |
| Created the dataset | Each scope, for that dataset only; the study row, the study page header, and the asset rows stay hidden |
| Is the researcher of a submitted DAR that has the dataset (not a draft or a canceled DAR: the draft endpoint accepts any dataset id) | The two dataset scopes only (the DAR pages, the DUC addendum of a submitted DAR) |

The rule applies to the library, the study scope, the two dataset scopes, the `assets` view, and
the `counts` view. The DAC scope keeps its own rule (the DACs of the user), and the owned-by-the-user
scope is exempt, as today. The dashboards keep their own endpoints and rules (contract section 3).
An Admin sees everything. A dataset without a study stays visible to each user. For any other user,
a target that the user cannot see gets 404 (section 3 of this document).

**What changes from today.**

- A Researcher who has the id of a study that is not public no longer reads it on the study page,
  the dataset page, or through dataset ids, unless a grant applies.
- A Researcher who created a study that is not public, or is its custodian, now finds it in the
  Data Library.
- A Member sees no more in the library than today, and keeps the study page and DAR review for the
  datasets of their DACs; a Member of another DAC gets 404 there.

The users of the examples:

| User | Roles | Relation to study 42, which is not public |
| --- | --- | --- |
| Rana | Researcher | None |
| Carl | Researcher | Data custodian of study 42 |
| Ravi | Researcher | Created dataset 105 of study 42, and nothing else |
| Dora | Researcher | Submitted a DAR on dataset 105 |
| Dana | Data Submitter | None |
| Cleo | Chairperson | Chair of the DAC of dataset 105 |
| Mo | Member | Member of the DAC of dataset 105 |
| Max | Member | Member of another DAC only |

### 1.1 The library

```json
{ "view": "studies", "scope": { "library": null }, "search": "kidney" }
```

| User | Study 42 |
| --- | --- |
| Rana | Not in `results` (as today) |
| Carl | **In `results`** (a change: the custodian grant) |
| Ravi | Not in `results`; Ravi sees dataset 105 in the `datasets` view with the same request |
| Dana | In `results` (as today) |
| Mo | Not in `results` (as today) |

The `datasets`, `assets`, and `counts` views follow the same rule.

### 1.2 The study page

```json
{ "view": "studies", "scope": { "studyId": 42 }, "page": { "offset": 0, "limit": 1 } }
```

| User | Response |
| --- | --- |
| Rana | **404** (a change: 200 today) |
| Carl | 200, the `header` row of study 42 |
| Dana | 200 |
| Ravi | **404** for the header; the `datasets` request of the grid returns dataset 105 only |
| Mo | 200, as today |
| Max | **404** (a change: 200 today) |

The `datasets` request of the study page grid gets the same status, except for Ravi.

### 1.3 Dataset ids

The DAR review page asks for the datasets of a DAR. Dataset 105 is in study 42.

```json
{ "view": "datasets", "scope": { "datasetIds": [101, 105] }, "page": { "offset": 0, "limit": 10000 } }
```

| User | Response |
| --- | --- |
| Rana | **404** for the whole request (a change: 200 today) |
| Carl | 200, both rows |
| Ravi | 200, both rows: Ravi created dataset 105 |
| Dora | 200, both rows: the submitted DAR grant (a draft DAR on dataset 105 gives no grant) |
| Cleo | 200, both rows: the Chairperson role |
| Mo | 200, both rows: the DAC Member grant |
| Max | **404** (a change: 200 today) |

The server checks each id before it runs the query. It does not drop an id that the user cannot
see: a smaller result would hide the failure from the page. The `datasetIdentifiers` scope (the
dataset page) works the same way.

### 1.4 The approved rule

Q6 also asked whether the approved rule (`dacApproval`) is a display filter only (OPEN-4). The
decision of 2026-11-25 keeps today's rules and does not address OPEN-4, so the approved rule stays
as today: on the `datasets` view of the library and the `datasets` total of the `counts` view.

## 2. Q12: two asset filters on one study

Design section 4.2. **Decided: keep today.** Two filters on assets of one type can match two
different assets of a study, and the study is in the result of the `studies`, `datasets`, and
`counts` views. In the `assets` view, the filters of the requested type are row filters, so they
must match the same row (asset registry, section 4).

Study 7 has model 45 (license MIT, format PyTorch) and model 46 (license Apache-2.0, format ONNX).

```json
{
  "view": "studies",
  "scope": { "library": null },
  "filters": [
    { "key": "modelLicense", "values": ["mit"] },
    { "key": "modelFormat", "values": ["onnx"] }
  ]
}
```

- Study 7 is in `results`: model 45 matches the license, model 46 the format.
- The same filters on the AI Models tab (`"view": "assets"`, `"assetType": "models"`) return no
  row of study 7: no one model matches both. The `models` total of the `counts` view is also 0 for
  study 7.
- A `clinicalTrialDates` or `fundingDate` range has two bounds. As a study filter, the two bounds
  can match two different assets, as today. Study 9 has trial 1 (2020-03-01 to 2030-06-30) and
  trial 2 (2010-01-01 to 2015-12-31): the range 2019-01-01 to 2025-12-31 keeps study 9 on the
  Studies tab, and no trial of study 9 is a row of the Clinical Trials tab.

## 3. Q13: a target that the user cannot read

Design section 7.4a. **Decided: 404.** A target is an id in the scope: a DAC, a study, a dataset,
or a dataset identifier. A target that the user cannot read and a target that does not exist get
the same status and the same body, so a caller cannot learn which ids exist.
`DatasetService.verifyStudyVisibilityAccess` returns 404 today.

| Request | Response |
| --- | --- |
| A Chairperson of DAC 3 sends `"dacIds": [3, 8]`, and DAC 8 is not theirs | 404, `{ "message": "Not found: dacIds", "code": 404 }` |
| The same Chairperson sends `"dacIds": [3, 999]`, and DAC 999 does not exist | The same 404 and body |
| Rana sends `"studyId": 42` (section 1.2) | 404, `Not found: studyId` |
| Rana sends `"studyId": 999`, which does not exist | The same 404 and body |

An Admin sees each target: for an Admin, a DAC id that does not exist adds no rows. The message
names the scope field, not the id. A user who cannot use a scope at all still gets 403
(contract section 9): a Researcher who sends `{ "ownedByMe": true }`, or a user with no DAC role
who sends `dacIds`.

## 4. Q15: the data custodian in the ownership rule

Design section 4.2. The ownership rule of the `ownedByMe` scope keeps a dataset if the user created
it, submitted its study (`study.dataSubmitterId`), or is a data custodian of its study. duos-ui
sends the custodian part as a `term` on `study.dataCustodianEmail`, an analyzed text field, so it
possibly never matches a full email address today. **Decided: correct it.** The custodian part
matches if the email of the user equals one of the custodian emails of the study, as
`DatasetService.isCreatorOrCustodian` does.

Dana (Data Submitter) is a data custodian of study 50 and did not create it or any of its datasets.

```json
{ "view": "studies", "scope": { "ownedByMe": true } }
```

- Study 50 is in `results`, with the `submission` projection (a change: not today).
- The `datasets`, `assets`, and `counts` views with the `ownedByMe` scope change in the same way.
  The parity test (design section 9.4) expects these extra rows.
- The scope is for a Data Submitter, Chairperson, or Admin (contract section 3). A Researcher who is
  only a custodian still gets 403, as today the Submissions page is not open to them.
- **The dashboard.** The Data Submissions total of the Researcher dashboard uses the same ownership
  clause and keeps its own endpoint. DT-3849 corrects the clause there too, so Dana's total grows by
  study 50, its datasets, and its asset entries. The dashboard still counts for a Data Submitter
  only, and counts every model and workspace entry, with or without a record id (counts view,
  section 3).
