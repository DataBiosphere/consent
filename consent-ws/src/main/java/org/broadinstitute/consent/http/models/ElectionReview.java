package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ElectionReview {


    @JsonProperty
    private List<ElectionReviewVote> reviewVote;

    @JsonProperty
    private Election election;

    @JsonProperty
    private Consent consent;

    @JsonProperty
    private Vote voteAgreement;

    @JsonProperty
    private Vote finalVote;

    @JsonProperty
    private Integer rpElectionId;

    public ElectionReview(){}

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

    public Vote getVoteAgreement() {
        return voteAgreement;
    }

    public void setVoteAgreement(Vote voteAgreement) {
        this.voteAgreement = voteAgreement;
    }

    public Vote getFinalVote() {
        return finalVote;
    }

    public void setFinalVote(Vote finalVote) {
        this.finalVote = finalVote;
    }

}