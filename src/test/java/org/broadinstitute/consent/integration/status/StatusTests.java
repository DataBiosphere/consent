package org.broadinstitute.consent.integration.status;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.integration.ContainerTests;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StatusTests extends ContainerTests {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://localhost:%d/status",
        "http://localhost:%d/liveness",
        "http://localhost:%d/version"
      })
  void testStatusPaths(String path) {
    Response response =
        getClient().target(String.format(path, APPLICATION.getLocalPort())).request().get();
    logWarn(response.readEntity(String.class));
    assertEquals(200, response.getStatus());
  }
}
