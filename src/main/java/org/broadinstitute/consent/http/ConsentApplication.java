package org.broadinstitute.consent.http;

import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.jdbi3.bundles.JdbiExceptionsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.authentication.BasicAuthenticator;
import org.broadinstitute.consent.http.authentication.BasicCustomAuthFilter;
import org.broadinstitute.consent.http.authentication.DefaultAuthFilter;
import org.broadinstitute.consent.http.authentication.DefaultAuthenticator;
import org.broadinstitute.consent.http.authentication.OAuthAuthenticator;
import org.broadinstitute.consent.http.authentication.OAuthCustomAuthFilter;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.filters.ResponseServerFilter;
import org.broadinstitute.consent.http.health.ElasticSearchHealthCheck;
import org.broadinstitute.consent.http.health.GCSHealthCheck;
import org.broadinstitute.consent.http.health.OntologyHealthCheck;
import org.broadinstitute.consent.http.health.SamHealthCheck;
import org.broadinstitute.consent.http.health.SendGridHealthCheck;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.resources.ConsentAssociationResource;
import org.broadinstitute.consent.http.resources.ConsentCasesResource;
import org.broadinstitute.consent.http.resources.ConsentElectionResource;
import org.broadinstitute.consent.http.resources.ConsentResource;
import org.broadinstitute.consent.http.resources.ConsentVoteResource;
import org.broadinstitute.consent.http.resources.DACUserResource;
import org.broadinstitute.consent.http.resources.DacResource;
import org.broadinstitute.consent.http.resources.DarCollectionResource;
import org.broadinstitute.consent.http.resources.DataAccessRequestResource;
import org.broadinstitute.consent.http.resources.DataAccessRequestResourceVersion2;
import org.broadinstitute.consent.http.resources.DataRequestCasesResource;
import org.broadinstitute.consent.http.resources.DataRequestElectionResource;
import org.broadinstitute.consent.http.resources.DataRequestReportsResource;
import org.broadinstitute.consent.http.resources.DataRequestVoteResource;
import org.broadinstitute.consent.http.resources.DataUseLetterResource;
import org.broadinstitute.consent.http.resources.DatasetAssociationsResource;
import org.broadinstitute.consent.http.resources.DatasetResource;
import org.broadinstitute.consent.http.resources.ElectionResource;
import org.broadinstitute.consent.http.resources.ElectionReviewResource;
import org.broadinstitute.consent.http.resources.EmailNotifierResource;
import org.broadinstitute.consent.http.resources.ErrorResource;
import org.broadinstitute.consent.http.resources.IndexerResource;
import org.broadinstitute.consent.http.resources.InstitutionResource;
import org.broadinstitute.consent.http.resources.LibraryCardResource;
import org.broadinstitute.consent.http.resources.LivenessResource;
import org.broadinstitute.consent.http.resources.MatchResource;
import org.broadinstitute.consent.http.resources.MetricsResource;
import org.broadinstitute.consent.http.resources.NihAccountResource;
import org.broadinstitute.consent.http.resources.SamResource;
import org.broadinstitute.consent.http.resources.StatusResource;
import org.broadinstitute.consent.http.resources.SwaggerResource;
import org.broadinstitute.consent.http.resources.TDRResource;
import org.broadinstitute.consent.http.resources.TosResource;
import org.broadinstitute.consent.http.resources.UserResource;
import org.broadinstitute.consent.http.resources.VersionResource;
import org.broadinstitute.consent.http.resources.VoteResource;
import org.broadinstitute.consent.http.service.AuditService;
import org.broadinstitute.consent.http.service.ConsentService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.DarCollectionService;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetAssociationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.InstitutionService;
import org.broadinstitute.consent.http.service.LibraryCardService;
import org.broadinstitute.consent.http.service.MatchService;
import org.broadinstitute.consent.http.service.MetricsService;
import org.broadinstitute.consent.http.service.NihService;
import org.broadinstitute.consent.http.service.PendingCaseService;
import org.broadinstitute.consent.http.service.ResearcherService;
import org.broadinstitute.consent.http.service.ReviewResultsService;
import org.broadinstitute.consent.http.service.SummaryService;
import org.broadinstitute.consent.http.service.SupportRequestService;
import org.broadinstitute.consent.http.service.TDRService;
import org.broadinstitute.consent.http.service.UseRestrictionValidator;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.VoteService;
import org.broadinstitute.consent.http.service.ontology.IndexOntologyService;
import org.broadinstitute.consent.http.service.ontology.IndexerService;
import org.broadinstitute.consent.http.service.ontology.IndexerServiceImpl;
import org.broadinstitute.consent.http.service.ontology.StoreOntologyService;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.dhatim.dropwizard.sentry.logging.SentryBootstrap;
import org.dhatim.dropwizard.sentry.logging.UncaughtExceptionHandlers;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Top-level entry point to the entire application.
 * <p>
 * See the Dropwizard docs here:
 * https://dropwizard.github.io
 */
public class ConsentApplication extends Application<ConsentConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger("ConsentApplication");

    public static final String GCS_CHECK = "google-cloud-storage";
    public static final String ES_CHECK = "elastic-search";
    public static final String ONTOLOGY_CHECK = "ontology";
    public static final String SAM_CHECK = "sam";
    public static final String SG_CHECK = "sendgrid";

    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting Consent Application");
        try {
            String dsn = System.getProperties().getProperty("sentry.dsn");
            if (StringUtils.isNotBlank(dsn)) {
                SentryBootstrap.Builder.withDsn(dsn).bootstrap();
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
        final HttpClientUtil clientUtil = new HttpClientUtil();
        final GCSStore googleStore = injector.getProvider(GCSStore.class).get();

        // Services
        final ConsentService consentService = injector.getProvider(ConsentService.class).get();
        final DarCollectionService darCollectionService= injector.getProvider(DarCollectionService.class).get();
        final DacService dacService = injector.getProvider(DacService.class).get();
        final DataAccessRequestService dataAccessRequestService = injector.getProvider(DataAccessRequestService.class).get();
        final DatasetAssociationService datasetAssociationService = injector.getProvider(DatasetAssociationService.class).get();
        final DatasetService datasetService = injector.getProvider(DatasetService.class).get();
        final ElectionService electionService = injector.getProvider(ElectionService.class).get();
        final EmailNotifierService emailNotifierService = injector.getProvider(EmailNotifierService.class).get();
        final GCSService gcsService = injector.getProvider(GCSService.class).get();
        final InstitutionService institutionService = injector.getProvider(InstitutionService.class).get();
        final MetricsService metricsService = injector.getProvider(MetricsService.class).get();
        final PendingCaseService pendingCaseService = injector.getProvider(PendingCaseService.class).get();
        final UserService userService = injector.getProvider(UserService.class).get();
        final VoteService voteService = injector.getProvider(VoteService.class).get();
        final AuditService auditService = injector.getProvider(AuditService.class).get();
        final SummaryService summaryService = injector.getProvider(SummaryService.class).get();
        final ReviewResultsService reviewResultsService = injector.getProvider(ReviewResultsService.class).get();
        final UseRestrictionValidator useRestrictionValidator = injector.getProvider(UseRestrictionValidator.class).get();
        final MatchService matchService = injector.getProvider(MatchService.class).get();
        final OAuthAuthenticator authenticator = injector.getProvider(OAuthAuthenticator.class).get();
        final LibraryCardService libraryCardService = injector.getProvider(LibraryCardService.class).get();
        final SamService samService = injector.getProvider(SamService.class).get();
        final SupportRequestService supportRequestService = injector.getProvider(SupportRequestService.class).get();
        final TDRService tdrService = injector.getProvider(TDRService.class).get();

        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        configureCors(env);

        // Health Checks
        env.healthChecks().register(GCS_CHECK, new GCSHealthCheck(gcsService));
        env.healthChecks().register(ES_CHECK, new ElasticSearchHealthCheck(config.getElasticSearchConfiguration()));
        env.healthChecks().register(ONTOLOGY_CHECK, new OntologyHealthCheck(clientUtil, config.getServicesConfiguration()));
        env.healthChecks().register(SAM_CHECK, new SamHealthCheck(clientUtil, config.getServicesConfiguration()));
        env.healthChecks().register(SG_CHECK, new SendGridHealthCheck(clientUtil, config.getMailConfiguration()));

        final StoreOntologyService storeOntologyService = new StoreOntologyService(
                googleStore,
                config.getStoreOntologyConfiguration().getBucketSubdirectory(),
                config.getStoreOntologyConfiguration().getConfigurationFileName());
        final ResearcherService researcherService = injector.getProvider(ResearcherService.class).get();
        final NihService nihService = injector.getProvider(NihService.class).get();


        final IndexOntologyService indexOntologyService = new IndexOntologyService(config.getElasticSearchConfiguration());
        final IndexerService indexerService = new IndexerServiceImpl(storeOntologyService, indexOntologyService);

        // Custom Error handling. Expand to include other codes when necessary
        final ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
        errorHandler.addErrorPage(404, "/error/404");
        env.getApplicationContext().setErrorHandler(errorHandler);
        env.jersey().register(ResponseServerFilter.class);
        env.jersey().register(ErrorResource.class);

        // Register standard application resources.
        env.jersey().register(new DataAccessRequestResourceVersion2(dataAccessRequestService, emailNotifierService, gcsService, userService, matchService));
        env.jersey().register(new DataAccessRequestResource(dataAccessRequestService, userService, consentService));
        env.jersey().register(new DatasetResource(consentService, datasetService, userService, dataAccessRequestService));
        env.jersey().register(new DatasetAssociationsResource(datasetAssociationService));
        env.jersey().register(new ConsentResource(auditService, userService, consentService, matchService, useRestrictionValidator));
        env.jersey().register(new ConsentAssociationResource(consentService, userService));
        env.jersey().register(new ConsentElectionResource(consentService, dacService, emailNotifierService, voteService, electionService));
        env.jersey().register(new ConsentVoteResource(emailNotifierService, electionService, voteService));
        env.jersey().register(new ConsentCasesResource(electionService, pendingCaseService, summaryService));
        env.jersey().register(new DacResource(dacService, userService, datasetService));
        env.jersey().register(new DACUserResource(userService));
        env.jersey().register(new DarCollectionResource(dataAccessRequestService, darCollectionService, userService));
        env.jersey().register(new DataRequestElectionResource(dataAccessRequestService, emailNotifierService, voteService, electionService));
        env.jersey().register(new DataRequestVoteResource(dataAccessRequestService, darCollectionService, datasetAssociationService, emailNotifierService, voteService, datasetService, electionService, userService));
        env.jersey().register(new DataRequestCasesResource(electionService, pendingCaseService, summaryService));
        env.jersey().register(new DataRequestReportsResource(dataAccessRequestService));
        env.jersey().register(new DataUseLetterResource(auditService, googleStore, userService, consentService));
        env.jersey().register(new ElectionResource(voteService, electionService));
        env.jersey().register(new ElectionReviewResource(dataAccessRequestService, consentService, electionService, reviewResultsService));
        env.jersey().register(new EmailNotifierResource(emailNotifierService));
        env.jersey().register(new IndexerResource(indexerService, googleStore));
        env.jersey().register(new InstitutionResource(userService, institutionService));
        env.jersey().register(new LibraryCardResource(userService, libraryCardService));
        env.jersey().register(new MatchResource(matchService));
        env.jersey().register(new MetricsResource(metricsService));
        env.jersey().register(new NihAccountResource(nihService, userService));
        env.jersey().register(new SamResource(samService, userService));
        env.jersey().register(new SwaggerResource(config.getGoogleAuthentication()));
        env.jersey().register(new StatusResource(env.healthChecks()));
        env.jersey().register(new UserResource(samService, userService, datasetService, supportRequestService));
        env.jersey().register(new TosResource(samService));
        env.jersey().register(injector.getInstance(VersionResource.class));
        env.jersey().register(new VoteResource(userService, voteService, electionService));
        env.jersey().register(new LivenessResource());
        env.jersey().register(new TDRResource(tdrService, datasetService));

        // Authentication filters
        final UserRoleDAO userRoleDAO = injector.getProvider(UserRoleDAO.class).get();
        AuthFilter defaultAuthFilter = new DefaultAuthFilter.Builder<AuthUser>()
                .setAuthenticator(new DefaultAuthenticator())
                .setRealm(" ")
                .buildAuthFilter();
        List<AuthFilter> filters = Lists.newArrayList(
                defaultAuthFilter,
                new BasicCustomAuthFilter(new BasicAuthenticator(config.getBasicAuthentication())),
                new OAuthCustomAuthFilter(authenticator, userRoleDAO));
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

    private void configureCors(Environment environment) {
        Dynamic filter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
        filter.setInitParameter("allowedOrigins", "*");
        filter.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD,PATCH");
        filter.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin,Authorization,Content-Disposition,Access-Control-Expose-Headers,Pragma,Cache-Control,Expires,X-App-ID");
        filter.setInitParameter("exposeHeaders", "Content-Type,Pragma,Cache-Control,Expires");
        filter.setInitParameter("allowCredentials", "true");
    }

    private void initializeLiquibase(ConsentConfiguration config) throws LiquibaseException, SQLException {
        Connection connection = DriverManager.getConnection(
                config.getDataSourceFactory().getUrl(),
                config.getDataSourceFactory().getUser(),
                config.getDataSourceFactory().getPassword()
        );
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        Liquibase liquibase = new Liquibase(liquibaseFile(), new ClassLoaderResourceAccessor(), database);
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
