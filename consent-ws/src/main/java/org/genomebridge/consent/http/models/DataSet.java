package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataSet {

    @JsonProperty
    private Integer dataSetId;

    @JsonProperty
    private Integer associationId;

    @JsonProperty
    private String description;

    public DataSet() {
    }

    public DataSet(Integer dataSetId, Integer associationId, String description) {
        this.dataSetId = dataSetId;
        this.associationId = associationId;
        this.description = description;
    }

    public Integer getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(Integer dataSetId) {
        this.dataSetId = dataSetId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getAssociationId() {
        return associationId;
    }

    public void setAssociationId(Integer associationId) {
        this.associationId = associationId;
    }


}
