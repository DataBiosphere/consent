package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.Document;
import org.genomebridge.consent.http.models.grammar.UseRestriction;

public class ResearchPurpose extends Document {

    @JsonProperty
    private String id;

    @JsonProperty
    private UseRestriction purpose;

    public ResearchPurpose() {
    }

    public ResearchPurpose(String id, UseRestriction purpose) {
        setId(id);
        setPurpose(purpose);
    }

    public String getId() {
        return this.getString("_id");
    }

    public void setId(String id) {
        this.put("_id", id);
    }

    public UseRestriction getPurpose() {
        return this.get("purpose", UseRestriction.class);
    }

    public void setPurpose(UseRestriction purpose) {
        this.put("purpose", purpose);
    }
}