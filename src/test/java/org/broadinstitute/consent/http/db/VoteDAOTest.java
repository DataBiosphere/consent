package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Assert;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

public class VoteDAOTest extends DAOTestFramework {

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
