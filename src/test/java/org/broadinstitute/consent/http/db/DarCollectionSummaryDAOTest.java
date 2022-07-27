package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DarCollectionSummaryDAOTest extends DAOTestHelper {

  private DataAccessRequest createDataAccessRequest(Integer collectionId, Integer userId) {
    String referenceId = UUID.randomUUID().toString();
    Date createDate = new Date();
    Date submissionDate = new Date();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle(RandomStringUtils.randomAlphabetic(20));
    data.setStatus("test");
    dataAccessRequestDAO.insertDataAccessRequest(collectionId, referenceId, userId, createDate, new Date(), submissionDate, new Date(), data);
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private Institution createInstitution(Integer userId) {
    Integer institutionId = institutionDAO.insertInstitution(RandomStringUtils.randomAlphabetic(20), "itDirectorName", "itDirectorEmail", userId, new Date());
    return institutionDAO.findInstitutionById(institutionId);
  }

  private User createUserForTest() {
    Integer userId = userDAO.insertUser(RandomStringUtils.random(10), RandomStringUtils.randomAlphabetic(10), new Date());
    return userDAO.findUserById(userId);
  }

  private User assignInstitutionToUser(User user, Integer institutionId) {
    userDAO.updateUser(user.getDisplayName(), user.getUserId(), institutionId);
    return userDAO.findUserById(user.getUserId());
  }

  private Integer createDarCollection(Integer createUserId) {
    String darCode = RandomStringUtils.randomAlphabetic(20);
    return darCollectionDAO.insertDarCollection(darCode, createUserId, new Date());
  }

  private Dataset createDataset(Integer userId) {
    Integer datasetId = datasetDAO.insertDataset(RandomStringUtils.randomAlphabetic(20), new Timestamp(System.currentTimeMillis()), userId, null, true);
    return datasetDAO.findDatasetById(datasetId);
  }

  private Election createElection(String type, String status, String referenceId, Integer datasetId) {
    Integer electionId = electionDAO.insertElection(type, status, new Date(), referenceId, null, null, null, datasetId);
    return electionDAO.findElectionById(electionId);
  }

  private Vote createVote(Integer dacUserId, Integer electionId, String type) {
    Integer voteId = voteDAO.insertVote(dacUserId, electionId, type);
    return voteDAO.findVoteById(voteId);
  }

  private Dac createDacForTest() {
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(10), "test description", new Date());
    return dacDAO.findById(dacId);
  }

  @Test
  public void testGetDarCollectionSummaryForDAC() {
    Dac dac = createDacForTest();
    User userOne = createUserForTest();
    User userTwo = createUserForTest();
    User userChair = createUserForTest();
    Integer userOneId = userOne.getUserId();
    Integer userTwoId = userTwo.getUserId();
    Integer userChairId = userChair.getUserId();

    Institution institution = createInstitution(userOneId);
    Integer institutionId = institution.getId();
    userOne = assignInstitutionToUser(userOne, institutionId);
    userTwo = assignInstitutionToUser(userTwo, institutionId);
    userChair = assignInstitutionToUser(userChair, institutionId);
    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Dataset excludedDataset = createDataset(userOneId); //represents dataset that does not fall under user DAC's purview
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    Integer excludedDarCollectionId = createDarCollection(userOneId);
    DataAccessRequest excludedDar = createDataAccessRequest(excludedDarCollectionId, userOneId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest darTwo = createDataAccessRequest(collectionTwoId, userTwoId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDataSetId());
    dataAccessRequestDAO.insertDARDatasetRelation(darTwo.getReferenceId(), datasetTwo.getDataSetId());
    dataAccessRequestDAO.insertDARDatasetRelation(excludedDar.getReferenceId(), excludedDataset.getDataSetId());

    Election collectionOnePrevElection = createElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.CLOSED.getValue(),
        darOne.getReferenceId(), dataset.getDataSetId()); //non-latest dataset, need to make sure this isn't pulled into query results
    Election collectionOneElection = createElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue(), darOne.getReferenceId(), dataset.getDataSetId());
    Integer collectionOneElectionId = collectionOneElection.getElectionId();
    Integer collectionOnePrevElectionId = collectionOnePrevElection.getElectionId();
    Election excludedElection = createElection(ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.CLOSED.getValue(), //tied to excluded dataset, it should not be pulled in
        excludedDar.getReferenceId(), excludedDataset.getDataSetId());
    Election collectionTwoElection = createElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue(),
        darTwo.getReferenceId(), datasetTwo.getDataSetId());
    Integer collectionTwoElectionId = collectionTwoElection.getElectionId();
    Integer excludedElectionId = excludedElection.getElectionId();

    //create old votes to ensure that they don't get pulled in by the query
    createVote(userOneId, collectionOnePrevElectionId, VoteType.DAC.getValue());
    createVote(userTwoId, collectionOnePrevElectionId, VoteType.DAC.getValue());
    createVote(userChairId, collectionOnePrevElectionId, VoteType.DAC.getValue());
    createVote(userChairId, collectionOnePrevElectionId, VoteType.CHAIRPERSON.getValue());

    //create votes for dataset that should NOT be pulled by the query (tied to exluded dataset)
    createVote(userOneId, excludedElectionId, VoteType.DAC.getValue());

    Vote collectionOneVoteOne = createVote(userOneId, collectionOneElectionId, VoteType.DAC.getValue());
    Vote collectionOneVoteTwo = createVote(userTwoId, collectionOneElectionId, VoteType.DAC.getValue());
    Vote collectionOneVoteThree = createVote(userChairId, collectionOneElectionId, VoteType.DAC.getValue());
    Vote collectionOneVoteChair = createVote(userChairId, collectionOneElectionId, VoteType.CHAIRPERSON.getValue());

    Vote collectionTwoVoteOne = createVote(userOneId, collectionTwoElectionId, VoteType.DAC.getValue());
    Vote collectionTwoVoteTwo = createVote(userTwoId, collectionTwoElectionId, VoteType.DAC.getValue());
    Vote collectionTwoVoteThree = createVote(userChairId, collectionTwoElectionId, VoteType.DAC.getValue());
    Vote collectionTwoVoteChair = createVote(userChairId, collectionTwoElectionId, VoteType.CHAIRPERSON.getValue());

    List<Integer> targetDatasets = List.of(dataset.getDataSetId(), datasetTwo.getDataSetId());
    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForDAC(userChairId, targetDatasets);

    assertNotNull(summaries);
    assertEquals(2, summaries.size());
    summaries.forEach((s) -> {
      assertEquals(1, s.getDatasetIds().size());
      s.getDatasetIds().stream()
        .forEach((id) -> assertTrue(targetDatasets.contains(id)));

      List<Integer> targetVotes;
      Integer electionId;

      if(s.getDarCollectionId() == collectionOneId) {
        targetVotes = List.of(collectionOneVoteChair.getVoteId(), collectionOneVoteThree.getVoteId());
        electionId = collectionOneElection.getElectionId();
      } else {
        targetVotes = List.of(collectionTwoVoteChair.getVoteId(), collectionTwoVoteThree.getVoteId());
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
  public void testGetDarCollectionSummaryForDAC_NoElectionsPresent() {
    Dac dac = createDacForTest();
    User userOne = createUserForTest();
    User userChair = createUserForTest();
    Integer userOneId = userOne.getUserId();
    Integer userChairId = userChair.getUserId();

    Institution institution = createInstitution(userOneId);
    Integer institutionId = institution.getId();
    userOne = assignInstitutionToUser(userOne, institutionId);
    userChair = assignInstitutionToUser(userChair, institutionId);
    Dataset dataset = createDataset(userOneId);
    Dataset excludedDataset = createDataset(userOneId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer excludedDarCollectionId = createDarCollection(userOneId);
    DataAccessRequest excludedDar = createDataAccessRequest(excludedDarCollectionId, userOneId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDataSetId());
    dataAccessRequestDAO.insertDARDatasetRelation(excludedDar.getReferenceId(), excludedDataset.getDataSetId());

    Election excludedElection = createElection(ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.CLOSED.getValue(),
        excludedDar.getReferenceId(), excludedDataset.getDataSetId());
    Integer excludedElectionId = excludedElection.getElectionId();

    // create votes for dataset that should NOT be pulled by the query
    createVote(userOneId, excludedElectionId, VoteType.DAC.getValue());

    List<Integer> targetDatasets = List.of(dataset.getDataSetId());
    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForDAC(userChairId,
        targetDatasets);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    summaries.forEach((s) -> {
      assertEquals(1, s.getDatasetIds().size());
      s.getDatasetIds().stream()
          .forEach((id) -> assertTrue(targetDatasets.contains(id)));

      assertEquals(0, s.getElections().size());
      assertEquals(0, s.getVotes().size());
      assertEquals(1, s.getDatasetCount());
    });
  }

  @Test
  public void testGetDarCollectionSummaryForSO() {

    Dac dac = createDacForTest();
    User userOne = createUserForTest();
    User userTwo = createUserForTest();
    Integer userOneId = userOne.getUserId();
    Integer userTwoId = userTwo.getUserId();

    Institution institution = createInstitution(userOneId);
    Institution institutionTwo = createInstitution(userTwoId);
    Integer institutionId = institution.getId(); // query should only pull in collections that were created by users with this instituion_id
    userOne = assignInstitutionToUser(userOne, institutionId);
    userTwo = assignInstitutionToUser(userTwo, institutionTwo.getId());
    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest darTwo = createDataAccessRequest(collectionTwoId, userTwoId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDataSetId());
    dataAccessRequestDAO.insertDARDatasetRelation(darTwo.getReferenceId(), datasetTwo.getDataSetId());

    Election collectionOnePrevElection = createElection(ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.CLOSED.getValue(),
        darOne.getReferenceId(), dataset.getDataSetId());
    Election collectionOneElection = createElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue(),
        darOne.getReferenceId(), dataset.getDataSetId());
    Integer collectionOneElectionId = collectionOneElection.getElectionId();
    Integer collectionOnePrevElectionId = collectionOnePrevElection.getElectionId();
    Election collectionTwoElection = createElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue(),
        darTwo.getReferenceId(), datasetTwo.getDataSetId());
    Integer collectionTwoElectionId = collectionTwoElection.getElectionId();

    List<Integer> targetDatasets = List.of(dataset.getDataSetId());
    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForSO(institutionId);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    summaries.forEach((s) -> {
      assertEquals(1, s.getDatasetIds().size());
      s.getDatasetIds().stream()
          .forEach((id) -> assertTrue(targetDatasets.contains(id)));

      Integer electionId = collectionOneElection.getElectionId();
      s.getElections().entrySet().stream()
          .forEach((e) -> {
            assertEquals(electionId, e.getKey());
          });
      assertEquals(1, s.getDatasetCount());
    });
  }

  @Test
  public void testGetDarCollectionSummaryForSO_NoElectionsPresent() {

    Dac dac = createDacForTest();
    User userOne = createUserForTest();
    User userTwo = createUserForTest();
    Integer userOneId = userOne.getUserId();
    Integer userTwoId = userTwo.getUserId();

    Institution institution = createInstitution(userOneId);
    Institution institutionTwo = createInstitution(userTwoId);
    Integer institutionId = institution.getId();
    userOne = assignInstitutionToUser(userOne, institutionId);
    userTwo = assignInstitutionToUser(userTwo, institutionTwo.getId());
    Dataset dataset = createDataset(userOneId);
    Integer collectionOneId = createDarCollection(userOneId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDataSetId());

    List<Integer> targetDatasets = List.of(dataset.getDataSetId());
    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForSO(institutionId);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    summaries.forEach((s) -> {
      assertEquals(1, s.getDatasetIds().size());
      s.getDatasetIds().stream()
          .forEach((id) -> assertTrue(targetDatasets.contains(id)));

      assertEquals(0, s.getElections().size());
      assertEquals(1, s.getDatasetCount());
    });
  }

  @Test
  public void testGetDarCollectionSummaryForResearcher() {

    Dac dac = createDacForTest();
    User userOne = createUserForTest();
    User userTwo = createUserForTest();
    Integer userOneId = userOne.getUserId(); //query should only pull in collection made by this user
    Integer userTwoId = userTwo.getUserId();

    Institution institution = createInstitution(userOneId);
    Institution institutionTwo = createInstitution(userTwoId);
    Integer institutionId = institution.getId();
    userOne = assignInstitutionToUser(userOne, institutionId);
    userTwo = assignInstitutionToUser(userTwo, institutionTwo.getId());
    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest darTwo = createDataAccessRequest(collectionTwoId, userTwoId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDataSetId());
    dataAccessRequestDAO.insertDARDatasetRelation(darTwo.getReferenceId(), datasetTwo.getDataSetId());

    Election collectionOnePrevElection = createElection(ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.CLOSED.getValue(),
        darOne.getReferenceId(), dataset.getDataSetId());
    Election collectionOneElection = createElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue(),
        darOne.getReferenceId(), dataset.getDataSetId());
    Integer collectionOneElectionId = collectionOneElection.getElectionId();
    Integer collectionOnePrevElectionId = collectionOnePrevElection.getElectionId();
    Election collectionTwoElection = createElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue(),
        darTwo.getReferenceId(), datasetTwo.getDataSetId());
    Integer collectionTwoElectionId = collectionTwoElection.getElectionId();

    List<Integer> targetDatasets = List.of(dataset.getDataSetId());
    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(userOneId);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    summaries.forEach((s) -> {
      assertEquals(1, s.getDatasetIds().size());
      s.getDatasetIds().stream()
          .forEach((id) -> assertTrue(targetDatasets.contains(id)));

      Integer electionId = collectionOneElection.getElectionId();
      s.getElections().entrySet().stream()
          .forEach((e) -> {
            assertEquals(electionId, e.getKey());
          });
      assertEquals(1, s.getDarStatuses().size());
      s.getDarStatuses().values().forEach(status -> assertEquals("test", status));
      assertEquals(1, s.getDatasetCount());
    });
  }

  @Test
  public void testGetDarCollectionSummaryForResearcher_NoElectionsPresent() {

    Dac dac = createDacForTest();
    User userOne = createUserForTest();
    User userTwo = createUserForTest();
    Integer userOneId = userOne.getUserId(); //query should only pull collections made by this user
    Integer userTwoId = userTwo.getUserId();

    Institution institution = createInstitution(userOneId);
    Institution institutionTwo = createInstitution(userTwoId);
    Integer institutionId = institution.getId();
    userOne = assignInstitutionToUser(userOne, institutionId);
    userTwo = assignInstitutionToUser(userTwo, institutionTwo.getId());
    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest darTwo = createDataAccessRequest(collectionTwoId, userTwoId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDataSetId());
    dataAccessRequestDAO.insertDARDatasetRelation(darTwo.getReferenceId(), datasetTwo.getDataSetId());

    Election collectionTwoElection = createElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue(),
        darTwo.getReferenceId(), datasetTwo.getDataSetId());
    Integer collectionTwoElectionId = collectionTwoElection.getElectionId();

    List<Integer> targetDatasets = List.of(dataset.getDataSetId());
    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(userOneId);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    summaries.forEach((s) -> {
      assertEquals(1, s.getDatasetIds().size());
      s.getDatasetIds().stream()
          .forEach((id) -> assertTrue(targetDatasets.contains(id)));
      assertEquals(0, s.getElections().size());
      assertEquals(1, s.getDatasetCount());
    });
  }

  @Test
  public void testGetDarCollectionSummaryForAdmin() {

    Dac dac = createDacForTest();
    User userOne = createUserForTest();
    User userTwo = createUserForTest();
    Integer userOneId = userOne.getUserId();
    Integer userTwoId = userTwo.getUserId();

    Institution institution = createInstitution(userOneId);
    Institution institutionTwo = createInstitution(userTwoId);
    Integer institutionId = institution.getId();
    userOne = assignInstitutionToUser(userOne, institutionId);
    userTwo = assignInstitutionToUser(userTwo, institutionTwo.getId());
    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest darTwo = createDataAccessRequest(collectionTwoId, userTwoId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDataSetId());
    dataAccessRequestDAO.insertDARDatasetRelation(darTwo.getReferenceId(), datasetTwo.getDataSetId());

    Election collectionOnePrevElection = createElection(ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.CLOSED.getValue(),
        darOne.getReferenceId(), dataset.getDataSetId());
    Election collectionOneElection = createElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue(),
        darOne.getReferenceId(), dataset.getDataSetId());
    Integer collectionOneElectionId = collectionOneElection.getElectionId();
    Integer collectionOnePrevElectionId = collectionOnePrevElection.getElectionId();
    Election collectionTwoElection = createElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue(),
        darTwo.getReferenceId(), datasetTwo.getDataSetId());
    Integer collectionTwoElectionId = collectionTwoElection.getElectionId();

    List<Integer> targetDatasets = List.of(dataset.getDataSetId(), datasetTwo.getDataSetId());
    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForAdmin();
    assertNotNull(summaries);
    assertEquals(2, summaries.size());
    summaries.forEach((s) -> {
      assertEquals(1, s.getDatasetIds().size());
      s.getDatasetIds().stream()
          .forEach((id) -> assertTrue(targetDatasets.contains(id)));

      Integer electionId;

      if (s.getDarCollectionId() == collectionOneId) {
        electionId = collectionOneElection.getElectionId();
      } else {
        electionId = collectionTwoElection.getElectionId();
      }

      s.getElections().entrySet().stream()
          .forEach((e) -> {
            assertEquals(electionId, e.getKey());
          });
      assertEquals(1, s.getDatasetCount());
    });
  }

  @Test
  public void testGetDarCollectionSummaryForAdmin_NoPresentElections() {

    Dac dac = createDacForTest();
    User userOne = createUserForTest();
    User userTwo = createUserForTest();
    Integer userOneId = userOne.getUserId();
    Integer userTwoId = userTwo.getUserId();

    Institution institution = createInstitution(userOneId);
    Institution institutionTwo = createInstitution(userTwoId);
    Integer institutionId = institution.getId();
    userOne = assignInstitutionToUser(userOne, institutionId);
    userTwo = assignInstitutionToUser(userTwo, institutionTwo.getId());
    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest darTwo = createDataAccessRequest(collectionTwoId, userTwoId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDataSetId());
    dataAccessRequestDAO.insertDARDatasetRelation(darTwo.getReferenceId(), datasetTwo.getDataSetId());

    List<Integer> targetDatasets = List.of(dataset.getDataSetId(), datasetTwo.getDataSetId());
    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForAdmin();
    assertNotNull(summaries);
    assertEquals(2, summaries.size());
    summaries.forEach((s) -> {
      assertEquals(1, s.getDatasetIds().size());
      s.getDatasetIds().stream()
          .forEach((id) -> assertTrue(targetDatasets.contains(id)));
      s.getDarStatuses().values()
        .forEach((st) -> assertTrue(st.equalsIgnoreCase("test")));
      assertEquals(0, s.getElections().size());
      assertEquals(1, s.getDatasetCount());
    });
  }

  @Test
  public void testGetDarCollectionSummaryByCollectionId() {
    Dac dac = createDacForTest();
    User userOne = createUserForTest();
    User userTwo = createUserForTest();
    Integer userOneId = userOne.getUserId();
    Integer userTwoId = userTwo.getUserId();

    Institution institution = createInstitution(userOneId);
    Institution institutionTwo = createInstitution(userTwoId);
    Integer institutionId = institution.getId();
    userOne = assignInstitutionToUser(userOne, institutionId);
    userTwo = assignInstitutionToUser(userTwo, institutionTwo.getId());
    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest darTwo = createDataAccessRequest(collectionTwoId, userTwoId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDataSetId());
    dataAccessRequestDAO.insertDARDatasetRelation(darTwo.getReferenceId(), datasetTwo.getDataSetId());

    Election collectionOnePrevElection = createElection(ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.CLOSED.getValue(),
            darOne.getReferenceId(), dataset.getDataSetId());
    Election collectionOneElection = createElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue(),
            darOne.getReferenceId(), dataset.getDataSetId());
    Integer collectionOneElectionId = collectionOneElection.getElectionId();
    Integer collectionOnePrevElectionId = collectionOnePrevElection.getElectionId();
    Election collectionTwoElection = createElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue(),
            darTwo.getReferenceId(), datasetTwo.getDataSetId());
    Integer collectionTwoElectionId = collectionTwoElection.getElectionId();

    List<Integer> targetDatasets = List.of(dataset.getDataSetId());
    DarCollectionSummary summary = darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionOneId);

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
  public void testGetDarCollectionSummaryByCollectionId_NoElectionsPresent() {
    Dac dac = createDacForTest();
    User userOne = createUserForTest();
    User userTwo = createUserForTest();
    Integer userOneId = userOne.getUserId();
    Integer userTwoId = userTwo.getUserId();

    Institution institution = createInstitution(userOneId);
    Institution institutionTwo = createInstitution(userTwoId);
    Integer institutionId = institution.getId();
    userOne = assignInstitutionToUser(userOne, institutionId);
    userTwo = assignInstitutionToUser(userTwo, institutionTwo.getId());
    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest darTwo = createDataAccessRequest(collectionTwoId, userTwoId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDataSetId());
    dataAccessRequestDAO.insertDARDatasetRelation(darTwo.getReferenceId(), datasetTwo.getDataSetId());

    Election collectionTwoElection = createElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue(),
            darTwo.getReferenceId(), datasetTwo.getDataSetId());
    Integer collectionTwoElectionId = collectionTwoElection.getElectionId();

    List<Integer> targetDatasets = List.of(dataset.getDataSetId());
    DarCollectionSummary summary = darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionOneId);

    assertNotNull(summary);
    assertEquals(collectionOneId, summary.getDarCollectionId());
    assertEquals(1, summary.getDatasetIds().size());
    summary.getDatasetIds()
              .forEach((id) -> assertTrue(targetDatasets.contains(id)));
    assertEquals(0, summary.getElections().size());
    assertEquals(1, summary.getDatasetCount());
  }

  @Test
  public void testGetDarCollectionSummaryForDACByCollectionId() {
    Dac dac = createDacForTest();
    User userOne = createUserForTest();
    User userTwo = createUserForTest();
    User userChair = createUserForTest();
    Integer userOneId = userOne.getUserId();
    Integer userTwoId = userTwo.getUserId();
    Integer userChairId = userChair.getUserId();

    Institution institution = createInstitution(userOneId);
    Integer institutionId = institution.getId();
    userOne = assignInstitutionToUser(userOne, institutionId);
    userTwo = assignInstitutionToUser(userTwo, institutionId);
    userChair = assignInstitutionToUser(userChair, institutionId);
    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer excludedCollectionId = createDarCollection(userTwoId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);
    DataAccessRequest excludedDar = createDataAccessRequest(excludedCollectionId, userTwoId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDataSetId());
    dataAccessRequestDAO.insertDARDatasetRelation(excludedDar.getReferenceId(), datasetTwo.getDataSetId());

    Election collectionOnePrevElection = createElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.CLOSED.getValue(),
            darOne.getReferenceId(), dataset.getDataSetId()); //non-latest dataset, need to make sure this isn't pulled into query results
    Election collectionOneElection = createElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue(), darOne.getReferenceId(), dataset.getDataSetId());
    Integer collectionOneElectionId = collectionOneElection.getElectionId();
    Integer collectionOnePrevElectionId = collectionOnePrevElection.getElectionId();
    Election excludedCollectionElection = createElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue(),
            excludedDar.getReferenceId(), datasetTwo.getDataSetId());
    Integer excludedCollectionElectionId = excludedCollectionElection.getElectionId();

    //create old votes to ensure that they don't get pulled in by the query
    createVote(userOneId, collectionOnePrevElectionId, VoteType.DAC.getValue());
    createVote(userTwoId, collectionOnePrevElectionId, VoteType.DAC.getValue());
    createVote(userChairId, collectionOnePrevElectionId, VoteType.DAC.getValue());
    createVote(userChairId, collectionOnePrevElectionId, VoteType.CHAIRPERSON.getValue());

    Vote collectionOneVoteOne = createVote(userOneId, collectionOneElectionId, VoteType.DAC.getValue());
    Vote collectionOneVoteTwo = createVote(userTwoId, collectionOneElectionId, VoteType.DAC.getValue());
    Vote collectionOneVoteThree = createVote(userChairId, collectionOneElectionId, VoteType.DAC.getValue());
    Vote collectionOneVoteChair = createVote(userChairId, collectionOneElectionId, VoteType.CHAIRPERSON.getValue());

    Vote collectionTwoVoteOne = createVote(userOneId, excludedCollectionElectionId, VoteType.DAC.getValue());
    Vote collectionTwoVoteTwo = createVote(userTwoId, excludedCollectionElectionId, VoteType.DAC.getValue());
    Vote collectionTwoVoteThree = createVote(userChairId, excludedCollectionElectionId, VoteType.DAC.getValue());
    Vote collectionTwoVoteChair = createVote(userChairId, excludedCollectionElectionId, VoteType.CHAIRPERSON.getValue());

    List<Integer> targetDatasets = List.of(dataset.getDataSetId(), datasetTwo.getDataSetId());
    DarCollectionSummary summary = darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(userChairId, targetDatasets, collectionOneId);

    assertNotNull(summary);
    assertEquals(collectionOneId, summary.getDarCollectionId());
    assertEquals(1, summary.getDatasetIds().size());
    summary.getDatasetIds()
              .forEach((id) -> assertTrue(targetDatasets.contains(id)));

    List<Integer> targetVotes = List.of(collectionOneVoteChair.getVoteId(), collectionOneVoteThree.getVoteId());
    Integer electionId = collectionOneElection.getElectionId();

    summary.getElections().entrySet()
            .forEach((e) -> assertEquals(electionId, e.getKey()));
    summary.getVotes().forEach((v) -> assertTrue(targetVotes.contains(v.getVoteId())));
    assertEquals(1, summary.getDatasetCount());
  }

  @Test
  public void testGetDarCollectionSummaryForDACByCollectionId_NoElectionsPresent() {
    Dac dac = createDacForTest();
    User userOne = createUserForTest();
    User userChair = createUserForTest();
    Integer userOneId = userOne.getUserId();
    Integer userChairId = userChair.getUserId();

    Institution institution = createInstitution(userOneId);
    Integer institutionId = institution.getId();
    userOne = assignInstitutionToUser(userOne, institutionId);
    userChair = assignInstitutionToUser(userChair, institutionId);
    Dataset dataset = createDataset(userOneId);
    Dataset excludedDataset = createDataset(userOneId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer excludedDarCollectionId = createDarCollection(userOneId);
    DataAccessRequest excludedDar = createDataAccessRequest(excludedDarCollectionId, userOneId);
    DataAccessRequest darOne = createDataAccessRequest(collectionOneId, userOneId);

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDataSetId());
    dataAccessRequestDAO.insertDARDatasetRelation(excludedDar.getReferenceId(), excludedDataset.getDataSetId());

    Election excludedElection = createElection(ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.CLOSED.getValue(),
            excludedDar.getReferenceId(), excludedDataset.getDataSetId());
    Integer excludedElectionId = excludedElection.getElectionId();

    // create votes for dataset that should NOT be pulled by the query
    createVote(userOneId, excludedElectionId, VoteType.DAC.getValue());

    List<Integer> targetDatasets = List.of(dataset.getDataSetId());
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
  public void testGetDarCollectionSummaryForDACByCollectionId_ArchivedCollection() {
    Dac dac = createDacForTest();
    User userOne = createUserForTest();
    User userChair = createUserForTest();
    Integer userOneId = userOne.getUserId();
    Integer userChairId = userChair.getUserId();

    Institution institution = createInstitution(userOneId);
    Integer institutionId = institution.getId();
    userOne = assignInstitutionToUser(userOne, institutionId);
    userChair = assignInstitutionToUser(userChair, institutionId);
    Dataset dataset = createDataset(userOneId);
    Integer archivedCollectionId = createDarCollection(userOneId);
    DataAccessRequest archivedDar = createDataAccessRequest(archivedCollectionId, userOneId);
    dataAccessRequestDAO.archiveByReferenceIds(List.of(archivedDar.getReferenceId()));
    dataAccessRequestDAO.insertDARDatasetRelation(archivedDar.getReferenceId(), dataset.getDataSetId());


    List<Integer> targetDatasets = List.of(dataset.getDataSetId());
    DarCollectionSummary summary = darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(
    userChairId, targetDatasets, archivedCollectionId);

    assertNull(summary);
  }

  @Test
  public void testGetDarCollectionSummaryByCollectionId_ArchivedCollection() {
    Dac dac = createDacForTest();
    User userOne = createUserForTest();
    Integer userOneId = userOne.getUserId();

    Institution institution = createInstitution(userOneId);
    Integer institutionId = institution.getId();
    userOne = assignInstitutionToUser(userOne, institutionId);
    Dataset dataset = createDataset(userOneId);
    Integer archivedCollectionId = createDarCollection(userOneId);
    DataAccessRequest archivedDar = createDataAccessRequest(archivedCollectionId, userOneId);
    dataAccessRequestDAO.archiveByReferenceIds(List.of(archivedDar.getReferenceId()));
    dataAccessRequestDAO.insertDARDatasetRelation(archivedDar.getReferenceId(), dataset.getDataSetId());

    DarCollectionSummary summary = darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(archivedCollectionId);
    assertNull(summary);
  }
}
