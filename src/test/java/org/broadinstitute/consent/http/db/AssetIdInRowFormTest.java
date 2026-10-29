package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AssetIdInRowFormTest extends DAOTestHelper {

  @ParameterizedTest
  @ValueSource(strings = {"7", "7.0", "\"7\"", "\"2147483647\"", "2147483647"})
  void testAssetIdInRowForm(String id) {
    assertTrue(assetIdInRowForm(id));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(
      strings = {
        "\"007\"",
        "\"00000000001\"",
        "7.5",
        "0",
        "-1",
        "2147483648",
        "\"3000000000\"",
        "\"2147483648\"",
        "\"0d9e1b8c-4c1f-4a52-9a3e-3f4f8d6a2b10\"",
        "\"\"",
        "\" 7\"",
        "null",
        "true",
        "[7]"
      })
  void testAssetIdNotInRowForm(String id) {
    assertFalse(assetIdInRowForm(id));
  }

  private static boolean assetIdInRowForm(String id) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT asset_id_in_row_form(:id::jsonb)")
                .bind("id", id)
                .mapTo(Boolean.class)
                .one());
  }
}
