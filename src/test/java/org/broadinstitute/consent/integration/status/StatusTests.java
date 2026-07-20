package org.broadinstitute.consent.integration.status;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.integration.ContainerTests;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StatusTests extends ContainerTests {

  @ParameterizedTest
  @ValueSource(strings = {"/status", "/liveness", "/version"})
  void testStatusPaths(String path) {
    try (Response response = getClient().target(serviceUrl(path)).request().get()) {
      if (response.getStatus() != 200) {
        logWarn(response.readEntity(String.class));
      }
      assertEquals(200, response.getStatus());
    }
  }
}
