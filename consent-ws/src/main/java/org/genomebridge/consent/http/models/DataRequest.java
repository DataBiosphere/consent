package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataRequest {

    @JsonProperty
    private Integer requestId;

    @JsonProperty
    private String purpose;

    @JsonProperty
    private String description;

    @JsonProperty
    private String researcher;

    public DataRequest() {
    }

    public DataRequest(Integer requestId, String purpose, String description, String researcher) {
        this.requestId = requestId;
        this.purpose = purpose;
        this.description = description;
        this.researcher = researcher;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResearcher() {
        return researcher;
    }

    public void setResearcher(String researcher) {
        this.researcher = researcher;
    }


}
