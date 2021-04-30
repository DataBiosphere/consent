package org.broadinstitute.consent.http.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.NotFoundException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DAOContainer;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataAccessRequestManage;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DatasetDetailEntry;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.darsummary.DARModalDetailsDTO;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DataAccessRequestServiceTest {

    @Mock
    private AuthUser authUser;
    @Mock
    private ConsentDAO consentDAO;
    @Mock
    private CounterService counterService;
    @Mock
    private DataAccessRequestDAO dataAccessRequestDAO;
    @Mock
    private DacDAO dacDAO;
    @Mock
    private UserDAO userDAO;
    @Mock
    private DatasetDAO dataSetDAO;
    @Mock
    private ElectionDAO electionDAO;
    @Mock
    private DacService dacService;
    @Mock
    private UserService userService;
    @Mock
    private VoteDAO voteDAO;
    @Mock
    private UserPropertyDAO userPropertyDAO;
    @Mock
    private ElectionService electionService;

    private static final Gson gson = new GsonBuilder().setDateFormat("MMM d, yyyy").create();
    private DataAccessRequestService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doNothings();
    }

    private void doNothings() {
        doNothing().when(electionDAO).updateElectionStatus(any(), any());
    }

    private void initService() {
        DAOContainer container = new DAOContainer();
        container.setConsentDAO(consentDAO);
        container.setDataAccessRequestDAO(dataAccessRequestDAO);
        container.setDacDAO(dacDAO);
        container.setUserDAO(userDAO);
        container.setDatasetDAO(dataSetDAO);
        container.setElectionDAO(electionDAO);
        container.setVoteDAO(voteDAO);
        container.setResearcherPropertyDAO(userPropertyDAO);
        service = new DataAccessRequestService(counterService, container, dacService, userService);
    }

    @Test
    public void testGetTotalUnreviewedDars() {
        Integer genericId = 1;
        DataAccessRequest dar = generateDataAccessRequest();
        dar.setData(new DataAccessRequestData());
        dar.getData().setDatasetIds(Collections.singletonList(genericId));
        when(dataAccessRequestDAO.findAllDataAccessRequests()).thenReturn(Collections.singletonList(dar));
        Election e = new Election();
        e.setReferenceId(dar.getReferenceId());
        e.setElectionId(genericId);
        when(electionDAO.findLastElectionsByReferenceIdsAndType(any(), any())).thenReturn(Collections.singletonList(e));
        DataSet ds = new DataSet();
        ds.setConsentName(dar.getReferenceId());
        ds.setDataSetId(1);
        ds.setName("test dataset");
        when(dataSetDAO.findDataSetsByAuthUserEmail(authUser.getName()))
                .thenReturn(Collections.singletonList(ds));
        initService();

        Integer count = service.getTotalUnReviewedDars(authUser);
        assertEquals(Integer.valueOf(1), count);
    }

    @Test
    public void testCancelDataAccessRequestSuccess() {
        List<Election> electionList = new ArrayList<Election>();
        when(electionDAO.findElectionsByReferenceId(anyString())).thenReturn(electionList);
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
        doNothing().when(dataAccessRequestDAO).updateDataByReferenceId(any(), any());
        initService();

        DataAccessRequest updated = service.cancelDataAccessRequest(dar.getReferenceId());
        assertNotNull(updated);
        assertNotNull(updated.getData());
        assertNotNull(updated.getData().getStatus());
        assertEquals(ElectionStatus.CANCELED.getValue(), updated.getData().getStatus());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCancelDataAccessRequestWithElectionPresentFail() {
        List<Election> electionList = new ArrayList<Election>();
        electionList.add(new Election());
        when(electionDAO.findElectionsByReferenceId(anyString())).thenReturn(electionList);
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
        doNothing().when(dataAccessRequestDAO).updateDataByReferenceId(any(), any());
        initService();

        service.cancelDataAccessRequest(dar.getReferenceId());
    }
    
    @Test(expected = NotFoundException.class)
    public void testCancelDataAccessRequestNotFound() {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(null);
        initService();

        service.cancelDataAccessRequest(dar.getReferenceId());
    }

    @Test
    public void testCreateDataAccessRequest() {
        DataAccessRequest dar = generateDataAccessRequest();
        dar.getData().setDatasetIds(Arrays.asList(1, 2, 3));
        User user = new User(1, "email@test.org", "Display Name", new Date());
        when(counterService.getNextDarSequence()).thenReturn(1);
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(null);
        doNothing().when(dataAccessRequestDAO).updateDraftByReferenceId(any(), any());
        doNothing().when(dataAccessRequestDAO).updateDataByReferenceIdVersion2(any(), any(), any(), any(), any(), any());
        doNothing().when(dataAccessRequestDAO).insertVersion2(any(), any(), any(), any(), any(), any(), any());
        initService();
        List<DataAccessRequest> newDars = service.createDataAccessRequest(user, dar);
        assertEquals(3, newDars.size());
    }

    @Test
    public void testUpdateByReferenceIdVersion2() {
        DataAccessRequest dar = generateDataAccessRequest();
        User user = new User(1, "email@test.org", "Display Name", new Date());
        dar.getData().setDatasetIds(Arrays.asList(1, 2, 3));
        doNothing().when(dataAccessRequestDAO).updateDataByReferenceIdVersion2(any(), any(),
            any(), any(), any(), any());
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
        initService();
        DataAccessRequest newDar = service.updateByReferenceIdVersion2(user, dar);
        assertNotNull(newDar);
    }

    @Test
    public void testInsertDraftDataAccessRequest() {
        User user = new User();
        user.setDacUserId(1);
        DataAccessRequest draft = generateDataAccessRequest();
        doNothing()
            .when(dataAccessRequestDAO)
            .insertVersion2(any(), any(), any(), any(), any(), any(), any());
        doNothing()
            .when(dataAccessRequestDAO)
            .updateDraftByReferenceId(any(), any());
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(draft);
        initService();
        DataAccessRequest dar = service.insertDraftDataAccessRequest(user, draft);
        assertNotNull(dar);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertDraftDataAccessRequestFailure() {
        initService();
        DataAccessRequest dar = service.insertDraftDataAccessRequest(null, null);
        assertNotNull(dar);
    }

    @Test
    public void testDescribeDataAccessRequestManageV2() {
        Integer genericId = 1;
        DataAccessRequest dar = generateDataAccessRequest();
        dar.setData(new DataAccessRequestData());
        dar.getData().setDatasetIds(Collections.singletonList(genericId));
        when(dataAccessRequestDAO.findAllDataAccessRequests()).thenReturn(Collections.singletonList(dar));
        when(dacService.filterDataAccessRequestsByDac(any(), any())).thenReturn(Collections.singletonList(dar));

        Election e = new Election();
        e.setReferenceId(dar.getReferenceId());
        e.setElectionId(genericId);
        when(electionDAO.findLastElectionsByReferenceIdsAndType(any(), any())).thenReturn(Collections.singletonList(e));

        Vote v = new Vote();
        v.setVoteId(genericId);
        v.setElectionId(e.getElectionId());
        when(voteDAO.findVotesByElectionIds(any())).thenReturn(Collections.singletonList(v));

        Dac d = new Dac();
        d.setDacId(genericId);
        d.addDatasetId(genericId);
        when(dacDAO.findDacsForDatasetIds(any())).thenReturn(Collections.singleton(d));
        initService();

        List<DataAccessRequestManage> manages =  service.describeDataAccessRequestManageV2(authUser);
        assertNotNull(manages);
        assertFalse(manages.isEmpty());
        assertEquals(dar.getReferenceId(), manages.get(0).getDar().getReferenceId());
        assertEquals(1, manages.size());
        assertEquals(e.getElectionId(), manages.get(0).getElection().getElectionId());
        assertEquals(d.getDacId(), manages.get(0).getDac().getDacId());
        assertFalse(manages.get(0).getVotes().isEmpty());
    }

    @Test
    public void testDescribeDataAccessRequestFieldsById() {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
        initService();

        Document doc = service.describeDataAccessRequestFieldsById(dar.getReferenceId(), Arrays.asList(DarConstants.DATASET_ID, DarConstants.REFERENCE_ID));
        assertNotNull(doc);
        assertEquals(dar.getReferenceId(), doc.get(DarConstants.REFERENCE_ID));
    }

    @Test
    public void testDescribeDraftDataAccessRequestManage() {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestDAO.findAllDraftDataAccessRequests())
                .thenReturn(Collections.singletonList(dar));
        initService();

        List<Document> docs = service.describeDraftDataAccessRequestManage(null);
        assertNotNull(docs);
        assertEquals(1, docs.size());
        assertEquals(dar.getReferenceId(), docs.get(0).get(DarConstants.REFERENCE_ID));
    }

    @Test
    public void testCreateApprovedDARDocument() {
        Election election = generateElection(1);
        when(electionDAO.findDataAccessClosedElectionsByFinalResult(any()))
                .thenReturn(Collections.singletonList(election));
        DataAccessRequest dar = generateDataAccessRequest();
        dar.setUserId(1);
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
        when(dataSetDAO.getAssociatedConsentIdByDataSetId(any()))
                .thenReturn("CONS-1");

        Consent consent = new Consent();
        consent.setConsentId("CONS-1");
        when(consentDAO.findConsentById("CONS-1")).thenReturn(consent);

        Map<String, String> researcherProperties = getResearcherProperties();
        when(userPropertyDAO.findPropertyValueByPK(dar.getUserId(), DarConstants.PROFILE_NAME))
                .thenReturn(researcherProperties.get(DarConstants.PROFILE_NAME));
        when(userPropertyDAO.findPropertyValueByPK(dar.getUserId(), DarConstants.INSTITUTION))
                .thenReturn(researcherProperties.get(DarConstants.INSTITUTION));

        initService();
        try {
            File file = service.createApprovedDARDocument();
            assertNotNull(file);
        } catch (IOException ioe) {
            assert false;
        }
    }

    @Test
    public void testCreateReviewedDARDocument() {
        Election election = generateElection(1);
        election.setFinalVote(true);
        election.setFinalVoteDate(new Date());
        when(electionDAO.findDataAccessClosedElectionsByFinalResult(true))
                .thenReturn(Collections.singletonList(election));
        when(electionDAO.findDataAccessClosedElectionsByFinalResult(false))
                .thenReturn(Collections.emptyList());
        DataAccessRequest dar = generateDataAccessRequest();
        dar.setUserId(1);
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
        when(dataSetDAO.getAssociatedConsentIdByDataSetId(any()))
                .thenReturn("CONS-1");

        Consent consent = new Consent();
        consent.setConsentId("CONS-1");
        consent.setName("Consent 1");
        consent.setUseRestriction(new Everything());
        consent.setTranslatedUseRestriction(new Everything().toString());
        when(consentDAO.findConsentById("CONS-1")).thenReturn(consent);
        initService();

        try {
            File file = service.createReviewedDARDocument();

            assertNotNull(file);
        } catch (Exception e) {
            assert false;
        }
    }

    @Test
    public void testCreateDataSetApprovedUsersDocument() {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestDAO.findAllDataAccessRequests())
                .thenReturn(Collections.singletonList(dar));
        when(dataAccessRequestDAO.findByReferenceId(dar.getReferenceId()))
                .thenReturn(dar);
        when(electionDAO.findApprovalAccessElectionDate(dar.getReferenceId()))
                .thenReturn(new Date());

        Map<String, String> researcherProperties = getResearcherProperties();
        when(userPropertyDAO.findPropertyValueByPK(dar.getUserId(), DarConstants.ACADEMIC_BUSINESS_EMAIL))
            .thenReturn(researcherProperties.get(DarConstants.ACADEMIC_BUSINESS_EMAIL));
        when(userPropertyDAO.findPropertyValueByPK(dar.getUserId(), DarConstants.PROFILE_NAME))
            .thenReturn(researcherProperties.get(DarConstants.PROFILE_NAME));
        when(userPropertyDAO.findPropertyValueByPK(dar.getUserId(), DarConstants.INSTITUTION))
            .thenReturn(researcherProperties.get(DarConstants.INSTITUTION));

        initService();

        try {
            File file = service.createDataSetApprovedUsersDocument(1);

            assertNotNull(file);
        } catch (IOException ioe) {
            assert false;
        }
    }

    @Test
    public void testDARModalDetailsDTOBuilder() {
        DataAccessRequest dar = generateDataAccessRequest();
        Document doc = generateDarDocument(dar);

        when(dataAccessRequestDAO.findByReferenceId(dar.getReferenceId()))
                .thenReturn(dar);
        DataSet ds = new DataSet();
        ds.setDataSetId(1);
        ds.setName("DS-1");
        ds.setConsentName(dar.getReferenceId());
        when(dataSetDAO.findDataSetsByIdList(dar.data.getDatasetIds()))
                .thenReturn(Collections.singletonList(ds));
        User user = new User();
        user.setDacUserId(1);
        user.setEmail("test@test.com");
        user.setDisplayName("Test User");
        when(userPropertyDAO.findResearcherPropertiesByUser(user.getDacUserId()))
                .thenReturn(Collections.emptyList());
        initService();

        DARModalDetailsDTO darModalDetailsDTO = service.DARModalDetailsDTOBuilder(doc, user, electionService);
        assertNotNull(darModalDetailsDTO);
        assertEquals(dar.data.getInstitution(), darModalDetailsDTO.getInstitutionName());
    }

    private DataAccessRequest generateDataAccessRequest() {
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        dar.setReferenceId(UUID.randomUUID().toString());
        data.setReferenceId(dar.getReferenceId());
        data.setDatasetIds(Collections.singletonList(1));
        data.setForProfit(false);
        data.setAcademicEmail("acad@email.com");
        data.setAddiction(false);
        data.setAddress1("");
        data.setAddress2("");
        data.setAnvilUse(true);
        data.setCheckCollaborator(false);
        data.setCity("");
        data.setCloudUse(true);
        data.setCloudProvider("Google Cloud");
        data.setCloudProviderDescription("Google");
        data.setControls(false);
        data.setOneGender(false);
        data.setPediatric(false);
        data.setHmb(false);
        data.setDiseases(false);
        data.setSexualDiseases(false);
        data.setPoa(false);
        data.setIllegalBehavior(false);
        data.setCountry("United States");
        data.setState("");
        data.setZipCode("");
        data.setProjectTitle("Title");
        data.setStigmatizedDiseases(false);
        data.setVulnerablePopulation(false);
        data.setPopulation(false);
        data.setPopulationMigration(true);
        data.setPsychiatricTraits(false);
        data.setNotHealth(true);
        data.setOntologies(Collections.emptyList());
        data.setMethods(false);
        data.setOther(false);

        DatasetDetailEntry detailEntry = new DatasetDetailEntry();
        detailEntry.setDatasetId("DS-1");
        detailEntry.setName("DS-1");
        data.setDatasetDetail(Collections.singletonList(detailEntry));
        dar.setData(data);
        return dar;
    }

    private Document generateDarDocument(DataAccessRequest dar) {
        Document document = Document.parse(gson.toJson(dar.getData()));
        document.put(DarConstants.DATA_ACCESS_REQUEST_ID, dar.getId());
        document.put(DarConstants.REFERENCE_ID, dar.getReferenceId());
        document.put(DarConstants.CREATE_DATE, dar.getCreateDate());
        document.put(DarConstants.SORT_DATE, dar.getSortDate());
        return document;
    }

    private Election generateElection(Integer dataSetId) {
        String refId = UUID.randomUUID().toString();
        Election election = new Election();
        election.setDataSetId(dataSetId);
        election.setReferenceId(refId);

        return election;
    }

    private Document getDocument(String linkedIn, String orcid, String researcherGate) {
        Document dar = new Document();
        dar.put(UserFields.LINKEDIN_PROFILE.getValue(), linkedIn);
        dar.put(UserFields.ORCID.getValue(), orcid);
        dar.put(UserFields.RESEARCHER_GATE.getValue(), researcherGate);
        dar.put(DarConstants.INVESTIGATOR, randomString());
        dar.put(DarConstants.PI_EMAIL, randomString());
        dar.put(DarConstants.PROJECT_TITLE, randomString());
        dar.put(DarConstants.DATASET_DETAIL, new ArrayList<Document>());
        dar.put(DarConstants.RUS, randomString());
        dar.put(DarConstants.NON_TECH_RUS, randomString());
        dar.put(DarConstants.METHODS, true);
        dar.put(DarConstants.CONTROLS, true);
        dar.put(DarConstants.OTHER, true);
        dar.put(DarConstants.POA, true);
        dar.put(DarConstants.HMB, true);
        dar.put(DarConstants.OTHER_TEXT, randomString());
        return dar;
    }

    private Map<String, String> getResearcherProperties() {
        Map<String, String> researcherProperties = new HashMap<>();
        researcherProperties.put(UserFields.INSTITUTION.getValue(), randomString());
        researcherProperties.put(UserFields.DEPARTMENT.getValue(), randomString());
        researcherProperties.put(UserFields.STREET_ADDRESS_1.getValue(), randomString());
        researcherProperties.put(UserFields.CITY.getValue(), randomString());
        researcherProperties.put(UserFields.ZIP_POSTAL_CODE.getValue(), randomString());
        researcherProperties.put(UserFields.COUNTRY.getValue(), randomString());
        researcherProperties.put(UserFields.STATE.getValue(), randomString());
        researcherProperties.put(UserFields.STREET_ADDRESS_2.getValue(), randomString());
        researcherProperties.put(UserFields.DIVISION.getValue(), randomString());
        researcherProperties.put(DarConstants.ACADEMIC_BUSINESS_EMAIL, randomString());
        researcherProperties.put(DarConstants.ERA_COMMONS_ID, randomString());
        researcherProperties.put(DarConstants.PUBMED_ID, randomString());
        researcherProperties.put(DarConstants.SCIENTIFIC_URL, randomString());
        researcherProperties.put(UserFields.ARE_YOU_PRINCIPAL_INVESTIGATOR.getValue(), "true");
        researcherProperties.put(UserFields.PROFILE_NAME.getValue(), randomString());
        return researcherProperties;
    }

    private String randomString() {
        return RandomStringUtils.random(10, true, false);
    }

}
