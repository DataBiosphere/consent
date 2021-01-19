package org.broadinstitute.dsp.consent.models

object MatchModels {
    case class Match(
        id: Int,
        consent: String,
        purpose: String,
        `match`: Boolean,
        failed: Boolean,
        createDate: Option[Long] = None
    )
    object MatchBuilder {
        def empty(): Match = {
            Match(
                id = 0,
                consent = "",
                purpose = "",
                `match` = false,
                failed = false,
                createDate = None
            )
        }
    }
}