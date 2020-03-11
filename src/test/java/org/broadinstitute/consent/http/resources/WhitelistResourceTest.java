package org.broadinstitute.consent.http.resources;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class WhitelistResourceTest {

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new WhitelistResource())
            .build();

    @Test
    public void testPostWhiteList() {
        Response response = resources.
                target("api/whitelist").
                request().
                post(null);
        assertEquals(200, response.getStatus());
    }

}
