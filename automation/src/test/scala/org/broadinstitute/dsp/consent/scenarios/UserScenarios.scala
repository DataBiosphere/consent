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
import scala.collection.mutable.ListBuffer

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
                    implicit val dataSetFormatJson = JsonProtocols.DataSetFormat
                    implicit val dataSetEntryJson = JsonProtocols.dataSetEntryFormat
                    implicit val dataAccessRequestJson = JsonProtocols.DataAccessRequestFormat

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
                    val dar = DataAccessRequest(dacUserId, entrySets, setIds)
                    session.set("darRequestBody", dar.toJson.compactPrint)
                }
                .exec(
                    Requests.Dar.partialSave(201, """${darRequestBody}""", TestConfig.researcherHeader)
                )
        )
    )
}