package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.ShowcaseStatsResponse;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.showcase.ShowcaseCatalogService;
import org.broadinstitute.consent.http.service.showcase.ShowcaseScope;
import org.broadinstitute.consent.http.service.showcase.ShowcaseStatsLookup;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.StatementContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The {@code stats} mode against the real count query. Synthetic records only. */
class ShowcaseStatsLookupDAOTest extends DAOTestHelper {

  private static final String ACCESS = "accessManagement";
  private static final String CREATED = "2026-10-01T12:00:00Z";

  private User user;
  private Integer studyA;
  private Integer studyB;
  private Integer dacX;
  private Integer dacY;
  private ShowcaseCatalogService service;

  @BeforeEach
  void setUp() {
    user = createUser();
    studyA = insertStudy(true);
    studyB = insertStudy(true);
    dacX = dacDAO.createDac("Example DAC X", "synthetic", user.getUserId());
    dacY = dacDAO.createDac("Example DAC Y", "synthetic", user.getUserId());
    service = new ShowcaseCatalogService(datasetDAO);
  }

  /** Every access type that a shelf can show counts, whatever the section rules. */
  @Test
  void testCountsDatasetsAndDistinctStudies() {
    dataset(studyA, "open", null);
    dataset(studyA, "external", dacX);
    approvedDataset(studyB, dacY);

    assertEquals(new ShowcaseStatsResponse(2, 3), stats(null));
  }

  @Test
  void testEmptyCatalog() {
    assertEquals(new ShowcaseStatsResponse(0, 0), stats(null));
  }

  /**
   * Hidden datasets, and the studies that hold only hidden datasets, leave the counts unchanged.
   */
  @Test
  void testHiddenDatasetsLeaveCountsUnchanged() {
    dataset(studyA, "open", null);
    ShowcaseStatsResponse before = stats(null);

    Integer privateStudy = insertStudy(false);
    dataset(privateStudy, "open", null);
    dataset(studyB, "controlled", dacX);
    Integer deletedDac = dacDAO.createDac("Example DAC Z", "synthetic", user.getUserId());
    approvedDataset(studyB, deletedDac);
    dacDAO.deleteDac(deletedDac, user.getUserId());
    Integer hardDeleted = dataset(studyB, "open", null);
    datasetDAO.deleteDatasetPropertiesByDatasetId(hardDeleted);
    datasetDAO.deleteDatasetById(hardDeleted);

    assertEquals(before, stats(null));
  }

  /** DAC names match exactly; a dataset with no DAC is outside every DAC scope. */
  @Test
  void testDacScope() {
    dataset(studyA, "open", null);
    dataset(studyA, "open", dacX);
    approvedDataset(studyB, dacX);
    approvedDataset(studyB, dacY);

    assertEquals(
        new ShowcaseStatsResponse(2, 2), stats(new ShowcaseScope(List.of("Example DAC X"))));
    assertEquals(
        new ShowcaseStatsResponse(2, 3),
        stats(new ShowcaseScope(List.of("Example DAC X", "Example DAC Y"))));
    assertEquals(new ShowcaseStatsResponse(0, 0), stats(new ShowcaseScope(List.of("Example DAC"))));
    assertEquals(new ShowcaseStatsResponse(2, 4), stats(new ShowcaseScope(null)));
  }

  /** An empty DAC list matches nothing, and reads nothing. */
  @Test
  void testEmptyDacScope() {
    dataset(studyA, "open", dacX);

    assertEquals(new ShowcaseStatsResponse(0, 0), stats(new ShowcaseScope(List.of())));
    assertEquals(0, countStatements(() -> stats(new ShowcaseScope(List.of()))));
  }

  @Test
  void testScopedStatsIsOneQuery() {
    dataset(studyA, "open", dacX);

    assertEquals(1, countStatements(() -> stats(new ShowcaseScope(List.of("Example DAC X")))));
  }

  private ShowcaseStatsResponse stats(ShowcaseScope scope) {
    return service.lookupStats(new ShowcaseStatsLookup(scope));
  }

  private int countStatements(Runnable lookup) {
    AtomicInteger statements = new AtomicInteger();
    SqlStatements config = jdbi.getConfig(SqlStatements.class);
    SqlLogger previous = config.getSqlLogger();
    config.setSqlLogger(
        new SqlLogger() {
          @Override
          public void logAfterExecution(StatementContext context) {
            statements.incrementAndGet();
          }
        });
    try {
      lookup.run();
    } finally {
      config.setSqlLogger(previous);
    }
    return statements.get();
  }

  private Integer dataset(Integer studyId, String access, Integer dacId) {
    Integer id =
        datasetDAO.insertDataset(
            "Example Cohort " + UUID.randomUUID(),
            Timestamp.from(Instant.parse(CREATED)),
            user.getUserId(),
            null,
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            dacId);
    datasetDAO.updateStudyId(id, studyId);
    insertProperty(id, access);
    return id;
  }

  /** A DAC-approved controlled dataset. */
  private void approvedDataset(Integer studyId, Integer dacId) {
    Integer id = dataset(studyId, "controlled", dacId);
    jdbi.useHandle(
        handle ->
            handle.execute(
                "UPDATE dataset SET dac_approval = true, dac_approval_date = now()"
                    + " WHERE dataset_id = ?",
                id));
  }

  private Integer insertStudy(Boolean publicVisibility) {
    return studyDAO.insertStudy(
        "Example Study " + UUID.randomUUID(),
        "synthetic",
        "Example PI",
        "pi@example.org",
        List.of(),
        publicVisibility,
        user.getUserId(),
        Instant.now(),
        UUID.randomUUID());
  }

  private void insertProperty(Integer datasetId, String value) {
    jdbi.useHandle(
        handle ->
            handle.execute(
                """
                INSERT INTO dataset_property
                    (dataset_id, property_key, schema_property, property_value, property_type,
                    create_date)
                VALUES (?, 1, ?, ?, ?, now())
                """,
                datasetId,
                ACCESS,
                value,
                PropertyType.String.toString()));
  }
}
