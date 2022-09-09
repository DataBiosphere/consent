package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.junit.Assert;
import org.junit.Test;

import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ElectionDAOTest extends DAOTestHelper {

  @Test
  public void testGetOpenElectionIdByReferenceId() {
    String accessReferenceId = UUID.randomUUID().toString();
    Dataset dataset = createDataset();
    Election accessElection = createDataAccessElection(accessReferenceId, dataset.getDataSetId());

    Integer electionId = electionDAO.getOpenElectionIdByReferenceId(accessReferenceId);
    assertEquals(accessElection.getElectionId(), electionId);
    Integer missingElectionId = electionDAO.getOpenElectionIdByReferenceId("accessReferenceId");
    assertNull(missingElectionId);
  }

  @Test
  public void testGetElectionIdsByReferenceIds() {
    String accessReferenceId1 = UUID.randomUUID().toString();
    String accessReferenceId2 = UUID.randomUUID().toString();
    Dataset dataset1 = createDataset();
    Dataset dataset2 = createDataset();
    Election accessElection1 = createDataAccessElection(accessReferenceId1, dataset1.getDataSetId());
    Election accessElection2 = createDataAccessElection(accessReferenceId2, dataset2.getDataSetId());

    List<Integer> electionIds = electionDAO.getElectionIdsByReferenceIds(List.of(accessReferenceId1, accessReferenceId2));
    assertEquals(2, electionIds.size());
    assertTrue(electionIds.contains(accessElection1.getElectionId()));
    assertTrue(electionIds.contains(accessElection2.getElectionId()));
    List<Integer> missingElectionIds = electionDAO.getElectionIdsByReferenceIds(List.of("1", "2", "3"));
    assertTrue(missingElectionIds.isEmpty());
  }

  @Test
  public void testFindDacForConsentElection() {
    Dac dac = createDac();
    Consent consent = createConsent();
    Dataset dataset = createDataset();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
    Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());

    Dac foundDac = electionDAO.findDacForElection(election.getElectionId());
    assertNotNull(foundDac);
    Assert.assertEquals(dac.getDacId(), foundDac.getDacId());
  }

  @Test
  public void testFindDacForConsentElectionWithNoAssociation() {
    Dac dac = createDac();
    Consent consent = createConsent();
    Dataset dataset = createDataset();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());

    Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());

    Dac foundDac = electionDAO.findDacForElection(election.getElectionId());
    assertNotNull(foundDac);
    Assert.assertEquals(dac.getDacId(), foundDac.getDacId());
  }

  @Test
  public void testFindDacForConsentElectionNotFound() {
    Consent consent = createConsent();
    Dataset dataset = createDataset();
    Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());

    Dac foundDac = electionDAO.findDacForElection(election.getElectionId());
    Assert.assertNull(foundDac);
  }

  @Test
  public void testFindElectionByDacId() {
    Dac dac = createDac();
    Consent consent = createConsent();
    Dataset dataset = createDataset();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());

    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
    Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());

    List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
    assertNotNull(foundElections);
    Assert.assertEquals(election.getElectionId(), foundElections.get(0).getElectionId());
  }

  @Test
  public void testFindElectionsByReferenceId() {
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
  public void testFindElectionsByReferenceIds() {
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
  public void testFindLastElectionByReferenceIdDatasetIdAndType() {
    // Goal is to create elections for a single dar across two datasets
    // One set of elections will be canceled
    // A new set will then be created
    // We should find ONLY the most recent elections with this method
    User user = createUser();
    String darCode = "DAR-1234567890";
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
    Dataset d1 = createDataset();
    Dataset d2 = createDataset();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d1.getDataSetId());
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d2.getDataSetId());
    // Create OPEN elections
    List<Integer> firstElectionIds = Stream
      .of(createElectionsForDarDataset(dar, d1), createElectionsForDarDataset(dar, d2))
      .flatMap(List::stream)
      .collect(Collectors.toList());
    // Cancel those elections
    firstElectionIds.forEach(id -> electionDAO.updateElectionById(id, ElectionStatus.CANCELED.getValue(), new Date(), true));
    // Create a new set of elections
    List<Integer> latestElectionIds = Stream
      .of(createElectionsForDarDataset(dar, d1), createElectionsForDarDataset(dar, d2))
      .flatMap(List::stream)
      .collect(Collectors.toList());

    Election latestAccessForD1 = electionDAO.findLastElectionByReferenceIdDatasetIdAndType(dar.getReferenceId(), d1.getDataSetId(), ElectionType.DATA_ACCESS.getValue());
    assertNotNull(latestAccessForD1);
    assertFalse(firstElectionIds.contains(latestAccessForD1.getElectionId()));
    assertTrue(latestElectionIds.contains(latestAccessForD1.getElectionId()));

    Election latestRPForD1 = electionDAO.findLastElectionByReferenceIdDatasetIdAndType(dar.getReferenceId(), d1.getDataSetId(), ElectionType.RP.getValue());
    assertNotNull(latestRPForD1);
    assertFalse(firstElectionIds.contains(latestRPForD1.getElectionId()));
    assertTrue(latestElectionIds.contains(latestRPForD1.getElectionId()));

    Election latestAccessForD2 = electionDAO.findLastElectionByReferenceIdDatasetIdAndType(dar.getReferenceId(), d2.getDataSetId(), ElectionType.DATA_ACCESS.getValue());
    assertNotNull(latestAccessForD2);
    assertFalse(firstElectionIds.contains(latestAccessForD2.getElectionId()));
    assertTrue(latestElectionIds.contains(latestAccessForD2.getElectionId()));

    Election latestRPForD2 = electionDAO.findLastElectionByReferenceIdDatasetIdAndType(dar.getReferenceId(), d2.getDataSetId(), ElectionType.RP.getValue());
    assertNotNull(latestRPForD2);
    assertFalse(firstElectionIds.contains(latestRPForD2.getElectionId()));
    assertTrue(latestElectionIds.contains(latestRPForD2.getElectionId()));
  }

  /**
   * Small helper method for `testFindLastElectionByReferenceIdDatasetIdAndType()`
   * Creates OPEN Access and RP elections for dar/dataset combination
   *
   * @param dar DataAccessRequest
   * @param d Dataset
   * @return List of created electionIds
   */
  private List<Integer> createElectionsForDarDataset(DataAccessRequest dar, Dataset d) {
    Election accessElection = createDataAccessElection(dar.getReferenceId(), d.getDataSetId());
    Election rpElection = createRPElection(dar.getReferenceId(), d.getDataSetId());
    electionDAO.insertAccessRP(accessElection.getElectionId(), rpElection.getElectionId());
    return List.of(accessElection.getElectionId(), rpElection.getElectionId());
  }


  @Test
  public void testDeleteElectionFromAccessRP() {
    Dac dac = createDac();
    Consent consent = createConsent();
    Dataset dataset = createDataset();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());

    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
    Election accessElection = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
    Election rpElection = createRPElection(consent.getConsentId(), dataset.getDataSetId());

    electionDAO.insertAccessRP(accessElection.getElectionId(), rpElection.getElectionId());

    assertEquals(rpElection.getElectionId(), electionDAO.findRPElectionByElectionAccessId(accessElection.getElectionId()));
    assertEquals(accessElection.getElectionId(), electionDAO.findAccessElectionByElectionRPId(rpElection.getElectionId()));

    // can delete using access election
    electionDAO.deleteElectionFromAccessRP(accessElection.getElectionId());

    assertNull(electionDAO.findRPElectionByElectionAccessId(accessElection.getElectionId()));
    assertNull(electionDAO.findAccessElectionByElectionRPId(rpElection.getElectionId()));

    electionDAO.insertAccessRP(accessElection.getElectionId(), rpElection.getElectionId());

    assertEquals(rpElection.getElectionId(), electionDAO.findRPElectionByElectionAccessId(accessElection.getElectionId()));
    assertEquals(accessElection.getElectionId(), electionDAO.findAccessElectionByElectionRPId(rpElection.getElectionId()));

    // or by using rp election
    electionDAO.deleteElectionFromAccessRP(rpElection.getElectionId());

    assertNull(electionDAO.findRPElectionByElectionAccessId(accessElection.getElectionId()));
    assertNull(electionDAO.findAccessElectionByElectionRPId(rpElection.getElectionId()));
  }

  @Test
  public void testFindElectionByDacIdWithNoAssociation() {
    Dac dac = createDac();
    Consent consent = createConsent();
    Dataset dataset = createDataset();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());

    Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());

    List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
    assertNotNull(foundElections);
    Assert.assertEquals(election.getElectionId(), foundElections.get(0).getElectionId());
  }

  @Test
  public void testFindElectionByDacIdNotFound() {
    Dac dac = createDac();
    Consent consent = createConsent();
    Dataset dataset = createDataset();
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
    createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());

    List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
    Assert.assertTrue(foundElections.isEmpty());
  }

  @Test
  public void testFindAccessElectionWithFinalVoteById() {
    User u = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    Dac dac = createDac();
    Consent c = createConsent();
    Dataset d = createDataset();
    datasetDAO.updateDatasetDacId(d.getDataSetId(), dac.getDacId());

    consentDAO.insertConsentAssociation(c.getConsentId(), ASSOCIATION_TYPE_TEST, d.getDataSetId());
    Election e = createDataAccessElection(c.getConsentId(), d.getDataSetId());
    Integer voteId = voteDAO.insertVote(u.getUserId(), e.getElectionId(), VoteType.FINAL.getValue());
    voteDAO.updateVote(true, "rationale", new Date(), voteId, false, e.getElectionId(), new Date(), false);
    Vote v = voteDAO.findVoteById(voteId);

    Election election = electionDAO.findElectionWithFinalVoteById(e.getElectionId());
    assertNotNull(election);
    assertEquals(e.getElectionId(), election.getElectionId());
    assertEquals(v.getVote(), election.getFinalVote());
  }

  @Test
  public void testRPFindElectionWithFinalVoteById() {
    User u = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    Dac dac = createDac();
    Consent c = createConsent();
    Dataset d = createDataset();
    datasetDAO.updateDatasetDacId(d.getDataSetId(), dac.getDacId());

    consentDAO.insertConsentAssociation(c.getConsentId(), ASSOCIATION_TYPE_TEST, d.getDataSetId());
    Election e = createRPElection(c.getConsentId(), d.getDataSetId());
    Vote v = createPopulatedChairpersonVote(u.getUserId(), e.getElectionId());

    Election election = electionDAO.findElectionWithFinalVoteById(e.getElectionId());
    assertNotNull(election);
    assertEquals(e.getElectionId(), election.getElectionId());
    assertEquals(v.getVote(), election.getFinalVote());
  }

  @Test
  public void testDatasetFindElectionWithFinalVoteById() {
    User u = createUserWithRole(UserRoles.DATAOWNER.getRoleId());
    Dac dac = createDac();
    Consent c = createConsent();
    Dataset d = createDataset();
    datasetDAO.updateDatasetDacId(d.getDataSetId(), dac.getDacId());

    consentDAO.insertConsentAssociation(c.getConsentId(), ASSOCIATION_TYPE_TEST, d.getDataSetId());
    Integer electionId = electionDAO.insertElection(
      ElectionType.DATA_SET.getValue(),
      ElectionStatus.OPEN.getValue(),
      new Date(),
      c.getConsentId(),
      d.getDataSetId());
    Election e = electionDAO.findElectionById(electionId);

    Integer voteId = voteDAO.insertVote(u.getUserId(), e.getElectionId(), VoteType.DATA_OWNER.getValue());
    voteDAO.updateVote(true, "rationale", new Date(), voteId, false, e.getElectionId(), new Date(), false);
    Vote v = voteDAO.findVoteById(voteId);

    Election election = electionDAO.findElectionWithFinalVoteById(e.getElectionId());
    assertNotNull(election);
    assertEquals(e.getElectionId(), election.getElectionId());
    assertEquals(v.getVote(), election.getFinalVote());
  }

  @Test
  public void testDULFindElectionWithFinalVoteById() {
    User u = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    Dac dac = createDac();
    Consent c = createConsent();
    Dataset d = createDataset();
    datasetDAO.updateDatasetDacId(d.getDataSetId(), dac.getDacId());

    consentDAO.insertConsentAssociation(c.getConsentId(), ASSOCIATION_TYPE_TEST, d.getDataSetId());
    Integer electionId = electionDAO.insertElection(
      ElectionType.TRANSLATE_DUL.getValue(),
      ElectionStatus.OPEN.getValue(),
      new Date(),
      c.getConsentId(),
      d.getDataSetId());
    Election e = electionDAO.findElectionById(electionId);
    Vote v = createPopulatedChairpersonVote(u.getUserId(), e.getElectionId());

    Election election = electionDAO.findElectionWithFinalVoteById(e.getElectionId());
    assertNotNull(election);
    assertEquals(e.getElectionId(), election.getElectionId());
    assertEquals(v.getVote(), election.getFinalVote());
  }

  @Test
  public void testFindElectionsByReferenceIdCase1() {
    DataAccessRequest dar = createDataAccessRequestV3();
    Dataset d = createDataset();
    createDataAccessElection(dar.getReferenceId(), d.getDataSetId());
    createRPElection(dar.getReferenceId(), d.getDataSetId());

    List<Election> elections = electionDAO.findElectionsByReferenceId(dar.getReferenceId());
    assertNotNull(elections);
    assertEquals(2, elections.size());
  }

  @Test
  public void testInsertExtendedElection() {
    Dac dac = createDac();
    Consent c = createConsent();
    Dataset d = createDataset();
    datasetDAO.updateDatasetDacId(d.getDataSetId(), dac.getDacId());

    consentDAO.insertConsentAssociation(c.getConsentId(), ASSOCIATION_TYPE_TEST, d.getDataSetId());
    Integer electionId = electionDAO.insertElection(
      ElectionType.DATA_ACCESS.getValue(),
      ElectionStatus.OPEN.getValue(),
      new Date(),
      c.getConsentId(),
      Boolean.TRUE,
      "dataUseLetter",
      "dulName",
      d.getDataSetId());
    Election e = electionDAO.findElectionById(electionId);
    Election election = electionDAO.findElectionWithFinalVoteById(e.getElectionId());
    assertNotNull(election);
    assertEquals(e.getElectionId(), election.getElectionId());
  }

  @Test
  public void testFindLastElectionsByReferenceIdsAndType() {
    DataAccessRequest dar = createDataAccessRequestV3();
    Dataset d = createDataset();
    electionDAO.insertElection(
      ElectionType.DATA_ACCESS.getValue(),
      ElectionStatus.OPEN.getValue(),
      new Date(),
      dar.getReferenceId(),
      Boolean.TRUE,
      "dataUseLetter",
      "dulName",
      d.getDataSetId());
    List<Election> elections =
        electionDAO.findLastElectionsByReferenceIdsAndType(
            Collections.singletonList(dar.getReferenceId()), ElectionType.DATA_ACCESS.getValue());
    assertNotNull(elections);
    assertFalse(elections.isEmpty());
    assertEquals(1, elections.size());
  }

  @Test
  public void testFindAllDacsForElectionIds() {
    Dac dac = createDac();
    String accessReferenceId = UUID.randomUUID().toString();
    Dataset dataset = createDataset();
    Integer datasetId = dataset.getDataSetId();
    Consent consent = createConsent();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    Integer electionId = electionDAO.insertElection(
      ElectionType.TRANSLATE_DUL.getValue(),
      ElectionStatus.OPEN.getValue(),
      new Date(),
      consent.getConsentId(),
      datasetId);
    Election dulElection = electionDAO.findElectionById(electionId);
    Election accessElection = createDataAccessElection(accessReferenceId, datasetId);
    electionDAO.insertAccessAndConsentElection(accessElection.getElectionId(), dulElection.getElectionId());

    List<Integer> electionIds = Collections.singletonList(accessElection.getElectionId());
    List<Dac> dacList = electionDAO.findAllDacsForElectionIds(electionIds);
    Dac dacRecord = dacList.get(0);
    assertEquals(1, dacList.size());
    assertEquals(dac.getName(), dacRecord.getName());
    assertEquals(dac.getDacId(), dacRecord.getDacId());
  }

  @Test
  public void testFindAllDacsForElectionIds_EmptyList() {
    List<Integer> electionIds = Collections.singletonList(10000);
    List<Dac> dacList = electionDAO.findAllDacsForElectionIds(electionIds);
    assertTrue(dacList.isEmpty());
  }

  @Test
  public void testFindLastElectionsByReferenceIds() {
    Dac dac = createDac();
    Consent consent = createConsent();
    Dataset dataset = createDataset();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());

    DataAccessRequest dar = createDataAccessRequestV3();

    String darReferenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), datasetId);
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());

    Election cancelledAccessElection = createDataAccessElection(darReferenceId, datasetId);
    Election cancelledRPElection = createRPElection(darReferenceId, datasetId);
    electionDAO.updateElectionById(
      cancelledAccessElection.getElectionId(), ElectionStatus.CANCELED.getValue(), new Date(), true);
    electionDAO.updateElectionById(
      cancelledRPElection.getElectionId(), ElectionStatus.CANCELED.getValue(), new Date(), true);

    Election prevClosedAccessElection = createDataAccessElection(darReferenceId, dataset.getDataSetId());
    Election prevClosedRPElection = createDataAccessElection(darReferenceId, datasetId);
    electionDAO.updateElectionById(
      prevClosedAccessElection.getElectionId(), ElectionStatus.CLOSED.getValue(), new Date(), true);
    electionDAO.updateElectionById(
      prevClosedRPElection.getElectionId(), ElectionStatus.CLOSED.getValue(), new Date(), true);

    Election recentClosedAccessElection = createDataAccessElection(darReferenceId, dataset.getDataSetId());
    Election recentClosedRPElection = createRPElection(darReferenceId, datasetId);
    electionDAO.updateElectionById(
      recentClosedAccessElection.getElectionId(), ElectionStatus.CLOSED.getValue(), new Date(), true);
    electionDAO.updateElectionById(
      recentClosedRPElection.getElectionId(), ElectionStatus.CLOSED.getValue(), new Date(), true);
    List<Election> elections =
      electionDAO.findLastElectionsByReferenceIds(Collections.singletonList(dar.referenceId));
    List<Integer> electionIds = elections.stream().map(Election::getElectionId).collect(Collectors.toList());
    assertFalse(elections.isEmpty());
    assertEquals(2, elections.size());
    assertTrue(electionIds.contains(recentClosedAccessElection.getElectionId()));
    assertTrue(electionIds.contains(recentClosedRPElection.getElectionId()));
  }

  @Test
  public void testFindLastElectionsByReferenceIds_EmptyList() {
    List<Election> elections =
      electionDAO.findLastElectionsByReferenceIds(Collections.singletonList(UUID.randomUUID().toString()));
    assertTrue(elections.isEmpty());
  }

  @Test
  public void testFindElectionsByVoteIdsAndType_DataAccess() {
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
  public void testFindElectionsByVoteIdsAndType_RP() {
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
  public void testFindElectionsWithCardHoldingUsersByElectionIds() {
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
    List<Election> elections = electionDAO.findElectionsWithCardHoldingUsersByElectionIds(electionIds);

    assertEquals(1, elections.size());
    assertEquals(elections.get(0).getElectionId(), lcElection.getElectionId());
  }

  @Test
  public void testFindOpenElectionsByReferenceIds() {
    DataAccessRequest dar = createDataAccessRequestV3();
    Dataset dataset = createDataset();
    String referenceId = dar.getReferenceId();
    int datasetId = dataset.getDataSetId();
    Election accessElection = createDataAccessElection(referenceId, datasetId);
    Election rpElection = createRPElection(referenceId, datasetId);

    List<Election> elections = electionDAO.findOpenElectionsByReferenceIds(List.of(dar.referenceId));
    assertEquals(2, elections.size());

    electionDAO.updateElectionStatus(List.of(accessElection.getElectionId(), rpElection.getElectionId()), ElectionStatus.CANCELED.getValue());
    List<Election> electionsV2 = electionDAO.findOpenElectionsByReferenceIds(List.of(dar.referenceId));
    assertEquals(0, electionsV2.size());
  }

  @Test
  public void testDeleteByReferenceId() {
    Dataset dataset = createDataset();
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();
    Election e = createDataAccessElection(referenceId, datasetId);

    List<Election> elections = electionDAO.findOpenElectionsByReferenceIds(List.of(dar.referenceId));
    assertEquals(1, elections.size());

    electionDAO.deleteElectionById(e.getElectionId());

    elections = electionDAO.findOpenElectionsByReferenceIds(List.of(dar.referenceId));
    assertEquals(0, elections.size());

  }

  @Test
  public void testDeleteByReferenceIds() {
    Dataset dataset = createDataset();
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();
    Election accessElection = createDataAccessElection(referenceId, datasetId);
    Election rpElection = createRPElection(referenceId, datasetId);

    List<Election> elections = electionDAO.findOpenElectionsByReferenceIds(List.of(dar.referenceId));
    assertEquals(2, elections.size());

    electionDAO.deleteElectionsByIds(List.of(accessElection.getElectionId(), rpElection.getElectionId()));

    elections = electionDAO.findOpenElectionsByReferenceIds(List.of(dar.referenceId));
    assertEquals(0, elections.size());
  }


  @Test
  public void testFindElectionsWithFinalVoteByReferenceId() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();

    // create elections with final vote
    Election accessElection = createDataAccessElection(referenceId, datasetId);
    Election rpElection = createRPElection(referenceId, datasetId);
    createFinalVote(user.getUserId(), accessElection.getElectionId());
    createFinalVote(user.getUserId(), rpElection.getElectionId());

    // create irrelevant elections that should not be returned
    createDataAccessElection(referenceId, datasetId);
    createDataAccessElection(referenceId, datasetId);
    createDataAccessElection(referenceId, datasetId);
    createRPElection(referenceId, datasetId);
    createRPElection(referenceId, datasetId);
    createRPElection(referenceId, datasetId);

    List<Election> elections = electionDAO.findElectionsWithFinalVoteByReferenceId(referenceId);

    assertEquals(2, elections.size());
    assertTrue(elections.contains(accessElection));
    assertTrue(elections.contains(rpElection));
  }

  @Test
  public void testInsertAndFindElection() {

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

    assertEquals(
            ElectionType.DATA_ACCESS.getValue(),
            e.getElectionType());
    assertEquals(
            ElectionStatus.OPEN.getValue(),
            e.getStatus());
    assertNotNull(e.getCreateDate());
    assertEquals(referenceId, e.getReferenceId());
    assertEquals(datasetId, e.getDataSetId());

  }

  @Test
  public void testUpdateElectionById() {
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
  public void testUpdateElectionById_FinalAccessVote() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();

    Election before = createDataAccessElection(referenceId, datasetId);

    assertEquals(ElectionStatus.OPEN.getValue(), before.getStatus());
    assertNull(before.getLastUpdate());
    assertEquals(null, before.getFinalAccessVote());

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
  public void testUpdateElectionStatus() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();

    Election e1 = createDataAccessElection(referenceId, datasetId);
    Election e2 = createRPElection(referenceId, datasetId);
    Election e3 = createDataAccessElection(referenceId, datasetId);

    assertEquals(ElectionStatus.OPEN.getValue(), e1.getStatus());
    assertEquals(ElectionStatus.OPEN.getValue(), e2.getStatus());
    assertEquals(ElectionStatus.OPEN.getValue(), e3.getStatus());

    electionDAO.updateElectionStatus(
            List.of(e1.getElectionId(), e2.getElectionId(), e3.getElectionId()),
            ElectionStatus.FINAL.getValue());

    e1 = electionDAO.findElectionById(e1.getElectionId());
    e2 = electionDAO.findElectionById(e2.getElectionId());
    e3 = electionDAO.findElectionById(e3.getElectionId());


    assertEquals(ElectionStatus.FINAL.getValue(), e1.getStatus());
    assertEquals(ElectionStatus.FINAL.getValue(), e2.getStatus());
    assertEquals(ElectionStatus.FINAL.getValue(), e3.getStatus());
  }

  @Test
  public void testGetOpenElectionWithFinalVoteByReferenceIdAndType() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();

    // create elections with final vote
    Election accessElection = createDataAccessElection(referenceId, datasetId);
    Election rpElection = createRPElection(referenceId, datasetId);
    createFinalVote(user.getUserId(), accessElection.getElectionId());
    createFinalVote(user.getUserId(), rpElection.getElectionId());

    Election canceled = createDataAccessElection(referenceId, datasetId);
    createFinalVote(user.getUserId(), canceled.getElectionId());

    electionDAO.updateElectionStatus(
            List.of(canceled.getElectionId()),
            ElectionStatus.CANCELED.getValue());

    // create irrelevant elections that should not be returned
    createDataAccessElection(referenceId, datasetId);
    createRPElection(referenceId, datasetId);

    // returns data access even if rp exists
    Election returned =
            electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(
              referenceId,
              ElectionType.DATA_ACCESS.getValue());

    assertEquals(accessElection.getElectionId(),
            returned.getElectionId());

    // returns rp even if data access exists
    returned =
            electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(
                    referenceId,
                    ElectionType.RP.getValue());

    assertEquals(rpElection.getElectionId(),
            returned.getElectionId());
  }

  @Test
  public void testGetElectionWithFinalVoteByReferenceIdAndType() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();

    // create elections with final vote
    Election accessElection = createDataAccessElection(referenceId, datasetId);
    Election rpElection = createRPElection(referenceId, datasetId);
    createFinalVote(user.getUserId(), accessElection.getElectionId());
    createFinalVote(user.getUserId(), rpElection.getElectionId());
    // does not use status, should return given any status
    electionDAO.updateElectionStatus(
            List.of(accessElection.getElectionId()),
            ElectionStatus.CANCELED.getValue());


    // create irrelevant elections that should not be returned
    createDataAccessElection(referenceId, datasetId);
    createRPElection(referenceId, datasetId);

    // returns data access even if rp exists
    Election returned =
            electionDAO.getElectionWithFinalVoteByReferenceIdAndType(
                    referenceId,
                    ElectionType.DATA_ACCESS.getValue());

    assertEquals(accessElection.getElectionId(),
            returned.getElectionId());

    // returns rp even if data access exists
    returned =
            electionDAO.getElectionWithFinalVoteByReferenceIdAndType(
                    referenceId,
                    ElectionType.RP.getValue());

    assertEquals(rpElection.getElectionId(),
            returned.getElectionId());
  }

  @Test
  public void testFindElectionWithFinalVoteById() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();

    Election e = createDataAccessElection(referenceId, datasetId);

    Election returned =
            electionDAO.findElectionWithFinalVoteById(
                    e.getElectionId());

    assertNull(returned);

    createFinalVote(user.getUserId(), e.getElectionId());

    returned =
            electionDAO.findElectionWithFinalVoteById(
                    e.getElectionId());

    assertEquals(e.getElectionId(),
            returned.getElectionId());
  }

  @Test
  public void testFindElectionByVoteId() {
    Dac dac = createDac();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
    DataAccessRequest dar = createDataAccessRequestV3();
    String referenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();

    Election e = createDataAccessElection(referenceId, datasetId);
    Vote v = createFinalVote(user.getUserId(), e.getElectionId());

    Election returned = electionDAO.findElectionByVoteId(v.getVoteId());

    assertEquals(e.getElectionId(), returned.getElectionId());
  }

  @Test
  public void testFindElectionsWithFinalVoteByTypeAndStatus() {

  }

  @Test
  public void testFindLastElectionsWithFinalVoteByType() {

  }

  @Test
  public void testFindLastDataAccessElectionsWithFinalVoteByStatus() {

  }

  @Test
  public void testFindOpenLastElectionsByTypeAndFinalAccessVoteForChairPerson() {

  }

  @Test
  public void testFindTotalElectionsByTypeStatusAndVote() {

  }

  @Test
  public void testVerifyOpenElections() {

  }

  @Test
  public void testFindLastElectionsWithFinalVoteByReferenceIdsAndType() {

  }

  @Test
  public void testFindLastElectionsByReferenceIdAndType() {

  }

  @Test
  public void testFindLastElectionByReferenceIdAndStatus() {

  }

  @Test
  public void testFindLastElectionsWithFinalVoteByReferenceIdsTypeAndStatus() {

  }

  @Test
  public void testFindLastElectionByReferenceIdAndType() {

  }

  @Test
  public void testFindRPElectionByElectionAccessId() {

  }

  @Test
  public void testInsertAccessRp() {

  }

  @Test
  public void testInsertAccessAndConsentElection() {

  }

  @Test
  public void testGetElectionConsentIdByDARElectionId() {

  }

  @Test
  public void testDeleteAccessRP() {

  }

  @Test
  public void testDeleteElectionsFromAccessRPs() {

  }

  @Test
  public void testFindAccessElectionByElectionRPId() {

  }

  @Test
  public void testFindElectionsByIds() {

  }

  @Test
  public void testFindElectionById() {

  }

  @Test
  public void testGetOpenElectionByReferenceIdAndDataSet() {

  }


  @Test
  public void testArchiveElectionById() {

  }

  @Test
  public void testFindDataAccessClosedElectionsByFinalResult() {

  }

  @Test
  public void testFindApprovalAccessElectionDate() {

  }

  @Test
  public void testFindDacForElection() {

  }

  @Test
  public void testFindOpenElectionsByDacId() {

  }



}
