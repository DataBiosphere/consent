package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef._
import org.broadinstitute.dsp.consent.{TestConfig, TestRunner}
import org.broadinstitute.dsp.consent.requests.Requests
import scala.concurrent.duration._
import org.broadinstitute.dsp.consent.chains.{DarChains, DataSetChains, NihChains, AdminChains, MemberChains, ChairChains, AccessReviewChains}
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
                    .pause(TestConfig.defaultPause)
                    .exec(
                        DataSetChains.dataSetCatalogPickTwo(TestConfig.researcherHeader)
                    )
                    .pause(TestConfig.defaultPause)
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
                        .pause(TestConfig.defaultPause)
                        .exec(
                            AdminChains.manageAccess(TestConfig.adminHeader)
                        )
                        .pause(TestConfig.defaultPause)
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
                            .pause(TestConfig.defaultPause)
                            .exec(
                                AccessReviewChains.voteOnPendingDars(TestConfig.memberHeader, AccessReviewChains.submitVote(AccessReviewChains.electionDacVotes, TestConfig.memberHeader))
                            )
                        }
                    )
                    .andThen(
                        defaultPopulation(
                            scenario("Chair Voting")
                            .exitBlockOnFail {
                                exec(
                                    ChairChains.loginToConsole(TestConfig.chairHeader)
                                )
                                .pause(TestConfig.defaultPause)
                                .exec(
                                    AccessReviewChains.voteOnPendingDars(TestConfig.memberHeader, ChairChains.submitVote(TestConfig.chairHeader))
                                )
                            }
                        )
                    )
                )
            )
        )
    )
}
