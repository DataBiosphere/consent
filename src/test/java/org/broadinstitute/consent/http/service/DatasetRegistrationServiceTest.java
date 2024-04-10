package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dataset_registration_v1.AlternativeDataSharingPlanReason;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.AlternativeDataSharingPlanAccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject;
import org.broadinstitute.consent.http.models.dataset_registration_v1.NihICsSupportingStudy;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO.DatasetUpdate;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetRegistrationServiceTest {

  private DatasetRegistrationService datasetRegistrationService;

  @Mock
  private DatasetDAO datasetDAO;

  @Mock
  private DacDAO dacDAO;

  @Mock
  private DatasetServiceDAO datasetServiceDAO;

  @Mock
  private StudyDAO studyDAO;

  @Mock
  private GCSService gcsService;

  @Mock
  private ElasticSearchService elasticSearchService;

  @Mock
  private EmailService emailService;

  private void initService() {
    datasetRegistrationService = new DatasetRegistrationService(datasetDAO, dacDAO,
        datasetServiceDAO, gcsService, elasticSearchService, studyDAO, emailService);
  }


  // captor: allows you to inspect the arguments sent to a function.
  @Captor
  ArgumentCaptor<List<DatasetServiceDAO.DatasetInsert>> datasetInsertCaptor;
  @Captor
  ArgumentCaptor<DatasetServiceDAO.StudyInsert> studyInsert;

  // ------------------------ test multiple dataset insert ----------------------------------- //
  @Test
  void testInsertCompleteDatasetRegistration() throws Exception {
    User user = mock();
    DatasetRegistrationSchemaV1 schema = createRandomCompleteDatasetRegistration(user);

    FormDataContentDisposition content = FormDataContentDisposition
        .name("file")
        .fileName("sharing_plan.txt")
        .build();

    InputStream is = new ByteArrayInputStream("HelloWorld".getBytes(StandardCharsets.UTF_8));
    FormDataBodyPart bodyPart = mock();
    when(bodyPart.getMediaType()).thenReturn(MediaType.TEXT_PLAIN_TYPE);
    when(bodyPart.getContentDisposition()).thenReturn(content);
    when(bodyPart.getValueAs(any())).thenReturn(is);

    initService();

    Map<String, FormDataBodyPart> files = Map.of("alternativeDataSharingPlan",
        bodyPart, "consentGroups[0].nihInstitutionalCertificationFile",
        bodyPart, "otherUnused", bodyPart);
    when(gcsService.storeDocument(any(), any(), any())).thenReturn(BlobId.of("asdf", "hjkl"),
        BlobId.of("qwer", "tyuio"));
    when(dacDAO.findById(any())).thenReturn(new Dac());

    datasetRegistrationService.createDatasetsFromRegistration(schema, user, files);

    verify(datasetServiceDAO).insertDatasetRegistration(studyInsert.capture(),
        datasetInsertCaptor.capture());

    // only two files are stored; extra "unused" file not used
    verify(gcsService, times(2)).storeDocument(any(), any(), any());

    DatasetServiceDAO.StudyInsert capturedStudyInsert = studyInsert.getValue();
    List<DatasetServiceDAO.DatasetInsert> inserts = datasetInsertCaptor.getValue();

    assertEquals(1, inserts.size());

    assertEquals(schema.getConsentGroups().get(0).getConsentGroupName(),
        inserts.get(0).name());
    assertDataUse(schema.getConsentGroups().get(0), inserts.get(0).dataUse());
    assertEquals(user.getUserId(), inserts.get(0).userId());

    assertEquals(1, inserts.get(0).files().size());

    assertEquals(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION,
        inserts.get(0).files().get(0).getCategory());
    assertEquals(
        files.get("consentGroups[0].nihInstitutionalCertificationFile").getContentDisposition()
            .getFileName(),
        inserts.get(0).files().get(0).getFileName());
    assertEquals(BlobId.of("qwer", "tyuio"),
        inserts.get(0).files().get(0).getBlobId());

    assertEquals(schema.getStudyName(), capturedStudyInsert.name());
    assertEquals(schema.getPiName(), capturedStudyInsert.piName());
    assertEquals(schema.getStudyDescription(), capturedStudyInsert.description());
    assertEquals(schema.getDataTypes(), capturedStudyInsert.dataTypes());
    assertEquals(schema.getPublicVisibility(),
        capturedStudyInsert.publicVisibility());
    assertEquals(user.getUserId(), capturedStudyInsert.userId());

    assertEquals(1, capturedStudyInsert.files().size());
    assertEquals(FileCategory.ALTERNATIVE_DATA_SHARING_PLAN,
        capturedStudyInsert.files().get(0).getCategory());

    // TODO: is there a way to ensure we don't miss anything?
    List<StudyProperty> studyProps = capturedStudyInsert.props();
    assertContainsStudyProperty(studyProps, "studyType", schema.getStudyType().value());
    assertContainsStudyProperty(studyProps, "phenotypeIndication", schema.getPhenotypeIndication());
    assertContainsStudyProperty(studyProps, "species", schema.getSpecies());
    assertContainsStudyProperty(studyProps, "dataSubmitterUserId", schema.getDataSubmitterUserId());
    assertContainsStudyProperty(studyProps, "dataCustodianEmail",
        PropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getDataCustodianEmail())));
    assertContainsStudyProperty(studyProps, "nihAnvilUse", schema.getNihAnvilUse().value());
    assertContainsStudyProperty(studyProps, "submittingToAnvil", schema.getSubmittingToAnvil());
    assertContainsStudyProperty(studyProps, "dbGaPPhsID", schema.getDbGaPPhsID());
    assertContainsStudyProperty(studyProps, "dbGaPStudyRegistrationName",
        schema.getDbGaPStudyRegistrationName());
    assertContainsStudyProperty(studyProps, "embargoReleaseDate",
        PropertyType.coerceToDate(schema.getEmbargoReleaseDate()));
    assertContainsStudyProperty(studyProps, "sequencingCenter", schema.getSequencingCenter());
    assertContainsStudyProperty(studyProps, "piInstitution", schema.getPiInstitution());
    assertContainsStudyProperty(studyProps, "nihGrantContractNumber",
        schema.getNihGrantContractNumber());
    assertContainsStudyProperty(studyProps, "nihICsSupportingStudy", PropertyType.coerceToJson(
        GsonUtil.getInstance().toJson(
            schema.getNihICsSupportingStudy().stream().map(NihICsSupportingStudy::value)
                .toList())));
    assertContainsStudyProperty(studyProps, "nihProgramOfficerName",
        schema.getNihProgramOfficerName());
    assertContainsStudyProperty(studyProps, "nihInstitutionCenterSubmission",
        schema.getNihInstitutionCenterSubmission().value());
    assertContainsStudyProperty(studyProps, "nihGenomicProgramAdministratorName",
        schema.getNihGenomicProgramAdministratorName());
    assertContainsStudyProperty(studyProps, "multiCenterStudy", schema.getMultiCenterStudy());
    assertContainsStudyProperty(studyProps, "collaboratingSites",
        PropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getCollaboratingSites())));
    assertContainsStudyProperty(studyProps, "controlledAccessRequiredForGenomicSummaryResultsGSR",
        schema.getControlledAccessRequiredForGenomicSummaryResultsGSR());
    assertContainsStudyProperty(studyProps,
        "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation",
        schema.getControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation());
    assertContainsStudyProperty(studyProps, "alternativeDataSharingPlan",
        schema.getAlternativeDataSharingPlan());
    assertContainsStudyProperty(studyProps, "alternativeDataSharingPlanReasons",
        PropertyType.coerceToJson(GsonUtil.getInstance().toJson(
            schema.getAlternativeDataSharingPlanReasons().stream()
                .map(AlternativeDataSharingPlanReason::value).toList())));
    assertContainsStudyProperty(studyProps, "alternativeDataSharingPlanExplanation",
        schema.getAlternativeDataSharingPlanExplanation());
    assertContainsStudyProperty(studyProps, "alternativeDataSharingPlanFileName",
        schema.getAlternativeDataSharingPlanFileName());
    assertContainsStudyProperty(studyProps, "alternativeDataSharingPlanDataSubmitted",
        schema.getAlternativeDataSharingPlanDataSubmitted().value());
    assertContainsStudyProperty(studyProps, "alternativeDataSharingPlanDataReleased",
        schema.getAlternativeDataSharingPlanDataReleased());
    assertContainsStudyProperty(studyProps, "alternativeDataSharingPlanTargetDeliveryDate",
        PropertyType.Date.coerce(schema.getAlternativeDataSharingPlanTargetDeliveryDate()));
    assertContainsStudyProperty(studyProps, "alternativeDataSharingPlanTargetPublicReleaseDate",
        PropertyType.Date.coerce(schema.getAlternativeDataSharingPlanTargetPublicReleaseDate()));
    assertContainsStudyProperty(studyProps, "alternativeDataSharingPlanAccessManagement",
        schema.getAlternativeDataSharingPlanAccessManagement().value());

    List<DatasetProperty> datasetProps = inserts.get(0).props();
    assertContainsDatasetProperty(datasetProps, "dataLocation",
        schema.getConsentGroups().get(0).getDataLocation().value());
    assertContainsDatasetProperty(datasetProps, "numberOfParticipants",
        schema.getConsentGroups().get(0).getNumberOfParticipants());
    assertContainsDatasetProperty(datasetProps, "fileTypes", PropertyType.coerceToJson(
        GsonUtil.getInstance().toJson(schema.getConsentGroups().get(0).getFileTypes())));
    assertContainsDatasetProperty(datasetProps, "url",
        schema.getConsentGroups().get(0).getUrl().toString());
    assertContainsDatasetProperty(datasetProps, "accessManagement",
        schema.getConsentGroups().get(0).getAccessManagement().value());

  }


  // inserts only required fields to ensure that null fields are ok
  @Test
  void testInsertMinimumDatasetRegistration() throws Exception {
    User user = mock();
    DatasetRegistrationSchemaV1 schema = createRandomMinimumDatasetRegistration(user);

    initService();
    when(dacDAO.findById(any())).thenReturn(new Dac());

    datasetRegistrationService.createDatasetsFromRegistration(schema, user, Map.of());

    verify(datasetServiceDAO).insertDatasetRegistration(studyInsert.capture(),
        datasetInsertCaptor.capture());

    verify(gcsService, times(0)).storeDocument(any(), any(), any());

    DatasetServiceDAO.StudyInsert capturedStudyInsert = studyInsert.getValue();
    List<DatasetServiceDAO.DatasetInsert> inserts = datasetInsertCaptor.getValue();

    assertEquals(1, inserts.size());

    assertEquals(schema.getConsentGroups().get(0).getConsentGroupName(),
        inserts.get(0).name());

    ConsentGroup consentGroup = schema.getConsentGroups().get(0);
    DataUse dataUse = inserts.get(0).dataUse();

    assertEquals(consentGroup.getGeneralResearchUse(), dataUse.getGeneralUse());

    assertEquals(schema.getStudyName(), capturedStudyInsert.name());
    assertEquals(schema.getPiName(), capturedStudyInsert.piName());
    assertEquals(schema.getStudyDescription(), capturedStudyInsert.description());
    assertEquals(schema.getDataTypes(), capturedStudyInsert.dataTypes());
    assertEquals(schema.getPublicVisibility(),
        capturedStudyInsert.publicVisibility());
    assertEquals(user.getUserId(), capturedStudyInsert.userId());

    assertEquals(user.getUserId(), inserts.get(0).userId());

    assertEquals(0, inserts.get(0).files().size());

    List<DatasetProperty> datasetProps = inserts.get(0).props();
    List<StudyProperty> studyProps = capturedStudyInsert.props();
    assertContainsStudyProperty(studyProps, "phenotypeIndication", schema.getPhenotypeIndication());
    assertContainsStudyProperty(studyProps, "species", schema.getSpecies());
    assertContainsStudyProperty(studyProps, "dataSubmitterUserId", schema.getDataSubmitterUserId());
    assertContainsDatasetProperty(datasetProps, "numberOfParticipants",
        schema.getConsentGroups().get(0).getNumberOfParticipants());
    assertContainsDatasetProperty(datasetProps, "fileTypes", PropertyType.coerceToJson(
        GsonUtil.getInstance().toJson(schema.getConsentGroups().get(0).getFileTypes())));
  }

  @Test
  void testDatasetCreateRegistrationEmails() throws Exception {
    User user = mock();
    DatasetRegistrationSchemaV1 schema = createRandomCompleteDatasetRegistration(user);

    initService();
    when(dacDAO.findById(any())).thenReturn(new Dac());

    DatasetRegistrationService registrationSpy = spy(datasetRegistrationService);
    registrationSpy.createDatasetsFromRegistration(schema, user, Map.of());
    verify(registrationSpy, times(1)).sendDatasetSubmittedEmails(any());
  }

  @Test
  void testStudyUpdateNewDatasetEmails() throws Exception {
    User user = mock();
    DatasetRegistrationSchemaV1 schema = createRandomCompleteDatasetRegistration(user);
    Study study = mock();
    Set<Dataset> datasets = Set.of(new Dataset());

    initService();
    when(dacDAO.findById(any())).thenReturn(new Dac());
    when(datasetServiceDAO.updateStudy(any(), any(), any())).thenReturn(study);
    when(study.getDatasets()).thenReturn(datasets);

    DatasetRegistrationService registrationSpy = spy(datasetRegistrationService);
    registrationSpy.updateStudyFromRegistration(1, schema, user, Map.of());
    verify(registrationSpy, times(1)).sendDatasetSubmittedEmails(any());
  }

  @Test
  void testSendDatasetSubmittedEmailsExistingChairs() throws Exception {
    User user = new User();
    user.setRoles(List.of(new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName())));
    Dac dac = mock();
    Dataset dataset = new Dataset();
    dataset.setDacId(1);

    initService();
    when(dacDAO.findById(any())).thenReturn(dac);
    when(dacDAO.findMembersByDacId(any())).thenReturn(List.of(user));

    datasetRegistrationService.sendDatasetSubmittedEmails(List.of(dataset));
    verify(emailService, times(1)).sendDatasetSubmittedMessage(any(), any(), any(), any());
  }

  @Test
  void testSendDatasetSubmittedEmailsNoChairs() throws Exception {
    Dac dac = mock();
    Dataset dataset = new Dataset();
    dataset.setDacId(1);

    initService();
    when(dacDAO.findById(any())).thenReturn(dac);
    when(dacDAO.findMembersByDacId(any())).thenReturn(List.of());

    datasetRegistrationService.sendDatasetSubmittedEmails(List.of(dataset));
    verify(emailService, never()).sendDatasetSubmittedMessage(any(), any(), any(), any());
  }

  @Test
  void testCreatedDatasetsFromUpdatedStudy() {
    Study study = mock();
    Set<Dataset> allDatasets = Stream.of(1, 2, 3, 4, 5).map((i) -> {
      Dataset dataset = new Dataset();
      dataset.setDataSetId(i);
      return dataset;
    }).collect(Collectors.toSet());
    List<DatasetUpdate> updatedDatasets = Stream.of(3, 4)
        .map((i) -> new DatasetUpdate(i, "update", 1, 1, null, null)).toList();

    initService();
    when(study.getDatasets()).thenReturn(allDatasets);

    List<Dataset> datasets = datasetRegistrationService.createdDatasetsFromUpdatedStudy(study,
        updatedDatasets);

    assertEquals(3, datasets.size());

    List<Integer> expectedIds = List.of(1, 2, 5);
    List<Integer> actualIds = datasets.stream().map(Dataset::getDataSetId).toList();

    assertEquals(expectedIds, actualIds);
  }

  @Test
  void testCreatedDatasetsFromUpdatedStudyNoDatasets() {
    Study study = mock();
    List<DatasetUpdate> updatedDatasets = null;
    initService();
    when(study.getDatasets()).thenReturn(null);
    List<Dataset> datasets = datasetRegistrationService.createdDatasetsFromUpdatedStudy(study,
        updatedDatasets);
    assertTrue(datasets.isEmpty());
  }

  @Test
  void testInsertAccessManagement() throws Exception {
    User user = mock();
    DatasetRegistrationSchemaV1 schema = createAccessManagementRegistrationNoDacId(user);

    initService();

    datasetRegistrationService.createDatasetsFromRegistration(schema, user, Map.of());

    verify(datasetServiceDAO).insertDatasetRegistration(studyInsert.capture(),
        datasetInsertCaptor.capture());
    verify(gcsService, times(0)).storeDocument(any(), any(), any());

    List<DatasetServiceDAO.DatasetInsert> inserts = datasetInsertCaptor.getValue();

    assertEquals(1, inserts.size());

    verify(dacDAO, never()).findById(any());
  }


  // test inset multiple consent groups
  @Test
  void testInsertMultipleDatasetRegistration() throws Exception {
    User user = mock();
    DatasetRegistrationSchemaV1 schema = createRandomMultipleDatasetRegistration(user);

    FormDataContentDisposition content = FormDataContentDisposition
        .name("file")
        .fileName("sharing_plan.txt")
        .build();

    InputStream is = new ByteArrayInputStream("HelloWorld".getBytes(StandardCharsets.UTF_8));
    FormDataBodyPart bodyPart = mock();
    when(bodyPart.getMediaType()).thenReturn(MediaType.TEXT_PLAIN_TYPE);
    when(bodyPart.getContentDisposition()).thenReturn(content);
    when(bodyPart.getValueAs(any())).thenReturn(is);

    initService();

    when(dacDAO.findById(any())).thenReturn(new Dac());
    Map<String, FormDataBodyPart> files = Map.of("alternativeDataSharingPlan",
        bodyPart, "consentGroups[0].nihInstitutionalCertificationFile",
        bodyPart, "otherUnused", bodyPart);
    when(gcsService.storeDocument(any(), any(), any())).thenReturn(BlobId.of("asdf", "hjkl"),
        BlobId.of("qwer", "tyuio"));

    datasetRegistrationService.createDatasetsFromRegistration(schema, user, files);

    verify(datasetServiceDAO).insertDatasetRegistration(studyInsert.capture(),
        datasetInsertCaptor.capture());

    // only two files are stored; extra "unused" file not used
    verify(gcsService, times(2)).storeDocument(any(), any(), any());

    DatasetServiceDAO.StudyInsert capturedStudyInsert = studyInsert.getValue();
    List<DatasetServiceDAO.DatasetInsert> inserts = datasetInsertCaptor.getValue();

    assertEquals(2, inserts.size());

    // check first dataset insert is ok

    assertEquals(schema.getConsentGroups().get(0).getConsentGroupName(),
        inserts.get(0).name());

    ConsentGroup consentGroup = schema.getConsentGroups().get(0);
    DataUse dataUse = inserts.get(0).dataUse();

    assertEquals(consentGroup.getGeneralResearchUse(), dataUse.getGeneralUse());

    assertEquals(user.getUserId(), inserts.get(0).userId());

    assertEquals(schema.getStudyName(), capturedStudyInsert.name());
    assertEquals(schema.getPiName(), capturedStudyInsert.piName());
    assertEquals(schema.getStudyDescription(), capturedStudyInsert.description());
    assertEquals(schema.getDataTypes(), capturedStudyInsert.dataTypes());
    assertEquals(schema.getPublicVisibility(),
        capturedStudyInsert.publicVisibility());
    assertEquals(user.getUserId(), capturedStudyInsert.userId());

    assertEquals(1, inserts.get(0).files().size());

    List<StudyProperty> studyProps = capturedStudyInsert.props();
    assertContainsStudyProperty(studyProps, "studyType", schema.getStudyType().value());
    assertContainsStudyProperty(studyProps, "phenotypeIndication", schema.getPhenotypeIndication());
    assertContainsStudyProperty(studyProps, "species", schema.getSpecies());
    assertContainsStudyProperty(studyProps, "dataSubmitterUserId", schema.getDataSubmitterUserId());

    List<DatasetProperty> props = inserts.get(0).props();
    assertContainsDatasetProperty(props, "fileTypes", PropertyType.coerceToJson(
        GsonUtil.getInstance().toJson(schema.getConsentGroups().get(0).getFileTypes())));
    assertContainsDatasetProperty(props, "accessManagement",
        schema.getConsentGroups().get(0).getAccessManagement().value());
    assertContainsDatasetProperty(props, "numberOfParticipants",
        schema.getConsentGroups().get(0).getNumberOfParticipants());

    // assert on all the same properties, but for the second dataset

    assertEquals(schema.getConsentGroups().get(1).getConsentGroupName(),
        inserts.get(1).name());

    ConsentGroup consentGroup2 = schema.getConsentGroups().get(1);
    DataUse dataUse2 = inserts.get(1).dataUse();

    assertEquals(consentGroup2.getGeneralResearchUse(), dataUse2.getGeneralUse());

    assertEquals(user.getUserId(), inserts.get(1).userId());

    assertEquals(0, inserts.get(1).files().size());

    List<DatasetProperty> props2 = inserts.get(1).props();
    assertContainsDatasetProperty(props2, "fileTypes", PropertyType.coerceToJson(
        GsonUtil.getInstance().toJson(schema.getConsentGroups().get(1).getFileTypes())));
    assertContainsDatasetProperty(props2, "accessManagement",
        schema.getConsentGroups().get(1).getAccessManagement().value());
    assertContainsDatasetProperty(props2, "numberOfParticipants",
        schema.getConsentGroups().get(1).getNumberOfParticipants());


  }

  @Test
  void testRegistrationErrorsOnInvalidDacId() throws Exception {

    User user = mock();
    DatasetRegistrationSchemaV1 schema = createRandomMinimumDatasetRegistration(user);

    when(dacDAO.findById(any())).thenReturn(null);

    initService();
    assertThrows(NotFoundException.class, () -> {
      datasetRegistrationService.createDatasetsFromRegistration(schema, user, Map.of());
    });
  }

  @Test
  void testRegistrationSucceedsWithESError() throws Exception {
    User user = mock();
    DatasetRegistrationSchemaV1 schema = createRandomMinimumDatasetRegistration(user);
    when(dacDAO.findById(any())).thenReturn(new Dac());
    when(elasticSearchService.indexDatasets(any())).thenThrow(new ServerErrorException("Timeout connecting to [elasticsearch]", 500));
    initService();
    assertDoesNotThrow(() -> {
      datasetRegistrationService.createDatasetsFromRegistration(schema, user, Map.of());
    }, "Registration Error");
  }

  @Test
  void testUpdateDatasetSucceedsWithESError() {
    User user = mock();
    Dac dac = new Dac();
    dac.setDacId(RandomUtils.nextInt(1, 100));
    Dataset dataset = new Dataset();
    dataset.setDataSetId(RandomUtils.nextInt(1, 100));
    dataset.setDacId(dac.getDacId());
    String name = RandomStringUtils.randomAlphabetic(10);
    org.broadinstitute.consent.http.models.DatasetUpdate update = new org.broadinstitute.consent.http.models.DatasetUpdate(
        name,
        dac.getDacId(),
        List.of());
    when(datasetDAO.findDatasetById(any())).thenReturn(dataset);

    initService();
    assertDoesNotThrow(() -> {
      datasetRegistrationService.updateDataset(dataset.getDataSetId(), user, update, Map.of());
    }, "Update Error");
  }

  @Test
  void testExtractStudyProperty() {
    DatasetRegistrationService.StudyPropertyExtractor extractor = new DatasetRegistrationService.StudyPropertyExtractor(
        RandomStringUtils.randomAlphabetic(10),
        PropertyType.String,
        DatasetRegistrationSchemaV1::getStudyName
    );

    DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();

    // null value -> empty extraction
    assertTrue(extractor.extract(schemaV1).isEmpty());

    schemaV1.setStudyName(RandomStringUtils.randomAlphabetic(10));

    Optional<StudyProperty> prop = extractor.extract(schemaV1);

    // non-null value -> turn value into dataset prop
    assertTrue(prop.isPresent());

    assertEquals(schemaV1.getStudyName(), prop.get().getValue());
    assertEquals(extractor.key(), prop.get().getKey());
    assertEquals(extractor.type(), prop.get().getType());
  }

  @Test
  void testExtractDatasetProperty() {
    DatasetRegistrationService.DatasetPropertyExtractor extractor = new DatasetRegistrationService.DatasetPropertyExtractor(
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        PropertyType.String,
        ConsentGroup::getConsentGroupName
    );

    ConsentGroup group = new ConsentGroup();

    // null value -> empty extraction
    assertTrue(extractor.extract(group).isEmpty());

    group.setConsentGroupName(RandomStringUtils.randomAlphabetic(10));

    Optional<DatasetProperty> prop = extractor.extract(group);

    // non-null value -> turn value into dataset prop
    assertTrue(prop.isPresent());

    assertEquals(group.getConsentGroupName(), prop.get().getPropertyValue());
    assertEquals(extractor.name(), prop.get().getPropertyName());
    assertEquals(extractor.schemaProp(), prop.get().getSchemaProperty());
    assertEquals(extractor.type(), prop.get().getPropertyType());
  }


  @Test
  void testExtractStudyPropertyTyped() {
    DatasetRegistrationService.StudyPropertyExtractor extractor = new DatasetRegistrationService.StudyPropertyExtractor(
        RandomStringUtils.randomAlphabetic(10),
        PropertyType.Json,
        (registration) -> GsonUtil.getInstance().toJson(registration.getDataTypes())
    );

    DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();

    schemaV1.setDataTypes(List.of("type1", "type2", "type3"));

    Optional<StudyProperty> prop = extractor.extract(schemaV1);

    assertTrue(prop.isPresent());

    assertEquals(GsonUtil.getInstance().toJsonTree(schemaV1.getDataTypes()),
        prop.get().getValue());
    assertEquals(extractor.key(), prop.get().getKey());
    assertEquals(extractor.type(), prop.get().getType());
  }

  @Test
  void testExtractDatasetPropertyTyped() {
    DatasetRegistrationService.DatasetPropertyExtractor extractor = new DatasetRegistrationService.DatasetPropertyExtractor(
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        PropertyType.Json,
        (consentGroup) -> GsonUtil.getInstance().toJson(consentGroup.getDiseaseSpecificUse())
    );

    ConsentGroup group = new ConsentGroup();

    group.setDiseaseSpecificUse(List.of("asdf", "sdfg", "dfgh"));

    Optional<DatasetProperty> prop = extractor.extract(group);

    assertTrue(prop.isPresent());

    assertEquals(GsonUtil.getInstance().toJsonTree(group.getDiseaseSpecificUse()),
        prop.get().getPropertyValue());
    assertEquals(extractor.name(), prop.get().getPropertyName());
    assertEquals(extractor.schemaProp(), prop.get().getSchemaProperty());
    assertEquals(extractor.type(), prop.get().getPropertyType());
  }

  private void assertDataUse(ConsentGroup consentGroup, DataUse dataUse) {
    assertEquals(consentGroup.getCol(), dataUse.getCollaboratorRequired());
    assertEquals(consentGroup.getDiseaseSpecificUse(),
        dataUse.getDiseaseRestrictions());
    assertEquals(consentGroup.getIrb(), dataUse.getEthicsApprovalRequired());
    assertEquals(consentGroup.getGeneralResearchUse(), dataUse.getGeneralUse());
    assertEquals(consentGroup.getGs(), dataUse.getGeographicalRestrictions());
    assertEquals(consentGroup.getGso(), dataUse.getGeneticStudiesOnly());
    assertEquals(consentGroup.getHmb(), dataUse.getHmbResearch());
    assertEquals(consentGroup.getMorDate(), dataUse.getPublicationMoratorium());
    if (Objects.isNull(consentGroup.getNmds()) || !consentGroup.getNmds()) {
      assertNull(dataUse.getMethodsResearch());
    } else {
      assertFalse(dataUse.getMethodsResearch());
    }
    assertEquals(consentGroup.getNpu(), !dataUse.getCommercialUse());
    assertEquals(consentGroup.getOtherPrimary(), dataUse.getOther());
    assertEquals(consentGroup.getOtherSecondary(), dataUse.getSecondaryOther());
    assertEquals(consentGroup.getPoa(), dataUse.getPopulationOriginsAncestry());
    assertEquals(consentGroup.getPub(), dataUse.getPublicationResults());
  }

  private void assertContainsDatasetProperty(Collection<DatasetProperty> props, String schema,
      Object value) {
    Optional<DatasetProperty> prop = props.stream()
        .filter((p) -> p.getSchemaProperty().equals(schema)).findFirst();
    assertTrue(prop.isPresent());
    assertEquals(value, prop.get().getPropertyValue());
  }

  private void assertContainsStudyProperty(Collection<StudyProperty> props, String key,
      Object value) {
    Optional<StudyProperty> prop = props.stream().filter((p) -> p.getKey().equals(key)).findFirst();
    assertTrue(prop.isPresent());
    assertEquals(value, prop.get().getValue());
  }

  private DatasetRegistrationSchemaV1 createRandomMinimumDatasetRegistration(User user) {
    DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();
    schemaV1.setStudyName(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setStudyDescription(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setDataTypes(List.of(RandomStringUtils.randomAlphabetic(10)));
    schemaV1.setPhenotypeIndication(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setSpecies(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setPiName(RandomStringUtils.randomAlphabetic(10));
    when(user.getUserId()).thenReturn(1);
    schemaV1.setDataSubmitterUserId(user.getUserId());
    schemaV1.setDataCustodianEmail(List.of(RandomStringUtils.randomAlphabetic(10) + "@domain.org"));
    schemaV1.setPublicVisibility(true);

    ConsentGroup consentGroup = new ConsentGroup();
    consentGroup.setConsentGroupName(RandomStringUtils.randomAlphabetic(10));
    consentGroup.setGeneralResearchUse(true);
    FileTypeObject fileType = new FileTypeObject();
    fileType.setFileType(FileTypeObject.FileType.ARRAYS);
    fileType.setFunctionalEquivalence(RandomStringUtils.randomAlphabetic(10));
    consentGroup.setNumberOfParticipants(new Random().nextInt());
    consentGroup.setFileTypes(List.of(fileType));
    consentGroup.setDataAccessCommitteeId(new Random().nextInt());

    schemaV1.setConsentGroups(List.of(consentGroup));
    return schemaV1;
  }

  private DatasetRegistrationSchemaV1 createAccessManagementRegistrationNoDacId(User user) {
    DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();
    schemaV1.setStudyName(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setStudyType(DatasetRegistrationSchemaV1.StudyType.OBSERVATIONAL);
    schemaV1.setStudyDescription(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setDataTypes(List.of(RandomStringUtils.randomAlphabetic(10)));
    schemaV1.setPhenotypeIndication(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setSpecies(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setPiName(RandomStringUtils.randomAlphabetic(10));
    when(user.getUserId()).thenReturn(1);
    schemaV1.setDataSubmitterUserId(user.getUserId());
    schemaV1.setDataCustodianEmail(List.of(RandomStringUtils.randomAlphabetic(10) + "@domain.org"));
    schemaV1.setPublicVisibility(true);

    ConsentGroup consentGroup = new ConsentGroup();
    consentGroup.setConsentGroupName(RandomStringUtils.randomAlphabetic(10));
    consentGroup.setAccessManagement(AccessManagement.OPEN);
    FileTypeObject fileType = new FileTypeObject();
    fileType.setFileType(FileTypeObject.FileType.ARRAYS);
    fileType.setFunctionalEquivalence(RandomStringUtils.randomAlphabetic(10));
    consentGroup.setNumberOfParticipants(new Random().nextInt());
    consentGroup.setFileTypes(List.of(fileType));

    schemaV1.setConsentGroups(List.of(consentGroup));
    return schemaV1;
  }

  private DatasetRegistrationSchemaV1 createRandomMultipleDatasetRegistration(User user) {
    DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();
    schemaV1.setStudyName(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setStudyType(DatasetRegistrationSchemaV1.StudyType.OBSERVATIONAL);
    schemaV1.setStudyDescription(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setDataTypes(List.of(RandomStringUtils.randomAlphabetic(10)));
    schemaV1.setPhenotypeIndication(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setSpecies(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setPiName(RandomStringUtils.randomAlphabetic(10));
    when(user.getUserId()).thenReturn(1);
    schemaV1.setDataSubmitterUserId(user.getUserId());
    schemaV1.setDataCustodianEmail(List.of(RandomStringUtils.randomAlphabetic(10) + "@domain.org"));
    schemaV1.setPublicVisibility(true);

    ConsentGroup consentGroup1 = new ConsentGroup();
    consentGroup1.setConsentGroupName(RandomStringUtils.randomAlphabetic(10));
    consentGroup1.setGeneralResearchUse(true);
    FileTypeObject fileType1 = new FileTypeObject();
    fileType1.setFileType(FileTypeObject.FileType.ARRAYS);
    fileType1.setFunctionalEquivalence(RandomStringUtils.randomAlphabetic(10));
    consentGroup1.setNumberOfParticipants(new Random().nextInt());
    consentGroup1.setFileTypes(List.of(fileType1));
    consentGroup1.setDataAccessCommitteeId(new Random().nextInt());
    consentGroup1.setAccessManagement(AccessManagement.CONTROLLED);

    ConsentGroup consentGroup2 = new ConsentGroup();
    consentGroup2.setConsentGroupName(RandomStringUtils.randomAlphabetic(10));
    consentGroup2.setGeneralResearchUse(true);
    FileTypeObject fileType2 = new FileTypeObject();
    fileType2.setFileType(FileTypeObject.FileType.ARRAYS);
    fileType2.setFunctionalEquivalence(RandomStringUtils.randomAlphabetic(10));
    consentGroup2.setNumberOfParticipants(new Random().nextInt());
    consentGroup2.setFileTypes(List.of(fileType2));
    consentGroup2.setAccessManagement(AccessManagement.OPEN);

    schemaV1.setConsentGroups(List.of(consentGroup1, consentGroup2));
    return schemaV1;
  }


  private DatasetRegistrationSchemaV1 createRandomCompleteDatasetRegistration(User user) {
    // TODO: find a better way to initialize this object
    DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();
    schemaV1.setStudyName(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setStudyType(DatasetRegistrationSchemaV1.StudyType.OBSERVATIONAL);
    schemaV1.setStudyDescription(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setDataTypes(List.of(RandomStringUtils.randomAlphabetic(10)));
    schemaV1.setPhenotypeIndication(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setSpecies(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setPiName(RandomStringUtils.randomAlphabetic(10));
    when(user.getUserId()).thenReturn(1);
    schemaV1.setDataSubmitterUserId(user.getUserId());
    schemaV1.setDataCustodianEmail(List.of(RandomStringUtils.randomAlphabetic(10) + "@domain.org"));
    schemaV1.setPublicVisibility(true);
    schemaV1.setSubmittingToAnvil(true);
    schemaV1.setDbGaPPhsID(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setDbGaPStudyRegistrationName(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setEmbargoReleaseDate("2007-12-03");
    schemaV1.setSequencingCenter(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setNihAnvilUse(
        DatasetRegistrationSchemaV1.NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL);
    schemaV1.setNihGrantContractNumber(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setNihICsSupportingStudy(List.of(NihICsSupportingStudy.CC, NihICsSupportingStudy.CIT));
    schemaV1.setNihProgramOfficerName(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setNihInstitutionCenterSubmission(
        DatasetRegistrationSchemaV1.NihInstitutionCenterSubmission.CSR);
    schemaV1.setNihGenomicProgramAdministratorName(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setMultiCenterStudy(true);
    schemaV1.setCollaboratingSites(
        List.of(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10)));
    schemaV1.setControlledAccessRequiredForGenomicSummaryResultsGSR(true);
    schemaV1.setControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation(
        RandomStringUtils.randomAlphabetic(10));
    schemaV1.setAlternativeDataSharingPlan(true);
    schemaV1.setAlternativeDataSharingPlanReasons(List.of(
        AlternativeDataSharingPlanReason.INFORMED_CONSENT_PROCESSES_ARE_INADEQUATE_TO_SUPPORT_DATA_FOR_SHARING_FOR_THE_FOLLOWING_REASONS));
    schemaV1.setAlternativeDataSharingPlanExplanation(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setAlternativeDataSharingPlanFileName(RandomStringUtils.randomAlphabetic(10));
    schemaV1.setAlternativeDataSharingPlanDataSubmitted(
        DatasetRegistrationSchemaV1.AlternativeDataSharingPlanDataSubmitted.WITHIN_3_MONTHS_OF_THE_LAST_DATA_GENERATED_OR_LAST_CLINICAL_VISIT);
    schemaV1.setAlternativeDataSharingPlanDataReleased(true);
    schemaV1.setAlternativeDataSharingPlanTargetDeliveryDate("2011-11-11");
    schemaV1.setAlternativeDataSharingPlanTargetPublicReleaseDate("2012-10-08");
    schemaV1.setAlternativeDataSharingPlanAccessManagement(
        AlternativeDataSharingPlanAccessManagement.OPEN_ACCESS);
    schemaV1.setPiInstitution(10);

    ConsentGroup consentGroup = new ConsentGroup();
    consentGroup.setConsentGroupName(RandomStringUtils.randomAlphabetic(10));
    consentGroup.setGeneralResearchUse(true);
    consentGroup.setNumberOfParticipants(new Random().nextInt());
    FileTypeObject fileType1 = new FileTypeObject();
    fileType1.setFileType(FileTypeObject.FileType.ARRAYS);
    fileType1.setFunctionalEquivalence(RandomStringUtils.randomAlphabetic(10));
    FileTypeObject fileType2 = new FileTypeObject();
    fileType2.setFileType(FileTypeObject.FileType.PHENOTYPE);
    fileType2.setFunctionalEquivalence(RandomStringUtils.randomAlphabetic(10));
    consentGroup.setFileTypes(List.of(fileType1, fileType2));
    consentGroup.setUrl(URI.create("https://asdf.gov"));
    consentGroup.setMor(false);
    consentGroup.setNmds(false);
    consentGroup.setNpu(false);
    consentGroup.setAccessManagement(AccessManagement.CONTROLLED);
    consentGroup.setDataLocation(ConsentGroup.DataLocation.TDR_LOCATION);
    consentGroup.setDataAccessCommitteeId(new Random().nextInt());

    schemaV1.setConsentGroups(List.of(consentGroup));
    return schemaV1;
  }

}
