package org.broadinstitute.consent.http;


import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.resources.Resource;
import org.broadinstitute.consent.http.service.users.handler.DACUserRolesHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DACUserTest extends DACUserServiceTest {

    public static final int CREATED = Response.Status.CREATED
            .getStatusCode();
    public static final int OK = Response.Status.OK.getStatusCode();
    public static final int NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();

    private static final String INVALID_USER_EMAIL = "invalidemail@broad.com";
    private static final String DAC_USER_EMAIL = "dacmember@broad.com";
    private static final String CHAIR_USER_EMAIL = "chairperson@broad.com";
    private static final String CHAIR_2_USER_EMAIL = "chairperson2@broad.com";
    private static final String CHAIRPERSON = Resource.CHAIRPERSON;
    private static final String DACMEMBER = "DACMEMBER";
    private static String DACMEMBERMAIL;

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));


    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    private DACUser createDacUser(String name, String email, String status) {
        DACUser user = new DACUser();
        user.setDisplayName(name);
        user.setEmail(email);
        UserRole role = new UserRole();
        role.setRoleId(UserRoles.ALUMNI.getRoleId());
        role.setName(StringUtils.capitalize(UserRoles.ALUMNI.getRoleName().toLowerCase()));
        user.setRoles(Collections.singletonList(role));
        return user;
    }

    @Before
    public void initialize() throws Exception{
        DACUser chairperson = testCreate(createDacUser("Chair Person", CHAIR_USER_EMAIL, CHAIRPERSON));
        DACUser dacmember = testCreate(createDacUser("DAC Member", DAC_USER_EMAIL, DACMEMBER));

        assertThat(dacmember.getDisplayName()).isEqualTo("DAC Member");
        assertThat(dacmember.getEmail()).isEqualTo(DAC_USER_EMAIL);
        DACMEMBERMAIL = dacmember.getEmail();

        assertThat(chairperson.getDisplayName()).isEqualTo("Chair Person");
        assertThat(chairperson.getEmail()).isEqualTo(CHAIR_USER_EMAIL);
        assertThat(chairperson.getEmail()).isEqualTo(CHAIR_USER_EMAIL);
    }

    @Override
    public DACUser retrieveDacUser(Client client, String url) throws IOException {
        mockValidateTokenResponse();
        return super.retrieveDacUser(client, url);
    }

    @After
    public void removeTestData() throws IOException {
        Client client = ClientBuilder.newClient();
        delete(client, dacUserPathByEmail(CHAIR_USER_EMAIL));
        delete(client, dacUserPathByEmail(DAC_USER_EMAIL));
        delete(client, dacUserPathByEmail(CHAIR_2_USER_EMAIL));
    }

    private DACUser testCreate(DACUser dacuser) throws IOException {
        Client client = ClientBuilder.newClient();
        Response response = checkStatus(CREATED, post(client, dacUserPath(), dacuser));
        String createdLocation = checkHeader(response, "Location");
        return retrieveDacUser(client, createdLocation);
    }

    @Test
    public void deleteUser() throws IOException {
        Client client = ClientBuilder.newClient();
        DACUser user = getJson(client, dacUserPathByEmail(DACMEMBERMAIL)).readEntity(DACUser.class);
        checkStatus(OK, delete(client, dacUserPathByEmail(user.getEmail())));
        checkStatus(NOT_FOUND, getJson(client, dacUserPathByEmail(user.getEmail())));
    }

    @Test
    public void retrieveDACUserWithInvalidEmail() throws IOException {
        Client client = ClientBuilder.newClient();
        checkStatus(NOT_FOUND, getJson(client, dacUserPathByEmail(INVALID_USER_EMAIL)));
    }

    @Test
    public void testUpdateDACUser() throws IOException {
        Client client = ClientBuilder.newClient();
        mockValidateTokenResponse();
        DACUser user = testCreate(createDacUser("Updated Chair Person", CHAIR_2_USER_EMAIL, CHAIRPERSON));
        Map<String, Object> updateUserMap = new HashMap<>();
        updateUserMap.put(DACUserRolesHandler.UPDATED_USER_KEY, user);
        checkStatus(OK, put(client, dacUserPathById(user.getDacUserId()), updateUserMap));
        user = getJson(client, dacUserPathByEmail(user.getEmail())).readEntity(DACUser.class);
        assertThat(user.getDisplayName()).isEqualTo("Updated Chair Person");
    }

    @Test
    public void testValidateDacUserDelegationNotNeeded() throws IOException {
        Client client = ClientBuilder.newClient();
        DACUser user = getJson(client, dacUserPathByEmail(DAC_USER_EMAIL)).readEntity(DACUser.class);
        user.setEmail(DAC_USER_EMAIL);
        UserRole role = new UserRole();
        role.setName(UserRoles.MEMBER.getRoleName());
        user.setRoles(Collections.singletonList(role));
        HashMap response = post(client, validateDelegationPath(UserRoles.MEMBER.getRoleName()), user).readEntity(HashMap.class);
        boolean needsDelegation = (Boolean) response.get("needsDelegation");
        List<DACUser> dacUsers = (List<DACUser>) response.get("delegateCandidates");
        assertThat(dacUsers).isEmpty();
        assertThat(needsDelegation).isFalse();
    }

    @Test
    public void testGetUserStatusWithInvalidId() throws IOException {
        Client client = ClientBuilder.newClient();
        checkStatus(NOT_FOUND, getJson(client, statusValue(42525)));
    }

    @Test
    public void testUpdateStatus() throws IOException {
        Client client = ClientBuilder.newClient();
        DACUser postUser = new DACUser();
        postUser.setStatus(RoleStatus.APPROVED.name());
        Response response = put(client, statusValue(1), postUser);
        checkStatus(OK, response);
        DACUser user = response.readEntity(DACUser.class);
        assertTrue(user.getStatus().equalsIgnoreCase(RoleStatus.APPROVED.name()));
    }

    @Test
    public void testUpdateStatusUserNotFound() throws IOException {
        Client client = ClientBuilder.newClient();
        DACUser postUser = new DACUser();
        postUser.setStatus(RoleStatus.REJECTED.name());
        Response response = put(client, statusValue(10), postUser);
        checkStatus(NOT_FOUND, response);
    }

    @Test
    public void testUpdateStatusBadRequest() throws IOException {
        Client client = ClientBuilder.newClient();
        DACUser postUser = new DACUser();
        postUser.setStatus("Test");
        Response response = put(client, statusValue(4), postUser);
        checkStatus(BAD_REQUEST, response);
    }

    @Test
    public void testUpdateDisplayNameSuccess() throws IOException {
        final String displayName = "Test";
        Client client = ClientBuilder.newClient();
        DACUser user = new DACUser();
        user.setDisplayName(displayName);
        user.setDacUserId(4);
        Response response = put(client, dacUserPath()+ "/name/4", user);
        checkStatus(OK, response);
        DACUser dacUser = response.readEntity(DACUser.class);
        assertEquals(dacUser.getDisplayName(), displayName);
    }

    @Test
    public void testUpdateDisplayNameWithInvalidUser() throws IOException {
        final String displayName = "Test";
        Client client = ClientBuilder.newClient();
        DACUser user = new DACUser();
        user.setDisplayName(displayName);
        user.setDacUserId(4);
        Response response = put(client, dacUserPath()+ "/name/99", user);
        checkStatus(NOT_FOUND, response);
    }

    @Test
    public void testUpdateDisplayNameWithEmptyName() throws IOException {
        Client client = ClientBuilder.newClient();
        DACUser user = new DACUser();
        user.setDacUserId(4);
        Response response = put(client, dacUserPath() + "/name/4", user);
        checkStatus(BAD_REQUEST, response);
    }

    @Test
    public void testConvertJsonToDACUser() {
        String jsonRole = "[{\"roleId\": 1, \"name\":\"name\", \"what\": \"Huh?\", \"rationale\": \"rationale\", \"status\": \"pending\"}]";
        String json = "{\"dacUserId\": 1, \"email\":\"email\", \"what\": \"Huh?\", \"createDate\": 1302828677828, \"additionalEmail\": \"additionalEmail\", \"emailPreference\": false, \"roles\": " + jsonRole + "}";
        DACUser user = new DACUser(json);
        Assert.assertNotNull(user);
        Assert.assertEquals(user.getDacUserId().intValue(), 1);
        Assert.assertEquals(user.getEmail(), "email");
        Assert.assertEquals(user.getAdditionalEmail(), "additionalEmail");
        Assert.assertEquals(user.getEmailPreference(), false);
        Assert.assertFalse(user.getRoles().isEmpty());
        Assert.assertEquals(user.getRoles().get(0).getRoleId().intValue(), 1);
        System.out.println(user.toString());
    }

}
