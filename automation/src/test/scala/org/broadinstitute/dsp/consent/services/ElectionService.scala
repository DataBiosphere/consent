package org.broadinstitute.dsp.consent.services

import org.broadinstitute.dsp.consent.models.ElectionModels._

object ElectionService {
    def getElectionVotesByTypeAndUser(votes: Seq[Vote], voteType: String, userId: Int, electionId: Int): Seq[Vote] = {
        votes.filter { v => 
            v.vType.getOrElse("") == voteType && v.dacUserId.getOrElse(0) == userId && 
                v.electionId.getOrElse(0) == electionId
        }
    }
}