package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef._
import org.broadinstitute.dsp.consent.{TestConfig, TestRunner}
import org.broadinstitute.dsp.consent.requests.Requests

class UserScenarios extends Simulation with TestRunner {
    runScenarios(
        List(
            scenario("User Login")
                .exec(Requests.User.me(200, TestConfig.researcherHeader))
        )
    )
}