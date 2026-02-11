package org.broadinstitute.consent.http.service.ontology;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ElasticSearchSupportTest {

  // This is a base64 encoded cloudId for testing purposes. It decodes to
  // "test:localhost:9200$myuuid$kibanauuid"
  static final String CLOUD_ID = "test:bG9jYWxob3N0OjkyMDAkbXl1dWlkJGtpYmFuYXV1aWQ=";

  @Test
  void testCreateRestClientWithServers() {
    ElasticSearchConfiguration config = new ElasticSearchConfiguration();
    config.setServers(List.of("localhost"));
    config.setPort(9200);
    config.setProtocol("http");
    config.setIndexName("test");
    config.setDatasetIndexName("test");

    RestClient client = ElasticSearchSupport.createRestClient(config);
    assertNotNull(client);
  }

  @Test
  void testCreateRestClientWithCloudId() {
    ElasticSearchConfiguration config = new ElasticSearchConfiguration();
    config.setCloudId(CLOUD_ID);
    config.setIndexName("test");
    config.setDatasetIndexName("test");
    config.setServers(List.of());

    RestClient client = ElasticSearchSupport.createRestClient(config);
    assertNotNull(client);
  }

  @Test
  void testCreateRestClientWithAuth() {
    ElasticSearchConfiguration config = new ElasticSearchConfiguration();
    config.setServers(List.of("localhost"));
    config.setPort(9200);
    config.setProtocol("http");
    config.setAuthUser("user");
    config.setAuthPassword("password");
    config.setIndexName("test");
    config.setDatasetIndexName("test");

    RestClient client = ElasticSearchSupport.createRestClient(config);
    assertNotNull(client);
  }

  @Test
  void testCreateRestClientWithCloudIdAndAuth() {
    ElasticSearchConfiguration config = new ElasticSearchConfiguration();
    config.setCloudId(CLOUD_ID);
    config.setAuthUser("user");
    config.setAuthPassword("password");
    config.setIndexName("test");
    config.setDatasetIndexName("test");
    config.setServers(List.of());

    RestClient client = ElasticSearchSupport.createRestClient(config);
    assertNotNull(client);
  }

  @Test
  void testCreateRestClientWithBlankCloudId() {
    ElasticSearchConfiguration config = new ElasticSearchConfiguration();
    config.setCloudId(" ");
    config.setServers(List.of("localhost"));
    config.setPort(9200);
    config.setProtocol("http");
    config.setIndexName("test");
    config.setDatasetIndexName("test");

    RestClient client = ElasticSearchSupport.createRestClient(config);
    assertNotNull(client);
  }

  @Test
  void testCreateRestClientWithBlankAuth() {
    ElasticSearchConfiguration config = new ElasticSearchConfiguration();
    config.setServers(List.of("localhost"));
    config.setPort(9200);
    config.setProtocol("http");
    config.setAuthUser(" ");
    config.setAuthPassword("password");
    config.setIndexName("test");
    config.setDatasetIndexName("test");

    RestClient client = ElasticSearchSupport.createRestClient(config);
    assertNotNull(client);
  }

  @Test
  void testCreateRestClientWithCloudIdAndBlankAuth() {
    ElasticSearchConfiguration config = new ElasticSearchConfiguration();
    config.setCloudId(CLOUD_ID);
    config.setAuthUser("user");
    config.setAuthPassword("");
    config.setIndexName("test");
    config.setDatasetIndexName("test");
    config.setServers(List.of());

    RestClient client = ElasticSearchSupport.createRestClient(config);
    assertNotNull(client);
  }
}
