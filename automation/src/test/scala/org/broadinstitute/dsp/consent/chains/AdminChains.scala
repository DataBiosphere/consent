package org.broadinstitute.dsp.consent.chains

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import org.broadinstitute.dsp.consent.requests.Requests
import spray.json._
import DefaultJsonProtocol._
import org.broadinstitute.dsp.consent.models.DataAccessRequestModels._
import org.broadinstitute.dsp.consent.models.ElectionModels._
import org.broadinstitute.dsp.consent.models.JsonProtocols
import org.broadinstitute.dsp.consent.services._
import scala.concurrent.duration._
import io.netty.handler.codec.http.HttpResponseStatus._

object AdminChains {
    def loginToConsole(additionalHeaders: Map[String, String]): ChainBuilder = {
        exec(
            Requests.User.me(OK.code, additionalHeaders)
        )
        .pause(1)
        .exec(
            Requests.Admin.initConsole(OK.code, additionalHeaders)
        )
    }

    def manageAccess(additionalHeaders: Map[String, String]): ChainBuilder = {
        exec(
            Requests.Dar.manageDar(OK.code, additionalHeaders)
        )
        .exitBlockOnFail {
            exec { session =>
                implicit val darManageFormat: JsonProtocols.DataAccessRequestManageFormat.type = JsonProtocols.DataAccessRequestManageFormat
                implicit val userFormat: JsonProtocols.userFormat.type = JsonProtocols.userFormat
                implicit val dacFormat: JsonProtocols.dacFormat.type = JsonProtocols.dacFormat
                implicit val electionStatusFormat: JsonProtocols.electionStatusFormat.type = JsonProtocols.electionStatusFormat

                val manageDarStr: String = session(Requests.Dar.manageDarResponse).as[String]
                val manageDars: Seq[DataAccessRequestManage] = manageDarStr.parseJson.convertTo[Seq[DataAccessRequestManage]]

                val newManageDars: Seq[DataAccessRequestManage] = DarService.setManageRolesByOwner(manageDars)

                val researcherDars: Seq[DataAccessRequestManage] = DarService.getPendingDARsByMostRecent(newManageDars, 2)
                val electionStatus: ElectionStatus = ElectionStatus(status = Status.OPEN, finalAccessVote = false)
                
                val newSession = session.set(Requests.Dar.manageDarResponse, researcherDars)
                newSession.set("electionStatusBody", electionStatus.toJson.compactPrint)
            }
        }   
        .exec(
            Requests.Dac.list(OK.code, additionalHeaders)
        )
    }

    def createElections(additionalHeaders: Map[String, String]): ChainBuilder = {
        repeat (session => session(Requests.Dar.manageDarResponse).as[Seq[DataAccessRequestManage]].size, "darIndex") {
            exec { session =>
                val manageDars: Seq[DataAccessRequestManage] = session(Requests.Dar.manageDarResponse).as[Seq[DataAccessRequestManage]]
                val darIndex: Int = session("darIndex").as[Int]

                session.set("dataRequestId", manageDars(darIndex).dataRequestId.getOrElse(""))
            }
            .exec(
                Requests.Election.createElection(CREATED.code, "${dataRequestId}", "${electionStatusBody}", additionalHeaders)
            )
        }
    }
}
