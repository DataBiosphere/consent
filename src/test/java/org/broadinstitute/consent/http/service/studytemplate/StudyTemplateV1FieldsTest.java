package org.broadinstitute.consent.http.service.studytemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.DataLocation;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihInstitutionCenterSubmission;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.StudyType;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject;
import org.broadinstitute.consent.http.models.dataset_registration_v1.NihICsSupportingStudy;
import org.broadinstitute.consent.http.models.dto.registration.ConsentGroupRequest;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequest;
import org.broadinstitute.consent.http.models.dto.registration.template.StudyTemplateValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Pins the whole v1 field catalogue against one template that exercises every supported field. The
 * expectations are keyed by CSV field name and checked for completeness, so adding a field to
 * {@link StudyTemplateV1Fields} without mapping it here fails rather than going quietly untested.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StudyTemplateV1FieldsTest {

  /** The consent group carrying general research use and every optional consent-group field. */
  private static final String PRIMARY_GROUP = "cg-primary";

  private StudyRegistrationRequest registration;

  @BeforeEach
  void mapEveryField() {
    StudyTemplateValidationResult result =
        new StudyTemplateValidationService()
            .validate(
                new ByteArrayInputStream(everyFieldTemplate().getBytes(StandardCharsets.UTF_8)));

    assertEquals(List.of(), result.errors(), "the every-field template must be valid");
    registration = result.registration();
    assertNotNull(registration);
  }

  @Test
  void testStudyExpectationsCoverTheCatalogue() {
    assertEquals(StudyTemplateV1Fields.STUDY.keySet(), expectedStudyValues().keySet());
  }

  @Test
  void testConsentGroupExpectationsCoverTheCatalogue() {
    assertEquals(
        StudyTemplateV1Fields.CONSENT_GROUP.keySet(), expectedConsentGroupValues().keySet());
  }

  @Test
  void testFileTypeExpectationsCoverTheCatalogue() {
    assertEquals(StudyTemplateV1Fields.FILE_TYPES.keySet(), expectedFileTypeValues().keySet());
  }

  @ParameterizedTest(name = "study.{0}")
  @MethodSource("studyFields")
  void testStudyFieldMapsToItsWireProperty(String field, Object expected) {
    assertEquals(expected, StudyTemplateV1Fields.STUDY.get(field).reader().apply(registration));
  }

  @ParameterizedTest(name = "consentGroup.{0}")
  @MethodSource("consentGroupFields")
  void testConsentGroupFieldMapsToItsWireProperty(String field, Object expected) {
    ConsentGroupRequest group = groupWith(field);
    assertEquals(
        expected, StudyTemplateV1Fields.CONSENT_GROUP.get(field).reader().apply(group), field);
  }

  @ParameterizedTest(name = "fileType.{0}")
  @MethodSource("fileTypeFields")
  void testFileTypeFieldMapsToItsWireProperty(String field, Object expected) {
    FileTypeObject fileType = group(PRIMARY_GROUP).getFileTypes().getFirst();
    assertEquals(expected, StudyTemplateV1Fields.FILE_TYPES.get(field).reader().apply(fileType));
  }

  private List<Arguments> studyFields() {
    return arguments(expectedStudyValues());
  }

  private List<Arguments> consentGroupFields() {
    return arguments(expectedConsentGroupValues());
  }

  private List<Arguments> fileTypeFields() {
    return arguments(expectedFileTypeValues());
  }

  private static List<Arguments> arguments(Map<String, Object> expected) {
    return expected.entrySet().stream()
        .map(entry -> Arguments.of(entry.getKey(), entry.getValue()))
        .toList();
  }

  /**
   * The primary data-use options are mutually exclusive, so each one lives on its own consent
   * group. Every other consent-group field is on {@link #PRIMARY_GROUP}.
   */
  private ConsentGroupRequest groupWith(String field) {
    return switch (field) {
      case "hmb" -> group("cg-hmb");
      case "diseaseSpecificUse" -> group("cg-ds");
      case "poa" -> group("cg-poa");
      case "otherPrimary" -> group("cg-other");
      default -> group(PRIMARY_GROUP);
    };
  }

  private ConsentGroupRequest group(String recordId) {
    int index =
        switch (recordId) {
          case PRIMARY_GROUP -> 0;
          case "cg-hmb" -> 1;
          case "cg-ds" -> 2;
          case "cg-poa" -> 3;
          default -> 4;
        };
    return registration.getConsentGroups().get(index);
  }

  private static Map<String, Object> expectedStudyValues() {
    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("studyName", "Sample Study");
    expected.put("studyType", StudyType.OBSERVATIONAL);
    expected.put("studyDescription", "Sample description");
    expected.put("dataTypes", List.of("Genomic", "Phenotypic"));
    expected.put("phenotypeIndication", "Sample indication");
    expected.put("species", "Homo sapiens");
    expected.put("embargoReleaseDate", "2030-01-01");
    expected.put("sequencingCenter", "Sample Center");
    expected.put("piName", "Sample Investigator");
    expected.put("piEmail", "pi@example.org");
    expected.put("dataCustodianEmail", List.of("custodian@example.org"));
    expected.put("piInstitution", 1);
    expected.put("throughBioId", "bio-1");
    expected.put("nihAnvilUse", NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    expected.put("submittingToAnvil", Boolean.TRUE);
    expected.put("dbGaPPhsID", "phs000000");
    expected.put("dbGaPStudyRegistrationName", "Sample Registration");
    expected.put("nihGrantContractNumber", "HG-000000");
    expected.put("nihICsSupportingStudy", List.of(NihICsSupportingStudy.NHGRI));
    expected.put("nihProgramOfficerName", "Sample Officer");
    expected.put("nihInstitutionCenterSubmission", NihInstitutionCenterSubmission.NHGRI);
    expected.put("nihGenomicProgramAdministratorName", "Sample Administrator");
    expected.put("publicVisibility", Boolean.TRUE);
    expected.put("multiCenterStudy", Boolean.TRUE);
    expected.put("collaboratingSites", List.of("Sample Site"));
    expected.put("controlledAccessRequiredForGenomicSummaryResultsGSR", Boolean.TRUE);
    expected.put(
        "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation",
        "Sample explanation");
    return expected;
  }

  private static Map<String, Object> expectedConsentGroupValues() {
    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("consentGroupName", "Sample Primary Dataset");
    expected.put("accessManagement", AccessManagement.CONTROLLED);
    expected.put("generalResearchUse", Boolean.TRUE);
    expected.put("hmb", Boolean.TRUE);
    expected.put("diseaseSpecificUse", List.of("Sample condition"));
    expected.put("poa", Boolean.TRUE);
    expected.put("otherPrimary", "Sample primary");
    expected.put("nmds", Boolean.TRUE);
    expected.put("gso", Boolean.TRUE);
    expected.put("pub", Boolean.TRUE);
    expected.put("col", Boolean.TRUE);
    expected.put("irb", Boolean.TRUE);
    expected.put("gs", "Sample geography");
    expected.put("mor", Boolean.TRUE);
    expected.put("morDate", "2030-01-01");
    expected.put("npu", Boolean.TRUE);
    expected.put("otherSecondary", "Sample secondary");
    expected.put("dataAccessCommitteeId", 1);
    expected.put("dataLocation", DataLocation.TERRA_WORKSPACE);
    expected.put("url", URI.create("https://example.org/data"));
    expected.put("requestLocation", URI.create("https://example.org/request"));
    expected.put("numberOfParticipants", 1);
    return expected;
  }

  private static Map<String, Object> expectedFileTypeValues() {
    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("fileType", FileTypeObject.FileType.GENOME);
    expected.put("functionalEquivalence", "Sample equivalence");
    return expected;
  }

  /**
   * A valid template that assigns every supported field. The NHGRI-funded-with-PHS-ID choice is
   * used because it is the one that makes the conditional dbGaP, institution, and grant fields
   * required, so they can be set without contradicting the validator.
   */
  private static String everyFieldTemplate() {
    return String.join(
        "\n",
        "templateVersion,recordType,recordId,parentRecordId,field,value",
        "1,study,study,,studyName,Sample Study",
        "1,study,study,,studyType,Observational",
        "1,study,study,,studyDescription,Sample description",
        "1,study,study,,dataTypes,Genomic",
        "1,study,study,,dataTypes,Phenotypic",
        "1,study,study,,phenotypeIndication,Sample indication",
        "1,study,study,,species,Homo sapiens",
        "1,study,study,,embargoReleaseDate,2030-01-01",
        "1,study,study,,sequencingCenter,Sample Center",
        "1,study,study,,piName,Sample Investigator",
        "1,study,study,,piEmail,pi@example.org",
        "1,study,study,,dataCustodianEmail,custodian@example.org",
        "1,study,study,,piInstitution,1",
        "1,study,study,,throughBioId,bio-1",
        "1,study,study,,nihAnvilUse,I am NHGRI funded and I have a dbGaP PHS ID already",
        "1,study,study,,submittingToAnvil,true",
        "1,study,study,,dbGaPPhsID,phs000000",
        "1,study,study,,dbGaPStudyRegistrationName,Sample Registration",
        "1,study,study,,nihGrantContractNumber,HG-000000",
        "1,study,study,,nihICsSupportingStudy,NHGRI",
        "1,study,study,,nihProgramOfficerName,Sample Officer",
        "1,study,study,,nihInstitutionCenterSubmission,NHGRI",
        "1,study,study,,nihGenomicProgramAdministratorName,Sample Administrator",
        "1,study,study,,publicVisibility,true",
        "1,study,study,,multiCenterStudy,true",
        "1,study,study,,collaboratingSites,Sample Site",
        "1,study,study,,controlledAccessRequiredForGenomicSummaryResultsGSR,true",
        "1,study,study,,controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation,"
            + "Sample explanation",
        "1,consentGroup,cg-primary,study,consentGroupName,Sample Primary Dataset",
        "1,consentGroup,cg-primary,study,accessManagement,controlled",
        "1,consentGroup,cg-primary,study,generalResearchUse,true",
        "1,consentGroup,cg-primary,study,nmds,true",
        "1,consentGroup,cg-primary,study,gso,true",
        "1,consentGroup,cg-primary,study,pub,true",
        "1,consentGroup,cg-primary,study,col,true",
        "1,consentGroup,cg-primary,study,irb,true",
        "1,consentGroup,cg-primary,study,gs,Sample geography",
        "1,consentGroup,cg-primary,study,mor,true",
        "1,consentGroup,cg-primary,study,morDate,2030-01-01",
        "1,consentGroup,cg-primary,study,npu,true",
        "1,consentGroup,cg-primary,study,otherSecondary,Sample secondary",
        "1,consentGroup,cg-primary,study,dataAccessCommitteeId,1",
        "1,consentGroup,cg-primary,study,dataLocation,Terra Workspace",
        "1,consentGroup,cg-primary,study,url,https://example.org/data",
        "1,consentGroup,cg-primary,study,requestLocation,https://example.org/request",
        "1,consentGroup,cg-primary,study,numberOfParticipants,1",
        "1,fileType,ft-genome,cg-primary,fileType,Genome",
        "1,fileType,ft-genome,cg-primary,functionalEquivalence,Sample equivalence",
        "1,consentGroup,cg-hmb,study,consentGroupName,Sample HMB Dataset",
        "1,consentGroup,cg-hmb,study,accessManagement,controlled",
        "1,consentGroup,cg-hmb,study,hmb,true",
        "1,consentGroup,cg-hmb,study,dataAccessCommitteeId,1",
        "1,consentGroup,cg-hmb,study,numberOfParticipants,2",
        "1,consentGroup,cg-ds,study,consentGroupName,Sample Disease Dataset",
        "1,consentGroup,cg-ds,study,accessManagement,controlled",
        "1,consentGroup,cg-ds,study,diseaseSpecificUse,Sample condition",
        "1,consentGroup,cg-ds,study,dataAccessCommitteeId,1",
        "1,consentGroup,cg-ds,study,numberOfParticipants,3",
        "1,consentGroup,cg-poa,study,consentGroupName,Sample POA Dataset",
        "1,consentGroup,cg-poa,study,accessManagement,controlled",
        "1,consentGroup,cg-poa,study,poa,true",
        "1,consentGroup,cg-poa,study,dataAccessCommitteeId,1",
        "1,consentGroup,cg-poa,study,numberOfParticipants,4",
        "1,consentGroup,cg-other,study,consentGroupName,Sample Other Dataset",
        "1,consentGroup,cg-other,study,accessManagement,external",
        "1,consentGroup,cg-other,study,otherPrimary,Sample primary",
        "1,consentGroup,cg-other,study,numberOfParticipants,5");
  }
}
