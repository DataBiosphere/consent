package org.broadinstitute.consent.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.google.gson.JsonObject;
import org.apache.http.entity.StringEntity;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.models.elastic_search.ElasticSearchLicenseActivation;
import org.broadinstitute.consent.http.models.elastic_search.LicenseActivationOutcome;
import org.broadinstitute.consent.http.service.ontology.ElasticSearchSupport;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Tag;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * Base class for tests that need a security-enabled Elasticsearch cluster with document-level and
 * field-level security (DLS/FLS) available. This is the harness for the native DLS/FLS work; see
 * Epic D in {@code docs/plans/elasticsearch-service-duos-ui-usage.md}.
 *
 * <p>The container is started in a static initializer, so it is running before any subclass test
 * executes, and Testcontainers registers a JVM shutdown hook via Ryuk — no explicit teardown is
 * required. The cluster is shared by every subclass in the same JVM, so tests must not assume an
 * empty cluster; use {@link #recreateIndex(String, String)} to get a known-empty index.
 *
 * <p>Two container defaults are deliberately overridden:
 *
 * <ul>
 *   <li>For image versions 8.0.0 and above, {@code ElasticsearchContainer} automatically applies
 *       {@code withPassword("changeme")} and {@code withCertPath(...)}, which serves the HTTP layer
 *       over TLS with a self-signed CA. {@link ElasticSearchSupport#createRestClient} builds its
 *       client without an {@code SSLContext} hook and so cannot trust that CA, so the HTTP layer is
 *       forced back to plain {@code http} here, matching {@code config/docker-compose.yaml}. Tests
 *       that need TLS must build their own client using the container's {@code caCertAsBytes()} or
 *       {@code createSslContextFromCa()} rather than the application's client.
 *   <li>Transport SSL is disabled, which is correct for a single-node cluster.
 * </ul>
 *
 * <p>DLS/FLS is a Platinum feature and every fresh container self-generates a <em>basic</em>
 * license, under which a DLS query or FLS field grant is rejected with HTTP 403 {@code current
 * license is non-compliant for [field and document level security]}. Note that {@code POST
 * /_security/api_key} accepts a DLS/FLS role descriptor even on a basic license — the rejection
 * surfaces later, on the search request.
 *
 * <p><b>This class does not activate the trial license.</b> A subclass that needs one calls {@link
 * #activateTrialLicense()} in a {@code @BeforeAll}, which goes through the admin endpoint the same
 * way an operator would in a deployed environment. Activating it here instead would put an
 * irreversible, once-per-major-version-per-cluster license change inside the setup of every
 * subclass, including the ones whose subject is what a basic license does — and would leave the
 * license tier of the cluster under test decided by a static initializer nobody reading the test
 * can see.
 *
 * <p>The {@code elasticsearch} tag is inherited by every subclass and keeps these tests out of the
 * automatic CI run; see {@code excludedTestGroups} in {@code pom.xml}.
 */
@Tag("elasticsearch")
public abstract class ElasticSearchContainerTests {

  /**
   * Security-enabled, single-node Elasticsearch container, shared per JVM. Started before any
   * subclass test runs and reaped by Ryuk at JVM exit. The image is pinned in {@link
   * ElasticSearchTestCluster#IMAGE}.
   */
  private static final ElasticsearchContainer ELASTIC = ElasticSearchTestCluster.container(true);

  private static final ElasticSearchConfiguration CONFIGURATION;

  /**
   * Client built through the application's own factory, so tests exercise the same authentication
   * path the service uses at runtime rather than a test-only client.
   */
  private static final RestClient CLIENT;

  static {
    ELASTIC.start();

    CONFIGURATION = ElasticSearchTestCluster.configuration(ELASTIC, "dataset-test");
    CLIENT = ElasticSearchSupport.createRestClient(CONFIGURATION);

    try {
      // The basic license is published after the container reports ready; see the helper's javadoc.
      // Waited on here rather than per subclass because activateTrialLicense() reads the license
      // before deciding anything, and would read the 404 this closes.
      ElasticSearchTestCluster.awaitLicense(CLIENT);
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * Puts the shared cluster on the trial license, which is what makes a DLS or FLS grant permitted,
   * by invoking the admin endpoint {@code POST /api/elasticSearch/license/trial}.
   *
   * <p>An explicit step, called from the {@code @BeforeAll} of each class that needs it. The
   * cluster is shared per JVM, so the first class to run activates the trial and the rest find it
   * already active — hence the assertion is on the license the cluster ends up with rather than on
   * which of those two happened.
   *
   * @return the endpoint's response, for a test that wants to assert more
   */
  protected static ElasticSearchLicenseActivation activateTrialLicense() {
    ElasticSearchLicenseActivation activation =
        ElasticSearchAdminEndpoints.activateTrialLicense(CLIENT, CONFIGURATION);
    assertNotEquals(
        LicenseActivationOutcome.TRIAL_UNAVAILABLE,
        activation.outcome(),
        "this cluster's trial is spent, so DLS/FLS cannot be exercised on it: " + activation);
    assertEquals(
        Boolean.TRUE,
        activation.licenseAfter().dlsFlsLicensed(),
        "the cluster is not licensed for DLS/FLS after activation: " + activation);
    return activation;
  }

  /**
   * Configuration pointing at the running container, suitable for constructing services under test.
   * The returned instance is shared — treat it as read-only.
   */
  protected static ElasticSearchConfiguration elasticSearchConfiguration() {
    return CONFIGURATION;
  }

  /** Never close this client; it is shared for the lifetime of the JVM. */
  protected static RestClient restClient() {
    return CLIENT;
  }

  protected static String datasetIndex() {
    return CONFIGURATION.getDatasetIndexName();
  }

  /** Executes a request as the privileged {@code elastic} user and parses the JSON body. */
  protected static JsonObject jsonResponse(Request request) throws Exception {
    return ElasticSearchTestCluster.json(CLIENT, request);
  }

  protected static int statusOf(Request request) throws Exception {
    return ElasticSearchTestCluster.status(CLIENT, request);
  }

  protected static StringEntity jsonEntity(String json) {
    return ElasticSearchTestCluster.jsonEntity(json);
  }

  /**
   * Drops the index if present and recreates it with the supplied mapping body, giving a test a
   * known-empty index in a cluster shared with other test classes.
   */
  protected static void recreateIndex(String index, String mappingJson) throws Exception {
    ElasticSearchTestCluster.recreateIndex(CLIENT, index, mappingJson);
  }

  /** Indexes a document and refreshes so it is immediately searchable. */
  protected static void indexDocument(String index, String id, String documentJson)
      throws Exception {
    ElasticSearchTestCluster.indexDocument(CLIENT, index, id, documentJson);
  }

  /**
   * Creates a short-lived API key carrying the supplied inline role descriptors and returns its
   * encoded form, ready for an {@code Authorization: ApiKey} header. This mirrors the per-request
   * credential construction described in Ticket D-2.
   *
   * @param roleDescriptorsJson the value of the {@code role_descriptors} field
   */
  protected static String createApiKey(String name, String roleDescriptorsJson) throws Exception {
    Request request = new Request("POST", "/_security/api_key");
    request.setEntity(
        jsonEntity(
            """
            {"name":"%s","expiration":"5m","role_descriptors":%s}
            """
                .formatted(name, roleDescriptorsJson)));
    return jsonResponse(request).get("encoded").getAsString();
  }

  /**
   * Runs a match-all search against the index using the given API key rather than the privileged
   * client credentials, so any DLS/FLS restrictions attached to the key are applied.
   */
  protected static JsonObject searchAsApiKey(String index, String encodedApiKey) throws Exception {
    Request request =
        ElasticSearchTestCluster.asApiKey(
            new Request("GET", "/" + index + "/_search"), encodedApiKey);
    return jsonResponse(request);
  }
}
