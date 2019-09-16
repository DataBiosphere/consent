package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.users.UserAPI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class UserResourceTest {

    @Mock
    private UserAPI userAPI;

    private UserResource userResource;

    @Mock
    private UriInfo uriInfo;

    @Mock
    UriBuilder uriBuilder;

    private final String TEST_EMAIL = "test@gmail.com";

    @Before
    public void setUp() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);
        when(uriInfo.getRequestUriBuilder())
                .thenReturn(uriBuilder);
        when(uriBuilder.path(Mockito.anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build(anyString())).thenReturn(new URI("http://localhost:8180/dacuser/api"));
        userResource = new UserResource(userAPI);
    }

    @Test
    public void testCreateUserWithInvalidRole() {
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
        IllegalArgumentException ie = new IllegalArgumentException("Email should be unique.");
        when(userAPI.createUser(user, TEST_EMAIL)).thenThrow(ie);
        Response response = userResource.createUser(uriInfo, user.toString(), new AuthUser(TEST_EMAIL));
        Assert.assertTrue(response.getStatus() == Response.Status.CONFLICT.getStatusCode());
        Error error = (Error) response.getEntity();
        Assert.assertTrue(error.getCode() == Response.Status.CONFLICT.getStatusCode());
        Assert.assertTrue(error.getMessage().contains("Email should be unique."));
    }

    @Test
    public void testCreateUserWithoutRoles() {
        DACUser user = new DACUser();
        user.setEmail(TEST_EMAIL);
        UserRole admin = new UserRole();
        admin.setName(UserRoles.ADMIN.getRoleName());
        IllegalArgumentException ie = new IllegalArgumentException("Roles are required.");
        when(userAPI.createUser(user, TEST_EMAIL)).thenThrow(ie);
        Response response = userResource.createUser(uriInfo, user.toString(), new AuthUser(TEST_EMAIL));
        Assert.assertTrue(response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode());
        Error error = (Error) response.getEntity();
        Assert.assertTrue(error.getCode() == Response.Status.BAD_REQUEST.getStatusCode());
        Assert.assertTrue(error.getMessage().contains("Roles are required."));
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
        when(userAPI.createUser(user, TEST_EMAIL)).thenReturn(user);
        Response response = userResource.createUser(uriInfo, user.toString(), new AuthUser(TEST_EMAIL));
        Assert.assertTrue(response.getStatus() == Response.Status.CREATED.getStatusCode());
    }

}
