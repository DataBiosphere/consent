package org.broadinstitute.dsp.consent.chains

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.netty.handler.codec.http.HttpResponseStatus._
import org.broadinstitute.dsp.consent.TestConfig
import org.broadinstitute.dsp.consent.requests.Requests

object MemberChains {
    def loginToConsole(additionalHeaders: Map[String, String]): ChainBuilder = {
        exec(
            Requests.User.me(OK.code, additionalHeaders)
        )
        .pause(TestConfig.defaultPause)
        .exec(
            Requests.PendingCases.getPendingDataRequests(OK.code, additionalHeaders)
        )
        .exec(
            Requests.PendingCases.getPendingCasesByUserId(OK.code, "${dacUserId}", additionalHeaders)
        )
    }
}
