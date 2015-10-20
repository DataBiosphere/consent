package org.genomebridge.consent.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.db.DataSourceFactory;
import org.genomebridge.consent.http.cloudstore.StoreConfiguration;
import org.genomebridge.consent.http.db.mongo.MongoConfiguration;
import org.genomebridge.consent.http.service.ServicesConfiguration;
import org.genomebridge.consent.http.service.UseRestrictionConfig;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class ConsentConfiguration extends Configuration {

    public ConsentConfiguration() {
    }

    @Valid
    @NotNull
    @JsonProperty
    private final DataSourceFactory database = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty
    private final UseRestrictionConfig ontology = new UseRestrictionConfig();

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

}
