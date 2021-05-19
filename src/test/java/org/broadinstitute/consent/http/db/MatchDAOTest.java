package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Match;
import org.junit.Test;

import java.util.List;

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
  public void testMpdateMatch() {
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
}
