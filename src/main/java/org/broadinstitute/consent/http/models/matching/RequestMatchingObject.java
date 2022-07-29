package org.broadinstitute.consent.http.models.matching;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestMatchingObject {

    @Deprecated
    @JsonProperty
    public UseRestriction consent;
    @Deprecated
    @JsonProperty
    public UseRestriction purpose;

    @JsonProperty
    public DataUse datasetDataUse;

    @JsonProperty
    public DataUse purposeDataUse;

    public RequestMatchingObject(){
    }

    @Deprecated
    public RequestMatchingObject(UseRestriction consent, UseRestriction purpose) {
        this.consent = consent;
        this.purpose = purpose;
    }

    public RequestMatchingObject(DataUse datasetDataUse, DataUse purposeDataUse) {
        this.datasetDataUse = datasetDataUse;
        this.purposeDataUse = purposeDataUse;
    }

    @Deprecated
    @JsonProperty
    public UseRestriction getConsent() {
        return consent;
    }

    @Deprecated
    @JsonProperty
    public void setConsent(UseRestriction consent) {
        this.consent = consent;
    }

    @Deprecated
    @JsonProperty
    public UseRestriction getPurpose() {
        return purpose;
    }

    @Deprecated
    @JsonProperty
    public void setPurpose(UseRestriction purpose) {
        this.purpose = purpose;
    }

    public DataUse getDatasetDataUse() {
        return datasetDataUse;
    }

    public void setDatasetDataUse(DataUse datasetDataUse) {
        this.datasetDataUse = datasetDataUse;
    }

    public DataUse getPurposeDataUse() {
        return purposeDataUse;
    }

    public void setPurposeDataUse(DataUse purposeDataUse) {
        this.purposeDataUse = purposeDataUse;
    }
}
