package org.broadinstitute.consent.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import org.elasticsearch.client.Request;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Cluster-level security preconditions for Epic D, asserted against a secured cluster with the
 * trial license active. This is the automated form of the trial-license half of the A-0 probes in
 * {@code docs/plans/elasticsearch-service-duos-ui-usage.md}.
 *
 * <p>One of four classes that together qualify an Elasticsearch version: this one covers the
 * distribution, license and transport preconditions; {@link ElasticSearchBasicLicenseTest} covers
 * what a basic license refuses and that it fails closed; {@link ElasticSearchSecurityDisabledTest}
 * covers the security-off compose default; {@link ElasticSearchDlsFlsEnforcementTest} covers actual
 * DLS/FLS enforcement of documents and fields. To qualify a new version, bump {@link
 * ElasticSearchTestCluster#IMAGE} and run all four.
 */
@Tag("elasticsearch")
class ElasticSearchSecurityBaselineTest extends ElasticSearchContainerTests {

  /**
   * Puts the cluster on the trial license through the admin endpoint. Explicit rather than
   * inherited from the harness: the DLS, FLS and run_as assertions below are assertions about a
   * trial-licensed cluster, and the step that makes it one is part of what they establish.
   */
  @BeforeAll
  static void putClusterOnTrialLicense() {
    activateTrialLicense();
  }

  /**
   * The cluster must report the version we pinned and the {@code default} build flavor. The OSS
   * flavor ships without X-Pack, so security, API keys and DLS/FLS would silently not exist.
   */
  @Test
  void clusterReportsPinnedVersionAndDefaultDistribution() throws Exception {
    JsonObject version = jsonResponse(new Request("GET", "/")).getAsJsonObject("version");

    assertEquals(ElasticSearchTestCluster.pinnedVersion(), version.get("number").getAsString());
    assertEquals(
        "default",
        version.get("build_flavor").getAsString(),
        "the OSS build flavor has no X-Pack, so security and DLS/FLS would be unavailable");
  }

  /** X-Pack security must be both available under the license and switched on. */
  @Test
  void xpackSecurityIsAvailableAndEnabled() throws Exception {
    JsonObject security =
        jsonResponse(new Request("GET", "/_xpack"))
            .getAsJsonObject("features")
            .getAsJsonObject("security");

    assertTrue(security.get("available").getAsBoolean(), "security not available under license");
    assertTrue(security.get("enabled").getAsBoolean(), "security not enabled on the cluster");
  }

  /**
   * A trial can be started only once per major version per cluster, so CI cannot rely on a
   * long-lived trial and a developer whose trial has expired must wipe the {@code elastic} volume.
   * The activation step in {@link #putClusterOnTrialLicense()} is what makes this false here — and
   * the same once-per-major-version constraint is why that step is something a test asks for rather
   * than something it inherits.
   */
  @Test
  void trialLicenseCannotBeStartedTwiceInTheSameMajorVersion() throws Exception {
    JsonObject trialStatus = jsonResponse(new Request("GET", "/_license/trial_status"));

    assertFalse(
        trialStatus.get("eligible_to_start_trial").getAsBoolean(),
        "trial should already be spent — see putClusterOnTrialLicense()");
  }

  /** A DLS role is accepted under the trial license. Rejected under basic; see the sibling test. */
  @Test
  void documentLevelSecurityRoleIsPermittedUnderTrialLicense() throws Exception {
    Request request =
        ElasticSearchTestCluster.jsonRequest(
            "PUT",
            "/_security/role/baseline-dls",
            ElasticSearchTestCluster.documentLevelSecurityRole(datasetIndex()));

    assertEquals(200, statusOf(request));
  }

  /**
   * An FLS role is accepted under the trial license. Rejected under basic; see the sibling test.
   */
  @Test
  void fieldLevelSecurityRoleIsPermittedUnderTrialLicense() throws Exception {
    Request request =
        ElasticSearchTestCluster.jsonRequest(
            "PUT",
            "/_security/role/baseline-fls",
            ElasticSearchTestCluster.fieldLevelSecurityRole(datasetIndex()));

    assertEquals(200, statusOf(request));
  }

  /**
   * {@code run_as} is part of the capability inventory Ticket A-1 must record, and is the fallback
   * if per-request API keys prove too costly in D-2.
   */
  @Test
  void runAsPrivilegeIsSupported() throws Exception {
    Request request =
        ElasticSearchTestCluster.jsonRequest(
            "PUT",
            "/_security/role/baseline-run-as",
            ElasticSearchTestCluster.runAsRole(datasetIndex()));

    assertEquals(200, statusOf(request));
  }

  /**
   * The HTTP layer must stay plain {@code http}: {@code ElasticSearchSupport.createRestClient} has
   * no {@code SSLContext} hook, so a version that started serving TLS on the same port by default
   * would break both local development and this harness. A TLS handshake must fail outright rather
   * than return any HTTP response.
   */
  @Test
  void tlsIsNotServedOnTheHttpPort() throws Exception {
    ElasticSearchTestCluster.assertTlsIsNotServed(elasticSearchConfiguration());
  }
}
