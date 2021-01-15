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
                implicit val voteFormat: JsonProtocols.VoteFormat.type = JsonProtocols.VoteFormat
                implicit val electionFormat: JsonProtocols.electionFormat.type = JsonProtocols.electionFormat
                implicit val electionReviewVoteFormat: JsonProtocols.electionReviewVoteFormat.type = JsonProtocols.electionReviewVoteFormat
                implicit val electionReviewFormat: JsonProtocols.electionReviewFormat.type = JsonProtocols.electionReviewFormat

                val voteListStr: String = session(Requests.Votes.getDarVoteListResponse).as[String]
                val voteList: Seq[Vote] = voteListStr.parseJson.convertTo[Seq[Vote]]
                val dacUserId: Int = session(Requests.User.dacUserId).as[String].toInt
                val electionId: Int = session(Requests.Votes.darElectionId).as[String].toInt

                val electionReviewStr: String = session(Requests.Election.getDataAccessElectionReviewResponse).as[String]
                val electionReview: ElectionReview = electionReviewStr.parseJson.convertTo[ElectionReview]
                val election: Election = electionReview.election.getOrElse(ElectionBuilder.empty)

                val session1 = session.set(electionDacVotes, ElectionService.getElectionVotesByTypeAndUser(voteList, VoteType.DAC, dacUserId, electionId))
                val session2 = session1.set(electionFinalVotes, ElectionService.getElectionVotesByTypeAndUser(voteList, VoteType.FINAL, dacUserId, electionId))
                val session3 = session2.set(electionAgreementVotes, ElectionService.getElectionVotesByTypeAndUser(voteList, VoteType.AGREEMENT, dacUserId, electionId))
                val session4 = session3.set(electionChairpersonVotes, ElectionService.getElectionVotesByTypeAndUser(voteList, VoteType.CHAIRPERSON, dacUserId, electionId))
                session4.set(accessElection, election)
            }
        }
    }

    def voteOnPendingDars(additionalHeaders: Map[String, String], chain: ChainBuilder, limit: Int = 2): ChainBuilder = {
        exitBlockOnFail {
            exec { session => 
                implicit val pendingCaseFormat: JsonProtocols.pendingCaseFormat.type = JsonProtocols.pendingCaseFormat
                implicit val dacFormat: JsonProtocols.dacFormat.type = JsonProtocols.dacFormat
                implicit val userRoleFormat: JsonProtocols.userRoleFormat.type = JsonProtocols.userRoleFormat
                implicit val userFormat: JsonProtocols.userFormat.type = JsonProtocols.userFormat
                val pendingCasesStr: String = session(Requests.PendingCases.dataRequestPendingResponse).as[String]
                val pendingCases: Seq[PendingCase] = pendingCasesStr.parseJson.convertTo[Seq[PendingCase]]

                val automationPending: Seq[PendingCase] = pendingCases.filter { pc =>
                    pc.projectTitle.getOrElse("") == DarService.projectTitle && !pc.alreadyVoted.getOrElse(false)
                }
                .sortWith { (pc1, pc2) =>
                    pc1.createDate.getOrElse(0L) > pc2.createDate.getOrElse(0L)
                }
                .take(limit).toSeq

                session.set("pendingCases", automationPending)
            }
            .repeat(session => session("pendingCases").as[Seq[PendingCase]].size, "pendingIndex") {
                exitBlockOnFail {
                    exec { session =>
                        val pendingCases: Seq[PendingCase] = session("pendingCases").as[Seq[PendingCase]]
                        val index: Int = session("pendingIndex").as[Int]
                        val pendingCase: PendingCase = pendingCases(index)

                        val session1 = session.set(AccessReviewChains.referenceId, pendingCase.referenceId.getOrElse(""))
                        val session2 = session1.set(AccessReviewChains.voteId, pendingCase.voteId.getOrElse(0))
                        session2
                    }
                    .exec(
                        AccessReviewChains.init(additionalHeaders)
                    )
                    .exec(
                        chain
                    )
                }
            }
        }
    }

    def submitVote(votesString: String, additionalHeaders: Map[String, String], result: String = "true"): ChainBuilder = {
        exitBlockOnFail {
            exec { session => 
                implicit val votePostFormat: JsonProtocols.votePostFormat.type = JsonProtocols.votePostFormat
                
                try {
                    val votes: Seq[Vote] = session(votesString).as[Seq[Vote]]
                    val vote = votes(0)
                    val dacUserId: Int = session(Requests.User.dacUserId).as[String].toInt

                    val votePostObject: VotePostObject = VotePostObject(
                        vote = Some(result == "true"),
                        dacUserId = Some(dacUserId),
                        rationale = Some(""),
                        hasConcerns = Some(false)
                    )

                    session.set("submitVoteBody", votePostObject.toJson.compactPrint)
                } catch {
                    case _: Throwable => session.set("submitVoteBody", "")
                }
            }
            .doIf { session =>
                session("submitVoteBody").as[String].length > 0
            } {
                exec(
                    Requests.Votes.postDarVote(OK.code, "${darReferenceId}", "${voteId}", "${submitVoteBody}", additionalHeaders)
                )
            }
        }
    }
}
