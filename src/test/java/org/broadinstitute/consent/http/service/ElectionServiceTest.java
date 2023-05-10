package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.ws.rs.NotFoundException;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetEntry;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class ElectionServiceTest {

    private ElectionService service;

    @Mock
    private MailMessageDAO mailMessageDAO;
    @Mock
    private ConsentDAO consentDAO;
    @Mock
    private ElectionDAO electionDAO;
    @Mock
    private VoteDAO voteDAO;
    @Mock
    private UserDAO userDAO;
    @Mock
    private DatasetDAO dataSetDAO;
    @Mock
    private DataAccessRequestService dataAccessRequestService;
    @Mock
    private DataAccessRequestDAO dataAccessRequestDAO;
    @Mock
    private EmailService emailService;

    private static Election sampleElection1;
    private static Election sampleElection2;
    private static Election sampleElectionRP;
    private static Dataset sampleDataset1;
    private static DataAccessRequest sampleDataAccessRequest1;
    private static AuthUser authUser;
    private static Consent sampleConsent1;
    private static User sampleUserChairperson;
    private static User sampleUserMember;
    private static Dac sampleDac1;
    private static Vote sampleVoteChairpersonApproval;
    private static Vote sampleVoteChairpersonReject;
    private static Vote sampleVoteMember;
    private static Vote sampleVoteRP;

    @BeforeAll
    public static void setUpClass() {
        String referenceId = "CONS-1";

        sampleDataset1 = new Dataset();
        sampleDataset1.setDataSetId(1);
        sampleDataset1.setObjectId("ObjectID 1");
        sampleDataset1.setActive(true);
        sampleDataset1.setNeedsApproval(false);
        sampleDataset1.setConsentName(referenceId);
        sampleDataset1.setName("Dataset 1");
        sampleDataset1.setAlias(1);

        sampleElection1 = new Election(1, ElectionType.DATA_ACCESS.getValue(),
                ElectionStatus.OPEN.getValue(), new Date(),
                sampleDataset1.getConsentName(), new Date(), false, sampleDataset1.getDataSetId());
        sampleElection2 = new Election(2, ElectionType.DATA_ACCESS.getValue(),
                ElectionStatus.CLOSED.getValue(), new Date(),
                sampleDataset1.getConsentName(), new Date(), false, sampleDataset1.getDataSetId());
        sampleElectionRP = new Election(3, ElectionType.RP.getValue(), ElectionStatus.OPEN.getValue(), new Date(),
                sampleDataset1.getConsentName(), new Date(), false, sampleDataset1.getDataSetId());

        authUser = new AuthUser("test@test.com");
        sampleUserChairperson = new User(1, "test@test.com", "Test User", new Date());
        sampleUserChairperson.addRole(new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName()));

        sampleUserMember = new User(2, "test@test.com", "Test User", new Date());
        sampleUserMember.addRole(new UserRole(UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName()));

        sampleConsent1 = new Consent(false, "A data use letter", "sampleConsent1", null, null, null, "Group Name Test");
        sampleConsent1.setConsentId(sampleDataset1.getConsentName());

        sampleDataAccessRequest1 = new DataAccessRequest();
        sampleDataAccessRequest1.setUserId(2);
        DataAccessRequestData data = new DataAccessRequestData();
        data.setReferenceId(sampleElection1.getReferenceId());
        DatasetEntry entry = new DatasetEntry();
        entry.setKey(sampleDataset1.getConsentName());
        entry.setValue(sampleDataset1.getName());
        data.setDatasets(List.of(entry));
        sampleDataAccessRequest1.setData(data);
        sampleDataAccessRequest1.addDatasetId(sampleDataset1.getDataSetId());

        sampleDac1 = new Dac();
        sampleDac1.setDacId(1);
        sampleDac1.setName("DAC-1");
        sampleDac1.setCreateDate(new Date());
        sampleDac1.setUpdateDate(new Date());

        sampleVoteChairpersonApproval = new Vote();
        sampleVoteChairpersonApproval.setElectionId(sampleElection1.getElectionId());
        sampleVoteChairpersonApproval.setUserId(sampleUserChairperson.getUserId());
        sampleVoteChairpersonApproval.setVote(true);
        sampleVoteChairpersonApproval.setVoteId(1);
        sampleVoteChairpersonApproval.setRationale("Go for it");

        sampleVoteChairpersonReject = new Vote();
        sampleVoteChairpersonReject.setElectionId(sampleElection1.getElectionId());
        sampleVoteChairpersonReject.setUserId(sampleUserChairperson.getUserId());
        sampleVoteChairpersonReject.setVote(false);
        sampleVoteChairpersonReject.setVoteId(1);
        sampleVoteChairpersonReject.setRationale("Rejection vote");

        sampleVoteMember = new Vote();
        sampleVoteMember.setElectionId(sampleElection1.getElectionId());
        sampleVoteMember.setUserId(sampleUserMember.getUserId());
        sampleVoteMember.setVote(true);
        sampleVoteMember.setVoteId(2);
        sampleVoteMember.setRationale("Go for it");

        sampleVoteRP = new Vote();
        sampleVoteRP.setElectionId(sampleElectionRP.getElectionId());
        sampleVoteRP.setUserId(sampleUserMember.getUserId());
        sampleVoteRP.setVote(true);
        sampleVoteRP.setVoteId(3);
        sampleVoteRP.setRationale("Yep");
    }


    @BeforeEach
    public void setUp() throws Exception {
        openMocks(this);
        bunchOfDoNothings();
        goGetters();
    }

    private void bunchOfDoNothings() throws Exception {
        doNothing().when(emailService).sendDisabledDatasetsMessage(any(), any(), any());
        doNothing().when(consentDAO).updateConsentSortDate(any(), any());
        doNothing().when(dataAccessRequestDAO).updateDataByReferenceId(any(), any(), any(), any(), any(), any());
        doNothing().when(electionDAO).insertAccessAndConsentElection(any(), any());
        doNothing().when(electionDAO).insertAccessRP(any(), any());
        doNothing().when(electionDAO).updateElectionById(any(), any(), any());
        doNothing().when(electionDAO).updateElectionById(any(), any(), any(), any());
        doNothing().when(electionDAO).archiveElectionById(any(), any());
        doNothing().when(electionDAO).updateElectionStatus(any(), any());
        doNothing().when(emailService).sendResearcherDarApproved(any(), any(), any(), any());
        doNothing().when(emailService).sendClosedDataSetElectionsMessage(any());
        doNothing().when(electionDAO).deleteElectionById(any());
        doNothing().when(electionDAO).deleteAccessRP(any());
    }

    private void goGetters() {
        userStubs();
        electionStubs();
        voteStubs();

        when(dataSetDAO.findDatasetsByAuthUserEmail(authUser.getEmail()))
                .thenReturn(List.of(sampleDataset1));
        when(dataAccessRequestDAO.findByReferenceIds(any()))
                .thenReturn(List.of(sampleDataAccessRequest1));
        when(consentDAO.findConsentsFromConsentsIDs(any()))
                .thenReturn(List.of(sampleConsent1));
        when(dataAccessRequestService.findByReferenceId(any()))
                .thenReturn(sampleDataAccessRequest1);
        when(dataAccessRequestDAO.findByReferenceId(any()))
                .thenReturn(sampleDataAccessRequest1);
        when(dataSetDAO.findDatasetsByIdList(any())).thenReturn(
                List.of(sampleDataset1));
        when(consentDAO.checkConsentById(sampleConsent1.getConsentId())).thenReturn(sampleConsent1.getConsentId());
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(new DataAccessRequest());
    }

    private void userStubs() {
        when(userDAO.findUsersWithRoles(any())).thenReturn(Set.of(sampleUserChairperson, sampleUserMember));
        when(userDAO.findUsersEnabledToVoteByDAC(any())).thenReturn(Set.of(sampleUserChairperson, sampleUserMember));
        when(userDAO.findNonDacUsersEnabledToVote()).thenReturn(Set.of(sampleUserChairperson, sampleUserMember));
        when(userDAO.findUserById(sampleUserChairperson.getUserId())).thenReturn(sampleUserChairperson);
        when(userDAO.findUserByEmailAndRoleId("test@test.com", UserRoles.CHAIRPERSON.getRoleId()))
                .thenReturn(sampleUserChairperson);
        when(userDAO.findUserByEmailAndRoleId("test@test.com", UserRoles.MEMBER.getRoleId()))
                .thenReturn(sampleUserMember);
        when(userDAO.findUsersForElectionsByRoles(
                List.of(sampleVoteChairpersonApproval.getElectionId()),
                List.of(UserRoles.CHAIRPERSON.getRoleName(), UserRoles.MEMBER.getRoleName())))
                .thenReturn(Set.of(sampleUserChairperson, sampleUserMember));
    }

    private void electionStubs() {
        when(electionDAO.getElectionWithFinalVoteByReferenceIdAndType(sampleElection1.getReferenceId(), sampleElection1.getElectionType()))
                .thenReturn(sampleElection1);
        when(electionDAO.findElectionByVoteId(any()))
                .thenReturn(sampleElection1);
        when(electionDAO.findDacForElection(sampleElection1.getElectionId()))
                .thenReturn(sampleDac1);
        when(electionDAO.findElectionWithFinalVoteById(sampleElection1.getElectionId())).thenReturn(sampleElection1);
        when(electionDAO.findElectionById(sampleElection1.getElectionId()))
                .thenReturn(sampleElection1);
        when(electionDAO.findElectionById(sampleElectionRP.getElectionId()))
                .thenReturn(sampleElectionRP);
        when(electionDAO.findRPElectionByElectionAccessId(sampleElection1.getElectionId()))
                .thenReturn(sampleElectionRP.getElectionId());
        when(electionDAO.insertElection(any(), any(), any(), any(), any())).thenReturn(1);
        when(electionDAO.findLastDataAccessElectionsWithFinalVoteByStatus(ElectionStatus.OPEN.getValue()))
                .thenReturn(List.of(sampleElection1));
        when(electionDAO.findLastDataAccessElectionsWithFinalVoteByStatus(ElectionStatus.CLOSED.getValue()))
                .thenReturn(List.of(sampleElection2));
    }

    private void voteStubs() {
        when(voteDAO.findPendingVotesByElectionId(sampleElection1.getElectionId()))
                .thenReturn(List.of(sampleVoteMember, sampleVoteChairpersonApproval));
        when(voteDAO.findPendingVotesByElectionId(sampleElection2.getElectionId()))
                .thenReturn(List.of(sampleVoteMember, sampleVoteChairpersonApproval));
        when(voteDAO.findPendingVotesByElectionId(sampleElectionRP.getElectionId()))
                .thenReturn(List.of(sampleVoteRP));
        when(voteDAO.findVotesByElectionId(sampleElection1.getElectionId()))
                .thenReturn(List.of(sampleVoteMember, sampleVoteChairpersonApproval));
        when(voteDAO.findVotesByElectionIdAndType(sampleElection1.getElectionId(), VoteType.DATA_OWNER.getValue()))
                .thenReturn(List.of(sampleVoteMember));
    }

    private void initService() {
        service = new ElectionService(consentDAO, electionDAO, voteDAO, userDAO, mailMessageDAO, emailService, dataAccessRequestService);
    }

    @Test
    public void testUpdateElectionById() {
        when(electionDAO.findRPElectionByElectionAccessId(any())).thenReturn(1);

        initService();
        Election election = service.updateElectionById(sampleElection1, sampleElection1.getElectionId());
        assertNotNull(election);
        assertEquals(sampleElection1.getElectionId(), election.getElectionId());
    }

    @Test
    public void testUpdateElectionByIdWithArchival_Closed() {
        when(electionDAO.findRPElectionByElectionAccessId(any())).thenReturn(1);
        spy(electionDAO);
        sampleElection1.setStatus(ElectionStatus.CLOSED.getValue());
        initService();

        Election election = service.updateElectionById(sampleElection1, sampleElection1.getElectionId());
        assertNotNull(election);
        assertEquals(sampleElection1.getElectionId(), election.getElectionId());
        verify(electionDAO, times(1)).archiveElectionById(any(), any());
    }

    @Test
    public void testUpdateElectionByIdWithArchival_Canceled() {
        when(electionDAO.findRPElectionByElectionAccessId(any())).thenReturn(1);
        spy(electionDAO);
        sampleElection1.setStatus(ElectionStatus.CANCELED.getValue());
        initService();

        Election election = service.updateElectionById(sampleElection1, sampleElection1.getElectionId());
        assertNotNull(election);
        assertEquals(sampleElection1.getElectionId(), election.getElectionId());
        verify(electionDAO, times(1)).archiveElectionById(any(), any());
    }

    @Test
    public void testUpdateElectionByIdWithArchival_Final() {
        when(electionDAO.findRPElectionByElectionAccessId(any())).thenReturn(1);
        spy(electionDAO);
        sampleElection1.setStatus(ElectionStatus.FINAL.getValue());
        initService();

        Election election = service.updateElectionById(sampleElection1, sampleElection1.getElectionId());
        assertNotNull(election);
        assertEquals(sampleElection1.getElectionId(), election.getElectionId());
        verify(electionDAO, times(1)).archiveElectionById(any(), any());
    }

    @Test
    public void testUpdateElectionByIdWithArchival_Open() {
        when(electionDAO.findRPElectionByElectionAccessId(any())).thenReturn(1);
        spy(electionDAO);
        sampleElection1.setStatus(ElectionStatus.OPEN.getValue());
        initService();

        Election election = service.updateElectionById(sampleElection1, sampleElection1.getElectionId());
        assertNotNull(election);
        assertEquals(sampleElection1.getElectionId(), election.getElectionId());
        verify(electionDAO, times(0)).archiveElectionById(any(), any());
    }

    @Test
    public void testDescribeDataRequestElection() {
        initService();

        Election election = service.describeDataRequestElection(sampleElection1.getReferenceId());
        assertNotNull(election);
    }

    @Test
    public void testDescribeDataRequestElection_Throws() {
        when(electionDAO.getElectionWithFinalVoteByReferenceIdAndType(sampleElection1.getReferenceId(), sampleElection1.getElectionType()))
                .thenReturn(null);
        when(electionDAO.getElectionWithFinalVoteByReferenceIdAndType(sampleElection1.getReferenceId(), sampleElection1.getElectionType()))
                .thenReturn(null);
        initService();

        try {
            service.describeDataRequestElection(sampleElection1.getReferenceId());
        } catch (Exception e) {
            assertTrue(e instanceof NotFoundException);
        }
    }

    @Test
    public void testDescribeElectionByVoteId() {
        initService();

        Election election = service.describeElectionByVoteId(sampleElection1.getElectionId());
        assertNotNull(election);
        assertEquals(sampleElection1.getElectionId(), election.getElectionId());
    }

    @Test
    public void testDescribeElectionByVoteId_Throws() {
        when(electionDAO.findElectionByVoteId(1))
                .thenReturn(null);
        initService();

        try {
            service.describeElectionByVoteId(sampleElection1.getElectionId());
        } catch (Exception e) {
            assertTrue(e instanceof NotFoundException);
        }
    }

    @Test
    public void testValidateCollectDAREmailCondition_NoVotesNoChair() {
        when(electionDAO.findElectionWithFinalVoteById(sampleVoteChairpersonApproval.getElectionId()))
                .thenReturn(sampleElection1);
        when(userDAO.findUsersForElectionsByRoles(List.of(sampleElection1.getElectionId(), sampleElectionRP.getElectionId()),
                List.of(UserRoles.CHAIRPERSON.getRoleName())))
                .thenReturn(Set.of());
        when(mailMessageDAO.existsCollectDAREmail(null, sampleElectionRP.getReferenceId()))
                .thenReturn(null);
        when(voteDAO.findVotesByElectionIdAndUserIds(sampleElectionRP.getElectionId(),
                List.of(sampleUserChairperson.getUserId())))
                .thenReturn(List.of(sampleVoteChairpersonApproval));
        when(voteDAO.findVotesByElectionIdAndUserIds(sampleElection1.getElectionId(),
                List.of(sampleUserChairperson.getUserId())))
                .thenReturn(List.of(sampleVoteChairpersonApproval));
        when(voteDAO.findPendingVotesByElectionId(sampleElectionRP.getElectionId())).thenReturn(
                List.of());
        when(voteDAO.findPendingVotesByElectionId(sampleElection1.getElectionId())).thenReturn(
                List.of());
        initService();
        boolean validate = service.validateCollectDAREmailCondition(sampleVoteChairpersonApproval);
        assertTrue(validate);
    }

    @Test
    public void testValidateCollectDAREmailCondition_NoChairCreated() {
        when(electionDAO.findElectionWithFinalVoteById(sampleVoteChairpersonApproval.getElectionId()))
                .thenReturn(sampleElection1);
        when(userDAO.findUsersForElectionsByRoles(List.of(sampleElection1.getElectionId(), sampleElectionRP.getElectionId()),
                List.of(UserRoles.CHAIRPERSON.getRoleName())))
                .thenReturn(Set.of(sampleUserChairperson));
        when(mailMessageDAO.existsCollectDAREmail(null, sampleElectionRP.getReferenceId()))
                .thenReturn(null);
        when(voteDAO.findVotesByElectionIdAndUserIds(sampleElectionRP.getElectionId(),
                List.of(sampleUserChairperson.getUserId())))
                .thenReturn(List.of(new Vote(4, true, sampleUserChairperson.getUserId(), null, null,
                        sampleElectionRP.getElectionId(), "", VoteType.AGREEMENT.getValue(),
                        false, false)));
        when(voteDAO.findVotesByElectionIdAndUserIds(sampleElection1.getElectionId(),
                List.of(sampleUserChairperson.getUserId())))
                .thenReturn(List.of(sampleVoteChairpersonApproval));
        when(voteDAO.findPendingVotesByElectionId(sampleElectionRP.getElectionId())).thenReturn(
                List.of());
        when(voteDAO.findPendingVotesByElectionId(sampleElection1.getElectionId()))
                .thenReturn(List.of(sampleVoteMember));
        initService();
        boolean validate = service.validateCollectDAREmailCondition(sampleVoteMember);
        assertTrue(validate);
    }

    @Test
    public void testValidateCollectDAREmailCondition_NeitherChairCreated() {
        when(electionDAO.findElectionWithFinalVoteById(sampleVoteChairpersonApproval.getElectionId()))
                .thenReturn(sampleElection1);
        when(userDAO.findUsersForElectionsByRoles(List.of(sampleElection1.getElectionId(), sampleElectionRP.getElectionId()),
                List.of(UserRoles.CHAIRPERSON.getRoleName())))
                .thenReturn(Set.of(sampleUserChairperson));
        when(mailMessageDAO.existsCollectDAREmail(null, sampleElectionRP.getReferenceId()))
                .thenReturn(null);
        when(voteDAO.findVotesByElectionIdAndUserIds(sampleElectionRP.getElectionId(),
                List.of(sampleUserChairperson.getUserId())))
                .thenReturn(List.of());
        when(voteDAO.findVotesByElectionIdAndUserIds(sampleElection1.getElectionId(),
                List.of(sampleUserChairperson.getUserId())))
                .thenReturn(List.of());
        when(voteDAO.findPendingVotesByElectionId(sampleElectionRP.getElectionId())).thenReturn(
                List.of(sampleVoteRP));
        when(voteDAO.findPendingVotesByElectionId(sampleElection1.getElectionId()))
                .thenReturn(List.of(sampleVoteMember));
        initService();
        boolean validate = service.validateCollectDAREmailCondition(sampleVoteMember);
        assertTrue(validate);
    }

    @Test
    public void testCloseDataOwnerApprovalElection() {
        when(electionDAO.findLastElectionByReferenceIdAndType(any(), any()))
                .thenReturn(sampleElection1);
        initService();
        service.closeDataOwnerApprovalElection(sampleElection1.getElectionId());
    }

    @Test
    public void testCheckDataOwnerToCloseElection() {
        when(electionDAO.findElectionById(any()))
                .thenReturn(new Election(5, ElectionType.DATA_SET.getValue(),
                        ElectionStatus.CLOSED.getValue(), new Date(),
                        "CONS-1", new Date(), true, sampleDataset1.getDataSetId()));
        when(voteDAO.findDataOwnerPendingVotesByElectionId(any(), any()))
                .thenReturn(List.of());
        initService();

        boolean ownerToClose = service.checkDataOwnerToCloseElection(5);
        assertTrue(ownerToClose);
    }

    @Test
    public void findElectionsByVoteIdsAndType() {
        Election election = new Election();
        when(electionDAO.findElectionsByVoteIdsAndType(anyList(), anyString()))
                .thenReturn(List.of(election));
        initService();
        List<Election> elections = service.findElectionsByVoteIdsAndType(List.of(1, 2), "test");
        assertNotNull(elections);
        assertEquals(1, elections.size());
    }

    @Test
    public void findElectionsWithCardHoldingUsersByElectionIds() {
        Election election = new Election();
        when(electionDAO.findElectionsWithCardHoldingUsersByElectionIds(anyList()))
                .thenReturn(List.of(election));
        initService();
        List<Election> elections = service.findElectionsWithCardHoldingUsersByElectionIds(List.of(1));
        assertNotNull(elections);
        assertEquals(1, elections.size());
    }

}
