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

object AccessReviewChains {
    val referenceId: String = "darReferenceId"
    val voteId: String = "voteId"

    def init(additionalHeaders: Map[String, String]): ChainBuilder = {
        exec(
            Requests.Votes.getDarVote(200, "${darReferenceId}", "${voteId}", additionalHeaders)
        )
        .exec(
            Requests.Election.getDataAccessElectionReview(200, "${darElectionId}", "false", additionalHeaders)
        )
        .exec(
            DarChains.describeDarWithConsent(additionalHeaders)
        )
        .exec(
            Requests.Votes.getDarVoteList(200, "${darReferenceId}", additionalHeaders)
        )
        .exec(
            Requests.Researcher.getResearcherProperties(200, "${dataAccessUserId}", additionalHeaders)
        )
    }
}
