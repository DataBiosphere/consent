package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.DataUseDTO;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class UseRestrictionConverterTest {

    private ClientAndServer mockServer;
    private Integer port = 9000;

    @Before
    public void startUp() {
        mockServer = startClientAndServer(port);
    }

    @After
    public void tearDown() {
        mockServer.stop();
    }

    private void mockSuccess() {
        mockServer.reset();
        mockServer.when(
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

    private void mockFailure() {
        mockServer.reset();
        mockServer.when(
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
        config.setOntologyURL("http://localhost:" + port + "/");
        return config;
    }

    /*
     * Test that the UseRestrictionConverter makes a call to the ontology service and gets back a valid restriction
     */
    @Test
    public void testUseRestrictionConverterConnection() {
        mockSuccess();

        Client client = ClientBuilder.newClient();
        UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
        DataUseDTO dto = converter.parseDataUseDto("{  }");
        UseRestriction restriction = converter.parseUseRestriction(dto);
        assertNotNull(restriction);
        assertTrue(restriction.equals(new Everything()));
    }

    /*
     * Test that when the UseRestrictionConverter makes a failed call to the ontology service, a null is returned.
     */
    @Test
    public void testFailedUseRestrictionConverterConnection() {
        mockFailure();

        Client client = ClientBuilder.newClient();
        UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
        DataUseDTO dto = converter.parseDataUseDto("{  }");
        UseRestriction restriction = converter.parseUseRestriction(dto);
        assertNull(restriction);
    }

    /*
     * Testing a fleshed out DTO.
     */
    @Test
    public void testParseDataUseDTO() {
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
        DataUseDTO dto = converter.parseDataUseDto(json);
        assertNotNull(dto);
        assertTrue(dto.getMethodsResearch());
        assertTrue(dto.getPopulationStructure());
        assertTrue(dto.getControlSetOption().equalsIgnoreCase("Yes"));
        assertTrue(dto.getDiseaseRestrictions().contains("http://purl.obolibrary.org/obo/DOID_4023"));
        assertTrue(dto.getCommercialUse());
        assertTrue(dto.getPediatric());
        assertTrue(dto.getGender().equalsIgnoreCase("Female"));
    }

}
