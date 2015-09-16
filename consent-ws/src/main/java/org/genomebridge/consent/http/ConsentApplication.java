package org.genomebridge.consent.http;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.genomebridge.consent.http.cloudstore.GCSStore;
import org.genomebridge.consent.http.db.*;
import org.genomebridge.consent.http.resources.*;
import org.genomebridge.consent.http.service.*;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.EnumSet;
import org.genomebridge.consent.http.db.mongo.MongoConfiguration;

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

    public void run(ConsentConfiguration config, Environment env) {

        LOGGER.debug("ConsentApplication.run called.");
        // Set up the ConsentAPI and the ConsentDAO.  We are working around a dropwizard+Guice issue
        // with singletons and JDBI (see AbstractConsentAPI).
        final DBIFactory factory = new DBIFactory();
        final DBI jdbi = factory.build(env, config.getDataSourceFactory(), "db");
        final ConsentDAO consentDAO = jdbi.onDemand(ConsentDAO.class);

        DatabaseConsentAPI.initInstance(jdbi, consentDAO);
        final ElectionDAO electionDAO = jdbi.onDemand(ElectionDAO.class);
        final VoteDAO voteDAO = jdbi.onDemand(VoteDAO.class);
        final DataRequestDAO requestDAO = jdbi.onDemand(DataRequestDAO.class);
        final DataSetDAO dataSetDAO = jdbi.onDemand(DataSetDAO.class);
        final ResearchPurposeDAO purposeDAO = jdbi.onDemand(ResearchPurposeDAO.class);
        final DACUserDAO dacUserDAO = jdbi.onDemand(DACUserDAO.class);
        final DACUserRoleDAO dacUserRoleDAO = jdbi.onDemand(DACUserRoleDAO.class);

        DatabaseDataSetAPI.initInstance(dataSetDAO);
        DatabaseElectionAPI.initInstance(electionDAO, consentDAO, requestDAO, dacUserDAO);
        DatabaseDataRequestAPI.initInstance(requestDAO, dataSetDAO, purposeDAO);
        DatabaseSummaryAPI.initInstance(voteDAO, electionDAO, dacUserDAO);
        DatabaseElectionCaseAPI.initInstance(electionDAO, voteDAO, dacUserDAO, dacUserRoleDAO);
        DatabaseDACUserAPI.initInstance(dacUserDAO, dacUserRoleDAO);
        DatabaseVoteAPI.initInstance(voteDAO, dacUserDAO, electionDAO);
        DatabaseReviewResultsAPI.initInstance(electionDAO, voteDAO, consentDAO, dacUserDAO);

        UseRestrictionConverter structResearchPurposeConv = new UseRestrictionConverter(config.getUseRestrictionConfiguration());
        final MongoConfiguration mongoConfiguration = config.getMongoConfiguration();
        final MongoClient mongoClient = new MongoClient(new MongoClientURI(mongoConfiguration.getUri()));
        MongoDataAccessRequestAPI.initInstance(mongoClient, structResearchPurposeConv);
        MongoResearchPurposeAPI.initInstance(mongoClient);

        env.healthChecks().register("mongo", new MongoHealthCheck(mongoClient));

        final FilterRegistration.Dynamic cors = env.servlets().addFilter("crossOriginRequsts", CrossOriginFilter.class);

        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        cors.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, env.getApplicationContext().getContextPath() + "/*");

        // Configure CORS parameters
        cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
        cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_HEADERS_HEADER, "X-Requested-With,Content-Type,Accept,Origin,Authorization");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin,Authorization");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "OPTIONS,GET,PUT,POST,DELETE,HEAD");
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
