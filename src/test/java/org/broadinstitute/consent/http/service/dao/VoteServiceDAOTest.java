package org.broadinstitute.consent.http.service.dao;

import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VoteServiceDAOTest extends DAOTestHelper {

  private VoteServiceDAO serviceDAO;

  private void initService() {
    serviceDAO = new VoteServiceDAO(jdbi, voteDAO);
  }

  @Test
  public void testUpdateVotesWithValue_FinalVote() throws Exception {
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
    Election foundElection = electionDAO.findElectionById(vote.getElectionId());
    assertEquals(ElectionStatus.CLOSED.getValue(), foundElection.getStatus());
    assertEquals(vote.getVoteId(), votes.get(0).getVoteId());
  }

  @Test
  public void testUpdateVotesWithValue_NoRationale() throws Exception {
    User user = createUser();
    DataAccessRequest dar = createDataAccessRequestV3();
    DataSet dataset = createDataset();
    Election election = createAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    Vote vote = createFinalVote(user.getDacUserId(), election.getElectionId());
    initService();

    List<Vote> votes = serviceDAO.updateVotesWithValue(List.of(vote), true, null);
    assertNotNull(votes);
    assertFalse(votes.isEmpty());
    assertTrue(votes.get(0).getVote());
    assertNull(votes.get(0).getRationale());
    assertEquals(vote.getVoteId(), votes.get(0).getVoteId());
  }

  @Test
  public void testUpdateVotesWithValue_DacVote() throws Exception {
    User user = createUser();
    DataAccessRequest dar = createDataAccessRequestV3();
    DataSet dataset = createDataset();
    Election election = createAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());
    String rationale = "rationale";
    initService();

    List<Vote> votes = serviceDAO.updateVotesWithValue(List.of(vote), true, rationale);
    assertNotNull(votes);
    assertFalse(votes.isEmpty());
    assertTrue(votes.get(0).getVote());
    assertEquals(rationale, votes.get(0).getRationale());
    Election foundElection = electionDAO.findElectionById(vote.getElectionId());
    assertNotEquals(ElectionStatus.CLOSED.getValue(), foundElection.getStatus());
    assertEquals(vote.getVoteId(), votes.get(0).getVoteId());
  }

  @Test
  public void testUpdateVotesWithValue_MultipleVotes() throws Exception {
    User user = createUser();
    DataAccessRequest dar = createDataAccessRequestV3();
    DataSet dataset = createDataset();
    Election election = createAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    Vote vote1 = createDacVote(user.getDacUserId(), election.getElectionId());
    Vote vote2 = createDacVote(user.getDacUserId(), election.getElectionId());
    Vote vote3 = createDacVote(user.getDacUserId(), election.getElectionId());
    String rationale = "rationale";
    initService();

    List<Vote> votes = serviceDAO.updateVotesWithValue(List.of(vote1, vote2, vote3), true, rationale);

    assertNotNull(votes);
    assertFalse(votes.isEmpty());
    List<Integer> requestVoteIds = Stream.of(vote1, vote2, vote3)
            .map(Vote::getVoteId)
            .collect(Collectors.toList());
    votes.forEach(v -> {
      assertTrue(v.getVote());
      assertEquals(rationale, v.getRationale());
      assertTrue(requestVoteIds.contains(v.getVoteId()));
    });
  }


  @Test
  public void testUpdateVotesWithValue_MultipleElectionTypes() throws Exception {
    User user = createUser();
    DataAccessRequest dar = createDataAccessRequestV3();
    DataSet dataset = createDataset();
    Election rpElection1 = createRPElection(dar.getReferenceId(), dataset.getDataSetId());
    Election rpElection2 = createRPElection(dar.getReferenceId(), dataset.getDataSetId());
    Election accessElection = createAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    changeElectionStatus(rpElection1, ElectionStatus.CLOSED);
    Vote vote1 = createDacVote(user.getDacUserId(), rpElection1.getElectionId());
    Vote vote2 = createDacVote(user.getDacUserId(), rpElection2.getElectionId());
    Vote vote3 = createDacVote(user.getDacUserId(), accessElection.getElectionId());
    String rationale = "rationale";
    initService();

    List<Vote> votes = serviceDAO.updateVotesWithValue(List.of(vote1, vote2, vote3), true, rationale);

    assertNotNull(votes);
    assertFalse(votes.isEmpty());
    List<Integer> requestVoteIds = Stream.of(vote1, vote2, vote3)
            .map(Vote::getVoteId)
            .collect(Collectors.toList());
    votes.forEach(v -> {
      assertTrue(v.getVote());
      assertEquals(rationale, v.getRationale());
      assertTrue(requestVoteIds.contains(v.getVoteId()));
    });
  }
}
