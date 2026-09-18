package org.broadinstitute.consent.http.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Testcontainers and Liquibase plumbing for the standalone changeset tests, which run a single
 * changelog against the schema it expects to find rather than against the whole master changelog. A
 * subclass supplies only its changelog and that schema.
 */
abstract class MigrationTestHelper {

  // Shared by every subclass, and never stopped: Ryuk reaps it at JVM exit, and a container per
  // test class costs CI a start-up each.
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DAOTestHelper.POSTGRES_IMAGE);

  static {
    POSTGRES.start();
  }

  /** Classpath location of the changelog under test. */
  protected abstract String changelog();

  /** Builds the schema the changelog expects, over an emptied public schema. */
  protected abstract void createPreMigrationSchema() throws Exception;

  @BeforeEach
  void resetSchema() throws Exception {
    execute("DROP SCHEMA public CASCADE");
    execute("CREATE SCHEMA public");
    createPreMigrationSchema();
  }

  protected void update() throws Exception {
    update(changelog());
  }

  protected void update(String changelog) throws Exception {
    withLiquibase(changelog, liquibase -> liquibase.update(new Contexts(), new LabelExpression()));
  }

  protected void rollback() throws Exception {
    withLiquibase(
        changelog(), liquibase -> liquibase.rollback(1, new Contexts(), new LabelExpression()));
  }

  private void withLiquibase(String changelog, LiquibaseAction action) throws Exception {
    try (Connection connection = connection()) {
      Database database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (Liquibase liquibase =
          new Liquibase(changelog, new ClassLoaderResourceAccessor(), database)) {
        action.accept(liquibase);
      }
    }
  }

  @FunctionalInterface
  private interface LiquibaseAction {

    void accept(Liquibase liquibase) throws LiquibaseException;
  }

  protected static Connection connection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  protected void execute(String sql) throws SQLException {
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  protected long queryLong(String sql, Object... params) throws SQLException {
    return ((Number) queryObject(sql, params)).longValue();
  }

  protected boolean queryBoolean(String sql, Object... params) throws SQLException {
    return (Boolean) queryObject(sql, params);
  }

  /**
   * The first column of the first row, or null when the query returned no rows. Anything that
   * varies between calls belongs in params rather than in the concatenated SQL.
   */
  protected Object queryObject(String sql, Object... params) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++) {
        statement.setObject(i + 1, params[i]);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? resultSet.getObject(1) : null;
      }
    }
  }
}
