package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.ShowcaseDatasetSummary;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Synthetic records only. */
class DatasetShowcaseSummaryDAOTest extends DAOTestHelper {

  private static final Instant CREATED = Instant.parse("2026-10-02T12:00:00Z");
  private static final String APPROVED = "2026-09-30 23:30:00";
  private static final String ACCESS = "accessManagement";
  private static final String LEGACY_ACCESS = "consentGroup.accessManagement";

  private User user;
  private Integer publicStudyId;

  @BeforeEach
  void setUp() {
    user = createUser();
    publicStudyId = insertStudy(true);
  }

  @Test
  void testOpenDataset() {
    Integer dacId = dacDAO.createDac("Example DAC", "synthetic", user.getUserId());
    Integer id = insertDataset("Example Open Cohort", publicStudyId, dacId);
    insertProperty(id, ACCESS, "open");
    insertProperty(id, "numberOfParticipants", "1200");
    insertProperty(id, "url", "https://example.org/open-cohort");
    studyDAO.insertStudyProperty(publicStudyId, "species", "string", "  Human ");

    assertEquals(
        List.of(
            new ShowcaseDatasetSummary(
                identifier(id),
                "Example Open Cohort",
                "open",
                "Example DAC",
                List.of("GRU"),
                1200,
                "2026-10-02",
                "https://example.org/open-cohort",
                "Human")),
        find(id));
  }

  /** Columns: current value, legacy value, DAC approved, expected access ('excluded' if absent). */
  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      nullValues = "null",
      value = {
        "open|null|false|open",
        "' External\t'|null|false|external",
        "controlled|null|true|controlled",
        "controlled|null|false|excluded",
        "null|OPEN|false|open",
        "controlled|open|false|excluded",
        "'\fcontrolled\u000B'|open|false|excluded",
        "'\fopen\u000B'|null|false|open",
        "bogus|open|false|open",
        "bogus|null|true|null",
        "bogus|null|false|excluded",
        "null|null|true|null",
        "null|null|false|excluded",
      })
  void testAccessClassification(String current, String legacy, boolean approved, String expected) {
    Integer id = insertDataset("Example Cohort", publicStudyId, null);
    if (current != null) {
      insertProperty(id, ACCESS, current);
    }
    if (legacy != null) {
      insertProperty(id, LEGACY_ACCESS, legacy);
    }
    insertProperty(id, "url", "https://example.org/data");
    if (approved) {
      approve(id);
    }

    List<ShowcaseDatasetSummary> summaries = find(id);
    if ("excluded".equals(expected)) {
      assertTrue(summaries.isEmpty());
      return;
    }
    ShowcaseDatasetSummary summary = summaries.getFirst();
    boolean direct = "open".equals(expected) || "external".equals(expected);
    assertEquals(expected, summary.accessManagement());
    assertEquals(direct ? "https://example.org/data" : null, summary.accessUrl());
    assertEquals(direct ? "2026-10-02" : "2026-09-30", summary.availableDate());
  }

  @Test
  void testPrivateAndMissingStudiesAreExcluded() {
    Integer privateId = insertDataset("Example Private Cohort", insertStudy(false), null);
    Integer noStudyId = insertDataset("Example Orphan Cohort", null, null);
    for (Integer id : List.of(privateId, noStudyId)) {
      insertProperty(id, ACCESS, "open");
      approve(id);
    }

    assertTrue(find(privateId, noStudyId).isEmpty());
  }

  @Test
  void testSchemaRejectsNullVisibilityAndNullValues() {
    Integer id = insertDataset("Example Cohort", publicStudyId, null);
    assertThrows(UnableToExecuteStatementException.class, () -> insertStudy(null));
    assertThrows(
        UnableToExecuteStatementException.class,
        () -> insertProperty(id, "numberOfParticipants", null));
    assertThrows(
        UnableToExecuteStatementException.class,
        () -> studyDAO.insertStudyProperty(publicStudyId, "species", "string", null));
  }

  @Test
  void testHardDeletedDatasetIsAbsent() {
    Integer id = insertDataset("Example Cohort", publicStudyId, null);
    insertProperty(id, ACCESS, "open");
    String identifier = identifier(id);
    Integer alias = datasetDAO.findDatasetById(id).getAlias();
    datasetDAO.deleteDatasetPropertiesByDatasetId(id);
    datasetDAO.deleteDatasetById(id);

    assertTrue(datasetDAO.findShowcaseSummaries(List.of(alias)).isEmpty(), identifier);
  }

  @Test
  void testUnapprovedPropertiesAreNeverReturned() {
    Integer id = insertDataset("Example Cohort", publicStudyId, null);
    insertProperty(id, ACCESS, "open");
    insertProperty(id, "dataCustodianEmail", "custodian@example.org");
    insertProperty(id, "data", "{\"note\":\"internal\"}");
    studyDAO.insertStudyProperty(publicStudyId, "dataCustodianEmail", "string", "a@example.org");
    studyDAO.insertStudyProperty(publicStudyId, "phenotypeIndication", "string", "internal note");

    assertEquals(
        List.of(
            new ShowcaseDatasetSummary(
                identifier(id),
                "Example Cohort",
                "open",
                null,
                List.of("GRU"),
                null,
                "2026-10-02",
                null,
                null)),
        find(id));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "' 42 '|42",
        "0|0",
        "-5|",
        "abc|",
        "1.5|",
        "''|",
      })
  void testParticipantCountConversion(String value, Integer expected) {
    Integer id = insertDataset("Example Cohort", publicStudyId, null);
    insertProperty(id, ACCESS, "open");
    insertProperty(id, "numberOfParticipants", value);

    assertEquals(expected, find(id).getFirst().participantCount());
  }

  @Test
  void testLegacyUrlIsReadAfterCurrentUrl() {
    Integer legacyOnly = insertDataset("Example Cohort A", publicStudyId, null);
    Integer both = insertDataset("Example Cohort B", publicStudyId, null);
    for (Integer id : List.of(legacyOnly, both)) {
      insertProperty(id, ACCESS, "open");
      insertProperty(id, "consentGroup.url", "https://example.org/legacy");
    }
    insertProperty(both, "url", "https://example.org/current");

    assertEquals(
        List.of("https://example.org/legacy", "https://example.org/current"),
        find(legacyOnly, both).stream().map(ShowcaseDatasetSummary::accessUrl).toList());
  }

  @Test
  void testDuplicatePropertyUsesFirstRow() {
    Integer id = insertDataset("Example Cohort", publicStudyId, null);
    insertProperty(id, ACCESS, "open");
    insertProperty(id, "numberOfParticipants", "10");
    insertProperty(id, "numberOfParticipants", "20");

    assertEquals(10, find(id).getFirst().participantCount());
  }

  @Test
  void testMissingPropertiesAndBlankSpecies() {
    Integer id = insertDataset("Example Cohort", publicStudyId, null);
    insertProperty(id, ACCESS, "open");
    studyDAO.insertStudyProperty(publicStudyId, "species", "string", "   ");

    ShowcaseDatasetSummary summary = find(id).getFirst();
    assertNull(summary.participantCount());
    assertNull(summary.accessUrl());
    assertNull(summary.species());
  }

  @ParameterizedTest
  @CsvSource({
    "javascript:alert(1)",
    "ftp://example.org/data",
    "//example.org/data",
    "https://user@example.org/data",
    "not a url",
  })
  void testUnsafeUrlIsDropped(String url) {
    Integer id = insertDataset("Example Cohort", publicStudyId, null);
    insertProperty(id, ACCESS, "open");
    insertProperty(id, "url", url);

    assertNull(find(id).getFirst().accessUrl());
  }

  @Test
  void testResultsAreOrderedByAlias() {
    Integer first = insertDataset("Example Cohort A", publicStudyId, null);
    Integer second = insertDataset("Example Cohort B", publicStudyId, null);
    insertProperty(first, ACCESS, "open");
    insertProperty(second, ACCESS, "open");

    assertEquals(
        List.of(identifier(first), identifier(second)),
        find(second, first).stream().map(ShowcaseDatasetSummary::identifier).toList());
  }

  @Test
  void testEmptyAliases() {
    assertTrue(datasetDAO.findShowcaseSummaries(List.of()).isEmpty());
  }

  private List<ShowcaseDatasetSummary> find(Integer... datasetIds) {
    List<Integer> aliases =
        List.of(datasetIds).stream().map(id -> datasetDAO.findDatasetById(id).getAlias()).toList();
    return datasetDAO.findShowcaseSummaries(aliases);
  }

  private String identifier(Integer datasetId) {
    return datasetDAO.findDatasetById(datasetId).getDatasetIdentifier();
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

  private Integer insertDataset(String name, Integer studyId, Integer dacId) {
    Integer id =
        datasetDAO.insertDataset(
            name,
            Timestamp.from(CREATED),
            user.getUserId(),
            null,
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            dacId);
    if (studyId != null) {
      datasetDAO.updateStudyId(id, studyId);
    }
    return id;
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

  private void approve(Integer datasetId) {
    jdbi.useHandle(
        handle ->
            handle.execute(
                """
                UPDATE dataset SET dac_approval = true,
                    dac_approval_date = CAST(? AS timestamp)
                WHERE dataset_id = ?
                """,
                APPROVED,
                datasetId));
  }
}
