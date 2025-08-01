package org.broadinstitute.consent.http.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.google.api.client.http.HttpStatusCodes;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.RequestFailedException;
import org.broadinstitute.consent.http.MockServerTestHelper;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil.SimpleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.model.Delay;
import org.mockserver.verify.VerificationTimes;

@ExtendWith(MockitoExtension.class)
class HttpClientUtilTest extends MockServerTestHelper {

  private final String statusUrl = String.format("http://%s:%s/", CONTAINER.getHost(),
      CONTAINER.getServerPort());
  private HttpClientUtil clientUtil;

  @BeforeEach
  void init() {
    ServicesConfiguration configuration = new ServicesConfiguration();
    configuration.setTimeoutSeconds(1);
    clientUtil = new HttpClientUtil(configuration);
  }

  /**
   * Test that the cache works normally
   */
  @Test
  void testGetCachedResponse_case1() {
    mockServerClient.when(request())
        .respond(response()
            .withStatusCode(200));
    IntStream.range(3, 10).forEach(i -> {
      try {
        clientUtil.getCachedResponse(new HttpGet(statusUrl));
      } catch (Exception e) {
        fail(e.getMessage());
      }
    });
    mockServerClient.verify(request(), VerificationTimes.exactly(1));
  }

  /**
   * Test that when the cache is expired, all calls are made to external servers
   */
  @Test
  void testGetCachedResponse_case2() {
    ServicesConfiguration configuration = new ServicesConfiguration();
    configuration.setTimeoutSeconds(1);
    // Setting the cache to 0 effectively means no caching
    configuration.setCacheExpireMinutes(0);
    clientUtil = new HttpClientUtil(configuration);
    mockServerClient.when(request())
        .respond(response()
            .withStatusCode(200));

    int count = RandomUtils.nextInt(5, 10);
    IntStream.range(0, count).forEach(i -> {
      try {
        clientUtil.getCachedResponse(new HttpGet(statusUrl));
      } catch (Exception e) {
        fail(e.getMessage());
      }
    });
    mockServerClient.verify(request(), VerificationTimes.exactly(count));
  }

  @Test
  void testGetHttpResponseUnderTimeout() throws Exception {
    mockServerClient.when(request())
        .respond(response()
            .withStatusCode(200));
    SimpleResponse response = clientUtil.getHttpResponse(new HttpGet(statusUrl));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.code());
  }

  @Test
  void testGetHttpResponseOverTimeout() {
    mockServerClient.when(request())
        .respond(response()
            .withStatusCode(200)
            .withDelay(Delay.delay(TimeUnit.SECONDS, 3)));
    assertThrows(RequestFailedException.class, () -> {
      clientUtil.getHttpResponse(new HttpGet(statusUrl));
    });
  }

}
