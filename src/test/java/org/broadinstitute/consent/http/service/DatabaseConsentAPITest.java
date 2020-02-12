package org.broadinstitute.consent.http.service;

import io.dropwizard.testing.junit.DropwizardAppRule;

import org.broadinstitute.consent.http.AbstractTest;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.*;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;


public class DatabaseConsentAPITest extends AbstractTest {

    private DataUse generalUse = new DataUseBuilder().setGeneralUse(true).build();
    private final UseRestriction everything = new Everything();
    private static final String CONSENT_ID = "testId";
    private static final String CONSENT_DULNAME = "consent-dul";
    private static final String CONSENT_DUL_URL = "consent-url";

    private static final String ELECTION_DULNAME_V1 = "election-dul-version1";
    private static final String ELECTION_DUL_URL_V1 = "election-url-version1";
    private static final String ELECTION_DULNAME_V2 = "election-dul-version2";
    private static final String ELECTION_DUL_URL_V2 = "election-url-version2";

    private ConsentAPI databaseConsentApi = DatabaseConsentAPI.getInstance();

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE =
            new DropwizardAppRule<>(ConsentApplication.class,
                    resourceFilePath("consent-config.yml"));
    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Before
    public void setUp() {
        ElectionDAO electionDAO = getApplicationJdbi().onDemand(ElectionDAO.class);
        VoteDAO voteDAO = getApplicationJdbi().onDemand(VoteDAO.class);
        Integer electionId = electionDAO.getOpenElectionIdByReferenceId(CONSENT_ID);
        if (electionId != null) {
            List<Vote> votes = voteDAO.findVotesByElectionIds(Collections.singletonList(electionId));
            votes.forEach(v -> voteDAO.deleteVoteById(v.getVoteId()));
            electionDAO.deleteElectionById(electionId);
        }
    }

    @Test (expected = UnknownIdentifierException.class)
    public void testRetrieveUnkownIdentifier() throws UnknownIdentifierException {
        databaseConsentApi.retrieve("non-existent Id");
    }

    @Test
    public void testRetrieveElectionDulWithoutElectionId() throws IOException, UnknownIdentifierException {
        Client client = ClientBuilder.newClient();
        Consent consent = generateNewConsent(everything, generalUse);

        Election lastElection = createElection(ELECTION_DULNAME_V2, ELECTION_DUL_URL_V2, client);

        Election retrievedElection = databaseConsentApi.retrieveElection(null, consent.getConsentId());

        assertThat(retrievedElection.getDulName().equals(lastElection.getDulName()));
        assertThat(retrievedElection.getDataUseLetter().equals(lastElection.getDataUseLetter()));

        assertThat(consent.getDulName()).isNotEqualTo(retrievedElection.getDulName());
        assertThat(consent.getDataUseLetter()).isNotEqualTo(retrievedElection.getDataUseLetter());

        deleteElection(CONSENT_ID, lastElection.getElectionId(), client);
    }

    @Test
    public void testRetrieveElectionDulWithElectionId() throws IOException, UnknownIdentifierException {
        Client client = ClientBuilder.newClient();
        Consent consent = generateNewConsent(everything, generalUse);

        Election election = createElection(ELECTION_DULNAME_V1, ELECTION_DUL_URL_V1, client);

        Election retrievedElection = databaseConsentApi.retrieveElection(election.getElectionId(), consent.getConsentId());

        assertThat(retrievedElection.getDulName().equals(election.getDulName()));
        assertThat(retrievedElection.getDataUseLetter().equals(election.getDataUseLetter()));

        assertThat(consent.getDulName()).isNotEqualTo(retrievedElection.getDulName());
        assertThat(consent.getDataUseLetter()).isNotEqualTo(retrievedElection.getDataUseLetter());

        deleteElection(CONSENT_ID, election.getElectionId(), client);

    }

    private Election createElection(String dulName, String dulUrl, Client client) throws IOException {
        Election election = new Election();
        election.setDulName(dulName);
        election.setDataUseLetter(dulUrl);
        election.setStatus(ElectionStatus.OPEN.getValue());
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        Response response = checkStatus(CREATED,
                post(client, electionConsentPath(DatabaseConsentAPITest.CONSENT_ID), election));
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

    private Consent generateNewConsent(UseRestriction useRestriction, DataUse dataUse) {
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
