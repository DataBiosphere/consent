package org.broadinstitute.consent.http.resources;

import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LivenessResourceTest {
    @Test
    public void testHealthy() {
        LivenessResource resource = new LivenessResource();
        Response response = resource.healthCheck();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("Healthy!", response.getEntity());
    }
}
