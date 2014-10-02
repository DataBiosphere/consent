/*
 * Copyright 2014 Broad Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.genomebridge.consent.http;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.util.component.LifeCycle;
import org.genomebridge.consent.http.db.ConsentDAO;
import org.genomebridge.consent.http.resources.AllAssociationsResource;
import org.genomebridge.consent.http.resources.AllConsentsResource;
import org.genomebridge.consent.http.resources.ConsentAssociationResource;
import org.genomebridge.consent.http.resources.ConsentResource;
import org.genomebridge.consent.http.service.AbstractConsentAPI;
import org.genomebridge.consent.http.service.DatabaseConsentAPI;
import org.skife.jdbi.v2.DBI;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top-level entry point to the entire application.
 *
 * See the Dropwizard docs here:
 *   https://dropwizard.github.io/dropwizard/manual/core.html
 *
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
            final ConsentDAO dao = jdbi.onDemand(ConsentDAO.class);
            DatabaseConsentAPI.initInstance(dao);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

        // How register our resources.

        env.jersey().register(ConsentResource.class);
        env.jersey().register(AllConsentsResource.class);
        env.jersey().register(ConsentAssociationResource.class);
        env.jersey().register(AllAssociationsResource.class);

        // Register a listener to catch an application stop and clear out the API instance created above.
        // For normal exit, this is a no-op, but the junit tests that use the DropWizardAppRule will
        // repeatedly start and stop the application, all within the same JVM, causing the run() method to be
        // called multiple times.
        env.lifecycle().addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener() {
            @Override
            public void lifeCycleStopped(LifeCycle event) {
                LOGGER.debug("**** ConsentAppliction Server Stopped ****");
                AbstractConsentAPI.clearInstance();
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
