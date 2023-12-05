package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.jupiter.api.Test;

class VoteDAOTest extends DAOTestHelper {

  @Test
  void testFindVoteById() {
    User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    Dataset dataset = createDataset();
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequestWithDatasetAndCollectionInfo(collection_id,
        dataset.getDataSetId(), user.getUserId());
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    Vote vote = createDacVote(user.getUserId(), election.getElectionId());

    Vote foundVote = voteDAO.findVoteById(vote.getVoteId());
    assertNotNull(foundVote);
  }

  @Test
  void testFindVotesByIds() {
    User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    User user2 = createUserWithRole(UserRoles.MEMBER.getRoleId());
    User user3 = createUserWithRole(UserRoles.MEMBER.getRoleId());
    User user4 = createUserWithRole(UserRoles.MEMBER.getRoleId());
    Dataset dataset = createDataset();
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequestWithDatasetAndCollectionInfo(collection_id,
        dataset.getDataSetId(), user.getUserId());
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    Vote vote = createDacVote(user.getUserId(), election.getElectionId());
    Vote vote2 = createDacVote(user2.getUserId(), election.getElectionId());
    Vote vote3 = createDacVote(user3.getUserId(), election.getElectionId());
    Vote vote4 = createDacVote(user4.getUserId(), election.getElectionId());
    List<Integer> voteIds = List.of(vote.getVoteId(), vote2.getVoteId(), vote3.getVoteId(),
        vote4.getVoteId());

    List<Vote> foundVotes = voteDAO.findVotesByIds(voteIds);
    assertNotNull(foundVotes);
    assertFalse(foundVotes.isEmpty());
    assertTrue(
        foundVotes.stream().map(Vote::getVoteId).toList().containsAll(voteIds));
  }

  @Test
  void testFindVotesByElectionIds() {
    User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    Dataset dataset = createDataset();
    String darCode = "DAR-12345";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequestWithDatasetAndCollectionInfo(collection_id,
        dataset.getDataSetId(), user.getUserId());
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    createDacVote(user.getUserId(), election.getElectionId());

    Dataset dataset2 = createDataset();
    String darCode2 = "DAR-67890";
    Integer collection_id2 = darCollectionDAO.insertDarCollection(darCode2, user.getUserId(),
        new Date());
    DataAccessRequest dar2 = createDataAccessRequestWithDatasetAndCollectionInfo(collection_id2,
        dataset.getDataSetId(), user.getUserId());
    Election election2 = createDataAccessElection(dar2.getReferenceId(), dataset2.getDataSetId());
    createDacVote(user.getUserId(), election2.getElectionId());
    List<Integer> electionIds = Arrays.asList(election.getElectionId(), election2.getElectionId());

    List<Vote> foundVotes = voteDAO.findVotesByElectionIds(electionIds);
    assertNotNull(foundVotes);
    assertFalse(foundVotes.isEmpty());
    assertEquals(2, foundVotes.size());
  }

  @Test
  void testFindVotesByElectionIdAndType() {
    User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    Dataset dataset = createDataset();
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequestWithDatasetAndCollectionInfo(collection_id,
        dataset.getDataSetId(), user.getUserId());
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    Vote vote = createDacVote(user.getUserId(), election.getElectionId());

    List<Vote> foundVotes = voteDAO.findVotesByElectionIdAndType(election.getElectionId(),
        vote.getType());
    assertNotNull(foundVotes);
    assertFalse(foundVotes.isEmpty());
    assertEquals(1, foundVotes.size());

    List<Vote> foundVotes2 = voteDAO.findVotesByElectionIdAndType(election.getElectionId(),
        vote.getType().toLowerCase());
    assertNotNull(foundVotes2);
    assertFalse(foundVotes2.isEmpty());
    assertEquals(1, foundVotes2.size());

    List<Vote> foundVotes3 = voteDAO.findVotesByElectionIdAndType(election.getElectionId(),
        vote.getType().toUpperCase());
    assertNotNull(foundVotes3);
    assertFalse(foundVotes3.isEmpty());
    assertEquals(1, foundVotes3.size());
  }

  @Test
  void testFindVoteByElectionIdAndDACUserId() {
    User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    Dataset dataset = createDataset();
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequestWithDatasetAndCollectionInfo(collection_id,
        dataset.getDataSetId(), user.getUserId());
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    Vote vote = createDacVote(user.getUserId(), election.getElectionId());

    Vote foundVote = voteDAO.findVoteByElectionIdAndUserId(election.getElectionId(),
        user.getUserId());
    assertNotNull(foundVote);
    assertEquals(vote.getVoteId(), foundVote.getVoteId());
  }

  @Test
  void testCheckVoteById() {
    User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    Dataset dataset = createDataset();
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequestWithDatasetAndCollectionInfo(collection_id,
        dataset.getDataSetId(), user.getUserId());
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    Vote vote = createDacVote(user.getUserId(), election.getElectionId());

    Integer voteId = voteDAO.checkVoteById(election.getReferenceId(), vote.getVoteId());
    assertNotNull(voteId);
    assertEquals(vote.getVoteId(), voteId);
  }

  @Test
  void testInsertVote() {
    // no-op ... tested by `createDacVote` and `createFinalVote`
  }

  @Test
  void testCreateVote() {
    User user = createUser();
    Dataset dataset = createDataset();
    Dac dac = createDac();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequestWithDatasetAndCollectionInfo(collection_id,
        dataset.getDataSetId(), user.getUserId());
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    Vote v = createDacVote(user.getUserId(), election.getElectionId());

    Vote vote = voteDAO.findVoteById(v.getVoteId());
    assertNotNull(vote);
    assertNull(vote.getVote());
  }

  @Test
  void testUpdateVote() {
    User user = createUser();
    Dataset dataset = createDataset();
    Dac dac = createDac();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequestWithDatasetAndCollectionInfo(collection_id,
        dataset.getDataSetId(), user.getUserId());
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    Vote v = createDacVote(user.getUserId(), election.getElectionId());

    String rationale = "rationale";
    Date now = new Date();
    voteDAO.updateVote(true, rationale, now, v.getVoteId(), true,
        election.getElectionId(), now, true);
    Vote vote = voteDAO.findVoteById(v.getVoteId());
    assertTrue(vote.getVote());
    assertTrue(vote.getHasConcerns());
    assertTrue(vote.getIsReminderSent());
    assertEquals(vote.getRationale(), rationale);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    assertEquals(sdf.format(vote.getCreateDate()), sdf.format(now));
    assertEquals(sdf.format(vote.getUpdateDate()), sdf.format(now));
  }

  @Test
  void testDeleteVotes() {
    // No-op ... tested by `tearDown()`
  }

  @Test
  void testUpdateVoteReminderFlag() {
    User user = createUser();
    Dataset dataset = createDataset();
    Dac dac = createDac();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequestWithDatasetAndCollectionInfo(collection_id,
        dataset.getDataSetId(), user.getUserId());
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    Vote v = createDacVote(user.getUserId(), election.getElectionId());

    voteDAO.updateVoteReminderFlag(v.getVoteId(), true);
    Vote vote = voteDAO.findVoteById(v.getVoteId());
    assertTrue(vote.getIsReminderSent());

    voteDAO.updateVoteReminderFlag(v.getVoteId(), false);
    Vote vote2 = voteDAO.findVoteById(v.getVoteId());
    assertFalse(vote2.getIsReminderSent());
  }

  @Test
  void testFindTotalFinalVoteByElectionTypeAndVote() {
    User user = createUser();
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequestWithDatasetAndCollectionInfo(collection_id,
        dataset.getDataSetId(), user.getUserId());
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    electionDAO.updateElectionById(
        election.getElectionId(),
        ElectionStatus.CLOSED.getValue(),
        new Date());
    Vote v = createFinalVote(user.getUserId(), election.getElectionId());
    boolean voteValue = true;
    voteDAO.updateVote(
        voteValue,
        RandomStringUtils.randomAlphabetic(10),
        new Date(),
        v.getVoteId(),
        false,
        election.getElectionId(),
        v.getCreateDate(),
        false
    );

    int count = voteDAO.findTotalFinalVoteByElectionTypeAndVote(election.getElectionType(),
        voteValue);
    assertEquals(1, count);

    int count2 = voteDAO.findTotalFinalVoteByElectionTypeAndVote(
        election.getElectionType().toLowerCase(), voteValue);
    assertEquals(1, count2);

    int count3 = voteDAO.findTotalFinalVoteByElectionTypeAndVote(
        election.getElectionType().toUpperCase(), voteValue);
    assertEquals(1, count3);
  }

  @Test
  void testFindMaxNumberOfDACMembers() {
    User user = createUser();
    Dataset dataset = createDataset();
    Dac dac = createDac();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequestWithDatasetAndCollectionInfo(collection_id,
        dataset.getDataSetId(), user.getUserId());
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    electionDAO.updateElectionById(
        election.getElectionId(),
        ElectionStatus.CLOSED.getValue(),
        new Date());
    Vote v = createDacVote(user.getUserId(), election.getElectionId());
    boolean voteValue = true;
    voteDAO.updateVote(
        voteValue,
        RandomStringUtils.random(10),
        new Date(),
        v.getVoteId(),
        false,
        election.getElectionId(),
        v.getCreateDate(),
        false
    );

    int count = voteDAO.findMaxNumberOfDACMembers(
        Collections.singletonList(election.getElectionId()));
    assertEquals(1, count);
  }

  @Test
  void testInsertVotes() {
    User user1 = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    User user2 = createUserWithRole(UserRoles.MEMBER.getRoleId());
    User user3 = createUserWithRole(UserRoles.MEMBER.getRoleId());
    Dataset dataset = createDataset();
    Dac dac = createDac();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    User user4 = createUserWithRole(UserRoles.RESEARCHER.getRoleId());
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user4.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequestWithDatasetAndCollectionInfo(collection_id,
        dataset.getDataSetId(), user4.getUserId());
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    List<Integer> userIds = Arrays.asList(user1.getUserId(), user2.getUserId(), user3.getUserId());

    voteDAO.insertVotes(userIds, election.getElectionId(), VoteType.DAC.getValue());
    List<Vote> votes = voteDAO.findVotesByElectionIds(
        Collections.singletonList(election.getElectionId()));
    assertNotNull(votes);
    assertFalse(votes.isEmpty());
    assertEquals(3, votes.size());
  }

  @Test
  void testRemoveVotesByIds() {
    User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    Dataset dataset = createDataset();
    Dac dac = createDac();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequestWithDatasetAndCollectionInfo(collection_id,
        dataset.getDataSetId(), user.getUserId());
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    Vote vote = createDacVote(user.getUserId(), election.getElectionId());

    voteDAO.removeVotesByIds(Collections.singletonList(vote.getVoteId()));
    Vote v = voteDAO.findVoteById(vote.getVoteId());
    assertNull(v);
  }

  @Test
  void testFindVotesByUserId() {
    User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    Dataset dataset = createDataset();
    Dac dac = createDac();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequestWithDatasetAndCollectionInfo(collection_id,
        dataset.getDataSetId(), user.getUserId());
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    createChairpersonVote(user.getUserId(), election.getElectionId());

    List<Vote> userVotes = voteDAO.findVotesByUserId(user.getUserId());
    assertNotNull(userVotes);
    assertFalse(userVotes.isEmpty());
  }

  @Test
  void testUpdateRationaleByVoteIds() {
    Dataset dataset = createDataset();
    Dac dac = createDac();
    User user = createUserWithRoleInDac(UserRoles.MEMBER.getRoleId(), dac.getDacId());
    DataAccessRequest dar = createDataAccessRequestV3();
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    Vote dacVote = createDacVote(user.getUserId(), election.getElectionId());
    assertNull(dacVote.getRationale());

    String rationale = RandomStringUtils.random(10, true, false);
    voteDAO.updateRationaleByVoteIds(List.of(dacVote.getVoteId()), rationale);

    Vote updatedVote = voteDAO.findVoteById(dacVote.getVoteId());
    assertEquals(rationale, updatedVote.getRationale());
  }

  @Test
  void testFindVoteUsersByElectionReferenceIdList_Empty() {
    // Empty case
    List<User> voteUsers = voteDAO.findVoteUsersByElectionReferenceIdList(
        List.of("invalid reference id"));
    assertTrue(voteUsers.isEmpty());
  }

  @Test
  void testFindVoteUsersByElectionReferenceIdList() {
    // Populated case requires:
    // * DAC
    // * Dataset
    // * DarCollection
    // * DarCollection User
    //      helper method also creates elections and votes for user, so make that user a chairperson
    Dac dac = createDac();
    User chair = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    // This creates an election and votes for the user passed in as the creator
    DarCollection collection = createDarCollectionWithDatasets(chair,
        List.of(dataset));
    Optional<DataAccessRequest> dar = collection.getDars().values().stream().findFirst();
    assertTrue(dar.isPresent());

    List<User> voteUsers = voteDAO.findVoteUsersByElectionReferenceIdList(
        List.of(dar.get().getReferenceId()));
    assertFalse(voteUsers.isEmpty());
    assertEquals(1, voteUsers.size());
    assertEquals(chair.getUserId(), voteUsers.get(0).getUserId());
  }

  private Vote createChairpersonVote(Integer userId, Integer electionId) {
    Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.CHAIRPERSON.getValue());
    return voteDAO.findVoteById(voteId);
  }

  private DarCollection createDarCollectionWithDatasets(User user,
      List<Dataset> datasets) {
    String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    datasets
        .forEach(dataset -> {
          DataAccessRequest dar = createDataAccessRequestWithDatasetAndCollectionInfo(collectionId,
              dataset.getDataSetId(), user.getUserId());
          Election cancelled = createCanceledAccessElection(dar.getReferenceId(),
              dataset.getDataSetId());
          Election access = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
          createFinalVote(user.getUserId(), cancelled.getElectionId());
          createFinalVote(user.getUserId(), access.getElectionId());
        });
    return darCollectionDAO.findDARCollectionByCollectionId(collectionId);
  }

  private DataAccessRequest createDataAccessRequestWithDatasetAndCollectionInfo(int collectionId,
      int datasetId, int userId) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle(RandomStringUtils.randomAlphabetic(10));
    String referenceId = RandomStringUtils.randomAlphanumeric(20);
    dataAccessRequestDAO.insertDataAccessRequest(collectionId, referenceId, userId, new Date(),
        new Date(), new Date(), new Date(), data);
    dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private Election createCanceledAccessElection(String referenceId, Integer datasetId) {
    Integer electionId = electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.CANCELED.getValue(),
        new Date(),
        referenceId,
        datasetId
    );
    return electionDAO.findElectionById(electionId);
  }

  private Dac createDac() {
    Integer id = dacDAO.createDac(
        "Test_" + RandomStringUtils.random(20, true, true),
        "Test_" + RandomStringUtils.random(20, true, true),
        new Date());
    return dacDAO.findById(id);
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
    dsp.setDataSetId(datasetId);
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

  private Dataset createDatasetWithDac(Integer dacId) {
    User user = createUser();
    String name = "Name_" + RandomStringUtils.random(20, true, true);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), dacId);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
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

  private User createUserWithRoleInDac(Integer roleId, Integer dacId) {
    User user = createUserWithRole(roleId);
    dacDAO.addDacMember(roleId, user.getUserId(), dacId);
    return user;
  }

  private User createUserWithRole(Integer roleId) {
    int i1 = RandomUtils.nextInt(5, 10);
    int i2 = RandomUtils.nextInt(5, 10);
    int i3 = RandomUtils.nextInt(3, 5);
    String email = RandomStringUtils.randomAlphabetic(i1) +
        "@" +
        RandomStringUtils.randomAlphabetic(i2) +
        "." +
        RandomStringUtils.randomAlphabetic(i3);
    Integer userId = userDAO.insertUser(email, "display name", new Date());
    userRoleDAO.insertSingleUserRole(roleId, userId);
    return userDAO.findUserById(userId);
  }

}
