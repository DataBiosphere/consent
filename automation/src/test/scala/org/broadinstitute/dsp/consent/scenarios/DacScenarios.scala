package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef.{Simulation, scenario}
import org.broadinstitute.dsp.consent.{TestConfig, TestRunner}
import org.broadinstitute.dsp.consent.requests.Requests

class DacScenarios  extends Simulation with TestRunner {

  runScenarios(
    List(
      scenario("List DACs Scenario").exec(Requests.Dac.list(200, TestConfig.adminHeader))
    )
  )

}
