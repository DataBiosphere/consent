package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef.{Simulation, scenario}
import org.broadinstitute.dsp.consent.TestRunner
import org.broadinstitute.dsp.consent.requests.Requests

class DacScenarios  extends Simulation with TestRunner {

  runScenarios(
    List(
      scenario("Status Scenario").exec(Requests.Dac.list)
    )
  )

}
