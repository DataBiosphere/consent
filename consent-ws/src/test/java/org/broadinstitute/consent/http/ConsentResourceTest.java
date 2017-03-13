package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Election;
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

import static org.junit.Assert.assertEquals;
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

        // It should not be returned at this point, because there's no election for it
        assertEquals("should be a bad request when no election", BAD_REQUEST, response.getStatus());

        // insert an open election - not approved yet
        Election election = createElection(client, location);

        // re-query, should still be BAD_REQUEST
        mockValidateTokenResponse();
        Response response2 = webTarget.request(MediaType.APPLICATION_JSON_TYPE).
                header("Authorization", "Bearer access-token").get(Response.class);
        assertEquals("should be a bad request when election is still open", BAD_REQUEST, response2.getStatus());

        // update election to be closed/approved
        updateElection(client, election.getElectionId());

        // re-query, should be OK
        mockValidateTokenResponse();
        Response response3 = webTarget.request(MediaType.APPLICATION_JSON_TYPE).
                header("Authorization", "Bearer access-token").get(Response.class);
        assertEquals("should be OK once election is final and approved", OK, response3.getStatus());

        Consent consent = response3.readEntity(Consent.class);
        assertNotNull(consent);
        // And that the location (ID) and name are what we expect
        assertTrue(location.equals(consent.consentId));
        assertTrue(name.equals(consent.name));
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

    private Election createElection(Client client, String consentId) throws IOException {
        String electionPath = path2Url( "/consent/"+consentId+"/election");
        Election election = new Election();
        election.setStatus(ElectionStatus.OPEN.getValue());
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        election.setReferenceId(consentId);
        Response response = checkStatus(CREATED, post(client, electionPath, election));
        String createdLocation = checkHeader(response, "Location");
        Election created = getJson(client, createdLocation).readEntity(Election.class);
        return created;
    }

    private Election updateElection(Client client, Integer electionId) throws IOException {
        String electionPath = path2Url( "/election/"+electionId);
        Election election = getJson(client, electionPath).readEntity(Election.class);

        election.setStatus(ElectionStatus.FINAL.getValue());
        election.setFinalVote(true);
        election.setFinalRationale("Unit test");
        checkStatus(OK, put(client, electionPath, election));

        Election updated = getJson(client, electionPath).readEntity(Election.class);
        return updated;
    }

}
