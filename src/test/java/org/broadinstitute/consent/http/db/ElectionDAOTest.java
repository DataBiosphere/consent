package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
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
import org.junit.Assert;
import org.junit.Test;

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
  public void testFindRpAccessElectionIdPairs() {
    String accessReferenceId = UUID.randomUUID().toString();
    String rpReferenceId = UUID.randomUUID().toString();
    Dataset dataset = createDataset();
    Election accessElection = createDataAccessElection(accessReferenceId, dataset.getDataSetId());
    Election rpElection = createRPElection(rpReferenceId, dataset.getDataSetId());
    electionDAO.insertAccessRP(accessElection.getElectionId(), rpElection.getElectionId());
    List<Integer> electionIds =
        Arrays.asList(accessElection.getElectionId(), rpElection.getElectionId());

    List<Pair<Integer, Integer>> rpAccessElectionIdPairs =
        electionDAO.findRpAccessElectionIdPairs(electionIds);
    assertNotNull(rpAccessElectionIdPairs);
    assertFalse(rpAccessElectionIdPairs.isEmpty());
    assertEquals(1, rpAccessElectionIdPairs.size());
    assertEquals(rpElection.getElectionId(), rpAccessElectionIdPairs.get(0).getKey());
    assertEquals(accessElection.getElectionId(), rpAccessElectionIdPairs.get(0).getValue());
  }

  @Test
  public void testFindDacForConsentElection() {
    Dac dac = createDac();
    Consent consent = createConsent(dac.getDacId());
    Dataset dataset = createDataset();
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
    Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());

    Dac foundDac = electionDAO.findDacForElection(election.getElectionId());
    assertNotNull(foundDac);
    Assert.assertEquals(dac.getDacId(), foundDac.getDacId());
  }

  @Test
  public void testFindDacForConsentElectionWithNoAssociation() {
    Dac dac = createDac();
    Consent consent = createConsent(dac.getDacId());
    Dataset dataset = createDataset();
    Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());

    Dac foundDac = electionDAO.findDacForElection(election.getElectionId());
    assertNotNull(foundDac);
    Assert.assertEquals(dac.getDacId(), foundDac.getDacId());
  }

  @Test
  public void testFindDacForConsentElectionNotFound() {
    Consent consent = createConsent(null);
    Dataset dataset = createDataset();
    Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());

    Dac foundDac = electionDAO.findDacForElection(election.getElectionId());
    Assert.assertNull(foundDac);
  }

  @Test
  public void testFindElectionByDacId() {
    Dac dac = createDac();
    Consent consent = createConsent(dac.getDacId());
    Dataset dataset = createDataset();
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
    Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());

    List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
    assertNotNull(foundElections);
    Assert.assertEquals(election.getElectionId(), foundElections.get(0).getElectionId());
  }

  @Test
  public void testFindElectionByDacIdWithNoAssociation() {
    Dac dac = createDac();
    Consent consent = createConsent(dac.getDacId());
    Dataset dataset = createDataset();
    Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());

    List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
    assertNotNull(foundElections);
    Assert.assertEquals(election.getElectionId(), foundElections.get(0).getElectionId());
  }

  @Test
  public void testFindElectionByDacIdNotFound() {
    Dac dac = createDac();
    Consent consent = createConsent(null);
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
    Consent c = createConsent(dac.getDacId());
    Dataset d = createDataset();
    consentDAO.insertConsentAssociation(c.getConsentId(), ASSOCIATION_TYPE_TEST, d.getDataSetId());
    Election e = createDataAccessElection(c.getConsentId(), d.getDataSetId());
    Integer voteId = voteDAO.insertVote(u.getDacUserId(), e.getElectionId(), VoteType.FINAL.getValue());
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
    Consent c = createConsent(dac.getDacId());
    Dataset d = createDataset();
    consentDAO.insertConsentAssociation(c.getConsentId(), ASSOCIATION_TYPE_TEST, d.getDataSetId());
    Election e = createRPElection(c.getConsentId(), d.getDataSetId());
    Vote v = createPopulatedChairpersonVote(u.getDacUserId(), e.getElectionId());

    Election election = electionDAO.findElectionWithFinalVoteById(e.getElectionId());
    assertNotNull(election);
    assertEquals(e.getElectionId(), election.getElectionId());
    assertEquals(v.getVote(), election.getFinalVote());
  }

  @Test
  public void testDatasetFindElectionWithFinalVoteById() {
    User u = createUserWithRole(UserRoles.DATAOWNER.getRoleId());
    Dac dac = createDac();
    Consent c = createConsent(dac.getDacId());
    Dataset d = createDataset();
    consentDAO.insertConsentAssociation(c.getConsentId(), ASSOCIATION_TYPE_TEST, d.getDataSetId());
    Integer electionId = electionDAO.insertElection(
      ElectionType.DATA_SET.getValue(),
      ElectionStatus.OPEN.getValue(),
      new Date(),
      c.getConsentId(),
      d.getDataSetId());
    Election e = electionDAO.findElectionById(electionId);

    Integer voteId = voteDAO.insertVote(u.getDacUserId(), e.getElectionId(), VoteType.DATA_OWNER.getValue());
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
    Consent c = createConsent(dac.getDacId());
    Dataset d = createDataset();
    consentDAO.insertConsentAssociation(c.getConsentId(), ASSOCIATION_TYPE_TEST, d.getDataSetId());
    Integer electionId = electionDAO.insertElection(
      ElectionType.TRANSLATE_DUL.getValue(),
      ElectionStatus.OPEN.getValue(),
      new Date(),
      c.getConsentId(),
      d.getDataSetId());
    Election e = electionDAO.findElectionById(electionId);
    Vote v = createPopulatedChairpersonVote(u.getDacUserId(), e.getElectionId());

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
    Consent c = createConsent(dac.getDacId());
    Dataset d = createDataset();
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
    Consent consent = createConsent(dac.getDacId());
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
    Consent consent = createConsent(dac.getDacId());
    Dataset dataset = createDataset();
    DataAccessRequest dar = createDataAccessRequestV3();

    String darReferenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();
    dar.getData().setDatasetIds(Collections.singletonList(datasetId));
    dataAccessRequestDAO.updateDataByReferenceId(darReferenceId, dar.getData());
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
    List<Integer> electionIds = elections.stream().map(e -> e.getElectionId()).collect(Collectors.toList());
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
    int userId = user.getDacUserId();
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
    int userId = user.getDacUserId();
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
    DataAccessRequest lcDAR = createDataAccessRequestWithUserIdV3(lcUser.getDacUserId());
    DataAccessRequest nonLCDAR = createDataAccessRequestWithUserIdV3(nonLCUser.getDacUserId());
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
}
