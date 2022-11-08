package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.ElectionDAO;

public class ReviewResultsService {
    private final ElectionDAO electionDAO;

    @Inject
    public ReviewResultsService(ElectionDAO electionDAO) {
        this.electionDAO = electionDAO;
    }

    public Boolean openElections() {
        Boolean openElections = false;
        if(electionDAO.verifyOpenElections() != null && electionDAO.verifyOpenElections() > 0){
            openElections = true;
        }
        return openElections;
    }

//    public List<Vote> describeAgreementVote(Integer electionId) {
//        return voteDAO.findVoteByTypeAndElectionId(electionId, VoteType.AGREEMENT.getValue());
//    }

}
