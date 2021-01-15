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
import org.broadinstitute.dsp.consent.models.PendingModels._
import org.broadinstitute.dsp.consent.models.ConsentModels._
import org.broadinstitute.dsp.consent.models.MatchModels._


import scala.util.{Failure, Success, Try}

object JsonProtocols extends DefaultJsonProtocol {
    implicit val researcherPropertyFormat: JsonFormat[ResearcherProperty] = jsonFormat4(ResearcherProperty)
    implicit val userRoleFormat: JsonFormat[UserRole] = jsonFormat5(UserRole)
    implicit val whiteListEntryFormat: JsonFormat[WhiteListEntry] = jsonFormat8(WhiteListEntry) 
    implicit val dataSetPropertyFormat: JsonFormat[DataSetProperty] = jsonFormat2(DataSetProperty)
    implicit val dataUseFormat: JsonFormat[DataUse] = jsonFormat5(DataUse)
    implicit val dataSetEntryFormat: JsonFormat[DataSetEntry] = jsonFormat4(DataSetEntry)
    implicit val collaboratorFormat: JsonFormat[Collaborator] = jsonFormat6(Collaborator)
    implicit val dataSetDetailFormat: JsonFormat[DataSetDetailEntry] = jsonFormat3(DataSetDetailEntry)
    implicit val ontologyEntryFormat: JsonFormat[OntologyEntry] = jsonFormat4(OntologyEntry)
    implicit val userFormat: JsonFormat[User] = jsonFormat10(User)
    implicit val dataSetFormat: JsonFormat[DataSet] = jsonFormat16(DataSet)
    implicit val dataAccessRequestDraftFormat: JsonFormat[DataAccessRequestDraft] = jsonFormat3(DataAccessRequestDraft)
    implicit val fireCloudProfileFormat: JsonFormat[FireCloudProfile] = jsonFormat11(FireCloudProfile)
    implicit val nihUserFormat: JsonFormat[NihUserAccount] = jsonFormat4(NihUserAccount)
    implicit val nihVerify: JsonFormat[NihVerify] = jsonFormat1(NihVerify)
    implicit val dacFormat: JsonFormat[Dac] = jsonFormat8(Dac)
    implicit val electionStatusFormat: JsonFormat[ElectionStatus] = jsonFormat2(ElectionStatus)
    implicit val pendingCaseFormat: JsonFormat[PendingCase] = jsonFormat18(PendingCase)
    implicit val consentFormat: JsonFormat[Consent] = jsonFormat15(Consent)
    implicit val votePostFormat: JsonFormat[VotePostObject] = jsonFormat4(VotePostObject)
    implicit val electionFormat: JsonFormat[Election] = jsonFormat18(Election)
    implicit val electionReviewVoteFormat: JsonFormat[ElectionReviewVote] = jsonFormat3(ElectionReviewVote)
    implicit val electionReviewFormat: JsonFormat[ElectionReview] = jsonFormat6(ElectionReview)

    def optionalEntryReader[T](fieldName: String, data: Map[String,JsValue], converter: JsValue => T, default: T): T = {
        data.getOrElse(fieldName, None) match {
            case j:JsValue => Try(converter(j)).toOption.getOrElse(
                throw DeserializationException(s"unexpected json type for $fieldName")
            )
            case None => default
        }
    }

    implicit object MatchFormat extends JsonFormat[Match] {
        def write(matchObj: Match) = {
            var map = collection.mutable.Map[String, JsValue]()
            val manualList = List("cMatch")
            matchObj.getClass.getDeclaredFields
                .filterNot(f => manualList.contains(f.getName))
                .foreach { f =>
                    f.setAccessible(true)
                    f.get(matchObj) match {
                        case Some(x: Boolean) => map += f.getName -> x.toJson
                        case Some(y: String) => map += f.getName -> y.toJson
                        case Some(l: Long) => map += f.getName -> l.toJson
                        case Some(i: Int) => map += f.getName -> i.toJson
                        case _ => map += f.getName -> JsNull
                    }
                }

            map += ("match" -> JsBoolean(matchObj.cMatch))
            
            JsObject(map.toMap)
        }

        def read(value: JsValue): Match = {
            val fields = value.asJsObject.fields
            Match(
                id = optionalEntryReader("id", fields, _.convertTo[Int], 0),
                consent = optionalEntryReader("consent", fields, _.convertTo[String], ""),
                purpose = optionalEntryReader("purpose", fields, _.convertTo[String], ""),
                cMatch = optionalEntryReader("match", fields, _.convertTo[Boolean], false),
                failed = optionalEntryReader("failed", fields, _.convertTo[Boolean], false),
                createDate = optionalEntryReader("createDate", fields, _.convertTo[Option[Long]], None)
            )
        }
    }

    implicit object VoteFormat extends JsonFormat[Vote] {
        def write(vote: Vote) = {
            var map = collection.mutable.Map[String, JsValue]()
            val manualList = List("vType")
            vote.getClass.getDeclaredFields
                .filterNot(f => manualList.contains(f.getName))
                .foreach { f =>
                    f.setAccessible(true)
                    f.get(vote) match {
                        case Some(x: Boolean) => map += f.getName -> x.toJson
                        case Some(y: String) => map += f.getName -> y.toJson
                        case Some(l: Long) => map += f.getName -> l.toJson
                        case Some(i: Int) => map += f.getName -> i.toJson
                        case _ => map += f.getName -> JsNull
                    }
                }

            map += ("type" -> JsString(vote.vType.getOrElse("")))
            
            JsObject(map.toMap)
        }

        def read(value: JsValue): Vote = {
            val fields = value.asJsObject.fields
            Vote(
                voteId = optionalEntryReader("voteId", fields, _.convertTo[Option[Int]], None),
                vote = optionalEntryReader("vote", fields, _.convertTo[Option[Boolean]], None),
                dacUserId = optionalEntryReader("dacUserId", fields, _.convertTo[Option[Int]], None),
                createDate = optionalEntryReader("createDate", fields, _.convertTo[Option[Long]], None),
                updateDate = optionalEntryReader("updateDate", fields, _.convertTo[Option[Long]], None),
                electionId = optionalEntryReader("electionId", fields, _.convertTo[Option[Int]], None),
                rationale = optionalEntryReader("rationale", fields, _.convertTo[Option[String]], None),
                vType = optionalEntryReader("type", fields, _.convertTo[Option[String]], None),
                isReminderSent = optionalEntryReader("isReminderSent", fields, _.convertTo[Option[Boolean]], None),
                hasConcerns = optionalEntryReader("hasConcerns", fields, _.convertTo[Option[Boolean]], None)
            )
        }
    }

    implicit object DataAccessRequestManageFormat extends JsonFormat[DataAccessRequestManage] {
        def write(darm: DataAccessRequestManage) = {
            var map = collection.mutable.Map[String, JsValue]()
            val manualList = List("errors")
            darm.getClass.getDeclaredFields
                .filterNot(f => manualList.contains(f.getName))
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

            if (darm.errors.isDefined)
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
              .filterNot(f => manualList.contains(f.getName)).foreach { f =>
                f.setAccessible(true)
                f.get(ri) match {
                    case Some(x: Boolean) => map += f.getName -> x.toJson
                    case Some(y: String) => map += f.getName -> y.toJson
                    case Some(z: Long) => map += f.getName -> z.toJson
                    case Some(i: Int) => map += f.getName -> i.toJson
                    case _ => map += f.getName -> JsNull
                }
              }

            if (ri.libraryCards.isDefined)
              map += ("libraryCards" -> ri.libraryCards.toJson)
            if (ri.libraryCardEntries.isDefined)
              map += ("libraryCardEntries" -> ri.libraryCardEntries.toJson)

            JsObject(map.toMap)
        }

        def read(value: JsValue): ResearcherInfo = {
            val fields = value.asJsObject.fields
            ResearcherInfo(
                profileName = optionalEntryReader("profileName", fields, _.convertTo[Option[String]], None),
                academicEmail = optionalEntryReader("academicEmail", fields, _.convertTo[Option[String]], None),
                institution = optionalEntryReader("institution", fields, _.convertTo[Option[String]], None),
                department = optionalEntryReader("department", fields, _.convertTo[Option[String]], None),
                address1 = optionalEntryReader("address1", fields, _.convertTo[Option[String]], None),
                city = optionalEntryReader("city", fields, _.convertTo[Option[String]], None),
                zipcode = optionalEntryReader("zipcode", fields, _.convertTo[Option[String]], None),
                isThePI = optionalEntryReader("isThePI", fields, _.convertTo[Option[String]], None),
                country = optionalEntryReader("country", fields, _.convertTo[Option[String]], None),
                division = optionalEntryReader("division", fields, _.convertTo[Option[String]], None),
                address2 = optionalEntryReader("address2", fields, _.convertTo[Option[String]], None),
                state = optionalEntryReader("state", fields, _.convertTo[Option[String]], None),
                eRACommonsID = optionalEntryReader("eRACommonsID", fields, _.convertTo[Option[String]], None),
                pubmedID = optionalEntryReader("pubmedID", fields, _.convertTo[Option[String]], None),
                scientificURL = optionalEntryReader("scientificURL", fields, _.convertTo[Option[String]], None),
                havePI = optionalEntryReader("havePI", fields, _.convertTo[Option[String]], None),
                piName = optionalEntryReader("piName", fields, _.convertTo[Option[String]], None),
                piEmail = optionalEntryReader("piEmail", fields, _.convertTo[Option[String]], None),
                piERACommonsID = optionalEntryReader("piERACommonsID", fields, _.convertTo[Option[String]], None),
                completed = optionalEntryReader("completed", fields, _.convertTo[Option[String]], None),
                investigator = optionalEntryReader("investigator", fields, _.convertTo[Option[String]], None),
                eraExpiration = optionalEntryReader("eraExpiration", fields, _.convertTo[Option[String]], None),
                eraAuthorized = optionalEntryReader("eraAuthorized", fields, _.convertTo[Option[String]], None),
                nihUsername = optionalEntryReader("nihUsername", fields, _.convertTo[Option[String]], None),
                linkedIn = optionalEntryReader("linkedIn", fields, _.convertTo[Option[String]], None),
                researcherGate = optionalEntryReader("researcherGate", fields, _.convertTo[Option[String]], None),
                orcid = optionalEntryReader("orcid", fields, _.convertTo[Option[String]], None),
                checkNotifications = optionalEntryReader("checkNotifications", fields, _.convertTo[Option[String]], None),
                libraryCards = optionalEntryReader("libraryCards", fields, _.convertTo[Option[Seq[String]]], None),
                libraryCardEntries = optionalEntryReader("libraryCardEntries", fields, _.convertTo[Option[Seq[WhiteListEntry]]], None)
            )
        }
    }

    implicit object DataAccessRequestDataFormat extends JsonFormat[DataAccessRequestData] {
        def write(dar: DataAccessRequestData) = {
            var map = collection.mutable.Map[String, JsValue]()
            val manualList = List("ontologies", "labCollaborators", "internalCollaborators", "externalCollaborators", "datasets", "datasetDetail")

            dar.getClass.getDeclaredFields
                .filterNot(f => manualList.contains(f.getName)).foreach { f =>
                    f.setAccessible(true)
                    f.get(dar) match {
                        case Some(x: Boolean) => map += f.getName -> x.toJson
                        case Some(y: String) => map += f.getName -> y.toJson
                        case Some(z: Long) => map += f.getName -> z.toJson
                        case Some((h: String) :: tail) => map += f.getName -> (h +: tail.collect { case z: String => z }).toJson
                        case _ => map += f.getName -> JsNull
                    }
                }

            if (dar.ontologies.isDefined)
                map += ("ontologies" -> dar.ontologies.toJson)
            if (dar.labCollaborators.isDefined)
                map += ("labCollaborators" -> dar.labCollaborators.toJson)
            if (dar.internalCollaborators.isDefined)
                map += ("internalCollaborators" -> dar.internalCollaborators.toJson)
            if (dar.externalCollaborators.isDefined)
                map += ("externalCollaborators" -> dar.externalCollaborators.toJson)
            if (dar.datasets.isDefined)
                map += ("datasets" -> dar.datasets.toJson)
            if (dar.datasetIds.isDefined)
                map += ("datasetIds" -> dar.datasetIds.toJson)
            if (dar.datasetDetail.isDefined)
                map += ("datasetDetail" -> dar.datasetDetail.toJson)

            JsObject(map.toMap)
        }

        def read(value: JsValue): DataAccessRequestData = {
            val fields = value.asJsObject.fields
            DataAccessRequestData(
                referenceId = optionalEntryReader("referenceId", fields, _.convertTo[Option[String]], None),
                investigator = optionalEntryReader("investigator", fields, _.convertTo[Option[String]], None),
                institution = optionalEntryReader("institution", fields, _.convertTo[Option[String]], None),
                department = optionalEntryReader("department", fields, _.convertTo[Option[String]], None),
                division = optionalEntryReader("division", fields, _.convertTo[Option[String]], None),
                address1 = optionalEntryReader("address1", fields, _.convertTo[Option[String]], None),
                address2 = optionalEntryReader("address2", fields, _.convertTo[Option[String]], None),
                city = optionalEntryReader("city", fields, _.convertTo[Option[String]], None),
                zipCode = optionalEntryReader("zipCode", fields, _.convertTo[Option[String]], None),
                state = optionalEntryReader("state", fields, _.convertTo[Option[String]], None),
                country = optionalEntryReader("country", fields, _.convertTo[Option[String]], None),
                projectTitle = optionalEntryReader("projectTitle", fields, _.convertTo[Option[String]], None),
                checkCollaborator = optionalEntryReader("checkCollaborator", fields, _.convertTo[Option[Boolean]], None),
                researcher = optionalEntryReader("researcher", fields, _.convertTo[Option[String]], None),
                userId = optionalEntryReader("userId", fields, _.convertTo[Option[Int]], None),
                isThePi = optionalEntryReader("isThePi", fields, _.convertTo[Option[String]], None),
                havePi = optionalEntryReader("havePi", fields, _.convertTo[Option[String]], None),
                piEmail = optionalEntryReader("piEmail", fields, _.convertTo[Option[String]], None),
                profileName = optionalEntryReader("profileName", fields, _.convertTo[Option[String]], None),
                pubmedId = optionalEntryReader("pubmedId", fields, _.convertTo[Option[String]], None),
                scientificUrl = optionalEntryReader("scientificUrl", fields, _.convertTo[Option[String]], None),
                eraExpiration = optionalEntryReader("eraExpiration", fields, _.convertTo[Option[Boolean]], None),
                academicEmail = optionalEntryReader("academicEmail", fields, _.convertTo[Option[String]], None),
                eraAuthorized = optionalEntryReader("eraAuthorized", fields, _.convertTo[Option[Boolean]], None),
                nihUsername = optionalEntryReader("nihUsername", fields, _.convertTo[Option[String]], None),
                linkedIn = optionalEntryReader("linkedIn", fields, _.convertTo[Option[String]], None),
                orcid = optionalEntryReader("orcid", fields, _.convertTo[Option[String]], None),
                researcherGate = optionalEntryReader("researcherGate", fields, _.convertTo[Option[String]], None),
                rus = optionalEntryReader("rus", fields, _.convertTo[Option[String]], None),
                nonTechRus = optionalEntryReader("nonTechRus", fields, _.convertTo[Option[String]], None),
                diseases = optionalEntryReader("diseases", fields, _.convertTo[Option[Boolean]], None),
                methods = optionalEntryReader("methods", fields, _.convertTo[Option[Boolean]], None),
                controls = optionalEntryReader("controls", fields, _.convertTo[Option[Boolean]], None),
                population = optionalEntryReader("population", fields, _.convertTo[Option[Boolean]], None),
                other = optionalEntryReader("other", fields, _.convertTo[Option[Boolean]], None),
                otherText = optionalEntryReader("otherText", fields, _.convertTo[Option[String]], None),
                ontologies = optionalEntryReader("ontologies", fields, _.convertTo[Option[Seq[OntologyEntry]]], None),
                forProfit = optionalEntryReader("forProfit", fields, _.convertTo[Option[Boolean]], None),
                oneGender = optionalEntryReader("oneGender", fields, _.convertTo[Option[Boolean]], None),
                gender = optionalEntryReader("gender", fields, _.convertTo[Option[String]], None),
                pediatric = optionalEntryReader("pediatric", fields, _.convertTo[Option[Boolean]], None),
                illegalBehavior = optionalEntryReader("illegalBehavior", fields, _.convertTo[Option[Boolean]], None),
                addiction = optionalEntryReader("addiction", fields, _.convertTo[Option[Boolean]], None),
                sexualDiseases = optionalEntryReader("sexualDiseases", fields, _.convertTo[Option[Boolean]], None),
                stigmatizedDiseases = optionalEntryReader("stigmatizedDiseases", fields, _.convertTo[Option[Boolean]], None),
                vulnerablePopulation = optionalEntryReader("vulnerablePopulation", fields, _.convertTo[Option[Boolean]], None),
                populationMigration = optionalEntryReader("populationMigration", fields, _.convertTo[Option[Boolean]], None),
                psychiatricTraits = optionalEntryReader("psychiatricTraits", fields, _.convertTo[Option[Boolean]], None),
                notHealth = optionalEntryReader("notHealth", fields, _.convertTo[Option[Boolean]], None),
                hmb = optionalEntryReader("hmb", fields, _.convertTo[Option[Boolean]], None),
                status = optionalEntryReader("status", fields, _.convertTo[Option[String]], None),
                poa = optionalEntryReader("poa", fields, _.convertTo[Option[Boolean]], None),
                datasets = optionalEntryReader("datasets", fields, _.convertTo[Option[Seq[DataSetEntry]]], None),
                darCode = optionalEntryReader("darCode", fields, _.convertTo[Option[String]], None),
                partialDarCode = optionalEntryReader("partialDarCode", fields, _.convertTo[Option[String]], None),
                restriction = optionalEntryReader("restriction", fields, jv => None, None),
                validRestriction = optionalEntryReader("validRestriction", fields, _.convertTo[Option[Boolean]], None),
                translatedUseRestriction = optionalEntryReader("translatedUseRestriction", fields, _.convertTo[Option[String]], None),
                createDate = optionalEntryReader("createDate", fields, _.convertTo[Option[Long]], None),
                sortDate = optionalEntryReader("sortDate", fields, _.convertTo[Option[Long]], None),
                datasetIds = optionalEntryReader("datasetIds", fields, _.convertTo[Option[Seq[Int]]], None),
                datasetDetail = optionalEntryReader("datasetDetail", fields, _.convertTo[Option[Seq[DataSetDetailEntry]]], None),
                anvilUse = optionalEntryReader("anvilUse", fields, _.convertTo[Option[Boolean]], None),
                cloudUse = optionalEntryReader("cloudUse", fields, _.convertTo[Option[Boolean]], None),
                localUse = optionalEntryReader("localUse", fields, _.convertTo[Option[Boolean]], None),
                cloudProvider = optionalEntryReader("cloudProvider", fields, _.convertTo[Option[String]], None),
                cloudProviderType = optionalEntryReader("cloudProviderType", fields, _.convertTo[Option[String]], None),
                cloudProviderDescription = optionalEntryReader("cloudProviderDescription", fields, _.convertTo[Option[String]], None),
                geneticStudiesOnly = optionalEntryReader("geneticStudiesOnly", fields, _.convertTo[Option[Boolean]], None),
                irb = optionalEntryReader("irb", fields, _.convertTo[Option[Boolean]], None),
                irbDocumentLocation = optionalEntryReader("irbDocumentLocation", fields, _.convertTo[Option[String]], None),
                irbDocumentName = optionalEntryReader("irbDocumentName", fields, _.convertTo[Option[String]], None),
                irbProtocolExpiration = optionalEntryReader("irbProtocolExpiration", fields, _.convertTo[Option[String]], None),
                itDirector = optionalEntryReader("itDirector", fields, _.convertTo[Option[String]], None),
                signingOfficial = optionalEntryReader("signingOfficial", fields, _.convertTo[Option[String]], None),
                publication = optionalEntryReader("publication", fields, _.convertTo[Option[Boolean]], None),
                collaboration = optionalEntryReader("collaboration", fields, _.convertTo[Option[Boolean]], None),
                collaborationLetterLocation = optionalEntryReader("collaborationLetterLocation", fields, _.convertTo[Option[String]], None),
                collaborationLetterName = optionalEntryReader("collaborationLetterName", fields, _.convertTo[Option[String]], None),
                forensicActivities = optionalEntryReader("forensicActivities", fields, _.convertTo[Option[Boolean]], None),
                sharingDistribution = optionalEntryReader("sharingDistribution", fields, _.convertTo[Option[Boolean]], None),
                labCollaborators = optionalEntryReader("labCollaborators", fields, _.convertTo[Option[Seq[Collaborator]]], None),
                internalCollaborators = optionalEntryReader("internalCollaborators", fields, _.convertTo[Option[Seq[Collaborator]]], None),
                externalCollaborators = optionalEntryReader("externalCollaborators", fields, _.convertTo[Option[Seq[Collaborator]]], None),
                dsAcknowledgement = optionalEntryReader("dsAcknowledgement", fields, _.convertTo[Option[Boolean]], None),
                gsoAcknowledgement = optionalEntryReader("gsoAcknowledgement", fields, _.convertTo[Option[Boolean]], None),
                pubAcknowledgement = optionalEntryReader("pubAcknowledgement", fields, _.convertTo[Option[Boolean]], None)
            )
        }
    }

    implicit val dataAccessRequestFormat: JsonFormat[DataAccessRequest] = jsonFormat9(DataAccessRequest)
}