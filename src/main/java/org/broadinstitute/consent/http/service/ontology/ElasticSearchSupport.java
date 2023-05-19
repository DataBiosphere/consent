package org.broadinstitute.consent.http.service.ontology;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
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

  public static ElasticsearchClient createClient(ElasticSearchConfiguration configuration) {
    RestClient restClient = createRestClient(configuration);

    // Create the transport with a Jackson mapper
    ElasticsearchTransport transport = new RestClientTransport(
        restClient, new JacksonJsonpMapper());

    // And create the API client
    ElasticsearchClient client = new ElasticsearchClient(transport);

    return client;

  }

  public static String getClusterHealthPath() {
    return "/_cluster/health";
  }

  public static Header jsonHeader = new BasicHeader("Content-Type", "application/json");

}
