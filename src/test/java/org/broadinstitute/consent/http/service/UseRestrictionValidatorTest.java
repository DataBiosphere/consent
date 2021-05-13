package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.WithMockServer;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.testcontainers.containers.MockServerContainer;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class UseRestrictionValidatorTest implements WithMockServer {

    private UseRestrictionValidator validator;
    private MockServerClient client;
    private final String useRestriction = new Everything().toString();

    @Rule
    public MockServerContainer container = new MockServerContainer(IMAGE);

    @Before
    public void startUp() {
        client = new MockServerClient(container.getHost(), container.getServerPort());
        this.validator = new UseRestrictionValidator(ClientBuilder.newClient(), config());
    }

    @After
    public void tearDown() {
        stop(container);
    }

    public ServicesConfiguration config() {
        ServicesConfiguration config = new ServicesConfiguration();
        config.setLocalURL("http://localhost:8180/");
        config.setOntologyURL(getRootUrl(container));
        return config;
    }

    private void mockSuccess() {
        client.reset();
        client.when(
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
        client.reset();
        client.when(
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
        client.reset();
        client.when(
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
        validator.validateUseRestriction(useRestriction);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateUseRestriction_Invalid() {
        mockInvalid();
        validator.validateUseRestriction(useRestriction);
    }

    @Test
    public void testValidateUseRestriction_Success() {
        mockSuccess();
        try {
            validator.validateUseRestriction(useRestriction);
            assert true;
        } catch (IllegalArgumentException iae) {
            assert false;
        }
    }
}