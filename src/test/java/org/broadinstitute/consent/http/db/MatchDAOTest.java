package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.MatchAlgorithm;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchDAOTest extends DAOTestHelper {

  @Test
  void testFindMatchesByPurposeId() {
    Match m = createMatch();

    List<Match> matches = matchDAO.findMatchesByPurposeId(m.getPurpose());
    assertFalse(matches.isEmpty());
    Match found = matches.getFirst();
    assertEquals(found.getId(), m.getId());
    assertEquals(found.getDatasetId(), m.getDatasetId());
    assertEquals(found.getPurpose(), m.getPurpose());
    assertEquals(found.getConsent(), m.getConsent());
    assertEquals(found.getFailed(), m.getFailed());
    assertEquals(found.getMatch(), m.getMatch());
  }

  @Test
  void testPublicIdentifierDoesNotTruncateAliasesLongerThanSixDigits() {
    Dataset dataset = createDataset();
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate("UPDATE dataset SET alias = 1000000 WHERE dataset_id = :datasetId")
                .bind("datasetId", dataset.getDatasetId())
                .execute());
    String purposeId = UUID.randomUUID().toString();
    Integer matchId =
        matchDAO.insertMatch(
            dataset.getDatasetId(),
            purposeId,
            true,
            false,
            FIXED_DATE,
            MatchAlgorithm.V4.getVersion(),
            false);

    Match match = matchDAO.findMatchById(matchId);

    assertEquals("DUOS-1000000", match.getConsent());
  }

  private Match makeMockMatch() {
    Dataset dataset = createDataset();
    Match match = new Match();
    match.setDatasetId(dataset.getDatasetId());
    match.setConsent(dataset.getDatasetIdentifier());
    match.setPurpose(UUID.randomUUID().toString());
    match.setFailed(false);
    match.setCreateDate(FIXED_DATE);
    match.setMatch(randomBoolean());
    match.setAlgorithmVersion(MatchAlgorithm.V1.getVersion());
    match.setAbstain(false);
    return match;
  }

  @Test
  void testDeleteMatchesByPurposeId() {
    Match m = createMatch();

    matchDAO.deleteMatchesByPurposeId(m.getPurpose());
    List<Match> matches = matchDAO.findMatchesByPurposeId(m.getPurpose());
    assertTrue(matches.isEmpty());
  }

  @Test
  void testCountMatchesByResult() {
    Match m1 = createMatch();
    Match m2 = createMatch();

    Integer count1 = matchDAO.countMatchesByResult(m1.getMatch());
    assertTrue(count1 >= 1);
    Integer count2 = matchDAO.countMatchesByResult(m2.getMatch());
    assertTrue(count2 >= 1);
  }

  @Test
  void testFindMatchesForLatestDataAccessElectionsByPurposeIds() {
    Dataset dataset = createDataset();
    // query should pull the latest election for a given reference id
    // creating two access elections with the same reference id and datasetid to test that condition
    String darReferenceId = UUID.randomUUID().toString();
    Election targetElection = createDataAccessElection(darReferenceId, dataset.getDatasetId());
    Election ignoredAccessElection =
        createDataAccessElection(UUID.randomUUID().toString(), dataset.getDatasetId());

    // Generate an unknown election to test that the query only references DataAccess elections
    Election unknownElection =
        createUnknownElection(UUID.randomUUID().toString(), dataset.getDatasetId());
    Dataset datasetWithoutElection = createDataset();

    // This match represents the match record generated for the target election
    matchDAO.insertMatch(
        dataset.getDatasetId(),
        darReferenceId,
        true,
        false,
        FIXED_DATE,
        MatchAlgorithm.V4.getVersion(),
        false);

    // This match represents the match record generated for the ignored access election
    matchDAO.insertMatch(
        dataset.getDatasetId(),
        ignoredAccessElection.getReferenceId(),
        false,
        false,
        FIXED_DATE,
        MatchAlgorithm.V4.getVersion(),
        false);

    // This match is never created under consent's workflow (unless the cause is a bug)
    // This is included simply to test the DataAccess conditional on the INNER JOIN statement
    matchDAO.insertMatch(
        dataset.getDatasetId(),
        unknownElection.getReferenceId(),
        false,
        false,
        FIXED_DATE,
        MatchAlgorithm.V4.getVersion(),
        true);

    // A match for the same DAR but a different dataset must not join to the target election.
    matchDAO.insertMatch(
        datasetWithoutElection.getDatasetId(),
        darReferenceId,
        false,
        false,
        FIXED_DATE,
        MatchAlgorithm.V4.getVersion(),
        false);

    List<Match> matchResults =
        matchDAO.findMatchesForLatestDataAccessElectionsByPurposeIds(List.of(darReferenceId));
    assertEquals(1, matchResults.size());
    Match result = matchResults.getFirst();
    assertEquals(targetElection.getReferenceId(), result.getPurpose());
  }

  @Test
  void testFindMatchesForLatestDataAccessElectionsByPurposeIds_NegativeTest() {
    Dataset dataset = createDataset();
    String darReferenceId = UUID.randomUUID().toString();

    // Generate access election for test
    Election accessElection =
        createDataAccessElection(UUID.randomUUID().toString(), dataset.getDatasetId());

    // Generate an unknown election for test
    Election unknownElection = createUnknownElection(darReferenceId, dataset.getDatasetId());

    // This match represents the match record generated for the access election
    matchDAO.insertMatch(
        dataset.getDatasetId(),
        accessElection.getReferenceId(),
        true,
        false,
        FIXED_DATE,
        MatchAlgorithm.V4.getVersion(),
        false);

    // This match is never created under consent's workflow (unless the cause is a bug)
    // This is included simply to test the DataAccess conditional on the INNER JOIN statement
    matchDAO.insertMatch(
        dataset.getDatasetId(),
        unknownElection.getReferenceId(),
        false,
        false,
        FIXED_DATE,
        MatchAlgorithm.V4.getVersion(),
        false);

    // Negative testing means we'll feed the query a reference id that isn't tied to a DataAccess
    // election
    // Again, a match like this usually isn't generated in a normal workflow unless bug occurs, but
    // having the 'DataAccess' condition is a nice safety net
    List<Match> matchResults =
        matchDAO.findMatchesForLatestDataAccessElectionsByPurposeIds(List.of(darReferenceId));
    assertTrue(matchResults.isEmpty());
  }

  @Test
  void testFindMatchById() {
    Match match = makeMockMatch();
    Integer matchId =
        matchDAO.insertMatch(
            match.getDatasetId(),
            match.getPurpose(),
            match.getMatch(),
            match.getFailed(),
            match.getCreateDate(),
            match.getAlgorithmVersion(),
            match.getAbstain());
    Match foundMatch = matchDAO.findMatchById(matchId);
    assertNotNull(foundMatch);
    assertEquals(match.getDatasetId(), foundMatch.getDatasetId());
    assertEquals(match.getConsent(), foundMatch.getConsent());
  }

  @Test
  void testMatchUniquenessUsesPurposeAndDatasetId() {
    Dataset firstDataset = createDataset();
    Dataset secondDataset = createDataset();
    String purposeId = UUID.randomUUID().toString();

    matchDAO.insertMatch(
        firstDataset.getDatasetId(),
        purposeId,
        true,
        false,
        FIXED_DATE,
        MatchAlgorithm.V4.getVersion(),
        false);
    matchDAO.insertMatch(
        secondDataset.getDatasetId(),
        purposeId,
        true,
        false,
        FIXED_DATE,
        MatchAlgorithm.V4.getVersion(),
        false);

    Integer firstDatasetId = firstDataset.getDatasetId();
    String algorithmVersion = MatchAlgorithm.V4.getVersion();

    assertThrows(
        UnableToExecuteStatementException.class,
        () ->
            matchDAO.insertMatch(
                firstDatasetId, purposeId, false, false, FIXED_DATE, algorithmVersion, false));
  }

  @Test
  void testInsertFailureReason() {
    Match match = makeMockMatch();
    match.setMatch(false);
    match.setAlgorithmVersion(MatchAlgorithm.V4.getVersion());
    match.addRationale(randomAlphabetic(100));
    match.addRationale(randomAlphabetic(100));
    Integer matchId =
        matchDAO.insertMatch(
            match.getDatasetId(),
            match.getPurpose(),
            match.getMatch(),
            match.getFailed(),
            match.getCreateDate(),
            match.getAlgorithmVersion(),
            match.getAbstain());
    match.getRationales().forEach(f -> matchDAO.insertRationale(matchId, f));
    Match foundMatch = matchDAO.findMatchById(matchId);
    assertNotNull(foundMatch);
    assertEquals(match.getRationales().size(), foundMatch.getRationales().size());
  }

  @Test
  void testDeleteFailureReasonsByPurposeIds() {
    Match match = makeMockMatch();
    match.setMatch(false);
    match.setAlgorithmVersion(MatchAlgorithm.V4.getVersion());
    match.addRationale(randomAlphabetic(100));
    match.addRationale(randomAlphabetic(100));
    Integer matchId =
        matchDAO.insertMatch(
            match.getDatasetId(),
            match.getPurpose(),
            match.getMatch(),
            match.getFailed(),
            match.getCreateDate(),
            match.getAlgorithmVersion(),
            match.getAbstain());
    match.getRationales().forEach(f -> matchDAO.insertRationale(matchId, f));
    matchDAO.deleteRationalesByPurposeIds(List.of(match.getPurpose()));
    Match foundMatch = matchDAO.findMatchById(matchId);
    assertNotNull(foundMatch);
    assertEquals(0, foundMatch.getRationales().size());
  }

  private Match createMatch() {
    DataAccessRequest dar = createDataAccessRequestV3();
    createDac();
    Dataset dataset = createDataset();
    Integer matchId =
        matchDAO.insertMatch(
            dataset.getDatasetId(),
            dar.getReferenceId(),
            randomBoolean(),
            false,
            FIXED_DATE,
            MatchAlgorithm.V4.getVersion(),
            false);
    return matchDAO.findMatchById(matchId);
  }

  private void createDac() {
    dacDAO.createDac(
        "Test_" + randomAlphanumeric(20),
        "Test_" + randomAlphanumeric(20),
        createUser().getUserId());
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + randomAlphanumeric(20);
    String objectId = "Object ID_" + randomAlphanumeric(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id =
        datasetDAO.insertDataset(
            name, FIXED_TIMESTAMP, user.getUserId(), objectId, dataUse.toString(), null);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private void createDatasetProperties(Integer datasetId) {
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDatasetId(datasetId);
    dsp.setPropertyKey(1);
    dsp.setPropertyValue("Test_PropertyValue");
    dsp.setCreateDate(FIXED_DATE);
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
  }

  private Election createUnknownElection(String referenceId, Integer datasetId) {
    Integer electionId =
        electionDAO.insertElection(
            "UnknownElection", ElectionStatus.OPEN.getValue(), FIXED_DATE, referenceId, datasetId);
    return electionDAO.findElectionById(electionId);
  }

  private Election createDataAccessElection(String referenceId, Integer datasetId) {
    Integer electionId =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.OPEN.getValue(),
            FIXED_DATE,
            referenceId,
            datasetId);
    return electionDAO.findElectionById(electionId);
  }
}
