package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef._
import org.broadinstitute.dsp.consent.{TestConfig, TestRunner}
import scala.concurrent.duration._
import org.broadinstitute.dsp.consent.chains.AdminChains

class AdminScenarios extends Simulation with TestRunner {
    runScenarios(
        List(
            scenario("Admins Voting")
                .exec(
                    AdminChains.loginToConsole(TestConfig.adminHeader)
                )
                .pause(1)
                .exec(
                    AdminChains.manageAccess(TestConfig.adminHeader)
                )
                .pause(1)
                .exec(
                    AdminChains.createElections(TestConfig.adminHeader)
                )
        )
    )
}