package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.ShowcaseDatasetSummary;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.showcase.ShowcaseCatalogService;
import org.broadinstitute.consent.http.service.showcase.ShowcaseLookupLimits;
import org.broadinstitute.consent.http.service.showcase.ShowcaseRanking;
import org.broadinstitute.consent.http.service.showcase.ShowcaseRankingLookup;
import org.broadinstitute.consent.http.service.showcase.ShowcaseScope;
import org.broadinstitute.consent.http.service.showcase.ShowcaseSection;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.StatementContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The {@code ranking} mode against the real queries: pins, exclusions, scope and the query count.
 * {@link ShowcaseCatalogQueriesDAOTest} covers the order and eligibility of the ranking query.
 * Dates are fixed, so the order does not depend on the clock. Synthetic records only.
 */
class ShowcaseRankingLookupDAOTest extends DAOTestHelper {

  private static final String ACCESS = "accessManagement";

  private User user;
  private Integer publicStudyId;
  private Integer privateStudyId;
  private Integer dacX;
  private Integer dacY;
  private ShowcaseCatalogService service;

  @BeforeEach
  void setUp() {
    user = createUser();
    publicStudyId = insertStudy(true);
    privateStudyId = insertStudy(false);
    dacX = dacDAO.createDac("Example DAC X", "synthetic", user.getUserId());
    dacY = dacDAO.createDac("Example DAC Y", "synthetic", user.getUserId());
    service = new ShowcaseCatalogService(datasetDAO);
  }

  @Test
  void testDacScope() {
    String inX = approvedDataset("2026-10-01 00:00:00", dacX);
    String inY = approvedDataset("2026-10-02 00:00:00", dacY);
    String openInX = dataset("open", "2026-10-03T12:00:00Z", dacX);

    assertEquals(
        List.of(openInX, inX),
        identifiers(
            rank(
                ShowcaseSection.LATEST_DATA_RELEASES,
                new ShowcaseScope(List.of("Example DAC X")),
                12,
                List.of(inY),
                List.of())));
    assertEquals(
        List.of(),
        rank(
            ShowcaseSection.LATEST_DATA_RELEASES,
            new ShowcaseScope(List.of()),
            12,
            List.of(inX),
            List.of()));
    assertEquals(
        List.of(),
        rank(
            ShowcaseSection.LATEST_DATA_RELEASES,
            new ShowcaseScope(List.of("Example DAC")),
            12,
            List.of(),
            List.of()));
  }

  @Test
  void testPinsAndExclusions() {
    String oldest = dataset("open", "2026-09-01T12:00:00Z", null);
    String middle = dataset("open", "2026-09-15T12:00:00Z", null);
    String newest = dataset("open", "2026-10-01T12:00:00Z", null);
    String excluded = dataset("open", "2026-10-02T12:00:00Z", null);
    String hidden = dataset(privateStudyId, "open", "2026-10-03T12:00:00Z", null);

    // The private pin is dropped, the pinned exclusion stays excluded, and the oldest pin leads.
    assertEquals(
        List.of(oldest, newest, middle),
        identifiers(
            rank(
                ShowcaseSection.LATEST_DATA_RELEASES,
                null,
                12,
                List.of(hidden, excluded, oldest),
                List.of(excluded))));
  }

  @Test
  void testPinsNeverPassTheLimit() {
    String first = dataset("open", "2026-09-01T12:00:00Z", null);
    String second = dataset("open", "2026-09-02T12:00:00Z", null);
    String third = dataset("open", "2026-09-03T12:00:00Z", null);

    assertEquals(
        List.of(first, second),
        identifiers(
            rank(
                ShowcaseSection.LATEST_DATA_RELEASES,
                null,
                2,
                List.of(first, second, third),
                List.of())));
  }

  /** A pinned dataset is not ranked a second time. */
  @Test
  void testPinIsNotRepeated() {
    String pinned = dataset("open", "2026-10-02T12:00:00Z", null);
    String other = dataset("open", "2026-10-01T12:00:00Z", null);

    assertEquals(
        List.of(pinned, other),
        identifiers(
            rank(ShowcaseSection.LATEST_DATA_RELEASES, null, 12, List.of(pinned), List.of())));
  }

  @Test
  void testMaximumRankingIsThreeQueries() {
    IntStream.range(0, 60).forEach(i -> dataset("open", "2026-10-01T12:00:00Z", dacX));
    List<String> pins = numbered(5000, ShowcaseLookupLimits.MAX_PINS);
    List<String> exclusions = numbered(1000, ShowcaseLookupLimits.MAX_EXCLUSIONS);

    int statements =
        countStatements(
            () ->
                rank(
                    ShowcaseSection.LATEST_DATA_RELEASES,
                    new ShowcaseScope(List.of("Example DAC X")),
                    ShowcaseLookupLimits.MAX_RANKING_LIMIT,
                    pins,
                    exclusions));

    assertEquals(3, statements);
  }

  private List<ShowcaseDatasetSummary> rank(
      ShowcaseSection section,
      ShowcaseScope scope,
      int limit,
      List<String> pins,
      List<String> exclusions) {
    return service.lookupRanking(
        new ShowcaseRankingLookup(section, ShowcaseRanking.NEWEST, scope, limit, pins, exclusions));
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

  private static List<String> identifiers(List<ShowcaseDatasetSummary> summaries) {
    return summaries.stream().map(ShowcaseDatasetSummary::identifier).toList();
  }

  private static List<String> numbered(int first, int count) {
    return IntStream.range(first, first + count).mapToObj("DUOS-%06d"::formatted).toList();
  }

  private String dataset(String access, String created, Integer dacId) {
    return dataset(publicStudyId, access, created, dacId);
  }

  private String dataset(Integer studyId, String access, String created, Integer dacId) {
    Integer id =
        datasetDAO.insertDataset(
            "Example Cohort " + UUID.randomUUID(),
            Timestamp.from(Instant.parse(created)),
            user.getUserId(),
            null,
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            dacId);
    datasetDAO.updateStudyId(id, studyId);
    insertProperty(id, ACCESS, access);
    String identifier = datasetDAO.findDatasetById(id).getDatasetIdentifier();
    assertNotNull(identifier);
    return identifier;
  }

  /** A DAC-approved controlled dataset; a null date leaves it with no "new in DUOS" date. */
  private String approvedDataset(String approvalDate, Integer dacId) {
    String identifier = dataset("controlled", "2020-01-01T12:00:00Z", dacId);
    jdbi.useHandle(
        handle ->
            handle.execute(
                """
                UPDATE dataset SET dac_approval = true,
                    dac_approval_date = CAST(? AS timestamp)
                WHERE alias = ?
                """,
                approvalDate,
                Integer.parseInt(identifier.substring("DUOS-".length()))));
    return identifier;
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

  private void insertProperty(Integer datasetId, String schemaProperty, String value) {
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
                schemaProperty,
                value,
                PropertyType.String.toString()));
  }
}
