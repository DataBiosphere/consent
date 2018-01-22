package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.*;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AbstractVoteAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.VoteAPI;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class ConsentResourceTest extends AbstractTest {

    private String name;

    private ElectionAPI electionAPI = AbstractElectionAPI.getInstance();
    private VoteAPI voteAPI = AbstractVoteAPI.getInstance();

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
        Election election = createElection(location);

        // re-query, should still be BAD_REQUEST
        mockValidateTokenResponse();
        Response response2 = webTarget.request(MediaType.APPLICATION_JSON_TYPE).
                header("Authorization", "Bearer access-token").get(Response.class);
        assertEquals("should be a bad request when election is still open", BAD_REQUEST, response2.getStatus());

        // update election to be closed/approved
        updateElection(location, election.getElectionId());

        // re-query, should be OK
        mockValidateTokenResponse();
        Response response3 = webTarget.request(MediaType.APPLICATION_JSON_TYPE).
                header("Authorization", "Bearer access-token").get(Response.class);
        assertEquals("should be OK once election is final and approved", OK, response3.getStatus());

        Consent consent = response3.readEntity(Consent.class);
        assertNotNull(consent);
        // And that the location (ID) and name are what we expect
        assertTrue(location.equals(consent.consentId));
        assertEquals(name, consent.name);
    }

    private String createConsent(Client client) throws IOException {
        String consentPath = path2Url("/consent");
        DataUseDTO dataUse = new DataUseBuilder().setGeneralUse(true).build();
        Consent consent = generateNewConsent(new Everything(), dataUse);
        consent.setName(name);
        consent.setTranslatedUseRestriction("translated");
        Response response = checkStatus(CREATED, post(client, consentPath, consent));
        String createdLocation = checkHeader(response, "Location");
        return createdLocation.substring(createdLocation.lastIndexOf("/") + 1);
    }

    // drop to the apis for the create/update election methods.
    // we are testing consent-by-name, not election/vote updates
    private Election createElection(String consentId) throws Exception {
        Election election = new Election();
        election.setStatus(ElectionStatus.OPEN.getValue());
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        election.setReferenceId(consentId);

        Election created = electionAPI.createElection(election, consentId, ElectionType.TRANSLATE_DUL);
        assertNotNull("API should create election", created);
        return created;
    }

    private Election updateElection(String consentId, Integer electionId) throws IOException {
        Election election = electionAPI.describeElectionById(electionId);
        assertNotNull("existing election should exist", election);

        voteAPI.createVotes(electionId, ElectionType.TRANSLATE_DUL, false);

        List<Vote> votes = voteAPI.describeVotes(consentId);
        assertFalse("existing votes should exist", votes.isEmpty());
        for (Vote vote : votes) {
            vote.setHasConcerns(false);
            vote.setVote(true);
            vote.setRationale("unit test");
            voteAPI.updateVote(vote, vote.getVoteId(), consentId);
        }

        election.setFinalRationale("unit test");
        election.setFinalVote(true);
        election.setFinalAccessVote(true);
        election.setStatus(ElectionStatus.CLOSED.getValue());
        Election updated = electionAPI.updateElectionById(election, election.getElectionId());
        assertNotNull("API should update election", updated);

        return updated;
    }

}
