package org.broadinstitute.consent.http.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.DatasetAssociationDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class VoteServiceTest {

    private VoteService service;

    @Mock
    UserDAO userDAO;
    @Mock
    DatasetAssociationDAO dataSetAssociationDAO;
    @Mock
    DatasetDAO datasetDAO;
    @Mock
    ElectionDAO electionDAO;
    @Mock
    VoteDAO voteDAO;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doNothings();
    }

    private void doNothings() {
        doNothing().when(voteDAO).updateVote(anyBoolean(), anyString(), any(), anyInt(), anyBoolean(), anyInt(), any(), anyBoolean());
        doNothing().when(voteDAO).deleteVoteById(anyInt());
        doNothing().when(voteDAO).deleteVotes(anyString());
    }

    private void initService() {
        service = new VoteService(userDAO, dataSetAssociationDAO, electionDAO, voteDAO);
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

        Vote vote = service.updateVote(v, 11, "test");
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
        when(dataSetAssociationDAO.getDataOwnersOfDataSet(anyInt())).thenReturn(Collections.singletonList(1));
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
        List<Vote> votes = service.describeVotes("test");
    }

    @Test
    public void testDescribeVotes() {
        Vote v = setUpTestVote(false, false);
        when(voteDAO.findVotesByReferenceId("test"))
                .thenReturn(Arrays.asList(v));
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
        Vote v = setUpTestVote(false, false);
        when(electionDAO.findElectionsWithFinalVoteByReferenceId(any()))
                .thenReturn(Arrays.asList(new Election()));
        initService();

        service.deleteVotes("test");
    }

    @Test(expected = NotFoundException.class)
    public void testDescribeDataOwnerVote_NotFound() {
        when(voteDAO.findVotesByReferenceIdTypeAndUser(any(), any(), any()))
                .thenReturn(null);
        initService();

        Vote vote = service.describeDataOwnerVote("test", 1);
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
