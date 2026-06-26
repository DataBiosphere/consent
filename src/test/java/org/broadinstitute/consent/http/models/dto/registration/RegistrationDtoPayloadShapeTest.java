package org.broadinstitute.consent.http.models.dto.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.models.dataset_registration_v1.AlternativeDataSharingPlanReason;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject;
import org.broadinstitute.consent.http.models.dataset_registration_v1.NihICsSupportingStudy;
import org.junit.jupiter.api.Test;

/**
 * Verifies that StudyRegistrationRequest, StudyUpdateRequest, and ConsentGroupRequest share the
 * same external JSON payload shape as DatasetRegistrationSchemaV1 and ConsentGroup. Each test
 * serializes the existing model class and deserializes the resulting JSON into the new DTO,
 * asserting every mapped field round-trips correctly.
 */
class RegistrationDtoPayloadShapeTest {

  private final ObjectMapper mapper = new ObjectMapper();

  // ── ConsentGroupRequest ────────────────────────────────────────────────────

  @Test
  void consentGroupRequest_roundTripsAllFieldsFromConsentGroupJson() throws Exception {
    ConsentGroup source = buildConsentGroup();
    String json = mapper.writeValueAsString(source);

    ConsentGroupRequest result = mapper.readValue(json, ConsentGroupRequest.class);

    assertEquals(source.getDatasetId(), result.getDatasetId());
    assertEquals(source.getConsentGroupName(), result.getConsentGroupName());
    assertEquals(source.getDataAccessCommitteeId(), result.getDataAccessCommitteeId());
    assertEquals(source.getAccessManagement(), result.getAccessManagement());
    assertEquals(source.getGeneralResearchUse(), result.getGeneralResearchUse());
    assertEquals(source.getHmb(), result.getHmb());
    assertEquals(source.getDiseaseSpecificUse(), result.getDiseaseSpecificUse());
    assertEquals(source.getPoa(), result.getPoa());
    assertEquals(source.getOtherPrimary(), result.getOtherPrimary());
    assertEquals(source.getNmds(), result.getNmds());
    assertEquals(source.getGso(), result.getGso());
    assertEquals(source.getPub(), result.getPub());
    assertEquals(source.getCol(), result.getCol());
    assertEquals(source.getIrb(), result.getIrb());
    assertEquals(source.getGs(), result.getGs());
    assertEquals(source.getMor(), result.getMor());
    assertEquals(source.getMorDate(), result.getMorDate());
    assertEquals(source.getNpu(), result.getNpu());
    assertEquals(source.getOtherSecondary(), result.getOtherSecondary());
    assertEquals(source.getDataLocation(), result.getDataLocation());
    assertEquals(source.getUrl(), result.getUrl());
    assertEquals(source.getRequestLocation(), result.getRequestLocation());
    assertEquals(source.getNumberOfParticipants(), result.getNumberOfParticipants());
    assertNotNull(result.getFileTypes());
    assertEquals(1, result.getFileTypes().size());
    assertEquals(
        source.getFileTypes().get(0).getFileType(), result.getFileTypes().get(0).getFileType());
    assertEquals(
        source.getFileTypes().get(0).getFunctionalEquivalence(),
        result.getFileTypes().get(0).getFunctionalEquivalence());
    assertEquals(source.getData(), result.getData());
    // datasetIdentifier is server-assigned and intentionally absent from ConsentGroupRequest
  }

  @Test
  void consentGroupRequest_serializesBackToConsentGroupJson() throws Exception {
    ConsentGroup source = buildConsentGroup();

    // ConsentGroup → JSON → ConsentGroupRequest → JSON → ConsentGroup
    String dtoJson =
        mapper.writeValueAsString(
            mapper.readValue(mapper.writeValueAsString(source), ConsentGroupRequest.class));
    ConsentGroup roundTripped = mapper.readValue(dtoJson, ConsentGroup.class);

    assertEquals(source.getDatasetId(), roundTripped.getDatasetId());
    assertEquals(source.getConsentGroupName(), roundTripped.getConsentGroupName());
    assertEquals(source.getDataAccessCommitteeId(), roundTripped.getDataAccessCommitteeId());
    assertEquals(source.getAccessManagement(), roundTripped.getAccessManagement());
    assertEquals(source.getGeneralResearchUse(), roundTripped.getGeneralResearchUse());
    assertEquals(source.getHmb(), roundTripped.getHmb());
    assertEquals(source.getDiseaseSpecificUse(), roundTripped.getDiseaseSpecificUse());
    assertEquals(source.getPoa(), roundTripped.getPoa());
    assertEquals(source.getOtherPrimary(), roundTripped.getOtherPrimary());
    assertEquals(source.getNmds(), roundTripped.getNmds());
    assertEquals(source.getGso(), roundTripped.getGso());
    assertEquals(source.getPub(), roundTripped.getPub());
    assertEquals(source.getCol(), roundTripped.getCol());
    assertEquals(source.getIrb(), roundTripped.getIrb());
    assertEquals(source.getGs(), roundTripped.getGs());
    assertEquals(source.getMor(), roundTripped.getMor());
    assertEquals(source.getMorDate(), roundTripped.getMorDate());
    assertEquals(source.getNpu(), roundTripped.getNpu());
    assertEquals(source.getOtherSecondary(), roundTripped.getOtherSecondary());
    assertEquals(source.getDataLocation(), roundTripped.getDataLocation());
    assertEquals(source.getUrl(), roundTripped.getUrl());
    assertEquals(source.getRequestLocation(), roundTripped.getRequestLocation());
    assertEquals(source.getNumberOfParticipants(), roundTripped.getNumberOfParticipants());
    assertNotNull(roundTripped.getFileTypes());
    assertEquals(1, roundTripped.getFileTypes().size());
    assertEquals(
        source.getFileTypes().get(0).getFileType(),
        roundTripped.getFileTypes().get(0).getFileType());
    assertEquals(
        source.getFileTypes().get(0).getFunctionalEquivalence(),
        roundTripped.getFileTypes().get(0).getFunctionalEquivalence());
    assertEquals(source.getData(), roundTripped.getData());
    // datasetIdentifier is server-assigned and intentionally absent from ConsentGroupRequest
  }

  // ── StudyRegistrationRequest ───────────────────────────────────────────────

  @Test
  void studyRegistrationRequest_roundTripsAllFieldsFromDatasetRegistrationSchemaV1Json()
      throws Exception {
    DatasetRegistrationSchemaV1 source = buildFullyPopulatedSchema();
    String json = mapper.writeValueAsString(source);

    StudyRegistrationRequest result = mapper.readValue(json, StudyRegistrationRequest.class);

    assertStudyFieldsMatch(source, result);
  }

  @Test
  void studyRegistrationRequest_serializesBackToDatasetRegistrationSchemaV1() throws Exception {
    DatasetRegistrationSchemaV1 source = buildFullyPopulatedSchema();

    // Schema → DTO → JSON → Schema
    String dtoJson =
        mapper.writeValueAsString(
            mapper.readValue(mapper.writeValueAsString(source), StudyRegistrationRequest.class));
    DatasetRegistrationSchemaV1 roundTripped =
        mapper.readValue(dtoJson, DatasetRegistrationSchemaV1.class);

    assertEquals(source.getStudyName(), roundTripped.getStudyName());
    assertEquals(source.getStudyType(), roundTripped.getStudyType());
    assertEquals(source.getStudyDescription(), roundTripped.getStudyDescription());
    assertEquals(source.getDataTypes(), roundTripped.getDataTypes());
    assertEquals(source.getPiName(), roundTripped.getPiName());
    assertEquals(source.getNihAnvilUse(), roundTripped.getNihAnvilUse());
    assertEquals(source.getNihICsSupportingStudy(), roundTripped.getNihICsSupportingStudy());
    assertEquals(
        source.getAlternativeDataSharingPlanReasons(),
        roundTripped.getAlternativeDataSharingPlanReasons());
    assertEquals(source.getExternalIdentifier(), roundTripped.getExternalIdentifier());
    assertEquals(source.getExternalIdentifierType(), roundTripped.getExternalIdentifierType());
    assertNotNull(roundTripped.getConsentGroups());
    assertEquals(1, roundTripped.getConsentGroups().size());
    assertEquals(
        source.getConsentGroups().get(0).getConsentGroupName(),
        roundTripped.getConsentGroups().get(0).getConsentGroupName());
    assertEquals(
        source.getConsentGroups().get(0).getAccessManagement(),
        roundTripped.getConsentGroups().get(0).getAccessManagement());
  }

  // ── StudyUpdateRequest ─────────────────────────────────────────────────────

  @Test
  void studyUpdateRequest_roundTripsAllFieldsFromDatasetRegistrationSchemaV1Json()
      throws Exception {
    DatasetRegistrationSchemaV1 source = buildFullyPopulatedSchema();
    String json = mapper.writeValueAsString(source);

    StudyUpdateRequest result = mapper.readValue(json, StudyUpdateRequest.class);

    // StudyUpdateRequest extends StudyRegistrationRequest — same payload shape
    assertStudyFieldsMatch(source, result);
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private ConsentGroup buildConsentGroup() {
    FileTypeObject ft = new FileTypeObject();
    ft.setFileType(FileTypeObject.FileType.GENOME);
    ft.setFunctionalEquivalence("WGS equivalent");

    ConsentGroup cg = new ConsentGroup();
    cg.setDatasetId(99);
    cg.setDatasetIdentifier("DUOS-000099"); // read-only; excluded from DTO
    cg.setConsentGroupName("Test Consent Group");
    cg.setDataAccessCommitteeId(7);
    cg.setAccessManagement(ConsentGroup.AccessManagement.CONTROLLED);
    cg.setGeneralResearchUse(false);
    cg.setHmb(true);
    cg.setDiseaseSpecificUse(List.of("MONDO:0004975", "MONDO:0005148"));
    cg.setPoa(false);
    cg.setOtherPrimary("primary restriction text");
    cg.setNmds(true);
    cg.setGso(false);
    cg.setPub(true);
    cg.setCol(false);
    cg.setIrb(true);
    cg.setGs("USA");
    cg.setMor(true);
    cg.setMorDate("2025-12-31");
    cg.setNpu(false);
    cg.setOtherSecondary("secondary restriction text");
    cg.setDataLocation(ConsentGroup.DataLocation.TERRA_WORKSPACE);
    cg.setUrl(URI.create("https://example.com/data"));
    cg.setRequestLocation(URI.create("https://example.com/request"));
    cg.setNumberOfParticipants(500);
    cg.setFileTypes(List.of(ft));
    cg.setData(Map.of("key", "value"));
    return cg;
  }

  private DatasetRegistrationSchemaV1 buildFullyPopulatedSchema() {
    DatasetRegistrationSchemaV1 schema = new DatasetRegistrationSchemaV1();
    schema.setStudyName("Shape Parity Test Study");
    schema.setStudyType(DatasetRegistrationSchemaV1.StudyType.OBSERVATIONAL);
    schema.setStudyDescription("Study to verify DTO shape parity");
    schema.setDataTypes(List.of("WGS", "Phenotype"));
    schema.setPiName("Jane Researcher");
    schema.setPiEmail("jane@example.com");
    schema.setPhenotypeIndication("Cardiovascular");
    schema.setSpecies("Human");
    schema.setDataCustodianEmail(List.of("custodian@example.com"));
    schema.setPublicVisibility(true);
    schema.setThroughBioId("bio-12345");
    schema.setNihAnvilUse(
        DatasetRegistrationSchemaV1.NihAnvilUse
            .I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    schema.setSubmittingToAnvil(true);
    schema.setDbGaPPhsID("phs000001");
    schema.setDbGaPStudyRegistrationName("GWAS Study");
    schema.setEmbargoReleaseDate("2026-01-01");
    schema.setSequencingCenter("Broad Institute");
    schema.setPiInstitution(42);
    schema.setNihGrantContractNumber("R01HG012345");
    schema.setNihICsSupportingStudy(
        List.of(NihICsSupportingStudy.NHGRI, NihICsSupportingStudy.NCI));
    schema.setNihProgramOfficerName("Officer Smith");
    schema.setNihInstitutionCenterSubmission(
        DatasetRegistrationSchemaV1.NihInstitutionCenterSubmission.NHGRI);
    schema.setNihGenomicProgramAdministratorName("Admin Jones");
    schema.setMultiCenterStudy(true);
    schema.setCollaboratingSites(List.of("Site A", "Site B"));
    schema.setControlledAccessRequiredForGenomicSummaryResultsGSR(false);
    schema.setControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation("N/A");
    schema.setAlternativeDataSharingPlan(true);
    schema.setAlternativeDataSharingPlanReasons(
        List.of(AlternativeDataSharingPlanReason.LEGAL_RESTRICTIONS));
    schema.setAlternativeDataSharingPlanExplanation("Legal hold");
    schema.setAlternativeDataSharingPlanFileName("plan.pdf");
    schema.setAlternativeDataSharingPlanDataSubmitted(
        DatasetRegistrationSchemaV1.AlternativeDataSharingPlanDataSubmitted
            .WITHIN_3_MONTHS_OF_THE_LAST_DATA_GENERATED_OR_LAST_CLINICAL_VISIT);
    schema.setAlternativeDataSharingPlanDataReleased(true);
    schema.setAlternativeDataSharingPlanTargetDeliveryDate("2026-03-01");
    schema.setAlternativeDataSharingPlanTargetPublicReleaseDate("2026-06-01");
    schema.setAlternativeDataSharingPlanAccessManagement(
        DatasetRegistrationSchemaV1.AlternativeDataSharingPlanAccessManagement.CONTROLLED_ACCESS);
    schema.setExternalIdentifier("ext-001");
    schema.setExternalIdentifierType("dbGaP");
    schema.setAssets(Map.of("workspaceId", "ws-001"));
    schema.setData(Map.of("note", "extra metadata"));

    ConsentGroup cg = new ConsentGroup();
    cg.setConsentGroupName("Consent Group 1");
    cg.setDatasetId(10);
    cg.setAccessManagement(ConsentGroup.AccessManagement.OPEN);
    cg.setGeneralResearchUse(true);
    cg.setNumberOfParticipants(100);
    cg.setDataLocation(ConsentGroup.DataLocation.AN_VIL_WORKSPACE);
    cg.setUrl(URI.create("https://anvil.example.com/workspace"));
    schema.setConsentGroups(List.of(cg));

    return schema;
  }

  private void assertStudyFieldsMatch(
      DatasetRegistrationSchemaV1 source, StudyRegistrationRequest result) {
    // studyId and dataSubmitterUserId are intentionally excluded from the DTO (system-set fields)
    assertEquals(source.getStudyName(), result.getStudyName());
    assertEquals(source.getStudyType(), result.getStudyType());
    assertEquals(source.getStudyDescription(), result.getStudyDescription());
    assertEquals(source.getDataTypes(), result.getDataTypes());
    assertEquals(source.getPiName(), result.getPiName());
    assertEquals(source.getPiEmail(), result.getPiEmail());
    assertEquals(source.getPhenotypeIndication(), result.getPhenotypeIndication());
    assertEquals(source.getSpecies(), result.getSpecies());
    assertEquals(source.getDataCustodianEmail(), result.getDataCustodianEmail());
    assertEquals(source.getPublicVisibility(), result.getPublicVisibility());
    assertEquals(source.getThroughBioId(), result.getThroughBioId());
    assertEquals(source.getNihAnvilUse(), result.getNihAnvilUse());
    assertEquals(source.getSubmittingToAnvil(), result.getSubmittingToAnvil());
    assertEquals(source.getDbGaPPhsID(), result.getDbGaPPhsID());
    assertEquals(source.getDbGaPStudyRegistrationName(), result.getDbGaPStudyRegistrationName());
    assertEquals(source.getEmbargoReleaseDate(), result.getEmbargoReleaseDate());
    assertEquals(source.getSequencingCenter(), result.getSequencingCenter());
    assertEquals(source.getPiInstitution(), result.getPiInstitution());
    assertEquals(source.getNihGrantContractNumber(), result.getNihGrantContractNumber());
    assertEquals(source.getNihICsSupportingStudy(), result.getNihICsSupportingStudy());
    assertEquals(source.getNihProgramOfficerName(), result.getNihProgramOfficerName());
    assertEquals(
        source.getNihInstitutionCenterSubmission(), result.getNihInstitutionCenterSubmission());
    assertEquals(
        source.getNihGenomicProgramAdministratorName(),
        result.getNihGenomicProgramAdministratorName());
    assertEquals(source.getMultiCenterStudy(), result.getMultiCenterStudy());
    assertEquals(source.getCollaboratingSites(), result.getCollaboratingSites());
    assertEquals(
        source.getControlledAccessRequiredForGenomicSummaryResultsGSR(),
        result.getControlledAccessRequiredForGenomicSummaryResultsGSR());
    assertEquals(
        source.getControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation(),
        result.getControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation());
    assertEquals(source.getAlternativeDataSharingPlan(), result.getAlternativeDataSharingPlan());
    assertEquals(
        source.getAlternativeDataSharingPlanReasons(),
        result.getAlternativeDataSharingPlanReasons());
    assertEquals(
        source.getAlternativeDataSharingPlanExplanation(),
        result.getAlternativeDataSharingPlanExplanation());
    assertEquals(
        source.getAlternativeDataSharingPlanFileName(),
        result.getAlternativeDataSharingPlanFileName());
    assertEquals(
        source.getAlternativeDataSharingPlanDataSubmitted(),
        result.getAlternativeDataSharingPlanDataSubmitted());
    assertEquals(
        source.getAlternativeDataSharingPlanDataReleased(),
        result.getAlternativeDataSharingPlanDataReleased());
    assertEquals(
        source.getAlternativeDataSharingPlanTargetDeliveryDate(),
        result.getAlternativeDataSharingPlanTargetDeliveryDate());
    assertEquals(
        source.getAlternativeDataSharingPlanTargetPublicReleaseDate(),
        result.getAlternativeDataSharingPlanTargetPublicReleaseDate());
    assertEquals(
        source.getAlternativeDataSharingPlanAccessManagement(),
        result.getAlternativeDataSharingPlanAccessManagement());
    assertEquals(source.getExternalIdentifier(), result.getExternalIdentifier());
    assertEquals(source.getExternalIdentifierType(), result.getExternalIdentifierType());
    assertEquals(source.getAssets(), result.getAssets());
    assertEquals(source.getData(), result.getData());

    // consent groups
    assertNotNull(result.getConsentGroups());
    assertEquals(source.getConsentGroups().size(), result.getConsentGroups().size());
    ConsentGroup sourceCg = source.getConsentGroups().get(0);
    ConsentGroupRequest resultCg = result.getConsentGroups().get(0);
    assertEquals(sourceCg.getDatasetId(), resultCg.getDatasetId());
    assertEquals(sourceCg.getConsentGroupName(), resultCg.getConsentGroupName());
    assertEquals(sourceCg.getAccessManagement(), resultCg.getAccessManagement());
    assertEquals(sourceCg.getGeneralResearchUse(), resultCg.getGeneralResearchUse());
    assertEquals(sourceCg.getNumberOfParticipants(), resultCg.getNumberOfParticipants());
    assertEquals(sourceCg.getDataLocation(), resultCg.getDataLocation());
    assertEquals(sourceCg.getUrl(), resultCg.getUrl());
  }
}
