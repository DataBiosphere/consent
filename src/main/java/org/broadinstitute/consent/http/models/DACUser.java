package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DACUser {

    private static final Logger logger = LoggerFactory.getLogger(DACUser.class.getName());

    @JsonProperty
    private Integer dacUserId;

    @JsonProperty
    private String email;

    @JsonProperty
    private String displayName;

    @JsonProperty
    private Date createDate;

    @JsonProperty
    private String additionalEmail;

    @JsonProperty
    private List<UserRole> roles;

    @JsonProperty
    private Boolean emailPreference;

    @JsonProperty
    private String status;

    @JsonProperty
    private String rationale;

    @JsonProperty
    private Boolean profileCompleted;

    public DACUser() {
    }

    public DACUser(Integer dacUserId, String email, String displayName, Date createDate) {
        this.dacUserId = dacUserId;
        this.email = email;
        this.displayName = displayName;
        this.createDate = createDate;
    }

    public DACUser(Integer dacUserId, String email, String displayName, Date createDate, String additionalEmail) {
        this.dacUserId = dacUserId;
        this.email = email;
        this.displayName = displayName;
        this.createDate = createDate;
        this.additionalEmail = additionalEmail;
    }

    public DACUser(Integer dacUserId, String email, String displayName,
                   String isApproved, Date createDate, List<UserRole> roles, String additionalEmail) {
        this.dacUserId = dacUserId;
        this.email = email;
        this.displayName = displayName;
        this.createDate = createDate;
        this.roles = roles;
        this.additionalEmail = additionalEmail;
    }

    /**
     * Convenience method for backwards compatibility support for older clients.
     *
     * @param json A json string that may or may not be correctly structured as a DACUser
     */
    public DACUser(String json) {
        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
        if (jsonObject.has("dacUserId") && !jsonObject.get("dacUserId").isJsonNull()) {
            try {
                this.setDacUserId(jsonObject.get("dacUserId").getAsInt());
            } catch (Exception e) {
                logger.debug(e.getMessage());
            }
        }
        if (jsonObject.has("email") && !jsonObject.get("email").isJsonNull()) {
            try {
                this.setEmail(jsonObject.get("email").getAsString());
            } catch (Exception e) {
                logger.debug(e.getMessage());
            }
        }
        if (jsonObject.has("displayName") && !jsonObject.get("displayName").isJsonNull()) {
            try {
                this.setDisplayName(jsonObject.get("displayName").getAsString());
            } catch (Exception e) {
                logger.debug(e.getMessage());
            }
        }
        if (jsonObject.has("additionalEmail") && !jsonObject.get("additionalEmail").isJsonNull()) {
            try {
                this.setAdditionalEmail(jsonObject.get("additionalEmail").getAsString());
            } catch (Exception e) {
                logger.debug(e.getMessage());
            }
        }
        if (jsonObject.has("emailPreference") && !jsonObject.get("emailPreference").isJsonNull()) {
            try {
                this.setEmailPreference(jsonObject.get("emailPreference").getAsBoolean());
            } catch (Exception e) {
                logger.debug(e.getMessage());
            }
        }
        if (jsonObject.has("roles") && !jsonObject.get("roles").isJsonNull()) {
            this.setRoles(new ArrayList<>());
            try {
                jsonObject.get("roles").getAsJsonArray().forEach(jsonElement ->
                {
                    this.getRoles().add(new UserRole(jsonElement.toString()));
                });
            } catch (Exception e) {
                logger.debug(e.getMessage());
            }
        }
        if (jsonObject.has("status") && !jsonObject.get("status").isJsonNull()) {
            try {
                this.setStatus(jsonObject.get("status").getAsString());
            } catch (Exception e) {
                logger.debug(e.getMessage());
            }
        }
        if (jsonObject.has("rationale") && !jsonObject.get("rationale").isJsonNull()) {
            try {
                this.setRationale(jsonObject.get("rationale").getAsString());
            } catch (Exception e) {
                logger.debug(e.getMessage());
            }
        }
    }

    public Integer getDacUserId() {
        return dacUserId;
    }

    public void setDacUserId(Integer dacUserId) {
        this.dacUserId = dacUserId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<UserRole> getRoles() {
        return roles;
    }

    public void setRoles(List<UserRole> roles) {
        this.roles = roles;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getAdditionalEmail() {
        return additionalEmail;
    }

    public void setAdditionalEmail(String additionalEmail) {
        this.additionalEmail = additionalEmail;
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
    public int hashCode(){
        return  dacUserId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        DACUser other = (DACUser) obj;
        return new EqualsBuilder().append(dacUserId, other.dacUserId).isEquals();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
