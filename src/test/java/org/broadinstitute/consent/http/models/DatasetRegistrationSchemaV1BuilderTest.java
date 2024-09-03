package org.broadinstitute.consent.http.models;

import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.accessManagement;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanAccessManagement;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanDataReleased;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanDataSubmitted;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanExplanation;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanFileName;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanReasons;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanTargetDeliveryDate;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanTargetPublicReleaseDate;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.col;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.collaboratingSites;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.controlledAccessRequiredForGenomicSummaryResultsGSR;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.dataAccessCommitteeId;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.dataCustodianEmail;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.dataLocation;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.dbGaPPhsID;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.dbGaPStudyRegistrationName;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.diseaseSpecificUse;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.embargoReleaseDate;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.fileTypes;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.generalResearchUse;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.gs;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.gso;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.hmb;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.irb;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.mor;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.morDate;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.multiCenterStudy;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.nihAnvilUse;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.nihGenomicProgramAdministratorName;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.nihGrantContractNumber;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.nihICsSupportingStudy;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.nihInstitutionCenterSubmission;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.nihProgramOfficerName;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.nmds;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.npu;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.numberOfParticipants;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.otherPrimary;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.otherSecondary;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.phenotypeIndication;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.piInstitution;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.poa;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.pub;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.sequencingCenter;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.species;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.studyType;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.submittingToAnvil;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.url;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.dataset_registration_v1.AlternativeDataSharingPlanReason;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.DataLocation;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.AlternativeDataSharingPlanAccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.AlternativeDataSharingPlanDataSubmitted;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihInstitutionCenterSubmission;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.StudyType;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject;
import org.broadinstitute.consent.http.models.dataset_registration_v1.NihICsSupportingStudy;
import org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetRegistrationSchemaV1BuilderTest {

  @Test
  void testBuildEmptySchema() {
    DatasetRegistrationSchemaV1Builder builder = new DatasetRegistrationSchemaV1Builder();
    Study study = new Study();
    DatasetRegistrationSchemaV1 schemaV1 = builder.build(study, List.of());
    assertNotNull(schemaV1);
  }

  @Test
  void testBuildSchemaWithStudyProps() {
    DatasetRegistrationSchemaV1Builder builder = new DatasetRegistrationSchemaV1Builder();
    Study study = createMockStudy();
    addAllStudyProperties(study);

    DatasetRegistrationSchemaV1 schemaV1 = builder.build(study, List.of());
    assertNotNull(schemaV1);
    assertNotNull(schemaV1.getStudyId());
    assertNotNull(schemaV1.getStudyName());
    assertNotNull(schemaV1.getStudyType());
    assertNotNull(schemaV1.getStudyDescription());
    assertNotNull(schemaV1.getDataTypes());
    assertNotNull(schemaV1.getPhenotypeIndication());
    assertNotNull(schemaV1.getSpecies());
    assertNotNull(schemaV1.getPiName());
    assertNotNull(schemaV1.getDataSubmitterUserId());
    assertNotNull(schemaV1.getDataCustodianEmail());
    assertNotNull(schemaV1.getPublicVisibility());
    assertNotNull(schemaV1.getNihAnvilUse());
    assertNotNull(schemaV1.getSubmittingToAnvil());
    assertNotNull(schemaV1.getDbGaPPhsID());
    assertNotNull(schemaV1.getDbGaPStudyRegistrationName());
    assertNotNull(schemaV1.getEmbargoReleaseDate());
    assertNotNull(schemaV1.getSequencingCenter());
    assertNotNull(schemaV1.getPiInstitution());
    assertNotNull(schemaV1.getNihGrantContractNumber());
    assertNotNull(schemaV1.getNihICsSupportingStudy());
    assertNotNull(schemaV1.getNihProgramOfficerName());
    assertNotNull(schemaV1.getNihInstitutionCenterSubmission());
    assertNotNull(schemaV1.getNihGenomicProgramAdministratorName());
    assertNotNull(schemaV1.getMultiCenterStudy());
    assertNotNull(schemaV1.getCollaboratingSites());
    assertNotNull(schemaV1.getControlledAccessRequiredForGenomicSummaryResultsGSR());
    assertNotNull(
        schemaV1.getControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation());
    assertNotNull(schemaV1.getAlternativeDataSharingPlanReasons());
    assertNotNull(schemaV1.getAlternativeDataSharingPlanExplanation());
    assertNotNull(schemaV1.getAlternativeDataSharingPlanFileName());
    assertNotNull(schemaV1.getAlternativeDataSharingPlanDataSubmitted());
    assertNotNull(schemaV1.getAlternativeDataSharingPlanDataReleased());
    assertNotNull(schemaV1.getAlternativeDataSharingPlanTargetDeliveryDate());
    assertNotNull(schemaV1.getAlternativeDataSharingPlanTargetPublicReleaseDate());
    assertNotNull(schemaV1.getAlternativeDataSharingPlanAccessManagement());
  }

  @Test
  void testBuildSchemaWithDatasetProps() {
    DatasetRegistrationSchemaV1Builder builder = new DatasetRegistrationSchemaV1Builder();
    Study study = createMockStudy();
    Dataset dataset = createMockDataset();
    addAllDatasetProperties(dataset);
    DatasetRegistrationSchemaV1 schemaV1 = builder.build(study, List.of(dataset));
    assertNotNull(schemaV1.getConsentGroups());
    assertFalse(schemaV1.getConsentGroups().isEmpty());
    ConsentGroup consentGroup = schemaV1.getConsentGroups().get(0);
    assertNotNull(consentGroup);
    assertNotNull(consentGroup.getDatasetId());
    assertNotNull(consentGroup.getDatasetIdentifier());
    assertNotNull(consentGroup.getConsentGroupName());
    assertNotNull(consentGroup.getAccessManagement());
    assertNotNull(consentGroup.getGeneralResearchUse());
    assertNotNull(consentGroup.getHmb());
    assertNotNull(consentGroup.getDiseaseSpecificUse());
    assertFalse(consentGroup.getDiseaseSpecificUse().isEmpty());
    assertNotNull(consentGroup.getPoa());
    assertNotNull(consentGroup.getOtherPrimary());
    assertNotNull(consentGroup.getNmds());
    assertNotNull(consentGroup.getGso());
    assertNotNull(consentGroup.getPub());
    assertNotNull(consentGroup.getCol());
    assertNotNull(consentGroup.getIrb());
    assertNotNull(consentGroup.getGs());
    assertNotNull(consentGroup.getMor());
    assertNotNull(consentGroup.getMorDate());
    assertNotNull(consentGroup.getNpu());
    assertNotNull(consentGroup.getOtherSecondary());
    assertNotNull(consentGroup.getDataAccessCommitteeId());
    assertNotNull(consentGroup.getDataLocation());
    assertNotNull(consentGroup.getUrl());
    assertNotNull(consentGroup.getNumberOfParticipants());
    assertNotNull(consentGroup.getFileTypes());
    assertFalse(consentGroup.getFileTypes().isEmpty());
  }

  @Test
  void testBuildSchemaWithDatasetPropWithNullSchema() {
    DatasetRegistrationSchemaV1Builder builder = new DatasetRegistrationSchemaV1Builder();
    Study study = createMockStudy();
    Dataset dataset = createMockDataset();
    DatasetProperty prop = new DatasetProperty();
    prop.setDataSetId(dataset.getDataSetId());
    prop.setSchemaProperty(null);
    prop.setPropertyName(generalResearchUse);
    prop.setPropertyType(PropertyType.Boolean);
    dataset.addProperty(prop);
    assertDoesNotThrow(() -> builder.build(study, List.of(dataset)));
  }

  private Study createMockStudy() {
    Date now = new Date();
    Study study = new Study();
    study.setStudyId(randomInt());
    study.setName(randomString());
    study.setDescription(randomString());
    study.setPublicVisibility(Boolean.TRUE);
    study.setPiName(randomString());
    study.setDataTypes(List.of(randomString(), randomString(), randomString()));
    study.setAlternativeDataSharingPlan(new FileStorageObject());
    study.setCreateDate(now);
    study.setCreateUserEmail(randomString());
    study.setCreateUserId(randomInt());
    study.setUpdateDate(now);
    study.setUpdateUserId(randomInt());
    study.setUuid(UUID.randomUUID());
    return study;
  }

  private void addAllStudyProperties(Study study) {
    study.addProperty(
        createStudyProperty(studyType, study.getStudyId(), StudyType.OBSERVATIONAL.value(),
            PropertyType.String));
    study.addProperty(createStudyProperty(phenotypeIndication, study.getStudyId(), randomString(),
        PropertyType.String));
    study.addProperty(
        createStudyProperty(species, study.getStudyId(), randomString(), PropertyType.String));
    study.addProperty(createStudyProperty(dataCustodianEmail, study.getStudyId(),
        GsonUtil.getInstance().toJson(List.of(randomString())), PropertyType.Json));
    study.addProperty(createStudyProperty(nihAnvilUse, study.getStudyId(),
        NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY.value(),
        PropertyType.String));
    study.addProperty(createStudyProperty(submittingToAnvil, study.getStudyId(), Boolean.TRUE,
        PropertyType.Boolean));
    study.addProperty(
        createStudyProperty(dbGaPPhsID, study.getStudyId(), randomString(), PropertyType.String));
    study.addProperty(
        createStudyProperty(dbGaPStudyRegistrationName, study.getStudyId(), randomString(),
            PropertyType.String));
    study.addProperty(createStudyProperty(embargoReleaseDate, study.getStudyId(), randomString(),
        PropertyType.String));
    study.addProperty(createStudyProperty(sequencingCenter, study.getStudyId(), randomString(),
        PropertyType.String));
    study.addProperty(
        createStudyProperty(piInstitution, study.getStudyId(), randomInt(), PropertyType.Number));
    study.addProperty(
        createStudyProperty(nihGrantContractNumber, study.getStudyId(), randomString(),
            PropertyType.String));
    study.addProperty(createStudyProperty(nihICsSupportingStudy, study.getStudyId(),
        GsonUtil.getInstance().toJson(List.of(NihICsSupportingStudy.CC.value())),
        PropertyType.Json));
    study.addProperty(createStudyProperty(nihProgramOfficerName, study.getStudyId(), randomString(),
        PropertyType.String));
    study.addProperty(createStudyProperty(nihInstitutionCenterSubmission, study.getStudyId(),
        NihInstitutionCenterSubmission.CC.value(), PropertyType.String));
    study.addProperty(
        createStudyProperty(nihGenomicProgramAdministratorName, study.getStudyId(), randomString(),
            PropertyType.String));
    study.addProperty(createStudyProperty(multiCenterStudy, study.getStudyId(), Boolean.TRUE,
        PropertyType.Boolean));
    study.addProperty(createStudyProperty(collaboratingSites, study.getStudyId(),
        GsonUtil.getInstance().toJson(List.of(randomString())), PropertyType.Json));
    study.addProperty(
        createStudyProperty(controlledAccessRequiredForGenomicSummaryResultsGSR, study.getStudyId(),
            Boolean.TRUE, PropertyType.Boolean));
    study.addProperty(
        createStudyProperty(controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation,
            study.getStudyId(), randomString(), PropertyType.String));
    study.addProperty(createStudyProperty(alternativeDataSharingPlanReasons, study.getStudyId(),
        GsonUtil.getInstance().toJson(List.of(AlternativeDataSharingPlanReason.OTHER.value())),
        PropertyType.Json));
    study.addProperty(createStudyProperty(alternativeDataSharingPlanExplanation, study.getStudyId(),
        randomString(), PropertyType.String));
    study.addProperty(
        createStudyProperty(alternativeDataSharingPlanFileName, study.getStudyId(), randomString(),
            PropertyType.String));
    study.addProperty(
        createStudyProperty(alternativeDataSharingPlanDataSubmitted, study.getStudyId(),
            AlternativeDataSharingPlanDataSubmitted.BY_BATCHES_OVER_STUDY_TIMELINE_E_G_BASED_ON_CLINICAL_TRIAL_ENROLLMENT_BENCHMARKS.value(),
            PropertyType.String));
    study.addProperty(
        createStudyProperty(alternativeDataSharingPlanDataReleased, study.getStudyId(),
            Boolean.TRUE, PropertyType.Boolean));
    study.addProperty(
        createStudyProperty(alternativeDataSharingPlanTargetDeliveryDate, study.getStudyId(),
            randomString(), PropertyType.String));
    study.addProperty(
        createStudyProperty(alternativeDataSharingPlanTargetPublicReleaseDate, study.getStudyId(),
            randomString(), PropertyType.String));
    study.addProperty(
        createStudyProperty(alternativeDataSharingPlanAccessManagement, study.getStudyId(),
            AlternativeDataSharingPlanAccessManagement.OPEN_ACCESS.value(), PropertyType.String));
  }

  private String randomString() {
    return RandomStringUtils.randomAlphabetic(5);
  }

  private Integer randomInt() {
    return RandomUtils.nextInt(1, 100);
  }

  private StudyProperty createStudyProperty(String key, Integer studyId, Object value,
      PropertyType type) {
    StudyProperty property = new StudyProperty();
    property.setKey(key);
    property.setStudyId(studyId);
    property.setValue(value);
    property.setType(type);
    return property;
  }

  private Dataset createMockDataset() {
    User user = new User();
    user.setUserId(randomInt());
    user.setDisplayName(randomString());
    user.setEmail(randomString());
    Date now = new Date();
    Dataset dataset = new Dataset();
    dataset.setName(randomString());
    dataset.setDataSetId(randomInt());
    dataset.setAlias(randomInt());
    // datasetIdentifier is derived from `alias`
    dataset.setDatasetIdentifier();
    dataset.setDatasetName(randomString());
    dataset.setCreateUser(user);
    dataset.setCreateUserId(user.getUserId());
    dataset.setCreateDate(now);
    dataset.setDacApproval(true);
    dataset.setUpdateUserId(user.getUserId());
    dataset.setUpdateDate(now);
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());
    dataset.setTranslatedDataUse(randomString());
    return dataset;
  }

  private void addAllDatasetProperties(Dataset dataset) {
    dataset.addProperty(
        createDatasetProperty(dataset, accessManagement, PropertyType.String,
            AccessManagement.CONTROLLED.value()));
    dataset.addProperty(
        createDatasetProperty(dataset, generalResearchUse, PropertyType.Boolean, null));
    dataset.addProperty(createDatasetProperty(dataset, hmb, PropertyType.Boolean, null));
    dataset.addProperty(
        createDatasetProperty(dataset, diseaseSpecificUse, PropertyType.Json, null));
    dataset.addProperty(createDatasetProperty(dataset, poa, PropertyType.Boolean, null));
    dataset.addProperty(createDatasetProperty(dataset, otherPrimary, PropertyType.String, null));
    dataset.addProperty(createDatasetProperty(dataset, nmds, PropertyType.Boolean, null));
    dataset.addProperty(createDatasetProperty(dataset, gso, PropertyType.Boolean, null));
    dataset.addProperty(createDatasetProperty(dataset, pub, PropertyType.Boolean, null));
    dataset.addProperty(createDatasetProperty(dataset, col, PropertyType.Boolean, null));
    dataset.addProperty(createDatasetProperty(dataset, irb, PropertyType.Boolean, null));
    dataset.addProperty(createDatasetProperty(dataset, gs, PropertyType.String, null));
    dataset.addProperty(createDatasetProperty(dataset, mor, PropertyType.Boolean, null));
    dataset.addProperty(createDatasetProperty(dataset, morDate, PropertyType.String, null));
    dataset.addProperty(createDatasetProperty(dataset, npu, PropertyType.Boolean, null));
    dataset.addProperty(createDatasetProperty(dataset, otherSecondary, PropertyType.String, null));
    dataset.addProperty(
        createDatasetProperty(dataset, dataAccessCommitteeId, PropertyType.Number, null));
    dataset.addProperty(createDatasetProperty(dataset, dataLocation, PropertyType.String,
        DataLocation.NOT_DETERMINED.value()));
    dataset.addProperty(
        createDatasetProperty(dataset, url, PropertyType.String, "http://www.abc.com"));
    dataset.addProperty(
        createDatasetProperty(dataset, numberOfParticipants, PropertyType.Number, null));
    dataset.addProperty(
        createDatasetProperty(dataset, fileTypes, PropertyType.Json, new FileTypeObject()));
  }

  private DatasetProperty createDatasetProperty(Dataset dataset, String schemaProp,
      PropertyType type, Object propValue) {
    DatasetProperty prop = new DatasetProperty();
    prop.setDataSetId(dataset.getDataSetId());
    prop.setSchemaProperty(schemaProp);
    prop.setPropertyName(schemaProp);
    prop.setPropertyType(type);
    switch (type) {
      case Boolean -> prop.setPropertyValue(Objects.nonNull(propValue) ? propValue : true);
      case Number -> prop.setPropertyValue(Objects.nonNull(propValue) ? propValue : randomInt());
      case Json -> {
        List<Object> list = new ArrayList<>();
        if (Objects.nonNull(propValue)) {
          list.add(GsonUtil.getInstance().toJson(propValue));
        } else {
          list.add(randomString());
        }
        prop.setPropertyValue(list);
      }
      // Default to string
      default -> prop.setPropertyValue(Objects.nonNull(propValue) ? propValue : randomString());
    }
    return prop;
  }

}
