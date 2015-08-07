package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.genomebridge.consent.http.models.grammar.UseRestriction;

/**
 * Consent Representation object.
 * <p/>
 * Created by grushton on 6/3/15.
 */

public class Consent {

    @JsonProperty
    public String consentId;

    @JsonProperty
    public Boolean requiresManualReview;

    @JsonProperty
    public String dataUseLetter;

    @JsonProperty
    public UseRestriction useRestriction;

    @JsonProperty
    public String name;

    @JsonProperty
    public String structuredDataUseLetter;

    @JsonProperty
    public String dulName;

    public Consent() {
    }


    public Consent(Boolean requiresManualReview, UseRestriction useRestriction, String name) {
        this.name = name;
        this.requiresManualReview = requiresManualReview;
        this.useRestriction = useRestriction;
    }

    public Consent(Boolean requiresManualReview, UseRestriction useRestriction, String dataUseLetter, String structuredDataUseLetter, String name) {
        this.requiresManualReview = requiresManualReview;
        this.useRestriction = useRestriction;
        this.dataUseLetter = dataUseLetter;
        this.structuredDataUseLetter = structuredDataUseLetter;
        this.name = name;
    }


    @JsonProperty
    public String getDataUseLetter() {
        return dataUseLetter;
    }

    @JsonProperty
    public void setDataUseLetter(String dataUseLetter) {
       this.dataUseLetter = dataUseLetter;
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

    @JsonProperty
    public String getStructuredDataUseLetter() {
        return structuredDataUseLetter;
    }

    @JsonProperty
    public void setStructuredDataUseLetter(String structuredDataUseLetter) {
        this.structuredDataUseLetter = structuredDataUseLetter;
    }

    @JsonProperty
    public String getName() {
        return name;
    }

    @JsonProperty
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty
    public String getConsentId() {
        return consentId;
    }

    @JsonProperty
    public void setConsentId(String consentId) {
        this.consentId = consentId;
    }

    @JsonProperty
    public String getDulName() {
        return dulName;
    }

    @JsonProperty
    public void setDulName(String dulName) {
        this.dulName = dulName;
    }


}
