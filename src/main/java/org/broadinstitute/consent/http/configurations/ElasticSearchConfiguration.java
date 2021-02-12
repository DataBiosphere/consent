package org.broadinstitute.consent.http.configurations;

import java.util.List;
import javax.validation.constraints.NotNull;

public class ElasticSearchConfiguration {

    @NotNull
    private String indexName;

    @NotNull
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
