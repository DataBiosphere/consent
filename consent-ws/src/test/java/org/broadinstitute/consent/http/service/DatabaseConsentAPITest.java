package org.broadinstitute.consent.http.service;

import io.dropwizard.testing.junit.DropwizardAppRule;

import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.ConsentServiceTest;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.*;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.BatchUpdateException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;


public class DatabaseConsentAPITest extends ConsentServiceTest {

    private DataUseDTO generalUse = new DataUseBuilder().setGeneralUse(true).build();
    private final UseRestriction everything = new Everything();
    private static final String CONSENT_ID = "testId";
    private static final String CONSENT_DULNAME = "consent-dul";
    private static final String CONSENT_DUL_URL = "consent-url";
    private static final String ELECTION_DULNAME = "election-dul";
    private static final String ELECTION_DUL_URL = "election-url";

    private ConsentAPI databaseConsentApi = DatabaseConsentAPI.getInstance();

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE =
            new DropwizardAppRule<>(ConsentApplication.class,
                    resourceFilePath("consent-config.yml"));
    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }


    @Test (expected = UnknownIdentifierException.class)
    public void testRetrieveUnkownIdentifier() throws UnknownIdentifierException {
        databaseConsentApi.retrieve("non-existent Id");
    }

    @Test
    public void testRetrieveElectionDulWithElectionId() throws IOException, UnknownIdentifierException {
        Client client = ClientBuilder.newClient();
        Consent consent = generateNewConsent(everything, generalUse);
        Election election = createElection(CONSENT_ID, client);

        databaseConsentApi.retrieveElectionDul(election.getElectionId(), consent);

        Consent retrievedConsent = databaseConsentApi.retrieve(CONSENT_ID);

        assertThat(retrievedConsent.getDulName().equals(election.getDulName()));

        deleteElection(CONSENT_ID, election.getElectionId(), client);
    }

    @Test
    public void testRetrieveElectionDulWithoutElectionId() throws IOException, UnknownIdentifierException {
        Client client = ClientBuilder.newClient();
        Consent consent = generateNewConsent(everything, generalUse);
        Election election = createElection(CONSENT_ID, client);

        databaseConsentApi.retrieveElectionDul(null, consent);

        Consent retrievedConsent = databaseConsentApi.retrieve(CONSENT_ID);

        assertThat(retrievedConsent.getDulName().equals(election.getDulName()));

        deleteElection(CONSENT_ID, election.getElectionId(), client);
    }


    private Election createElection(String consentId, Client client) throws IOException {
        Election election = new Election();
        election.setDulName(ELECTION_DULNAME);
        election.setDataUseLetter(ELECTION_DUL_URL);
        election.setStatus(ElectionStatus.OPEN.getValue());
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        Response response = checkStatus(CREATED,
                post(client, electionConsentPath(consentId), election));
        String createdLocation = checkHeader(response, "Location");
        return getJson(client, createdLocation).readEntity(Election.class);
    }

    private void deleteElection(String consentId, Integer electionId, Client client) throws IOException {
        mockValidateTokenResponse();
        List<Vote> votes = getJson(client, voteConsentPath(consentId)).readEntity(new GenericType<List<Vote>>() {
        });

        for (Vote vote : votes) {
            checkStatus(OK,
                    delete(client, voteConsentIdPath(consentId, vote.getVoteId())));
        }

        checkStatus(OK, delete(client, electionConsentPathById(consentId, electionId)));
    }

    Consent generateNewConsent(UseRestriction useRestriction, DataUseDTO dataUse) {
        Timestamp createDate = new Timestamp(new Date().getTime());
        return new ConsentBuilder().
                setConsentId(CONSENT_ID).
                setRequiresManualReview(false).
                setUseRestriction(useRestriction).
                setDataUse(dataUse).
                setName(UUID.randomUUID().toString()).
                setCreateDate(createDate).
                setLastUpdate(createDate).
                setSortDate(createDate).
                setDulName(CONSENT_DULNAME).
                setDataUseLetter(CONSENT_DUL_URL).
                build();
    }

}
