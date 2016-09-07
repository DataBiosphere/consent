package org.broadinstitute.consent.http;


import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.DACUserRoles;
import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DACUserRole;
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
    private static final String CHAIRPERSON = "CHAIRPERSON";
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

    public DACUser testCreate(DACUser dacuser) throws IOException {
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
        updateUserMap.put("updatedUser",user);
        checkStatus(OK, put(client, dacUserPathById(user.getDacUserId()), updateUserMap));
        user = getJson(client, dacUserPathByEmail(user.getEmail())).readEntity(DACUser.class);
        assertThat(user.getDisplayName()).isEqualTo("Updated Chair Person");
    }

    @Test
    public void testValidateDacUserDelegationNotNeeded() throws IOException {
        Client client = ClientBuilder.newClient();
        DACUser user = getJson(client, dacUserPathByEmail(DAC_USER_EMAIL)).readEntity(DACUser.class);
        user.setEmail(DAC_USER_EMAIL);
        DACUserRole role = new DACUserRole();
        role.setName(DACUserRoles.MEMBER.getValue());
        user.setRoles(new ArrayList<>(Arrays.asList(role)));
        Map<String, Object> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser",user);
        HashMap response = post(client, validateDelegationPath(DACUserRoles.MEMBER.getValue()), user).readEntity(HashMap.class);
        boolean needsDelegation = (Boolean)response.get("needsDelegation");
        List<DACUser> dacUsers = (List<DACUser>)response.get("delegateCandidates");
        assertThat(dacUsers).isEmpty();
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
        DACUserRole userRole = response.readEntity(DACUserRole.class);
        assertThat(userRole.getStatus().equalsIgnoreCase(RoleStatus.PENDING.name()));
    }

    @Test
    public void testUpdateStatus() throws IOException {
        Client client = ClientBuilder.newClient();
        DACUserRole role = new DACUserRole();
        role.setRoleId(5);
        role.setStatus(RoleStatus.APPROVED.name());
        Response response = put(client, statusValue(1), role);
        checkStatus(OK, response);
        DACUser user = response.readEntity(DACUser.class);
        DACUserRole researcher = user.getRoles().stream().filter(userRole ->
                userRole.getName().equalsIgnoreCase(DACUserRoles.RESEARCHER.getValue()))
                .findFirst().get();
        assertThat(researcher.getStatus().equalsIgnoreCase(RoleStatus.APPROVED.name()));
    }

    @Test
    public void testUpdateStatusUsrNotFound() throws IOException {
        Client client = ClientBuilder.newClient();
        DACUserRole role = new DACUserRole();
        role.setStatus(RoleStatus.REJECTED.name());
        Response response = put(client, statusValue(10), role);
        checkStatus(NOT_FOUND, response);
    }

    @Test
    public void testUpdateStatusBadRequest() throws IOException {
        Client client = ClientBuilder.newClient();
        DACUserRole role = new DACUserRole();
        role.setRoleId(11);
        role.setStatus("Test");
        Response response = put(client, statusValue(4), role);
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
        assertThat(dacUser.getDisplayName().equals(displayName));
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

}
