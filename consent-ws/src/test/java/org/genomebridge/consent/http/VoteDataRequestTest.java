package org.genomebridge.consent.http;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.List;

import io.dropwizard.testing.junit.DropwizardAppRule;

import org.genomebridge.consent.http.enumeration.ElectionStatus;
import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.models.PendingCase;
import org.genomebridge.consent.http.models.Vote;
import org.junit.ClassRule;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;

public class VoteDataRequestTest extends ElectionVoteServiceTest {

    public static final int CREATED = ClientResponse.Status.CREATED
            .getStatusCode();
    public static final int OK = ClientResponse.Status.OK.getStatusCode();
    public static final int BADREQUEST = ClientResponse.Status.BAD_REQUEST
            .getStatusCode();
    private static final String DATA_REQUEST_ID = "1";
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
        List<Vote> votes =  get(client, voteDataRequestPath(DATA_REQUEST_ID)).getEntity(new GenericType<List<Vote>>() {});
        Vote vote = new Vote();
        vote.setVote(false);
        vote.setRationale(RATIONALE);
        checkStatus(OK,
                post(client, voteDataRequestIdPath(DATA_REQUEST_ID,votes.get(0).getVoteId()), vote));
        //describe vote
        Vote created = retrieveVote(client, voteDataRequestIdPath(DATA_REQUEST_ID, votes.get(0).getVoteId()));
        assertThat(created.getElectionId()).isEqualTo(electionId);
        assertThat(created.getRationale()).isEqualTo(RATIONALE);
        assertThat(created.getUpdateDate()).isNull();
        assertThat(created.getVote()).isFalse();
        assertThat(created.getVoteId()).isNotNull();
        testDataRequestPendingCase(DAC_USER_ID);
        updateVote(created.getVoteId(), created);
        deleteVotes(votes);
        delete(client, electionDataRequestPathById(DATA_REQUEST_ID, electionId));
     
    }

   
    public void deleteVotes(List<Vote> votes) {
        Client client = new Client();
        for(Vote vote : votes){
        	checkStatus(OK,
                    delete(client, voteDataRequestIdPath(DATA_REQUEST_ID, vote.getVoteId())));	
        }
        
    }

    public void updateVote(Integer id, Vote vote) {
        Client client = new Client();
        vote.setVote(true);
        vote.setRationale(null);
        checkStatus(OK,
                put(client, voteDataRequestIdPath(DATA_REQUEST_ID, id), vote));
        vote = retrieveVote(client, voteDataRequestIdPath(DATA_REQUEST_ID, id));
        assertThat(vote.getRationale()).isNull();
        assertThat(vote.getUpdateDate()).isNotNull();
        assertThat(vote.getVote()).isTrue();

    }

    private Integer createElection() {
        Client client = new Client();
        Election election = new Election();
        election.setStatus(ElectionStatus.OPEN.getValue());
        post(client, electionDataRequestPath(DATA_REQUEST_ID), election);
        election = get(client, electionDataRequestPath(DATA_REQUEST_ID))
                .getEntity(Election.class);
        return election.getElectionId();
    }
    

    public void testDataRequestPendingCase(Integer dacUserId) {
        Client client = new Client();
        List<PendingCase> pendingCases = get(client, dataRequestPendingCasesPath(dacUserId)).getEntity(new GenericType<List<PendingCase>>() {});
        assertThat(pendingCases).isNotNull();
        assertThat(pendingCases.size()).isEqualTo(1);
        assertThat(pendingCases.get(0).getAlreadyVoted()).isEqualTo(false);
        assertThat(pendingCases.get(0).getLogged()).isEqualTo("1/2");
        assertThat(pendingCases.get(0).getReferenceId()).isEqualTo(DATA_REQUEST_ID);
        

    }

    @Test
    public void testDataRequestPendingCaseWithInvalidUser() {
        Client client = new Client();
        List<PendingCase> pendingCases = get(client, dataRequestPendingCasesPath(789)).getEntity(new GenericType<List<PendingCase>>() {});
        assertThat(pendingCases).isEmpty();

    }

}
