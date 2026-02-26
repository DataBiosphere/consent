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
    String cloudId = configuration.getCloudId();
    if (cloudId != null && !cloudId.trim().isEmpty()) {
      builder = RestClient.builder(cloudId);
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
    String authUser = configuration.getAuthUser();
    String authPassword = configuration.getAuthPassword();
    if (authUser != null
        && !authUser.trim().isEmpty()
        && authPassword != null
        && !authPassword.trim().isEmpty()) {
      CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(
          AuthScope.ANY, new UsernamePasswordCredentials(authUser, authPassword));
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
