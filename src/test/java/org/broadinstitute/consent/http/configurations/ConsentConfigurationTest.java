package org.broadinstitute.consent.http.configurations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.dropwizard.jersey.validation.Validators;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConsentConfigurationTest {

  private final Validator validator = Validators.newValidator();

  @Test
  void testTheDefaultFileLimitIsBelowTheRequestLimit() {
    assertTrue(new ConsentConfiguration().isFileLimitBelowRequestLimit());
  }

  // A file can't be larger than the request that carries it. Checked through Bean Validation, as
  // Dropwizard does at startup.
  @Test
  void testAFileLimitAtOrAboveTheRequestLimitFailsValidation() {
    ConsentConfiguration config = new ConsentConfiguration();
    config.getUploadConfiguration().setMaxRequestBytes(10_000);
    config.getCloudStoreConfiguration().setMaxUploadBytes(10_000);

    Set<ConstraintViolation<ConsentConfiguration>> violations = validator.validate(config);

    assertEquals(
        1,
        violations.stream()
            .filter(v -> v.getPropertyPath().toString().equals("fileLimitBelowRequestLimit"))
            .count());
    config.getCloudStoreConfiguration().setMaxUploadBytes(9_999);
    assertTrue(
        validator.validate(config).stream()
            .noneMatch(v -> v.getPropertyPath().toString().equals("fileLimitBelowRequestLimit")));
  }
}
