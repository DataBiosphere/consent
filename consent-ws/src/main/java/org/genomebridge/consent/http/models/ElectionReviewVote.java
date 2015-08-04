package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ElectionReviewVote {

    @JsonProperty
    private Vote vote;

    @JsonProperty
    private String displayName;

    @JsonProperty
    private String email;

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
