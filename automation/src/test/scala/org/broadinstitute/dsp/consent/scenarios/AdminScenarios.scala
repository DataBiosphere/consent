package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef._
import org.broadinstitute.dsp.consent.{TestConfig, TestRunner}
import org.broadinstitute.dsp.consent.chains.AdminChains
import org.broadinstitute.dsp.consent.scenarios.GroupedScenarios._

class AdminScenarios extends Simulation with TestRunner {
    runScenarios(
        List(
            GroupedScenario("Admin Login") {
                exec(
                    AdminChains.loginToConsole(TestConfig.adminHeader)
                )
            }
        )
    )
}