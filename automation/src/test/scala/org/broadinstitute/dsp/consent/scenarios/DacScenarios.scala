package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef.{Simulation, scenario}
import org.broadinstitute.dsp.consent.{TestConfig, TestRunner}
import org.broadinstitute.dsp.consent.requests.Requests
import io.netty.handler.codec.http.HttpResponseStatus._

class DacScenarios  extends Simulation with TestRunner {

  runScenarios(
    List(
      scenario("Dac List Scenario").exec(Requests.Dac.list(OK.code, TestConfig.adminHeader))
    )
  )

}
