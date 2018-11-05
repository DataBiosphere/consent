package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class ConsentDataSet {

    @JsonProperty
    public String consentId;

    @JsonProperty
    public String objectId;

    /**
     * Map<String, String> dataSets content model is
     * Key = dataset name Value = object id
    */
    @JsonProperty
    public Map<String, String> dataSets;

    public ConsentDataSet(String consentId,  Map<String, String> dataSets, String objectId) {
        this.consentId = consentId;
        this.objectId = objectId;
        this.dataSets = dataSets;
    }

    public String getConsentId() {
        return consentId;
    }

    public void setConsentId(String consentId) {
        this.consentId = consentId;
    }

    public Map<String, String> getDataSets() {
        return dataSets;
    }

    public void setDataSets(Map<String, String> dataSets) {
        this.dataSets = dataSets;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

}
