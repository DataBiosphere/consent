package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.gson.Gson;
import org.broadinstitute.consent.http.enumeration.UserRoles;

public class UserRole {

    @JsonProperty
    private Integer roleId;

    @JsonProperty
    private String name;

    public UserRole() {
    }

    public UserRole(Integer roleId, String name) {
        this.roleId = roleId;
        this.name = name;
    }

    /**
     * Convenience method for backwards compatibility support for older clients.
     *
     * @param json A json string that may or may not be correctly structured as a UserRole
     */
    public UserRole(String json) {
        Gson gson = new Gson();
        UserRole ur = gson.fromJson(json, UserRole.class);
        if (ur.getName() != null) {
            this.name = ur.getName();
        }
        if (ur.getRoleId() != null) {
            this.roleId = ur.getRoleId();
        } else {
            UserRoles r = UserRoles.getUserRoleFromName(this.getName());
            if (r != null) {
                this.setRoleId(r.getRoleId());
            }
        }
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

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
