package org.genomebridge.consent.http;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.genomebridge.consent.http.db.ConsentDAO;
import org.genomebridge.consent.http.resources.*;
import org.genomebridge.consent.http.db.DACUserDAO;
import org.genomebridge.consent.http.db.DataRequestDAO;
import org.genomebridge.consent.http.db.DataSetDAO;
import org.genomebridge.consent.http.db.ElectionDAO;
import org.genomebridge.consent.http.db.ResearchPurposeDAO;
import org.genomebridge.consent.http.db.VoteDAO;
import org.genomebridge.consent.http.resources.AllAssociationsResource;
import org.genomebridge.consent.http.resources.AllConsentsResource;
import org.genomebridge.consent.http.resources.ConsentAssociationResource;
import org.genomebridge.consent.http.resources.ConsentResource;
import org.genomebridge.consent.http.resources.ConsentElectionResource;
import org.genomebridge.consent.http.resources.DataRequestElectionResource;
import org.genomebridge.consent.http.resources.ConsentVoteResource;
import org.genomebridge.consent.http.resources.DataRequestVoteResource;
import org.genomebridge.consent.http.service.AbstractConsentAPI;
import org.genomebridge.consent.http.service.AbstractDataRequestAPI;
import org.genomebridge.consent.http.service.AbstractElectionAPI;
import org.genomebridge.consent.http.service.AbstractVoteAPI;
import org.genomebridge.consent.http.service.DatabaseConsentAPI;
import org.genomebridge.consent.http.service.DatabaseDataRequestAPI;
import org.genomebridge.consent.http.service.DatabaseElectionAPI;
import org.genomebridge.consent.http.service.DatabaseVoteAPI;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        try {
            final DBIFactory factory = new DBIFactory();
            final DBI jdbi = factory.build(env, config.getDataSourceFactory(), "db");
            final ConsentDAO consentDAO = jdbi.onDemand(ConsentDAO.class);

            DatabaseConsentAPI.initInstance(jdbi, consentDAO);
            final DACUserDAO dacUserDAO = jdbi.onDemand(DACUserDAO.class);
            final ElectionDAO electionDAO = jdbi.onDemand(ElectionDAO.class);
            final VoteDAO voteDAO = jdbi.onDemand(VoteDAO.class);
            final DataRequestDAO requestDAO = jdbi.onDemand(DataRequestDAO.class);
            final DataSetDAO dataSetDAO = jdbi.onDemand(DataSetDAO.class);
            final ResearchPurposeDAO purposeDAO = jdbi.onDemand(ResearchPurposeDAO.class);
            DatabaseElectionAPI.initInstance(electionDAO, consentDAO, requestDAO);
            DatabaseVoteAPI.initInstance(voteDAO, dacUserDAO, electionDAO);
            DatabaseDataRequestAPI.initInstance(requestDAO,dataSetDAO,purposeDAO);
            
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

        // How register our resources.

        env.jersey().register(ConsentResource.class);
        env.jersey().register(ConsentsResource.class);
        env.jersey().register(AllConsentsResource.class);
        env.jersey().register(ConsentAssociationResource.class);
        env.jersey().register(AllAssociationsResource.class);
        env.jersey().register(ConsentElectionResource.class);
        env.jersey().register(DataRequestElectionResource.class);
        env.jersey().register(ConsentVoteResource.class);
        env.jersey().register(DataRequestVoteResource.class);
        env.jersey().register(DataRequestResource.class);
        // Register a listener to catch an application stop and clear out the API instance created above.
        // For normal exit, this is a no-op, but the junit tests that use the DropWizardAppRule will
        // repeatedly start and stop the application, all within the same JVM, causing the run() method to be
        // called multiple times.
        env.lifecycle().addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener() {
            @Override
            public void lifeCycleStopped(LifeCycle event) {
                LOGGER.debug("**** ConsentAppliction Server Stopped ****");
                AbstractConsentAPI.clearInstance();
                AbstractElectionAPI.clearInstance();
                AbstractVoteAPI.clearInstance();
                AbstractDataRequestAPI.clearInstance();
            }
        });
    }

    public void initialize(Bootstrap<ConsentConfiguration> bootstrap) {

        bootstrap.addBundle(new MigrationsBundle<ConsentConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(ConsentConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });

        bootstrap.addBundle(new AssetsBundle("/assets/", "/site"));
    }
}
