package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.WithMockServer;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.MockServerContainer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    List<ResourceType> mockResponseList = new ArrayList<>();
    ResourceType resourceType = new ResourceType();
    resourceType.setName(RandomStringUtils.random(10));
    resourceType.setReuseIds(false);
    mockResponseList.add(resourceType);
    Gson gson = new Gson();
    mockServerClient.when(request()).respond(response().withStatusCode(200).withBody(gson.toJson(mockResponseList)));

    List<ResourceType> resourceTypeList = service.getResourceTypes(authUser);
    assertFalse(resourceTypeList.isEmpty());
    assertEquals(mockResponseList.size(), resourceTypeList.size());
  }
}
