package org.broadinstitute.consent.http.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InvalidRestriction {

    @JsonProperty
    private String name;

    @JsonProperty
    private String useRestriction;

    public InvalidRestriction(){
    }

    public InvalidRestriction(String name, String useRestriction){
        this.name = name;
        this.useRestriction = useRestriction;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUseRestriction() {
        return useRestriction;
    }

    public void setUseRestriction(String useRestriction) {
        this.useRestriction = useRestriction;
    }
}
