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

case class DataAccessRequest(
    userId: Int,
    datasets: Seq[DataSetEntry],
    datasetId: Seq[Int]
)

object JsonProtocols extends DefaultJsonProtocol {
    implicit val researcherPropertyFormat = jsonFormat4(ResearcherProperty)
    implicit val userRoleFormat = jsonFormat5(UserRole)
    implicit val whiteListEntryFormat = jsonFormat8(WhiteListEntry) 
    implicit val dataSetPropertyFormat = jsonFormat2(DataSetProperty)
    implicit val dataUseFormat = jsonFormat5(DataUse)
    implicit val dataSetEntryFormat = jsonFormat4(DataSetEntry)

    def optionalEntryReader[T](fieldName: String, data: Map[String,JsValue], converter: JsValue => T, default: T): T = {
        data.getOrElse(fieldName, None) match {
            case j:JsValue => Try(converter(j)).toOption.getOrElse(
                throw DeserializationException(s"unexpected json type for $fieldName")
            )
            case None => default
        }
    }

    implicit object UserJsonFormat extends JsonFormat[User] {
        def write(user: User) = JsObject(Map(
            "dacUserId" -> JsNumber(user.dacUserId),
            "email" -> JsString(user.email),
            "displayName" -> JsString(user.displayName),
            "createDate" -> JsString(user.createDate),
            "roles" -> JsArray(user.roles.map(_.toJson).toVector),
            "emailPreference" -> JsBoolean(user.emailPreference),
            "status" -> JsString(user.status),
            "researcherProperties" -> JsArray(user.researcherProperties.map(_.toJson).toVector),
            "whitelistEntries" -> JsArray(user.whitelistEntries.map(_.toJson).toVector)
        ))
        def read(value: JsValue): User = {
            val fields = value.asJsObject.fields
            User(
                fields("dacUserId").convertTo[Int],
                fields("email").convertTo[String],
                fields("displayName").convertTo[String],
                fields("createDate").convertTo[String],
                fields("roles").convertTo[Seq[UserRole]],
                fields("emailPreference").convertTo[Boolean],
                fields("status").convertTo[String],
                fields("researcherProperties").convertTo[Seq[ResearcherProperty]],
                fields("whitelistEntries").convertTo[Seq[WhiteListEntry]]
            )
        }
    }

    implicit object DataSetFormat extends JsonFormat[DataSet] {
        def write(dataSet: DataSet) = JsObject(Map(
            "dacId" -> JsNumber(dataSet.dacId),
            "dataSetId" -> JsNumber(dataSet.dataSetId),
            "consentId" -> JsString(dataSet.consentId),
            "translatedUseRestriction" -> dataSet.translatedUseRestriction.map(JsString(_)).getOrElse(JsNull),
            "deletable" -> dataSet.deletable.map(JsBoolean(_)).getOrElse(JsNull),
            "properties" -> JsArray(dataSet.properties.map(_.toJson).toVector),
            "active" -> JsBoolean(dataSet.active),
            "needsApproval" -> JsBoolean(dataSet.needsApproval),
            "isAssociatedToDataOwners" -> dataSet.isAssociatedToDataOwners.map(JsBoolean(_)).getOrElse(JsNull),
            "updateAssociationToDataOwnerAllowed" -> dataSet.updateAssociationToDataOwnerAllowed.map(JsBoolean(_)).getOrElse(JsNull),
            "alias" -> JsString(dataSet.alias),
            "objectId" -> dataSet.objectId.map(JsString(_)).getOrElse(JsNull),
            "createDate" -> dataSet.createDate.map(JsNumber(_)).getOrElse(JsNull),
            "createUserId" -> dataSet.createUserId.map(JsNumber(_)).getOrElse(JsNull),
            "updateDate" -> dataSet.updateDate.map(JsNumber(_)).getOrElse(JsNull),
            "updateUserId" -> dataSet.updateUserId.map(JsNumber(_)).getOrElse(JsNull),
            "dataUse" -> dataSet.dataUse.toJson
        ))
        def read(value: JsValue): DataSet = {
            val fields = value.asJsObject.fields
            DataSet(
                fields("dacId").convertTo[Int],
                fields("dataSetId").convertTo[Int],
                fields("consentId").convertTo[String],
                optionalEntryReader("translatedUseRestriction", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("deletable", fields, _.convertTo[Option[Boolean]], None),
                fields("properties").convertTo[Seq[DataSetProperty]],
                fields("active").convertTo[Boolean],
                fields("needsApproval").convertTo[Boolean],
                optionalEntryReader("isAssocatedToDataOwners", fields, _.convertTo[Option[Boolean]], None),
                optionalEntryReader("updateAssociationToDataOwnerAllowed", fields, _.convertTo[Option[Boolean]], None),
                fields("alias").convertTo[String],
                optionalEntryReader("objectId", fields, _.convertTo[Option[String]], None),
                optionalEntryReader("createDate", fields, _.convertTo[Option[Long]], None),
                optionalEntryReader("createUserId", fields, _.convertTo[Option[Int]], None),
                optionalEntryReader("updateDate", fields, _.convertTo[Option[Long]], None),
                optionalEntryReader("updateUserId", fields, _.convertTo[Option[Int]], None),
                fields("dataUse").convertTo[DataUse]
            )
        }
    }

    implicit object DataAccessRequestFormat extends JsonFormat[DataAccessRequest] {
        def write(dar: DataAccessRequest): JsObject = JsObject(Map(
            "userId" -> JsNumber(dar.userId),
            "datasets" -> JsArray(dar.datasets.map(_.toJson).toVector),
            "datasetId" -> JsArray(dar.datasetId.map(_.toJson).toVector)
        ))

        def read(value: JsValue): DataAccessRequest = {
            val fields = value.asJsObject.fields
            DataAccessRequest(
                fields("userId").convertTo[Int],
                fields("datasets").convertTo[Seq[DataSetEntry]],
                fields("datasetId").convertTo[Seq[Int]]
            )
        }
    }
}