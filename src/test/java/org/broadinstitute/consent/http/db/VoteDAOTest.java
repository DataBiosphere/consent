package org.broadinstitute.consent.http.db;

import com.google.common.io.Resources;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.AbstractTest;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

public class VoteDAOTest extends AbstractTest {

    @SuppressWarnings("UnstableApiUsage")
    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, Resources.getResource("consent-config.yml").getFile());

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    private final int USER_ID = 1;
    private ElectionDAO electionDAO;
    private VoteDAO voteDAO;

    @Before
    public void setUp() {
        electionDAO = getApplicationJdbi().onDemand(ElectionDAO.class);
        voteDAO = getApplicationJdbi().onDemand(VoteDAO.class);
    }

    @Test
    public void testCreateVote() {
        Election election = createElection();
        String voteType = VoteType.DAC.getValue();
        int voteId = voteDAO.insertVote(USER_ID, election.getElectionId(), voteType);
        Vote vote = voteDAO.findVoteById(voteId);
        Assert.assertNotNull(vote);
        Assert.assertNull(vote.getVote());
    }

    @Test
    public void testUpdateVote() {
        Election election = createElection();
        String voteType = VoteType.DAC.getValue();
        int voteId = voteDAO.insertVote(USER_ID, election.getElectionId(), voteType);

        String rationale = "rationale";
        Date now = new Date();
        voteDAO.updateVote(true, rationale, now, voteId, true,
                election.getElectionId(), now, true);
        Vote vote = voteDAO.findVoteById(voteId);
        System.out.println(vote);
        Assert.assertTrue(vote.getVote());
        Assert.assertTrue(vote.getHasConcerns());
        Assert.assertTrue(vote.getIsReminderSent());
        Assert.assertEquals(vote.getRationale(), rationale);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Assert.assertEquals(sdf.format(vote.getCreateDate()), sdf.format(now));
        Assert.assertEquals(sdf.format(vote.getUpdateDate()), sdf.format(now));
    }

    private Election createElection() {
        String electionType = ElectionType.DATA_ACCESS.getValue();
        String status = ElectionStatus.OPEN.getValue();
        String referenceId = RandomStringUtils.random(10);
        int datasetId = 1;
        int electionId = electionDAO.insertElection(electionType, status, new Date(), referenceId, datasetId);
        return electionDAO.findElectionById(electionId);
    }

}
