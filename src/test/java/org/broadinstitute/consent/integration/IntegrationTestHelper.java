package org.broadinstitute.consent.integration;

import java.util.Optional;
import java.util.function.Predicate;

public interface IntegrationTestHelper {

  /**
   * Integration tests can pass in an alternative url to test against. By default, we'll test
   * against develop.
   * @return Base URL string: `baseUrl`
   */
  default String getBaseUrl() {
    String baseUrl = System.getenv("baseUrl");
    return Optional.ofNullable(baseUrl)
        .filter(Predicate.not(String::isBlank))
        .orElse("https://consent.dsde-dev.broadinstitute.org/");
  }

}
