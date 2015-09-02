package org.genomebridge.consent.http.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DataSetDTO {

    @JsonProperty
    private String consentId;

    @JsonProperty
    private List<DataSetPropertyDTO> properties;

    public DataSetDTO(List<DataSetPropertyDTO> properties) {
        this.properties= properties;
    }

    public String getConsentId() {
        return consentId;
    }

    public void setConsentId(String consentId) {
        this.consentId = consentId;
    }


    public List<DataSetPropertyDTO> getProperties() {
        return properties;
    }

    public void setProperties(List<DataSetPropertyDTO> properties) {
        this.properties = properties;
    }
}
