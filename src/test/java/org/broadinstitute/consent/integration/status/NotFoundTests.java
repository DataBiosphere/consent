package org.broadinstitute.consent.integration.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.integration.ContainerTests;
import org.junit.jupiter.api.Test;

class NotFoundTests extends ContainerTests {

  @Test
  void testUnmatchedPathReturnsJsonError() {
    try (Response response = getClient().target(serviceUrl("/where/am/i")).request().get()) {
      assertEquals(404, response.getStatus());
      assertTrue(response.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE));
      String body = response.readEntity(String.class);
      assertTrue(body.contains("/where/am/i"));
    }
  }
}
