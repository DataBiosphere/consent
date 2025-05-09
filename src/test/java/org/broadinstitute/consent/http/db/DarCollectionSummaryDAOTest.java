package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DarCollectionSummaryDAOTest extends DAOTestHelper {

  private DataAccessRequest createDataAccessRequest(Integer collectionId, Integer userId) {
    String referenceId = UUID.randomUUID().toString();
    Date createDate = new Date();
    Date submissionDate = new Date();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle(RandomStringUtils.randomAlphabetic(20));
    data.setStatus("test");
    dataAccessRequestDAO.insertDataAccessRequest(collectionId, referenceId, userId, createDate,
        new Date(), submissionDate, new Date(), data, randomAlphabetic(10));
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private DataAccessRequest createProgressReportFromDAR(DataAccessRequest dar) {
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertProgressReport(
        dar.getId(),
        dar.getCollectionId(),
        referenceId,
        dar.getUserId(),
        dar.getData());
    DataAccessRequest progressReport = dataAccessRequestDAO.findByReferenceId(referenceId);
    dar.getDatasetIds().forEach(datasetId -> {
      dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);
    });
    return progressReport;
  }

  private Integer createDarCollection(Integer createUserId) {
    String darCode = RandomStringUtils.randomAlphabetic(20);
    return darCollectionDAO.insertDarCollection(darCode, createUserId, new Date());
  }

  private Dataset createDataset(Integer userId) {
    Integer datasetId = datasetDAO.insertDataset(RandomStringUtils.randomAlphabetic(20),
        new Timestamp(System.currentTimeMillis()), userId, null,
        new DataUseBuilder().setGeneralUse(true).build().toString(), null);
    return datasetDAO.findDatasetById(datasetId);
  }

  private Dataset createDatasetWithDac(Integer userId, Integer dacId) {
    Integer datasetId = datasetDAO.insertDataset(RandomStringUtils.randomAlphabetic(20),
        new Timestamp(System.currentTimeMillis()), userId, null,
        new DataUseBuilder().setGeneralUse(true).build().toString(), dacId);
    return datasetDAO.findDatasetById(datasetId);
  }

  private Dac createDac() {
    Integer id = dacDAO.createDac(
        "Test_" + RandomStringUtils.random(20, true, true),
        "Test_" + RandomStringUtils.random(20, true, true),
        new Date());
    return dacDAO.findById(id);
  }

  private Election createElection(ElectionType type, String status, String referenceId,
      Integer datasetId) {
    Integer electionId = electionDAO.insertElection(type.getValue(), status, new Date(), referenceId,
        datasetId);
    return electionDAO.findElectionById(electionId);
  }

  private Vote createVote(Integer dacUserId, Integer electionId, String type) {
    Integer voteId = voteDAO.insertVote(dacUserId, electionId, type);
    return voteDAO.findVoteById(voteId);
  }


  private Election createElectionWithVotes(Integer userId, String darReferenceId, Integer datasetId) {
    User userTwo = createUser();
    Integer userTwoId = userTwo.getUserId();
    User userChair = createUser();
    Integer userChairId = userChair.getUserId();
    Election election = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.OPEN.getValue(), darReferenceId, datasetId);
    Integer electionId = election.getElectionId();
    Vote voteOne = createVote(userId, electionId, VoteType.DAC.getValue());
    Vote voteTwo = createVote(userTwoId, electionId, VoteType.DAC.getValue());
    Vote voteThree = createVote(userChairId, electionId, VoteType.DAC.getValue());
    Vote voteFinal = createVote(userChairId, electionId, VoteType.FINAL.getValue());
    election.setVotes(Map.of(
        voteOne.getVoteId(), voteOne,
        voteTwo.getVoteId(), voteTwo,
        voteThree.getVoteId(), voteThree,
        voteFinal.getVoteId(), voteFinal
    ));
    return election;
  }

  @Test
  void testGetDarCollectionSummaryForDAC() {
    User userOne = createUser();
    User userTwo = createUser();
    User userChair = createUser();
    Integer userOneId = userOne.getUserId();
    Integer userTwoId = userTwo.getUserId();
    Integer userChairId = userChair.getUserId();

    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Dataset excludedDataset = createDataset(
        userOneId); //represents dataset that does not fall under user DAC's purview
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    Integer excludedDarCollectionId = createDarCollection(userOneId);
    DataAccessRequest excludedDar = createDataAccessRequest(excludedDarCollectionId, userOneId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest darTwo = createDataAccessRequest(collectionTwoId, userTwoId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(darTwo.getReferenceId(),
        datasetTwo.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(excludedDar.getReferenceId(),
        excludedDataset.getDatasetId());

    Election collectionOnePrevElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.CLOSED.getValue(),
        darOne.getReferenceId(),
        dataset.getDatasetId()); //non-latest dataset, need to make sure this isn't pulled into query results
    Election collectionOneElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.OPEN.getValue(), darOne.getReferenceId(), dataset.getDatasetId());
    Integer collectionOneElectionId = collectionOneElection.getElectionId();
    Integer collectionOnePrevElectionId = collectionOnePrevElection.getElectionId();
    Election excludedElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.CLOSED.getValue(), //tied to excluded dataset, it should not be pulled in
        excludedDar.getReferenceId(), excludedDataset.getDatasetId());
    Election collectionTwoElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.OPEN.getValue(),
        darTwo.getReferenceId(), datasetTwo.getDatasetId());
    Integer collectionTwoElectionId = collectionTwoElection.getElectionId();
    Integer excludedElectionId = excludedElection.getElectionId();

    //create old votes to ensure that they don't get pulled in by the query
    createVote(userOneId, collectionOnePrevElectionId, VoteType.DAC.getValue());
    createVote(userTwoId, collectionOnePrevElectionId, VoteType.DAC.getValue());
    createVote(userChairId, collectionOnePrevElectionId, VoteType.DAC.getValue());
    createVote(userChairId, collectionOnePrevElectionId, VoteType.CHAIRPERSON.getValue());

    //create votes for dataset that should NOT be pulled by the query (tied to exluded dataset)
    createVote(userOneId, excludedElectionId, VoteType.DAC.getValue());

    Vote collectionOneVoteOne = createVote(userOneId, collectionOneElectionId,
        VoteType.DAC.getValue());
    Vote collectionOneVoteTwo = createVote(userTwoId, collectionOneElectionId,
        VoteType.DAC.getValue());
    Vote collectionOneVoteThree = createVote(userChairId, collectionOneElectionId,
        VoteType.DAC.getValue());
    Vote collectionOneVoteChair = createVote(userChairId, collectionOneElectionId,
        VoteType.CHAIRPERSON.getValue());

    Vote collectionTwoVoteOne = createVote(userOneId, collectionTwoElectionId,
        VoteType.DAC.getValue());
    Vote collectionTwoVoteTwo = createVote(userTwoId, collectionTwoElectionId,
        VoteType.DAC.getValue());
    Vote collectionTwoVoteThree = createVote(userChairId, collectionTwoElectionId,
        VoteType.DAC.getValue());
    Vote collectionTwoVoteChair = createVote(userChairId, collectionTwoElectionId,
        VoteType.CHAIRPERSON.getValue());

    List<Integer> targetDatasets = List.of(dataset.getDatasetId(), datasetTwo.getDatasetId());
    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForDAC(
        userChairId, targetDatasets);

    assertNotNull(summaries);
    assertEquals(2, summaries.size());
    summaries.forEach((s) -> {
      assertEquals(1, s.getDatasetIds().size());
      s.getDatasetIds().stream()
          .forEach((id) -> assertTrue(targetDatasets.contains(id)));

      List<Integer> targetVotes;
      Integer electionId;

      if (s.getDarCollectionId() == collectionOneId) {
        targetVotes = List.of(collectionOneVoteChair.getVoteId(),
            collectionOneVoteThree.getVoteId());
        electionId = collectionOneElection.getElectionId();
      } else {
        targetVotes = List.of(collectionTwoVoteChair.getVoteId(),
            collectionTwoVoteThree.getVoteId());
        electionId = collectionTwoElection.getElectionId();
      }
      s.getElections().entrySet().stream()
          .forEach((e) -> {
            assertEquals(electionId, e.getKey());
          });
      s.getVotes().forEach((v) -> {
        assertTrue(targetVotes.contains(v.getVoteId()));
      });
      assertEquals(1, s.getDatasetCount());
    });
  }

  @Test
  void testGetDarCollectionSummaryForDAC_NoElectionsPresent() {
    User userOne = createUser();
    User userChair = createUser();
    Integer userOneId = userOne.getUserId();
    Integer userChairId = userChair.getUserId();

    Dataset dataset = createDataset(userOneId);
    Dataset excludedDataset = createDataset(userOneId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer excludedDarCollectionId = createDarCollection(userOneId);
    DataAccessRequest excludedDar = createDataAccessRequest(excludedDarCollectionId, userOneId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(excludedDar.getReferenceId(),
        excludedDataset.getDatasetId());

    Election excludedElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.CLOSED.getValue(),
        excludedDar.getReferenceId(), excludedDataset.getDatasetId());
    Integer excludedElectionId = excludedElection.getElectionId();

    // create votes for dataset that should NOT be pulled by the query
    createVote(userOneId, excludedElectionId, VoteType.DAC.getValue());

    List<Integer> targetDatasets = List.of(dataset.getDatasetId());
    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForDAC(
        userChairId,
        targetDatasets);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    summaries.forEach((s) -> {
      assertEquals(1, s.getDatasetIds().size());
      s.getDatasetIds()
          .forEach((id) -> assertTrue(targetDatasets.contains(id)));
      assertFalse(s.isExpired());
      assertTrue(s.getExpiresAt().after(new Date()));
      assertEquals(0, s.getElections().size());
      assertEquals(0, s.getVotes().size());
      assertEquals(1, s.getDatasetCount());
    });
  }

  @Test
  void testGetDarCollectionSummaryForDAC_ArchivedCollection() {
    User userOne = createUser();
    Integer userOneId = userOne.getUserId();

    Dataset dataset = createDataset(userOneId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer archivedCollectionId = createDarCollection(userOneId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest archivedDar = createDataAccessRequest(archivedCollectionId, userOneId);
    dataAccessRequestDAO.archiveByReferenceIds(List.of(archivedDar.getReferenceId()));

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(archivedDar.getReferenceId(),
        dataset.getDatasetId());

    List<Integer> targetDatasets = List.of(dataset.getDatasetId());
    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForDAC(
        userOneId, targetDatasets);

    //test that only the non-archived collection was pulled by the query
    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    assertEquals(collectionOneId, summaries.get(0).getDarCollectionId());
  }

  @Test
  void testGetDarCollectionSummaryForSO() {

    User userOne = createUserWithInstitution();
    Integer userOneId = userOne.getUserId();
    Integer userTwoId = createUserWithInstitution().getUserId();

    // query should only pull in collections that were created by users with this instituion_id
    Integer institutionId = getUserInstitution(userOne).getId();
    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest darTwo = createDataAccessRequest(collectionTwoId, userTwoId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(darTwo.getReferenceId(),
        datasetTwo.getDatasetId());

    Election collectionOnePrevElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.CLOSED.getValue(),
        darOne.getReferenceId(), dataset.getDatasetId());
    Election collectionOneElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.OPEN.getValue(),
        darOne.getReferenceId(), dataset.getDatasetId());
    Integer collectionOneElectionId = collectionOneElection.getElectionId();
    Integer collectionOnePrevElectionId = collectionOnePrevElection.getElectionId();
    Election collectionTwoElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.OPEN.getValue(),
        darTwo.getReferenceId(), datasetTwo.getDatasetId());
    Integer collectionTwoElectionId = collectionTwoElection.getElectionId();

    List<Integer> targetDatasets = List.of(dataset.getDatasetId());
    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForSO(
        institutionId);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    summaries.forEach((s) -> {
      assertEquals(1, s.getDatasetIds().size());
      s.getDatasetIds().forEach(id -> assertTrue(targetDatasets.contains(id)));

      Integer electionId = collectionOneElection.getElectionId();
      s.getElections().forEach((key, value) -> assertEquals(electionId, key));
      assertEquals(1, s.getDatasetCount());
    });
  }

  @Test
  void testGetDarCollectionSummaryForSO_NoElectionsPresent() {
    User userOne = createUserWithInstitution();
    Integer userOneId = userOne.getUserId();

    Integer institutionId = getUserInstitution(userOne).getId();
    Dataset dataset = createDataset(userOneId);
    Integer collectionOneId = createDarCollection(userOneId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());

    List<Integer> targetDatasets = List.of(dataset.getDatasetId());
    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForSO(
        institutionId);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    summaries.forEach((s) -> {
      assertEquals(1, s.getDatasetIds().size());
      s.getDatasetIds().forEach(id -> assertTrue(targetDatasets.contains(id)));

      assertEquals(0, s.getElections().size());
      assertEquals(1, s.getDatasetCount());
    });
  }

  @Test
  void testGetDarCollectionSummaryForSO_ArchivedCollection() {
    User userOne = createUserWithInstitution();
    Integer userOneId = userOne.getUserId();

    Integer institutionId = getUserInstitution(userOne).getId();
    Dataset dataset = createDataset(userOneId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer archivedCollectionId = createDarCollection(userOneId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest archivedDar = createDataAccessRequest(archivedCollectionId, userOneId);
    dataAccessRequestDAO.archiveByReferenceIds(List.of(archivedDar.getReferenceId()));

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(archivedDar.getReferenceId(),
        dataset.getDatasetId());

    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForSO(
        institutionId);

    //test that only the non-archived collection was pulled by the query
    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    assertEquals(collectionOneId, summaries.get(0).getDarCollectionId());
  }

  @Test
  void testGetDarCollectionSummaryForResearcher() {
    // query should only pull in collection made by this user
    Integer userOneId = createUserWithInstitution().getUserId();
    Integer userTwoId = createUserWithInstitution().getUserId();

    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest darTwo = createDataAccessRequest(collectionTwoId, userTwoId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(darTwo.getReferenceId(),
        datasetTwo.getDatasetId());

    Election collectionOnePrevElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.CLOSED.getValue(),
        darOne.getReferenceId(), dataset.getDatasetId());
    Election collectionOneElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.OPEN.getValue(),
        darOne.getReferenceId(), dataset.getDatasetId());
    Integer collectionOneElectionId = collectionOneElection.getElectionId();
    Integer collectionOnePrevElectionId = collectionOnePrevElection.getElectionId();
    Election collectionTwoElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.OPEN.getValue(),
        darTwo.getReferenceId(), datasetTwo.getDatasetId());
    Integer collectionTwoElectionId = collectionTwoElection.getElectionId();

    List<Integer> targetDatasets = List.of(dataset.getDatasetId());
    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(
        userOneId);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    summaries.forEach((s) -> {
      assertEquals(1, s.getDatasetIds().size());
      s.getDatasetIds()
          .forEach(id -> assertTrue(targetDatasets.contains(id)));

      Integer electionId = collectionOneElection.getElectionId();
      s.getElections().forEach((key, value) -> assertEquals(electionId, key));
      assertEquals(1, s.getDarStatuses().size());
      s.getDarStatuses().values().forEach(status -> assertEquals("test", status));
      assertEquals(1, s.getDatasetCount());
    });
  }

  @Test
  void testGetDarCollectionSummaryForResearcher_NoElectionsPresent() {

    // query should only pull collections made by this usera
    Integer userOneId = createUserWithInstitution().getUserId();
    Integer userTwoId = createUserWithInstitution().getUserId();

    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest darTwo = createDataAccessRequest(collectionTwoId, userTwoId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(darTwo.getReferenceId(),
        datasetTwo.getDatasetId());

    Election collectionTwoElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.OPEN.getValue(),
        darTwo.getReferenceId(), datasetTwo.getDatasetId());
    Integer collectionTwoElectionId = collectionTwoElection.getElectionId();

    List<Integer> targetDatasets = List.of(dataset.getDatasetId());
    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(
        userOneId);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    summaries.forEach((s) -> {
      assertEquals(1, s.getDatasetIds().size());
      s.getDatasetIds()
          .forEach((id) -> assertTrue(targetDatasets.contains(id)));
      assertEquals(0, s.getElections().size());
      assertEquals(1, s.getDatasetCount());
    });
  }

  @Test
  void testGetDarCollectionSummaryForResearcher_ArchivedCollection() {
    Integer userOneId = createUserWithInstitution().getUserId();

    Dataset dataset = createDataset(userOneId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer archivedCollectionId = createDarCollection(userOneId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest archivedDar = createDataAccessRequest(archivedCollectionId, userOneId);
    dataAccessRequestDAO.archiveByReferenceIds(List.of(archivedDar.getReferenceId()));

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(archivedDar.getReferenceId(),
        dataset.getDatasetId());

    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(
        userOneId);

    //test that only the non-archived collection was pulled by the query
    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    assertEquals(collectionOneId, summaries.get(0).getDarCollectionId());
  }

  @Test
  void testGetDarCollectionSummaryForResearcher_DraftedDarCollection() {
    // query should only pull collections made by this user
    Integer userId = createUserWithInstitution().getUserId();

    Dataset dataset = createDataset(userId);
    Integer collectionId = createDarCollection(userId);
    DataAccessRequest dar = createDataAccessRequest(collectionId, userId);

    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.updateDataByReferenceId(dar.getReferenceId(), dar.userId, new Date(), null,
        new Date(), dar.getData(), randomAlphabetic(10)); // draft DAR

    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(
        userId);

    assertEquals(0, summaries.size());

  }

  @Test
  void testGetDarCollectionSummaryForAdmin() {

    User userOne = createUser();
    User userTwo = createUser();
    Dac dacOne = createDac();
    Dac dacTwo = createDac();
    Integer dacOneId = dacOne.getDacId();
    Integer dacTwoId = dacTwo.getDacId();
    String dacOneName = dacOne.getName();
    String dacTwoName = dacTwo.getName();
    Integer userOneId = userOne.getUserId();
    Integer userTwoId = userTwo.getUserId();
    Dataset dataset = createDatasetWithDac(userOneId, dacOneId);
    Dataset datasetTwo = createDatasetWithDac(userTwoId, dacTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest darTwo = createDataAccessRequest(collectionTwoId, userTwoId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(darTwo.getReferenceId(),
        datasetTwo.getDatasetId());

    Election collectionOnePrevElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.CLOSED.getValue(),
        darOne.getReferenceId(), dataset.getDatasetId());
    Election collectionOneElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.OPEN.getValue(),
        darOne.getReferenceId(), dataset.getDatasetId());
    Integer collectionOneElectionId = collectionOneElection.getElectionId();
    Integer collectionOnePrevElectionId = collectionOnePrevElection.getElectionId();
    Election collectionTwoElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.OPEN.getValue(),
        darTwo.getReferenceId(), datasetTwo.getDatasetId());
    Integer collectionTwoElectionId = collectionTwoElection.getElectionId();

    List<Integer> targetDatasets = List.of(dataset.getDatasetId(), datasetTwo.getDatasetId());
    List<String> targetDatasetDacNames = List.of(dacOneName, dacTwoName);
    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForAdmin();
    assertNotNull(summaries);
    assertEquals(2, summaries.size());
    summaries.forEach((s) -> {
      assertEquals(1, s.getDatasetIds().size());
      s.getDatasetIds()
          .forEach((id) -> assertTrue(targetDatasets.contains(id)));

      assertEquals(1, s.getDacNames().size());
      s.getDacNames()
          .forEach((dacId) -> assertTrue(targetDatasetDacNames.contains(dacId)));

      Integer electionId;

      if (Objects.equals(s.getDarCollectionId(), collectionOneId)) {
        electionId = collectionOneElection.getElectionId();
      } else {
        electionId = collectionTwoElection.getElectionId();
      }

      s.getElections().forEach((key, value) -> assertEquals(electionId, key));
      assertEquals(1, s.getDatasetCount());
    });
  }

  @Test
  void testGetDarCollectionSummaryForAdmin_NoPresentElections() {

    User userOne = createUser();
    User userTwo = createUser();
    Integer userOneId = userOne.getUserId();
    Integer userTwoId = userTwo.getUserId();
    Dac dacOne = createDac();
    Dac dacTwo = createDac();
    Integer dacOneId = dacOne.getDacId();
    Integer dacTwoId = dacTwo.getDacId();
    String dacOneName = dacOne.getName();
    String dacTwoName = dacTwo.getName();
    Dataset dataset = createDatasetWithDac(userOneId, dacOneId);
    Dataset datasetTwo = createDatasetWithDac(userTwoId, dacTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest darTwo = createDataAccessRequest(collectionTwoId, userTwoId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(darTwo.getReferenceId(),
        datasetTwo.getDatasetId());

    List<Integer> targetDatasets = List.of(dataset.getDatasetId(), datasetTwo.getDatasetId());
    List<String> targetDatasetDacNames = List.of(dacOneName, dacTwoName);
    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForAdmin();
    assertNotNull(summaries);
    assertEquals(2, summaries.size());
    summaries.forEach((s) -> {
      assertEquals(1, s.getDatasetIds().size());
      s.getDatasetIds().stream()
          .forEach((id) -> assertTrue(targetDatasets.contains(id)));

      assertEquals(1, s.getDacNames().size());
      s.getDacNames()
          .forEach((dacId) -> assertTrue(targetDatasetDacNames.contains(dacId)));

      s.getDarStatuses().values()
          .forEach((st) -> assertTrue(st.equalsIgnoreCase("test")));
      assertEquals(0, s.getElections().size());
      assertEquals(1, s.getDatasetCount());
    });
  }

  @Test
  void testGetDarCollectionSummaryForAdmin_ArchivedCollection() {
    User userOne = createUser();
    Integer userOneId = userOne.getUserId();

    Dataset dataset = createDataset(userOneId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer archivedCollectionId = createDarCollection(userOneId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest archivedDar = createDataAccessRequest(archivedCollectionId, userOneId);
    dataAccessRequestDAO.archiveByReferenceIds(List.of(archivedDar.getReferenceId()));

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(archivedDar.getReferenceId(),
        dataset.getDatasetId());

    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForAdmin();

    //test that only the non-archived collection was pulled by the query
    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    assertEquals(collectionOneId, summaries.get(0).getDarCollectionId());
  }

  @ParameterizedTest
  @ValueSource(strings= {"admin", "researcher", "SO", "DAC", "dacCollectionId", "collectionId"})
  void testGetDarCollectionSummaryArchivedNotIncluded(String type) {
    User user = createUserWithInstitution();
    Integer userId = user.getUserId();

    Dataset dataset = createDataset(userId);

    Integer collectionId = createDarCollection(userId);

    // Create two DataAccessRequests in the same collection
    DataAccessRequest olderDar = createDataAccessRequest(collectionId, userId);
    DataAccessRequest archivedDar = createProgressReportFromDAR(olderDar);

    // Insert dataset relations for all DARs
    dataAccessRequestDAO.insertDARDatasetRelation(olderDar.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(archivedDar.getReferenceId(), dataset.getDatasetId());

    // archived DAR
    dataAccessRequestDAO.archiveByReferenceIds(List.of(archivedDar.getReferenceId()));

    List<DarCollectionSummary> summaries = switch (type) {
      case "admin" -> darCollectionSummaryDAO.getDarCollectionSummariesForAdmin();
      case "researcher" -> darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(userId);
      case "SO" -> darCollectionSummaryDAO.getDarCollectionSummariesForSO(user.getInstitutionId());
      case "DAC" -> darCollectionSummaryDAO.getDarCollectionSummariesForDAC(userId, List.of(dataset.getDatasetId()));
      case "dacCollectionId" -> List.of(darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(userId, List.of(dataset.getDatasetId()), collectionId));
      case "collectionId" -> List.of(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId));
      default -> throw new IllegalArgumentException("Invalid type: " + type);
    };

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    DarCollectionSummary summary = summaries.get(0);
    assertEquals(collectionId, summary.getDarCollectionId());

    // Ensure the reference IDs include only the non-draft non-archived DAR
    assertNotNull(summary.getReferenceIds());
    assertEquals(1, summary.getReferenceIds().size());
    assertTrue(summary.getReferenceIds().contains(olderDar.getReferenceId()));

    // Ensure the summary represents the older DAR
    assertEquals(olderDar.getSubmissionDate(), summary.getSubmissionDate());
    assertEquals(olderDar.getExpiresAt(), summary.getExpiresAt());
  }

  @ParameterizedTest
  @ValueSource(strings= {"admin", "researcher", "SO", "DAC", "dacCollectionId", "collectionId"})
  void testGetDarCollectionSummaryTwoDataAccessRequests(String type) {
    User user = createUserWithInstitution();
    Integer userId = user.getUserId();

    Dataset dataset = createDataset(userId);
    Dataset dataset1 = createDataset(userId);

    Integer collectionId = createDarCollection(userId);

    // Create two DataAccessRequests in the same collection
    DataAccessRequest olderDar = createDataAccessRequest(collectionId, userId);
    DataAccessRequest newerDar = createProgressReportFromDAR(olderDar);

    // Insert dataset relations for both DARs
    // the older DAR has two datasets and the newer DAR has one
    dataAccessRequestDAO.insertDARDatasetRelation(olderDar.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(olderDar.getReferenceId(), dataset1.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(newerDar.getReferenceId(), dataset.getDatasetId());

    // Create an election for the older DAR
    createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.OPEN.getValue(),
        olderDar.getReferenceId(), dataset.getDatasetId());

    List<DarCollectionSummary> summaries = switch (type) {
      case "admin" -> darCollectionSummaryDAO.getDarCollectionSummariesForAdmin();
      case "researcher" ->
          darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(userId);
      case "SO" -> darCollectionSummaryDAO.getDarCollectionSummariesForSO(user.getInstitutionId());
      case "DAC" -> darCollectionSummaryDAO.getDarCollectionSummariesForDAC(userId, List.of(dataset.getDatasetId(), dataset1.getDatasetId()));
      case "dacCollectionId" -> List.of(darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(userId, List.of(dataset.getDatasetId(), dataset1.getDatasetId()), collectionId));
      case "collectionId" -> List.of(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId));
      default -> throw new IllegalArgumentException("Invalid type: " + type);
    };
    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    DarCollectionSummary summary = summaries.get(0);
    assertEquals(collectionId, summary.getDarCollectionId());

    // Ensure the reference IDs include both DARs
    assertNotNull(summary.getReferenceIds());
    assertEquals(2, summary.getReferenceIds().size());
    assertTrue(summary.getReferenceIds().containsAll(List.of(olderDar.getReferenceId(), newerDar.getReferenceId())));

    // Ensure the election from the older DAR is not included
    assertTrue(summary.getElections().isEmpty());

    // Ensure the summary represents the most recently submitted DAR
    assertEquals(newerDar.getSubmissionDate(), summary.getSubmissionDate());
    assertEquals(newerDar.getExpiresAt(), summary.getExpiresAt());
    // should only be one dataset because the newer DAR has one dataset
    assertEquals(1, summary.getDatasetIds().size());
    assertTrue(summary.getDatasetIds().contains(dataset.getDatasetId()));
  }

  @ParameterizedTest
  @ValueSource(strings= {"DatasetIds", "CollectionId"})
  void testGetDarCollectionSummaryTwoDataAccessRequestsForDACsWithVotes(String type) {
    User user = createUserWithInstitution();
    Integer userId = user.getUserId();

    Dataset dataset = createDataset(userId);
    Integer collectionId = createDarCollection(userId);

    // Create two DataAccessRequests in the same collection
    DataAccessRequest olderDar = createDataAccessRequest(collectionId, userId);
    DataAccessRequest newerDar = createProgressReportFromDAR(olderDar);

    // Insert dataset relations for both DARs
    dataAccessRequestDAO.insertDARDatasetRelation(olderDar.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(newerDar.getReferenceId(), dataset.getDatasetId());

    // Create an election for the new DAR
    Election expectedElection = createElectionWithVotes(userId,
        newerDar.getReferenceId(), dataset.getDatasetId());

    // Create an election for the older DAR with old votes and make sure they are not included
    createElectionWithVotes(userId,
        olderDar.getReferenceId(), dataset.getDatasetId());

    List<DarCollectionSummary> summaries = switch (type) {
      case "DatasetIds" -> darCollectionSummaryDAO.getDarCollectionSummariesForDAC(userId, List.of(dataset.getDatasetId()));
      case "CollectionId" -> List.of(darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(userId, List.of(dataset.getDatasetId()), collectionId));
      default -> throw new IllegalArgumentException("Invalid type: " + type);
    };

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    DarCollectionSummary summary = summaries.get(0);
    assertEquals(collectionId, summary.getDarCollectionId());

    // Ensure the new election and its votes are included
    assertEquals(1, summary.getElections().size());
    assertTrue(summary.getElections().containsKey(expectedElection.getElectionId()));
    assertEquals(2, summary.getVotes().size());
    // votes from expected election that are from the specified user or are the final vote
    List<Integer> expectedVotes = expectedElection.getVotes().values().stream()
        .filter(v -> v.getUserId().equals(userId) || v.getType().equals(VoteType.FINAL.getValue()))
        .map(Vote::getVoteId)
        .toList();
   summary.getVotes().forEach((v) -> assertTrue(expectedVotes.contains(v.getVoteId())));
  }

  @Test
  void testGetDarCollectionSummaryByCollectionId() {
    User userOne = createUser();
    User userTwo = createUser();
    Integer userOneId = userOne.getUserId();
    Integer userTwoId = userTwo.getUserId();

    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest darTwo = createDataAccessRequest(collectionTwoId, userTwoId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(darTwo.getReferenceId(),
        datasetTwo.getDatasetId());

    Election collectionOnePrevElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.CLOSED.getValue(),
        darOne.getReferenceId(), dataset.getDatasetId());
    Election collectionOneElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.OPEN.getValue(),
        darOne.getReferenceId(), dataset.getDatasetId());
    Integer collectionOneElectionId = collectionOneElection.getElectionId();
    Integer collectionOnePrevElectionId = collectionOnePrevElection.getElectionId();
    Election collectionTwoElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.OPEN.getValue(),
        darTwo.getReferenceId(), datasetTwo.getDatasetId());
    Integer collectionTwoElectionId = collectionTwoElection.getElectionId();

    List<Integer> targetDatasets = List.of(dataset.getDatasetId());
    DarCollectionSummary summary = darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(
        collectionOneId);

    assertNotNull(summary);
    assertEquals(collectionOneId, summary.getDarCollectionId());
    assertEquals(1, summary.getDatasetIds().size());
    summary.getDatasetIds()
        .forEach((id) -> assertTrue(targetDatasets.contains(id)));

    Integer electionId = collectionOneElection.getElectionId();
    summary.getElections().entrySet()
        .forEach((e) -> assertEquals(electionId, e.getKey()));
    assertEquals(1, summary.getDarStatuses().size());
    summary.getDarStatuses().values().forEach(status -> assertEquals("test", status));
    assertEquals(1, summary.getDatasetCount());
  }

  @Test
  void testGetDarCollectionSummaryByCollectionId_NoElectionsPresent() {
    User userOne = createUser();
    User userTwo = createUser();
    Integer userOneId = userOne.getUserId();
    Integer userTwoId = userTwo.getUserId();

    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest darTwo = createDataAccessRequest(collectionTwoId, userTwoId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(darTwo.getReferenceId(),
        datasetTwo.getDatasetId());

    Election collectionTwoElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.OPEN.getValue(),
        darTwo.getReferenceId(), datasetTwo.getDatasetId());
    Integer collectionTwoElectionId = collectionTwoElection.getElectionId();

    List<Integer> targetDatasets = List.of(dataset.getDatasetId());
    DarCollectionSummary summary = darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(
        collectionOneId);

    assertNotNull(summary);
    assertEquals(collectionOneId, summary.getDarCollectionId());
    assertEquals(1, summary.getDatasetIds().size());
    summary.getDatasetIds()
        .forEach((id) -> assertTrue(targetDatasets.contains(id)));
    assertEquals(0, summary.getElections().size());
    assertEquals(1, summary.getDatasetCount());
  }

  @Test
  void testGetDarCollectionSummaryForDACByCollectionId() {
    User userOne = createUser();
    User userTwo = createUser();
    User userChair = createUser();
    Integer userOneId = userOne.getUserId();
    Integer userTwoId = userTwo.getUserId();
    Integer userChairId = userChair.getUserId();

    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer excludedCollectionId = createDarCollection(userTwoId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest excludedDar = createDataAccessRequest(excludedCollectionId, userTwoId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(excludedDar.getReferenceId(),
        datasetTwo.getDatasetId());

    Election collectionOnePrevElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.CLOSED.getValue(),
        darOne.getReferenceId(),
        dataset.getDatasetId()); //non-latest dataset, need to make sure this isn't pulled into query results
    Election collectionOneElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.OPEN.getValue(), darOne.getReferenceId(), dataset.getDatasetId());
    Integer collectionOneElectionId = collectionOneElection.getElectionId();
    Integer collectionOnePrevElectionId = collectionOnePrevElection.getElectionId();
    Election excludedCollectionElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.OPEN.getValue(),
        excludedDar.getReferenceId(), datasetTwo.getDatasetId());
    Integer excludedCollectionElectionId = excludedCollectionElection.getElectionId();

    //create old votes to ensure that they don't get pulled in by the query
    createVote(userOneId, collectionOnePrevElectionId, VoteType.DAC.getValue());
    createVote(userTwoId, collectionOnePrevElectionId, VoteType.DAC.getValue());
    createVote(userChairId, collectionOnePrevElectionId, VoteType.DAC.getValue());
    createVote(userChairId, collectionOnePrevElectionId, VoteType.CHAIRPERSON.getValue());

    Vote collectionOneVoteOne = createVote(userOneId, collectionOneElectionId,
        VoteType.DAC.getValue());
    Vote collectionOneVoteTwo = createVote(userTwoId, collectionOneElectionId,
        VoteType.DAC.getValue());
    Vote collectionOneVoteThree = createVote(userChairId, collectionOneElectionId,
        VoteType.DAC.getValue());
    Vote collectionOneVoteChair = createVote(userChairId, collectionOneElectionId,
        VoteType.CHAIRPERSON.getValue());

    Vote collectionTwoVoteOne = createVote(userOneId, excludedCollectionElectionId,
        VoteType.DAC.getValue());
    Vote collectionTwoVoteTwo = createVote(userTwoId, excludedCollectionElectionId,
        VoteType.DAC.getValue());
    Vote collectionTwoVoteThree = createVote(userChairId, excludedCollectionElectionId,
        VoteType.DAC.getValue());
    Vote collectionTwoVoteChair = createVote(userChairId, excludedCollectionElectionId,
        VoteType.CHAIRPERSON.getValue());

    List<Integer> targetDatasets = List.of(dataset.getDatasetId(), datasetTwo.getDatasetId());
    DarCollectionSummary summary = darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(
        userChairId, targetDatasets, collectionOneId);

    assertNotNull(summary);
    assertEquals(collectionOneId, summary.getDarCollectionId());
    assertEquals(1, summary.getDatasetIds().size());
    summary.getDatasetIds()
        .forEach((id) -> assertTrue(targetDatasets.contains(id)));

    List<Integer> targetVotes = List.of(collectionOneVoteChair.getVoteId(),
        collectionOneVoteThree.getVoteId());
    Integer electionId = collectionOneElection.getElectionId();

    summary.getElections().entrySet()
        .forEach((e) -> assertEquals(electionId, e.getKey()));
    summary.getVotes().forEach((v) -> assertTrue(
        targetVotes.contains(v.getVoteId())));
    assertEquals(1, summary.getDatasetCount());
  }

  @Test
  void testGetDarCollectionSummaryForDACByCollectionId_NoElectionsPresent() {
    User userOne = createUser();
    User userChair = createUser();
    Integer userOneId = userOne.getUserId();
    Integer userChairId = userChair.getUserId();

    Dataset dataset = createDataset(userOneId);
    Dataset excludedDataset = createDataset(userOneId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer excludedDarCollectionId = createDarCollection(userOneId);
    DataAccessRequest excludedDar = createDataAccessRequest(excludedDarCollectionId, userOneId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(excludedDar.getReferenceId(),
        excludedDataset.getDatasetId());

    Election excludedElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.CLOSED.getValue(),
        excludedDar.getReferenceId(), excludedDataset.getDatasetId());
    Integer excludedElectionId = excludedElection.getElectionId();

    // create votes for dataset that should NOT be pulled by the query
    createVote(userOneId, excludedElectionId, VoteType.DAC.getValue());

    List<Integer> targetDatasets = List.of(dataset.getDatasetId());
    DarCollectionSummary summary = darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(
        userChairId, targetDatasets, collectionOneId);

    assertNotNull(summary);
    assertEquals(collectionOneId, summary.getDarCollectionId());
    assertEquals(1, summary.getDatasetIds().size());
    summary.getDatasetIds()
        .forEach((id) -> assertTrue(targetDatasets.contains(id)));

    assertEquals(0, summary.getElections().size());
    assertEquals(0, summary.getVotes().size());
    assertEquals(1, summary.getDatasetCount());
  }

  @Test
  void testGetDarCollectionSummaryForDACByCollectionId_ArchivedCollection() {
    User userOne = createUser();
    User userChair = createUser();
    Integer userOneId = userOne.getUserId();
    Integer userChairId = userChair.getUserId();

    Dataset dataset = createDataset(userOneId);
    Integer archivedCollectionId = createDarCollection(userOneId);
    DataAccessRequest archivedDar = createDataAccessRequest(archivedCollectionId, userOneId);
    dataAccessRequestDAO.archiveByReferenceIds(List.of(archivedDar.getReferenceId()));
    dataAccessRequestDAO.insertDARDatasetRelation(archivedDar.getReferenceId(),
        dataset.getDatasetId());

    List<Integer> targetDatasets = List.of(dataset.getDatasetId());
    DarCollectionSummary summary = darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(
        userChairId, targetDatasets, archivedCollectionId);

    assertNull(summary);
  }

  @Test
  void testGetDarCollectionSummaryByCollectionId_ArchivedCollection() {
    User userOne = createUser();
    Integer userOneId = userOne.getUserId();

    Dataset dataset = createDataset(userOneId);
    Integer archivedCollectionId = createDarCollection(userOneId);
    DataAccessRequest archivedDar = createDataAccessRequest(archivedCollectionId, userOneId);
    dataAccessRequestDAO.archiveByReferenceIds(List.of(archivedDar.getReferenceId()));
    dataAccessRequestDAO.insertDARDatasetRelation(archivedDar.getReferenceId(),
        dataset.getDatasetId());

    DarCollectionSummary summary = darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(
        archivedCollectionId);
    assertNull(summary);
  }

  @Test
  void testGetDarCollectionSummaryForResearcherWithProgressReports() {
    Setup setup = createDarCollectionSummaryForUser();
    Integer userId = setup.userId();
    DarCollectionSummary summary = setup.summary();

    List<DarCollectionSummary> summariesForResearcher = darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(userId);
    assertEquals(1, summariesForResearcher.size());
    DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(summariesForResearcher.get(0).getReferenceIds().stream().findFirst().get());

    assertEquals(summary.getDarCollectionId(), dar.getCollectionId());
    //create child progress report
    String progressReportReferenceId = createProgressReportFromDAR(dar).getReferenceId();
    validateSummaryObjectForResearcherWithParent(userId, progressReportReferenceId);
    //create grandchild progress report
    DataAccessRequest progressReportDar = dataAccessRequestDAO.findByReferenceId(progressReportReferenceId);
    String progressReportReferenceId2 = createProgressReportFromDAR(progressReportDar).getReferenceId();
    validateSummaryObjectForResearcherWithParent(userId, progressReportReferenceId2);
  }

  @Test
  void testGetDarCollectionSummaryForDACWithProgressReports() {
    Setup setup = createDarCollectionSummaryForUser();
    Integer chairId = setup.chairId();
    DarCollectionSummary summary = setup.summary();

    DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(summary.getReferenceIds().stream().findFirst().get());

    createProgressReportFromDAR(dar);

    List<DarCollectionSummary> summariesForDAC =
        darCollectionSummaryDAO.getDarCollectionSummariesForDAC(chairId, dar.getDatasetIds());
    assertTrue(summariesForDAC.get(0).getProgressReport());
  }

  @Test
  void testGetDarCollectionSummaryForSOWithProgressReports() {
    Setup setup = createDarCollectionSummaryForUser();
    DarCollectionSummary summary = setup.summary();

    DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(summary.getReferenceIds().stream().findFirst().get());

    createProgressReportFromDAR(dar);

    User user = userDAO.findUserById(dar.getUserId());
    Integer institutionId = user.getInstitutionId();

    List<DarCollectionSummary> summariesForDAC =
        darCollectionSummaryDAO.getDarCollectionSummariesForSO(institutionId);
    assertTrue(summariesForDAC.get(0).getProgressReport());
  }

  @Test
  void testGetDarCollectionSummaryForAdminWithProgressReports() {
    Setup setup = createDarCollectionSummaryForUser();
    DarCollectionSummary summary = setup.summary();

    DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(summary.getReferenceIds().stream().findFirst().get());

    createProgressReportFromDAR(dar);

    List<DarCollectionSummary> summariesForDAC =
        darCollectionSummaryDAO.getDarCollectionSummariesForAdmin();
    assertTrue(summariesForDAC.get(0).getProgressReport());
  }

  @Test
  void testGetDarCollectionSummaryForDACByCollectionIdWithProgressReports() {
    Setup setup = createDarCollectionSummaryForUser();
    Integer chairId = setup.chairId();
    DarCollectionSummary summary = setup.summary();

    DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(summary.getReferenceIds().stream().findFirst().get());

    createProgressReportFromDAR(dar);

    DarCollectionSummary summaryForDAC =
        darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(chairId, dar.getDatasetIds(), dar.getCollectionId());
    assertTrue(summaryForDAC.getProgressReport());
  }

  private Setup createDarCollectionSummaryForUser() {
    Dac dac = createDac();
    User user = createUserWithInstitution();
    User userChair = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
    Integer userId = user.getUserId();
    Integer chairId = userChair.getUserId();
    Integer dacId = dac.getDacId();
    Dataset dataset = createDatasetWithDac(userId, dacId);

    DarCollectionSummary summary = createDarWithVotes(userId, chairId, dataset.getDatasetId());
    return new Setup(userId, chairId, summary);
  }

  public record Setup(Integer userId, Integer chairId, DarCollectionSummary summary) {};

  private void validateSummaryObjectForResearcherWithParent(Integer userId, String referenceId) {
    DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(
        referenceId);
    List<DarCollectionSummary> summariesForResearcher =
        darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(userId);
    assertTrue(summariesForResearcher.get(0).getProgressReport());
    assertTrue(dar.getProgressReport());
  }

  private DarCollectionSummary createDarWithVotes(Integer userId, Integer chairId, Integer datasetId) {
    Integer collectionOneId = createDarCollection(userId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), datasetId);


    Election collectionOneElection = createElection(ElectionType.DATA_ACCESS,
        ElectionStatus.CLOSED.getValue(),
        darOne.getReferenceId(), datasetId);


    createVote(chairId, collectionOneElection.getElectionId(),
        VoteType.FINAL.getValue());

    return darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(
        collectionOneId);
  }
}
