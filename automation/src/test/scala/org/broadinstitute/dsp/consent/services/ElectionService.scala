package org.broadinstitute.dsp.consent.services

import org.broadinstitute.dsp.consent.models.ElectionModels._

object ElectionService {
    def getElectionVotesByTypeAndUser(votes: Seq[Vote], voteType: String, userId: Int, electionId: Int): Seq[Vote] = {
        votes.filter { v =>
            v.`type`.getOrElse("") == voteType && v.userId.getOrElse(0) == userId &&
                v.electionId.getOrElse(0) == electionId
        }
    }
}
