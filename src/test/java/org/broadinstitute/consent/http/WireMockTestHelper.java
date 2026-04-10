package org.broadinstitute.consent.http;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

public class WireMockTestHelper extends AbstractTestHelper
    implements TestExecutionListener, WithWireMock {

  public static final WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());

  @Override
  public void testPlanExecutionStarted(@NonNull TestPlan testPlan) {
    try {
      if (enableTestContainers() && !wireMockServer.isRunning()) {
        wireMockServer.start();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void testPlanExecutionFinished(@NonNull TestPlan testPlan) {
    stop(wireMockServer);
  }

  @BeforeEach
  void beforeEach() {
    wireMockServer.resetAll();
  }

  public static String mockServerBaseUrl() {
    return wireMockServer.baseUrl();
  }

  public static String mockServerHost() {
    return "localhost";
  }

  public static int mockServerPort() {
    return wireMockServer.port();
  }
}
