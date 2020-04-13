package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.users.UserAPI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class UserResourceTest {

    @Mock
    private UserAPI userAPI;

    @Mock
    private UserService userService;

    private UserResource userResource;

    @Mock
    private UriInfo uriInfo;

    @Mock
    UriBuilder uriBuilder;

    private final String TEST_EMAIL = "test@gmail.com";

    private AuthUser authUser;

    @Before
    public void setUp() throws URISyntaxException {
        GoogleUser googleUser = new GoogleUser();
        googleUser.setName("Test User");
        googleUser.setEmail(TEST_EMAIL);
        authUser = new AuthUser(googleUser);
        MockitoAnnotations.initMocks(this);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build(anyString())).thenReturn(new URI("http://localhost:8180/dacuser/api"));
    }

    private void initResource() {
        userResource = new UserResource(userAPI, userService);
    }

    @Test
    public void testCreateExistingUser() {
        DACUser user = new DACUser();
        user.setEmail(TEST_EMAIL);
        List<UserRole> roles = new ArrayList<>();
        UserRole admin = new UserRole();
        admin.setName(UserRoles.ADMIN.getRoleName());
        UserRole researcher = new UserRole();
        researcher.setName(UserRoles.RESEARCHER.getRoleName());
        roles.add(researcher);
        roles.add(admin);
        user.setRoles(roles);
        when(userService.findUserByEmail(user.getEmail())).thenReturn(user);
        initResource();

        Response response = userResource.createResearcher(uriInfo, authUser);
        Assert.assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateFailingGoogleIdentity() {
        DACUser user = new DACUser();
        user.setEmail(TEST_EMAIL);
        initResource();

        Response response = userResource.createResearcher(uriInfo, new AuthUser(TEST_EMAIL));
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void createUserSuccess() {
        DACUser user = new DACUser();
        user.setDisplayName("Test");
        UserRole researcher = new UserRole();
        List<UserRole> roles = new ArrayList<>();
        researcher.setName(UserRoles.RESEARCHER.getRoleName());
        roles.add(researcher);
        user.setRoles(roles);
        when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
        when(userAPI.createUser(user)).thenReturn(user);
        initResource();

        Response response = userResource.createResearcher(uriInfo, authUser);
        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

}
