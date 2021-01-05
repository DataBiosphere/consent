package org.broadinstitute.dsp.consent.services

import org.broadinstitute.dsp.consent.models.JsonProtocols
import org.broadinstitute.dsp.consent.models.DataSetModels._
import org.broadinstitute.dsp.consent.models.ResearcherModels._
import org.broadinstitute.dsp.consent.models.DataAccessRequestModels._
import org.broadinstitute.dsp.consent.models.UserModels._
import spray.json._
import DefaultJsonProtocol._

object DarService {
    def createDar(dar: DataAccessRequest,
                  researcherInfo: ResearcherInfo, 
                  userId: Int, 
                  referenceId: String, 
                  dataSetIds: Seq[Int], 
                  datasets: Seq[DataSet], 
                  finalSubmit: Boolean = false): DataAccessRequest = {
        implicit val dataAccessRequestDataFormat = JsonProtocols.DataAccessRequestDataFormat
        implicit val dataSetEntryFormat = JsonProtocols.dataSetEntryFormat
        var dataMap = collection.mutable.Map[String, JsValue]()
        var darMap = collection.mutable.Map[String, JsValue]()

        dataMap += ("datasets" -> JsArray(datasets.map(DataSetEntryBuilder.fromDataSet(_).toJson).toVector))
        dataMap += ("datasetIds" -> JsArray(dataSetIds.map(_.toJson).toVector))

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

    def setManageRolesByOwner(manageDars: Seq[DataAccessRequestManage]): Seq[DataAccessRequestManage] = {
        manageDars.map { dar =>
            dar.ownerUser match {
                case Some(u: User) => {
                    if (u.roles.getOrElse(List()).exists(_.name == "Researcher"))
                        dar.copy(status = Some(u.status.getOrElse("")))
                    else
                        dar
                }
                case _ => dar
            }
        }
    }

    def getPendingDARsByMostRecent(manageDars: Seq[DataAccessRequestManage], limit: Int = 2): Seq[DataAccessRequestManage] = {
        manageDars.filter(md => md.electionStatus.getOrElse("") != "Open" && md.electionStatus.getOrElse("") != "Final"
            && md.electionStatus.getOrElse("") != "Pending Approval")
            .sortWith((a, b) => a.createDate.getOrElse(0L) > b.createDate.getOrElse(0L))
            .take(limit)
    }
}