package org.broadinstitute.consent.http.configurations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RateLimitConfigurationTest {

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    validatorFactory.close();
  }

  private RateLimitConfiguration buildConfig(int requestsPerMinute, int podCount) {
    RateLimitConfiguration config = new RateLimitConfiguration();
    config.setEnabled(true);
    config.setRequestsPerMinute(requestsPerMinute);
    config.setPodCount(podCount);
    return config;
  }

  @Test
  void testValidConfigurationHasNoViolations() {
    Set<ConstraintViolation<RateLimitConfiguration>> violations =
        validator.validate(buildConfig(100, 3));

    assertTrue(violations.isEmpty());
  }

  @Test
  void testEqualRequestsPerMinuteAndPodCountIsValid() {
    Set<ConstraintViolation<RateLimitConfiguration>> violations =
        validator.validate(buildConfig(3, 3));

    assertTrue(violations.isEmpty());
  }

  @Test
  void testShowcaseDefaultsAreValid() {
    RateLimitConfiguration config = buildConfig(100, 3);

    assertTrue(validator.validate(config).isEmpty());
    assertEquals(
        PublicRateLimitConfiguration.DEFAULT_REQUESTS_PER_MINUTE,
        config.getShowcase().getRequestsPerMinute());
    assertEquals(List.of("127.0.0.1/32", "::1/128"), config.getShowcase().getTrustedProxies());
  }

  @Test
  void testShowcaseRequestsPerMinuteLessThanPodCountIsInvalid() {
    RateLimitConfiguration config = buildConfig(100, 3);
    config.getShowcase().setRequestsPerMinute(2);

    Set<ConstraintViolation<RateLimitConfiguration>> violations = validator.validate(config);

    assertEquals(1, violations.size());
    assertEquals(
        "showcaseRequestsPerMinuteAtLeastPodCount",
        violations.iterator().next().getPropertyPath().toString());
  }

  @Test
  void testShowcaseTrustedProxyHostnameIsInvalid() {
    RateLimitConfiguration config = buildConfig(100, 3);
    config.getShowcase().setTrustedProxies(List.of("127.0.0.1/32", "proxy.example.org"));

    Set<ConstraintViolation<RateLimitConfiguration>> violations = validator.validate(config);

    assertEquals(1, violations.size());
    assertEquals(
        "showcase.trustedProxiesValid", violations.iterator().next().getPropertyPath().toString());
  }

  @Test
  void testRequestsPerMinuteLessThanPodCountIsInvalid() {
    Set<ConstraintViolation<RateLimitConfiguration>> violations =
        validator.validate(buildConfig(2, 3));

    assertEquals(1, violations.size());
    assertEquals(
        "requestsPerMinuteAtLeastPodCount",
        violations.iterator().next().getPropertyPath().toString());
  }

  private static final String LONG_SECRET = "test-consent-proxy-secret-0123456789abcdef";

  private Set<ConstraintViolation<RateLimitConfiguration>> validateSecrets(
      String current, String previous) {
    RateLimitConfiguration config = buildConfig(100, 3);
    config.setClientIpSecret(current);
    config.setClientIpPreviousSecret(previous);
    return validator.validate(config);
  }

  @Test
  void testClientIpSecretsAreOptional() {
    assertTrue(validateSecrets(null, null).isEmpty());
  }

  @Test
  void testCurrentAndPreviousClientIpSecretsAreValid() {
    assertTrue(validateSecrets(LONG_SECRET, LONG_SECRET + "-previous").isEmpty());
  }

  @Test
  void testShortClientIpSecretIsInvalid() {
    Set<ConstraintViolation<RateLimitConfiguration>> violations =
        validateSecrets("too-short-secret", null);

    assertEquals(1, violations.size());
    assertEquals("clientIpSecretValid", violations.iterator().next().getPropertyPath().toString());
  }

  @Test
  void testPreviousClientIpSecretWithoutCurrentIsInvalid() {
    assertEquals(1, validateSecrets(null, LONG_SECRET).size());
  }
}
