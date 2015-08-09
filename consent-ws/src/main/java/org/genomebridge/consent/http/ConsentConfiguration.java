package org.genomebridge.consent.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import org.genomebridge.consent.http.cloudstore.StoreConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class ConsentConfiguration extends Configuration {

    public ConsentConfiguration() {}

    @Valid
    @NotNull
    @JsonProperty
    private DataSourceFactory database = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty
    private StoreConfiguration googleStore = new StoreConfiguration();

    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    public StoreConfiguration getCloudStoreConfiguration() {
        return googleStore;
    }

}
