package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;


public class DataRequestElectionReview extends ElectionReview {

    @JsonProperty
    private String sDataRequest;

    public String getSDataRequest() {
        return sDataRequest;
    }

    public void setSDataRequest(String sDataRequest) {
        this.sDataRequest = sDataRequest;
    }

}