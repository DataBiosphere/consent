package org.genomebridge.consent.http.models;


import com.google.gson.Gson;
import org.bson.Document;
import org.genomebridge.consent.http.models.grammar.UseRestriction;

import java.io.IOException;
import java.util.Map;

public class ResearchPurpose extends Document {

    public ResearchPurpose() {
    }

    public ResearchPurpose(UseRestriction restriction) {
        setRestriction(restriction);
    }

    public ResearchPurpose(String id, UseRestriction restriction) {
        this.put("_id", id);
        this.put("restriction", restriction);
    }

    public String getId() {
        return this.getString("_id");
    }

    public void setId(String id) {
        this.put("_id", id);
    }

    public UseRestriction getRestriction() throws IOException{
        return UseRestriction.parse(new Gson().toJson(this.get("restriction", Map.class)));
    }

    public void setRestriction(UseRestriction restriction) {
        this.put("restriction", restriction);
    }
}