package org.broadinstitute.consent.http;

import org.broadinstitute.consent.http.service.DatabaseDataRequestAPI;
import org.broadinstitute.consent.http.service.DatabaseElectionAPI;
import org.broadinstitute.consent.http.service.DatabaseMatchProcessAPI;
import org.broadinstitute.consent.http.service.DatabaseElectionCaseAPI;
import org.broadinstitute.consent.http.service.DatabaseMatchAPI;
import org.broadinstitute.consent.http.service.DatabaseDACUserAPI;
import org.broadinstitute.consent.http.service.AbstractEmailNotifierAPI;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.DatabaseMatchingServiceAPI;
import org.broadinstitute.consent.http.service.AbstractMatchAPI;
import org.broadinstitute.consent.http.service.DatabaseReviewResultsAPI;
import org.broadinstitute.consent.http.service.AbstractMatchingServiceAPI;
import org.broadinstitute.consent.http.service.AbstractPendingCaseAPI;
import org.broadinstitute.consent.http.service.AbstractReviewResultsAPI;
import org.broadinstitute.consent.http.service.AbstractDACUserAPI;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.AbstractMatchProcessAPI;
import org.broadinstitute.consent.http.service.AbstractSummaryAPI;
import org.broadinstitute.consent.http.service.AbstractDataRequestAPI;
import org.broadinstitute.consent.http.service.DatabaseResearchPurposeAPI;
import org.broadinstitute.consent.http.service.DatabaseConsentAPI;
import org.broadinstitute.consent.http.service.UseRestrictionConverter;
import org.broadinstitute.consent.http.service.AbstractTranslateServiceAPI;
import org.broadinstitute.consent.http.service.AbstractDataSetAPI;
import org.broadinstitute.consent.http.service.DatabaseVoteAPI;
import org.broadinstitute.consent.http.service.DatabaseDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.AbstractVoteAPI;
import org.broadinstitute.consent.http.service.DatabaseDataSetAPI;
import org.broadinstitute.consent.http.service.DatabaseSummaryAPI;
import org.broadinstitute.consent.http.service.DatabaseTranslateServiceAPI;
import org.broadinstitute.consent.http.service.AbstractResearchPurposeAPI;
import org.broadinstitute.consent.http.resources.ConsentResource;
import org.broadinstitute.consent.http.resources.DataAccessRequestResource;
import org.broadinstitute.consent.http.resources.ElectionReviewResource;
import org.broadinstitute.consent.http.resources.DataRequestElectionResource;
import org.broadinstitute.consent.http.resources.ConsentElectionResource;
import org.broadinstitute.consent.http.resources.DataUseLetterResource;
import org.broadinstitute.consent.http.resources.ConsentsResource;
import org.broadinstitute.consent.http.resources.ConsentAssociationResource;
import org.broadinstitute.consent.http.resources.EmailNotifierResource;
import org.broadinstitute.consent.http.resources.AllAssociationsResource;
import org.broadinstitute.consent.http.resources.DataRequestVoteResource;
import org.broadinstitute.consent.http.resources.ResearchPurposeResource;
import org.broadinstitute.consent.http.resources.DataRequestResource;
import org.broadinstitute.consent.http.resources.ConsentVoteResource;
import org.broadinstitute.consent.http.resources.ElectionResource;
import org.broadinstitute.consent.http.resources.MatchResource;
import org.broadinstitute.consent.http.resources.ConsentManageResource;
import org.broadinstitute.consent.http.resources.DataRequestCasesResource;
import org.broadinstitute.consent.http.resources.DataSetResource;
import org.broadinstitute.consent.http.resources.DACUserResource;
import org.broadinstitute.consent.http.resources.ConsentCasesResource;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.ResearchPurposeDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.DataRequestDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.DACUserRoleDAO;
import org.broadinstitute.consent.http.db.ConsentDAO;
import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.configurations.MongoConfiguration;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.filter.CORSFilter;
import org.broadinstitute.consent.http.mail.AbstractMailServiceAPI;
import org.broadinstitute.consent.http.mail.MailService;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.ws.rs.client.Client;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.EnumSet;

/**
 * Top-level entry point to the entire application.
 * <p/>
 * See the Dropwizard docs here:
 * https://dropwizard.github.io/dropwizard/manual/core.html
 */
public class ConsentApplication extends Application<ConsentConfiguration> {
    public static final Logger LOGGER = LoggerFactory.getLogger("ConsentApplication");

    public static void main(String[] args) throws Exception {
        new ConsentApplication().run(args);
    }

    @Override
    public void run(ConsentConfiguration config, Environment env) {

        LOGGER.debug("ConsentApplication.run called.");
        // Client to consume another services
        final Client client = new JerseyClientBuilder(env).using(config.getJerseyClientConfiguration())
                .build(getName());
        // Set up the ConsentAPI and the ConsentDAO.  We are working around a dropwizard+Guice issue
        // with singletons and JDBI (see AbstractConsentAPI).
        
        final MongoConfiguration mongoConfiguration = config.getMongoConfiguration();
        final MongoClient mongoClient;
        
        if (mongoConfiguration.isTestMode()) {
            Fongo fongo = new Fongo("TestServer");
            mongoClient =  fongo.getMongo();
        } else {
            mongoClient = new MongoClient(new MongoClientURI(mongoConfiguration.getUri()));
        }
       
        final MongoConsentDB mongoInstance = new MongoConsentDB(mongoClient);
        mongoInstance.configureMongo();
                
        final DBIFactory factory = new DBIFactory();
        final DBI jdbi = factory.build(env, config.getDataSourceFactory(), "db");
        final ConsentDAO consentDAO = jdbi.onDemand(ConsentDAO.class);
        final ElectionDAO electionDAO = jdbi.onDemand(ElectionDAO.class);
        final VoteDAO voteDAO = jdbi.onDemand(VoteDAO.class);
        final DataRequestDAO requestDAO = jdbi.onDemand(DataRequestDAO.class);
        final DataSetDAO dataSetDAO = jdbi.onDemand(DataSetDAO.class);
        final ResearchPurposeDAO purposeDAO = jdbi.onDemand(ResearchPurposeDAO.class);
        final DACUserDAO dacUserDAO = jdbi.onDemand(DACUserDAO.class);
        final DACUserRoleDAO dacUserRoleDAO = jdbi.onDemand(DACUserRoleDAO.class);
        final MatchDAO matchDAO = jdbi.onDemand(MatchDAO.class);
        final MailMessageDAO emailDAO = jdbi.onDemand(MailMessageDAO.class);

        UseRestrictionConverter structResearchPurposeConv = new UseRestrictionConverter(config.getUseRestrictionConfiguration());
        DatabaseDataAccessRequestAPI.initInstance(mongoInstance, structResearchPurposeConv, electionDAO);
        DatabaseConsentAPI.initInstance(jdbi, consentDAO ,electionDAO , mongoInstance);
        DatabaseMatchAPI.initInstance(matchDAO, consentDAO);
        DatabaseDataSetAPI.initInstance(dataSetDAO, electionDAO, dacUserRoleDAO , consentDAO);
        DatabaseMatchingServiceAPI.initInstance(client, config.getServicesConfiguration());
        DatabaseMatchProcessAPI.initInstance(consentDAO, mongoInstance);
        DatabaseDataRequestAPI.initInstance(requestDAO, dataSetDAO, purposeDAO);
        DatabaseSummaryAPI.initInstance(voteDAO, electionDAO, dacUserDAO, consentDAO, dataSetDAO ,matchDAO, mongoInstance );
        DatabaseElectionCaseAPI.initInstance(electionDAO, voteDAO, dacUserDAO, dacUserRoleDAO,consentDAO, mongoInstance);
        DatabaseDACUserAPI.initInstance(dacUserDAO, dacUserRoleDAO);
        DatabaseVoteAPI.initInstance(voteDAO, dacUserDAO, electionDAO);
        DatabaseReviewResultsAPI.initInstance(electionDAO, voteDAO, consentDAO);
        DatabaseResearchPurposeAPI.initInstance(mongoInstance);
        DatabaseTranslateServiceAPI.initInstance(client, config.getServicesConfiguration());
        //env.healthChecks().register("mongo", new MongoHealthCheck(mongoClient));
        DatabaseElectionAPI.initInstance(electionDAO, consentDAO, dacUserDAO, mongoInstance, voteDAO, emailDAO, dataSetDAO);

        // Mail Services
        try {
            MailService.initInstance(config.getMailConfiguration());
            EmailNotifierService.initInstance(voteDAO, electionDAO, dacUserDAO, emailDAO, new FreeMarkerTemplateHelper(config.getFreeMarkerConfiguration()), config.getServicesConfiguration().getLocalURL(), config.getMailConfiguration().isActivateEmailNotifications());
        } catch (IOException e) {
            LOGGER.error("Error on Mail Notificacion Service initialization. Service won't work.", e);
        }

        final FilterRegistration.Dynamic cors = env.servlets().addFilter("crossOriginRequsts", CORSFilter.class);
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        cors.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, env.getApplicationContext().getContextPath() + "/*");

        // Configure CORS parameters
        cors.setInitParameter(CORSFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(CORSFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin,Authorization");
        cors.setInitParameter(CORSFilter.ALLOWED_METHODS_PARAM, "OPTIONS,GET,PUT,POST,DELETE,HEAD");

        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
        GCSStore googleStore;
        try {
            googleStore = new GCSStore(config.getCloudStoreConfiguration());
        } catch (GeneralSecurityException | IOException e) {
            LOGGER.error("Couldn't connect to to Google Cloud Storage.", e);
            throw new IllegalStateException(e);
        }

        // How register our resources.
        env.jersey().register(DataAccessRequestResource.class);
        env.jersey().register(DataSetResource.class);
        env.jersey().register(ConsentResource.class);
        env.jersey().register(ConsentsResource.class);
        env.jersey().register(ConsentAssociationResource.class);
        env.jersey().register(new DataUseLetterResource(googleStore));
        env.jersey().register(AllAssociationsResource.class);
        env.jersey().register(ConsentElectionResource.class);
        env.jersey().register(DataRequestElectionResource.class);
        env.jersey().register(ConsentVoteResource.class);
        env.jersey().register(DataRequestVoteResource.class);
        env.jersey().register(ConsentCasesResource.class);
        env.jersey().register(DataRequestCasesResource.class);
        env.jersey().register(DataRequestResource.class);
        env.jersey().register(DACUserResource.class);
        env.jersey().register(ElectionReviewResource.class);
        env.jersey().register(ConsentManageResource.class);
        env.jersey().register(ResearchPurposeResource.class);
        env.jersey().register(ElectionResource.class);
        env.jersey().register(MatchResource.class);
        env.jersey().register(EmailNotifierResource.class);
        // Register a listener to catch an application stop and clear out the API instance created above.
        // For normal exit, this is a no-op, but the junit tests that use the DropWizardAppRule will
        // repeatedly start and stop the application, all within the same JVM, causing the run() method to be
        // called multiple times.
        env.lifecycle().addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener() {

            @Override
            public void lifeCycleStopped(LifeCycle event) {
                LOGGER.debug("**** ConsentApplication Server Stopped ****");
                AbstractTranslateServiceAPI.clearInstance();
                AbstractConsentAPI.clearInstance();
                AbstractElectionAPI.clearInstance();
                AbstractVoteAPI.clearInstance();
                AbstractPendingCaseAPI.clearInstance();
                AbstractDataRequestAPI.clearInstance();
                AbstractDACUserAPI.clearInstance();
                AbstractSummaryAPI.clearInstance();
                AbstractReviewResultsAPI.clearInstance();
                AbstractResearchPurposeAPI.clearInstance();
                AbstractDataSetAPI.clearInstance();
                AbstractDataAccessRequestAPI.clearInstance();
                AbstractMatchingServiceAPI.clearInstance();
                AbstractMatchAPI.clearInstance();
                AbstractMatchProcessAPI.clearInstance();
                AbstractMailServiceAPI.clearInstance();
                AbstractEmailNotifierAPI.clearInstance();
                super.lifeCycleStopped(event);
            }
        });
    }

    @Override
    public void initialize(Bootstrap<ConsentConfiguration> bootstrap) {

        bootstrap.addBundle(new MultiPartBundle());
        bootstrap.addBundle(new AssetsBundle("/assets/", "/site"));
        bootstrap.addBundle(new MigrationsBundle<ConsentConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(ConsentConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });
        bootstrap.addBundle(new DBIExceptionsBundle());
    }
}
