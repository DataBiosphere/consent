package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Summary {

    @JsonProperty
    private Integer reviewedPositiveCases;

    @JsonProperty
    private Integer reviewedNegativeCases;

    @JsonProperty
    private Integer pendingCases;


    public Summary() {
    }


    public Integer getReviewedPositiveCases() {
        return reviewedPositiveCases;
    }


    public void setReviewedPositiveCases(Integer reviewedPositiveCases) {
        this.reviewedPositiveCases = reviewedPositiveCases;
    }


    public Integer getReviewedNegativeCases() {
        return reviewedNegativeCases;
    }


    public void setReviewedNegativeCases(Integer reviewedNegativeCases) {
        this.reviewedNegativeCases = reviewedNegativeCases;
    }


    public Integer getPendingCases() {
        return pendingCases;
    }


    public void setPendingCases(Integer pendingCases) {
        this.pendingCases = pendingCases;
    }


}
