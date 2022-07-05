package org.broadinstitute.dsp.consent.models

import org.broadinstitute.dsp.consent.models.DataUseModels._

object DatasetModels {
    case class DataSetProperty(
        propertyName: String,
        propertyValue: String
    )

    case class Dataset(
        dacId: Option[Int] = None,
        dataSetId: Int,
        consentId: String,
        translatedUseRestriction: Option[String],
        deletable: Option[Boolean],
        properties: Seq[DataSetProperty],
        active: Boolean,
        needsApproval: Boolean,
        isAssociatedToDataOwners: Option[Boolean],
        //updateAssociationToDataOwnerAllowed: Option[Boolean],
        alias: String,
        objectId: Option[String],
        createDate: Option[Long],
        createUserId: Option[Int],
        updateDate: Option[Long],
        updateUserId: Option[Int],
        dataUse: DataUse
    )

    case class DataSetEntry(
        key: String,
        value: String,
        label: String,
        concatenation: String
    )
    object DataSetEntryBuilder {
        def fromDataSet(set: Dataset): DataSetEntry = {
            val prop = set.properties.filter(_.propertyName == "Dataset Name").head.propertyValue
            DataSetEntry(
                set.dataSetId.toString,
                set.dataSetId.toString,
                prop,
                prop
            )
        }
    }

    case class DataSetDetailEntry(
        datasetId: Option[String],
        name: Option[String],
        objectId: Option[String]
    )
}