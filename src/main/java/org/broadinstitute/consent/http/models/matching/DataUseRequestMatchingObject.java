package org.broadinstitute.consent.http.models.matching;

import org.broadinstitute.consent.http.models.DataUse;

public class DataUseRequestMatchingObject {

    public DataUse consent;

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
