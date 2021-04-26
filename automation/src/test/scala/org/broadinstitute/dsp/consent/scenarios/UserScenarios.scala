package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef._
import org.broadinstitute.dsp.consent.{TestConfig, TestRunner}
import org.broadinstitute.dsp.consent.requests.Requests
import io.netty.handler.codec.http.HttpResponseStatus._
import org.broadinstitute.dsp.consent.scenarios.GroupedScenarios._

class UserScenarios extends Simulation with TestRunner {
    runScenarios(
        List(
            GroupedScenario("User Login") { exec(Requests.User.me(OK.code, TestConfig.researcherHeader)) }
        )
    )
}