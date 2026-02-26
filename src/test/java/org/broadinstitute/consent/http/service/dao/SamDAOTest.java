package org.broadinstitute.consent.http.service.dao;

import static org.broadinstitute.consent.http.db.SamDAO.getErrorMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.MockServerTestHelper;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.sam.CombinedState;
import org.broadinstitute.consent.http.models.sam.EmailResponse;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.model.Delay;
import org.mockserver.model.Header;
import org.mockserver.model.HttpError;
import org.mockserver.model.MediaType;

@ExtendWith(MockitoExtension.class)
class SamDAOTest extends MockServerTestHelper {

  private static SamDAO samDAO;

  @Mock private DuosUser duosUser;

  private UserStatus status;

  @BeforeAll
  static void setUp() {
    ServicesConfiguration servicesConfig = new ServicesConfiguration();
    servicesConfig.setTimeoutSeconds(1);
    servicesConfig.setSamUrl(
        "http://" + CONTAINER.getHost() + ":" + CONTAINER.getServerPort() + "/");
    samDAO = new SamDAO(new HttpClientUtil(servicesConfig), servicesConfig);
  }

  @BeforeEach
  void init() {
    UserStatus.UserInfo info =
        new UserStatus.UserInfo().setUserEmail("test@test.org").setUserSubjectId("subjectId");
    UserStatus.Enabled enabled =
        new UserStatus.Enabled().setAllUsersGroup(true).setGoogle(true).setLdap(true);
    status = new UserStatus().setUserInfo(info).setEnabled(enabled);
  }

  @Test
  void testGetResourceTypes() throws Exception {
    ResourceType resourceType =
        new ResourceType().setName(randomAlphanumeric(10)).setReuseIds(randomBoolean());
    List<ResourceType> mockResponseList = Collections.singletonList(resourceType);
    Gson gson = new Gson();
    mockServerClient
        .when(request())
        .respond(
            response()
                .withStatusCode(HttpStatusCodes.STATUS_CODE_OK)
                .withBody(gson.toJson(mockResponseList)));

    List<ResourceType> resourceTypeList = samDAO.getResourceTypes(duosUser);
    assertFalse(resourceTypeList.isEmpty());
    assertEquals(mockResponseList.size(), resourceTypeList.size());
  }

  @Test
  void testGetRegistrationInfo() throws Exception {
    UserStatusInfo userInfo =
        new UserStatusInfo()
            .setAdminEnabled(randomBoolean())
            .setUserEmail("test@test.org")
            .setUserSubjectId(randomAlphanumeric(10))
            .setEnabled(randomBoolean());
    mockServerClient
        .when(request())
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_OK)
                .withBody(userInfo.toString()));

    UserStatusInfo authUserUserInfo = samDAO.getRegistrationInfo(duosUser);
    assertNotNull(authUserUserInfo);
    assertEquals(userInfo.getUserEmail(), authUserUserInfo.getUserEmail());
    assertEquals(userInfo.getEnabled(), authUserUserInfo.getEnabled());
    assertEquals(userInfo.getUserSubjectId(), authUserUserInfo.getUserSubjectId());
  }

  @Test
  void testGetRegistrationInfoBadRequest() {
    mockServerClient
        .when(request())
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_BAD_REQUEST));
    assertThrows(BadRequestException.class, () -> samDAO.getRegistrationInfo(duosUser));
  }

  @Test
  void testNotAuthorized() {
    mockServerClient
        .when(request())
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED));
    assertThrows(NotAuthorizedException.class, () -> samDAO.getRegistrationInfo(duosUser));
  }

  @Test
  void testForbidden() {
    mockServerClient
        .when(request())
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_FORBIDDEN));
    assertThrows(ForbiddenException.class, () -> samDAO.getRegistrationInfo(duosUser));
  }

  @Test
  void testNotFound() {
    setDebugLogging();
    mockServerClient
        .when(request())
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_NOT_FOUND));
    assertThrows(NotFoundException.class, () -> samDAO.getRegistrationInfo(duosUser));
  }

  @Test
  void testConflict() {
    mockServerClient
        .when(request())
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_CONFLICT));
    assertThrows(ConsentConflictException.class, () -> samDAO.getRegistrationInfo(duosUser));
  }

  @Test
  void testGetSelfDiagnostics() throws Exception {
    UserStatusDiagnostics diagnostics =
        new UserStatusDiagnostics()
            .setAdminEnabled(randomBoolean())
            .setEnabled(randomBoolean())
            .setInAllUsersGroup(randomBoolean())
            .setInGoogleProxyGroup(randomBoolean())
            .setTosAccepted(randomBoolean());
    mockServerClient
        .when(request())
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_OK)
                .withBody(diagnostics.toString()));

    UserStatusDiagnostics userDiagnostics = samDAO.getSelfDiagnostics(duosUser);
    assertNotNull(userDiagnostics);
    assertEquals(diagnostics.getEnabled(), userDiagnostics.getEnabled());
    assertEquals(diagnostics.getInAllUsersGroup(), userDiagnostics.getInAllUsersGroup());
    assertEquals(diagnostics.getInGoogleProxyGroup(), userDiagnostics.getInGoogleProxyGroup());
  }

  @Test
  void testPostRegistrationInfo() throws Exception {
    mockServerClient
        .when(request())
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_CREATED)
                .withBody(status.toString()));

    UserStatus userStatus = samDAO.postRegistrationInfo(duosUser);
    assertNotNull(userStatus);
  }

  @Test
  void testPostRegistrationInfo_Error() {
    mockServerClient
        .when(request())
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_SERVER_ERROR)
                .withBody(("{\"message\":\"errorMessage\"}")));
    when(duosUser.getEmail()).thenReturn("email@email.com");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> samDAO.postRegistrationInfo(duosUser));
    assertEquals(
        "Error posting user registration information. Email: email@email.com. errorMessage.",
        ex.getMessage());
  }

  /**
   * This test doesn't technically work due to some sort of async issue. The response is terminated
   * before the http request can finish executing. The response completes as expected in the
   * non-async case (see #testPostRegistrationInfo()). In practice, the async calls work as
   * expected.
   */
  @Test
  void testAsyncPostRegistrationInfo() {
    mockServerClient
        .when(request())
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_CREATED)
                .withBody(status.toString()));

    try {
      samDAO.asyncPostRegistrationInfo(duosUser);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  // Provide all combinations of true/false for isCurrentVersion and permitsSystemUsage to test the
  // logic in getCombinedUserStatusInfo
  private static Stream<Arguments> provideBooleansForUserStatus() {
    return Stream.of(
        Arguments.of(false, false),
        Arguments.of(false, true),
        Arguments.of(true, false),
        Arguments.of(true, true));
  }

  @ParameterizedTest
  @MethodSource("provideBooleansForUserStatus")
  void testGetCombinedUserStatusInfo(boolean isCurrentVersion, boolean permitsSystemUsage)
      throws Exception {
    CombinedState.SamUser samUser =
        new CombinedState.SamUser(
            "azureB2CId", "createdAt", "email", true, "googleSubjectId", "id", "updatedAt");
    CombinedState.TermsOfServiceDetails tosDetails =
        new CombinedState.TermsOfServiceDetails(
            "acceptedOn", isCurrentVersion, "latestAcceptedVersion", permitsSystemUsage);
    CombinedState combinedState =
        new CombinedState().setSamUser(samUser).setTermsOfServiceDetails(tosDetails);
    Gson gson = new Gson();
    mockServerClient
        .when(request())
        .respond(
            response()
                .withStatusCode(HttpStatusCodes.STATUS_CODE_OK)
                .withBody(gson.toJson(combinedState)));

    UserStatusInfo userStatusInfo = samDAO.getCombinedUserStatusInfo(duosUser);
    assertNotNull(userStatusInfo);
    assertEquals(samUser.email(), userStatusInfo.getUserEmail());
    assertEquals(samUser.googleSubjectId(), userStatusInfo.getUserSubjectId());
    assertEquals(samUser.enabled(), userStatusInfo.getEnabled());
    assertEquals(
        (tosDetails.permitsSystemUsage() && tosDetails.isCurrentVersion()),
        userStatusInfo.getTosAccepted());
  }

  @Test
  void testGetCombinedUserStatusInfoNotFound() {
    mockServerClient
        .when(request())
        .respond(response().withStatusCode(HttpStatusCodes.STATUS_CODE_FORBIDDEN));

    assertThrows(NotFoundException.class, () -> samDAO.getCombinedUserStatusInfo(duosUser));
  }

  @Test
  void testGetCombinedUserStatusInfoNonSuccessStatus() {
    mockServerClient
        .when(request())
        .respond(response().withStatusCode(HttpStatusCodes.STATUS_CODE_MOVED_PERMANENTLY));

    assertThrows(WebApplicationException.class, () -> samDAO.getCombinedUserStatusInfo(duosUser));
  }

  @Test
  void testGetToSText() {
    String mockText = "Plain Text";
    mockServerClient
        .when(request())
        .respond(
            response()
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
    mockServerClient
        .when(request())
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_OK));
    try {
      samDAO.getTosResponse(duosUser);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testPostTosAcceptedStatus() {
    mockServerClient
        .when(request())
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_OK));

    try {
      samDAO.acceptTosStatus(duosUser);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testRemoveTosAcceptedStatus() {
    mockServerClient
        .when(request())
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_OK));

    try {
      samDAO.rejectTosStatus(duosUser);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testGetV1UserByEmail() {
    EmailResponse emailResponse = new EmailResponse("googleId", "email", "subjectId");
    Gson gson = GsonUtil.buildGson();
    mockServerClient
        .when(request())
        .respond(
            response()
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_OK)
                .withBody(gson.toJson(emailResponse)));

    try {
      EmailResponse response = samDAO.getV1UserByEmail(duosUser, "test@gmail.com");
      assertNotNull(response);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  @SuppressWarnings({"java:S5778"})
  void testConnectTimeout() {
    mockServerClient.when(request()).error(HttpError.error().withDropConnection(true));
    assertThrows(
        ServerErrorException.class, () -> samDAO.getV1UserByEmail(duosUser, randomAlphabetic(10)));
  }

  @Test
  @SuppressWarnings({"java:S5778"})
  void testReadTimeout() {
    // Increase the delay to push the response beyond the read timeout value
    int delayMilliseconds = samDAO.readTimeoutMilliseconds + 10;
    mockServerClient
        .when(request())
        .respond(
            response()
                .withDelay(new Delay(TimeUnit.MILLISECONDS, delayMilliseconds))
                .withHeader(Header.header("Content-Type", "application/json"))
                .withStatusCode(HttpStatusCodes.STATUS_CODE_OK));
    assertThrows(
        ServerErrorException.class, () -> samDAO.getV1UserByEmail(duosUser, randomAlphabetic(10)));
  }

  @Test
  void testGetErrorMessageAzureB2cId() {
    when(duosUser.getEmail()).thenReturn("email@email.com");
    String body =
        """
            {"code":500, "message": "Cannot update azureB2cId"}""";
    assertEquals(
        "Email: email@email.com. You may have previously signed in with a different authentication provider (Google or Microsoft). Please sign in with that provider. For more information visit: https://support.terra.bio/hc/en-us/community/posts/24089648317467-Cannot-update-azureB2cId-for-user",
        getErrorMessage(duosUser, body));
  }

  @Test
  void testGetErrorMessageOther() {
    when(duosUser.getEmail()).thenReturn("email@email.com");
    String body =
        """
            {"code":500, "message": "some other error"}""";
    assertEquals(
        "Error posting user registration information. Email: email@email.com. some other error.",
        getErrorMessage(duosUser, body));
  }

  @Test
  void testGetErrorMessageNoMessage() {
    when(duosUser.getEmail()).thenReturn("email@email.com");
    String body =
        """
            {"code":500}""";
    assertEquals(
        """
            Error posting user registration information. Email: email@email.com. {"code":500}.""",
        getErrorMessage(duosUser, body));
  }

  @Test
  void testGetErrorMessageNoBody() {
    when(duosUser.getEmail()).thenReturn("email@email.com");
    String body = null;
    assertEquals(
        """
            Error posting user registration information. Email: email@email.com.""",
        getErrorMessage(duosUser, body));
  }

  @Test
  void testGetErrorMessageNotJson() {
    when(duosUser.getEmail()).thenReturn("email@email.com");
    String body = "random non-JSON string";
    assertEquals(
        """
            Error posting user registration information. Email: email@email.com. random non-JSON string.""",
        getErrorMessage(duosUser, body));
  }
}
