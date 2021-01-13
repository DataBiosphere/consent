package org.broadinstitute.dsp.consent.chains

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import org.broadinstitute.dsp.consent.requests.Requests
import spray.json._
import DefaultJsonProtocol._
import org.broadinstitute.dsp.consent.models.PendingModels._
import org.broadinstitute.dsp.consent.models.ElectionModels._
import org.broadinstitute.dsp.consent.models.JsonProtocols
import org.broadinstitute.dsp.consent.services._
import scala.concurrent.duration._
import io.netty.handler.codec.http.HttpResponseStatus._

object AccessReviewChains {
    val referenceId: String = "darReferenceId"
    val voteId: String = "voteId"

    val electionDacVotes: String = "electionDacVotes"
    val electionFinalVotes: String = "electionFinalVotes"
    val electionAgreementVotes: String = "electionAgreementVotes"
    val electionChairpersonVotes: String = "electionChairpersonVotes"

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
        .exec { session =>
            implicit val voteFormat: JsonProtocols.VoteFormat.type = JsonProtocols.VoteFormat

            val voteListStr: String = session(Requests.Votes.getDarVoteListResponse).as[String]
            val voteList: Seq[Vote] = voteListStr.parseJson.convertTo[Seq[Vote]]
            val dacUserId: Int = session(Requests.User.dacUserId).as[String].toInt
            val electionId: Int = session(Requests.Votes.darElectionId).as[String].toInt

            val session1 = session.set(electionDacVotes, ElectionService.getElectionVotesByTypeAndUser(voteList, VoteType.DAC, dacUserId, electionId))
            val session2 = session1.set(electionFinalVotes, ElectionService.getElectionVotesByTypeAndUser(voteList, VoteType.FINAL, dacUserId, electionId))
            val session3 = session2.set(electionAgreementVotes, ElectionService.getElectionVotesByTypeAndUser(voteList, VoteType.AGREEMENT, dacUserId, electionId))
            session3.set(electionChairpersonVotes, ElectionService.getElectionVotesByTypeAndUser(voteList, VoteType.CHAIRPERSON, dacUserId, electionId))
        }
    }

    def submitVote(isChair: Boolean, additionalHeaders: Map[String, String]): ChainBuilder = {
        exec { session => 
            implicit val votePostFormat: JsonProtocols.votePostFormat.type = JsonProtocols.votePostFormat
            
            val votes: Seq[Vote] = session(electionDacVotes).as[Seq[Vote]]
            val vote = votes(0)
            val dacUserId: Int = session(Requests.User.dacUserId).as[String].toInt

            val votePostObject: VotePostObject = VotePostObject(
                vote = true,
                dacUserId = dacUserId,
                rationale = Some(""),
                hasConcerns = Some(false)
            )

            session.set("submitVoteBody", votePostObject.toJson.compactPrint)
        }
        .exec(
            Requests.Votes.postDarVote(OK.code, "${darReferenceId}", "${voteId}", "${submitVoteBody}", additionalHeaders)
        )
    }
}
