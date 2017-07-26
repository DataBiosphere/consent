package org.broadinstitute.consent.http.configurations;

import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

public class ElasticSearchConfiguration {

    @NotEmpty
    private String indexName;

    @NotEmpty
    private List<String> servers;

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }
}
