package org.broadinstitute.consent.http.configurations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StoreConfigurationTest {

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

  private static StoreConfiguration withLimit(long maxUploadBytes) {
    StoreConfiguration config = new StoreConfiguration();
    config.setEndpoint("https://storage.googleapis.com/");
    config.setBucket("bucket");
    config.setMaxUploadBytes(maxUploadBytes);
    return config;
  }

  // The default and the bounds are accepted: 25 MB, 1 byte, 2 GB.
  @ParameterizedTest
  @ValueSource(longs = {25_000_000L, 1L, 2_000_000_000L})
  void testValidUploadLimits(long maxUploadBytes) {
    assertTrue(validator.validate(withLimit(maxUploadBytes)).isEmpty());
  }

  // A limit of zero, below it, or past what one array can hold doesn't start the app.
  @ParameterizedTest
  @ValueSource(longs = {0L, -1L, 2_000_000_001L, Long.MAX_VALUE})
  void testInvalidUploadLimits(long maxUploadBytes) {
    assertEquals(1, validator.validate(withLimit(maxUploadBytes)).size());
  }
}
