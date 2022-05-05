package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Match;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MatchDAOTest extends DAOTestHelper {

  @Test
  public void testFindMatchesByConsentId() {
    Match m = createMatch();

    List<Match> matches = matchDAO.findMatchesByConsentId(m.getConsent());
    assertFalse(matches.isEmpty());
    Match found = matches.get(0);
    assertEquals(found.getId(), m.getId());
    assertEquals(found.getPurpose(), m.getPurpose());
    assertEquals(found.getConsent(), m.getConsent());
    assertEquals(found.getFailed(), m.getFailed());
    assertEquals(found.getMatch(), m.getMatch());
  }

  @Test
  public void testFindMatchesByPurposeId() {
    Match m = createMatch();

    List<Match> matches = matchDAO.findMatchesByPurposeId(m.getPurpose());
    assertFalse(matches.isEmpty());
    Match found = matches.get(0);
    assertEquals(found.getId(), m.getId());
    assertEquals(found.getPurpose(), m.getPurpose());
    assertEquals(found.getConsent(), m.getConsent());
    assertEquals(found.getFailed(), m.getFailed());
    assertEquals(found.getMatch(), m.getMatch());
  }

  @Test
  public void testInsertAll() {
    String consentId = createConsent(createDac().getDacId()).getConsentId();
    List<Match> matches = new ArrayList<>();
    IntStream
            .range(1, RandomUtils.nextInt(5, 10))
            .forEach(i -> matches.add(makeMockMatch(consentId)));

    matchDAO.insertAll(matches);
    List<Match> foundMatches = matchDAO.findMatchesByConsentId(consentId);
    assertFalse(foundMatches.isEmpty());
    assertEquals(matches.size(), foundMatches.size());
  }

  private Match makeMockMatch(String consentId) {
    Match match = new Match();
    match.setConsent(consentId);
    match.setPurpose(UUID.randomUUID().toString());
    match.setFailed(false);
    match.setCreateDate(new Date());
    match.setMatch(RandomUtils.nextBoolean());
    return match;
  }

  @Test
  public void testUpdateMatch() {
    Match m = createMatch();

    matchDAO.updateMatch(
            m.getId(),
            true,
            m.getConsent(),
            m.getPurpose(),
            false);
    Match found = matchDAO.findMatchById(m.getId());
    assertTrue(found.getMatch());
    assertFalse(found.getFailed());
  }

  @Test
  public void testDeleteMatchesByConsentId() {
    Match m = createMatch();

    matchDAO.deleteMatchesByConsentId(m.getConsent());
    List<Match> matches = matchDAO.findMatchesByConsentId(m.getConsent());
    assertTrue(matches.isEmpty());
  }

  @Test
  public void testDeleteMatchesByPurposeId() {
    Match m = createMatch();

    matchDAO.deleteMatchesByPurposeId(m.getPurpose());
    List<Match> matches = matchDAO.findMatchesByPurposeId(m.getConsent());
    assertTrue(matches.isEmpty());
  }

  @Test
  public void testCountMatchesByResult() {
    Match m1 = createMatch();
    Match m2 = createMatch();

    Integer count1 = matchDAO.countMatchesByResult(m1.getMatch());
    assertTrue(count1 >= 1);
    Integer count2 = matchDAO.countMatchesByResult(m2.getMatch());
    assertTrue(count2 >= 1);
  }

  @Test
  public void testFindMatchesForPurposeIds() {
    Dataset dataset = createDataset();
    //query should pull the latest election for a given reference id
    //creating two access elections with the same reference id and datasetid to test that condition
    String darReferenceId = UUID.randomUUID().toString();
    Election targetElection = createAccessElection(
      darReferenceId, dataset.getDataSetId()
    );
    Election ignoredAccessElection = createAccessElection(
      UUID.randomUUID().toString(), dataset.getDataSetId()
    );

    //Generate RP election to test that the query only references DataAccess elections
    Election rpElection = createRPElection(UUID.randomUUID().toString(), dataset.getDataSetId());
    Dac dac = createDac();
    String consentId = createConsent(dac.getDacId()).getConsentId();

    //This match represents the match record generated for the target election
    matchDAO.insertMatch(consentId, darReferenceId, true, false, new Date());

    // This match represents the match record generated for the ignored access election
    matchDAO.insertMatch(consentId, ignoredAccessElection.getReferenceId(), false, false, new Date());

    // This match is never created under consent's workflow (unless the cause is a bug)
    // This is included simply to test the DataAccess conditional on the INNER JOIN statement
    matchDAO.insertMatch(consentId, rpElection.getReferenceId(), false, false, new Date());

    List<Match> matchResults = matchDAO.findMatchesForPurposeIds(List.of(darReferenceId));
    assertEquals(1, matchResults.size());
    Match result = matchResults.get(0);
    assertEquals(targetElection.getReferenceId(), result.getPurpose());
  }

  @Test
  public void testFindMatchesForPurposeIds_NegativeTest() {
    Dataset dataset = createDataset();
    String darReferenceId = UUID.randomUUID().toString();

    //Generate access election for test
    Election accessElection = createAccessElection(
        UUID.randomUUID().toString(), dataset.getDataSetId());

    //Generate RP election for test
    Election rpElection = createRPElection(darReferenceId, dataset.getDataSetId());
    Dac dac = createDac();
    String consentId = createConsent(dac.getDacId()).getConsentId();

    // This match represents the match record generated for the access election
    matchDAO.insertMatch(consentId, accessElection.getReferenceId(), true, false, new Date());

    // This match is never created under consent's workflow (unless the cause is a bug)
    // This is included simply to test the DataAccess conditional on the INNER JOIN statement
    matchDAO.insertMatch(consentId, rpElection.getReferenceId(), false, false, new Date());

    //Negative testing means we'll feed the query a reference id that isn't tied to a DataAccess election
    //Again, a match like this usually isn't generated in a normal workflow unless bug occurs, but having the 'DataAccess' condition is a nice safety net
    List<Match> matchResults = matchDAO.findMatchesForPurposeIds(List.of(darReferenceId));
    assertTrue(matchResults.isEmpty());
  }
}
