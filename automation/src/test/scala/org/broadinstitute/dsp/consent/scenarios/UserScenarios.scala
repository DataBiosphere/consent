package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef._
import org.broadinstitute.dsp.consent.{TestConfig, TestRunner}
import org.broadinstitute.dsp.consent.requests.Requests
import scala.concurrent.duration._
import org.broadinstitute.dsp.consent.chains.{DarChains, DataSetChains, NihChains}

class UserScenarios extends Simulation with TestRunner {
    runScenarios(
        List(
            scenario("User Login Scenario")
                .exec(Requests.User.me(200, TestConfig.researcherHeader)),
            scenario("Researcher DAR with 2 DataSets")
                .exec(
                    Requests.User.me(200, TestConfig.researcherHeader)
                )
                .pause(1 second)
                .exec(
                    DataSetChains.dataSetCatalogPickTwo(TestConfig.researcherHeader)
                )
                .pause(1)
                .exec(
                    DarChains.darApplicationPageLoad(TestConfig.researcherHeader)
                )
                .exec(
                    NihChains.authenticate(TestConfig.researcherHeader)
                )
                .exec(
                    DarChains.finalDarSubmit(TestConfig.researcherHeader)
                )
        )
    )
}