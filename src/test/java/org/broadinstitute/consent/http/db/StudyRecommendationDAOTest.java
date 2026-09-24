package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.StudyRecommendation;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StudyRecommendationDAOTest extends DAOTestHelper {

  private StudyRecommendationDAO studyRecommendationDAO;

  @BeforeEach
  void setUpDao() {
    studyRecommendationDAO = jdbi.onDemand(StudyRecommendationDAO.class);
  }

  @Test
  void testFindSimilar() {
    String piName = randomAlphabetic(20);
    String typeOne = randomAlphabetic(20);
    String typeTwo = randomAlphabetic(20);

    Integer sourceId = insertStudy(piName, List.of(typeOne, typeTwo), true);
    // Shares the PI and both data types — strongest match
    Integer bothMatchId = insertStudy(piName, List.of(typeOne, typeTwo), true);
    Integer bothMatchDatasetId = insertDatasetForStudy(bothMatchId);
    // Shares one data type only
    Integer typeMatchId = insertStudy(randomAlphabetic(20), List.of(typeOne), true);
    // Shares the PI only
    Integer piMatchId = insertStudy(piName, List.of(randomAlphabetic(20)), true);
    // Matches but is not publicly visible
    insertStudy(piName, List.of(typeOne), false);
    // No relation to the source study
    insertStudy(randomAlphabetic(20), List.of(randomAlphabetic(20)), true);

    List<StudyRecommendation> similar = studyRecommendationDAO.findSimilar(sourceId);

    assertEquals(
        List.of(bothMatchId, typeMatchId, piMatchId),
        similar.stream().map(StudyRecommendation::studyId).toList());
    assertTrue(similar.stream().map(StudyRecommendation::studyId).noneMatch(sourceId::equals));

    StudyRecommendation bothMatch = similar.getFirst();
    assertEquals(1L, bothMatch.datasetCount());
    assertEquals(List.of(bothMatchDatasetId), bothMatch.datasetIds());

    StudyRecommendation typeMatch = similar.get(1);
    assertEquals(0L, typeMatch.datasetCount());
    assertTrue(typeMatch.datasetIds().isEmpty());
  }

  @Test
  void testFindFrequentlyRequestedWith() {
    Integer sourceId = insertStudy(randomAlphabetic(20), List.of(randomAlphabetic(20)), true);
    Integer sourceDatasetId = insertDatasetForStudy(sourceId);

    Integer frequentId = insertStudy(randomAlphabetic(20), List.of(randomAlphabetic(20)), true);
    Integer frequentDatasetId = insertDatasetForStudy(frequentId);
    Integer occasionalId = insertStudy(randomAlphabetic(20), List.of(randomAlphabetic(20)), true);
    Integer occasionalDatasetId = insertDatasetForStudy(occasionalId);
    Integer privateId = insertStudy(randomAlphabetic(20), List.of(randomAlphabetic(20)), false);
    Integer privateDatasetId = insertDatasetForStudy(privateId);

    Integer draftOnlyId = insertStudy(randomAlphabetic(20), List.of(randomAlphabetic(20)), true);
    Integer draftOnlyDatasetId = insertDatasetForStudy(draftOnlyId);
    Integer archivedOnlyId = insertStudy(randomAlphabetic(20), List.of(randomAlphabetic(20)), true);
    Integer archivedOnlyDatasetId = insertDatasetForStudy(archivedOnlyId);

    // Two DARs request the source dataset together with the frequent study's dataset, one of
    // them also with the private study's dataset; one DAR pairs it with the occasional study's.
    insertSubmittedDarForDatasets(sourceDatasetId, frequentDatasetId, privateDatasetId);
    insertSubmittedDarForDatasets(sourceDatasetId, frequentDatasetId);
    insertSubmittedDarForDatasets(sourceDatasetId, occasionalDatasetId);
    // A DAR not touching the source dataset never counts
    insertSubmittedDarForDatasets(occasionalDatasetId);
    // An unsubmitted draft cart is not a request, and an archived DAR stops counting
    insertDraftDarForDatasets(sourceDatasetId, draftOnlyDatasetId);
    insertArchivedDarForDatasets(sourceDatasetId, archivedOnlyDatasetId);

    List<StudyRecommendation> recommendations =
        studyRecommendationDAO.findFrequentlyRequestedWith(sourceId);

    List<Integer> recommendedIds =
        recommendations.stream().map(StudyRecommendation::studyId).toList();
    assertEquals(List.of(frequentId, occasionalId), recommendedIds);
    assertFalse(recommendedIds.contains(draftOnlyId));
    assertFalse(recommendedIds.contains(archivedOnlyId));
    assertEquals(1L, recommendations.getFirst().datasetCount());
    assertEquals(List.of(frequentDatasetId), recommendations.getFirst().datasetIds());
  }

  /** A blank pi_name is not an identity, so blank-PI studies must not match each other. */
  @Test
  void testFindSimilarDoesNotMatchOnBlankPiNames() {
    Integer sourceId = insertStudy("", List.of(randomAlphabetic(20)), true);
    insertStudy("", List.of(randomAlphabetic(20)), true);

    assertTrue(studyRecommendationDAO.findSimilar(sourceId).isEmpty());
  }

  private Integer insertStudy(String piName, List<String> dataTypes, boolean publicVisibility) {
    User user = createUser();
    return studyDAO.insertStudy(
        randomAlphabetic(20),
        randomAlphabetic(20),
        piName,
        null,
        dataTypes,
        publicVisibility,
        user.getUserId(),
        Instant.now(),
        UUID.randomUUID());
  }

  private Integer insertDatasetForStudy(Integer studyId) {
    User user = createUser();
    Integer datasetId =
        datasetDAO.insertDataset(
            randomAlphabetic(20),
            new Timestamp(new Date().getTime()),
            user.getUserId(),
            randomAlphabetic(20),
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            null);
    datasetDAO.updateStudyId(datasetId, studyId);
    return datasetId;
  }

  private void insertSubmittedDarForDatasets(Integer... datasetIds) {
    insertDarForDatasets(new DataAccessRequestData(), true, datasetIds);
  }

  private void insertDraftDarForDatasets(Integer... datasetIds) {
    insertDarForDatasets(new DataAccessRequestData(), false, datasetIds);
  }

  private void insertArchivedDarForDatasets(Integer... datasetIds) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setStatus("Archived");
    insertDarForDatasets(data, true, datasetIds);
  }

  private void insertDarForDatasets(
      DataAccessRequestData data, boolean submitted, Integer... datasetIds) {
    User user = createUser();
    String referenceId = UUID.randomUUID().toString();
    Date now = new Date();
    if (submitted) {
      Integer collectionId =
          darCollectionDAO.insertDarCollection(
              "DAR-" + randomAlphabetic(10), user.getUserId(), now);
      dataAccessRequestDAO.insertDataAccessRequest(
          collectionId, referenceId, user.getUserId(), now, now, now, data, randomAlphabetic(10));
    } else {
      dataAccessRequestDAO.insertDraftDataAccessRequest(
          referenceId, user.getUserId(), now, now, data);
    }
    for (Integer datasetId : datasetIds) {
      dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);
    }
  }
}
