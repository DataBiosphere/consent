package org.broadinstitute.consent.performance.scenarios;

import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.status;

import com.google.api.client.http.HttpStatusCodes;
import io.gatling.javaapi.core.ScenarioBuilder;
import java.util.List;
import org.broadinstitute.consent.performance.Endpoints;

public class Status implements Endpoints {

  public record ScenarioTest(String name, ScenarioBuilder scenarioBuilder) {}

  public List<ScenarioTest> scenarioTests = List.of(
      new ScenarioTest("Liveness",
          scenario("Liveness").exec(liveness().check(status().is(HttpStatusCodes.STATUS_CODE_OK)))
              .pause(1, 5)),
      new ScenarioTest("Status",
          scenario("Status").exec(systemStatus().check(status().is(HttpStatusCodes.STATUS_CODE_OK)))
              .pause(1, 5)),
      new ScenarioTest("Version", scenario("Version").exec(
          systemVersion().check(status().is(HttpStatusCodes.STATUS_CODE_OK))).pause(1, 5))
  );
}
