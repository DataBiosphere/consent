package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
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

    @After
    public void tearDown() {
        mockServer.stop();
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
        Client client = ClientBuilder.newClient();
        UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
        UseRestriction restriction = converter.parseJsonFormulary("{  }");
        assertNotNull(restriction);
        assertTrue(restriction.equals(new Everything()));
    }


}
