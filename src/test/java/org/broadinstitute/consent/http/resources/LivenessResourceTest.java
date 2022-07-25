package org.broadinstitute.consent.http.resources;

import org.junit.Test;
import javax.ws.rs.core.Response;
import static org.junit.Assert.assertEquals;

public class LivenessResourceTest {
  @Test
  public void testHealthy() {
    LivenessResource resource = new LivenessResource();
    Response response = resource.healthCheck();
    assertEquals(200, response.getStatus());
  }
}
