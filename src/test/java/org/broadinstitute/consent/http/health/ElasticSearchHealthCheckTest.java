package org.broadinstitute.consent.http.health;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import java.util.Collections;
import org.broadinstitute.consent.http.WithMockServer;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.MockServerContainer;

@ExtendWith(MockitoExtension.class)
class ElasticSearchHealthCheckTest implements WithMockServer {

  private ElasticSearchHealthCheck healthCheck;
  private ElasticSearchConfiguration config;
  private MockServerClient mockServerClient;

  private static final MockServerContainer container = new MockServerContainer(IMAGE);

  @BeforeAll
  static void setUp() {
    container.start();
  }

  @AfterAll
  static void tearDown() {
    container.stop();
  }

  @BeforeEach
  void init() {

    config = new ElasticSearchConfiguration();
    config.setServers(Collections.singletonList("localhost"));
    config.setPort(container.getServerPort());

    mockServerClient = new MockServerClient(container.getHost(), container.getServerPort());
    mockServerClient.reset();
  }

  private void initHealthCheck(String status, Integer statusCode) {
    try {
      String stringResponse = "{ \"status\": \"" + status + "\" }";
      mockServerClient.when(request()).respond(response()
          .withStatusCode(statusCode)
          .withBody(stringResponse));

      healthCheck = new ElasticSearchHealthCheck(config);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testCheckSuccessGreen() throws Exception {
    initHealthCheck("green", HttpStatusCodes.STATUS_CODE_OK);

    HealthCheck.Result result = healthCheck.check();
    assertTrue(result.isHealthy());
  }

  @Test
  void testCheckSuccessYellow() throws Exception {
    initHealthCheck("yellow", HttpStatusCodes.STATUS_CODE_OK);

    HealthCheck.Result result = healthCheck.check();
    assertTrue(result.isHealthy());
  }

  @Test
  void testCheckFailureRed() throws Exception {
    initHealthCheck("red", HttpStatusCodes.STATUS_CODE_OK);

    HealthCheck.Result result = healthCheck.check();
    assertFalse(result.isHealthy());
  }

  @Test
  void testCheckServerFailure() throws Exception {
    initHealthCheck("green", HttpStatusCodes.STATUS_CODE_SERVER_ERROR);

    HealthCheck.Result result = healthCheck.check();
    assertFalse(result.isHealthy());
  }
}
