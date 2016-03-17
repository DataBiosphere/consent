package org.broadinstitute.consent.http;


import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.DACUser;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

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
    public void initialize() {
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
    public DACUser retrieveDacUser(Client client, String url) {
        return super.retrieveDacUser(client, url);
    }

    @After
    public void removeTestData() {
        Client client = ClientBuilder.newClient();
        delete(client, dacUserPathByEmail(CHAIR_USER_EMAIL));
        delete(client, dacUserPathByEmail(DAC_USER_EMAIL));
        delete(client, dacUserPathByEmail(CHAIR_2_USER_EMAIL));
    }

    public DACUser testCreate(DACUser dacuser) {
        Client client = ClientBuilder.newClient();
        Response response = checkStatus(CREATED, post(client, dacUserPath(), dacuser));
        String createdLocation = checkHeader(response, "Location");
        return retrieveDacUser(client, createdLocation);
    }

    @Test
    public void deleteUser() {
        Client client = ClientBuilder.newClient();
        DACUser user = getJson(client, dacUserPathByEmail(DACMEMBERMAIL)).readEntity(DACUser.class);
        checkStatus(OK, delete(client, dacUserPathByEmail(user.getEmail())));
        checkStatus(NOT_FOUND, getJson(client, dacUserPathByEmail(user.getEmail())));
    }

    @Test
    public void retrieveDACUserWithInvalidEmail() {
        Client client = ClientBuilder.newClient();
        checkStatus(NOT_FOUND, getJson(client, dacUserPathByEmail(INVALID_USER_EMAIL)));
    }

    @Test
    public void testUpdateDACUser() {
        Client client = ClientBuilder.newClient();
        DACUser user = testCreate(createDacUser("Updated Chair Person", CHAIR_2_USER_EMAIL, CHAIRPERSON));
        checkStatus(OK, put(client, dacUserPathById(user.getDacUserId()), user));
        user = getJson(client, dacUserPathByEmail(user.getEmail())).readEntity(DACUser.class);
        assertThat(user.getDisplayName()).isEqualTo("Updated Chair Person");
    }

}
