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
                        .filter(ds => ds.active && !ds.dataUse.collaboratorRequired.getOrElse(false))
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
                .exec { session =>
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
                        Requests.FireCloud.verifyUser(TestConfig.researcherHeader)
                    )
                    .exec { session => 
                        val verifyStatus = session(Requests.FireCloud.fireCloudVerifyStatus).as[String]
                        val isFCUser = if (verifyStatus == "200") true else false
                        session.set("isFCUser", isFCUser)
                    }
                    .doIfEquals("${isFCUser}", false) {
                        exitBlockOnFail {
                            exec(
                                Requests.FireCloud.registerUser(200, "${fireCloudRegisterUserBody}", TestConfig.researcherHeader)
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
                            Requests.FireCloud.saveNihUser(200, "${nihUserBody}", TestConfig.researcherHeader)
                        )
                    }
                }
                .exec { session =>
                    implicit val researchFormat = JsonProtocols.researcherPropertyFormat
                    implicit val researcherFormat = JsonProtocols.ResearcherInfoFormat
                    implicit val dataAccessRequestDataFormat = JsonProtocols.DataAccessRequestDataFormat
                    implicit val dataAccessFormat = JsonProtocols.dataAccessRequestFormat
                    implicit val dataSetFormat = JsonProtocols.dataSetFormat
                    implicit val dataSetEntryFormat = JsonProtocols.dataSetEntryFormat
                    var dataMap = collection.mutable.Map[String, JsValue]()
                    var darMap = collection.mutable.Map[String, JsValue]()

                    val researcherPropsStr = session(Requests.Researcher.researcherPropertiesResponse).as[String]
                    val researcherInfo = researcherPropsStr.parseJson.convertTo[ResearcherInfo]
                    val partialDar = session(Requests.Dar.darPartialJson).as[String]
                        .parseJson.convertTo[DataAccessRequest];

                    val dsIds = session("dataSetIds").as[Seq[Int]]
                    var datasetList = collection.mutable.ListBuffer[DataSet]()
                    for (id <- 0 to 1) {
                        val dataSetStr = session(Requests.DataSet.dataSetsByDataSetId + id).as[String]
                        datasetList += dataSetStr.parseJson.convertTo[DataSet]
                    }

                    dataMap += ("datasets" -> JsArray(datasetList.map(ds => {
                        val prop = ds.properties.filter(_.propertyName == "Dataset Name").head.propertyValue
                        DataSetEntry(
                            ds.dataSetId.toString,
                            ds.dataSetId.toString,
                            prop,
                            prop
                        ).toJson
                    }).toVector))
                    dataMap += ("datasetIds" -> JsArray(dsIds.map(_.toJson).toVector))

                    val piName = researcherInfo.piName.getOrElse("")
                    val profileName = researcherInfo.profileName.getOrElse("Test Researcher")
                    val isThePI = researcherInfo.isThePI.getOrElse("false")

                    dataMap += ("researcher" -> JsString(profileName))
                    
                    if (piName == "" && isThePI == "true") {
                        dataMap += ("investigator" -> JsString(profileName))
                    } else if (piName == "" && isThePI == "false") {
                        dataMap += ("investigator" -> JsString("--"))
                    } else {
                        dataMap += ("investigator" -> JsString(piName))
                    }
                    
                    val referenceId = session("darReferenceId").as[String]
                    dataMap += ("referenceId" -> JsString(referenceId))
                    dataMap += ("linkedIn" -> JsString(researcherInfo.linkedIn.getOrElse("")))
                    dataMap += ("researcherGate" -> JsString(researcherInfo.researcherGate.getOrElse("")))
                    dataMap += ("orcid" -> JsString(researcherInfo.orcid.getOrElse("")))
                    dataMap += ("institution" -> JsString(researcherInfo.institution.getOrElse("")))
                    dataMap += ("department" -> JsString(researcherInfo.department.getOrElse("")))
                    dataMap += ("division" -> JsString(researcherInfo.division.getOrElse("")))
                    dataMap += ("address1" -> JsString(researcherInfo.address1.getOrElse("")))
                    dataMap += ("address2" -> JsString(researcherInfo.address2.getOrElse("")))
                    dataMap += ("city" -> JsString(researcherInfo.city.getOrElse("")))
                    dataMap += ("zipCode" -> JsString(researcherInfo.zipcode.getOrElse("")))
                    dataMap += ("country" -> JsString(researcherInfo.country.getOrElse("")))
                    dataMap += ("state" -> JsString(researcherInfo.state.getOrElse("")))
                    dataMap += ("piName" -> JsString(researcherInfo.piName.getOrElse("")))
                    dataMap += ("academicEmail" -> JsString(researcherInfo.academicEmail.getOrElse("")))
                    dataMap += ("piEmail" -> JsString(researcherInfo.piEmail.getOrElse("")))
                    dataMap += ("isThePi" -> JsString(researcherInfo.isThePI.getOrElse("")))
                    dataMap += ("havePi" -> JsString(researcherInfo.havePI.getOrElse("")))
                    dataMap += ("pubmedId" -> JsString(researcherInfo.pubmedID.getOrElse("")))
                    dataMap += ("scientificUrl" -> JsString(researcherInfo.scientificURL.getOrElse("")))
                    dataMap += ("userId" -> JsNumber(session("dacUserId").as[Int]))
                    dataMap += ("itDirector" -> JsString("Test IT Director"))
                    dataMap += ("signingOfficial" -> JsString("Test Signing Official"))
                    dataMap += ("checkCollaborator" -> JsBoolean(false))
                    dataMap += ("anvilUse" -> JsBoolean(false))
                    dataMap += ("cloudUse" -> JsBoolean(false))
                    dataMap += ("localUse" -> JsBoolean(true))
                    dataMap += ("projectTitle" -> JsString("Test Project"))
                    dataMap += ("rus" -> JsString("test"))
                    dataMap += ("nonTechRus" -> JsString("test non technical"))
                    dataMap += ("hmb" -> JsBoolean(false))
                    dataMap += ("poa" -> JsBoolean(true))
                    dataMap += ("other" -> JsBoolean(false))
                    dataMap += ("forProfit" -> JsBoolean(false))
                    dataMap += ("oneGender" -> JsBoolean(false))
                    dataMap += ("pediatric" -> JsBoolean(false))
                    dataMap += ("illegalBehavior" -> JsBoolean(false))
                    dataMap += ("addiction" -> JsBoolean(false))
                    dataMap += ("sexualDiseases" -> JsBoolean(false))
                    dataMap += ("stigmatizedDiseases" -> JsBoolean(false))
                    dataMap += ("vulnerablePopulation" -> JsBoolean(false))
                    dataMap += ("populationMigration" -> JsBoolean(false))
                    dataMap += ("psychiatricTraits" -> JsBoolean(false))
                    dataMap += ("notHealth" -> JsBoolean(false))

                    val newPartialDar = DataAccessRequest(
                        id = partialDar.id,
                        referenceId = partialDar.referenceId,
                        data = dataMap.toMap.toJson.convertTo[Option[DataAccessRequestData]],
                        draft = partialDar.draft,
                        userId = partialDar.userId,
                        createDate = partialDar.createDate,
                        sortDate = partialDar.sortDate,
                        submissionDate = partialDar.submissionDate,
                        updateDate = partialDar.updateDate
                    )

                    val newSession = session.set("dataAccessRequestJson", newPartialDar.toJson.prettyPrint)
                    val finalDar = DataAccessRequest(
                        id = partialDar.id,
                        referenceId = partialDar.referenceId,
                        data = dataMap.toMap.toJson.convertTo[Option[DataAccessRequestData]],
                        draft = Some(false),
                        userId = partialDar.userId,
                        createDate = None,
                        sortDate = None,
                        submissionDate = partialDar.submissionDate,
                        updateDate = partialDar.updateDate
                    )
                    newSession.set("dataAccessRequestSubmit", finalDar.toJson.prettyPrint)
                }
                .exec(
                    Requests.Dar.updatePartial(200, "${darReferenceId}", "${dataAccessRequestJson}", TestConfig.researcherHeader)
                )
                .exec(
                    Requests.Dar.saveFinal(201, "${dataAccessRequestSubmit}", TestConfig.researcherHeader)
                )
        )
    )
}