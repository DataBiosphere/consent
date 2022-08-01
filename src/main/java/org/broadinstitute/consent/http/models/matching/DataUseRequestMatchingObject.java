package org.broadinstitute.consent.http.models.matching;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;

public class DataUseRequestMatchingObject {

    @JsonProperty
    public DataUse consent;
    @JsonProperty
    public DataUse purpose;

    public DataUseRequestMatchingObject(){
    }

    public DataUseRequestMatchingObject(DataUse consent, DataUse purpose) {
        this.consent = consent;
        this.purpose = purpose;
    }

    public DataUse getConsent() {
        return consent;
    }

    public void setConsent(DataUse consent) {
        this.consent = consent;
    }

    public DataUse getPurpose() {
        return purpose;
    }

    public void setPurpose(DataUse purpose) {
        this.purpose = purpose;
    }

}
