package org.broadinstitute.consent.http.resources;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URI;
import java.util.Collections;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
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

    /**
     * Utility method to construct a user json object as the API expects it to be formatted.
     * @param user The User
     * @return Json object in the form of { "updatedUser": {... user json ...}}
     */
    private JsonObject makeUserMapJsonObject(User user) {
        JsonObject json = new JsonObject();
        JsonElement userJson = new Gson().toJsonTree(user);
        json.add(UserRolesHandler.UPDATED_USER_KEY, userJson);
        return json;
    }

    @Test
    public void testUpdateResearcherAsSelf() {
        User researcher = createDacUser(UserRoles.RESEARCHER);
        JsonObject json = makeUserMapJsonObject(researcher);

        when(userService.findUserByEmail(any())).thenReturn(researcher);
        when(userService.findUserById(any())).thenReturn(researcher);
        when(userService.updateDACUserById(any(), anyInt())).thenReturn(researcher);
        doNothing().when(userService).updateEmailPreference(anyBoolean(), anyInt());
        initResource();

        Response response = resource.update(authUser, uriInfo, json.toString(), researcher.getUserId());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testUpdateResearcherAsSomeoneElse() {
        User researcher = createDacUser(UserRoles.RESEARCHER);
        JsonObject json = makeUserMapJsonObject(researcher);

        when(userService.findUserByEmail(any())).thenReturn(researcher);
        when(userService.findUserById(any())).thenReturn(researcher);
        when(userService.updateDACUserById(any(), anyInt())).thenReturn(researcher);
        doNothing().when(userService).updateEmailPreference(anyBoolean(), anyInt());
        initResource();

        Response response = resource.update(authUser, uriInfo, json.toString(), researcher.getUserId() + 1);
        assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testUpdateResearcherAsAdmin() {
        User admin = createDacUser(UserRoles.ADMIN);
        JsonObject json = makeUserMapJsonObject(admin);

        when(userService.findUserByEmail(any())).thenReturn(admin);
        when(userService.findUserById(any())).thenReturn(admin);
        when(userService.updateDACUserById(any(), anyInt())).thenReturn(admin);
        doNothing().when(userService).updateEmailPreference(anyBoolean(), anyInt());
        initResource();

        Response response = resource.update(authUser, uriInfo, json.toString(), admin.getUserId() + 1);
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testSetSOInstitutionOK() {
        User user = createDacUser(UserRoles.SIGNINGOFFICIAL);
        user.setInstitutionId(null);
        user.setUserId(RandomUtils.nextInt(1, 10));
        when(userService.findUserById(any())).thenReturn(user);
        when(userService.findUserByEmail(any())).thenReturn(user);
        when(userService.updateDACUserById(any(), anyInt())).thenReturn(user);
        initResource();

        // Update passed in user to have a different institution id to trigger SO error
        Gson gson = new Gson();
        User updateUser = gson.fromJson(gson.toJson(user), User.class);
        updateUser.setInstitutionId(RandomUtils.nextInt(1, 10));

        JsonObject json = makeUserMapJsonObject(updateUser);

        Response response = resource.update(authUser, uriInfo, json.toString(), updateUser.getUserId());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testUpdateSOInstitutionBadRequest() {
        User user = createDacUser(UserRoles.SIGNINGOFFICIAL);
        user.setUserId(RandomUtils.nextInt(1, 10));
        when(userService.findUserById(any())).thenReturn(user);
        initResource();

        // Update passed in user to have a different institution id to trigger SO error
        Gson gson = new Gson();
        User updateUser = gson.fromJson(gson.toJson(user), User.class);
        updateUser.setInstitutionId(user.getInstitutionId() + 10);

        JsonObject json = makeUserMapJsonObject(updateUser);

        Response response = resource.update(authUser, uriInfo, json.toString(), updateUser.getUserId());
        assertEquals(400, response.getStatus());
    }

    @Test(expected = NotFoundException.class)
    public void testRetrieveDACUserWithInvalidEmail() {
        when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
        initResource();
        resource.describe(authUser, RandomStringUtils.random(10));
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

    private User createDacUser(UserRoles roles) {
        User user = new User();
        user.setUserId(RandomUtils.nextInt(1, 100));
        user.setDisplayName("name");
        user.setEmail("email");
        user.setEmailPreference(true);
        user.setInstitutionId(RandomUtils.nextInt(1, 100));
        UserRole userRole = new UserRole(roles.getRoleId(), roles.getRoleName());
        user.setRoles(Collections.singletonList(userRole));
        return user;
    }

}
