# Study Template CSV Contract v1

Jira: [DT-3869](https://broadworkbench.atlassian.net/browse/DT-3869)

Status: **product and Consent API owner approval required before dependent parser or endpoint work
merges**

This is the canonical v1 contract for importing one study and its datasets into a DUOS study
registration draft. It maps template fields to Consent's `StudyRegistrationRequest` wire contract;
it does not define a new registration API or replace existing registration validation.

Canonical synthetic fixtures live in `src/test/resources/fixtures/study-template/v1`, where Consent
parser tests can load them directly. DT-3869 itself does not implement parsing or UI behavior.

DT-3869 is Ticket 1 of the workflow planned in duos-ui at
`docs/plans/study-template-validation-workflow.md` (DT-1855). That plan defers to this document for
the layout and records how the decisions taken here change the later tickets — in particular that
the UI must offer a blank-template download, since v1 expresses every structured value as a row.

## File contract

- File extension: `.csv`.
- Maximum size: 5 MiB (5,242,880 bytes), including a UTF-8 BOM when present.
- Encoding: UTF-8. A UTF-8 BOM is accepted only at the beginning of the file.
- Dialect: RFC 4180-style comma-separated values. Double quotes enclose values; a literal quote is
  escaped as `""`. CRLF and LF record separators are accepted.
- One file represents exactly one study.
- The case-sensitive header is required exactly once and in this order:

```text
templateVersion,recordType,recordId,parentRecordId,field,value
```

- `templateVersion` is required on every data row and must be major version `1`. Mixed or
  unsupported versions are errors.
- Blank physical lines may be ignored. A zero-byte, BOM-only, header-only, or otherwise record-free
  file is invalid.
- Leading and trailing whitespace is significant in string values. Producers should not add
  whitespace around unquoted values.

### Size limit rationale

5 MiB is a template-specific limit that the import endpoint must enforce itself. It is not the
platform upload limit: the shared check in `Resource#validateFileDetails` uses the OWASP
`FileValidator` default of 500,000,000 bytes (500 MB), which is appropriate for institutional
certification documents but far too permissive for a text template.

The limit is generous for the intended client. Rows average roughly 120 bytes. A study with 40
study-level rows and 100 consent groups at about 25 rows each, file types included, is around 2,540
rows, or roughly 300 KB — about a tenth of the limit. A file that exceeds 5 MiB is not a study
template that a person authored in a spreadsheet.

### Spreadsheet compatibility

Producers are expected to author templates in Excel or Google Sheets, so v1 accepts what those tools
emit by default:

| Producer behavior | v1 handling |
| --- | --- |
| CRLF record separators (Excel default) | Accepted; LF is also accepted. |
| UTF-8 BOM (Excel "CSV UTF-8 (Comma delimited)") | Accepted at the beginning of the file only. |
| Quoting only cells that need it | Accepted; quoting is per RFC 4180 and never required for cells without commas, quotes, or line breaks. |
| Trailing blank lines at end of file | Ignored. |
| Google Sheets "Download → Comma-separated values" | Accepted; it is comma-delimited UTF-8 without a BOM. |

Excel is not strictly RFC 4180-conformant, and two of its deviations are rejected rather than
guessed at:

- **Locale list separator.** Excel writes the Windows list separator, which is a semicolon in many
  European locales. v1 is comma-delimited only. When the header row parses as a single cell that
  contains a semicolon or a tab, reject with an actionable message naming the detected character and
  telling the producer to re-export as comma-delimited, rather than reporting a missing header. An
  optional producer-supplied delimiter is a deferred v2 item, not a v1 requirement.
- **Value auto-formatting.** Excel may coerce cells that look like dates or long numbers, so a
  `value` cell can arrive as `15-Jan-2026` or in scientific notation. v1 does not reverse this. Such
  values fail their field's normal type rule with the usual row and column context.

The importer treats every cell as literal text and never evaluates a leading `=`, `+`, or `@` as a
formula. Any template Consent generates for download must escape those prefixes so the exported file
is not a CSV-injection vector in the producer's spreadsheet application.

## Record model

Each row assigns one `field` to one logical record.

| `recordType` | `recordId` | `parentRecordId` | Meaning |
| --- | --- | --- | --- |
| `study` | Must be `study` | Empty | One field on the single `StudyRegistrationRequest`. |
| `consentGroup` | Unique non-empty template ID | Must be `study` | One field on a `ConsentGroupRequest`; each ID becomes one `consentGroups[]` item. |
| `fileType` | Unique non-empty template ID | Must be a `consentGroup` `recordId` | One field on a `fileTypes[]` item belonging to that consent group. |

`recordId` is template-only grouping metadata. It never reaches the wire: numeric study and dataset
IDs remain server-owned, and `fileTypes[]` items are positional. A `fileType` record's
`parentRecordId` must name a `consentGroup` record declared in the same file; `fileTypes` items are
attached to that consent group in file order.

Rows may appear in any order after the header. A `(recordType, recordId, field)` tuple may occur
only once for a single-valued field; duplicate assignments are errors even if their values match.
Scalar array fields are the one exception: they repeat the tuple once per item, as described under
Value encoding. Unknown headers, record types, and fields are errors and are never silently
ignored.

## Value encoding

| Kind | Encoding |
| --- | --- |
| String or enum | Exact wire value; enum matching is case-sensitive. Use normal CSV quoting for commas, quotes, or line breaks. |
| Boolean | Exactly lowercase `true` or `false`. |
| Integer | Base-10 digits with an optional leading minus sign; no decimal point or separator. Domain validation may reject negative values. |
| Date | ISO local date `YYYY-MM-DD`; calendar-invalid dates are rejected. |
| URI | Absolute `http` or `https` URI. |
| Scalar array | One row per item, repeating the same `(recordType, recordId, field)` tuple. Each `value` cell holds one plain string or enum item, encoded exactly as a single value of that kind. |
| Empty value | An empty `value` cell means absent (`null`), not an empty string or empty array. Omit optional fields; empty required values fail validation. |

Every `value` cell is a single scalar. **v1 has no JSON encoding at all**: structured wire values
are expressed as rows, not as embedded documents. Arrays of scalars repeat their row, and the only
wire array of objects, `fileTypes`, is a record type rather than a cell. So the wire value
`["Genomic","Phenotypic"]` is two rows, and neither cell needs quoting:

```csv
1,study,study,,dataTypes,Genomic
1,study,study,,dataTypes,Phenotypic
```

Repeated rows are collected in file order and that order is preserved on the wire. A scalar array
field is absent when it has no rows; there is no encoding for an explicitly empty array. An empty
`value` cell on a repeated row is an error, not a skipped item, and the same item value may not
appear twice within one field.

Product asked for a template with no JSON in it, which is what set the v1 boundary: the wire
properties that cannot be expressed as scalar rows without inventing new encodings are excluded from
v1 rather than embedded as JSON. That is why non-file assets are not in this contract.

Product also confirmed that producers get the template by downloading it from the DUOS UI rather
than building it from this contract. The download must emit the header and the `templateVersion`,
`recordType`, `recordId`, `parentRecordId`, and `field` columns for the fields being offered,
leaving `value` empty for the producer to fill in, and it must escape leading `=`, `+`, and `@` as
described under Spreadsheet compatibility.

That download is a v1 dependency rather than a convenience, and it is **not currently tracked by any
ticket**. It was a convenience while asset payloads were JSON; removing JSON made every structured
value a row, so a hand-built file means keeping `recordType`, `recordId`, and `parentRecordId`
consistent across dozens of rows. The work belongs to duos-ui rather than Consent, so it needs a
ticket in that repo's scope before the import path is considered usable — DT-3869 covers this
contract only.

## Study field mapping

Wire paths are relative to `StudyRegistrationRequest`. Conditional requirements reuse Consent's
existing `StudyRegistrationRequestValidator` rules.

| CSV `field` | Wire target | Type | Requirement / existing rule |
| --- | --- | --- | --- |
| `studyName` | `studyName` | String | Required and non-blank. |
| `studyType` | `studyType` | Enum | Optional; exact `StudyType` wire value. |
| `studyDescription` | `studyDescription` | String | Required and non-blank. |
| `dataTypes` | `dataTypes` | String array | Required and non-empty. |
| `phenotypeIndication` | `phenotypeIndication` | String | Optional. |
| `species` | `species` | String | Optional. |
| `piName` | `piName` | String | Required and non-blank. |
| `piEmail` | `piEmail` | String | Optional; must be a valid email when present. |
| `dataCustodianEmail` | `dataCustodianEmail` | String array | Optional; every item must be a valid email. |
| `publicVisibility` | `publicVisibility` | Boolean | Required. |
| `throughBioId` | `throughBioId` | String | Optional. |
| `nihAnvilUse` | `nihAnvilUse` | Enum | Required; exact `NihAnvilUse` wire value. |
| `submittingToAnvil` | `submittingToAnvil` | Boolean | Optional. |
| `dbGaPPhsID` | `dbGaPPhsID` | String | Required when `nihAnvilUse` states that an existing dbGaP PHS ID is available. |
| `dbGaPStudyRegistrationName` | `dbGaPStudyRegistrationName` | String | Optional. |
| `embargoReleaseDate` | `embargoReleaseDate` | Date | Optional; valid ISO local date when present. |
| `sequencingCenter` | `sequencingCenter` | String | Optional. |
| `piInstitution` | `piInstitution` | Integer | Required for NHGRI-funded or AnVIL-seeking `nihAnvilUse` choices; an existing DUOS institution ID. |
| `nihGrantContractNumber` | `nihGrantContractNumber` | String | Required for NHGRI-funded or AnVIL-seeking `nihAnvilUse` choices. |
| `nihICsSupportingStudy` | `nihICsSupportingStudy` | Enum array | Optional; exact NIH institute/center abbreviations. |
| `nihProgramOfficerName` | `nihProgramOfficerName` | String | Optional. |
| `nihInstitutionCenterSubmission` | `nihInstitutionCenterSubmission` | Enum | Optional; exact NIH institute/center abbreviation. |
| `nihGenomicProgramAdministratorName` | `nihGenomicProgramAdministratorName` | String | Optional. |
| `multiCenterStudy` | `multiCenterStudy` | Boolean | Optional. |
| `collaboratingSites` | `collaboratingSites` | String array | Optional. |
| `controlledAccessRequiredForGenomicSummaryResultsGSR` | Same name | Boolean | Optional. |
| `controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation` | Same name | String | Required when controlled GSR access is `true`. |

The following wire properties are deliberately not direct template fields:

| Wire property | Reason |
| --- | --- |
| `consentGroups` | Constructed from `consentGroup` records. |
| `assets` | Non-file assets are out of scope for v1. See below. |
| All `alternativeDataSharingPlan*` properties | The alternative sharing plan is file-backed and its file cannot be uploaded through the template. See below. |
| `data` | Opaque client metadata is not part of the stable v1 contract. |
| `externalIdentifier`, `externalIdentifierType` | Integration-owned identifiers are outside this user-authored template. |

The whole `alternativeDataSharingPlan*` group is excluded, not just
`alternativeDataSharingPlanFileName`. The remaining properties only describe a plan whose document
the template cannot carry, so importing them would populate a draft that still cannot be completed
without returning to the form for the file. A user who needs an alternative sharing plan fills that
section in on the populated draft, where the file upload lives, and the group is a candidate for a
later template version once file-bearing imports are supported. (Confirmed as the intended v1 scope
with @otchet-broad and @jlaw-codes.)

Non-file assets — models, workspaces, publications, presentations, clinical trials, funding, and
intellectual properties — are excluded from v1 for a different reason. Their payloads nest: a
publication carries an `authors` array of `Author` objects, a presentation carries a `presenter`
object, and a model carries a `maintainer` object. Expressing those without JSON needs encodings
this contract otherwise has no use for, such as dotted field paths or a second level of child
records, which is a poor trade in a template meant to be filled in by hand. Assets are optional,
additive metadata: excluding them leaves nothing blocked, since a user can add them on the populated
draft the same way they do today. They are the first candidate for v2, where they can be designed as
records rather than retrofitted. (Product preference for no JSON confirmed by @jlaw-codes.)

NIH institutional certification files are also excluded. They are not fields on the canonical wire
request and must be uploaded on the populated draft form.

## Consent-group field mapping

Wire paths are relative to one `consentGroups[]` item.

| CSV `field` | Wire target | Type | Requirement / existing rule |
| --- | --- | --- | --- |
| `consentGroupName` | `consentGroupName` | String | Required and non-blank; displayed as the dataset name. |
| `accessManagement` | `accessManagement` | Enum | Required by this template contract: `open`, `controlled`, or `external`. |
| `generalResearchUse` | `generalResearchUse` | Boolean | Primary data-use option. |
| `hmb` | `hmb` | Boolean | Primary data-use option. |
| `diseaseSpecificUse` | `diseaseSpecificUse` | String array | Primary data-use option; selected when non-empty. |
| `poa` | `poa` | Boolean | Primary data-use option. |
| `otherPrimary` | `otherPrimary` | String | Primary data-use option; selected when non-blank. |
| `nmds` | `nmds` | Boolean | Optional secondary restriction. |
| `gso` | `gso` | Boolean | Optional secondary restriction. |
| `pub` | `pub` | Boolean | Optional secondary restriction. |
| `col` | `col` | Boolean | Optional secondary restriction. |
| `irb` | `irb` | Boolean | Optional secondary restriction. |
| `gs` | `gs` | String | Optional geographic restriction. |
| `mor` | `mor` | Boolean | Optional publication-moratorium flag. |
| `morDate` | `morDate` | Date | Optional; valid ISO local date when present. |
| `npu` | `npu` | Boolean | Optional non-profit-use restriction. |
| `otherSecondary` | `otherSecondary` | String | Optional secondary restriction. |
| `dataAccessCommitteeId` | `dataAccessCommitteeId` | Integer | Required for `controlled`; an existing DUOS DAC ID. Not required for `open` or `external`. |
| `dataLocation` | `dataLocation` | Enum | Optional; exact `DataLocation` wire value. |
| `url` | `url` | URI | Optional absolute HTTP(S) data URL. |
| `requestLocation` | `requestLocation` | URI | Optional absolute HTTP(S) external-request URL. |
| `numberOfParticipants` | `numberOfParticipants` | Integer | Required. |
| `fileTypes` | `fileTypes` | Records, not a field | Optional. Never appears as a `field` on a `consentGroup` row; supplied as `fileType` records that name this consent group as their parent. A `fileTypes` row on a `consentGroup` record is an unknown field and is rejected. See below. |

Primary data-use rules are unchanged: `open` requires no primary data use; `controlled` and
`external` require exactly one of general research use, HMB, disease-specific use, POA, or other.
`datasetId` is server-assigned, and the opaque consent-group `data` property is unsupported.

## File-type mapping

`fileTypes` is the only array of objects on the wire, so v1 expresses it as a record type rather
than as a JSON cell. Each
`fileType` record contributes one `fileTypes[]` item to the consent group named by its
`parentRecordId`. These describe data formats; they are not uploads.

| CSV `field` | Wire target | Type | Requirement / existing rule |
| --- | --- | --- | --- |
| `fileType` | `fileType` | Enum | Required; exact `FileType` wire value. |
| `functionalEquivalence` | `functionalEquivalence` | String | Optional. |

```csv
1,consentGroup,dataset-controlled,study,consentGroupName,Synthetic Controlled Dataset
1,fileType,ft-genome,dataset-controlled,fileType,Genome
1,fileType,ft-genome,dataset-controlled,functionalEquivalence,Synthetic reference build
```

A `fileType` record whose `parentRecordId` does not name a `consentGroup` record in the same file is
an orphan and is rejected. Biospecimens are excluded because the current study form does not allow
users to add them, and no file-storage object or file-content asset is supported.

## Invalid-input behavior

Validation collects independently actionable errors in deterministic order: file/structure errors,
then row conversion errors in file order, then existing DTO/domain violations in validator order.
Return at most 100 errors and report explicitly if additional errors were omitted.

File and structural errors stop validation before DTO construction because the record model is not
trustworthy. Once structure is valid, conversion and DTO/domain validation may both report errors.
A field with a conversion error is omitted from the DTO, but its derivative required or conditional
DTO violation is suppressed; the conversion error is the actionable error for that field. Independent
DTO/domain violations are still reported. This prevents duplicate or misleading errors while
preserving aggregation across otherwise independent fields.

| Condition | Behavior |
| --- | --- |
| Empty or record-free file | Reject; no draft is created. |
| File larger than 5 MiB | Reject before parsing; no draft is created. |
| Malformed CSV or invalid UTF-8 | Reject with location when known. |
| Missing, unknown, reordered, or duplicate header | Reject. |
| Non-comma delimiter detected in the header | Reject, naming the detected character and asking for a comma-delimited re-export. |
| Missing, unsupported, or mixed version | Reject affected rows; only major version `1` is supported. |
| Unknown record type or field | Reject the row. |
| Duplicate `(recordType, recordId, field)` on a single-valued field | Reject the later assignment. |
| Repeated item value within one scalar array field | Reject the later row. |
| Missing parent, multiple study records, or orphan record | Reject the affected record. |
| Empty required value, empty scalar array item, or invalid scalar | Reject with row and column context. |
| Existing registration-rule violation | Reject with the existing validator message; row and column may be absent. |

Errors and general logs must not echo raw rows or sensitive free-text values.

## Canonical fixtures

Fixtures live under `src/test/resources/fixtures/study-template/v1`:

- `valid/minimal-valid.csv`
- `valid/multi-consent-group-valid.csv`
- `valid/excel-export-valid.csv` — the minimal study as Excel "CSV UTF-8" writes it: BOM and CRLF
- `invalid/empty-file.csv`
- `invalid/duplicate-header.csv`
- `invalid/duplicate-field.csv`
- `invalid/semicolon-delimited.csv` — a European-locale Excel export
- `invalid/duplicate-array-item.csv`
- `invalid/orphan-file-type.csv`
- `invalid/unknown-field.csv`
- `invalid/unsupported-version.csv`
- `invalid/field-values.csv`

Every invalid CSV has a sibling `.errors.json` file containing the expected structured errors. All
values are synthetic and contain no production study, participant, user, institution, or DAC data.

## Validation error contract

Expected CSV and registration errors are returned as a completed validation result, not as thrown
request failures:

```ts
interface TemplateValidationError {
  row?: number
  column?: string
  message: string
}

type TemplateValidationResponse =
  | { valid: false, errors: TemplateValidationError[] }
  | {
      valid: true
      errors: []
      draft: {
        id: string
        draftType: 'StudyDatasetSubmissionV1'
      }
    }
```

`row` is one-based and counts the CSV header as row 1. Parser and conversion errors include row and
column when known. Violations from the existing registration validator may contain only a message;
the UI must not infer locations by parsing that message.

Authentication, authorization, multipart, size-limit, network, and unexpected server failures are
request failures and must be rendered separately from `valid: false` results.

## Draft hydration boundary

On success, route with the exact returned draft UUID and require
`draftType === 'StudyDatasetSubmissionV1'`. The loaded document has the
`StudyRegistrationRequest` wire shape described by this contract. Hydration must:

- populate supported study fields, consent groups, and their file types;
- leave file upload controls empty and asset sections untouched;
- allow review and edits through the existing study registration form; and
- submit through the existing study-creation path.

The CSV contract's `recordId` values are template grouping keys and do not reach the wire. Numeric
study and dataset IDs remain server-owned.

## Approval gate

Product and Consent API owners must approve the canonical contract before dependent parser and
endpoint work merges. Approval is tracked in DT-3869 and its pull requests, not in this consumer
summary.
