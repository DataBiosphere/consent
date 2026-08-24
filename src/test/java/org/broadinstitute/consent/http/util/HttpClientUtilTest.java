package org.broadinstitute.consent.http.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.api.client.http.HttpStatusCodes;
import java.util.stream.IntStream;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.RequestFailedException;
import org.broadinstitute.consent.http.WireMockTestHelper;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil.SimpleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HttpClientUtilTest extends WireMockTestHelper {

  private String statusUrl;
  private HttpClientUtil clientUtil;

  @BeforeEach
  void init() {
    statusUrl = mockServerBaseUrl() + "/";
    ServicesConfiguration configuration = new ServicesConfiguration();
    configuration.setTimeoutSeconds(1);
    clientUtil = new HttpClientUtil(configuration);
  }

  /** Test that the cache works normally */
  @Test
  void testGetCachedResponse_case1() {
    wireMockServer.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)));
    IntStream.range(3, 10)
        .forEach(
            i -> {
              try {
                clientUtil.getCachedResponse(new HttpGet(statusUrl));
              } catch (Exception e) {
                fail(e.getMessage());
              }
            });
    wireMockServer.verify(exactly(1), anyRequestedFor(anyUrl()));
  }

  /** Test that when the cache is expired, all calls are made to external servers */
  @Test
  void testGetCachedResponse_case2() {
    ServicesConfiguration configuration = new ServicesConfiguration();
    configuration.setTimeoutSeconds(1);
    // Setting the cache to 0 effectively means no caching
    configuration.setCacheExpireMinutes(0);
    clientUtil = new HttpClientUtil(configuration);
    wireMockServer.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)));

    int count = randomInt(5, 10);
    IntStream.range(0, count)
        .forEach(
            i -> {
              try {
                clientUtil.getCachedResponse(new HttpGet(statusUrl));
              } catch (Exception e) {
                fail(e.getMessage());
              }
            });
    wireMockServer.verify(exactly(count), anyRequestedFor(anyUrl()));
  }

  @Test
  void testGetHttpResponseUnderTimeout() throws Exception {
    wireMockServer.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)));
    SimpleResponse response = clientUtil.getHttpResponse(new HttpGet(statusUrl));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.code());
  }

  /** Test that repeated requests do not leak timeout threads. */
  @Test
  void testGetHttpResponseDoesNotLeakThreads() throws Exception {
    wireMockServer.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)));
    long before = countTimeoutThreads();
    for (int i = 0; i < 25; i++) {
      clientUtil.getHttpResponse(new HttpGet(statusUrl));
    }
    long growth = countTimeoutThreads() - before;
    // The shared pool holds at most the default pool size of 10 threads.
    assertTrue(growth <= 10, "Timeout thread count grew by " + growth);
  }

  private long countTimeoutThreads() {
    return Thread.getAllStackTraces().keySet().stream()
        .filter(t -> t.getName().startsWith("http-client-timeout"))
        .count();
  }

  @Test
  void testGetHttpResponseOverTimeout() {
    wireMockServer.stubFor(
        any(anyUrl()).willReturn(aResponse().withStatus(200).withFixedDelay(3000)));
    assertThrows(
        RequestFailedException.class,
        () -> {
          clientUtil.getHttpResponse(new HttpGet(statusUrl));
        });
  }
}
