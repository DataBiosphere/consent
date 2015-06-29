package org.genomebridge.consent.http;

import static org.fest.assertions.api.Assertions.assertThat;

import io.dropwizard.testing.junit.DropwizardAppRule;

import org.genomebridge.consent.http.enumeration.Status;
import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.models.Vote;
import org.junit.ClassRule;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

public class VoteDataRequestTest extends ElectionVoteServiceTest {

    public static final int CREATED = ClientResponse.Status.CREATED
            .getStatusCode();
    public static final int OK = ClientResponse.Status.OK.getStatusCode();
    public static final int BADREQUEST = ClientResponse.Status.BAD_REQUEST
            .getStatusCode();
    private static final String DATA_REQUEST_ID = "1";
    private static final Integer INVALID_ID = 0;
    private static final Integer DAC_USER_ID = 2;
    private static final String RATIONALE = "Test";

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testCreateDataRequestVote() {
        // should exist an election for specified data request
        Integer electionId = createElection();
        Client client = new Client();
        Vote vote = new Vote();
        vote.setDacUserId(DAC_USER_ID);
        vote.setVote(false);
        vote.setRationale(RATIONALE);
        ClientResponse response = checkStatus(CREATED,
                post(client, voteDataRequestPath(DATA_REQUEST_ID), vote));
        String createdLocation = checkHeader(response, "Location");
        //describe vote
        Vote created = retrieveVote(client, createdLocation);
        assertThat(created.getDacUserId()).isEqualTo(DAC_USER_ID);
        assertThat(created.getElectionId()).isEqualTo(electionId);
        assertThat(created.getRationale()).isEqualTo(RATIONALE);
        assertThat(created.getUpdateDate()).isNull();
        assertThat(created.getVote()).isFalse();
        assertThat(created.getVoteId()).isNotNull();
        updateVote(created.getVoteId().toString(), created);
        deleteVote(created.getVoteId().toString());
        delete(client, electionDataRequestPath(DATA_REQUEST_ID));
    }

    @Test
    public void testCreateDataRequestVoteWithInvalidDacUser() {
        // should exist an election for specified data request
        createElection();
        Client client = new Client();
        Vote vote = new Vote();
        vote.setDacUserId(INVALID_ID);
        vote.setVote(false);
        vote.setRationale(RATIONALE);
        checkStatus(BADREQUEST,
                post(client, voteDataRequestPath(DATA_REQUEST_ID), vote));
        delete(client, electionDataRequestPath(DATA_REQUEST_ID));
    }

    @Test
    public void testCreateDataRequestVoteWithoutElection() {
        Client client = new Client();
        Vote vote = new Vote();
        vote.setDacUserId(INVALID_ID);
        vote.setVote(false);
        vote.setRationale(RATIONALE);
        checkStatus(BADREQUEST,
                post(client, voteDataRequestPath(DATA_REQUEST_ID), vote));
    }

    public void deleteVote(String id) {
        Client client = new Client();
        checkStatus(OK,
                delete(client, voteDataRequestIdPath(DATA_REQUEST_ID, id)));
    }

    public void updateVote(String id, Vote vote) {
        Client client = new Client();
        vote.setVote(true);
        vote.setRationale(null);
        checkStatus(OK,
                put(client, voteDataRequestIdPath(DATA_REQUEST_ID, id), vote));
        vote = retrieveVote(client, voteDataRequestIdPath(DATA_REQUEST_ID, id));
        assertThat(vote.getDacUserId()).isEqualTo(DAC_USER_ID);
        assertThat(vote.getRationale()).isNull();
        assertThat(vote.getUpdateDate()).isNotNull();
        assertThat(vote.getVote()).isTrue();

    }

    private Integer createElection() {
        Client client = new Client();
        Election election = new Election();
        election.setStatus(Status.OPEN.getValue());
        post(client, electionDataRequestPath(DATA_REQUEST_ID), election);
        election = get(client, electionDataRequestPath(DATA_REQUEST_ID))
                .getEntity(Election.class);
        return election.getElectionId();
    }

}
