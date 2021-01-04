package org.broadinstitute.dsp.consent.chains

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import org.broadinstitute.dsp.consent.requests.Requests
import spray.json._
import DefaultJsonProtocol._
import org.broadinstitute.dsp.consent.models.DataAccessRequestModels._
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

            val manageDarStr = session(Requests.Dar.manageDarResponse).as[String]
            var manageDars = manageDarStr.parseJson.convertTo[Seq[DataAccessRequestManage]]

            val newManageDars = DarService.setManageRolesByOwner(manageDars)
            
            session.set(Requests.Dar.manageDarResponse, newManageDars.toJson.compactPrint)
        }
        .exec(
            Requests.Dac.list(200, additionalHeaders)
        )
    }
}