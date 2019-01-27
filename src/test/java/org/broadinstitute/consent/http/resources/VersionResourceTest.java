package org.broadinstitute.consent.http.resources;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;

public class VersionResourceTest {

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new VersionResource())
            .build();


    @Test
    public void testGetVersion() {
        Response response = resources.target("/version").request().get();
        Assert.assertEquals(response.getStatus(), 200);
    }

}
