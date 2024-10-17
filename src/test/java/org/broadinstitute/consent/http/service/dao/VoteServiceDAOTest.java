package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VoteServiceDAOTest extends DAOTestHelper {

  private VoteServiceDAO serviceDAO;

  @BeforeEach
  void initService() {
    serviceDAO = new VoteServiceDAO(jdbi, voteDAO);
  }

  @Test
  void testUpdateVotesWithValue_FinalVote() throws Exception {
    User user = createUser();
    DataAccessRequest dar = createDataAccessRequestV3();
    Dataset dataset = createDataset();
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDatasetId());
    Vote vote = createFinalVote(user.getUserId(), election.getElectionId());
    String rationale = "rationale";

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
  void testUpdateVotesWithValue_NoRationale() throws Exception {
    User user = createUser();
    DataAccessRequest dar = createDataAccessRequestV3();
    Dataset dataset = createDataset();
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDatasetId());
    Vote vote = createFinalVote(user.getUserId(), election.getElectionId());

    List<Vote> votes = serviceDAO.updateVotesWithValue(List.of(vote), true, null);
    assertNotNull(votes);
    assertFalse(votes.isEmpty());
    assertTrue(votes.get(0).getVote());
    assertNull(votes.get(0).getRationale());
    assertEquals(vote.getVoteId(), votes.get(0).getVoteId());
  }

  @Test
  void testUpdateVotesWithValue_DacVote() throws Exception {
    User user = createUser();
    DataAccessRequest dar = createDataAccessRequestV3();
    Dataset dataset = createDataset();
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDatasetId());
    Vote vote = createDacVote(user.getUserId(), election.getElectionId());
    String rationale = "rationale";

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
  void testUpdateVotesWithValue_MultipleVotes() throws Exception {
    User user = createUser();
    DataAccessRequest dar = createDataAccessRequestV3();
    Dataset dataset = createDataset();
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDatasetId());
    Vote vote1 = createDacVote(user.getUserId(), election.getElectionId());
    Vote vote2 = createDacVote(user.getUserId(), election.getElectionId());
    Vote vote3 = createDacVote(user.getUserId(), election.getElectionId());
    String rationale = "rationale";

    List<Vote> votes = serviceDAO.updateVotesWithValue(List.of(vote1, vote2, vote3), true,
        rationale);

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
  void testUpdateVotesWithValue_MultipleElectionTypes() throws Exception {
    User user = createUser();
    DataAccessRequest dar = createDataAccessRequestV3();
    Dataset dataset = createDataset();
    Election rpElection1 = createRPElection(dar.getReferenceId(), dataset.getDatasetId());
    Election rpElection2 = createRPElection(dar.getReferenceId(), dataset.getDatasetId());
    Election accessElection = createDataAccessElection(dar.getReferenceId(),
        dataset.getDatasetId());
    electionDAO.updateElectionById(
        rpElection1.getElectionId(),
        ElectionStatus.CLOSED.getValue(),
        new Date());

    Vote vote1 = createDacVote(user.getUserId(), rpElection1.getElectionId());
    Vote vote2 = createDacVote(user.getUserId(), rpElection2.getElectionId());
    Vote vote3 = createDacVote(user.getUserId(), accessElection.getElectionId());
    String rationale = "rationale";

    List<Vote> votes = serviceDAO.updateVotesWithValue(List.of(vote1, vote2, vote3), true,
        rationale);

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

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + RandomStringUtils.random(20, true, true);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), null);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private void createDatasetProperties(Integer datasetId) {
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDatasetId(datasetId);
    dsp.setPropertyKey(1);
    dsp.setPropertyValue("Test_PropertyValue");
    dsp.setCreateDate(new Date());
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
  }

  private Vote createDacVote(Integer userId, Integer electionId) {
    Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.DAC.getValue());
    return voteDAO.findVoteById(voteId);
  }

  private Vote createFinalVote(Integer userId, Integer electionId) {
    Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.FINAL.getValue());
    return voteDAO.findVoteById(voteId);
  }

  private Election createRPElection(String referenceId, Integer datasetId) {
    Integer electionId = electionDAO.insertElection(
        ElectionType.RP.getValue(),
        ElectionStatus.OPEN.getValue(),
        new Date(),
        referenceId,
        datasetId
    );
    return electionDAO.findElectionById(electionId);
  }

  private Election createDataAccessElection(String referenceId, Integer datasetId) {
    Integer electionId = electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(),
        new Date(),
        referenceId,
        datasetId
    );
    return electionDAO.findElectionById(electionId);
  }

}
