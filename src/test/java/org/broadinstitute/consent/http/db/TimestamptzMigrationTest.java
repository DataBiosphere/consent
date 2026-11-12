package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The DT-4452 changesets, one for each table. The test finds each changeset file and reads its
 * table and columns, so a new changeset file needs no change here. Each test builds the table with
 * only those columns and one row at 23:30 UTC. Liquibase runs with the JVM zone set to New York, so
 * a changeset that did not set the session zone to UTC would move the stored moments.
 */
class TimestamptzMigrationTest extends MigrationTestHelper {

  private static final String PREFIX = "changelog-consent-2026-11-09-timestamptz-";
  private static final Pattern TABLE = Pattern.compile("ALTER TABLE (\\w+)\\s");
  private static final Pattern COLUMN =
      Pattern.compile("ALTER COLUMN (\\w+) TYPE timestamp with time zone");
  private static final String PLAIN = "timestamp without time zone";
  private static final String WITH_ZONE = "timestamp with time zone";
  private static final String MOMENT = "TIMESTAMPTZ '2026-11-09 23:30:00+00'";

  private final TimeZone originalZone = TimeZone.getDefault();
  private String changelog;

  /** One changeset file: its classpath location, its table and its columns. */
  record Change(String changelog, String table, List<String> columns) {

    @Override
    public String toString() {
      return table;
    }
  }

  static Stream<Change> changes() throws Exception {
    Path directory =
        Path.of(TimestamptzMigrationTest.class.getClassLoader().getResource("changesets").toURI());
    try (Stream<Path> files = Files.list(directory)) {
      List<Change> changes =
          files
              .filter(p -> p.getFileName().toString().startsWith(PREFIX))
              .sorted()
              .map(TimestamptzMigrationTest::change)
              .toList();
      assertFalse(changes.isEmpty(), "No DT-4452 changeset files found");
      return changes.stream();
    }
  }

  private static Change change(Path path) {
    try {
      String xml = Files.readString(path);
      Matcher table = TABLE.matcher(xml);
      assertTrue(table.find(), "No ALTER TABLE in " + path);
      List<String> columns = COLUMN.matcher(xml).results().map(m -> m.group(1)).toList();
      return new Change("changesets/" + path.getFileName(), table.group(1), columns);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  protected String changelog() {
    return changelog;
  }

  @Override
  protected void createPreMigrationSchema() {
    // Empty: each test builds its own table, because the table comes from the test parameter.
  }

  @BeforeEach
  void useNewYork() {
    TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
  }

  @AfterEach
  void restoreZone() {
    TimeZone.setDefault(originalZone);
  }

  /** The stored clock time keeps its UTC meaning. */
  @ParameterizedTest
  @MethodSource("changes")
  void testConvertsStoredValuesAsUtc(Change change) throws Exception {
    createTable(change, PLAIN);
    update();
    assertColumns(change, WITH_ZONE, MOMENT);
  }

  /** A deployed column can already be timestamptz (DT-4457). It keeps its value. */
  @ParameterizedTest
  @MethodSource("changes")
  void testKeepsColumnThatIsAlreadyTimestamptz(Change change) throws Exception {
    createTable(change, WITH_ZONE);
    update();
    assertColumns(change, WITH_ZONE, MOMENT);
  }

  /** A column with another type means that the schema is not as expected, so the deploy halts. */
  @ParameterizedTest
  @MethodSource("changes")
  void testHaltsOnDateColumn(Change change) throws Exception {
    createTable(change, "date");
    assertThrows(LiquibaseException.class, this::update);
    assertEquals(1, typeCount(change, "date"));
  }

  /** The rollback gives back timestamp columns with the same clock times. */
  @ParameterizedTest
  @MethodSource("changes")
  void testRollbackRestoresTimestamp(Change change) throws Exception {
    createTable(change, PLAIN);
    update();
    rollback();
    assertColumns(change, PLAIN, "TIMESTAMP '2026-11-09 23:30:00'");
  }

  /**
   * Builds the table with one row. The first column gets {@code firstType}, and the others are
   * timestamp. The +00 offset fixes the moment for a timestamptz column, and a timestamp or date
   * column ignores it, so the session zone does not change the row.
   */
  private void createTable(Change change, String firstType) throws Exception {
    changelog = change.changelog();
    List<String> columns = change.columns();
    String definitions =
        columns.stream()
            .map(c -> c + " " + (c.equals(columns.getFirst()) ? firstType : PLAIN))
            .collect(Collectors.joining(", "));
    String values =
        columns.stream().map(c -> "'2026-11-09 23:30:00+00'").collect(Collectors.joining(", "));
    String names = String.join(", ", columns);
    execute("CREATE TABLE %s (%s)".formatted(change.table(), definitions));
    execute("INSERT INTO %s (%s) VALUES (%s)".formatted(change.table(), names, values));
  }

  /** Each column of the change has the type, and its value equals the expected value. */
  private void assertColumns(Change change, String type, String expected) throws Exception {
    assertEquals(change.columns().size(), typeCount(change, type));
    String checks =
        change.columns().stream()
            .map(c -> c + " = " + expected)
            .collect(Collectors.joining(" AND "));
    assertTrue(queryBoolean("SELECT %s FROM %s".formatted(checks, change.table())));
  }

  private long typeCount(Change change, String type) throws Exception {
    return queryLong(
        """
        SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = ?
          AND column_name = ANY(string_to_array(?, ',')) AND data_type = ?
        """,
        change.table(),
        String.join(",", change.columns()),
        type);
  }
}
