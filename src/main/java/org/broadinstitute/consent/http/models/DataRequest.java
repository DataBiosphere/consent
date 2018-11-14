package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataRequest {

    @JsonProperty
    private Integer requestId;

    @JsonProperty
    private Integer purposeId;

    @JsonProperty
    private String description;

    @JsonProperty
    private String researcher;

    @JsonProperty
    private Integer dataSetId;

    public DataRequest() {
    }

    public DataRequest(Integer requestId, Integer purposeId, String description, String researcher, Integer dataSetId) {
        this.requestId = requestId;
        this.purposeId = purposeId;
        this.description = description;
        this.researcher = researcher;
        this.dataSetId = dataSetId;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public Integer getPurposeId() {
        return purposeId;
    }

    public void setPurposeId(Integer purposeId) {
        this.purposeId = purposeId;
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

    public Integer getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(Integer dataSetId) {
        this.dataSetId = dataSetId;
    }


}
