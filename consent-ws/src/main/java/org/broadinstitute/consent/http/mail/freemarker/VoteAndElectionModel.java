package org.broadinstitute.consent.http.mail.freemarker;

public class VoteAndElectionModel {

    private String electionNumber;
    private String electionType;
    private String voteType;

    public VoteAndElectionModel(String electionNumber, String electionType, String voteType) {
        this.electionNumber = electionNumber;
        this.electionType = electionType;
        this.voteType = voteType;
    }

    public String getElectionNumber() {
        return electionNumber;
    }

    public void setElectionNumber(String electionNumber) {
        this.electionNumber = electionNumber;
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
