package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.users.UserService;
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
    private UserService userService;

    @Mock
    private UriInfo uriInfo;

    @Mock
    UriBuilder uriBuilder;

    private UserResource userResource;
    private final String TEST_EMAIL = "test@gmail.com";

    @Before
    public void setUp() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);
        when(uriInfo.getRequestUriBuilder())
                .thenReturn(uriBuilder);
       when(uriBuilder.path(Mockito.anyString())).thenReturn(uriBuilder);
       when(uriBuilder.build(anyString())).thenReturn(new URI("http://localhost:8180/dacuser/api"));
       userResource = new UserResource(userService);
    }

    @Test
    public void testCreateUserWithInvalidRole(){
        User user = new User();
        user.setEmail(TEST_EMAIL);
        List<UserRole> roles = new ArrayList<>();
        UserRole admin = new UserRole();
        admin.setName(UserRoles.ADMIN.getValue());
        UserRole researcher = new UserRole();
        researcher.setName(UserRoles.RESEARCHER.getValue());
        roles.add(researcher);
        roles.add(admin);
        user.setRoles(roles);
        IllegalArgumentException ie = new IllegalArgumentException("Email should be unique.");
        when(userService.createUser(user, TEST_EMAIL)).thenThrow(ie);
        Response response = userResource.createUser(uriInfo, user, new AuthUser(TEST_EMAIL));
        Assert.assertTrue(response.getStatus() == Response.Status.CONFLICT.getStatusCode());
        Error error = (Error) response.getEntity();
        Assert.assertTrue(error.getCode() == Response.Status.CONFLICT.getStatusCode());
        Assert.assertTrue(error.getMessage().contains("Email should be unique."));
    }

    @Test
    public void testCreateUserWithoutRoles(){
        User user = new User();
        user.setEmail(TEST_EMAIL);
        UserRole admin = new UserRole();
        admin.setName(UserRoles.ADMIN.getValue());
        IllegalArgumentException ie = new IllegalArgumentException("Roles are required.");
        when(userService.createUser(user, TEST_EMAIL)).thenThrow(ie);
        Response response = userResource.createUser(uriInfo, user, new AuthUser(TEST_EMAIL));
        Assert.assertTrue(response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode());
        Error error = (Error) response.getEntity();
        Assert.assertTrue(error.getCode() == Response.Status.BAD_REQUEST.getStatusCode());
        Assert.assertTrue(error.getMessage().contains("Roles are required."));
    }

    @Test
    public void createUserSuccess(){
        User user = new User();
        user.setDisplayName("Test");
        UserRole researcher = new UserRole();
        List<UserRole> roles = new ArrayList<>();
        researcher.setName(UserRoles.RESEARCHER.getValue());
        roles.add(researcher);
        user.setRoles(roles);
        when(userService.createUser(user, TEST_EMAIL)).thenReturn(user);
        Response response = userResource.createUser(uriInfo, user, new AuthUser(TEST_EMAIL));
        Assert.assertTrue(response.getStatus() == Response.Status.CREATED.getStatusCode());
    }

    @Test
    public void testUpdateUserWithInvalidRole() throws UserRoleHandlerException {
        User user = new User();
        user.setDisplayName(TEST_EMAIL);
        List<UserRole> roles = new ArrayList<>();
        UserRole admin = new UserRole();
        admin.setName(UserRoles.CHAIRPERSON.getValue());
        UserRole researcher = new UserRole();
        researcher.setName(UserRoles.RESEARCHER.getValue());
        roles.add(researcher);
        roles.add(admin);
        user.setRoles(roles);
        when(userService.updateUser(user, TEST_EMAIL)).thenThrow(new IllegalArgumentException());
        Response response = userResource.update(user, new AuthUser(TEST_EMAIL));
        Assert.assertTrue(response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testUpdateUserWithInvalidEmail() throws UserRoleHandlerException {
        User user = new User();
        user.setDisplayName(TEST_EMAIL);
        List<UserRole> roles = new ArrayList<>();
        UserRole researcher = new UserRole();
        researcher.setName(UserRoles.RESEARCHER.getValue());
        roles.add(researcher);
        user.setRoles(roles);
        when(userService.updateUser(user, "invalid_mail@gmail.com")).thenThrow(NotAuthorizedException.class);
        Response response = userResource.update(user, new AuthUser("invalid_mail@gmail.com"));
        Assert.assertTrue(response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    public void testUpdateNotExistentUser() throws UserRoleHandlerException {
        User user = new User();
        user.setDisplayName(TEST_EMAIL);
        List<UserRole> roles = new ArrayList<>();
        UserRole researcher = new UserRole();
        researcher.setName(UserRoles.RESEARCHER.getValue());
        roles.add(researcher);
        user.setRoles(roles);
        when(userService.updateUser(user, "invalid_mail@gmail.com")).thenThrow(NotFoundException.class);
        Response response = userResource.update(user, new AuthUser("invalid_mail@gmail.com"));
        Assert.assertTrue(response.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
    }
}
