package org.broadinstitute.consent.http;


import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.PendingCase;
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

public class VoteConsentTest extends ElectionVoteServiceTest {

    public static final int CREATED = Response.Status.CREATED.getStatusCode();
    public static final int OK = Response.Status.OK.getStatusCode();
    private static final String CONSENT_ID = "testId";
    private static final Integer DAC_USER_ID = 1;
    private static final String RATIONALE = "Test";

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testCreateConsentVote() throws IOException {
        // should exist an election for specified consent
        mockValidateTokenResponse();
        Integer electionId = createElection();
        Client client = ClientBuilder.newClient();
        List<Vote> votes = getJson(client, voteConsentPath(CONSENT_ID)).readEntity(new GenericType<List<Vote>>() {
        });
        Vote vote = new Vote();
        vote.setVote(false);
        vote.setRationale(RATIONALE);
        checkStatus(OK,
                post(client, voteConsentIdPath(CONSENT_ID, votes.get(0).getVoteId()), vote));
        mockValidateTokenResponse();
        Vote created = retrieveVote(client, voteConsentIdPath(CONSENT_ID, votes.get(0).getVoteId()));
        assertThat(created.getElectionId()).isEqualTo(electionId);
        assertThat(created.getRationale()).isEqualTo(RATIONALE);
        assertThat(created.getUpdateDate()).isNull();
        assertThat(created.getVote()).isFalse();
        assertThat(created.getVoteId()).isNotNull();
        testConsentPendingCase(DAC_USER_ID);
        updateVote(created);
        deleteVotes(votes);
        delete(client, electionConsentPathById(CONSENT_ID, electionId));
    }


    public void deleteVotes(List<Vote> votes) throws IOException {
        Client client = ClientBuilder.newClient();
        for (Vote vote : votes) {
            checkStatus(OK,
                    delete(client, voteConsentIdPath(CONSENT_ID, vote.getVoteId())));
        }
    }


    public void updateVote(Vote vote) throws IOException {
        Client client = ClientBuilder.newClient();
        vote.setVote(true);
        vote.setRationale(null);
        checkStatus(OK,
                put(client, voteConsentIdPath(CONSENT_ID, vote.getVoteId()), vote));
        mockValidateTokenResponse();
        vote = retrieveVote(client, voteConsentIdPath(CONSENT_ID, vote.getVoteId()));
        assertThat(vote.getRationale()).isNull();
        assertThat(vote.getUpdateDate()).isNotNull();
        assertThat(vote.getVote()).isTrue();
    }

    private Integer createElection() throws IOException {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setStatus(ElectionStatus.OPEN.getValue());
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        post(client, electionConsentPath(CONSENT_ID), election);
        election = getJson(client, electionConsentPath(CONSENT_ID))
                .readEntity(Election.class);
        return election.getElectionId();
    }

    public void testConsentPendingCase(Integer dacUserId) throws IOException {
        Client client = ClientBuilder.newClient();
        List<PendingCase> pendingCases = getJson(client, consentPendingCasesPath(dacUserId)).readEntity(new GenericType<List<PendingCase>>() {
        });
        assertThat(pendingCases).isNotNull();
        assertThat(pendingCases.size()).isEqualTo(2);
        assertThat(pendingCases.get(0).getLogged()).isEqualTo("1/5");
        assertThat(pendingCases.get(0).getReferenceId()).isEqualTo(CONSENT_ID);
    }

    @Test
    public void testConsentPendingCaseWithInvalidUser() throws IOException {
        Client client = ClientBuilder.newClient();
        List<PendingCase> pendingCases = getJson(client, consentPendingCasesPath(789)).readEntity(new GenericType<List<PendingCase>>() {
        });
        assertThat(pendingCases).isEmpty();
    }

}