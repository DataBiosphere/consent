package org.broadinstitute.consent.http.health;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import java.util.Collections;
import org.broadinstitute.consent.http.WireMockTestHelper;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ElasticSearchHealthCheckTest extends WireMockTestHelper {

  private ElasticSearchHealthCheck healthCheck;
  private ElasticSearchConfiguration config;

  @BeforeEach
  void init() {
    config = new ElasticSearchConfiguration();
    config.setServers(Collections.singletonList(mockServerHost()));
    config.setPort(mockServerPort());
  }

  private void initHealthCheck(String status, Integer statusCode) {
    try {
      String stringResponse = "{ \"status\": \"" + status + "\" }";
      wireMockServer.stubFor(
          any(anyUrl()).willReturn(aResponse().withStatus(statusCode).withBody(stringResponse)));

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
