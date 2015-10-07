package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

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