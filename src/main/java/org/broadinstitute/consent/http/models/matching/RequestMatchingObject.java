package org.broadinstitute.consent.http.models.matching;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;

public class RequestMatchingObject {

    @JsonProperty
    public UseRestriction consent;
    @JsonProperty
    public UseRestriction purpose;

    public RequestMatchingObject(){
    }

    public RequestMatchingObject(UseRestriction consent, UseRestriction purpose) {
        this.consent = consent;
        this.purpose = purpose;
    }

    @JsonProperty
    public UseRestriction getConsent() {
        return consent;
    }

    @JsonProperty
    public void setConsent(UseRestriction consent) {
        this.consent = consent;
    }

    @JsonProperty
    public UseRestriction getPurpose() {
        return purpose;
    }

    @JsonProperty
    public void setPurpose(UseRestriction purpose) {
        this.purpose = purpose;
    }
}
