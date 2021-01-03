package org.broadinstitute.dsp.consent.chains

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import org.broadinstitute.dsp.consent.requests.Requests
import spray.json._
import DefaultJsonProtocol._
import org.broadinstitute.dsp.consent.models._
import org.broadinstitute.dsp.consent.services._
import scala.collection.mutable.ListBuffer

object DataSetChains {
    def dataSetCatalogPickTwo(additionalHeaders: Map[String, String]): ChainBuilder = {
        exec(
            Requests.User.dataSetCatalog(200, "${dacUserId}", additionalHeaders)
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
                entrySets += DataSetEntryBuilder.fromDataSet(set)
            })
            
            val dacUserId: Int = session("dacUserId").as[Int]
            val dar = DataAccessRequestDraft(dacUserId, entrySets, setIds)
            val newSession = session.set("darRequestBody", dar.toJson.compactPrint)
            newSession.set("dataSetIds", setIds)
        }
        .exec(
            Requests.Dar.partialSave(201, """${darRequestBody}""", additionalHeaders)
        )
    }
}