package org.broadinstitute.consent.http.resources;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class WhitelistResourceTest {

    @Mock
    private static GCSStore gcsStore;

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addProvider(MultiPartFeature.class)
            .addResource(new WhitelistResource(gcsStore))
            .build();

    @Before
    public void setup () {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPostWhiteList() throws Exception {
        FormDataMultiPart multiPart = new FormDataMultiPart()
                .field("fileData", "Hello Multipart");
        Response response = resources.
                target("api/whitelist").
                register(MultiPartFeature.class).
                request().
                post(Entity.entity(multiPart, multiPart.getMediaType()));
        String results = response.readEntity(String.class);

        assertEquals(200, response.getStatus());
        assertEquals("Hello Multipart", results);
    }

}
