package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.GenericUrl;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.service.WhitelistService;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhitelistEntryResourceTest extends ResourceTestHelper {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final WhitelistService service = mock(WhitelistService.class);

    @SuppressWarnings("deprecation")
    @Rule
    public ResourceTestRule resources = testRuleBuilder(new WhitelistResource(service)).build();

    private final String urlString = "http://localhost:8000/whitelist.txt";

    @Before
    public void setup() {
        try {
            GenericUrl url = new GenericUrl(urlString);
            when(service.postWhitelist(any())).thenReturn(url);
        } catch (Exception e) {
            logger.error("Exception initializing test: " + e);
            fail(e.getMessage());
        }
    }

    @Test
    public void testPostWhitelist() {
        String fileData = "Hello Multipart";
        FormDataMultiPart multiPart = new FormDataMultiPart()
                .field("fileData", fileData);
        Response response = resources.
                target("api/whitelist").
                register(MultiPartFeature.class).
                request().
                // Here is the auth trick. Pass in the UserRole name as the bearer token and the auth
                // framework will look for that value when it checks `@RolesAllowed`
                header(HttpHeaders.AUTHORIZATION, getRoleAuthorizationToken(UserRoles.ADMIN)).
                post(Entity.entity(multiPart, multiPart.getMediaType()));
        String location = response.getHeaderString(HttpHeaders.LOCATION);
        assertEquals(201, response.getStatus());
        assertEquals(urlString, location);
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
