package org.broadinstitute.consent.http;

import com.github.fakemongo.Fongo;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.mongodb.MongoClient;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Environment;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.configurations.MongoConfiguration;
import org.broadinstitute.consent.http.db.ApprovalExpirationTimeDAO;
import org.broadinstitute.consent.http.db.AssociationDAO;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DACUserRoleDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.DataSetAuditDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.HelpReportDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.MailServiceDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.ResearcherPropertyDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.WorkspaceAuditDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.UseRestrictionConverter;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class ConsentModule extends AbstractModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentModule.class.getName());

    @Inject
    private final ConsentConfiguration config;
    @Inject
    private final Environment environment;

    private final Client client;
    private final DBI jdbi;
    private final MongoConsentDB mongoInstance;
    private final ConsentDAO consentDAO;
    private final ElectionDAO electionDAO;
    private final HelpReportDAO helpReportDAO;
    private final VoteDAO voteDAO;
    private final DataSetDAO datasetDAO;
    private final DataSetAssociationDAO datasetAssociationDAO;
    private final DacDAO dacDAO;
    private final DACUserDAO dacUserDAO;
    private final DACUserRoleDAO dacUserRoleDAO;
    private final MatchDAO matchDAO;
    private final MailMessageDAO mailMessageDAO;
    private final ApprovalExpirationTimeDAO approvalExpirationTimeDAO;
    private final DataSetAuditDAO datasetAuditDAO;
    private final MailServiceDAO mailServiceDAO;
    private final ResearcherPropertyDAO researcherPropertyDAO;
    private final WorkspaceAuditDAO workspaceAuditDAO;
    private final AssociationDAO associationDAO;

    ConsentModule(ConsentConfiguration consentConfiguration, Environment environment) {
        this.config = consentConfiguration;
        this.environment = environment;
        this.client = new JerseyClientBuilder(environment)
                .using(config.getJerseyClientConfiguration())
                .build(this.getClass().getName());

        this.jdbi = new DBIFactory().build(this.environment, config.getDataSourceFactory(), "mysql");
        this.mongoInstance = initMongoDBInstance();

        this.consentDAO = this.jdbi.onDemand(ConsentDAO.class);
        this.electionDAO = this.jdbi.onDemand(ElectionDAO.class);
        this.helpReportDAO = this.jdbi.onDemand(HelpReportDAO.class);
        this.voteDAO = this.jdbi.onDemand(VoteDAO.class);
        this.datasetDAO = this.jdbi.onDemand(DataSetDAO.class);
        this.datasetAssociationDAO = this.jdbi.onDemand(DataSetAssociationDAO.class);
        this.dacDAO = this.jdbi.onDemand(DacDAO.class);
        this.dacUserDAO = this.jdbi.onDemand(DACUserDAO.class);
        this.dacUserRoleDAO= this.jdbi.onDemand(DACUserRoleDAO.class);
        this.matchDAO = this.jdbi.onDemand(MatchDAO.class);
        this.mailMessageDAO = this.jdbi.onDemand(MailMessageDAO.class);
        this.approvalExpirationTimeDAO = this.jdbi.onDemand(ApprovalExpirationTimeDAO.class);
        this.datasetAuditDAO = this.jdbi.onDemand(DataSetAuditDAO.class);
        this.mailServiceDAO = this.jdbi.onDemand(MailServiceDAO.class);
        this.researcherPropertyDAO = this.jdbi.onDemand(ResearcherPropertyDAO.class);
        this.workspaceAuditDAO = this.jdbi.onDemand(WorkspaceAuditDAO.class);
        this.associationDAO = this.jdbi.onDemand(AssociationDAO.class);
    }

    @Override
    protected void configure() {
        bind(Configuration.class).toInstance(config);
        bind(Environment.class).toInstance(environment);
    }

    @Provides
    Client providesClient() {
        return client;
    }

    @Provides
    DBI providesDBI() {
        return this.jdbi;
    }

    @Provides
    MongoConsentDB providesMongoConsentDB() {
        return this.mongoInstance;
    }

    @Provides
    UseRestrictionConverter providesUseRestrictionConverter() {
        return new UseRestrictionConverter(providesClient(), config.getServicesConfiguration());
    }

    @Provides
    GCSStore providesGCSStore() {
        try {
            return new GCSStore(config.getCloudStoreConfiguration());
        } catch (GeneralSecurityException | IOException e) {
            LOGGER.error("Couldn't connect to to Google Cloud Storage.", e);
            throw new IllegalStateException(e);
        }
    }

    @Provides
    ConsentDAO providesConsentDAO() {
        return consentDAO;
    }

    @Provides
    ElectionDAO providesElectionDAO() {
        return electionDAO;
    }

    @Provides
    HelpReportDAO providesHelpReportDAO() {
        return helpReportDAO;
    }

    @Provides
    VoteDAO providesVoteDAO() {
        return voteDAO;
    }

    @Provides
    DataSetDAO providesDataSetDAO() {
        return datasetDAO;
    }

    @Provides
    DataSetAssociationDAO providesDataSetAssociationDAO() {
        return datasetAssociationDAO;
    }

    @Provides
    DacDAO providesDacDAO() {
        return dacDAO;
    }

    @Provides
    DacService providesDacService() {
        return new DacService(providesDacDAO());
    }

    @Provides
    DACUserDAO providesDACUserDAO() {
        return dacUserDAO;
    }

    @Provides
    DACUserRoleDAO providesDACUserRoleDAO() {
        return dacUserRoleDAO;
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

    // Private helpers

    private MongoConsentDB initMongoDBInstance() {
        MongoClient mongoClient = getMongoClient();
        String dbName = config.getMongoConfiguration().getDbName();
        MongoConsentDB instance = new MongoConsentDB(mongoClient, dbName);
        instance.configureMongo();
        return instance;
    }

    private MongoClient getMongoClient() {
        MongoConfiguration mongoConfiguration = config.getMongoConfiguration();
        if (mongoConfiguration.isTestMode()) {
            Fongo fongo = new Fongo("TestServer");
            return fongo.getMongo();
        } else {
            return mongoConfiguration.getMongoClient();
        }
    }

}
