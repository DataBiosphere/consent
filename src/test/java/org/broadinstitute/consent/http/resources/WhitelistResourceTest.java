package org.broadinstitute.consent.http.resources;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class WhitelistResourceTest extends ResourceTestHelper {

    @Mock
    private static GCSStore gcsStore;

    @ClassRule
    public static final ResourceTestRule resources = buildRule(new WhitelistResource(gcsStore));

    @Before
    public void setup () {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPostWhitelist() {
        FormDataMultiPart multiPart = new FormDataMultiPart()
                .field("fileData", "Hello Multipart");
        Response response = resources.
                target("api/whitelist").
                register(MultiPartFeature.class).
                request().
                // Here is the auth trick. Pass in the UserRole name as the bearer token and the auth
                // framework will look for that value when it checks `@RolesAllowed`
                header(HttpHeaders.AUTHORIZATION, getRoleAuthorizationToken(UserRoles.ADMIN)).
                post(Entity.entity(multiPart, multiPart.getMediaType()));
        String results = response.readEntity(String.class);
        assertEquals(200, response.getStatus());
        assertEquals("Hello Multipart", results);
    }

    @Test
    public void testPostWhitelistNotAllowed() {
        FormDataMultiPart multiPart = new FormDataMultiPart()
                .field("fileData", "Hello Multipart");
        Response response = resources.
                target("api/whitelist").
                register(MultiPartFeature.class).
                request().
                header(HttpHeaders.AUTHORIZATION, "Bearer " + UserRoles.MEMBER.getRoleName()).
                post(Entity.entity(multiPart, multiPart.getMediaType()));
        assertEquals(403, response.getStatus());
    }

}
