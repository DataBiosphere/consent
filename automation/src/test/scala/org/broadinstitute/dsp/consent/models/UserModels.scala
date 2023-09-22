package org.broadinstitute.dsp.consent.models

import org.broadinstitute.dsp.consent.models.WhiteListModels._
import org.broadinstitute.dsp.consent.models.ResearcherModels._

object UserModels {
    case class UserRole(
        userRoleId: Int,
        userId: Int,
        role_id: Int,
        name: String,
        dacId: Option[Int]
    )

    case class User(
        userId: Option[Int] = None,
        email: Option[String] = None,
        displayName: Option[String] = None,
        roles: Option[Seq[UserRole]] = None,
        emailPreference: Option[Boolean] = None,
        status: Option[String] = None,
        rationale: Option[String] = None,
        //createDate: Option[Long] = None,
        researcherProperties: Option[Seq[ResearcherProperty]] = None,
        whitelistEntries: Option[Seq[WhiteListEntry]] = None,
        profileCompleted: Option[Boolean] = None
    )
}