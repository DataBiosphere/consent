package org.broadinstitute.consent.http.mail.freemarker;

public class VoteAndElectionModel {

    private String electionIdentifier;
    private String referenceId;
    private String electionType;
    private String voteType;

    public VoteAndElectionModel(String electionIdentifier, String referenceId, String electionType, String voteType) {
        this.electionIdentifier = electionIdentifier;
        this.referenceId = referenceId;
        this.electionType = electionType;
        this.voteType = voteType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getElectionIdentifier() {
        return electionIdentifier;
    }

    public void setElectionNumber(String electionIdentifier) {
        this.electionIdentifier = electionIdentifier;
    }

    public String getElectionType() {
        return electionType;
    }

    public void setElectionType(String electionType) {
        this.electionType = electionType;
    }

    public String getVoteType() {
        return voteType;
    }

    public void setVoteType(String voteType) {
        this.voteType = voteType;
    }
}
