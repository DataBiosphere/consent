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

    public VoteUpdateInfo(Boolean vote, String rationale, List<Integer> voteIds) {
        this.vote = vote;
        this.rationale = rationale;
        this.voteIds = voteIds;
    }

    public Boolean getVote() {
        return vote;
    }

    public String getRationale() {
        return rationale;
    }

    public List<Integer> getVoteIds() {
        return voteIds;
    }

}
