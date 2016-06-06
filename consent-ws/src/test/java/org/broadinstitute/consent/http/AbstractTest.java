package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.service.DatabaseTranslateServiceAPI;
import org.eclipse.jetty.http.HttpHeader;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.validate.ValidateResponse;
import org.broadinstitute.consent.http.service.validate.UseRestrictionValidator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An abstract superclass for Tests that involve the BOSS API, includes helper methods for setting up
 * a Dropwizard Configuration as well as for all the standard calls (createObject, etc) into the API
 * through the Jersey client API.
 */
abstract public class AbstractTest extends ResourcedTest {

    public static final int CREATED = Response.Status.CREATED.getStatusCode();
    public static final int OK = Response.Status.OK.getStatusCode();
    public static final int NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    public static final int BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    //testuser
    //public static final String BASIC_AUTHENTICATION = "Basic dGVzdHVzZXI6dGVzdHBhc3N3b3Jk";

    abstract public DropwizardAppRule<ConsentConfiguration> rule();

    /*
     * Some utility methods for interacting with HTTP-services.
     */

    public <T> Response post(Client client, String path, T value) {
        return post(client, path, "testuser", value);
    }

    public <T> Response post(Client client, String path, String user, T value) {
        return client.target(path)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("REMOTE_USER", user)
                //.header(HttpHeader.AUTHORIZATION.asString(), BASIC_AUTHENTICATION)
                .post(Entity.json(value), Response.class);
    }

    public <T> Response put(Client client, String path, T value) {
        return put(client, path, "testuser", value);
    }

    public <T> Response put(Client client, String path, String user, T value) {
        return client.target(path)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("REMOTE_USER", user)
                //.header(HttpHeader.AUTHORIZATION.asString(), BASIC_AUTHENTICATION)
                .put(Entity.json(value), Response.class);
    }

    public Response delete(Client client, String path) {
        return delete(client, path, "testuser");
    }

    public Response delete(Client client, String path, String user) {
        return client.target(path)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("REMOTE_USER", user)
                //.header("Authorization", BASIC_AUTHENTICATION)
                .delete(Response.class);
    }

    public Response getJson(Client client, String path) {
        return getWithMediaType(client, path, MediaType.APPLICATION_JSON_TYPE);
    }

    public Response getTextPlain(Client client, String path) {
        return getWithMediaType(client, path, MediaType.TEXT_PLAIN_TYPE);
    }

    private Response getWithMediaType(Client client, String path, MediaType mediaType) {
        return client.target(path)
                .request(mediaType)
                .header("REMOTE_USER", "testuser")
                //.header("Authorization", BASIC_AUTHENTICATION)
                .get(Response.class);
    }

    public Response check200(Response response) {
        return checkStatus(200, response);
    }

    public Response checkStatus(int status, Response response) {
        assertThat(response.getStatus()).isEqualTo(status);
        return response;
    }

    public String checkHeader(Response response, String header) {
        MultivaluedMap<String, Object> map = response.getHeaders();
        assertThat(map).describedAs(String.format("header \"%s\"", header)).containsKey(header);
        return map.getFirst(header).toString();
    }

    public String path2Url(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }
        return String.format("http://localhost:%d/%s", rule().getLocalPort(), path);
    }

    public void mockTranslateResponse(){
        final Invocation.Builder builderMock = Mockito.mock(Invocation.Builder.class);
        final WebTarget webTargetMock = Mockito.mock(WebTarget.class);
        String mockString = "TranslateMock";
        InputStream stream = new ByteArrayInputStream(mockString.getBytes(StandardCharsets.UTF_8));
        Response responseMock = Response.status(Response.Status.OK).entity(stream).build();
        Mockito.when(builderMock.post(Entity.json(Mockito.anyString()))).thenReturn(responseMock);
        Mockito.when(webTargetMock.request(MediaType.APPLICATION_JSON)).thenReturn(builderMock);
        final Client clientMock= Mockito.mock(Client.class);
        Mockito.when(clientMock.target(Mockito.anyString())).thenReturn(webTargetMock);
        Mockito.when(webTargetMock.queryParam(Mockito.anyString(), Mockito.anyString())).thenReturn(webTargetMock);
        DatabaseTranslateServiceAPI.getInstance().setClient(clientMock);
    }

    public void mockValidateResponse(){
        final Invocation.Builder builderMock = Mockito.mock(Invocation.Builder.class);
        final WebTarget webTargetMock = Mockito.mock(WebTarget.class);
        ValidateResponse entity = new ValidateResponse(true, "mockedValidatedRestriction");


        final Response responseMock = Mockito.mock(Response.class);
        Mockito.when(responseMock.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        Mockito.when(responseMock.readEntity(ValidateResponse.class)).thenReturn(entity);

        Mockito.when(builderMock.post(Entity.json(Mockito.anyString()))).thenReturn(responseMock);
        Mockito.when(webTargetMock.request(MediaType.APPLICATION_JSON)).thenReturn(builderMock);
        final Client clientMock= Mockito.mock(Client.class);
        Mockito.when(clientMock.target(Mockito.anyString())).thenReturn(webTargetMock);
        Mockito.when(webTargetMock.queryParam(Mockito.anyString(), Mockito.anyString())).thenReturn(webTargetMock);
        UseRestrictionValidator.getInstance().setClient(clientMock);
    }
}
