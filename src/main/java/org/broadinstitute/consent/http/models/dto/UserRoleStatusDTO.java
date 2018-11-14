package org.broadinstitute.consent.http.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserRoleStatusDTO {

    @JsonProperty
    private String status;

    @JsonProperty
    private String role;

    @JsonProperty
    private String rationale;

    public UserRoleStatusDTO(){

    }

    public UserRoleStatusDTO(String status, String role, String rationale){
        this.status = status;
        this.role = role;
        this.rationale = rationale;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }
}
