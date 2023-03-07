package org.broadinstitute.consent.http.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DAOContainer;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.DarStatus;
import org.broadinstitute.consent.http.enumeration.HeaderDAR;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataAccessRequestManage;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.broadinstitute.consent.http.service.dao.DataAccessRequestServiceDAO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

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
    private DarCollectionDAO darCollectionDAO;
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
    private VoteDAO voteDAO;
    @Mock
    private InstitutionDAO institutionDAO;
    @Mock
    private MatchDAO matchDAO;
    @Mock
    private DataAccessRequestServiceDAO dataAccessRequestServiceDAO;

    private DataAccessRequestService service;

    @Before
    public void setUp() {
        openMocks(this);
        doNothings();
    }

    private void doNothings() {
        doNothing().when(electionDAO).updateElectionStatus(any(), any());
    }

    private void initService() {
        DAOContainer container = new DAOContainer();
        container.setConsentDAO(consentDAO);
        container.setDataAccessRequestDAO(dataAccessRequestDAO);
        container.setDarCollectionDAO(darCollectionDAO);
        container.setInstitutionDAO(institutionDAO);
        container.setDacDAO(dacDAO);
        container.setUserDAO(userDAO);
        container.setDatasetDAO(dataSetDAO);
        container.setElectionDAO(electionDAO);
        container.setVoteDAO(voteDAO);
        container.setMatchDAO(matchDAO);
        service = new DataAccessRequestService(counterService, container, dacService, dataAccessRequestServiceDAO);
    }

    @Test
    public void testGetTotalUnreviewedDars() {
        Integer genericId = 1;
        DataAccessRequest dar = generateDataAccessRequest();
        dar.setData(new DataAccessRequestData());
        dar.addDatasetId(genericId);
        when(dataAccessRequestDAO.findAllDataAccessRequests()).thenReturn(Collections.singletonList(dar));
        Election e = new Election();
        e.setReferenceId(dar.getReferenceId());
        e.setElectionId(genericId);
        when(electionDAO.findLastElectionsByReferenceIdsAndType(any(), any())).thenReturn(Collections.singletonList(e));
        Dataset ds = new Dataset();
        ds.setConsentName(dar.getReferenceId());
        ds.setDataSetId(1);
        ds.setName("test dataset");
        when(dataSetDAO.findDatasetsByAuthUserEmail(authUser.getEmail()))
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
        when(userDAO.findUserByEmail(any())).thenReturn(new User());
        initService();

        DataAccessRequest updated = service.cancelDataAccessRequest(authUser, dar.getReferenceId());
        assertNotNull(updated);
        assertNotNull(updated.getData());
        assertNotNull(updated.getData().getStatus());
        assertEquals(DarStatus.CANCELED.getValue(), updated.getData().getStatus());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCancelDataAccessRequestWithElectionPresentFail() {
        when(electionDAO.getElectionIdsByReferenceIds(anyList())).thenReturn(List.of(1));
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
        initService();

        service.cancelDataAccessRequest(authUser, dar.getReferenceId());
    }

    @Test(expected = NotFoundException.class)
    public void testCancelDataAccessRequestNotFound() {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(null);
        initService();

        service.cancelDataAccessRequest(authUser, dar.getReferenceId());
    }

    @Test
    public void testCreateDataAccessRequest_Update() {
        DataAccessRequest dar = generateDataAccessRequest();
        dar.addDatasetIds(List.of(1, 2, 3));
        User user = new User(1, "email@test.org", "Display Name", new Date());
        when(counterService.getNextDarSequence()).thenReturn(1);
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
        when(dataAccessRequestDAO.findDARDatasetRelations(any())).thenReturn(List.of(1, 2, 3));
        doNothing().when(dataAccessRequestDAO).updateDraftByReferenceId(any(), any());
        doNothing().when(dataAccessRequestDAO).updateDataByReferenceId(any(), any(), any(), any(), any(), any());
        doNothing().when(dataAccessRequestDAO).insertDraftDataAccessRequest(any(), any(), any(), any(), any(), any(), any());
        initService();
        DataAccessRequest newDar = service.createDataAccessRequest(user, dar);
        assertNotNull(newDar);
    }

    @Test
    public void testCreateDataAccessRequest_Create() {
        DataAccessRequest dar = generateDataAccessRequest();
        dar.addDatasetIds(List.of(1, 2, 3));
        dar.setCreateDate(new Timestamp(1000));
        dar.setSortDate(new Timestamp(1000));
        dar.setReferenceId("id");
        User user = new User(1, "email@test.org", "Display Name", new Date());
        when(counterService.getNextDarSequence()).thenReturn(1);
        when(dataAccessRequestDAO.findByReferenceId("id")).thenReturn(null);
        when(dataAccessRequestDAO.findByReferenceId(argThat(new LongerThanTwo()))).thenReturn(dar);
        when(dataAccessRequestDAO.findDARDatasetRelations(any())).thenReturn(List.of(1, 2, 3));
        when(darCollectionDAO.insertDarCollection(anyString(), anyInt(), any(Date.class))).thenReturn(RandomUtils.nextInt(1,100));
        doNothing().when(dataAccessRequestDAO).insertDataAccessRequest(anyInt(), anyString(), anyInt(), any(Date.class), any(Date.class), any(Date.class), any(Date.class), any(DataAccessRequestData.class));
        initService();
        DataAccessRequest newDar = service.createDataAccessRequest(user, dar);
        assertNotNull(newDar);
    }

    @Test
    public void testUpdateByReferenceIdVersion2() throws Exception {
        DataAccessRequest dar = generateDataAccessRequest();
        dar.setCollectionId(RandomUtils.nextInt(0, 100));
        User user = new User(1, "email@test.org", "Display Name", new Date());
        dar.addDatasetIds(List.of(1, 2, 3));
        when(dataAccessRequestServiceDAO.updateByReferenceId(any(), any())).thenReturn(dar);
        initService();
        DataAccessRequest newDar = service.updateByReferenceId(user, dar);
        assertNotNull(newDar);
    }

    @Test
    public void testUpdateByReferenceIdVersion2_WithCollection() throws Exception {
        DataAccessRequest dar = generateDataAccessRequest();
        User user = new User(1, "email@test.org", "Display Name", new Date());
        dar.addDatasetIds(List.of(1, 2, 3));
        when(dataAccessRequestServiceDAO.updateByReferenceId(user, dar)).thenReturn(dar);
        initService();
        DataAccessRequest newDar = service.updateByReferenceId(user, dar);
        assertNotNull(newDar);
    }

    @Test
    public void testGetUsersApprovedForDataset() {
        Dataset d = new Dataset();
        d.setDataSetId(10);

        User user1 = new User();
        user1.setUserId(10);
        User user2 = new User();
        user2.setUserId(20);

        DataAccessRequest dar1 = new DataAccessRequest();
        dar1.setUserId(10);
        DataAccessRequest dar2 = new DataAccessRequest();
        dar2.setUserId(20);


        when(this.dataAccessRequestDAO
                .findAllUserIdsWithApprovedDARsByDatasetId(d.getDataSetId()))
                .thenReturn(List.of(dar1.getUserId(), dar2.getUserId()));

        when(this.userDAO.findUsers(List.of(dar1.getUserId(), dar2.getUserId())))
                .thenReturn(List.of(user1, user2));

        initService();

        assertEquals(List.of(user1, user2),
                service.getUsersApprovedForDataset(d));
    }

    @Test
    public void testInsertDraftDataAccessRequest() {
        User user = new User();
        user.setUserId(1);
        DataAccessRequest draft = generateDataAccessRequest();
        doNothing()
            .when(dataAccessRequestDAO)
            .insertDraftDataAccessRequest(any(), any(), any(), any(), any(), any(), any());
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

    @Test(expected = IllegalArgumentException.class)
    public void testDescribeDataAccessRequestManageV2_Admin() {
        User user = new User();
        user.setRoles(List.of(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName())));
        initService();
        service.describeDataAccessRequestManageV2(user, UserRoles.ADMIN);
    }

    @Test
    public void testDescribeDataAccessRequestManageV2_SO() {
        User user = new User();
        user.setInstitutionId(1);
        user.setRoles(new ArrayList<>());
        user.getRoles().add(new UserRole(7, UserRoles.SIGNINGOFFICIAL.getRoleName()));

        Integer genericId = 1;
        DataAccessRequest dar = generateDataAccessRequest();
        dar.setData(new DataAccessRequestData());
        dar.addDatasetId(genericId);
        when(dataAccessRequestDAO.findAllDataAccessRequestsForInstitution(any())).thenReturn(Collections.singletonList(dar));

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

        List<DataAccessRequestManage> manages =  service.describeDataAccessRequestManageV2(user, UserRoles.SIGNINGOFFICIAL);
        assertNotNull(manages);
        assertFalse(manages.isEmpty());
        assertEquals(dar.getReferenceId(), manages.get(0).getDar().getReferenceId());
        assertEquals(1, manages.size());
        assertEquals(e.getElectionId(), manages.get(0).getElection().getElectionId());
        assertEquals(d.getDacId(), manages.get(0).getDac().getDacId());
        assertFalse(manages.get(0).getVotes().isEmpty());
    }

    @Test(expected = NotFoundException.class)
    public void testDescribeDataAccessRequestManageV2_SO_InstitutionNotFound() {
        User user = new User();
        user.setRoles(new ArrayList<>());
        user.getRoles().add(new UserRole(7, UserRoles.SIGNINGOFFICIAL.getRoleName()));
        initService();
        service.describeDataAccessRequestManageV2(user, UserRoles.SIGNINGOFFICIAL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDescribeDataAccessRequestManageV2_Researcher() {
        User user = new User();
        user.setRoles(List.of(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
        initService();
        service.describeDataAccessRequestManageV2(user, UserRoles.RESEARCHER);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDescribeDataAccessRequestManageV2_NullUserRole() {
        User user = new User();
        initService();
        service.describeDataAccessRequestManageV2(user, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDescribeDataAccessRequestManageV2_NullUser() {
        initService();
        service.describeDataAccessRequestManageV2(null, UserRoles.MEMBER);
    }

    @Test
    public void testCreateApprovedDARDocument() {
        Election election = generateElection(1);
        when(electionDAO.findDataAccessClosedElectionsByFinalResult(any()))
                .thenReturn(Collections.singletonList(election));
        DataAccessRequest dar = generateDataAccessRequest();
        dar.setUserId(1);
        DarCollection collection = new DarCollection();
        Map<String, DataAccessRequest> dars = new HashMap<>();
        dars.put(election.getReferenceId(), dar);
        collection.setDars(dars);
        Institution institution = new Institution();
        institution.setName("Institution");
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
        when(darCollectionDAO.findDARCollectionByReferenceId(any())).thenReturn(collection);
        when(dataSetDAO.getAssociatedConsentIdByDatasetId(any()))
                .thenReturn("CONS-1");

        Consent consent = new Consent();
        consent.setConsentId("CONS-1");
        when(consentDAO.findConsentById("CONS-1")).thenReturn(consent);
        when(institutionDAO.findInstitutionById(any())).thenReturn(institution);
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
        DarCollection collection = new DarCollection();
        Map<String, DataAccessRequest> dars = new HashMap<>();
        dars.put(election.getReferenceId(), dar);
        collection.setDars(dars);
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
        when(darCollectionDAO.findDARCollectionByReferenceId(any())).thenReturn(collection);
        when(dataSetDAO.getAssociatedConsentIdByDatasetId(any()))
                .thenReturn("CONS-1");

        Consent consent = new Consent();
        consent.setConsentId("CONS-1");
        consent.setName("Consent 1");
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
    public void testCreateDatasetApprovedUsersContentAsNonPrivilegedUser() {
        DataAccessRequest dar = generateDataAccessRequest();
        dar.setUserId(1);
        User user = new User();
        user.setUserId(1);
        user.setDisplayName("displayName");
        user.setInstitutionId(1);
        Institution institution = new Institution();
        institution.setName("Institution");
        when(institutionDAO.findInstitutionById(any())).thenReturn(institution);
        when(dataAccessRequestDAO.findAllDataAccessRequests())
                .thenReturn(Collections.singletonList(dar));
        when(dataAccessRequestDAO.findByReferenceId(dar.getReferenceId()))
                .thenReturn(dar);
        when(darCollectionDAO.findDARCollectionByReferenceId(dar.getReferenceId()))
                .thenReturn(new DarCollection());
        when(electionDAO.findApprovalAccessElectionDate(dar.getReferenceId()))
                .thenReturn(new Date());
        when(userDAO.findUserByEmail(any())).thenReturn(user);

        initService();

        try {
            String approvedUsers = service.getDatasetApprovedUsersContent(new AuthUser(), 1);
            System.out.println(approvedUsers);
            assertNotNull(approvedUsers);
            assertFalse(approvedUsers.contains(HeaderDAR.USERNAME.getValue()));
        } catch (Exception ioe) {
            assert false;
        }
    }

    @Test
    public void testCreateDatasetApprovedUsersContentAsPrivilegedUser() {
        DataAccessRequest dar = generateDataAccessRequest();
        dar.setUserId(1);
        User user = new User();
        UserRole userRole = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
        user.addRole(userRole);
        user.setUserId(1);
        user.setDisplayName("displayName");
        user.setInstitutionId(1);
        Institution institution = new Institution();
        institution.setName("Institution");
        when(institutionDAO.findInstitutionById(any())).thenReturn(institution);
        when(dataAccessRequestDAO.findAllDataAccessRequests())
                .thenReturn(Collections.singletonList(dar));
        when(dataAccessRequestDAO.findByReferenceId(dar.getReferenceId()))
                .thenReturn(dar);
        when(darCollectionDAO.findDARCollectionByReferenceId(dar.getReferenceId()))
                .thenReturn(new DarCollection());
        when(electionDAO.findApprovalAccessElectionDate(dar.getReferenceId()))
                .thenReturn(new Date());
        when(userDAO.findUserByEmail(any())).thenReturn(user);

        initService();

        try {
            String approvedUsers = service.getDatasetApprovedUsersContent(new AuthUser(), 1);
            System.out.println(approvedUsers);
            assertNotNull(approvedUsers);
            assertTrue(approvedUsers.contains(HeaderDAR.USERNAME.getValue()));
        } catch (Exception ioe) {
            assert false;
        }
    }

    private DataAccessRequest generateDataAccessRequest() {
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        Integer userId = userDAO.insertUser(UUID.randomUUID().toString(), "displayName", new Date());
        dar.setUserId(userId);
        dar.setReferenceId(UUID.randomUUID().toString());
        data.setReferenceId(dar.getReferenceId());
        dar.addDatasetId(1);
        data.setForProfit(false);
        data.setAddiction(false);
        data.setAnvilUse(true);
        data.setCheckCollaborator(false);
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
        dar.setData(data);
        return dar;
    }

    private Election generateElection(Integer datasetId) {
        String refId = UUID.randomUUID().toString();
        Election election = new Election();
        election.setDataSetId(datasetId);
        election.setReferenceId(refId);

        return election;
    }

    @Test
    public void testFindAllDraftDataAccessRequests() {
        when(dataAccessRequestDAO.findAllDraftDataAccessRequests()).thenReturn(List.of(new DataAccessRequest()));
        initService();
        List<DataAccessRequest> drafts = service.findAllDraftDataAccessRequests();
        assertEquals(drafts.size(), 1);
    }

    @Test
    public void testFindAllDraftDataAccessRequestsByUser() {
        when(dataAccessRequestDAO.findAllDraftsByUserId(any())).thenReturn(List.of(new DataAccessRequest()));
        initService();
        List<DataAccessRequest> drafts = service.findAllDraftDataAccessRequestsByUser(1);
        assertEquals(drafts.size(), 1);
    }

    @Test
    public void getDataAccessRequestsForUser() {
        List<DataAccessRequest> dars = List.of(new DataAccessRequest());
        when(dataAccessRequestDAO.findAllDataAccessRequests()).thenReturn(dars);
        when(dacService.filterDataAccessRequestsByDac(eq(dars), any())).thenReturn(dars);
        initService();
        List<DataAccessRequest> foundDars = service.getDataAccessRequestsByUserRole(new User());
        assertEquals(foundDars.size(), 1);
    }

    @Test
    public void getDraftDataAccessRequestManage_NullUserId() {
        DataAccessRequest dar = new DataAccessRequest();
        dar.setReferenceId("referenceId");
        dar.setUserId(1);
        DataAccessRequestData data = new DataAccessRequestData();
        dar.addDatasetId(361);
        dar.setData(data);
        when(dataAccessRequestDAO.findAllDraftDataAccessRequests()).thenReturn(List.of(dar));
        initService();
        List<DataAccessRequestManage> darManages = service.getDraftDataAccessRequestManage(null);
        assertEquals(1, darManages.size());
    }

    @Test
    public void getDraftDataAccessRequestManage() {
        DataAccessRequest dar = new DataAccessRequest();
        dar.setReferenceId("referenceId");
        dar.setUserId(1);
        DataAccessRequestData data = new DataAccessRequestData();
        dar.addDatasetId(361);
        dar.setData(data);
        when(dataAccessRequestDAO.findAllDraftsByUserId(any())).thenReturn(List.of(dar));
        initService();
        List<DataAccessRequestManage> darManages = service.getDraftDataAccessRequestManage(1);
        assertEquals(1, darManages.size());
    }

    @Test
    public void testFindByReferenceId() {
        initService();
        DataAccessRequest dar = new DataAccessRequest();
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
        DataAccessRequest foundDar = service.findByReferenceId("refId");
        assertEquals(dar, foundDar);
    }

    @Test(expected = NotFoundException.class)
    public void testFindByReferenceId_NotFound() {
        initService();
        when(dataAccessRequestDAO.findByReferenceId(any())).thenThrow(new NotFoundException());
        service.findByReferenceId("referenceId");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateDraftDarFromCanceledCollection_NoDars() {
        User user = new User();
        DarCollection sourceCollection = new DarCollection();
        initService();
        service.createDraftDarFromCanceledCollection(user, sourceCollection);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateDraftDarFromCanceledCollection_NoDarData() {
        User user = new User();
        DarCollection sourceCollection = new DarCollection();
        DataAccessRequest newDar = new DataAccessRequest();
        newDar.setReferenceId(UUID.randomUUID().toString());
        sourceCollection.addDar(newDar);
        initService();
        service.createDraftDarFromCanceledCollection(user, sourceCollection);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateDraftDarFromCanceledCollection_NoCanceledDars() {
        User user = new User();
        DarCollection sourceCollection = new DarCollection();
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        data.setReferenceId(UUID.randomUUID().toString());
        data.setStatus("Not Canceled");
        dar.setData(data);
        dar.setReferenceId(data.getReferenceId());
        sourceCollection.addDar(dar);
        initService();
        service.createDraftDarFromCanceledCollection(user, sourceCollection);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateDraftDarFromCanceledCollection_NoDatasets() {
        User user = new User();
        DarCollection sourceCollection = new DarCollection();
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        data.setReferenceId(UUID.randomUUID().toString());
        data.setStatus(DarStatus.CANCELED.getValue());
        dar.setData(data);
        dar.setDatasetIds(null);
        dar.setReferenceId(data.getReferenceId());
        sourceCollection.addDar(dar);
        initService();
        service.createDraftDarFromCanceledCollection(user, sourceCollection);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateDraftDarFromCanceledCollection_OpenElectionsOnCanceledDars() {
        User user = new User();
        DarCollection sourceCollection = new DarCollection();
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        data.setStatus(DarStatus.CANCELED.getValue());
        dar.addDatasetId(1);
        data.setReferenceId(UUID.randomUUID().toString());
        dar.setData(data);
        dar.setReferenceId(data.getReferenceId());
        sourceCollection.addDar(dar);
        when(electionDAO.getElectionIdsByReferenceIds(any())).thenReturn(List.of(1));
        initService();
        service.createDraftDarFromCanceledCollection(user, sourceCollection);
    }

    @Test
    public void testCreateDraftDarFromCanceledCollection() {
        User user = new User();
        DarCollection sourceCollection = new DarCollection();
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        data.setStatus(DarStatus.CANCELED.getValue());
        dar.addDatasetId(1);
        data.setReferenceId(UUID.randomUUID().toString());
        dar.setData(data);
        dar.setReferenceId(data.getReferenceId());
        sourceCollection.addDar(dar);
        when(electionDAO.getElectionIdsByReferenceIds(any())).thenReturn(List.of());
        doNothing().when(dataAccessRequestDAO).insertDraftDataAccessRequest(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
        );
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(new DataAccessRequest());
        initService();
        service.createDraftDarFromCanceledCollection(user, sourceCollection);
    }

    @Test
    public void testDeleteByReferenceIdAdmin() {
        String referenceId = UUID.randomUUID().toString();
        User adminUser = new User();
        adminUser.addRole(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
        Election election = new Election();
        election.setElectionId(1);
        election.setReferenceId(referenceId);
        when(electionDAO.findElectionsByReferenceId(any())).thenReturn(List.of(election));
        doNothing().when(voteDAO).deleteVotesByReferenceId(any());
        doNothing().when(electionDAO).deleteElectionFromAccessRP(any());
        doNothing().when(electionDAO).deleteElectionById(any());
        doNothing().when(matchDAO).deleteMatchesByPurposeId(any());
        doNothing().when(dataAccessRequestDAO).deleteByReferenceId(any());
        initService();

        try {
            service.deleteByReferenceId(adminUser, referenceId);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testDeleteByReferenceIdResearcherSuccess() {
        String referenceId = UUID.randomUUID().toString();
        User user = new User();
        user.addRole(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
        when(electionDAO.findElectionsByReferenceId(any())).thenReturn(List.of());
        doNothing().when(voteDAO).deleteVotesByReferenceId(any());
        doNothing().when(electionDAO).deleteElectionFromAccessRP(any());
        doNothing().when(electionDAO).deleteElectionById(any());
        doNothing().when(matchDAO).deleteMatchesByPurposeId(any());
        doNothing().when(dataAccessRequestDAO).deleteByReferenceId(any());
        doNothing().when(dataAccessRequestDAO).deleteDARDatasetRelationByReferenceId(any());
        initService();

        service.deleteByReferenceId(user, referenceId);
    }

    @Test(expected = NotAcceptableException.class)
    public void testDeleteByReferenceIdResearcherFailure() {
        String referenceId = UUID.randomUUID().toString();
        User user = new User();
        user.addRole(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
        Election election = new Election();
        election.setElectionId(1);
        election.setReferenceId(referenceId);
        when(electionDAO.findElectionsByReferenceId(any())).thenReturn(List.of(election));
        doNothing().when(voteDAO).deleteVotesByReferenceId(any());
        doNothing().when(electionDAO).deleteElectionFromAccessRP(any());
        doNothing().when(electionDAO).deleteElectionById(any());
        doNothing().when(matchDAO).deleteMatchesByPurposeId(any());
        doNothing().when(dataAccessRequestDAO).deleteByReferenceId(any());
        doNothing().when(dataAccessRequestDAO).deleteDARDatasetRelationByReferenceId(any());
        initService();

        service.deleteByReferenceId(user, referenceId);
    }

    private class LongerThanTwo implements ArgumentMatcher<String> {

        @Override
        public boolean matches(String argument) {
            return argument.length() > 2;
        }
    }
}
