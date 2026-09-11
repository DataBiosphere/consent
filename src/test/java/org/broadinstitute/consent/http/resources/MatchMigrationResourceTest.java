package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.matchmigration.MatchMigrationPopulation;
import org.broadinstitute.consent.http.models.matchmigration.SnapshotReconciliation;
import org.broadinstitute.consent.http.service.MatchMigrationService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchMigrationResourceTest {

  @Mock private MatchMigrationService service;

  private final AuthUser authUser = new AuthUser("test");
  private final List<UserRole> roles = List.of(UserRoles.Admin());
  private final User user = new User(1, authUser.getEmail(), "Display Name", new Date(), roles);
  private final DuosUser duosUser = new DuosUser(authUser, user);

  private MatchMigrationResource resource;

  private void initResource() {
    resource = new MatchMigrationResource(service);
  }

  @Test
  void testGetPopulation() {
    when(service.findPopulation()).thenReturn(population());
    initResource();

    Response response = resource.getPopulation(duosUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(population(), response.getEntity());
  }

  @Test
  void testGetPopulationHandlesAFailure() {
    when(service.findPopulation()).thenThrow(new IllegalStateException("boom"));
    initResource();

    Response response = resource.getPopulation(duosUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
  }

  /**
   * Responses serialize with Gson, which reads fields and drops computed accessors. The release
   * gate has to be a component rather than a method to reach the operator reading the response.
   */
  @Test
  void testPopulationResponseCarriesTheDerivedGate() {
    when(service.findPopulation()).thenReturn(population());
    initResource();

    String json = GsonUtil.getInstance().toJson(resource.getPopulation(duosUser).getEntity());
    assertTrue(json.contains("\"blocksConstraints\""));
  }

  @Test
  void testGetReconciliation() {
    when(service.reconcile()).thenReturn(reconciliation());
    initResource();

    Response response = resource.getReconciliation(duosUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(reconciliation(), response.getEntity());
  }

  @Test
  void testGetReconciliationHandlesAFailure() {
    when(service.reconcile()).thenThrow(new IllegalStateException("boom"));
    initResource();

    Response response = resource.getReconciliation(duosUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
  }

  private static MatchMigrationPopulation population() {
    return new MatchMigrationPopulation(409, 405, 409, 0, 0, 409, 405, 0, 4, 0, true);
  }

  private static SnapshotReconciliation reconciliation() {
    return new SnapshotReconciliation(409, 409, 0, 0, 405, 405, 0, 0, true);
  }
}
