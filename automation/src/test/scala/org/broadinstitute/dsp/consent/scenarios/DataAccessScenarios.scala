package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef._
import org.broadinstitute.dsp.consent.{TestConfig, TestRunner}
import org.broadinstitute.dsp.consent.requests.Requests
import scala.concurrent.duration._
import org.broadinstitute.dsp.consent.chains.{DarChains, DataSetChains, NihChains, AdminChains, MemberChains}
import io.netty.handler.codec.http.HttpResponseStatus._

class DataAccessScenarios extends Simulation with TestRunner {
    runPopulations(
        List(
            defaultPopulation(
                scenario("Researcher DAR with 2 DataSets")
                .exitBlockOnFail {
                    exec(
                        Requests.User.me(OK.code, TestConfig.researcherHeader)
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
                }
            )
            .andThen(
                defaultPopulation(
                    scenario("Admins Election Creation")
                    .exitBlockOnFail {
                        exec(
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
                    }
                )
                .andThen(
                    defaultPopulation(
                        scenario("Member Voting")
                        .exitBlockOnFail {
                            exec(
                                MemberChains.loginToConsole(TestConfig.memberHeader)
                            )
                            .pause(1)
                            .exec(
                                MemberChains.voteOnPendingDars(TestConfig.memberHeader)
                            )
                        }
                    )
                )
            )
        )
    )
}
