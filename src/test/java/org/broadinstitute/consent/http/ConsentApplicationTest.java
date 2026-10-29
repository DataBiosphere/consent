package org.broadinstitute.consent.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.inject.Injector;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.atomic.AtomicReference;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.analytics.configuration.AnalyticsArgs;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.configurations.LiquibaseConfiguration;
import org.broadinstitute.consent.http.configurations.ShowcaseConfiguration;
import org.broadinstitute.consent.http.resources.PublicShowcaseCatalogResource;
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

  @Test
  void runLiquibaseIfEnabledSkipsLiquibaseByDefault() throws Exception {
    when(configuration.getLiquibaseConfiguration()).thenReturn(new LiquibaseConfiguration());

    try (var mockedDriverManager = mockStatic(DriverManager.class);
        var mockedLiquibase = mockConstruction(Liquibase.class)) {
      invoke("runLiquibaseIfEnabled", configuration);

      mockedDriverManager.verify(() -> DriverManager.getConnection(any(), any(), any()), never());
      assertEquals(0, mockedLiquibase.constructed().size());
    }
  }

  @Test
  void runLiquibaseIfEnabledRunsUpdateWhenEnabled() throws Exception {
    LiquibaseConfiguration liquibaseConfiguration = new LiquibaseConfiguration();
    liquibaseConfiguration.setRunOnStartup(true);
    when(configuration.getLiquibaseConfiguration()).thenReturn(liquibaseConfiguration);
    stubDataSource();

    try (var mockedDriverManager = mockStatic(DriverManager.class);
        var mockedDatabaseFactory = mockStatic(DatabaseFactory.class);
        var mockedLiquibase = mockConstruction(Liquibase.class)) {
      mockedDriverManager
          .when(() -> DriverManager.getConnection(URL, USER, PASSWORD))
          .thenReturn(connection);
      mockedDatabaseFactory.when(DatabaseFactory::getInstance).thenReturn(databaseFactory);
      when(databaseFactory.findCorrectDatabaseImplementation(any())).thenReturn(database);

      invoke("runLiquibaseIfEnabled", configuration);

      verify(mockedLiquibase.constructed().getFirst())
          .update(any(Contexts.class), any(LabelExpression.class));
    }
  }

  @Test
  void initializeLiquibaseDisablesAnalytics() throws Exception {
    // Scope values take precedence over system properties, so the update must see false even when
    // the JVM enables analytics.
    String analyticsKey = AnalyticsArgs.ENABLED.getKey();
    String previous = System.setProperty(analyticsKey, "true");
    AtomicReference<Boolean> analyticsDuringUpdate = new AtomicReference<>();
    stubDataSource();
    try (var mockedDriverManager = mockStatic(DriverManager.class);
        var mockedDatabaseFactory = mockStatic(DatabaseFactory.class);
        var mockedLiquibase =
            mockConstruction(
                Liquibase.class,
                (mock, context) ->
                    doAnswer(
                            invocation -> {
                              analyticsDuringUpdate.set(AnalyticsArgs.ENABLED.getCurrentValue());
                              return null;
                            })
                        .when(mock)
                        .update(any(Contexts.class), any(LabelExpression.class)))) {
      mockedDriverManager
          .when(() -> DriverManager.getConnection(URL, USER, PASSWORD))
          .thenReturn(connection);
      mockedDatabaseFactory.when(DatabaseFactory::getInstance).thenReturn(databaseFactory);
      when(databaseFactory.findCorrectDatabaseImplementation(any())).thenReturn(database);

      invokeInitializeLiquibase(configuration);

      verify(mockedLiquibase.constructed().getFirst())
          .update(any(Contexts.class), any(LabelExpression.class));
      assertFalse(analyticsDuringUpdate.get());
    } finally {
      if (previous == null) {
        System.clearProperty(analyticsKey);
      } else {
        System.setProperty(analyticsKey, previous);
      }
    }
  }

  @Test
  void initializeLiquibaseClosesConnectionAfterSuccessfulUpdate() throws Exception {
    stubDataSource();
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
    stubDataSource();

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

  // Only the tests that open a connection read the datasource, so they stub it themselves. This
  // keeps strict stubbing: a skipped run never reads these values.
  private void stubDataSource() {
    when(configuration.getDataSourceFactory()).thenReturn(dataSourceFactory);
    when(dataSourceFactory.getUrl()).thenReturn(URL);
    when(dataSourceFactory.getUser()).thenReturn(USER);
    when(dataSourceFactory.getPassword()).thenReturn(PASSWORD);
  }

  @Test
  void registerPublicShowcaseLookupSkipsTheRouteByDefault() {
    when(configuration.getShowcaseConfiguration()).thenReturn(new ShowcaseConfiguration());
    JerseyEnvironment jersey = mock(JerseyEnvironment.class);
    Injector injector = mock(Injector.class);

    ConsentApplication.registerPublicShowcaseLookup(configuration, jersey, injector);

    verifyNoInteractions(jersey, injector);
  }

  @Test
  void registerPublicShowcaseLookupAddsTheRouteWhenEnabled() {
    ShowcaseConfiguration showcase = new ShowcaseConfiguration();
    showcase.setPublicLookupEnabled(true);
    when(configuration.getShowcaseConfiguration()).thenReturn(showcase);
    JerseyEnvironment jersey = mock(JerseyEnvironment.class);
    Injector injector = mock(Injector.class);
    PublicShowcaseCatalogResource resource = mock(PublicShowcaseCatalogResource.class);
    when(injector.getInstance(PublicShowcaseCatalogResource.class)).thenReturn(resource);

    ConsentApplication.registerPublicShowcaseLookup(configuration, jersey, injector);

    verify(jersey).register(resource);
  }

  private void invokeInitializeLiquibase(ConsentConfiguration configuration) throws Exception {
    invoke("initializeLiquibase", configuration);
  }

  private void invoke(String methodName, ConsentConfiguration configuration) throws Exception {
    Method method =
        ConsentApplication.class.getDeclaredMethod(methodName, ConsentConfiguration.class);
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
