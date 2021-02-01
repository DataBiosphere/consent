package org.broadinstitute.consent.http;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Environment;
import javax.ws.rs.client.Client;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.db.ApprovalExpirationTimeDAO;
import org.broadinstitute.consent.http.db.AssociationDAO;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.CounterDAO;
import org.broadinstitute.consent.http.db.DAOContainer;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetAssociationDAO;
import org.broadinstitute.consent.http.db.DataSetAuditDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.MailServiceDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.MetricsDAO;
import org.broadinstitute.consent.http.db.ResearcherPropertyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.WorkspaceAuditDAO;
import org.broadinstitute.consent.http.mail.MailService;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.service.ApprovalExpirationTimeService;
import org.broadinstitute.consent.http.service.AuditService;
import org.broadinstitute.consent.http.service.ConsentService;
import org.broadinstitute.consent.http.service.CounterService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetAssociationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.MetricsService;
import org.broadinstitute.consent.http.service.PendingCaseService;
import org.broadinstitute.consent.http.service.SummaryService;
import org.broadinstitute.consent.http.service.UseRestrictionConverter;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.VoteService;
import org.broadinstitute.consent.http.service.WhitelistService;
import org.broadinstitute.consent.http.service.users.handler.UserRolesHandler;
import org.broadinstitute.consent.http.util.WhitelistCache;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.gson2.Gson2Plugin;
import org.jdbi.v3.guava.GuavaPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

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
    private final DataSetDAO datasetDAO;
    private final DatasetAssociationDAO datasetAssociationDAO;
    private final DacDAO dacDAO;
    private final UserDAO userDAO;
    private final UserRoleDAO userRoleDAO;
    private final MatchDAO matchDAO;
    private final MailMessageDAO mailMessageDAO;
    private final MetricsDAO metricsDAO;
    private final ApprovalExpirationTimeDAO approvalExpirationTimeDAO;
    private final DataSetAuditDAO datasetAuditDAO;
    private final MailServiceDAO mailServiceDAO;
    private final ResearcherPropertyDAO researcherPropertyDAO;
    private final WorkspaceAuditDAO workspaceAuditDAO;
    private final AssociationDAO associationDAO;
    private final DataAccessRequestDAO dataAccessRequestDAO;

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
        this.datasetDAO = this.jdbi.onDemand(DataSetDAO.class);
        this.datasetAssociationDAO = this.jdbi.onDemand(DatasetAssociationDAO.class);
        this.dacDAO = this.jdbi.onDemand(DacDAO.class);
        this.userDAO = this.jdbi.onDemand(UserDAO.class);
        this.userRoleDAO = this.jdbi.onDemand(UserRoleDAO.class);
        this.matchDAO = this.jdbi.onDemand(MatchDAO.class);
        this.mailMessageDAO = this.jdbi.onDemand(MailMessageDAO.class);
        this.metricsDAO = this.jdbi.onDemand(MetricsDAO.class);
        this.approvalExpirationTimeDAO = this.jdbi.onDemand(ApprovalExpirationTimeDAO.class);
        this.datasetAuditDAO = this.jdbi.onDemand(DataSetAuditDAO.class);
        this.mailServiceDAO = this.jdbi.onDemand(MailServiceDAO.class);
        this.researcherPropertyDAO = this.jdbi.onDemand(ResearcherPropertyDAO.class);
        this.workspaceAuditDAO = this.jdbi.onDemand(WorkspaceAuditDAO.class);
        this.associationDAO = this.jdbi.onDemand(AssociationDAO.class);
        this.dataAccessRequestDAO = this.jdbi.onDemand(DataAccessRequestDAO.class);
    }

    @Override
    protected void configure() {
        bind(Configuration.class).toInstance(config);
        bind(Environment.class).toInstance(environment);
    }

    @Provides
    public DAOContainer providesDAOContainer() {
        DAOContainer container = new DAOContainer();
        container.setApprovalExpirationTimeDAO(providesApprovalExpirationTimeDAO());
        container.setAssociationDAO(providesAssociationDAO());
        container.setConsentDAO(providesConsentDAO());
        container.setCounterDAO(providesCounterDAO());
        container.setDacDAO(providesDacDAO());
        container.setDataAccessRequestDAO(providesDataAccessRequestDAO());
        container.setDatasetAssociationDAO(providesDatasetAssociationDAO());
        container.setDatasetAuditDAO(providesDataSetAuditDAO());
        container.setDatasetDAO(providesDataSetDAO());
        container.setElectionDAO(providesElectionDAO());
        container.setMailMessageDAO(providesMailMessageDAO());
        container.setMailServiceDAO(providesMailServiceDAO());
        container.setMatchDAO(providesMatchDAO());
        container.setResearcherPropertyDAO(providesResearcherPropertyDAO());
        container.setUserDAO(providesUserDAO());
        container.setUserRoleDAO(providesUserRoleDAO());
        container.setVoteDAO(providesVoteDAO());
        container.setWorkspaceAuditDAO(providesWorkspaceAuditDAO());
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
    ApprovalExpirationTimeService providesApprovalExpirationTimeService() {
        return new ApprovalExpirationTimeService(
            providesApprovalExpirationTimeDAO(),
            providesUserDAO());
    }

    @Provides
    AuditService providesAuditService() {
        return new AuditService(
                providesUserDAO(),
                providesWorkspaceAuditDAO());
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
                providesVoteDAO(),
                providesDacService(),
                providesDataAccessRequestDAO());
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
                providesUserService());
    }

    @Provides
    DatasetService providesDatasetService() {
        return new DatasetService(providesConsentDAO(), providesDataSetDAO(), providesUserRoleDAO(), providesUseRestrictionConverter());
    }

    @Provides
    DatasetAssociationService providesDatasetAssociationService() {
        return new DatasetAssociationService(
            providesDatasetAssociationDAO(),
            providesUserDAO(),
            providesDataSetDAO(),
            providesUserRoleDAO()
        );
    }

    @Provides
    ElectionService providesElectionService() {
        return new ElectionService(
                providesConsentDAO(),
                providesElectionDAO(),
                providesDacService(),
                providesDataAccessRequestService());
    }

    @Provides
    FreeMarkerTemplateHelper providesFreeMarkerTemplateHelper() {
        return new FreeMarkerTemplateHelper(config.getFreeMarkerConfiguration());
    }

    @Provides
    EmailNotifierService providesEmailNotifierService() {
        return new EmailNotifierService(
                providesConsentDAO(),
                providesDataAccessRequestService(),
                providesVoteDAO(),
                providesElectionDAO(),
                providesUserDAO(),
                providesMailMessageDAO(),
                providesMailService(),
                providesMailServiceDAO(),
                providesFreeMarkerTemplateHelper(),
                config.getServicesConfiguration().getLocalURL(),
                config.getMailConfiguration().isActivateEmailNotifications(),
                providesResearcherPropertyDAO()
        );
    }

    @Provides
    MailService providesMailService() {
        return new MailService(config.getMailConfiguration());
    }

    @Provides
    PendingCaseService providesPendingCaseService() {
        return new PendingCaseService(
                providesConsentDAO(),
                providesDataAccessRequestService(),
                providesDataSetDAO(),
                providesElectionDAO(),
                providesVoteDAO(),
                providesDacService(),
                providesUserService(),
                providesVoteService());
    }

    @Provides
    DataAccessRequestDAO providesDataAccessRequestDAO() {
        return dataAccessRequestDAO;
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
    VoteService providesVoteService() {
        return new VoteService(
                providesUserDAO(),
                providesDatasetAssociationDAO(),
                providesElectionDAO(),
                providesVoteDAO());
    }

    @Provides
    DataSetDAO providesDataSetDAO() {
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
                providesDataSetDAO(),
                providesElectionDAO(),
                providesDataAccessRequestDAO(),
                providesUserService(),
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
    UserRolesHandler providesUserRolesHandler() {
        return new UserRolesHandler(
                providesUserDAO(),
                providesDataAccessRequestService(),
                providesElectionDAO(),
                providesUserRoleDAO(),
                providesVoteDAO());
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
    MetricsDAO providesMetricsDAO() {
        return metricsDAO;
    }

    @Provides
    MetricsService providesMetricsService() {
        return new MetricsService(
                providesDacService(),
                providesDataSetDAO(),
                providesMetricsDAO()
        );
    }

    @Provides
    ApprovalExpirationTimeDAO providesApprovalExpirationTimeDAO() {
        return approvalExpirationTimeDAO;
    }

    @Provides
    DataSetAuditDAO providesDataSetAuditDAO() {
        return datasetAuditDAO;
    }

    @Provides
    MailServiceDAO providesMailServiceDAO() {
        return mailServiceDAO;
    }

    @Provides
    ResearcherPropertyDAO providesResearcherPropertyDAO() {
        return researcherPropertyDAO;
    }

    @Provides
    WorkspaceAuditDAO providesWorkspaceAuditDAO() {
        return workspaceAuditDAO;
    }

    @Provides
    AssociationDAO providesAssociationDAO() {
        return associationDAO;
    }

    @Provides
    UserService providesUserService() {
        return new UserService(
                providesUserDAO(),
                providesResearcherPropertyDAO(),
                providesUserRoleDAO(),
                providesVoteDAO());
    }

    @Provides
    SummaryService providesSummaryService() {
        return new SummaryService(
            providesDataAccessRequestService(),
            providesVoteDAO(),
            providesElectionDAO(),
            providesUserDAO(),
            providesConsentDAO(),
            providesDataSetDAO(),
            providesMatchDAO()
        );
    }

    @Provides
    WhitelistService providesWhitelistService() {
        return new WhitelistService(
                providesGCSService(),
                providesWhitelistCache());
    }

    @Provides
    WhitelistCache providesWhitelistCache() {
        return new WhitelistCache(providesGCSService());
    }

}
