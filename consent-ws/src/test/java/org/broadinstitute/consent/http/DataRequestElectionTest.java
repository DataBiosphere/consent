package org.broadinstitute.consent.http;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCursor;
import de.flapdoodle.embedmongo.MongoDBRuntime;
import de.flapdoodle.embedmongo.MongodExecutable;
import de.flapdoodle.embedmongo.MongodProcess;
import de.flapdoodle.embedmongo.config.MongodConfig;
import de.flapdoodle.embedmongo.distribution.Version;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.bson.Document;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.DatabaseElectionAPI;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class DataRequestElectionTest extends ElectionVoteServiceTest {

    private static String DATA_REQUEST_ID;
    private static String DATA_REQUEST_ID_2;
    private static final String INVALID_DATA_REQUEST_ID = "55fb15569a434c232c5d50a9";
    private static final String INVALID_STATUS = "testStatus";
    private static final String FINAL_RATIONALE = "Test";
    private static final String TEST_DATABASE_NAME = "TestConsent";
    private MongodExecutable mongodExe;
    private MongodProcess mongod;
    private MongoClient mongo;


    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Before
    public void setup() throws Exception {

        // Creating Mongodbruntime instance
        MongoDBRuntime runtime = MongoDBRuntime.getDefaultInstance();

        // Creating MongodbExecutable
        mongodExe = runtime.prepare(new MongodConfig(Version.V2_1_2, 37017, false, "127.0.0.1"));

        // Starting Mongodb
        mongod = mongodExe.start();
        mongo = new MongoClient("127.0.0.1", 37017);

        MongoConsentDB mongoi = new MongoConsentDB(mongo, TEST_DATABASE_NAME);

        // configuring ResearchPurposeAPI instance to use in memory Mongo
        DatabaseElectionAPI.getInstance().setMongoDBInstance(mongoi);

        // Create Documents needed in mongo for testing
        UseRestriction useRestriction = UseRestriction.parse("{\"type\":\"everything\"}");
        Document doc = new Document().append("testingInfo1", "someValue");
        doc.append("datasetId", "SC-20660");
        doc.append("restriction", Document.parse(useRestriction.toString()));
        doc.append("translated_restriction","translated_test_restriction");
        Document doc2 = new Document().append("testingInfo2", "someValue2").append("datasetId", "SC-20660").append("restriction", Document.parse(useRestriction.toString())).append("translated_restriction","translated_test_restriction");
        mongoi.getDataAccessRequestCollection().insertOne(doc);
        mongoi.getDataAccessRequestCollection().insertOne(doc2);
        MongoCursor<Document> dars = mongoi.getDataAccessRequestCollection().find().iterator();
        DATA_REQUEST_ID = String.valueOf(dars.next().get("_id"));
        DATA_REQUEST_ID_2 = String.valueOf(dars.next().get("_id"));
    }

    @After
    public void teardown() throws Exception {
        mongod.stop();
        mongodExe.cleanup();
    }

    @Test
    public void testCreateDataRequestElection() {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setElectionType(ElectionType.DATA_ACCESS.getValue());
        election.setStatus(ElectionStatus.OPEN.getValue());
        election.setReferenceId(DATA_REQUEST_ID);
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
        checkStatus(BAD_REQUEST,
                post(client, electionDataRequestPath(DATA_REQUEST_ID), election));
        testUpdateDataRequestElection(created);
        deleteElection(created.getElectionId());
    }

    public void testUpdateDataRequestElection(Election created) {
        Client client = ClientBuilder.newClient();
        checkStatus(OK, put(client, electionPathById(created.getElectionId()), created));
        created = retrieveElection(client, electionDataRequestPath(DATA_REQUEST_ID));
        assertThat(created.getElectionType()).isEqualTo(ElectionType.DATA_ACCESS.getValue());
        assertThat(created.getReferenceId()).isEqualTo(DATA_REQUEST_ID);
        assertThat(created.getCreateDate()).isNotNull();
        assertThat(created.getElectionId()).isNotNull();
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
    public void retrieveElectionWithInvalidDataRequestId() {
        Client client = ClientBuilder.newClient();
        checkStatus(NOT_FOUND,
                getJson(client, electionDataRequestPath(INVALID_DATA_REQUEST_ID)));
    }

    @Test
    public void testDataRequestElectionWithInvalidDataRequest() {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setElectionType(ElectionType.DATA_ACCESS.getValue());
        election.setStatus(ElectionStatus.OPEN.getValue());
        // should return 400 bad request because the data request id does not exist
        checkStatus(NOT_FOUND,
                post(client, electionDataRequestPath(INVALID_DATA_REQUEST_ID), election));
    }

    @Test
    public void testUpdateDataRequestElectionWithId() {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        // should return 400 bad request because the election id does not exist
        checkStatus(NOT_FOUND,
                put(client, electionPathById(1010), election));
    }

    @Test
    public void testCreateDataRequestElectionWithInvalidStatus() {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setElectionType(ElectionType.DATA_ACCESS.getValue());
        election.setStatus(INVALID_STATUS);
        // should return 400 bad request because status is invalid
        checkStatus(BAD_REQUEST,
                post(client, electionDataRequestPath(DATA_REQUEST_ID_2), election));
    }

}
