package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentAssociation;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.eclipse.jetty.http.HttpHeader;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

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


    @Before
    public void setup() throws Exception {
        mockTranslateResponse();
        mockValidateResponse();
    }

    //@Test
    public void testFindConsents() {
        Collection<String> ids = populateConsents();
        assertThat(ids.size() == N);

        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.
                target(path2Url("/consents")).
                queryParam("ids", StringUtils.join(ids, ","));
        Response response = webTarget.
                request(MediaType.APPLICATION_JSON_TYPE).
                header("REMOTE_USER", "testuser").
                get(Response.class);
        assertThat(response.getStatus() == OK);

        List<Consent> consents = response.readEntity(new GenericType<List<Consent>>() {});
        assertThat(consents.size() == N);
    }

    @Test
    public void testFindNoConsents() {
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(path2Url("/consents"));
        Response response = webTarget.
                request(MediaType.APPLICATION_JSON_TYPE).
                header("REMOTE_USER", "testuser").
                get(Response.class);
        assertThat(response.getStatus() == NOT_FOUND);
    }

    @Test
    public void testFindMissingConsents() {
        Collection<String> ids = Arrays.asList("missing-id1", "missing-id2", UUID.randomUUID().toString());
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.
                target(path2Url("/consents")).
                queryParam("ids", StringUtils.join(ids, ","));
        Response response = webTarget.
                request(MediaType.APPLICATION_JSON_TYPE).
                header("REMOTE_USER", "testuser").
                get(Response.class);
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

        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(path2Url("/consents/sample"));
        Response response = webTarget
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(Response.class);
        assertThat(response.getStatus() == OK);

        List<Consent> consents = response.readEntity(new GenericType<List<Consent>>() {});
        assertThat(consents.size() == N);
    }

    @Test
    public void testFindConsentsByBadAssociation() {
        Collection<String> ids = populateConsentAssociations();
        assertThat(ids.size() == N);

        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(path2Url("/consents/nothing"));
        Response response = webTarget.
                request(MediaType.APPLICATION_JSON_TYPE).
                header("REMOTE_USER", "testuser").
                get(Response.class);
        assertThat(response.getStatus() == NOT_FOUND);
    }

    @Test
    public void testFindConsentsWithoutAssociation() {
        Collection<String> ids = populateConsentAssociations();
        assertThat(ids.size() == N);

        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(path2Url("/consents/"));
        Response response = webTarget.
                request(MediaType.APPLICATION_JSON_TYPE).
                header("REMOTE_USER", "testuser").
                get(Response.class);
        assertThat(response.getStatus() == NOT_FOUND);
    }

    private Collection<String> populateConsentAssociations() {
        Collection<String> ids = populateConsents();
        Client client = ClientBuilder.newClient();
        for (String id : ids) {
            ConsentAssociation ca = new ConsentAssociation();
            ca.setAssociationType("sample");
            String element1 = "SM- "+Math.random();
            String element2 = "SM- "+Math.random();
            String element3 = "SM- "+Math.random();
            ca.setElements(Arrays.asList(element1,element2,element3));
            post(client, path2Url("/consent/") + id + "/association", Collections.singletonList(ca));
        }
        return ids;
    }

    private Collection<String> populateConsents() {
        Collection<String> ids = new ArrayList<>();
        Client client = ClientBuilder.newClient();
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
        consent.name = Math.random()+"Name";
        Timestamp createDate = new Timestamp(new Date().getTime());
        consent.setSortDate(createDate);
        consent.setLastUpdate(createDate);
        consent.setCreateDate(createDate);
        Response response = checkStatus(CREATED, post(client, consentPath, consent));
        String createdLocation = checkHeader(response, "Location");
        return createdLocation.substring(createdLocation.lastIndexOf("/") + 1);
    }

}