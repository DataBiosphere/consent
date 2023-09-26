package org.broadinstitute.consent.performance;


import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.util.List;
import java.util.stream.Stream;
import org.broadinstitute.consent.performance.scenarios.Status;
import org.broadinstitute.consent.performance.scenarios.Status.ScenarioTest;


/**
 * This is the main entry point for smoke/performance tests Headers common to all requests are
 * defined here in the protocol builder.
 */
public class TestRunner extends Simulation {

  private final TestConfig config = new TestConfig();

  private final HttpProtocolBuilder protocol = http
      .baseUrl(config.getBaseUrl())
      .header("X-App-ID", "DUOS")
      .doNotTrackHeader("1")
      .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0");

  {
    List<ScenarioTest> scenarioTests = new Status().scenarioTests;
    List<ScenarioBuilder> scenarios = scenarioTests
        .stream()
        .map(ScenarioTest::scenarioBuilder)
        .toList();
    List<String> scenarioNames = scenarioTests.stream().map(ScenarioTest::name).toList();
    setUp(
        Stream.of(scenarios)
            .flatMap(List::stream)
            .map(scn -> scn.injectOpen(atOnceUsers(1)))
            .toList())
        .assertions(
            scenarioNames.stream()
                .map(name -> details(name).successfulRequests().percent().is(100.0)).toList())
        .protocols(protocol);
  }
}
