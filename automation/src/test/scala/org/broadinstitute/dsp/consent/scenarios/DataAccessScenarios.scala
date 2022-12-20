package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef._
import org.broadinstitute.dsp.consent.{TestConfig, TestRunner}
import org.broadinstitute.dsp.consent.requests.Requests
import org.broadinstitute.dsp.consent.chains.{DarChains, DataSetChains, AdminChains, MemberChains, ChairChains, AccessReviewChains}
import io.netty.handler.codec.http.HttpResponseStatus._
import org.broadinstitute.dsp.consent.scenarios.GroupedScenarios._

class DataAccessScenarios extends Simulation with TestRunner {
    runPopulations(
        List(
            defaultPopulation(
                GroupedScenario("Researcher DAR with 2 DataSets") {
                    exitBlockOnFail {
                        exec(
                            Requests.User.me(OK.code, TestConfig.researcherHeader)
                        )
                          .pause(TestConfig.defaultPause)
                          .exec(
                              DataSetChains.dataSetCatalogPickTwo(TestConfig.researcherHeader)
                          )
                          .pause(TestConfig.defaultPause)
                          .exec(
                              DarChains.darApplicationPageLoad(TestConfig.researcherHeader)
                          )
                          .exec(
                              DarChains.finalDarSubmit(TestConfig.researcherHeader)
                          )
                    }
                }
            )
            .andThen(
                defaultPopulation(
                    GroupedScenario("Admins Election Creation") {
                        exitBlockOnFail {
                            exec(
                                AdminChains.loginToConsole(TestConfig.adminHeader)
                            )
                              .pause(TestConfig.defaultPause)
                              .exec(
                                  AdminChains.createElections(TestConfig.adminHeader)
                              )
                        }
                    }
                )
            )
        )
    )
}
