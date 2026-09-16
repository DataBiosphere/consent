package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.JsonSyntaxException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.StudyAssetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudyAssetResourceTest extends AbstractTestHelper {

  @Mock private StudyAssetService service;
  @Mock private DuosUser duosUser;

  private final User user = new User();

  private StudyAssetResource resource;

  @BeforeEach
  void setUp() {
    when(duosUser.getUser()).thenReturn(user);
    resource = new StudyAssetResource(service);
  }

  @Test
  void testPublications() {
    when(service.getAssetsByType(1, user, "publications")).thenReturn(List.of("pub"));

    Response response = resource.publications(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(List.of("pub"), response.getEntity());
  }

  @Test
  void testPublicationsNotFound() {
    when(service.getAssetsByType(1, user, "publications")).thenThrow(new NotFoundException());

    Response response = resource.publications(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testListEndpointsDelegateByAssetKey() {
    when(service.getAssetsByType(1, user, "models")).thenReturn(List.of("model"));
    when(service.getAssetsByType(1, user, "workspaces")).thenReturn(List.of("workspace"));
    when(service.getAssetsByType(1, user, "presentations")).thenReturn(List.of("presentation"));
    when(service.getAssetsByType(1, user, "clinicalTrials")).thenReturn(List.of("trial"));
    when(service.getAssetsByType(1, user, "intellectualProperties")).thenReturn(List.of("ip"));
    when(service.getAssetsByType(1, user, "funding")).thenReturn(List.of("grant"));

    assertEquals(List.of("model"), resource.models(duosUser, 1).getEntity());
    assertEquals(List.of("workspace"), resource.workspaces(duosUser, 1).getEntity());
    assertEquals(List.of("presentation"), resource.presentations(duosUser, 1).getEntity());
    assertEquals(List.of("trial"), resource.clinicalTrials(duosUser, 1).getEntity());
    assertEquals(List.of("ip"), resource.intellectualProperty(duosUser, 1).getEntity());
    assertEquals(List.of("grant"), resource.fundingResources(duosUser, 1).getEntity());
  }

  /**
   * Every endpoint on this resource reports errors the same way, through createExceptionResponse.
   */
  @Test
  void testListEndpointsReturnErrorResponsesRatherThanThrowing() {
    when(service.getAssetsByType(1, user, "models")).thenThrow(new NotFoundException());
    when(service.getAssetsByType(1, user, "funding"))
        .thenThrow(new JsonSyntaxException("malformed assets"));

    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, resource.models(duosUser, 1).getStatus());
    assertEquals(
        HttpStatusCodes.STATUS_CODE_BAD_REQUEST,
        resource.fundingResources(duosUser, 1).getStatus());
  }
}
