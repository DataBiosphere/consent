package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef._
import org.broadinstitute.dsp.consent.{TestConfig, TestRunner}
import org.broadinstitute.dsp.consent.chains.AdminChains

class AdminScenarios extends Simulation with TestRunner {
    runScenarios(
        List(
            scenario("Admin Login")
            .exec(
                AdminChains.loginToConsole(TestConfig.adminHeader)
            )
        )
    )
}