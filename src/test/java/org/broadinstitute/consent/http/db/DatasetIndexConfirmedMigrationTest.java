package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Runs the SQL from the shipped changeset over data. */
class DatasetIndexConfirmedMigrationTest extends DAOTestHelper {

  private static final Path CHANGESET =
      Path.of(
          "src/main/resources/changesets/changelog-consent-2026-11-05-dataset-index-confirmed.xml");

  @Test
  void testEachDatasetStartsConfirmedAtItsIndexedDate() throws Exception {
    Integer indexed = insertDataset();
    Integer neverIndexed = insertDataset();
    executeSql(
        "UPDATE dataset SET indexed_date = '2026-01-02T03:04:05Z' WHERE dataset_id = %d"
            .formatted(indexed));
    executeSql("UPDATE dataset SET index_confirmed_date = NULL");

    sqlBlocks(Files.readString(CHANGESET)).forEach(this::executeSql);

    assertEquals(Instant.parse("2026-01-02T03:04:05Z"), confirmedDate(indexed));
    assertNull(confirmedDate(neverIndexed));
  }

  private Instant confirmedDate(int datasetId) {
    return jdbi.withHandle(
        h ->
            h.createQuery("SELECT index_confirmed_date FROM dataset WHERE dataset_id = :id")
                .bind("id", datasetId)
                .mapTo(Instant.class)
                .findOne()
                .orElse(null));
  }

  private Integer insertDataset() {
    return datasetDAO.insertDataset(
        randomAlphabetic(20),
        Timestamp.from(Instant.now()),
        createUser().getUserId(),
        UUID.randomUUID().toString(),
        "{}",
        null);
  }
}
