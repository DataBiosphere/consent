package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

/**
 * Exercises the data migration in changelog-consent-2026-09-02-study-pi-institution-backfill.xml,
 * which copies the registration-time `piInstitution` study property into the
 * study.pi_institution_id column the study page reads.
 *
 * <p>Liquibase runs the changeset against an empty schema at test startup, which proves the SQL
 * parses but never exercises it against data. This test reads the SQL out of the shipped changeset
 * and runs it over registration-shaped rows, so the test cannot drift from what actually ships.
 */
class StudyPiInstitutionBackfillMigrationTest extends DAOTestHelper {

  private static final Path CHANGESET =
      Path.of(
          "src/main/resources/changesets/changelog-consent-2026-09-02-study-pi-institution-backfill.xml");

  @Test
  void testBackfillsThePiInstitutionColumnFromTheRegistrationProperty() throws Exception {
    User user = createUserWithInstitution();
    Institution institution = getUserInstitution(user);
    Integer studyId = insertStudy(user);
    insertProperty(studyId, "piInstitution", institution.getId().toString());

    runMigration();

    assertEquals(institution.getId(), piInstitutionId(studyId));
  }

  /** A study that never recorded a PI institution is left alone. */
  @Test
  void testLeavesAStudyWithoutThePropertyAlone() throws Exception {
    Integer studyId = insertStudy(createUserWithInstitution());

    runMigration();

    assertNull(piInstitutionId(studyId));
  }

  /**
   * The property is stored in a text column, and fk_study_pi_institution would reject an id with no
   * institution row, so both are skipped rather than failing the whole migration.
   */
  @Test
  void testSkipsValuesThatCannotBeAppliedToTheColumn() throws Exception {
    Integer nonNumeric = insertStudy(createUserWithInstitution());
    insertProperty(nonNumeric, "piInstitution", "Broad Institute");
    Integer orphanId = insertStudy(createUserWithInstitution());
    insertProperty(orphanId, "piInstitution", "-1");

    runMigration();

    assertNull(piInstitutionId(nonNumeric));
    assertNull(piInstitutionId(orphanId));
  }

  /** A PI institution already set on the column, by PATCH, is authoritative. */
  @Test
  void testDoesNotOverwriteAnAlreadyPopulatedColumn() throws Exception {
    User user = createUserWithInstitution();
    Institution institution = getUserInstitution(user);
    Integer studyId = insertStudy(user);
    studyDAO.updateStudyPiInstitutionId(studyId, institution.getId());
    // A stale registration property pointing somewhere else
    User otherUser = createUserWithInstitution();
    insertProperty(studyId, "piInstitution", getUserInstitution(otherUser).getId().toString());

    runMigration();

    assertEquals(institution.getId(), piInstitutionId(studyId));
  }

  @Test
  void testIsIdempotent() throws Exception {
    User user = createUserWithInstitution();
    Institution institution = getUserInstitution(user);
    Integer studyId = insertStudy(user);
    insertProperty(studyId, "piInstitution", institution.getId().toString());

    runMigration();
    runMigration();

    assertEquals(institution.getId(), piInstitutionId(studyId));
  }

  /** Runs every &lt;sql&gt; block from the shipped changeset, in order. */
  private void runMigration() throws Exception {
    String changeset = Files.readString(CHANGESET);
    Matcher matcher = Pattern.compile("<sql>(.*?)</sql>", Pattern.DOTALL).matcher(changeset);
    List<String> blocks = matcher.results().map(result -> result.group(1)).toList();
    assertEquals(1, blocks.size(), "changeset should ship one <sql> block");
    jdbi.useHandle(handle -> blocks.forEach(block -> handle.createScript(block.trim()).execute()));
  }

  private Integer insertStudy(User user) {
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
    studyDAO.insertStudyProperty(studyId, key, PropertyType.Number.toString(), value);
  }

  private Integer piInstitutionId(Integer studyId) {
    Optional<Integer> value =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("SELECT pi_institution_id FROM study WHERE study_id = :studyId")
                    .bind("studyId", studyId)
                    .mapTo(Integer.class)
                    .findFirst());
    return value.orElse(null);
  }
}
