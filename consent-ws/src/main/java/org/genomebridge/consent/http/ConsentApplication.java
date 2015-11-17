package org.genomebridge.consent.http;

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
import org.genomebridge.consent.http.cloudstore.GCSStore;
import org.genomebridge.consent.http.configurations.ConsentConfiguration;
import org.genomebridge.consent.http.configurations.MongoConfiguration;
import org.genomebridge.consent.http.db.*;
import org.genomebridge.consent.http.db.mongo.MongoConsentDB;
import org.genomebridge.consent.http.filter.CORSFilter;
import org.genomebridge.consent.http.mail.AbstractMailServiceAPI;
import org.genomebridge.consent.http.mail.MailService;
import org.genomebridge.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.genomebridge.consent.http.resources.*;
import org.genomebridge.consent.http.service.*;
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
        //env.healthChecks().register("mongo", new MongoHealthCheck(mongoClient));
        DatabaseElectionAPI.initInstance(electionDAO, consentDAO, dacUserDAO, mongoInstance, voteDAO, emailDAO);

        // Mail Services
        try {
            MailService.initInstance(config.getMailConfiguration());
            EmailNotifierService.initInstance(voteDAO, electionDAO, dacUserDAO, emailDAO, new FreeMarkerTemplateHelper(config.getFreeMarkerConfiguration()), config.getServicesConfiguration().getLocalURL(), config.getMailConfiguration().isActivateEmailNotifications());
        } catch (IOException e) {
            LOGGER.error("Error on Mail Notificacion Service initialization. Service won't work.", e);
            e.printStackTrace();
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
