package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DacDatasetExternalizationRequestTest {

  // ── constructor validation ────────────────────────────────────────────────

  @Test
  void testConstructor_NullReason_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new DacDatasetExternalizationRequest(null, false, true, true, null));
  }

  @Test
  void testConstructor_BlankReason_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new DacDatasetExternalizationRequest("   ", false, true, true, null));
  }

  @Test
  void testConstructor_ValidReason_DoesNotThrow() {
    assertDoesNotThrow(
        () -> new DacDatasetExternalizationRequest("valid reason", false, true, true, null));
  }

  // ── isDryRun ──────────────────────────────────────────────────────────────

  @Test
  void testIsDryRun_NullDryRun_ReturnsFalse() {
    DacDatasetExternalizationRequest req =
        new DacDatasetExternalizationRequest("reason", null, true, true, null);
    assertFalse(req.isDryRun());
  }

  @Test
  void testIsDryRun_FalseDryRun_ReturnsFalse() {
    DacDatasetExternalizationRequest req =
        new DacDatasetExternalizationRequest("reason", false, true, true, null);
    assertFalse(req.isDryRun());
  }

  @Test
  void testIsDryRun_TrueDryRun_ReturnsTrue() {
    DacDatasetExternalizationRequest req =
        new DacDatasetExternalizationRequest("reason", true, true, true, null);
    assertTrue(req.isDryRun());
  }

  // ── shouldRevokeApprovedAccess ────────────────────────────────────────────

  @Test
  void testShouldRevokeApprovedAccess_NullValue_ReturnsTrue() {
    DacDatasetExternalizationRequest req =
        new DacDatasetExternalizationRequest("reason", false, null, true, null);
    assertTrue(req.shouldRevokeApprovedAccess());
  }

  @Test
  void testShouldRevokeApprovedAccess_TrueValue_ReturnsTrue() {
    DacDatasetExternalizationRequest req =
        new DacDatasetExternalizationRequest("reason", false, true, true, null);
    assertTrue(req.shouldRevokeApprovedAccess());
  }

  @Test
  void testShouldRevokeApprovedAccess_FalseValue_ReturnsFalse() {
    DacDatasetExternalizationRequest req =
        new DacDatasetExternalizationRequest("reason", false, false, true, null);
    assertFalse(req.shouldRevokeApprovedAccess());
  }

  // ── shouldCancelOpenElections ─────────────────────────────────────────────

  @Test
  void testShouldCancelOpenElections_NullValue_ReturnsTrue() {
    DacDatasetExternalizationRequest req =
        new DacDatasetExternalizationRequest("reason", false, true, null, null);
    assertTrue(req.shouldCancelOpenElections());
  }

  @Test
  void testShouldCancelOpenElections_TrueValue_ReturnsTrue() {
    DacDatasetExternalizationRequest req =
        new DacDatasetExternalizationRequest("reason", false, true, true, null);
    assertTrue(req.shouldCancelOpenElections());
  }

  @Test
  void testShouldCancelOpenElections_FalseValue_ReturnsFalse() {
    DacDatasetExternalizationRequest req =
        new DacDatasetExternalizationRequest("reason", false, true, false, null);
    assertFalse(req.shouldCancelOpenElections());
  }

  // ── shouldConvertOpenAccessDatasets ──────────────────────────────────────

  @Test
  void testShouldConvertOpenAccessDatasets_NullValue_ReturnsFalse() {
    DacDatasetExternalizationRequest req =
        new DacDatasetExternalizationRequest("reason", false, true, true, null);
    assertFalse(req.shouldConvertOpenAccessDatasets());
  }

  @Test
  void testShouldConvertOpenAccessDatasets_FalseValue_ReturnsFalse() {
    DacDatasetExternalizationRequest req =
        new DacDatasetExternalizationRequest("reason", false, true, true, false);
    assertFalse(req.shouldConvertOpenAccessDatasets());
  }

  @Test
  void testShouldConvertOpenAccessDatasets_TrueValue_ReturnsTrue() {
    DacDatasetExternalizationRequest req =
        new DacDatasetExternalizationRequest("reason", false, true, true, true);
    assertTrue(req.shouldConvertOpenAccessDatasets());
  }
}
