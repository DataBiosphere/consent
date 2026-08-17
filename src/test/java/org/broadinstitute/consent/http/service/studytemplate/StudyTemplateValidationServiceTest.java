package org.broadinstitute.consent.http.service.studytemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.DataLocation;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.StudyType;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject.FileType;
import org.broadinstitute.consent.http.models.dto.registration.ConsentGroupRequest;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequest;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequestValidator;
import org.broadinstitute.consent.http.models.dto.registration.template.StudyTemplateValidationResult;
import org.broadinstitute.consent.http.models.dto.registration.template.TemplateValidationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class StudyTemplateValidationServiceTest {

  private static final String FIXTURE_ROOT = "fixtures/study-template/v1/";
  private static final String HEADER =
      "templateVersion,recordType,recordId,parentRecordId,field,value";
  private static final String NOT_NHGRI_FUNDED =
      "I am not NHGRI funded and do not plan to store data in AnVIL";

  /** The row an appended perturbation lands on, given the ten rows of {@link #minimalTemplate}. */
  private static final int APPENDED_ROW = 11;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final StudyTemplateValidationService service = new StudyTemplateValidationService();

  // ── Canonical fixtures ───────────────────────────────────────────────────

  @ParameterizedTest
  @ValueSource(
      strings = {"minimal-valid.csv", "multi-consent-group-valid.csv", "excel-export-valid.csv"})
  void testValidate_validFixture(String fixture) throws IOException {
    StudyTemplateValidationResult result = validateFixture("valid/" + fixture);

    assertEquals(List.of(), result.errors(), fixture);
    assertTrue(result.valid(), fixture);
    assertFalse(result.truncated(), fixture);
    assertNotNull(result.registration(), fixture);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "duplicate-array-item.csv",
        "duplicate-field.csv",
        "duplicate-header.csv",
        "empty-file.csv",
        "field-values.csv",
        "orphan-file-type.csv",
        "semicolon-delimited.csv",
        "unknown-field.csv",
        "unsupported-version.csv"
      })
  void testValidate_invalidFixtureMatchesExpectedErrors(String fixture) throws IOException {
    StudyTemplateValidationResult result = validateFixture("invalid/" + fixture);

    JsonNode expected =
        OBJECT_MAPPER.readTree(readFixture("invalid/" + fixture.replace(".csv", ".errors.json")));
    assertEquals(expected, OBJECT_MAPPER.valueToTree(result.errors()), fixture);
    assertFalse(result.valid(), fixture);
    assertNull(result.registration(), fixture);
  }

  @Test
  void testValidate_minimalFixtureMapsDeterministically() throws IOException {
    StudyRegistrationRequest registration =
        validateFixture("valid/minimal-valid.csv").registration();

    assertEquals("Synthetic Minimal Study", registration.getStudyName());
    assertEquals(
        "A synthetic study used only for contract tests.", registration.getStudyDescription());
    assertEquals(List.of("Genomic"), registration.getDataTypes());
    assertEquals(Boolean.TRUE, registration.getPublicVisibility());
    assertEquals(
        NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_AND_DO_NOT_PLAN_TO_STORE_DATA_IN_AN_VIL,
        registration.getNihAnvilUse());
    assertEquals("Synthetic Investigator", registration.getPiName());
    assertNull(registration.getStudyType());
    assertNull(registration.getAssets());

    assertEquals(1, registration.getConsentGroups().size());
    ConsentGroupRequest consentGroup = registration.getConsentGroups().getFirst();
    assertEquals("Synthetic Open Dataset", consentGroup.getConsentGroupName());
    assertEquals(AccessManagement.OPEN, consentGroup.getAccessManagement());
    assertEquals(10, consentGroup.getNumberOfParticipants().intValue());
    assertNull(consentGroup.getFileTypes());
  }

  @Test
  void testValidate_multiConsentGroupFixtureMapsDeterministically() throws IOException {
    StudyRegistrationRequest registration =
        validateFixture("valid/multi-consent-group-valid.csv").registration();

    assertEquals(StudyType.OBSERVATIONAL, registration.getStudyType());
    assertEquals(List.of("Genomic", "Phenotypic"), registration.getDataTypes());
    assertEquals(
        List.of("custodian.one@example.org", "custodian.two@example.org"),
        registration.getDataCustodianEmail());
    assertEquals(
        List.of("Synthetic Site One", "Synthetic Site Two"), registration.getCollaboratingSites());
    assertEquals(Boolean.FALSE, registration.getPublicVisibility());
    assertEquals(
        "A synthetic study with multiple datasets, quoted values, and non-file assets.",
        registration.getStudyDescription());

    assertEquals(2, registration.getConsentGroups().size());
    ConsentGroupRequest open = registration.getConsentGroups().getFirst();
    assertEquals(AccessManagement.OPEN, open.getAccessManagement());
    assertEquals(DataLocation.TERRA_WORKSPACE, open.getDataLocation());
    assertEquals(URI.create("https://example.org/data/synthetic-open"), open.getUrl());
    assertNull(open.getFileTypes());

    ConsentGroupRequest controlled = registration.getConsentGroups().get(1);
    assertEquals(AccessManagement.CONTROLLED, controlled.getAccessManagement());
    assertEquals(1, controlled.getDataAccessCommitteeId().intValue());
    assertEquals(Boolean.TRUE, controlled.getGeneralResearchUse());
    assertEquals(2, controlled.getFileTypes().size());
    assertEquals(FileType.GENOME, controlled.getFileTypes().getFirst().getFileType());
    assertEquals(
        "Synthetic reference build",
        controlled.getFileTypes().getFirst().getFunctionalEquivalence());
    assertEquals(FileType.PHENOTYPE, controlled.getFileTypes().get(1).getFileType());
    assertNull(controlled.getFileTypes().get(1).getFunctionalEquivalence());
  }

  @Test
  void testValidate_excelExportMatchesTheMinimalFixture() throws IOException {
    StudyRegistrationRequest excel = validateFixture("valid/excel-export-valid.csv").registration();
    StudyRegistrationRequest plain = validateFixture("valid/minimal-valid.csv").registration();

    assertEquals(OBJECT_MAPPER.valueToTree(plain), OBJECT_MAPPER.valueToTree(excel));
  }

  // ── CSV dialect ──────────────────────────────────────────────────────────

  private static Stream<Arguments> recordSeparators() {
    return Stream.of(Arguments.of("LF", "\n"), Arguments.of("CRLF", "\r\n"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("recordSeparators")
  void testValidate_acceptsBothRecordSeparators(String label, String separator) {
    StudyTemplateValidationResult result = validate(minimalTemplate(separator));

    assertEquals(List.of(), result.errors(), label);
  }

  @Test
  void testValidate_acceptsLeadingByteOrderMark() {
    StudyTemplateValidationResult result = validate("﻿" + minimalTemplate("\n"));

    assertEquals(List.of(), result.errors());
  }

  @Test
  void testValidate_acceptsQuotedDelimitersAndEscapedQuotes() {
    StudyTemplateValidationResult result =
        validate(
            minimalTemplate("\n")
                .replace(
                    "1,study,study,,studyDescription,A synthetic study used only for contract tests.",
                    "1,study,study,,studyDescription,\"A synthetic, \"\"quoted\"\" study\""));

    assertEquals(List.of(), result.errors());
    assertEquals("A synthetic, \"quoted\" study", result.registration().getStudyDescription());
  }

  @Test
  void testValidate_preservesSignificantWhitespaceInValues() {
    StudyTemplateValidationResult result =
        validate(minimalTemplate("\n") + "\n1,study,study,,species, Homo sapiens ");

    assertEquals(List.of(), result.errors());
    assertEquals(" Homo sapiens ", result.registration().getSpecies());
  }

  @Test
  void testValidate_ignoresBlankLines() {
    StudyTemplateValidationResult result = validate(minimalTemplate("\n") + "\n\n");

    assertEquals(List.of(), result.errors());
  }

  @Test
  void testValidate_rejectsTabDelimitedHeader() {
    StudyTemplateValidationResult result = validate(HEADER.replace(',', '\t') + "\n");

    assertEquals(1, result.errors().size());
    assertEquals(1, result.errors().getFirst().row().intValue());
    assertNull(result.errors().getFirst().column());
    assertTrue(
        result.errors().getFirst().message().contains("Detected '\t' as the column separator"),
        result.errors().toString());
  }

  @Test
  void testValidate_rejectsReorderedHeader() {
    StudyTemplateValidationResult result =
        validate("recordType,templateVersion,recordId,parentRecordId,field,value\n");

    assertEquals(
        List.of(
            TemplateValidationError.at(
                1,
                "Template header must be exactly:"
                    + " templateVersion,recordType,recordId,parentRecordId,field,value")),
        result.errors());
  }

  @Test
  void testValidate_rejectsUnknownHeaderColumn() {
    StudyTemplateValidationResult result = validate(HEADER + ",notes\n");

    assertEquals(
        List.of(
            TemplateValidationError.at(
                1,
                "Template header must be exactly:"
                    + " templateVersion,recordType,recordId,parentRecordId,field,value")),
        result.errors());
  }

  @Test
  void testValidate_rejectsMalformedCsv() {
    StudyTemplateValidationResult result =
        validate(HEADER + "\n1,study,study,,studyName,\"unterminated\n");

    assertEquals(
        List.of(TemplateValidationError.of("Template file is not valid CSV")), result.errors());
  }

  @Test
  void testValidate_rejectsInvalidUtf8() {
    byte[] invalid = {(byte) 0xC3, (byte) 0x28};

    StudyTemplateValidationResult result = service.validate(new ByteArrayInputStream(invalid));

    assertEquals(
        List.of(TemplateValidationError.of("Template file must be UTF-8 encoded")),
        result.errors());
  }

  @Test
  void testValidate_rejectsHeaderOnlyFile() {
    StudyTemplateValidationResult result = validate(HEADER + "\n");

    assertEquals(
        List.of(TemplateValidationError.of("Template file has no data rows")), result.errors());
  }

  @Test
  void testValidate_rejectsFileOverTheSizeLimit() {
    byte[] oversized = new byte[StudyTemplateValidationService.MAX_TEMPLATE_BYTES + 1];

    StudyTemplateValidationResult result = service.validate(new ByteArrayInputStream(oversized));

    assertEquals(
        List.of(TemplateValidationError.of("Template file must be no larger than 5 MiB")),
        result.errors());
  }

  // ── Scalar conversion ────────────────────────────────────────────────────

  @ParameterizedTest
  @CsvSource({
    "submittingToAnvil,yes,submittingToAnvil must be true or false",
    "submittingToAnvil,TRUE,submittingToAnvil must be true or false",
    "piInstitution,1.5,piInstitution must be a whole number",
    "piInstitution,one,piInstitution must be a whole number",
    "studyType,observational,Unknown studyType value: observational",
    "nihICsSupportingStudy,NOPE,Unknown nihICsSupportingStudy value: NOPE"
  })
  void testValidate_rejectsInvalidStudyScalar(String field, String value, String message) {
    StudyTemplateValidationResult result =
        validate(minimalTemplate("\n") + "\n1,study,study,," + field + ',' + value);

    assertTrue(
        result.errors().contains(TemplateValidationError.at(APPENDED_ROW, "value", message)),
        result.errors().toString());
  }

  @ParameterizedTest
  @CsvSource({
    "url,example.org,url must be an absolute http or https URL",
    "url,ftp://example.org/data,url must be an absolute http or https URL",
    "requestLocation,/relative,requestLocation must be an absolute http or https URL",
    "dataLocation,terra workspace,Unknown dataLocation value: terra workspace",
    "dataAccessCommitteeId,many,dataAccessCommitteeId must be a whole number",
    "mor,1,mor must be true or false"
  })
  void testValidate_rejectsInvalidConsentGroupScalar(String field, String value, String message) {
    StudyTemplateValidationResult result =
        validate(
            minimalTemplate("\n") + "\n1,consentGroup,dataset-open,study," + field + ',' + value);

    assertTrue(
        result.errors().contains(TemplateValidationError.at(APPENDED_ROW, "value", message)),
        result.errors().toString());
  }

  @Test
  void testValidate_acceptsRepeatedRowsAsAnArrayInFileOrder() {
    StudyTemplateValidationResult result =
        validate(
            minimalTemplate("\n")
                + "\n1,study,study,,dataTypes,Phenotypic"
                + "\n1,study,study,,dataTypes,Clinical");

    assertEquals(List.of(), result.errors());
    assertEquals(
        List.of("Genomic", "Phenotypic", "Clinical"), result.registration().getDataTypes());
  }

  // ── Record model ─────────────────────────────────────────────────────────

  @Test
  void testValidate_rejectsUnknownRecordType() {
    StudyTemplateValidationResult result =
        validate(minimalTemplate("\n") + "\n1,dataset,dataset-open,study,consentGroupName,Nope");

    assertEquals(
        List.of(
            TemplateValidationError.at(APPENDED_ROW, "recordType", "Unknown record type: dataset")),
        result.errors());
  }

  @Test
  void testValidate_rejectsFileBackedFieldsAsUnsupported() {
    StudyTemplateValidationResult result =
        validate(
            minimalTemplate("\n")
                + "\n1,study,study,,alternativeDataSharingPlanFileName,plan.pdf"
                + "\n1,study,study,,nihInstitutionalCertificationFile,cert.pdf");

    assertEquals(
        List.of(
            TemplateValidationError.at(
                APPENDED_ROW,
                "field",
                "Unsupported study field: alternativeDataSharingPlanFileName. It must be set on the"
                    + " draft form after import."),
            TemplateValidationError.at(
                APPENDED_ROW + 1,
                "field",
                "Unsupported study field: nihInstitutionalCertificationFile. It must be set on the"
                    + " draft form after import.")),
        result.errors());
  }

  @Test
  void testValidate_rejectsFileTypesAsAConsentGroupField() {
    StudyTemplateValidationResult result =
        validate(minimalTemplate("\n") + "\n1,consentGroup,dataset-open,study,fileTypes,Genome");

    assertEquals(
        List.of(
            TemplateValidationError.at(
                APPENDED_ROW, "field", "Unknown consentGroup field: fileTypes")),
        result.errors());
  }

  @Test
  void testValidate_attachesFileTypesToTheNamedConsentGroup() {
    StudyTemplateValidationResult result =
        validate(
            minimalTemplate("\n")
                + "\n1,consentGroup,dataset-two,study,consentGroupName,Second Dataset"
                + "\n1,consentGroup,dataset-two,study,accessManagement,open"
                + "\n1,consentGroup,dataset-two,study,numberOfParticipants,5"
                + "\n1,fileType,ft-one,dataset-open,fileType,Genome"
                + "\n1,fileType,ft-two,dataset-two,fileType,Exome");

    assertEquals(List.of(), result.errors());
    List<ConsentGroupRequest> consentGroups = result.registration().getConsentGroups();
    assertEquals(FileType.GENOME, consentGroups.getFirst().getFileTypes().getFirst().getFileType());
    assertEquals(FileType.EXOME, consentGroups.get(1).getFileTypes().getFirst().getFileType());
  }

  @Test
  void testValidate_rejectsFileTypeRecordWithoutAFileType() {
    StudyTemplateValidationResult result =
        validate(
            minimalTemplate("\n")
                + "\n1,fileType,ft-one,dataset-open,functionalEquivalence,Synthetic build");

    assertEquals(
        List.of(
            TemplateValidationError.at(
                APPENDED_ROW, "field", "fileType is required for fileType record 'ft-one'")),
        result.errors());
  }

  @Test
  void testValidate_rejectsConsentGroupWithoutAccessManagement() {
    StudyTemplateValidationResult result =
        validate(
            minimalTemplate("\n")
                .replace("1,consentGroup,dataset-open,study,accessManagement,open\n", ""));

    assertTrue(
        result.errors().stream()
            .anyMatch(
                error ->
                    error
                        .message()
                        .equals(
                            "accessManagement is required for consentGroup record 'dataset-open'")),
        result.errors().toString());
  }

  @Test
  void testValidate_rejectsStudyRecordWithAnUnexpectedRecordId() {
    StudyTemplateValidationResult result =
        validate(minimalTemplate("\n") + "\n1,study,other-study,,species,Homo sapiens");

    assertEquals(
        List.of(
            TemplateValidationError.at(
                APPENDED_ROW, "recordId", "Study records must use recordId 'study'")),
        result.errors());
  }

  @Test
  void testValidate_rejectsConsentGroupParentedOutsideTheStudy() {
    StudyTemplateValidationResult result =
        validate(
            minimalTemplate("\n")
                + "\n1,consentGroup,dataset-two,dataset-open,consentGroupName,Second Dataset");

    assertEquals(
        List.of(
            TemplateValidationError.at(
                APPENDED_ROW,
                "parentRecordId",
                "consentGroup record 'dataset-two' must set parentRecordId to 'study'")),
        result.errors());
  }

  // ── Version dispatch ─────────────────────────────────────────────────────

  @Test
  void testValidate_rejectsMissingVersion() {
    StudyTemplateValidationResult result =
        validate(HEADER + "\n,study,study,,studyName,Synthetic Study");

    assertEquals(
        List.of(TemplateValidationError.at(2, "templateVersion", "Template version is required")),
        result.errors());
  }

  @Test
  void testValidate_rejectsAnUnsupportedVersionOnASingleRow() {
    StudyTemplateValidationResult result =
        validate(minimalTemplate("\n") + "\n2,study,study,,species,Homo sapiens");

    assertEquals(
        List.of(
            TemplateValidationError.at(
                APPENDED_ROW, "templateVersion", "Unsupported template version: 2")),
        result.errors());
  }

  // ── Error aggregation, ordering, and the cap ─────────────────────────────

  @Test
  void testValidate_ordersLocatedErrorsByRowThenUnlocatedViolations() throws IOException {
    StudyTemplateValidationResult result = validateFixture("invalid/field-values.csv");

    assertEquals(
        List.of(2, 5, 6, 8, 9),
        result.errors().stream()
            .map(TemplateValidationError::row)
            .filter(Objects::nonNull)
            .toList());
    assertNull(result.errors().getLast().row());
  }

  @Test
  void testValidate_reportsEveryIndependentConversionError() {
    StudyTemplateValidationResult result =
        validate(
            minimalTemplate("\n")
                + "\n1,study,study,,submittingToAnvil,maybe"
                + "\n1,study,study,,multiCenterStudy,maybe");

    assertEquals(
        List.of(
            TemplateValidationError.at(
                APPENDED_ROW, "value", "submittingToAnvil must be true or false"),
            TemplateValidationError.at(
                APPENDED_ROW + 1, "value", "multiCenterStudy must be true or false")),
        result.errors());
  }

  @Test
  void testValidate_reportsTruncationOnceTheCapIsReached() {
    StringBuilder template = new StringBuilder(HEADER);
    IntStream.range(0, StudyTemplateValidationService.MAX_ERRORS + 5)
        .forEach(
            index ->
                template.append("\n1,study,study,,unknownField").append(index).append(",value"));

    StudyTemplateValidationResult result = validate(template.toString());

    assertTrue(result.truncated());
    assertEquals(StudyTemplateValidationService.MAX_ERRORS + 1, result.errors().size());
    assertEquals(
        "Only the first 100 errors are reported; 5 further errors were omitted",
        result.errors().getLast().message());
  }

  // ── Reuse of the registration validator ──────────────────────────────────

  @ParameterizedTest
  @ValueSource(
      strings = {"minimal-valid.csv", "multi-consent-group-valid.csv", "excel-export-valid.csv"})
  void testValidate_acceptedTemplateAlsoPassesDirectRegistrationValidation(String fixture)
      throws IOException {
    StudyRegistrationRequest registration = validateFixture("valid/" + fixture).registration();

    assertEquals(
        List.of(),
        new StudyRegistrationRequestValidator().collectViolations(registration),
        fixture);
  }

  @Test
  void testValidate_reportsTheSameBusinessViolationAsDirectRegistration() {
    StudyTemplateValidationResult result =
        validate(
            minimalTemplate("\n")
                    .replace(
                        "1,consentGroup,dataset-open,study,accessManagement,open",
                        "1,consentGroup,dataset-open,study,accessManagement,controlled")
                + "\n1,consentGroup,dataset-open,study,generalResearchUse,true");

    assertEquals(
        new StudyRegistrationRequestValidator().collectViolations(controlledWithoutDac()),
        result.errors().stream().map(TemplateValidationError::message).toList());
  }

  @Test
  void testValidate_attributesAValidatorViolationToTheRowThatCausedIt() {
    StudyTemplateValidationResult result =
        validate(minimalTemplate("\n").replace(",piName,Synthetic Investigator", ",piName,"));

    assertEquals(
        List.of(TemplateValidationError.at(7, "value", "Principal Investigator Name is required")),
        result.errors());
  }

  @Test
  void testValidate_attributesAListItemViolationToItsOwnRow() {
    StudyTemplateValidationResult result =
        validate(
            minimalTemplate("\n")
                + "\n1,study,study,,dataCustodianEmail,custodian@example.org"
                + "\n1,study,study,,dataCustodianEmail,not-an-email");

    assertEquals(
        List.of(
            TemplateValidationError.at(
                APPENDED_ROW + 1,
                "value",
                "Data Custodian Email is not a valid email address: not-an-email")),
        result.errors());
  }

  @Test
  void testValidate_leavesAViolationNoRowExplainsUnlocated() {
    StudyTemplateValidationResult result =
        validate(
            minimalTemplate("\n").replace("1,study,study,,piName,Synthetic Investigator\n", ""));

    assertEquals(
        List.of(TemplateValidationError.of("Principal Investigator Name is required")),
        result.errors());
  }

  @Test
  void testValidate_suppressesTheViolationDerivedFromAConversionError() {
    StudyTemplateValidationResult result =
        validate(minimalTemplate("\n").replace(",publicVisibility,true", ",publicVisibility,yes"));

    assertEquals(
        List.of(TemplateValidationError.at(5, "value", "publicVisibility must be true or false")),
        result.errors());
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  /** The minimal valid fixture, inline so tests can perturb or append single rows. */
  private static String minimalTemplate(String separator) {
    return String.join(
        separator,
        HEADER,
        "1,study,study,,studyName,Synthetic Minimal Study",
        "1,study,study,,studyDescription,A synthetic study used only for contract tests.",
        "1,study,study,,dataTypes,Genomic",
        "1,study,study,,publicVisibility,true",
        "1,study,study,,nihAnvilUse," + NOT_NHGRI_FUNDED,
        "1,study,study,,piName,Synthetic Investigator",
        "1,consentGroup,dataset-open,study,consentGroupName,Synthetic Open Dataset",
        "1,consentGroup,dataset-open,study,accessManagement,open",
        "1,consentGroup,dataset-open,study,numberOfParticipants,10");
  }

  /** The hand-built equivalent of the perturbed minimal template, for the regression comparison. */
  private static StudyRegistrationRequest controlledWithoutDac() {
    StudyRegistrationRequest registration = new StudyRegistrationRequest();
    registration.setStudyName("Synthetic Minimal Study");
    registration.setStudyDescription("A synthetic study used only for contract tests.");
    registration.setDataTypes(List.of("Genomic"));
    registration.setPublicVisibility(true);
    registration.setNihAnvilUse(
        NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_AND_DO_NOT_PLAN_TO_STORE_DATA_IN_AN_VIL);
    registration.setPiName("Synthetic Investigator");

    ConsentGroupRequest consentGroup = new ConsentGroupRequest();
    consentGroup.setConsentGroupName("Synthetic Open Dataset");
    consentGroup.setAccessManagement(AccessManagement.CONTROLLED);
    consentGroup.setGeneralResearchUse(true);
    consentGroup.setNumberOfParticipants(10);
    registration.setConsentGroups(List.of(consentGroup));
    return registration;
  }

  private StudyTemplateValidationResult validate(String csv) {
    return service.validate(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
  }

  private StudyTemplateValidationResult validateFixture(String relativePath) throws IOException {
    return service.validate(new ByteArrayInputStream(readFixture(relativePath)));
  }

  private static byte[] readFixture(String relativePath) throws IOException {
    try (InputStream input =
        StudyTemplateValidationServiceTest.class
            .getClassLoader()
            .getResourceAsStream(FIXTURE_ROOT + relativePath)) {
      assertNotNull(input, relativePath);
      return input.readAllBytes();
    }
  }
}
