package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.elastic_search.CapabilityVerdict;
import org.broadinstitute.consent.http.models.elastic_search.ElasticSearchCapability;
import org.broadinstitute.consent.http.models.elastic_search.ElasticSearchCapabilityReport;
import org.broadinstitute.consent.http.models.elastic_search.ElasticSearchLicenseActivation;
import org.broadinstitute.consent.http.models.elastic_search.ElasticSearchLicenseStatus;
import org.broadinstitute.consent.http.models.elastic_search.LicenseActivationOutcome;
import org.broadinstitute.consent.http.service.ElasticSearchCapabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ElasticSearchCapabilityResourceTest extends AbstractTestHelper {

  @Mock private ElasticSearchCapabilityService capabilityService;

  private ElasticSearchCapabilityResource resource;
  private final AuthUser authUser = new AuthUser("admin@test.com");
  private final User user = new User(1, "admin@test.com", "Admin", new Date());
  private final DuosUser duosUser = new DuosUser(authUser, user);

  @BeforeEach
  void setUp() {
    resource = new ElasticSearchCapabilityResource(capabilityService);
  }

  private ElasticSearchCapabilityReport report() {
    return new ElasticSearchCapabilityReport(
        "duos-cluster",
        "9.3.3",
        "elasticsearch",
        "trial",
        "trial",
        "active",
        false,
        true,
        "consent",
        List.of("superuser"),
        Map.of("manage_api_key", true),
        Map.of("xpack.security.enabled", "true"),
        false,
        List.of(
            new ElasticSearchCapability(
                "X-Pack Security", CapabilityVerdict.SUPPORTED, "enabled", "GET /_xpack")),
        "Compatible",
        "Epic D",
        List.of("All probes are non-destructive."));
  }

  @Test
  void testGetCapabilities() {
    when(capabilityService.getCapabilityReport(null, false)).thenReturn(report());

    Response response = resource.getCapabilities(duosUser, null);

    assertEquals(200, response.getStatus());
    assertNotNull(response.getEntity());
    assertEquals(report(), response.getEntity());
    verify(capabilityService).getCapabilityReport(null, false);
  }

  @Test
  void testGetCapabilitiesPassesRunAsUserThrough() {
    when(capabilityService.getCapabilityReport("someone-else", false)).thenReturn(report());

    Response response = resource.getCapabilities(duosUser, "someone-else");

    assertEquals(200, response.getStatus());
    verify(capabilityService).getCapabilityReport("someone-else", false);
  }

  /**
   * The GET is the whole read-only contract: creating credentials on the cluster must not be
   * reachable by anything that merely follows a link, so no argument to it can turn writes on.
   */
  @Test
  void testGetNeverRunsWriteProbes() {
    when(capabilityService.getCapabilityReport(null, false)).thenReturn(report());

    resource.getCapabilities(duosUser, null);

    verify(capabilityService, never()).getCapabilityReport(null, true);
  }

  /** Write probes are what the POST is for, and the only thing that reaches them. */
  @Test
  void testPostRunsWriteProbes() {
    when(capabilityService.getCapabilityReport(null, true)).thenReturn(report());

    Response response = resource.runCapabilityProbes(duosUser, null);

    assertEquals(200, response.getStatus());
    assertEquals(report(), response.getEntity());
    verify(capabilityService).getCapabilityReport(null, true);
    verify(capabilityService, never()).getCapabilityReport(null, false);
  }

  @Test
  void testPostPassesRunAsUserThrough() {
    when(capabilityService.getCapabilityReport("someone-else", true)).thenReturn(report());

    Response response = resource.runCapabilityProbes(duosUser, "someone-else");

    assertEquals(200, response.getStatus());
    verify(capabilityService).getCapabilityReport("someone-else", true);
  }

  private ElasticSearchLicenseStatus licenseStatus() {
    return new ElasticSearchLicenseStatus(
        "duos-cluster",
        "basic",
        "active",
        false,
        true,
        "License 'basic', status 'active'.",
        List.of("Read-only."));
  }

  private ElasticSearchLicenseActivation activation() {
    return new ElasticSearchLicenseActivation(
        LicenseActivationOutcome.ACTIVATED,
        "The trial license was started.",
        licenseStatus(),
        new ElasticSearchLicenseStatus(
            "duos-cluster",
            "trial",
            "active",
            true,
            false,
            "License 'trial', status 'active'.",
            List.of()),
        List.of("A trial can be started only once per cluster."));
  }

  @Test
  void testGetLicense() {
    when(capabilityService.getLicenseStatus()).thenReturn(licenseStatus());

    Response response = resource.getLicense(duosUser);

    assertEquals(200, response.getStatus());
    assertEquals(licenseStatus(), response.getEntity());
    verify(capabilityService).getLicenseStatus();
  }

  /** Reading the license must not be able to change it, whatever else the resource can do. */
  @Test
  void testGetLicenseNeverActivatesTheTrial() {
    when(capabilityService.getLicenseStatus()).thenReturn(licenseStatus());

    resource.getLicense(duosUser);

    verify(capabilityService, never()).activateTrialLicense();
  }

  @Test
  void testActivateTrialLicense() {
    when(capabilityService.activateTrialLicense()).thenReturn(activation());

    Response response = resource.activateTrialLicense(duosUser, true);

    assertEquals(200, response.getStatus());
    assertEquals(activation(), response.getEntity());
    verify(capabilityService).activateTrialLicense();
  }

  /**
   * The guard that carries the weight here. A trial can be started once per cluster and cannot be
   * reverted, so an unacknowledged call must be refused before the service — and therefore the
   * cluster — is reached at all, not merely reported on afterwards.
   */
  @Test
  void testActivateTrialLicenseRequiresAcknowledgement() {
    Response response = resource.activateTrialLicense(duosUser, null);

    assertEquals(400, response.getStatus());
    verify(capabilityService, never()).activateTrialLicense();
  }

  /** An explicit {@code acknowledge=false} is a refusal to acknowledge, not an acknowledgement. */
  @Test
  void testActivateTrialLicenseRejectsFalseAcknowledgement() {
    Response response = resource.activateTrialLicense(duosUser, false);

    assertEquals(400, response.getStatus());
    verify(capabilityService, never()).activateTrialLicense();
  }

  @Test
  void testGetLicenseHandlesServiceFailure() {
    when(capabilityService.getLicenseStatus()).thenThrow(new RuntimeException("cluster exploded"));

    Response response = resource.getLicense(duosUser);

    assertEquals(500, response.getStatus());
  }

  @Test
  void testActivateTrialLicenseHandlesServiceFailure() {
    when(capabilityService.activateTrialLicense())
        .thenThrow(new RuntimeException("cluster exploded"));

    Response response = resource.activateTrialLicense(duosUser, true);

    assertEquals(500, response.getStatus());
  }

  @Test
  void testGetCapabilitiesHandlesServiceFailure() {
    when(capabilityService.getCapabilityReport(null, false))
        .thenThrow(new RuntimeException("cluster exploded"));

    Response response = resource.getCapabilities(duosUser, null);

    assertEquals(500, response.getStatus());
  }

  @Test
  void testPostHandlesServiceFailure() {
    when(capabilityService.getCapabilityReport(null, true))
        .thenThrow(new RuntimeException("cluster exploded"));

    Response response = resource.runCapabilityProbes(duosUser, null);

    assertEquals(500, response.getStatus());
  }
}
