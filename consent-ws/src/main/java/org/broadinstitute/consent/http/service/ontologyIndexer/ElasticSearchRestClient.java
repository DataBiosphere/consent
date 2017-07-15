package org.broadinstitute.consent.http.service.ontologyIndexer;

import org.apache.http.HttpHost;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.elasticsearch.client.RestClient;

import java.util.stream.Collectors;

public class ElasticSearchRestClient {

    public static RestClient getRestClient(ElasticSearchConfiguration configuration) {
        HttpHost[] hosts = configuration.
            getServers().
            stream().
            map(server -> new HttpHost(server, 9200, "http")).
            collect(Collectors.toList()).toArray(new HttpHost[configuration.getServers().size()]);
        return RestClient.builder(hosts).build();
    }

}
