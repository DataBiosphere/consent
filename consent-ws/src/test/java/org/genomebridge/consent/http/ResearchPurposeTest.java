package org.genomebridge.consent.http;

import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import de.flapdoodle.embedmongo.MongoDBRuntime;
import de.flapdoodle.embedmongo.MongodExecutable;
import de.flapdoodle.embedmongo.MongodProcess;
import de.flapdoodle.embedmongo.config.MongodConfig;
import de.flapdoodle.embedmongo.distribution.Version;
import de.flapdoodle.embedmongo.runtime.Network;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.genomebridge.consent.http.db.mongo.MongoConsentDB;
import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.models.Match;
import org.genomebridge.consent.http.models.ResearchPurpose;
import org.genomebridge.consent.http.models.grammar.*;
import org.genomebridge.consent.http.service.DatabaseResearchPurposeAPI;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ResearchPurposeTest extends ResearchPurposeServiceTest {

    private MongodExecutable mongodExe;
    private MongodProcess mongod;
    private MongoClient mongo;
    private static final String ACCESS_JSON = " { \"investigator\" : \"Veronica\",    \"institution\" : \"Test\",   \"division\" : \"Test\",    \"department\" : \"Test\",    \"address1\" : \"Street 1\",   \"address2\" : \"Street 2\",    \"state\" : \"\",    \"city\" : \"\",    \"zipcode\" : \"5502\",    \"country\" : \"22\",    \"projectTitle\" : \"Test\",    \"datasetId\" : \"SM- 0.5301803889973727\",    \"rus\" : \"Test\",   \"non_tech_rus\" : \"Test\",   \"diseases\" : true,   \"methods\" : false,   \"controls\" : false,    \"population\" : false,    \"other\" : false,  \"othertext\" : \"\",  \"forProfit\" : false,  \"onegender\" : true,    \"gender\" : \"M\",  \"pediatric\" : false,   \"illegalbehave\" : false,   \"addiction\" : false,   \"sexualdiseases\" : false,  \"stigmatizediseases\" : false,  \"vulnerablepop\" : false, \"popmigration\" : false,  \"psychtraits\" : false, \"nothealth\" : false, \"restriction\" : { \"type\" : \"and\",  \"operands\" : [  {   \"type\" : \"named\",  \"name\" : \"http://www.broadinstitute.org/ontologies/DURPO/Non_profit\" }, { \"type\" : \"named\",  \"name\" : \"http://www.broadinstitute.org/ontologies/DURPO/men\" }    ]    }}";
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
        });
        assertThat(researchPurposes.size() == 2);
        deleteResearchPurpose(created1.getId());
        researchPurposes = getJson(client, purposePath() + "?ids=" + created1.getId() + "," + created2.getId()).readEntity(new GenericType<List<ResearchPurpose>>() {
        });
        assertThat(researchPurposes.size() == 1);
        deleteResearchPurpose(created2.getId());
        researchPurposes = getJson(client, purposePath() + "?ids=" + created1.getId() + "," + created2.getId()).readEntity(new GenericType<List<ResearchPurpose>>() {
        });
        assertThat(researchPurposes.size() == 0);
    }


    @Test
    public void testCreateMatchRPAndConsent() throws IOException {
        String  consent = createConsent();
        String purpose = createDataAccessRP();
        Match match = new Match();
        match.setConsent(consent);
        match.setPurpose(purpose);
        match.setMatch(true);
        Client client = ClientBuilder.newClient();
        Response response = checkStatus(CREATED, post(client, matchPath(), match));
        String createdLocation = checkHeader(response, "Location");
        Match createdMatch = retrieveMatch(client, createdLocation);
        assertThat(createdMatch.getId() > 0);
        assertThat(createdMatch.getConsent().equals(consent));
        assertThat(createdMatch.getPurpose().equals(purpose));
        assertThat(createdMatch.getMatch().equals(true));
        //update Match
        put(client, matchPathById(match.getId()), match);
        Match updatedMatch = retrieveMatch(client, matchPathById(createdMatch.getId()));

        assertThat(updatedMatch.getId() > 0);
        assertThat(updatedMatch.getConsent().equals(consent));
        assertThat(updatedMatch.getPurpose().equals(purpose));
        assertThat(updatedMatch.getMatch().equals(false));
        // delete data
        deleteDataAccessRP(purpose);
        deleteMatch(createdMatch.getId());

    }


    private void deleteDataAccessRP(String id) {
        Client client = ClientBuilder.newClient();
        Response response = checkStatus(OK,
                delete(client, accessPathById(id)));
    }

    private void deleteMatch(Integer id) {
        Client client = ClientBuilder.newClient();
        Response response = checkStatus(OK,
                delete(client, matchPathById(id)));
    }

    private String createDataAccessRP() {
        Client client = ClientBuilder.newClient();
        DBObject dbObject = (DBObject) JSON.parse(ACCESS_JSON);
        Response response = checkStatus(CREATED,
                post(client, accessPath(), dbObject));
        String createdLocation = checkHeader(response, "Location");
        Document document = retrieveAccess(client, createdLocation);
        LinkedHashMap id= document.get("_id",LinkedHashMap.class);
        Integer processIdentifier = (Integer)id.get("processIdentifier");
        ObjectId objectId = new ObjectId((Integer)id.get("timestamp"),(Integer)id.get("machineIdentifier"),(short)processIdentifier.intValue(),(Integer)id.get("counter"));
        return objectId.toString();
    }

    private String createConsent() {
        Timestamp createDate = new Timestamp(new Date().getTime());
        Client client = ClientBuilder.newClient();
        Consent rec = new Consent(true, new Everything(), null, null, UUID.randomUUID().toString(), createDate, createDate, createDate);
        Response response = checkStatus(CREATED, post(client, consentPath(), rec));
        String createdLocation = checkHeader(response, "Location");
        return retrieveConsent(client, createdLocation).getConsentId();

    }


}
