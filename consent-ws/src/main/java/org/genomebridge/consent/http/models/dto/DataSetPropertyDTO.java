package org.genomebridge.consent.http.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataSetPropertyDTO {

    @JsonProperty
    private String propertyName;

    @JsonProperty
    private String propertyValue;

    public DataSetPropertyDTO(String propertyName, String propertyValue) {
        this.propertyName=propertyName;
        this.propertyValue=propertyValue;
    }


    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyKey(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }
}