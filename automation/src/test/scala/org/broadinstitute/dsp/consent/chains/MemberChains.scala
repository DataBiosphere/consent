package org.broadinstitute.dsp.consent.chains

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import org.broadinstitute.dsp.consent.requests.Requests
import spray.json._
import DefaultJsonProtocol._
import org.broadinstitute.dsp.consent.models.PendingModels._
import org.broadinstitute.dsp.consent.models.JsonProtocols
import org.broadinstitute.dsp.consent.services.{DarService}
import scala.concurrent.duration._
import io.netty.handler.codec.http.HttpResponseStatus._

object MemberChains {
    def loginToConsole(additionalHeaders: Map[String, String]): ChainBuilder = {
        exec(
            Requests.User.me(OK.code, additionalHeaders)
        )
        .pause(1)
        .exec(
            Requests.PendingCases.getPendingDataRequestsByUserId(OK.code, "${dacUserId}", additionalHeaders)
        )
        .exec(
            Requests.PendingCases.getPendingCasesByUserId(OK.code, "${dacUserId}", additionalHeaders)
        )
    }
}
