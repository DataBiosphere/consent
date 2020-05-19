package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataAccessRequest {

    @JsonProperty
    public Integer id;

    @JsonProperty
    public String referenceId;

    @JsonProperty
    public DataAccessRequestData data;

    @JsonProperty
    public Boolean draft;

    public DataAccessRequest() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public DataAccessRequestData getData() {
        return data;
    }

    public void setData(DataAccessRequestData data) {
        this.data = data;
    }

    public Boolean getDraft() {
        return draft;
    }

    public void setDraft(Boolean draft) {
        this.draft = draft;
    }

}
