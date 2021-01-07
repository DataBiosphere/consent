package org.broadinstitute.dsp.consent.models

import org.broadinstitute.dsp.consent.models.UserModels._

object DacModels {
    case class Dac(
        dacId: Option[Int],
        name: Option[String],
        description: Option[String],
        createDate: Option[Long],
        updateDate: Option[Long],
        chairpersons: Option[User],
        members: Option[User],
        electionIds: Option[Seq[Int]]
    )
}