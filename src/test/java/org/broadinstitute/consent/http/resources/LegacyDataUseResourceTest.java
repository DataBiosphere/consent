package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.broadinstitute.consent.http.models.datause.LegacyDataUseRunReport;
import org.broadinstitute.consent.http.models.datause.LegacyDataUseRunResult;
import org.broadinstitute.consent.http.models.datause.NoncanonicalDataUseView;
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

  private static LegacyDataUseRunResult runResult(LegacyDataUseRunReport run) {
    var counts = new PersistedDataUseReport(1, Map.of("SINGLE(GRU)", 1), Map.of(), 0, 0, 0);
    return new LegacyDataUseRunResult(counts, counts, run);
  }

  private void initResource() {
    resource = new LegacyDataUseResource(service);
  }

  /** The report reaches a ticket or a log, so the payload must stay free of Other text. */
  @Test
  void testGetNoncanonicalDatasets() {
    var views =
        List.of(new NoncanonicalDataUseView(7, "MULTIPLE(HMB,OTHER)", "controlled", 2, true));
    when(service.findNoncanonicalViews()).thenReturn(views);
    initResource();

    Response response = resource.getNoncanonicalDatasets(duosUser);

    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(views, response.getEntity());
  }

  @Test
  void testGetNoncanonicalDatasetsServiceFailure() {
    when(service.findNoncanonicalViews()).thenThrow(new RuntimeException("db down"));
    initResource();

    Response response = resource.getNoncanonicalDatasets(duosUser);

    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
  }

  @Test
  void testRecomputeMatches() {
    var result = runResult(new LegacyDataUseRunReport(3, 0, 0, 0, 5, List.of()));
    when(service.recomputeAbstainingMatches()).thenReturn(result);
    initResource();

    Response response = resource.recomputeAbstainingMatches(duosUser);

    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(result, response.getEntity());
  }

  @Test
  void testRecomputeMatchesReportsFailedDatasetsForRerun() {
    when(service.recomputeAbstainingMatches())
        .thenReturn(runResult(new LegacyDataUseRunReport(1, 0, 1, 1, 1, List.of(42))));
    initResource();

    Response response = resource.recomputeAbstainingMatches(duosUser);

    var entity = assertInstanceOf(LegacyDataUseRunResult.class, response.getEntity());
    assertEquals(List.of(42), entity.run().failedDatasetIds());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testRecomputeMatchesServiceFailure() {
    when(service.recomputeAbstainingMatches()).thenThrow(new RuntimeException("db down"));
    initResource();

    Response response = resource.recomputeAbstainingMatches(duosUser);

    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
  }
}
