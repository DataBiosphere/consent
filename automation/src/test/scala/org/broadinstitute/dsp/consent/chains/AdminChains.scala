package org.broadinstitute.dsp.consent.chains

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import org.broadinstitute.dsp.consent.requests.Requests
import org.broadinstitute.dsp.consent.TestConfig
import org.broadinstitute.dsp.consent.models.DataAccessRequestModels._
import io.netty.handler.codec.http.HttpResponseStatus._

object AdminChains {
    def loginToConsole(additionalHeaders: Map[String, String]): ChainBuilder = {
        exec(
            Requests.User.me(OK.code, additionalHeaders)
        )
    }

    def createElections(additionalHeaders: Map[String, String]): ChainBuilder = {
        repeat (session => session(Requests.Dar.manageDarResponse).as[Seq[DataAccessRequestManage]].size, "darIndex") {
            exec { session =>
                val manageDars: Seq[DataAccessRequestManage] = session(Requests.Dar.manageDarResponse).as[Seq[DataAccessRequestManage]]
                val darIndex: Int = session("darIndex").as[Int]
                session.set("dataRequestId", manageDars(darIndex).dar.get.referenceId)
            }
            .exec(
                Requests.Election.createElection(CREATED.code, "${dataRequestId}", "${electionStatusBody}", additionalHeaders)
            )
        }
    }
}
