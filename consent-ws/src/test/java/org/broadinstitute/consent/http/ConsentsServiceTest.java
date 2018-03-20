package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.*;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
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

    @Test
    public void testFindConsents() throws IOException {
        Collection<String> ids = populateConsents();
        assertThat(ids.size()).isEqualTo(N);

        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.
                target(path2Url("/consents")).
                queryParam("ids", StringUtils.join(ids, ","));
        mockValidateTokenResponse();
        Response response = webTarget.
                request(MediaType.APPLICATION_JSON_TYPE).
                header("Authorization", "Bearer access-token").
                get(Response.class);
        assertThat(response.getStatus()).isEqualTo(OK);
        mockValidateTokenResponse();
        List<Consent> consents = response.readEntity(new GenericType<List<Consent>>() {});
        assertThat(consents.size()).isEqualTo(N);
    }

    @Test
    public void testFindConsentsWithNoIds() throws IOException {
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.
                target(path2Url("/consents")).
                queryParam("ids", "");
        mockValidateTokenResponse();
        Response response = webTarget.
                request(MediaType.APPLICATION_JSON_TYPE).
                header("Authorization", "Bearer access-token").
                get(Response.class);
        assertThat(response.getStatus()).isEqualTo(NOT_FOUND);
    }

    @Test
    public void testFindNoConsents() throws IOException {
        Client client = ClientBuilder.newClient();
        mockValidateTokenResponse();
        WebTarget webTarget = client.target(path2Url("/consents"));
        Response response = webTarget.
                request(MediaType.APPLICATION_JSON_TYPE).
                header("Authorization", "Bearer access-token").
                get(Response.class);
        assertThat(response.getStatus()).isEqualTo(NOT_FOUND);
    }

    @Test
    public void testFindMissingConsents() throws IOException {
        Collection<String> ids = Arrays.asList("missing-id1", "missing-id2", UUID.randomUUID().toString());
        Client client = ClientBuilder.newClient();
        mockValidateTokenResponse();
        WebTarget webTarget = client.
                target(path2Url("/consents")).
                queryParam("ids", StringUtils.join(ids, ","));
        Response response = webTarget.
                request(MediaType.APPLICATION_JSON_TYPE).
                header("Authorization", "Bearer access-token").
                get(Response.class);
        List<Consent> consents = response.readEntity(new GenericType<List<Consent>>() {});
        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(consents).isEmpty();
    }

    /**
     * Post a number of consents, then a number of consent associations.
     * Then find the associations that were just created.
     *
     * TODO: This test is broken. The asserts were never actually tested.
     * TODO: There is also a cartesian product bug in the endpoint:
     * See https://broadinstitute.atlassian.net/browse/GAWB-3313
     *
     * Additionally, this test is not idempotent with other tests. If there are consents with
     * sample associations, they will be retrieved here and throw off the count. Need a completely
     * clean db to test this feature.
     */
    @Test
    @Ignore
    public void testFindConsentsBySampleAssociation() throws IOException {
        Collection<String> ids = populateConsentAssociations();
        assertThat(ids.size()).isEqualTo(N);
        mockValidateTokenResponse();
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(path2Url("/consents/sample"));
        Response response = webTarget
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer access-token")
                .get(Response.class);
        assertThat(response.getStatus()).isEqualTo(OK);
        List<Consent> consents = response.readEntity(new GenericType<List<Consent>>() {});
        assertThat(consents.size()).isEqualTo(N);
    }

    @Test
    public void testFindConsentsByBadAssociation() throws IOException {
        Collection<String> ids = populateConsentAssociations();
        assertThat(ids.size()).isEqualTo(N);
        mockValidateTokenResponse();
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(path2Url("/consents/nothing"));
        Response response = webTarget.
                request(MediaType.APPLICATION_JSON_TYPE).
                header("Authorization", "Bearer access-token").
                get(Response.class);
        assertThat(response.getStatus()).isEqualTo(NOT_FOUND);
    }

    @Test
    public void testFindConsentsWithoutAssociation() throws IOException {
        Collection<String> ids = populateConsentAssociations();
        assertThat(ids.size()).isEqualTo(N);
        mockValidateTokenResponse();
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(path2Url("/consents/"));
        Response response = webTarget.
                request(MediaType.APPLICATION_JSON_TYPE).
                header("Authorization", "Bearer access-token").
                get(Response.class);
        assertThat(response.getStatus()).isEqualTo(NOT_FOUND);
    }

    private Collection<String> populateConsentAssociations() throws IOException {
        Collection<String> ids = populateConsents();
        Client client = ClientBuilder.newClient();
        for (String id : ids) {
            ConsentAssociation ca = new ConsentAssociation();
            ca.setAssociationType("sample");
            String element1 = "SM- "+Math.random();
            String element2 = "SM- "+Math.random();
            String element3 = "SM- "+Math.random();
            ca.setElements(Arrays.asList(element1,element2,element3));
            mockValidateTokenResponse();
            post(client, path2Url("/consent/") + id + "/association", Collections.singletonList(ca));
        }
        return ids;
    }

    private Collection<String> populateConsents() throws IOException {
        Collection<String> ids = new ArrayList<>();
        Client client = ClientBuilder.newClient();
        for (int i = 1; i <= N; i++) {
            ids.add(postConsent(client));
        }
        return ids;
    }

    private String postConsent(Client client) throws IOException {
        String consentPath = path2Url("/consent");
        DataUseDTO dataUse = new DataUseBuilder().setGeneralUse(true).build();
        Consent consent = generateNewConsent(new Everything(), dataUse);
        Response response = checkStatus(CREATED, post(client, consentPath, consent));
        String createdLocation = checkHeader(response, "Location");
        return createdLocation.substring(createdLocation.lastIndexOf("/") + 1);
    }

}