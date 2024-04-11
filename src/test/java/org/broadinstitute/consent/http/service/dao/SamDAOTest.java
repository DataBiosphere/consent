package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.WithMockServer;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.EmailResponse;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Delay;
import org.mockserver.model.Header;
import org.mockserver.model.HttpError;
import org.mockserver.model.MediaType;
import org.testcontainers.containers.MockServerContainer;

@ExtendWith(MockitoExtension.class)
class SamDAOTest implements WithMockServer {

  private SamDAO samDAO;

  private MockServerClient mockServerClient;

  @Mock
  private AuthUser authUser;

  private static final MockServerContainer container = new MockServerContainer(IMAGE);

  @BeforeAll
  public static void setUp() {
    container.start();
  }

  @AfterAll
  public static void tearDown() {
    container.stop();
  }

  @BeforeEach
  public void init() {
    mockServerClient = new MockServerClient(container.getHost(), container.getServerPort());
    mockServerClient.reset();
    ServicesConfiguration config = new ServicesConfiguration();
    config.setTimeoutSeconds(1);
    config.setSamUrl("http://" + container.getHost() + ":" + container.getServerPort() + "/");
    samDAO = new SamDAO(new HttpClientUtil(config), config);
  }

  @Test
  void testGetResourceTypes() throws Exception {
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
  void testGetRegistrationInfo() throws Exception {
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

  @Test
  void testGetRegistrationInfoBadRequest() {
    mockServerClient.when(request())
        .respond(response()
            .withHeader(Header.header("Content-Type", "application/json"))
            .withStatusCode(HttpStatusCodes.STATUS_CODE_BAD_REQUEST));
    assertThrows(BadRequestException.class, () -> {
      samDAO.getRegistrationInfo(authUser);
    });
  }

  @Test
  void testNotAuthorized() {
    mockServerClient.when(request())
        .respond(response()
            .withHeader(Header.header("Content-Type", "application/json"))
            .withStatusCode(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED));
    assertThrows(NotAuthorizedException.class, () -> {
      samDAO.getRegistrationInfo(authUser);
    });
  }

  @Test
  void testForbidden() {
    mockServerClient.when(request())
        .respond(response()
            .withHeader(Header.header("Content-Type", "application/json"))
            .withStatusCode(HttpStatusCodes.STATUS_CODE_FORBIDDEN));
    assertThrows(ForbiddenException.class, () -> {
      samDAO.getRegistrationInfo(authUser);
    });
  }

  @Test
  void testNotFound() {
    setDebugLogging();
    mockServerClient.when(request())
        .respond(response()
            .withHeader(Header.header("Content-Type", "application/json"))
            .withStatusCode(HttpStatusCodes.STATUS_CODE_NOT_FOUND));
    assertThrows(NotFoundException.class, () -> {
      samDAO.getRegistrationInfo(authUser);
    });
  }

  @Test
  void testConflict() {
    mockServerClient.when(request())
        .respond(response()
            .withHeader(Header.header("Content-Type", "application/json"))
            .withStatusCode(HttpStatusCodes.STATUS_CODE_CONFLICT));
    assertThrows(ConsentConflictException.class, () -> {
      samDAO.getRegistrationInfo(authUser);
    });
  }

  @Test
  void testGetSelfDiagnostics() throws Exception {
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
    assertEquals(diagnostics.getInAllUsersGroup(),
        userDiagnostics.getInAllUsersGroup());
    assertEquals(diagnostics.getInGoogleProxyGroup(),
        userDiagnostics.getInGoogleProxyGroup());
  }

  @Test
  void testPostRegistrationInfo() throws Exception {
    UserStatus.UserInfo info = new UserStatus.UserInfo().setUserEmail("test@test.org")
        .setUserSubjectId("subjectId");
    UserStatus.Enabled enabled = new UserStatus.Enabled().setAllUsersGroup(true).setGoogle(true)
        .setLdap(true);
    UserStatus status = new UserStatus().setUserInfo(info).setEnabled(enabled);
    mockServerClient.when(request())
        .respond(response()
            .withHeader(Header.header("Content-Type", "application/json"))
            .withStatusCode(HttpStatusCodes.STATUS_CODE_CREATED)
            .withBody(status.toString()));

    UserStatus userStatus = samDAO.postRegistrationInfo(authUser);
    assertNotNull(userStatus);
  }

  @Test
  void testPostRegistrationInfo_Conflict() {
    UserStatus.UserInfo info = new UserStatus.UserInfo().setUserEmail("test@test.org")
        .setUserSubjectId("subjectId");
    UserStatus.Enabled enabled = new UserStatus.Enabled().setAllUsersGroup(true).setGoogle(true)
        .setLdap(true);
    UserStatus status = new UserStatus().setUserInfo(info).setEnabled(enabled);
    mockServerClient.when(request())
        .respond(response()
            .withHeader(Header.header("Content-Type", "application/json"))
            .withStatusCode(HttpStatusCodes.STATUS_CODE_CONFLICT)
            .withBody(status.toString()));

    assertThrows(ConsentConflictException.class, () -> {
      samDAO.postRegistrationInfo(authUser);
    });
  }

  @Test
  void testPostRegistrationInfo_Error() {
    UserStatus.UserInfo info = new UserStatus.UserInfo().setUserEmail("test@test.org")
        .setUserSubjectId("subjectId");
    UserStatus.Enabled enabled = new UserStatus.Enabled().setAllUsersGroup(true).setGoogle(true)
        .setLdap(true);
    UserStatus status = new UserStatus().setUserInfo(info).setEnabled(enabled);
    mockServerClient.when(request())
        .respond(response()
            .withHeader(Header.header("Content-Type", "application/json"))
            .withStatusCode(HttpStatusCodes.STATUS_CODE_SERVER_ERROR)
            .withBody(status.toString()));

    assertThrows(Exception.class, () -> {
      samDAO.postRegistrationInfo(authUser);
    });
  }

  /**
   * This test doesn't technically work due to some sort of async issue. The response is terminated
   * before the http request can finish executing. The response completes as expected in the
   * non-async case (see #testPostRegistrationInfo()). In practice, the async calls work as
   * expected.
   */
  @Test
  void testAsyncPostRegistrationInfo() {
    UserStatus.UserInfo info = new UserStatus.UserInfo().setUserEmail("test@test.org")
        .setUserSubjectId("subjectId");
    UserStatus.Enabled enabled = new UserStatus.Enabled().setAllUsersGroup(true).setGoogle(true)
        .setLdap(true);
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
  void testGetToSText() {
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
  void testGetTosResponse() {
    mockServerClient.when(request())
        .respond(response()
            .withHeader(Header.header("Content-Type", "application/json"))
            .withStatusCode(HttpStatusCodes.STATUS_CODE_OK));
    try {
      samDAO.getTosResponse(authUser);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testPostTosAcceptedStatus() {
    mockServerClient.when(request())
        .respond(response()
            .withHeader(Header.header("Content-Type", "application/json"))
            .withStatusCode(HttpStatusCodes.STATUS_CODE_OK));

    try {
      samDAO.acceptTosStatus(authUser);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testRemoveTosAcceptedStatus() {
    mockServerClient.when(request())
        .respond(response()
            .withHeader(Header.header("Content-Type", "application/json"))
            .withStatusCode(HttpStatusCodes.STATUS_CODE_OK));

    try {
      samDAO.rejectTosStatus(authUser);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testGetV1UserByEmail() {
    EmailResponse emailResponse = new EmailResponse("googleId", "email", "subjectId");
    Gson gson = GsonUtil.buildGson();
    mockServerClient.when(request())
        .respond(response()
            .withHeader(Header.header("Content-Type", "application/json"))
            .withStatusCode(HttpStatusCodes.STATUS_CODE_OK)
            .withBody(gson.toJson(emailResponse)));

    try {
      EmailResponse response = samDAO.getV1UserByEmail(authUser, "test@gmail.com");
      assertNotNull(response);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testConnectTimeout() {
    mockServerClient.when(request()).error(HttpError.error().withDropConnection(true));
    assertThrows(
        ServerErrorException.class,
        () -> samDAO.getV1UserByEmail(authUser, RandomStringUtils.randomAlphabetic(10)));
  }

  @Test
  void testReadTimeout() {
    // Increase the delay to push the response beyond the read timeout value
    int delayMilliseconds = samDAO.readTimeoutMilliseconds + 10;
    mockServerClient.when(request())
        .respond(response()
            .withDelay(new Delay(TimeUnit.MILLISECONDS, delayMilliseconds))
            .withHeader(Header.header("Content-Type", "application/json"))
            .withStatusCode(HttpStatusCodes.STATUS_CODE_OK));
    assertThrows(
        ServerErrorException.class,
        () -> samDAO.getV1UserByEmail(authUser, RandomStringUtils.randomAlphabetic(10)));
  }

}
