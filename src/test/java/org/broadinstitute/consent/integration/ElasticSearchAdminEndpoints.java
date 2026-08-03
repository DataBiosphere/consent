package org.broadinstitute.consent.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.Response;
import java.util.Date;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.elastic_search.ElasticSearchLicenseActivation;
import org.broadinstitute.consent.http.models.elastic_search.ElasticSearchLicenseStatus;
import org.broadinstitute.consent.http.resources.ElasticSearchCapabilityResource;
import org.broadinstitute.consent.http.service.ElasticSearchCapabilityService;
import org.elasticsearch.client.RestClient;

/**
 * Reaches a container through the admin-only Elasticsearch endpoints rather than through the
 * cluster's own REST API.
 *
 * <p>This is how the license gets activated in these tests: a class that needs a trial-licensed
 * cluster asks for one, by calling the endpoint an operator would call in a deployed environment.
 * Not by {@code POST /_license/start_trial} from a shared harness — a trial is one-shot per cluster
 * and cannot be reverted, so a class whose subject is what a *basic* license refuses must not have
 * one spent underneath it, and no test should run against a license tier that some initializer
 * changed on its behalf.
 *
 * <p>Going through {@link ElasticSearchCapabilityResource} rather than the service beneath it is
 * deliberate — it is the resource that owns the acknowledgement guard, so a test can assert that an
 * unacknowledged call leaves the cluster alone. What a direct call cannot exercise is the
 * {@code @RolesAllowed} check, which Jersey applies at dispatch; the admin principal here stands in
 * for a request that has already passed it.
 */
final class ElasticSearchAdminEndpoints {

  private ElasticSearchAdminEndpoints() {}

  /** The admin endpoints, wired to the cluster {@code client} points at. */
  static ElasticSearchCapabilityResource resourceFor(
      RestClient client, ElasticSearchConfiguration configuration) {
    return new ElasticSearchCapabilityResource(
        new ElasticSearchCapabilityService(client, configuration));
  }

  /** An authenticated admin principal, standing in for a request Jersey has already authorized. */
  static DuosUser admin() {
    AuthUser authUser = new AuthUser("es-integration-admin@test.com");
    return new DuosUser(
        authUser, new User(1, authUser.getEmail(), "ES Integration Admin", new Date()));
  }

  /**
   * Activates the trial license through the admin endpoint, failing the calling test if the
   * endpoint itself errors.
   *
   * <p>Safe to call more than once against the same cluster, which matters because the container is
   * shared per JVM: the first caller gets {@code ACTIVATED} and any later one {@code
   * ALREADY_LICENSED}. Callers should therefore assert on the resulting license state rather than
   * on the outcome, unless they own the cluster outright.
   */
  static ElasticSearchLicenseActivation activateTrialLicense(
      RestClient client, ElasticSearchConfiguration configuration) {
    Response response = resourceFor(client, configuration).activateTrialLicense(admin(), true);
    assertEquals(
        200, response.getStatus(), "the trial activation endpoint failed: " + response.getEntity());
    return (ElasticSearchLicenseActivation) response.getEntity();
  }

  /** Reads the license state through the read-only admin endpoint. */
  static ElasticSearchLicenseStatus licenseStatus(
      RestClient client, ElasticSearchConfiguration configuration) {
    Response response = resourceFor(client, configuration).getLicense(admin());
    assertEquals(
        200, response.getStatus(), "the license status endpoint failed: " + response.getEntity());
    return (ElasticSearchLicenseStatus) response.getEntity();
  }
}
