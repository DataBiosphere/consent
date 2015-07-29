package org.genomebridge.consent.http;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.genomebridge.consent.http.enumeration.ElectionStatus;
import org.genomebridge.consent.http.models.ConsentManage;
import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.models.Vote;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

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
    public void testConsentManage() {
        Integer electionId = createElection(CONSENT_ID);
        Client client = new Client();
        List<ConsentManage> consentManage = get(client, consentManagePath()).getEntity(new GenericType<List<ConsentManage>>() {
        });
        Assert.assertTrue(consentManage.size() > 0);
        Assert.assertTrue(consentManage.get(consentManage.size() - 1).getConsentId().equals(CONSENT_ID));
        Assert.assertTrue(consentManage.get(consentManage.size() - 1).getElectionStatus().equals(ElectionStatus.OPEN.getValue()));
        Assert.assertTrue(consentManage.get(consentManage.size() - 1).getElectionId().equals(electionId));
        Assert.assertNotNull(consentManage.get(0).getConsentId());
        Assert.assertTrue(consentManage.get(0).getElectionStatus().equals("un-reviewed"));
        Integer electionId_2 = createElection(CONSENT_ID_2);
        List<ConsentManage> consentManageUpdated = get(client, consentManagePath()).getEntity(new GenericType<List<ConsentManage>>() {
        });
        Assert.assertTrue(consentManageUpdated.size() > 1);
        Assert.assertTrue(consentManageUpdated.get(consentManage.size() - 2).getElectionStatus()
                .equals(ElectionStatus.OPEN.getValue()));
        Assert.assertTrue(consentManageUpdated.get(consentManage.size() - 2).getElectionStatus()
                .equals(ElectionStatus.OPEN.getValue()));
        List<Vote> votes = get(client, voteConsentPath(CONSENT_ID)).getEntity(new GenericType<List<Vote>>() {
        });
        deleteVotes(votes, CONSENT_ID);
        delete(client, electionConsentPathById(CONSENT_ID, electionId));
        List<Vote> votesElection2 = get(client, voteConsentPath(CONSENT_ID_2)).getEntity(new GenericType<List<Vote>>() {
        });
        deleteVotes(votesElection2, CONSENT_ID_2);
        delete(client, electionConsentPathById(CONSENT_ID_2, electionId_2));
    }

    public void deleteVotes(List<Vote> votes, String consentId) {
        Client client = new Client();
        for (Vote vote : votes) {
            checkStatus(
                    OK,
                    delete(client,
                            voteConsentIdPath(consentId, vote.getVoteId())));
        }

    }

    private Integer createElection(String consentId) {
        Client client = new Client();
        Election election = new Election();
        election.setStatus(ElectionStatus.OPEN.getValue());
        post(client, electionConsentPath(consentId), election);
        election = get(client, electionConsentPath(consentId)).getEntity(
                Election.class);
        return election.getElectionId();
    }

}
