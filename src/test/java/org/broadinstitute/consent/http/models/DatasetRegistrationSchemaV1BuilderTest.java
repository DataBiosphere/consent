package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.dataset_registration_v1.AlternativeDataSharingPlanReason;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.AlternativeDataSharingPlanControlledOpenAccess;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.AlternativeDataSharingPlanDataSubmitted;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihInstitutionCenterSubmission;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.StudyType;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1Builder;
import org.broadinstitute.consent.http.models.dataset_registration_v1.NihICsSupportingStudy;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;

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
    assertNotNull(schemaV1.getControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation());
    assertNotNull(schemaV1.getAlternativeDataSharingPlanReasons());
    assertNotNull(schemaV1.getAlternativeDataSharingPlanExplanation());
    assertNotNull(schemaV1.getAlternativeDataSharingPlanFileName());
    assertNotNull(schemaV1.getAlternativeDataSharingPlanDataSubmitted());
    assertNotNull(schemaV1.getAlternativeDataSharingPlanDataReleased());
    assertNotNull(schemaV1.getAlternativeDataSharingPlanTargetDeliveryDate());
    assertNotNull(schemaV1.getAlternativeDataSharingPlanTargetPublicReleaseDate());
    assertNotNull(schemaV1.getAlternativeDataSharingPlanControlledOpenAccess());

    // TODO: Test code ... remove when complete.
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    System.out.println(gson.toJson(schemaV1));
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
    assertNotNull(consentGroup.getConsentGroupName());

    // TODO: Test code ... remove when complete.
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    System.out.println(gson.toJson(consentGroup));
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
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.studyType, study.getStudyId(), StudyType.OBSERVATIONAL.value(), PropertyType.String));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.phenotypeIndication, study.getStudyId(), randomString(), PropertyType.String));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.species, study.getStudyId(), randomString(), PropertyType.String));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.dataCustodianEmail, study.getStudyId(), GsonUtil.getInstance().toJson(List.of(randomString())), PropertyType.Json));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.nihAnvilUse, study.getStudyId(), NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY.value(), PropertyType.String));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.submittingToAnvil, study.getStudyId(), Boolean.TRUE, PropertyType.Boolean));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.dbGaPPhsID, study.getStudyId(), randomString(), PropertyType.String));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.dbGaPStudyRegistrationName, study.getStudyId(), randomString(), PropertyType.String));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.embargoReleaseDate, study.getStudyId(), randomString(), PropertyType.String));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.sequencingCenter, study.getStudyId(), randomString(), PropertyType.String));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.piInstitution, study.getStudyId(), randomInt(), PropertyType.Number));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.nihGrantContractNumber, study.getStudyId(), randomString(), PropertyType.String));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.nihICsSupportingStudy, study.getStudyId(), GsonUtil.getInstance().toJson(List.of(NihICsSupportingStudy.CC.value())), PropertyType.Json));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.nihProgramOfficerName, study.getStudyId(), randomString(), PropertyType.String));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.nihInstitutionCenterSubmission, study.getStudyId(), NihInstitutionCenterSubmission.CC.value(), PropertyType.String));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.nihGenomicProgramAdministratorName, study.getStudyId(), randomString(), PropertyType.String));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.multiCenterStudy, study.getStudyId(), Boolean.TRUE, PropertyType.Boolean));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.collaboratingSites, study.getStudyId(), GsonUtil.getInstance().toJson(List.of(randomString())), PropertyType.Json));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.controlledAccessRequiredForGenomicSummaryResultsGSR, study.getStudyId(), Boolean.TRUE, PropertyType.Boolean));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation, study.getStudyId(), randomString(), PropertyType.String));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanReasons, study.getStudyId(), GsonUtil.getInstance().toJson(List.of(AlternativeDataSharingPlanReason.OTHER.value())), PropertyType.Json));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanExplanation, study.getStudyId(), randomString(), PropertyType.String));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanFileName, study.getStudyId(), randomString(), PropertyType.String));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanDataSubmitted, study.getStudyId(), AlternativeDataSharingPlanDataSubmitted.BY_BATCHES_OVER_STUDY_TIMELINE_E_G_BASED_ON_CLINICAL_TRIAL_ENROLLMENT_BENCHMARKS.value(), PropertyType.String));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanDataReleased, study.getStudyId(), Boolean.TRUE, PropertyType.Boolean));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanTargetDeliveryDate, study.getStudyId(), randomString(), PropertyType.String));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanTargetPublicReleaseDate, study.getStudyId(), randomString(), PropertyType.String));
    study.addProperty(createStudyProperty(DatasetRegistrationSchemaV1Builder.alternativeDataSharingPlanControlledOpenAccess, study.getStudyId(), AlternativeDataSharingPlanControlledOpenAccess.OPEN_ACCESS.value(), PropertyType.String));
  }

  private String randomString() {
    return RandomStringUtils.randomAlphabetic(5);
  }

  private Integer randomInt() {
    return RandomUtils.nextInt(1, 100);
  }

  private StudyProperty createStudyProperty(String key, Integer studyId, Object value, PropertyType type) {
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
    dataset.addProperty(createDatasetProperty(dataset, DatasetRegistrationSchemaV1Builder.openAccess, PropertyType.Boolean));
    dataset.addProperty(createDatasetProperty(dataset, DatasetRegistrationSchemaV1Builder.generalResearchUse, PropertyType.Boolean));
    dataset.addProperty(createDatasetProperty(dataset, DatasetRegistrationSchemaV1Builder.hmb, PropertyType.Boolean));
  }

  private DatasetProperty createDatasetProperty(Dataset dataset, String schemaProp, PropertyType type) {
    DatasetProperty prop = new DatasetProperty();
    prop.setDataSetId(dataset.getDataSetId());
    prop.setSchemaProperty(schemaProp);
    prop.setPropertyName(schemaProp);
    prop.setPropertyType(type);
    switch (type) {
      case Boolean -> prop.setPropertyValue(true);
      case Number -> prop.setPropertyValue(randomInt());
      default -> prop.setPropertyValue(randomString());
    }
    return prop;
  }

}
