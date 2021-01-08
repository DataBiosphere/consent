package org.broadinstitute.dsp.consent.chains

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import org.broadinstitute.dsp.consent.requests.Requests
import org.broadinstitute.dsp.consent.models.DataAccessRequestModels._
import org.broadinstitute.dsp.consent.models.JsonProtocols
import org.broadinstitute.dsp.consent.models.ResearcherModels._
import org.broadinstitute.dsp.consent.models.DataSetModels._
import spray.json._
import DefaultJsonProtocol._
import org.broadinstitute.dsp.consent.services.DarService

object DarChains {
    def darApplicationPageLoad(additionalHeaders: Map[String, String]): ChainBuilder = exec(
        Requests.Dar.getPartial(200, "${darReferenceId}", additionalHeaders)
    )
    .exec(
        Requests.DataSet.getDataSetsByDataSetIds(
            200, 
            additionalHeaders,
            Requests.DataSet.getDataSetsByDataSetId(200, "${dataSetIds(0)}", additionalHeaders, 0),
            Requests.DataSet.getDataSetsByDataSetId(200, "${dataSetIds(1)}", additionalHeaders, 1))
    )
    .exec(
        Requests.Researcher.getResearcherProperties(200, "${dacUserId}", additionalHeaders)
    )

    def finalDarSubmit(additionalHeaders: Map[String, String]): ChainBuilder = {
        exec { session =>
            implicit val researchFormat: JsonProtocols.researcherPropertyFormat.type = JsonProtocols.researcherPropertyFormat
            implicit val researcherFormat: JsonProtocols.ResearcherInfoFormat.type = JsonProtocols.ResearcherInfoFormat
            implicit val dataAccessRequestDataFormat: JsonProtocols.DataAccessRequestDataFormat.type = JsonProtocols.DataAccessRequestDataFormat
            implicit val dataAccessFormat: JsonProtocols.dataAccessRequestFormat.type = JsonProtocols.dataAccessRequestFormat
            implicit val dataSetFormat: JsonProtocols.dataSetFormat.type = JsonProtocols.dataSetFormat
            implicit val dataSetEntryFormat: JsonProtocols.dataSetEntryFormat.type = JsonProtocols.dataSetEntryFormat

            val researcherPropsStr: String = session(Requests.Researcher.researcherPropertiesResponse).as[String]
            val researcherInfo: ResearcherInfo = researcherPropsStr.parseJson.convertTo[ResearcherInfo]
            val partialDar: DataAccessRequest = session(Requests.Dar.darPartialJson).as[String]
                .parseJson.convertTo[DataAccessRequest];
            val referenceId: String = session("darReferenceId").as[String]

            val indices: Seq[Int] = Seq(0, 1)
            val dataSets: Seq[DataSet] = indices.map(idx => {
                val dataSetStr: String = session(Requests.DataSet.dataSetsByDataSetId + idx).as[String]
                val dataSet: DataSet = dataSetStr.parseJson.convertTo[DataSet]
                dataSet
            }).toSeq
            val dsIds:Seq[Int] = dataSets.map(_.dataSetId).toSeq

            val partialUpdate: DataAccessRequest = DarService.createDar(partialDar, researcherInfo, partialDar.userId, referenceId, dsIds, dataSets, false)
            val newSession = session.set("dataAccessRequestJson", partialUpdate.data.toJson.compactPrint)
            val finalDar: DataAccessRequest = DarService.createDar(partialDar, researcherInfo, partialDar.userId, referenceId, dsIds, dataSets, true)
            newSession.set("dataAccessRequestSubmit", finalDar.data.toJson.compactPrint)
        }
        .exec(
            Requests.Dar.updatePartial(200, "${darReferenceId}", "${dataAccessRequestJson}", additionalHeaders)
        )
        .exec(
            Requests.Dar.saveFinal(201, "${dataAccessRequestSubmit}", additionalHeaders)
        )
    }
}