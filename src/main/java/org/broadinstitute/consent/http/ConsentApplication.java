package org.broadinstitute.consent.http;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jersey3.InstrumentedResourceMethodApplicationListener;
import com.google.common.util.concurrent.UncaughtExceptionHandlers;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.PolymorphicAuthDynamicFeature;
import io.dropwizard.auth.PolymorphicAuthValueFactoryProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.jdbi3.bundles.JdbiExceptionsBundle;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.ui.LoggerUIService;
import liquibase.util.SmartMap;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.authentication.AuthorizationHelper;
import org.broadinstitute.consent.http.authentication.DuosUserAuthenticator;
import org.broadinstitute.consent.http.authentication.OAuthAuthenticator;
import org.broadinstitute.consent.http.authentication.OAuthCustomAuthFilter;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.filters.RateLimitFilter;
import org.broadinstitute.consent.http.filters.RequestHeaderCacheFilter;
import org.broadinstitute.consent.http.filters.ResponseServerFilter;
import org.broadinstitute.consent.http.health.EcmHealthCheck;
import org.broadinstitute.consent.http.health.ElasticSearchHealthCheck;
import org.broadinstitute.consent.http.health.GCSHealthCheck;
import org.broadinstitute.consent.http.health.SamHealthCheck;
import org.broadinstitute.consent.http.health.SendGridHealthCheck;
import org.broadinstitute.consent.http.mappers.ForbiddenExceptionMapper;
import org.broadinstitute.consent.http.mappers.JsonErrorHandler;
import org.broadinstitute.consent.http.mappers.NotFoundExceptionMapper;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.resources.DACAutomationRuleResource;
import org.broadinstitute.consent.http.resources.DaaResource;
import org.broadinstitute.consent.http.resources.DacDashboardResource;
import org.broadinstitute.consent.http.resources.DacResource;
import org.broadinstitute.consent.http.resources.DarCollectionResource;
import org.broadinstitute.consent.http.resources.DataAccessRequestResource;
import org.broadinstitute.consent.http.resources.DatasetResource;
import org.broadinstitute.consent.http.resources.DocumentResource;
import org.broadinstitute.consent.http.resources.DraftResource;
import org.broadinstitute.consent.http.resources.ElasticSearchCapabilityResource;
import org.broadinstitute.consent.http.resources.EmailNotifierResource;
import org.broadinstitute.consent.http.resources.FeatureFlagResource;
import org.broadinstitute.consent.http.resources.InstitutionResource;
import org.broadinstitute.consent.http.resources.LibraryCardResource;
import org.broadinstitute.consent.http.resources.LivenessResource;
import org.broadinstitute.consent.http.resources.MailResource;
import org.broadinstitute.consent.http.resources.MatchResource;
import org.broadinstitute.consent.http.resources.MetricsResource;
import org.broadinstitute.consent.http.resources.NihAccountResource;
import org.broadinstitute.consent.http.resources.OAuth2Resource;
import org.broadinstitute.consent.http.resources.OntologyResource;
import org.broadinstitute.consent.http.resources.PassportResource;
import org.broadinstitute.consent.http.resources.PublicFeatureFlagResource;
import org.broadinstitute.consent.http.resources.ResearcherDashboardResource;
import org.broadinstitute.consent.http.resources.SamResource;
import org.broadinstitute.consent.http.resources.SigningOfficialDashboardResource;
import org.broadinstitute.consent.http.resources.StatusResource;
import org.broadinstitute.consent.http.resources.StudyDatasetTemplateResource;
import org.broadinstitute.consent.http.resources.StudyResource;
import org.broadinstitute.consent.http.resources.SupportResource;
import org.broadinstitute.consent.http.resources.SwaggerResource;
import org.broadinstitute.consent.http.resources.TDRResource;
import org.broadinstitute.consent.http.resources.TosResource;
import org.broadinstitute.consent.http.resources.UserResource;
import org.broadinstitute.consent.http.resources.VersionResource;
import org.broadinstitute.consent.http.resources.VoteResource;
import org.broadinstitute.consent.http.util.gson.JerseyGsonProvider;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top-level entry point to the entire application.
 *
 * <p>See the Dropwizard docs here: <a
 * href="https://dropwizard.github.io">https://dropwizard.github.io</a>
 */
public class ConsentApplication extends Application<ConsentConfiguration> {

  public static final String GCS_CHECK = "google-cloud-storage";
  public static final String ES_CHECK = "elastic-search";
  public static final String ECM_CHECK = "ecm";
  public static final String SAM_CHECK = "sam";
  public static final String SG_CHECK = "sendgrid";
  private static final Logger LOGGER = LoggerFactory.getLogger("ConsentApplication");

  public static void main(String[] args) throws Exception {
    LOGGER.info("Starting Consent Application");
    try {
      String dsn = System.getProperties().getProperty("sentry.dsn");
      if (StringUtils.isNotBlank(dsn)) {
        Sentry.init(
            config -> {
              config.setDsn(dsn);
              config.setDiagnosticLevel(SentryLevel.ERROR);
              config.setServerName("Consent");
              config.addContextTag("Consent");
              config.addInAppInclude("org.broadinstitute");
            });
        Thread.currentThread().setUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());
      } else {
        LOGGER.error("Unable to bootstrap sentry logging.");
      }
    } catch (Exception e) {
      LOGGER.error(
          MessageFormat.format("Exception loading sentry properties: {0}", e.getMessage()));
    }
    new ConsentApplication().run(args);
    LOGGER.info("Consent Application Started");
  }

  @Override
  public void run(ConsentConfiguration config, Environment env) {

    try {
      initializeLiquibase(config);
    } catch (LiquibaseException | SQLException e) {
      LOGGER.error(MessageFormat.format("Exception initializing liquibase: {0}", e));
    }

    final Injector injector = Guice.createInjector(new ConsentModule(config, env));
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    env.jersey().register(JerseyGsonProvider.class);

    // Metric Registry
    MetricRegistry metricRegistry = new MetricRegistry();
    env.jersey().register(new InstrumentedResourceMethodApplicationListener(metricRegistry));

    // Health Checks
    env.healthChecks().register(GCS_CHECK, injector.getInstance(GCSHealthCheck.class));
    env.healthChecks().register(ES_CHECK, injector.getInstance(ElasticSearchHealthCheck.class));
    env.healthChecks().register(ECM_CHECK, injector.getInstance(EcmHealthCheck.class));
    env.healthChecks().register(SAM_CHECK, injector.getInstance(SamHealthCheck.class));
    env.healthChecks().register(SG_CHECK, injector.getInstance(SendGridHealthCheck.class));

    // Custom error handling for exceptions that would otherwise reach Jetty with an empty
    // response body. Expand to include other codes when necessary.
    env.jersey().register(NotFoundExceptionMapper.class);
    env.jersey().register(ForbiddenExceptionMapper.class);
    // Last-resort net for requests that never reach Jersey's dispatch at all (e.g. a path that
    // matches no @Path template), which the ExceptionMappers above cannot intercept.
    env.getApplicationContext().setErrorHandler(new JsonErrorHandler());
    env.jersey().register(ResponseServerFilter.class);

    // Register standard application resources.
    env.jersey().register(injector.getInstance(DaaResource.class));
    env.jersey().register(injector.getInstance(DACAutomationRuleResource.class));
    env.jersey().register(injector.getInstance(DacDashboardResource.class));
    env.jersey().register(injector.getInstance(DacResource.class));
    env.jersey().register(injector.getInstance(DarCollectionResource.class));
    env.jersey().register(injector.getInstance(DataAccessRequestResource.class));
    env.jersey().register(injector.getInstance(DatasetResource.class));
    env.jersey().register(injector.getInstance(DocumentResource.class));
    env.jersey().register(injector.getInstance(DraftResource.class));
    env.jersey().register(injector.getInstance(ElasticSearchCapabilityResource.class));
    env.jersey().register(injector.getInstance(EmailNotifierResource.class));
    env.jersey().register(injector.getInstance(FeatureFlagResource.class));
    env.jersey().register(injector.getInstance(PublicFeatureFlagResource.class));
    env.jersey().register(injector.getInstance(InstitutionResource.class));
    env.jersey().register(injector.getInstance(LibraryCardResource.class));
    env.jersey().register(injector.getInstance(LivenessResource.class));
    env.jersey().register(injector.getInstance(MailResource.class));
    env.jersey().register(injector.getInstance(MatchResource.class));
    env.jersey().register(injector.getInstance(MetricsResource.class));
    env.jersey().register(injector.getInstance(NihAccountResource.class));
    env.jersey().register(injector.getInstance(OAuth2Resource.class));
    env.jersey().register(injector.getInstance(OntologyResource.class));
    env.jersey().register(injector.getInstance(PassportResource.class));
    env.jersey().register(injector.getInstance(ResearcherDashboardResource.class));
    env.jersey().register(injector.getInstance(SamResource.class));
    env.jersey().register(injector.getInstance(SigningOfficialDashboardResource.class));
    env.jersey().register(injector.getInstance(SwaggerResource.class));
    env.jersey().register(injector.getInstance(StatusResource.class));
    env.jersey().register(injector.getInstance(StudyDatasetTemplateResource.class));
    env.jersey().register(injector.getInstance(StudyResource.class));
    env.jersey().register(injector.getInstance(SupportResource.class));
    env.jersey().register(injector.getInstance(TDRResource.class));
    env.jersey().register(injector.getInstance(TosResource.class));
    env.jersey().register(injector.getInstance(UserResource.class));
    env.jersey().register(injector.getInstance(VersionResource.class));
    env.jersey().register(injector.getInstance(VoteResource.class));

    // Authentication filters
    final OAuthAuthenticator authenticator = injector.getProvider(OAuthAuthenticator.class).get();
    final DuosUserAuthenticator duosUserAuthenticator =
        injector.getProvider(DuosUserAuthenticator.class).get();
    final AuthorizationHelper authorizationHelper =
        injector.getProvider(AuthorizationHelper.class).get();
    // Requests annotated with @Auth AuthUser will be authenticated through this filter
    final AuthFilter<String, AuthUser> primaryAuthFilter =
        new OAuthCustomAuthFilter<>(authenticator, authorizationHelper);
    // Requests annotated with @Auth DuosUser will be authenticated through this filter and are
    // guaranteed to have a populated User object
    final AuthFilter<String, DuosUser> duosAuthUserFilter =
        new OAuthCustomAuthFilter<>(duosUserAuthenticator, authorizationHelper);
    final PolymorphicAuthDynamicFeature<AuthUser> feature =
        new PolymorphicAuthDynamicFeature<>(
            Map.of(
                AuthUser.class, primaryAuthFilter,
                DuosUser.class, duosAuthUserFilter));
    final AbstractBinder binder =
        new PolymorphicAuthValueFactoryProvider.Binder<>(Set.of(AuthUser.class, DuosUser.class));
    env.jersey().register(feature);
    env.jersey().register(binder);

    // Filters and providers that have @Inject dependencies must be registered via
    // injector.getInstance() so Guice performs field/constructor injection. Class-literal
    // registration (e.g. RolesAllowedDynamicFeature.class below) is only safe for classes
    // with a public no-arg constructor and no @Inject dependencies.
    env.jersey().register(injector.getInstance(RequestHeaderCacheFilter.class));
    env.jersey().register(injector.getInstance(RateLimitFilter.class));
    env.jersey().register(RolesAllowedDynamicFeature.class);
  }

  @Override
  public void initialize(Bootstrap<ConsentConfiguration> bootstrap) {
    bootstrap.addBundle(new AssetsBundle("/assets/", "/api-docs", "index.html"));
    bootstrap.addBundle(new MultiPartBundle());
    bootstrap.addBundle(new JdbiExceptionsBundle());
  }

  private void initializeLiquibase(ConsentConfiguration config)
      throws LiquibaseException, SQLException {
    // Disable Liquibase's System.out logging.
    // See https://github.com/liquibase/liquibase/issues/2396 for more info
    try {
      Field field = Scope.getCurrentScope().getClass().getDeclaredField("values");
      field.setAccessible(true);
      SmartMap values = ((SmartMap) field.get(Scope.getCurrentScope()));
      values.set("ui", new LoggerUIService());
    } catch (IllegalAccessException | NoSuchFieldException ignored) {
    }
    Connection connection =
        DriverManager.getConnection(
            config.getDataSourceFactory().getUrl(),
            config.getDataSourceFactory().getUser(),
            config.getDataSourceFactory().getPassword());
    Database database =
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(connection));
    Liquibase liquibase =
        new Liquibase(liquibaseFile(), new ClassLoaderResourceAccessor(), database);
    liquibase.update(new Contexts(), new LabelExpression());
  }

  private String liquibaseFile() {
    String changeLogFile = System.getenv("CONSENT_CHANGELOG_FILE");
    if (Objects.isNull(changeLogFile) || changeLogFile.trim().isEmpty()) {
      changeLogFile = "changelog-master.xml";
    }
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(MessageFormat.format("Initializing db with: {0}", changeLogFile));
    }
    return changeLogFile;
  }
}
