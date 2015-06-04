package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Consent Representation object.
 *
 * Created by grushton on 6/3/15.
 */
public class Consent {

    @JsonProperty
    public Boolean requiresManualReview;

    @JsonProperty
    public UseRestriction useRestriction;

    public Consent() {
    }

    public Consent(Boolean requiresManualReview, UseRestriction useRestriction) {
        this.requiresManualReview = requiresManualReview;
        this.useRestriction = useRestriction;
    }

    @JsonProperty
    public Boolean getRequiresManualReview() {
        return requiresManualReview;
    }

    @JsonProperty
    public void setRequiresManualReview(Boolean requiresManualReview) {
        this.requiresManualReview = requiresManualReview;
    }

    @JsonProperty
    public UseRestriction getUseRestriction() {
        return useRestriction;
    }

    @JsonProperty
    public void setUseRestriction(UseRestriction useRestriction) {
        this.useRestriction = useRestriction;
    }

}
