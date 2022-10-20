package org.broadinstitute.consent.http.resources;

import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;


public class DACUserResourceTest {

    @Mock
    UserService userService;

    @Mock
    private UriInfo uriInfo;

    @Mock
    UriBuilder uriBuilder;

    private AuthUser authUser;

    private DACUserResource resource;

    @Before
    public void setUp() throws Exception {
        openMocks(this);
        GoogleUser googleUser = new GoogleUser();
        googleUser.setName("Test User");
        googleUser.setEmail("test@gmail.com");
        authUser = new AuthUser(googleUser);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(Mockito.anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build(anyString())).thenReturn(new URI("http://localhost:8180/api/dacuser/"));
    }

    private void initResource() {
        resource = new DACUserResource(userService);
    }

    @Test(expected = NotFoundException.class)
    public void testRetrieveDACUserWithInvalidEmail() {
        when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
        initResource();
        resource.describe(authUser, RandomStringUtils.randomAlphabetic(10));
    }

    @Test
    public void testConvertJsonToDACUserDateIgnoredCase() {
        String jsonRole = "[{\"roleId\": 1, \"name\":\"name\", \"what\": \"Huh?\", \"rationale\": \"rationale\", \"status\": \"pending\"}]";
        String json = "{\"userId\": 1, \"email\":\"email\", \"what\": \"Huh?\", \"createDate\": \"Oct 28, 2020\", \"emailPreference\": false, \"roles\": " + jsonRole + "}";
        User user = new User(json);
        Assert.assertNotNull(user);
        Assert.assertNull(user.getCreateDate());
        Assert.assertEquals(user.getUserId().intValue(), 1);
        Assert.assertEquals(user.getEmail(), "email");
        Assert.assertEquals(user.getEmailPreference(), false);
        Assert.assertFalse(user.getRoles().isEmpty());
        Assert.assertEquals(user.getRoles().get(0).getRoleId().intValue(), 1);
    }

    @Test
    public void testConvertJsonToDACUserNoCreateDate() {
        String jsonRole = "[{\"roleId\": 1, \"name\":\"name\", \"what\": \"Huh?\", \"rationale\": \"rationale\", \"status\": \"pending\"}]";
        String json = "{\"userId\": 1, \"email\":\"email\", \"what\": \"Huh?\", \"emailPreference\": false, \"roles\": " + jsonRole + "}";
        User user = new User(json);
        Assert.assertNotNull(user);
        Assert.assertNull(user.getCreateDate());
        Assert.assertEquals(user.getUserId().intValue(), 1);
        Assert.assertEquals(user.getEmail(), "email");
        Assert.assertEquals(user.getEmailPreference(), false);
        Assert.assertFalse(user.getRoles().isEmpty());
        Assert.assertEquals(user.getRoles().get(0).getRoleId().intValue(), 1);
    }
}
