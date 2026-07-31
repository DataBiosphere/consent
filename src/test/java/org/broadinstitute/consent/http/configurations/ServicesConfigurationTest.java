package org.broadinstitute.consent.http.configurations;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ServicesConfigurationTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://services.example.org",
        "https://services.example.org/",
        "https://services.example.org///",
        "  https://services.example.org///  "
      })
  void testServiceBaseUrlNormalization(String configuredBaseUrl) {
    ServicesConfiguration configuration = new ServicesConfiguration();
    configuration.setLocalURL(configuredBaseUrl);
    configuration.setSamUrl(configuredBaseUrl);
    configuration.setEcmUrl(configuredBaseUrl);

    String baseUrl = "https://services.example.org/";
    assertAll(
        () -> assertEquals(baseUrl, configuration.getLocalURL()),
        () -> assertEquals(baseUrl, configuration.getSamUrl()),
        () -> assertEquals(baseUrl, configuration.getEcmUrl()),
        () -> assertEquals(baseUrl + "api/oauth/v1/ras", configuration.getEcmRasProviderUrl()),
        () -> assertEquals(baseUrl + "status", configuration.getEcmStatusUrl()),
        () ->
            assertEquals(
                baseUrl + "api/config/v1/resourceTypes", configuration.getV1ResourceTypesUrl()),
        () -> assertEquals(baseUrl + "status", configuration.getSamStatusUrl()),
        () ->
            assertEquals(
                baseUrl + "register/user/v2/self/info",
                configuration.getRegisterUserV2SelfInfoUrl()),
        () ->
            assertEquals(
                baseUrl + "register/user/v2/self/diagnostics",
                configuration.getV2SelfDiagnosticsUrl()),
        () ->
            assertEquals(
                baseUrl + "register/user/v2/self", configuration.postRegisterUserV2SelfUrl()),
        () ->
            assertEquals(
                baseUrl + "api/users/v2/self/combinedState", configuration.getCombinedStateUrl()),
        () -> assertEquals(baseUrl + "termsOfService/v1/docs", configuration.getToSTextUrl()),
        () ->
            assertEquals(
                baseUrl + "api/termsOfService/v1/user/self", configuration.getSelfTosUrl()),
        () ->
            assertEquals(
                baseUrl + "api/termsOfService/v1/user/self/accept", configuration.acceptTosUrl()),
        () ->
            assertEquals(
                baseUrl + "api/termsOfService/v1/user/self/reject", configuration.rejectTosUrl()),
        () ->
            assertEquals(
                baseUrl + "api/users/v1/user%2Btag%40example.org",
                configuration.getV1UserUrl("user+tag@example.org")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "\t\n"})
  void testBlankServiceBaseUrlsAreRejected(String configuredBaseUrl) {
    ServicesConfiguration configuration = new ServicesConfiguration();

    assertAll(
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> configuration.setLocalURL(configuredBaseUrl)),
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> configuration.setSamUrl(configuredBaseUrl)),
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> configuration.setEcmUrl(configuredBaseUrl)));
  }
}
