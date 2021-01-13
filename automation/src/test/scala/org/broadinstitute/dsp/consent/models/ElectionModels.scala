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

    object VoteType extends Enumeration {
        type VoteType = String
        val DAC = "DAC"
        val FINAL = "FINAL"
        val AGREEMENT = "AGREEMENT"
        val CHAIRPERSON = "Chairperson"
    }

    case class ElectionStatus(
        status: String,
        finalAccessVote: Boolean
    )

    case class Vote(
        voteId: Int,
        vote: Option[Boolean] = None,
        dacUserId: Option[Int] = None,
        createDate: Option[Long] = None,
        updateDate: Option[Long] = None,
        electionId: Option[Int] = None,
        rationale: Option[String] = None,
        vType: Option[String] = None,
        isReminderSent: Option[Boolean] = None,
        hasConcerns: Option[Boolean] = None
    )

    case class VotePostObject(
        vote: Boolean,
        dacUserId: Int,
        rationale: Option[String] = None,
        hasConcerns: Option[Boolean] = None
    )
}