package org.broadinstitute.dsp.consent.models

import org.broadinstitute.dsp.consent.models.ConsentModels._

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

    case class Election(
        electionId: Option[Int] = None,
        electionType: Option[String] = None,
        finalVote: Option[Boolean] = None,
        status: Option[String] = None,
        createDate: Option[Long] = None,
        lastUpdate: Option[Long] = None,
        finalVoteDate: Option[Long] = None,
        referenceId: Option[String] = None,
        finalRationale: Option[String] = None,
        finalAccessVote: Option[Boolean] = None,
        dataSetId: Option[Int] = None,
        displayId: Option[String] = None,
        dataUseLetter: Option[String] = None,
        dulName: Option[String] = None,
        archived: Option[Boolean] = None,
        version: Option[Int] = None,
        consentGroupName: Option[String] = None,
        projectTitle: Option[String] = None
    )
    object ElectionBuilder {
        def empty(): Election = {
            Election(
                electionId = Some(0),
                electionType = None,
                finalVote = Some(false),
                status = Some(""),
                createDate = None,
                lastUpdate = None,
                finalVoteDate = None,
                referenceId = Some(""),
                finalRationale = Some(""),
                finalAccessVote = Some(false),
                dataSetId = None,
                displayId = Some(""),
                dataUseLetter = Some(""),
                dulName = Some(""),
                archived = Some(false),
                version = None,
                consentGroupName = Some(""),
                projectTitle = Some("")
            )
        }
    }

    case class Vote(
        voteId: Option[Int] = None,
        vote: Option[Boolean] = None,
        dacUserId: Option[Int] = None,
        createDate: Option[Long] = None,
        updateDate: Option[Long] = None,
        electionId: Option[Int] = None,
        rationale: Option[String] = None,
        `type`: Option[String] = None,
        isReminderSent: Option[Boolean] = None,
        hasConcerns: Option[Boolean] = None
    )

    case class VotePostObject(
        vote: Option[Boolean] = None,
        dacUserId: Option[Int] = None,
        rationale: Option[String] = None,
        hasConcerns: Option[Boolean] = None
    )

    case class ElectionReview(
        reviewVote: Option[Seq[ElectionReviewVote]] = None,
        election: Option[Election] = None,
        consent: Option[Consent] = None,
        voteAgreement: Option[Vote] = None,
        finalVote: Option[Vote] = None,
        rpElectionId: Option[Int] = None
    )

    case class ElectionReviewVote(
        vote: Option[Vote] = None,
        displayName: Option[String] = None,
        email: Option[String] = None
    )
}
