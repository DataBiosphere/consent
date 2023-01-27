package org.broadinstitute.consent.http.service;

import com.google.cloud.storage.BlobId;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.enumeration.DatasetPropertyType;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dataset_registration_v1.AlternativeDataSharingPlanReason;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DataLocation;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject;
import org.broadinstitute.consent.http.models.dataset_registration_v1.NihAnvilUse;
import org.broadinstitute.consent.http.models.dataset_registration_v1.NihICsSupportingStudy;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatasetRegistrationServiceTest {

    private DatasetRegistrationService datasetRegistrationService;

    @Mock
    private DatasetDAO datasetDAO;

    @Mock
    private DacDAO dacDAO;

    @Mock
    private DatasetServiceDAO datasetServiceDAO;

    @Mock
    private GCSService gcsService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private void initService() {
        datasetRegistrationService = new DatasetRegistrationService(datasetDAO, dacDAO, datasetServiceDAO, gcsService);
    }


    // captor: allows you to inspect the arguments sent to a function.
    @Captor
    ArgumentCaptor<List<DatasetServiceDAO.DatasetInsert>> datasetInsertCaptor;

    // ------------------------ test multiple dataset insert ----------------------------------- //
    @Test
    public void testInsertCompleteDatasetRegistration() throws SQLException, IOException {
        User user = mock();
        DatasetRegistrationSchemaV1 schema = createRandomCompleteDatasetRegistration(user);

        spy(gcsService);

        initService();

        Map<String, FormDataBodyPart> files = Map.of("alternativeDataSharingPlan", createFormDataBodyPart(), "consentGroups[0].nihInstitutionalCertificationFile", createFormDataBodyPart(), "otherUnused", createFormDataBodyPart());
        when(gcsService.storeDocument(any(), any(), any())).thenReturn(BlobId.of("asdf", "hjkl"), BlobId.of("qwer", "tyuio"));
        when(dacDAO.findById(any())).thenReturn(new Dac());

        datasetRegistrationService.createDatasetsFromRegistration(schema, user, files);

        verify(datasetServiceDAO).insertDatasets(datasetInsertCaptor.capture());

        // only two files are stored; extra "unused" file not used
        verify(gcsService, times(2)).storeDocument(any(), any(), any());

        List<DatasetServiceDAO.DatasetInsert> inserts = datasetInsertCaptor.getValue();

        assertEquals(1, inserts.size());

        assertEquals(schema.getDataAccessCommitteeId(), inserts.get(0).dacId());
        assertEquals(schema.getConsentGroups().get(0).getConsentGroupName(), inserts.get(0).name());
        assertDataUse(schema.getConsentGroups().get(0), inserts.get(0).dataUse());
        assertEquals(user.getUserId(), inserts.get(0).userId());

        assertEquals(2, inserts.get(0).files().size());
        assertEquals(FileCategory.ALTERNATIVE_DATA_SHARING_PLAN, inserts.get(0).files().get(0).getCategory());
        assertEquals(files.get("alternativeDataSharingPlan").getContentDisposition().getFileName(), inserts.get(0).files().get(0).getFileName());
        assertEquals(BlobId.of("asdf", "hjkl"), inserts.get(0).files().get(0).getBlobId());

        assertEquals(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION, inserts.get(0).files().get(1).getCategory());
        assertEquals(files.get("consentGroups[0].nihInstitutionalCertificationFile").getContentDisposition().getFileName(), inserts.get(0).files().get(1).getFileName());
        assertEquals(BlobId.of("qwer", "tyuio"), inserts.get(0).files().get(1).getBlobId());


        List<DatasetProperty> props = inserts.get(0).props();
        assertContainsDatasetProperty(props, "piName", schema.getPiName());
        assertContainsDatasetProperty(props, "studyName", schema.getStudyName());
        assertContainsDatasetProperty(props, "studyType", schema.getStudyType().value());
        assertContainsDatasetProperty(props, "dataTypes", DatasetPropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getDataTypes())));
        assertContainsDatasetProperty(props, "studyDescription", schema.getStudyDescription());
        assertContainsDatasetProperty(props, "phenotypeIndication", schema.getPhenotypeIndication());
        assertContainsDatasetProperty(props, "species", schema.getSpecies());
        assertContainsDatasetProperty(props, "dataSubmitterUserId", schema.getDataSubmitterUserId());
        assertContainsDatasetProperty(props, "dataCustodianEmail", DatasetPropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getDataCustodianEmail())));
        assertContainsDatasetProperty(props, "publicVisibility", schema.getPublicVisibility());
        assertContainsDatasetProperty(props, "nihAnvilUse", DatasetPropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getNihAnvilUse().stream().map(NihAnvilUse::value).toList())));
        assertContainsDatasetProperty(props, "submittingToAnvil", schema.getSubmittingToAnvil());
        assertContainsDatasetProperty(props, "dbGaPPhsID", schema.getDbGaPPhsID());
        assertContainsDatasetProperty(props, "dbGaPStudyRegistrationName", schema.getDbGaPStudyRegistrationName());
        assertContainsDatasetProperty(props, "embargoReleaseDate", DatasetPropertyType.coerceToDate(schema.getEmbargoReleaseDate()));
        assertContainsDatasetProperty(props, "sequencingCenter", schema.getSequencingCenter());
        assertContainsDatasetProperty(props, "piEmail", schema.getPiEmail());
        assertContainsDatasetProperty(props, "piInstitution", schema.getPiInstitution());
        assertContainsDatasetProperty(props, "nihGrantContractNumber", schema.getNihGrantContractNumber());
        assertContainsDatasetProperty(props, "nihICsSupportingStudy", DatasetPropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getNihICsSupportingStudy().stream().map(NihICsSupportingStudy::value).toList())));
        assertContainsDatasetProperty(props, "nihProgramOfficerName", schema.getNihProgramOfficerName());
        assertContainsDatasetProperty(props, "nihInstitutionCenterSubmission", schema.getNihInstitutionCenterSubmission().value());
        assertContainsDatasetProperty(props, "nihGenomicProgramAdministratorName", schema.getNihGenomicProgramAdministratorName());
        assertContainsDatasetProperty(props, "multiCenterStudy", schema.getMultiCenterStudy());
        assertContainsDatasetProperty(props, "collaboratingSites", DatasetPropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getCollaboratingSites())));
        assertContainsDatasetProperty(props, "controlledAccessRequiredForGenomicSummaryResultsGSR", schema.getControlledAccessRequiredForGenomicSummaryResultsGSR());
        assertContainsDatasetProperty(props, "controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation", schema.getControlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation());
        assertContainsDatasetProperty(props, "alternativeDataSharingPlan", schema.getAlternativeDataSharingPlan());
        assertContainsDatasetProperty(props, "alternativeDataSharingPlanReasons", DatasetPropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getAlternativeDataSharingPlanReasons().stream().map(AlternativeDataSharingPlanReason::value).toList())));
        assertContainsDatasetProperty(props, "alternativeDataSharingPlanExplanation", schema.getAlternativeDataSharingPlanExplanation());
        assertContainsDatasetProperty(props, "alternativeDataSharingPlanFileName", schema.getAlternativeDataSharingPlanFileName());
        assertContainsDatasetProperty(props, "alternativeDataSharingPlanDataSubmitted", schema.getAlternativeDataSharingPlanDataSubmitted().value());
        assertContainsDatasetProperty(props, "alternativeDataSharingPlanDataReleased", schema.getAlternativeDataSharingPlanDataReleased());
        assertContainsDatasetProperty(props, "alternativeDataSharingPlanTargetDeliveryDate", DatasetPropertyType.Date.coerce(schema.getAlternativeDataSharingPlanTargetDeliveryDate()));
        assertContainsDatasetProperty(props, "alternativeDataSharingPlanTargetPublicReleaseDate", DatasetPropertyType.Date.coerce(schema.getAlternativeDataSharingPlanTargetPublicReleaseDate()));
        assertContainsDatasetProperty(props, "alternativeDataSharingPlanControlledOpenAccess", schema.getAlternativeDataSharingPlanControlledOpenAccess().value());
        assertContainsDatasetProperty(props, "consentGroup.dataLocation", DatasetPropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getConsentGroups().get(0).getDataLocation().stream().map(DataLocation::value).toList())));
        assertContainsDatasetProperty(props, "consentGroup.fileTypes", DatasetPropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getConsentGroups().get(0).getFileTypes())));
        assertContainsDatasetProperty(props, "consentGroup.url", schema.getConsentGroups().get(0).getUrl().toString());
    }


    // inserts only required fields to ensure that null fields are ok
    @Test
    public void testInsertMinimumDatasetRegistration() throws SQLException, IOException {
        User user = mock();
        DatasetRegistrationSchemaV1 schema = createRandomMinimumDatasetRegistration(user);

        spy(gcsService);

        initService();
        when(dacDAO.findById(any())).thenReturn(new Dac());

        datasetRegistrationService.createDatasetsFromRegistration(schema, user, Map.of());

        verify(datasetServiceDAO).insertDatasets(datasetInsertCaptor.capture());

        verify(gcsService, times(0)).storeDocument(any(), any(), any());

        List<DatasetServiceDAO.DatasetInsert> inserts = datasetInsertCaptor.getValue();

        assertEquals(1, inserts.size());

        assertEquals(schema.getDataAccessCommitteeId(), inserts.get(0).dacId());
        assertEquals(schema.getConsentGroups().get(0).getConsentGroupName(), inserts.get(0).name());

        ConsentGroup consentGroup = schema.getConsentGroups().get(0);
        DataUse dataUse = inserts.get(0).dataUse();

        assertEquals(consentGroup.getGeneralResearchUse(), dataUse.getGeneralUse());


        assertEquals(user.getUserId(), inserts.get(0).userId());

        assertEquals(0, inserts.get(0).files().size());


        List<DatasetProperty> props = inserts.get(0).props();
        assertContainsDatasetProperty(props, "piName", schema.getPiName());
        assertContainsDatasetProperty(props, "studyName", schema.getStudyName());
        assertContainsDatasetProperty(props, "studyType", schema.getStudyType().value());
        assertContainsDatasetProperty(props, "dataTypes", DatasetPropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getDataTypes())));
        assertContainsDatasetProperty(props, "studyDescription", schema.getStudyDescription());
        assertContainsDatasetProperty(props, "phenotypeIndication", schema.getPhenotypeIndication());
        assertContainsDatasetProperty(props, "species", schema.getSpecies());
        assertContainsDatasetProperty(props, "dataSubmitterUserId", schema.getDataSubmitterUserId());
        assertContainsDatasetProperty(props, "consentGroup.fileTypes", DatasetPropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getConsentGroups().get(0).getFileTypes())));
    }


    // test inset multiple consent groups
    @Test
    public void testInsertMultipleDatasetRegistration() throws SQLException, IOException {
        User user = mock();
        DatasetRegistrationSchemaV1 schema = createRandomMultipleDatasetRegistration(user);


        spy(gcsService);

        initService();

        when(dacDAO.findById(any())).thenReturn(new Dac());
        Map<String, FormDataBodyPart> files = Map.of("alternativeDataSharingPlan", createFormDataBodyPart(), "consentGroups[0].nihInstitutionalCertificationFile", createFormDataBodyPart(), "otherUnused", createFormDataBodyPart());
        when(gcsService.storeDocument(any(), any(), any())).thenReturn(BlobId.of("asdf", "hjkl"), BlobId.of("qwer", "tyuio"));


        datasetRegistrationService.createDatasetsFromRegistration(schema, user, files);

        verify(datasetServiceDAO).insertDatasets(datasetInsertCaptor.capture());

        // only two files are stored; extra "unused" file not used
        verify(gcsService, times(2)).storeDocument(any(), any(), any());

        List<DatasetServiceDAO.DatasetInsert> inserts = datasetInsertCaptor.getValue();

        assertEquals(2, inserts.size());

        // check first dataset insert is ok

        assertEquals(schema.getDataAccessCommitteeId(), inserts.get(0).dacId());
        assertEquals(schema.getConsentGroups().get(0).getConsentGroupName(), inserts.get(0).name());

        ConsentGroup consentGroup = schema.getConsentGroups().get(0);
        DataUse dataUse = inserts.get(0).dataUse();

        assertEquals(consentGroup.getGeneralResearchUse(), dataUse.getGeneralUse());

        assertEquals(user.getUserId(), inserts.get(0).userId());

        assertEquals(2, inserts.get(0).files().size());

        List<DatasetProperty> props = inserts.get(0).props();
        assertContainsDatasetProperty(props, "piName", schema.getPiName());
        assertContainsDatasetProperty(props, "studyName", schema.getStudyName());
        assertContainsDatasetProperty(props, "studyType", schema.getStudyType().value());
        assertContainsDatasetProperty(props, "dataTypes", DatasetPropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getDataTypes())));
        assertContainsDatasetProperty(props, "studyDescription", schema.getStudyDescription());
        assertContainsDatasetProperty(props, "phenotypeIndication", schema.getPhenotypeIndication());
        assertContainsDatasetProperty(props, "species", schema.getSpecies());
        assertContainsDatasetProperty(props, "dataSubmitterUserId", schema.getDataSubmitterUserId());
        assertContainsDatasetProperty(props, "consentGroup.fileTypes", DatasetPropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getConsentGroups().get(0).getFileTypes())));



        // assert on all the same properties, but for the second dataset

        assertEquals(schema.getDataAccessCommitteeId(), inserts.get(1).dacId());
        assertEquals(schema.getConsentGroups().get(1).getConsentGroupName(), inserts.get(1).name());

        ConsentGroup consentGroup2 = schema.getConsentGroups().get(1);
        DataUse dataUse2 = inserts.get(1).dataUse();

        assertEquals(consentGroup2.getGeneralResearchUse(), dataUse2.getGeneralUse());

        assertEquals(user.getUserId(), inserts.get(1).userId());

        assertEquals(1, inserts.get(1).files().size());

        List<DatasetProperty> props2 = inserts.get(1).props();
        assertContainsDatasetProperty(props2, "piName", schema.getPiName());
        assertContainsDatasetProperty(props2, "studyName", schema.getStudyName());
        assertContainsDatasetProperty(props2, "studyType", schema.getStudyType().value());
        assertContainsDatasetProperty(props2, "dataTypes", DatasetPropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getDataTypes())));
        assertContainsDatasetProperty(props2, "studyDescription", schema.getStudyDescription());
        assertContainsDatasetProperty(props2, "phenotypeIndication", schema.getPhenotypeIndication());
        assertContainsDatasetProperty(props2, "species", schema.getSpecies());
        assertContainsDatasetProperty(props2, "dataSubmitterUserId", schema.getDataSubmitterUserId());
        assertContainsDatasetProperty(props2, "consentGroup.fileTypes", DatasetPropertyType.coerceToJson(GsonUtil.getInstance().toJson(schema.getConsentGroups().get(1).getFileTypes())));
    }

    @Test(expected = NotFoundException.class)
    public void testRegistrationErrorsOnInvalidDacId() throws SQLException, IOException {

        User user = mock();
        DatasetRegistrationSchemaV1 schema = createRandomMinimumDatasetRegistration(user);

        when(dacDAO.findById(any())).thenReturn(null);
        spy(gcsService);

        initService();
        datasetRegistrationService.createDatasetsFromRegistration(schema, user, Map.of());
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

    private void assertContainsDatasetProperty(List<DatasetProperty> props, String schema, Object value) {
        Optional<DatasetProperty> prop = props.stream().filter((p) -> p.getSchemaProperty().equals(schema)).findFirst();
        assertTrue(prop.isPresent());
        assertEquals(value, prop.get().getPropertyValue());
    }

    // test insert minimum properties
    // test insert multiple consent groups


    private FormDataBodyPart createFormDataBodyPart() {
        FormDataContentDisposition content = FormDataContentDisposition
                .name("file")
                .fileName("sharing_plan.txt")
                .build();

        InputStream is = new ByteArrayInputStream("HelloWorld".getBytes(StandardCharsets.UTF_8));
        FormDataBodyPart bodyPart = mock();
        when(bodyPart.getMediaType()).thenReturn(MediaType.TEXT_PLAIN_TYPE);
        when(bodyPart.getContentDisposition()).thenReturn(content);
        when(bodyPart.getValueAs(any())).thenReturn(is);
        return bodyPart;
    }

    private DatasetRegistrationSchemaV1 createRandomMinimumDatasetRegistration(User user) {
        DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();
        schemaV1.setStudyName(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setStudyType(DatasetRegistrationSchemaV1.StudyType.Observational);
        schemaV1.setStudyDescription(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setDataTypes(List.of(RandomStringUtils.randomAlphabetic(10)));
        schemaV1.setPhenotypeIndication(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setSpecies(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setPiName(RandomStringUtils.randomAlphabetic(10));
        when(user.getUserId()).thenReturn(1);
        schemaV1.setDataSubmitterUserId(user.getUserId());
        schemaV1.setDataCustodianEmail(List.of(RandomStringUtils.randomAlphabetic(10)+"@domain.org"));
        schemaV1.setPublicVisibility(true);
        schemaV1.setDataAccessCommitteeId(1);

        ConsentGroup consentGroup = new ConsentGroup();
        consentGroup.setConsentGroupName(RandomStringUtils.randomAlphabetic(10));
        consentGroup.setGeneralResearchUse(true);
        FileTypeObject fileType = new FileTypeObject();
        fileType.setFileType(FileTypeObject.FileType.Arrays);
        fileType.setFunctionalEquivalence(RandomStringUtils.randomAlphabetic(10));
        fileType.setNumberOfParticipants(new Random().nextInt());
        consentGroup.setFileTypes(List.of(fileType));

        schemaV1.setConsentGroups(List.of(consentGroup));
        return schemaV1;
    }

    private DatasetRegistrationSchemaV1 createRandomMultipleDatasetRegistration(User user) {
        DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();
        schemaV1.setStudyName(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setStudyType(DatasetRegistrationSchemaV1.StudyType.Observational);
        schemaV1.setStudyDescription(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setDataTypes(List.of(RandomStringUtils.randomAlphabetic(10)));
        schemaV1.setPhenotypeIndication(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setSpecies(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setPiName(RandomStringUtils.randomAlphabetic(10));
        when(user.getUserId()).thenReturn(1);
        schemaV1.setDataSubmitterUserId(user.getUserId());
        schemaV1.setDataCustodianEmail(List.of(RandomStringUtils.randomAlphabetic(10)+"@domain.org"));
        schemaV1.setPublicVisibility(true);
        schemaV1.setDataAccessCommitteeId(1);

        ConsentGroup consentGroup1 = new ConsentGroup();
        consentGroup1.setConsentGroupName(RandomStringUtils.randomAlphabetic(10));
        consentGroup1.setGeneralResearchUse(true);
        FileTypeObject fileType1 = new FileTypeObject();
        fileType1.setFileType(FileTypeObject.FileType.Arrays);
        fileType1.setFunctionalEquivalence(RandomStringUtils.randomAlphabetic(10));
        fileType1.setNumberOfParticipants(new Random().nextInt());
        consentGroup1.setFileTypes(List.of(fileType1));

        ConsentGroup consentGroup2 = new ConsentGroup();
        consentGroup1.setConsentGroupName(RandomStringUtils.randomAlphabetic(10));
        consentGroup1.setGeneralResearchUse(true);
        FileTypeObject fileType2 = new FileTypeObject();
        fileType2.setFileType(FileTypeObject.FileType.Arrays);
        fileType2.setFunctionalEquivalence(RandomStringUtils.randomAlphabetic(10));
        fileType2.setNumberOfParticipants(new Random().nextInt());
        consentGroup1.setFileTypes(List.of(fileType2));


        schemaV1.setConsentGroups(List.of(consentGroup1, consentGroup2));
        return schemaV1;
    }


    private DatasetRegistrationSchemaV1 createRandomCompleteDatasetRegistration(User user) {
        DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();
        schemaV1.setStudyName(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setStudyType(DatasetRegistrationSchemaV1.StudyType.Observational);
        schemaV1.setStudyDescription(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setDataTypes(List.of(RandomStringUtils.randomAlphabetic(10)));
        schemaV1.setPhenotypeIndication(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setSpecies(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setPiName(RandomStringUtils.randomAlphabetic(10));
        when(user.getUserId()).thenReturn(1);
        schemaV1.setDataSubmitterUserId(user.getUserId());
        schemaV1.setDataCustodianEmail(List.of(RandomStringUtils.randomAlphabetic(10)+"@domain.org"));
        schemaV1.setPublicVisibility(true);
        schemaV1.setDataAccessCommitteeId(1);
        schemaV1.setSubmittingToAnvil(true);
        schemaV1.setDbGaPPhsID(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setDbGaPStudyRegistrationName(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setEmbargoReleaseDate("2007-12-03");
        schemaV1.setSequencingCenter(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setPiEmail(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setNihGrantContractNumber(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setNihICsSupportingStudy(List.of(NihICsSupportingStudy.CC, NihICsSupportingStudy.CIT));
        schemaV1.setNihProgramOfficerName(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setNihInstitutionCenterSubmission(DatasetRegistrationSchemaV1.NihInstitutionCenterSubmission.CSR);
        schemaV1.setNihGenomicProgramAdministratorName(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setMultiCenterStudy(true);
        schemaV1.setCollaboratingSites(List.of(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10)));
        schemaV1.setControlledAccessRequiredForGenomicSummaryResultsGSR(true);
        schemaV1.setControlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setAlternativeDataSharingPlan(true);
        schemaV1.setAlternativeDataSharingPlanReasons(List.of(AlternativeDataSharingPlanReason.Informed_consent_processes_are_inadequate_to_support_data_for_sharing_for_the_following_reasons));
        schemaV1.setAlternativeDataSharingPlanExplanation(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setAlternativeDataSharingPlanFileName(RandomStringUtils.randomAlphabetic(10));
        schemaV1.setAlternativeDataSharingPlanDataSubmitted(DatasetRegistrationSchemaV1.AlternativeDataSharingPlanDataSubmitted.Within_3_months_of_the_last_data_generated_or_last_clinical_visit);
        schemaV1.setAlternativeDataSharingPlanDataReleased(true);
        schemaV1.setAlternativeDataSharingPlanTargetDeliveryDate("2011-11-11");
        schemaV1.setAlternativeDataSharingPlanTargetPublicReleaseDate("2012-10-08");
        schemaV1.setAlternativeDataSharingPlanControlledOpenAccess(DatasetRegistrationSchemaV1.AlternativeDataSharingPlanControlledOpenAccess.OPEN_ACCESS);
        schemaV1.setPiInstitution(10);

        ConsentGroup consentGroup = new ConsentGroup();
        consentGroup.setConsentGroupName(RandomStringUtils.randomAlphabetic(10));
        consentGroup.setGeneralResearchUse(true);
        FileTypeObject fileType1 = new FileTypeObject();
        fileType1.setFileType(FileTypeObject.FileType.Arrays);
        fileType1.setFunctionalEquivalence(RandomStringUtils.randomAlphabetic(10));
        fileType1.setNumberOfParticipants(new Random().nextInt());
        FileTypeObject fileType2 = new FileTypeObject();
        fileType2.setFileType(FileTypeObject.FileType.Phenotype);
        fileType2.setFunctionalEquivalence(RandomStringUtils.randomAlphabetic(10));
        fileType2.setNumberOfParticipants(new Random().nextInt());
        consentGroup.setFileTypes(List.of(fileType1, fileType2));
        consentGroup.setUrl(URI.create("https://asdf.gov"));
        consentGroup.setMor(false);
        consentGroup.setNmds(false);
        consentGroup.setNpu(false);
        consentGroup.setDataLocation(List.of(DataLocation.TDR_Location, DataLocation.An_VIL_Workspace));

        schemaV1.setConsentGroups(List.of(consentGroup));
        return schemaV1;
    }

}
