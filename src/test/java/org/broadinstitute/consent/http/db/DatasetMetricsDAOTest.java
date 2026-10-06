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
        dao.countDatasetsCreated(FROM, TO, "quarter"));
    assertEquals(
        List.of(new CreatedBucket(startOf(LocalDate.of(2026, 1, 1)), 1)),
        dao.countStudiesCreated(FROM, TO, "quarter"));
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
        List.of(new DatasetBucket(FROM, 1, 0)), dao.countDatasetsCreated(FROM, TO, "quarter"));
    assertEquals(List.of(new CreatedBucket(FROM, 1)), dao.countStudiesCreated(FROM, TO, "quarter"));
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
        dao.countDatasetsCreated(FROM, TO, "quarter"));
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

    assertTrue(dao.countDatasetsCreated(FROM, TO, "quarter").isEmpty());
  }

  private static Instant startOf(LocalDate date) {
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
  }

  private static Instant at(LocalDateTime time) {
    return time.atZone(ZoneId.systemDefault()).toInstant();
  }

  private Integer dataset(LocalDateTime created) {
    return datasetDAO.insertDataset(
        "Dataset " + UUID.randomUUID(),
        Timestamp.from(at(created)),
        userId,
        UUID.randomUUID().toString(),
        "{}",
        null);
  }

  private void approve(Integer datasetId, boolean approved) {
    datasetDAO.updateDatasetApproval(approved, Instant.now(), userId, datasetId);
  }

  private void study(LocalDateTime created) {
    studyDAO.insertStudy(
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
