package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import org.broadinstitute.consent.http.enumeration.AssetType;
import org.junit.jupiter.api.Test;

class AssetCatalogEntryTest {

  @Test
  void testALinkedUnstampedEntryIsNotRemoved() {
    assertFalse(entry(null, 7).isRemoved());
  }

  @Test
  void testAStampedEntryIsRemoved() {
    assertTrue(entry(Timestamp.from(Instant.now()), null).isRemoved());
  }

  @Test
  void testAnUnlinkedUnstampedEntryIsRemoved() {
    assertTrue(entry(null, null).isRemoved());
  }

  private static AssetCatalogEntry entry(Timestamp removed, Integer modelId) {
    return new AssetCatalogEntry(
        1, AssetType.MODEL, 7, null, removed, null, null, null, modelId, null, null);
  }
}
