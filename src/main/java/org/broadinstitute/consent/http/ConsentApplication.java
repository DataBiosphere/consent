package org.broadinstitute.consent.http;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jersey3.InstrumentedResourceMethodApplicationListener;
import com.google.common.util.concurrent.UncaughtExceptionHandlers;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.chained.ChainedAuthFilter;
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
import java.util.List;
import java.util.Objects;
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
import org.broadinstitute.consent.http.authentication.DefaultAuthFilter;
import org.broadinstitute.consent.http.authentication.DefaultAuthenticator;
import org.broadinstitute.consent.http.authentication.OAuthAuthenticator;
import org.broadinstitute.consent.http.authentication.OAuthCustomAuthFilter;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.filters.RequestHeaderCacheFilter;
import org.broadinstitute.consent.http.filters.ResponseServerFilter;
import org.broadinstitute.consent.http.health.ElasticSearchHealthCheck;
import org.broadinstitute.consent.http.health.GCSHealthCheck;
import org.broadinstitute.consent.http.health.OntologyHealthCheck;
import org.broadinstitute.consent.http.health.SamHealthCheck;
import org.broadinstitute.consent.http.health.SendGridHealthCheck;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.resources.DACUserResource;
import org.broadinstitute.consent.http.resources.DaaResource;
import org.broadinstitute.consent.http.resources.DacResource;
import org.broadinstitute.consent.http.resources.DarCollectionResource;
import org.broadinstitute.consent.http.resources.DataAccessRequestResource;
import org.broadinstitute.consent.http.resources.DatasetResource;
import org.broadinstitute.consent.http.resources.DraftSubmissionResource;
import org.broadinstitute.consent.http.resources.EmailNotifierResource;
import org.broadinstitute.consent.http.resources.ErrorResource;
import org.broadinstitute.consent.http.resources.InstitutionResource;
import org.broadinstitute.consent.http.resources.LibraryCardResource;
import org.broadinstitute.consent.http.resources.LivenessResource;
import org.broadinstitute.consent.http.resources.MailResource;
import org.broadinstitute.consent.http.resources.MatchResource;
import org.broadinstitute.consent.http.resources.MetricsResource;
import org.broadinstitute.consent.http.resources.NihAccountResource;
import org.broadinstitute.consent.http.resources.OAuth2Resource;
import org.broadinstitute.consent.http.resources.SamResource;
import org.broadinstitute.consent.http.resources.SchemaResource;
import org.broadinstitute.consent.http.resources.StatusResource;
import org.broadinstitute.consent.http.resources.StudyResource;
import org.broadinstitute.consent.http.resources.SwaggerResource;
import org.broadinstitute.consent.http.resources.TDRResource;
import org.broadinstitute.consent.http.resources.TosResource;
import org.broadinstitute.consent.http.resources.UserResource;
import org.broadinstitute.consent.http.resources.VersionResource;
import org.broadinstitute.consent.http.resources.VoteResource;
import org.broadinstitute.consent.http.service.AcknowledgementService;
import org.broadinstitute.consent.http.service.DarCollectionService;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetRegistrationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.DraftSubmissionService;
import org.broadinstitute.consent.http.service.ElasticSearchService;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.EmailService;
import org.broadinstitute.consent.http.service.InstitutionService;
import org.broadinstitute.consent.http.service.LibraryCardService;
import org.broadinstitute.consent.http.service.MatchService;
import org.broadinstitute.consent.http.service.MetricsService;
import org.broadinstitute.consent.http.service.NihService;
import org.broadinstitute.consent.http.service.OidcService;
import org.broadinstitute.consent.http.service.TDRService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.VoteService;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.gson.JerseyGsonProvider;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top-level entry point to the entire application.
 * <p>
 * See the Dropwizard docs here:
 * <a href="https://dropwizard.github.io">https://dropwizard.github.io</a>
 */
public class ConsentApplication extends Application<ConsentConfiguration> {

  public static final String GCS_CHECK = "google-cloud-storage";
  public static final String ES_CHECK = "elastic-search";
  public static final String ONTOLOGY_CHECK = "ontology";
  public static final String SAM_CHECK = "sam";
  public static final String SG_CHECK = "sendgrid";
  private static final Logger LOGGER = LoggerFactory.getLogger("ConsentApplication");

  public static void main(String[] args) throws Exception {
    LOGGER.info("Starting Consent Application");
    try {
      String dsn = System.getProperties().getProperty("sentry.dsn");
      if (StringUtils.isNotBlank(dsn)) {
        Sentry.init(config -> {
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
      LOGGER.error("Exception loading sentry properties: " + e.getMessage());
    }
    new ConsentApplication().run(args);
    LOGGER.info("Consent Application Started");
  }

  @Override
  public void run(ConsentConfiguration config, Environment env) {

    try {
      initializeLiquibase(config);
    } catch (LiquibaseException | SQLException e) {
      LOGGER.error("Exception initializing liquibase: " + e);
    }

    // Previously, this code was working around a dropwizard+Guice issue with singletons and JDBI.
    final Injector injector = Guice.createInjector(new ConsentModule(config, env));

    // Clients
    final HttpClientUtil clientUtil = new HttpClientUtil(config.getServicesConfiguration());

    // Services
    final DarCollectionService darCollectionService = injector.getProvider(
        DarCollectionService.class).get();
    final DataAccessRequestService dataAccessRequestService = injector.getProvider(
        DataAccessRequestService.class).get();
    final DatasetService datasetService = injector.getProvider(DatasetService.class).get();
    final ElectionService electionService = injector.getProvider(ElectionService.class).get();
    final EmailService emailService = injector.getProvider(EmailService.class).get();
    final GCSService gcsService = injector.getProvider(GCSService.class).get();
    final InstitutionService institutionService = injector.getProvider(InstitutionService.class)
        .get();
    final MetricsService metricsService = injector.getProvider(MetricsService.class).get();
    final UserService userService = injector.getProvider(UserService.class).get();
    final VoteService voteService = injector.getProvider(VoteService.class).get();
    final MatchService matchService = injector.getProvider(MatchService.class).get();
    final OAuthAuthenticator authenticator = injector.getProvider(OAuthAuthenticator.class).get();
    final LibraryCardService libraryCardService = injector.getProvider(LibraryCardService.class)
        .get();
    final SamService samService = injector.getProvider(SamService.class).get();
    final TDRService tdrService = injector.getProvider(TDRService.class).get();
    final AcknowledgementService acknowledgementService = injector.getProvider(
        AcknowledgementService.class).get();
    final DatasetRegistrationService datasetRegistrationService = injector.getProvider(
        DatasetRegistrationService.class).get();
    final ElasticSearchService elasticSearchService = injector.getProvider(
        ElasticSearchService.class).get();
    final OidcService oidcService = injector.getProvider(OidcService.class).get();
    final DraftSubmissionService draftSubmissionService = injector.getProvider(
        DraftSubmissionService.class).get();

    System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

    env.jersey().register(JerseyGsonProvider.class);

    // Metric Registry
    MetricRegistry metricRegistry = new MetricRegistry();
    env.jersey().register(new InstrumentedResourceMethodApplicationListener(metricRegistry));

    // Health Checks
    env.healthChecks().register(GCS_CHECK, new GCSHealthCheck(gcsService));
    env.healthChecks()
        .register(ES_CHECK, new ElasticSearchHealthCheck(config.getElasticSearchConfiguration()));
    env.healthChecks().register(ONTOLOGY_CHECK,
        new OntologyHealthCheck(clientUtil, config.getServicesConfiguration()));
    env.healthChecks()
        .register(SAM_CHECK, new SamHealthCheck(clientUtil, config.getServicesConfiguration()));
    env.healthChecks()
        .register(SG_CHECK, new SendGridHealthCheck(clientUtil, config.getMailConfiguration()));

    final NihService nihService = injector.getProvider(NihService.class).get();
    // Custom Error handling. Expand to include other codes when necessary
    final ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
    errorHandler.addErrorPage(404, "/error/404");
    env.getApplicationContext().setErrorHandler(errorHandler);
    env.jersey().register(ResponseServerFilter.class);
    env.jersey().register(ErrorResource.class);

    // Register standard application resources.
    env.jersey().register(injector.getInstance(DaaResource.class));
    env.jersey().register(injector.getInstance(DataAccessRequestResource.class));
    env.jersey().register(new DatasetResource(datasetService, userService,
        datasetRegistrationService, elasticSearchService));
    env.jersey().register(injector.getInstance(DacResource.class));
    env.jersey().register(new DACUserResource(userService));
    env.jersey().register(
        new DarCollectionResource(darCollectionService, userService));
    env.jersey().register(new EmailNotifierResource(emailService));
    env.jersey().register(new InstitutionResource(userService, institutionService));
    env.jersey().register(new LibraryCardResource(userService, libraryCardService));
    env.jersey().register(new MatchResource(matchService));
    env.jersey().register(new MetricsResource(metricsService));
    env.jersey().register(new NihAccountResource(nihService, userService));
    env.jersey().register(new SamResource(samService, userService));
    env.jersey().register(new SchemaResource());
    env.jersey().register(new SwaggerResource(config.getGoogleAuthentication()));
    env.jersey().register(new StatusResource(env.healthChecks()));
    env.jersey().register(
        new UserResource(samService, userService, datasetService, acknowledgementService));
    env.jersey().register(new TosResource(samService));
    env.jersey().register(injector.getInstance(VersionResource.class));
    env.jersey().register(new VoteResource(userService, voteService, electionService));
    env.jersey().register(new LivenessResource());
    env.jersey().register(
        new TDRResource(tdrService, datasetService, userService, dataAccessRequestService));
    env.jersey().register(new MailResource(emailService));
    env.jersey().register(injector.getInstance(StudyResource.class));
    env.jersey().register(new OAuth2Resource(oidcService));
    env.jersey().register(new DraftSubmissionResource(userService, draftSubmissionService));

    // Authentication filters
    final UserRoleDAO userRoleDAO = injector.getProvider(UserRoleDAO.class).get();
    AuthFilter defaultAuthFilter = new DefaultAuthFilter.Builder<AuthUser>()
        .setAuthenticator(new DefaultAuthenticator())
        .setRealm(" ")
        .buildAuthFilter();
    List<AuthFilter> filters = List.of(
        defaultAuthFilter,
        new OAuthCustomAuthFilter(authenticator, userRoleDAO));
    env.jersey().register(RequestHeaderCacheFilter.class);
    env.jersey().register(new AuthDynamicFeature(new ChainedAuthFilter(filters)));
    env.jersey().register(RolesAllowedDynamicFeature.class);
    env.jersey().register(new AuthValueFactoryProvider.Binder<>(AuthUser.class));
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
    Connection connection = DriverManager.getConnection(
        config.getDataSourceFactory().getUrl(),
        config.getDataSourceFactory().getUser(),
        config.getDataSourceFactory().getPassword()
    );
    Database database = DatabaseFactory.getInstance()
        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
    Liquibase liquibase = new Liquibase(liquibaseFile(), new ClassLoaderResourceAccessor(),
        database);
    liquibase.update(new Contexts(), new LabelExpression());
  }

  private String liquibaseFile() {
    String changeLogFile = System.getenv("CONSENT_CHANGELOG_FILE");
    if (Objects.isNull(changeLogFile) || changeLogFile.trim().isEmpty()) {
      changeLogFile = "changelog-master.xml";
    }
    LOGGER.info("Initializing db with: " + changeLogFile);
    return changeLogFile;
  }

}
