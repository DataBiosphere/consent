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

object AdminChains {
    def loginToConsole(additionalHeaders: Map[String, String]): ChainBuilder = {
        exec(
            Requests.User.me(200, additionalHeaders)
        )
        .pause(1)
        .exec(
            Requests.Admin.initConsole(200, additionalHeaders)
        )
    }

    def manageAccess(additionalHeaders: Map[String, String]): ChainBuilder = {
        exec(
            Requests.Dar.manageDar(200, additionalHeaders)
        )
        .exec { session =>
            implicit val darManageFormat = JsonProtocols.DataAccessRequestManageFormat
            implicit val userFormat = JsonProtocols.userFormat
            implicit val dacFormat = JsonProtocols.dacFormat
            implicit val electionStatusFormat = JsonProtocols.electionStatusFormat

            val manageDarStr = session(Requests.Dar.manageDarResponse).as[String]
            var manageDars = manageDarStr.parseJson.convertTo[Seq[DataAccessRequestManage]]

            val newManageDars = DarService.setManageRolesByOwner(manageDars)

            val researcherDars = DarService.getPendingDARsByMostRecent(manageDars, 2)
            val electionStatus = ElectionStatus("Open", false)
            
            val newSession = session.set(Requests.Dar.manageDarResponse, researcherDars)
            newSession.set("electionStatusBody", electionStatus.toJson.compactPrint)
        }
        .exec(
            Requests.Dac.list(200, additionalHeaders)
        )
    }

    def createElections(additionalHeaders: Map[String, String]): ChainBuilder = {
        repeat (session => session(Requests.Dar.manageDarResponse).as[Seq[DataAccessRequestManage]].size, "darIndex") {
            exec { session =>
                val manageDars = session(Requests.Dar.manageDarResponse).as[Seq[DataAccessRequestManage]]
                val darIndex = session("darIndex").as[Int]

                session.set("dataRequestId", manageDars(darIndex).dataRequestId.getOrElse(""))
            }
            .exec(
                Requests.Election.createElection(201, "${dataRequestId}", "${electionStatusBody}", additionalHeaders)
            )
        }
    }
}