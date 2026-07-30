package org.broadinstitute.consent.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.service.ontology.ElasticSearchSupport;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared plumbing for the Elasticsearch container tests: the single place the image is pinned,
 * factories for the two modes {@code config/docker-compose.yaml} supports, and the request payloads
 * the security probes assert against.
 *
 * <p>The security probes recorded in the A-0 outcome of {@code
 * docs/plans/elasticsearch-service-duos-ui-usage.md} are automated by {@link
 * ElasticSearchSecurityBaselineTest}, {@link ElasticSearchBasicLicenseTest} and {@link
 * ElasticSearchSecurityDisabledTest}. To qualify a new Elasticsearch version, change {@link #IMAGE}
 * here and run those three classes plus {@link ElasticSearchDlsFlsEnforcementTest}: between them
 * they assert every capability Epic D depends on, in both license tiers and both security modes. No
 * other test file carries a version.
 *
 * <p>The role and role-descriptor payloads are deliberately shared between the basic-license and
 * trial-license tests, so the only difference between "rejected" and "permitted" is the license
 * tier, not the request.
 */
final class ElasticSearchTestCluster {

  /**
   * The only Elasticsearch version pin in the test tree. Keep in step with the {@code elastic}
   * service in {@code config/docker-compose.yaml}.
   */
  static final String IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:9.4.4";

  static final String USER = "elastic";
  static final String PASSWORD = "devpassword";

  /** How long {@link #awaitLicense} waits for the self-generated license to become readable. */
  private static final long LICENSE_TIMEOUT_MILLIS = 60_000;

  private static final long LICENSE_POLL_INTERVAL_MILLIS = 250;

  /**
   * The error Elasticsearch returns when a document- or field-level security grant is exercised
   * under a basic license. Asserted on directly: a future version that changed this to a silent
   * degradation rather than a hard failure would be a security regression for Epic D.
   */
  static final String LICENSE_NONCOMPLIANT_MESSAGE =
      "current license is non-compliant for [field and document level security]";

  private ElasticSearchTestCluster() {}

  /**
   * A single-node container configured exactly like the {@code elastic} service in {@code
   * config/docker-compose.yaml}: plain {@code http}, no transport SSL, {@code ELASTIC_PASSWORD}
   * bootstrapped in both modes (Elasticsearch ignores it when security is off).
   *
   * <p>The HTTP layer must stay on plain {@code http} because {@code
   * ElasticSearchSupport.createRestClient} has no {@code SSLContext} hook and so cannot trust the
   * self-signed CA the image generates. See {@link ElasticSearchContainerTests} for the full
   * explanation.
   *
   * @param securityEnabled the value for {@code xpack.security.enabled}
   */
  @SuppressWarnings("resource")
  static ElasticsearchContainer container(boolean securityEnabled) {
    return new ElasticsearchContainer(DockerImageName.parse(IMAGE))
        .withEnv("xpack.security.enabled", Boolean.toString(securityEnabled))
        .withEnv("xpack.security.http.ssl.enabled", "false")
        .withEnv("xpack.security.transport.ssl.enabled", "false")
        .withPassword(PASSWORD);
  }

  /**
   * Blocks until the cluster will answer {@code GET /_license}, which happens strictly later than
   * the container reporting itself started.
   *
   * <p>Testcontainers considers an {@code ElasticsearchContainer} ready as soon as the HTTP layer
   * answers on port 9200, but the self-generated basic license is published to the cluster state
   * after that point. In between, {@code GET /_license} returns {@code 404} with an empty body —
   * documented as a transient of a master node still building cluster state, not a
   * misconfiguration. The gap is on the order of a second on an idle machine and widens under the
   * load of a full test run, where three of these containers boot alongside the Postgres one.
   *
   * <p>Any class that reads or changes the license immediately after {@code start()} must call this
   * first, or its first request can fail on that 404.
   */
  static void awaitLicense(RestClient client) throws IOException {
    long deadline = System.nanoTime() + LICENSE_TIMEOUT_MILLIS * 1_000_000L;
    ResponseException lastRejection;
    while (true) {
      try {
        execute(client, new Request("GET", "/_license"));
        return;
      } catch (ResponseException e) {
        if (e.getResponse().getStatusLine().getStatusCode() != 404) {
          throw e;
        }
        lastRejection = e;
      }
      if (System.nanoTime() - deadline >= 0) {
        throw new IOException(
            "no license after "
                + LICENSE_TIMEOUT_MILLIS
                + "ms; last response: "
                + bodyOf(lastRejection),
            lastRejection);
      }
      try {
        // Polling a cluster that has no readiness signal for this; see the javadoc above.
        Thread.sleep(LICENSE_POLL_INTERVAL_MILLIS); // NOSONAR
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("interrupted while waiting for the license", e);
      }
    }
  }

  /** Configuration pointing at a running container, carrying the shared dev credentials. */
  static ElasticSearchConfiguration configuration(
      ElasticsearchContainer container, String datasetIndexName) {
    ElasticSearchConfiguration configuration = new ElasticSearchConfiguration();
    configuration.setServers(List.of(container.getHost()));
    configuration.setPort(container.getMappedPort(9200));
    configuration.setProtocol("http");
    configuration.setAuthUser(USER);
    configuration.setAuthPassword(PASSWORD);
    configuration.setIndexName("ontology-test");
    configuration.setDatasetIndexName(datasetIndexName);
    return configuration;
  }

  /** Copy of {@code source} with no credentials, for probing unauthenticated access. */
  static ElasticSearchConfiguration withoutCredentials(ElasticSearchConfiguration source) {
    ElasticSearchConfiguration copy = copyOf(source);
    copy.setAuthUser(null);
    copy.setAuthPassword(null);
    return copy;
  }

  /** Copy of {@code source} that speaks {@code https} to the same host and port. */
  static ElasticSearchConfiguration overTls(ElasticSearchConfiguration source) {
    ElasticSearchConfiguration copy = copyOf(source);
    copy.setProtocol("https");
    return copy;
  }

  private static ElasticSearchConfiguration copyOf(ElasticSearchConfiguration source) {
    ElasticSearchConfiguration copy = new ElasticSearchConfiguration();
    copy.setServers(source.getServers());
    copy.setPort(source.getPort());
    copy.setProtocol(source.getProtocol());
    copy.setAuthUser(source.getAuthUser());
    copy.setAuthPassword(source.getAuthPassword());
    copy.setIndexName(source.getIndexName());
    copy.setDatasetIndexName(source.getDatasetIndexName());
    return copy;
  }

  /** A role granting read on {@code index} restricted by a DLS query. Platinum-only. */
  static String documentLevelSecurityRole(String index) {
    return """
        {"indices":[{"names":["%s"],"privileges":["read"],
          "query":"{\\"term\\":{\\"accessPolicy.publicVisibility\\":true}}"}]}
        """
        .formatted(index);
  }

  /** A role granting read on {@code index} restricted to named fields. Platinum-only. */
  static String fieldLevelSecurityRole(String index) {
    return """
        {"indices":[{"names":["%s"],"privileges":["read"],
          "field_security":{"grant":["datasetId","datasetName"]}}]}
        """
        .formatted(index);
  }

  /** A plain read role with neither DLS nor FLS — the control, valid on any license tier. */
  static String readOnlyRole(String index) {
    return """
        {"indices":[{"names":["%s"],"privileges":["read"]}]}
        """
        .formatted(index);
  }

  /** A role carrying a {@code run_as} privilege, part of the A-1 capability record. */
  static String runAsRole(String index) {
    return """
        {"run_as":["some-other-user"],"indices":[{"names":["%s"],"privileges":["read"]}]}
        """
        .formatted(index);
  }

  /** Inline API-key role descriptors combining DLS and FLS, as Ticket D-2 would issue them. */
  static String dlsFlsRoleDescriptors(String index) {
    return """
        {"restricted":{"indices":[{"names":["%s"],"privileges":["read"],
          "query":"{\\"term\\":{\\"accessPolicy.publicVisibility\\":true}}",
          "field_security":{"grant":["datasetId","datasetName"]}}]}}
        """
        .formatted(index);
  }

  /** Mapping and documents used by every probe that needs something to search. */
  static final String DATASET_MAPPING =
      """
      {"mappings":{"properties":{
        "datasetId":{"type":"keyword"},
        "datasetName":{"type":"keyword"},
        "secretField":{"type":"keyword"},
        "accessPolicy":{"properties":{"publicVisibility":{"type":"boolean"}}}}}}
      """;

  static final String PUBLIC_DOCUMENT =
      """
      {"datasetId":"DS-1","datasetName":"public dataset",
       "secretField":"MUST-NOT-BE-RETURNED","accessPolicy":{"publicVisibility":true}}
      """;

  static final String PRIVATE_DOCUMENT =
      """
      {"datasetId":"DS-2","datasetName":"private dataset",
       "secretField":"MUST-NOT-BE-RETURNED","accessPolicy":{"publicVisibility":false}}
      """;

  static StringEntity jsonEntity(String json) {
    return new StringEntity(json, ContentType.APPLICATION_JSON);
  }

  /** A request carrying a JSON body. */
  static Request jsonRequest(String method, String path, String body) {
    Request request = new Request(method, path);
    request.setEntity(jsonEntity(body));
    return request;
  }

  /** A request authenticated with an encoded API key rather than the client's own credentials. */
  static Request asApiKey(Request request, String encodedApiKey) {
    RequestOptions.Builder options = RequestOptions.DEFAULT.toBuilder();
    options.addHeader("Authorization", "ApiKey " + encodedApiKey);
    request.setOptions(options.build());
    return request;
  }

  static JsonObject json(RestClient client, Request request) throws Exception {
    return parse(client.performRequest(request));
  }

  static int status(RestClient client, Request request) throws Exception {
    Response response = client.performRequest(request);
    // The entity is unused here, but it still has to be consumed — see parse(Response).
    EntityUtils.consumeQuietly(response.getEntity());
    return response.getStatusLine().getStatusCode();
  }

  /**
   * Reads a response body as JSON.
   *
   * <p>{@link EntityUtils} rather than a raw {@code getEntity().getContent()} read: the client
   * {@code ElasticSearchSupport} builds is backed by Apache HttpClient with a pooled connection
   * manager, and a response stream that is read but never closed holds its connection out of the
   * pool. Tests that issue many requests would then block waiting for a lease and fail
   * intermittently on timeout. Every {@code EntityUtils} call below closes the stream, releasing
   * the connection.
   */
  static JsonObject parse(Response response) throws IOException {
    String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
    return JsonParser.parseString(body).getAsJsonObject();
  }

  /** Executes a request whose response body is not needed, consuming the entity. */
  static void execute(RestClient client, Request request) throws IOException {
    EntityUtils.consumeQuietly(client.performRequest(request).getEntity());
  }

  /**
   * Drops {@code index} if it exists and recreates it with the supplied mapping body.
   *
   * <p>The drop is what makes seeding idempotent. A cluster outlives a single test class — it is
   * shared per JVM here and left running by an IDE between re-runs — so a plain create would fail
   * with {@code resource_already_exists_exception} the second time. Recreating also guarantees the
   * index is empty, rather than carrying documents another class indexed.
   */
  static void recreateIndex(RestClient client, String index, String mappingJson)
      throws IOException {
    try {
      execute(client, new Request("DELETE", "/" + index));
    } catch (ResponseException e) {
      if (e.getResponse().getStatusLine().getStatusCode() != 404) {
        throw e;
      }
    }
    execute(client, jsonRequest("PUT", "/" + index, mappingJson));
  }

  /** Indexes a document and refreshes so it is immediately searchable. */
  static void indexDocument(RestClient client, String index, String id, String documentJson)
      throws IOException {
    execute(
        client, jsonRequest("PUT", "/%s/_doc/%s?refresh=true".formatted(index, id), documentJson));
  }

  /** The response body of a failed request, for asserting on Elasticsearch's error text. */
  static String bodyOf(ResponseException exception) throws IOException {
    return EntityUtils.toString(exception.getResponse().getEntity(), StandardCharsets.UTF_8);
  }

  /** The version tag of {@link #IMAGE}, which the cluster is expected to report back. */
  static String pinnedVersion() {
    return IMAGE.substring(IMAGE.lastIndexOf(':') + 1);
  }

  /**
   * Asserts that the cluster serves plain {@code http} on its port and no TLS, which is what {@code
   * config/docker-compose.yaml} configures and what {@code ElasticSearchSupport.createRestClient}
   * requires — it builds its client with no {@code SSLContext} hook and so cannot trust the
   * self-signed CA the image would otherwise generate.
   *
   * <p>The plain-http leg runs first deliberately: without it a dead or misidentified port would
   * also produce a failed TLS request, and the probe would pass for the wrong reason.
   */
  static void assertTlsIsNotServed(ElasticSearchConfiguration configuration) throws Exception {
    try (RestClient plainClient = ElasticSearchSupport.createRestClient(configuration)) {
      assertEquals(
          200,
          status(plainClient, new Request("GET", "/")),
          "port is not serving plain http, so the TLS assertion below would be meaningless");
    }
    try (RestClient tlsClient = ElasticSearchSupport.createRestClient(overTls(configuration))) {
      IOException thrown =
          assertThrows(IOException.class, () -> tlsClient.performRequest(new Request("GET", "/")));
      assertFalse(
          thrown instanceof ResponseException,
          "expected no HTTP response over TLS, but the port answered: " + thrown);
    }
  }
}
