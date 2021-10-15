package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @JsonProperty
    private Map<String, Object> associatedConsent;

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

    public Map<String, Object> getAssociatedConsent() {
        return associatedConsent;
    }

    public void setAssociatedConsent(Consent consent, Election consentElection) {
        this.associatedConsent = new LinkedHashMap<>();
        this.associatedConsent.put("electionId", consentElection != null ? consentElection.getElectionId() : null);
        this.associatedConsent.put("consentId", consent.getConsentId());
        this.associatedConsent.put("name", consent.getName());
    }
}