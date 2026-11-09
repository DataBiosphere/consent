package org.broadinstitute.consent.http.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.List;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.RequestFailedException;
import org.broadinstitute.consent.http.WireMockTestHelper;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil.SimpleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

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
    for (int i = 3; i < 10; i++) {
      assertDoesNotThrow(() -> clientUtil.getCachedResponse(new HttpGet(statusUrl)));
    }
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
    for (int i = 0; i < count; i++) {
      assertDoesNotThrow(() -> clientUtil.getCachedResponse(new HttpGet(statusUrl)));
    }
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
        RequestFailedException.class, () -> clientUtil.getHttpResponse(new HttpGet(statusUrl)));
  }

  /**
   * A downstream server can answer 401 without an HTTP reason phrase. Tomcat omits the phrase by
   * default, and an ingress that forwards the status line unchanged delivers it that way. The
   * status message is then null. A null must not reach the NotAuthorizedException challenge
   * parameter, because that constructor answers with a NullPointerException, which callers that
   * catch NotAuthorizedException cannot handle.
   */
  @Test
  void testHandleHttpRequestUnauthorizedWithoutReasonPhrase() throws Exception {
    HttpRequest request = requestReturning(null);

    assertThrows(NotAuthorizedException.class, () -> clientUtil.handleHttpRequest(request));
  }

  /** A 401 that does carry a reason phrase keeps that phrase in the exception message. */
  @Test
  void testHandleHttpRequestUnauthorizedKeepsReasonPhrase() throws Exception {
    HttpRequest request = requestReturning("Token expired");

    NotAuthorizedException e =
        assertThrows(NotAuthorizedException.class, () -> clientUtil.handleHttpRequest(request));
    assertTrue(e.getMessage().contains("Token expired"), e.getMessage());
  }

  /**
   * The timer name has no query string. A caller can send any query value, so a name with the query
   * string adds a new timer for each value, and the registry grows without limit.
   */
  @Test
  void testHandleHttpRequestQueryValuesShareOneTimer() throws Exception {
    wireMockServer.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)));
    String path = "/timer-" + randomAlphabetic(10);
    String firstQuery = randomAlphabetic(10);
    String secondQuery = randomAlphabetic(10);

    clientUtil.handleHttpRequest(
        clientUtil.buildUnAuthedGetRequest(
            new GenericUrl(mockServerBaseUrl() + path + "?code=" + firstQuery)));
    clientUtil.handleHttpRequest(
        clientUtil.buildUnAuthedGetRequest(
            new GenericUrl(mockServerBaseUrl() + path + "?state=" + secondQuery)));

    String timerName =
        "org.broadinstitute.consent.http.util.HttpClientUtil-GET-" + mockServerBaseUrl() + path;
    assertEquals(2, timerCount(timerName));
    assertFalse(
        SharedMetricRegistries.getDefault().getTimers().keySet().stream()
            .anyMatch(name -> name.contains(firstQuery) || name.contains(secondQuery)));
  }

  /** A caller can name the timer with a label, to keep an ID in the URL path out of the name. */
  @Test
  void testHandleHttpRequestWithTimerLabel() throws Exception {
    wireMockServer.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)));
    String id = randomAlphabetic(10);
    String label = "timer-label-" + randomAlphabetic(10);

    clientUtil.handleHttpRequest(
        clientUtil.buildUnAuthedGetRequest(new GenericUrl(mockServerBaseUrl() + "/items/" + id)),
        label);

    String timerName = "org.broadinstitute.consent.http.util.HttpClientUtil-GET-" + label;
    assertEquals(1, timerCount(timerName));
    assertFalse(
        SharedMetricRegistries.getDefault().getTimers().keySet().stream()
            .anyMatch(name -> name.contains(id)));
  }

  @Test
  void testTimerLabelKeepsSchemeHostPortAndPath() {
    assertEquals(
        "https://example.org:8443/api/token",
        HttpClientUtil.timerLabel(
            new GenericUrl("https://user:secret@example.org:8443/api/token?code=abc#frag")));
    assertEquals(
        "https://example.org/api/token",
        HttpClientUtil.timerLabel(new GenericUrl("https://example.org/api/token?code=abc")));
  }

  /**
   * executeRequest uses the same timer name as handleHttpRequest. The OIDC token endpoint sends
   * client query values through executeRequest, so this is the path that a scanner reaches.
   */
  @Test
  void testExecuteRequestQueryValuesShareOneTimer() throws Exception {
    wireMockServer.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(400)));
    String path = "/token-" + randomAlphabetic(10);
    String firstQuery = randomAlphabetic(10);
    String secondQuery = randomAlphabetic(10);

    clientUtil.executeRequest(
        clientUtil.buildUnAuthedGetRequest(
            new GenericUrl(mockServerBaseUrl() + path + "?code=" + firstQuery)));
    clientUtil.executeRequest(
        clientUtil.buildUnAuthedGetRequest(
            new GenericUrl(mockServerBaseUrl() + path + "?code=" + secondQuery)));

    String timerName =
        "org.broadinstitute.consent.http.util.HttpClientUtil-GET-" + mockServerBaseUrl() + path;
    assertEquals(2, timerCount(timerName));
    assertFalse(
        SharedMetricRegistries.getDefault().getTimers().keySet().stream()
            .anyMatch(name -> name.contains(firstQuery) || name.contains(secondQuery)));
  }

  /** executeRequest gives the caller every status, so the caller can read an error body. */
  @Test
  void testExecuteRequestReturnsUnauthorizedResponse() throws Exception {
    HttpRequest request = requestReturning("Unauthorized");

    HttpResponse response = clientUtil.executeRequest(request);
    assertEquals(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, response.getStatusCode());
  }

  /** A path segment that holds an ID becomes "{id}" in the default label. */
  @Test
  void testTimerLabelReplacesIdSegments() {
    assertEquals(
        "https://example.org/api/users/v1/{id}",
        HttpClientUtil.timerLabel(
            new GenericUrl("https://example.org/api/users/v1/someone%40example.com")));
    assertEquals(
        "https://example.org/api/dar/{id}/votes",
        HttpClientUtil.timerLabel(new GenericUrl("https://example.org/api/dar/12345/votes")));
    assertEquals(
        "https://example.org/api/study/{id}",
        HttpClientUtil.timerLabel(
            new GenericUrl("https://example.org/api/study/0b6a2b4e-3f1c-4d2a-9e8f-1a2b3c4d5e6f")));
    assertEquals(
        "https://example.org/register/user/v2/self/info",
        HttpClientUtil.timerLabel(
            new GenericUrl("https://example.org/register/user/v2/self/info")));
    assertEquals(
        "https://example.org", HttpClientUtil.timerLabel(new GenericUrl("https://example.org")));
  }

  /** The error log line has neither the email in the path nor the query values. */
  @Test
  void testHandleHttpRequestErrorLogHasNoEmailOrQuery() throws Exception {
    String localPart = randomAlphabetic(12);
    String query = randomAlphabetic(12);
    HttpRequest request =
        requestReturning(
            HttpStatusCodes.STATUS_CODE_NOT_FOUND,
            "Not Found",
            "https://example.org/api/users/v1/" + localPart + "%40example.com?state=" + query);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    Logger logger = (Logger) LoggerFactory.getLogger(HttpClientUtil.class);
    appender.start();
    logger.addAppender(appender);
    try {
      assertThrows(NotFoundException.class, () -> clientUtil.handleHttpRequest(request));
    } finally {
      logger.detachAppender(appender);
    }

    List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    assertTrue(
        messages.stream().anyMatch(m -> m.contains("https://example.org/api/users/v1/{id}")),
        messages.toString());
    assertFalse(messages.stream().anyMatch(m -> m.contains(localPart) || m.contains(query)));
  }

  private long timerCount(String timerName) {
    Timer timer = SharedMetricRegistries.getDefault().getTimers().get(timerName);
    assertNotNull(
        timer,
        "No timer named " + timerName + " in " + SharedMetricRegistries.getDefault().getNames());
    return timer.getCount();
  }

  private HttpRequest requestReturning(String reasonPhrase) throws IOException {
    return requestReturning(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, reasonPhrase, statusUrl);
  }

  private HttpRequest requestReturning(int statusCode, String reasonPhrase, String url)
      throws IOException {
    MockLowLevelHttpResponse response =
        new MockLowLevelHttpResponse().setStatusCode(statusCode).setReasonPhrase(reasonPhrase);
    HttpTransport transport =
        new MockHttpTransport.Builder().setLowLevelHttpResponse(response).build();
    return transport.createRequestFactory().buildGetRequest(new GenericUrl(url));
  }
}
