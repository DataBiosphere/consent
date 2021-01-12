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

    def voteOnPendingDars(additionalHeaders: Map[String, String], limit: Int = 2): ChainBuilder = {
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
                }
            }
        }
    }
}
