package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class ConsentConfiguration extends Configuration {

    public ConsentConfiguration() {
    }

    @Valid
    @NotNull
    @JsonProperty
    private final DataSourceFactory database = new DataSourceFactory();

    private final UseRestrictionConfig ontology = new UseRestrictionConfig(
        "http://www.broadinstitute.org/ontologies/DURPO/methods_research",
        "http://www.broadinstitute.org/ontologies/DURPO/aggregate_analysis",
        "http://www.broadinstitute.org/ontologies/DURPO/control",
        "http://www.broadinstitute.org/ontologies/DURPO/population",
        "http://www.broadinstitute.org/ontologies/DURPO/male",
        "http://www.broadinstitute.org/ontologies/DURPO/female",
        "http://www.broadinstitute.org/ontologies/DURPO/For_profit",
        "http://www.broadinstitute.org/ontologies/DURPO/Non_profit",
        "http://www.broadinstitute.org/ontologies/DURPO/boys",
        "http://www.broadinstitute.org/ontologies/DURPO/girls",
        "http://www.broadinstitute.org/ontologies/DURPO/children"
    );

    @Valid
    @NotNull
    @JsonProperty
    private final StoreConfiguration googleStore = new StoreConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    private final MongoConfiguration mongo = new MongoConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    private final ServicesConfiguration services = new ServicesConfiguration();

    @Valid
    @NotNull
    private JerseyClientConfiguration httpClient = new JerseyClientConfiguration();

    @Valid
    @NotNull
    private MailConfiguration mailConfiguration = new MailConfiguration();

    @Valid
    @NotNull
    private FreeMarkerConfiguration freeMarkerConfiguration = new FreeMarkerConfiguration();

    @JsonProperty("httpClient")
    public JerseyClientConfiguration getJerseyClientConfiguration() {
        return httpClient;
    }

    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    public StoreConfiguration getCloudStoreConfiguration() {
        return googleStore;
    }

    public UseRestrictionConfig getUseRestrictionConfiguration() {
        return ontology;
    }

    public MongoConfiguration getMongoConfiguration() {
        return mongo;
    }

    public ServicesConfiguration getServicesConfiguration() {
        return services;
    }

    public MailConfiguration getMailConfiguration() {
        return mailConfiguration;
    }

    public FreeMarkerConfiguration getFreeMarkerConfiguration() {
        return freeMarkerConfiguration;
    }
}
