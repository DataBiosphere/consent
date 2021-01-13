package org.broadinstitute.dsp.consent.models

import org.broadinstitute.dsp.consent.models.DataUseModels._

object ConsentModels {
    case class Consent(
        consentId: String,
        lastElectionStatus: Option[String] = None,
        lastElectionArchived: Option[Boolean] = None,
        requiresManualReview: Option[Boolean] = None,
        dataUseLetter: Option[String] = None,
        name: String,
        dulName: Option[String] = None,
        createDate: Option[Long] = None,
        lastUpdate: Option[Long] = None,
        sortDate: Option[Long] = None,
        translatedUseRestriction: Option[String] = None,
        dataUse: Option[DataUse] = None,
        groupName: Option[String] = None,
        updateStatus: Option[Boolean] = None,
        dacId: Option[Int] = None
    )
}