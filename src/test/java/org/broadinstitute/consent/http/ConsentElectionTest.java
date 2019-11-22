package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Election;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

public class ConsentElectionTest extends ElectionVoteServiceTest {

    public static final int CREATED = Response.Status.CREATED
            .getStatusCode();
    public static final int OK = Response.Status.OK.getStatusCode();
    private static final int BADREQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    public static final int NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    private static final String CONSENT_ID_2 = "testId2";
    private static final String INVALID_CONSENT_ID = "invalidId";
    private static final String INVALID_STATUS = "testStatus";

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void retrieveElectionWithInvalidConsentId() {
        Client client = ClientBuilder.newClient();
        checkStatus(NOT_FOUND,
                getJson(client, electionConsentPath(INVALID_CONSENT_ID)));
    }

    @Test
    public void testCreateConsentElectionWithInvalidConsent() {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        election.setStatus(ElectionStatus.OPEN.getValue());
        // should return 400 bad request because the consent id does not exist
        checkStatus(BADREQUEST,
                post(client, electionConsentPath(INVALID_CONSENT_ID), election));
    }

    @Test
    public void testUpdateConsentElectionWithInvalidId() {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        // should return 400 bad request because the election id does not exist
        checkStatus(NOT_FOUND,
                put(client, electionPathById(8555), election));
    }

    @Test
    public void testCreateConsentElectionWithInvalidStatus() {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        election.setStatus(INVALID_STATUS);
        // should return 400 bad request because status is invalid
        checkStatus(BAD_REQUEST,
                post(client, electionConsentPath(CONSENT_ID_2), election));
    }

}
