package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.broadinstitute.consent.http.enumeration.AssetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AssetIdentifierTest {

  @ParameterizedTest
  @CsvSource({
    "DATASET, 0, DUOS-000000",
    "DATASET, 123, DUOS-000123",
    "DATASET, 1234567, DUOS-1234567",
    "MODEL, 45, DUOS-M-000045",
    "WORKSPACE, 12, DUOS-W-000012",
    "BIOSPECIMEN, 301, DUOS-B-000301",
    "BIOSPECIMEN, 2147483647, DUOS-B-2147483647"
  })
  void testFormatAndParseRoundTrip(AssetType type, int number, String identifier) {
    assertEquals(identifier, AssetIdentifier.format(type, number));
    assertEquals(new AssetIdentifier(type, number), AssetIdentifier.parse(identifier));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        " ",
        "duos-000123",
        "DUOS-m-000045",
        "DUOS-M-45",
        "DUOS-123",
        "DUOS-0000123",
        "DUOS-M-0000045",
        "DUOS-D12",
        "DUOS-S12",
        "DUOS-D-000012",
        "DUOS-S-000012",
        "DUOS-X-000012",
        "DUOS--000012",
        "DUOS-+000012",
        "DUOS-M-+00012",
        "DUOS-2147483648",
        "DUOS-9999999999",
        "DUOS-000123 ",
        "XDUOS-000123",
        "DUOS-\u0660\u0660\u0660\u0661\u0662\u0663"
      })
  void testParseRejectsAnythingButTheCanonicalForm(String text) {
    assertThrows(IllegalArgumentException.class, () -> AssetIdentifier.parse(text));
  }

  @Test
  void testANegativeNumberIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.MODEL, -1));
  }

  @Test
  void testATypeIsRequired() {
    assertThrows(NullPointerException.class, () -> new AssetIdentifier(null, 1));
  }

  @ParameterizedTest
  @CsvSource({"0, DUOS-000000", "7, DUOS-000007", "999999, DUOS-999999", "1000000, DUOS-1000000"})
  void testDatasetIdentifiersAreUnchanged(int alias, String identifier) {
    Dataset dataset = new Dataset();
    dataset.setAlias(alias);
    assertEquals(identifier, dataset.getDatasetIdentifier());
    assertEquals(identifier, Dataset.parseAliasToIdentifier(alias));
  }
}
