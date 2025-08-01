package org.broadinstitute.consent.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class MockServerTestHelper extends AbstractTestHelper implements TestExecutionListener,
    WithMockServer {

  public static final MockServerContainer CONTAINER = new MockServerContainer(IMAGE).waitingFor(
      Wait.forLogMessage(".*started on port:.*", 1));
  public static MockServerClient mockServerClient;


  @Override
  public void testPlanExecutionStarted(TestPlan testPlan) {
    try {
      if (enableTestContainers()) {
        CONTAINER.start();
        mockServerClient = new MockServerClient(CONTAINER.getHost(), CONTAINER.getServerPort());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeEach
  void beforeEach() {
    mockServerClient.reset();
  }
}
