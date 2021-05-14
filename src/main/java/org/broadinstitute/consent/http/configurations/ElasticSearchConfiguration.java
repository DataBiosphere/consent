package org.broadinstitute.consent.http.configurations;

import javax.validation.constraints.NotNull;
import java.util.List;

public class ElasticSearchConfiguration {

    @NotNull
    private String indexName;

    @NotNull
    private List<String> servers;

    /**
     * This is configurable for testing purposes
     */
    private int port = 9200;

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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
