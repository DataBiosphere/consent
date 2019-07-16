package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * Convenience method for backwards compatibility support for older clients.
     *
     * @param json A json string that may or may not be correctly structured as a UserRole
     */
    public UserRole(String json) {
        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
        Logger log = LoggerFactory.getLogger(UserRole.class.getName());
        if (jsonObject.has("roleId") && !jsonObject.get("roleId").isJsonNull()) {
            try {
                this.setRoleId(jsonObject.get("roleId").getAsInt());
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
        }
        if (jsonObject.has("name") && !jsonObject.get("name").isJsonNull()) {
            try {
                this.setName(jsonObject.get("name").getAsString());
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
        }
        if (jsonObject.has("status") && !jsonObject.get("status").isJsonNull()) {
            try {
                this.setStatus(jsonObject.get("status").getAsString());
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
        }
        if (jsonObject.has("rationale") && !jsonObject.get("rationale").isJsonNull()) {
            try {
                this.setRationale(jsonObject.get("rationale").getAsString());
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
        }
        if (jsonObject.has("profileCompleted") && !jsonObject.get("profileCompleted").isJsonNull()) {
            try {
                this.setProfileCompleted(jsonObject.get("profileCompleted").getAsBoolean());
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
        }
        if (this.getRoleId() == null) {
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

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
