package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import freemarker.template.TemplateException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.mail.message.DatasetSubmittedMessage;
import org.broadinstitute.consent.http.mail.message.NewStudyRegistrationConfirmationMessage;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetRegistrationServiceTest extends AbstractTestHelper {

  private DatasetRegistrationService datasetRegistrationService;

  @Mock private DatasetDAO datasetDAO;

  @Mock private DacDAO dacDAO;

  @Mock private DatasetServiceDAO datasetServiceDAO;

  @Mock private FileStorageObjectDAO fileStorageObjectDAO;

  @Mock private StudyDAO studyDAO;

  @Mock private GCSService gcsService;

  @Mock private ElasticSearchService elasticSearchService;

  @Mock private EmailService emailService;

  @BeforeEach
  void setUp() {
    datasetRegistrationService =
        new DatasetRegistrationService(
            datasetDAO,
            dacDAO,
            datasetServiceDAO,
            fileStorageObjectDAO,
            gcsService,
            elasticSearchService,
            studyDAO,
            emailService);
  }

  // captor: allows you to inspect the arguments sent to a function.
  @Captor ArgumentCaptor<List<DatasetServiceDAO.DatasetInsert>> datasetInsertCaptor;
  @Captor ArgumentCaptor<DatasetServiceDAO.StudyInsert> studyInsert;

  // ------------------------ test multiple dataset insert ----------------------------------- //
  @Test
  void testInsertCompleteDatasetRegistration() throws Exception {
    User user = mock();
    DatasetRegistrationSchemaV1 schema = createRandomCompleteDatasetRegistration(user);

    FormDataContentDisposition content =
        FormDataContentDisposition.name("file").fileName("sharing_plan.txt").build();

    InputStream is = new ByteArrayInputStream("HelloWorld".getBytes(StandardCharsets.UTF_8));
    FormDataBodyPart bodyPart = mock();
    when(bodyPart.getMediaType()).thenReturn(MediaType.TEXT_PLAIN_TYPE);
    when(bodyPart.getContentDisposition()).thenReturn(content);
    when(bodyPart.getValueAs(any())).thenReturn(is);

    Map<String, FormDataBodyPart> files =
        Map.of(
            "alternativeDataSharingPlan",
            bodyPart,
            "consentGroups[0].nihInstitutionalCertificationFile",
            bodyPart,
            "otherUnused",
            bodyPart);
    when(gcsService.storeDocument(any(), any(), any()))
        .thenReturn(BlobId.of("asdf", "hjkl"), BlobId.of("qwer", "tyuio"));
    when(gcsService.hasBytes(any())).thenReturn(true);
    when(dacDAO.findById(any())).thenReturn(new Dac());

    Dataset dataset = new Dataset();
    dataset.setDacId(1);
    dataset.setDatasetId(1);
    dataset.setStudyId(123);
    when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(dataset));

    datasetRegistrationService.createDatasetsFromRegistration(schema, user, files);

    verify(datasetServiceDAO)
        .insertDatasetRegistration(studyInsert.capture(), datasetInsertCaptor.capture());

    // only two files are stored; extra "unused" file not used
    verify(gcsService, times(2)).storeDocument(any(), any(), any());

    DatasetServiceDAO.StudyInsert capturedStudyInsert = studyInsert.getValue();
    List<DatasetServiceDAO.DatasetInsert> inserts = datasetInsertCaptor.getValue();

    assertEquals(1, inserts.size());

    assertEquals(
        schema.getConsentGroups().getFirst().getConsentGroupName(), inserts.getFirst().name());
    assertDataUse(schema.getConsentGroups().getFirst(), inserts.getFirst().dataUse());
    assertEquals(user.getUserId(), inserts.getFirst().userId());

    assertEquals(1, inserts.getFirst().files().size());

    assertEquals(
        FileCategory.NIH_INSTITUTIONAL_CERTIFICATION,
        inserts.getFirst().files().getFirst().getCategory());
    assertEquals(
        files
            .get("consentGroups[0].nihInstitutionalCertificationFile")
            .getContentDisposition()
            .getFileName(),
        inserts.getFirst().files().getFirst().getFileName());
    assertEquals(BlobId.of("qwer", "tyuio"), inserts.getFirst().files().getFirst().getBlobId());

    assertEquals(schema.getStudyName(), capturedStudyInsert.name());
    assertEquals(schema.getPiName(), capturedStudyInsert.piName());
    assertEquals(schema.getStudyDescription(), capturedStudyInsert.description());
    assertEquals(schema.getDataTypes(), capturedStudyInsert.dataTypes());
    assertEquals(schema.getPublicVisibility(), capturedStudyInsert.publicVisibility());
    assertEquals(user.getUserId(), capturedStudyInsert.userId());

    assertEquals(1, capturedStudyInsert.files().size());
    assertEquals(
        FileCategory.ALTERNATIVE_DATA_SHARING_PLAN,
        capturedStudyInsert.files().getFirst().getCategory());

    // TODO: is there a way to ensure we don't miss anything?
    List<StudyProperty> studyProps = capturedStudyInsert.props();
    assertContainsStudyProperty(studyProps, "studyType", schema.getStudyType().value());
    assertContainsStudyProperty(studyProps, "phenotypeIndication", schema.getPhenotypeIndication());
    assertContainsStudyProperty(studyProps, "species", schema.getSpecies());
    assertContainsStudyProperty(
        studyProps,
        "dataCustodianEmail",
        PropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getDataCustodianEmail())));
    assertContainsStudyProperty(studyProps, "nihAnvilUse", schema.getNihAnvilUse().value());
    assertContainsStudyProperty(studyProps, "submittingToAnvil", schema.getSubmittingToAnvil());
    assertContainsStudyProperty(studyProps, "dbGaPPhsID", schema.getDbGaPPhsID());
    assertContainsStudyProperty(
        studyProps, "dbGaPStudyRegistrationName", schema.getDbGaPStudyRegistrationName());
    assertContainsStudyProperty(
        studyProps,
        "embargoReleaseDate",
        PropertyType.coerceToDate(schema.getEmbargoReleaseDate()));
    assertContainsStudyProperty(studyProps, "sequencingCenter", schema.getSequencingCenter());
    assertContainsStudyProperty(studyProps, "piInstitution", schema.getPiInstitution());
    assertContainsStudyProperty(
        studyProps, "nihGrantContractNumber", schema.getNihGrantContractNumber());
    assertContainsStudyProperty(
        studyProps,
        "nihICsSupportingStudy",
        PropertyType.coerceToJson(
            GsonUtil.getInstance()
                .toJson(
                    schema.getNihICsSupportingStudy().stream()
                        .map(NihICsSupportingStudy::value)
                        .toList())));
    assertContainsStudyProperty(
        studyProps, "nihProgramOfficerName", schema.getNihProgramOfficerName());
    assertContainsStudyProperty(
        studyProps,
        "nihInstitutionCenterSubmission",
        schema.getNihInstitutionCenterSubmission().value());
    assertContainsStudyProperty(
        studyProps,
        "nihGenomicProgramAdministratorName",
        schema.getNihGenomicProgramAdministratorName());
    assertContainsStudyProperty(studyProps, "multiCenterStudy", schema.getMultiCenterStudy());
    assertContainsStudyProperty(
        studyProps,
        "collaboratingSites",
        PropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getCollaboratingSites())));
    assertContainsStudyProperty(
        studyProps,
        "controlledAccessRequiredForGenomicSummaryResultsGSR",
        schema.getControlledAccessRequiredForGenomicSummaryResultsGSR());
    assertContainsStudyProperty(
        studyProps,
        "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation",
        schema.getControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation());
    assertContainsStudyProperty(
        studyProps, "alternativeDataSharingPlan", schema.getAlternativeDataSharingPlan());
    assertContainsStudyProperty(
        studyProps,
        "alternativeDataSharingPlanReasons",
        PropertyType.coerceToJson(
            GsonUtil.getInstance()
                .toJson(
                    schema.getAlternativeDataSharingPlanReasons().stream()
                        .map(AlternativeDataSharingPlanReason::value)
                        .toList())));
    assertContainsStudyProperty(
        studyProps,
        "alternativeDataSharingPlanExplanation",
        schema.getAlternativeDataSharingPlanExplanation());
    assertContainsStudyProperty(
        studyProps,
        "alternativeDataSharingPlanFileName",
        schema.getAlternativeDataSharingPlanFileName());
    assertContainsStudyProperty(
        studyProps,
        "alternativeDataSharingPlanDataSubmitted",
        schema.getAlternativeDataSharingPlanDataSubmitted().value());
    assertContainsStudyProperty(
        studyProps,
        "alternativeDataSharingPlanDataReleased",
        schema.getAlternativeDataSharingPlanDataReleased());
    assertContainsStudyProperty(
        studyProps,
        "alternativeDataSharingPlanTargetDeliveryDate",
        PropertyType.Date.coerce(schema.getAlternativeDataSharingPlanTargetDeliveryDate()));
    assertContainsStudyProperty(
        studyProps,
        "alternativeDataSharingPlanTargetPublicReleaseDate",
        PropertyType.Date.coerce(schema.getAlternativeDataSharingPlanTargetPublicReleaseDate()));
    assertContainsStudyProperty(
        studyProps,
        "alternativeDataSharingPlanAccessManagement",
        schema.getAlternativeDataSharingPlanAccessManagement().value());
    assertContainsStudyProperty(
        studyProps,
        "assets",
        PropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getAssets())));

    List<DatasetProperty> datasetProps = inserts.getFirst().props();
    assertContainsDatasetProperty(
        datasetProps,
        "dataLocation",
        schema.getConsentGroups().getFirst().getDataLocation().value());
    assertContainsDatasetProperty(
        datasetProps,
        "numberOfParticipants",
        schema.getConsentGroups().getFirst().getNumberOfParticipants());
    assertContainsDatasetProperty(
        datasetProps,
        "fileTypes",
        PropertyType.coerceToJson(
            GsonUtil.getInstance().toJson(schema.getConsentGroups().getFirst().getFileTypes())));
    assertContainsDatasetProperty(
        datasetProps, "url", schema.getConsentGroups().getFirst().getUrl().toString());
    assertContainsDatasetProperty(
        datasetProps,
        "accessManagement",
        schema.getConsentGroups().getFirst().getAccessManagement().value());
  }

  // inserts only required fields to ensure that null fields are ok
  @Test
  void testInsertMinimumDatasetRegistration() throws Exception {
    User user = mock();
    DatasetRegistrationSchemaV1 schema = createRandomMinimumDatasetRegistration(user);

    when(dacDAO.findById(any())).thenReturn(new Dac());

    Dataset dataset = new Dataset();
    dataset.setDacId(1);
    dataset.setDatasetId(1);
    dataset.setStudyId(1);
    when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(dataset));

    datasetRegistrationService.createDatasetsFromRegistration(schema, user, Map.of());

    verify(datasetServiceDAO)
        .insertDatasetRegistration(studyInsert.capture(), datasetInsertCaptor.capture());

    verify(gcsService, times(0)).storeDocument(any(), any(), any());

    DatasetServiceDAO.StudyInsert capturedStudyInsert = studyInsert.getValue();
    List<DatasetServiceDAO.DatasetInsert> inserts = datasetInsertCaptor.getValue();

    assertEquals(1, inserts.size());

    assertEquals(
        schema.getConsentGroups().getFirst().getConsentGroupName(), inserts.getFirst().name());

    ConsentGroup consentGroup = schema.getConsentGroups().getFirst();
    DataUse dataUse = inserts.getFirst().dataUse();

    assertEquals(consentGroup.getGeneralResearchUse(), dataUse.getGeneralUse());

    assertEquals(schema.getStudyName(), capturedStudyInsert.name());
    assertEquals(schema.getPiName(), capturedStudyInsert.piName());
    assertEquals(schema.getStudyDescription(), capturedStudyInsert.description());
    assertEquals(schema.getDataTypes(), capturedStudyInsert.dataTypes());
    assertEquals(schema.getPublicVisibility(), capturedStudyInsert.publicVisibility());
    assertEquals(user.getUserId(), capturedStudyInsert.userId());

    assertEquals(user.getUserId(), inserts.getFirst().userId());

    assertEquals(0, inserts.getFirst().files().size());

    List<DatasetProperty> datasetProps = inserts.getFirst().props();
    List<StudyProperty> studyProps = capturedStudyInsert.props();
    assertContainsStudyProperty(studyProps, "phenotypeIndication", schema.getPhenotypeIndication());
    assertContainsStudyProperty(studyProps, "species", schema.getSpecies());
    assertContainsDatasetProperty(
        datasetProps,
        "numberOfParticipants",
        schema.getConsentGroups().getFirst().getNumberOfParticipants());
    assertContainsDatasetProperty(
        datasetProps,
        "fileTypes",
        PropertyType.coerceToJson(
            GsonUtil.getInstance().toJson(schema.getConsentGroups().getFirst().getFileTypes())));
  }

  @Test
  void testDatasetCreateRegistrationEmails() throws Exception {
    User user = mock();
    DatasetRegistrationSchemaV1 schema = createRandomCompleteDatasetRegistration(user);

    when(dacDAO.findById(any())).thenReturn(new Dac());
    Dataset dataset = new Dataset();
    dataset.setDacId(1);
    dataset.setDatasetId(1);
    dataset.setStudyId(123);
    when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(dataset));

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
    user.setChairpersonRole();
    Dac dac = mock();
    User createUser = new User();
    createUser.setDisplayName("Create User");
    Dataset dataset = new Dataset();
    dataset.setDacId(1);
    dataset.setCreateUser(createUser);

    when(dacDAO.findById(any())).thenReturn(dac);
    when(dacDAO.findMembersByDacId(any())).thenReturn(List.of(user));

    datasetRegistrationService.sendDatasetSubmittedEmails(List.of(dataset));
    verify(emailService, times(1)).sendMessage(any(DatasetSubmittedMessage.class), any());
  }

  @Test
  void testSendDatasetSubmittedEmailsNoChairs() throws Exception {
    Dac dac = mock();
    Dataset dataset = new Dataset();
    dataset.setDacId(1);

    when(dacDAO.findById(any())).thenReturn(dac);
    when(dacDAO.findMembersByDacId(any())).thenReturn(List.of());

    datasetRegistrationService.sendDatasetSubmittedEmails(List.of(dataset));
    verify(emailService, never()).sendMessage(any(DatasetSubmittedMessage.class), any());
  }

  @Test
  void testSendDatasetSubmittedEmailsNoDAC() throws Exception {
    Dataset dataset = new Dataset();
    when(dacDAO.findById(any())).thenReturn(null);

    datasetRegistrationService.sendDatasetSubmittedEmails(List.of(dataset));
    verify(emailService, never()).sendMessage(any(DatasetSubmittedMessage.class), any());
  }

  @Test
  void testGetAssetsWithDatasets() {
    DatasetRegistrationSchemaV1 registration = new DatasetRegistrationSchemaV1();
    registration.setAssets(Map.of("asset1", List.of("file1", "file2")));
    ConsentGroup cg = new ConsentGroup();
    registration.setConsentGroups(List.of(cg));

    Map<String, Object> result = datasetRegistrationService.getAssetsWithDatasets(registration);

    assertTrue(result.containsKey("asset1"));
    assertTrue(result.containsKey("datasets"));
    assertEquals(List.of(cg), result.get("datasets"));
  }

  @Test
  void testGetAssetsWithDatasetsEmptyAssets() {
    DatasetRegistrationSchemaV1 registration = new DatasetRegistrationSchemaV1();
    ConsentGroup cg = new ConsentGroup();
    registration.setConsentGroups(List.of(cg));

    Map<String, Object> result = datasetRegistrationService.getAssetsWithDatasets(registration);

    assertTrue(result.isEmpty() || !result.containsKey("asset1"));
    assertTrue(result.containsKey("datasets"));
    assertEquals(List.of(cg), result.get("datasets"));
  }

  @Test
  void testCreateDatasetFromRegistrationSendsEmails() throws Exception {
    User user = mock();
    DatasetRegistrationSchemaV1 registration = createRandomCompleteDatasetRegistration(user);
    assertNull(registration.getStudyId());
    when(dacDAO.findById(any())).thenReturn(new Dac());

    Dataset dataset = new Dataset();
    dataset.setDacId(1);
    dataset.setDatasetId(1);
    dataset.setStudyId(123);
    when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(dataset));

    datasetRegistrationService.createDatasetsFromRegistration(registration, user, Map.of());

    verify(emailService, times(1))
        .sendMessage(any(NewStudyRegistrationConfirmationMessage.class), any());
  }

  @Test
  void testSendSubmissionConfirmationEmail() throws TemplateException, IOException {
    User submitter = new User();
    DatasetRegistrationSchemaV1 registration = new DatasetRegistrationSchemaV1();
    registration.setStudyName("Study");
    registration.setAssets(Map.of("asset1", List.of("file1")));
    registration.setConsentGroups(List.of(new ConsentGroup()));
    Integer studyId = 123;

    // study id does not come in with a new registration, it is generated by consent as part of
    // processing the registration
    datasetRegistrationService.sendSubmissionConfirmationEmail(submitter, registration, studyId);

    verify(emailService, times(1))
        .sendMessage(any(NewStudyRegistrationConfirmationMessage.class), any());
  }

  @Test
  void testCreatedDatasetsFromUpdatedStudy() {
    Study study = mock();
    Set<Dataset> allDatasets =
        Stream.of(1, 2, 3, 4, 5)
            .map(
                i -> {
                  Dataset dataset = new Dataset();
                  dataset.setDatasetId(i);
                  return dataset;
                })
            .collect(Collectors.toSet());
    List<DatasetUpdate> updatedDatasets =
        Stream.of(3, 4).map(i -> new DatasetUpdate(i, "update", 1, 1, null, null)).toList();

    when(study.getDatasets()).thenReturn(allDatasets);

    List<Dataset> datasets =
        datasetRegistrationService.createdDatasetsFromUpdatedStudy(study, updatedDatasets);

    assertEquals(3, datasets.size());

    List<Integer> expectedIds = List.of(1, 2, 5);
    List<Integer> actualIds = datasets.stream().map(Dataset::getDatasetId).toList();

    assertEquals(expectedIds, actualIds);
  }

  @Test
  void testCreatedDatasetsFromUpdatedStudyNoDatasets() {
    Study study = mock();
    List<DatasetUpdate> updatedDatasets = null;
    when(study.getDatasets()).thenReturn(null);
    List<Dataset> datasets =
        datasetRegistrationService.createdDatasetsFromUpdatedStudy(study, updatedDatasets);
    assertTrue(datasets.isEmpty());
  }

  @Test
  void testInsertAccessManagement() throws Exception {
    User user = mock();
    DatasetRegistrationSchemaV1 schema = createAccessManagementRegistrationNoDacId(user);

    Dataset dataset = new Dataset();
    dataset.setDacId(1);
    dataset.setDatasetId(1);
    dataset.setStudyId(123);
    when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(dataset));

    datasetRegistrationService.createDatasetsFromRegistration(schema, user, Map.of());

    verify(datasetServiceDAO)
        .insertDatasetRegistration(studyInsert.capture(), datasetInsertCaptor.capture());
    verify(gcsService, times(0)).storeDocument(any(), any(), any());

    List<DatasetServiceDAO.DatasetInsert> inserts = datasetInsertCaptor.getValue();

    assertEquals(1, inserts.size());
  }

  // test inset multiple consent groups
  @Test
  void testInsertMultipleDatasetRegistration() throws Exception {
    User user = mock();
    DatasetRegistrationSchemaV1 schema = createRandomMultipleDatasetRegistration(user);

    FormDataContentDisposition content =
        FormDataContentDisposition.name("file").fileName("sharing_plan.txt").build();

    InputStream is = new ByteArrayInputStream("HelloWorld".getBytes(StandardCharsets.UTF_8));
    FormDataBodyPart bodyPart = mock();
    when(bodyPart.getMediaType()).thenReturn(MediaType.TEXT_PLAIN_TYPE);
    when(bodyPart.getContentDisposition()).thenReturn(content);
    when(bodyPart.getValueAs(any())).thenReturn(is);

    when(dacDAO.findById(any())).thenReturn(new Dac());
    Map<String, FormDataBodyPart> files =
        Map.of(
            "alternativeDataSharingPlan",
            bodyPart,
            "consentGroups[0].nihInstitutionalCertificationFile",
            bodyPart,
            "otherUnused",
            bodyPart);
    when(gcsService.storeDocument(any(), any(), any()))
        .thenReturn(BlobId.of("asdf", "hjkl"), BlobId.of("qwer", "tyuio"));

    Dataset dataset = new Dataset();
    dataset.setDacId(1);
    dataset.setDatasetId(1);
    dataset.setStudyId(123);
    when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(dataset));

    datasetRegistrationService.createDatasetsFromRegistration(schema, user, files);

    verify(datasetServiceDAO)
        .insertDatasetRegistration(studyInsert.capture(), datasetInsertCaptor.capture());

    // only two files are stored; extra "unused" file not used
    verify(gcsService, times(2)).storeDocument(any(), any(), any());

    DatasetServiceDAO.StudyInsert capturedStudyInsert = studyInsert.getValue();
    List<DatasetServiceDAO.DatasetInsert> inserts = datasetInsertCaptor.getValue();

    assertEquals(2, inserts.size());

    // check first dataset insert is ok

    assertEquals(
        schema.getConsentGroups().getFirst().getConsentGroupName(), inserts.getFirst().name());

    ConsentGroup consentGroup = schema.getConsentGroups().getFirst();
    DataUse dataUse = inserts.getFirst().dataUse();

    assertEquals(consentGroup.getGeneralResearchUse(), dataUse.getGeneralUse());

    assertEquals(user.getUserId(), inserts.getFirst().userId());

    assertEquals(schema.getStudyName(), capturedStudyInsert.name());
    assertEquals(schema.getPiName(), capturedStudyInsert.piName());
    assertEquals(schema.getStudyDescription(), capturedStudyInsert.description());
    assertEquals(schema.getDataTypes(), capturedStudyInsert.dataTypes());
    assertEquals(schema.getPublicVisibility(), capturedStudyInsert.publicVisibility());
    assertEquals(user.getUserId(), capturedStudyInsert.userId());

    assertEquals(1, inserts.getFirst().files().size());

    List<StudyProperty> studyProps = capturedStudyInsert.props();
    assertContainsStudyProperty(studyProps, "studyType", schema.getStudyType().value());
    assertContainsStudyProperty(studyProps, "phenotypeIndication", schema.getPhenotypeIndication());
    assertContainsStudyProperty(studyProps, "species", schema.getSpecies());

    List<DatasetProperty> props = inserts.getFirst().props();
    assertContainsDatasetProperty(
        props,
        "fileTypes",
        PropertyType.coerceToJson(
            GsonUtil.getInstance().toJson(schema.getConsentGroups().getFirst().getFileTypes())));
    assertContainsDatasetProperty(
        props, "accessManagement", schema.getConsentGroups().get(0).getAccessManagement().value());
    assertContainsDatasetProperty(
        props, "numberOfParticipants", schema.getConsentGroups().get(0).getNumberOfParticipants());

    // assert on all the same properties, but for the second dataset

    assertEquals(schema.getConsentGroups().get(1).getConsentGroupName(), inserts.get(1).name());

    ConsentGroup consentGroup2 = schema.getConsentGroups().get(1);
    DataUse dataUse2 = inserts.get(1).dataUse();

    assertEquals(consentGroup2.getGeneralResearchUse(), dataUse2.getGeneralUse());

    assertEquals(user.getUserId(), inserts.get(1).userId());

    assertEquals(0, inserts.get(1).files().size());

    List<DatasetProperty> props2 = inserts.get(1).props();
    assertContainsDatasetProperty(
        props2,
        "fileTypes",
        PropertyType.coerceToJson(
            GsonUtil.getInstance().toJson(schema.getConsentGroups().get(1).getFileTypes())));
    assertContainsDatasetProperty(
        props2, "accessManagement", schema.getConsentGroups().get(1).getAccessManagement().value());
    assertContainsDatasetProperty(
        props2, "numberOfParticipants", schema.getConsentGroups().get(1).getNumberOfParticipants());
  }

  @Test
  void testGenerateDataUseFromConsentGroup() {
    ConsentGroup consentGroup = new ConsentGroup();
    consentGroup.setGeneralResearchUse(false);
    consentGroup.setHmb(true);
    consentGroup.setDiseaseSpecificUse(List.of("disease1", "disease2"));
    consentGroup.setPoa(true);
    consentGroup.setNmds(true);
    consentGroup.setNpu(true);
    consentGroup.setOtherPrimary("other primary use");
    consentGroup.setOtherSecondary("other secondary use");
    consentGroup.setIrb(true);
    consentGroup.setCol(true);
    consentGroup.setGs("USA");
    consentGroup.setGso(true);
    consentGroup.setPub(true);
    consentGroup.setMor(true);
    consentGroup.setMorDate("2025-12-31");
    DataUse dataUse = datasetRegistrationService.generateDataUseFromConsentGroup(consentGroup);
    assertDataUse(consentGroup, dataUse);
  }

  @Test
  void testRegistrationErrorsOnInvalidDacId() {

    User user = mock();
    DatasetRegistrationSchemaV1 schema = createRandomMinimumDatasetRegistration(user);

    when(dacDAO.findById(any())).thenReturn(null);

    assertThrows(
        NotFoundException.class,
        () -> datasetRegistrationService.createDatasetsFromRegistration(schema, user, Map.of()));
  }

  @Test
  void testRegistrationSucceedsWithESError() throws Exception {
    User user = mock();
    DatasetRegistrationSchemaV1 schema = createRandomMinimumDatasetRegistration(user);
    when(dacDAO.findById(any())).thenReturn(new Dac());
    Dataset dataset = new Dataset();
    dataset.setDacId(1);
    dataset.setDatasetId(1);
    dataset.setStudyId(123);
    when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(dataset));

    when(elasticSearchService.indexDatasets(any()))
        .thenThrow(new ServerErrorException("Timeout connecting to [elasticsearch]", 500));
    assertDoesNotThrow(
        () -> {
          datasetRegistrationService.createDatasetsFromRegistration(schema, user, Map.of());
        },
        "Registration Error");
  }

  @Test
  void testUpdateDatasetSucceedsWithESError() {
    User user = mock();
    Dac dac = new Dac();
    dac.setDacId(randomInt(1, 100));
    Dataset dataset = new Dataset();
    dataset.setDatasetId(randomInt(1, 100));
    dataset.setDacId(dac.getDacId());
    String name = randomAlphabetic(10);
    org.broadinstitute.consent.http.models.DatasetUpdate update =
        new org.broadinstitute.consent.http.models.DatasetUpdate(name, dac.getDacId(), List.of());
    when(datasetDAO.findDatasetById(any())).thenReturn(dataset);

    assertDoesNotThrow(
        () -> {
          datasetRegistrationService.updateDataset(dataset.getDatasetId(), user, update, Map.of());
        },
        "Update Error");
  }

  @Test
  void testExtractStudyProperty() {
    DatasetRegistrationService.StudyPropertyExtractor extractor =
        new DatasetRegistrationService.StudyPropertyExtractor(
            randomAlphabetic(10), PropertyType.String, DatasetRegistrationSchemaV1::getStudyName);

    DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();

    // null value -> empty extraction
    assertTrue(extractor.extract(schemaV1).isEmpty());

    schemaV1.setStudyName(randomAlphabetic(10));

    Optional<StudyProperty> prop = extractor.extract(schemaV1);

    // non-null value -> turn value into dataset prop
    assertTrue(prop.isPresent());

    assertEquals(schemaV1.getStudyName(), prop.get().getValue());
    assertEquals(extractor.key(), prop.get().getKey());
    assertEquals(extractor.type(), prop.get().getType());
  }

  @Test
  void testExtractDatasetProperty() {
    DatasetRegistrationService.DatasetPropertyExtractor extractor =
        new DatasetRegistrationService.DatasetPropertyExtractor(
            randomAlphabetic(10),
            randomAlphabetic(10),
            PropertyType.String,
            ConsentGroup::getConsentGroupName);

    ConsentGroup group = new ConsentGroup();

    // null value -> empty extraction
    assertTrue(extractor.extract(group).isEmpty());

    group.setConsentGroupName(randomAlphabetic(10));

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
    DatasetRegistrationService.StudyPropertyExtractor extractor =
        new DatasetRegistrationService.StudyPropertyExtractor(
            randomAlphabetic(10),
            PropertyType.Json,
            registration -> GsonUtil.getInstance().toJson(registration.getDataTypes()));

    DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();

    schemaV1.setDataTypes(List.of("type1", "type2", "type3"));

    Optional<StudyProperty> prop = extractor.extract(schemaV1);

    assertTrue(prop.isPresent());

    assertEquals(GsonUtil.getInstance().toJsonTree(schemaV1.getDataTypes()), prop.get().getValue());
    assertEquals(extractor.key(), prop.get().getKey());
    assertEquals(extractor.type(), prop.get().getType());
  }

  @Test
  void testExtractDatasetPropertyTyped() {
    DatasetRegistrationService.DatasetPropertyExtractor extractor =
        new DatasetRegistrationService.DatasetPropertyExtractor(
            randomAlphabetic(10),
            randomAlphabetic(10),
            PropertyType.Json,
            consentGroup -> GsonUtil.getInstance().toJson(consentGroup.getDiseaseSpecificUse()));

    ConsentGroup group = new ConsentGroup();

    group.setDiseaseSpecificUse(List.of("asdf", "sdfg", "dfgh"));

    Optional<DatasetProperty> prop = extractor.extract(group);

    assertTrue(prop.isPresent());

    assertEquals(
        GsonUtil.getInstance().toJsonTree(group.getDiseaseSpecificUse()),
        prop.get().getPropertyValue());
    assertEquals(extractor.name(), prop.get().getPropertyName());
    assertEquals(extractor.schemaProp(), prop.get().getSchemaProperty());
    assertEquals(extractor.type(), prop.get().getPropertyType());
  }

  @Test
  void cleanupEmptyDatasetNihInstitutionalCertificationFile() {
    User user = new User();
    user.setUserId(1);

    Study study1 = new Study();
    study1.setStudyId(5);
    study1.setUuid(UUID.randomUUID());
    FileStorageObject alternateSharingPlan1 = new FileStorageObject();
    alternateSharingPlan1.setFileStorageObjectId(92);
    BlobId asp2BlobId = mock(BlobId.class);
    alternateSharingPlan1.setBlobId(asp2BlobId);
    study1.setAlternativeDataSharingPlan(alternateSharingPlan1);

    Dataset dataset1 = new Dataset();
    dataset1.setDatasetId(1);
    FileStorageObject fso1 = new FileStorageObject();
    fso1.setFileStorageObjectId(7);
    BlobId fso1BlobId = mock(BlobId.class);
    fso1.setBlobId(fso1BlobId);
    dataset1.setNihInstitutionalCertificationFile(fso1);
    dataset1.setStudy(study1);

    Dataset dataset2 = new Dataset();
    dataset2.setDatasetId(2);
    Study study7 = new Study();
    study7.setStudyId(8);
    dataset2.setStudy(study7);

    Study study3 = new Study();
    study3.setStudyId(9);
    study3.setUuid(UUID.randomUUID());
    FileStorageObject alternateSharingPlan3 = new FileStorageObject();
    alternateSharingPlan3.setFileStorageObjectId(91);
    BlobId asp3BlobId = mock(BlobId.class);
    String asp3Name = "ASP3";
    alternateSharingPlan3.setBlobId(asp3BlobId);
    study3.setAlternativeDataSharingPlan(alternateSharingPlan3);

    Dataset dataset3 = new Dataset();
    FileStorageObject fso3 = new FileStorageObject();
    fso3.setFileStorageObjectId(8);
    dataset3.setDatasetId(3);
    BlobId fso3BlobId = mock(BlobId.class);
    fso3.setBlobId(fso3BlobId);
    String fso3Name = "FSO3";
    dataset3.setNihInstitutionalCertificationFile(fso3);
    dataset3.setStudy(study3);

    Dataset dataset4 = new Dataset();
    FileStorageObject fso4 = new FileStorageObject();
    fso4.setFileStorageObjectId(9);
    dataset4.setDatasetId(4);
    BlobId fso4BlobId = mock(BlobId.class);
    fso4.setBlobId(fso4BlobId);
    dataset4.setNihInstitutionalCertificationFile(fso4);
    Study study2 = new Study();
    study2.setStudyId(4);
    study2.setUuid(UUID.randomUUID());
    FileStorageObject alternateSharingPlan = new FileStorageObject();
    BlobId asp1BlobId = mock(BlobId.class);
    alternateSharingPlan.setBlobId(asp1BlobId);
    alternateSharingPlan.setFileStorageObjectId(90);
    study2.setAlternativeDataSharingPlan(alternateSharingPlan);
    dataset4.setStudy(study2);

    List<Dataset> datasetList = List.of(dataset1, dataset2, dataset3, dataset4);
    List<Integer> datasetIds = datasetList.stream().map(Dataset::getDatasetId).toList();
    when(datasetDAO.findAllDatasetIds()).thenReturn(datasetIds);
    when(datasetDAO.findDatasetById(dataset1.getDatasetId())).thenReturn(dataset1);
    when(datasetDAO.findDatasetById(dataset2.getDatasetId())).thenReturn(dataset2);
    when(datasetDAO.findDatasetById(dataset3.getDatasetId())).thenReturn(dataset3);
    when(datasetDAO.findDatasetById(dataset4.getDatasetId())).thenReturn(dataset4);
    when(studyDAO.findStudyById(study2.getStudyId())).thenReturn(study2);
    when(studyDAO.findStudyById(study1.getStudyId())).thenReturn(study1);
    when(studyDAO.findStudyById(study3.getStudyId())).thenReturn(study3);
    when(studyDAO.findStudyById(study7.getStudyId())).thenReturn(study7);
    when(fso3BlobId.getName()).thenReturn(fso3Name);
    when(asp3BlobId.getName()).thenReturn(asp3Name);
    doThrow(new NotFoundException("File not found")).when(gcsService).hasBytes(fso3BlobId);
    doThrow(new NotFoundException("File not found")).when(gcsService).deleteDocument(fso3Name);
    doThrow(new NotFoundException("File not found")).when(gcsService).hasBytes(asp3BlobId);
    doThrow(new NotFoundException("File not found")).when(gcsService).deleteDocument(asp3Name);

    when(gcsService.hasBytes(fso1BlobId)).thenReturn(true);
    when(gcsService.hasBytes(fso4BlobId)).thenReturn(false);
    when(gcsService.hasBytes(asp1BlobId)).thenReturn(true);
    when(gcsService.hasBytes(asp2BlobId)).thenReturn(false);

    assertDoesNotThrow(
        () -> datasetRegistrationService.cleanupDatasetsAndStudiesWithEmptyFiles(user));

    verify(gcsService, times(4)).deleteDocument(any());
    verify(fileStorageObjectDAO, times(4)).deleteFileById(anyInt(), anyInt());
  }

  @Test
  void testDeleteFile_No_File() {
    assertDoesNotThrow(() -> datasetRegistrationService.deleteFile(null, new User()));
  }

  @Test
  void testDeleteFile_No_User() {
    assertDoesNotThrow(() -> datasetRegistrationService.deleteFile(new FileStorageObject(), null));
  }

  private void assertDataUse(ConsentGroup consentGroup, DataUse dataUse) {
    assertEquals(consentGroup.getCol(), dataUse.getCollaboratorRequired());
    assertEquals(consentGroup.getDiseaseSpecificUse(), dataUse.getDiseaseRestrictions());
    assertEquals(consentGroup.getIrb(), dataUse.getEthicsApprovalRequired());
    assertEquals(consentGroup.getGeneralResearchUse(), dataUse.getGeneralUse());
    assertEquals(consentGroup.getGs(), dataUse.getGeographicalRestrictions());
    assertEquals(consentGroup.getGso(), dataUse.getGeneticStudiesOnly());
    assertEquals(consentGroup.getHmb(), dataUse.getHmbResearch());
    assertEquals(consentGroup.getMorDate(), dataUse.getPublicationMoratorium());
    // NMDS is an inverse condition flag:
    // Methods research (analytic/software/technology development) is prohibited
    // https://github.com/EBISPOT/DUO
    if (Objects.isNull(consentGroup.getNmds()) || !consentGroup.getNmds()) {
      assertNull(dataUse.getMethodsResearch());
    } else {
      assertFalse(dataUse.getMethodsResearch());
    }
    assertEquals(consentGroup.getNpu(), dataUse.getNonProfitUse());
    assertEquals(consentGroup.getOtherPrimary(), dataUse.getOther());
    assertEquals(consentGroup.getOtherSecondary(), dataUse.getSecondaryOther());
    assertEquals(consentGroup.getPoa(), dataUse.getPopulationOriginsAncestry());
    assertEquals(consentGroup.getPub(), dataUse.getPublicationResults());
  }

  private void assertContainsDatasetProperty(
      Collection<DatasetProperty> props, String schema, Object value) {
    Optional<DatasetProperty> prop =
        props.stream().filter(p -> p.getSchemaProperty().equals(schema)).findFirst();
    assertTrue(prop.isPresent());
    assertEquals(value, prop.get().getPropertyValue());
  }

  private void assertContainsStudyProperty(
      Collection<StudyProperty> props, String key, Object value) {
    Optional<StudyProperty> prop = props.stream().filter(p -> p.getKey().equals(key)).findFirst();
    assertTrue(prop.isPresent());
    assertEquals(value, prop.get().getValue());
  }

  private DatasetRegistrationSchemaV1 createRandomMinimumDatasetRegistration(User user) {
    DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();
    schemaV1.setStudyName(randomAlphabetic(10));
    schemaV1.setStudyDescription(randomAlphabetic(10));
    schemaV1.setDataTypes(List.of(randomAlphabetic(10)));
    schemaV1.setPhenotypeIndication(randomAlphabetic(10));
    schemaV1.setSpecies(randomAlphabetic(10));
    schemaV1.setPiName(randomAlphabetic(10));
    when(user.getUserId()).thenReturn(1);
    schemaV1.setDataSubmitterUserId(user.getUserId());
    schemaV1.setDataCustodianEmail(List.of(randomAlphabetic(10) + "@domain.org"));
    schemaV1.setPublicVisibility(true);

    ConsentGroup consentGroup = new ConsentGroup();
    consentGroup.setConsentGroupName(randomAlphabetic(10));
    consentGroup.setGeneralResearchUse(true);
    FileTypeObject fileType = new FileTypeObject();
    fileType.setFileType(FileTypeObject.FileType.ARRAYS);
    fileType.setFunctionalEquivalence(randomAlphabetic(10));
    consentGroup.setNumberOfParticipants(new Random().nextInt());
    consentGroup.setFileTypes(List.of(fileType));
    consentGroup.setDataAccessCommitteeId(new Random().nextInt());

    schemaV1.setConsentGroups(List.of(consentGroup));
    return schemaV1;
  }

  private DatasetRegistrationSchemaV1 createAccessManagementRegistrationNoDacId(User user) {
    DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();
    schemaV1.setStudyName(randomAlphabetic(10));
    schemaV1.setStudyType(DatasetRegistrationSchemaV1.StudyType.OBSERVATIONAL);
    schemaV1.setStudyDescription(randomAlphabetic(10));
    schemaV1.setDataTypes(List.of(randomAlphabetic(10)));
    schemaV1.setPhenotypeIndication(randomAlphabetic(10));
    schemaV1.setSpecies(randomAlphabetic(10));
    schemaV1.setPiName(randomAlphabetic(10));
    when(user.getUserId()).thenReturn(1);
    schemaV1.setDataSubmitterUserId(user.getUserId());
    schemaV1.setDataCustodianEmail(List.of(randomAlphabetic(10) + "@domain.org"));
    schemaV1.setPublicVisibility(true);

    ConsentGroup consentGroup = new ConsentGroup();
    consentGroup.setConsentGroupName(randomAlphabetic(10));
    consentGroup.setAccessManagement(AccessManagement.OPEN);
    FileTypeObject fileType = new FileTypeObject();
    fileType.setFileType(FileTypeObject.FileType.ARRAYS);
    fileType.setFunctionalEquivalence(randomAlphabetic(10));
    consentGroup.setNumberOfParticipants(new Random().nextInt());
    consentGroup.setFileTypes(List.of(fileType));

    schemaV1.setConsentGroups(List.of(consentGroup));
    return schemaV1;
  }

  private DatasetRegistrationSchemaV1 createRandomMultipleDatasetRegistration(User user) {
    DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();
    schemaV1.setStudyName(randomAlphabetic(10));
    schemaV1.setStudyType(DatasetRegistrationSchemaV1.StudyType.OBSERVATIONAL);
    schemaV1.setStudyDescription(randomAlphabetic(10));
    schemaV1.setDataTypes(List.of(randomAlphabetic(10)));
    schemaV1.setPhenotypeIndication(randomAlphabetic(10));
    schemaV1.setSpecies(randomAlphabetic(10));
    schemaV1.setPiName(randomAlphabetic(10));
    when(user.getUserId()).thenReturn(1);
    schemaV1.setDataSubmitterUserId(user.getUserId());
    schemaV1.setDataCustodianEmail(List.of(randomAlphabetic(10) + "@domain.org"));
    schemaV1.setPublicVisibility(true);

    ConsentGroup consentGroup1 = new ConsentGroup();
    consentGroup1.setConsentGroupName(randomAlphabetic(10));
    consentGroup1.setGeneralResearchUse(true);
    FileTypeObject fileType1 = new FileTypeObject();
    fileType1.setFileType(FileTypeObject.FileType.ARRAYS);
    fileType1.setFunctionalEquivalence(randomAlphabetic(10));
    consentGroup1.setNumberOfParticipants(new Random().nextInt());
    consentGroup1.setFileTypes(List.of(fileType1));
    consentGroup1.setDataAccessCommitteeId(new Random().nextInt());
    consentGroup1.setAccessManagement(AccessManagement.CONTROLLED);

    ConsentGroup consentGroup2 = new ConsentGroup();
    consentGroup2.setConsentGroupName(randomAlphabetic(10));
    consentGroup2.setGeneralResearchUse(true);
    FileTypeObject fileType2 = new FileTypeObject();
    fileType2.setFileType(FileTypeObject.FileType.ARRAYS);
    fileType2.setFunctionalEquivalence(randomAlphabetic(10));
    consentGroup2.setNumberOfParticipants(new Random().nextInt());
    consentGroup2.setFileTypes(List.of(fileType2));
    consentGroup2.setAccessManagement(AccessManagement.OPEN);

    schemaV1.setConsentGroups(List.of(consentGroup1, consentGroup2));
    return schemaV1;
  }

  private DatasetRegistrationSchemaV1 createRandomCompleteDatasetRegistration(User user) {
    // TODO: find a better way to initialize this object
    DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();
    schemaV1.setStudyName(randomAlphabetic(10));
    schemaV1.setStudyType(DatasetRegistrationSchemaV1.StudyType.OBSERVATIONAL);
    schemaV1.setStudyDescription(randomAlphabetic(10));
    schemaV1.setDataTypes(List.of(randomAlphabetic(10)));
    schemaV1.setPhenotypeIndication(randomAlphabetic(10));
    schemaV1.setSpecies(randomAlphabetic(10));
    schemaV1.setPiName(randomAlphabetic(10));
    when(user.getUserId()).thenReturn(1);
    schemaV1.setDataSubmitterUserId(user.getUserId());
    schemaV1.setDataCustodianEmail(List.of(randomAlphabetic(10) + "@domain.org"));
    schemaV1.setPublicVisibility(true);
    schemaV1.setSubmittingToAnvil(true);
    schemaV1.setDbGaPPhsID(randomAlphabetic(10));
    schemaV1.setDbGaPStudyRegistrationName(randomAlphabetic(10));
    schemaV1.setEmbargoReleaseDate("2007-12-03");
    schemaV1.setSequencingCenter(randomAlphabetic(10));
    schemaV1.setNihAnvilUse(
        DatasetRegistrationSchemaV1.NihAnvilUse
            .I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL);
    schemaV1.setNihGrantContractNumber(randomAlphabetic(10));
    schemaV1.setNihICsSupportingStudy(List.of(NihICsSupportingStudy.CC, NihICsSupportingStudy.CIT));
    schemaV1.setNihProgramOfficerName(randomAlphabetic(10));
    schemaV1.setNihInstitutionCenterSubmission(
        DatasetRegistrationSchemaV1.NihInstitutionCenterSubmission.CSR);
    schemaV1.setNihGenomicProgramAdministratorName(randomAlphabetic(10));
    schemaV1.setMultiCenterStudy(true);
    schemaV1.setCollaboratingSites(List.of(randomAlphabetic(10), randomAlphabetic(10)));
    schemaV1.setControlledAccessRequiredForGenomicSummaryResultsGSR(true);
    schemaV1.setControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation(
        randomAlphabetic(10));
    schemaV1.setAlternativeDataSharingPlan(true);
    schemaV1.setAlternativeDataSharingPlanReasons(
        List.of(
            AlternativeDataSharingPlanReason
                .INFORMED_CONSENT_PROCESSES_ARE_INADEQUATE_TO_SUPPORT_DATA_FOR_SHARING_FOR_THE_FOLLOWING_REASONS));
    schemaV1.setAlternativeDataSharingPlanExplanation(randomAlphabetic(10));
    schemaV1.setAlternativeDataSharingPlanFileName(randomAlphabetic(10));
    schemaV1.setAlternativeDataSharingPlanDataSubmitted(
        DatasetRegistrationSchemaV1.AlternativeDataSharingPlanDataSubmitted
            .WITHIN_3_MONTHS_OF_THE_LAST_DATA_GENERATED_OR_LAST_CLINICAL_VISIT);
    schemaV1.setAlternativeDataSharingPlanDataReleased(true);
    schemaV1.setAlternativeDataSharingPlanTargetDeliveryDate("2011-11-11");
    schemaV1.setAlternativeDataSharingPlanTargetPublicReleaseDate("2012-10-08");
    schemaV1.setAlternativeDataSharingPlanAccessManagement(
        AlternativeDataSharingPlanAccessManagement.OPEN_ACCESS);
    schemaV1.setPiInstitution(10);

    ConsentGroup consentGroup = new ConsentGroup();
    consentGroup.setConsentGroupName(randomAlphabetic(10));
    consentGroup.setGeneralResearchUse(true);
    consentGroup.setNumberOfParticipants(new Random().nextInt());
    FileTypeObject fileType1 = new FileTypeObject();
    fileType1.setFileType(FileTypeObject.FileType.ARRAYS);
    fileType1.setFunctionalEquivalence(randomAlphabetic(10));
    FileTypeObject fileType2 = new FileTypeObject();
    fileType2.setFileType(FileTypeObject.FileType.PHENOTYPE);
    fileType2.setFunctionalEquivalence(randomAlphabetic(10));
    consentGroup.setFileTypes(List.of(fileType1, fileType2));
    consentGroup.setUrl(URI.create("https://asdf.gov"));
    consentGroup.setMor(false);
    consentGroup.setNmds(false);
    consentGroup.setNpu(false);
    consentGroup.setAccessManagement(AccessManagement.CONTROLLED);
    consentGroup.setDataLocation(ConsentGroup.DataLocation.TDR_LOCATION);
    consentGroup.setDataAccessCommitteeId(new Random().nextInt());
    schemaV1.setAssets(Map.of("key", List.of("value1", "value2")));
    schemaV1.setConsentGroups(List.of(consentGroup));
    return schemaV1;
  }

  @Test
  void testCreateDatasetRegistrationWithBlankConsentGroupName() {
    User user = mock();
    DatasetRegistrationSchemaV1 schema = createRandomMinimumDatasetRegistration(user);

    // Set consent group name to blank
    schema.getConsentGroups().getFirst().setConsentGroupName("");

    assertThrows(BadRequestException.class, () -> invokeCreateRegistration(schema, user));
  }

  @Test
  void testCreateDatasetRegistrationWithNullConsentGroupName() {
    User user = mock();
    DatasetRegistrationSchemaV1 schema = createRandomMinimumDatasetRegistration(user);

    // Set consent group name to null
    schema.getConsentGroups().getFirst().setConsentGroupName(null);

    assertThrows(BadRequestException.class, () -> invokeCreateRegistration(schema, user));
  }

  private void invokeCreateRegistration(DatasetRegistrationSchemaV1 schema, User user)
      throws SQLException, IOException {
    datasetRegistrationService.createDatasetsFromRegistration(schema, user, Map.of());
  }
}
