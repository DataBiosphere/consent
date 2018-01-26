package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.models.grammar.UseRestriction;

import java.sql.Timestamp;

/**
 * Syntactic sugar for creating a Consent object.
 */
public class ConsentBuilder {
    private Consent consent;

    public ConsentBuilder() {
        consent = new Consent();
    }

    public Consent build() {
        return this.consent;
    }

    public ConsentBuilder setCreateDate(Timestamp createDate) {
        this.consent.setCreateDate(createDate);
        return this;
    }

    public ConsentBuilder setLastUpdate(Timestamp lastUpdate) {
        this.consent.setLastUpdate(lastUpdate);
        return this;
    }

    public ConsentBuilder setSortDate(Timestamp sortDate) {
        this.consent.setSortDate(sortDate);
        return this;
    }

    public ConsentBuilder setDataUseLetter(String dataUseLetter) {
        this.consent.setDataUseLetter(dataUseLetter);
        return this;
    }

    public ConsentBuilder setRequiresManualReview(Boolean requiresManualReview) {
        this.consent.setRequiresManualReview(requiresManualReview);
        return this;
    }

    public ConsentBuilder setUseRestriction(UseRestriction useRestriction) {
        this.consent.setUseRestriction(useRestriction);
        return this;
    }

    public ConsentBuilder setName(String name) {
        this.consent.setName(name);
        return this;
    }

    public ConsentBuilder setConsentId(String consentId) {
        this.consent.setConsentId(consentId);
        return this;
    }

    public ConsentBuilder setDulName(String dulName) {
        this.consent.setDulName(dulName);
        return this;
    }

    public ConsentBuilder setTranslatedUseRestriction(String translatedUseRestriction) {
        this.consent.setTranslatedUseRestriction(translatedUseRestriction);
        return this;
    }

    public ConsentBuilder setDataUse(DataUseDTO dataUse) {
        this.consent.setDataUse(dataUse);
        return this;
    }
}
