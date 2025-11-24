package org.broadinstitute.consent.http.service.ontology;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.opensearch.client.RestClient;

@SuppressWarnings("WeakerAccess")
public class ElasticSearchSupport {

  public static RestClient createRestClient(ElasticSearchConfiguration configuration) {
    HttpHost[] hosts =
        configuration.getServers().stream()
            .map(server -> new HttpHost("http", server, configuration.getPort()))
            .toList()
            .toArray(new HttpHost[configuration.getServers().size()]);
    return RestClient.builder(hosts).build();
  }

  public static String getClusterHealthPath() {
    return "/_cluster/health";
  }

  public static Header jsonHeader = new BasicHeader("Content-Type", "application/json");
}
