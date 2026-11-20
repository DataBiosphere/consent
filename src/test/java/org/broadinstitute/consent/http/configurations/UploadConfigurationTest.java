package org.broadinstitute.consent.http.configurations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

class UploadConfigurationTest {

  @Test
  void testTheDefaultRequestLimitIs100Megabytes() {
    assertEquals(100_000_000L, new UploadConfiguration().getMaxRequestBytes());
  }

  @Test
  void testTheRequestLimitMustBePositive() {
    UploadConfiguration config = new UploadConfiguration();
    config.setMaxRequestBytes(0);
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      assertEquals(1, factory.getValidator().validate(config).size());
      config.setMaxRequestBytes(1);
      assertTrue(factory.getValidator().validate(config).isEmpty());
    }
  }
}
