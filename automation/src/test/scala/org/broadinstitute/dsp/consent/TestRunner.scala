package org.broadinstitute.dsp.consent

import io.gatling.core.Predef.{Simulation, atOnceUsers, _}
import io.gatling.core.structure.{ScenarioBuilder, PopulationBuilder}


trait TestRunner extends Simulation {

  def runPopulations(populations: List[PopulationBuilder],
                     assertions: List[Assertion] = List(global.failedRequests.count.is(0))): SetUp = {
    setUp(
      populations
    )
      .protocols(TestConfig.defaultHttpProtocol)
      .assertions(assertions)
  }

  def runScenarios(scenarios: List[ScenarioBuilder],
                   assertions: List[Assertion] = List(global.failedRequests.count.is(0))): SetUp = {
    setUp(
      scenarios.map { scn =>
        defaultPopulation(scn)
      }
    )
      .protocols(TestConfig.defaultHttpProtocol)
      .assertions(assertions)
  }

  def defaultPopulation(scenario: ScenarioBuilder): PopulationBuilder = {
    scenario
      .pause(TestConfig.defaultPause)
      .inject(atOnceUsers(TestConfig.defaultUsers))
  }
}
