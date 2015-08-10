package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ElectionReview {

    @JsonProperty
    private Consent consent;

    @JsonProperty
    private List<ElectionReviewVote> reviewVote;

    @JsonProperty
    private Election election;

    public Consent getConsent() {
        return consent;
    }

    public void setConsent(Consent consent) {
        this.consent = consent;
    }

    public List<ElectionReviewVote> getReviewVote() {
        return reviewVote;
    }

    public void setReviewVote(List<ElectionReviewVote> reviewVote) {
        this.reviewVote = reviewVote;
    }

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
    }
}