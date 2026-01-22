package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.broadinstitute.consent.http.models.FeatureFlag;
import org.junit.jupiter.api.Test;

class FeatureFlagDAOTest extends DAOTestHelper {

  @Test
  void testInsertAndFindById() {
    String id = "test-feature-" + randomAlphanumeric(10);
    String value = "test-value";
    featureFlagDAO.insert(id, value);

    FeatureFlag flag = featureFlagDAO.findById(id);
    assertNotNull(flag);
    assertEquals(id, flag.getId());
    assertEquals(value, flag.getValue());
    assertNotNull(flag.getCreateDate());
    assertNotNull(flag.getUpdateDate());
  }

  @Test
  void testFindById_NotFound() {
    String id = "non-existent-" + randomAlphanumeric(10);
    FeatureFlag flag = featureFlagDAO.findById(id);
    assertNull(flag);
  }

  @Test
  void testFindAll() {
    String id1 = "feature-1-" + randomAlphanumeric(10);
    String id2 = "feature-2-" + randomAlphanumeric(10);
    featureFlagDAO.insert(id1, "value1");
    featureFlagDAO.insert(id2, "value2");

    List<FeatureFlag> flags = featureFlagDAO.findAll();
    assertNotNull(flags);
    assertTrue(flags.size() >= 2);
    assertTrue(flags.stream().anyMatch(f -> f.getId().equals(id1)));
    assertTrue(flags.stream().anyMatch(f -> f.getId().equals(id2)));
  }

  @Test
  void testUpdate() {
    String id = "update-test-" + randomAlphanumeric(10);
    String originalValue = "original";
    String updatedValue = "updated";

    featureFlagDAO.insert(id, originalValue);
    FeatureFlag flag = featureFlagDAO.findById(id);
    assertEquals(originalValue, flag.getValue());

    featureFlagDAO.update(id, updatedValue);
    FeatureFlag updatedFlag = featureFlagDAO.findById(id);
    assertEquals(updatedValue, updatedFlag.getValue());
    assertTrue(
        updatedFlag.getUpdateDate().isAfter(flag.getUpdateDate())
            || updatedFlag.getUpdateDate().equals(flag.getUpdateDate()));
  }

  @Test
  void testDeleteById() {
    String id = "delete-test-" + randomAlphanumeric(10);
    featureFlagDAO.insert(id, "value");

    assertTrue(featureFlagDAO.exists(id));
    featureFlagDAO.deleteById(id);
    assertFalse(featureFlagDAO.exists(id));
    assertNull(featureFlagDAO.findById(id));
  }

  @Test
  void testExists() {
    String id = "exists-test-" + randomAlphanumeric(10);
    assertFalse(featureFlagDAO.exists(id));

    featureFlagDAO.insert(id, "value");
    assertTrue(featureFlagDAO.exists(id));

    featureFlagDAO.deleteById(id);
    assertFalse(featureFlagDAO.exists(id));
  }

  @Test
  void testMultipleOperations() {
    String id = "multi-test-" + randomAlphanumeric(10);

    // Insert
    featureFlagDAO.insert(id, "initial");
    assertTrue(featureFlagDAO.exists(id));

    // Update multiple times
    featureFlagDAO.update(id, "updated1");
    assertEquals("updated1", featureFlagDAO.findById(id).getValue());

    featureFlagDAO.update(id, "updated2");
    assertEquals("updated2", featureFlagDAO.findById(id).getValue());

    // Delete
    featureFlagDAO.deleteById(id);
    assertFalse(featureFlagDAO.exists(id));
  }

  @Test
  void testFindAllOrdering() {
    String id1 = "aaa-" + randomAlphanumeric(10);
    String id2 = "zzz-" + randomAlphanumeric(10);
    String id3 = "mmm-" + randomAlphanumeric(10);

    featureFlagDAO.insert(id2, "value2");
    featureFlagDAO.insert(id1, "value1");
    featureFlagDAO.insert(id3, "value3");

    List<FeatureFlag> flags = featureFlagDAO.findAll();
    assertNotNull(flags);

    // Verify ordering (should be alphabetical by id)
    int idx1 = -1, idx2 = -1, idx3 = -1;
    for (int i = 0; i < flags.size(); i++) {
      if (flags.get(i).getId().equals(id1)) idx1 = i;
      if (flags.get(i).getId().equals(id2)) idx2 = i;
      if (flags.get(i).getId().equals(id3)) idx3 = i;
    }
    assertTrue(idx1 < idx3);
    assertTrue(idx3 < idx2);
  }
}
