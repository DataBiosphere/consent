package org.broadinstitute.dsp.consent.models

object MatchModels {
    case class Match(
        id: Int,
        consent: String,
        purpose: String,
        cMatch: Boolean,
        failed: Boolean,
        createDate: Option[Long] = None
    )
    object MatchBuilder {
        def empty(): Match = {
            Match(
                id = 0,
                consent = "",
                purpose = "",
                cMatch = false,
                failed = false,
                createDate = None
            )
        }
    }
}