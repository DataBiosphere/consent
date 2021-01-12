package org.broadinstitute.dsp.consent.models

object ElectionModels {
    object Status extends Enumeration {
        type Status = String
        val OPEN = "Open"
        val CLOSED = "Closed"
        val CANCELED = "Canceled"
        val FINAL = "Final"
        val PENDING_APPROVAL = "PendingApproval"
    }

    case class ElectionStatus(
        status: String,
        finalAccessVote: Boolean
    )
}