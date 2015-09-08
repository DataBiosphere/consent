package org.genomebridge.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.genomebridge.consent.http.enumeration.ElectionStatus;
import org.genomebridge.consent.http.enumeration.ElectionType;
import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.models.Vote;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DataRequestElectionTest extends ElectionVoteServiceTest {

    public static final int CREATED = Response.Status.CREATED.getStatusCode();
    public static final int OK = Response.Status.OK.getStatusCode();
    public static final int BADREQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    public static final int NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    private static final String DATA_REQUEST_ID = "1";
    private static final String DATA_REQUEST_ID_2 = "2";
    private static final String INVALID_DATA_REQUEST_ID = "invalidId";
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
    public void testCreateDataRequestElection() {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setStatus(ElectionStatus.OPEN.getValue());
        Response response = checkStatus(CREATED,
                post(client, electionDataRequestPath(DATA_REQUEST_ID), election));
        String createdLocation = checkHeader(response, "Location");
        Election created = retrieveElection(client, createdLocation);
        assertThat(created.getElectionType()).isEqualTo(ElectionType.DATA_ACCESS.getValue());
        assertThat(created.getStatus()).isEqualTo(ElectionStatus.OPEN.getValue());
        assertThat(created.getReferenceId()).isEqualTo(DATA_REQUEST_ID);
        assertThat(created.getCreateDate()).isNotNull();
        assertThat(created.getElectionId()).isNotNull();
        assertThat(created.getFinalRationale()).isNull();
        // try to create other election for the same data request
        checkStatus(BADREQUEST,
                post(client, electionDataRequestPath(DATA_REQUEST_ID), election));
        testUpdateDataRequestElection(created);
        deleteElection(created.getElectionId());
    }

    public void testUpdateDataRequestElection(Election created) {
        Client client = ClientBuilder.newClient();
        created.setFinalVote(true);
        created.setFinalRationale(FINAL_RATIONALE);
        checkStatus(OK, put(client, electionDataRequestPathById(DATA_REQUEST_ID, created.getElectionId()), created));
        created = retrieveElection(client, electionDataRequestPath(DATA_REQUEST_ID));
        assertThat(created.getElectionType()).isEqualTo(ElectionType.DATA_ACCESS.getValue());
        assertThat(created.getReferenceId()).isEqualTo(DATA_REQUEST_ID);
        assertThat(created.getCreateDate()).isNotNull();
        assertThat(created.getElectionId()).isNotNull();
        assertThat(created.getFinalRationale()).isEqualTo(FINAL_RATIONALE);
        assertThat(created.getFinalVote()).isTrue();

    }

    public void deleteElection(Integer electionId) {
        Client client = ClientBuilder.newClient();
        List<Vote> votes = getJson(client, voteDataRequestPath(DATA_REQUEST_ID)).readEntity(new GenericType<List<Vote>>() {
        });
        for (Vote vote : votes) {
            checkStatus(OK,
                    delete(client, voteConsentIdPath(DATA_REQUEST_ID, vote.getVoteId())));
        }
        checkStatus(OK,
                delete(client, electionDataRequestPathById(DATA_REQUEST_ID, electionId)));
    }

    @Test
    public void retrieveElectionWithInvalidConsentId() {
        Client client = ClientBuilder.newClient();
        checkStatus(NOT_FOUND,
                getJson(client, electionDataRequestPath(INVALID_DATA_REQUEST_ID)));
    }

    @Test
    public void testDataRequestElectionWithInvalidDataRequest() {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setStatus(ElectionStatus.OPEN.getValue());
        // should return 400 bad request because the data request id does not exist
        checkStatus(NOT_FOUND,
                post(client, electionDataRequestPath(INVALID_DATA_REQUEST_ID), election));
    }

    @Test
    public void testUpdateDataRequestElectionWithId() {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        // should return 400 bad request because the data request id does not exist
        checkStatus(NOT_FOUND,
                put(client, electionDataRequestPathById(INVALID_DATA_REQUEST_ID, 1010), election));
    }

    @Test
    public void testCreateDataRequestElectionWithInvalidStatus() {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setStatus(INVALID_STATUS);
        // should return 400 bad request because status is invalid
        checkStatus(BADREQUEST,
                post(client, electionDataRequestPath(DATA_REQUEST_ID_2), election));
    }

}
