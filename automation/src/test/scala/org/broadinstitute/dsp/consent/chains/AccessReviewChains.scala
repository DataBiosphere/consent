package org.broadinstitute.dsp.consent.chains

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import org.broadinstitute.dsp.consent.requests.Requests
import spray.json._
import DefaultJsonProtocol._
import org.broadinstitute.dsp.consent.models.PendingModels._
import org.broadinstitute.dsp.consent.models.JsonProtocols
import org.broadinstitute.dsp.consent.services._
import scala.concurrent.duration._
import io.netty.handler.codec.http.HttpResponseStatus._

object AccessReviewChains {
    val referenceId: String = "darReferenceId"
    val voteId: String = "voteId"

    def init(additionalHeaders: Map[String, String]): ChainBuilder = {
        exec(
            Requests.Votes.getDarVote(OK.code, "${darReferenceId}", "${voteId}", additionalHeaders)
        )
        .exec(
            Requests.Election.getDataAccessElectionReview(OK.code, "${darElectionId}", "false", additionalHeaders)
        )
        .exec(
            DarChains.describeDarWithConsent(additionalHeaders)
        )
        .exec(
            Requests.Votes.getDarVoteList(OK.code, "${darReferenceId}", additionalHeaders)
        )
        .exec(
            Requests.Researcher.getResearcherProperties(OK.code, "${dataAccessUserId}", additionalHeaders)
        )
    }
}
