package org.broadinstitute.consent.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.service.ontology.ElasticSearchSupport;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * Covers the default local mode — {@code ES_SECURITY_ENABLED} unset, so {@code
 * xpack.security.enabled=false} — which is what every developer not working on Epic D runs.
 *
 * <p>The assertion that earns its keep is {@link
 * #configuredCredentialsAreToleratedWhenSecurityIsDisabled()}: it is why {@code
 * config/consent.yaml} can carry {@code authUser}/{@code authPassword} unconditionally instead of
 * requiring a per-mode edit. The Apache HttpClient credentials provider that {@code
 * ElasticSearchSupport.createRestClient} installs only sends credentials in response to a 401
 * challenge, which a security-disabled cluster never issues. If a future version started rejecting
 * requests that carry unexpected credentials, switching modes would become an edit to {@code
 * consent.yaml} again.
 *
 * <p>See {@link ElasticSearchTestCluster} for the version pin, and {@link
 * ElasticSearchSecurityBaselineTest} for the security-enabled counterparts.
 */
class ElasticSearchSecurityDisabledTest {

  /** Single-node container with security off, matching the compose default. */
  private static final ElasticsearchContainer ELASTIC = ElasticSearchTestCluster.container(false);

  private static final ElasticSearchConfiguration CONFIGURATION;

  static {
    ELASTIC.start();
    CONFIGURATION = ElasticSearchTestCluster.configuration(ELASTIC, "dataset-insecure-test");
  }

  @Test
  void unauthenticatedRequestsSucceed() throws Exception {
    try (RestClient anonymous =
        ElasticSearchSupport.createRestClient(
            ElasticSearchTestCluster.withoutCredentials(CONFIGURATION))) {

      assertEquals(200, ElasticSearchTestCluster.status(anonymous, new Request("GET", "/")));
    }
  }

  /** Credentials in the configuration are harmless when the cluster never challenges for them. */
  @Test
  void configuredCredentialsAreToleratedWhenSecurityIsDisabled() throws Exception {
    try (RestClient credentialed = ElasticSearchSupport.createRestClient(CONFIGURATION)) {
      assertEquals(200, ElasticSearchTestCluster.status(credentialed, new Request("GET", "/")));
    }
  }

  /**
   * Port 9200 serves plain {@code http} only, so {@code protocol: http} in {@code consent.yaml}
   * needs no TLS trust configuration locally.
   */
  @Test
  void tlsIsNotServedOnTheHttpPort() throws Exception {
    ElasticSearchTestCluster.assertTlsIsNotServed(CONFIGURATION);
  }
}
