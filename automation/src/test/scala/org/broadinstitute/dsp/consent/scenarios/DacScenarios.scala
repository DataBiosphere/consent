package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef.{Simulation, scenario, exec}
import org.broadinstitute.dsp.consent.{TestConfig, TestRunner}
import org.broadinstitute.dsp.consent.requests.Requests
import io.netty.handler.codec.http.HttpResponseStatus._
import org.broadinstitute.dsp.consent.scenarios.GroupedScenarios._

class DacScenarios  extends Simulation with TestRunner {

  runScenarios(
    List(
      GroupedScenario("Dac List Scenario") { exec(Requests.Dac.list(OK.code, TestConfig.adminHeader)) }
    )
  )

}
