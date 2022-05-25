package org.broadinstitute.consent.http.service;

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
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.exceptions.UnknownIdentifierException;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.dao.VoteServiceDAO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.NotFoundException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
    private EmailNotifierService emailNotifierService;
    @Mock
    private UseRestrictionConverter useRestrictionConverter;
    @Mock
    private VoteDAO voteDAO;
    @Mock
    private VoteServiceDAO voteServiceDAO;

    @Before
    public void setUp() {
        openMocks(this);
        doNothings();
    }

    private void doNothings() {
        doNothing().when(voteDAO).updateVote(anyBoolean(), anyString(), any(), anyInt(), anyBoolean(), anyInt(), any(), anyBoolean());
        doNothing().when(voteDAO).deleteVoteById(anyInt());
        doNothing().when(voteDAO).deleteVotes(anyString());
    }

    private void initService() {
        service = new VoteService(userDAO, darCollectionDAO, dataAccessRequestDAO, datasetAssociationDAO, datasetDAO, electionDAO, emailNotifierService, useRestrictionConverter, voteDAO, voteServiceDAO);
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
            Assert.fail("Should not error: " + e.getMessage());
        }
    }

    @Test
    public void testUpdateVote() {
        Vote v = setUpTestVote(false, false);
        initService();

        Vote vote = service.updateVote(v);
        assertNotNull(vote);
    }

    @Test
    public void testUpdateVoteById() {
        Vote v = setUpTestVote(false, false);
        initService();

        Vote vote = service.updateVoteById(v, v.getVoteId());
        assertNotNull(vote);
        assertEquals(v.getVoteId(), vote.getVoteId());
    }

    @Test(expected = NotFoundException.class)
    public void testUpdateVote_InvalidReferenceId() {
        when(voteDAO.checkVoteById("test", 11))
        .thenReturn(null);
        Vote v = setUpTestVote(false, false);
        initService();

        service.updateVote(v, 11, "test");
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
        assertNotNull(vote);
    }

    @Test
    public void testUpdateVotesWithValue() throws Exception {
        initService();

        List<Vote> votes = service.updateVotesWithValue(List.of(), true, "rationale");
        assertNotNull(votes);
        assertTrue(votes.isEmpty());
    }

    @Test
    public void testFindVotesByIds() {
        when(voteDAO.findVotesByIds(any())).thenReturn(List.of(new Vote()));
        initService();
        List<Vote> votes = service.findVotesByIds(List.of(1));
        assertNotNull(votes);
        assertFalse(votes.isEmpty());
    }

    @Test
    public void testFindVotesByIds_emptyList() {
        initService();
        List<Vote> votes = service.findVotesByIds(List.of());
        assertNotNull(votes);
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
        assertEquals(4, votes.size());
    }

    @Test
    public void testMemberCreateVotesDataAccess() {
        setUpUserAndElectionVotes(UserRoles.MEMBER);
        initService();

        List<Vote> votes = service.createVotes(new Election(), ElectionType.DATA_ACCESS, false);
        assertFalse(votes.isEmpty());
        // Should create 1 member vote
        assertEquals(1, votes.size());
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
        assertEquals(3, votes.size());
    }

    @Test
    public void testMemberCreateVotesDataAccessManualReview() {
        setUpUserAndElectionVotes(UserRoles.MEMBER);
        initService();

        List<Vote> votes = service.createVotes(new Election(), ElectionType.DATA_ACCESS, false);
        assertFalse(votes.isEmpty());
        // Should create 1 member vote
        assertEquals(1, votes.size());
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
        assertEquals(2, votes.size());
    }

    @Test
    public void testMemberCreateVotesTranslateDUL() {
        setUpUserAndElectionVotes(UserRoles.MEMBER);
        initService();

        List<Vote> votes = service.createVotes(new Election(), ElectionType.TRANSLATE_DUL, false);
        assertFalse(votes.isEmpty());
        // Should create 1 member vote
        assertEquals(1, votes.size());
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
        assertEquals(2, votes.size());
    }

    @Test
    public void testMemberCreateVotesRP() {
        setUpUserAndElectionVotes(UserRoles.MEMBER);
        initService();

        List<Vote> votes = service.createVotes(new Election(), ElectionType.RP, false);
        assertFalse(votes.isEmpty());
        // Should create 1 member vote
        assertEquals(1, votes.size());
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

    @Test(expected = NotFoundException.class)
    public void testDescribeVotes_InvalidReferenceId() {
        when(voteDAO.findVotesByReferenceId("test"))
                .thenReturn(null);
        initService();
        service.describeVotes("test");
    }

    @Test
    public void testDescribeVotes() {
        Vote v = setUpTestVote(false, false);
        when(voteDAO.findVotesByReferenceId("test"))
                .thenReturn(List.of(v));
        initService();
        List<Vote> votes = service.describeVotes("test");
        assertNotNull(votes);
        assertEquals(1, votes.size());
        assertEquals(v.getVoteId(), votes.get(0).getVoteId());
    }

    @Test(expected = NotFoundException.class)
    public void testFindVoteById_InvalidId() {
        when(voteDAO.findVoteById(any())).thenReturn(null);
        initService();
        service.findVoteById(1);
    }

    @Test
    public void testDescribeVoteById() {
        Vote v = setUpTestVote(false, false);
        initService();

        Vote vote = service.findVoteById(v.getVoteId());
        assertNotNull(vote);
        assertEquals(v.getVoteId(), vote.getVoteId());
    }

    @Test(expected = NotFoundException.class)
    public void testDeleteVote_NotFound() {
        when(voteDAO.checkVoteById(any(), any()))
                .thenReturn(null);
        initService();

        service.deleteVote(1, "test");
    }

    @Test
    public void testDeleteVote() {
        Vote v = setUpTestVote(false, false);
        when(voteDAO.checkVoteById(any(), any()))
                .thenReturn(v.getVoteId());
        initService();

        service.deleteVote(v.getVoteId(), "test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteVotes_NotFound() throws IllegalArgumentException, UnknownIdentifierException {
        when(electionDAO.findElectionsWithFinalVoteByReferenceId(any()))
                .thenReturn(null);
        initService();

        service.deleteVotes("test");
    }

    @Test
    public void testDeleteVotes() throws IllegalArgumentException, UnknownIdentifierException {
        setUpTestVote(false, false);
        when(electionDAO.findElectionsWithFinalVoteByReferenceId(any())).thenReturn(List.of(new Election()));
        initService();

        service.deleteVotes("test");
    }

    @Test(expected = NotFoundException.class)
    public void testDescribeDataOwnerVote_NotFound() {
        when(voteDAO.findVotesByReferenceIdTypeAndUser(any(), any(), any()))
                .thenReturn(null);
        initService();

        service.describeDataOwnerVote("test", 1);
    }

    @Test
    public void testDescribeDataOwnerVote() {
        Vote v = setUpTestVote(false, false);
        when(voteDAO.findVotesByReferenceIdTypeAndUser("test", 1, VoteType.DATA_OWNER.getValue()))
                .thenReturn(v);
        initService();

        Vote vote = service.describeDataOwnerVote("test", 1);
        assertNotNull(vote);
        assertEquals(v.getVoteId(), vote.getVoteId());
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
            fail(e.getMessage());
        }
    }

    @Test
    public void testUpdateVotesWithValue_emptyList() throws Exception {
        when(voteServiceDAO.updateVotesWithValue(any(), anyBoolean(), any())).thenReturn(List.of());
        initService();
        List<Vote> votes = service.updateVotesWithValue(List.of(), true, "rationale");
        assertNotNull(votes);
        assertTrue(votes.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
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

        service.updateVotesWithValue(List.of(v), true, "rationale");
    }


    @Test(expected = IllegalArgumentException.class)
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

        service.updateVotesWithValue(List.of(v), true, "rationale");
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
            fail(e.getMessage());
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
            fail(e.getMessage());
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
            fail(e.getMessage());
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
            fail(e.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateRationaleByVoteIds_NonOpenDataAccessElection() {
        doNothing().when(voteDAO).updateRationaleByVoteIds(any(), any());
        Vote v = setUpTestVote(true, true);
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);

        Election election = new Election();
        election.setElectionType(ElectionType.DATA_ACCESS.getValue());
        election.setStatus(ElectionStatus.CLOSED.getValue());
        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(election));
        initService();

        service.updateRationaleByVoteIds(List.of(1), "rationale");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateRationaleByVoteIds_NonDataAccessElection() {
        doNothing().when(voteDAO).updateRationaleByVoteIds(any(), any());
        Vote v = setUpTestVote(true, true);
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);

        Election election = new Election();
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        election.setStatus(ElectionStatus.OPEN.getValue());
        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(election));
        initService();

        service.updateRationaleByVoteIds(List.of(1), "rationale");
    }

    @Test
    public void testNotifyResearchersOfDarApproval_2Dars_1Collection() throws Exception {
        String referenceId1 = UUID.randomUUID().toString();
        String referenceId2 = UUID.randomUUID().toString();

        Vote v = new Vote();
        v.setVote(true);
        v.setType(VoteType.FINAL.getValue());
        v.setElectionId(1);
        v.setDacUserId(1);

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

        DataAccessRequest dar1 = new DataAccessRequest();
        DataAccessRequestData data1 = new DataAccessRequestData();
        data1.setDatasetIds(List.of(d1.getDataSetId()));
        dar1.setCollectionId(1);
        dar1.setData(data1);
        dar1.setReferenceId(referenceId1);

        DataAccessRequest dar2 = new DataAccessRequest();
        DataAccessRequestData data2 = new DataAccessRequestData();
        data2.setDatasetIds(List.of(d1.getDataSetId()));
        dar2.setCollectionId(1);
        dar2.setData(data2);
        dar2.setReferenceId(referenceId2);

        DarCollection c = new DarCollection();
        c.setDarCollectionId(1);
        c.addDar(dar1);
        c.addDar(dar2);
        c.setDarCode("DAR-CODE");

        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(e1, e2));
        when(dataAccessRequestDAO.findByReferenceIds(any())).thenReturn(List.of(dar1, dar2));
        when(darCollectionDAO.findDARCollectionByCollectionIds(any())).thenReturn(List.of(c));
        when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(d1, d2));
        spy(emailNotifierService);

        initService();
        service.notifyResearchersOfDarApproval(List.of(v));
        // Since we have 1 collection with different DAR/Datasets, we should be sending 1 email
        verify(emailNotifierService, times(1)).sendResearcherDarApproved(any(), any(), anyList(), any());
    }

    @Test
    public void testNotifyResearchersOfDarApproval_2Dars_2Collections() throws Exception {
        String referenceId1 = UUID.randomUUID().toString();
        String referenceId2 = UUID.randomUUID().toString();

        Vote v = new Vote();
        v.setVote(true);
        v.setType(VoteType.FINAL.getValue());
        v.setElectionId(1);
        v.setDacUserId(1);

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

        DataAccessRequest dar1 = new DataAccessRequest();
        DataAccessRequestData data1 = new DataAccessRequestData();
        data1.setDatasetIds(List.of(d1.getDataSetId()));
        dar1.setCollectionId(1);
        dar1.setData(data1);
        dar1.setReferenceId(referenceId1);

        DataAccessRequest dar2 = new DataAccessRequest();
        DataAccessRequestData data2 = new DataAccessRequestData();
        data2.setDatasetIds(List.of(d1.getDataSetId()));
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

        when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(e1, e2));
        when(dataAccessRequestDAO.findByReferenceIds(any())).thenReturn(List.of(dar1, dar2));
        when(darCollectionDAO.findDARCollectionByCollectionIds(any())).thenReturn(List.of(c1, c2));
        when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(d1, d2));
        spy(emailNotifierService);

        initService();
        service.notifyResearchersOfDarApproval(List.of(v));
        // Since we have 2 collections with different DAR/Datasets, we should be sending 2 emails
        verify(emailNotifierService, times(2)).sendResearcherDarApproved(any(), any(), anyList(), any());
    }

    private void setUpUserAndElectionVotes(UserRoles userRoles) {
        User user = new User();
        user.setDacUserId(RandomUtils.nextInt(1, 10));
        UserRole chairRole = new UserRole();
        chairRole.setUserId(user.getDacUserId());
        chairRole.setRoleId(userRoles.getRoleId());
        chairRole.setName(userRoles.getRoleName());
        user.setRoles(Collections.singletonList(chairRole));
        Election e = new Election();
        when(electionDAO.findElectionById(anyInt())).thenReturn(e);
        when(datasetDAO.findDatasetAndDacIds()).thenReturn(Collections.emptyList());
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
        v.setDacUserId(RandomUtils.nextInt(1, 10));
        v.setElectionId(RandomUtils.nextInt(1, 10));
        v.setIsReminderSent(reminderSent);
        v.setVote(vote);
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);

        return v;
    }

}
