package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.datause.PersistedDataUseReport;
import org.broadinstitute.consent.http.service.LegacyDataUseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LegacyDataUseResourceTest {

  @Mock private LegacyDataUseService service;

  private final AuthUser authUser = new AuthUser("test");
  private final List<UserRole> roles = List.of(UserRoles.Admin());
  private final User user = new User(1, authUser.getEmail(), "Display Name", new Date(), roles);
  private final DuosUser duosUser = new DuosUser(authUser, user);

  private LegacyDataUseResource resource;

  private void initResource() {
    resource = new LegacyDataUseResource(service);
  }

  @Test
  void testGetReport() {
    var report =
        new PersistedDataUseReport(
            4,
            Map.of("SINGLE(GRU)", 3, "MULTIPLE(HMB,OTHER)", 1),
            Map.of("controlled", Map.of("SINGLE(GRU)", 3, "MULTIPLE(HMB,OTHER)", 1)),
            1,
            1,
            2);
    when(service.report()).thenReturn(report);
    initResource();

    Response response = resource.getReport(duosUser);

    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(report, response.getEntity());
  }

  /** The report reaches a ticket or a log, so the payload must stay free of Other text. */
  @Test
  void testGetReportEntityCarriesCountsOnly() {
    when(service.report())
        .thenReturn(
            new PersistedDataUseReport(
                1,
                Map.of("SINGLE(OTHER)", 1),
                Map.of("controlled", Map.of("SINGLE(OTHER)", 1)),
                0,
                0,
                0));
    initResource();

    Response response = resource.getReport(duosUser);

    var entity = assertInstanceOf(PersistedDataUseReport.class, response.getEntity());
    assertFalse(entity.countsByClassification().keySet().toString().contains("bespoke"));
    assertEquals(Map.of("SINGLE(OTHER)", 1), entity.countsByClassification());
  }

  @Test
  void testGetReportServiceFailure() {
    when(service.report()).thenThrow(new RuntimeException("database unavailable"));
    initResource();

    Response response = resource.getReport(duosUser);

    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
  }
}
