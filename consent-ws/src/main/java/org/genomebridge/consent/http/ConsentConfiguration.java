package org.genomebridge.consent.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import org.genomebridge.consent.http.cloudstore.StoreConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.genomebridge.consent.http.db.mongo.MongoConfiguration;
import org.genomebridge.consent.http.service.UseRestrictionConfig;

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

}
