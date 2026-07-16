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
import org.broadinstitute.consent.http.filters.ClaimsCache;
import org.broadinstitute.consent.http.filters.RateLimitFilter;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.matching.DataUseMatcherV4;
import org.broadinstitute.consent.http.matching.DataUseUtil;
import org.broadinstitute.consent.http.matching.TranslationUtil;
import org.broadinstitute.consent.http.models.dto.registration.RegistrationRequestMapper;
import org.broadinstitute.consent.http.models.support.TicketFactory;
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
import org.broadinstitute.consent.http.util.CountryValidator;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.InstitutionUtil;
import org.broadinstitute.consent.http.util.JsonSchemaUtil;
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
    this.libraryCardDAO = this.jdbi.onDemand(LibraryCardDAO.class);
    this.fileStorageObjectDAO = this.jdbi.onDemand(FileStorageObjectDAO.class);
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
  @Singleton
  private ExecutorService providesExecutorService() {
    return executorService;
  }

  @Provides
  @Singleton
  private Client providesClient() {
    return client;
  }

  @Provides
  @Singleton
  private HttpClientUtil providesHttpClientUtil() {
    return new HttpClientUtil(config.getServicesConfiguration());
  }

  @Provides
  @Singleton
  private Jdbi providesJdbi() {
    return jdbi;
  }

  @Provides
  @Singleton
  private ElasticSearchConfiguration providesElasticSearchConfiguration() {
    return config.getElasticSearchConfiguration();
  }

  @Provides
  @Singleton
  private MailConfiguration providesMailConfiguration() {
    return config.getMailConfiguration();
  }

  @Provides
  @Singleton
  private ServicesConfiguration providesServicesConfiguration() {
    return config.getServicesConfiguration();
  }

  @Provides
  @Singleton
  private HealthCheckRegistry providesHealthCheckRegistry() {
    return environment.healthChecks();
  }

  @Provides
  @Singleton
  private OidcConfiguration providesOidcConfiguration() {
    return config.getOidcConfiguration();
  }

  @Provides
  @Singleton
  private UseRestrictionConverter providesUseRestrictionConverter() {
    return new UseRestrictionConverter();
  }

  @Provides
  @Singleton
  private ElectionDAO providesElectionDAO() {
    return electionDAO;
  }

  @Provides
  @Singleton
  private VoteDAO providesVoteDAO() {
    return voteDAO;
  }

  @Provides
  @Singleton
  private StudyDAO providesStudyDAO() {
    return studyDAO;
  }

  @Provides
  @Singleton
  private DatasetDAO providesDatasetDAO() {
    return datasetDAO;
  }

  @Provides
  @Singleton
  private DatasetAuthorizationReaderDAO providesDatasetAuthorizationReaderDAO() {
    return datasetAuthorizationReaderDAO;
  }

  @Provides
  @Singleton
  private DaaDAO providesDaaDAO() {
    return daaDAO;
  }

  @Provides
  @Singleton
  private UserDAO providesUserDAO() {
    return userDAO;
  }

  @Provides
  @Singleton
  private UserRoleDAO providesUserRoleDAO() {
    return userRoleDAO;
  }

  @Provides
  @Singleton
  private DataAccessRequestDAO providesDataAccessRequestDAO() {
    return dataAccessRequestDAO;
  }

  @Provides
  @Singleton
  private DarCollectionDAO providesDARCollectionDAO() {
    return darCollectionDAO;
  }

  @Provides
  @Singleton
  private LibraryCardDAO providesLibraryCardDAO() {
    return libraryCardDAO;
  }

  @Provides
  @Singleton
  private FileStorageObjectDAO providesFileStorageObjectDAO() {
    return fileStorageObjectDAO;
  }

  @Provides
  @Singleton
  private DraftDAO providesDraftDAO() {
    return draftDAO;
  }

  @Provides
  @Singleton
  private TranslationUtil providesTranslationUtil(Jdbi jdbi) {
    return new TranslationUtil(jdbi);
  }

  @Provides
  @Singleton
  private GCSService providesGCSService() {
    return new GCSService(config.getCloudStoreConfiguration());
  }

  @Provides
  @Singleton
  private OntologyIndexService providesOntologyIndexService(GCSService gcsService) {
    return new OntologyIndexService(gcsService, config.getCloudStoreConfiguration());
  }

  @Provides
  @Singleton
  private OntologyService providesOntologyService(
      Jdbi jdbi,
      OntologyIndexService indexService,
      ExecutorService executorService,
      TranslationUtil translationUtil) {
    return new OntologyService(jdbi, indexService, executorService, translationUtil);
  }

  @Provides
  @Singleton
  private DataUseUtil providesDataUseUtil(OntologyService ontologyService) {
    return new DataUseUtil(ontologyService);
  }

  @Provides
  @Singleton
  private DataUseMatcherV4 providesDataUseMatcherV4(DataUseUtil dataUseUtil) {
    return new DataUseMatcherV4(dataUseUtil);
  }

  @Provides
  @Singleton
  private ElasticSearchService providesElasticSearchService(
      Jdbi jdbi,
      DatasetServiceDAO datasetServiceDAO,
      ElasticSearchConfiguration elasticSearchConfiguration,
      OntologyService ontologyService) {
    return new ElasticSearchService(
        jdbi,
        datasetServiceDAO,
        ElasticSearchSupport.createRestClient(elasticSearchConfiguration),
        elasticSearchConfiguration,
        ontologyService);
  }

  @Provides
  @Singleton
  private FreeMarkerTemplateHelper providesFreeMarkerTemplateHelper() {
    return new FreeMarkerTemplateHelper(config.getFreeMarkerConfiguration());
  }

  @Provides
  @Singleton
  private SendGridAPI providesSendGridAPI(Jdbi jdbi, MailConfiguration mailConfiguration) {
    return new SendGridAPI(mailConfiguration, jdbi);
  }

  @Provides
  @Singleton
  private EmailService providesEmailService(
      Jdbi jdbi, SendGridAPI sendGridAPI, FreeMarkerTemplateHelper freeMarkerTemplateHelper) {
    return new EmailService(jdbi, sendGridAPI, freeMarkerTemplateHelper, config);
  }

  @Provides
  @Singleton
  private SamDAO providesSamDAO(
      HttpClientUtil httpClientUtil,
      ServicesConfiguration servicesConfiguration,
      ExecutorService executorService) {
    return new SamDAO(httpClientUtil, servicesConfiguration, executorService);
  }

  @Provides
  @Singleton
  private SamService providesSamService(SamDAO samDAO) {
    return new SamService(samDAO);
  }

  @Provides
  @Singleton
  private OidcAuthorityDAO providesOidcAuthorityDAO(
      HttpClientUtil httpClientUtil, OidcConfiguration oidcConfiguration) {
    return new OidcAuthorityDAO(httpClientUtil, oidcConfiguration);
  }

  @Provides
  @Singleton
  private OidcService providesOidcService(
      OidcAuthorityDAO oidcAuthorityDAO, OidcConfiguration oidcConfiguration) {
    return new OidcService(oidcAuthorityDAO, oidcConfiguration);
  }

  @Provides
  @Singleton
  private ClaimsCache providesClaimsCache() {
    return new ClaimsCache();
  }

  @Provides
  @Singleton
  private RateLimitFilter providesRateLimitFilter() {
    return new RateLimitFilter(config.getRateLimitConfiguration());
  }

  @Provides
  @Singleton
  private AuthorizationHelper providesAuthorizationHelper(
      SamService samService, UserService userService, ClaimsCache claimsCache) {
    return new AuthorizationHelper(samService, userService, claimsCache);
  }

  @Provides
  @Singleton
  private OAuthAuthenticator providesOAuthAuthenticator(AuthorizationHelper authorizationHelper) {
    return new OAuthAuthenticator(authorizationHelper);
  }

  @Provides
  @Singleton
  private DuosUserAuthenticator providesDuosUserOAuthAuthenticator(
      AuthorizationHelper authorizationHelper) {
    return new DuosUserAuthenticator(authorizationHelper);
  }

  @Provides
  @Singleton
  private DatasetServiceDAO providesDatasetServiceDAO(Jdbi jdbi) {
    return new DatasetServiceDAO(jdbi);
  }

  @Provides
  @Singleton
  private VoteServiceDAO providesVoteServiceDAO(Jdbi jdbi) {
    return new VoteServiceDAO(jdbi);
  }

  @Provides
  @Singleton
  private DarCollectionServiceDAO providesDarCollectionServiceDAO(Jdbi jdbi) {
    return new DarCollectionServiceDAO(jdbi);
  }

  @Provides
  @Singleton
  private DataAccessRequestServiceDAO providesDataAccessRequestServiceDAO(Jdbi jdbi) {
    return new DataAccessRequestServiceDAO(jdbi);
  }

  @Provides
  @Singleton
  private DaaServiceDAO providesDaaServiceDAO(Jdbi jdbi) {
    return new DaaServiceDAO(jdbi);
  }

  @Provides
  @Singleton
  private DacServiceDAO providesDacServiceDAO(Jdbi jdbi) {
    return new DacServiceDAO(jdbi);
  }

  @Provides
  @Singleton
  private NihServiceDAO providesNIHServiceDAO(Jdbi jdbi) {
    return new NihServiceDAO(jdbi);
  }

  @Provides
  @Singleton
  private UserServiceDAO providesUserServiceDAO(Jdbi jdbi) {
    return new UserServiceDAO(jdbi);
  }

  @Provides
  @Singleton
  private DraftFileStorageServiceDAO providesDraftFileStorageService(
      Jdbi jdbi, GCSService gcsService) {
    return new DraftFileStorageServiceDAO(jdbi, gcsService);
  }

  @Provides
  @Singleton
  private DraftServiceDAO providesDraftServiceDAO(
      Jdbi jdbi, DraftFileStorageServiceDAO draftFileStorageServiceDAO) {
    return new DraftServiceDAO(jdbi, draftFileStorageServiceDAO);
  }

  @Provides
  @Singleton
  private ElectionService providesElectionService(Jdbi jdbi) {
    return new ElectionService(jdbi);
  }

  @Provides
  @Singleton
  private FeatureFlagService providesFeatureFlagService(Jdbi jdbi) {
    return new FeatureFlagService(jdbi);
  }

  @Provides
  @Singleton
  private CounterService providesCounterService(Jdbi jdbi) {
    return new CounterService(jdbi);
  }

  @Provides
  @Singleton
  private MetricsService providesMetricsService(Jdbi jdbi) {
    return new MetricsService(jdbi);
  }

  @Provides
  @Singleton
  private InstitutionAndLibraryCardEnforcement providesInstitutionAndLibraryCardEnforcement(
      Jdbi jdbi, UserServiceDAO userServiceDAO, ExecutorService executorService) {
    return new InstitutionAndLibraryCardEnforcement(jdbi, userServiceDAO, executorService);
  }

  @Provides
  @Singleton
  private InstitutionService providesInstitutionService(
      Jdbi jdbi, InstitutionAndLibraryCardEnforcement enforcement) {
    return new InstitutionService(jdbi, enforcement);
  }

  @Provides
  @Singleton
  private LibraryCardService providesLibraryCardService(
      Jdbi jdbi, InstitutionService institutionService, EmailService emailService) {
    return new LibraryCardService(jdbi, institutionService, emailService);
  }

  @Provides
  @Singleton
  private UserService providesUserService(
      Jdbi jdbi,
      UserServiceDAO userServiceDAO,
      InstitutionService institutionService,
      InstitutionAndLibraryCardEnforcement enforcement) {
    return new UserService(jdbi, userServiceDAO, institutionService, enforcement);
  }

  @Provides
  @Singleton
  private AcknowledgementService providesAcknowledgementService(
      Jdbi jdbi, EmailService emailService) {
    return new AcknowledgementService(jdbi, emailService);
  }

  @Provides
  @Singleton
  private VoteService providesVoteService(
      Jdbi jdbi,
      VoteServiceDAO voteServiceDAO,
      EmailService emailService,
      OntologyService ontologyService) {
    return new VoteService(jdbi, voteServiceDAO, emailService, ontologyService);
  }

  @Provides
  @Singleton
  private DACAutomationRuleService providesRuleService(
      Jdbi jdbi, VoteServiceDAO voteServiceDAO, VoteService voteService) {
    return new DACAutomationRuleService(jdbi, voteServiceDAO, voteService);
  }

  @Provides
  @Singleton
  private DaaService providesDaaService(
      Jdbi jdbi,
      DaaServiceDAO daaServiceDAO,
      GCSService gcsService,
      EmailService emailService,
      UserService userService,
      LibraryCardService libraryCardService) {
    return new DaaService(
        jdbi, daaServiceDAO, gcsService, emailService, userService, libraryCardService);
  }

  @Provides
  @Singleton
  private DacService providesDacService(
      Jdbi jdbi,
      DacServiceDAO dacServiceDAO,
      VoteService voteService,
      ElasticSearchService elasticSearchService,
      DaaService daaService) {
    return new DacService(jdbi, dacServiceDAO, voteService, elasticSearchService, daaService);
  }

  @Provides
  @Singleton
  private DatasetService providesDatasetService(
      Jdbi jdbi,
      DatasetServiceDAO datasetServiceDAO,
      ElasticSearchService elasticSearchService,
      EmailService emailService,
      OntologyService ontologyService) {
    return new DatasetService(
        jdbi, datasetServiceDAO, elasticSearchService, emailService, ontologyService);
  }

  @Provides
  @Singleton
  private DataAccessRequestService providesDataAccessRequestService(
      Jdbi jdbi,
      DataAccessRequestServiceDAO dataAccessRequestServiceDAO,
      CounterService counterService,
      DacService dacService,
      UserService userService,
      InstitutionService institutionService,
      EmailService emailService,
      DACAutomationRuleService ruleService,
      CountryValidator countryValidator) {
    return new DataAccessRequestService(
        jdbi,
        dataAccessRequestServiceDAO,
        counterService,
        dacService,
        userService,
        institutionService,
        emailService,
        ruleService,
        countryValidator,
        config);
  }

  @Provides
  @Singleton
  private DarCollectionService providesDarCollectionService(
      Jdbi jdbi,
      DarCollectionServiceDAO darCollectionServiceDAO,
      EmailService emailService,
      DACAutomationRuleService ruleService) {
    return new DarCollectionService(jdbi, darCollectionServiceDAO, emailService, ruleService);
  }

  @Provides
  @Singleton
  private FileStorageObjectService providesFileStorageObjectService(
      Jdbi jdbi,
      GCSService gcsService,
      DatasetService datasetService,
      DacService dacService,
      DaaService daaService,
      DataAccessRequestService dataAccessRequestService) {
    return new FileStorageObjectService(
        jdbi, gcsService, datasetService, dacService, daaService, dataAccessRequestService);
  }

  @Provides
  @Singleton
  private DatasetRegistrationService providesDatasetRegistrationService(
      Jdbi jdbi,
      DatasetServiceDAO datasetServiceDAO,
      GCSService gcsService,
      ElasticSearchService elasticSearchService,
      EmailService emailService,
      ExecutorService executorService,
      RegistrationRequestMapper registrationRequestMapper) {
    return new DatasetRegistrationService(
        jdbi,
        datasetServiceDAO,
        gcsService,
        elasticSearchService,
        emailService,
        executorService,
        registrationRequestMapper);
  }

  @Provides
  @Singleton
  private MatchService providesMatchService(
      Jdbi jdbi,
      UseRestrictionConverter useRestrictionConverter,
      DataUseMatcherV4 dataUseMatcherV4) {
    return new MatchService(jdbi, useRestrictionConverter, dataUseMatcherV4);
  }

  @Provides
  @Singleton
  private NihService providesNihService(
      Jdbi jdbi,
      NihServiceDAO nihServiceDAO,
      HttpClientUtil httpClientUtil,
      ServicesConfiguration servicesConfiguration) {
    return new NihService(jdbi, nihServiceDAO, httpClientUtil, servicesConfiguration);
  }

  @Provides
  @Singleton
  private SupportRequestService providesSupportRequestService(
      HttpClientUtil httpClientUtil,
      TicketFactory ticketFactory,
      ServicesConfiguration servicesConfiguration) {
    return new SupportRequestService(httpClientUtil, ticketFactory, servicesConfiguration);
  }

  @Provides
  @Singleton
  private CountryValidator providesCountryValidator() {
    return new CountryValidator();
  }

  @Provides
  @Singleton
  private InstitutionUtil providesInstitutionUtil() {
    return new InstitutionUtil();
  }

  @Provides
  @Singleton
  private JsonSchemaUtil providesJsonSchemaUtil() {
    return new JsonSchemaUtil();
  }

  @Provides
  @Singleton
  private TicketFactory providesTicketFactory() {
    return new TicketFactory();
  }
}
