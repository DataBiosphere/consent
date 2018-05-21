package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.DataUseDTO;
import org.broadinstitute.consent.http.models.grammar.*;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ConsentAcceptanceTest extends ConsentServiceTest {

    private final UseRestriction everything = new Everything();
    private final UseRestriction nothing = new Nothing();
    private DataUseDTO generalUse = new DataUseBuilder().setGeneralUse(true).build();
    private DataUseDTO notGeneralUse = new DataUseBuilder().setGeneralUse(false).build();

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
    public void testCreateConsent() throws IOException {
        Client client = ClientBuilder.newClient();
        Consent rec = generateNewConsent(everything, generalUse);
        assertValidConsentResource(client, rec);
    }

    @Test
    public void testUpdateConsent() throws IOException {
        Client client = ClientBuilder.newClient();
        Consent rec = generateNewConsent(everything, generalUse);
        Response response = checkStatus(CREATED, post(client, consentPath(), rec));
        String createdLocation = checkHeader(response, "Location");
        mockValidateTokenResponse();
        Consent created = retrieveConsent(client, createdLocation);
        assertThat(created.requiresManualReview).isEqualTo(rec.requiresManualReview);
        assertThat(created.useRestriction).isEqualTo(rec.useRestriction);
        Consent update = generateNewConsent(nothing, notGeneralUse);
        update.setRequiresManualReview(true);
        check200(put(client, createdLocation, update));
        Consent updated = retrieveConsent(client, createdLocation);

        assertThat(updated.requiresManualReview).isEqualTo(update.requiresManualReview);
        assertThat(updated.useRestriction).isEqualTo(update.useRestriction);
    }

    @Test
    public void testDeleteConsent() throws IOException {
        Client client = ClientBuilder.newClient();
        Consent rec = generateNewConsent(everything, generalUse);
        Response response = checkStatus(CREATED, post(client, consentPath(), rec));
        String createdLocation = checkHeader(response, "Location");
        check200(delete(client, createdLocation));
    }


    @Test
    public void testOnlyOrNamedConsent() throws IOException {
        Client client = ClientBuilder.newClient();
        UseRestriction only = new Only("http://broadinstitute.org/ontology/consent/research_on", new Or(new Named("DOID:1"), new Named("DOID:2")));
        Consent rec = generateNewConsent(only, notGeneralUse);
        assertValidConsentResource(client, rec);
    }

    @Test
    public void testAndConsent() throws IOException {
        Client client = ClientBuilder.newClient();
        Consent rec = generateNewConsent(new And(new Named("DOID:1"), new Named("DOID:2")), notGeneralUse);
        assertValidConsentResource(client, rec);
    }

    @Test
    public void testNotConsent() throws IOException {
        Client client = ClientBuilder.newClient();
        Consent rec = generateNewConsent(new Not(new Named("DOID:1")), notGeneralUse);
        assertValidConsentResource(client, rec);
    }

    @Test
    public void testNothingConsent() throws IOException {
        Client client = ClientBuilder.newClient();
        Consent rec = generateNewConsent(nothing, notGeneralUse);
        assertValidConsentResource(client, rec);
    }

    @Test
    public void testSomeConsent() throws IOException {
        Client client = ClientBuilder.newClient();
        UseRestriction some = new Some("http://broadinstitute.org/ontology/consent/research_on", new Named("DOID:1"));
        Consent rec = generateNewConsent(some, notGeneralUse);
        assertValidConsentResource(client, rec);
    }

    @Test
    public void testMissingDataUseCreate() throws Exception {
        Client client = ClientBuilder.newClient();
        Consent rec = generateNewConsent(everything, null);
        Response response = post(client, consentPath(), rec);
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void testMissingDataUseUpdate() throws Exception {
        Client client = ClientBuilder.newClient();
        Consent rec = generateNewConsent(everything, generalUse);
        Response response = checkStatus(CREATED, post(client, consentPath(), rec));
        String createdLocation = checkHeader(response, "Location");

        Consent update = generateNewConsent(everything, null);
        Response updateResponse = put(client, createdLocation, update);
        assertThat(updateResponse.getStatus()).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void testInvalidDULCreate() throws Exception {
        Client client = ClientBuilder.newClient();
        Consent rec = generateNewConsent(everything, generalUse);
        rec.setDataUseLetter("invalidUrl");
        Response response = post(client, consentPath(), rec);
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void testInvalidDULUpdate() throws Exception {
        Client client = ClientBuilder.newClient();
        Consent rec = generateNewConsent(everything, generalUse);
        Response response = checkStatus(CREATED, post(client, consentPath(), rec));
        String createdLocation = checkHeader(response, "Location");

        Consent update = generateNewConsent(everything, null);
        update.setDataUseLetter("invalidUrl");
        Response updateResponse = put(client, createdLocation, update);
        assertThat(updateResponse.getStatus()).isEqualTo(BAD_REQUEST);
    }

    private void assertValidConsentResource(Client client, Consent rec) throws IOException {
        Response response = checkStatus(CREATED, post(client, consentPath(), rec));
        String createdLocation = checkHeader(response, "Location");
        mockValidateTokenResponse();
        Consent created = retrieveConsent(client, createdLocation);

        assertThat(created.requiresManualReview).isEqualTo(rec.requiresManualReview);
        assertThat(created.useRestriction).isEqualTo(rec.useRestriction);
    }

}
