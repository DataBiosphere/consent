package org.broadinstitute.consent.http.models.dto;

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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        final DataSetPropertyDTO other = (DataSetPropertyDTO) obj;
        if (this.propertyName.equals(other.getPropertyName())) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.propertyName != null ? this.propertyName.hashCode() : 0);
        hash = 53 * hash + this.propertyValue.hashCode();
        return hash;
    }
}