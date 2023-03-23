package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ElectionReviewVote {

    @JsonProperty
    private Vote vote;

    @JsonProperty
    private String displayName;

    @JsonProperty
    private String email;

    public ElectionReviewVote(){}

    public ElectionReviewVote(Vote vote, String displayName, String email){
        this.vote = vote;
        this.displayName = displayName;
        this.email = email;
    }

    public Vote getVote() {
        return vote;
    }

    public void setVote(Vote vote) {
        this.vote = vote;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
