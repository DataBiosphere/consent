package org.broadinstitute.dsp.consent.models

import org.broadinstitute.dsp.consent.models.UserModels._

object DacModels {
    case class Dac(
        dacId: Option[Int],
        name: Option[String],
        description: Option[String],
        createDate: Option[Long],
        updateDate: Option[Long],
        chairpersons: Option[Seq[User]],
        members: Option[Seq[User]],
        electionIds: Option[Seq[Int]]
    )
}
