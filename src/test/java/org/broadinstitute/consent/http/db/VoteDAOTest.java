package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.ElectionReviewVote;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Assert;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class VoteDAOTest extends DAOTestHelper {

    @Test
    public void testFindVotesByReferenceId() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        List<Vote> votes = voteDAO.findVotesByReferenceId(election.getReferenceId());
        Assert.assertFalse(votes.isEmpty());
        Assert.assertEquals(vote.getVoteId(), votes.get(0).getVoteId());
    }

    @Test
    public void testFindElectionReviewVotesByElectionId() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        createDacVote(user.getDacUserId(), election.getElectionId());

        List<ElectionReviewVote> votes = voteDAO.findElectionReviewVotesByElectionId(election.getElectionId());
        Assert.assertFalse(votes.isEmpty());
        Assert.assertEquals(user.getEmail(), votes.get(0).getEmail());
    }

    @Test
    public void testFindElectionReviewVotesByElectionIdAndType() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        List<ElectionReviewVote> votes = voteDAO.findElectionReviewVotesByElectionId(election.getElectionId(), vote.getType());
        Assert.assertFalse(votes.isEmpty());
        Assert.assertEquals(user.getEmail(), votes.get(0).getEmail());

        List<ElectionReviewVote> votes2 = voteDAO.findElectionReviewVotesByElectionId(election.getElectionId(), vote.getType().toUpperCase());
        Assert.assertFalse(votes2.isEmpty());
        Assert.assertEquals(user.getEmail(), votes2.get(0).getEmail());

        List<ElectionReviewVote> votes3 = voteDAO.findElectionReviewVotesByElectionId(election.getElectionId(), vote.getType().toLowerCase());
        Assert.assertFalse(votes3.isEmpty());
        Assert.assertEquals(user.getEmail(), votes3.get(0).getEmail());
    }

    @Test
    public void testFindVoteById() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        Vote foundVote = voteDAO.findVoteById(vote.getVoteId());
        assertNotNull(foundVote);
    }

    @Test
    public void testFindDACVotesByElectionId() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        List<Vote> foundVotes = voteDAO.findDACVotesByElectionId(election.getElectionId());
        assertNotNull(foundVotes);
        assertFalse(foundVotes.isEmpty());
        assertEquals(1, foundVotes.size());
        assertEquals(vote.getVoteId(), foundVotes.get(0).getVoteId());
    }

    @Test
    public void testFindVotesByElectionIds() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        createDacVote(user.getDacUserId(), election.getElectionId());

        Consent consent2 = createConsent(null);
        DataSet dataset2 = createDataset();
        Election election2 = createElection(consent2.getConsentId(), dataset2.getDataSetId());
        createDacVote(user.getDacUserId(), election2.getElectionId());
        List<Integer> electionIds = Arrays.asList(election.getElectionId(), election2.getElectionId());

        List<Vote> foundVotes = voteDAO.findVotesByElectionIds(electionIds);
        assertNotNull(foundVotes);
        assertFalse(foundVotes.isEmpty());
        assertEquals(2, foundVotes.size());
    }

    @Test
    public void testFindVotesByTypeAndElectionIds() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        Consent consent2 = createConsent(null);
        DataSet dataset2 = createDataset();
        Election election2 = createElection(consent2.getConsentId(), dataset2.getDataSetId());
        createDacVote(user.getDacUserId(), election2.getElectionId());
        List<Integer> electionIds = Arrays.asList(election.getElectionId(), election2.getElectionId());

        List<Vote> foundVotes = voteDAO.findVotesByTypeAndElectionIds(electionIds, vote.getType());
        assertNotNull(foundVotes);
        assertFalse(foundVotes.isEmpty());
        assertEquals(2, foundVotes.size());

        List<Vote> foundVotes2 = voteDAO.findVotesByTypeAndElectionIds(electionIds, vote.getType().toLowerCase());
        assertNotNull(foundVotes2);
        assertFalse(foundVotes2.isEmpty());
        assertEquals(2, foundVotes2.size());

        List<Vote> foundVotes3 = voteDAO.findVotesByTypeAndElectionIds(electionIds, vote.getType().toUpperCase());
        assertNotNull(foundVotes3);
        assertFalse(foundVotes3.isEmpty());
        assertEquals(2, foundVotes3.size());
    }

    @Test
    public void testFindVotesByElectionIdAndType() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        List<Vote> foundVotes = voteDAO.findVotesByElectionIdAndType(election.getElectionId(), vote.getType());
        assertNotNull(foundVotes);
        assertFalse(foundVotes.isEmpty());
        assertEquals(1, foundVotes.size());

        List<Vote> foundVotes2 = voteDAO.findVotesByElectionIdAndType(election.getElectionId(), vote.getType().toLowerCase());
        assertNotNull(foundVotes2);
        assertFalse(foundVotes2.isEmpty());
        assertEquals(1, foundVotes2.size());

        List<Vote> foundVotes3 = voteDAO.findVotesByElectionIdAndType(election.getElectionId(), vote.getType().toUpperCase());
        assertNotNull(foundVotes3);
        assertFalse(foundVotes3.isEmpty());
        assertEquals(1, foundVotes3.size());
    }

    @Test
    public void testFindPendingVotesByElectionId() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        List<Vote> foundVotes = voteDAO.findPendingVotesByElectionId(election.getElectionId());
        assertNotNull(foundVotes);
        assertFalse(foundVotes.isEmpty());
        assertEquals(1, foundVotes.size());
        assertEquals(vote.getVoteId(), foundVotes.get(0).getVoteId());
    }

    @Test
    public void testFindVoteByElectionIdAndDACUserId() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        Vote foundVote = voteDAO.findVoteByElectionIdAndDACUserId(election.getElectionId(), user.getDacUserId());
        assertNotNull(foundVote);
        assertEquals(vote.getVoteId(), foundVote.getVoteId());
    }

    @Test
    public void testFindVotesByElectionIdAndDACUserIds() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        List<Vote> foundVotes = voteDAO.findVotesByElectionIdAndDACUserIds(election.getElectionId(), Collections.singletonList(user.getDacUserId()));
        assertNotNull(foundVotes);
        assertFalse(foundVotes.isEmpty());
        assertEquals(vote.getVoteId(), foundVotes.get(0).getVoteId());
    }

    @Test
    public void testCreateVote() {
        DACUser user = createUser();
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote v = createDacVote(user.getDacUserId(), election.getElectionId());

        Vote vote = voteDAO.findVoteById(v.getVoteId());
        assertNotNull(vote);
        Assert.assertNull(vote.getVote());
    }

    @Test
    public void testUpdateVote() {
        DACUser user = createUser();
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote v = createDacVote(user.getDacUserId(), election.getElectionId());

        String rationale = "rationale";
        Date now = new Date();
        voteDAO.updateVote(true, rationale, now, v.getVoteId(), true,
                election.getElectionId(), now, true);
        Vote vote = voteDAO.findVoteById(v.getVoteId());
        Assert.assertTrue(vote.getVote());
        Assert.assertTrue(vote.getHasConcerns());
        Assert.assertTrue(vote.getIsReminderSent());
        Assert.assertEquals(vote.getRationale(), rationale);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Assert.assertEquals(sdf.format(vote.getCreateDate()), sdf.format(now));
        Assert.assertEquals(sdf.format(vote.getUpdateDate()), sdf.format(now));
    }

    @Test
    public void testDeleteVotes() {
        // No-op ... tested by `tearDown()`
    }

}
