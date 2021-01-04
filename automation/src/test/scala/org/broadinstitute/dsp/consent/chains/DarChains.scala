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
            val referenceId = session("darReferenceId").as[String]

            val dsIds = session("dataSetIds").as[Seq[Int]]
            var datasetList = collection.mutable.ListBuffer[DataSet]()
            for (id <- 0 to 1) {
                val dataSetStr = session(Requests.DataSet.dataSetsByDataSetId + id).as[String]
                datasetList += dataSetStr.parseJson.convertTo[DataSet]
            }

            val partialUpdate = DarService.createDar(partialDar, researcherInfo, partialDar.userId, referenceId, dsIds, datasetList.toSeq, false)
            val newSession = session.set("dataAccessRequestJson", partialUpdate.toJson.compactPrint)
            val finalDar = DarService.createDar(partialDar, researcherInfo, partialDar.userId, referenceId, dsIds, datasetList.toSeq, true)
            newSession.set("dataAccessRequestSubmit", finalDar.toJson.compactPrint)
        }
        .exec(
            Requests.Dar.updatePartial(200, "${darReferenceId}", "${dataAccessRequestJson}", additionalHeaders)
        )
        .exec(
            Requests.Dar.saveFinal(201, "${dataAccessRequestSubmit}", additionalHeaders)
        )
    }
}