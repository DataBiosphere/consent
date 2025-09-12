package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DatasetMetrics;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsResourceTest {

  @Mock
  private MetricsService service;

  private MetricsResource resource;

  private final AuthUser authUser = new AuthUser("test");
  private final List<UserRole> roles = List.of(UserRoles.Researcher());
  private final User user = new User(1, authUser.getEmail(), "Display Name", new Date(), roles);

  private final DuosUser duosUser = new DuosUser(authUser, user);

  @BeforeEach
  void initResource() {
    resource = new MetricsResource(service);
  }

  @Test
  void testGetDatasetMetricsData() {
    DatasetMetrics metrics = new DatasetMetrics();
    when(service.generateDatasetMetrics(any())).thenReturn(metrics);

    Response response = resource.getDatasetMetricsData(duosUser, 1);
    assertEquals(200, response.getStatus());
    assertFalse(response.getEntity().toString().isEmpty());
  }

  @Test
  void testGetDatasetMetricsDataNotFound() {
    when(service.generateDatasetMetrics(any())).thenThrow(new NotFoundException());

    Response response = resource.getDatasetMetricsData(duosUser, 1);
    assertEquals(404, response.getStatus());
  }
}
