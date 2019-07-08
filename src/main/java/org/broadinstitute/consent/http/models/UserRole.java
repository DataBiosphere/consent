package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class UserRole {

    @JsonProperty
    private Integer roleId;

    @JsonProperty
    private String name;

    @JsonProperty
    private String status;

    @JsonProperty
    private String rationale;

    @JsonProperty
    private Boolean profileCompleted;


    public UserRole() {
    }

    public UserRole(Integer roleId, String name) {
        this.roleId = roleId;
        this.name = name;
    }

    public UserRole(Integer roleId, String name, String rationale, String status) {
        this.roleId = roleId;
        this.name = name;
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
        if (!(o instanceof UserRole)) {
            return false;
        }
        UserRole otherConsent = (UserRole) o;
        return Objects.equal(this.getRoleId(), otherConsent.getRoleId());
    }


}
