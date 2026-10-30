package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The showcase ranking and paging queries, which return aliases only. Dates are fixed at midday
 * UTC, so the order does not depend on the clock or the time zone. Synthetic records only.
 */
class ShowcaseCatalogQueriesDAOTest extends DAOTestHelper {

  private static final String ACCESS = "accessManagement";

  private User user;
  private Integer publicStudyId;
  private Integer privateStudyId;
  private Integer dacX;
  private Integer dacY;

  @BeforeEach
  void setUp() {
    user = createUser();
    publicStudyId = insertStudy(true);
    privateStudyId = insertStudy(false);
    dacX = dacDAO.createDac("Example DAC X", "synthetic", user.getUserId());
    dacY = dacDAO.createDac("Example DAC Y", "synthetic", user.getUserId());
  }

  /** Newest date first; datasets on the same date are in alias order, whatever the time. */
  @Test
  void testNewestOrderAndTies() {
    Integer older = dataset("open", "2026-10-01T09:00:00Z", null);
    Integer sameDayLowerAlias = dataset("open", "2026-10-03T14:00:00Z", null);
    Integer sameDayHigherAlias = dataset("external", "2026-10-03T10:00:00Z", null);
    Integer approved = approvedDataset("2026-10-02 12:00:00", dacX);

    assertEquals(
        List.of(sameDayLowerAlias, sameDayHigherAlias, approved, older), newest(false, null));
  }

  /** Ineligible datasets and an approved dataset with no approval date never take a place. */
  @Test
  void testHiddenDatasetsNeverTakeAPlace() {
    Integer eligible = dataset("open", "2026-10-01T12:00:00Z", null);
    dataset(privateStudyId, "open", "2026-10-09T12:00:00Z", null);
    dataset(publicStudyId, "controlled", "2026-10-09T12:00:00Z", dacX);
    approvedDataset(null, dacX);

    assertEquals(
        List.of(eligible), datasetDAO.findShowcaseNewestAliases(false, null, List.of(), 1));
  }

  @Test
  void testOpenAccessOnly() {
    Integer open = dataset("open", "2026-10-01T12:00:00Z", null);
    dataset("external", "2026-10-05T12:00:00Z", null);
    approvedDataset("2026-10-06 12:00:00", dacX);

    assertEquals(List.of(open), newest(true, null));
  }

  /** DAC names match exactly, and an empty list matches no dataset. */
  @Test
  void testDacScope() {
    Integer inX = approvedDataset("2026-10-01 12:00:00", dacX);
    approvedDataset("2026-10-02 12:00:00", dacY);
    Integer openInX = dataset("open", "2026-10-03T12:00:00Z", dacX);

    assertEquals(List.of(openInX, inX), newest(false, List.of("Example DAC X")));
    assertEquals(List.of(), newest(false, List.of("Example DAC")));
    assertEquals(List.of(), newest(false, List.of()));
  }

  /** The eligibility SQL copies findShowcaseSummaries, so a deleted DAC hides its datasets here. */
  @Test
  void testDeletedDacHidesItsDatasets() {
    Integer kept = approvedDataset("2026-10-01 12:00:00", dacX);
    approvedDataset("2026-10-02 12:00:00", dacY);
    jdbi.useHandle(
        handle -> handle.execute("UPDATE dac SET deleted = true WHERE dac_id = ?", dacY));

    assertEquals(List.of(kept), newest(false, null));
    assertEquals(List.of(kept), datasetDAO.findShowcaseAliasPage(-1, 12));
  }

  /** The legacy key counts, and an access value matches whatever its case and outer whitespace. */
  @Test
  void testLegacyKeyAndLooseAccessValues() {
    Integer legacy =
        dataset(
            publicStudyId,
            Dataset.LEGACY_ACCESS_MANAGEMENT_SCHEMA_PROPERTY,
            "open",
            "2026-10-01T12:00:00Z",
            null);
    Integer loose = dataset("  OPEN\t", "2026-10-02T12:00:00Z", null);

    assertEquals(List.of(loose, legacy), newest(true, null));
  }

  @Test
  void testExclusionsAndLimit() {
    Integer oldest = dataset("open", "2026-09-01T12:00:00Z", null);
    Integer middle = dataset("open", "2026-09-15T12:00:00Z", null);
    Integer newest = dataset("open", "2026-10-01T12:00:00Z", null);

    assertEquals(
        List.of(newest, oldest),
        datasetDAO.findShowcaseNewestAliases(false, null, List.of(middle), 12));
    assertEquals(
        List.of(newest, middle, oldest),
        datasetDAO.findShowcaseNewestAliases(false, null, null, 12));
    assertEquals(List.of(newest), datasetDAO.findShowcaseNewestAliases(false, null, List.of(), 1));
    assertEquals(List.of(), datasetDAO.findShowcaseNewestAliases(false, null, List.of(), 0));
  }

  @Test
  void testExclusionInsideDacScope() {
    Integer kept = approvedDataset("2026-10-01 12:00:00", dacX);
    Integer excluded = approvedDataset("2026-10-02 12:00:00", dacX);
    approvedDataset("2026-10-03 12:00:00", dacY);

    assertEquals(
        List.of(kept),
        datasetDAO.findShowcaseNewestAliases(
            false, List.of("Example DAC X"), List.of(excluded), 12));
  }

  /** Pages are in alias order, start after the given alias, and skip ineligible datasets. */
  @Test
  void testAliasPage() {
    Integer first = dataset("open", "2026-10-01T12:00:00Z", null);
    dataset(privateStudyId, "open", "2026-10-01T12:00:00Z", null);
    Integer second = dataset("external", "2026-10-01T12:00:00Z", null);
    Integer third = approvedDataset("2026-10-01 12:00:00", dacX);

    assertEquals(List.of(first, second), datasetDAO.findShowcaseAliasPage(-1, 2));
    assertEquals(List.of(third), datasetDAO.findShowcaseAliasPage(second, 2));
    assertEquals(List.of(), datasetDAO.findShowcaseAliasPage(third, 2));
    assertEquals(List.of(), datasetDAO.findShowcaseAliasPage(-1, 0));
  }

  /** Alias 0 is a valid legacy identifier, so the first page, after -1, starts with it. */
  @Test
  void testFirstPageStartsAtAliasZero() {
    Integer legacy = dataset("open", "2026-10-01T12:00:00Z", null);
    jdbi.useHandle(
        handle -> handle.execute("UPDATE dataset SET alias = 0 WHERE alias = ?", legacy));
    Integer next = dataset("open", "2026-10-01T12:00:00Z", null);

    assertEquals(List.of(0, next), datasetDAO.findShowcaseAliasPage(-1, 2));
  }

  private List<Integer> newest(boolean openAccessOnly, List<String> dacNames) {
    return datasetDAO.findShowcaseNewestAliases(openAccessOnly, dacNames, List.of(), 12);
  }

  private Integer dataset(String access, String created, Integer dacId) {
    return dataset(publicStudyId, access, created, dacId);
  }

  private Integer dataset(Integer studyId, String access, String created, Integer dacId) {
    return dataset(studyId, ACCESS, access, created, dacId);
  }

  private Integer dataset(
      Integer studyId, String accessKey, String access, String created, Integer dacId) {
    Integer id =
        datasetDAO.insertDataset(
            "Example Cohort " + UUID.randomUUID(),
            Timestamp.from(Instant.parse(created)),
            user.getUserId(),
            null,
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            dacId);
    datasetDAO.updateStudyId(id, studyId);
    insertProperty(id, accessKey, access);
    Integer alias = datasetDAO.findDatasetById(id).getAlias();
    assertNotNull(alias);
    return alias;
  }

  /** A DAC-approved controlled dataset; a null date leaves it with no "new in DUOS" date. */
  private Integer approvedDataset(String approvalDate, Integer dacId) {
    Integer alias = dataset("controlled", "2020-01-01T12:00:00Z", dacId);
    jdbi.useHandle(
        handle ->
            handle.execute(
                """
                UPDATE dataset SET dac_approval = true,
                    dac_approval_date = CAST(? AS timestamp)
                WHERE alias = ?
                """,
                approvalDate,
                alias));
    return alias;
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
