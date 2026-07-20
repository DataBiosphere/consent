package org.broadinstitute.consent.http.configurations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
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
  void testRequestsPerMinuteLessThanPodCountIsInvalid() {
    Set<ConstraintViolation<RateLimitConfiguration>> violations =
        validator.validate(buildConfig(2, 3));

    assertEquals(1, violations.size());
    assertEquals(
        "requestsPerMinuteAtLeastPodCount",
        violations.iterator().next().getPropertyPath().toString());
  }
}
