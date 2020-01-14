package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef._
import org.broadinstitute.dsp.consent.TestRunner
import org.broadinstitute.dsp.consent.requests.Requests

class StatusScenarios extends Simulation with TestRunner {

  runScenarios(
    List(
      scenario("Consent Status Scenario").exec(Requests.statusRequest)
    )
  )

}
