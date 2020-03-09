package org.broadinstitute.consent.http;

import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.authentication.OAuthAuthenticator;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.jdbi.v3.core.Jdbi;
import org.mockito.Mockito;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.broadinstitute.consent.http.ConsentModule.DB_ENV;

/**
 * An abstract superclass for Tests that involve the BOSS API, includes helper methods for setting up
 * a Dropwizard Configuration as well as for all the standard calls (createObject, etc) into the API
 * through the Jersey client API.
 */
abstract public class AbstractTest extends ResourcedTest {

    private final Logger logger = Logger.getLogger(AbstractTest.class);
    public static final int BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();

    abstract public DropwizardAppRule<ConsentConfiguration> rule();

    /*
     * Some utility methods for interacting with HTTP-services.
     */

    public <T> Response put(Client client, String path, T value) {
        mockValidateTokenResponse();
        return client.target(path)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer api_token")
                .put(Entity.json(value), Response.class);
    }

    public Response getJson(Client client, String path) {
        mockValidateTokenResponse();
        return getWithMediaType(client, path, MediaType.APPLICATION_JSON_TYPE);
    }

    private Response getWithMediaType(Client client, String path, MediaType mediaType) {
        return client.target(path)
                .request(mediaType)
                .header("Authorization", "Bearer api_token")
                .get(Response.class);
    }
    public String path2Url(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }
        return String.format("http://localhost:%d/api/%s", rule().getLocalPort(), path);
    }

    public void mockValidateTokenResponse() {
        Client client = Mockito.mock(Client.class);
        // Mock the token response from google
        String tokenResponse = "{ " +
                "\"azp\": \"1234564ko.apps.googleusercontent.com\", " +
                "\"aud\": \"clientId\", " +
                "\"sub\": \"1234564\", " +
                "\"scope\": \"https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/plus.me\", " +
                "\"exp\": \"1472235144\", " +
                "\"expires_in\": \"3567\", " +
                "\"email\": \"oauthuser@broadinstitute.org\", " +
                "\"email_verified\": \"true\", " +
                "\"access_type\": \"online\" " +
                " }";
        mockUrlAndEntityResponse(client, "tokeninfo", tokenResponse);
        // Mock the user info response from google
        String userProfile = "{" +
                "\"sub\": \"...\", " +
                "\"name\": \"oauth user\", " +
                "\"given_name\": \"oauth\", " +
                "\"family_name\": \"user\", " +
                "\"profile\": \"https://plus.google.com/....\", " +
                "\"picture\": \"https://lh3.googleusercontent.com/....\", " +
                "\"email\": \"oauthuser@broadinstitute.org\", " +
                "\"email_verified\": true," +
                "\"locale\": \"en\", " +
                "\"hd\": \"broadinstitute.org\"" +
                "}";
        mockUrlAndEntityResponse(client, "userinfo", userProfile);
        OAuthAuthenticator.getInstance().setClient(client);
    }

    private void mockUrlAndEntityResponse(Client client, String urlMatch, String entityString) {
        WebTarget webTarget = Mockito.mock(WebTarget.class);
        Invocation.Builder builder = Mockito.mock(Invocation.Builder.class);
        Response response = Mockito.mock(Response.class);
        Mockito.when(response.readEntity(String.class)).thenReturn(entityString);
        Mockito.when(webTarget.request(Mockito.any(MediaType.class))).thenReturn(builder);
        Mockito.when(builder.get(Response.class)).thenReturn(response);
        Mockito.when(client.target(Mockito.contains(urlMatch))).thenReturn(webTarget);
    }

    protected Jdbi getApplicationJdbi() {
        String dbiExtension = "_" + RandomStringUtils.random(10, true, false);
        ConsentConfiguration configuration = rule().getConfiguration();
        Environment environment = rule().getEnvironment();
        return new JdbiFactory().build(environment, configuration.getDataSourceFactory(), DB_ENV + dbiExtension);
    }

}
