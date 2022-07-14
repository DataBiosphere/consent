package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.DarDataset;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DarCollectionSummaryDAOTest extends DAOTestHelper {

  private DataAccessRequest createDataAccessRequest(Integer collectionId, Integer userId) {
    String referenceId = UUID.randomUUID().toString();
    Date createDate = new Date();
    Date submissionDate = new Date();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle(RandomStringUtils.randomAlphabetic(20));
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
    userDAO.updateUser(user.getDisplayName(), user.getUserId(), null, institutionId);
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
    //two collections
      //1st collection
        //two datasets
        //three elections
        //dac -> two members, one chair(user)
        //one dar
      //2nd collection
        //two datasets
        //four elections
        //dac -> two members, one chair(user)
        //two dars (legacy)

    Dac dac = createDacForTest(); //NOTE: may not need this, remove if true
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
    Dataset excludedDataset = createDataset(userOneId);
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
        darOne.getReferenceId(), dataset.getDataSetId());
    Election collectionOneElection = createElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue(), darOne.getReferenceId(), dataset.getDataSetId());
    Integer collectionOneElectionId = collectionOneElection.getElectionId();
    Integer collectionOnePrevElectionId = collectionOnePrevElection.getElectionId();
    Election collectionTwoElection = createElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue(),
        darTwo.getReferenceId(), datasetTwo.getDataSetId());
    Integer collectionTwoElectionId = collectionTwoElection.getElectionId(); 
    Election excludedElection = createElection(ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.CLOSED.getValue(),
        excludedDar.getReferenceId(), excludedDataset.getDataSetId());
    Integer excludedElectionId = excludedElection.getElectionId();
    
    //create old votes to ensure that they don't get pulled in by the query
    createVote(userOneId, collectionOnePrevElectionId, VoteType.DAC.getValue());
    createVote(userTwoId, collectionOnePrevElectionId, VoteType.DAC.getValue());
    createVote(userChairId, collectionOnePrevElectionId, VoteType.DAC.getValue());
    createVote(userChairId, collectionOnePrevElectionId, VoteType.CHAIRPERSON.getValue());

    //create votes for dataset that should NOT be pulled by the query
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
    List<DarCollectionSummary> summaries = darCollectionSummaryDAO.getDarCollectionSummariesForDAC(userOneId, targetDatasets);
    assertNotNull(summaries);
  }

  
}
