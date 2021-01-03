package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef._
import io.gatling.core.session.Session
import io.gatling.http.request.builder.HttpRequestBuilder
import io.gatling.http.Predef._
import org.broadinstitute.dsp.consent.{TestConfig, TestRunner}
import org.broadinstitute.dsp.consent.requests.Requests
import scala.concurrent.duration._
import spray.json._
import DefaultJsonProtocol._
import org.broadinstitute.dsp.consent.models._
import org.broadinstitute.dsp.consent.services._
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.IntMap
import org.broadinstitute.dsp.consent.chains.{DarChains, DataSetChains, NihChains, AdminChains}

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
                ),
            scenario("Admins Voting")
                .exec(
                    AdminChains.loginToConsole(TestConfig.adminHeader)
                )
                .pause(1)
                .exec(
                    AdminChains.manageAccess(TestConfig.adminHeader)
                )
        )
    )
}