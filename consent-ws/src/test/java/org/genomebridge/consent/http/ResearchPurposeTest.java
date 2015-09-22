package org.genomebridge.consent.http;

import com.mongodb.MongoClient;
import de.flapdoodle.embedmongo.MongoDBRuntime;
import de.flapdoodle.embedmongo.MongodExecutable;
import de.flapdoodle.embedmongo.MongodProcess;
import de.flapdoodle.embedmongo.config.MongodConfig;
import de.flapdoodle.embedmongo.distribution.Version;
import de.flapdoodle.embedmongo.runtime.Network;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.genomebridge.consent.http.models.ResearchPurpose;
import org.genomebridge.consent.http.models.grammar.And;
import org.genomebridge.consent.http.models.grammar.Named;
import org.genomebridge.consent.http.models.grammar.Or;
import org.genomebridge.consent.http.models.grammar.UseRestriction;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.genomebridge.consent.http.db.mongo.MongoConsentDB;
import org.genomebridge.consent.http.service.DatabaseResearchPurposeAPI;
import org.junit.After;
import org.junit.Before;

public class ResearchPurposeTest extends ResearchPurposeServiceTest {

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
        mongodExe = runtime.prepare(new MongodConfig(Version.V2_1_2, 37017, Network.localhostIsIPv6()));
        
        // Starting Mongodb
        mongod = mongodExe.start();
        mongo = new MongoClient("localhost", 37017);

        MongoConsentDB mongoi = new MongoConsentDB(mongo);

        // configuring ResearchPurposeAPI instance to use in memory Mongo
        DatabaseResearchPurposeAPI.clearInstance();
        DatabaseResearchPurposeAPI.initInstance(mongoi);
        
    }

    @After
    public void teardown() throws Exception {
        mongod.stop();
        mongodExe.cleanup();
    }

    @Test
    public void testCreateResearchPurpose() throws IOException {
        Client client = ClientBuilder.newClient();
        ResearchPurpose purpose = new ResearchPurpose();
        UseRestriction useRestriction = new And(new Named("DOID:1"), new Named("DOID:2"));
        purpose.setRestriction(useRestriction);
        Response response = checkStatus(CREATED,
                post(client, purposePath(), purpose));
        String createdLocation = checkHeader(response, "Location");
        ResearchPurpose created = retrieveResearchPurpose(client, createdLocation);
        assertThat(created.getId()).isNotEmpty();
        assertThat(created.getRestriction().equals(useRestriction));
        updateResearchPurpose(created.getId());
        deleteResearchPurpose(created.getId());
    }

    public void updateResearchPurpose(String id) throws IOException {
        Client client = ClientBuilder.newClient();
        ResearchPurpose purpose = new ResearchPurpose();
        UseRestriction useRestriction = new Or(new Named("IOID:1"), new Named("IOID:3"));
        purpose.setRestriction(useRestriction);
        checkStatus(OK, put(client, researchPurposePathById(id), purpose));
        ResearchPurpose created = retrieveResearchPurpose(client, researchPurposePathById(id));
        assertThat(created.getId()).isNotEmpty();
        assertThat(created.getRestriction().equals(useRestriction));
    }

    public void deleteResearchPurpose(String id) {
        Client client = ClientBuilder.newClient();
        checkStatus(OK, delete(client, researchPurposePathById(id)));
    }

    @Test
    public void deleteResearchPurposeWithInvalidId() {
        Client client = ClientBuilder.newClient();
        checkStatus(NOT_FOUND, delete(client, researchPurposePathById("test")));
    }

    @Test
    public void testGetListResearchPurposeByIds() throws IOException {
        Client client = ClientBuilder.newClient();
        ResearchPurpose purpose = new ResearchPurpose();
        UseRestriction useRestriction = new And(new Named("DOID:1"), new Named("DOID:2"));
        purpose.setRestriction(useRestriction);
        Response response = checkStatus(CREATED,
                post(client, purposePath(), purpose));
        Response response2 = checkStatus(CREATED,
                post(client, purposePath(), purpose));
        String createdLocation = checkHeader(response, "Location");
        String createdLocation2 = checkHeader(response2, "Location");
        ResearchPurpose created1 = retrieveResearchPurpose(client, createdLocation);
        ResearchPurpose created2 = retrieveResearchPurpose(client, createdLocation2);
        List<ResearchPurpose> researchPurposes = getJson(client, purposePath() + "?ids=" + created1.getId() + "," + created2.getId()).readEntity(new GenericType<List<ResearchPurpose>>() {
        });;
        assertThat(researchPurposes.size() == 2);
        deleteResearchPurpose(created1.getId());
        researchPurposes = getJson(client, purposePath() + "?ids=" + created1.getId() + "," + created2.getId()).readEntity(new GenericType<List<ResearchPurpose>>() {
        });;
        assertThat(researchPurposes.size() == 1);
        deleteResearchPurpose(created2.getId());
        researchPurposes = getJson(client, purposePath() + "?ids=" + created1.getId() + "," + created2.getId()).readEntity(new GenericType<List<ResearchPurpose>>() {
        });;
        assertThat(researchPurposes.size() == 0);
    }

}
