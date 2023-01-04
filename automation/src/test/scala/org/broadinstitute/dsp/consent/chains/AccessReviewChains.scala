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

    val accessElection: String = "accessElection"

    def init(additionalHeaders: Map[String, String]): ChainBuilder = {
        exitBlockOnFail {
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
                implicit val voteFormat: JsonProtocols.voteFormat.type = JsonProtocols.voteFormat
                implicit val electionFormat: JsonProtocols.electionFormat.type = JsonProtocols.electionFormat
                implicit val electionReviewVoteFormat: JsonProtocols.electionReviewVoteFormat.type = JsonProtocols.electionReviewVoteFormat
                implicit val electionReviewFormat: JsonProtocols.electionReviewFormat.type = JsonProtocols.electionReviewFormat

                val voteListStr: String = session(Requests.Votes.getDarVoteListResponse).as[String]
                val voteList: Seq[Vote] = voteListStr.parseJson.convertTo[Seq[Vote]]
                val userId: Int = session(Requests.User.userId).as[String].toInt
                val electionId: Int = session(Requests.Votes.darElectionId).as[String].toInt

                val electionReviewStr: String = session(Requests.Election.getDataAccessElectionReviewResponse).as[String]
                val electionReview: ElectionReview = electionReviewStr.parseJson.convertTo[ElectionReview]
                val election: Election = electionReview.election.getOrElse(ElectionBuilder.empty())

                val session1 = session.set(electionDacVotes, ElectionService.getElectionVotesByTypeAndUser(voteList, VoteType.DAC, userId, electionId))
                val session2 = session1.set(electionFinalVotes, ElectionService.getElectionVotesByTypeAndUser(voteList, VoteType.FINAL, userId, electionId))
                val session3 = session2.set(electionAgreementVotes, ElectionService.getElectionVotesByTypeAndUser(voteList, VoteType.AGREEMENT, userId, electionId))
                val session4 = session3.set(electionChairpersonVotes, ElectionService.getElectionVotesByTypeAndUser(voteList, VoteType.CHAIRPERSON, userId, electionId))
                session4.set(accessElection, election)
            }
        }
    }

    def submitVote(votesString: String, additionalHeaders: Map[String, String], result: String = "true"): ChainBuilder = {
        exitBlockOnFail {
            exec { session =>
                implicit val votePostFormat: JsonProtocols.votePostFormat.type = JsonProtocols.votePostFormat

                try {
                    val votes: Seq[Vote] = session(votesString).as[Seq[Vote]]
                    val vote = votes.head
                    val userId: Int = session(Requests.User.userId).as[String].toInt

                    val votePostObject: VotePostObject = VotePostObject(
                        vote = Some(result == "true"),
                        userId = Some(userId),
                        rationale = Some(if (vote != null) vote.rationale.getOrElse("") else ""),
                        hasConcerns = Some(if (vote != null) vote.hasConcerns.getOrElse(false) else false)
                    )

                    if (vote != null) {
                        val session1: Session = session.set(AccessReviewChains.voteId, vote.voteId.getOrElse(0))
                        session1.set("submitVoteBody", votePostObject.toJson.compactPrint)
                    } else {
                        session.set("submitVoteBody", "")
                    }
                } catch {
                    case _: Throwable => session.set("submitVoteBody", "")
                }
            }
            .doIf { session =>
                session("submitVoteBody").as[String].nonEmpty
            } {
                exec(
                    Requests.Votes.postDarVote(OK.code, "${darReferenceId}", "${voteId}", "${submitVoteBody}", additionalHeaders)
                )
            }
        }
    }
}
