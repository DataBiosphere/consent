package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.genomebridge.consent.http.models.grammar.UseRestriction;

public class ResearchPurpose {

    @JsonProperty
    private Integer purposeId;

    @JsonProperty
    private UseRestriction purpose;

    public ResearchPurpose() {
    }

    public ResearchPurpose(Integer purposeId, UseRestriction purpose) {
        this.purposeId = purposeId;
        this.purpose = purpose;
    }

    public Integer getPurposeId() {
        return purposeId;
    }

    public void setPurposeId(Integer purposeId) {
        this.purposeId = purposeId;
    }

    public UseRestriction getPurpose() {
        return purpose;
    }

    public void setPurpose(UseRestriction purpose) {
        this.purpose = purpose;
    }


}
