package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef.{Simulation, scenario}
import org.broadinstitute.dsp.consent.{TestConfig, TestRunner}
import org.broadinstitute.dsp.consent.requests.Requests
import io.netty.handler.codec.http.HttpResponseStatus._

class DataSetScenarios extends Simulation with TestRunner {
    runScenarios(
        List(
            scenario("DataSet By DAC User Id Scenario")
                .exec(Requests.User.me(OK.code, TestConfig.researcherHeader))
                .exec(Requests.DataSet.byUserId(OK.code, "${dacUserId}", TestConfig.researcherHeader))
        )
    )
}