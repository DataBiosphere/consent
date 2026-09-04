package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.StudyAssets;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

/**
 * Exercises the data migration in changelog-consent-2026-09-02-study-assets.xml, which splits the
 * legacy client-managed `assets` object into one study_property row per promoted asset type.
 *
 * <p>Liquibase runs the changeset against an empty schema at test startup, which proves the SQL
 * parses but never exercises it against data. This test reads the SQL out of the shipped changeset
 * and runs it over legacy-shaped rows, so the test cannot drift from what actually ships.
 */
class StudyAssetsMigrationTest extends DAOTestHelper {

  private static final Path CHANGESET =
      Path.of("src/main/resources/changesets/changelog-consent-2026-09-02-study-assets.xml");

  @Test
  void testPromotesAssetListsAndLeavesUnpromotedKeysBehind() throws Exception {
    Integer studyId = insertStudy();
    insertProperty(
        studyId,
        StudyAssets.ASSETS,
        """
        {"models": [{"modelId": "m-1"}, {"modelId": "m-2"}],
         "publications": [{"title": "A publication"}],
         "workspaces": [],
         "uiLabels": {"tab": "Assets"}}
        """);

    runMigration();

    // Promoted keys become their own rows, preserving the stored array
    assertEquals(
        JsonParser.parseString("[{\"modelId\": \"m-1\"}, {\"modelId\": \"m-2\"}]"),
        JsonParser.parseString(property(studyId, StudyAssets.MODELS).orElseThrow()));
    assertEquals(
        JsonParser.parseString("[{\"title\": \"A publication\"}]"),
        JsonParser.parseString(property(studyId, StudyAssets.PUBLICATIONS).orElseThrow()));

    // An empty list and an absent key produce no row
    assertTrue(property(studyId, StudyAssets.WORKSPACES).isEmpty());
    assertTrue(property(studyId, StudyAssets.BIOSPECIMENS).isEmpty());

    // The legacy object survives, holding only what was not promoted
    assertEquals(
        JsonParser.parseString("{\"uiLabels\": {\"tab\": \"Assets\"}}"),
        JsonParser.parseString(property(studyId, StudyAssets.ASSETS).orElseThrow()));
  }

  @Test
  void testDropsTheLegacyObjectWhenNothingIsLeft() throws Exception {
    Integer studyId = insertStudy();
    insertProperty(studyId, StudyAssets.ASSETS, "{\"funding\": [{\"grant\": \"R01\"}]}");

    runMigration();

    assertEquals(
        JsonParser.parseString("[{\"grant\": \"R01\"}]"),
        JsonParser.parseString(property(studyId, StudyAssets.FUNDING).orElseThrow()));
    assertTrue(property(studyId, StudyAssets.ASSETS).isEmpty());
  }

  @Test
  void testLeavesAlreadyPromotedAndNonObjectValuesAlone() throws Exception {
    Integer studyId = insertStudy();
    insertProperty(studyId, StudyAssets.ASSETS, "{\"models\": [{\"modelId\": \"from-assets\"}]}");
    insertProperty(studyId, StudyAssets.MODELS, "[{\"modelId\": \"already-promoted\"}]");

    Integer nonObjectStudyId = insertStudy();
    insertProperty(nonObjectStudyId, StudyAssets.ASSETS, "\"not an object\"");

    runMigration();

    // A study already carrying a promoted row keeps it, and gains no duplicate
    assertEquals(
        JsonParser.parseString("[{\"modelId\": \"already-promoted\"}]"),
        JsonParser.parseString(property(studyId, StudyAssets.MODELS).orElseThrow()));
    assertEquals(1, propertyCount(studyId, StudyAssets.MODELS));

    // A value that is not a JSON object is skipped rather than failing the migration
    assertEquals("\"not an object\"", property(nonObjectStudyId, StudyAssets.ASSETS).orElseThrow());
  }

  @Test
  void testIsIdempotent() throws Exception {
    Integer studyId = insertStudy();
    insertProperty(studyId, StudyAssets.ASSETS, "{\"models\": [{\"modelId\": \"m-1\"}]}");

    runMigration();
    runMigration();

    assertEquals(1, propertyCount(studyId, StudyAssets.MODELS));
    assertTrue(property(studyId, StudyAssets.ASSETS).isEmpty());
  }

  /** Runs every &lt;sql&gt; block from the shipped changeset, in order. */
  private void runMigration() throws Exception {
    String changeset = Files.readString(CHANGESET);
    Matcher matcher = Pattern.compile("<sql>(.*?)</sql>", Pattern.DOTALL).matcher(changeset);
    List<String> blocks = matcher.results().map(result -> result.group(1)).toList();
    assertEquals(2, blocks.size(), "changeset should ship two <sql> blocks");
    jdbi.useHandle(handle -> blocks.forEach(block -> handle.createScript(block.trim()).execute()));
  }

  private Integer insertStudy() {
    User user = createUser();
    return studyDAO.insertStudy(
        randomAlphabetic(20),
        randomAlphabetic(20),
        randomAlphabetic(20),
        null,
        List.of(randomAlphabetic(10)),
        true,
        user.getUserId(),
        Instant.now(),
        UUID.randomUUID());
  }

  private void insertProperty(Integer studyId, String key, String value) {
    studyDAO.insertStudyProperty(studyId, key, PropertyType.Json.toString(), value);
  }

  private Optional<String> property(Integer studyId, String key) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "SELECT value FROM study_property WHERE study_id = :studyId AND key = :key")
                .bind("studyId", studyId)
                .bind("key", key)
                .mapTo(String.class)
                .findFirst());
  }

  private int propertyCount(Integer studyId, String key) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "SELECT COUNT(*) FROM study_property WHERE study_id = :studyId AND key = :key")
                .bind("studyId", studyId)
                .bind("key", key)
                .mapTo(Integer.class)
                .one());
  }
}
