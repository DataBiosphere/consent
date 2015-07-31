package org.genomebridge.consent.http;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang.StringUtils;
import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.models.ConsentAssociation;
import org.genomebridge.consent.http.models.grammar.Everything;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.util.*;

import static org.fest.assertions.api.Assertions.assertThat;

public class ConsentsServiceTest extends AbstractTest {

    private static final int N = 5;

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE =
            new DropwizardAppRule<>(ConsentApplication.class,
                    resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testFindConsents() {
        Collection<String> ids = populateConsents();
        assertThat(ids.size() == N);

        Client client = new Client();
        WebResource webResource = client.
                resource(path2Url("/consents")).
                queryParam("ids", StringUtils.join(ids, ","));
        ClientResponse response = webResource.
                accept(MediaType.APPLICATION_JSON_TYPE).
                header("REMOTE_USER", "testuser").
                get(ClientResponse.class);
        assertThat(response.getStatus() == OK);

        List<Consent> consents = webResource.get(new GenericType<List<Consent>>() {});
        assertThat(consents.size() == N);
    }

    @Test
    public void testFindNoConsents() {
        Client client = new Client();
        WebResource webResource = client.resource(path2Url("/consents"));
        ClientResponse response = webResource.
                accept(MediaType.APPLICATION_JSON_TYPE).
                header("REMOTE_USER", "testuser").
                get(ClientResponse.class);
        assertThat(response.getStatus() == NOT_FOUND);
    }

    @Test
    public void testFindMissingConsents() {
        Collection<String> ids = Arrays.asList("missing-id1", "missing-id2", UUID.randomUUID().toString());
        Client client = new Client();
        WebResource webResource = client.
                resource(path2Url("/consents")).
                queryParam("ids", StringUtils.join(ids, ","));
        ClientResponse response = webResource.
                accept(MediaType.APPLICATION_JSON_TYPE).
                header("REMOTE_USER", "testuser").
                get(ClientResponse.class);
        assertThat(response.getStatus() == NOT_FOUND);
    }

    /**
     * Post a number of consents, then a number of consent associations.
     * Then find the associations that were just created.
     */
    @Test
    public void testFindConsentsBySampleAssociation() {
        Collection<String> ids = populateConsentAssociations();
        assertThat(ids.size() == N);

        Client client = new Client();
        WebResource webResource = client.resource(path2Url("/consents/sample"));
        ClientResponse response = webResource.
                accept(MediaType.APPLICATION_JSON_TYPE).
                header("REMOTE_USER", "testuser").
                get(ClientResponse.class);
        assertThat(response.getStatus() == OK);

        List<Consent> consents = webResource.get(new GenericType<List<Consent>>() {});
        assertThat(consents.size() == N);
    }

    @Test
    public void testFindConsentsByBadAssociation() {
        Collection<String> ids = populateConsentAssociations();
        assertThat(ids.size() == N);

        Client client = new Client();
        WebResource webResource = client.resource(path2Url("/consents/nothing"));
        ClientResponse response = webResource.
                accept(MediaType.APPLICATION_JSON_TYPE).
                header("REMOTE_USER", "testuser").
                get(ClientResponse.class);
        assertThat(response.getStatus() == NOT_FOUND);
    }

    @Test
    public void testFindConsentsWithoutAssociation() {
        Collection<String> ids = populateConsentAssociations();
        assertThat(ids.size() == N);

        Client client = new Client();
        WebResource webResource = client.resource(path2Url("/consents/"));
        ClientResponse response = webResource.
                accept(MediaType.APPLICATION_JSON_TYPE).
                header("REMOTE_USER", "testuser").
                get(ClientResponse.class);
        assertThat(response.getStatus() == NOT_FOUND);
    }

    private Collection<String> populateConsentAssociations() {
        Collection<String> ids = populateConsents();
        Client client = new Client();
        for (String id : ids) {
            ConsentAssociation ca = new ConsentAssociation();
            ca.setAssociationType("sample");
            ca.setElements(Arrays.asList("SM-1", "SM-2", "SM-3"));
            post(client, path2Url("/consent/") + id + "/association", Collections.singletonList(ca));
        }
        return ids;
    }

    private Collection<String> populateConsents() {
        Collection<String> ids = new ArrayList<>();
        Client client = new Client();
        for (int i = 1; i <= N; i++) {
            ids.add(postConsent(client));
        }
        return ids;
    }

    private String postConsent(Client client) {
        String consentPath = path2Url("/consent");
        Consent consent = new Consent();
        consent.requiresManualReview = true;
        consent.useRestriction = new Everything();
        ClientResponse response = checkStatus(CREATED, put(client, consentPath, consent));
        String createdLocation = checkHeader(response, "Location");
        return createdLocation.substring(createdLocation.lastIndexOf("/") + 1);
    }

}
