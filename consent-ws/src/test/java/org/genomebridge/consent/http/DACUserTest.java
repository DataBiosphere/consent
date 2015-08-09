package org.genomebridge.consent.http;

import static org.fest.assertions.api.Assertions.assertThat;

import io.dropwizard.testing.junit.DropwizardAppRule;

import org.genomebridge.consent.http.models.DACUser;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

public class DACUserTest extends DACUserServiceTest {

    public static final int CREATED = ClientResponse.Status.CREATED
            .getStatusCode();
    public static final int OK = ClientResponse.Status.OK.getStatusCode();
    public static final int NOT_FOUND = ClientResponse.Status.NOT_FOUND.getStatusCode();

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
        user.setMemberStatus(status);
        return user;
    }

    @Before
    public void initialize() {
        DACUser chairperson = testCreate(createDacUser("Chair Person", CHAIR_USER_EMAIL, CHAIRPERSON));
        DACUser dacmember = testCreate(createDacUser("DAC Member", DAC_USER_EMAIL, DACMEMBER));

        assertThat(dacmember.getDisplayName()).isEqualTo("DAC Member");
        assertThat(dacmember.getEmail()).isEqualTo(DAC_USER_EMAIL);
        assertThat(dacmember.getMemberStatus()).isEqualTo(DACMEMBER);
        DACMEMBERMAIL = dacmember.getEmail();

        assertThat(chairperson.getDisplayName()).isEqualTo("Chair Person");
        assertThat(chairperson.getEmail()).isEqualTo(CHAIR_USER_EMAIL);
        assertThat(chairperson.getMemberStatus()).isEqualTo(CHAIRPERSON);
        assertThat(chairperson.getEmail()).isEqualTo(CHAIR_USER_EMAIL);
    }

    @After
    public void removeTestData() {
        Client client = new Client();
        delete(client, dacUserPathByEmail(CHAIR_USER_EMAIL));
        delete(client, dacUserPathByEmail(DAC_USER_EMAIL));
        delete(client, dacUserPathByEmail(CHAIR_2_USER_EMAIL));
    }

    public DACUser testCreate(DACUser dacuser) {
        Client client = new Client();
        ClientResponse response = checkStatus(CREATED,
                post(client, dacUserPath(), dacuser));
        String createdLocation = checkHeader(response, "Location");
        return retrieveDacUser(client, createdLocation);
    }

    @Test
    public void deleteUser() {
        Client client = new Client();
        DACUser user = get(client, dacUserPathByEmail(DACMEMBERMAIL))
                .getEntity(DACUser.class);
        checkStatus(OK, delete(client, dacUserPathByEmail(user.getEmail())));
        checkStatus(NOT_FOUND, get(client, dacUserPathByEmail(user.getEmail())));
    }

    @Test
    public void retrieveDACUserWithInvalidEmail() {
        Client client = new Client();
        checkStatus(NOT_FOUND, get(client, dacUserPathByEmail(INVALID_USER_EMAIL)));
    }

    @Test
    public void testUpdateDACUser() {
        Client client = new Client();
        DACUser user = testCreate(createDacUser("Updated Chair Person", CHAIR_2_USER_EMAIL, CHAIRPERSON));
        checkStatus(OK, put(client, dacUserPath(), user));
        user = get(client, dacUserPathByEmail(user.getEmail())).getEntity(DACUser.class);
        assertThat(user.getDisplayName()).isEqualTo("Updated Chair Person");
    }

}
