package org.broadinstitute.dsp.consent.models

import org.broadinstitute.dsp.consent.models.WhiteListModels._

object ResearcherModels {
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
        eRACommonsID: Option[String] = None,
        pubmedID: Option[String] = None,
        scientificURL: Option[String] = None,
        havePI: Option[String] = None,
        piName: Option[String] = None,
        piEmail: Option[String] = None,
        piERACommonsID: Option[String] = None,
        completed: Option[String] = None,
        investigator: Option[String] = None,
        eraExpiration: Option[String] = None,
        eraAuthorized: Option[String] = None,
        nihUsername: Option[String] = None,
        linkedIn: Option[String] = None,
        researcherGate: Option[String] = None,
        orcid: Option[String] = None,
        checkNotifications: Option[String] = None,
        libraryCards: Option[Seq[String]] = None,
        libraryCardEntries: Option[Seq[WhiteListEntry]] = None
    )
}