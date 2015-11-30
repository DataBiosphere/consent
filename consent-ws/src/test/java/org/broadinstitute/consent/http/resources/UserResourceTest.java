package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.enumeration.DACUserRoles;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.users.UserAPI;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
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
    public void testCreateUserWithInvalidRole(){
        DACUser user = new DACUser();
        user.setEmail(TEST_EMAIL);
        List<DACUserRole> roles = new ArrayList<>();
        DACUserRole admin = new DACUserRole();
        admin.setName(DACUserRoles.ADMIN.getValue());
        DACUserRole researcher = new DACUserRole();
        researcher.setName(DACUserRoles.RESEARCHER.getValue());
        roles.add(researcher);
        roles.add(admin);
        user.setRoles(roles);
        IllegalArgumentException ie = new IllegalArgumentException("Email should be unique.");
        when(userAPI.createUser(user, TEST_EMAIL)).thenThrow(ie);
        Response response = userResource.createUser(uriInfo, user, new User(TEST_EMAIL));
        Assert.assertTrue(response.getStatus() == Response.Status.CONFLICT.getStatusCode());
        Error error = (Error) response.getEntity();
        Assert.assertTrue(error.getCode() == Response.Status.CONFLICT.getStatusCode());
        Assert.assertTrue(error.getMessage().contains("Email should be unique."));
    }

    @Test
    public void testCreateUserWithoutRoles(){
        DACUser user = new DACUser();
        user.setEmail(TEST_EMAIL);
        DACUserRole admin = new DACUserRole();
        admin.setName(DACUserRoles.ADMIN.getValue());
        IllegalArgumentException ie = new IllegalArgumentException("Roles are required.");
        when(userAPI.createUser(user, TEST_EMAIL)).thenThrow(ie);
        Response response = userResource.createUser(uriInfo, user, new User(TEST_EMAIL));
        Assert.assertTrue(response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode());
        Error error = (Error) response.getEntity();
        Assert.assertTrue(error.getCode() == Response.Status.BAD_REQUEST.getStatusCode());
        Assert.assertTrue(error.getMessage().contains("Roles are required."));
    }

    @Test
    public void createUserSuccess(){
        DACUser user = new DACUser();
        user.setDisplayName("Test");
        DACUserRole researcher = new DACUserRole();
        List<DACUserRole> roles = new ArrayList<>();
        researcher.setName(DACUserRoles.RESEARCHER.getValue());
        roles.add(researcher);
        user.setRoles(roles);
        when(userAPI.createUser(user, TEST_EMAIL)).thenReturn(user);
        Response response = userResource.createUser(uriInfo, user, new User(TEST_EMAIL));
        Assert.assertTrue(response.getStatus() == Response.Status.CREATED.getStatusCode());
    }

    @Test
    public void testUpdateUserWithInvalidRole() throws UserRoleHandlerException {
        DACUser user = new DACUser();
        user.setDisplayName(TEST_EMAIL);
        List<DACUserRole> roles = new ArrayList<>();
        DACUserRole admin = new DACUserRole();
        admin.setName(DACUserRoles.CHAIRPERSON.getValue());
        DACUserRole researcher = new DACUserRole();
        researcher.setName(DACUserRoles.RESEARCHER.getValue());
        roles.add(researcher);
        roles.add(admin);
        user.setRoles(roles);
        when(userAPI.updateUser(user, TEST_EMAIL)).thenThrow(new IllegalArgumentException());
        Response response = userResource.update(user, new User(TEST_EMAIL));
        Assert.assertTrue(response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testUpdateUserWithInvalidEmail() throws UserRoleHandlerException {
        DACUser user = new DACUser();
        user.setDisplayName(TEST_EMAIL);
        List<DACUserRole> roles = new ArrayList<>();
        DACUserRole researcher = new DACUserRole();
        researcher.setName(DACUserRoles.RESEARCHER.getValue());
        roles.add(researcher);
        user.setRoles(roles);
        when(userAPI.updateUser(user, "invalid_mail@gmail.com")).thenThrow(NotAuthorizedException.class);
        Response response = userResource.update(user, new User("invalid_mail@gmail.com"));
        Assert.assertTrue(response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    public void testUpdateNotExistentUser() throws UserRoleHandlerException {
        DACUser user = new DACUser();
        user.setDisplayName(TEST_EMAIL);
        List<DACUserRole> roles = new ArrayList<>();
        DACUserRole researcher = new DACUserRole();
        researcher.setName(DACUserRoles.RESEARCHER.getValue());
        roles.add(researcher);
        user.setRoles(roles);
        when(userAPI.updateUser(user, "invalid_mail@gmail.com")).thenThrow(NotFoundException.class);
        Response response = userResource.update(user, new User("invalid_mail@gmail.com"));
        Assert.assertTrue(response.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
    }
}
