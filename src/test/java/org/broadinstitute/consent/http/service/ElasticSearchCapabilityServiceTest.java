package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.message.BasicStatusLine;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.models.elastic_search.CapabilityVerdict;
import org.broadinstitute.consent.http.models.elastic_search.ElasticSearchCapability;
import org.broadinstitute.consent.http.models.elastic_search.ElasticSearchCapabilityReport;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ElasticSearchCapabilityServiceTest {

  private static final String ROOT = "/";
  private static final String XPACK = "/_xpack";
  private static final String LICENSE = "/_license";
  private static final String SETTINGS =
      "/_cluster/settings?include_defaults=true&flat_settings=true";
  private static final String AUTHENTICATE = "/_security/_authenticate";
  private static final String RUN_AS = "/_security/_authenticate#runas";
  // A POST, so it is keyed with its method like the other write-shaped calls.
  private static final String HAS_PRIVILEGES = "POST /_security/user/_has_privileges";

  // Write-probe endpoints. The three key creations share a method and endpoint, so they are keyed
  // by the role descriptor in the body — which is also what distinguishes them on a real cluster.
  private static final String CREATE_KEY = "POST /_security/api_key";
  private static final String CREATE_DLS_KEY = "POST /_security/api_key#dls";
  private static final String CREATE_FLS_KEY = "POST /_security/api_key#fls";
  private static final String INVALIDATE_KEY = "DELETE /_security/api_key";
  private static final String CREATE_ROLE = "PUT /_security/role";
  private static final String DELETE_ROLE = "DELETE /_security/role";
  private static final String COUNT = "/dataset/_count";
  private static final String SEARCH = "/dataset/_search?size=1";

  private static final String ROOT_BODY =
      """
      {"cluster_name":"duos-cluster","version":{"number":"9.3.3","build_flavor":"default"}}""";

  @Mock private RestClient esClient;

  private ElasticSearchConfiguration config;

  /** Canned responses keyed by endpoint; the run_as probe is keyed separately by header. */
  private Map<String, StubResponse> stubs;

  /** Every request the service issued, so teardown can be asserted on rather than assumed. */
  private List<Request> requests;

  private RestClient keyClient;
  private String activeApiKey;

  @BeforeEach
  void setUp() {
    config = new ElasticSearchConfiguration();
    config.setDatasetIndexName("dataset");
    config.setIndexName("ontology");
    stubs = new HashMap<>();
    requests = new java.util.ArrayList<>();
  }

  private ElasticSearchCapabilityService service() throws IOException {
    stubClient(esClient, this::keyFor);
    return new ElasticSearchCapabilityService(esClient, config, this::apiKeyClient);
  }

  /**
   * Answers from {@link #stubs}, turning a stubbed non-2xx into the ResponseException ES throws.
   */
  private void stubClient(RestClient client, java.util.function.Function<Request, String> keying)
      throws IOException {
    when(client.performRequest(any(Request.class)))
        .thenAnswer(
            invocation -> {
              Request request = invocation.getArgument(0);
              requests.add(request);
              String key = keying.apply(request);
              StubResponse stub = stubs.get(key);
              if (stub == null) {
                throw new IOException("no stub for " + key);
              }
              Response response = response(stub.status(), stub.body());
              if (stub.status() >= 300) {
                throw new ResponseException(response);
              }
              return response;
            });
  }

  /**
   * A client that authenticates as an API key. One mock serves all of them, routing on which key
   * was most recently handed to the factory — safe because the probes run sequentially, and it
   * keeps the DLS key's search distinguishable from the FLS key's.
   */
  private RestClient apiKeyClient(String encodedApiKey) {
    activeApiKey = encodedApiKey;
    if (keyClient != null) {
      return keyClient;
    }
    keyClient = mock(RestClient.class);
    try {
      stubClient(keyClient, request -> activeApiKey + "|" + request.getEndpoint());
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    return keyClient;
  }

  private String keyFor(Request request) {
    boolean isRunAs =
        request.getOptions().getHeaders().stream()
            .anyMatch(h -> h.getName().equals("es-security-runas-user"));
    String endpoint = request.getEndpoint();
    if (isRunAs && endpoint.equals(AUTHENTICATE)) {
      return RUN_AS;
    }
    String method = request.getMethod();
    if (endpoint.equals("/_security/api_key") && method.equals("POST")) {
      String body = bodyOf(request);
      if (body.contains("dls_probe")) {
        return CREATE_DLS_KEY;
      }
      return body.contains("fls_probe") ? CREATE_FLS_KEY : CREATE_KEY;
    }
    // Probe role names carry a timestamp, so match the family rather than the exact name.
    if (endpoint.startsWith("/_security/role/")) {
      return method + " /_security/role";
    }
    return method.equals("GET") ? endpoint : method + " " + endpoint;
  }

  private String bodyOf(Request request) {
    try {
      return request.getEntity() == null
          ? ""
          : new String(
              request.getEntity().getContent().readAllBytes(),
              java.nio.charset.StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private List<Request> requestsTo(String method, String endpointPrefix) {
    return requests.stream()
        .filter(r -> r.getMethod().equals(method) && r.getEndpoint().startsWith(endpointPrefix))
        .toList();
  }

  /**
   * Only the error path builds a {@link ResponseException}, which is what reads the request line;
   * the 2xx path reads just the status and entity. Stubbing the request line only for non-2xx
   * responses keeps every stub read by the test it's created for, so no stub needs to be lenient.
   */
  private Response response(int status, String body) {
    Response response = mock(Response.class);
    when(response.getStatusLine())
        .thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, status, "reason"));
    when(response.getEntity()).thenReturn(new StringEntity(body, ContentType.APPLICATION_JSON));
    if (status >= 300) {
      when(response.getRequestLine())
          .thenReturn(new BasicRequestLine("GET", "/", HttpVersion.HTTP_1_1));
    }
    return response;
  }

  private void stub(String key, int status, String body) {
    stubs.put(key, new StubResponse(status, body));
  }

  /**
   * Baseline: a cluster with security switched off, as the local docker-compose one is. X-Pack is
   * still installed and still reports a license — it has shipped in every default distribution
   * since 6.3 — so this fixture matches what the local cluster was actually measured to return
   * rather than the OSS shape.
   */
  private void stubSecurityDisabledCluster() {
    stub(ROOT, 200, ROOT_BODY);
    stub(
        XPACK,
        200,
        """
        {"features":{"security":{"available":true,"enabled":false}}}""");
    stub(
        LICENSE,
        200,
        """
        {"license":{"status":"active","type":"basic"}}""");
    stub(
        SETTINGS,
        200,
        """
        {"defaults":{"xpack.security.enabled":"false"},"persistent":{},"transient":{}}""");
    stub(
        AUTHENTICATE,
        400,
        """
        {"error":{"reason":"Security must be explicitly enabled"}}""");
  }

  /** A security-enabled cluster; the license tier is what varies between DLS/FLS outcomes. */
  private void stubSecurityEnabledCluster(String licenseType) {
    stub(ROOT, 200, ROOT_BODY);
    stub(
        XPACK,
        200,
        """
        {"features":{"security":{"available":true,"enabled":true}}}""");
    stub(
        LICENSE,
        200,
        """
        {"license":{"status":"active","type":"%s"}}"""
            .formatted(licenseType));
    stub(
        SETTINGS,
        200,
        """
        {"defaults":{"xpack.security.enabled":"true","xpack.security.dls_fls.enabled":"true",
        "xpack.security.authc.api_key.enabled":"true",
        "xpack.security.audit.logfile.events.emit_request_body":"false",
        "xpack.security.crypto.thread_pool.size":"4",
        "xpack.security.authc.api_key.cache.ttl":"24h"},
        "persistent":{"xpack.security.authc.password_hashing.algorithm":"PBKDF2"},
        "transient":{}}""");
    stub(
        AUTHENTICATE,
        200,
        """
        {"username":"consent","roles":["superuser"]}""");
    stub(
        HAS_PRIVILEGES,
        200,
        """
        {"cluster":{"manage_security":true,"manage_api_key":true,"grant_api_key":true,
        "manage_own_api_key":true,"read_security":true,"monitor":true}}""");
    stub(
        RUN_AS,
        200,
        """
        {"username":"consent","roles":["superuser"]}""");
  }

  private ElasticSearchCapability capability(ElasticSearchCapabilityReport report, String name) {
    return report.capabilities().stream()
        .filter(c -> c.name().equals(name))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no capability named " + name));
  }

  @Test
  void testSecurityDisabledClusterReportsEverythingUnavailable() throws IOException {
    stubSecurityDisabledCluster();

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    assertEquals("9.3.3", report.version());
    assertEquals("elasticsearch", report.distribution());
    assertEquals("duos-cluster", report.clusterName());
    assertEquals(Boolean.FALSE, report.securityEnabled());
    // X-Pack is installed and licensed here; only security is switched off. Calling that "OSS"
    // would hide the fact that this cluster could have security enabled by configuration alone.
    assertEquals("basic", report.edition());
    for (ElasticSearchCapability capability : report.capabilities()) {
      assertEquals(
          CapabilityVerdict.UNAVAILABLE,
          capability.verdict(),
          capability.name() + " should be unavailable when security is off");
    }
    assertTrue(report.recommendation().contains("Epic E"));
    // No privilege probe should be attempted when the security API is absent.
    assertTrue(report.clusterPrivileges().isEmpty());
  }

  @Test
  void testBasicLicenseBlocksDlsFlsButAllowsApiKeysAndRunAs() throws IOException {
    stubSecurityEnabledCluster("basic");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    assertEquals(Boolean.TRUE, report.securityEnabled());
    assertEquals("basic", report.licenseType());
    assertEquals("active", report.licenseStatus());
    assertEquals(
        CapabilityVerdict.LICENSE_BLOCKED,
        capability(report, "Document-level security (DLS)").verdict());
    assertEquals(
        CapabilityVerdict.LICENSE_BLOCKED,
        capability(report, "Field-level security (FLS)").verdict());
    assertEquals(CapabilityVerdict.INFERRED_SUPPORTED, capability(report, "API keys").verdict());
    assertEquals(CapabilityVerdict.SUPPORTED, capability(report, "run_as impersonation").verdict());
    assertTrue(report.recommendation().contains("Epic E"));
  }

  @Test
  void testTrialLicenseMakesNativePathViable() throws IOException {
    stubSecurityEnabledCluster("trial");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    assertEquals(
        CapabilityVerdict.INFERRED_SUPPORTED,
        capability(report, "Document-level security (DLS)").verdict());
    assertEquals(
        CapabilityVerdict.INFERRED_SUPPORTED,
        capability(report, "Field-level security (FLS)").verdict());
    assertEquals(CapabilityVerdict.SUPPORTED, capability(report, "X-Pack Security").verdict());
    assertTrue(report.recommendation().contains("Epic D"));
  }

  @Test
  void testSecuritySettingsAreFilteredToTheInformativeOnes() throws IOException {
    stubSecurityEnabledCluster("trial");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    assertTrue(report.securitySettings().containsKey("xpack.security.enabled"));
    assertTrue(report.securitySettings().containsKey("xpack.security.dls_fls.enabled"));
    // Audit-logfile, cache, and tuning defaults are noise for a capability question. A real
    // cluster reports around fifty of these, which would bury the few that gate a capability.
    assertFalse(
        report
            .securitySettings()
            .containsKey("xpack.security.audit.logfile.events.emit_request_body"));
    assertFalse(report.securitySettings().containsKey("xpack.security.authc.api_key.cache.ttl"));
    assertFalse(report.securitySettings().containsKey("xpack.security.crypto.thread_pool.size"));
    // But an operator having explicitly overridden a setting is itself informative, so a
    // persistent value is reported even though the same key as a default would be filtered out.
    assertEquals(
        "PBKDF2",
        report.securitySettings().get("xpack.security.authc.password_hashing.algorithm"),
        "explicitly configured security settings should always be reported");
  }

  @Test
  void testCredentialWithoutKeyMintingPrivilegesIsReportedAsNotPermitted() throws IOException {
    stubSecurityEnabledCluster("trial");
    stub(
        HAS_PRIVILEGES,
        200,
        """
        {"cluster":{"manage_security":false,"manage_api_key":false,"grant_api_key":false,
        "manage_own_api_key":true,"read_security":false,"monitor":true}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    ElasticSearchCapability apiKeys = capability(report, "API keys");
    assertEquals(CapabilityVerdict.NOT_PERMITTED, apiKeys.verdict());
    assertTrue(apiKeys.detail().contains("manage_api_key"));
    assertEquals(Boolean.FALSE, report.clusterPrivileges().get("grant_api_key"));
  }

  /**
   * A privileges body that failed to interpolate would be rejected by the cluster, and a rejected
   * probe degrades quietly to "no privileges reported" rather than to a visible error — so the body
   * itself is asserted on, not just the verdict it produces.
   */
  @Test
  void testPrivilegeProbeBodyIsFullyInterpolated() throws IOException {
    stubSecurityEnabledCluster("trial");
    stub(
        HAS_PRIVILEGES,
        200,
        """
        {"cluster":{"manage_api_key":true}}""");

    service().getCapabilityReport(null, false);

    List<Request> probes = requestsTo("POST", "/_security/user/_has_privileges");
    assertEquals(1, probes.size());
    String body = bodyOf(probes.getFirst());
    assertFalse(body.contains("%s"), "every placeholder should have been substituted: " + body);
    assertTrue(body.contains("\"manage_api_key\""), body);
    assertTrue(body.contains("\"names\":[\"dataset\"]"), body);
  }

  @Test
  void testRunAsDeniedByPrivilegeIsDistinguishedFromLicenseBlock() throws IOException {
    stubSecurityEnabledCluster("trial");
    stub(
        RUN_AS,
        403,
        """
        {"error":{"reason":"action [cluster:admin/xpack/security/user/authenticate] is unauthorized
         for user [consent] because user is unauthorized to run as [other]"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport("other", false);

    assertEquals(
        CapabilityVerdict.NOT_PERMITTED, capability(report, "run_as impersonation").verdict());
  }

  /**
   * The run_as target is caller-supplied and ends up in a request header on the deployment's own
   * credential. The transport does not validate header values, so a CRLF would let a caller append
   * requests of their own to the one this service sends.
   */
  @Test
  void testRunAsUserCarryingHeaderTerminatorsIsRejectedBeforeAnyRequestIsSent() throws IOException {
    stubSecurityEnabledCluster("trial");

    ElasticSearchCapabilityReport report =
        service().getCapabilityReport("victim\r\nDELETE /dataset HTTP/1.1", false);

    ElasticSearchCapability runAs = capability(report, "run_as impersonation");
    assertEquals(CapabilityVerdict.UNKNOWN, runAs.verdict());
    assertTrue(runAs.detail().contains("not a valid Elasticsearch username"), runAs.detail());
    assertTrue(
        requests.stream()
            .noneMatch(
                r ->
                    r.getOptions().getHeaders().stream()
                        .anyMatch(h -> h.getName().equals("es-security-runas-user"))),
        "no run_as request should have been issued for an invalid username");
  }

  @Test
  void testRunAsBlockedByLicenseIsReportedAsSuch() throws IOException {
    stubSecurityEnabledCluster("basic");
    stub(
        RUN_AS,
        403,
        """
        {"error":{"reason":"current license is non-compliant for [run_as]"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport("other", false);

    assertEquals(
        CapabilityVerdict.LICENSE_BLOCKED, capability(report, "run_as impersonation").verdict());
  }

  @Test
  void testUnreachableClusterProducesAReportRatherThanAnException() throws IOException {
    when(esClient.performRequest(any(Request.class)))
        .thenThrow(new IOException("connection refused"));

    ElasticSearchCapabilityReport report =
        new ElasticSearchCapabilityService(esClient, config, this::apiKeyClient)
            .getCapabilityReport(null, false);

    assertNotNull(report);
    assertEquals("unknown", report.edition());
    assertEquals(1, report.capabilities().size());
    assertEquals(CapabilityVerdict.UNKNOWN, report.capabilities().get(0).verdict());
    // The published schema enumerates the capability names, so this one entry — the only shape
    // the report takes other than the five probed capabilities — has to stay in that enum.
    assertEquals("Cluster reachability", report.capabilities().get(0).name());
    assertTrue(report.recommendation().contains("unreachable"));
  }

  @Test
  void testEveryCapabilityCarriesEvidence() throws IOException {
    stubSecurityEnabledCluster("trial");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    List<ElasticSearchCapability> capabilities = report.capabilities();
    assertEquals(5, capabilities.size());
    for (ElasticSearchCapability capability : capabilities) {
      assertNotNull(capability.evidence(), capability.name() + " must cite its probe");
      assertFalse(capability.evidence().isBlank());
      assertNotNull(capability.detail());
    }
  }

  // ---------------------------------------------------------------------------
  // Write probes
  // ---------------------------------------------------------------------------

  /** Stubs a cluster on which every write probe succeeds and DLS/FLS are genuinely enforced. */
  private void stubWorkingWriteProbes() {
    stub(
        CREATE_KEY,
        200,
        """
        {"id":"plain-key-id","api_key":"plain-secret","encoded":"cGxhaW4="}""");
    stub(
        "cGxhaW4=|" + AUTHENTICATE,
        200,
        """
        {"username":"consent"}""");
    stub(
        CREATE_ROLE,
        200,
        """
        {"role":{"created":true}}""");
    stub(
        COUNT,
        200,
        """
        {"count":2}""");
    stub(
        CREATE_DLS_KEY,
        200,
        """
        {"id":"dls-key-id","api_key":"dls-secret","encoded":"ZGxz"}""");
    // A match_none DLS key must see none of the two documents.
    stub(
        "ZGxz|" + SEARCH,
        200,
        """
        {"hits":{"total":{"value":0},"hits":[]}}""");
    stub(
        CREATE_FLS_KEY,
        200,
        """
        {"id":"fls-key-id","api_key":"fls-secret","encoded":"Zmxz"}""");
    // A key granting only datasetIdentifier must return only that field.
    stub(
        "Zmxz|" + SEARCH,
        200,
        """
        {"hits":{"total":{"value":2},"hits":[{"_source":{"datasetIdentifier":"DUOS-000001"}}]}}""");
    stub(
        INVALIDATE_KEY,
        200,
        """
        {"invalidated_api_keys":["x"]}""");
    stub(
        DELETE_ROLE,
        200,
        """
        {"found":true}""");
  }

  @Test
  void testWriteProbesObserveApiKeysDlsAndFlsRatherThanInferringThem() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    assertEquals(Boolean.TRUE, report.writeProbesRun());
    assertEquals(CapabilityVerdict.SUPPORTED, capability(report, "API keys").verdict());
    assertEquals(
        CapabilityVerdict.SUPPORTED, capability(report, "Document-level security (DLS)").verdict());
    assertEquals(
        CapabilityVerdict.SUPPORTED, capability(report, "Field-level security (FLS)").verdict());
    // The point of the exercise: proven, not inferred.
    assertTrue(
        capability(report, "Document-level security (DLS)").detail().contains("Proven end to end"),
        "DLS should report end-to-end proof, not license inference");
    assertTrue(capability(report, "Document-level security (DLS)").detail().contains("0 of 2"));
    // The evidence line has to stay a request an operator can paste into curl.
    assertTrue(
        capability(report, "Document-level security (DLS)").evidence().contains("_search?size=1"),
        "evidence must cite the real query string: "
            + capability(report, "Document-level security (DLS)").evidence());
    assertTrue(
        capability(report, "Field-level security (FLS)").detail().contains("Proven end to end"));
    assertTrue(report.recommendation().contains("Epic D"));
    assertTrue(report.recommendation().contains("observed rather than"));
    assertFalse(
        report.notes().stream().anyMatch(n -> n.contains("non-destructive")),
        "a write-probe run must not claim to have been non-destructive");
  }

  @Test
  void testWriteProbesTearDownEveryResourceTheyCreate() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    List<Request> invalidations = requestsTo("DELETE", "/_security/api_key");
    assertEquals(3, invalidations.size(), "each of the three probe keys must be invalidated");
    String invalidated = invalidations.stream().map(this::bodyOf).reduce("", String::concat);
    assertTrue(invalidated.contains("plain-key-id"));
    assertTrue(invalidated.contains("dls-key-id"));
    assertTrue(invalidated.contains("fls-key-id"));
    assertEquals(
        1, requestsTo("DELETE", "/_security/role/").size(), "the probe role must be deleted");
    // Nothing was left behind, so nothing should be reported as left behind.
    assertFalse(report.notes().stream().anyMatch(n -> n.contains("could not be")));
  }

  @Test
  void testProbeResourcesAreNamespacedAndShortLived() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();

    service().getCapabilityReport(null, true);

    for (Request creation : requestsTo("POST", "/_security/api_key")) {
      String body = bodyOf(creation);
      assertTrue(body.contains("duos-capability-probe-"), "probe keys must be namespaced: " + body);
      assertTrue(body.contains("\"expiration\":\"10m\""), "probe keys must expire: " + body);
    }
    Request role = requestsTo("PUT", "/_security/role/").get(0);
    assertTrue(role.getEndpoint().contains("duos_dlsfls_probe_"), role.getEndpoint());
  }

  /**
   * A key created without role_descriptors inherits a snapshot of the creating credential's own
   * permissions, which on a cluster where the deployment authenticates broadly would make the
   * round-trip probe mint a live copy of that credential for ten minutes. Every probe key must
   * carry a descriptor bounding what it can do.
   */
  @Test
  void testEveryProbeKeyIsScopedByARoleDescriptor() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();

    service().getCapabilityReport(null, true);

    List<Request> creations = requestsTo("POST", "/_security/api_key");
    assertEquals(3, creations.size());
    for (Request creation : creations) {
      assertTrue(
          bodyOf(creation).contains("\"role_descriptors\""),
          "a probe key must not inherit the deployment credential's privileges: "
              + bodyOf(creation));
    }
    // The plain round-trip key needs no privileges at all to prove it can authenticate.
    String plainKey =
        creations.stream()
            .map(this::bodyOf)
            .filter(b -> !b.contains("dls_probe") && !b.contains("fls_probe"))
            .findFirst()
            .orElseThrow();
    assertTrue(
        plainKey.contains("\"cluster\":[]") && plainKey.contains("\"indices\":[]"),
        "the round-trip probe key should grant nothing: " + plainKey);
  }

  @Test
  void testFailedTeardownIsReportedInTheNotesRatherThanSwallowed() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    stub(
        DELETE_ROLE,
        500,
        """
        {"error":{"reason":"boom"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    assertTrue(
        report.notes().stream().anyMatch(n -> n.contains("could not be deleted")),
        "an operator must be told a probe role was left on the cluster");
    assertTrue(report.notes().stream().anyMatch(n -> n.contains("duos_dlsfls_probe_")));
  }

  @Test
  void testDlsFilterAcceptedButNotEnforcedIsReportedAsUnavailable() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    // The dangerous case: the cluster took the filter and ignored it.
    stub(
        "ZGxz|" + SEARCH,
        200,
        """
        {"hits":{"total":{"value":2},"hits":[{"_source":{"x":1}}]}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    ElasticSearchCapability dls = capability(report, "Document-level security (DLS)");
    assertEquals(CapabilityVerdict.UNAVAILABLE, dls.verdict());
    assertTrue(dls.detail().contains("Not enforced"));
    assertTrue(dls.detail().contains("2 of 2"));
    assertTrue(report.recommendation().contains("Epic E"), report.recommendation());
  }

  @Test
  void testFlsGrantAcceptedButNotProjectedIsReportedAsUnavailable() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    stub(
        "Zmxz|" + SEARCH,
        200,
        """
        {"hits":{"total":{"value":2},"hits":[{"_source":{"datasetIdentifier":"D","secret":"leaked"}}]}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    ElasticSearchCapability fls = capability(report, "Field-level security (FLS)");
    assertEquals(CapabilityVerdict.UNAVAILABLE, fls.verdict());
    assertTrue(fls.detail().contains("Not enforced"));
    assertTrue(
        fls.detail().contains("secret"), "the leaked field should be named: " + fls.detail());
  }

  @Test
  void testBasicLicenseRefusesTheDlsFlsRoleAndIsReportedAsLicenseBlocked() throws IOException {
    stubSecurityEnabledCluster("basic");
    stubWorkingWriteProbes();
    stub(
        CREATE_ROLE,
        403,
        """
        {"error":{"reason":"current license is non-compliant for [field and document level security]"}}""");
    // A Basic cluster accepts the key and only fails at search time — the failure mode the record
    // doc calls out, so the probe has to reach the same verdict from a 403 on search.
    stub(
        "ZGxz|" + SEARCH,
        403,
        """
        {"error":{"reason":"current license is non-compliant for [field and document level security]"}}""");
    stub(
        "Zmxz|" + SEARCH,
        403,
        """
        {"error":{"reason":"current license is non-compliant for [field and document level security]"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    assertEquals(
        CapabilityVerdict.LICENSE_BLOCKED,
        capability(report, "Document-level security (DLS)").verdict());
    assertEquals(
        CapabilityVerdict.LICENSE_BLOCKED,
        capability(report, "Field-level security (FLS)").verdict());
    // API keys are a Basic feature, so they should still come back proven.
    assertEquals(CapabilityVerdict.SUPPORTED, capability(report, "API keys").verdict());
    assertTrue(report.recommendation().contains("Epic E"));
    // The role was refused, so there is nothing to delete and no note claiming otherwise.
    assertTrue(requestsTo("DELETE", "/_security/role/").isEmpty());
  }

  @Test
  void testCredentialThatCannotMintKeysIsReportedAsNotPermitted() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    stub(
        CREATE_KEY,
        403,
        """
        {"error":{"reason":"action [cluster:admin/xpack/security/api_key/create] is unauthorized"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    ElasticSearchCapability apiKeys = capability(report, "API keys");
    assertEquals(CapabilityVerdict.NOT_PERMITTED, apiKeys.verdict());
    assertTrue(apiKeys.detail().contains("refused"));
    // Without a key, the enforcement probes cannot run, so no key-scoped search is attempted.
    assertTrue(requestsTo("GET", "/dataset/_search").isEmpty());
    // The role probe still answers the license question on its own.
    assertEquals(
        CapabilityVerdict.SUPPORTED, capability(report, "Document-level security (DLS)").verdict());
  }

  /**
   * The likeliest real-world case: the deployment's shared credential holds neither manage_security
   * nor manage_api_key. The probes then describe the credential, not the cluster, and must not be
   * read as a verdict against the native path.
   */
  @Test
  void testCredentialWithoutWritePrivilegesYieldsInconclusiveRatherThanEpicE() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    stub(
        CREATE_KEY,
        403,
        """
        {"error":{"reason":"action [cluster:admin/xpack/security/api_key/create] is unauthorized"}}""");
    stub(
        CREATE_ROLE,
        403,
        """
        {"error":{"reason":"action [cluster:admin/xpack/security/role/put] is unauthorized"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    assertEquals(
        CapabilityVerdict.NOT_PERMITTED,
        capability(report, "Document-level security (DLS)").verdict());
    assertTrue(
        report.recommendation().contains("Inconclusive from the write probes"),
        "a privilege refusal says nothing about the cluster: " + report.recommendation());
    // It should still surface what the license implies rather than leaving the reader with nothing.
    assertTrue(report.recommendation().contains("Epic D"), report.recommendation());
    assertTrue(report.recommendation().contains("cluster_privileges"));
  }

  @Test
  void testNoteDoesNotClaimResourcesWereCreatedWhenNothingWas() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    stub(
        CREATE_KEY,
        403,
        """
        {"error":{"reason":"unauthorized"}}""");
    stub(
        CREATE_ROLE,
        403,
        """
        {"error":{"reason":"unauthorized"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    assertTrue(
        report.notes().stream().anyMatch(n -> n.contains("accepted nothing")),
        "notes must say nothing was created: " + report.notes());
    assertFalse(
        report.notes().stream().anyMatch(n -> n.contains("were created under")),
        "notes must not claim resources were created and removed when none were");
    assertTrue(requestsTo("DELETE", "/_security/api_key").isEmpty());
    assertTrue(requestsTo("DELETE", "/_security/role/").isEmpty());
  }

  /**
   * A dropped connection is not a licensing finding. Reporting one as LICENSE_BLOCKED would push
   * the recommendation to Epic E off the back of a network blip.
   */
  @Test
  void testTransportFailureDuringEnforcementSearchIsUnknownNotLicenseBlocked() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    // No stub for the DLS key's search: the client throws, which is how a transport failure
    // arrives.
    stubs.remove("ZGxz|" + SEARCH);

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    ElasticSearchCapability dls = capability(report, "Document-level security (DLS)");
    assertEquals(CapabilityVerdict.UNKNOWN, dls.verdict());
    assertTrue(dls.detail().contains("transport failure"), dls.detail());
  }

  /**
   * An API key is capped by its owner's privileges, so a search 403 need not be about licensing.
   */
  @Test
  void testPrivilegeRefusalDuringEnforcementSearchIsNotReportedAsLicenseBlocked()
      throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    stub(
        "ZGxz|" + SEARCH,
        403,
        """
        {"error":{"reason":"action [indices:data/read/search] is unauthorized for API key"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    ElasticSearchCapability dls = capability(report, "Document-level security (DLS)");
    assertEquals(CapabilityVerdict.NOT_PERMITTED, dls.verdict());
    assertTrue(dls.detail().contains("privilege rather than licensing"), dls.detail());
    // And a privilege refusal must not be read as a verdict against the native path.
    assertTrue(report.recommendation().contains("Inconclusive"), report.recommendation());
  }

  /**
   * "Not enforced" is the gravest verdict this report can return — a filter accepted and ignored.
   * It must never come from a response shape the parser simply did not recognise.
   */
  @Test
  void testUnparseableSearchResponseIsUnknownRatherThanNotEnforced() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    stub(
        "ZGxz|" + SEARCH,
        200,
        """
        {"took":1,"timed_out":false}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    ElasticSearchCapability dls = capability(report, "Document-level security (DLS)");
    assertEquals(CapabilityVerdict.UNKNOWN, dls.verdict());
    assertFalse(dls.detail().contains("Not enforced"), dls.detail());
    assertTrue(dls.detail().contains("no hit total"), dls.detail());
  }

  @Test
  void testFlsWithNoInspectableFieldsFallsBackAndSaysWhy() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    // A hit with no _source at all: nothing to check the projection against.
    stub(
        "Zmxz|" + SEARCH,
        200,
        """
        {"hits":{"total":{"value":2},"hits":[{"_id":"1"}]}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    // Role acceptance stands, but the report must not let that read as proven enforcement.
    assertEquals(
        CapabilityVerdict.SUPPORTED, capability(report, "Field-level security (FLS)").verdict());
    assertFalse(capability(report, "Field-level security (FLS)").detail().contains("Proven"));
    assertTrue(
        report.notes().stream().anyMatch(n -> n.contains("no document fields to inspect")),
        "the reader must be told the projection check was inconclusive: " + report.notes());
  }

  @Test
  void testMissingProbeKeyIsSaidToLimitTheDlsAndFlsVerdicts() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    stub(
        CREATE_KEY,
        403,
        """
        {"error":{"reason":"unauthorized"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    assertTrue(
        report.notes().stream().anyMatch(n -> n.contains("No usable probe key")),
        "role acceptance must not be presented as enforcement: " + report.notes());
  }

  @Test
  void testProbeNamesAreUniquePerRunNotPerMillisecond() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();

    service().getCapabilityReport(null, true);
    String first = requestsTo("PUT", "/_security/role/").get(0).getEndpoint();
    requests.clear();
    service().getCapabilityReport(null, true);
    String second = requestsTo("PUT", "/_security/role/").get(0).getEndpoint();

    assertNotEquals(first, second, "two runs must not be able to collide on a probe role name");
  }

  @Test
  void testEmptyIndexFallsBackToRoleAcceptanceAndSaysSo() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    stub(
        COUNT,
        200,
        """
        {"count":0}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    // Role acceptance still stands, but it must not be dressed up as end-to-end proof.
    ElasticSearchCapability dls = capability(report, "Document-level security (DLS)");
    assertEquals(CapabilityVerdict.SUPPORTED, dls.verdict());
    assertFalse(dls.detail().contains("Proven end to end"));
    assertTrue(
        report.notes().stream().anyMatch(n -> n.contains("empty")),
        "an empty index must be called out rather than read as a pass");
    assertTrue(requestsTo("GET", "/dataset/_search").isEmpty());
  }

  @Test
  void testUnreadableIndexIsCalledOutRatherThanTreatedAsEnforcement() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    stub(
        COUNT,
        403,
        """
        {"error":{"reason":"unauthorized"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    assertTrue(report.notes().stream().anyMatch(n -> n.contains("not readable")));
    assertEquals(
        CapabilityVerdict.SUPPORTED, capability(report, "Document-level security (DLS)").verdict());
  }

  @Test
  void testWriteProbesAreNotRunWhenSecurityIsDisabled() throws IOException {
    stubSecurityDisabledCluster();

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    assertEquals(Boolean.FALSE, report.writeProbesRun());
    assertTrue(
        report.notes().stream().anyMatch(n -> n.contains("requested but not run")),
        "asking for write probes on a security-disabled cluster must be answered explicitly");
    assertTrue(requestsTo("POST", "/_security/api_key").isEmpty());
    for (ElasticSearchCapability capability : report.capabilities()) {
      assertEquals(CapabilityVerdict.UNAVAILABLE, capability.verdict());
    }
  }

  @Test
  void testReadOnlyRunCreatesNothing() throws IOException {
    stubSecurityEnabledCluster("trial");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    assertEquals(Boolean.FALSE, report.writeProbesRun());
    assertTrue(requestsTo("POST", "/_security/api_key").isEmpty());
    assertTrue(requestsTo("PUT", "/_security/role/").isEmpty());
    assertTrue(requestsTo("DELETE", "/_security/api_key").isEmpty());
    assertTrue(report.notes().stream().anyMatch(n -> n.contains("non-destructive")));
  }

  @Test
  void testElasticCloudDeploymentIsReportedAsSuch() throws IOException {
    config.setCloudId("duos-cloud:ZXhhbXBsZQ==");
    stubSecurityEnabledCluster("trial");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    assertEquals("Elastic Cloud (X-Pack always present)", report.edition());
    assertTrue(report.elasticCloud());
    assertTrue(
        report.notes().stream().anyMatch(n -> n.contains("cloud ID")),
        "an Elastic Cloud deployment must be called out in the notes");
  }

  @Test
  void testOssDistributionIsReportedWhenXPackEndpointIsMissing() throws IOException {
    stubSecurityDisabledCluster();
    stub(
        XPACK,
        404,
        """
        {"error":{"reason":"no handler found"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    assertEquals("OSS (no X-Pack endpoint)", report.edition());
  }

  @Test
  void testSecurityReportedEnabledButApiUnreachableIsDistinguishedFromDisabled()
      throws IOException {
    stubSecurityDisabledCluster();
    stub(
        XPACK,
        200,
        """
        {"features":{"security":{"available":true,"enabled":true}}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    ElasticSearchCapability security = capability(report, "X-Pack Security");
    assertEquals(CapabilityVerdict.UNAVAILABLE, security.verdict());
    assertTrue(
        security.detail().contains("reports itself enabled"),
        "the two signals disagreeing must be described, not silently resolved: "
            + security.detail());
  }

  @Test
  void testApiKeysExplicitlyDisabledByClusterSettingIsReportedAsUnavailable() throws IOException {
    stubSecurityEnabledCluster("trial");
    stub(
        SETTINGS,
        200,
        """
        {"defaults":{"xpack.security.enabled":"true","xpack.security.dls_fls.enabled":"true",
        "xpack.security.authc.api_key.enabled":"false"},"persistent":{},"transient":{}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    ElasticSearchCapability apiKeys = capability(report, "API keys");
    assertEquals(CapabilityVerdict.UNAVAILABLE, apiKeys.verdict());
    assertTrue(apiKeys.detail().contains("explicitly disabled by cluster setting"));
  }

  @Test
  void testUnmappedLicenseTierYieldsUnknownDlsFlsVerdict() throws IOException {
    stubSecurityEnabledCluster("some-future-tier");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    ElasticSearchCapability dls = capability(report, "Document-level security (DLS)");
    assertEquals(CapabilityVerdict.UNKNOWN, dls.verdict());
    assertTrue(dls.detail().contains("could not be mapped"));
    assertTrue(report.recommendation().contains("Inconclusive"));
  }

  @Test
  void testRunAsAcceptedButResolvedToADifferentUserIsUnknown() throws IOException {
    stubSecurityEnabledCluster("trial");
    stub(
        RUN_AS,
        200,
        """
        {"username":"someone-else","roles":["other"]}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    ElasticSearchCapability runAs = capability(report, "run_as impersonation");
    assertEquals(CapabilityVerdict.UNKNOWN, runAs.verdict());
    assertTrue(runAs.detail().contains("still resolved to"));
  }

  @Test
  void testRunAsUnexpectedStatusIsUnknown() throws IOException {
    stubSecurityEnabledCluster("trial");
    stub(
        RUN_AS,
        500,
        """
        {"error":{"reason":"internal server error"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    ElasticSearchCapability runAs = capability(report, "run_as impersonation");
    assertEquals(CapabilityVerdict.UNKNOWN, runAs.verdict());
    assertTrue(runAs.detail().contains("unexpected status"));
  }

  @Test
  void testApiKeyCreatedButUnableToAuthenticateIsReportedAsUnknown() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    stub(
        "cGxhaW4=|" + AUTHENTICATE,
        403,
        """
        {"error":{"reason":"unauthorized"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    ElasticSearchCapability apiKeys = capability(report, "API keys");
    assertEquals(CapabilityVerdict.UNKNOWN, apiKeys.verdict());
    assertTrue(apiKeys.detail().contains("could not authenticate"));
  }

  @Test
  void testMalformedProbeKeyResponseLeavesEnforcementInconclusive() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    // Created, but the response carries neither an encoded key nor an id/secret pair to build one.
    stub(
        CREATE_DLS_KEY,
        200,
        """
        {"id":"dls-key-id"}""");
    stub(
        CREATE_FLS_KEY,
        200,
        """
        {"id":"fls-key-id"}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    assertTrue(
        report.notes().stream()
            .anyMatch(n -> n.contains("DLS enforcement check could not be run to a conclusion")));
    assertTrue(
        report.notes().stream()
            .anyMatch(n -> n.contains("FLS projection check returned no document fields")));
  }

  @Test
  void testFailedKeyInvalidationIsReportedInTheNotesRatherThanSwallowed() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    stub(
        INVALIDATE_KEY,
        500,
        """
        {"error":{"reason":"boom"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    assertTrue(
        report.notes().stream().anyMatch(n -> n.contains("could not be invalidated")),
        "an operator must be told a probe key was left on the cluster: " + report.notes());
  }

  @Test
  void testRoleRefusalWithNoLicenseOrPrivilegeReasonIsUnknownRatherThanMisclassified()
      throws IOException {
    stubSecurityEnabledCluster("trial");
    // No usable probe key, so the enforcement probes are skipped and the DLS/FLS verdicts stand at
    // role acceptance alone rather than being overwritten by an end-to-end enforcement result.
    stub(
        CREATE_KEY,
        500,
        """
        {"error":{"reason":"internal server error"}}""");
    stub(
        CREATE_ROLE,
        500,
        """
        {"error":{"reason":"internal server error"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    ElasticSearchCapability dls = capability(report, "Document-level security (DLS)");
    assertEquals(CapabilityVerdict.UNKNOWN, dls.verdict());
    assertTrue(dls.detail().contains("The role was rejected"));
  }

  @Test
  void testEncodedApiKeyIsBuiltFromIdAndSecretWhenEncodedFieldIsAbsent() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    stub(
        CREATE_KEY,
        200,
        """
        {"id":"plain-key-id","api_key":"plain-secret"}""");
    String builtKey =
        java.util.Base64.getEncoder()
            .encodeToString(
                "plain-key-id:plain-secret".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    stub(
        builtKey + "|" + AUTHENTICATE,
        200,
        """
        {"username":"consent"}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    assertEquals(CapabilityVerdict.SUPPORTED, capability(report, "API keys").verdict());
  }

  @Test
  void testUnrecognisedSearchResponseShapeFallsBackRatherThanFailingTheProbe() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    // No "total" under "hits" at all: hitCount() must return -1 rather than throw.
    stub(
        "ZGxz|" + SEARCH,
        200,
        """
        {"hits":{}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    ElasticSearchCapability dls = capability(report, "Document-level security (DLS)");
    assertEquals(CapabilityVerdict.UNKNOWN, dls.verdict());
    assertTrue(dls.detail().contains("no hit total"));
  }

  @Test
  void testRunAsWithNoAuthenticatedOrRequestedUserIsUnknown() throws IOException {
    stubSecurityEnabledCluster("trial");
    stub(
        AUTHENTICATE,
        200,
        """
        {"roles":["consent"]}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    ElasticSearchCapability runAs = capability(report, "run_as impersonation");
    assertEquals(CapabilityVerdict.UNKNOWN, runAs.verdict());
    assertTrue(runAs.detail().contains("No target user was available"));
  }

  @Test
  void testApiKeyCreatedWithNoUsableCredentialAtAllIsReportedAsUnknown() throws IOException {
    stubSecurityEnabledCluster("trial");
    stub(
        CREATE_KEY,
        200,
        """
        {"name":"probe-key"}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    ElasticSearchCapability apiKeys = capability(report, "API keys");
    assertEquals(CapabilityVerdict.UNKNOWN, apiKeys.verdict());
    assertTrue(apiKeys.detail().contains("neither an encoded form nor an id and"));
  }

  @Test
  void testDlsKeyCreationRefusedOnLicenseGroundsIsReportedAtCreationRatherThanSearch()
      throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    stub(
        CREATE_DLS_KEY,
        403,
        """
        {"error":{"reason":"current license is non-compliant for [field and document level security]"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    ElasticSearchCapability dls = capability(report, "Document-level security (DLS)");
    assertEquals(CapabilityVerdict.LICENSE_BLOCKED, dls.verdict());
    assertTrue(dls.detail().contains("A key carrying"));
  }

  @Test
  void testDlsSearchUnexpectedStatusIsUnknownRatherThanMisclassified() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    stub(
        "ZGxz|" + SEARCH,
        500,
        """
        {"error":{"reason":"internal server error"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    ElasticSearchCapability dls = capability(report, "Document-level security (DLS)");
    assertEquals(CapabilityVerdict.UNKNOWN, dls.verdict());
    assertTrue(dls.detail().contains("unexpected status"));
  }

  @Test
  void testHitCountReadsAPlainNumericTotalAsWellAsAnObjectShapedOne() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    stub(
        "ZGxz|" + SEARCH,
        200,
        """
        {"hits":{"total":0,"hits":[]}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    ElasticSearchCapability dls = capability(report, "Document-level security (DLS)");
    assertEquals(CapabilityVerdict.SUPPORTED, dls.verdict());
  }

  @Test
  void testFlsSearchWithNoHitsIsReportedAsInconclusiveRatherThanUnprojected() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    // No "hits" array under "hits" at all: firstHitSourceFields() must fall back rather than throw.
    stub(
        "Zmxz|" + SEARCH,
        200,
        """
        {"hits":{"total":{"value":0}}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    assertTrue(
        report.notes().stream()
            .anyMatch(n -> n.contains("FLS projection check returned no document fields")));
  }

  @Test
  void testFlsSearchWithAnEmptyHitsArrayIsReportedAsInconclusive() throws IOException {
    stubSecurityEnabledCluster("trial");
    stubWorkingWriteProbes();
    // "hits" is present as an array, but empty: no document to read fields from.
    stub(
        "Zmxz|" + SEARCH,
        200,
        """
        {"hits":{"total":{"value":0},"hits":[]}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, true);

    assertTrue(
        report.notes().stream()
            .anyMatch(n -> n.contains("FLS projection check returned no document fields")));
  }

  @Test
  void testProbeResponseBodyThatIsNotValidJsonFallsBackToAnEmptyBodyRatherThanThrowing()
      throws IOException {
    stubSecurityEnabledCluster("trial");
    stub(HAS_PRIVILEGES, 200, "not valid json");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    assertTrue(report.clusterPrivileges().isEmpty());
  }

  @Test
  void testReasonFallsBackToAGenericMessageWhenTheErrorBodyHasNoReasonField() throws IOException {
    stubSecurityEnabledCluster("trial");
    stub(
        RUN_AS,
        500,
        """
        {"error":{}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    ElasticSearchCapability runAs = capability(report, "run_as impersonation");
    assertTrue(runAs.detail().contains("no reason reported by the cluster"));
  }

  @Test
  void testClusterPrivilegesProbeFailureYieldsAnEmptyMapRatherThanAnException() throws IOException {
    stubSecurityEnabledCluster("trial");
    stub(
        HAS_PRIVILEGES,
        500,
        """
        {"error":{"reason":"internal server error"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    assertTrue(report.clusterPrivileges().isEmpty());
  }

  @Test
  void testSecuritySettingsProbeFailureYieldsAnEmptyMapRatherThanAnException() throws IOException {
    stubSecurityEnabledCluster("trial");
    stub(
        SETTINGS,
        500,
        """
        {"error":{"reason":"internal server error"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    assertTrue(report.securitySettings().isEmpty());
  }

  @Test
  void testSecuritySettingsSkipsSectionsThatAreAbsentOrNotAnObject() throws IOException {
    stubSecurityEnabledCluster("trial");
    stub(
        SETTINGS,
        200,
        """
        {"defaults":{"xpack.security.enabled":"true","cluster.name":"duos-cluster"},
        "persistent":"not-an-object"}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    assertEquals("true", report.securitySettings().get("xpack.security.enabled"));
    assertFalse(
        report.securitySettings().containsKey("cluster.name"),
        "a non-security default must be filtered out rather than reported");
  }

  @Test
  void testSecurityEnabledFallsBackToClusterSettingWhenXPackIsUnreachable() throws IOException {
    stubSecurityDisabledCluster();
    stub(
        XPACK,
        404,
        """
        {"error":{"reason":"no handler found"}}""");
    stub(
        SETTINGS,
        200,
        """
        {"defaults":{"xpack.security.enabled":"true"},"persistent":{},"transient":{}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    assertEquals(Boolean.TRUE, report.securityEnabled());
  }

  @Test
  void testMissingClusterVersionCannotBeCompatibilityChecked() throws IOException {
    stubSecurityDisabledCluster();
    stub(
        ROOT,
        200,
        """
        {"cluster_name":"duos-cluster"}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    assertTrue(report.restClientCompatibility().contains("Could not determine"));
  }

  @Test
  void testMajorVersionSkewIsCalledOutRatherThanAssumedCompatible() throws IOException {
    stubSecurityDisabledCluster();
    stub(
        ROOT,
        200,
        """
        {"cluster_name":"duos-cluster","version":{"number":"8.1.2","build_flavor":"default"}}""");

    ElasticSearchCapabilityReport report = service().getCapabilityReport(null, false);

    assertTrue(report.restClientCompatibility().contains("Major-version skew"));
  }

  private record StubResponse(int status, String body) {}
}
