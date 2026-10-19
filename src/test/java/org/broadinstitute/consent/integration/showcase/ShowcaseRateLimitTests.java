package org.broadinstitute.consent.integration.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.broadinstitute.consent.http.configurations.RateLimitConfiguration;
import org.broadinstitute.consent.integration.ContainerTests;
import org.junit.jupiter.api.Test;

/**
 * Runs the public limit through the real Jetty stack, so Jersey must inject the servlet request
 * that gives the socket peer. The test client connects from loopback, which is a trusted proxy by
 * default, so each X-Real-IP address gets its own bucket. The authenticated limiter does not run on
 * these paths. Nothing is mapped under {@code showcase/} yet, so allowed requests get 404.
 *
 * <p>This test uses the JDK client: the Apache client behind {@code getClient()} retries a 429
 * after its {@code Retry-After} delay, so the test would never see the 429.
 */
class ShowcaseRateLimitTests extends ContainerTests {

  private static final String PATH = "/showcase/rate-limit-probe";
  private static final HttpClient HTTP = HttpClient.newHttpClient();

  @Test
  void testRequestsOverTheLimitGet429PerRealIp() throws Exception {
    RateLimitConfiguration config = APPLICATION.getConfiguration().getRateLimitConfiguration();
    int capacityPerPod =
        (int)
            Math.ceil(config.getShowcase().getRequestsPerMinute() / (double) config.getPodCount());

    for (int i = 0; i < capacityPerPod; i++) {
      assertEquals(404, get("192.0.2.1").statusCode());
    }
    // The bucket refills continuously, so one more request can pass while the loop runs.
    HttpResponse<Void> limited = get("192.0.2.1");
    if (limited.statusCode() == 404) {
      limited = get("192.0.2.1");
    }
    assertEquals(429, limited.statusCode());
    assertTrue(limited.headers().firstValue("Retry-After").isPresent());

    assertEquals(404, get("192.0.2.2").statusCode());
  }

  private HttpResponse<Void> get(String realIp) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(serviceUrl(PATH)))
            .header("X-Real-IP", realIp)
            .GET()
            .build();
    return HTTP.send(request, BodyHandlers.discarding());
  }
}
