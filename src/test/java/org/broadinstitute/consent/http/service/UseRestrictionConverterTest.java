package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.WithMockServer;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.testcontainers.containers.MockServerContainer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class UseRestrictionConverterTest implements WithMockServer {

    private MockServerClient client;

    @Rule
    public MockServerContainer container = new MockServerContainer(IMAGE);

    @Before
    public void startUp() {
        client = new MockServerClient(container.getHost(), container.getServerPort());
    }

    @After
    public void tearDown() {
        stop(container);
    }

    private void mockDataUseTranslateSuccess() {
        client.reset();
        client
            .when(request().withMethod("POST").withPath("/translate"))
            .respond(
                response()
                    .withStatusCode(200)
                    .withHeaders(new Header("Content-Type", MediaType.APPLICATION_JSON))
                    .withBody(
                        "Samples are restricted for use under the following conditions:\n"
                            + "Data is limited for health/medical/biomedical research. [HMB]\n"
                            + "Commercial use is not prohibited.\n"
                            + "Data use for methods development research irrespective of the specified data use limitations is not prohibited.\n"
                            + "Restrictions for use as a control set for diseases other than those defined were not specified."));
    }

    private void mockDataUseTranslateFailure() {
        client.reset();
        client
            .when(request().withMethod("POST").withPath("/translate"))
            .respond(
                response()
                        .withStatusCode(500)
                        .withHeaders(new Header("Content-Type", MediaType.APPLICATION_JSON))
                        .withBody("Exception")
            );
    }


    private void mockDarTranslateSuccess() {
        client.reset();
        client.when(
            request()
                .withMethod("POST")
                .withPath("/schemas/data-use/dar/translate")
        ).respond(
            response()
                .withStatusCode(200)
                .withHeaders(new Header("Content-Type", MediaType.APPLICATION_JSON))
                .withBody("{ \"type\": \"everything\" }")
        );
    }

    private void mockDarTranslateFailure() {
        client.reset();
        client.when(
            request()
                .withMethod("POST")
                .withPath("/schemas/data-use/dar/translate")
        ).respond(
            response()
                .withStatusCode(500)
                .withHeaders(new Header("Content-Type", MediaType.APPLICATION_JSON))
                .withBody("Exception")
        );
    }

    public ServicesConfiguration config() {
        ServicesConfiguration config = new ServicesConfiguration();
        config.setLocalURL("http://localhost:8180/");
        config.setOntologyURL(getRootUrl(container));
        return config;
    }

    /*
     * Test that the UseRestrictionConverter makes a call to the ontology service and gets back a valid restriction
     */
    @Test
    public void testUseRestrictionConverterConnection() {
        mockDarTranslateSuccess();

        Client client = ClientBuilder.newClient();
        UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
        DataUse dataUse = converter.parseDataUsePurpose("{  }");
        UseRestriction restriction = converter.parseUseRestriction(dataUse);
        assertNotNull(restriction);
        assertEquals(restriction, new Everything());
    }

    /*
     * Test that when the UseRestrictionConverter makes a failed call to the ontology service, a null is returned.
     */
    @Test
    public void testFailedUseRestrictionConverterConnection() {
        mockDarTranslateFailure();

        Client client = ClientBuilder.newClient();
        UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
        DataUse dataUse = converter.parseDataUsePurpose("{  }");
        UseRestriction restriction = converter.parseUseRestriction(dataUse);
        assertNull(restriction);
    }

    /*
     * Test that the UseRestrictionConverter makes a call to the ontology service and gets back a valid translation
     */
    @Test
    public void testTranslateDataUsePurpose() {
        mockDataUseTranslateSuccess();
        Client client = ClientBuilder.newClient();
        UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
        DataUse dataUse = new DataUseBuilder().setHmbResearch(true).build();
        String translation = converter.translateDataUse(dataUse, DataUseTranslationType.PURPOSE);
        assertNotNull(translation);
    }

    /*
     * Test that the UseRestrictionConverter makes a call to the ontology service and gets back a valid translation
     */
    @Test
    public void testTranslateDataUseDataset() {
        mockDataUseTranslateSuccess();
        Client client = ClientBuilder.newClient();
        UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
        DataUse dataUse = new DataUseBuilder().setHmbResearch(true).build();
        String translation = converter.translateDataUse(dataUse, DataUseTranslationType.DATASET);
        assertNotNull(translation);
    }

    /*
     * Test that when the UseRestrictionConverter makes a failed call to the ontology service, a null is returned.
     */
    @Test
    public void testFailedDataUseTranslateConverterConnection() {
        mockDataUseTranslateFailure();

        Client client = ClientBuilder.newClient();
        UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
        DataUse dataUse = new DataUseBuilder().setHmbResearch(true).build();
        String translation = converter.translateDataUse(dataUse, DataUseTranslationType.PURPOSE);
        assertNull(translation);
    }

    /*
     * Testing a fleshed out DataUse.
     */
    @Test
    public void testParseDataUse() {
        String json = "{ " +
            "\"methods\":true, " +
            "\"population\":true, " +
            "\"controls\":true, " +
            "\"ontologies\":[  " +
            "      {  " +
            "         \"id\":\"http://purl.obolibrary.org/obo/DOID_4023\"," +
            "         \"label\":\"linitis-plastica\"," +
            "         \"definition\":null," +
            "         \"synonyms\":[  " +
            "            \"Linitis plastica (morphologic abnormality)\"," +
            "            \"Leather-bottle stomach\"" +
            "         ]" +
            "      }" +
            "]," +
            "\"forProfit\":true," +
            "\"onegender\":true," +
            "\"pediatric\":true," +
            "\"gender\":\"F\"" +
        "}";

        Client client = ClientBuilder.newClient();
        UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
        DataUse dataUse = converter.parseDataUsePurpose(json);
        assertNotNull(dataUse);
        assertTrue(dataUse.getMethodsResearch());
        assertTrue(dataUse.getPopulationStructure());
        assertTrue(dataUse.getControlSetOption().equalsIgnoreCase("Yes"));
        assertTrue(dataUse.getDiseaseRestrictions().contains("http://purl.obolibrary.org/obo/DOID_4023"));
        assertTrue(dataUse.getCommercialUse());
        assertTrue(dataUse.getPediatric());
        assertTrue(dataUse.getGender().equalsIgnoreCase("Female"));
    }

    /*
     * Testing a DataUse with invalid ontologies.
     */
    @Test
    public void testParseDataUseInvalidOntologiesCase1() {
        String json = "{ " +
            "\"hmb\":true, " +
            "\"ontologies\":[{},{},{}]" +
            "}";

        Client client = ClientBuilder.newClient();
        UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
        DataUse dataUse = converter.parseDataUsePurpose(json);
        assertNotNull(dataUse);
        assertNull(dataUse.getDiseaseRestrictions());
    }

    /*
     * Testing a DataUse with invalid ontologies.
     */
    @Test
    public void testParseDataUseInvalidOntologiesCase2() {
        String json = "{ " +
            "\"ontologies\":[null]" +
            "}";

        Client client = ClientBuilder.newClient();
        UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
        DataUse dataUse = converter.parseDataUsePurpose(json);
        assertNotNull(dataUse);
        assertNull(dataUse.getDiseaseRestrictions());
    }

    /*
     * Test that the DataUse parser does not set false values incorrectly
     */
    @Test
    public void testTranslateFalseValues() {
        Client client = ClientBuilder.newClient();
        UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
        String json = "{ " +
                "\"methods\":false, " +
                "\"population\":false, " +
                "\"controls\":false, " +
                "\"poa\":false, " +
                "\"hmb\":false " +
                "}";
        DataUse dataUse = converter.parseDataUsePurpose(json);
        assertNull(dataUse.getMethodsResearch());
        assertNull(dataUse.getPopulationStructure());
        assertNull(dataUse.getControlSetOption());
        assertNull(dataUse.getPopulationOriginsAncestry());
        assertNull(dataUse.getHmbResearch());
    }

    /*
     * Test that the DataUse parser sets true values correctly
     */
    @Test
    public void testTranslateTrueValues() {
        Client client = ClientBuilder.newClient();
        UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
        String json = "{ " +
                "\"methods\":true, " +
                "\"population\":true, " +
                "\"controls\":true, " +
                "\"poa\":true, " +
                "\"hmb\":true " +
                "}";
        DataUse dataUse = converter.parseDataUsePurpose(json);
        assertTrue(dataUse.getMethodsResearch());
        assertTrue(dataUse.getPopulationStructure());
        assertTrue(dataUse.getControlSetOption().equalsIgnoreCase("Yes"));
        assertTrue(dataUse.getPopulationOriginsAncestry());
        assertTrue(dataUse.getHmbResearch());
    }

}
