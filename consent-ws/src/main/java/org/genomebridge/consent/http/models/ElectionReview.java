package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ElectionReview {

    @JsonProperty
    private String referenceId;

    @JsonProperty
    private String dataUseLetter;

    @JsonProperty
    private List<ElectionReviewVote> reviewVote;

    @JsonProperty
    private Election election;

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getDataUseLetter() {
        return dataUseLetter;
    }

    public void setDataUseLetter(String dataUseLetter) {
        this.dataUseLetter = dataUseLetter;
    }

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
    }

    public List<ElectionReviewVote> getReviewVote() {
        return reviewVote;
    }

    public void setReviewVote(List<ElectionReviewVote> reviewVote) {
        this.reviewVote = reviewVote;
    }
}