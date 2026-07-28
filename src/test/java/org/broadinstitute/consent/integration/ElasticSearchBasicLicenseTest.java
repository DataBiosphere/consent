package org.broadinstitute.consent.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.service.ontology.ElasticSearchSupport;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * Asserts what a <em>basic</em> license refuses, and — the part that matters for security — that it
 * refuses it by failing closed rather than by silently returning unrestricted data.
 *
 * <p>This class needs its own container because {@link ElasticSearchContainerTests} activates the
 * trial license in its static initializer, and a trial can be started only once per cluster.
 * Nothing here starts the trial until the final, deliberately-ordered test.
 *
 * <p>The trap this pins down: {@code POST /_security/api_key} <em>accepts</em> a DLS/FLS role
 * descriptor under a basic license, so a D-2 implementation looks correct at credential-creation
 * time and only fails later, on the search. A future version that instead honored the descriptor
 * loosely — returning documents the DLS query excludes — would leak data, which is what {@link
 * #searchWithDlsFlsApiKeyFailsClosedUnderBasicLicense()} exists to catch.
 *
 * <p>See {@link ElasticSearchTestCluster} for the version pin and the shared probe payloads.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ElasticSearchBasicLicenseTest {

  private static final String INDEX = "dataset-basic-license-test";

  /** Security-enabled but left on the self-generated basic license. Reaped by Ryuk at JVM exit. */
  private static final ElasticsearchContainer ELASTIC = ElasticSearchTestCluster.container(true);

  private static final ElasticSearchConfiguration CONFIGURATION;
  private static final RestClient CLIENT;

  static {
    ELASTIC.start();
    CONFIGURATION = ElasticSearchTestCluster.configuration(ELASTIC, INDEX);
    CLIENT = ElasticSearchSupport.createRestClient(CONFIGURATION);
  }

  @BeforeAll
  static void seedIndex() throws Exception {
    CLIENT.performRequest(
        ElasticSearchTestCluster.jsonRequest(
            "PUT", "/" + INDEX, ElasticSearchTestCluster.DATASET_MAPPING));
    CLIENT.performRequest(
        ElasticSearchTestCluster.jsonRequest(
            "PUT", "/" + INDEX + "/_doc/1?refresh=true", ElasticSearchTestCluster.PUBLIC_DOCUMENT));
    CLIENT.performRequest(
        ElasticSearchTestCluster.jsonRequest(
            "PUT",
            "/" + INDEX + "/_doc/2?refresh=true",
            ElasticSearchTestCluster.PRIVATE_DOCUMENT));
  }

  /** A fresh container self-generates an active basic license — no license file is needed. */
  @Test
  @Order(1)
  void freshClusterSelfGeneratesAnActiveBasicLicense() throws Exception {
    JsonObject license =
        ElasticSearchTestCluster.json(CLIENT, new Request("GET", "/_license"))
            .getAsJsonObject("license");

    assertEquals("basic", license.get("type").getAsString());
    assertEquals("active", license.get("status").getAsString());
  }

  /** Authentication itself works on basic: an anonymous request is rejected. */
  @Test
  @Order(2)
  void unauthenticatedRequestsAreRejected() throws Exception {
    try (RestClient anonymous =
        ElasticSearchSupport.createRestClient(
            ElasticSearchTestCluster.withoutCredentials(CONFIGURATION))) {

      ResponseException thrown =
          assertThrows(
              ResponseException.class, () -> anonymous.performRequest(new Request("GET", "/")));

      assertEquals(401, thrown.getResponse().getStatusLine().getStatusCode());
    }
  }

  /** The application's own client authenticates over plain http with the shared credentials. */
  @Test
  @Order(3)
  void applicationClientAuthenticatesOverPlainHttp() throws Exception {
    assertEquals(200, ElasticSearchTestCluster.status(CLIENT, new Request("GET", "/")));
  }

  /** DLS role creation is refused under basic. The identical request succeeds under trial. */
  @Test
  @Order(4)
  void documentLevelSecurityRoleIsRejectedUnderBasicLicense() throws Exception {
    assertRejectedAsNonCompliant(
        ElasticSearchTestCluster.jsonRequest(
            "PUT",
            "/_security/role/basic-dls",
            ElasticSearchTestCluster.documentLevelSecurityRole(INDEX)));
  }

  /** FLS role creation is refused under basic. The identical request succeeds under trial. */
  @Test
  @Order(5)
  void fieldLevelSecurityRoleIsRejectedUnderBasicLicense() throws Exception {
    assertRejectedAsNonCompliant(
        ElasticSearchTestCluster.jsonRequest(
            "PUT",
            "/_security/role/basic-fls",
            ElasticSearchTestCluster.fieldLevelSecurityRole(INDEX)));
  }

  /**
   * Control: plain RBAC is unaffected by the license tier, so only the DLS/FLS grants are gated —
   * not roles, users or API keys in general.
   */
  @Test
  @Order(6)
  void readOnlyRoleWithoutDlsOrFlsIsAcceptedUnderBasicLicense() throws Exception {
    Request request =
        ElasticSearchTestCluster.jsonRequest(
            "PUT", "/_security/role/basic-read-only", ElasticSearchTestCluster.readOnlyRole(INDEX));

    assertEquals(200, ElasticSearchTestCluster.status(CLIENT, request));
  }

  /**
   * The deferred-check trap: creating the key succeeds even though the grant it carries is not
   * licensed. If a future version starts rejecting this at creation time that is an improvement,
   * and this assertion is the signal to revisit the D-2 error handling.
   */
  @Test
  @Order(7)
  void apiKeyWithDlsFlsDescriptorIsAcceptedUnderBasicLicense() throws Exception {
    assertEquals(
        200, ElasticSearchTestCluster.status(CLIENT, dlsFlsApiKeyRequest("basic-accepts")));
  }

  /**
   * The load-bearing security assertion: a DLS/FLS-restricted key that cannot be enforced must fail
   * the search closed, not fall back to returning everything.
   */
  @Test
  @Order(8)
  void searchWithDlsFlsApiKeyFailsClosedUnderBasicLicense() throws Exception {
    String apiKey =
        ElasticSearchTestCluster.json(CLIENT, dlsFlsApiKeyRequest("basic-fails-closed"))
            .get("encoded")
            .getAsString();
    Request search =
        ElasticSearchTestCluster.asApiKey(new Request("GET", "/" + INDEX + "/_search"), apiKey);

    ResponseException thrown =
        assertThrows(ResponseException.class, () -> CLIENT.performRequest(search));
    String body = ElasticSearchTestCluster.bodyOf(thrown);

    assertEquals(403, thrown.getResponse().getStatusLine().getStatusCode());
    assertTrue(
        body.contains(ElasticSearchTestCluster.LICENSE_NONCOMPLIANT_MESSAGE),
        "unexpected rejection reason: " + body);
    assertTrue(
        body.contains("indices_with_dls_or_fls"),
        "rejection should name the restricted index: " + body);
    assertFalse(body.contains("MUST-NOT-BE-RETURNED"), "document contents leaked: " + body);
  }

  /**
   * Runs last, and must: it changes the cluster's license and so would invalidate every assertion
   * above. Proves the trial is reachable from a fresh basic cluster and is one-shot, which is what
   * the harness in {@link ElasticSearchContainerTests} relies on.
   */
  @Test
  @Order(Integer.MAX_VALUE)
  void trialLicenseCanBeActivatedOnceFromBasic() throws Exception {
    JsonObject activation =
        ElasticSearchTestCluster.json(
            CLIENT, new Request("POST", "/_license/start_trial?acknowledge=true"));
    assertTrue(activation.get("trial_was_started").getAsBoolean());
    assertEquals("trial", activation.get("type").getAsString());

    JsonObject license =
        ElasticSearchTestCluster.json(CLIENT, new Request("GET", "/_license"))
            .getAsJsonObject("license");
    assertEquals("trial", license.get("type").getAsString());
    assertEquals("active", license.get("status").getAsString());

    JsonObject trialStatus =
        ElasticSearchTestCluster.json(CLIENT, new Request("GET", "/_license/trial_status"));
    assertFalse(
        trialStatus.get("eligible_to_start_trial").getAsBoolean(),
        "a second trial should not be available on the same cluster");
  }

  private static Request dlsFlsApiKeyRequest(String name) {
    return ElasticSearchTestCluster.jsonRequest(
        "POST",
        "/_security/api_key",
        """
        {"name":"%s","expiration":"5m","role_descriptors":%s}
        """
            .formatted(name, ElasticSearchTestCluster.dlsFlsRoleDescriptors(INDEX)));
  }

  private static void assertRejectedAsNonCompliant(Request request) throws Exception {
    ResponseException thrown =
        assertThrows(ResponseException.class, () -> CLIENT.performRequest(request));
    String body = ElasticSearchTestCluster.bodyOf(thrown);

    assertEquals(403, thrown.getResponse().getStatusLine().getStatusCode());
    assertTrue(
        body.contains(ElasticSearchTestCluster.LICENSE_NONCOMPLIANT_MESSAGE),
        "unexpected rejection reason: " + body);
  }
}
