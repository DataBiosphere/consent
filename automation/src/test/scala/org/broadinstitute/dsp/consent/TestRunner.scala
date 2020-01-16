package org.broadinstitute.dsp.consent

import io.gatling.core.Predef.{Simulation, atOnceUsers, _}
import io.gatling.core.structure.ScenarioBuilder


trait TestRunner extends Simulation {

  def runScenarios(scenarios: List[ScenarioBuilder],
                   assertions: List[Assertion] = List(global.failedRequests.count.is(0))): SetUp = {
    setUp(
      scenarios.map { scn =>
        scn
          .pause(TestConfig.defaultPause)
          .inject(atOnceUsers(TestConfig.defaultUsers))
      }
    )
      .protocols(TestConfig.defaultHttpProtocol)
      .assertions(assertions)
  }

}
