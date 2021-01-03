package org.broadinstitute.dsp.consent.chains

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import org.broadinstitute.dsp.consent.requests.Requests
import spray.json._
import DefaultJsonProtocol._
import org.broadinstitute.dsp.consent.models._
import org.broadinstitute.dsp.consent.services._
import scala.collection.mutable.ListBuffer

object NihChains {
    def authenticate(additionalHeaders: Map[String, String]): ChainBuilder = {
        exec { session =>
            implicit val researcherInfoFormat = JsonProtocols.ResearcherInfoFormat
            implicit val fireCloudProfileFormat = JsonProtocols.fireCloudProfileFormat

            val researcherPropStr = session(Requests.Researcher.researcherPropertiesResponse).as[String]
            val researcherInfo = researcherPropStr.parseJson.convertTo[ResearcherInfo]

            implicit val userFormat = JsonProtocols.userFormat
            val userStr = session("userResponse").as[String]
            val user = userStr.parseJson.convertTo[User]

            val registerUserBody = NihService.parseProfile(researcherInfo, user, "test")

            session.set("fireCloudRegisterUserBody", registerUserBody.toJson.compactPrint)
        }
        .doIf { session => 
            implicit val researchFormat = JsonProtocols.researcherPropertyFormat
            implicit val researcherFormat = JsonProtocols.ResearcherInfoFormat
            val researcherPropsStr = session(Requests.Researcher.researcherPropertiesResponse).as[String]
            val researcherInfo = researcherPropsStr.parseJson.convertTo[ResearcherInfo]
            
            val expCount = NihService.expirationCount(researcherInfo.eraExpiration.getOrElse("0").toLong)
            !researcherInfo.eraAuthorized.getOrElse("false").toBoolean || expCount <= 0
        } {
            exec(
                Requests.FireCloud.verifyUser(additionalHeaders)
            )
            .exec { session => 
                val verifyStatus = session(Requests.FireCloud.fireCloudVerifyStatus).as[String]
                val isFCUser = if (verifyStatus == "200") true else false
                session.set("isFCUser", isFCUser)
            }
            .doIfEquals("${isFCUser}", false) {
                exitBlockOnFail {
                    exec(
                        Requests.FireCloud.registerUser(200, "${fireCloudRegisterUserBody}", additionalHeaders)
                    )
                    .exec{ session =>
                        session.set("isFCUser", true)
                    }
                }
            }
            .doIfEquals("${isFCUser}", true) {
                exec { session =>
                    implicit val userFormat = JsonProtocols.userFormat
                    implicit val nihUserFormat = JsonProtocols.nihUserFormat

                    val userStr = session("userResponse").as[String]
                    val user = userStr.parseJson.convertTo[User]

                    val nihUser = NihUserAccount(user.email, None, NihService.nextDate.toString, true)
                    session.set("nihUserBody", nihUser.toJson.compactPrint)
                }
                .exec(
                    Requests.FireCloud.saveNihUser(200, "${nihUserBody}", additionalHeaders)
                )
            }
        }
    }
}