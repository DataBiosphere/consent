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
