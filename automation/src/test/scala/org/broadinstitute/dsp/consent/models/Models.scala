package org.broadinstitute.dsp.consent.models

import spray.json._
import DefaultJsonProtocol._

import scala.util.{Failure, Success, Try}

case class ResearcherProperty(
    propertyId: Int,
    userId: Int,
    propertyKey: String,
    propertyValue: String
)

case class ResearcherInfo(
    profileName: Option[String] = None,
    academicEmail: Option[String] = None,
    institution: Option[String] = None,
    department: Option[String] = None,
    address1: Option[String] = None,
    city: Option[String] = None,
    zipcode: Option[String] = None,
    isThePI: Option[String] = None,
    country: Option[String] = None,
    division: Option[String] = None,
    address2: Option[String] = None,
    state: Option[String] = None,
    eRACommonsID: Option[Int] = None,
    pubmedID: Option[String] = None,
    scientificURL: Option[String] = None,
    havePI: Option[String] = None,
    piName: Option[String] = None,
    piEmail: Option[String] = None,
    piERACommonsID: Option[Int] = None,
    completed: Option[String] = None,
    investigator: Option[String] = None,
    eraExpiration: Option[Long] = None,
    eraAuthorized: Option[Boolean] = None,
    nihUsername: Option[String] = None,
    linkedIn: Option[String] = None,
    researcherGate: Option[String] = None,
    orcid: Option[String] = None,
    checkNotifications: Option[Boolean] = None,
    libraryCards: Option[Seq[String]] = None,
    libraryCardEntries: Option[Seq[WhiteListEntry]] = None
)

case class UserRole(
    userRoleId: Int,
    userId: Int,
    roleId: Int,
    name: String,
    dacId: Option[Int]
)

case class WhiteListEntry(
    organization: String,
    commonsId: String,
    name: String,
    email: String,
    signingOfficialName: String,
    signingOfficialEmail: String,
    itDirectorName: String,
    itDirectorEmail: String
)

case class User(
    dacUserId: Int, 
    email: String,
    displayName: String,
    createDate: String,
    roles: Seq[UserRole],
    emailPreference: Boolean,
    status: String,
    researcherProperties: Seq[ResearcherProperty],
    whitelistEntries: Seq[WhiteListEntry]
)

case class DataSet(
    dacId: Int,
    dataSetId: Int,
    consentId: String,
    translatedUseRestriction: Option[String],
    deletable: Option[Boolean],
    properties: Seq[DataSetProperty],
    active: Boolean,
    needsApproval: Boolean,
    isAssociatedToDataOwners: Option[Boolean],
    updateAssociationToDataOwnerAllowed: Option[Boolean],
    alias: String,
    objectId: Option[String],
    createDate: Option[Long],
    createUserId: Option[Int],
    updateDate: Option[Long],
    updateUserId: Option[Int],
    dataUse: DataUse
)

case class DataUse(
    hmbResearch: Option[Boolean],
    populationOriginsAncestry: Option[Boolean],
    commercialUse: Option[Boolean],
    pediatric: Option[Boolean],
    collaboratorRequired: Option[Boolean]
)

case class DataSetProperty(
    propertyName: String,
    propertyValue: String
)

case class DataSetEntry(
    key: String,
    value: String,
    label: String,
    concatenation: String
)

case class DataAccessRequestDraft(
    userId: Int,
    datasets: Seq[DataSetEntry],
    datasetId: Seq[Int]
)

case class DataAccessRequest(
    id: Int,
    referenceId: String,
    data: Option[DataAccessRequestData] = None,
    draft: Option[Boolean] = None,
    userId: Int,
    createDate: Option[Long] = None,
    sortDate: Option[Long] = None,
    submissionDate: Option[Long] = None,
    updateDate: Option[Long] = None
)

case class DataAccessRequestData(
    referenceId: Option[String] = None,
    investigator: Option[String] = None,
    institution: Option[String] = None,
    department: Option[String] = None,
    division: Option[String] = None,
    address1: Option[String] = None,
    address2: Option[String] = None,
    city: Option[String] = None,
    zipCode: Option[String] = None,
    state: Option[String] = None,
    country: Option[String] = None,
    projectTitle: Option[String] = None,
    checkCollaborator: Option[Boolean] = None,
    researcher: Option[String] = None,
    userId: Option[Int] = None,
    isThePi: Option[String] = None,
    havePi: Option[String] = None,
    piEmail: Option[String] = None,
    profileName: Option[String] = None,
    pubmedId: Option[String] = None,
    scientificUrl: Option[String] = None,
    eraExpiration: Option[Boolean] = None,
    academicEmail: Option[String] = None,
    eraAuthorized: Option[Boolean] = None,
    nihUsername: Option[String] = None,
    linkedIn: Option[String] = None,
    orcid: Option[String] = None,
    researcherGate: Option[String] = None,
    rus: Option[String] = None,
    nonTechRus: Option[String] = None,
    diseases: Option[Boolean] = None,
    methods: Option[Boolean] = None,
    controls: Option[Boolean] = None,
    population: Option[Boolean] = None,
    other: Option[Boolean] = None,
    otherText: Option[String] = None,
    ontologies: Option[Seq[OntologyEntry]] = None,
    forProfit: Option[Boolean] = None,
    oneGender: Option[Boolean] = None,
    gender: Option[String] = None,
    pediatric: Option[Boolean] = None,
    illegalBehavior: Option[Boolean] = None,
    addiction: Option[Boolean] = None,
    sexualDiseases: Option[Boolean] = None,
    stigmatizedDiseases: Option[Boolean] = None,
    vulnerablePopulation: Option[Boolean] = None,
    populationMigration: Option[Boolean] = None,
    psychiatricTraits: Option[Boolean] = None,
    notHealth: Option[Boolean] = None,
    hmb: Option[Boolean] = None,
    status: Option[String] = None,
    poa: Option[Boolean] = None,
    datasets: Option[Seq[DataSetEntry]] = None,
    darCode: Option[String] = None,
    partialDarCode: Option[String] = None,
    restriction: Option[Any] = None,
    validRestriction: Option[Boolean] = None,
    translatedUseRestriction: Option[String] = None,
    createDate: Option[Long] = None,
    sortDate: Option[Long] = None,
    datasetIds: Option[Seq[Int]] = None,
    datasetDetail: Option[Seq[DataSetDetailEntry]] = None,
    anvilUse: Option[Boolean] = None,
    cloudUse: Option[Boolean] = None,
    localUse: Option[Boolean] = None,
    cloudProvider: Option[String] = None,
    cloudProviderType: Option[String] = None,
    cloudProviderDescription: Option[String] = None,
    geneticStudiesOnly: Option[Boolean] = None,
    irb: Option[Boolean] = None,
    irbDocumentLocation: Option[String] = None,
    irbDocumentName: Option[String] = None,
    irbProtocolExpiration: Option[String] = None,
    itDirector: Option[String] = None,
    signingOfficial: Option[String] = None,
    publication: Option[Boolean] = None,
    collaboration: Option[Boolean] = None,
    collaborationLetterLocation: Option[String] = None,
    collaborationLetterName: Option[String] = None,
    forensicActivities: Option[Boolean] = None,
    sharingDistribution: Option[Boolean] = None,
    labCollaborators: Option[Seq[Collaborator]] = None,
    internalCollaborators: Option[Seq[Collaborator]] = None,
    externalCollaborators: Option[Seq[Collaborator]] = None,
    dsAcknowledgement: Option[Boolean] = None,
    gsoAcknowledgement: Option[Boolean] = None,
    pubAcknowledgement: Option[Boolean] = None
)

case class OntologyEntry(
    id: Option[String],
    label: Option[String],
    definition: Option[String],
    synonyms: Option[Seq[String]]
)

case class DataSetDetailEntry(
    datasetId: Option[String],
    name: Option[String],
    objectId: Option[String]
)

case class Collaborator(
    approverStatus: Option[Boolean],
    email: Option[String],
    eraCommonsId: Option[String],
    name: Option[String],
    title: Option[String],
    uuid: Option[String]
)

case class FireCloudProfile(
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    title: Option[String] = None,
    contactEmail: Option[String] = None,
    institute: Option[String] = None,
    institutionalProgram: Option[String] = None,
    programLocationCity: Option[String] = None,
    programLocationState: Option[String] = None,
    programLocationCountry: Option[String] = None,
    pi: Option[String] = None,
    nonProfitStatus: Option[String] = None
)

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
    implicit val userFormat = jsonFormat9(User)
    implicit val dataSetFormat = jsonFormat17(DataSet)
    implicit val dataAccessRequestDraftFormat = jsonFormat3(DataAccessRequestDraft)
    implicit val fireCloudProfileFormat = jsonFormat11(FireCloudProfile)

    def optionalEntryReader[T](fieldName: String, data: Map[String,JsValue], converter: JsValue => T, default: T): T = {
        data.getOrElse(fieldName, None) match {
            case j:JsValue => Try(converter(j)).toOption.getOrElse(
                throw DeserializationException(s"unexpected json type for $fieldName")
            )
            case None => default
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
              map += ("libraryCards" -> JsArray(ri.libraryCards.map(_.toJson).toVector))
            if (!ri.libraryCardEntries.isEmpty)
              map += ("libraryCardEntries" -> JsArray(ri.libraryCardEntries.map(_.toJson).toVector))

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
                optionalEntryReader("eRACommonsID", fields, _.convertTo[Option[Int]], None),
                optionalEntryReader("pubmedID", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("scientificURL", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("havePI", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("piName", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("piEmail", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("piERACommonsID", fields, _.convertTo[Option[Int]], None),
                optionalEntryReader("completed", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("investigator", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("eraExpiration", fields, _.convertTo[Option[Long]], None),
                optionalEntryReader("eraAuthorized", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("nihUsername", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("linkedIn", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("researcherGate", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("orcid", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("checkNotifications", fields, _.convertTo[Option[Boolean]], None),
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
                map += ("ontologies" -> JsArray(dar.ontologies.map(_.toJson).toVector))
            if (!dar.labCollaborators.isEmpty)
                map += ("labCollaborators" -> JsArray(dar.labCollaborators.map(_.toJson).toVector))
            if (!dar.internalCollaborators.isEmpty)
                map += ("internalCollaborators" -> JsArray(dar.internalCollaborators.map(_.toJson).toVector))
            if (!dar.externalCollaborators.isEmpty)
                map += ("externalCollaborators" -> JsArray(dar.externalCollaborators.map(_.toJson).toVector))
            if (!dar.datasets.isEmpty)
                map += ("datasets" -> JsArray(dar.datasets.map(_.toJson).toVector))
            if (!dar.datasetDetail.isEmpty)
                map += ("datasetDetail" -> JsArray(dar.datasetDetail.map(_.toJson).toVector))

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