package org.genomebridge.consent.http;

import static org.fest.assertions.api.Assertions.assertThat;

import io.dropwizard.testing.junit.DropwizardAppRule;

import org.genomebridge.consent.http.enumeration.ElectionType;
import org.genomebridge.consent.http.enumeration.ElectionStatus;
import org.genomebridge.consent.http.models.Election;
import org.junit.ClassRule;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

public class ConsentElectionTest extends ElectionVoteServiceTest {

    public static final int CREATED = ClientResponse.Status.CREATED
            .getStatusCode();
    public static final int OK = ClientResponse.Status.OK.getStatusCode();
    public static final int BADREQUEST = ClientResponse.Status.BAD_REQUEST.getStatusCode();
    public static final int NOT_FOUND = ClientResponse.Status.NOT_FOUND.getStatusCode();
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
    public void testCreateConsentElection() {
        Client client = new Client();
        Election election = new Election();
        election.setStatus(ElectionStatus.OPEN.getValue());
        ClientResponse response = checkStatus(CREATED,
                post(client, electionConsentPath(CONSENT_ID), election));
        String createdLocation = checkHeader(response, "Location");
        Election created = retrieveElection(client, createdLocation);
        assertThat(created.getElectionType()).isEqualTo(
                ElectionType.TRANSLATE_DUL.getValue());
        assertThat(created.getStatus()).isEqualTo(ElectionStatus.OPEN.getValue());
        assertThat(created.getReferenceId()).isEqualTo(CONSENT_ID);
        assertThat(created.getCreateDate()).isNotNull();
        assertThat(created.getElectionId()).isNotNull();
        assertThat(created.getFinalRationale()).isNull();
        // try to create other election for the same consent
        checkStatus(BADREQUEST,
                post(client, electionConsentPath(CONSENT_ID), election));
        testUpdateConsentElection(created);
        deleteElection();
    }

    public void testUpdateConsentElection(Election created) {
        Client client = new Client();
        created.setFinalVote(true);
        created.setFinalRationale(FINAL_RATIONALE);
        created.setStatus(ElectionStatus.CLOSED.getValue());
        checkStatus(OK, put(client, electionConsentPathById(CONSENT_ID, created.getElectionId()), created));
        created = retrieveElection(client, electionConsentPath(CONSENT_ID));
        assertThat(created.getElectionType()).isEqualTo(
                ElectionType.TRANSLATE_DUL.getValue());
        assertThat(created.getStatus()).isEqualTo(ElectionStatus.CLOSED.getValue());
        assertThat(created.getReferenceId()).isEqualTo(CONSENT_ID);
        assertThat(created.getCreateDate()).isNotNull();
        assertThat(created.getElectionId()).isNotNull();
        assertThat(created.getFinalRationale()).isEqualTo(FINAL_RATIONALE);
        assertThat(created.getFinalVote()).isTrue();

    }

    public void deleteElection() {
        Client client = new Client();
        checkStatus(OK,
                delete(client, electionConsentPath(CONSENT_ID)));
    }

    @Test
    public void retrieveElectionWithInvalidConsentId() {
        Client client = new Client();
        checkStatus(NOT_FOUND,
                get(client, electionConsentPath(INVALID_CONSENT_ID)));
    }

    @Test
    public void testCreateConsentElectionWithInvalidConsent() {
        Client client = new Client();
        Election election = new Election();
        election.setStatus(ElectionStatus.OPEN.getValue());
        // should return 400 bad request because the consent id does not exist
        checkStatus(BADREQUEST,
                post(client, electionConsentPath(INVALID_CONSENT_ID), election));
    }

    @Test
    public void testUpdateConsentElectionWithInvalidConsent() {
        Client client = new Client();
        Election election = new Election();
        // should return 400 bad request because the consent id does not exist
        checkStatus(NOT_FOUND,
                put(client, electionConsentPathById(INVALID_CONSENT_ID, 8555), election));
    }

    @Test
    public void testCreateConsentElectionWithInvalidStatus() {
        Client client = new Client();
        Election election = new Election();
        election.setStatus(INVALID_STATUS);
        // should return 400 bad request because status is invalid
        checkStatus(BAD_REQUEST,
                post(client, electionConsentPath(CONSENT_ID_2), election));
    }

}
