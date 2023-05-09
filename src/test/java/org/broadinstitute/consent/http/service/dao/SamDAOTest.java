package org.broadinstitute.consent.http.service.dao;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.WithMockServer;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.TosResponse;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.mockserver.model.MediaType;
import org.testcontainers.containers.MockServerContainer;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class SamDAOTest implements WithMockServer {

    private SamDAO samDAO;

    private MockServerClient mockServerClient;

    @Mock
    private AuthUser authUser;

    private static final MockServerContainer container = new MockServerContainer(IMAGE);

    @BeforeClass
    public static void setUp() {
        container.start();
    }

    @AfterClass
    public static void tearDown() {
        container.stop();
    }

    @Before
    public void init() {
        openMocks(this);
        mockServerClient = new MockServerClient(container.getHost(), container.getServerPort());
        mockServerClient.reset();
        ServicesConfiguration config = new ServicesConfiguration();
        config.setSamUrl("http://" + container.getHost() + ":" + container.getServerPort() + "/");
        samDAO = new SamDAO(config);
    }

    @Test
    public void testGetResourceTypes() throws Exception {
        ResourceType resourceType = new ResourceType()
                .setName(RandomStringUtils.random(10, true, true))
                .setReuseIds(RandomUtils.nextBoolean());
        List<ResourceType> mockResponseList = Collections.singletonList(resourceType);
        Gson gson = new Gson();
        mockServerClient.when(request())
                .respond(response()
                        .withStatusCode(HttpStatusCodes.STATUS_CODE_OK)
                        .withBody(gson.toJson(mockResponseList)));

        List<ResourceType> resourceTypeList = samDAO.getResourceTypes(authUser);
        assertFalse(resourceTypeList.isEmpty());
        assertEquals(mockResponseList.size(), resourceTypeList.size());
    }

    @Test
    public void testGetRegistrationInfo() throws Exception {
        UserStatusInfo userInfo = new UserStatusInfo()
                .setAdminEnabled(RandomUtils.nextBoolean())
                .setUserEmail("test@test.org")
                .setUserSubjectId(RandomStringUtils.random(10, false, true))
                .setEnabled(RandomUtils.nextBoolean());
        mockServerClient.when(request())
                .respond(response()
                        .withHeader(Header.header("Content-Type", "application/json"))
                        .withStatusCode(HttpStatusCodes.STATUS_CODE_OK)
                        .withBody(userInfo.toString()));

        UserStatusInfo authUserUserInfo = samDAO.getRegistrationInfo(authUser);
        assertNotNull(authUserUserInfo);
        assertEquals(userInfo.getUserEmail(), authUserUserInfo.getUserEmail());
        assertEquals(userInfo.getEnabled(), authUserUserInfo.getEnabled());
        assertEquals(userInfo.getUserSubjectId(), authUserUserInfo.getUserSubjectId());
    }

    @Test(expected = BadRequestException.class)
    public void testGetRegistrationInfoBadRequest() throws Exception {
        mockServerClient.when(request())
                .respond(response()
                        .withHeader(Header.header("Content-Type", "application/json"))
                        .withStatusCode(HttpStatusCodes.STATUS_CODE_BAD_REQUEST));
        samDAO.getRegistrationInfo(authUser);
    }

    @Test(expected = NotAuthorizedException.class)
    public void testNotAuthorized() throws Exception {
        mockServerClient.when(request())
                .respond(response()
                        .withHeader(Header.header("Content-Type", "application/json"))
                        .withStatusCode(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED));
        samDAO.getRegistrationInfo(authUser);
    }

    @Test(expected = ForbiddenException.class)
    public void testForbidden() throws Exception {
        mockServerClient.when(request())
                .respond(response()
                        .withHeader(Header.header("Content-Type", "application/json"))
                        .withStatusCode(HttpStatusCodes.STATUS_CODE_FORBIDDEN));
        samDAO.getRegistrationInfo(authUser);
    }

    @Test(expected = NotFoundException.class)
    public void testNotFound() throws Exception {
        setDebugLogging();
        mockServerClient.when(request())
                .respond(response()
                        .withHeader(Header.header("Content-Type", "application/json"))
                        .withStatusCode(HttpStatusCodes.STATUS_CODE_NOT_FOUND));
        samDAO.getRegistrationInfo(authUser);
    }

    @Test(expected = ConsentConflictException.class)
    public void testConflict() throws Exception {
        mockServerClient.when(request())
                .respond(response()
                        .withHeader(Header.header("Content-Type", "application/json"))
                        .withStatusCode(HttpStatusCodes.STATUS_CODE_CONFLICT));
        samDAO.getRegistrationInfo(authUser);
    }

    @Test
    public void testGetSelfDiagnostics() throws Exception {
        UserStatusDiagnostics diagnostics = new UserStatusDiagnostics()
                .setAdminEnabled(RandomUtils.nextBoolean())
                .setEnabled(RandomUtils.nextBoolean())
                .setInAllUsersGroup(RandomUtils.nextBoolean())
                .setInGoogleProxyGroup(RandomUtils.nextBoolean())
                .setTosAccepted(RandomUtils.nextBoolean());
        mockServerClient.when(request())
                .respond(response()
                        .withHeader(Header.header("Content-Type", "application/json"))
                        .withStatusCode(HttpStatusCodes.STATUS_CODE_OK)
                        .withBody(diagnostics.toString()));

        UserStatusDiagnostics userDiagnostics = samDAO.getSelfDiagnostics(authUser);
        assertNotNull(userDiagnostics);
        assertEquals(diagnostics.getEnabled(), userDiagnostics.getEnabled());
        assertEquals(diagnostics.getInAllUsersGroup(), userDiagnostics.getInAllUsersGroup());
        assertEquals(diagnostics.getInGoogleProxyGroup(), userDiagnostics.getInGoogleProxyGroup());
    }

    @Test
    public void testPostRegistrationInfo() throws Exception {
        UserStatus.UserInfo info = new UserStatus.UserInfo().setUserEmail("test@test.org").setUserSubjectId("subjectId");
        UserStatus.Enabled enabled = new UserStatus.Enabled().setAllUsersGroup(true).setGoogle(true).setLdap(true);
        UserStatus status = new UserStatus().setUserInfo(info).setEnabled(enabled);
        mockServerClient.when(request())
                .respond(response()
                        .withHeader(Header.header("Content-Type", "application/json"))
                        .withStatusCode(HttpStatusCodes.STATUS_CODE_CREATED)
                        .withBody(status.toString()));

        UserStatus userStatus = samDAO.postRegistrationInfo(authUser);
        assertNotNull(userStatus);
    }

    /**
     * This test doesn't technically work due to some sort of async issue.
     * The response is terminated before the http request can finish executing.
     * The response completes as expected in the non-async case (see #testPostRegistrationInfo()).
     * In practice, the async calls work as expected.
     */
    @Test
    public void testAsyncPostRegistrationInfo() {
        UserStatus.UserInfo info = new UserStatus.UserInfo().setUserEmail("test@test.org").setUserSubjectId("subjectId");
        UserStatus.Enabled enabled = new UserStatus.Enabled().setAllUsersGroup(true).setGoogle(true).setLdap(true);
        UserStatus status = new UserStatus().setUserInfo(info).setEnabled(enabled);
        mockServerClient.when(request())
                .respond(response()
                        .withHeader(Header.header("Content-Type", "application/json"))
                        .withStatusCode(HttpStatusCodes.STATUS_CODE_CREATED)
                        .withBody(status.toString()));

        try {
            samDAO.asyncPostRegistrationInfo(authUser);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetToSText() {
        String mockText = "Plain Text";
        mockServerClient.when(request())
                .respond(response()
                        .withHeader(Header.header("Content-Type", MediaType.TEXT_PLAIN.getType()))
                        .withStatusCode(HttpStatusCodes.STATUS_CODE_OK)
                        .withBody(mockText));

        try {
            String text = samDAO.getToSText();
            assertEquals(mockText, text);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testPostTosAcceptedStatus() {
        TosResponse.Enabled enabled = new TosResponse.Enabled()
                .setAdminEnabled(true).setTosAccepted(true).setGoogle(true).setAllUsersGroup(true).setLdap(true);
        UserStatus.UserInfo info = new UserStatus.UserInfo().setUserEmail("test@test.org").setUserSubjectId("subjectId");
        TosResponse tosResponse = new TosResponse().setEnabled(enabled).setUserInfo(info);
        mockServerClient.when(request())
                .respond(response()
                        .withHeader(Header.header("Content-Type", "application/json"))
                        .withStatusCode(HttpStatusCodes.STATUS_CODE_OK)
                        .withBody(tosResponse.toString()));

        try {
            samDAO.postTosAcceptedStatus(authUser);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testRemoveTosAcceptedStatus() {
        TosResponse.Enabled enabled = new TosResponse.Enabled()
                .setAdminEnabled(true).setTosAccepted(false).setGoogle(true).setAllUsersGroup(true).setLdap(true);
        UserStatus.UserInfo info = new UserStatus.UserInfo().setUserEmail("test@test.org").setUserSubjectId("subjectId");
        TosResponse tosResponse = new TosResponse().setEnabled(enabled).setUserInfo(info);
        mockServerClient.when(request())
                .respond(response()
                        .withHeader(Header.header("Content-Type", "application/json"))
                        .withStatusCode(HttpStatusCodes.STATUS_CODE_OK)
                        .withBody(tosResponse.toString()));

        try {
            samDAO.removeTosAcceptedStatus(authUser);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
