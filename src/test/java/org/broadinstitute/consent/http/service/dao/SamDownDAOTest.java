package org.broadinstitute.consent.http.service.dao;

import static org.broadinstitute.consent.http.WithMockServer.IMAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpError.error;
import static org.mockserver.model.HttpRequest.request;

import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.EmailResponse;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.TosResponse;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.MockServerContainer;

@ExtendWith(MockitoExtension.class)
public class SamDownDAOTest {

  private SamDAO samDAO;

  private final AuthUser authUser = new AuthUser()
      .setEmail(RandomStringUtils.randomAlphabetic(10));

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
    @SuppressWarnings("resource")
    MockServerClient mockServerClient = new MockServerClient(container.getHost(),
        container.getServerPort());
    mockServerClient.reset();
    ServicesConfiguration config = new ServicesConfiguration();
    config.setTimeoutSeconds(1);
    config.setSamUrl("http://" + container.getHost() + ":" + container.getServerPort() + "/");
    samDAO = new SamDAO(new HttpClientUtil(config), config);
    mockServerClient
        .when(request())
        .error(error().withDropConnection(true));
  }

  @Test
  void testGetResourceTypes() throws Exception {
    List<ResourceType> types = samDAO.getResourceTypes(authUser);
    assertTrue(types.isEmpty());
  }

  @Test
  void testGetRegistrationInfo() throws Exception {
    UserStatusInfo info = samDAO.getRegistrationInfo(authUser);
    UserStatusInfo def = samDAO.getDefaultUserStatusInfo(authUser);
    assertNotNull(info);
    assertThat(info, samePropertyValuesAs(def));
  }

  @Test
  void testGetSelfDiagnostics() throws Exception {
    UserStatusDiagnostics userStatus = samDAO.getSelfDiagnostics(authUser);
    UserStatusDiagnostics def = samDAO.getDefaultUserStatusDiagnostics();
    assertNotNull(userStatus);
    assertThat(userStatus, samePropertyValuesAs(def));
  }

  @Test
  void testPostRegistrationInfo() throws Exception {
    UserStatus userStatus = samDAO.postRegistrationInfo(authUser);
    UserStatus def = samDAO.getDefaultUserStatus(authUser);
    assertNotNull(userStatus);
    assertThat(userStatus.getEnabled(), samePropertyValuesAs(def.getEnabled()));
    assertThat(userStatus.getUserInfo(), samePropertyValuesAs(def.getUserInfo()));
  }

  @Test
  void testGetTosResponse() throws Exception {
    TosResponse tos = samDAO.getTosResponse(authUser);
    TosResponse def = samDAO.getDefaultTosResponse();
    assertNotNull(tos);
    assertThat(tos, samePropertyValuesAs(def));
  }

  @Test
  void testGetToSText() throws Exception {
    String text = samDAO.getToSText();
    assertEquals(samDAO.getDefaultToSText(), text);
  }

  @Test
  void testAcceptTosStatus() throws Exception {
    int acceptStatus = samDAO.acceptTosStatus(authUser);
    assertEquals(samDAO.getDefaultTosStatusCode(), acceptStatus);
  }

  @Test
  void testRejectTosStatus() throws Exception {
    int rejectStatus = samDAO.rejectTosStatus(authUser);
    assertEquals(samDAO.getDefaultTosStatusCode(), rejectStatus);
  }

  @Test
  void testGetV1UserByEmail() throws Exception {
    EmailResponse userByEmail = samDAO.getV1UserByEmail(authUser, authUser.getEmail());
    EmailResponse def = samDAO.getDefaultEmailResponse(authUser);
    assertNotNull(userByEmail);
    assertThat(userByEmail, samePropertyValuesAs(def));
  }

}
