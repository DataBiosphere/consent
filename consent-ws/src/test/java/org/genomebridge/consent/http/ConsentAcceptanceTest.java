package org.genomebridge.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.models.grammar.*;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ConsentAcceptanceTest extends ConsentServiceTest {


    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE =
            new DropwizardAppRule<>(ConsentApplication.class,
                    resourceFilePath("consent-config.yml"));


    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testCreateConsent() {
        Client client = ClientBuilder.newClient();
        Consent rec = new Consent(true,  new Everything(), null, null, UUID.randomUUID().toString());
        assertValidConsentResource(client, rec);
    }

    @Test
    public void testUpdateConsent() {
        Client client = ClientBuilder.newClient();
        Consent rec = new Consent(true,  new Everything(), null, null, UUID.randomUUID().toString());
        Response response = checkStatus(CREATED, post(client, consentPath(), rec));
        String createdLocation = checkHeader(response, "Location");
        Consent created = retrieveConsent(client, createdLocation);
        assertThat(created.requiresManualReview).isEqualTo(rec.requiresManualReview);
        assertThat(created.useRestriction).isEqualTo(rec.useRestriction);
        Consent update = new Consent(false,  new Nothing(), null, null, UUID.randomUUID().toString());
        check200(put(client, createdLocation, update));
        Consent updated = retrieveConsent(client, createdLocation);

        assertThat(updated.requiresManualReview).isEqualTo(update.requiresManualReview);
        assertThat(updated.useRestriction).isEqualTo(update.useRestriction);
    }

    @Test
    public void testOnlyOrNamedConsent() {
        Client client = ClientBuilder.newClient();
        Consent rec = new Consent(false, new Only("http://broadinstitute.org/ontology/consent/research_on", new Or(new Named("DOID:1"), new Named("DOID:2"))),
                                  null, null, UUID.randomUUID().toString());
        assertValidConsentResource(client, rec);
    }

    @Test
    public void testAndConsent() {
        Client client = ClientBuilder.newClient();
        Consent rec = new Consent(false, new And(new Named("DOID:1"), new Named("DOID:2")), null, null, UUID.randomUUID().toString());
        assertValidConsentResource(client, rec);
    }

    @Test
    public void testNotConsent() {
        Client client = ClientBuilder.newClient();
        Consent rec = new Consent(false, new Not(new Named("DOID:1")), null, null, UUID.randomUUID().toString());
        assertValidConsentResource(client, rec);
    }

    @Test
    public void testNothingConsent() {
        Client client = ClientBuilder.newClient();
        Consent rec = new Consent(false,  new Nothing(), null, null, UUID.randomUUID().toString());
        assertValidConsentResource(client, rec);
    }

    @Test
    public void testSomeConsent() {
        Client client = ClientBuilder.newClient();
        Consent rec = new Consent(false,  new Some(
                "http://broadinstitute.org/ontology/consent/research_on",
                new Named("DOID:1")), null, null, UUID.randomUUID().toString());
        assertValidConsentResource(client, rec);
    }


    private void assertValidConsentResource(Client client, Consent rec) {
        Response response = checkStatus(CREATED, post(client, consentPath(), rec));
        String createdLocation = checkHeader(response, "Location");
        Consent created = retrieveConsent(client, createdLocation);

        assertThat(created.requiresManualReview).isEqualTo(rec.requiresManualReview);
        assertThat(created.useRestriction).isEqualTo(rec.useRestriction);
    }

}
