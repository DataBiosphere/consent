package org.broadinstitute.consent.integration;

import com.google.gson.JsonObject;
import org.apache.http.entity.StringEntity;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
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
 * license is non-compliant for [field and document level security]}. The static initializer
 * therefore activates the 30-day trial license before any test runs. Note that {@code POST
 * /_security/api_key} accepts a DLS/FLS role descriptor even on a basic license — the rejection
 * surfaces later, on the search request.
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
      ElasticSearchTestCluster.awaitLicense(CLIENT);
      // Required before any DLS/FLS grant will be honored; see the class javadoc.
      jsonResponse(new Request("POST", "/_license/start_trial?acknowledge=true"));
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
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
