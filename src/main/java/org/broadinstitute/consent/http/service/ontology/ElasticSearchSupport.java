package org.broadinstitute.consent.http.service.ontology;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.elasticsearch.client.RestClient;

@SuppressWarnings("WeakerAccess")
public class ElasticSearchSupport {

    public static RestClient createRestClient(ElasticSearchConfiguration configuration) {
        HttpHost[] hosts = configuration.
                getServers().
                stream().
                map(server -> new HttpHost(server, configuration.getPort(), "http")).
                toList().
                toArray(new HttpHost[configuration.getServers().size()]);
        return RestClient.builder(hosts).build();
    }

    public static String getClusterHealthPath() {
        return "/_cluster/health";
    }

    public static Header jsonHeader = new BasicHeader("Content-Type", "application/json");

}
