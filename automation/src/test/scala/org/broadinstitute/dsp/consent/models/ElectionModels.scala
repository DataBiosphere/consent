package org.broadinstitute.dsp.consent.models

object ElectionModels {
    case class ElectionStatus(
        status: String,
        finalAccessVote: Boolean
    )
}