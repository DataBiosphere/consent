package org.broadinstitute.consent.http;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jdbi3.JdbiFactory;
import jakarta.ws.rs.client.Client;
import org.broadinstitute.consent.http.authentication.OAuthAuthenticator;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.db.AcknowledgementDAO;
import org.broadinstitute.consent.http.db.CounterDAO;
import org.broadinstitute.consent.http.db.DAOContainer;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DarCollectionSummaryDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.DraftSubmissionDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.OidcAuthorityDAO;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.service.AcknowledgementService;
import org.broadinstitute.consent.http.service.CounterService;
import org.broadinstitute.consent.http.service.DaaService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.DarCollectionService;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetRegistrationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.DraftFileStorageService;
import org.broadinstitute.consent.http.service.DraftSubmissionService;
import org.broadinstitute.consent.http.service.ElasticSearchService;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.EmailService;
import org.broadinstitute.consent.http.service.FileStorageObjectService;
import org.broadinstitute.consent.http.service.InstitutionService;
import org.broadinstitute.consent.http.service.LibraryCardService;
import org.broadinstitute.consent.http.service.MatchService;
import org.broadinstitute.consent.http.service.MetricsService;
import org.broadinstitute.consent.http.service.NihService;
import org.broadinstitute.consent.http.service.OidcService;
import org.broadinstitute.consent.http.service.OntologyService;
import org.broadinstitute.consent.http.service.ResearcherService;
import org.broadinstitute.consent.http.service.SupportRequestService;
import org.broadinstitute.consent.http.service.UseRestrictionConverter;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.VoteService;
import org.broadinstitute.consent.http.service.dao.DaaServiceDAO;
import org.broadinstitute.consent.http.service.dao.DacServiceDAO;
import org.broadinstitute.consent.http.service.dao.DarCollectionServiceDAO;
import org.broadinstitute.consent.http.service.dao.DataAccessRequestServiceDAO;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.broadinstitute.consent.http.service.dao.NihServiceDAO;
import org.broadinstitute.consent.http.service.dao.UserServiceDAO;
import org.broadinstitute.consent.http.service.dao.VoteServiceDAO;
import org.broadinstitute.consent.http.service.ontology.ElasticSearchSupport;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.gson2.Gson2Config;
import org.jdbi.v3.gson2.Gson2Plugin;
import org.jdbi.v3.guava.GuavaPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

public class ConsentModule extends AbstractModule {

  public static final String DB_ENV = "postgresql";
  @Inject
  private final ConsentConfiguration config;
  @Inject
  private final Environment environment;
  private final Client client;
  private final Jdbi jdbi;
  private final CounterDAO counterDAO;
  private final ElectionDAO electionDAO;
  private final VoteDAO voteDAO;
  private final StudyDAO studyDAO;
  private final DatasetDAO datasetDAO;
  private final DaaDAO daaDAO;
  private final DacDAO dacDAO;
  private final UserDAO userDAO;
  private final UserRoleDAO userRoleDAO;
  private final MatchDAO matchDAO;
  private final MailMessageDAO mailMessageDAO;
  private final UserPropertyDAO userPropertyDAO;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DarCollectionDAO darCollectionDAO;
  private final DarCollectionSummaryDAO darCollectionSummaryDAO;
  private final InstitutionDAO institutionDAO;
  private final LibraryCardDAO libraryCardDAO;
  private final FileStorageObjectDAO fileStorageObjectDAO;
  private final AcknowledgementDAO acknowledgementDAO;
  private final DraftSubmissionDAO draftSubmissionDAO;

  ConsentModule(ConsentConfiguration consentConfiguration, Environment environment) {
    this.config = consentConfiguration;
    this.environment = environment;
    this.client = new JerseyClientBuilder(environment)
        .using(config.getJerseyClientConfiguration())
        .build(this.getClass().getName());

    this.jdbi = new JdbiFactory().build(environment, config.getDataSourceFactory(), DB_ENV);
    jdbi.installPlugin(new SqlObjectPlugin());
    jdbi.installPlugin(new Gson2Plugin());
    jdbi.installPlugin(new GuavaPlugin());
    jdbi.getConfig().get(Gson2Config.class).setGson(GsonUtil.buildGson());

    this.counterDAO = this.jdbi.onDemand(CounterDAO.class);
    this.electionDAO = this.jdbi.onDemand(ElectionDAO.class);
    this.voteDAO = this.jdbi.onDemand(VoteDAO.class);
    this.studyDAO = this.jdbi.onDemand(StudyDAO.class);
    this.datasetDAO = this.jdbi.onDemand(DatasetDAO.class);
    this.daaDAO = this.jdbi.onDemand(DaaDAO.class);
    this.dacDAO = this.jdbi.onDemand(DacDAO.class);
    this.userDAO = this.jdbi.onDemand(UserDAO.class);
    this.userRoleDAO = this.jdbi.onDemand(UserRoleDAO.class);
    this.matchDAO = this.jdbi.onDemand(MatchDAO.class);
    this.mailMessageDAO = this.jdbi.onDemand(MailMessageDAO.class);
    this.userPropertyDAO = this.jdbi.onDemand(UserPropertyDAO.class);
    this.dataAccessRequestDAO = this.jdbi.onDemand(DataAccessRequestDAO.class);
    this.darCollectionDAO = this.jdbi.onDemand(DarCollectionDAO.class);
    this.darCollectionSummaryDAO = this.jdbi.onDemand(DarCollectionSummaryDAO.class);
    this.institutionDAO = this.jdbi.onDemand((InstitutionDAO.class));
    this.libraryCardDAO = this.jdbi.onDemand((LibraryCardDAO.class));
    this.fileStorageObjectDAO = this.jdbi.onDemand((FileStorageObjectDAO.class));
    this.acknowledgementDAO = this.jdbi.onDemand((AcknowledgementDAO.class));
    this.draftSubmissionDAO = this.jdbi.onDemand(DraftSubmissionDAO.class);
  }

  @Override
  protected void configure() {
    bind(Configuration.class).toInstance(config);
    bind(Environment.class).toInstance(environment);
  }

  @Provides
  public DAOContainer providesDAOContainer() {
    DAOContainer container = new DAOContainer();
    container.setCounterDAO(providesCounterDAO());
    container.setDacDAO(providesDacDAO());
    container.setDataAccessRequestDAO(providesDataAccessRequestDAO());
    container.setDarCollectionDAO(providesDARCollectionDAO());
    container.setDarCollectionSummaryDAO(providesDarCollectionSummaryDAO());
    container.setDatasetDAO(providesDatasetDAO());
    container.setElectionDAO(providesElectionDAO());
    container.setMailMessageDAO(providesMailMessageDAO());
    container.setMatchDAO(providesMatchDAO());
    container.setUserPropertyDAO(providesUserPropertyDAO());
    container.setUserDAO(providesUserDAO());
    container.setUserRoleDAO(providesUserRoleDAO());
    container.setVoteDAO(providesVoteDAO());
    container.setStudyDAO(providesStudyDAO());
    container.setInstitutionDAO(providesInstitutionDAO());
    container.setFileStorageObjectDAO(providesFileStorageObjectDAO());
    container.setAcknowledgementDAO(providesAcknowledgementDAO());
    container.setDraftSubmissionDAO(providesDraftSubmissionDAO());
    return container;
  }

  @Provides
  Client providesClient() {
    return client;
  }

  @Provides
  HttpClientUtil providesHttpClientUtil() {
    return new HttpClientUtil(config.getServicesConfiguration());
  }

  @Provides
  Jdbi providesJdbi() {
    return jdbi;
  }

  @Provides
  UseRestrictionConverter providesUseRestrictionConverter() {
    return new UseRestrictionConverter(providesClient(), config.getServicesConfiguration());
  }

  @Provides
  OntologyService providesOntologyService() {
    return new OntologyService(providesClient(), config.getServicesConfiguration());
  }

  @Provides
  OAuthAuthenticator providesOAuthAuthenticator() {
    return new OAuthAuthenticator(providesSamService());
  }

  @Provides
  DarCollectionService providesDarCollectionService() {
    return new DarCollectionService(
        providesDARCollectionDAO(),
        providesDarCollectionServiceDAO(),
        providesDatasetDAO(),
        providesElectionDAO(),
        providesDataAccessRequestDAO(),
        providesEmailService(),
        providesVoteDAO(),
        providesMatchDAO(),
        providesDarCollectionSummaryDAO()
    );
  }

  @Provides
  FileStorageObjectService providesFileStorageObjectService() {
    return new FileStorageObjectService(
        providesFileStorageObjectDAO(),
        providesGCSService()
    );
  }

  @Provides
  GCSService providesGCSService() {
    return new GCSService(config.getCloudStoreConfiguration());
  }

  @Provides
  CounterDAO providesCounterDAO() {
    return counterDAO;
  }

  @Provides
  CounterService providesCounterService() {
    return new CounterService(providesCounterDAO());
  }

  @Provides
  DataAccessRequestService providesDataAccessRequestService() {
    return new DataAccessRequestService(
        providesCounterService(),
        providesDAOContainer(),
        providesDacService(),
        providesDataAccessRequestServiceDAO(),
        providesUseRestrictionConverter()
    );
  }

  @Provides
  DatasetServiceDAO providesDatasetServiceDAO() {
    return new DatasetServiceDAO(
        jdbi,
        providesDatasetDAO(),
        providesStudyDAO());
  }

  @Provides
  DatasetService providesDatasetService() {
    return new DatasetService(
        providesDatasetDAO(),
        providesDaaDAO(),
        providesDacDAO(),
        providesEmailService(),
        providesOntologyService(),
        providesStudyDAO(),
        providesDatasetServiceDAO(),
        providesUserDAO());
  }

  @Provides
  ElectionService providesElectionService() {
    return new ElectionService(
        providesElectionDAO()
    );
  }

  @Provides
  FreeMarkerTemplateHelper providesFreeMarkerTemplateHelper() {
    return new FreeMarkerTemplateHelper(config.getFreeMarkerConfiguration());
  }

  @Provides
  EmailService providesEmailService() {
    return new EmailService(
        providesDARCollectionDAO(),
        providesVoteDAO(),
        providesElectionDAO(),
        providesUserDAO(),
        providesMailMessageDAO(),
        providesDatasetDAO(),
        providesDacDAO(),
        providesSendGridAPI(),
        providesFreeMarkerTemplateHelper(),
        config.getServicesConfiguration().getLocalURL()
    );
  }

  @Provides
  SendGridAPI providesSendGridAPI() {
    return new SendGridAPI(
        config.getMailConfiguration(),
        providesUserDAO());
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
  DarCollectionSummaryDAO providesDarCollectionSummaryDAO() {
    return darCollectionSummaryDAO;
  }

  @Provides
  DarCollectionServiceDAO providesDarCollectionServiceDAO() {
    return new DarCollectionServiceDAO(
        providesDatasetDAO(),
        providesElectionDAO(),
        providesJdbi(),
        providesUserDAO());
  }

  @Provides
  DataAccessRequestServiceDAO providesDataAccessRequestServiceDAO() {
    return new DataAccessRequestServiceDAO(
        providesDataAccessRequestDAO(),
        providesJdbi(),
        providesDARCollectionDAO()
    );
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
  VoteServiceDAO providesVoteServiceDAO() {
    return new VoteServiceDAO(
        providesJdbi(),
        providesVoteDAO());
  }

  @Provides
  VoteService providesVoteService() {
    return new VoteService(
        providesUserDAO(),
        providesDARCollectionDAO(),
        providesDataAccessRequestDAO(),
        providesDatasetDAO(),
        providesElectionDAO(),
        providesEmailService(),
        providesElasticSearchService(),
        providesUseRestrictionConverter(),
        providesVoteDAO(),
        providesVoteServiceDAO());
  }

  @Provides
  DatasetDAO providesDatasetDAO() {
    return datasetDAO;
  }

  @Provides
  DaaServiceDAO providesDaaServiceDAO() {
    return new DaaServiceDAO(
        providesJdbi(),
        providesDaaDAO(),
        providesFileStorageObjectDAO());
  }

  @Provides
  DacServiceDAO providesDacServiceDAO() {
    return new DacServiceDAO(
        providesJdbi());
  }

  @Provides
  DaaDAO providesDaaDAO() {
    return daaDAO;
  }

  @Provides
  DacDAO providesDacDAO() {
    return dacDAO;
  }

  @Provides
  DaaService providesDaaService() {
    return new DaaService(
        providesDaaServiceDAO(),
        providesDaaDAO(),
        providesGCSService(),
        providesEmailService(),
        providesUserService(),
        providesInstitutionDAO(),
        providesDacDAO());
  }

  @Provides
  DacService providesDacService() {
    return new DacService(
        providesDacDAO(),
        providesUserDAO(),
        providesDatasetDAO(),
        providesElectionDAO(),
        providesDataAccessRequestDAO(),
        providesVoteService(),
        providesDaaService(),
        providesDacServiceDAO());
  }

  @Provides
  ElasticSearchService providesElasticSearchService() {
    return new ElasticSearchService(
        ElasticSearchSupport.createRestClient(config.getElasticSearchConfiguration()),
        config.getElasticSearchConfiguration(),
        providesDacDAO(),
        providesDataAccessRequestDAO(),
        providesUserDAO(),
        providesOntologyService(),
        providesInstitutionDAO(),
        providesDatasetDAO(),
        providesStudyDAO()
    );
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
  MatchService providesMatchService() {
    return new MatchService(
        providesClient(),
        config.getServicesConfiguration(),
        providesMatchDAO(),
        providesDataAccessRequestDAO(),
        providesDatasetDAO(),
        providesUseRestrictionConverter());
  }

  @Provides
  MatchDAO providesMatchDAO() {
    return matchDAO;
  }

  @Provides
  MailMessageDAO providesMailMessageDAO() {
    return mailMessageDAO;
  }

  @Provides
  MetricsService providesMetricsService() {
    return new MetricsService(
        providesDacService(),
        providesDatasetDAO(),
        providesDataAccessRequestDAO(),
        providesDARCollectionDAO(),
        providesMatchDAO(),
        providesElectionDAO()
    );
  }

  @Provides
  UserPropertyDAO providesUserPropertyDAO() {
    return userPropertyDAO;
  }

  @Provides
  InstitutionDAO providesInstitutionDAO() {
    return institutionDAO;
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
  AcknowledgementDAO providesAcknowledgementDAO() {
    return acknowledgementDAO;
  }

  @Provides
  InstitutionService providesInstitutionService() {
    return new InstitutionService(providesInstitutionDAO(), providesUserDAO());
  }

  @Provides
  LibraryCardService providesLibraryCardService() {
    return new LibraryCardService(
        providesLibraryCardDAO(),
        providesInstitutionDAO(),
        providesUserDAO());
  }

  @Provides
  AcknowledgementService providesAcknowledgementService() {
    return new AcknowledgementService(
        providesAcknowledgementDAO()
    );
  }

  @Provides
  DatasetRegistrationService providesDatasetRegistrationService() {
    return new DatasetRegistrationService(
        providesDatasetDAO(),
        providesDacDAO(),
        providesDatasetServiceDAO(),
        providesGCSService(),
        providesElasticSearchService(),
        providesStudyDAO(),
        providesEmailService()
    );
  }

  @Provides
  UserServiceDAO providesUserServiceDAO() {
    return new UserServiceDAO(
        providesJdbi(),
        providesUserDAO(),
        providesUserRoleDAO()
    );
  }

  @Provides
  UserService providesUserService() {
    return new UserService(
        providesUserDAO(),
        providesUserPropertyDAO(),
        providesUserRoleDAO(),
        providesVoteDAO(),
        providesInstitutionDAO(),
        providesLibraryCardDAO(),
        providesAcknowledgementDAO(),
        providesFileStorageObjectDAO(),
        providesSamDAO(),
        providesUserServiceDAO(),
        providesDaaDAO(),
        providesEmailService(),
        providesDraftSubmissionService());
  }

  @Provides
  ResearcherService providesResearcherService() {
    return new ResearcherService(
        providesUserPropertyDAO(),
        providesUserDAO()
    );
  }

  @Provides
  NihService providesNihService() {
    return new NihService(
        providesResearcherService(),
        providesUserDAO(),
        providesNIHServiceDAO());
  }

  @Provides
  NihServiceDAO providesNIHServiceDAO() {
    return new NihServiceDAO(jdbi);
  }

  @Provides
  SamService providesSamService() {
    return new SamService(providesSamDAO());
  }

  @Provides
  SamDAO providesSamDAO() {
    return new SamDAO(providesHttpClientUtil(), config.getServicesConfiguration());
  }

  @Provides
  OidcAuthorityDAO providesOidcAuthorityDAO() {
    return new OidcAuthorityDAO(providesHttpClientUtil(), config.getOidcConfiguration());
  }

  @Provides
  OidcService providesOidcService() {
    return new OidcService(providesOidcAuthorityDAO(), config.getOidcConfiguration());
  }

  @Provides
  SupportRequestService providesSupportRequestService() {
    return new SupportRequestService(config.getServicesConfiguration(), providesInstitutionDAO(),
        providesUserDAO());
  }

  @Provides
  DraftSubmissionDAO providesDraftSubmissionDAO() {
    return draftSubmissionDAO;
  }

  @Provides
  DraftFileStorageService providesDraftFileStorageService() {
    return new DraftFileStorageService(providesJdbi(), providesGCSService(),
        providesFileStorageObjectDAO());
  }

  @Provides
  DraftSubmissionService providesDraftSubmissionService() {
    return new DraftSubmissionService(providesJdbi(), providesDraftSubmissionDAO(),
        providesDraftFileStorageService());
  }
}
