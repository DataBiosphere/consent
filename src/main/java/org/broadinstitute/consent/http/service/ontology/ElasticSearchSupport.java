package org.broadinstitute.consent.http.service.ontology;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

@SuppressWarnings("WeakerAccess")
public class ElasticSearchSupport {

  public static RestClient createRestClient(ElasticSearchConfiguration configuration) {
    RestClientBuilder builder;
    if (configuration.getCloudId() != null && !configuration.getCloudId().isBlank()) {
      builder = RestClient.builder(configuration.getCloudId());
    } else {
      HttpHost[] hosts =
          configuration.getServers().stream()
              .map(
                  server ->
                      new HttpHost(server, configuration.getPort(), configuration.getProtocol()))
              .toList()
              .toArray(new HttpHost[configuration.getServers().size()]);
      builder = RestClient.builder(hosts);
    }
    if (configuration.getAuthUser() != null
        && !configuration.getAuthUser().isBlank()
        && configuration.getAuthPassword() != null
        && !configuration.getAuthPassword().isBlank()) {
      CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(
          AuthScope.ANY,
          new UsernamePasswordCredentials(
              configuration.getAuthUser(), configuration.getAuthPassword()));
      builder.setHttpClientConfigCallback(
          httpClientBuilder ->
              httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
    }
    return builder.build();
  }

  public static String getClusterHealthPath() {
    return "/_cluster/health";
  }

  public static Header jsonHeader = new BasicHeader("Content-Type", "application/json");
}
