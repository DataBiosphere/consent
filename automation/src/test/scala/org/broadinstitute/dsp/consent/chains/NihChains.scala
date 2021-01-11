package org.broadinstitute.dsp.consent.chains

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import org.broadinstitute.dsp.consent.requests.Requests
import spray.json._
import DefaultJsonProtocol._
import org.broadinstitute.dsp.consent.models.JsonProtocols
import org.broadinstitute.dsp.consent.models.NihModels._
import org.broadinstitute.dsp.consent.models.ResearcherModels._
import org.broadinstitute.dsp.consent.models.UserModels._
import org.broadinstitute.dsp.consent.services._

object NihChains {
    def authenticate(additionalHeaders: Map[String, String]): ChainBuilder = {
        exec { session =>
            implicit val researcherInfoFormat: JsonProtocols.ResearcherInfoFormat.type = JsonProtocols.ResearcherInfoFormat
            implicit val fireCloudProfileFormat: JsonProtocols.fireCloudProfileFormat.type = JsonProtocols.fireCloudProfileFormat

            val researcherPropStr: String = session(Requests.Researcher.researcherPropertiesResponse).as[String]
            val researcherInfo: ResearcherInfo = researcherPropStr.parseJson.convertTo[ResearcherInfo]

            implicit val userFormat: JsonProtocols.userFormat.type = JsonProtocols.userFormat
            val userStr: String = session("userResponse").as[String]
            val user: User = userStr.parseJson.convertTo[User]

            val registerUserBody: FireCloudProfile = NihService.parseProfile(researcherInfo, user, "test")

            session.set("fireCloudRegisterUserBody", registerUserBody.toJson.compactPrint)
        }
        .doIf { session => 
            implicit val researchFormat: JsonProtocols.researcherPropertyFormat.type = JsonProtocols.researcherPropertyFormat
            implicit val researcherFormat: JsonProtocols.ResearcherInfoFormat.type = JsonProtocols.ResearcherInfoFormat
            val researcherPropsStr: String = session(Requests.Researcher.researcherPropertiesResponse).as[String]
            val researcherInfo: ResearcherInfo = researcherPropsStr.parseJson.convertTo[ResearcherInfo]
            
            val expCount: Int = NihService.expirationCount(researcherInfo.eraExpiration.getOrElse("0").toLong)
            !researcherInfo.eraAuthorized.getOrElse("false").toBoolean || expCount <= 0
        } {
            exec(
                Requests.FireCloud.verifyUser(additionalHeaders)
            )
            .exec { session => 
                val verifyStatus: String = session(Requests.FireCloud.fireCloudVerifyStatus).as[String]
                val isFCUser: Boolean = if (verifyStatus == "200") true else false
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
                    implicit val userFormat: JsonProtocols.userFormat.type = JsonProtocols.userFormat
                    implicit val nihUserFormat: JsonProtocols.nihUserFormat.type = JsonProtocols.nihUserFormat

                    val userStr: String = session("userResponse").as[String]
                    val user: User = userStr.parseJson.convertTo[User]

                    val nihUser: NihUserAccount = NihUserAccount(
                        linkedNihUsername = user.email.getOrElse(""), 
                        datasetPermissions = None, 
                        linkExpireTime = NihService.nextDate.toString, 
                        status = true
                    )
                    session.set("nihUserBody", nihUser.toJson.compactPrint)
                }
                .exec(
                    Requests.FireCloud.saveNihUser(200, "${nihUserBody}", additionalHeaders)
                )
            }
        }
    }
}