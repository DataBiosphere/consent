package org.genomebridge.consent.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import org.genomebridge.consent.http.cloudstore.StoreConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.genomebridge.consent.http.db.mongo.MongoClientFactory;
import org.genomebridge.consent.http.db.mongo.MongoConfiguration;

public class ConsentConfiguration extends Configuration {

    public ConsentConfiguration() {
    }

    @Valid
    @NotNull
    @JsonProperty
    private DataSourceFactory database = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty
    private StoreConfiguration googleStore = new StoreConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    private MongoConfiguration mongo = new MongoConfiguration();
    
    @Valid
    @NotNull
    private MongoClientFactory mongoFactory = new MongoClientFactory();
        
    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    public StoreConfiguration getCloudStoreConfiguration() {
        return googleStore;
    }

    public MongoConfiguration getMongoConfiguration() {
        return mongo;
    }

    public MongoClientFactory getMongoFactory() {
        return mongoFactory;
    }

}
