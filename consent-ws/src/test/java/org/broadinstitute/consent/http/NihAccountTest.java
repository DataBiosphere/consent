package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class NihAccountTest extends AbstractTest{

    private final String AUTHENTICATE_NIH_TOKEN = "nih-login/%s/%s";
    private final String DELETE_NIH_ACCOUNT = "nih-login/%s";
    private final String RESEARCHER_DAR_URL = "/researcher/%s/dar";

    private final String VALID_TOKEN = "eyJhbGciOiJIUzI1NiJ9.RXJhVGVzdA.TsIuAZEIBwKRuLNr7WpdB7TJhoooWyX8oxJfoZ6tO_0";
    private final String INVALID_TOKEN_SIGNATURE = "eyJhbGciOiJIUzI1NiJ9.RXJhVGVzdA.Ts2uAZEIBwKRuLNr7WpdB7TJhoooWyX8o3JfoZstO_0";
    private final String INVALID_TOKEN_MALFORMED = "eyJhbGciOiJIUzI1NiJ9.RXJhVGVzdATsIuAZEIBwKRuLNr7WpdB7TJhoooWyX8oxJfoZ6tO_0";

    private final String RESEARCHER_USER_ID = "2";

    private final String ERA_NAME = "EraTest";
    private final String ERA_AUTHORIZED = "true";

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));


    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testAuthValidToken() throws IOException {
        Client client = ClientBuilder.newClient();
        Map<String, List<Object>> val = new HashMap<>();

        // Verifies that NIH account is correclty validated and saved
        checkStatus(OK, post(client, path2Url(String.format(AUTHENTICATE_NIH_TOKEN, RESEARCHER_USER_ID, VALID_TOKEN)), val));


        Response response = checkStatus(OK, getJson(client, path2Url(String.format(RESEARCHER_DAR_URL, RESEARCHER_USER_ID))));
        Map<String, String> properties = response.readEntity(Map.class);

        // Check decoded values and generated data in data base
        assertEquals(ERA_NAME, properties.get(ResearcherFields.ERA_USERNAME.getValue()));
        assertEquals(ERA_AUTHORIZED, properties.get(ResearcherFields.ERA_STATUS.getValue()));
        assertNotNull(properties.get(ResearcherFields.ERA_DATE.getValue()));
        assertNotNull(properties.get(ResearcherFields.ERA_EXPIRATION_DATE.getValue()));

        // Check if expiration date is 30 days ahead of updated date
        assertTrue(checkExpirationDays(properties.get(ResearcherFields.ERA_DATE.getValue()),
                                properties.get(ResearcherFields.ERA_EXPIRATION_DATE.getValue())));

        delete(client, path2Url(String.format(DELETE_NIH_ACCOUNT, RESEARCHER_USER_ID)));

        // Verifies if NIH account has been correctly deleted
        properties = checkStatus(OK, getJson(client, path2Url(String.format(RESEARCHER_DAR_URL, RESEARCHER_USER_ID)))).readEntity(Map.class);
        assertNull(properties.get(ResearcherFields.ERA_USERNAME.getValue()));
        assertNull(properties.get(ResearcherFields.ERA_EXPIRATION_DATE.getValue()));
        assertNull(properties.get(ResearcherFields.ERA_STATUS.getValue()));
        assertNull(properties.get(ResearcherFields.ERA_DATE.getValue()));

        }

    @Test
    public void testAuthInvalidToken() throws IOException {
        Client client = ClientBuilder.newClient();
        Map<String, List<Object>> val = new HashMap<>();

        // Check null, not found, and not allowed users
        checkStatus(NOT_FOUND, post(client, path2Url(String.format(AUTHENTICATE_NIH_TOKEN, null, VALID_TOKEN)), val));
        checkStatus(NOT_FOUND, post(client, path2Url(String.format(AUTHENTICATE_NIH_TOKEN, 10101010, VALID_TOKEN)), val));

        // Check different invalid tokens
        checkStatus(BAD_REQUEST, post(client, path2Url(String.format(AUTHENTICATE_NIH_TOKEN, RESEARCHER_USER_ID, INVALID_TOKEN_SIGNATURE)), val));
        checkStatus(BAD_REQUEST, post(client, path2Url(String.format(AUTHENTICATE_NIH_TOKEN, RESEARCHER_USER_ID, INVALID_TOKEN_MALFORMED)), val));
        checkStatus(BAD_REQUEST, post(client, path2Url(String.format(AUTHENTICATE_NIH_TOKEN, RESEARCHER_USER_ID, null)), val));


    }

    private Boolean checkExpirationDays(String updatedDate, String expirationDate) {
        Long updatedDateMilis = Long.valueOf(updatedDate);
        Long expirationDateMilis = Long.valueOf(expirationDate);

        Long diffInMillies = expirationDateMilis - updatedDateMilis;
        return TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS) == 30;
    }
}
