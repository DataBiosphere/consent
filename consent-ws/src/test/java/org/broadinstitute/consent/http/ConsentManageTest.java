package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.ConsentManage;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import java.io.IOException;
import java.util.List;

public class ConsentManageTest extends ElectionVoteServiceTest {

    private static final String CONSENT_ID = "testId";
    private static final String CONSENT_ID_2 = "testId2";

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testConsentManage() throws IOException {
        Integer electionId = createElection(CONSENT_ID);
        Client client = ClientBuilder.newClient();
        List<ConsentManage> consentManage = getJson(client, consentManagePath()).readEntity(new GenericType<List<ConsentManage>>() { });
        Assert.assertTrue(consentManage.size() > 0);
        Assert.assertTrue(consentManage.get(0).getConsentId().equals(CONSENT_ID));
        Assert.assertTrue(consentManage.get(0).getElectionStatus().equals(ElectionStatus.OPEN.getValue()));
        Assert.assertTrue(consentManage.get(0).getElectionId().equals(electionId));

        Integer electionId_2 = createElection(CONSENT_ID_2);
        List<ConsentManage> consentManageUpdated = getJson(client, consentManagePath()).readEntity(new GenericType<List<ConsentManage>>() {
        });
        Assert.assertTrue(consentManageUpdated.size() > 1);
        Assert.assertTrue(consentManageUpdated.get(0).getElectionStatus()
                .equals(ElectionStatus.OPEN.getValue()));
        Assert.assertTrue(consentManageUpdated.get(0).getElectionStatus()
                .equals(ElectionStatus.OPEN.getValue()));
        List<Vote> votes = getJson(client, voteConsentPath(CONSENT_ID)).readEntity(new GenericType<List<Vote>>() { });
        deleteVotes(votes, CONSENT_ID);
        delete(client, electionConsentPathById(CONSENT_ID, electionId));
        List<Vote> votesElection2 = getJson(client, voteConsentPath(CONSENT_ID_2)).readEntity(new GenericType<List<Vote>>() {
        });
        deleteVotes(votesElection2, CONSENT_ID_2);
        delete(client, electionConsentPathById(CONSENT_ID_2, electionId_2));
    }

    public void deleteVotes(List<Vote> votes, String consentId) {
        Client client = ClientBuilder.newClient();
        votes.stream().forEach((vote) -> {
            try {
                checkStatus(
                        OK,
                        delete(client,
                                voteConsentIdPath(consentId, vote.getVoteId())));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    private Integer createElection(String consentId) throws IOException {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        election.setStatus(ElectionStatus.OPEN.getValue());
        post(client, electionConsentPath(consentId), election);
        election = getJson(client, electionConsentPath(consentId)).readEntity(Election.class);
        return election.getElectionId();
    }

}
