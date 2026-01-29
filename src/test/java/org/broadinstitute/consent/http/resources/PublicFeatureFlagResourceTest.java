package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.models.FeatureFlag;
import org.broadinstitute.consent.http.service.FeatureFlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicFeatureFlagResourceTest extends AbstractTestHelper {

  @Mock private FeatureFlagService featureFlagService;

  private PublicFeatureFlagResource resource;

  @BeforeEach
  void setUp() {
    resource = new PublicFeatureFlagResource(featureFlagService);
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
}
