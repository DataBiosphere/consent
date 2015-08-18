package org.genomebridge.consent.autocomplete;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

public class AutocompleteConfiguration extends Configuration {

    public AutocompleteConfiguration() {}

    @JsonProperty
    private ElasticSearchConfiguration elasticSearch = new ElasticSearchConfiguration();

    public ElasticSearchConfiguration getElasticSearchConfiguration() {
        return elasticSearch;
    }

    @JsonProperty
    private CorsConfiguration cors = new CorsConfiguration();

    public CorsConfiguration getCorsConfiguration() {
        return cors;
    }

}
