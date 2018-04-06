package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.ElectionReview;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class ElectionReviewTest  extends ElectionReviewServiceTest {

    private static final String CONSENT_ID = "testId";

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testGetElectionsReviewEmpty() throws IOException {
        Client client = ClientBuilder.newClient();
        ElectionReview electionReview = getJson(client, electionReviewPath(CONSENT_ID, ElectionType.TRANSLATE_DUL.getValue())).readEntity(new GenericType<ElectionReview>() {
        });
        assertThat(electionReview).isNull();
    }

    @Test
    public void testGetElectionReview() throws IOException {
        Client client = ClientBuilder.newClient();
        createElection();
        ElectionReview electionReview = getJson(client, electionReviewPath(CONSENT_ID, ElectionType.TRANSLATE_DUL.getValue())).readEntity(new GenericType<ElectionReview>() {
        });
        assertThat(electionReview).isNotNull();
        assertThat(electionReview.getConsent().getConsentId().equals(CONSENT_ID));
        assertThat(electionReview.getElection().getReferenceId().equals(CONSENT_ID));
        assertThat(!electionReview.getReviewVote().isEmpty());
        deleteElection();
    }

    @Test
    public void testVerifyOpenElectionsWithoutElections() throws IOException {
        Client client = ClientBuilder.newClient();
        HashMap openElections = getJson(client, openElectionReviewPath()).readEntity(HashMap.class);
        assertThat(openElections.get("open").equals(Boolean.FALSE));
    }

    @Test
    public void testVerifyOpenElectionsWithExistentElections() throws IOException {
        Client client = ClientBuilder.newClient();
        createElection();
        HashMap openElections = getJson(client, openElectionReviewPath()).readEntity(HashMap.class);
        assertThat(openElections.get("open").equals(Boolean.TRUE));
        deleteElection();
    }

    @Test
    public void testGetElectionReviewByNonExistentElectionId() throws IOException {
        Client client = ClientBuilder.newClient();
        ElectionReview electionReview = getJson(client, electionReviewByElectionIdPath(123)).readEntity(new GenericType<ElectionReview>() {
        });
        assertThat(electionReview).isNull();
    }

    @Test
    public void testGetElectionReviewByExistentElectionId() throws IOException {
        Client client = ClientBuilder.newClient();
        Election election = createElection();
        ElectionReview electionReview = getJson(client, electionReviewByElectionIdPath(election.getElectionId())).readEntity(new GenericType<ElectionReview>() {
        });
        assertThat(electionReview).isNotNull();
        assertThat(electionReview.getElection().getElectionId().equals(election.getElectionId()));
        assertThat(electionReview.getElection().getReferenceId().equals(CONSENT_ID));
        assertThat(electionReview.getConsent().getConsentId().equals(CONSENT_ID));
        deleteElection();
    }

    @Test
    public void testGetLastClosedElectionReviewByReferenceId() throws IOException {
        Client client = ClientBuilder.newClient();
        mockValidateTokenResponse();
        createElection();
        ElectionReview electionReview = getJson(client, lastElectionReviewPath(CONSENT_ID)).readEntity(new GenericType<ElectionReview>() {
        });
        assertThat(electionReview).isNull();
        deleteElection();
    }


    private Election createElection() throws IOException {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setStatus(ElectionStatus.OPEN.getValue());
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        Response response = checkStatus(CREATED,
                post(client, electionConsentPath(CONSENT_ID), election));
        String createdLocation = checkHeader(response, "Location");
        return getJson(client, createdLocation).readEntity(Election.class);
    }

    private void deleteElection() throws IOException {
        Client client = ClientBuilder.newClient();
        mockValidateTokenResponse();
        List<Vote> votes = getJson(client, voteConsentPath(CONSENT_ID)).readEntity(new GenericType<List<Vote>>() {
        });
        Integer electionId = votes.get(0).getElectionId();
        for (Vote vote : votes) {
            checkStatus(OK,
                    delete(client, voteConsentIdPath(CONSENT_ID, vote.getVoteId())));
        }
        checkStatus(OK,
                delete(client, electionConsentPathById(CONSENT_ID, electionId)));

    }


}
