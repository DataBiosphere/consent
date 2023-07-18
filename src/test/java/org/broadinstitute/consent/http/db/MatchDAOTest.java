package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.MatchAlgorithm;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

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
    String consentId = createConsent().getConsentId();
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
    match.setAlgorithmVersion(MatchAlgorithm.V1.getVersion());
    return match;
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
  public void testFindMatchesForLatestDataAccessElectionsByPurposeIds() {
    Dataset dataset = createDataset();
    //query should pull the latest election for a given reference id
    //creating two access elections with the same reference id and datasetid to test that condition
    String darReferenceId = UUID.randomUUID().toString();
    Election targetElection = createDataAccessElection(
        darReferenceId, dataset.getDataSetId()
    );
    Election ignoredAccessElection = createDataAccessElection(
        UUID.randomUUID().toString(), dataset.getDataSetId()
    );

    //Generate RP election to test that the query only references DataAccess elections
    Election rpElection = createRPElection(UUID.randomUUID().toString(), dataset.getDataSetId());
    String consentId = createConsent().getConsentId();

    //This match represents the match record generated for the target election
    matchDAO.insertMatch(consentId, darReferenceId, true, false, new Date());

    // This match represents the match record generated for the ignored access election
    matchDAO.insertMatch(consentId, ignoredAccessElection.getReferenceId(), false, false,
        new Date());

    // This match is never created under consent's workflow (unless the cause is a bug)
    // This is included simply to test the DataAccess conditional on the INNER JOIN statement
    matchDAO.insertMatch(consentId, rpElection.getReferenceId(), false, false, new Date());

    List<Match> matchResults = matchDAO.findMatchesForLatestDataAccessElectionsByPurposeIds(
        List.of(darReferenceId));
    assertEquals(1, matchResults.size());
    Match result = matchResults.get(0);
    assertEquals(targetElection.getReferenceId(), result.getPurpose());
  }

  @Test
  public void testFindMatchesForLatestDataAccessElectionsByPurposeIds_NegativeTest() {
    Dataset dataset = createDataset();
    String darReferenceId = UUID.randomUUID().toString();

    //Generate access election for test
    Election accessElection = createDataAccessElection(
        UUID.randomUUID().toString(), dataset.getDataSetId());

    //Generate RP election for test
    Election rpElection = createRPElection(darReferenceId, dataset.getDataSetId());
    String consentId = createConsent().getConsentId();

    // This match represents the match record generated for the access election
    matchDAO.insertMatch(consentId, accessElection.getReferenceId(), true, false, new Date());

    // This match is never created under consent's workflow (unless the cause is a bug)
    // This is included simply to test the DataAccess conditional on the INNER JOIN statement
    matchDAO.insertMatch(consentId, rpElection.getReferenceId(), false, false, new Date());

    //Negative testing means we'll feed the query a reference id that isn't tied to a DataAccess election
    //Again, a match like this usually isn't generated in a normal workflow unless bug occurs, but having the 'DataAccess' condition is a nice safety net
    List<Match> matchResults = matchDAO.findMatchesForLatestDataAccessElectionsByPurposeIds(
        List.of(darReferenceId));
    assertTrue(matchResults.isEmpty());
  }

  @Test
  public void testFindMatchByPurposeIdAndConsentId() {
    Match match = makeMockMatch(UUID.randomUUID().toString());
    matchDAO.insertAll(List.of(match));
    Match foundMatch = matchDAO.findMatchByPurposeIdAndConsentId(match.getPurpose(),
        match.getConsent());
    assertNotNull(foundMatch);
  }

  @Test
  public void testFindMatchById() {
    Match match = makeMockMatch(UUID.randomUUID().toString());
    Integer matchId = matchDAO.insertMatch(
        match.getConsent(),
        match.getPurpose(),
        match.getMatch(),
        match.getFailed(),
        match.getCreateDate(),
        match.getAlgorithmVersion());
    Match foundMatch = matchDAO.findMatchById(matchId);
    assertNotNull(foundMatch);
  }

  @Test
  public void testInsertFailureReason() {
    Match match = makeMockMatch(UUID.randomUUID().toString());
    match.setMatch(false);
    match.setAlgorithmVersion(MatchAlgorithm.V3.getVersion());
    match.addFailureReason(RandomStringUtils.randomAlphabetic(100));
    match.addFailureReason(RandomStringUtils.randomAlphabetic(100));
    Integer matchId = matchDAO.insertMatch(
        match.getConsent(),
        match.getPurpose(),
        match.getMatch(),
        match.getFailed(),
        match.getCreateDate(),
        match.getAlgorithmVersion());
    match.getFailureReasons().forEach(f -> matchDAO.insertFailureReason(matchId, f));
    Match foundMatch = matchDAO.findMatchById(matchId);
    assertNotNull(foundMatch);
    assertEquals(match.getFailureReasons().size(),
        foundMatch.getFailureReasons().size());
  }

  @Test
  public void testDeleteFailureReasonsByConsentIds() {
    Match match = makeMockMatch(UUID.randomUUID().toString());
    match.setMatch(false);
    match.setAlgorithmVersion(MatchAlgorithm.V3.getVersion());
    match.addFailureReason(RandomStringUtils.randomAlphabetic(100));
    match.addFailureReason(RandomStringUtils.randomAlphabetic(100));
    Integer matchId = matchDAO.insertMatch(
        match.getConsent(),
        match.getPurpose(),
        match.getMatch(),
        match.getFailed(),
        match.getCreateDate(),
        match.getAlgorithmVersion());
    match.getFailureReasons().forEach(f -> matchDAO.insertFailureReason(matchId, f));
    matchDAO.deleteFailureReasonsByConsentIds(List.of(match.getConsent()));
    Match foundMatch = matchDAO.findMatchById(matchId);
    assertNotNull(foundMatch);
    assertEquals(0, foundMatch.getFailureReasons().size());
  }

  @Test
  public void testDeleteFailureReasonsByPurposeIds() {
    Match match = makeMockMatch(UUID.randomUUID().toString());
    match.setMatch(false);
    match.setAlgorithmVersion(MatchAlgorithm.V3.getVersion());
    match.addFailureReason(RandomStringUtils.randomAlphabetic(100));
    match.addFailureReason(RandomStringUtils.randomAlphabetic(100));
    Integer matchId = matchDAO.insertMatch(
        match.getConsent(),
        match.getPurpose(),
        match.getMatch(),
        match.getFailed(),
        match.getCreateDate(),
        match.getAlgorithmVersion());
    match.getFailureReasons().forEach(f -> matchDAO.insertFailureReason(matchId, f));
    matchDAO.deleteFailureReasonsByPurposeIds(List.of(match.getPurpose()));
    Match foundMatch = matchDAO.findMatchById(matchId);
    assertNotNull(foundMatch);
    assertEquals(0, foundMatch.getFailureReasons().size());
  }

  private Match createMatch() {
    DataAccessRequest dar = createDataAccessRequestV3();
    createDac();
    Dataset dataset = createDataset();
    Integer matchId =
        matchDAO.insertMatch(
            dataset.getDatasetIdentifier(),
            dar.getReferenceId(),
            RandomUtils.nextBoolean(),
            false,
            new Date(),
            MatchAlgorithm.V3.getVersion());
    return matchDAO.findMatchById(matchId);
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
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId, false,
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

  private Consent createConsent() {
    String consentId = UUID.randomUUID().toString();
    consentDAO.insertConsent(consentId,
        false,
        "{\"generalUse\": true }",
        "dul",
        consentId,
        "dulName",
        new Date(),
        new Date(),
        "Group");
    return consentDAO.findConsentById(consentId);
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

}
