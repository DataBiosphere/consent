package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Reminder;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserVoteReminder;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.mail.MailMessageInsert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ElectionDAOTest extends DAOTestHelper {

  @Test
  void testGetElectionIdsByReferenceIds() {
    String accessReferenceId1 = UUID.randomUUID().toString();
    String accessReferenceId2 = UUID.randomUUID().toString();
    Dataset dataset1 = createDataset();
    Dataset dataset2 = createDataset();
    Election accessElection1 =
        createDataAccessElection(accessReferenceId1, dataset1.getDatasetId());
    Election accessElection2 =
        createDataAccessElection(accessReferenceId2, dataset2.getDatasetId());

    List<Integer> electionIds =
        electionDAO.getElectionIdsByReferenceIds(List.of(accessReferenceId1, accessReferenceId2));
    assertEquals(2, electionIds.size());
    assertTrue(electionIds.contains(accessElection1.getElectionId()));
    assertTrue(electionIds.contains(accessElection2.getElectionId()));
    List<Integer> missingElectionIds =
        electionDAO.getElectionIdsByReferenceIds(List.of("1", "2", "3"));
    assertTrue(missingElectionIds.isEmpty());
  }

  @Test
  void testFindElectionByDacId() {
    Dac dac = createDac();
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId);
    Dataset dataset = createDataset();
    datasetDAO.updateDatasetDacId(dataset.getDatasetId(), dac.getDacId());
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDatasetId());

    List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
    assertNotNull(foundElections);
    assertEquals(election.getElectionId(), foundElections.getFirst().getElectionId());
  }

  @Test
  void testFindElectionsByReferenceId() {
    Dataset dataset = createDataset();
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDatasetId();
    Election election1 = createDataAccessElection(referenceId, datasetId);
    Election election2 = createDataAccessElection(referenceId, datasetId);

    List<Election> found = electionDAO.findElectionsByReferenceId(referenceId);
    assertEquals(2, found.size());

    assertTrue(found.contains(election1));
    assertTrue(found.contains(election2));
  }

  @Test
  void testFindElectionsByReferenceIds() {
    Dataset dataset = createDataset();
    DataAccessRequest dar1 = createDataAccessRequestV3();
    DataAccessRequest dar2 = createDataAccessRequestV3();

    String referenceId1 = dar1.getReferenceId();
    String referenceId2 = dar2.getReferenceId();

    Integer datasetId = dataset.getDatasetId();

    Election election1 = createDataAccessElection(referenceId1, datasetId);
    Election election2 = createDataAccessElection(referenceId1, datasetId);

    Election election3 = createDataAccessElection(referenceId2, datasetId);
    Election election4 = createDataAccessElection(referenceId2, datasetId);

    List<Election> found = electionDAO.findElectionsByReferenceId(referenceId1);
    assertEquals(2, found.size());

    found = electionDAO.findElectionsByReferenceId(referenceId2);
    assertEquals(2, found.size());

    found = electionDAO.findElectionsByReferenceIds(List.of(referenceId1, referenceId2));
    assertEquals(4, found.size());

    assertTrue(found.contains(election1));
    assertTrue(found.contains(election2));
    assertTrue(found.contains(election3));
    assertTrue(found.contains(election4));
  }

  @Test
  void testFindLastElectionByReferenceIdDatasetIdAndType() {
    // Goal is to create elections for a single dar across two datasets
    // One set of elections will be canceled
    // A new set will then be created
    // We should find ONLY the most recent elections with this method
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId);
    Dataset d1 = createDataset();
    Dataset d2 = createDataset();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d1.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d2.getDatasetId());
    // Create OPEN elections
    List<Integer> firstElectionIds =
        Stream.of(createElectionsForDarDataset(dar, d1), createElectionsForDarDataset(dar, d2))
            .flatMap(List::stream)
            .toList();
    // Cancel those elections
    firstElectionIds.forEach(
        id ->
            electionDAO.updateElectionById(
                id, ElectionStatus.CANCELED.getValue(), new Date(), true));
    // Create a new set of elections
    List<Integer> latestElectionIds =
        Stream.of(createElectionsForDarDataset(dar, d1), createElectionsForDarDataset(dar, d2))
            .flatMap(List::stream)
            .toList();

    Election latestAccessForD1 =
        electionDAO.findLastElectionByReferenceIdDatasetIdAndType(
            dar.getReferenceId(), d1.getDatasetId(), ElectionType.DATA_ACCESS.getValue());
    assertNotNull(latestAccessForD1);
    assertFalse(firstElectionIds.contains(latestAccessForD1.getElectionId()));
    assertTrue(latestElectionIds.contains(latestAccessForD1.getElectionId()));

    Election latestRPForD1 =
        electionDAO.findLastElectionByReferenceIdDatasetIdAndType(
            dar.getReferenceId(), d1.getDatasetId(), ElectionType.RP.getValue());
    assertNotNull(latestRPForD1);
    assertFalse(firstElectionIds.contains(latestRPForD1.getElectionId()));
    assertTrue(latestElectionIds.contains(latestRPForD1.getElectionId()));

    Election latestAccessForD2 =
        electionDAO.findLastElectionByReferenceIdDatasetIdAndType(
            dar.getReferenceId(), d2.getDatasetId(), ElectionType.DATA_ACCESS.getValue());
    assertNotNull(latestAccessForD2);
    assertFalse(firstElectionIds.contains(latestAccessForD2.getElectionId()));
    assertTrue(latestElectionIds.contains(latestAccessForD2.getElectionId()));

    Election latestRPForD2 =
        electionDAO.findLastElectionByReferenceIdDatasetIdAndType(
            dar.getReferenceId(), d2.getDatasetId(), ElectionType.RP.getValue());
    assertNotNull(latestRPForD2);
    assertFalse(firstElectionIds.contains(latestRPForD2.getElectionId()));
    assertTrue(latestElectionIds.contains(latestRPForD2.getElectionId()));
  }

  /**
   * Small helper method for `testFindLastElectionByReferenceIdDatasetIdAndType()` Creates OPEN
   * Access and RP elections for dar/dataset combination
   *
   * @param dar DataAccessRequest
   * @param d Dataset
   * @return List of created electionIds
   */
  private List<Integer> createElectionsForDarDataset(DataAccessRequest dar, Dataset d) {
    Election accessElection = createDataAccessElection(dar.getReferenceId(), d.getDatasetId());
    Election rpElection = createRPElection(dar.getReferenceId(), d.getDatasetId());
    return List.of(accessElection.getElectionId(), rpElection.getElectionId());
  }

  @Test
  void testFindElectionsByReferenceIdAndDatasetId() {
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId);
    Dataset d1 = createDataset();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d1.getDatasetId());
    createRPElection(dar.getReferenceId(), d1.getDatasetId());
    createDataAccessElection(dar.getReferenceId(), d1.getDatasetId());

    List<Election> elections =
        electionDAO.findElectionsByReferenceIdAndDatasetId(dar.getReferenceId(), d1.getDatasetId());
    assertEquals(2, elections.size());
  }

  @Test
  void testFindElectionByDacIdWithNoAssociation() {
    Dac dac = createDac();
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId);
    Dataset dataset = createDataset();
    datasetDAO.updateDatasetDacId(dataset.getDatasetId(), dac.getDacId());

    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDatasetId());

    List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
    assertNotNull(foundElections);
    assertEquals(election.getElectionId(), foundElections.getFirst().getElectionId());
  }

  @Test
  void testFindElectionByDacIdNotFound() {
    Dac dac = createDac();
    Dataset dataset = createDataset();
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId);
    createDataAccessElection(dar.getReferenceId(), dataset.getDatasetId());

    List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
    assertTrue(foundElections.isEmpty());
  }

  @Test
  void testFindAccessElectionWithFinalVoteById() {
    User u = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    Dac dac = createDac();
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId);
    Dataset d = createDataset();
    datasetDAO.updateDatasetDacId(d.getDatasetId(), dac.getDacId());

    Election e = createDataAccessElection(dar.getReferenceId(), d.getDatasetId());
    Integer voteId =
        voteDAO.insertVote(u.getUserId(), e.getElectionId(), VoteType.FINAL.getValue());
    updateVote(true, "rationale", new Date(), voteId, false, e.getElectionId(), new Date(), false);
    Vote v = voteDAO.findVoteById(voteId);

    Election election = electionDAO.findElectionWithFinalVoteById(e.getElectionId());
    assertNotNull(election);
    assertEquals(e.getElectionId(), election.getElectionId());
    assertEquals(v.getVote(), election.getFinalVote());
  }

  @Test
  void testRPFindElectionWithFinalVoteById() {
    User u = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    Dac dac = createDac();
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId);
    Dataset d = createDataset();
    datasetDAO.updateDatasetDacId(d.getDatasetId(), dac.getDacId());

    Election e = createRPElection(dar.getReferenceId(), d.getDatasetId());
    Vote v = createPopulatedChairpersonVote(u.getUserId(), e.getElectionId());

    Election election = electionDAO.findElectionWithFinalVoteById(e.getElectionId());
    assertNotNull(election);
    assertEquals(e.getElectionId(), election.getElectionId());
    assertEquals(v.getVote(), election.getFinalVote());
  }

  @Test
  void testDULFindElectionWithFinalVoteById() {
    User u = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    Dac dac = createDac();
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId);
    Dataset d = createDataset();
    datasetDAO.updateDatasetDacId(d.getDatasetId(), dac.getDacId());

    Integer electionId =
        electionDAO.insertElection(
            ElectionType.TRANSLATE_DUL.getValue(),
            ElectionStatus.OPEN.getValue(),
            new Date(),
            dar.getReferenceId(),
            d.getDatasetId());
    Election e = electionDAO.findElectionById(electionId);
    Vote v = createPopulatedChairpersonVote(u.getUserId(), e.getElectionId());

    Election election = electionDAO.findElectionWithFinalVoteById(e.getElectionId());
    assertNotNull(election);
    assertEquals(e.getElectionId(), election.getElectionId());
    assertEquals(v.getVote(), election.getFinalVote());
  }

  @Test
  void testFindElectionsByReferenceIdCase1() {
    DataAccessRequest dar = createDataAccessRequestV3();
    Dataset d = createDataset();
    createDataAccessElection(dar.getReferenceId(), d.getDatasetId());
    createRPElection(dar.getReferenceId(), d.getDatasetId());

    List<Election> elections = electionDAO.findElectionsByReferenceId(dar.getReferenceId());
    assertNotNull(elections);
    assertEquals(2, elections.size());
  }

  @Test
  void testInsertExtendedElection() {
    Dac dac = createDac();
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId);
    Dataset d = createDataset();
    User u = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
    datasetDAO.updateDatasetDacId(d.getDatasetId(), dac.getDacId());

    Integer electionId =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.OPEN.getValue(),
            new Date(),
            dar.getReferenceId(),
            d.getDatasetId());
    Election e = electionDAO.findElectionById(electionId);
    createFinalVote(u.getUserId(), e.getElectionId());
    Election election = electionDAO.findElectionWithFinalVoteById(e.getElectionId());
    assertNotNull(election);
    assertEquals(e.getElectionId(), election.getElectionId());
  }

  @Test
  void testFindLastElectionsByReferenceIds() {
    Dac dac = createDac();
    Dataset dataset = createDataset();
    datasetDAO.updateDatasetDacId(dataset.getDatasetId(), dac.getDacId());

    DataAccessRequest dar = createDataAccessRequestV3();

    String darReferenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDatasetId();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), datasetId);

    Election recentClosedAccessElection =
        createDataAccessElection(darReferenceId, dataset.getDatasetId());
    Election recentClosedRPElection = createRPElection(darReferenceId, datasetId);
    List<Election> elections =
        electionDAO.findLastElectionsByReferenceIds(Collections.singletonList(dar.referenceId));
    List<Integer> electionIds = elections.stream().map(Election::getElectionId).toList();
    assertFalse(elections.isEmpty());
    assertEquals(2, elections.size());
    assertTrue(electionIds.contains(recentClosedAccessElection.getElectionId()));
    assertTrue(electionIds.contains(recentClosedRPElection.getElectionId()));
  }

  @Test
  void testFindLastElectionsByReferenceIds_EmptyList() {
    List<Election> elections =
        electionDAO.findLastElectionsByReferenceIds(
            Collections.singletonList(UUID.randomUUID().toString()));
    assertTrue(elections.isEmpty());
  }

  @Test
  void testFindElectionsByVoteIdsAndType_DataAccess() {
    DataAccessRequest dar = createDataAccessRequestV3();
    Dataset dataset = createDataset();
    String referenceId = dar.getReferenceId();
    int datasetId = dataset.getDatasetId();
    Election accessElection = createDataAccessElection(referenceId, datasetId);
    Election rpElection = createRPElection(referenceId, datasetId);
    User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    int userId = user.getUserId();
    Vote accessVote = createChairpersonVote(userId, accessElection.getElectionId());
    Vote rpVote = createChairpersonVote(userId, rpElection.getElectionId());
    List<Integer> voteIds = List.of(accessVote.getVoteId(), rpVote.getVoteId());
    List<Election> elections = electionDAO.findElectionsByVoteIdsAndType(voteIds, "dataaccess");

    assertEquals(1, elections.size());
    assertEquals(accessElection.getElectionId(), elections.getFirst().getElectionId());
  }

  @Test
  void testFindElectionsByVoteIdsAndType_RP() {
    DataAccessRequest dar = createDataAccessRequestV3();
    Dataset dataset = createDataset();
    String referenceId = dar.getReferenceId();
    int datasetId = dataset.getDatasetId();
    Election accessElection = createDataAccessElection(referenceId, datasetId);
    Election rpElection = createRPElection(referenceId, datasetId);
    User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    int userId = user.getUserId();
    Vote accessVote = createChairpersonVote(userId, accessElection.getElectionId());
    Vote rpVote = createChairpersonVote(userId, rpElection.getElectionId());
    List<Integer> voteIds = List.of(accessVote.getVoteId(), rpVote.getVoteId());
    List<Election> elections = electionDAO.findElectionsByVoteIdsAndType(voteIds, "rp");

    assertEquals(1, elections.size());
    assertEquals(rpElection.getElectionId(), elections.getFirst().getElectionId());
  }

  @Test
  void testFindElectionsWithCardHoldingUsersByElectionIds() {
    User lcUser = createUser();
    User nonLCUser = createUser();
    Dataset dataset = createDataset();
    int datasetId = dataset.getDatasetId();
    createLibraryCard(lcUser);
    DataAccessRequest lcDAR =
        createDataAccessRequestWithUserIdV3(lcUser.getUserId(), "DAR-0001000");
    DataAccessRequest nonLCDAR =
        createDataAccessRequestWithUserIdV3(nonLCUser.getUserId(), "DAR-0002000");
    Election lcElection = createDataAccessElection(lcDAR.getReferenceId(), datasetId);
    Election nonLCElection = createDataAccessElection(nonLCDAR.getReferenceId(), datasetId);
    List<Integer> electionIds = List.of(lcElection.getElectionId(), nonLCElection.getElectionId());
    List<Election> elections =
        electionDAO.findElectionsWithCardHoldingUsersByElectionIds(electionIds);

    assertEquals(1, elections.size());
    assertEquals(elections.getFirst().getElectionId(), lcElection.getElectionId());
  }

  @Test
  void testInsertAndFindElection() {

    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDatasetId();

    Date d = new Date();

    Integer id =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.OPEN.getValue(),
            d,
            referenceId,
            datasetId);

    Election e = electionDAO.findElectionById(id);

    assertEquals(ElectionType.DATA_ACCESS.getValue(), e.getElectionType());
    assertEquals(ElectionStatus.OPEN.getValue(), e.getStatus());
    assertNotNull(e.getCreateDate());
    assertEquals(referenceId, e.getReferenceId());
    assertEquals(datasetId, e.getDatasetId());
  }

  @Test
  void testUpdateElectionById() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDatasetId();

    Election before = createDataAccessElection(referenceId, datasetId);

    assertEquals(ElectionStatus.OPEN.getValue(), before.getStatus());
    assertNull(before.getLastUpdate());

    electionDAO.updateElectionById(
        before.getElectionId(), ElectionStatus.FINAL.getValue(), new Date());

    Election after = electionDAO.findElectionById(before.getElectionId());

    assertEquals(ElectionStatus.FINAL.getValue(), after.getStatus());
    assertNotNull(after.getLastUpdate());
  }

  @Test
  void testUpdateElectionById_FinalAccessVote() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDatasetId();

    Election before = createDataAccessElection(referenceId, datasetId);

    assertEquals(ElectionStatus.OPEN.getValue(), before.getStatus());
    assertNull(before.getLastUpdate());
    assertNull(before.getFinalAccessVote());

    electionDAO.updateElectionById(
        before.getElectionId(), ElectionStatus.FINAL.getValue(), new Date(), true);

    Election after = electionDAO.findElectionById(before.getElectionId());

    assertEquals(ElectionStatus.FINAL.getValue(), after.getStatus());
    assertEquals(true, after.getFinalAccessVote());
    assertNotNull(after.getLastUpdate());
  }

  @Test
  void testFindElectionWithFinalVoteById_NotFinal() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDatasetId();

    Election e = createDataAccessElection(referenceId, datasetId);

    Election returned = electionDAO.findElectionWithFinalVoteById(e.getElectionId());

    assertNull(returned);
  }

  @Test
  void testFindElectionWithFinalVoteById_Success() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDatasetId();

    Election e = createDataAccessElection(referenceId, datasetId);
    createFinalVote(user.getUserId(), e.getElectionId());

    Election returned = electionDAO.findElectionWithFinalVoteById(e.getElectionId());

    assertEquals(e.getElectionId(), returned.getElectionId());
  }

  @Test
  void testFindElectionsByIds() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());

    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDatasetId();

    Election datasetAccessElection = createDataAccessElection(referenceId, datasetId);
    Election rpElection = createRPElection(referenceId, datasetId);

    List<Election> found =
        electionDAO.findElectionsByIds(
            List.of(datasetAccessElection.getElectionId(), rpElection.getElectionId()));

    assertEquals(2, found.size());

    assertTrue(found.contains(datasetAccessElection));
    assertTrue(found.contains(rpElection));
  }

  @Test
  void testFindElectionById() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());

    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDatasetId();

    Election e = createDataAccessElection(referenceId, datasetId);

    Election found = electionDAO.findElectionById(e.getElectionId());

    assertEquals(e, found);
  }

  @Test
  void testArchiveElectionByIds() {
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId);
    Dataset d1 = createDataset();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d1.getDatasetId());
    createRPElection(dar.getReferenceId(), d1.getDatasetId());
    createDataAccessElection(dar.getReferenceId(), d1.getDatasetId());
    List<Election> elections =
        electionDAO.findElectionsByReferenceIdAndDatasetId(dar.getReferenceId(), d1.getDatasetId());
    List<Integer> electionIds = elections.stream().map(Election::getElectionId).toList();

    electionDAO.archiveElectionByIds(electionIds, new Date());
    List<Election> archivedElections = electionDAO.findElectionsByIds(electionIds);
    archivedElections.forEach(e -> assertTrue(e.getArchived()));
  }

  @Test
  void testFindDacForElection() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());

    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDatasetId();

    Election e = createDataAccessElection(referenceId, datasetId);

    assertEquals(dac.getDacId(), electionDAO.findDacForElection(e.getElectionId()).getDacId());
  }

  @Test
  void testFindOpenElectionsByDacId() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());

    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDatasetId();

    Election e1 = createDataAccessElection(referenceId, datasetId);
    Election e2 = createRPElection(referenceId, datasetId);
    Election e3 = createDataAccessElection(referenceId, datasetId);
    Election closed = createDataAccessElection(referenceId, datasetId);
    electionDAO.updateElectionById(
        closed.getElectionId(), ElectionStatus.CLOSED.getValue(), new Date());

    List<Election> found = electionDAO.findOpenElectionsByDacId(dac.getDacId());

    assertEquals(3, found.size());
    assertTrue(found.contains(e1));
    assertTrue(found.contains(e2));
    assertTrue(found.contains(e3));
  }

  @Test
  void testFindVotesThatNeedReminders() {
    Vote vote = createElectionAndVote();
    Election election = electionDAO.findElectionById(vote.getElectionId());
    // there's a timing issue in this test due to the precision of time in the column.
    // it's reliably resolved by setting the query start time early enough so that it
    // includes the SQL inserts contemplated by createElectionAndVote.
    // the core issue is that we're using Java dates for the election insert, not NOW()

    List<UserVoteReminder> userVoteReminders =
        electionDAO.findElectionReminders(
            -1, EmailType.COLLECT.getTypeInt(), Instant.now().toString());
    assertEquals(1, userVoteReminders.size());
    assertEquals(1, userVoteReminders.getFirst().getUserReminderList().size());
    Reminder reminder = userVoteReminders.getFirst().getUserReminderList().getFirst();
    assertNotNull(reminder.userId());
    assertNotNull(reminder.collectionId());
    assertNotNull(reminder.createDate());
    assertEquals(vote.getUserId(), reminder.userId());
    assertEquals(Instant.ofEpochMilli(election.getCreateDate().getTime()), reminder.createDate());
  }

  @Test
  void testFindVotesThatNeedReminders_AlreadyVoted() {
    Vote vote = createElectionAndVote();
    voteServiceDAO.updateVotesWithValue(List.of(vote), true, "rationale");

    List<UserVoteReminder> userVoteReminders =
        electionDAO.findElectionReminders(
            -1, EmailType.COLLECT.getTypeInt(), Instant.now().toString());
    assertEquals(0, userVoteReminders.size());
  }

  @Test
  void testFindVotesThatNeedReminders_AlreadyEmailed() {
    Vote vote = createElectionAndVote();
    String referenceId = Instant.now().toString();
    Integer emailType = EmailType.COLLECT.getTypeInt();
    mailMessageDAO.insert(
        new MailMessageInsert(
            referenceId,
            vote.getVoteId(),
            vote.getUserId(),
            emailType,
            Date.from(Instant.now()),
            "Extra, Extra!",
            null,
            null));

    List<UserVoteReminder> userVoteReminders =
        electionDAO.findElectionReminders(-1, emailType, referenceId);
    assertEquals(0, userVoteReminders.size());
  }

  private Vote createElectionAndVote() {
    Dac dac = createDac();
    User chairperson = createUser();
    Dataset dataset = createDatasetWithDac(dac.getDacId());

    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDatasetId();
    dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);

    Election e1 = createDataAccessElection(referenceId, datasetId);
    Integer v1 =
        voteDAO.insertVote(chairperson.getUserId(), e1.getElectionId(), VoteType.FINAL.getValue());
    return voteDAO.findVoteById(v1);
  }

  private Vote createPopulatedChairpersonVote(Integer userId, Integer electionId) {
    Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.CHAIRPERSON.getValue());
    updateVote(true, "rationale", new Date(), voteId, false, electionId, new Date(), false);
    return voteDAO.findVoteById(voteId);
  }

  private Vote createChairpersonVote(Integer userId, Integer electionId) {
    Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.CHAIRPERSON.getValue());
    return voteDAO.findVoteById(voteId);
  }

  private DataAccessRequest createDataAccessRequestWithUserIdV3(Integer userId, String darCode) {
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, userId, new Date());
    for (int i = 0; i < 4; i++) {
      createDataAccessRequest(userId, collectionId);
    }
    return createDataAccessRequest(userId, collectionId);
  }

  /**
   * Creates a new user, dataset, data access request, and dar collection
   *
   * @return Populated DataAccessRequest
   */
  private DataAccessRequest createDataAccessRequest(Integer userId, Integer collectionId) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle("Project Title: " + randomAlphabetic(50));
    data.setHmb(true);
    data.setMethods(false);
    String referenceId = UUID.randomUUID().toString();
    Date now = new Date();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId, referenceId, userId, now, now, now, data, randomAlphabetic(10));
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private Dac createDac() {
    Integer id =
        dacDAO.createDac(
            "Test_" + randomAlphabetic(20),
            "Test_" + randomAlphabetic(20),
            createUser().getUserId());
    return dacDAO.findById(id);
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + randomAlphabetic(20);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + randomAlphabetic(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id =
        datasetDAO.insertDataset(name, now, user.getUserId(), objectId, dataUse.toString(), null);
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

  private void createLibraryCard(User user) {
    Integer id =
        libraryCardDAO.insertLibraryCard(
            user.getUserId(), user.getDisplayName(), user.getEmail(), user.getUserId(), new Date());
    libraryCardDAO.findLibraryCardById(id);
  }

  private void createFinalVote(Integer userId, Integer electionId) {
    Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.FINAL.getValue());
    voteDAO.findVoteById(voteId);
  }

  private Dataset createDatasetWithDac(Integer dacId) {
    User user = createUser();
    String name = "Name_" + randomAlphabetic(20);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + randomAlphabetic(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id =
        datasetDAO.insertDataset(name, now, user.getUserId(), objectId, dataUse.toString(), dacId);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private Election createRPElection(String referenceId, Integer datasetId) {
    Integer electionId =
        electionDAO.insertElection(
            ElectionType.RP.getValue(),
            ElectionStatus.OPEN.getValue(),
            new Date(),
            referenceId,
            datasetId);
    return electionDAO.findElectionById(electionId);
  }

  private Election createDataAccessElection(String referenceId, Integer datasetId) {
    Integer electionId =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.OPEN.getValue(),
            new Date(),
            referenceId,
            datasetId);
    return electionDAO.findElectionById(electionId);
  }
}
