package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class ConsentAcceptanceTest extends AbstractTest {

    private final UseRestriction everything = new Everything();
    private DataUse generalUse = new DataUseBuilder().setGeneralUse(true).build();

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE =
            new DropwizardAppRule<>(ConsentApplication.class,
                    resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }


    @Before
    public void setup() {
        mockTranslateResponse();
        mockValidateResponse();
    }

    @Test
    public void testDeleteConsent() {
        Client client = ClientBuilder.newClient();
        Consent rec = generateNewConsent(everything, generalUse);
        Response response = checkStatus(CREATED, post(client, consentPath(), rec));
        String createdLocation = checkHeader(response, "Location");
        check200(delete(client, createdLocation));
    }

    @Test
    public void testMissingDataUseCreate() {
        Client client = ClientBuilder.newClient();
        Consent rec = generateNewConsent(everything, null);
        Response response = post(client, consentPath(), rec);
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void testMissingDataUseUpdate() {
        Client client = ClientBuilder.newClient();
        Consent rec = generateNewConsent(everything, generalUse);
        Response response = checkStatus(CREATED, post(client, consentPath(), rec));
        String createdLocation = checkHeader(response, "Location");

        Consent update = generateNewConsent(everything, null);
        Response updateResponse = put(client, createdLocation, update);
        assertThat(updateResponse.getStatus()).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void testInvalidDULCreate() {
        Client client = ClientBuilder.newClient();
        Consent rec = generateNewConsent(everything, generalUse);
        rec.setDataUseLetter("invalidUrl");
        Response response = post(client, consentPath(), rec);
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void testInvalidDULUpdate() {
        Client client = ClientBuilder.newClient();
        Consent rec = generateNewConsent(everything, generalUse);
        Response response = checkStatus(CREATED, post(client, consentPath(), rec));
        String createdLocation = checkHeader(response, "Location");

        Consent update = generateNewConsent(everything, null);
        update.setDataUseLetter("invalidUrl");
        Response updateResponse = put(client, createdLocation, update);
        assertThat(updateResponse.getStatus()).isEqualTo(BAD_REQUEST);
    }

}
