package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.users.handler.UserRolesHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;


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
        MockitoAnnotations.initMocks(this);
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

    @Test
    public void testUpdateResearcherAsSelf() throws Exception {
        User researcher = createDacUser(UserRoles.RESEARCHER);
        JsonObject json = new JsonObject();
        JsonElement userJson = new Gson().toJsonTree(researcher);
        json.add(UserRolesHandler.UPDATED_USER_KEY, userJson);

        when(userService.findUserByEmail(any())).thenReturn(researcher);
        when(userService.updateDACUserById(any(), anyInt())).thenReturn(researcher);
        doNothing().when(userService).updateEmailPreference(anyBoolean(), anyInt());
        initResource();

        Response response = resource.update(authUser, uriInfo, json.toString(), researcher.getDacUserId());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testUpdateResearcherAsSomeoneElse() throws Exception {
        User researcher = createDacUser(UserRoles.RESEARCHER);
        JsonObject json = new JsonObject();
        JsonElement userJson = new Gson().toJsonTree(researcher);
        json.add(UserRolesHandler.UPDATED_USER_KEY, userJson);

        when(userService.findUserByEmail(any())).thenReturn(researcher);
        when(userService.updateDACUserById(any(), anyInt())).thenReturn(researcher);
        doNothing().when(userService).updateEmailPreference(anyBoolean(), anyInt());
        initResource();

        Response response = resource.update(authUser, uriInfo, json.toString(), researcher.getDacUserId() + 1);
        assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testUpdateResearcherAsAdmin() throws Exception {
        User admin = createDacUser(UserRoles.ADMIN);
        JsonObject json = new JsonObject();
        JsonElement userJson = new Gson().toJsonTree(admin);
        json.add(UserRolesHandler.UPDATED_USER_KEY, userJson);

        when(userService.findUserByEmail(any())).thenReturn(admin);
        when(userService.updateDACUserById(any(), anyInt())).thenReturn(admin);
        doNothing().when(userService).updateEmailPreference(anyBoolean(), anyInt());
        initResource();

        Response response = resource.update(authUser, uriInfo, json.toString(), admin.getDacUserId() + 1);
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test(expected = NotFoundException.class)
    public void testRetrieveDACUserWithInvalidEmail() {
        when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
        initResource();
        resource.describe(authUser, RandomStringUtils.random(10));
    }

    @Test
    public void testUpdateStatus() {
        User user = createDacUser(UserRoles.RESEARCHER);
        user.setDacUserId(RandomUtils.nextInt(1, 10));
        user.setStatus("pending");
        user.setRationale("rationale");
        when(userService.findUserById(any())).thenReturn(user);
        when(userService.updateUserStatus(any(), any())).thenReturn(user);
        when(userService.updateUserRationale(any(), any())).thenReturn(user);
        initResource();
        Response response = resource.updateStatus(user.getDacUserId(), user.toString());
        assertEquals(200, response.getStatus());
    }

    @Test(expected = NotFoundException.class)
    public void testUpdateStatusUserNotFound() {
        User user = createDacUser(UserRoles.RESEARCHER);
        user.setDacUserId(RandomUtils.nextInt(1, 10));
        when(userService.findUserById(any())).thenThrow(new NotFoundException());
        initResource();
        Response response = resource.updateStatus(user.getDacUserId(), user.toString());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testUpdateStatusBadRequest() {
        User user = createDacUser(UserRoles.RESEARCHER);
        user.setDacUserId(RandomUtils.nextInt(1, 10));
        user.setStatus("Bad Status");
        when(userService.findUserById(any())).thenReturn(user);
        when(userService.updateUserStatus(any(), any())).thenThrow(new IllegalArgumentException());
        initResource();
        Response response = resource.updateStatus(user.getDacUserId(), user.toString());
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testConvertJsonToDACUserNumericDateCase() {
        String jsonRole = "[{\"roleId\": 1, \"name\":\"name\", \"what\": \"Huh?\", \"rationale\": \"rationale\", \"status\": \"pending\"}]";
        String json = "{\"dacUserId\": 1, \"email\":\"email\", \"what\": \"Huh?\", \"createDate\": 1302828677828, \"additionalEmail\": \"additionalEmail\", \"emailPreference\": false, \"roles\": " + jsonRole + "}";
        User user = new User(json);
        Assert.assertNotNull(user);
        Assert.assertNotNull(user.getCreateDate());
        Assert.assertEquals(user.getDacUserId().intValue(), 1);
        Assert.assertEquals(user.getEmail(), "email");
        Assert.assertEquals(user.getAdditionalEmail(), "additionalEmail");
        Assert.assertEquals(user.getEmailPreference(), false);
        Assert.assertFalse(user.getRoles().isEmpty());
        Assert.assertEquals(user.getRoles().get(0).getRoleId().intValue(), 1);
        System.out.println(user.toString());
    }

    @Test
    public void testConvertJsonToDACUserStringDateCase() {
        String jsonRole = "[{\"roleId\": 1, \"name\":\"name\", \"what\": \"Huh?\", \"rationale\": \"rationale\", \"status\": \"pending\"}]";
        String json = "{\"dacUserId\": 1, \"email\":\"email\", \"what\": \"Huh?\", \"createDate\": \"Oct 28, 2020\", \"additionalEmail\": \"additionalEmail\", \"emailPreference\": false, \"roles\": " + jsonRole + "}";
        User user = new User(json);
        Assert.assertNotNull(user);
        Assert.assertNotNull(user.getCreateDate());
        Assert.assertEquals(user.getDacUserId().intValue(), 1);
        Assert.assertEquals(user.getEmail(), "email");
        Assert.assertEquals(user.getAdditionalEmail(), "additionalEmail");
        Assert.assertEquals(user.getEmailPreference(), false);
        Assert.assertFalse(user.getRoles().isEmpty());
        Assert.assertEquals(user.getRoles().get(0).getRoleId().intValue(), 1);
        System.out.println(user.toString());
    }

    @Test
    public void testConvertJsonToDACUserNoCreateDate() {
        String jsonRole = "[{\"roleId\": 1, \"name\":\"name\", \"what\": \"Huh?\", \"rationale\": \"rationale\", \"status\": \"pending\"}]";
        String json = "{\"dacUserId\": 1, \"email\":\"email\", \"what\": \"Huh?\", \"additionalEmail\": \"additionalEmail\", \"emailPreference\": false, \"roles\": " + jsonRole + "}";
        User user = new User(json);
        Assert.assertNotNull(user);
        Assert.assertNull(user.getCreateDate());
        Assert.assertEquals(user.getDacUserId().intValue(), 1);
        Assert.assertEquals(user.getEmail(), "email");
        Assert.assertEquals(user.getAdditionalEmail(), "additionalEmail");
        Assert.assertEquals(user.getEmailPreference(), false);
        Assert.assertFalse(user.getRoles().isEmpty());
        Assert.assertEquals(user.getRoles().get(0).getRoleId().intValue(), 1);
        System.out.println(user.toString());
    }

    private User createDacUser(UserRoles roles) {
        User user = new User();
        user.setDacUserId(RandomUtils.nextInt(1, 100));
        user.setDisplayName("name");
        user.setEmail("email");
        user.setAdditionalEmail("additional email");
        user.setEmailPreference(true);
        UserRole userRole = new UserRole(roles.getRoleId(), roles.getRoleName());
        user.setRoles(Collections.singletonList(userRole));
        return user;
    }

}
