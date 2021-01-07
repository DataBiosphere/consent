package org.broadinstitute.dsp.consent.models

object WhiteListModels {
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
}