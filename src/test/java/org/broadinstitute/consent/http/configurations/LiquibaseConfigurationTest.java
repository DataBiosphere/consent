package org.broadinstitute.consent.http.configurations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.jackson.Jackson;
import org.junit.jupiter.api.Test;

class LiquibaseConfigurationTest {

  private final ObjectMapper mapper = Jackson.newObjectMapper(new YAMLFactory());

  @Test
  void testOmittedBlockDefaultsToFalse() throws Exception {
    ConsentConfiguration config =
        mapper.readValue("rateLimit:\n  enabled: true\n", ConsentConfiguration.class);

    assertFalse(config.getLiquibaseConfiguration().isRunOnStartup());
  }

  @Test
  void testOmittedFlagDefaultsToFalse() throws Exception {
    ConsentConfiguration config = mapper.readValue("liquibase: {}\n", ConsentConfiguration.class);

    assertFalse(config.getLiquibaseConfiguration().isRunOnStartup());
  }

  @Test
  void testFlagCanBeEnabled() throws Exception {
    ConsentConfiguration config =
        mapper.readValue("liquibase:\n  runOnStartup: true\n", ConsentConfiguration.class);

    assertTrue(config.getLiquibaseConfiguration().isRunOnStartup());
  }
}
