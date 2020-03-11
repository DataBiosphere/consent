package org.broadinstitute.consent.http.resources;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class WhitelistResourceTest {

    @Mock
    private static GCSStore gcsStore;

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new WhitelistResource(gcsStore))
            .build();

    @Before
    public void setup () {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPostWhiteList() {
        Response response = resources.
                target("api/whitelist").
                request().
                post(null);
        assertEquals(200, response.getStatus());
    }

}
