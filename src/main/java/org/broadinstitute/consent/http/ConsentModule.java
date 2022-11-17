package org.broadinstitute.consent.http;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Environment;
import org.broadinstitute.consent.http.authentication.OAuthAuthenticator;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.db.AcknowledgementDAO;
import org.broadinstitute.consent.http.db.ConsentAuditDAO;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.CounterDAO;
import org.broadinstitute.consent.http.db.DAOContainer;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DarCollectionSummaryDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetAssociationDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.service.AcknowledgementService;
import org.broadinstitute.consent.http.service.AuditService;
import org.broadinstitute.consent.http.service.ConsentService;
import org.broadinstitute.consent.http.service.CounterService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.DarCollectionService;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetAssociationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.EmailService;
import org.broadinstitute.consent.http.service.InstitutionService;
import org.broadinstitute.consent.http.service.LibraryCardService;
import org.broadinstitute.consent.http.service.MatchService;
import org.broadinstitute.consent.http.service.MetricsService;
import org.broadinstitute.consent.http.service.NihService;
import org.broadinstitute.consent.http.service.PendingCaseService;
import org.broadinstitute.consent.http.service.ResearcherService;
import org.broadinstitute.consent.http.service.SummaryService;
import org.broadinstitute.consent.http.service.SupportRequestService;
import org.broadinstitute.consent.http.service.UseRestrictionConverter;
import org.broadinstitute.consent.http.service.UseRestrictionValidator;
import org.broadinstitute.consent.http.service.FileStorageObjectService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.VoteService;
import org.broadinstitute.consent.http.service.dao.DarCollectionServiceDAO;
import org.broadinstitute.consent.http.service.dao.DataAccessRequestServiceDAO;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.broadinstitute.consent.http.service.dao.VoteServiceDAO;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.gson2.Gson2Plugin;
import org.jdbi.v3.guava.GuavaPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import javax.ws.rs.client.Client;

public class ConsentModule extends AbstractModule {

    @Inject
    private final ConsentConfiguration config;
    @Inject
    private final Environment environment;

    private final Client client;
    private final Jdbi jdbi;
    private final ConsentDAO consentDAO;
    private final CounterDAO counterDAO;
    private final ElectionDAO electionDAO;
    private final VoteDAO voteDAO;
    private final DatasetDAO datasetDAO;
    private final DatasetAssociationDAO datasetAssociationDAO;
    private final DacDAO dacDAO;
    private final UserDAO userDAO;
    private final UserRoleDAO userRoleDAO;
    private final MatchDAO matchDAO;
    private final MailMessageDAO mailMessageDAO;
    private final UserPropertyDAO userPropertyDAO;
    private final ConsentAuditDAO consentAuditDAO;
    private final DataAccessRequestDAO dataAccessRequestDAO;
    private final DarCollectionDAO darCollectionDAO;
    private final DarCollectionSummaryDAO darCollectionSummaryDAO;
    private final InstitutionDAO institutionDAO;
    private final LibraryCardDAO libraryCardDAO;
    private final FileStorageObjectDAO fileStorageObjectDAO;
    private final AcknowledgementDAO acknowledgementDAO;

    public static final String DB_ENV = "postgresql";

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

        this.consentDAO = this.jdbi.onDemand(ConsentDAO.class);
        this.counterDAO = this.jdbi.onDemand(CounterDAO.class);
        this.electionDAO = this.jdbi.onDemand(ElectionDAO.class);
        this.voteDAO = this.jdbi.onDemand(VoteDAO.class);
        this.datasetDAO = this.jdbi.onDemand(DatasetDAO.class);
        this.datasetAssociationDAO = this.jdbi.onDemand(DatasetAssociationDAO.class);
        this.dacDAO = this.jdbi.onDemand(DacDAO.class);
        this.userDAO = this.jdbi.onDemand(UserDAO.class);
        this.userRoleDAO = this.jdbi.onDemand(UserRoleDAO.class);
        this.matchDAO = this.jdbi.onDemand(MatchDAO.class);
        this.mailMessageDAO = this.jdbi.onDemand(MailMessageDAO.class);
        this.userPropertyDAO = this.jdbi.onDemand(UserPropertyDAO.class);
        this.consentAuditDAO = this.jdbi.onDemand(ConsentAuditDAO.class);
        this.dataAccessRequestDAO = this.jdbi.onDemand(DataAccessRequestDAO.class);
        this.darCollectionDAO = this.jdbi.onDemand(DarCollectionDAO.class);
        this.darCollectionSummaryDAO = this.jdbi.onDemand(DarCollectionSummaryDAO.class);
        this.institutionDAO = this.jdbi.onDemand((InstitutionDAO.class));
        this.libraryCardDAO = this.jdbi.onDemand((LibraryCardDAO.class));
        this.fileStorageObjectDAO = this.jdbi.onDemand((FileStorageObjectDAO.class));
        this.acknowledgementDAO = this.jdbi.onDemand((AcknowledgementDAO.class));
    }

    @Override
    protected void configure() {
        bind(Configuration.class).toInstance(config);
        bind(Environment.class).toInstance(environment);
    }

    @Provides
    public DAOContainer providesDAOContainer() {
        DAOContainer container = new DAOContainer();
        container.setConsentAuditDAO(providesConsentAuditDAO());
        container.setConsentDAO(providesConsentDAO());
        container.setCounterDAO(providesCounterDAO());
        container.setDacDAO(providesDacDAO());
        container.setDataAccessRequestDAO(providesDataAccessRequestDAO());
        container.setDarCollectionDAO(providesDARCollectionDAO());
        container.setDarCollectionSummaryDAO(providesDarCollectionSummaryDAO());
        container.setDatasetAssociationDAO(providesDatasetAssociationDAO());
        container.setDatasetDAO(providesDatasetDAO());
        container.setElectionDAO(providesElectionDAO());
        container.setMailMessageDAO(providesMailMessageDAO());
        container.setMatchDAO(providesMatchDAO());
        container.setResearcherPropertyDAO(providesResearcherPropertyDAO());
        container.setUserDAO(providesUserDAO());
        container.setUserRoleDAO(providesUserRoleDAO());
        container.setVoteDAO(providesVoteDAO());
        container.setInstitutionDAO(providesInstitutionDAO());
        container.setUserFileDAO(providesUserFileDAO());
        container.setAcknowledgementDAO(providesAcknowledgementDAO());
        return container;
    }

    @Provides
    Client providesClient() {
        return client;
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
    OAuthAuthenticator providesOAuthAuthenticator() {
        return new OAuthAuthenticator(providesClient(), providesSamService());
    }

    @Provides
    UseRestrictionValidator providesUseRestrictionValidator() {
        return new UseRestrictionValidator(providesClient(), config.getServicesConfiguration());
    }

    @Provides
    AuditService providesAuditService() {
        return new AuditService(
                providesUserDAO(),
                providesConsentAuditDAO());
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
    FileStorageObjectService providesUserFileService() {
        return new FileStorageObjectService(
                providesUserFileDAO(),
                providesGCSService()
        );
    }

    @Provides
    GCSStore providesGCSStore() {
        return new GCSStore(config.getCloudStoreConfiguration());
    }

    @Provides
    GCSService providesGCSService() {
        return new GCSService(config.getCloudStoreConfiguration());
    }

    @Provides
    ConsentService providesConsentService() {
        return new ConsentService(
                providesConsentDAO(),
                providesElectionDAO(),
                providesUseRestrictionConverter());
    }

    @Provides
    ConsentDAO providesConsentDAO() {
        return consentDAO;
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
                providesDataAccessRequestServiceDAO()
        );
    }

    @Provides
    DatasetServiceDAO providesDatasetServiceDAO() {
        return new DatasetServiceDAO(
                jdbi,
                providesDatasetDAO());
    }

    @Provides
    DatasetService providesDatasetService() {
        return new DatasetService(
                providesConsentDAO(),
                providesDataAccessRequestDAO(),
                providesDatasetDAO(),
                providesDatasetServiceDAO(),
                providesUserRoleDAO(),
                providesDacDAO(),
                providesUseRestrictionConverter(),
                providesEmailService());
    }

    @Provides
    DatasetAssociationService providesDatasetAssociationService() {
        return new DatasetAssociationService(
            providesDatasetAssociationDAO(),
            providesUserDAO(),
            providesDatasetDAO(),
            providesUserRoleDAO()
        );
    }

    @Provides
    ElectionService providesElectionService() {
        return new ElectionService(
                providesConsentDAO(),
                providesElectionDAO(),
                providesVoteDAO(),
                providesUserDAO(),
                providesDatasetDAO(),
                providesLibraryCardDAO(),
                providesDatasetAssociationDAO(),
                providesDataAccessRequestDAO(),
                providesDARCollectionDAO(),
                providesMailMessageDAO(),
                providesEmailService(),
                providesDataAccessRequestService(),
                providesUseRestrictionConverter());
    }

    @Provides
    FreeMarkerTemplateHelper providesFreeMarkerTemplateHelper() {
        return new FreeMarkerTemplateHelper(config.getFreeMarkerConfiguration());
    }

    @Provides
    EmailService providesEmailService() {
        return new EmailService(
                providesDARCollectionDAO(),
                providesConsentDAO(),
                providesVoteDAO(),
                providesElectionDAO(),
                providesUserDAO(),
                providesMailMessageDAO(),
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
    PendingCaseService providesPendingCaseService() {
        return new PendingCaseService(
                providesConsentDAO(),
                providesDataAccessRequestService(),
                providesElectionDAO(),
                providesVoteDAO(),
                providesDacService(),
                providesUserService(),
                providesVoteService(),
                providesDarCollectionService());
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
                providesDatasetAssociationDAO(),
                providesDatasetDAO(),
                providesElectionDAO(),
                providesEmailService(),
                providesUseRestrictionConverter(),
                providesVoteDAO(),
                providesVoteServiceDAO());
    }

    @Provides
    DatasetDAO providesDatasetDAO() {
        return datasetDAO;
    }

    @Provides
    DatasetAssociationDAO providesDatasetAssociationDAO() {
        return datasetAssociationDAO;
    }

    @Provides
    DacDAO providesDacDAO() {
        return dacDAO;
    }

    @Provides
    DacService providesDacService() {
        return new DacService(
                providesDacDAO(),
                providesUserDAO(),
                providesDatasetDAO(),
                providesElectionDAO(),
                providesDataAccessRequestDAO(),
                providesVoteService());
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
                providesConsentDAO(),
                providesMatchDAO(),
                providesElectionDAO(),
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
    UserPropertyDAO providesResearcherPropertyDAO() {
        return userPropertyDAO;
    }

    @Provides
    ConsentAuditDAO providesConsentAuditDAO() {
        return consentAuditDAO;
    }

    @Provides
    InstitutionDAO providesInstitutionDAO() {
        return institutionDAO;
    }

    @Provides
    FileStorageObjectDAO providesUserFileDAO() {
        return fileStorageObjectDAO;
    }

    @Provides
    LibraryCardDAO providesLibraryCardDAO() { return libraryCardDAO; }

    @Provides
    AcknowledgementDAO providesAcknowledgementDAO() { return acknowledgementDAO; }

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
    UserService providesUserService() {
        return new UserService(
                providesUserDAO(),
                providesResearcherPropertyDAO(),
                providesUserRoleDAO(),
                providesVoteDAO(),
                providesInstitutionDAO(),
                providesLibraryCardDAO(),
                providesSamDAO());
    }

    @Provides
    ResearcherService providesResearcherService() {
        return new ResearcherService(
                providesResearcherPropertyDAO(),
                providesUserDAO()
        );
    }

    @Provides
    NihService providesNihService() {
        return new NihService(providesResearcherService(), providesLibraryCardDAO(), providesUserDAO());
    }

    @Provides
    SummaryService providesSummaryService() {
        return new SummaryService(
            providesDataAccessRequestService(),
            providesVoteDAO(),
            providesElectionDAO(),
            providesUserDAO(),
            providesConsentDAO(),
            providesDatasetDAO(),
            providesMatchDAO(),
            providesDARCollectionDAO()
        );
    }

    @Provides
    SamService providesSamService() {
        return new SamService(providesSamDAO());
    }

    @Provides
    SamDAO providesSamDAO() {
        return new SamDAO(config.getServicesConfiguration());
    }

    @Provides
    SupportRequestService providesSupportRequestService() {
        return new SupportRequestService(config.getServicesConfiguration(), providesInstitutionDAO(), providesUserDAO());
    }
}
