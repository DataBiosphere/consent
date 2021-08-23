package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.WithMockServer;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.SamSelfDiagnostics;
import org.broadinstitute.consent.http.models.sam.SamUserInfo;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.testcontainers.containers.MockServerContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
    SamUserInfo userInfo = new SamUserInfo()
            .setUserEmail("test@test.org")
            .setUserSubjectId(RandomStringUtils.random(10, false, false))
            .setEnabled(RandomUtils.nextBoolean());
    mockServerClient.when(request()).respond(response().withHeader(Header.header("Content-Type", "application/json")).withStatusCode(200).withBody(userInfo.toString()));

    SamUserInfo authUserUserInfo = service.getRegistrationInfo(authUser);
    assertNotNull(authUserUserInfo);
    assertEquals(userInfo.getUserEmail(), authUserUserInfo.getUserEmail());
    assertEquals(userInfo.getEnabled(), authUserUserInfo.getEnabled());
    assertEquals(userInfo.getUserSubjectId(), authUserUserInfo.getUserSubjectId());
  }

  @Test
  public void testGetSelfDiagnostics() throws Exception {
    SamSelfDiagnostics diagnostics = new SamSelfDiagnostics()
            .setEnabled(RandomUtils.nextBoolean())
            .setInAllUsersGroup(RandomUtils.nextBoolean())
            .setInGoogleProxyGroup(RandomUtils.nextBoolean());
    mockServerClient.when(request()).respond(response().withHeader(Header.header("Content-Type", "application/json")).withStatusCode(200).withBody(diagnostics.toString()));

    SamSelfDiagnostics userDiagnostics = service.getSelfDiagnostics(authUser);
    assertNotNull(userDiagnostics);
    assertEquals(diagnostics.getEnabled(), userDiagnostics.getEnabled());
    assertEquals(diagnostics.getInAllUsersGroup(), userDiagnostics.getInAllUsersGroup());
    assertEquals(diagnostics.getInGoogleProxyGroup(), userDiagnostics.getInGoogleProxyGroup());
  }

  @Test
  public void testPostRegistrationInfo() throws Exception {

  }
}
