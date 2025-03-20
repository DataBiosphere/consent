package org.broadinstitute.consent.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class MockServerTestHelper extends AbstractTestHelper implements TestExecutionListener,
    WithMockServer {

  public static final MockServerContainer container = new MockServerContainer(IMAGE).waitingFor(
      Wait.forLogMessage(".*started on port:.*", 1));
  public static MockServerClient mockServerClient;

  static void startUp() {
    container.start();
    mockServerClient = new MockServerClient(container.getHost(), container.getServerPort());
  }

  @Override
  public void testPlanExecutionStarted(TestPlan testPlan) {
    try {
      startUp();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeEach
  void beforeEach() {
    mockServerClient.reset();
  }
}
