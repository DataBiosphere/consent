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
import org.apache.log4j.Logger;
import org.genomebridge.consent.http.db.ConsentDAO;
import org.genomebridge.consent.http.resources.AllAssociationsResource;
import org.genomebridge.consent.http.resources.AllConsentsResource;
import org.genomebridge.consent.http.resources.ConsentAssociationResource;
import org.genomebridge.consent.http.resources.ConsentResource;
import org.genomebridge.consent.http.service.ConsentAPI;
import org.genomebridge.consent.http.service.ConsentAPIProvider;
import org.genomebridge.consent.http.service.DatabaseConsentAPI;
import org.skife.jdbi.v2.DBI;

/**
 * Top-level entry point to the entire application.
 *
 * See the Dropwizard docs here:
 *   https://dropwizard.github.io/dropwizard/manual/core.html
 *
 */
public class ConsentApplication extends Application<ConsentConfiguration> {

    public static void main(String[] args) throws Exception {
        new ConsentApplication().run(args);
    }

    public void run(ConsentConfiguration config, Environment env) {

        Logger.getLogger("ConsentApplication").debug("ConsentApplication.run called.");
        // Set up the ConsentAPI and the ConsentDAO.  We are working around a dropwizard+Guice issue
        // with singletons and JDBI (see ConsentAPIProvider).
        try {
            final DBIFactory factory = new DBIFactory();
            final DBI jdbi = factory.build(env, config.getDataSourceFactory(), "db");
            final ConsentDAO dao = jdbi.onDemand(ConsentDAO.class);
            final ConsentAPI api = new DatabaseConsentAPI(dao);
            ConsentAPIProvider.setApi(api);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

        // How register our resources.

        env.jersey().register(ConsentResource.class);
        env.jersey().register(AllConsentsResource.class);
        env.jersey().register(ConsentAssociationResource.class);
        env.jersey().register(AllAssociationsResource.class);
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
