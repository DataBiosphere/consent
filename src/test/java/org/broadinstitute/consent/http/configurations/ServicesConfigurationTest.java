package org.broadinstitute.consent.http.configurations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ServicesConfigurationTest {

  @ParameterizedTest
  @CsvSource({
    "https://externalcreds.example.org, https://externalcreds.example.org/status",
    "https://externalcreds.example.org/, https://externalcreds.example.org/status",
    "https://externalcreds.example.org///, https://externalcreds.example.org/status"
  })
  void testGetEcmStatusUrl(String ecmUrl, String expectedStatusUrl) {
    ServicesConfiguration configuration = new ServicesConfiguration();
    configuration.setEcmUrl(ecmUrl);

    assertEquals(expectedStatusUrl, configuration.getEcmStatusUrl());
  }
}
