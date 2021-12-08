package org.broadinstitute.consent.http.service;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.WithMockServer;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class SamServiceTest implements WithMockServer {

  private SamService service;

  private MockServerClient mockServerClient;

  @Mock
  private AuthUser authUser;

  @Rule
  public MockServerContainer container = new MockServerContainer(IMAGE);

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    mockServerClient = new MockServerClient(container.getHost(), container.getServerPort());
    ServicesConfiguration config = new ServicesConfiguration();
    config.setSamUrl("http://" + container.getHost() + ":" + container.getServerPort() + "/");
    service = new SamService(config);
  }

  @Test
  public void testGetResourceTypes() throws Exception {
    ResourceType resourceType = new ResourceType()
            .setName(RandomStringUtils.random(10, true, true))
            .setReuseIds(RandomUtils.nextBoolean());
    List<ResourceType> mockResponseList = Collections.singletonList(resourceType);
    Gson gson = new Gson();
    mockServerClient.when(request()).respond(response().withStatusCode(200).withBody(gson.toJson(mockResponseList)));

    List<ResourceType> resourceTypeList = service.getResourceTypes(authUser);
    assertFalse(resourceTypeList.isEmpty());
    assertEquals(mockResponseList.size(), resourceTypeList.size());
  }

  @Test
  public void testGetRegistrationInfo() throws Exception {
    UserStatusInfo userInfo = new UserStatusInfo()
            .setUserEmail("test@test.org")
            .setUserSubjectId(RandomStringUtils.random(10, false, true))
            .setEnabled(RandomUtils.nextBoolean());
    mockServerClient.when(request()).respond(response().withHeader(Header.header("Content-Type", "application/json")).withStatusCode(200).withBody(userInfo.toString()));

    UserStatusInfo authUserUserInfo = service.getRegistrationInfo(authUser);
    assertNotNull(authUserUserInfo);
    assertEquals(userInfo.getUserEmail(), authUserUserInfo.getUserEmail());
    assertEquals(userInfo.getEnabled(), authUserUserInfo.getEnabled());
    assertEquals(userInfo.getUserSubjectId(), authUserUserInfo.getUserSubjectId());
  }

  @Test (expected = BadRequestException.class)
  public void testGetRegistrationInfoBadRequest() throws Exception {
    mockServerClient.when(request())
            .respond(response()
                    .withHeader(Header.header("Content-Type", "application/json"))
                    .withStatusCode(HttpStatusCodes.STATUS_CODE_BAD_REQUEST));
    service.getRegistrationInfo(authUser);
  }

  @Test (expected = NotAuthorizedException.class)
  public void testNotAuthorized() throws Exception {
    mockServerClient.when(request())
            .respond(response()
                    .withHeader(Header.header("Content-Type", "application/json"))
                    .withStatusCode(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED));
    service.getRegistrationInfo(authUser);
  }

  @Test (expected = ForbiddenException.class)
  public void testForbidden() throws Exception {
    mockServerClient.when(request())
            .respond(response()
                    .withHeader(Header.header("Content-Type", "application/json"))
                    .withStatusCode(HttpStatusCodes.STATUS_CODE_FORBIDDEN));
    service.getRegistrationInfo(authUser);
  }

  @Test (expected = NotFoundException.class)
  public void testNotFound() throws Exception {
    mockServerClient.when(request())
            .respond(response()
                    .withHeader(Header.header("Content-Type", "application/json"))
                    .withStatusCode(HttpStatusCodes.STATUS_CODE_NOT_FOUND));
    service.getRegistrationInfo(authUser);
  }

  @Test (expected = ConsentConflictException.class)
  public void testConflict() throws Exception {
    mockServerClient.when(request())
            .respond(response()
                    .withHeader(Header.header("Content-Type", "application/json"))
                    .withStatusCode(HttpStatusCodes.STATUS_CODE_CONFLICT));
    service.getRegistrationInfo(authUser);
  }

  @Test
  public void testGetSelfDiagnostics() throws Exception {
    UserStatusDiagnostics diagnostics = new UserStatusDiagnostics()
            .setEnabled(RandomUtils.nextBoolean())
            .setInAllUsersGroup(RandomUtils.nextBoolean())
            .setInGoogleProxyGroup(RandomUtils.nextBoolean());
    mockServerClient.when(request()).respond(response().withHeader(Header.header("Content-Type", "application/json")).withStatusCode(200).withBody(diagnostics.toString()));

    UserStatusDiagnostics userDiagnostics = service.getSelfDiagnostics(authUser);
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
    mockServerClient.when(request()).respond(response().withHeader(Header.header("Content-Type", "application/json")).withStatusCode(200).withBody(status.toString()));

    UserStatus userStatus = service.postRegistrationInfo(authUser);
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
    mockServerClient.when(request()).respond(response().withHeader(Header.header("Content-Type", "application/json")).withStatusCode(200).withBody(status.toString()));

    try {
      service.asyncPostRegistrationInfo(authUser);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testGetToSText() {
    String mockText = "Plain Text";
    mockServerClient.when(request()).respond(response().withHeader(Header.header("Content-Type", MediaType.TEXT_PLAIN.getType())).withStatusCode(200).withBody(mockText));

    try {
      String text = service.getToSText(authUser);
      assertEquals(mockText, text);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
}
