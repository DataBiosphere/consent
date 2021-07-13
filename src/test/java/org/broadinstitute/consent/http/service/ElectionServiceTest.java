package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javax.ws.rs.NotFoundException;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.DatasetAssociationDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import org.broadinstitute.consent.http.enumeration.DataSetElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DatasetDetailEntry;
import org.broadinstitute.consent.http.models.DatasetEntry;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.grammar.And;
import org.broadinstitute.consent.http.models.grammar.Named;
import org.broadinstitute.consent.http.models.grammar.Not;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private LibraryCardDAO libraryCardDAO;
    @Mock
    private DatasetAssociationDAO datasetAssociationDAO;
    @Mock
    private DacService dacService;
    @Mock
    private DataAccessRequestService dataAccessRequestService;
    @Mock
    private DataAccessRequestDAO dataAccessRequestDAO;
    @Mock
    private EmailNotifierService emailNotifierService;
    @Mock
    private UseRestrictionConverter useRestrictionConverter;

    private static final Gson gson = new GsonBuilder().setDateFormat("MMM d, yyyy").create();

    private static Election sampleElection1;
    private static Election sampleElection2;
    private static Election sampleElectionRP;
    private static Election sampleDatasetElection;
    private static Election sampleDatasetElectionDenied;
    private static Election sampleDatasetElectionApproved;
    private static DataSet sampleDataset1;
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
    private static LibraryCard sampleLibraryCard;

    @BeforeClass
    public static void setUpClass() {
        UseRestriction sampleUseRestriction1 = new And(
                new Not(new Named("http://purl.obolibrary.org/obo/DUO_0000015")),
                new Not(new Named("http://purl.obolibrary.org/obo/DUO_0000011")),
                new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/control"))
        );
        String referenceId = "CONS-1";

        sampleLibraryCard = new LibraryCard();

        sampleDataset1 = new DataSet();
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
        sampleDatasetElection = new Election(4, ElectionType.DATA_SET.getValue(), ElectionStatus.OPEN.getValue(), new Date(),
                sampleDataset1.getConsentName(), new Date(), false, sampleDataset1.getDataSetId());
        sampleDatasetElectionDenied = new Election(5, ElectionType.DATA_SET.getValue(), ElectionStatus.CLOSED.getValue(), new Date(),
                sampleDataset1.getConsentName(), new Date(), false, sampleDataset1.getDataSetId());
        sampleDatasetElectionApproved = new Election(6, ElectionType.DATA_SET.getValue(), ElectionStatus.CLOSED.getValue(), new Date(),
                sampleDataset1.getConsentName(), new Date(), true, sampleDataset1.getDataSetId());

        authUser = new AuthUser("test@test.com");
        sampleUserChairperson = new User(1, "test@test.com", "Test User", new Date());
        sampleUserChairperson.addRole(new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName()));

        sampleUserMember = new User(2, "test@test.com", "Test User", new Date());
        sampleUserMember.addRole(new UserRole(UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName()));

        sampleConsent1 = new Consent(false, sampleUseRestriction1, "A data use letter", "sampleConsent1", null, null, null, "Group Name Test");
        sampleConsent1.setConsentId(sampleDataset1.getConsentName());

        sampleDataAccessRequest1 = new DataAccessRequest();
        sampleDataAccessRequest1.setUserId(2);
        DataAccessRequestData data = new DataAccessRequestData();
        data.setReferenceId(sampleElection1.getReferenceId());
        data.setDatasetIds(Arrays.asList(sampleDataset1.getDataSetId()));
        DatasetEntry entry = new DatasetEntry();
        entry.setKey(sampleDataset1.getConsentName());
        entry.setValue(sampleDataset1.getName());
        data.setDatasets(Arrays.asList(entry));
        DatasetDetailEntry entryDetail = new DatasetDetailEntry();
        entryDetail.setDatasetId(sampleDataset1.getDataSetId().toString());
        entryDetail.setName(sampleDataset1.getName());
        entryDetail.setObjectId(sampleDataset1.getObjectId());
        data.setDatasetDetail(Arrays.asList(entryDetail));
        sampleDataAccessRequest1.setData(data);

        sampleDac1 = new Dac();
        sampleDac1.setDacId(1);
        sampleDac1.setName("DAC-1");
        sampleDac1.setCreateDate(new Date());
        sampleDac1.setUpdateDate(new Date());

        sampleVoteChairpersonApproval = new Vote();
        sampleVoteChairpersonApproval.setElectionId(sampleElection1.getElectionId());
        sampleVoteChairpersonApproval.setDacUserId(sampleUserChairperson.getDacUserId());
        sampleVoteChairpersonApproval.setVote(true);
        sampleVoteChairpersonApproval.setVoteId(1);
        sampleVoteChairpersonApproval.setRationale("Go for it");

        sampleVoteChairpersonReject = new Vote();
        sampleVoteChairpersonReject.setElectionId(sampleElection1.getElectionId());
        sampleVoteChairpersonReject.setDacUserId(sampleUserChairperson.getDacUserId());
        sampleVoteChairpersonReject.setVote(false);
        sampleVoteChairpersonReject.setVoteId(1);
        sampleVoteChairpersonReject.setRationale("Rejection vote");

        sampleVoteMember = new Vote();
        sampleVoteMember.setElectionId(sampleElection1.getElectionId());
        sampleVoteMember.setDacUserId(sampleUserMember.getDacUserId());
        sampleVoteMember.setVote(true);
        sampleVoteMember.setVoteId(2);
        sampleVoteMember.setRationale("Go for it");

        sampleVoteRP = new Vote();
        sampleVoteRP.setElectionId(sampleElectionRP.getElectionId());
        sampleVoteRP.setDacUserId(sampleUserMember.getDacUserId());
        sampleVoteRP.setVote(true);
        sampleVoteRP.setVoteId(3);
        sampleVoteRP.setRationale("Yep");
    }


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        bunchOfDoNothings();
        goGetters();
    }

    private void bunchOfDoNothings() throws Exception {
        doNothing().when(emailNotifierService).sendDisabledDatasetsMessage(any(), any(), any());
        doNothing().when(dataAccessRequestDAO).updateDataByReferenceId(any(), any());
        doNothing().when(consentDAO).updateConsentSortDate(any(), any());
        doNothing().when(dataAccessRequestDAO).updateDataByReferenceIdVersion2(any(), any(), any(), any(), any(), any());
        doNothing().when(electionDAO).insertAccessAndConsentElection(any(), any());
        doNothing().when(consentDAO).updateConsentUpdateStatus(any(), any());
        doNothing().when(electionDAO).insertAccessRP(any(), any());
        doNothing().when(electionDAO).updateElectionById(any(), any(), any());
        doNothing().when(electionDAO).updateElectionById(any(), any(), any(), any());
        doNothing().when(electionDAO).archiveElectionById(any(), any());
        doNothing().when(electionDAO).updateElectionStatus(any(), any());
        doNothing().when(emailNotifierService).sendResearcherDarApproved(any(), any(), any(), any());
        doNothing().when(emailNotifierService).sendDataCustodianApprovalMessage(any(), any(), any(), any(), any());
        doNothing().when(emailNotifierService).sendClosedDataSetElectionsMessage(any());
        doNothing().when(voteDAO).deleteVoteById(any());
        doNothing().when(electionDAO).deleteElectionById(any());
        doNothing().when(electionDAO).deleteAccessRP(any());
    }

    private void goGetters() {
        userStubs();
        electionStubs();
        voteStubs();

        when(dataSetDAO.findDataSetsByAuthUserEmail(authUser.getName()))
                .thenReturn(Arrays.asList(sampleDataset1));
        when(dataAccessRequestDAO.findByReferenceIds(any()))
                .thenReturn(Arrays.asList(sampleDataAccessRequest1));
        when(consentDAO.findConsentsFromConsentsIDs(any()))
                .thenReturn(Arrays.asList(sampleConsent1));
        when(dataAccessRequestService.findByReferenceId(any()))
                .thenReturn(sampleDataAccessRequest1);
        when(dataAccessRequestDAO.findByReferenceId(any()))
                .thenReturn(sampleDataAccessRequest1);
        when(dataSetDAO.findDatasetsByIdList(any())).thenReturn(Arrays.asList(sampleDataset1));
        when(consentDAO.checkConsentById(sampleConsent1.getConsentId())).thenReturn(sampleConsent1.getConsentId());
        when(dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(any())).thenReturn(new Document());
        when(consentDAO.findConsentFromDatasetID(sampleDataset1.getDataSetId())).thenReturn(sampleConsent1);
    }

    private void userStubs() {
        when(userDAO.findUsersWithRoles(any())).thenReturn(Set.of(sampleUserChairperson, sampleUserMember));
        when(userDAO.findUsersEnabledToVoteByDAC(any())).thenReturn(Set.of(sampleUserChairperson, sampleUserMember));
        when(userDAO.findNonDacUsersEnabledToVote()).thenReturn(Set.of(sampleUserChairperson, sampleUserMember));
        when(userDAO.findUserById(sampleUserChairperson.getDacUserId())).thenReturn(sampleUserChairperson);
        when(userDAO.findUserByEmailAndRoleId("test@test.com", UserRoles.CHAIRPERSON.getRoleId()))
                .thenReturn(sampleUserChairperson);
        when(userDAO.findUserByEmailAndRoleId("test@test.com", UserRoles.MEMBER.getRoleId()))
                .thenReturn(sampleUserMember);
        when(userDAO.findUsersForElectionsByRoles(Arrays.asList(sampleVoteChairpersonApproval.getElectionId()),
                Arrays.asList(UserRoles.CHAIRPERSON.getRoleName(), UserRoles.MEMBER.getRoleName())))
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
        when(electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(sampleElection1.getReferenceId(), sampleElection1.getElectionType()))
                .thenReturn(sampleElection1);
        when(electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(sampleElection2.getReferenceId(), sampleElection1.getElectionType()))
                .thenReturn(sampleElection2);
        when(electionDAO.insertElection(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(electionDAO.findLastDataAccessElectionsWithFinalVoteByStatus(ElectionStatus.OPEN.getValue()))
                .thenReturn(Arrays.asList(sampleElection1));
        when(electionDAO.findLastDataAccessElectionsWithFinalVoteByStatus(ElectionStatus.CLOSED.getValue()))
                .thenReturn(Arrays.asList(sampleElection2));
    }

    private void voteStubs() {
        when(voteDAO.findPendingVotesByElectionId(sampleElection1.getElectionId()))
                .thenReturn(Arrays.asList(sampleVoteMember, sampleVoteChairpersonApproval));
        when(voteDAO.findPendingVotesByElectionId(sampleElection2.getElectionId()))
                .thenReturn(Arrays.asList(sampleVoteMember, sampleVoteChairpersonApproval));
        when(voteDAO.findPendingVotesByElectionId(sampleElectionRP.getElectionId()))
                .thenReturn(Arrays.asList(sampleVoteRP));
        when(voteDAO.findVotesByElectionId(sampleElection1.getElectionId()))
                .thenReturn(Arrays.asList(sampleVoteMember, sampleVoteChairpersonApproval));
        when(voteDAO.findVotesByElectionIdAndType(sampleElection1.getElectionId(), VoteType.DATA_OWNER.getValue()))
                .thenReturn(Arrays.asList(sampleVoteMember));
    }

    private void initService() {
        service = new ElectionService(consentDAO, electionDAO, voteDAO, userDAO, dataSetDAO, libraryCardDAO, datasetAssociationDAO, mailMessageDAO, dacService, emailNotifierService, dataAccessRequestService, useRestrictionConverter);
    }

    @Test
    public void testDescribeClosedElectionsByType_DataAccess() {
        when(dacService.filterElectionsByDAC(any(), any()))
                .thenReturn(Arrays.asList(sampleElection2));
        initService();
        List<Election> elections = service.describeClosedElectionsByType(ElectionType.DATA_ACCESS.getValue(), authUser);
        assertNotNull(elections);
        assertEquals(1, elections.size());
    }

    @Test
    public void testDescribeClosedElectionsByType_Other() {
        when(dacService.filterElectionsByDAC(any(), any()))
                .thenReturn(Arrays.asList(sampleElection2));
        initService();
        List<Election> elections = service.describeClosedElectionsByType(ElectionType.DATA_SET.getValue(), authUser);
        assertNotNull(elections);
        assertEquals(1, elections.size());
    }

    @Test
    public void testCreateElection() throws Exception {
        when(electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(any(), any())).thenReturn(null);
        initService();
        Election election = service.createElection(sampleElection1, sampleElection1.getReferenceId(), ElectionType.DATA_ACCESS);
        assertNotNull(election);
        assertEquals(ElectionType.DATA_ACCESS.getValue(), election.getElectionType());
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
    public void testSubmitFinalAccessVoteDataRequestElection() throws Exception {
        initService();
        when(libraryCardDAO.findLibraryCardsByUserId(any())).thenReturn(List.of(sampleLibraryCard));
        Election election = service.submitFinalAccessVoteDataRequestElection(sampleElection1.getElectionId(), true);
        assertNotNull(election);
        assertEquals(sampleElection1.getElectionId(), election.getElectionId());
    }

    @Test(expected = NotFoundException.class)
    public void testSubmitFinalAccessVoteDataRequestElection_noLibraryCard_DataAccessApproval() throws Exception {
        initService();
        when(libraryCardDAO.findLibraryCardsByUserId(any())).thenReturn(List.of());
        service.submitFinalAccessVoteDataRequestElection(sampleElection1.getElectionId(), true);
    }

    @Test
    public void testSubmitFinalAccessVoteDataRequestElection_noLibraryCard_DataAccessRejection() {
        initService();
        when(libraryCardDAO.findLibraryCardsByUserId(any())).thenReturn(List.of());
        try{
            Election election = service.submitFinalAccessVoteDataRequestElection(sampleElection1.getElectionId(), false);
            assertNotNull(election);
            assertEquals(sampleElection1.getElectionId(), election.getElectionId());
        //function throws exception, need to have a catch block to handle it
        } catch(Exception e) {
            Assert.fail("Vote should not have failed");
        }
    }

    @Test
    public void testDeleteElection() {
        initService();

        service.deleteElection(sampleElection1.getReferenceId(), sampleElection1.getElectionId());
    }

    @Test
    public void testDescribeDataRequestElection() {
        initService();

        Election election = service.describeDataRequestElection(sampleElection1.getReferenceId());
        assertNotNull(election);
    }

    @Test(expected = NotFoundException.class)
    public void testDescribeDataRequestElection_Throws() {
        when(electionDAO.getElectionWithFinalVoteByReferenceIdAndType(sampleElection1.getReferenceId(), sampleElection1.getElectionType()))
                .thenReturn(null);
        when(electionDAO.getElectionWithFinalVoteByReferenceIdAndType(sampleElection1.getReferenceId(), sampleElection1.getElectionType()))
                .thenReturn(null);
        initService();

        Election election = service.describeDataRequestElection(sampleElection1.getReferenceId());
    }

    @Test
    public void testDescribeElectionByVoteId() {
        initService();

        Election election = service.describeElectionByVoteId(sampleElection1.getElectionId());
        assertNotNull(election);
        assertEquals(sampleElection1.getElectionId(), election.getElectionId());
    }

    @Test(expected = NotFoundException.class)
    public void testDescribeElectionByVoteId_Throws() {
        when(electionDAO.findElectionByVoteId(1))
                .thenReturn(null);
        initService();

        Election election = service.describeElectionByVoteId(sampleElection1.getElectionId());
    }

    @Test
    public void testValidateCollectEmailCondition_Member() {
        when(voteDAO.findPendingVotesByElectionId(sampleElection1.getElectionId())).thenReturn(Arrays.asList(sampleVoteMember));
        when(userDAO.findUsersWithRoles(any())).thenReturn(Set.of(sampleUserMember));
        initService();

        boolean validate = service.validateCollectEmailCondition(sampleVoteMember);
        assertEquals(false, validate);
    }

    @Test
    public void testValidateCollectEmailCondition_NoMember() {
        when(voteDAO.findPendingVotesByElectionId(sampleElection1.getElectionId())).thenReturn(Arrays.asList(sampleVoteChairpersonApproval));
        when(userDAO.findUsersWithRoles(any())).thenReturn(Set.of(sampleUserChairperson));
        initService();

        boolean validate = service.validateCollectEmailCondition(sampleVoteChairpersonApproval);
        assertEquals(true, validate);
    }

    @Test
    public void testValidateCollectDAREmailCondition_NoVotesNoChair() {
        when(electionDAO.findElectionWithFinalVoteById(sampleVoteChairpersonApproval.getElectionId()))
                .thenReturn(sampleElection1);
        when(userDAO.findUsersForElectionsByRoles(Arrays.asList(sampleElection1.getElectionId(), sampleElectionRP.getElectionId()),
                Collections.singletonList(UserRoles.CHAIRPERSON.getRoleName())))
                .thenReturn(Set.of());
        when(mailMessageDAO.existsCollectDAREmail(null, sampleElectionRP.getReferenceId()))
                .thenReturn(null);
        when(voteDAO.findVotesByElectionIdAndDACUserIds(sampleElectionRP.getElectionId(), Arrays.asList(sampleUserChairperson.getDacUserId())))
                .thenReturn(Arrays.asList(sampleVoteChairpersonApproval));
        when(voteDAO.findVotesByElectionIdAndDACUserIds(sampleElection1.getElectionId(), Arrays.asList(sampleUserChairperson.getDacUserId())))
                .thenReturn(Arrays.asList(sampleVoteChairpersonApproval));
        when(voteDAO.findPendingVotesByElectionId(sampleElectionRP.getElectionId())).thenReturn(Arrays.asList());
        when(voteDAO.findPendingVotesByElectionId(sampleElection1.getElectionId())).thenReturn(Arrays.asList());
        initService();
        boolean validate = service.validateCollectDAREmailCondition(sampleVoteChairpersonApproval);
        assertEquals(true, validate);
    }

    @Test
    public void testValidateCollectDAREmailCondition_NoChairCreated() {
        when(electionDAO.findElectionWithFinalVoteById(sampleVoteChairpersonApproval.getElectionId()))
                .thenReturn(sampleElection1);
        when(userDAO.findUsersForElectionsByRoles(Arrays.asList(sampleElection1.getElectionId(), sampleElectionRP.getElectionId()),
                Collections.singletonList(UserRoles.CHAIRPERSON.getRoleName())))
                .thenReturn(Set.of(sampleUserChairperson));
        when(mailMessageDAO.existsCollectDAREmail(null, sampleElectionRP.getReferenceId()))
                .thenReturn(null);
        when(voteDAO.findVotesByElectionIdAndDACUserIds(sampleElectionRP.getElectionId(), Arrays.asList(sampleUserChairperson.getDacUserId())))
                .thenReturn(Arrays.asList(new Vote(4, true, sampleUserChairperson.getDacUserId(), null, null,
                        sampleElectionRP.getElectionId(), "", VoteType.AGREEMENT.getValue(),
                        false, false)));
        when(voteDAO.findVotesByElectionIdAndDACUserIds(sampleElection1.getElectionId(), Arrays.asList(sampleUserChairperson.getDacUserId())))
                .thenReturn(Arrays.asList(sampleVoteChairpersonApproval));
        when(voteDAO.findPendingVotesByElectionId(sampleElectionRP.getElectionId())).thenReturn(Arrays.asList());
        when(voteDAO.findPendingVotesByElectionId(sampleElection1.getElectionId()))
                .thenReturn(Arrays.asList(sampleVoteMember));
        initService();
        boolean validate = service.validateCollectDAREmailCondition(sampleVoteMember);
        assertEquals(true, validate);
    }

    @Test
    public void testValidateCollectDAREmailCondition_NeitherChairCreated() {
        when(electionDAO.findElectionWithFinalVoteById(sampleVoteChairpersonApproval.getElectionId()))
                .thenReturn(sampleElection1);
        when(userDAO.findUsersForElectionsByRoles(Arrays.asList(sampleElection1.getElectionId(), sampleElectionRP.getElectionId()),
                Collections.singletonList(UserRoles.CHAIRPERSON.getRoleName())))
                .thenReturn(Set.of(sampleUserChairperson));
        when(mailMessageDAO.existsCollectDAREmail(null, sampleElectionRP.getReferenceId()))
                .thenReturn(null);
        when(voteDAO.findVotesByElectionIdAndDACUserIds(sampleElectionRP.getElectionId(), Arrays.asList(sampleUserChairperson.getDacUserId())))
                .thenReturn(Arrays.asList());
        when(voteDAO.findVotesByElectionIdAndDACUserIds(sampleElection1.getElectionId(), Arrays.asList(sampleUserChairperson.getDacUserId())))
                .thenReturn(Arrays.asList());
        when(voteDAO.findPendingVotesByElectionId(sampleElectionRP.getElectionId())).thenReturn(Arrays.asList(sampleVoteRP));
        when(voteDAO.findPendingVotesByElectionId(sampleElection1.getElectionId()))
                .thenReturn(Arrays.asList(sampleVoteMember));
        initService();
        boolean validate = service.validateCollectDAREmailCondition(sampleVoteMember);
        assertEquals(true, validate);
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
                .thenReturn(Arrays.asList());
        initService();

        boolean ownerToClose = service.checkDataOwnerToCloseElection(5);
        assertEquals(true, ownerToClose);
    }

    @Test
    public void testDarDatasetElectionStatus_NoApproval() {
        when(dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(sampleElection1.getReferenceId()))
                .thenReturn(createDocumentFromDar(sampleDataAccessRequest1));
        when(dataSetDAO.findNeedsApprovalDataSetByDataSetId(any()))
                .thenReturn(Arrays.asList());
        initService();

        String status = service.darDatasetElectionStatus(sampleElection1.getReferenceId());
        assertEquals(DataSetElectionStatus.APPROVAL_NOT_NEEDED.getValue(), status);
    }

    @Test
    public void testDarDatasetElectionStatus_Pending() {
        when(dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(sampleElection1.getReferenceId()))
                .thenReturn(createDocumentFromDar(sampleDataAccessRequest1));
        when(dataSetDAO.findNeedsApprovalDataSetByDataSetId(any()))
                .thenReturn(Arrays.asList(sampleDataset1));
        when(electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(any(), any()))
                .thenReturn(sampleElection1);
        when(electionDAO.findLastElectionsWithFinalVoteByReferenceIdsAndType(any(), any()))
                .thenReturn(Arrays.asList());
        initService();

        String status = service.darDatasetElectionStatus(sampleElection1.getReferenceId());
        assertEquals(DataSetElectionStatus.DS_PENDING.getValue(), status);
    }

    @Test
    public void testDarDatasetElectionStatus_OpenElection() {
        when(dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(sampleElection1.getReferenceId()))
                .thenReturn(createDocumentFromDar(sampleDataAccessRequest1));
        when(dataSetDAO.findNeedsApprovalDataSetByDataSetId(any()))
                .thenReturn(Arrays.asList(sampleDataset1));
        when(electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(any(), any()))
                .thenReturn(sampleElection2);
        when(electionDAO.findLastElectionsWithFinalVoteByReferenceIdsAndType(any(), any()))
                .thenReturn(Arrays.asList(sampleDatasetElection));
        initService();

        String status = service.darDatasetElectionStatus(sampleElection1.getReferenceId());
        assertEquals(DataSetElectionStatus.DS_PENDING.getValue(), status);
    }

    @Test
    public void testDarDatasetElectionStatus_DeniedElection() {
        when(dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(sampleElection1.getReferenceId()))
                .thenReturn(createDocumentFromDar(sampleDataAccessRequest1));
        when(dataSetDAO.findNeedsApprovalDataSetByDataSetId(any()))
                .thenReturn(Arrays.asList(sampleDataset1));
        when(electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(any(), any()))
                .thenReturn(sampleElection2);
        when(electionDAO.findLastElectionsWithFinalVoteByReferenceIdsAndType(any(), any()))
                .thenReturn(Arrays.asList(sampleDatasetElectionDenied));
        initService();

        String status = service.darDatasetElectionStatus(sampleElection1.getReferenceId());
        assertEquals(DataSetElectionStatus.DS_DENIED.getValue(), status);
    }

    @Test
    public void testDarDatasetElectionStatus_ApprovedElection() {
        when(dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(sampleElection1.getReferenceId()))
                .thenReturn(createDocumentFromDar(sampleDataAccessRequest1));
        when(dataSetDAO.findNeedsApprovalDataSetByDataSetId(any()))
                .thenReturn(Arrays.asList(sampleDataset1));
        when(electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(any(), any()))
                .thenReturn(sampleElection2);
        when(electionDAO.findLastElectionsWithFinalVoteByReferenceIdsAndType(any(), any()))
                .thenReturn(Arrays.asList(sampleDatasetElectionApproved));
        initService();

        String status = service.darDatasetElectionStatus(sampleElection1.getReferenceId());
        assertEquals(DataSetElectionStatus.DS_APPROVED.getValue(), status);
    }

    @Test
    public void testCreateDataSetElections() {
        when(electionDAO.getOpenElectionByReferenceIdAndDataSet(sampleElection1.getReferenceId(), sampleDataset1.getDataSetId()))
                .thenReturn(null);
        when(electionDAO.findElectionsByIds(Arrays.asList(sampleDatasetElection.getElectionId())))
            .thenReturn(Arrays.asList(sampleElection1));
        when(electionDAO.insertElection(any(), any(), any(), any(), any()))
                .thenReturn(sampleDatasetElection.getElectionId());
        initService();
        List<Election> elections = service.createDataSetElections(sampleElection1.getReferenceId(), Map.of(sampleUserMember, Arrays.asList(sampleDataset1)));
        assertNotNull(elections);
        assertEquals(1, elections.size());
        assertEquals(sampleDatasetElection.getReferenceId(), elections.get(0).getReferenceId());
    }

    @Test
    public void testIsDataSetElectionOpen() {
        when(electionDAO.getElectionByTypeAndStatus(ElectionType.DATA_SET.getValue(), ElectionStatus.OPEN.getValue()))
                .thenReturn(Arrays.asList(sampleElection1));
        initService();
        boolean isOpen = service.isDataSetElectionOpen();
        assertEquals(true, isOpen);
    }

    @Test
    public void testIsDataSetElectionOpen_False() {
        when(electionDAO.getElectionByTypeAndStatus(ElectionType.DATA_SET.getValue(), ElectionStatus.OPEN.getValue()))
                .thenReturn(Arrays.asList());
        initService();
        boolean isOpen = service.isDataSetElectionOpen();
        assertEquals(false, isOpen);
    }

    private Document createDocumentFromDar(DataAccessRequest d) {
        Document document = Document.parse(gson.toJson(d.getData()));
        document.put(DarConstants.DATA_ACCESS_REQUEST_ID, d.getId());
        document.put(DarConstants.REFERENCE_ID, d.getReferenceId());
        document.put(DarConstants.CREATE_DATE, d.getCreateDate());
        document.put(DarConstants.SORT_DATE, d.getSortDate());
        return document;
    }
}
