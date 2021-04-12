package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.WithMockServer;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class UseRestrictionValidatorTest implements WithMockServer {

    private ClientAndServer mockServer;
    private final Integer port = 9300;
    private UseRestrictionValidator validator;

    @Before
    public void startUp() {
        mockServer = startMockServer(port);
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

    public void initValidator() {
        this.validator = new UseRestrictionValidator(ClientBuilder.newClient(), config());
    }

    private void mockSuccess() {
        mockServer.reset();
        mockServer.when(
            request()
                .withMethod("POST")
                .withPath("/validate/userestriction")
        ).respond(
            response()
                .withStatusCode(200)
                .withHeaders(new Header("Content-Type", MediaType.APPLICATION_JSON))
                .withBody("{ \"valid\": true, \"useRestriction\": \"\" }")
        );
    }

    private void mockInvalid() {
        mockServer.reset();
        mockServer.when(
                request()
                        .withMethod("POST")
                        .withPath("/validate/userestriction")
        ).respond(
                response()
                        .withStatusCode(200)
                        .withHeaders(new Header("Content-Type", MediaType.APPLICATION_JSON))
                        .withBody("{ \"valid\": false, \"useRestriction\": \"\" }")
        );
    }

    private void mockFailure() {
        mockServer.reset();
        mockServer.when(
            request()
                .withMethod("POST")
                .withPath("/validate/userestriction")
        ).respond(
            response()
                .withStatusCode(500)
                .withHeaders(new Header("Content-Type", MediaType.APPLICATION_JSON))
                .withBody("Exception")
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateUseRestriction_Failure() {
        mockFailure();
        initValidator();
        validator.validateUseRestriction("{ \"type\": \"everything\" }");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateUseRestriction_Invalid() {
        String useRestriction = "{ \"type\": \"everything\" }";
        mockInvalid();
        initValidator();
        validator.validateUseRestriction(useRestriction);
    }

    @Test
    public void testValidateUseRestriction_Success() {
        String useRestriction = "{ \"type\": \"everything\" }";
        mockSuccess();
        initValidator();

        try {
            validator.validateUseRestriction(useRestriction);
            assert true;
        } catch (IllegalArgumentException iae) {
            assert false;
        }
    }
}