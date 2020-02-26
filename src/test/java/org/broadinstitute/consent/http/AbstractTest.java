package org.broadinstitute.consent.http;

import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.runtime.Network;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.authentication.OAuthAuthenticator;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentBuilder;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.broadinstitute.consent.http.models.validate.ValidateResponse;
import org.broadinstitute.consent.http.service.validate.UseRestrictionValidator;
import org.jdbi.v3.core.Jdbi;
import org.mockito.Mockito;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.broadinstitute.consent.http.ConsentModule.DB_ENV;

/**
 * An abstract superclass for Tests that involve the BOSS API, includes helper methods for setting up
 * a Dropwizard Configuration as well as for all the standard calls (createObject, etc) into the API
 * through the Jersey client API.
 */
abstract public class AbstractTest extends ResourcedTest {

    private final Logger logger = Logger.getLogger(AbstractTest.class);
    public static final int CREATED = Response.Status.CREATED.getStatusCode();
    static final int CONFLICT = Response.Status.CONFLICT.getStatusCode();
    static final int NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    public static final int OK = Response.Status.OK.getStatusCode();
    public static final int BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();

    abstract public DropwizardAppRule<ConsentConfiguration> rule();

    private MongodExecutable mongodExe;
    private MongodProcess mongod;

    /*
     * Some utility methods for interacting with HTTP-services.
     */

    public <T> Response post(Client client, String path, T value) {
        mockValidateTokenResponse();
        return client.target(path)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer api_token")
                .post(Entity.json(value), Response.class);
    }

    public <T> Response put(Client client, String path, T value) {
        mockValidateTokenResponse();
        return client.target(path)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer api_token")
                .put(Entity.json(value), Response.class);
    }

    public Response delete(Client client, String path) {
        mockValidateTokenResponse();
        return client.target(path)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer api_token")
                .delete(Response.class);
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

    void check200(Response response) {
        checkStatus(200, response);
    }

    public Response checkStatus(int status, Response response) {
        if (response.getStatus() != status) {
            logger.error("Incorrect response status: " + response.toString());
        }
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
        return String.format("http://localhost:%d/api/%s", rule().getLocalPort(), path);
    }

    void mockTranslateResponse() {
        final Invocation.Builder builderMock = Mockito.mock(Invocation.Builder.class);
        final WebTarget webTargetMock = Mockito.mock(WebTarget.class);
        String mockString = "TranslateMock";
        InputStream stream = new ByteArrayInputStream(mockString.getBytes(StandardCharsets.UTF_8));
        Response responseMock = Response.status(Response.Status.OK).entity(stream).build();
        Mockito.when(builderMock.post(Entity.json(Mockito.anyString()))).thenReturn(responseMock);
        Mockito.when(webTargetMock.request(MediaType.APPLICATION_JSON)).thenReturn(builderMock);
        final Client clientMock = Mockito.mock(Client.class);
        Mockito.when(clientMock.target(Mockito.anyString())).thenReturn(webTargetMock);
        Mockito.when(webTargetMock.queryParam(Mockito.anyString(), Mockito.anyString())).thenReturn(webTargetMock);
    }

    void mockValidateResponse() {
        final Invocation.Builder builderMock = Mockito.mock(Invocation.Builder.class);
        final WebTarget webTargetMock = Mockito.mock(WebTarget.class);
        ValidateResponse entity = new ValidateResponse(true, "mockedValidatedRestriction");


        final Response responseMock = Mockito.mock(Response.class);
        Mockito.when(responseMock.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        Mockito.when(responseMock.readEntity(ValidateResponse.class)).thenReturn(entity);

        Mockito.when(builderMock.post(Entity.json(Mockito.anyString()))).thenReturn(responseMock);
        Mockito.when(webTargetMock.request(MediaType.APPLICATION_JSON)).thenReturn(builderMock);
        final Client clientMock = Mockito.mock(Client.class);
        Mockito.when(clientMock.target(Mockito.anyString())).thenReturn(webTargetMock);
        Mockito.when(webTargetMock.queryParam(Mockito.anyString(), Mockito.anyString())).thenReturn(webTargetMock);
        UseRestrictionValidator.getInstance().setClient(clientMock);
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

    Consent generateNewConsent(UseRestriction useRestriction, DataUse dataUse) {
        Timestamp createDate = new Timestamp(new Date().getTime());
        return new ConsentBuilder().
                setRequiresManualReview(false).
                setUseRestriction(useRestriction).
                setDataUse(dataUse).
                setName(UUID.randomUUID().toString()).
                setCreateDate(createDate).
                setLastUpdate(createDate).
                setSortDate(createDate).
                build();
    }

    protected MongoClient setUpMongoClient() throws IOException {
        int port = 27017;
        String bindIp = "localhost";
        // Creating Mongodbruntime instance
        // Make flapdoodle/mongo respect dropwizard's logging framework
        // See https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo#usage---custom-mongod-process-output
        org.slf4j.Logger slf4jLogger = org.slf4j.LoggerFactory.getLogger(getClass().getName());
        IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                .defaultsWithLogger(Command.MongoD, slf4jLogger)
                .processOutput(ProcessOutput.getDefaultInstanceSilent())
                .build();
        MongodStarter starter = MongodStarter.getInstance(runtimeConfig);
        // Creating MongodbExecutable
        Storage replication = new Storage("target/mongo", null, 0);
        IMongodConfig config = new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .replication(replication)
                .net(new Net(bindIp, port, Network.localhostIsIPv6()))
                .build();
        mongodExe = starter.prepare(config);
        // Starting Mongodb
        mongod = mongodExe.start();
        return new MongoClient(bindIp, port);
    }

    protected void shutDownMongo() {
        mongod.stop();
        mongodExe.stop();
        // Force cleanup of any leftover mongo db files. Only deletes the `mongo` directory itself.
        try {
            FileUtils.deleteDirectory(new File("target/mongo"));
        } catch (IOException e) {
            logger.error("Unable to remove target/mongo directory");
        }
    }

    protected Jdbi getApplicationJdbi() {
        ConsentConfiguration configuration = rule().getConfiguration();
        Environment environment = rule().getEnvironment();
        return new JdbiFactory().build(environment, configuration.getDataSourceFactory(), DB_ENV);
    }

    String consentPath() {
        return path2Url("/consent");
    }

    String consentPath(String id) {
        try {
            return path2Url(String.format("consent/%s", URLEncoder.encode(id, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s", id);
        }
    }

    public String electionConsentPath(String id) {
        try {
            return path2Url(String.format("consent/%s/election", URLEncoder.encode(id, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s/election", id);
        }
    }

    public String electionConsentPathById(String referenceId, Integer electionId) {
        try {
            return path2Url(String.format("consent/%s/election/%s", URLEncoder.encode(referenceId, "UTF-8"), electionId));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s/election/%s", referenceId, electionId);
        }
    }

    public String voteConsentIdPath(String consentId, Integer voteId) {
        try {
            return path2Url(String.format("consent/%s/vote/%s", URLEncoder.encode(consentId, "UTF-8"), voteId));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s/vote/%s", consentId, voteId);
        }
    }

    public String voteConsentPath(String consentId) {
        try {
            return path2Url(String.format("consent/%s/vote", URLEncoder.encode(consentId, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s/vote", consentId);
        }
    }

    Consent retrieveConsent(Client client, String url) throws IOException {
        return getJson(client, url).readEntity(Consent.class);
    }

}
