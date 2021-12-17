package org.broadinstitute.consent.http.service.dao;

import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class VoteServiceDAOTest extends DAOTestHelper {
    
    private VoteServiceDAO serviceDAO;
    
    private void initService() {
        serviceDAO = new VoteServiceDAO(electionDAO, jdbi, voteDAO);
    }
    
    @Test
    public void testUpdateVotesWithValue() throws Exception {
        User user = createUser();
        DataAccessRequest dar = createDataAccessRequestV3();
        DataSet dataset = createDataset();
        Election election = createAccessElection(dar.getReferenceId(), dataset.getDataSetId());
        Vote vote = createFinalVote(user.getDacUserId(), election.getElectionId());
        String rationale = "rationale";
        initService();

        List<Vote> votes = serviceDAO.updateVotesWithValue(List.of(vote), true, rationale);
        assertNotNull(votes);
        assertFalse(votes.isEmpty());
        assertTrue(votes.get(0).getVote());
        assertEquals(rationale, votes.get(0).getRationale());
    }

    @Test
    public void testUpdateVotesWithValue_emptyList() throws Exception {
        initService();
        List<Vote> votes = serviceDAO.updateVotesWithValue(List.of(), true, "rationale");
        assertNotNull(votes);
        assertTrue(votes.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateVotesWithValue_closedElection() throws Exception {
        User user = createUser();
        DataAccessRequest dar = createDataAccessRequestV3();
        DataSet dataset = createDataset();
        Election election = createCancelledAccessElection(dar.getReferenceId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());
        initService();

        serviceDAO.updateVotesWithValue(List.of(vote), true, "rationale");
    }

}
