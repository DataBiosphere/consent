package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ConsentElectionTest extends ElectionVoteServiceTest {

    public static final int CREATED = Response.Status.CREATED
            .getStatusCode();
    public static final int OK = Response.Status.OK.getStatusCode();
    public static final int BADREQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    public static final int NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    private static final String CONSENT_ID = "testId";
    private static final String CONSENT_ID_2 = "testId2";
    private static final String INVALID_CONSENT_ID = "invalidId";
    private static final String INVALID_STATUS = "testStatus";
    private static final String FINAL_RATIONALE = "Test";

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testCreateConsentElection() throws IOException {
        Client client = ClientBuilder.newClient();
        Election created = createElection(CONSENT_ID);
        assertThat(created.getElectionType()).isEqualTo(
                ElectionType.TRANSLATE_DUL.getValue());
        assertThat(created.getStatus()).isEqualTo(ElectionStatus.OPEN.getValue());
        assertThat(created.getReferenceId()).isEqualTo(CONSENT_ID);
        assertThat(created.getCreateDate()).isNotNull();
        assertThat(created.getElectionId()).isNotNull();
        assertThat(created.getFinalRationale()).isNull();
        // try to create other election for the same consent
        checkStatus(BADREQUEST,
                post(client, electionConsentPath(CONSENT_ID), created));
        testUpdateConsentElection(created);
        deleteElection(created.getElectionId(), CONSENT_ID);
    }

    public void testUpdateConsentElection(Election created) throws IOException {
        Client client = ClientBuilder.newClient();
        created.setFinalVote(true);
        created.setFinalRationale(FINAL_RATIONALE);
        checkStatus(OK, put(client, electionPathById(created.getElectionId()), created));
        created = retrieveElection(client, electionPathById(created.getElectionId()));
        assertThat(created.getElectionType()).isEqualTo(
                ElectionType.TRANSLATE_DUL.getValue());
        assertThat(created.getReferenceId()).isEqualTo(CONSENT_ID);
        assertThat(created.getCreateDate()).isNotNull();
        assertThat(created.getElectionId()).isNotNull();
    }

    public void deleteElection(Integer electionId, String consentId) throws IOException {
        Client client = ClientBuilder.newClient();
        List<Vote> votes = getJson(client, voteConsentPath(consentId)).readEntity(new GenericType<List<Vote>>() {
        });
        for (Vote vote : votes) {
            checkStatus(OK,
                    delete(client, voteConsentIdPath(consentId, vote.getVoteId())));
        }
        checkStatus(OK,
                delete(client, electionConsentPathById(consentId, electionId)));

    }

    @Test
    public void retrieveElectionWithInvalidConsentId() throws IOException {
        Client client = ClientBuilder.newClient();
        checkStatus(NOT_FOUND,
                getJson(client, electionConsentPath(INVALID_CONSENT_ID)));
    }

    @Test
    public void testCreateConsentElectionWithInvalidConsent() throws IOException {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        election.setStatus(ElectionStatus.OPEN.getValue());
        // should return 400 bad request because the consent id does not exist
        checkStatus(BADREQUEST,
                post(client, electionConsentPath(INVALID_CONSENT_ID), election));
    }

    @Test
    public void testUpdateConsentElectionWithInvalidId() throws IOException {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        // should return 400 bad request because the election id does not exist
        checkStatus(NOT_FOUND,
                put(client, electionPathById(8555), election));
    }

    @Test
    public void testCreateConsentElectionWithInvalidStatus() throws IOException {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        election.setStatus(INVALID_STATUS);
        // should return 400 bad request because status is invalid
        checkStatus(BAD_REQUEST,
                post(client, electionConsentPath(CONSENT_ID_2), election));
    }

    public Election createElection(String consentId) throws IOException {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setStatus(ElectionStatus.OPEN.getValue());
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        Response response = checkStatus(CREATED,
                post(client, electionConsentPath(consentId), election));
        String createdLocation = checkHeader(response, "Location");
        return retrieveElection(client, createdLocation);
    }

}
