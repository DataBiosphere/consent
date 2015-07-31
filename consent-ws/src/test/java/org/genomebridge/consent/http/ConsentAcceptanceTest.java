package org.genomebridge.consent.http;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.genomebridge.consent.http.models.*;
import org.genomebridge.consent.http.models.grammar.*;
import org.junit.ClassRule;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

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
        Client client = new Client();

        Consent rec = new Consent();
        rec.requiresManualReview = true;
        rec.useRestriction = new Everything();

        assertValidConsentResource(client, rec);
    }

    @Test
    public void testUpdateConsent() {
        Client client = new Client();

        Consent rec = new Consent();
        rec.requiresManualReview = true;
        rec.useRestriction = new Everything();

        ClientResponse response = checkStatus(CREATED, put(client, consentPath(), rec));

        String createdLocation = checkHeader(response, "Location");

        Consent created = retrieveConsent(client, createdLocation);

        assertThat(created.requiresManualReview).isEqualTo(rec.requiresManualReview);
        assertThat(created.useRestriction).isEqualTo(rec.useRestriction);

        Consent update = new Consent();
        update.requiresManualReview = false;
        update.useRestriction = new Nothing();

        check200(post(client, createdLocation, update));

        Consent updated = retrieveConsent(client, createdLocation);

        assertThat(updated.requiresManualReview).isEqualTo(update.requiresManualReview);
        assertThat(updated.useRestriction).isEqualTo(update.useRestriction);
    }

    @Test
    public void testOnlyOrNamedConsent() {
        Client client = new Client();

        Consent rec = new Consent();
        rec.requiresManualReview = false;
        rec.useRestriction = new Only(
                "http://broadinstitute.org/ontology/consent/research_on",
                new Or(new Named("DOID:1"), new Named("DOID:2"))
        );

        assertValidConsentResource(client, rec);
    }

    @Test
    public void testAndConsent() {
        Client client = new Client();

        Consent rec = new Consent();
        rec.requiresManualReview = false;
        rec.useRestriction = new And(new Named("DOID:1"), new Named("DOID:2"));

        assertValidConsentResource(client, rec);
    }

    @Test
    public void testNotConsent() {
        Client client = new Client();

        Consent rec = new Consent();
        rec.requiresManualReview = false;
        rec.useRestriction = new Not(new Named("DOID:1"));

        assertValidConsentResource(client, rec);
    }

    @Test
    public void testNothingConsent() {
        Client client = new Client();

        Consent rec = new Consent();
        rec.requiresManualReview = false;
        rec.useRestriction = new Nothing();

        assertValidConsentResource(client, rec);
    }

    @Test
    public void testSomeConsent() {
        Client client = new Client();

        Consent rec = new Consent();
        rec.requiresManualReview = false;
        rec.useRestriction = new Some(
                "http://broadinstitute.org/ontology/consent/research_on",
                new Named("DOID:1")
        );

        assertValidConsentResource(client, rec);
    }


    private void assertValidConsentResource(Client client, Consent rec) {
        ClientResponse response = checkStatus(CREATED, put(client, consentPath(), rec));

        String createdLocation = checkHeader(response, "Location");
        Consent created = retrieveConsent(client, createdLocation);

        assertThat(created.requiresManualReview).isEqualTo(rec.requiresManualReview);
        assertThat(created.useRestriction).isEqualTo(rec.useRestriction);
    }

}
