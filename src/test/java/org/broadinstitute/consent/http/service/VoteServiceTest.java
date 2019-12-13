package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class VoteServiceTest {

    private VoteService service;

    @Mock
    DACUserDAO dacUserDAO;
    @Mock
    DataSetAssociationDAO dataSetAssociationDAO;
    @Mock
    DataSetDAO datasetDAO;
    @Mock
    ElectionDAO electionDAO;
    @Mock
    VoteDAO voteDAO;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initService() {
        service = new VoteService(dacUserDAO, dataSetAssociationDAO, datasetDAO, electionDAO, voteDAO);
    }

    @Test
    public void testFindVotesByReferenceId() {
        when(voteDAO.findVotesByReferenceId(any())).thenReturn(Collections.emptyList());
        initService();

        Collection<Vote> votes = service.findVotesByReferenceId(UUID.randomUUID().toString());
        Assert.assertTrue(votes.isEmpty());
    }

    @Test
    public void testAdvanceVotes() {
        doNothing().when(voteDAO).updateVote(anyBoolean(), anyString(), any(), anyInt(), anyBoolean(), anyInt(), any(), anyBoolean());
        Vote v = new Vote();
        v.setVoteId(RandomUtils.nextInt(1, 10));
        v.setDacUserId(RandomUtils.nextInt(1, 10));
        v.setElectionId(RandomUtils.nextInt(1, 10));
        v.setIsReminderSent(false);
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);
        initService();

        try {
            service.advanceVotes(Collections.singletonList(v), true, "New Rationale");
        } catch (Exception e) {
            Assert.fail("Should not error: " + e.getMessage());
        }
    }

    @Test
    public void testUpdateVote() {
        doNothing().when(voteDAO).updateVote(anyBoolean(), anyString(), any(), anyInt(), anyBoolean(), anyInt(), any(), anyBoolean());
        Vote v = new Vote();
        v.setVoteId(RandomUtils.nextInt(1, 10));
        v.setDacUserId(RandomUtils.nextInt(1, 10));
        v.setElectionId(RandomUtils.nextInt(1, 10));
        v.setIsReminderSent(false);
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);
        initService();

        Vote vote = service.updateVote(v);
        Assert.assertNotNull(vote);
    }

    @Test
    public void testChairCreateVotesDataAccess() {
        setUpUserAndElectionVotes(UserRoles.CHAIRPERSON);
        initService();

        List<Vote> votes = service.createVotes(1, ElectionType.DATA_ACCESS, false);
        Assert.assertFalse(votes.isEmpty());
        // Should create 4 votes:
        // Chairperson as a chair
        // Chairperson as a dac member
        // Final vote
        // Manual review Agreement vote
        Assert.assertEquals(4, votes.size());
    }

    @Test
    public void testMemberCreateVotesDataAccess() {
        setUpUserAndElectionVotes(UserRoles.MEMBER);
        initService();

        List<Vote> votes = service.createVotes(1, ElectionType.DATA_ACCESS, false);
        Assert.assertFalse(votes.isEmpty());
        // Should create 1 member vote:
        Assert.assertEquals(1, votes.size());
    }

    @Test
    public void testChairCreateVotesDataAccessManualReview() {
        setUpUserAndElectionVotes(UserRoles.CHAIRPERSON);
        initService();

        List<Vote> votes = service.createVotes(1, ElectionType.DATA_ACCESS, true);
        Assert.assertFalse(votes.isEmpty());
        // Should create 3 votes:
        // Chairperson as a chair
        // Chairperson as a dac member
        // Final vote
        Assert.assertEquals(3, votes.size());
    }

    @Test
    public void testMemberCreateVotesDataAccessManualReview() {
        setUpUserAndElectionVotes(UserRoles.MEMBER);
        initService();

        List<Vote> votes = service.createVotes(1, ElectionType.DATA_ACCESS, false);
        Assert.assertFalse(votes.isEmpty());
        // Should create 1 member vote:
        Assert.assertEquals(1, votes.size());
    }

    @Test
    public void testChairCreateVotesTranslateDUL() {
        setUpUserAndElectionVotes(UserRoles.CHAIRPERSON);
        initService();

        List<Vote> votes = service.createVotes(1, ElectionType.TRANSLATE_DUL, false);
        Assert.assertFalse(votes.isEmpty());
        // Should create 2 votes:
        // Chairperson as a chair
        // Chairperson as a dac member
        Assert.assertEquals(2, votes.size());
    }

    @Test
    public void testMemberCreateVotesTranslateDUL() {
        setUpUserAndElectionVotes(UserRoles.MEMBER);
        initService();

        List<Vote> votes = service.createVotes(1, ElectionType.TRANSLATE_DUL, false);
        Assert.assertFalse(votes.isEmpty());
        // Should create 1 member vote:
        Assert.assertEquals(1, votes.size());
    }

    @Test
    public void testChairCreateVotesRP() {
        setUpUserAndElectionVotes(UserRoles.CHAIRPERSON);
        initService();

        List<Vote> votes = service.createVotes(1, ElectionType.RP, false);
        Assert.assertFalse(votes.isEmpty());
        // Should create 2 votes:
        // Chairperson as a chair
        // Chairperson as a dac member
        Assert.assertEquals(2, votes.size());
    }

    @Test
    public void testMemberCreateVotesRP() {
        setUpUserAndElectionVotes(UserRoles.MEMBER);
        initService();

        List<Vote> votes = service.createVotes(1, ElectionType.RP, false);
        Assert.assertFalse(votes.isEmpty());
        // Should create 1 member vote:
        Assert.assertEquals(1, votes.size());
    }

    @Test
    public void testCreateVotesForElections() {
        // No-op ... tested in all of the create vote tests
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
        Assert.assertFalse(votes.isEmpty());
    }

    private void setUpUserAndElectionVotes(UserRoles userRoles) {
        DACUser user = new DACUser();
        user.setDacUserId(RandomUtils.nextInt(1, 10));
        UserRole chairRole = new UserRole();
        chairRole.setUserId(user.getDacUserId());
        chairRole.setRoleId(userRoles.getRoleId());
        chairRole.setName(userRoles.getRoleName());
        user.setRoles(Collections.singletonList(chairRole));
        Election e = new Election();
        when(electionDAO.findElectionById(anyInt())).thenReturn(e);
        when(datasetDAO.findDatasetAndDacIds()).thenReturn(Collections.emptyList());
        when(dacUserDAO.findDACUsersEnabledToVoteByDAC(anyInt())).thenReturn(Collections.singleton(user));
        when(dacUserDAO.findNonDACUsersEnabledToVote()).thenReturn(Collections.singleton(user));
        Vote v = new Vote();
        v.setVoteId(1);
        when(voteDAO.insertVote(anyInt(), anyInt(), any())).thenReturn(v.getVoteId());
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);
    }

}
