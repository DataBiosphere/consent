package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.ShowcaseDatasetSummary;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.resources.PublicShowcaseCatalogResource;
import org.broadinstitute.consent.http.service.showcase.ShowcaseCatalogService;
import org.broadinstitute.consent.http.service.showcase.ShowcaseLookupLimits;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.StatementContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The public lookup from the resource down to the real projection query. Synthetic records only;
 * the planted values stand in for data that must never leave Consent through this route.
 */
class ShowcaseCatalogLookupDAOTest extends DAOTestHelper {

  private static final String ACCESS = "accessManagement";
  private static final String PI_EMAIL = "planted-pi@example.org";
  private static final String CUSTODIAN_EMAIL = "planted-custodian@example.org";
  private static final String DATA_LOCATION = "gs://planted-location/data";
  private static final String UNAPPROVED_VALUE = "planted-unapproved-value";

  /** Above every alias a test inserts ({@link #identifier} checks it), so it matches nothing. */
  private static final String NONEXISTENT = "DUOS-999999";

  private User user;
  private Integer publicStudyId;
  private PublicShowcaseCatalogResource resource;

  @BeforeEach
  void setUp() {
    user = createUser();
    publicStudyId = insertStudy(true);
    resource = new PublicShowcaseCatalogResource(new ShowcaseCatalogService(datasetDAO));
  }

  /**
   * Each kind of dataset the lookup must not reveal gives the same status and body as an identifier
   * that matches nothing, alone and next to an eligible dataset, apart from the echoed identifier.
   */
  @Test
  void testHiddenDatasetsLookLikeNonexistentOnes() {
    String eligible = openDataset(publicStudyId);
    String privateStudy = openDataset(insertStudy(false));
    String unapprovedControlled = dataset(publicStudyId, "controlled", false);
    String approvedControlled = dataset(publicStudyId, "controlled", true);
    String external = dataset(publicStudyId, "external", false);
    String hardDeleted = hardDeletedDataset();

    for (String hidden :
        List.of(privateStudy, unapprovedControlled, approvedControlled, external, hardDeleted)) {
      for (List<String> batch : List.of(List.of(hidden), List.of(eligible, hidden))) {
        Response hiddenResponse = lookup(batch);
        Response nonexistentResponse =
            lookup(batch.stream().map(id -> id.equals(hidden) ? NONEXISTENT : id).toList());

        assertEquals(nonexistentResponse.getStatus(), hiddenResponse.getStatus(), hidden);
        String hiddenBody = body(hiddenResponse);
        assertEquals(body(nonexistentResponse), hiddenBody.replace(hidden, NONEXISTENT), hidden);
        assertResults(hiddenBody, batch, List.of(eligible));
      }
    }
  }

  /**
   * The SQL itself, past the section filter: the open-access section drops controlled and external
   * datasets in Java, so this checks the rules that the query alone must apply.
   */
  @Test
  void testQueryAppliesTheCatalogRules() {
    String open = openDataset(publicStudyId);
    String external = dataset(publicStudyId, "external", false);
    String approvedControlled = dataset(publicStudyId, "controlled", true);
    String unapprovedControlled = dataset(publicStudyId, "controlled", false);
    String privateStudy = openDataset(insertStudy(false));
    String deletedDac = dataset(publicStudyId, "controlled", true);
    jdbi.useHandle(
        handle ->
            handle.execute(
                """
                UPDATE dac SET deleted = true
                WHERE dac_id = (SELECT dac_id FROM dataset WHERE alias = ?)
                """,
                Dataset.parseIdentifierToAlias(deletedDac)));

    List<String> found =
        datasetDAO
            .findShowcaseSummaries(
                Stream.of(
                        open,
                        external,
                        approvedControlled,
                        unapprovedControlled,
                        privateStudy,
                        deletedDac)
                    .map(Dataset::parseIdentifierToAlias)
                    .toList())
            .stream()
            .map(ShowcaseDatasetSummary::identifier)
            .toList();

    assertEquals(List.of(open, external, approvedControlled), found);
  }

  @Test
  void testPlantedValuesNeverAppear() {
    Integer studyId = insertStudy(true);
    studyDAO.insertStudyProperty(studyId, "dataCustodianEmail", "string", CUSTODIAN_EMAIL);
    studyDAO.insertStudyProperty(studyId, "phenotypeIndication", "string", UNAPPROVED_VALUE);
    Integer id = insertDataset(studyId);
    insertProperty(id, ACCESS, "open");
    insertProperty(id, "dataCustodianEmail", CUSTODIAN_EMAIL);
    insertProperty(id, "dataLocation", DATA_LOCATION);
    // The access URL property goes through safeUrl, which drops a non-web URL.
    insertProperty(id, "url", DATA_LOCATION);
    insertProperty(id, "unapprovedProperty", UNAPPROVED_VALUE);
    String identifier = identifier(id);

    Response response = lookup(List.of(identifier, NONEXISTENT));

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    String body = body(response);
    assertResults(body, List.of(identifier, NONEXISTENT), List.of(identifier));
    for (String planted :
        List.of(PI_EMAIL, user.getEmail(), CUSTODIAN_EMAIL, DATA_LOCATION, UNAPPROVED_VALUE)) {
      assertFalse(body.contains(planted), planted);
    }
  }

  /** The full batch holds the real dataset, so the one query does return a row. */
  @Test
  void testMaximumLookupIsOneQuery() {
    String eligible = openDataset(publicStudyId);
    List<String> identifiers =
        Stream.concat(
                Stream.of(eligible),
                IntStream.iterate(1, i -> i + 1)
                    .mapToObj("DUOS-%06d"::formatted)
                    .filter(id -> !id.equals(eligible)))
            .limit(ShowcaseLookupLimits.MAX_IDENTIFIERS)
            .toList();
    AtomicReference<Response> response = new AtomicReference<>();
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
      response.set(lookup(identifiers));
    } finally {
      config.setSqlLogger(previous);
    }

    assertEquals(1, statements.get());
    assertEquals(Response.Status.OK.getStatusCode(), response.get().getStatus());
    assertResults(body(response.get()), identifiers, List.of(eligible));
  }

  private Response lookup(List<String> identifiers) {
    String request =
        """
        { "mode": "identifiers", "section": "open-access-datasets", "identifiers": [%s] }"""
            .formatted(
                String.join(", ", identifiers.stream().map(id -> "\"" + id + "\"").toList()));
    return resource.lookupDatasets(
        null, new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8)));
  }

  private static String body(Response response) {
    return (String) response.getEntity();
  }

  /** One result per identifier, in order: a summary for each eligible one, else unavailable. */
  private static void assertResults(String body, List<String> identifiers, List<String> eligible) {
    JsonArray datasets = JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("datasets");
    assertEquals(identifiers.size(), datasets.size(), body);
    for (int i = 0; i < identifiers.size(); i++) {
      JsonObject result = datasets.get(i).getAsJsonObject();
      String identifier = identifiers.get(i);
      assertEquals(identifier, result.get("identifier").getAsString(), body);
      assertEquals(eligible.contains(identifier), result.has("title"), identifier);
      assertEquals(!eligible.contains(identifier), result.has("status"), identifier);
    }
  }

  /** The dataset's identifier, checked to stay below {@link #NONEXISTENT}. */
  private String identifier(Integer datasetId) {
    String identifier = datasetDAO.findDatasetById(datasetId).getDatasetIdentifier();
    assertTrue(
        Dataset.parseIdentifierToAlias(identifier) < Dataset.parseIdentifierToAlias(NONEXISTENT),
        identifier);
    return identifier;
  }

  private String openDataset(Integer studyId) {
    return dataset(studyId, "open", false);
  }

  private String dataset(Integer studyId, String access, boolean approved) {
    Integer dacId =
        dacDAO.createDac("Example DAC " + UUID.randomUUID(), "synthetic", user.getUserId());
    Integer id = insertDataset(studyId, dacId);
    insertProperty(id, ACCESS, access);
    if (approved) {
      jdbi.useHandle(
          handle ->
              handle.execute(
                  "UPDATE dataset SET dac_approval = true, dac_approval_date = now() "
                      + "WHERE dataset_id = ?",
                  id));
    }
    return identifier(id);
  }

  private String hardDeletedDataset() {
    Integer id = insertDataset(publicStudyId);
    insertProperty(id, ACCESS, "open");
    String identifier = identifier(id);
    datasetDAO.deleteDatasetPropertiesByDatasetId(id);
    datasetDAO.deleteDatasetById(id);
    return identifier;
  }

  private Integer insertStudy(Boolean publicVisibility) {
    return studyDAO.insertStudy(
        "Example Study " + UUID.randomUUID(),
        "synthetic",
        "Example PI",
        PI_EMAIL,
        List.of(),
        publicVisibility,
        user.getUserId(),
        Instant.now(),
        UUID.randomUUID());
  }

  private Integer insertDataset(Integer studyId) {
    return insertDataset(studyId, null);
  }

  private Integer insertDataset(Integer studyId, Integer dacId) {
    Integer id =
        datasetDAO.insertDataset(
            "Example Cohort " + UUID.randomUUID(),
            Timestamp.from(Instant.parse("2026-10-02T12:00:00Z")),
            user.getUserId(),
            null,
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            dacId);
    datasetDAO.updateStudyId(id, studyId);
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
}
