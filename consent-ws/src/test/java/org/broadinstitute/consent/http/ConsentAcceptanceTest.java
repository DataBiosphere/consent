package org.broadinstitute.consent.http;

import org.broadinstitute.consent.http.models.grammar.Everything;
import org.broadinstitute.consent.http.models.grammar.Not;
import org.broadinstitute.consent.http.models.grammar.Nothing;
import org.broadinstitute.consent.http.models.grammar.And;
import org.broadinstitute.consent.http.models.grammar.Named;
import org.broadinstitute.consent.http.models.grammar.Some;
import org.broadinstitute.consent.http.models.grammar.Only;
import org.broadinstitute.consent.http.models.grammar.Or;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.Consent;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class ConsentAcceptanceTest extends ConsentServiceTest {

    Timestamp createDate = new Timestamp(new Date().getTime());



    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE =
            new DropwizardAppRule<>(ConsentApplication.class,
                    resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }


    @Before
    public void setup() throws Exception {
        mockTranslateResponse();
        mockValidateResponse();
    }

    @Test
    public void testCreateConsent() {
        Client client = ClientBuilder.newClient();
        Consent rec = new Consent(true,  new Everything(), null,  UUID.randomUUID().toString(), createDate, createDate, createDate);
        assertValidConsentResource(client, rec);
    }

    @Test
    public void testUpdateConsent() {
        Client client = ClientBuilder.newClient();
        Consent rec = new Consent(true,  new Everything(), null,  UUID.randomUUID().toString(), createDate, createDate, createDate);
        Response response = checkStatus(CREATED, post(client, consentPath(), rec));
        String createdLocation = checkHeader(response, "Location");
        Consent created = retrieveConsent(client, createdLocation);
        assertThat(created.requiresManualReview).isEqualTo(rec.requiresManualReview);
        assertThat(created.useRestriction).isEqualTo(rec.useRestriction);
        Consent update = new Consent(false,  new Nothing(), null,  UUID.randomUUID().toString(), createDate, createDate, createDate);
        check200(put(client, createdLocation, update));
        Consent updated = retrieveConsent(client, createdLocation);

        assertThat(updated.requiresManualReview).isEqualTo(update.requiresManualReview);
        assertThat(updated.useRestriction).isEqualTo(update.useRestriction);
    }

    @Test
    public void testDeleteConsent() {
        Client client = ClientBuilder.newClient();
        Consent rec = new Consent(true,  new Everything(), null,  UUID.randomUUID().toString(), createDate, createDate, createDate);
        Response response = checkStatus(CREATED, post(client, consentPath(), rec));
        String createdLocation = checkHeader(response, "Location");
        check200(delete(client, createdLocation));
    }


    @Test
    public void testOnlyOrNamedConsent() {
        Client client = ClientBuilder.newClient();
        Consent rec = new Consent(false, new Only("http://broadinstitute.org/ontology/consent/research_on", new Or(new Named("DOID:1"), new Named("DOID:2"))),
                                  null,  UUID.randomUUID().toString(), createDate, createDate, createDate);
        assertValidConsentResource(client, rec);
    }

    @Test
    public void testAndConsent() {
        Client client = ClientBuilder.newClient();
        Consent rec = new Consent(false, new And(new Named("DOID:1"), new Named("DOID:2")), null,  UUID.randomUUID().toString(), createDate, createDate, createDate);
        assertValidConsentResource(client, rec);
    }

    @Test
    public void testNotConsent() {
        Client client = ClientBuilder.newClient();
        Consent rec = new Consent(false, new Not(new Named("DOID:1")),  null, UUID.randomUUID().toString(), createDate, createDate, createDate);
        assertValidConsentResource(client, rec);
    }

    @Test
    public void testNothingConsent() {
        Client client = ClientBuilder.newClient();
        Consent rec = new Consent(false,  new Nothing(),  null, UUID.randomUUID().toString(), createDate, createDate, createDate);
        assertValidConsentResource(client, rec);
    }

    @Test
    public void testSomeConsent() {
        Client client = ClientBuilder.newClient();
        Consent rec = new Consent(false,  new Some(
                "http://broadinstitute.org/ontology/consent/research_on",
                new Named("DOID:1")), null, UUID.randomUUID().toString(), createDate, createDate, createDate);
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
