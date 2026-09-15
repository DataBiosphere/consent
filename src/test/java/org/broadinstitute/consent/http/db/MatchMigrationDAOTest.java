package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.MatchAlgorithm;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.matchmigration.MatchMigrationPopulation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchMigrationDAOTest extends DAOTestHelper {

  @Test
  void testFindPopulationCountsOnlyWhatTheConstraintsWouldReject() {
    Dataset dataset = createDataset();
    String legacy = createSubmittedDar(dataset, false);
    String current = createSubmittedDar(dataset, false);
    insertMatch(legacy, null, MatchAlgorithm.V1.getVersion());
    insertMatch(current, dataset.getDatasetId(), MatchAlgorithm.V5.getVersion());

    MatchMigrationPopulation population = matchMigrationDAO.findPopulation();
    assertEquals(1, population.affectedMatches());
    assertEquals(1, population.affectedPurposes());
    assertEquals(1, population.versionV1());
    assertEquals(1, population.missingDatasetId());
    assertTrue(population.blocksConstraints());
  }

  @Test
  void testFindPopulationDoesNotBlockConstraintsWhenNothingIsAffected() {
    Dataset dataset = createDataset();
    insertMatch(
        createSubmittedDar(dataset, false), dataset.getDatasetId(), MatchAlgorithm.V5.getVersion());

    MatchMigrationPopulation population = matchMigrationDAO.findPopulation();
    assertEquals(0, population.affectedMatches());
    assertEquals(0, population.duplicatePurposeDatasetPairs());
    assertFalse(population.blocksConstraints());
  }

  @Test
  void testFindPopulationCountsV2AndNullVersionsSeparately() {
    Dataset dataset = createDataset();
    insertMatch(createSubmittedDar(dataset, false), dataset.getDatasetId(), "v2");
    insertMatch(createSubmittedDar(dataset, false), dataset.getDatasetId(), null);

    MatchMigrationPopulation population = matchMigrationDAO.findPopulation();
    assertEquals(2, population.affectedMatches());
    assertEquals(1, population.versionV2());
    assertEquals(1, population.versionNull());
    // Both carry a dataset id already; only their stamps make them affected
    assertEquals(0, population.missingDatasetId());
  }

  @Test
  void testFindPopulationSeparatesArchivedPurposesFromResolvableOnes() {
    Dataset dataset = createDataset();
    insertMatch(createSubmittedDar(dataset, false), null, MatchAlgorithm.V1.getVersion());
    insertMatch(createSubmittedDar(dataset, true), null, MatchAlgorithm.V1.getVersion());

    MatchMigrationPopulation population = matchMigrationDAO.findPopulation();
    assertEquals(2, population.affectedPurposes());
    assertEquals(1, population.resolvablePurposes());
    assertEquals(1, population.archivedOrMissingPurposes());
  }

  @Test
  void testFindPopulationCountsAMatchWithNoDarAtAllAsUnresolvable() {
    // No data_access_request row at all, which findByReferenceId reports the same as an archived
    // one
    insertMatch(UUID.randomUUID().toString(), null, MatchAlgorithm.V1.getVersion());

    MatchMigrationPopulation population = matchMigrationDAO.findPopulation();
    assertEquals(1, population.affectedPurposes());
    assertEquals(0, population.resolvablePurposes());
    assertEquals(1, population.archivedOrMissingPurposes());
  }

  @Test
  void testFindPopulationCountsPurposesHoldingMoreThanOneAffectedMatch() {
    Dataset dataset = createDataset();
    String purposeId = createSubmittedDar(dataset, false);
    insertMatch(purposeId, null, MatchAlgorithm.V1.getVersion());
    insertMatch(purposeId, null, MatchAlgorithm.V1.getVersion());

    MatchMigrationPopulation population = matchMigrationDAO.findPopulation();
    assertEquals(2, population.affectedMatches());
    assertEquals(1, population.affectedPurposes());
    assertEquals(1, population.purposesWithMultipleAffectedMatches());
  }

  @Test
  void testFindPopulationCountsExistingDuplicatePurposeDatasetPairs() {
    Dataset dataset = createDataset();
    String purposeId = createSubmittedDar(dataset, false);
    // Two current rows on the same pair: nothing this migration created, but the uniqueness
    // constraint would still refuse them
    insertMatch(purposeId, dataset.getDatasetId(), MatchAlgorithm.V5.getVersion());
    insertMatch(purposeId, dataset.getDatasetId(), MatchAlgorithm.V5.getVersion());

    MatchMigrationPopulation population = matchMigrationDAO.findPopulation();
    assertEquals(0, population.affectedMatches());
    assertEquals(1, population.duplicatePurposeDatasetPairs());
    // Affected count is clear, but the pair collision still blocks
    assertTrue(population.blocksConstraints());
  }

  private Integer insertMatch(String purposeId, Integer datasetId, String algorithmVersion) {
    Match match = new Match();
    match.setConsent("DUOS-" + randomInt(1, 999999));
    match.setDatasetId(datasetId);
    match.setPurpose(purposeId);
    match.setMatch(true);
    match.setFailed(false);
    match.setAbstain(false);
    match.setCreateDate(FIXED_DATE);
    match.setAlgorithmVersion(algorithmVersion);
    return matchDAO.insertMatch(match);
  }

  /** A submitted DAR carrying one dataset association, archived or not. */
  private String createSubmittedDar(Dataset dataset, boolean archived) {
    User user = createUser();
    String darCode = "DAR-" + randomInt(1, 999999999);
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle("Project Title: " + randomAlphabetic(20));
    if (archived) {
      data.setStatus("Archived");
    }
    String referenceId = UUID.randomUUID().toString();
    Date now = new Date();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId, referenceId, user.getUserId(), now, now, now, data, randomAlphabetic(10));
    dataAccessRequestDAO.insertDARDatasetRelation(referenceId, dataset.getDatasetId());
    return referenceId;
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + randomAlphanumeric(20);
    String objectId = "Object ID_" + randomAlphanumeric(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id =
        datasetDAO.insertDataset(
            name, FIXED_TIMESTAMP, user.getUserId(), objectId, dataUse.toString(), null);
    List<DatasetProperty> properties = new ArrayList<>();
    DatasetProperty property = new DatasetProperty();
    property.setDatasetId(id);
    property.setPropertyKey(1);
    property.setPropertyValue("Test_PropertyValue");
    property.setCreateDate(FIXED_DATE);
    properties.add(property);
    datasetDAO.insertDatasetProperties(properties);
    return datasetDAO.findDatasetById(id);
  }
}
