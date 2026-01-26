package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.FeatureFlagDAO;
import org.broadinstitute.consent.http.models.FeatureFlag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest extends AbstractTestHelper {

  @Mock private FeatureFlagDAO featureFlagDAO;

  private FeatureFlagService service;

  @BeforeEach
  void setUp() {
    service = new FeatureFlagService(featureFlagDAO);
  }

  @Test
  void testGetAllFeatureFlags() {
    FeatureFlag flag1 = new FeatureFlag("feature1", "value1");
    FeatureFlag flag2 = new FeatureFlag("feature2", "value2");
    when(featureFlagDAO.findAll()).thenReturn(java.util.List.of(flag1, flag2));

    var flags = service.getAllFeatureFlags();
    assertNotNull(flags);
    assertEquals(2, flags.size());
    verify(featureFlagDAO).findAll();
  }

  @Test
  void testGetFeatureFlagById_Found() {
    FeatureFlag flag = new FeatureFlag("test-id", "test-value");
    when(featureFlagDAO.findById("test-id")).thenReturn(flag);

    FeatureFlag result = service.getFeatureFlagById("test-id");
    assertNotNull(result);
    assertEquals("test-id", result.getId());
    assertEquals("test-value", result.getValue());
    verify(featureFlagDAO).findById("test-id");
  }

  @Test
  void testGetFeatureFlagById_NotFound() {
    when(featureFlagDAO.findById("non-existent")).thenReturn(null);

    assertThrows(NotFoundException.class, () -> service.getFeatureFlagById("non-existent"));
    verify(featureFlagDAO).findById("non-existent");
  }

  @Test
  void testGetFeatureFlagValue_Found() {
    FeatureFlag flag = new FeatureFlag("test-id", "test-value");
    when(featureFlagDAO.findById("test-id")).thenReturn(flag);

    String value = service.getFeatureFlagValue("test-id");
    assertEquals("test-value", value);
    verify(featureFlagDAO).findById("test-id");
  }

  @Test
  void testGetFeatureFlagValue_NotFound() {
    when(featureFlagDAO.findById("non-existent")).thenReturn(null);

    String value = service.getFeatureFlagValue("non-existent");
    assertNull(value);
    verify(featureFlagDAO).findById("non-existent");
  }

  @Test
  void testGetFeatureFlagValue_WithDefault_Found() {
    FeatureFlag flag = new FeatureFlag("test-id", "test-value");
    when(featureFlagDAO.findById("test-id")).thenReturn(flag);

    String value = service.getFeatureFlagValue("test-id", "default");
    assertEquals("test-value", value);
    verify(featureFlagDAO).findById("test-id");
  }

  @Test
  void testGetFeatureFlagValue_WithDefault_NotFound() {
    when(featureFlagDAO.findById("non-existent")).thenReturn(null);

    String value = service.getFeatureFlagValue("non-existent", "default-value");
    assertEquals("default-value", value);
    verify(featureFlagDAO).findById("non-existent");
  }

  @Test
  void testIsFeatureEnabled_True() {
    FeatureFlag flag = new FeatureFlag("enabled-feature", "true");
    when(featureFlagDAO.findById("enabled-feature")).thenReturn(flag);

    boolean enabled = service.isFeatureEnabled("enabled-feature");
    assertTrue(enabled);
    verify(featureFlagDAO).findById("enabled-feature");
  }

  @Test
  void testIsFeatureEnabled_False() {
    FeatureFlag flag = new FeatureFlag("disabled-feature", "false");
    when(featureFlagDAO.findById("disabled-feature")).thenReturn(flag);

    boolean enabled = service.isFeatureEnabled("disabled-feature");
    assertFalse(enabled);
    verify(featureFlagDAO).findById("disabled-feature");
  }

  @Test
  void testIsFeatureEnabled_NotFound() {
    when(featureFlagDAO.findById("non-existent")).thenReturn(null);

    boolean enabled = service.isFeatureEnabled("non-existent");
    assertFalse(enabled);
    verify(featureFlagDAO).findById("non-existent");
  }

  @Test
  void testIsFeatureEnabled_CaseInsensitive() {
    FeatureFlag flag = new FeatureFlag("feature", "TRUE");
    when(featureFlagDAO.findById("feature")).thenReturn(flag);

    boolean enabled = service.isFeatureEnabled("feature");
    assertTrue(enabled);
  }

  @Test
  void testCreateOrUpdateFeatureFlag_Create() {
    Integer userId = 1;
    FeatureFlag createdFlag = new FeatureFlag("new-feature", "new-value");
    when(featureFlagDAO.exists("new-feature")).thenReturn(false);
    doNothing().when(featureFlagDAO).insert(anyString(), anyString());
    doNothing().when(featureFlagDAO).insertAudit(userId, "new-feature", "CREATE");
    when(featureFlagDAO.findById("new-feature")).thenReturn(createdFlag);

    FeatureFlag result = service.createOrUpdateFeatureFlag("new-feature", "new-value", userId);
    assertNotNull(result);
    assertEquals("new-feature", result.getId());
    assertEquals("new-value", result.getValue());
    verify(featureFlagDAO).exists("new-feature");
    verify(featureFlagDAO).insert("new-feature", "new-value");
    verify(featureFlagDAO).insertAudit(userId, "new-feature", "CREATE");
    verify(featureFlagDAO, never()).update(anyString(), anyString());
    verify(featureFlagDAO).findById("new-feature");
  }

  @Test
  void testCreateOrUpdateFeatureFlag_Update() {
    Integer userId = 1;
    FeatureFlag updatedFlag = new FeatureFlag("existing-feature", "updated-value");
    when(featureFlagDAO.exists("existing-feature")).thenReturn(true);
    doNothing().when(featureFlagDAO).update(anyString(), anyString());
    doNothing().when(featureFlagDAO).insertAudit(userId, "existing-feature", "UPDATE");
    when(featureFlagDAO.findById("existing-feature")).thenReturn(updatedFlag);

    FeatureFlag result =
        service.createOrUpdateFeatureFlag("existing-feature", "updated-value", userId);
    assertNotNull(result);
    assertEquals("existing-feature", result.getId());
    assertEquals("updated-value", result.getValue());
    verify(featureFlagDAO).exists("existing-feature");
    verify(featureFlagDAO).update("existing-feature", "updated-value");
    verify(featureFlagDAO).insertAudit(userId, "existing-feature", "UPDATE");
    verify(featureFlagDAO, never()).insert(anyString(), anyString());
    verify(featureFlagDAO).findById("existing-feature");
  }

  @Test
  void testCreateOrUpdateFeatureFlag_EmptyId() {
    assertThrows(
        IllegalArgumentException.class, () -> service.createOrUpdateFeatureFlag("", "value", 1));
    assertThrows(
        IllegalArgumentException.class, () -> service.createOrUpdateFeatureFlag("   ", "value", 1));
    assertThrows(
        IllegalArgumentException.class, () -> service.createOrUpdateFeatureFlag(null, "value", 1));
  }

  @Test
  void testCreateOrUpdateFeatureFlag_NullValue() {
    assertThrows(
        IllegalArgumentException.class, () -> service.createOrUpdateFeatureFlag("id", null, 1));
  }

  @Test
  void testDeleteFeatureFlag_Success() {
    Integer userId = 1;
    when(featureFlagDAO.exists("test-id")).thenReturn(true);
    doNothing().when(featureFlagDAO).deleteById("test-id");
    doNothing().when(featureFlagDAO).insertAudit(userId, "test-id", "DELETE");

    service.deleteFeatureFlag("test-id", userId);
    verify(featureFlagDAO).exists("test-id");
    verify(featureFlagDAO).deleteById("test-id");
    verify(featureFlagDAO).insertAudit(userId, "test-id", "DELETE");
  }

  @Test
  void testDeleteFeatureFlag_NotFound() {
    when(featureFlagDAO.exists("non-existent")).thenReturn(false);

    assertThrows(NotFoundException.class, () -> service.deleteFeatureFlag("non-existent", 1));
    verify(featureFlagDAO).exists("non-existent");
    verify(featureFlagDAO, never()).deleteById(anyString());
    verify(featureFlagDAO, never()).insertAudit(any(), anyString(), anyString());
  }

  @Test
  void testExists_True() {
    when(featureFlagDAO.exists("test-id")).thenReturn(true);

    boolean exists = service.exists("test-id");
    assertTrue(exists);
    verify(featureFlagDAO).exists("test-id");
  }

  @Test
  void testExists_False() {
    when(featureFlagDAO.exists("test-id")).thenReturn(false);

    boolean exists = service.exists("test-id");
    assertFalse(exists);
    verify(featureFlagDAO).exists("test-id");
  }
}
