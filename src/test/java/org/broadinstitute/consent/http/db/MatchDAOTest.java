package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
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
    Dac dac = createDac();
    Consent consent = createConsent(dac.getDacId());
    List<Match> matches = new ArrayList<>();
    IntStream
            .range(1, RandomUtils.nextInt(5, 10))
            .forEach(i -> matches.add(makeMockMatch(consent)));

    matchDAO.insertAll(matches);
    List<Match> foundMatches = matchDAO.findMatchesByConsentId(consent.getConsentId());
    assertFalse(foundMatches.isEmpty());
    assertEquals(matches.size(), foundMatches.size());
  }

  private Match makeMockMatch(Consent consent) {
    Match match = new Match();
    match.setConsent(consent.getConsentId());
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
}
