package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef.{Simulation, scenario}
import org.broadinstitute.dsp.consent.{TestConfig, TestRunner}
import org.broadinstitute.dsp.consent.requests.Requests

class DataSetScenarios extends Simulation with TestRunner {
    runScenarios(
        List(
            scenario("DatSet By DAC User Id Scenario")
                .exec(Requests.User.me(200, TestConfig.researcherHeader))
                
        )
    )
}