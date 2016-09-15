package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class DACUserRole {

    @JsonProperty
    private Integer roleId;

    @JsonProperty
    private String name;

    @JsonProperty
    private Boolean emailPreference;

    @JsonProperty
    private String status;

    @JsonProperty
    private String rationale;

    @JsonProperty
    private Boolean profileCompleted;


    public DACUserRole(){
    }

    public DACUserRole(Integer roleId, String name){
        this.roleId = roleId;
        this.name = name;
        this.emailPreference = true;
    }

    public DACUserRole(Integer roleId, String name, Boolean emailPreference){
        this.roleId = roleId;
        this.name = name;
        this.emailPreference = emailPreference;
    }

    public DACUserRole(Integer roleId, String name, Boolean emailPreference, String rationale, String status){
        this.roleId = roleId;
        this.name = name;
        this.emailPreference = emailPreference;
        this.rationale = rationale;
        this.status = status;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEmailPreference() {
        return emailPreference;
    }

    public void setEmailPreference(Boolean emailPreference) {
        this.emailPreference = emailPreference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public Boolean getProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(Boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.roleId, this.name);
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof DACUserRole)) { return false; }
        DACUserRole otherConsent = (DACUserRole) o;
        return Objects.equal(this.getRoleId(), otherConsent.getRoleId());
    }


}
