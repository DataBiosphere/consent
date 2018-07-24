package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConsentGroupName {
    @JsonProperty
    public String consentId;

    @JsonProperty
    public String groupName;

    @JsonProperty
    public String getConsentId() { return consentId; }

    @JsonProperty
    public String getGroupName() { return groupName; }

    @JsonProperty
    public void setConsentId(String consentId) { this.consentId = consentId; }

    @JsonProperty
    public void setGroupName(String groupName) { this.groupName = groupName; }
}
