package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetEntry;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
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
    Election accessElection1 = createDataAccessElection(accessReferenceId1,
        dataset1.getDataSetId());
    Election accessElection2 = createDataAccessElection(accessReferenceId2,
        dataset2.getDataSetId());

    List<Integer> electionIds = electionDAO.getElectionIdsByReferenceIds(
        List.of(accessReferenceId1, accessReferenceId2));
    assertEquals(2, electionIds.size());
    assertTrue(electionIds.contains(accessElection1.getElectionId()));
    assertTrue(electionIds.contains(accessElection2.getElectionId()));
    List<Integer> missingElectionIds = electionDAO.getElectionIdsByReferenceIds(
        List.of("1", "2", "3"));
    assertTrue(missingElectionIds.isEmpty());
  }

  @Test
  void testFindElectionByDacId() {
    Dac dac = createDac();
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
    Dataset dataset = createDataset();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());

    List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
    assertNotNull(foundElections);
    assertEquals(election.getElectionId(), foundElections.get(0).getElectionId());
  }

  @Test
  void testFindElectionsByReferenceId() {
    Dataset dataset = createDataset();
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();
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

    Integer datasetId = dataset.getDataSetId();

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
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
    Dataset d1 = createDataset();
    Dataset d2 = createDataset();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d1.getDataSetId());
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d2.getDataSetId());
    // Create OPEN elections
    List<Integer> firstElectionIds = Stream
        .of(createElectionsForDarDataset(dar, d1), createElectionsForDarDataset(dar, d2))
        .flatMap(List::stream).toList();
    // Cancel those elections
    firstElectionIds.forEach(
        id -> electionDAO.updateElectionById(id, ElectionStatus.CANCELED.getValue(), new Date(),
            true));
    // Create a new set of elections
    List<Integer> latestElectionIds = Stream
        .of(createElectionsForDarDataset(dar, d1), createElectionsForDarDataset(dar, d2))
        .flatMap(List::stream).toList();

    Election latestAccessForD1 = electionDAO.findLastElectionByReferenceIdDatasetIdAndType(
        dar.getReferenceId(), d1.getDataSetId(), ElectionType.DATA_ACCESS.getValue());
    assertNotNull(latestAccessForD1);
    assertFalse(firstElectionIds.contains(latestAccessForD1.getElectionId()));
    assertTrue(latestElectionIds.contains(latestAccessForD1.getElectionId()));

    Election latestRPForD1 = electionDAO.findLastElectionByReferenceIdDatasetIdAndType(
        dar.getReferenceId(), d1.getDataSetId(), ElectionType.RP.getValue());
    assertNotNull(latestRPForD1);
    assertFalse(firstElectionIds.contains(latestRPForD1.getElectionId()));
    assertTrue(latestElectionIds.contains(latestRPForD1.getElectionId()));

    Election latestAccessForD2 = electionDAO.findLastElectionByReferenceIdDatasetIdAndType(
        dar.getReferenceId(), d2.getDataSetId(), ElectionType.DATA_ACCESS.getValue());
    assertNotNull(latestAccessForD2);
    assertFalse(firstElectionIds.contains(latestAccessForD2.getElectionId()));
    assertTrue(latestElectionIds.contains(latestAccessForD2.getElectionId()));

    Election latestRPForD2 = electionDAO.findLastElectionByReferenceIdDatasetIdAndType(
        dar.getReferenceId(), d2.getDataSetId(), ElectionType.RP.getValue());
    assertNotNull(latestRPForD2);
    assertFalse(firstElectionIds.contains(latestRPForD2.getElectionId()));
    assertTrue(latestElectionIds.contains(latestRPForD2.getElectionId()));
  }

  /**
   * Small helper method for `testFindLastElectionByReferenceIdDatasetIdAndType()` Creates OPEN
   * Access and RP elections for dar/dataset combination
   *
   * @param dar DataAccessRequest
   * @param d   Dataset
   * @return List of created electionIds
   */
  private List<Integer> createElectionsForDarDataset(DataAccessRequest dar, Dataset d) {
    Election accessElection = createDataAccessElection(dar.getReferenceId(), d.getDataSetId());
    Election rpElection = createRPElection(dar.getReferenceId(), d.getDataSetId());
    return List.of(accessElection.getElectionId(), rpElection.getElectionId());
  }

  @Test
  void testFindElectionsByReferenceIdAndDatasetId() {
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
    Dataset d1 = createDataset();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d1.getDataSetId());
    createRPElection(dar.getReferenceId(), d1.getDataSetId());
    createDataAccessElection(dar.getReferenceId(), d1.getDataSetId());

    List<Election> elections = electionDAO.findElectionsByReferenceIdAndDatasetId(
        dar.getReferenceId(), d1.getDataSetId());
    assertEquals(2, elections.size());
  }

  @Test
  void testFindElectionByDacIdWithNoAssociation() {
    Dac dac = createDac();
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
    Dataset dataset = createDataset();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());

    Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());

    List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
    assertNotNull(foundElections);
    assertEquals(election.getElectionId(), foundElections.get(0).getElectionId());
  }

  @Test
  void testFindElectionByDacIdNotFound() {
    Dac dac = createDac();
    Dataset dataset = createDataset();
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
    createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());

    List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
    assertTrue(foundElections.isEmpty());
  }

  @Test
  void testFindAccessElectionWithFinalVoteById() {
    User u = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    Dac dac = createDac();
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
    Dataset d = createDataset();
    datasetDAO.updateDatasetDacId(d.getDataSetId(), dac.getDacId());

    Election e = createDataAccessElection(dar.getReferenceId(), d.getDataSetId());
    Integer voteId = voteDAO.insertVote(u.getUserId(), e.getElectionId(),
        VoteType.FINAL.getValue());
    voteDAO.updateVote(true, "rationale", new Date(), voteId, false, e.getElectionId(), new Date(),
        false);
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
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
    Dataset d = createDataset();
    datasetDAO.updateDatasetDacId(d.getDataSetId(), dac.getDacId());

    Election e = createRPElection(dar.getReferenceId(), d.getDataSetId());
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
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
    Dataset d = createDataset();
    datasetDAO.updateDatasetDacId(d.getDataSetId(), dac.getDacId());

    Integer electionId = electionDAO.insertElection(
        ElectionType.TRANSLATE_DUL.getValue(),
        ElectionStatus.OPEN.getValue(),
        new Date(),
        dar.getReferenceId(),
        d.getDataSetId());
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
    createDataAccessElection(dar.getReferenceId(), d.getDataSetId());
    createRPElection(dar.getReferenceId(), d.getDataSetId());

    List<Election> elections = electionDAO.findElectionsByReferenceId(dar.getReferenceId());
    assertNotNull(elections);
    assertEquals(2, elections.size());
  }

  @Test
  void testInsertExtendedElection() {
    Dac dac = createDac();
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
    Dataset d = createDataset();
    User u = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
    datasetDAO.updateDatasetDacId(d.getDataSetId(), dac.getDacId());

    Integer electionId = electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(),
        new Date(),
        dar.getReferenceId(),
        d.getDataSetId());
    Election e = electionDAO.findElectionById(electionId);
    createFinalVote(u.getUserId(), e.getElectionId());
    Election election = electionDAO.findElectionWithFinalVoteById(e.getElectionId());
    assertNotNull(election);
    assertEquals(e.getElectionId(), election.getElectionId());
  }

  @Test
  void testFindLastElectionsByReferenceIdsAndType() {
    DataAccessRequest dar = createDataAccessRequestV3();
    Dataset d = createDataset();
    electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(),
        new Date(),
        dar.getReferenceId(),
        d.getDataSetId());
    List<Election> elections =
        electionDAO.findLastElectionsByReferenceIdsAndType(
            Collections.singletonList(dar.getReferenceId()), ElectionType.DATA_ACCESS.getValue());
    assertNotNull(elections);
    assertFalse(elections.isEmpty());
    assertEquals(1, elections.size());
  }

  @Test
  void testFindAllDacsForElectionIds() {
    Dac dac = createDac();
    String accessReferenceId = UUID.randomUUID().toString();
    Dataset dataset = createDataset();
    Integer datasetId = dataset.getDataSetId();
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    Integer electionId = electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(),
        new Date(),
        dar.getReferenceId(),
        datasetId);
    Election dulElection = electionDAO.findElectionById(electionId);
    Election accessElection = createDataAccessElection(accessReferenceId, datasetId);
    electionDAO.insertAccessAndConsentElection(accessElection.getElectionId(),
        dulElection.getElectionId());

    List<Integer> electionIds = Collections.singletonList(accessElection.getElectionId());
    List<Dac> dacList = electionDAO.findAllDacsForElectionIds(electionIds);
    Dac dacRecord = dacList.get(0);
    assertEquals(1, dacList.size());
    assertEquals(dac.getName(), dacRecord.getName());
    assertEquals(dac.getDacId(), dacRecord.getDacId());
  }

  @Test
  void testFindAllDacsForElectionIds_EmptyList() {
    List<Integer> electionIds = Collections.singletonList(10000);
    List<Dac> dacList = electionDAO.findAllDacsForElectionIds(electionIds);
    assertTrue(dacList.isEmpty());
  }

  @Test
  void testFindLastElectionsByReferenceIds() {
    Dac dac = createDac();
    Dataset dataset = createDataset();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());

    DataAccessRequest dar = createDataAccessRequestV3();

    String darReferenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), datasetId);

    Election recentClosedAccessElection = createDataAccessElection(darReferenceId,
        dataset.getDataSetId());
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
    int datasetId = dataset.getDataSetId();
    Election accessElection = createDataAccessElection(referenceId, datasetId);
    Election rpElection = createRPElection(referenceId, datasetId);
    User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    int userId = user.getUserId();
    Vote accessVote = createChairpersonVote(userId, accessElection.getElectionId());
    Vote rpVote = createChairpersonVote(userId, rpElection.getElectionId());
    List<Integer> voteIds = List.of(accessVote.getVoteId(), rpVote.getVoteId());
    List<Election> elections = electionDAO.findElectionsByVoteIdsAndType(voteIds, "dataaccess");

    assertEquals(1, elections.size());
    assertEquals(accessElection.getElectionId(), elections.get(0).getElectionId());
  }

  @Test
  void testFindElectionsByVoteIdsAndType_RP() {
    DataAccessRequest dar = createDataAccessRequestV3();
    Dataset dataset = createDataset();
    String referenceId = dar.getReferenceId();
    int datasetId = dataset.getDataSetId();
    Election accessElection = createDataAccessElection(referenceId, datasetId);
    Election rpElection = createRPElection(referenceId, datasetId);
    User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    int userId = user.getUserId();
    Vote accessVote = createChairpersonVote(userId, accessElection.getElectionId());
    Vote rpVote = createChairpersonVote(userId, rpElection.getElectionId());
    List<Integer> voteIds = List.of(accessVote.getVoteId(), rpVote.getVoteId());
    List<Election> elections = electionDAO.findElectionsByVoteIdsAndType(voteIds, "rp");

    assertEquals(1, elections.size());
    assertEquals(rpElection.getElectionId(), elections.get(0).getElectionId());
  }

  @Test
  void testFindElectionsWithCardHoldingUsersByElectionIds() {
    User lcUser = createUser();
    User nonLCUser = createUser();
    Dataset dataset = createDataset();
    int datasetId = dataset.getDataSetId();
    createLibraryCard(lcUser);
    DataAccessRequest lcDAR = createDataAccessRequestWithUserIdV3(lcUser.getUserId());
    DataAccessRequest nonLCDAR = createDataAccessRequestWithUserIdV3(nonLCUser.getUserId());
    Election lcElection = createDataAccessElection(lcDAR.getReferenceId(), datasetId);
    Election nonLCElection = createDataAccessElection(nonLCDAR.getReferenceId(), datasetId);
    List<Integer> electionIds = List.of(lcElection.getElectionId(), nonLCElection.getElectionId());
    List<Election> elections = electionDAO.findElectionsWithCardHoldingUsersByElectionIds(
        electionIds);

    assertEquals(1, elections.size());
    assertEquals(elections.get(0).getElectionId(), lcElection.getElectionId());
  }

  @Test
  void testDeleteByReferenceIds() {
    Dataset dataset = createDataset();
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();
    Election accessElection = createDataAccessElection(referenceId, datasetId);
    Election rpElection = createRPElection(referenceId, datasetId);

    List<Election> elections = electionDAO.findOpenElectionsByReferenceIds(
        List.of(dar.referenceId));
    assertEquals(2, elections.size());

    electionDAO.deleteElectionsByIds(
        List.of(accessElection.getElectionId(), rpElection.getElectionId()));

    elections = electionDAO.findOpenElectionsByReferenceIds(List.of(dar.referenceId));
    assertEquals(0, elections.size());
  }

  @Test
  void testInsertAndFindElection() {

    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();

    Date d = new Date();

    Integer id = electionDAO.insertElection(
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
    assertEquals(datasetId, e.getDataSetId());

  }

  @Test
  void testUpdateElectionById() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();

    Election before = createDataAccessElection(referenceId, datasetId);

    assertEquals(ElectionStatus.OPEN.getValue(), before.getStatus());
    assertNull(before.getLastUpdate());

    electionDAO.updateElectionById(
        before.getElectionId(),
        ElectionStatus.FINAL.getValue(),
        new Date());

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
    Integer datasetId = dataset.getDataSetId();

    Election before = createDataAccessElection(referenceId, datasetId);

    assertEquals(ElectionStatus.OPEN.getValue(), before.getStatus());
    assertNull(before.getLastUpdate());
    assertNull(before.getFinalAccessVote());

    electionDAO.updateElectionById(
        before.getElectionId(),
        ElectionStatus.FINAL.getValue(),
        new Date(),
        true);

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
    Integer datasetId = dataset.getDataSetId();

    Election e = createDataAccessElection(referenceId, datasetId);

    Election returned =
        electionDAO.findElectionWithFinalVoteById(
            e.getElectionId());

    assertNull(returned);
  }

  @Test
  void testFindElectionWithFinalVoteById_Success() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();

    Election e = createDataAccessElection(referenceId, datasetId);
    createFinalVote(user.getUserId(), e.getElectionId());

    Election returned =
        electionDAO.findElectionWithFinalVoteById(
            e.getElectionId());

    assertEquals(e.getElectionId(), returned.getElectionId());
  }

  @Test
  void testFindElectionsByIds() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());

    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();

    Election datasetAccessElection = createDataAccessElection(referenceId, datasetId);
    Election rpElection = createRPElection(referenceId, datasetId);

    List<Election> found = electionDAO.findElectionsByIds(
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
    Integer datasetId = dataset.getDataSetId();

    Election e = createDataAccessElection(referenceId, datasetId);

    Election found = electionDAO.findElectionById(e.getElectionId());

    assertEquals(e, found);
  }

  @Test
  void testArchiveElectionByIds() {
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
    Dataset d1 = createDataset();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d1.getDataSetId());
    createRPElection(dar.getReferenceId(), d1.getDataSetId());
    createDataAccessElection(dar.getReferenceId(), d1.getDataSetId());
    List<Election> elections = electionDAO.findElectionsByReferenceIdAndDatasetId(
        dar.getReferenceId(), d1.getDataSetId());
    List<Integer> electionIds = elections.stream().map(Election::getElectionId).toList();

    electionDAO.archiveElectionByIds(electionIds, new Date());
    List<Election> archivedElections = electionDAO.findElectionsByIds(electionIds);
    archivedElections.forEach(e -> assertTrue(e.getArchived()));
  }

  @Test
  void testFindApprovalAccessElectionDate() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());

    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();

    Election e = createDataAccessElection(referenceId, datasetId);
    Vote v = createFinalVote(user.getUserId(), e.getElectionId());
    voteDAO.updateVote(true,
        "",
        new Date(),
        v.getVoteId(),
        false,
        e.getElectionId(),
        new Date(),
        null);
    electionDAO.updateElectionById(
        e.getElectionId(),
        ElectionStatus.FINAL.getValue(),
        new Date(),
        true);

    assertEquals(e.getCreateDate(),
        electionDAO.findApprovalAccessElectionDate(referenceId));
  }

  @Test
  void testFindApprovalAccessElectionDate_NotApproved() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());

    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();

    Election e = createDataAccessElection(referenceId, datasetId);
    createFinalVote(user.getUserId(), e.getElectionId());

    assertNull(electionDAO.findApprovalAccessElectionDate(referenceId));
  }

  @Test
  void testFindDacForElection() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());

    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();

    Election e = createDataAccessElection(referenceId, datasetId);

    assertEquals(dac.getDacId(),
        electionDAO.findDacForElection(e.getElectionId()).getDacId());
  }

  @Test
  void testFindOpenElectionsByDacId() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());

    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();

    Election e1 = createDataAccessElection(referenceId, datasetId);
    Election e2 = createRPElection(referenceId, datasetId);
    Election e3 = createDataAccessElection(referenceId, datasetId);
    Election closed = createDataAccessElection(referenceId, datasetId);
    electionDAO.updateElectionById(
        closed.getElectionId(),
        ElectionStatus.CLOSED.getValue(),
        new Date()
    );

    List<Election> found = electionDAO.findOpenElectionsByDacId(dac.getDacId());

    assertEquals(3, found.size());
    assertTrue(found.contains(e1));
    assertTrue(found.contains(e2));
    assertTrue(found.contains(e3));
  }

  private Vote createPopulatedChairpersonVote(Integer userId, Integer electionId) {
    Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.CHAIRPERSON.getValue());
    voteDAO.updateVote(true, "rationale", new Date(), voteId, false, electionId, new Date(), false);
    return voteDAO.findVoteById(voteId);
  }

  private Vote createChairpersonVote(Integer userId, Integer electionId) {
    Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.CHAIRPERSON.getValue());
    return voteDAO.findVoteById(voteId);
  }

  private DataAccessRequest createDataAccessRequestWithUserIdV3(Integer userId) {
    String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, userId, new Date());
    for (int i = 0; i < 4; i++) {
      createDataAccessRequest(userId, collectionId, darCode);
    }
    return createDataAccessRequest(userId, collectionId, darCode);
  }

  /**
   * Creates a new user, dataset, data access request, and dar collection
   *
   * @return Populated DataAccessRequest
   */
  private DataAccessRequest createDataAccessRequest(Integer userId, Integer collectionId,
      String darCode) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle("Project Title: " + RandomStringUtils.random(50, true, false));
    data.setDarCode(darCode);
    DatasetEntry entry = new DatasetEntry();
    entry.setKey("key");
    entry.setValue("value");
    entry.setLabel("label");
    data.setDatasets(List.of(entry));
    data.setHmb(true);
    data.setMethods(false);
    String referenceId = UUID.randomUUID().toString();
    Date now = new Date();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId,
        referenceId,
        userId,
        now, now, now, now,
        data);
    return dataAccessRequestDAO.findByReferenceId(referenceId);
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

  private LibraryCard createLibraryCard(User user) {
    Integer institutionId = createInstitution().getId();
    String stringValue = "value";
    Integer id = libraryCardDAO.insertLibraryCard(user.getUserId(), institutionId, stringValue,
        user.getDisplayName(), user.getEmail(), user.getUserId(), new Date());
    return libraryCardDAO.findLibraryCardById(id);
  }

  private Institution createInstitution() {
    User createUser = createUser();
    Integer id = institutionDAO.insertInstitution(RandomStringUtils.randomAlphabetic(20),
        "itDirectorName",
        "itDirectorEmail",
        RandomStringUtils.randomAlphabetic(10),
        new Random().nextInt(),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        OrganizationType.NON_PROFIT.getValue(),
        createUser.getUserId(),
        createUser.getCreateDate());
    Institution institution = institutionDAO.findInstitutionById(id);
    User updateUser = createUser();
    institutionDAO.updateInstitutionById(
        id,
        institution.getName(),
        institution.getItDirectorEmail(),
        institution.getItDirectorName(),
        institution.getInstitutionUrl(),
        institution.getDunsNumber(),
        institution.getOrgChartUrl(),
        institution.getVerificationUrl(),
        institution.getVerificationFilename(),
        institution.getOrganizationType().getValue(),
        updateUser.getUserId(),
        new Date()
    );
    return institutionDAO.findInstitutionById(id);
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
