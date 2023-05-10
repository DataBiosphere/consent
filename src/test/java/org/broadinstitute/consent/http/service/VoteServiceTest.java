package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetAssociationDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.dao.VoteServiceDAO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class VoteServiceTest {

    private VoteService service;

    @Mock
    private UserDAO userDAO;
    @Mock
    private DarCollectionDAO darCollectionDAO;
    @Mock
    private DataAccessRequestDAO dataAccessRequestDAO;
    @Mock
    private DatasetAssociationDAO datasetAssociationDAO;
    @Mock
    private DatasetDAO datasetDAO;
    @Mock
    private ElectionDAO electionDAO;
    @Mock
    private EmailService emailService;
    @Mock
    private UseRestrictionConverter useRestrictionConverter;
    @Mock
    private VoteDAO voteDAO;
    @Mock
    private VoteServiceDAO voteServiceDAO;

    @BeforeEach
    public void setUp() {
        openMocks(this);
        doNothings();
    }

    private void doNothings() {
        doNothing().when(voteDAO).updateVote(anyBoolean(), anyString(), any(), anyInt(), anyBoolean(), anyInt(), any(), anyBoolean());
    }

    private void initService() {
        service = new VoteService(userDAO, darCollectionDAO, dataAccessRequestDAO, datasetAssociationDAO, datasetDAO, electionDAO, emailService, useRestrictionConverter, voteDAO, voteServiceDAO);
    }

    @Test
    public void testFindVotesByReferenceId() {
        when(voteDAO.findVotesByReferenceId(any())).thenReturn(Collections.emptyList());
        initService();

        Collection<Vote> votes = service.findVotesByReferenceId(UUID.randomUUID().toString());
        assertTrue(votes.isEmpty());
    }

    @Test
    public void testAdvanceVotes() {
        Vote v = setUpTestVote(false, false);
        initService();

        try {
            service.advanceVotes(Collections.singletonList(v), true, "New Rationale");
        } catch (Exception e) {
            Assertions.fail("Should not error: " + e.getMessage());
        }
    }

    @Test
    public void testUpdateVote() {
        Vote v = setUpTestVote(false, false);
        initService();

        Vote vote = service.updateVote(v);
        Assertions.assertNotNull(vote);
    }

    @Test
    public void testUpdateVote_InvalidReferenceId() {
        when(voteDAO.checkVoteById("test", 11))
                .thenReturn(null);
        Vote v = setUpTestVote(false, false);
        initService();

        try {
            service.updateVote(v, 11, "test");
        } catch (Exception e) {
            assertTrue(e instanceof NotFoundException);
        }
    }

    @Test
    public void testUpdateVote_ByReferenceId() {
        Vote v = setUpTestVote(false, false);
        when(voteDAO.checkVoteById("test", v.getVoteId()))
                .thenReturn(v.getVoteId());
        when(electionDAO.getOpenElectionIdByReferenceId("test"))
                .thenReturn(1);
        initService();

        Vote vote = service.updateVote(v, v.getVoteId(), "test");
        Assertions.assertNotNull(vote);
    }

    @Test
    public void testUpdateVotesWithValue() throws Exception {
        initService();

        List<Vote> votes = service.updateVotesWithValue(List.of(), true, "rationale");
        Assertions.assertNotNull(votes);
        assertTrue(votes.isEmpty());
    }

    @Test
    public void testFindVotesByIds() {
        when(voteDAO.findVotesByIds(any())).thenReturn(List.of(new Vote()));
        initService();
        List<Vote> votes = service.findVotesByIds(List.of(1));
        Assertions.assertNotNull(votes);
        assertFalse(votes.isEmpty());
    }

    @Test
    public void testFindVotesByIds_emptyList() {
        initService();
        List<Vote> votes = service.findVotesByIds(List.of());
        Assertions.assertNotNull(votes);
        assertTrue(votes.isEmpty());
    }

    @Test
    public void testChairCreateVotesDataAccess() {
        setUpUserAndElectionVotes(UserRoles.CHAIRPERSON);
        initService();

        List<Vote> votes = service.createVotes(new Election(), ElectionType.DATA_ACCESS, false);
        assertFalse(votes.isEmpty());
        // Should create 4 votes:
        // Chairperson as a chair
        // Chairperson as a dac member
        // Final vote
        // Manual review Agreement vote
        Assertions.assertEquals(4, votes.size());
    }

    @Test
    public void testMemberCreateVotesDataAccess() {
        setUpUserAndElectionVotes(UserRoles.MEMBER);
        initService();

        List<Vote> votes = service.createVotes(new Election(), ElectionType.DATA_ACCESS, false);
        assertFalse(votes.isEmpty());
        // Should create 1 member vote
        Assertions.assertEquals(1, votes.size());
    }

    @Test
    public void testChairCreateVotesDataAccessManualReview() {
        setUpUserAndElectionVotes(UserRoles.CHAIRPERSON);
        initService();

        List<Vote> votes = service.createVotes(new Election(), ElectionType.DATA_ACCESS, true);
        assertFalse(votes.isEmpty());
        // Should create 3 votes:
        // Chairperson as a chair
        // Chairperson as a dac member
        // Final vote
        Assertions.assertEquals(3, votes.size());
    }

    @Test
    public void testMemberCreateVotesDataAccessManualReview() {
        setUpUserAndElectionVotes(UserRoles.MEMBER);
        initService();

        List<Vote> votes = service.createVotes(new Election(), ElectionType.DATA_ACCESS, false);
        assertFalse(votes.isEmpty());
        // Should create 1 member vote
        Assertions.assertEquals(1, votes.size());
    }

    @Test
    public void testChairCreateVotesTranslateDUL() {
        setUpUserAndElectionVotes(UserRoles.CHAIRPERSON);
        initService();

        List<Vote> votes = service.createVotes(new Election(), ElectionType.TRANSLATE_DUL, false);
        assertFalse(votes.isEmpty());
        // Should create 2 votes:
        // Chairperson as a chair
        // Chairperson as a dac member
        Assertions.assertEquals(2, votes.size());
    }

    @Test
    public void testMemberCreateVotesTranslateDUL() {
        setUpUserAndElectionVotes(UserRoles.MEMBER);
        initService();

        List<Vote> votes = service.createVotes(new Election(), ElectionType.TRANSLATE_DUL, false);
        assertFalse(votes.isEmpty());
        // Should create 1 member vote
        Assertions.assertEquals(1, votes.size());
    }

    @Test
    public void testChairCreateVotesRP() {
        setUpUserAndElectionVotes(UserRoles.CHAIRPERSON);
        initService();

        List<Vote> votes = service.createVotes(new Election(), ElectionType.RP, false);
        assertFalse(votes.isEmpty());
        // Should create 2 votes:
        // Chairperson as a chair
        // Chairperson as a dac member
        Assertions.assertEquals(2, votes.size());
    }

    @Test
    public void testMemberCreateVotesRP() {
        setUpUserAndElectionVotes(UserRoles.MEMBER);
        initService();

        List<Vote> votes = service.createVotes(new Election(), ElectionType.RP, false);
        assertFalse(votes.isEmpty());
        // Should create 1 member vote
        Assertions.assertEquals(1, votes.size());
    }

    @Test
    public void testCreateDataOwnersReviewVotes() {
        Election e = new Election();
        e.setElectionId(1);
        e.setDataSetId(1);
        when(datasetAssociationDAO.getDataOwnersOfDataSet(anyInt())).thenReturn(Collections.singletonList(1));
        Vote v = new Vote();
        v.setVoteId(1);
        when(voteDAO.insertVote(anyInt(), anyInt(), any())).thenReturn(v.getVoteId());
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);
        when(voteDAO.findVotesByElectionIdAndType(anyInt(), anyString())).thenReturn(Collections.singletonList(v));
        initService();

        List<Vote> votes = service.createDataOwnersReviewVotes(e);
        assertFalse(votes.isEmpty());
    }

    @Test
    public void testUpdateVotesWithValue_NoRationale() throws Exception {
        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of());
        Vote v = setUpTestVote(true, true);
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);
        when(voteServiceDAO.updateVotesWithValue(any(), anyBoolean(), any())).thenReturn(List.of(v));

        Election accessElection = new Election();
        accessElection.setElectionType(ElectionType.DATA_ACCESS.getValue());
        accessElection.setStatus(ElectionStatus.OPEN.getValue());
        Election rpElection = new Election();
        rpElection.setElectionType(ElectionType.RP.getValue());
        rpElection.setStatus(ElectionStatus.OPEN.getValue());
        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(accessElection, rpElection));

        initService();

        try {
            service.updateVotesWithValue(List.of(v), true, null);
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    public void testUpdateVotesWithValue_emptyList() throws Exception {
        when(voteServiceDAO.updateVotesWithValue(any(), anyBoolean(), any())).thenReturn(List.of());
        initService();
        List<Vote> votes = service.updateVotesWithValue(List.of(), true, "rationale");
        Assertions.assertNotNull(votes);
        assertTrue(votes.isEmpty());
    }

    @Test
    public void testUpdateVotesWithValue_ClosedElection() throws Exception {
        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of());
        Vote v = setUpTestVote(true, true);
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);
        when(voteServiceDAO.updateVotesWithValue(any(), anyBoolean(), any())).thenReturn(List.of(v));

        Election closedAccessElection = new Election();
        closedAccessElection.setElectionType(ElectionType.DATA_ACCESS.getValue());
        closedAccessElection.setStatus(ElectionStatus.CLOSED.getValue());
        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(closedAccessElection));

        initService();

        try {
            service.updateVotesWithValue(List.of(v), true, "rationale");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }


    @Test
    public void testUpdateVotesWithValue_MultipleElectionsDifferentStatuses() throws Exception {
        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of());
        Vote v = setUpTestVote(true, true);
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);
        when(voteServiceDAO.updateVotesWithValue(any(), anyBoolean(), any())).thenReturn(List.of(v));

        Election openAccessElection = new Election();
        openAccessElection.setElectionType(ElectionType.DATA_ACCESS.getValue());
        openAccessElection.setStatus(ElectionStatus.OPEN.getValue());
        Election closedAccessElection = new Election();
        closedAccessElection.setElectionType(ElectionType.DATA_ACCESS.getValue());
        closedAccessElection.setStatus(ElectionStatus.CLOSED.getValue());
        Election canceledAccessElection = new Election();
        canceledAccessElection.setElectionType(ElectionType.DATA_ACCESS.getValue());
        canceledAccessElection.setStatus(ElectionStatus.CANCELED.getValue());
        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(openAccessElection, closedAccessElection, canceledAccessElection));

        initService();

        try {
            service.updateVotesWithValue(List.of(v), true, "rationale");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testUpdateVotesWithValue_OpenRPElection() throws Exception {
        testUpdateVotesWithValue_RPElectionWithStatus(ElectionStatus.OPEN);
    }

    @Test
    public void testUpdateVotesWithValue_ClosedRPElection() throws Exception {
        testUpdateVotesWithValue_RPElectionWithStatus(ElectionStatus.CLOSED);
    }

    @Test
    public void testUpdateVotesWithValue_CanceledRPElection() throws Exception {
        testUpdateVotesWithValue_RPElectionWithStatus(ElectionStatus.CANCELED);
    }

    @Test
    public void testUpdateVotesWithValue_FinalRPElection() throws Exception {
        testUpdateVotesWithValue_RPElectionWithStatus(ElectionStatus.FINAL);
    }

    @Test
    public void testUpdateVotesWithValue_PendingApprovalRPElection() throws Exception {
        testUpdateVotesWithValue_RPElectionWithStatus(ElectionStatus.PENDING_APPROVAL);
    }

    @Test
    public void testUpdateVotesWithValue_MultipleElectionTypes() throws Exception {
        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of());
        Vote v = setUpTestVote(true, true);
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);
        when(voteServiceDAO.updateVotesWithValue(any(), anyBoolean(), any())).thenReturn(List.of(v));

        Election accessElection = new Election();
        accessElection.setElectionType(ElectionType.DATA_ACCESS.getValue());
        accessElection.setStatus(ElectionStatus.OPEN.getValue());
        Election rpElection = new Election();
        rpElection.setElectionType(ElectionType.RP.getValue());
        rpElection.setStatus(ElectionStatus.OPEN.getValue());
        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(accessElection, rpElection));

        initService();

        try {
            service.updateVotesWithValue(List.of(v), true, "rationale");
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }

    private void testUpdateVotesWithValue_RPElectionWithStatus(ElectionStatus status) throws Exception {
        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of());
        Vote v = setUpTestVote(true, true);
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);
        when(voteServiceDAO.updateVotesWithValue(any(), anyBoolean(), any())).thenReturn(List.of(v));

        Election rpElection = new Election();
        rpElection.setElectionType(ElectionType.RP.getValue());
        rpElection.setStatus(status.getValue());
        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(rpElection));

        initService();

        try {
            service.updateVotesWithValue(List.of(v), true, "rationale");
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }


    @Test
    public void testUpdateRationaleByVoteIds() {
        doNothing().when(voteDAO).updateRationaleByVoteIds(any(), any());
        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of());
        Vote v = setUpTestVote(true, true);
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);
        initService();

        try {
            service.updateRationaleByVoteIds(List.of(1), "rationale");
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    public void testUpdateRationaleByVoteIds_DataAccessAndRPElections() {
        doNothing().when(voteDAO).updateRationaleByVoteIds(any(), any());
        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of());
        Vote v = setUpTestVote(true, true);
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);

        Election accessElection = new Election();
        accessElection.setElectionType(ElectionType.DATA_ACCESS.getValue());
        accessElection.setStatus(ElectionStatus.OPEN.getValue());
        Election rpElection = new Election();
        rpElection.setElectionType(ElectionType.RP.getValue());
        rpElection.setStatus(ElectionStatus.OPEN.getValue());
        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(accessElection, rpElection));

        initService();

        try {
            service.updateRationaleByVoteIds(List.of(1), "rationale");
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    public void testUpdateRationaleByVoteIds_NonOpenDataAccessElection() {
        doNothing().when(voteDAO).updateRationaleByVoteIds(any(), any());
        Vote v = setUpTestVote(true, true);
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);

        Election election = new Election();
        election.setElectionType(ElectionType.DATA_ACCESS.getValue());
        election.setStatus(ElectionStatus.CLOSED.getValue());
        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(election));
        initService();

        try {
            service.updateRationaleByVoteIds(List.of(1), "rationale");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testUpdateRationaleByVoteIds_NonDataAccessElection() {
        doNothing().when(voteDAO).updateRationaleByVoteIds(any(), any());
        Vote v = setUpTestVote(true, true);
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);

        Election election = new Election();
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        election.setStatus(ElectionStatus.OPEN.getValue());
        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(election));
        initService();

        try {
            service.updateRationaleByVoteIds(List.of(1), "rationale");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testNotifyResearchersOfDarApproval_2Dars_1Collection() throws Exception {
        String referenceId1 = UUID.randomUUID().toString();
        String referenceId2 = UUID.randomUUID().toString();

        Vote v1 = new Vote();
        v1.setVote(true);
        v1.setType(VoteType.FINAL.getValue());
        v1.setElectionId(1);
        v1.setUserId(1);

        Vote v2 = new Vote();
        v2.setVote(true);
        v2.setType(VoteType.FINAL.getValue());
        v2.setElectionId(2);
        v2.setUserId(1);

        Dataset d1 = new Dataset();
        d1.setDataSetId(1);
        d1.setName(RandomStringUtils.random(50, true, false));
        d1.setAlias(1);
        d1.setDataUse(new DataUseBuilder().setGeneralUse(false).setCommercialUse(true).build());

        Dataset d2 = new Dataset();
        d2.setDataSetId(2);
        d2.setName(RandomStringUtils.random(50, true, false));
        d2.setAlias(2);
        d2.setDataUse(new DataUseBuilder().setGeneralUse(false).setHmbResearch(true).build());

        Election e1 = new Election();
        e1.setElectionId(1);
        e1.setReferenceId(referenceId1);
        e1.setElectionType(ElectionType.DATA_ACCESS.getValue());
        e1.setDataSetId(1);

        Election e2 = new Election();
        e2.setElectionId(2);
        e2.setReferenceId(referenceId2);
        e2.setElectionType(ElectionType.DATA_ACCESS.getValue());
        e2.setDataSetId(2);

        DatasetProperty depositorProp = new DatasetProperty();
        depositorProp.setPropertyName("Data Depositor");
        depositorProp.setPropertyValue("depositor@test.com");
        depositorProp.setPropertyType(PropertyType.String);

        DataAccessRequest dar1 = new DataAccessRequest();
        DataAccessRequestData data1 = new DataAccessRequestData();
        dar1.addDatasetId(d1.getDataSetId());
        dar1.setCollectionId(1);
        dar1.setData(data1);
        dar1.setReferenceId(referenceId1);
        d1.setProperties(Set.of(depositorProp));

        DataAccessRequest dar2 = new DataAccessRequest();
        DataAccessRequestData data2 = new DataAccessRequestData();
        dar2.addDatasetId(d2.getDataSetId());
        dar2.setCollectionId(1);
        dar2.setData(data2);
        dar2.setReferenceId(referenceId2);
        d2.setProperties(Set.of(depositorProp));

        DarCollection c = new DarCollection();
        c.setDarCollectionId(1);
        c.addDar(dar1);
        c.addDar(dar2);
        c.setDarCode("DAR-CODE");

        User researcher = new User();
        researcher.setEmail("researcher@test.com");
        researcher.setDisplayName("Researcher");
        researcher.setUserId(1);

        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(e1, e2));
        when(dataAccessRequestDAO.findByReferenceIds(any())).thenReturn(List.of(dar1, dar2));
        when(darCollectionDAO.findDARCollectionByCollectionIds(any())).thenReturn(List.of(c));
        when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(d1, d2));
        when(userDAO.findUserById(any())).thenReturn(researcher);
        spy(emailService);

        initService();
        service.sendDatasetApprovalNotifications(List.of(v1, v2));
        // Since we have 1 collection with different DAR/Datasets, we should be sending 1 email
        verify(emailService, times(1)).sendResearcherDarApproved(any(), any(), anyList(), any());
    }

    @Test
    public void testNotifyResearchersOfDarApproval_2Dars_2Collections() throws Exception {
        String referenceId1 = UUID.randomUUID().toString();
        String referenceId2 = UUID.randomUUID().toString();

        Vote v1 = new Vote();
        v1.setVote(true);
        v1.setType(VoteType.FINAL.getValue());
        v1.setElectionId(1);
        v1.setUserId(1);

        Vote v2 = new Vote();
        v2.setVote(true);
        v2.setType(VoteType.FINAL.getValue());
        v2.setElectionId(2);
        v2.setUserId(1);

        DatasetProperty depositorProp = new DatasetProperty();
        depositorProp.setPropertyName("Data Depositor");
        depositorProp.setPropertyValue("depositor@test.com");
        depositorProp.setPropertyType(PropertyType.String);

        Dataset d1 = new Dataset();
        d1.setDataSetId(1);
        d1.setName(RandomStringUtils.random(50, true, false));
        d1.setAlias(1);
        d1.setDataUse(new DataUseBuilder().setGeneralUse(false).setCommercialUse(true).build());
        d1.setProperties(Set.of(depositorProp));

        Dataset d2 = new Dataset();
        d2.setDataSetId(2);
        d2.setName(RandomStringUtils.random(50, true, false));
        d2.setAlias(2);
        d2.setDataUse(new DataUseBuilder().setGeneralUse(false).setHmbResearch(true).build());
        d2.setProperties(Set.of(depositorProp));

        Election e1 = new Election();
        e1.setElectionId(1);
        e1.setReferenceId(referenceId1);
        e1.setElectionType(ElectionType.DATA_ACCESS.getValue());
        e1.setDataSetId(1);

        Election e2 = new Election();
        e2.setElectionId(2);
        e2.setReferenceId(referenceId2);
        e2.setElectionType(ElectionType.DATA_ACCESS.getValue());
        e2.setDataSetId(2);

        DataAccessRequest dar1 = new DataAccessRequest();
        DataAccessRequestData data1 = new DataAccessRequestData();
        dar1.addDatasetId(d1.getDataSetId());
        dar1.setCollectionId(1);
        dar1.setData(data1);
        dar1.setReferenceId(referenceId1);

        DataAccessRequest dar2 = new DataAccessRequest();
        DataAccessRequestData data2 = new DataAccessRequestData();
        dar2.addDatasetId(d2.getDataSetId());
        dar2.setCollectionId(2);
        dar2.setData(data2);
        dar2.setReferenceId(referenceId2);

        DarCollection c1 = new DarCollection();
        c1.setDarCollectionId(1);
        c1.addDar(dar1);
        c1.setDarCode("DAR-CODE-1");

        DarCollection c2 = new DarCollection();
        c2.setDarCollectionId(2);
        c2.addDar(dar2);
        c2.setDarCode("DAR-CODE-2");

        User researcher = new User();
        researcher.setEmail("researcher@test.com");
        researcher.setDisplayName("Researcher");
        researcher.setUserId(1);

        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(e1, e2));
        when(dataAccessRequestDAO.findByReferenceIds(any())).thenReturn(List.of(dar1, dar2));
        when(darCollectionDAO.findDARCollectionByCollectionIds(any())).thenReturn(List.of(c1, c2));
        when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(d1, d2));
        when(userDAO.findUserById(any())).thenReturn(researcher);
        spy(emailService);

        initService();
        service.sendDatasetApprovalNotifications(List.of(v1, v2));
        // Since we have 2 collections with different DAR/Datasets, we should be sending 2 emails
        verify(emailService, times(2)).sendResearcherDarApproved(any(), any(), anyList(), any());
    }

    @Test
    public void testNotifyResearchersOfDarApproval_FalseVote() throws Exception {
        String referenceId1 = UUID.randomUUID().toString();

        Vote v1 = new Vote();
        v1.setVote(false);
        v1.setType(VoteType.FINAL.getValue());
        v1.setElectionId(1);
        v1.setUserId(1);

        Dataset d1 = new Dataset();
        d1.setDataSetId(1);
        d1.setName(RandomStringUtils.random(50, true, false));
        d1.setAlias(1);
        d1.setDataUse(new DataUseBuilder().setGeneralUse(false).setCommercialUse(true).build());

        Election e1 = new Election();
        e1.setElectionId(1);
        e1.setReferenceId(referenceId1);
        e1.setElectionType(ElectionType.DATA_ACCESS.getValue());
        e1.setDataSetId(1);

        DataAccessRequest dar1 = new DataAccessRequest();
        DataAccessRequestData data1 = new DataAccessRequestData();
        dar1.addDatasetId(d1.getDataSetId());
        dar1.setCollectionId(1);
        dar1.setData(data1);
        dar1.setReferenceId(referenceId1);

        DarCollection c1 = new DarCollection();
        c1.setDarCollectionId(1);
        c1.addDar(dar1);
        c1.setDarCode("DAR-CODE-1");

        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(e1));
        when(dataAccessRequestDAO.findByReferenceIds(any())).thenReturn(List.of(dar1));
        when(darCollectionDAO.findDARCollectionByCollectionIds(any())).thenReturn(List.of(c1));
        when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(d1));
        spy(emailService);

        initService();
        service.sendDatasetApprovalNotifications(List.of(v1));
        // Since we have a false vote, we should not be sending any email
        verify(emailService, times(0)).sendResearcherDarApproved(any(), any(), anyList(), any());
        // Similar check for all DAO calls
        verify(electionDAO, times(0)).findElectionsByIds(any());
        verify(dataAccessRequestDAO, times(0)).findByReferenceIds(any());
        verify(darCollectionDAO, times(0)).findDARCollectionByCollectionIds(any());
        verify(datasetDAO, times(0)).findDatasetsByIdList(any());
    }

    @Test
    public void testNotifyResearchersOfDarApproval_NonFinalVote() throws Exception {
        String referenceId1 = UUID.randomUUID().toString();

        Vote v1 = new Vote();
        v1.setVote(true);
        v1.setType(VoteType.DAC.getValue());
        v1.setElectionId(1);
        v1.setUserId(1);

        Dataset d1 = new Dataset();
        d1.setDataSetId(1);
        d1.setName(RandomStringUtils.random(50, true, false));
        d1.setAlias(1);
        d1.setDataUse(new DataUseBuilder().setGeneralUse(false).setCommercialUse(true).build());

        Election e1 = new Election();
        e1.setElectionId(1);
        e1.setReferenceId(referenceId1);
        e1.setElectionType(ElectionType.DATA_ACCESS.getValue());
        e1.setDataSetId(1);

        DataAccessRequest dar1 = new DataAccessRequest();
        DataAccessRequestData data1 = new DataAccessRequestData();
        dar1.addDatasetId(d1.getDataSetId());
        dar1.setCollectionId(1);
        dar1.setData(data1);
        dar1.setReferenceId(referenceId1);

        DarCollection c1 = new DarCollection();
        c1.setDarCollectionId(1);
        c1.addDar(dar1);
        c1.setDarCode("DAR-CODE-1");

        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(e1));
        when(dataAccessRequestDAO.findByReferenceIds(any())).thenReturn(List.of(dar1));
        when(darCollectionDAO.findDARCollectionByCollectionIds(any())).thenReturn(List.of(c1));
        when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(d1));
        spy(emailService);

        initService();
        service.sendDatasetApprovalNotifications(List.of(v1));
        // Since we have a non-final vote, we should not be sending any email
        verify(emailService, times(0)).sendResearcherDarApproved(any(), any(), anyList(), any());
        // Similar check for all DAO calls
        verify(electionDAO, times(0)).findElectionsByIds(any());
        verify(dataAccessRequestDAO, times(0)).findByReferenceIds(any());
        verify(darCollectionDAO, times(0)).findDARCollectionByCollectionIds(any());
        verify(datasetDAO, times(0)).findDatasetsByIdList(any());
    }

    @Test
    public void testNotifyCustodiansOfApprovedDatasets() {
        User submitter = new User();
        submitter.setEmail("submitter@test.com");
        submitter.setDisplayName("submitter");
        submitter.setUserId(4);

        DatasetProperty depositorProp = new DatasetProperty();
        depositorProp.setPropertyName("Data Depositor");
        depositorProp.setPropertyValue("depositor@test.com");
        depositorProp.setPropertyType(PropertyType.String);

        Dataset d1 = new Dataset();
        d1.setDataSetId(1);
        d1.setName(RandomStringUtils.random(50, true, false));
        d1.setAlias(1);
        d1.setDataUse(new DataUseBuilder().setGeneralUse(false).setCommercialUse(true).build());
        d1.setProperties(Set.of(depositorProp));
        d1.setCreateUserId(submitter.getUserId());

        Dataset d2 = new Dataset();
        d2.setDataSetId(2);
        d2.setName(RandomStringUtils.random(50, true, false));
        d2.setAlias(2);
        d2.setDataUse(new DataUseBuilder().setGeneralUse(false).setHmbResearch(true).build());
        d2.setProperties(Set.of(depositorProp));
        d2.setCreateUserId(submitter.getUserId());

        User researcher = new User();
        researcher.setEmail("researcher@test.com");
        researcher.setDisplayName("Researcher");
        researcher.setUserId(1);

        User depositor = new User();
        depositor.setEmail("depositor@test.com");
        depositor.setDisplayName("depositor");
        depositor.setUserId(2);

        User custodian = new User();
        custodian.setEmail("custodian@test.com");
        custodian.setDisplayName("custodian");
        custodian.setUserId(3);

        when(userDAO.findUserById(submitter.getUserId())).thenReturn(submitter);
        when(userDAO.findUsersByEmailList(List.of(depositor.getEmail()))).thenReturn(List.of(depositor));
        when(datasetAssociationDAO.getDataOwnersOfDataSet(any())).thenReturn(List.of(3));
        when(userDAO.findUsers(List.of(3))).thenReturn(List.of(custodian));
        spy(emailService);

        initService();
        try {
            service.notifyCustodiansOfApprovedDatasets(List.of(d1, d2), researcher, "Dar Code");
            verify(emailService, times(3)).sendDataCustodianApprovalMessage(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
            );
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    public void testNotifyCustodiansOfApprovedDatasetsNoSubmitterOrDepositorOrCustodians() throws Exception {
        User submitterNotFound = new User();
        submitterNotFound.setEmail("submitter@test.com");
        submitterNotFound.setDisplayName("submitter");
        submitterNotFound.setUserId(4);

        DatasetProperty depositorProp = new DatasetProperty();
        depositorProp.setPropertyName("Data Depositor");
        depositorProp.setPropertyValue("depositor@test.com");
        depositorProp.setPropertyType(PropertyType.String);

        Dataset d1 = new Dataset();
        d1.setDataSetId(1);
        d1.setName(RandomStringUtils.random(50, true, false));
        d1.setAlias(1);
        d1.setDataUse(new DataUseBuilder().setGeneralUse(false).setCommercialUse(true).build());
        d1.setProperties(Set.of(depositorProp));
        d1.setCreateUserId(submitterNotFound.getUserId());

        Dataset d2 = new Dataset();
        d2.setDataSetId(2);
        d2.setName(RandomStringUtils.random(50, true, false));
        d2.setAlias(2);
        d2.setDataUse(new DataUseBuilder().setGeneralUse(false).setHmbResearch(true).build());
        d2.setProperties(Set.of(depositorProp));
        d2.setCreateUserId(submitterNotFound.getUserId());

        User researcher = new User();
        researcher.setEmail("researcher@test.com");
        researcher.setDisplayName("Researcher");
        researcher.setUserId(1);

        User depositorNotFound = new User();
        depositorNotFound.setEmail("depositor@test.com");
        depositorNotFound.setDisplayName("depositor");
        depositorNotFound.setUserId(2);

        when(userDAO.findUserById(submitterNotFound.getUserId())).thenReturn(null);
        when(userDAO.findUserByEmail(depositorNotFound.getEmail())).thenReturn(null);
        when(datasetAssociationDAO.getDataOwnersOfDataSet(any())).thenReturn(List.of());
        spy(emailService);

        initService();
        try {
            service.notifyCustodiansOfApprovedDatasets(List.of(d1, d2), researcher, "Dar Code");
            Assertions.fail(
                "service.notifyCustodiansOfApprovedDatasets should fail in this condition");
        } catch (Exception e) {
            verify(emailService, times(0)).sendDataCustodianApprovalMessage(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
            );
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    private void setUpUserAndElectionVotes(UserRoles userRoles) {
        User user = new User();
        user.setUserId(RandomUtils.nextInt(1, 10));
        UserRole chairRole = new UserRole();
        chairRole.setUserId(user.getUserId());
        chairRole.setRoleId(userRoles.getRoleId());
        chairRole.setName(userRoles.getRoleName());
        user.setRoles(Collections.singletonList(chairRole));
        Election e = new Election();
        when(electionDAO.findElectionById(anyInt())).thenReturn(e);
        when(userDAO.findUsersEnabledToVoteByDAC(anyInt())).thenReturn(Collections.singleton(user));
        when(userDAO.findNonDacUsersEnabledToVote()).thenReturn(Collections.singleton(user));
        Vote v = new Vote();
        v.setVoteId(1);
        when(voteDAO.insertVote(anyInt(), anyInt(), any())).thenReturn(v.getVoteId());
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);
    }

    private Vote setUpTestVote(Boolean vote, Boolean reminderSent) {
        Vote v = new Vote();
        v.setVoteId(RandomUtils.nextInt(1, 10));
        v.setUserId(RandomUtils.nextInt(1, 10));
        v.setElectionId(RandomUtils.nextInt(1, 10));
        v.setIsReminderSent(reminderSent);
        v.setVote(vote);
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);

        return v;
    }

}
