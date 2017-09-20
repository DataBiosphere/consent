package org.broadinstitute.consent.http.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.grammar.And;
import org.broadinstitute.consent.http.models.grammar.Named;
import org.broadinstitute.consent.http.models.grammar.Not;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.broadinstitute.consent.http.models.matching.RequestMatchingObject;
import org.broadinstitute.consent.http.models.matching.ResponseMatchingObject;
import org.bson.Document;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class DatabaseMatchingServiceAPITest {

    @Mock
    private ServicesConfiguration config;
    @Mock
    private ConsentAPI consentAPI;
    @Mock
    private DataAccessRequestAPI dataAccessAPI;
    @Mock
    private DataSetAPI dsAPI;

    private static Consent sampleConsent1;
    private static Consent sampleConsent2;
    private static RequestMatchingObject reqmo1;
    private static RequestMatchingObject reqmo2;
    private static ResponseMatchingObject resmo1;
    private static ResponseMatchingObject resmo2;

    private static DatabaseMatchingServiceAPI matchApi;
    private static Client clientMock;

    @BeforeClass
    public static void setUpClass() throws IOException {
        UseRestriction sampleUseRestriction1 = new And(
                new Not(new Named("http://purl.obolibrary.org/obo/DUO_0000015")),
                new Not(new Named("http://purl.obolibrary.org/obo/DUO_0000011")),
                new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/control"))
        );
        UseRestriction sampleUseRestriction2 = new And(
                new Named("http://purl.obolibrary.org/obo/DUO_0000018"),
                new Named("http://www.broadinstitute.org/ontologies/DUOS/female")
        );
        UseRestriction sampleUsePurpose1 = new And(
                new Named("http://purl.obolibrary.org/obo/DUO_0000018")
        );
        sampleConsent1 = new Consent(false, sampleUseRestriction1, "A data use letter", "sampleConsent1", null, null, null);
        sampleConsent1.setConsentId("CONS-1");
        sampleConsent2 = new Consent(false, sampleUseRestriction2, "A data use letter", "sampleConsent1", null, null, null);
        sampleConsent2.setConsentId("CONS-2");

        reqmo1 = new RequestMatchingObject(sampleConsent1.getUseRestriction(), sampleUsePurpose1);
        resmo1 = new ResponseMatchingObject(true, reqmo1);
        reqmo2 = new RequestMatchingObject(sampleConsent2.getUseRestriction(), sampleUsePurpose1);
        resmo2 = new ResponseMatchingObject(false, reqmo2);
    }

    @Before
    public void setUp() throws UnknownIdentifierException, IOException {

        MockitoAnnotations.initMocks(this);
        setUpMockedResponses();
        when(config.getMatchURL()).thenReturn("http://ontology.org/match");
        matchApi = new DatabaseMatchingServiceAPI(clientMock, config, consentAPI, dataAccessAPI, dsAPI);

        when(dataAccessAPI.describeDataAccessRequestById("NullDar")).thenReturn(null);

        when(consentAPI.retrieve("NullConsent")).thenReturn(null);
        when(consentAPI.retrieve("AbsentConsent")).thenThrow(UnknownIdentifierException.class);

        when(dataAccessAPI.describeDataAccessRequestById("DAR-2")).thenReturn(getSampleDar());
        when(consentAPI.retrieve("CONS-1")).thenReturn(sampleConsent1);
        when(consentAPI.retrieve("CONS-2")).thenReturn(sampleConsent2);

        when(consentAPI.getConsentFromDatasetID("SC-20660")).thenReturn(sampleConsent1);
    }

    private void setUpMockedResponses() throws IOException {

        final Invocation.Builder builderMock = Mockito.mock(Invocation.Builder.class);
        final WebTarget webTargetMock = Mockito.mock(WebTarget.class);

        final Response okResponseMock = Mockito.mock(Response.class);
        Mockito.when(okResponseMock.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        Mockito.when(okResponseMock.readEntity((GenericType<Object>) any())).thenReturn(resmo1);

        final Response noMatchResponseMock = Mockito.mock(Response.class);
        Mockito.when(noMatchResponseMock.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        Mockito.when(noMatchResponseMock.readEntity((GenericType<Object>) any())).thenReturn(resmo2);

        Mockito.when(builderMock.post(Entity.json(new Gson().toJson(reqmo1)))).thenReturn(okResponseMock);
        Mockito.when(builderMock.post(Entity.json(new Gson().toJson(reqmo2)))).thenThrow(TimeoutException.class);

        Mockito.when(webTargetMock.request(MediaType.APPLICATION_JSON)).thenReturn(builderMock);
        clientMock = Mockito.mock(Client.class);
        Mockito.when(clientMock.target(Mockito.anyString())).thenReturn(webTargetMock);
        Mockito.when(webTargetMock.queryParam(Mockito.anyString(), Mockito.anyString())).thenReturn(webTargetMock);
    }

    @Test
    public void testFindSingleMatchNull() {
        Match match = matchApi.findSingleMatch("NullConsent", "NullDar");
        assertTrue(Objects.isNull(match));
        match = matchApi.findSingleMatch("AbsentConsent", "NullDar");
        assertTrue(Objects.isNull(match));
    }

    @Test
    public void testFindSingleMatch() throws UnknownIdentifierException, IOException {
        when(dataAccessAPI.describeDataAccessRequestById("DAR-2")).thenReturn(getSampleDar());
        when(consentAPI.retrieve("CONS-1")).thenReturn(sampleConsent1);
        Match match = matchApi.findSingleMatch("CONS-1", "DAR-2");
        assertTrue(!Objects.isNull(match));
        assertTrue(match.getMatch());
        assertTrue(!match.getFailed());
        match = matchApi.findSingleMatch("CONS-2", "DAR-2");
        assertTrue(Objects.isNull(match));
    }

    @Test
    public void testFindSingleMatchNoPurpose() throws UnknownIdentifierException, IOException {
        when(consentAPI.retrieve("CONS-1")).thenReturn(sampleConsent1);
        Match match = matchApi.findSingleMatch("CONS-1", "DAR-2");
        assertTrue(!Objects.isNull(match));
        assertTrue(match.getMatch());
        assertTrue(!match.getFailed());

    }

    @Test
    public void testFindMatchForPurpose(){
        Match match =  matchApi.findMatchForPurpose("DAR-2");
        assertTrue(match.getMatch());
        assertTrue(!match.getFailed());
    }

    @Test
    public void testFindMatchesForConsent() throws IOException {
        DataSet ds = new DataSet(1, "SC-20660", "SC-20660", null, true);
        List<DataSet> dsList = Collections.singletonList(ds);
        when(dsAPI.getDataSetsForConsent("CONS-1")).thenReturn(dsList);
        when(dsAPI.getDataSetsForConsent("CONS-2")).thenReturn(dsList);
        when(dataAccessAPI.describeDataAccessWithDataSetIdAndRestriction(dsList.stream().map(DataSet::getObjectId).collect(Collectors.toList()))).thenReturn(new ArrayList<>(Arrays.asList(getSampleDar())));
        List<Match> matches = matchApi.findMatchesForConsent("CONS-1");
        assertTrue(matches.size() == 1);
        assertTrue(!matches.get(0).getFailed());
        assertTrue(matches.get(0).getMatch());
        matches = matchApi.findMatchesForConsent("CONS-2");
        assertTrue(matches.size() == 1);
        assertTrue(matches.get(0).getFailed());
        assertTrue(!matches.get(0).getMatch());
        when(dataAccessAPI.describeDataAccessWithDataSetIdAndRestriction(dsList.stream().map(DataSet::getObjectId).collect(Collectors.toList()))).thenReturn(new ArrayList<>());
        matches = matchApi.findMatchesForConsent("CONS-1");
        assertTrue(matches.size() == 0);
    }

    private static Document getSampleDar() throws IOException {
        Document document = new Document();
        document.putAll(jsonAsMap(sampleDar1));
        return document;
    }

    private static Map<String, Object> jsonAsMap(String jsonSource) throws IOException {
        return new ObjectMapper().readValue(jsonSource, Map.class);
    }

    private static String sampleDar1 = "{\n" +
            "\t\"_id\": \"5771763734064d282c6600ed\",\n" +
            "\t\"investigator\": \"frernp\",\n" +
            "\t\"institution\": \"npn\",\n" +
            "\t\"department\": \"pon\",\n" +
            "\t\"division\": \"opn\",\n" +
            "\t\"address1\": \"pn\",\n" +
            "\t\"address2\": \"n\",\n" +
            "\t\"city\": \"pon\",\n" +
            "\t\"state\": \"pon\",\n" +
            "\t\"zipcode\": \"p\",\n" +
            "\t\"country\": \"np\",\n" +
            "\t\"projectTitle\": \"np\",\n" +
            "\t\"rus\": \"csvv\",\n" +
            "\t\"non_tech_rus\": \"vfvdvfdf\",\n" +
            "\t\"diseases\": true,\n" +
            "\t\"forProfit\": false,\n" +
            "\t\"onegender\": false,\n" +
            "\t\"pediatric\": false,\n" +
            "\t\"illegalbehave\": false,\n" +
            "\t\"addiction\": false,\n" +
            "\t\"sexualdiseases\": false,\n" +
            "\t\"stigmatizediseases\": false,\n" +
            "\t\"vulnerablepop\": false,\n" +
            "\t\"popmigration\": false,\n" +
            "\t\"psychtraits\": false,\n" +
            "\t\"nothealth\": false,\n" +
            "\t\"userId\": 3333,\n" +
            "\t\"restriction\": {\n" +
            "\t\t\"type\": \"and\",\n" +
            "\t\t\"operands\": [{\n" +
            "\t\t\t\"type\": \"named\",\n" +
            "\t\t\t\"name\": \"http://purl.obolibrary.org/obo/DUO_0000018\"\n" +
            "\t\t}]\n" +
            "\t},\n" +
            "\t\"datasetId\": [\n" +
            "\t\t\"SC-20660\"\n" +
            "\t],\n" +
            "\t\"datasetDetail\": [{\n" +
            "\t\t\"datasetId\": \"SC-20660\",\n" +
            "\t\t\"name\": \"UHN_AsaWW (University of Toronto)-Carcinoid\"\n" +
            "\t}],\n" +
            "\t\"dar_code\": \"DAR-6\",\n" +
            "\t\"valid_restriction\": true\n" +
            "}";

}