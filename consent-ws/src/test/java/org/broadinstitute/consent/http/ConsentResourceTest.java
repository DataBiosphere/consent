package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConsentResourceTest extends AbstractTest {

    private String name;

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE =
        new DropwizardAppRule<>(ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Before
    public void setup() throws Exception {
        // generated name used for comparison testing
        name = "consent_" + Math.random() + "_name";

        // Need to ensure that the Use Restriction Validator doesn't error out on us on container startup.
        mockValidateResponse();
    }

    @Test
    public void testFindByName() throws Exception {
        // First create the consent
        Client client = ClientBuilder.newClient();
        String location = createConsent(client);

        // Then query on it
        WebTarget webTarget = client.target(path2Url("/consent")).queryParam("name", name);
        mockValidateTokenResponse();
        Response response = webTarget.request(MediaType.APPLICATION_JSON_TYPE).
            header("Authorization", "Bearer access-token").get(Response.class);
        String entity = StringUtils.strip(response.readEntity(String.class), "\"");

        // Make sure we have a response ...
        assertThat(response.getStatus() == OK);
        assertNotNull(entity);
        // And that the location (ID) is the same as the cleaned up ID returned from findByName
        assertTrue(location.equals(entity));
    }

    private String createConsent(Client client) throws IOException {
        String consentPath = path2Url("/consent");
        Timestamp createDate = new Timestamp(new Date().getTime());
        Consent consent = new Consent(
            true,
            new Everything(),
            null,
            name,
            createDate,
            createDate,
            createDate
        );
        consent.setTranslatedUseRestriction("translated");
        Response response = checkStatus(CREATED, post(client, consentPath, consent));
        String createdLocation = checkHeader(response, "Location");
        return createdLocation.substring(createdLocation.lastIndexOf("/") + 1);
    }

}
