package org.broadinstitute.consent.integration.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import org.broadinstitute.consent.http.configurations.RateLimitConfiguration;
import org.broadinstitute.consent.http.filters.SignedClientIpVerifierTestSupport;
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

  private static int capacityPerPod() {
    RateLimitConfiguration config = APPLICATION.getConfiguration().getRateLimitConfiguration();
    return (int)
        Math.ceil(config.getShowcase().getRequestsPerMinute() / (double) config.getPodCount());
  }

  @Test
  void testRequestsOverTheLimitGet429PerRealIp() throws Exception {
    int capacityPerPod = capacityPerPod();

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

  /**
   * Requests that the BFF signs share one X-Real-IP (its egress address), but each browser IP gets
   * its own bucket. The headers go out in lowercase, as Node sends them.
   */
  @Test
  void testSignedClientIpsBehindOneRealIpGetTheirOwnBuckets() throws Exception {
    String bffEgress = "192.0.2.10";
    int capacityPerPod = capacityPerPod();

    for (int i = 0; i < capacityPerPod; i++) {
      assertEquals(404, getSigned(bffEgress, "203.0.113.20").statusCode());
    }
    HttpResponse<Void> limited = getSigned(bffEgress, "203.0.113.20");
    if (limited.statusCode() == 404) {
      limited = getSigned(bffEgress, "203.0.113.20");
    }
    assertEquals(429, limited.statusCode());

    assertEquals(404, getSigned(bffEgress, "203.0.113.21").statusCode());
  }

  private HttpResponse<Void> getSigned(String realIp, String clientIp) throws Exception {
    String hmacSecret =
        APPLICATION.getConfiguration().getRateLimitConfiguration().getClientIpSecret();
    long now = Instant.now().getEpochSecond();
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(serviceUrl(PATH)))
            .header("X-Real-IP", realIp)
            .header("x-duos-client-ip", clientIp)
            .header("x-duos-client-ip-timestamp", Long.toString(now))
            .header(
                "x-duos-client-ip-signature",
                SignedClientIpVerifierTestSupport.hmacHex(hmacSecret, clientIp, now))
            .GET()
            .build();
    return HTTP.send(request, BodyHandlers.discarding());
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
