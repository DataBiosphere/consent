package org.broadinstitute.consent.http.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.google.api.client.http.HttpStatusCodes;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.RequestFailedException;
import org.broadinstitute.consent.http.WithMockServer;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil.SimpleResponse;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Delay;
import org.testcontainers.containers.MockServerContainer;

public class HttpClientUtilTest implements WithMockServer {

  private HttpClientUtil clientUtil;

  private MockServerClient mockServerClient;

  private static final MockServerContainer container = new MockServerContainer(IMAGE);

  private final String statusUrl = String.format("http://%s:%s/", container.getHost(), container.getServerPort());

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
    ServicesConfiguration configuration = new ServicesConfiguration();
    configuration.setTimeoutSeconds(1);
    clientUtil = new HttpClientUtil(configuration);
  }

  @Test
  public void testGetHttpResponseUnderTimeout() throws Exception {
    mockServerClient.when(request())
      .respond(response()
      .withStatusCode(200));
    SimpleResponse response = clientUtil.getHttpResponse(new HttpGet(statusUrl));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.code());
  }

  @Test
  public void testGetHttpResponseOverTimeout() {
    mockServerClient.when(request())
      .respond(response()
      .withStatusCode(200)
      .withDelay(Delay.delay(TimeUnit.SECONDS, 3)));
    try {
      clientUtil.getHttpResponse(new HttpGet(statusUrl));
      fail("The above request should have thrown an exception");
    } catch (Exception e) {
      assertTrue(e instanceof RequestFailedException);
    }
  }

}
