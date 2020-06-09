package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.NotFoundException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class VoteServiceTest {

    private VoteService service;

    @Mock
    UserDAO userDAO;
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

    /**
     * Test the case where no final vote exists.
     */
    @Test(expected = NotFoundException.class)
    public void testDescribeFinalAccessVoteByElectionId_NotFound() {
        when(voteDAO.findFinalVotesByElectionId(any())).thenReturn(Collections.emptyList());
        initService();
        service.describeFinalAccessVoteByElectionId(1);
    }

    /**
     * Test the case where a single final vote exists.
     */
    @Test
    public void testDescribeFinalAccessVoteByElectionId_Case_1() {
        Vote vote = new Vote();
        vote.setVoteId(1);
        when(voteDAO.findFinalVotesByElectionId(any())).thenReturn(Collections.singletonList(vote));
        initService();
        Vote foundVote = service.describeFinalAccessVoteByElectionId(1);
        assertNotNull(foundVote);
        assertEquals(vote.getVoteId(), foundVote.getVoteId());
    }

    /**
     * Test the case where multiple final votes exist, but only one has a vote.
     * Similar to case 3
     */
    @Test
    public void testDescribeFinalAccessVoteByElectionId_Case_2() {
        Vote v1 = new Vote();
        Vote v2 = new Vote();
        v1.setVoteId(1);
        v1.setVote(true);
        v2.setVoteId(2);
        when(voteDAO.findFinalVotesByElectionId(any())).thenReturn(Arrays.asList(v1, v2));
        initService();
        Vote foundVote = service.describeFinalAccessVoteByElectionId(1);
        assertNotNull(foundVote);
        assertEquals(v1.getVoteId(), foundVote.getVoteId());
    }

    /**
     * Test the case where multiple final votes exist, but a different one has a vote.
     * Similar to case 2
     */
    @Test
    public void testDescribeFinalAccessVoteByElectionId_Case_3() {
        Vote v1 = new Vote();
        Vote v2 = new Vote();
        v1.setVoteId(1);
        v2.setVoteId(2);
        v2.setVote(true);
        when(voteDAO.findFinalVotesByElectionId(any())).thenReturn(Arrays.asList(v1, v2));
        initService();
        Vote foundVote = service.describeFinalAccessVoteByElectionId(1);
        assertNotNull(foundVote);
        assertEquals(v2.getVoteId(), foundVote.getVoteId());
    }

    /**
     * Test the case where multiple final votes exist, each with a vote, but with different update dates
     * Similar to case 5
     */
    @Test
    public void testDescribeFinalAccessVoteByElectionId_Case_4() {
        LocalDate local = LocalDate.now();
        ZoneId defaultZoneId = ZoneId.systemDefault();
        Date now = Date.from(local.atStartOfDay(defaultZoneId).toInstant());
        Date yesterday = Date.from(local.minusDays(1).atStartOfDay(defaultZoneId).toInstant());
        Vote v1 = new Vote();
        v1.setVoteId(1);
        v1.setUpdateDate(now);
        v1.setVote(false);
        Vote v2 = new Vote();
        v2.setVoteId(2);
        v2.setUpdateDate(yesterday);
        v2.setVote(true);
        when(voteDAO.findFinalVotesByElectionId(any())).thenReturn(Arrays.asList(v1, v2));
        initService();
        Vote foundVote = service.describeFinalAccessVoteByElectionId(1);
        assertNotNull(foundVote);
        assertEquals(v1.getVoteId(), foundVote.getVoteId());
    }

    /**
     * Test the case where multiple final votes exist, each with a vote, but with different update dates
     * Similar to case 4
     */
    @Test
    public void testDescribeFinalAccessVoteByElectionId_Case_5() {
        LocalDate local = LocalDate.now();
        ZoneId defaultZoneId = ZoneId.systemDefault();
        Date now = Date.from(local.atStartOfDay(defaultZoneId).toInstant());
        Date yesterday = Date.from(local.minusDays(1).atStartOfDay(defaultZoneId).toInstant());
        Vote v1 = new Vote();
        v1.setVoteId(1);
        v1.setUpdateDate(yesterday);
        v1.setVote(false);
        Vote v2 = new Vote();
        v2.setVoteId(2);
        v2.setUpdateDate(now);
        v2.setVote(true);
        when(voteDAO.findFinalVotesByElectionId(any())).thenReturn(Arrays.asList(v1, v2));
        initService();
        Vote foundVote = service.describeFinalAccessVoteByElectionId(1);
        assertNotNull(foundVote);
        assertEquals(v2.getVoteId(), foundVote.getVoteId());
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
        when(userDAO.findDACUsersEnabledToVoteByDAC(anyInt())).thenReturn(Collections.singleton(user));
        when(userDAO.findNonDACUsersEnabledToVote()).thenReturn(Collections.singleton(user));
        Vote v = new Vote();
        v.setVoteId(1);
        when(voteDAO.insertVote(anyInt(), anyInt(), any())).thenReturn(v.getVoteId());
        when(voteDAO.findVoteById(anyInt())).thenReturn(v);
    }

}
