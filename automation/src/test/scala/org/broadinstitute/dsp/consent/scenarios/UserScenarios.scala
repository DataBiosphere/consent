package org.broadinstitute.dsp.consent.scenarios

import io.gatling.core.Predef._
import io.gatling.core.session.Session
import io.gatling.http.request.builder.HttpRequestBuilder
import io.gatling.http.Predef._
import org.broadinstitute.dsp.consent.{TestConfig, TestRunner}
import org.broadinstitute.dsp.consent.requests.Requests
import scala.concurrent.duration._
import spray.json._
import DefaultJsonProtocol._
import org.broadinstitute.dsp.consent.models._
import org.broadinstitute.dsp.consent.services._
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.IntMap

class UserScenarios extends Simulation with TestRunner {
    runScenarios(
        List(
            scenario("User Scenario")
                .exec(Requests.User.me(200, TestConfig.researcherHeader)),
            scenario("Researcher DataSet Catalog")
                .exec(
                    Requests.User.me(200, TestConfig.researcherHeader)
                )
                .pause(1 second)
                .exec(
                    Requests.User.dataSetCatalog(200, "${dacUserId}", TestConfig.researcherHeader)
                )
                .exec { session =>
                    implicit val dataSetFormatJson = JsonProtocols.dataSetFormat
                    implicit val dataSetEntryJson = JsonProtocols.dataSetEntryFormat
                    implicit val dataAccessRequestJson = JsonProtocols.dataAccessRequestDraftFormat

                    var dataSetStr = session(Requests.DataSet.dataSetResponse).as[String]
                    val dataSets = dataSetStr.parseJson.convertTo[Seq[DataSet]]
                    
                    var chosenSets = dataSets
                        .filter(_.active)
                        .groupBy(_.dacId)
                        .map(_._2.head)
                        .take(2)

                    var setIds = new ListBuffer[Int]()
                    var entrySets = new ListBuffer[DataSetEntry]()
                    chosenSets.foreach(set => {
                        setIds += set.dataSetId
                        val prop = set.properties.filter(_.propertyName == "Dataset Name").head.propertyValue
                        entrySets += DataSetEntry(
                            set.dataSetId.toString,
                            set.dataSetId.toString,
                            prop,
                            prop
                        )
                    })
                    
                    val dacUserId: Int = session("dacUserId").as[Int]
                    val dar = DataAccessRequestDraft(dacUserId, entrySets, setIds)
                    val newSession = session.set("darRequestBody", dar.toJson.compactPrint)
                    newSession.set("dataSetIds", setIds)
                }
                .exec(
                    Requests.Dar.partialSave(201, """${darRequestBody}""", TestConfig.researcherHeader)
                )
                .pause(1)
                .exec(
                    Requests.Dar.getPartial(200, "${darReferenceId}", TestConfig.researcherHeader)
                )
                .exec(
                    Requests.DataSet.getDataSetsByDataSetIds(
                        200, 
                        TestConfig.researcherHeader,
                        Requests.DataSet.getDataSetsByDataSetId(200, "${dataSetIds(0)}", TestConfig.researcherHeader, 0),
                        Requests.DataSet.getDataSetsByDataSetId(200, "${dataSetIds(1)}", TestConfig.researcherHeader, 1))
                )
                .exec(
                    Requests.Researcher.getResearcherProperties(200, "${dacUserId}", TestConfig.researcherHeader)
                )
                .doIf { session => 
                    implicit val researchFormat = JsonProtocols.researcherPropertyFormat
                    implicit val researcherFormat = JsonProtocols.ResearcherInfoFormat
                    val researcherPropsStr = session(Requests.Researcher.researcherPropertiesResponse).as[String]
                    val researcherInfo = researcherPropsStr.parseJson.convertTo[ResearcherInfo]
                    
                    val expCount = NihService.expirationCount(researcherInfo.eraExpiration.getOrElse(0))
                    !researcherInfo.eraAuthorized.getOrElse(false) || expCount <= 0
                } {
                    exec(
                        Requests.FireCloud.verifyUser(TestConfig.researcherHeader)
                    )
                    .exec { session => 
                        val verifyStatus = session(Requests.FireCloud.fireCloudVerifyStatus).as[String]
                        session.set("isFCUser", if (verifyStatus == "404") false else true)
                    }
                    .doIfEquals("${isFCUser}", false) {
                        exec(
                            Requests.FireCloud.registerUser(200, TestConfig.researcherHeader)
                        )
                        /*.exec { session => 
                            session.set("isFCUser", true)
                        }*/
                    }
                }
                .exec { session =>
                    implicit val researchFormat = JsonProtocols.researcherPropertyFormat
                    implicit val researcherFormat = JsonProtocols.ResearcherInfoFormat
                    implicit val dataAccessFormat = JsonProtocols.dataAccessRequestFormat
                    implicit val dataSetFormat = JsonProtocols.dataSetFormat
                    var darMap = collection.mutable.Map[String, JsValue]()

                    val researcherPropsStr = session(Requests.Researcher.researcherPropertiesResponse).as[String]
                    val researcherInfo = researcherPropsStr.parseJson.convertTo[ResearcherInfo]
                    val partialDar = session(Requests.Dar.darPartialResponse).as[String]
                        .parseJson.convertTo[DataAccessRequest];

                    val dsIds = session("dataSetIds").as[Seq[Int]]
                    var datasetList = collection.mutable.ListBuffer[DataSet]()
                    for (id <- 0 to 1) {
                        val dataSetStr = session(Requests.DataSet.dataSetsByDataSetId + id).as[String]
                        datasetList += dataSetStr.parseJson.convertTo[DataSet]
                    }

                    darMap += ("datasets" -> JsArray(datasetList.map(_.toJson).toVector))

                    val piName = researcherInfo.piName.getOrElse("")
                    val profileName = researcherInfo.profileName.getOrElse("Test Researcher")
                    val isThePI = researcherInfo.isThePI.getOrElse("false")

                    darMap += ("researcher" -> JsString(profileName))
                    
                    if (piName == "" && isThePI == "true") {
                        darMap += ("investigator" -> JsString(profileName))
                    } else if (piName == "" && isThePI == "false") {
                        darMap += ("investigator" -> JsString("--"))
                    } else {
                        darMap += ("investigator" -> JsString(piName))
                    }

                    darMap += ("linkedIn" -> JsString(researcherInfo.linkedIn.getOrElse("")))
                    darMap += ("researcherGate" -> JsString(researcherInfo.researcherGate.getOrElse("")))
                    darMap += ("orcid" -> JsString(researcherInfo.orcid.getOrElse("")))
                    darMap += ("institution" -> JsString(researcherInfo.institution.getOrElse("")))
                    darMap += ("department" -> JsString(researcherInfo.department.getOrElse("")))
                    darMap += ("division" -> JsString(researcherInfo.division.getOrElse("")))
                    darMap += ("address1" -> JsString(researcherInfo.address1.getOrElse("")))
                    darMap += ("address2" -> JsString(researcherInfo.address2.getOrElse("")))
                    darMap += ("city" -> JsString(researcherInfo.city.getOrElse("")))
                    darMap += ("zipCode" -> JsString(researcherInfo.zipcode.getOrElse("")))
                    darMap += ("country" -> JsString(researcherInfo.country.getOrElse("")))
                    darMap += ("state" -> JsString(researcherInfo.state.getOrElse("")))
                    darMap += ("piName" -> JsString(researcherInfo.piName.getOrElse("")))
                    darMap += ("academicEmail" -> JsString(researcherInfo.academicEmail.getOrElse("")))
                    darMap += ("piEmail" -> JsString(researcherInfo.piEmail.getOrElse("")))
                    darMap += ("isThePi" -> JsString(researcherInfo.isThePI.getOrElse("")))
                    darMap += ("havePi" -> JsString(researcherInfo.havePI.getOrElse("")))
                    darMap += ("pubmedId" -> JsString(researcherInfo.pubmedID.getOrElse("")))
                    darMap += ("scientificUrl" -> JsString(researcherInfo.scientificURL.getOrElse("")))
                    darMap += ("userId" -> JsNumber(session("dacUserId").as[Int]))

                    session
                }
        )
    )
}