package org.broadinstitute.consent.http.service.ontologyIndexer;

import org.apache.http.HttpHost;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.elasticsearch.client.RestClient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class ElasticSearchSupport {

    public static RestClient getRestClient(ElasticSearchConfiguration configuration) {
        HttpHost[] hosts = configuration.
            getServers().
            stream().
            map(server -> new HttpHost(server, 9200, "http")).
            collect(Collectors.toList()).toArray(new HttpHost[configuration.getServers().size()]);
        return RestClient.builder(hosts).build();
    }

    public static String getIndexPath(String index) {
        return "/" + index;
    }

    public static String getTermIdPath(String index, String termId) throws UnsupportedEncodingException {
        return "/" + index + "/ontology_term/" + URLEncoder.encode(termId, "UTF-8");
    }

    public static String getClusterHealthPath(String index) {
        return "/_cluster/health/" + index;
    }

}
