package org.genomebridge.consent.http;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang.StringUtils;
import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.models.Everything;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

public class ConsentsServiceTest extends AbstractTest {

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
        assertThat(ids.size() == 5);

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
        assertThat(consents.size() == 5);
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
        Collection<String> ids = new ArrayList<>();
        ids.add("missing-id1");
        ids.add("missing-id2");
        ids.add("missing-id3");
        ids.add("missing-id4");
        ids.add("missing-id5");
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

    private Collection<String> populateConsents() {
        Collection<String> ids = new ArrayList<>();
        String consentPath = path2Url("/consent");
        Client client = new Client();
        for (int i = 1; i <= 5; i++ ) {
            Consent consent = new Consent();
            consent.requiresManualReview = true;
            consent.useRestriction = new Everything();
            ClientResponse response = checkStatus( CREATED, put(client, consentPath, consent) );
            String createdLocation = checkHeader(response, "Location");
            String id = createdLocation.substring(createdLocation.lastIndexOf("/")+1);
            ids.add(id);
        }
        return ids;
    }

}
