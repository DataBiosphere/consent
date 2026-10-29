package org.broadinstitute.consent.http.configurations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.jackson.Jackson;
import org.junit.jupiter.api.Test;

class ShowcaseConfigurationTest {

  private final ObjectMapper mapper = Jackson.newObjectMapper(new YAMLFactory());

  @Test
  void testOmittedBlockDefaultsToOff() throws Exception {
    ConsentConfiguration config =
        mapper.readValue("rateLimit:\n  enabled: true\n", ConsentConfiguration.class);

    assertFalse(config.getShowcaseConfiguration().isPublicLookupEnabled());
  }

  @Test
  void testOmittedFlagDefaultsToOff() throws Exception {
    ConsentConfiguration config = mapper.readValue("showcase: {}\n", ConsentConfiguration.class);

    assertFalse(config.getShowcaseConfiguration().isPublicLookupEnabled());
  }

  @Test
  void testFlagCanBeEnabled() throws Exception {
    ConsentConfiguration config =
        mapper.readValue("showcase:\n  publicLookupEnabled: true\n", ConsentConfiguration.class);

    assertTrue(config.getShowcaseConfiguration().isPublicLookupEnabled());
  }
}
