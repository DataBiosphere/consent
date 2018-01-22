package org.broadinstitute.consent.http;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCursor;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.PendingCase;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.DatabaseElectionAPI;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class VoteDataRequestTest extends ElectionVoteServiceTest {

    public static final int CREATED = Response.Status.CREATED.getStatusCode();
    public static final int OK = Response.Status.OK.getStatusCode();
    private static String DATA_REQUEST_ID;
    private static final Integer DAC_USER_ID = 2;
    private static final String RATIONALE = "Test";
    private static final String TEST_DATABASE_NAME = "TestConsent";


    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Before
    public void setup() throws Exception {
        MongoClient mongo = setUpMongoClient();
        MongoConsentDB mongoi = new MongoConsentDB(mongo, TEST_DATABASE_NAME);
        mongoi.getCountersCollection().drop();
        mongoi.getDataAccessRequestCollection().drop();
        mongoi.getResearchPurposeCollection().drop();
        mongoi.configureMongo();
        
        // configuring ResearchPurposeAPI instance to use in memory Mongo
        DatabaseElectionAPI.getInstance().setMongoDBInstance(mongoi);

        // Create Documents needed in mongo for testing
        List<String> dataSets = new ArrayList<>();
        dataSets.add("SC-20660");
        Document doc = new Document().append("testingInfo1", "someValue").append(DarConstants.DATASET_ID, dataSets);
        mongoi.getDataAccessRequestCollection().insertOne(doc);
        MongoCursor<Document> dars = mongoi.getDataAccessRequestCollection().find().iterator();
        DATA_REQUEST_ID = String.valueOf(dars.next().get(DarConstants.ID));
    }

    @After
    public void teardown() {
        shutDownMongo();
    }

    @Test
    public void testCreateDataRequestVote() throws IOException {
        // should exist an election for specified data request
        mockValidateTokenResponse();
        Integer electionId = createElection();
        Client client = ClientBuilder.newClient();
        List<Vote> votes = getJson(client, voteDataRequestPath(DATA_REQUEST_ID)).readEntity(new GenericType<List<Vote>>() {
        });
        Vote vote = new Vote();
        vote.setVote(false);
        vote.setRationale(RATIONALE);
        checkStatus(OK,
                post(client, voteDataRequestIdPath(DATA_REQUEST_ID, votes.get(0).getVoteId()), vote));
        mockValidateTokenResponse();
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


    private void deleteVotes(List<Vote> votes) throws IOException {
        Client client = ClientBuilder.newClient();
        for (Vote vote : votes) {
            checkStatus(OK,
                    delete(client, voteDataRequestIdPath(DATA_REQUEST_ID, vote.getVoteId())));
        }
    }

    private void updateVote(Integer id, Vote vote) throws IOException {
        Client client = ClientBuilder.newClient();
        vote.setVote(true);
        vote.setRationale(null);
        checkStatus(OK,
                put(client, voteDataRequestIdPath(DATA_REQUEST_ID, id), vote));
        mockValidateTokenResponse();
        vote = retrieveVote(client, voteDataRequestIdPath(DATA_REQUEST_ID, id));
        assertThat(vote.getRationale()).isNull();
        assertThat(vote.getUpdateDate()).isNotNull();
        assertThat(vote.getVote()).isTrue();
    }

    private Integer createElection() throws IOException {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setElectionType(ElectionType.DATA_ACCESS.getValue());
        election.setStatus(ElectionStatus.OPEN.getValue());
        post(client, electionDataRequestPath(DATA_REQUEST_ID), election);
        election = getJson(client, electionDataRequestPath(DATA_REQUEST_ID))
                .readEntity(Election.class);
        return election.getElectionId();
    }


    private void testDataRequestPendingCase(Integer dacUserId) throws IOException {
        Client client = ClientBuilder.newClient();
        List<PendingCase> pendingCases = getJson(client, dataRequestPendingCasesPath(dacUserId)).readEntity(new GenericType<List<PendingCase>>() {});
        assertThat(pendingCases).isNotNull();
        assertThat(pendingCases.size()).isEqualTo(2);
        assertThat(pendingCases.get(0).getLogged()).isEqualTo("1/5");
        assertThat(pendingCases.get(0).getReferenceId()).isEqualTo(DATA_REQUEST_ID);
    }

    @Test
    public void testDataRequestPendingCaseWithInvalidUser() throws IOException {
        Client client = ClientBuilder.newClient();
        List<PendingCase> pendingCases = getJson(client, dataRequestPendingCasesPath(789)).readEntity(new GenericType<List<PendingCase>>() {});
        assertThat(pendingCases).isEmpty();
    }
}
