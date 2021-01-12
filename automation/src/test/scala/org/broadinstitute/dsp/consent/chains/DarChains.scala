package org.broadinstitute.dsp.consent.chains

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import org.broadinstitute.dsp.consent.requests.Requests
import org.broadinstitute.dsp.consent.models.DataAccessRequestModels._
import org.broadinstitute.dsp.consent.models.JsonProtocols
import org.broadinstitute.dsp.consent.models.ResearcherModels._
import org.broadinstitute.dsp.consent.models.DataSetModels._
import org.broadinstitute.dsp.consent.models.DataUseModels._
import org.broadinstitute.dsp.consent.models.ConsentModels._
import spray.json._
import DefaultJsonProtocol._
import org.broadinstitute.dsp.consent.services.DarService
import io.netty.handler.codec.http.HttpResponseStatus._

object DarChains {
    def darApplicationPageLoad(additionalHeaders: Map[String, String]): ChainBuilder = exec(
        Requests.Dar.getPartial(OK.code, "${darReferenceId}", additionalHeaders)
    )
    .exec(
        Requests.DataSet.getDataSetsByDataSetIds(
            200, 
            additionalHeaders,
            Requests.DataSet.getDataSetsByDataSetId(OK.code, "${dataSetIds(0)}", additionalHeaders, 0),
            Requests.DataSet.getDataSetsByDataSetId(OK.code, "${dataSetIds(1)}", additionalHeaders, 1))
    )
    .exec(
        Requests.Researcher.getResearcherProperties(OK.code, "${dacUserId}", additionalHeaders)
    )

    def finalDarSubmit(additionalHeaders: Map[String, String]): ChainBuilder = {
        exitBlockOnFail {
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
                Requests.Dar.updatePartial(OK.code, "${darReferenceId}", "${dataAccessRequestJson}", additionalHeaders)
            )
            .exec(
                Requests.Dar.saveFinal(CREATED.code, "${dataAccessRequestSubmit}", additionalHeaders)
            )
        }
    }

    def describeDar(additionalHeaders: Map[String, String]): ChainBuilder = {
        exitBlockOnFail {
            exec(
                Requests.Dar.getPartial(OK.code, "${darReferenceId}", additionalHeaders)
            )
            .exec { session =>
                implicit val darFormat: JsonProtocols.DataAccessRequestDataFormat.type = JsonProtocols.DataAccessRequestDataFormat
                val darStr: String = session(Requests.Dar.darPartialJson).as[String]
                val dar: DataAccessRequestData = darStr.parseJson.convertTo[DataAccessRequestData]

                session.set("dataAccessUserId", dar.userId.getOrElse(0))
            }
            .exec(
                Requests.User.getById(OK.code, "${dataAccessUserId}", additionalHeaders)
            )
        }
    }

    def describeDarWithConsent(additionalHeaders: Map[String, String]): ChainBuilder = {
        exitBlockOnFail {
            exec(
                describeDar(additionalHeaders)
            )
            .exec(
                Requests.Dar.getConsent(OK.code, "${darReferenceId}", additionalHeaders)
            )
            .exec { session =>
                implicit val consentFormat: JsonProtocols.consentFormat.type = JsonProtocols.consentFormat
                implicit val dataUseFormat: JsonProtocols.dataUseFormat.type = JsonProtocols.dataUseFormat
                val consentStr: String = session(Requests.Dar.darConsentResponse).as[String]
                val consent: Consent = consentStr.parseJson.convertTo[Consent]
                
                session.set("consentDataUse", consent.dataUse.getOrElse(DataUseBuilder.empty).toJson.compactPrint)
            }
            .exec(
                Requests.Dar.translateDataUse(OK.code, "${consentDataUse}", additionalHeaders)
            )
        }
    }
}