package org.broadinstitute.dsp.consent.chains

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import org.broadinstitute.dsp.consent.requests.Requests
import spray.json._
import DefaultJsonProtocol._
import org.broadinstitute.dsp.consent.models.JsonProtocols
import org.broadinstitute.dsp.consent.models.DataSetModels._
import org.broadinstitute.dsp.consent.models.DataAccessRequestModels._
import org.broadinstitute.dsp.consent.services._

object DataSetChains {
    def dataSetCatalogPickTwo(additionalHeaders: Map[String, String]): ChainBuilder = {
        exitBlockOnFail {
            exec(
                Requests.User.dataSetCatalog(200, "${dacUserId}", additionalHeaders)
            )
            .exec { session =>
                implicit val dataSetFormatJson: JsonProtocols.dataSetFormat.type = JsonProtocols.dataSetFormat
                implicit val dataSetEntryJson: JsonProtocols.dataSetEntryFormat.type = JsonProtocols.dataSetEntryFormat
                implicit val dataAccessRequestJson: JsonProtocols.dataAccessRequestDraftFormat.type = JsonProtocols.dataAccessRequestDraftFormat

                val dataSetStr: String = session(Requests.DataSet.dataSetResponse).as[String]
                val dataSets: Seq[DataSet] = dataSetStr.parseJson.convertTo[Seq[DataSet]]
                
                val chosenSets: Seq[DataSet] = dataSets
                    .filter(ds => ds.active && !ds.dataUse.collaboratorRequired.getOrElse(false))
                    .groupBy(_.dacId)
                    .map(_._2.head)
                    .take(2)
                    .toSeq

                val setIds: Seq[Int] = chosenSets.map(s => s.dataSetId).toSeq
                val entrySets: Seq[DataSetEntry] = chosenSets.map(s => DataSetEntryBuilder.fromDataSet(s)).toSeq
                
                val dacUserId: Int = session("dacUserId").as[Int]
                val dar: DataAccessRequestDraft = DataAccessRequestDraft(
                    userId = dacUserId, 
                    datasets = entrySets, 
                    datasetId = setIds
                )
                val newSession: Session = session.set("darRequestBody", dar.toJson.compactPrint)
                newSession.set("dataSetIds", setIds)
            }
            .exec(
                Requests.Dar.partialSave(201, """${darRequestBody}""", additionalHeaders)
            )
        }
    }
}
