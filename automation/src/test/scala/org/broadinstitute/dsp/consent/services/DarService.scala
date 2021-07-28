package org.broadinstitute.dsp.consent.services

import org.broadinstitute.dsp.consent.models.DataAccessRequestModels._
import org.broadinstitute.dsp.consent.models.DataSetModels._
import org.broadinstitute.dsp.consent.models.JsonProtocols
import org.broadinstitute.dsp.consent.models.ResearcherModels._
import spray.json.DefaultJsonProtocol._
import spray.json._

object DarService {
    val projectTitle: String = "Test Automation Project"

    def createDar(dar: DataAccessRequest,
                  researcherInfo: ResearcherInfo,
                  userId: Int,
                  referenceId: String,
                  dataSetIds: Seq[Int],
                  datasets: Seq[DataSet],
                  finalSubmit: Boolean = false): DataAccessRequest = {
        implicit val dataAccessRequestDataFormat: JsonProtocols.DataAccessRequestDataFormat.type = JsonProtocols.DataAccessRequestDataFormat
        implicit val dataSetEntryFormat: JsonProtocols.dataSetEntryFormat.type = JsonProtocols.dataSetEntryFormat
        val dataMap: collection.mutable.Map[String, JsValue] = collection.mutable.Map[String, JsValue]()

        dataMap += ("datasets" -> JsArray(datasets.map(DataSetEntryBuilder.fromDataSet(_).toJson).toVector))
        dataMap += ("datasetIds" -> JsArray(dataSetIds.map(_.toJson).toVector))

        val piName: String = researcherInfo.piName.getOrElse("")
        val profileName: String = researcherInfo.profileName.getOrElse("Test Researcher")
        val isThePI: String = researcherInfo.isThePI.getOrElse("false")

        dataMap += ("researcher" -> JsString(profileName))

        if (piName == "" && isThePI == "true") {
            dataMap += ("investigator" -> JsString(profileName))
        } else if (piName == "" && isThePI == "false") {
            dataMap += ("investigator" -> JsString("--"))
        } else {
            dataMap += ("investigator" -> JsString(piName))
        }

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
        dataMap += ("userId" -> JsNumber(userId))
        dataMap += ("itDirector" -> JsString("Test IT Director"))
        dataMap += ("signingOfficial" -> JsString("Test Signing Official"))
        dataMap += ("checkCollaborator" -> JsBoolean(false))
        dataMap += ("anvilUse" -> JsBoolean(false))
        dataMap += ("cloudUse" -> JsBoolean(false))
        dataMap += ("localUse" -> JsBoolean(true))
        dataMap += ("projectTitle" -> JsString(projectTitle))
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

        DataAccessRequest(
            id = dar.id,
            referenceId = dar.referenceId,
            data = dataMap.toMap.toJson.convertTo[Option[DataAccessRequestData]],
            draft = if (finalSubmit) Some(false) else dar.draft,
            userId = dar.userId,
            createDate = if (finalSubmit) None else dar.createDate,
            sortDate = if (finalSubmit) None else dar.sortDate,
            submissionDate = dar.submissionDate,
            updateDate = dar.updateDate
        )
    }

    def getPendingDARsByMostRecent(manageDars: Seq[DataAccessRequestManage], limit: Int = 2): Seq[DataAccessRequestManage] = {
        manageDars.filter(md => md.election.get.status.getOrElse("") != "Open" && md.election.get.status.getOrElse("") != "Final"
          && md.election.get.status.getOrElse("") != "Pending Approval")
          .sortWith((a, b) => a.election.get.createDate.getOrElse(0L) > b.election.get.createDate.getOrElse(0L))
          .take(limit)
    }
}