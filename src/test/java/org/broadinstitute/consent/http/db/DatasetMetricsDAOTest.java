package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.models.CreatedBucket;
import org.broadinstitute.consent.http.models.DatasetBucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatasetMetricsDAOTest extends DAOTestHelper {

  private static final Instant FROM = startOf(LocalDate.of(2026, 1, 1));
  private static final Instant TO = startOf(LocalDate.of(2027, 1, 1));

  private DatasetMetricsDAO dao;
  private Integer userId;

  @BeforeEach
  void setUpDao() {
    dao = jdbi.onDemand(DatasetMetricsDAO.class);
    userId = createUser().getUserId();
  }

  @Test
  void bucketsDatasetsAndStudiesEitherSideOfAQuarterBoundary() {
    dataset(LocalDateTime.of(2026, 3, 31, 23, 0));
    dataset(LocalDateTime.of(2026, 4, 1, 1, 0));
    dataset(LocalDateTime.of(2026, 4, 2, 1, 0));
    study(LocalDateTime.of(2026, 3, 31, 23, 0));

    assertEquals(
        List.of(
            new DatasetBucket(startOf(LocalDate.of(2026, 1, 1)), 1, 0),
            new DatasetBucket(startOf(LocalDate.of(2026, 4, 1)), 2, 0)),
        dao.countDatasetsCreated(FROM, TO, null, "quarter"));
    assertEquals(
        List.of(new CreatedBucket(startOf(LocalDate.of(2026, 1, 1)), 1)),
        dao.countStudiesCreated(FROM, TO, null, "quarter"));
  }

  @Test
  void rangeIncludesItsStartAndExcludesItsEnd() {
    dataset(LocalDateTime.of(2025, 12, 31, 23, 59));
    dataset(LocalDateTime.of(2026, 1, 1, 0, 0));
    dataset(LocalDateTime.of(2027, 1, 1, 0, 0));
    study(LocalDateTime.of(2025, 12, 31, 23, 59));
    study(LocalDateTime.of(2026, 1, 1, 0, 0));
    study(LocalDateTime.of(2027, 1, 1, 0, 0));

    assertEquals(
        List.of(new DatasetBucket(FROM, 1, 0)),
        dao.countDatasetsCreated(FROM, TO, null, "quarter"));
    assertEquals(
        List.of(new CreatedBucket(FROM, 1)), dao.countStudiesCreated(FROM, TO, null, "quarter"));
  }

  @Test
  void countsOnlyDatasetsTheDacHasApproved() {
    LocalDateTime created = LocalDateTime.of(2026, 6, 1, 12, 0);
    approve(dataset(created), true);
    approve(dataset(created), true);
    approve(dataset(created), false);
    dataset(created);

    assertEquals(
        List.of(new DatasetBucket(startOf(LocalDate.of(2026, 4, 1)), 4, 2)),
        dao.countDatasetsCreated(FROM, TO, null, "quarter"));
  }

  @Test
  void skipsALegacyDatasetWithNoCreateDate() {
    Integer legacy = dataset(LocalDateTime.of(2026, 6, 1, 12, 0));
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate("UPDATE dataset SET create_date = NULL WHERE dataset_id = :id")
                .bind("id", legacy)
                .execute());

    assertTrue(dao.countDatasetsCreated(FROM, TO, null, "quarter").isEmpty());
  }

  @Test
  void scopesDatasetsAndStudiesToTheDacsTheirDatasetsAreNowIn() {
    LocalDateTime created = LocalDateTime.of(2026, 6, 1, 12, 0);
    Integer dacA = dac();
    Integer dacB = dac();
    Integer studyA = study(created);
    Integer studyB = study(created);
    datasetDAO.updateStudyId(dataset(created, dacA), studyA);
    datasetDAO.updateStudyId(dataset(created, dacA), studyA);
    datasetDAO.updateStudyId(dataset(created, dacB), studyB);
    dataset(created);
    study(created);
    Instant quarter = startOf(LocalDate.of(2026, 4, 1));

    assertEquals(
        List.of(new DatasetBucket(quarter, 2, 0)),
        dao.countDatasetsCreated(FROM, TO, List.of(dacA), "quarter"));
    assertEquals(
        List.of(new CreatedBucket(quarter, 1)),
        dao.countStudiesCreated(FROM, TO, List.of(dacA), "quarter"));
    assertEquals(
        List.of(new DatasetBucket(quarter, 3, 0)),
        dao.countDatasetsCreated(FROM, TO, List.of(dacA, dacB), "quarter"));
    assertEquals(
        List.of(new CreatedBucket(quarter, 2)),
        dao.countStudiesCreated(FROM, TO, List.of(dacA, dacB), "quarter"));
    assertTrue(dao.countDatasetsCreated(FROM, TO, List.of(), "quarter").isEmpty());
    assertTrue(dao.countStudiesCreated(FROM, TO, List.of(), "quarter").isEmpty());
  }

  private static Instant startOf(LocalDate date) {
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
  }

  private static Instant at(LocalDateTime time) {
    return time.atZone(ZoneId.systemDefault()).toInstant();
  }

  private Integer dataset(LocalDateTime created) {
    return dataset(created, null);
  }

  private Integer dataset(LocalDateTime created, Integer dacId) {
    return datasetDAO.insertDataset(
        "Dataset " + UUID.randomUUID(),
        Timestamp.from(at(created)),
        userId,
        UUID.randomUUID().toString(),
        "{}",
        dacId);
  }

  private Integer dac() {
    return dacDAO.createDac("DAC " + UUID.randomUUID(), UUID.randomUUID().toString(), userId);
  }

  private void approve(Integer datasetId, boolean approved) {
    datasetDAO.updateDatasetApproval(approved, Instant.now(), userId, datasetId);
  }

  private Integer study(LocalDateTime created) {
    return studyDAO.insertStudy(
        "Study " + UUID.randomUUID(),
        "description",
        "PI",
        null,
        List.of(),
        true,
        userId,
        at(created),
        UUID.randomUUID());
  }
}
