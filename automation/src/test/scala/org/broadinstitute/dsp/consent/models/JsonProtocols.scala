package org.broadinstitute.dsp.consent.models

import spray.json._
import DefaultJsonProtocol._
import org.broadinstitute.dsp.consent.models.UserModels._
import org.broadinstitute.dsp.consent.models.WhiteListModels._
import org.broadinstitute.dsp.consent.models.ResearcherModels._
import org.broadinstitute.dsp.consent.models.DataSetModels._
import org.broadinstitute.dsp.consent.models.DataUseModels._
import org.broadinstitute.dsp.consent.models.DataAccessRequestModels._
import org.broadinstitute.dsp.consent.models.NihModels._
import org.broadinstitute.dsp.consent.models.DacModels._
import org.broadinstitute.dsp.consent.models.ElectionModels._


import scala.util.{Failure, Success, Try}

object JsonProtocols extends DefaultJsonProtocol {
    implicit val researcherPropertyFormat = jsonFormat4(ResearcherProperty)
    implicit val userRoleFormat = jsonFormat5(UserRole)
    implicit val whiteListEntryFormat = jsonFormat8(WhiteListEntry) 
    implicit val dataSetPropertyFormat = jsonFormat2(DataSetProperty)
    implicit val dataUseFormat = jsonFormat5(DataUse)
    implicit val dataSetEntryFormat = jsonFormat4(DataSetEntry)
    implicit val collaboratorFormat = jsonFormat6(Collaborator)
    implicit val dataSetDetailFormat = jsonFormat3(DataSetDetailEntry)
    implicit val ontologyEntryFormat = jsonFormat4(OntologyEntry)
    implicit val userFormat = jsonFormat10(User)
    implicit val dataSetFormat = jsonFormat16(DataSet)
    implicit val dataAccessRequestDraftFormat = jsonFormat3(DataAccessRequestDraft)
    implicit val fireCloudProfileFormat = jsonFormat11(FireCloudProfile)
    implicit val nihUserFormat = jsonFormat4(NihUserAccount)
    implicit val nihVerify = jsonFormat1(NihVerify)
    implicit val dacFormat = jsonFormat8(Dac)
    implicit val electionStatusFormat = jsonFormat2(ElectionStatus)

    def optionalEntryReader[T](fieldName: String, data: Map[String,JsValue], converter: JsValue => T, default: T): T = {
        data.getOrElse(fieldName, None) match {
            case j:JsValue => Try(converter(j)).toOption.getOrElse(
                throw DeserializationException(s"unexpected json type for $fieldName")
            )
            case None => default
        }
    }

    implicit object DataAccessRequestManageFormat extends JsonFormat[DataAccessRequestManage] {
        def write(darm: DataAccessRequestManage) = {
            var map = collection.mutable.Map[String, JsValue]()
            val manualList = List("errors")
            darm.getClass.getDeclaredFields
                .filterNot(f => manualList.exists(_ == f.getName))
                .foreach { f =>
                    f.setAccessible(true)
                    f.get(darm) match {
                        case Some(x: Boolean) => map += f.getName -> x.toJson
                        case Some(y: String) => map += f.getName -> y.toJson
                        case Some(l: Long) => map += f.getName -> l.toJson
                        case Some(i: Int) => map += f.getName -> i.toJson
                        case Some(u: User) => map += f.getName -> u.toJson
                        case Some(d: Dac) => map += f.getName -> d.toJson
                        case _ => map += f.getName -> JsNull
                    }
                }

            if (!darm.errors.isEmpty)
                map += ("errors" -> darm.errors.toJson)

            JsObject(map.toMap)
        }

        def read(value: JsValue): DataAccessRequestManage = {
            val fields = value.asJsObject.fields
            DataAccessRequestManage(
                referenceId = optionalEntryReader("referenceId", fields, _.convertTo[Option[String]], None),
                logged = optionalEntryReader("logged", fields, _.convertTo[Option[String]], None),
                alreadyVoted = optionalEntryReader("alreadyVoted", fields, _.convertTo[Option[Boolean]], None),
                isReminderSent = optionalEntryReader("isReminderSent", fields, _.convertTo[Option[Boolean]], None),
                isFinalVote = optionalEntryReader("isFinalVote", fields, _.convertTo[Option[Boolean]], None),
                voteId = optionalEntryReader("voteId", fields, _.convertTo[Option[Int]], None),
                totalVotes = optionalEntryReader("totalVotes", fields, _.convertTo[Option[Int]], None),
                votesLogged = optionalEntryReader("votesLogged", fields, _.convertTo[Option[Int]], None),
                rpElectionId = optionalEntryReader("rpElectionId", fields, _.convertTo[Option[Int]], None),
                rpVoteId = optionalEntryReader("rpVoteId", fields, _.convertTo[Option[Int]], None),
                consentGroupName = optionalEntryReader("consentGroupName", fields, _.convertTo[Option[String]], None),
                dac = optionalEntryReader("dac", fields, _.convertTo[Option[Dac]], None),
                electionStatus = optionalEntryReader("electionStatus", fields, _.convertTo[Option[String]], None),
                status = optionalEntryReader("status", fields, _.convertTo[Option[String]], None),
                rus = optionalEntryReader("rus", fields, _.convertTo[Option[String]], None),
                dataRequestId = optionalEntryReader("dataRequestId", fields, _.convertTo[Option[String]], None),
                projectTitle = optionalEntryReader("projectTitle", fields, _.convertTo[Option[String]], None),
                frontEndId = optionalEntryReader("frontEndId", fields, _.convertTo[Option[String]], None),
                electionId = optionalEntryReader("electionId", fields, _.convertTo[Option[Int]], None),
                createDate = optionalEntryReader("createDate", fields, _.convertTo[Option[Long]], None),
                sortDate = optionalEntryReader("sortDate", fields, _.convertTo[Option[Long]], None),
                electionVote = optionalEntryReader("electionVote", fields, _.convertTo[Option[Boolean]], None),
                isCanceled = optionalEntryReader("isCanceled", fields, _.convertTo[Option[Boolean]], None),
                needsApproval = optionalEntryReader("needsApproval", fields, _.convertTo[Option[Boolean]], None),
                dataSetElectionResult = optionalEntryReader("dataSetElectionResult", fields, _.convertTo[Option[String]], None),
                datasetId = optionalEntryReader("datasetId", fields, _.convertTo[Option[Int]], None),
                dacId = optionalEntryReader("dacId", fields, _.convertTo[Option[Int]], None),
                errors = optionalEntryReader("errors", fields, _.convertTo[Option[Seq[String]]], None),
                ownerUser = optionalEntryReader("ownerUser", fields, _.convertTo[Option[User]], None)
            )
        }
    }

    implicit object ResearcherInfoFormat extends JsonFormat[ResearcherInfo] {
        def write(ri: ResearcherInfo) = {
            var map = collection.mutable.Map[String, JsValue]()
            val manualList = List("libraryCards", "libraryCardEntries")
            ri.getClass.getDeclaredFields
              .filterNot(f => manualList.exists(_ == f.getName)).foreach { f =>
                f.setAccessible(true)
                f.get(ri) match {
                    case Some(x: Boolean) => map += f.getName -> x.toJson
                    case Some(y: String) => map += f.getName -> y.toJson
                    case Some(z: Long) => map += f.getName -> z.toJson
                    case Some(i: Int) => map += f.getName -> i.toJson
                    case _ => map += f.getName -> JsNull
                }
              }

            if (!ri.libraryCards.isEmpty)
              map += ("libraryCards" -> ri.libraryCards.toJson)
            if (!ri.libraryCardEntries.isEmpty)
              map += ("libraryCardEntries" -> ri.libraryCardEntries.toJson)

            JsObject(map.toMap)
        }

        def read(value: JsValue): ResearcherInfo = {
            val fields = value.asJsObject.fields
            ResearcherInfo(
                optionalEntryReader("profileName", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("academicEmail", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("institution", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("department", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("address1", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("city", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("zipcode", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("isThePI", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("country", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("division", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("address2", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("state", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("eRACommonsID", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("pubmedID", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("scientificURL", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("havePI", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("piName", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("piEmail", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("piERACommonsID", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("completed", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("investigator", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("eraExpiration", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("eraAuthorized", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("nihUsername", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("linkedIn", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("researcherGate", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("orcid", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("checkNotifications", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("libraryCards", fields, _.convertTo[Option[Seq[String]]], None),
                optionalEntryReader("libraryCardEntries", fields, _.convertTo[Option[Seq[WhiteListEntry]]], None)
            )
        }
    }

    implicit object DataAccessRequestDataFormat extends JsonFormat[DataAccessRequestData] {
        def write(dar: DataAccessRequestData) = {
            var map = collection.mutable.Map[String, JsValue]()
            val manualList = List("ontologies", "labCollaborators", "internalCollaborators", "externalCollaborators", "datasets", "datasetDetail")

            dar.getClass.getDeclaredFields
                .filterNot(f => manualList.exists(_ == f.getName)).foreach { f =>
                    f.setAccessible(true)
                    f.get(dar) match {
                        case Some(x: Boolean) => map += f.getName -> x.toJson
                        case Some(y: String) => map += f.getName -> y.toJson
                        case Some(z: Long) => map += f.getName -> z.toJson
                        case Some((h: String) :: tail) => map += f.getName -> (h +: tail.collect { case z: String => z }).toJson
                        case _ => map += f.getName -> JsNull
                    }
                }

            if (!dar.ontologies.isEmpty)
                map += ("ontologies" -> dar.ontologies.toJson)
            if (!dar.labCollaborators.isEmpty)
                map += ("labCollaborators" -> dar.labCollaborators.toJson)
            if (!dar.internalCollaborators.isEmpty)
                map += ("internalCollaborators" -> dar.internalCollaborators.toJson)
            if (!dar.externalCollaborators.isEmpty)
                map += ("externalCollaborators" -> dar.externalCollaborators.toJson)
            if (!dar.datasets.isEmpty)
                map += ("datasets" -> dar.datasets.toJson)
            if (!dar.datasetIds.isEmpty)
                map += ("datasetIds" -> dar.datasetIds.toJson)
            if (!dar.datasetDetail.isEmpty)
                map += ("datasetDetail" -> dar.datasetDetail.toJson)

            JsObject(map.toMap)
        }

        def read(value: JsValue): DataAccessRequestData = {
            val fields = value.asJsObject.fields
            DataAccessRequestData(
                optionalEntryReader("referenceId", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("investigator", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("institution", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("department", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("division", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("address1", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("address2", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("city", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("zipCode", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("state", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("country", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("projectTitle", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("checkCollaborator", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("researcher", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("userId", fields, _.convertTo[Option[Int]], None),
                optionalEntryReader("isThePi", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("havePi", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("piEmail", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("profileName", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("pubmedId", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("scientificUrl", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("eraExpiration", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("academicEmail", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("eraAuthorized", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("nihUsername", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("linkedIn", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("orcid", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("researcherGate", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("rus", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("nonTechRus", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("diseases", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("methods", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("controls", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("population", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("other", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("otherText", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("ontologies", fields, _.convertTo[Option[Seq[OntologyEntry]]], None),
                optionalEntryReader("forProfit", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("oneGender", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("gender", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("pediatric", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("illegalBehavior", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("addiction", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("sexualDiseases", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("stigmatizedDiseases", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("vulnerablePopulation", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("populationMigration", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("psychiatricTraits", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("notHealth", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("hmb", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("status", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("poa", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("datasets", fields, _.convertTo[Option[Seq[DataSetEntry]]], None),
                optionalEntryReader("darCode", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("partialDarCode", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("restriction", fields, jv => None, None),
                optionalEntryReader("validRestriction", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("translatedUseRestriction", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("createDate", fields, _.convertTo[Option[Long]], None),
                optionalEntryReader("sortDate", fields, _.convertTo[Option[Long]], None),
                optionalEntryReader("datasetIds", fields, _.convertTo[Option[Seq[Int]]], None),
                optionalEntryReader("datasetDetail", fields, _.convertTo[Option[Seq[DataSetDetailEntry]]], None),
                optionalEntryReader("anvilUse", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("cloudUse", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("localUse", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("cloudProvider", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("cloudProviderType", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("cloudProviderDescription", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("geneticStudiesOnly", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("irb", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("irbDocumentLocation", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("irbDocumentName", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("irbProtocolExpiration", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("itDirector", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("signingOfficial", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("publication", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("collaboration", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("collaborationLetterLocation", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("collaborationLetterName", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("forensicActivities", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("sharingDistribution", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("labCollaborators", fields, _.convertTo[Option[Seq[Collaborator]]], None),
                optionalEntryReader("internalCollaborators", fields, _.convertTo[Option[Seq[Collaborator]]], None),
                optionalEntryReader("externalCollaborators", fields, _.convertTo[Option[Seq[Collaborator]]], None),
                optionalEntryReader("dsAcknowledgement", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("gsoAcknowledgement", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("pubAcknowledgement", fields, _.convertTo[Option[Boolean]], None)
            )
        }
    }

    implicit val dataAccessRequestFormat = jsonFormat9(DataAccessRequest)
}