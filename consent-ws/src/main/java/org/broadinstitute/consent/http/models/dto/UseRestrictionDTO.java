package org.broadinstitute.consent.http.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UseRestrictionDTO {

    @JsonProperty
    private String id;

    @JsonProperty
    private String name;

    @JsonProperty
    private String useRestriction;

    public UseRestrictionDTO(){
    }

    public UseRestrictionDTO(String name, String useRestriction){
        this.name = name;
        this.useRestriction = useRestriction;
    }

    public UseRestrictionDTO(String name, String useRestriction, String id){
        this.name = name;
        this.useRestriction = useRestriction;
        this.id = id;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
