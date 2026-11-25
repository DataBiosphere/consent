# Typed Search — Examples for the Open Product Questions

Ticket: [DT-3848](https://broadworkbench.atlassian.net/browse/DT-3848) (slice 4 of 6)

**Status: proposal (2026-11-25).** Four open product questions of the design change parts of
[`typed-search-contract.md`](typed-search-contract.md) ("the contract", section 10). Until product
answers, the contract and its examples use the behavior of today. This document shows both outcomes
of each question with the same request, so product can see what each answer changes, and the
server (DT-3849) and client (DT-4512) teams can write the test for either answer. "Design" refers
to [`elasticsearch-to-postgresql-plan.md`](elasticsearch-to-postgresql-plan.md).

In each pair, **outcome A** is the behavior of today, which the contract uses until product
decides, and **outcome B** is the change. Q13 is the exception: today the ES endpoints check no
target, so neither outcome is the behavior of today. The values are illustrative. Slice 5 turns each example
into a JSON fixture, with the outcome in its name, so a test can select the outcome that product
approves.

## 1. Q6: the visibility rule for a study that is not public

Design section 11, question 6, and sections 7.4, 7.4a, and 7.5.

- **Outcome A, the role rule (today).** In the library, a study that is not public is visible to a
  Chairperson, Data Submitter, Admin, or Signing Official, and to no other user. The study scope and
  the two dataset scopes are open to each signed-in user, with no visibility rule.
- **Outcome B, the contract rule.** A study that is not public is visible to an Admin, to the user
  who created the study, and to a data custodian of the study (exact match of the email,
  `DatasetService.canReadStudy`). A dataset of that study is also visible to the user who created
  that dataset, and only that dataset (`readBasis`). These examples apply the rule to the library
  and to the study and dataset scopes.
- Q6 also asks whether the DAC pages and the Signing Official dashboard get the rule. These examples
  keep the DAC scope rule of the contract (the DACs of the user) and leave the dashboards as they
  are.
- In the `ownedByMe` scope, each part of the ownership rule is also a grant of the contract rule
  (the dataset creator for that dataset, the study creator, who is the `study.dataSubmitterId` of
  the index, and a custodian), so the `datasets` view returns the same rows with both outcomes. The
  `studies` and `assets` views can return fewer: a Data Submitter who created only a dataset of
  another user's study that is not public sees that study and its assets with outcome A, and not
  with outcome B, as in section 1.1. If product extends the rule to them, a follow-up adds their examples.

The users of the examples:

| User | Roles | Relation to study 42, which is not public |
| --- | --- | --- |
| Rana | Researcher | None |
| Carl | Researcher | Data custodian (Carl's email is in `study.dataCustodianEmail`) |
| Dana | Data Submitter | None |
| Ada | Admin | None |

### 1.1 The library

```json
{
  "view": "studies",
  "scope": { "library": null },
  "search": "kidney",
  "page": { "offset": 0, "limit": 25 }
}
```

| User | Outcome A | Outcome B |
| --- | --- | --- |
| Rana | Study 42 is not in `results` | Not in `results` |
| Carl | Not in `results` | **In `results`** |
| Dana | In `results` | **Not in `results`** |
| Ada | In `results` | In `results` |

The `counts` view, the `datasets` view, and the `assets` view with the `library` scope change in
the same way. With outcome B, Dana sees fewer studies, and Carl sees a study that Carl is a
custodian of. A Researcher who created only dataset 105 of study 42 sees dataset 105 in the
`datasets` view. Study 42 is not a row of the `studies` view and its assets are not rows of the
`assets` view for that user: a study row and an asset row need access to the study
(`canReadStudy`, `StudyAssetService`), which creating a dataset does not give.

### 1.2 The study page

```json
{
  "view": "studies",
  "scope": { "studyId": 42 },
  "page": { "offset": 0, "limit": 1 }
}
```

| User | Outcome A | Outcome B |
| --- | --- | --- |
| Rana | 200, the `header` row of study 42 | **403 or 404 (Q13, section 3)** |
| Carl | 200 | 200 |
| Dana | 200 | **403 or 404** |
| Ada | 200 | 200 |

The `datasets` request of the study page grid (dataset examples, example 1.9) gets the same status.
With outcome A, the public visibility rule controls discovery in the library only: a user who has
the id reads the study (design section 7.4a).

### 1.3 Dataset ids

The DAR review page asks for the datasets of a DAR. Dataset 105 is in study 42.

```json
{
  "view": "datasets",
  "scope": { "datasetIds": [101, 105] },
  "page": { "offset": 0, "limit": 10000 }
}
```

| User | Outcome A | Outcome B |
| --- | --- | --- |
| Rana | 200, both rows | **403 or 404**: one id is not readable, so the whole request fails |
| Carl | 200, both rows | 200, both rows |
| Cleo, Chairperson of the DAC of dataset 105, who reviews the DAR | 200, both rows | **403 or 404**: the contract rule gives a DAC member or chair no access (design section 7.5, OPEN-3) |

- With outcome B, the server checks each id before it runs the query (design section 7.4a). It does
  not drop the ids that the user cannot read: a smaller result would hide the failure from the page.
- Outcome B thus breaks DAR review for a DAC that reviews a study that is not public. Design
  section 7.4a names a possible rule for these pages: the datasets must be in a DAR that the user
  can read. That rule is not part of outcome B; product decides it with Q6.
- With outcome B, an id that does not exist gets the same 403 or 404 as an id that the user cannot
  read (section 3), in the `studyId`, `datasetIds`, and `datasetIdentifiers` scopes. Dataset
  examples 1.7 to 1.10 and study example 6.3, which return fewer rows or `total` 0 for an id that
  does not exist, then change to that status.
- The `datasetIdentifiers` scope (the dataset page) works the same way.

### 1.4 The approved rule

Q6 also asks whether the approved rule (`dacApproval`) is a display filter only (OPEN-4). The
outcomes do not change the requests: with either answer, the server applies the approved rule to
the `datasets` view of the library and to the `datasets` total of the `counts` view, as today. If
product answers that the rule is not a display filter, a follow-up adds it to the scopes where it
applies; the examples then change in that follow-up.

## 2. Q12: two asset filters on one study

Design section 11, question 12, and section 4.2. Today, two filters on assets of one type can match
two different assets of a study, and the study is in the result of the `studies`, `datasets`, and
`counts` views (contract section 4).

Study 7 has two models:

| Model | `license` | `format` |
| --- | --- | --- |
| 45 | MIT | PyTorch |
| 46 | Apache-2.0 | ONNX |

### 2.1 Studies tab with two model filters

```json
{
  "view": "studies",
  "scope": { "library": null },
  "filters": [
    { "key": "modelLicense", "values": ["mit"] },
    { "key": "modelFormat", "values": ["onnx"] }
  ],
  "page": { "offset": 0, "limit": 25 }
}
```

| Outcome | Study 7 |
| --- | --- |
| A, today: each filter can match a different asset | In `results`: model 45 matches the license, model 46 the format |
| B, the filters must match the same asset | **Not in `results`**: no one model matches both |

The `datasets` view, the `studies` and `datasets` totals of the `counts` view, the asset totals of
the other types, and the `assets` view of another type (for example the Publications tab with these
two filters) change in the same way. The `models` total does not change (section 2.2).

### 2.2 The AI Models tab

```json
{
  "view": "assets",
  "assetType": "models",
  "scope": { "library": null },
  "filters": [
    { "key": "modelLicense", "values": ["mit"] },
    { "key": "modelFormat", "values": ["onnx"] }
  ],
  "page": { "offset": 0, "limit": 25 }
}
```

With both outcomes, the result has no row of study 7: the filters are row filters of the requested
type, so they apply to each row (asset registry, section 4). The `models` total of the `counts` view
is also 0 for study 7. With outcome A, the Studies tab thus shows study 7 while the AI Models tab
shows none of its models, as today.

### 2.3 A range on two fields

A `clinicalTrialDates` filter has two bounds, one on `startDate` and one on `endDate`. Study 9 has
trial 1 (2020-03-01 to 2030-06-30) and trial 2 (2010-01-01 to 2015-12-31).

```json
{
  "view": "studies",
  "scope": { "library": null },
  "filters": [ { "key": "clinicalTrialDates", "gte": "2019-01-01", "lte": "2025-12-31" } ],
  "page": { "offset": 0, "limit": 25 }
}
```

| Outcome | Study 9 |
| --- | --- |
| A, today: the two bounds can match two different trials | In `results`: trial 1 starts after 2019-01-01, trial 2 ends before 2025-12-31 |
| B, one trial must match both bounds | **Not in `results`** |

`fundingDate` works the same way. On the Clinical Trials tab, the filter is a row filter, and with
both outcomes no trial of study 9 is a row.

## 3. Q13: 403 or 404 for a target that the user cannot read

Design section 11, question 13, and section 7.4a. A target is an id in the scope: a DAC, a study, a
dataset, or a dataset identifier. The contract (section 9) returns "403 or 404" until product
decides.

- **Outcome A: 403.** The response says that the user cannot read the target.
- **Outcome B: 404.** The response is the same as for an id that does not exist, so a caller cannot
  learn that the target exists. `DatasetService.verifyStudyVisibilityAccess` returns 404 today.

Where the server checks a target (the DAC ids always, and the study and dataset ids with outcome
B of Q6), an id that does not exist and an id that the user cannot read get the same status and the
same message with each outcome. Otherwise the difference would tell the caller which ids exist.

### 3.1 A DAC of another user

A Chairperson of DAC 3 asks for DAC 8, which is not theirs.

```json
{
  "view": "datasets",
  "scope": { "dacIds": [3, 8] },
  "projection": "dacTable",
  "page": { "offset": 0, "limit": 10000 }
}
```

| Outcome | Status | Body |
| --- | --- | --- |
| A | 403 | `{ "message": "Not permitted: dacIds", "code": 403 }` |
| B | 404 | `{ "message": "Not found: dacIds", "code": 404 }` |

The message names the scope field, not the id, so the body does not repeat which of the ids failed.
The same request with DAC 999, which does not exist, gets the same status and body.

### 3.2 A study that the user cannot read

This example applies only if product selects outcome B of Q6 (section 1.2). With outcome A of Q6,
the server checks no study id: each signed-in user reads each study, and an id that does not exist
returns 200 with `total` 0, as today (study example 6.3).

```json
{
  "view": "studies",
  "scope": { "studyId": 42 },
  "page": { "offset": 0, "limit": 1 }
}
```

| Outcome | Rana (Researcher, no relation) | Study 999, which does not exist |
| --- | --- | --- |
| A | 403, `Not permitted: studyId` | 403, `Not permitted: studyId` |
| B | 404, `Not found: studyId` | 404, `Not found: studyId` |

### 3.3 The scope itself

A user who cannot use a scope at all gets a 403 with both outcomes (contract section 9). For
example, a Researcher without the Data Submitter, Chairperson, or Admin role who sends
`{ "ownedByMe": true }`, or a user with no DAC role who sends `dacIds`. Q13 is only about the
target ids of a scope that the user can use.

## 4. Q15: the data custodian in the ownership rule

Design section 11, question 15, and section 4.2. The ownership rule of the `ownedByMe` scope keeps
a dataset if the user created it, submitted its study (`study.dataSubmitterId`), or is a data
custodian of its study. duos-ui sends the custodian part as a `term` on `study.dataCustodianEmail`,
an analyzed text field, so it possibly never matches a full email address today. The dashboard
count (`DashboardSearchService`) has the same clause.

- **Outcome A: keep today.** The custodian part does not match. A data custodian sees a study in
  Submissions only if they created a dataset or submitted the study.
- **Outcome B: correct it.** The custodian part matches if the email of the user is one of the
  custodian emails of the study, with an exact match, as `DatasetService.isCreatorOrCustodian` does.

Dana (Data Submitter) is a data custodian of study 50. Dana did not create it or any of its datasets.

### 4.1 Submissions

```json
{
  "view": "studies",
  "scope": { "ownedByMe": true },
  "page": { "offset": 0, "limit": 25 }
}
```

| Outcome | Study 50 |
| --- | --- |
| A | Not in `results` |
| B | **In `results`**, with the `submission` projection |

The `datasets`, `assets`, and `counts` views with the `ownedByMe` scope change in the same way.
With outcome B, the parity test (design section 9.4) expects these extra rows and does not report
them as a defect.

### 4.2 Who can use the scope

The `ownedByMe` scope is for a Data Submitter, Chairperson, or Admin (contract section 3). A
Researcher who is only a data custodian gets a 403 with both outcomes, as today the Submissions page
is not open to them. Outcome B thus changes the result for a custodian with one of these roles only.

### 4.3 The dashboard

The Data Submissions total of the Researcher dashboard uses the same ownership clause. It keeps its
own endpoint (contract section 3), but the server team changes the clause there with the same
answer, so the dashboard and the Submissions page include the same studies. With outcome B, Dana's
total grows by study 50, its datasets, and its asset entries. This is the one dashboard number
that a question of this document changes; the counts view document (section 3) describes the
dashboards as they are today.

The two numbers still differ in the ways of the counts view document: the dashboard counts for a
Data Submitter only (`DashboardSearchService`), and it counts every model and workspace entry, with
or without a record id.

## 5. Summary

| Question | Outcome A (today; the contract uses it until product decides, except for Q13) | Outcome B | Sections of the contract |
| --- | --- | --- | --- |
| Q6 | Role rule in the library; study and dataset scopes open | Contract rule in the library and the study and dataset scopes | 3, and the scope rules of each view |
| Q12 | Two asset filters, or two bounds, can match two assets | One asset must match all | 4 |
| Q13 | 403 (no check today; the contract says "403 or 404" until product decides) | 404 | 9 |
| Q15 | The custodian email does not match | The custodian email matches | 3 (`ownedByMe`) |
