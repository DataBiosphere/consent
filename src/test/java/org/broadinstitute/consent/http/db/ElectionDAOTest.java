package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Assert;
import org.junit.Test;

public class ElectionDAOTest extends DAOTestHelper {

  @Test
  public void testFindRpAccessElectionIdPairs() {
    String accessReferenceId = UUID.randomUUID().toString();
    String rpReferenceId = UUID.randomUUID().toString();
    DataSet dataset = createDataset();
    Election accessElection = createAccessElection(accessReferenceId, dataset.getDataSetId());
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
    DataSet dataset = createDataset();
    createAssociation(consent.getConsentId(), dataset.getDataSetId());
    Election election = createAccessElection(consent.getConsentId(), dataset.getDataSetId());

    Dac foundDac = electionDAO.findDacForElection(election.getElectionId());
    assertNotNull(foundDac);
    Assert.assertEquals(dac.getDacId(), foundDac.getDacId());
  }

  @Test
  public void testFindDacForConsentElectionWithNoAssociation() {
    Dac dac = createDac();
    Consent consent = createConsent(dac.getDacId());
    DataSet dataset = createDataset();
    Election election = createAccessElection(consent.getConsentId(), dataset.getDataSetId());

    Dac foundDac = electionDAO.findDacForElection(election.getElectionId());
    assertNotNull(foundDac);
    Assert.assertEquals(dac.getDacId(), foundDac.getDacId());
  }

  @Test
  public void testFindDacForConsentElectionNotFound() {
    Consent consent = createConsent(null);
    DataSet dataset = createDataset();
    Election election = createAccessElection(consent.getConsentId(), dataset.getDataSetId());

    Dac foundDac = electionDAO.findDacForElection(election.getElectionId());
    Assert.assertNull(foundDac);
  }

  @Test
  public void testFindElectionByDacId() {
    Dac dac = createDac();
    Consent consent = createConsent(dac.getDacId());
    DataSet dataset = createDataset();
    createAssociation(consent.getConsentId(), dataset.getDataSetId());
    Election election = createAccessElection(consent.getConsentId(), dataset.getDataSetId());

    List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
    assertNotNull(foundElections);
    Assert.assertEquals(election.getElectionId(), foundElections.get(0).getElectionId());
  }

  @Test
  public void testFindElectionByDacIdWithNoAssociation() {
    Dac dac = createDac();
    Consent consent = createConsent(dac.getDacId());
    DataSet dataset = createDataset();
    Election election = createAccessElection(consent.getConsentId(), dataset.getDataSetId());

    List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
    assertNotNull(foundElections);
    Assert.assertEquals(election.getElectionId(), foundElections.get(0).getElectionId());
  }

  @Test
  public void testFindElectionByDacIdNotFound() {
    Dac dac = createDac();
    Consent consent = createConsent(null);
    DataSet dataset = createDataset();
    createAssociation(consent.getConsentId(), dataset.getDataSetId());
    createAccessElection(consent.getConsentId(), dataset.getDataSetId());

    List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
    Assert.assertTrue(foundElections.isEmpty());
  }

  @Test
  public void testFindAccessElectionWithFinalVoteById() {
    User u = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    Dac dac = createDac();
    Consent c = createConsent(dac.getDacId());
    DataSet d = createDataset();
    createAssociation(c.getConsentId(), d.getDataSetId());
    Election e = createAccessElection(c.getConsentId(), d.getDataSetId());
    Vote v = createPopulatedFinalVote(u.getDacUserId(), e.getElectionId());

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
    DataSet d = createDataset();
    createAssociation(c.getConsentId(), d.getDataSetId());
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
    DataSet d = createDataset();
    createAssociation(c.getConsentId(), d.getDataSetId());
    Election e = createDatasetElection(c.getConsentId(), d.getDataSetId());
    Vote v = createPopulatedDataOwnerVote(u.getDacUserId(), e.getElectionId());

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
    DataSet d = createDataset();
    createAssociation(c.getConsentId(), d.getDataSetId());
    Election e = createDULElection(c.getConsentId(), d.getDataSetId());
    Vote v = createPopulatedChairpersonVote(u.getDacUserId(), e.getElectionId());

    Election election = electionDAO.findElectionWithFinalVoteById(e.getElectionId());
    assertNotNull(election);
    assertEquals(e.getElectionId(), election.getElectionId());
    assertEquals(v.getVote(), election.getFinalVote());
  }

  @Test
  public void testFindElectionsByReferenceIdCase1() {
    DataAccessRequest dar = createDataAccessRequestV2();
    DataSet d = createDataset();
    createAccessElection(dar.getReferenceId(), d.getDataSetId());
    createRPElection(dar.getReferenceId(), d.getDataSetId());

    List<Election> elections = electionDAO.findElectionsByReferenceId(dar.getReferenceId());
    assertNotNull(elections);
    assertEquals(2, elections.size());
  }

  @Test
  public void testInsertExtendedElection() {
    Dac dac = createDac();
    Consent c = createConsent(dac.getDacId());
    DataSet d = createDataset();
    createAssociation(c.getConsentId(), d.getDataSetId());
    Election e = createExtendedElection(c.getConsentId(), d.getDataSetId());
    Election election = electionDAO.findElectionWithFinalVoteById(e.getElectionId());
    assertNotNull(election);
    assertEquals(e.getElectionId(), election.getElectionId());
  }

  @Test
  public void testFindLastElectionsByReferenceIdsAndType() {
    DataAccessRequest dar = createDataAccessRequestV2();
    DataSet d = createDataset();
    createExtendedElection(dar.getReferenceId(), d.getDataSetId());
    List<Election> elections =
        electionDAO.findLastElectionsByReferenceIdsAndType(
            Collections.singletonList(dar.getReferenceId()), ElectionType.DATA_ACCESS.getValue());
    assertNotNull(elections);
    assertFalse(elections.isEmpty());
    assertEquals(1, elections.size());
  }
}
