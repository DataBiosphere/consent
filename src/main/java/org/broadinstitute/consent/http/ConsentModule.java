package org.broadinstitute.consent.http;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.lifecycle.Managed;
import jakarta.ws.rs.client.Client;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.broadinstitute.consent.http.authentication.AuthorizationHelper;
import org.broadinstitute.consent.http.authentication.DuosUserAuthenticator;
import org.broadinstitute.consent.http.authentication.OAuthAuthenticator;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.configurations.OidcConfiguration;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetAuthorizationReaderDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.DraftDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.OidcAuthorityDAO;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.service.AcknowledgementService;
import org.broadinstitute.consent.http.service.CounterService;
import org.broadinstitute.consent.http.service.DACAutomationRuleService;
import org.broadinstitute.consent.http.service.DaaService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.DarCollectionService;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetRegistrationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElasticSearchService;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.EmailService;
import org.broadinstitute.consent.http.service.FeatureFlagService;
import org.broadinstitute.consent.http.service.FileStorageObjectService;
import org.broadinstitute.consent.http.service.InstitutionService;
import org.broadinstitute.consent.http.service.LibraryCardService;
import org.broadinstitute.consent.http.service.MatchService;
import org.broadinstitute.consent.http.service.MetricsService;
import org.broadinstitute.consent.http.service.NihService;
import org.broadinstitute.consent.http.service.OidcService;
import org.broadinstitute.consent.http.service.OntologyService;
import org.broadinstitute.consent.http.service.SupportRequestService;
import org.broadinstitute.consent.http.service.UseRestrictionConverter;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.VoteService;
import org.broadinstitute.consent.http.service.dao.DaaServiceDAO;
import org.broadinstitute.consent.http.service.dao.DacServiceDAO;
import org.broadinstitute.consent.http.service.dao.DarCollectionServiceDAO;
import org.broadinstitute.consent.http.service.dao.DataAccessRequestServiceDAO;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.broadinstitute.consent.http.service.dao.DraftFileStorageServiceDAO;
import org.broadinstitute.consent.http.service.dao.DraftServiceDAO;
import org.broadinstitute.consent.http.service.dao.NihServiceDAO;
import org.broadinstitute.consent.http.service.dao.UserServiceDAO;
import org.broadinstitute.consent.http.service.dao.VoteServiceDAO;
import org.broadinstitute.consent.http.service.feature.InstitutionAndLibraryCardEnforcement;
import org.broadinstitute.consent.http.service.ontology.ElasticSearchSupport;
import org.broadinstitute.consent.http.service.ontology.OntologyIndexService;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.gson2.Gson2Config;
import org.jdbi.v3.gson2.Gson2Plugin;
import org.jdbi.v3.guava.GuavaPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

public class ConsentModule extends AbstractModule implements ConsentLogger {

  public static final String DB_ENV = "postgresql";
  private final ConsentConfiguration config;
  private final Environment environment;
  private final Client client;
  private final Jdbi jdbi;
  private final ElectionDAO electionDAO;
  private final VoteDAO voteDAO;
  private final StudyDAO studyDAO;
  private final DatasetDAO datasetDAO;
  private final DatasetAuthorizationReaderDAO datasetAuthorizationReaderDAO;
  private final DaaDAO daaDAO;
  private final UserDAO userDAO;
  private final UserRoleDAO userRoleDAO;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DarCollectionDAO darCollectionDAO;
  private final LibraryCardDAO libraryCardDAO;
  private final FileStorageObjectDAO fileStorageObjectDAO;
  private final DraftDAO draftDAO;
  private final ExecutorService executorService;

  // Lazily-memoized singletons. @Singleton on a @Provides method only covers resolution
  // through Guice; providers in this module also call each other directly, which bypasses
  // Guice scoping, so these fields guarantee a single instance on both paths.
  private OntologyService ontologyService;
  private DatasetRegistrationService datasetRegistrationService;
  private InstitutionAndLibraryCardEnforcement institutionAndLibraryCardEnforcement;
  private SamDAO samDAO;
  private EmailService emailService;
  private ElasticSearchService elasticSearchService;
  private DatasetServiceDAO datasetServiceDAO;
  private VoteService voteService;
  private DatasetService datasetService;
  private UserService userService;
  private InstitutionService institutionService;
  private LibraryCardService libraryCardService;
  private DacService dacService;
  private DaaService daaService;
  private CounterService counterService;
  private VoteServiceDAO voteServiceDAO;
  private DaaServiceDAO daaServiceDAO;
  private DacServiceDAO dacServiceDAO;
  private DarCollectionServiceDAO darCollectionServiceDAO;
  private DataAccessRequestServiceDAO dataAccessRequestServiceDAO;
  private DataAccessRequestService dataAccessRequestService;
  private UserServiceDAO userServiceDAO;
  private DACAutomationRuleService ruleService;
  private GCSService gcsService;
  private SendGridAPI sendGridAPI;
  private FreeMarkerTemplateHelper freeMarkerTemplateHelper;
  private DarCollectionService darCollectionService;
  private FileStorageObjectService fileStorageObjectService;
  private ElectionService electionService;
  private FeatureFlagService featureFlagService;
  private MatchService matchService;
  private MetricsService metricsService;
  private NihService nihService;
  private NihServiceDAO nihServiceDAO;
  private AcknowledgementService acknowledgementService;
  private DraftFileStorageServiceDAO draftFileStorageServiceDAO;
  private DraftServiceDAO draftServiceDAO;
  private HttpClientUtil httpClientUtil;
  private OntologyIndexService ontologyIndexService;
  private SamService samService;
  private OidcAuthorityDAO oidcAuthorityDAO;
  private OidcService oidcService;
  private SupportRequestService supportRequestService;

  ConsentModule(ConsentConfiguration consentConfiguration, Environment environment) {
    this.config = consentConfiguration;
    this.environment = environment;
    this.client =
        new JerseyClientBuilder(environment)
            .using(config.getJerseyClientConfiguration())
            .build(this.getClass().getName());

    this.jdbi = new JdbiFactory().build(environment, config.getDataSourceFactory(), DB_ENV);
    jdbi.installPlugin(new SqlObjectPlugin());
    jdbi.installPlugin(new Gson2Plugin());
    jdbi.installPlugin(new GuavaPlugin());
    jdbi.getConfig().get(Gson2Config.class).setGson(GsonUtil.buildGson());

    this.electionDAO = this.jdbi.onDemand(ElectionDAO.class);
    this.voteDAO = this.jdbi.onDemand(VoteDAO.class);
    this.studyDAO = this.jdbi.onDemand(StudyDAO.class);
    this.datasetAuthorizationReaderDAO = this.jdbi.onDemand(DatasetAuthorizationReaderDAO.class);
    this.datasetDAO = this.jdbi.onDemand(DatasetDAO.class);
    this.daaDAO = this.jdbi.onDemand(DaaDAO.class);
    this.userDAO = this.jdbi.onDemand(UserDAO.class);
    this.userRoleDAO = this.jdbi.onDemand(UserRoleDAO.class);
    this.dataAccessRequestDAO = this.jdbi.onDemand(DataAccessRequestDAO.class);
    this.darCollectionDAO = this.jdbi.onDemand(DarCollectionDAO.class);
    this.libraryCardDAO = this.jdbi.onDemand((LibraryCardDAO.class));
    this.fileStorageObjectDAO = this.jdbi.onDemand((FileStorageObjectDAO.class));
    this.draftDAO = this.jdbi.onDemand(DraftDAO.class);

    // All async work in this application is blocking I/O (Sam calls, Elasticsearch indexing,
    // batch DB writes), so a single shared virtual-thread executor replaces the per-class
    // fixed pools previously created by ThreadUtils.
    this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    environment
        .lifecycle()
        .manage(
            new Managed() {
              @Override
              public void stop() {
                executorService.shutdown();
                // logWarns: We don't need to send operational noise to Sentry
                try {
                  if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    logWarn(
                        "Shared executor service did not terminate within 30 seconds; "
                            + "forcing shutdown of remaining tasks");
                    executorService.shutdownNow();
                  }
                } catch (InterruptedException e) {
                  logWarn(
                      "Interrupted while awaiting shared executor service termination; "
                          + "forcing shutdown of remaining tasks",
                      e);
                  executorService.shutdownNow();
                  Thread.currentThread().interrupt();
                }
              }
            });
  }

  @Override
  protected void configure() {
    bind(Configuration.class).toInstance(config);
    bind(Environment.class).toInstance(environment);
  }

  @Provides
  ExecutorService providesExecutorService() {
    return executorService;
  }

  @Provides
  Client providesClient() {
    return client;
  }

  @Provides
  @Singleton
  synchronized HttpClientUtil providesHttpClientUtil() {
    if (httpClientUtil == null) {
      httpClientUtil = new HttpClientUtil(config.getServicesConfiguration());
    }
    return httpClientUtil;
  }

  @Provides
  Jdbi providesJdbi() {
    return jdbi;
  }

  @Provides
  ElasticSearchConfiguration providesElasticSearchConfiguration() {
    return config.getElasticSearchConfiguration();
  }

  @Provides
  MailConfiguration providesMailConfiguration() {
    return config.getMailConfiguration();
  }

  @Provides
  ServicesConfiguration providesServicesConfiguration() {
    return config.getServicesConfiguration();
  }

  @Provides
  HealthCheckRegistry providesHealthCheckRegistry() {
    return environment.healthChecks();
  }

  @Provides
  UseRestrictionConverter providesUseRestrictionConverter() {
    return new UseRestrictionConverter();
  }

  @Provides
  @Singleton
  synchronized OntologyService providesOntologyService() {
    if (ontologyService == null) {
      ontologyService =
          new OntologyService(
              providesJdbi(), providesOntologyIndexService(), providesExecutorService());
    }
    return ontologyService;
  }

  @Provides
  @Singleton
  synchronized OntologyIndexService providesOntologyIndexService() {
    if (ontologyIndexService == null) {
      ontologyIndexService =
          new OntologyIndexService(providesGCSService(), config.getCloudStoreConfiguration());
    }
    return ontologyIndexService;
  }

  @Provides
  AuthorizationHelper providesAuthorizationHelper() {
    return new AuthorizationHelper(providesSamService(), providesUserService());
  }

  @Provides
  OAuthAuthenticator providesOAuthAuthenticator() {
    return new OAuthAuthenticator(providesAuthorizationHelper());
  }

  @Provides
  DuosUserAuthenticator providesDuosUserOAuthAuthenticator() {
    return new DuosUserAuthenticator(providesAuthorizationHelper());
  }

  @Provides
  @Singleton
  synchronized DarCollectionService providesDarCollectionService() {
    if (darCollectionService == null) {
      darCollectionService =
          new DarCollectionService(
              providesJdbi(),
              providesDarCollectionServiceDAO(),
              providesEmailService(),
              providesRuleService());
    }
    return darCollectionService;
  }

  @Provides
  @Singleton
  synchronized FileStorageObjectService providesFileStorageObjectService() {
    if (fileStorageObjectService == null) {
      fileStorageObjectService =
          new FileStorageObjectService(
              providesJdbi(),
              providesGCSService(),
              providesDatasetService(),
              providesDacService(),
              providesDaaService(),
              providesDataAccessRequestService());
    }
    return fileStorageObjectService;
  }

  @Provides
  @Singleton
  synchronized GCSService providesGCSService() {
    if (gcsService == null) {
      gcsService = new GCSService(config.getCloudStoreConfiguration());
    }
    return gcsService;
  }

  @Provides
  @Singleton
  synchronized CounterService providesCounterService() {
    if (counterService == null) {
      counterService = new CounterService(providesJdbi());
    }
    return counterService;
  }

  @Provides
  @Singleton
  synchronized DACAutomationRuleService providesRuleService() {
    if (ruleService == null) {
      ruleService =
          new DACAutomationRuleService(
              providesJdbi(), providesVoteServiceDAO(), providesVoteService());
    }
    return ruleService;
  }

  @Provides
  @Singleton
  synchronized DataAccessRequestService providesDataAccessRequestService() {
    if (dataAccessRequestService == null) {
      dataAccessRequestService =
          new DataAccessRequestService(
              providesJdbi(),
              providesDataAccessRequestServiceDAO(),
              providesCounterService(),
              providesDacService(),
              providesUserService(),
              providesInstitutionService(),
              providesEmailService(),
              providesRuleService(),
              config);
    }
    return dataAccessRequestService;
  }

  @Provides
  @Singleton
  synchronized DatasetServiceDAO providesDatasetServiceDAO() {
    if (datasetServiceDAO == null) {
      datasetServiceDAO =
          new DatasetServiceDAO(
              providesJdbi(),
              providesDatasetDAO(),
              providesStudyDAO(),
              providesDatasetAuthorizationReaderDAO());
    }
    return datasetServiceDAO;
  }

  @Provides
  @Singleton
  synchronized DatasetService providesDatasetService() {
    if (datasetService == null) {
      datasetService =
          new DatasetService(
              providesJdbi(),
              providesDatasetServiceDAO(),
              providesElasticSearchService(),
              providesEmailService(),
              providesOntologyService());
    }
    return datasetService;
  }

  @Provides
  @Singleton
  synchronized ElectionService providesElectionService() {
    if (electionService == null) {
      electionService = new ElectionService(providesJdbi());
    }
    return electionService;
  }

  @Provides
  @Singleton
  synchronized FreeMarkerTemplateHelper providesFreeMarkerTemplateHelper() {
    if (freeMarkerTemplateHelper == null) {
      freeMarkerTemplateHelper = new FreeMarkerTemplateHelper(config.getFreeMarkerConfiguration());
    }
    return freeMarkerTemplateHelper;
  }

  @Provides
  @Singleton
  synchronized EmailService providesEmailService() {
    if (emailService == null) {
      emailService =
          new EmailService(
              providesJdbi(), providesSendGridAPI(), providesFreeMarkerTemplateHelper(), config);
    }
    return emailService;
  }

  @Provides
  @Singleton
  synchronized FeatureFlagService providesFeatureFlagService() {
    if (featureFlagService == null) {
      featureFlagService = new FeatureFlagService(providesJdbi());
    }
    return featureFlagService;
  }

  @Provides
  @Singleton
  synchronized SendGridAPI providesSendGridAPI() {
    if (sendGridAPI == null) {
      // SendGridAPI predates the Jdbi migration and owns only a single read; migrating it
      // requires its own constructor change and is deferred to a separate ticket.
      sendGridAPI = new SendGridAPI(config.getMailConfiguration(), providesUserDAO());
    }
    return sendGridAPI;
  }

  @Provides
  DataAccessRequestDAO providesDataAccessRequestDAO() {
    return dataAccessRequestDAO;
  }

  @Provides
  DarCollectionDAO providesDARCollectionDAO() {
    return darCollectionDAO;
  }

  @Provides
  @Singleton
  synchronized DarCollectionServiceDAO providesDarCollectionServiceDAO() {
    if (darCollectionServiceDAO == null) {
      darCollectionServiceDAO =
          new DarCollectionServiceDAO(
              providesJdbi(), providesDatasetDAO(), providesElectionDAO(), providesUserDAO());
    }
    return darCollectionServiceDAO;
  }

  @Provides
  @Singleton
  synchronized DataAccessRequestServiceDAO providesDataAccessRequestServiceDAO() {
    if (dataAccessRequestServiceDAO == null) {
      dataAccessRequestServiceDAO =
          new DataAccessRequestServiceDAO(
              providesJdbi(), providesDataAccessRequestDAO(), providesDARCollectionDAO());
    }
    return dataAccessRequestServiceDAO;
  }

  @Provides
  ElectionDAO providesElectionDAO() {
    return electionDAO;
  }

  @Provides
  VoteDAO providesVoteDAO() {
    return voteDAO;
  }

  @Provides
  StudyDAO providesStudyDAO() {
    return studyDAO;
  }

  @Provides
  @Singleton
  synchronized VoteServiceDAO providesVoteServiceDAO() {
    if (voteServiceDAO == null) {
      voteServiceDAO = new VoteServiceDAO(providesJdbi(), providesVoteDAO());
    }
    return voteServiceDAO;
  }

  @Provides
  @Singleton
  synchronized VoteService providesVoteService() {
    if (voteService == null) {
      voteService =
          new VoteService(
              providesJdbi(),
              providesVoteServiceDAO(),
              providesEmailService(),
              providesOntologyService());
    }
    return voteService;
  }

  @Provides
  DatasetAuthorizationReaderDAO providesDatasetAuthorizationReaderDAO() {
    return datasetAuthorizationReaderDAO;
  }

  @Provides
  DatasetDAO providesDatasetDAO() {
    return datasetDAO;
  }

  @Provides
  @Singleton
  synchronized DaaServiceDAO providesDaaServiceDAO() {
    if (daaServiceDAO == null) {
      daaServiceDAO =
          new DaaServiceDAO(providesJdbi(), providesDaaDAO(), providesFileStorageObjectDAO());
    }
    return daaServiceDAO;
  }

  @Provides
  @Singleton
  synchronized DacServiceDAO providesDacServiceDAO() {
    if (dacServiceDAO == null) {
      dacServiceDAO = new DacServiceDAO(providesJdbi());
    }
    return dacServiceDAO;
  }

  @Provides
  DaaDAO providesDaaDAO() {
    return daaDAO;
  }

  @Provides
  @Singleton
  synchronized DaaService providesDaaService() {
    if (daaService == null) {
      daaService =
          new DaaService(
              providesJdbi(),
              providesDaaServiceDAO(),
              providesGCSService(),
              providesEmailService(),
              providesUserService(),
              providesLibraryCardService());
    }
    return daaService;
  }

  @Provides
  @Singleton
  synchronized DacService providesDacService() {
    if (dacService == null) {
      dacService =
          new DacService(
              providesJdbi(),
              providesDacServiceDAO(),
              providesVoteService(),
              providesElasticSearchService(),
              providesDaaService());
    }
    return dacService;
  }

  @Provides
  @Singleton
  synchronized ElasticSearchService providesElasticSearchService() {
    if (elasticSearchService == null) {
      elasticSearchService =
          new ElasticSearchService(
              providesJdbi(),
              providesDatasetServiceDAO(),
              ElasticSearchSupport.createRestClient(config.getElasticSearchConfiguration()),
              config.getElasticSearchConfiguration(),
              providesOntologyService());
    }
    return elasticSearchService;
  }

  @Provides
  UserDAO providesUserDAO() {
    return userDAO;
  }

  @Provides
  UserRoleDAO providesUserRoleDAO() {
    return userRoleDAO;
  }

  @Provides
  @Singleton
  synchronized MatchService providesMatchService() {
    if (matchService == null) {
      matchService =
          new MatchService(
              providesJdbi(), providesUseRestrictionConverter(), providesOntologyService());
    }
    return matchService;
  }

  @Provides
  @Singleton
  synchronized MetricsService providesMetricsService() {
    if (metricsService == null) {
      metricsService = new MetricsService(providesJdbi());
    }
    return metricsService;
  }

  @Provides
  FileStorageObjectDAO providesFileStorageObjectDAO() {
    return fileStorageObjectDAO;
  }

  @Provides
  LibraryCardDAO providesLibraryCardDAO() {
    return libraryCardDAO;
  }

  @Provides
  @Singleton
  synchronized InstitutionService providesInstitutionService() {
    if (institutionService == null) {
      institutionService =
          new InstitutionService(providesJdbi(), providesInstitutionAndLibraryCardEnforcement());
    }
    return institutionService;
  }

  @Provides
  @Singleton
  synchronized LibraryCardService providesLibraryCardService() {
    if (libraryCardService == null) {
      libraryCardService =
          new LibraryCardService(
              providesJdbi(), providesInstitutionService(), providesEmailService());
    }
    return libraryCardService;
  }

  @Provides
  @Singleton
  synchronized AcknowledgementService providesAcknowledgementService() {
    if (acknowledgementService == null) {
      acknowledgementService = new AcknowledgementService(providesJdbi(), providesEmailService());
    }
    return acknowledgementService;
  }

  @Provides
  @Singleton
  synchronized DatasetRegistrationService providesDatasetRegistrationService() {
    if (datasetRegistrationService == null) {
      datasetRegistrationService =
          new DatasetRegistrationService(
              providesJdbi(),
              providesDatasetServiceDAO(),
              providesGCSService(),
              providesElasticSearchService(),
              providesEmailService(),
              providesExecutorService());
    }
    return datasetRegistrationService;
  }

  @Provides
  @Singleton
  synchronized UserServiceDAO providesUserServiceDAO() {
    if (userServiceDAO == null) {
      userServiceDAO = new UserServiceDAO(providesJdbi());
    }
    return userServiceDAO;
  }

  @Provides
  @Singleton
  synchronized UserService providesUserService() {
    if (userService == null) {
      userService =
          new UserService(
              providesJdbi(),
              providesUserServiceDAO(),
              providesInstitutionService(),
              providesInstitutionAndLibraryCardEnforcement());
    }
    return userService;
  }

  @Provides
  @Singleton
  synchronized InstitutionAndLibraryCardEnforcement providesInstitutionAndLibraryCardEnforcement() {
    if (institutionAndLibraryCardEnforcement == null) {
      institutionAndLibraryCardEnforcement =
          new InstitutionAndLibraryCardEnforcement(
              providesJdbi(), providesUserServiceDAO(), providesExecutorService());
    }
    return institutionAndLibraryCardEnforcement;
  }

  @Provides
  @Singleton
  synchronized NihService providesNihService() {
    if (nihService == null) {
      nihService =
          new NihService(
              providesJdbi(),
              providesNIHServiceDAO(),
              providesHttpClientUtil(),
              config.getServicesConfiguration());
    }
    return nihService;
  }

  @Provides
  @Singleton
  synchronized NihServiceDAO providesNIHServiceDAO() {
    if (nihServiceDAO == null) {
      nihServiceDAO = new NihServiceDAO(providesJdbi());
    }
    return nihServiceDAO;
  }

  @Provides
  @Singleton
  synchronized SamService providesSamService() {
    if (samService == null) {
      samService = new SamService(providesSamDAO());
    }
    return samService;
  }

  @Provides
  @Singleton
  synchronized SamDAO providesSamDAO() {
    if (samDAO == null) {
      samDAO =
          new SamDAO(
              providesHttpClientUtil(),
              config.getServicesConfiguration(),
              providesExecutorService());
    }
    return samDAO;
  }

  @Provides
  OidcConfiguration providesOidcConfiguration() {
    return config.getOidcConfiguration();
  }

  @Provides
  @Singleton
  synchronized OidcAuthorityDAO providesOidcAuthorityDAO() {
    if (oidcAuthorityDAO == null) {
      oidcAuthorityDAO =
          new OidcAuthorityDAO(providesHttpClientUtil(), providesOidcConfiguration());
    }
    return oidcAuthorityDAO;
  }

  @Provides
  @Singleton
  synchronized OidcService providesOidcService() {
    if (oidcService == null) {
      oidcService = new OidcService(providesOidcAuthorityDAO(), providesOidcConfiguration());
    }
    return oidcService;
  }

  @Provides
  @Singleton
  synchronized SupportRequestService providesSupportRequestService() {
    if (supportRequestService == null) {
      supportRequestService = new SupportRequestService(config.getServicesConfiguration());
    }
    return supportRequestService;
  }

  @Provides
  DraftDAO providesDraftDAO() {
    return draftDAO;
  }

  @Provides
  @Singleton
  synchronized DraftFileStorageServiceDAO providesDraftFileStorageService() {
    if (draftFileStorageServiceDAO == null) {
      draftFileStorageServiceDAO =
          new DraftFileStorageServiceDAO(
              providesJdbi(), providesGCSService(), providesFileStorageObjectDAO());
    }
    return draftFileStorageServiceDAO;
  }

  @Provides
  @Singleton
  synchronized DraftServiceDAO providesDraftServiceDAO() {
    if (draftServiceDAO == null) {
      draftServiceDAO =
          new DraftServiceDAO(
              providesJdbi(), providesDraftDAO(), providesDraftFileStorageService());
    }
    return draftServiceDAO;
  }
}
