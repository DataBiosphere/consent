package org.broadinstitute.consent.http.resources;

import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.broadinstitute.consent.http.authentication.TestAuthorizer;
import org.broadinstitute.consent.http.authentication.TestOAuthAuthenticator;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class WhitelistResourceTest {

    private static final OAuthCredentialAuthFilter<AuthUser> filter;
    static {
        Map<String, String> authUserRoles = new HashMap<>();
        authUserRoles.put(UserRoles.ADMIN.getRoleName(), UserRoles.ADMIN.getRoleName());
        authUserRoles.put(UserRoles.MEMBER.getRoleName(), UserRoles.MEMBER.getRoleName());
        authUserRoles.put(UserRoles.CHAIRPERSON.getRoleName(), UserRoles.CHAIRPERSON.getRoleName());
        authUserRoles.put(UserRoles.RESEARCHER.getRoleName(), UserRoles.RESEARCHER.getRoleName());
        authUserRoles.put(UserRoles.DATAOWNER.getRoleName(), UserRoles.DATAOWNER.getRoleName());
        List<String> authUserNames = new ArrayList<>(authUserRoles.keySet());
        TestOAuthAuthenticator authenticator = new TestOAuthAuthenticator(authUserNames);
        TestAuthorizer authorizer = new TestAuthorizer(authUserRoles);
        filter = new OAuthCredentialAuthFilter.
                Builder<AuthUser>().
                setAuthenticator(authenticator).
                setAuthorizer(authorizer).
                setPrefix("Bearer").
                setRealm(" ").
                buildAuthFilter();
    }

    @Mock
    private static GCSStore gcsStore;

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addProvider(MultiPartFeature.class)
            .addProvider(RolesAllowedDynamicFeature.class)
            .addProvider(new AuthDynamicFeature(filter))
            .addProvider(new AuthValueFactoryProvider.Binder<>(AuthUser.class))
            .addResource(new WhitelistResource(gcsStore))
            .build();

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
                header(HttpHeaders.AUTHORIZATION, "Bearer " + UserRoles.ADMIN.getRoleName()).
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
