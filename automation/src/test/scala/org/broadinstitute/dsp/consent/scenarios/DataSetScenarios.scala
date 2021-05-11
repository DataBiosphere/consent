package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef.{Simulation, scenario, exec}
import org.broadinstitute.dsp.consent.{TestConfig, TestRunner}
import org.broadinstitute.dsp.consent.requests.Requests
import io.netty.handler.codec.http.HttpResponseStatus._
import org.broadinstitute.dsp.consent.scenarios.GroupedScenarios._

class DataSetScenarios extends Simulation with TestRunner {
    runScenarios(
        List(
            GroupedScenario("DataSet By DAC User Id Scenario") {
                exec(Requests.User.me(OK.code, TestConfig.researcherHeader))
                  .exec(Requests.DataSet.byUserId(OK.code, "${dacUserId}", TestConfig.researcherHeader))
            }
        )
    )
}