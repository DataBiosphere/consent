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
import org.broadinstitute.dsp.consent.services._
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.IntMap
import org.broadinstitute.dsp.consent.chains.{DarChains, DataSetChains, NihChains, AdminChains}

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
        )
    )
}