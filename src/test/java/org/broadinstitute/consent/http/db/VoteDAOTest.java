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
import java.util.Date;
import java.util.List;

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
        Assert.assertNotNull(vote);
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
