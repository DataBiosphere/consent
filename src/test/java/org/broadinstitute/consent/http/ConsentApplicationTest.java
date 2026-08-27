package org.broadinstitute.consent.http;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dropwizard.db.DataSourceFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsentApplicationTest {

  private static final String URL = "jdbc:postgresql://localhost:5432/consent";
  private static final String USER = "consent";
  private static final String PASSWORD = "password";

  private final ConsentApplication consentApplication = new ConsentApplication();

  @Mock private ConsentConfiguration configuration;
  @Mock private DataSourceFactory dataSourceFactory;
  @Mock private Connection connection;
  @Mock private DatabaseFactory databaseFactory;
  @Mock private Database database;

  @BeforeEach
  void setUp() {
    when(configuration.getDataSourceFactory()).thenReturn(dataSourceFactory);
    when(dataSourceFactory.getUrl()).thenReturn(URL);
    when(dataSourceFactory.getUser()).thenReturn(USER);
    when(dataSourceFactory.getPassword()).thenReturn(PASSWORD);
  }

  @Test
  void initializeLiquibaseClosesConnectionAfterSuccessfulUpdate() throws Exception {
    try (var mockedDriverManager = mockStatic(DriverManager.class);
        var mockedDatabaseFactory = mockStatic(DatabaseFactory.class);
        var mockedLiquibase = mockConstruction(Liquibase.class)) {
      mockedDriverManager
          .when(() -> DriverManager.getConnection(URL, USER, PASSWORD))
          .thenReturn(connection);
      mockedDatabaseFactory.when(DatabaseFactory::getInstance).thenReturn(databaseFactory);
      when(databaseFactory.findCorrectDatabaseImplementation(any())).thenReturn(database);

      invokeInitializeLiquibase(configuration);

      verify(mockedLiquibase.constructed().getFirst())
          .update(any(Contexts.class), any(LabelExpression.class));
      verify(connection).close();
    }
  }

  @Test
  void initializeLiquibaseClosesConnectionWhenUpdateFails() throws Exception {
    LiquibaseException liquibaseException = new LiquibaseException("update failed");

    try (var mockedDriverManager = mockStatic(DriverManager.class);
        var mockedDatabaseFactory = mockStatic(DatabaseFactory.class);
        var mockedLiquibase =
            mockConstruction(
                Liquibase.class,
                (mock, context) -> {
                  doThrow(liquibaseException)
                      .when(mock)
                      .update(any(Contexts.class), any(LabelExpression.class));
                })) {
      mockedDriverManager
          .when(() -> DriverManager.getConnection(URL, USER, PASSWORD))
          .thenReturn(connection);
      mockedDatabaseFactory.when(DatabaseFactory::getInstance).thenReturn(databaseFactory);
      when(databaseFactory.findCorrectDatabaseImplementation(any())).thenReturn(database);

      LiquibaseException thrown =
          assertThrows(LiquibaseException.class, () -> invokeInitializeLiquibase(configuration));

      assertSame(liquibaseException, thrown);
      verify(mockedLiquibase.constructed().getFirst())
          .update(any(Contexts.class), any(LabelExpression.class));
      verify(connection).close();
    }
  }

  private void invokeInitializeLiquibase(ConsentConfiguration configuration) throws Exception {
    Method method =
        ConsentApplication.class.getDeclaredMethod(
            "initializeLiquibase", ConsentConfiguration.class);
    method.setAccessible(true);
    try {
      method.invoke(consentApplication, configuration);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof Exception exception) {
        throw exception;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw new RuntimeException(cause);
    }
  }
}
