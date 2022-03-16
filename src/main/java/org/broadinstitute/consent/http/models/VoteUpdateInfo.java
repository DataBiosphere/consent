package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class VoteUpdateInfo {

    @JsonProperty
    private Boolean vote;

    @JsonProperty
    private String rationale;

    @JsonProperty
    private List<Integer> voteIds;

    public VoteUpdateInfo() {};

    public VoteUpdateInfo(Boolean vote, String rationale, List<Integer> voteIds) {
        this.vote = vote;
        this.rationale = rationale;
        this.voteIds = voteIds;
    }

    public Boolean getVote() {
        return vote;
    }

    public void setVote(Boolean vote) {
        this.vote = vote;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public List<Integer> getVoteIds() {
        return voteIds;
    }

    public void setVoteIds(List<Integer> voteIds) {
        this.voteIds = voteIds;
    }
}
