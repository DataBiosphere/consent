package org.broadinstitute.dsp.consent.models

import org.broadinstitute.dsp.consent.models.DacModels._

object PendingModels {
    case class PendingCase(
        referenceId: Option[String] = None,
        frontEndId: Option[String] = None,
        logged: Option[String] = None,
        alreadyVoted: Option[Boolean] = None,
        isReminderSent: Option[Boolean] = None,
        isFinalVote: Option[Boolean] = None,
        status: Option[String] = None,
        electionStatus: Option[String] = None,
        electionId: Option[Int] = None,
        voteId: Option[Int] = None,
        totalVotes: Option[Int] = None,
        votesLogged: Option[Int] = None,
        rpElectionId: Option[Int] = None,
        rpVoteId: Option[Int] = None,
        consentGroupName: Option[String] = None,
        projectTitle: Option[String] = None,
        dac: Option[Dac] = None,
        createDate: Option[Long] = None
    )
}
