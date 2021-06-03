package org.broadinstitute.consent.http.models;


import com.fasterxml.jackson.annotation.JsonProperty;

public class UserProperty {

    @JsonProperty
    private Integer propertyId;

    @JsonProperty
    private Integer userId;

    @JsonProperty
    private String propertyKey;

    @JsonProperty
    private String propertyValue;


    public UserProperty() {
    }

    public UserProperty(Integer propertyId, Integer userId, String propertyKey, String propertyValue) {
        this.propertyId = propertyId;
        this.userId = userId;
        this.propertyKey = propertyKey;
        this.propertyValue = propertyValue;
    }

    public UserProperty(Integer userId, String propertyKey, String propertyValue) {
        this.userId = userId;
        this.propertyKey = propertyKey;
        this.propertyValue = propertyValue;
    }

    public UserProperty(Integer userId, String propertyKey) {
        this.userId = userId;
        this.propertyKey = propertyKey;
    }

    public Integer getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Integer propertyId) {
        this.propertyId = propertyId;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public void setPropertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}
