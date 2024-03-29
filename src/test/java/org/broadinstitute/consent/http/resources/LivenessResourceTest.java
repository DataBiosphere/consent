package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

public class LivenessResourceTest {

  @Test
  public void testHealthy() {
    LivenessResource resource = new LivenessResource();
    Response response = resource.healthCheck();
    assertEquals(200, response.getStatus());
    assertEquals("Healthy!", response.getEntity());
  }
}
