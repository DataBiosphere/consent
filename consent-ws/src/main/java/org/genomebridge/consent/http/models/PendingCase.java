package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PendingCase {

    @JsonProperty
    private String referenceId;

    @JsonProperty
    private String logged;

    @JsonProperty
    private Boolean alreadyVoted;

    @JsonProperty
    private Boolean isFinalVote;

    @JsonProperty
    private String status;

    @JsonProperty
    private Integer voteId;


    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getLogged() {
        return logged;
    }

    public void setLogged(String logged) {
        this.logged = logged;
    }

    public Boolean getAlreadyVoted() {
        return alreadyVoted;
    }

    public void setAlreadyVoted(Boolean alreadyVoted) {
        this.alreadyVoted = alreadyVoted;
    }

    public Boolean getIsFinalVote() {
        return isFinalVote;
    }

    public void setIsFinalVote(Boolean isFinalVote) {
        this.isFinalVote = isFinalVote;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getVoteId() {
        return voteId;
    }

    public void setVoteId(Integer voteId) {
        this.voteId = voteId;
    }


}
