package org.broadinstitute.consent.http.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CountryValidatorTest {
  private final CountryValidator countryValidator = new CountryValidator();

  @Test
  void testIsInCountryListInvalid() {
    assertFalse(countryValidator.isInCountryList("US"));
  }

  @Test
  void testIsInCountryListValid() {
    assertTrue(countryValidator.isInCountryList("United States of America (the)"));
  }

}
