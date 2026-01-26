package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.FeatureFlag;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.FeatureFlagService;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeatureFlagResourceTest extends AbstractTestHelper {

  @Mock private FeatureFlagService featureFlagService;
  @Mock private UserService userService;

  private FeatureFlagResource resource;
  private final AuthUser authUser = new AuthUser("admin@test.com");
  private final User user = new User(1, "admin@test.com", "Admin", new java.util.Date());

  @BeforeEach
  void setUp() {
    resource = new FeatureFlagResource(featureFlagService, userService);
  }

  @Test
  void testGetAllFeatureFlags() {
    FeatureFlag flag1 = new FeatureFlag("feature1", "value1");
    FeatureFlag flag2 = new FeatureFlag("feature2", "value2");
    when(featureFlagService.getAllFeatureFlags()).thenReturn(List.of(flag1, flag2));

    Response response = resource.getAllFeatureFlags();
    assertEquals(200, response.getStatus());
    assertNotNull(response.getEntity());
    verify(featureFlagService).getAllFeatureFlags();
  }

  @Test
  void testGetAllFeatureFlags_Empty() {
    when(featureFlagService.getAllFeatureFlags()).thenReturn(List.of());

    Response response = resource.getAllFeatureFlags();
    assertEquals(200, response.getStatus());
    verify(featureFlagService).getAllFeatureFlags();
  }

  @Test
  void testGetFeatureFlagById_Found() {
    FeatureFlag flag = new FeatureFlag("test-feature", "test-value");
    when(featureFlagService.getFeatureFlagById("test-feature")).thenReturn(flag);

    Response response = resource.getFeatureFlagById("test-feature");
    assertEquals(200, response.getStatus());
    assertNotNull(response.getEntity());
    verify(featureFlagService).getFeatureFlagById("test-feature");
  }

  @Test
  void testGetFeatureFlagById_NotFound() {
    when(featureFlagService.getFeatureFlagById("non-existent"))
        .thenThrow(new NotFoundException("Feature flag with id 'non-existent' not found"));

    Response response = resource.getFeatureFlagById("non-existent");
    assertEquals(404, response.getStatus());
    verify(featureFlagService).getFeatureFlagById("non-existent");
  }

  @Test
  void testCreateOrUpdateFeatureFlag_Create() {
    UriInfo uriInfo = mock(UriInfo.class);
    UriBuilder uriBuilder = mock(UriBuilder.class);
    when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
    when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
    when(uriBuilder.build()).thenReturn(URI.create("http://localhost/api/feature/new-feature"));

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    FeatureFlag createdFlag = new FeatureFlag("new-feature", "new-value");
    when(featureFlagService.exists("new-feature")).thenReturn(false);
    when(featureFlagService.createOrUpdateFeatureFlag("new-feature", "new-value", user.getUserId()))
        .thenReturn(createdFlag);

    Map<String, String> body = Map.of("value", "new-value");
    Response response = resource.createOrUpdateFeatureFlag(uriInfo, authUser, "new-feature", body);

    assertEquals(201, response.getStatus());
    assertNotNull(response.getEntity());
    verify(userService).findUserByEmail(authUser.getEmail());
    verify(featureFlagService).exists("new-feature");
    verify(featureFlagService)
        .createOrUpdateFeatureFlag("new-feature", "new-value", user.getUserId());
  }

  @Test
  void testCreateOrUpdateFeatureFlag_Update() {
    UriInfo uriInfo = mock(UriInfo.class);
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    FeatureFlag updatedFlag = new FeatureFlag("existing-feature", "updated-value");
    when(featureFlagService.exists("existing-feature")).thenReturn(true);
    when(featureFlagService.createOrUpdateFeatureFlag(
            "existing-feature", "updated-value", user.getUserId()))
        .thenReturn(updatedFlag);

    Map<String, String> body = Map.of("value", "updated-value");
    Response response =
        resource.createOrUpdateFeatureFlag(uriInfo, authUser, "existing-feature", body);

    assertEquals(200, response.getStatus());
    assertNotNull(response.getEntity());
    verify(userService).findUserByEmail(authUser.getEmail());
    verify(featureFlagService).exists("existing-feature");
    verify(featureFlagService)
        .createOrUpdateFeatureFlag("existing-feature", "updated-value", user.getUserId());
  }

  @Test
  void testCreateOrUpdateFeatureFlag_MissingValue() {
    UriInfo uriInfo = mock(UriInfo.class);
    Map<String, String> body = Map.of();

    Response response = resource.createOrUpdateFeatureFlag(uriInfo, authUser, "test-id", body);

    assertEquals(400, response.getStatus());
    assertNotNull(response.getEntity());
  }

  @Test
  void testCreateOrUpdateFeatureFlag_ServiceError() {
    UriInfo uriInfo = mock(UriInfo.class);
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(featureFlagService.exists("test-id")).thenReturn(false);
    when(featureFlagService.createOrUpdateFeatureFlag("test-id", "value", user.getUserId()))
        .thenThrow(new RuntimeException("Database error"));

    Map<String, String> body = Map.of("value", "value");
    Response response = resource.createOrUpdateFeatureFlag(uriInfo, authUser, "test-id", body);

    assertEquals(500, response.getStatus());
    verify(userService).findUserByEmail(authUser.getEmail());
    verify(featureFlagService).exists("test-id");
    verify(featureFlagService).createOrUpdateFeatureFlag("test-id", "value", user.getUserId());
  }

  @Test
  void testDeleteFeatureFlag_Success() {
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    doNothing().when(featureFlagService).deleteFeatureFlag("test-id", user.getUserId());

    Response response = resource.deleteFeatureFlag(authUser, "test-id");

    assertEquals(204, response.getStatus());
    verify(userService).findUserByEmail(authUser.getEmail());
    verify(featureFlagService).deleteFeatureFlag("test-id", user.getUserId());
  }

  @Test
  void testDeleteFeatureFlag_NotFound() {
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    doThrow(new NotFoundException("Feature flag with id 'non-existent' not found"))
        .when(featureFlagService)
        .deleteFeatureFlag("non-existent", user.getUserId());

    Response response = resource.deleteFeatureFlag(authUser, "non-existent");

    assertEquals(404, response.getStatus());
    verify(userService).findUserByEmail(authUser.getEmail());
    verify(featureFlagService).deleteFeatureFlag("non-existent", user.getUserId());
  }

  @Test
  void testDeleteFeatureFlag_ServiceError() {
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    doThrow(new RuntimeException("Database error"))
        .when(featureFlagService)
        .deleteFeatureFlag("test-id", user.getUserId());

    Response response = resource.deleteFeatureFlag(authUser, "test-id");

    assertEquals(500, response.getStatus());
    verify(userService).findUserByEmail(authUser.getEmail());
    verify(featureFlagService).deleteFeatureFlag("test-id", user.getUserId());
  }
}
