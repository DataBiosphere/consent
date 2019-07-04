package org.broadinstitute.consent.http;


import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.resources.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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

    private User createDacUser(String name, String email, String status) {
        User user = new User();
        user.setDisplayName(name);
        user.setEmail(email);
        return user;
    }

    @Before
    public void initialize() throws Exception{
        User chairperson = testCreate(createDacUser("Chair Person", CHAIR_USER_EMAIL, CHAIRPERSON));
        User dacmember = testCreate(createDacUser("DAC Member", DAC_USER_EMAIL, DACMEMBER));

        assertThat(dacmember.getDisplayName()).isEqualTo("DAC Member");
        assertThat(dacmember.getEmail()).isEqualTo(DAC_USER_EMAIL);
        DACMEMBERMAIL = dacmember.getEmail();

        assertThat(chairperson.getDisplayName()).isEqualTo("Chair Person");
        assertThat(chairperson.getEmail()).isEqualTo(CHAIR_USER_EMAIL);
        assertThat(chairperson.getEmail()).isEqualTo(CHAIR_USER_EMAIL);
    }

    @Override
    public User retrieveDacUser(Client client, String url) throws IOException {
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

    public User testCreate(User dacuser) throws IOException {
        Client client = ClientBuilder.newClient();
        Response response = checkStatus(CREATED, post(client, dacUserPath(), dacuser));
        String createdLocation = checkHeader(response, "Location");
        return retrieveDacUser(client, createdLocation);
    }

    @Test
    public void deleteUser() throws IOException {
        Client client = ClientBuilder.newClient();
        User user = getJson(client, dacUserPathByEmail(DACMEMBERMAIL)).readEntity(User.class);
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
        User user = testCreate(createDacUser("Updated Chair Person", CHAIR_2_USER_EMAIL, CHAIRPERSON));
        Map<String, Object> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser",user);
        checkStatus(OK, put(client, dacUserPathById(user.getDacUserId()), updateUserMap));
        user = getJson(client, dacUserPathByEmail(user.getEmail())).readEntity(User.class);
        assertThat(user.getDisplayName()).isEqualTo("Updated Chair Person");
    }

    @Test
    public void testValidateDacUserDelegationNotNeeded() throws IOException {
        Client client = ClientBuilder.newClient();
        User user = getJson(client, dacUserPathByEmail(DAC_USER_EMAIL)).readEntity(User.class);
        user.setEmail(DAC_USER_EMAIL);
        UserRole role = new UserRole();
        role.setName(UserRoles.MEMBER.getValue());
        user.setRoles(new ArrayList<>(Arrays.asList(role)));
        Map<String, Object> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser",user);
        HashMap response = post(client, validateDelegationPath(UserRoles.MEMBER.getValue()), user).readEntity(HashMap.class);
        boolean needsDelegation = (Boolean)response.get("needsDelegation");
        List<User> users = (List<User>)response.get("delegateCandidates");
        assertThat(users).isEmpty();
        assertThat(needsDelegation).isFalse();
    }

    @Test
    public void testGetUserStatusWithInvalidId() throws IOException {
        Client client = ClientBuilder.newClient();
        checkStatus(NOT_FOUND, getJson(client, statusValue(42525)));
    }

    @Test
    public void testGetUserStatusSuccess() throws IOException {
        Client client = ClientBuilder.newClient();
        Response response = getJson(client, statusValue(1));
        UserRole userRole = response.readEntity(UserRole.class);
        assertThat(userRole.getStatus().equalsIgnoreCase(RoleStatus.PENDING.name()));
    }

    @Test
    public void testUpdateStatus() throws IOException {
        Client client = ClientBuilder.newClient();
        UserRole role = new UserRole();
        role.setRoleId(5);
        role.setStatus(RoleStatus.APPROVED.name());
        Response response = put(client, statusValue(1), role);
        checkStatus(OK, response);
        User user = response.readEntity(User.class);
        UserRole researcher = user.getRoles().stream().filter(userRole ->
                userRole.getName().equalsIgnoreCase(UserRoles.RESEARCHER.getValue()))
                .findFirst().get();
        assertThat(researcher.getStatus().equalsIgnoreCase(RoleStatus.APPROVED.name()));
    }

    @Test
    public void testUpdateStatusUsrNotFound() throws IOException {
        Client client = ClientBuilder.newClient();
        UserRole role = new UserRole();
        role.setStatus(RoleStatus.REJECTED.name());
        Response response = put(client, statusValue(10), role);
        checkStatus(NOT_FOUND, response);
    }

    @Test
    public void testUpdateStatusBadRequest() throws IOException {
        Client client = ClientBuilder.newClient();
        UserRole role = new UserRole();
        role.setRoleId(11);
        role.setStatus("Test");
        Response response = put(client, statusValue(4), role);
        checkStatus(BAD_REQUEST, response);
    }

    @Test
    public void testUpdateDisplayNameSuccess() throws IOException {
        final String displayName = "Test";
        Client client = ClientBuilder.newClient();
        User user = new User();
        user.setDisplayName(displayName);
        user.setDacUserId(4);
        Response response = put(client, dacUserPath()+ "/name/4", user);
        checkStatus(OK, response);
        User dacUser = response.readEntity(User.class);
        assertThat(dacUser.getDisplayName().equals(displayName));
    }

    @Test
    public void testUpdateDisplayNameWithInvalidUser() throws IOException {
        final String displayName = "Test";
        Client client = ClientBuilder.newClient();
        User user = new User();
        user.setDisplayName(displayName);
        user.setDacUserId(4);
        Response response = put(client, dacUserPath()+ "/name/99", user);
        checkStatus(NOT_FOUND, response);
    }


    @Test
    public void testUpdateDisplayNameWithEmptyName() throws IOException {
        Client client = ClientBuilder.newClient();
        User user = new User();
        user.setDacUserId(4);
        Response response = put(client, dacUserPath() + "/name/4", user);
        checkStatus(BAD_REQUEST, response);
    }

}
