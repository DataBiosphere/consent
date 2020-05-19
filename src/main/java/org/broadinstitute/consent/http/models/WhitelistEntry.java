package org.broadinstitute.consent.http.models;

import com.google.common.base.Objects;
import com.google.gson.Gson;

public class WhitelistEntry {

    String organization;
    String commonsId;
    String name;
    String email;
    String signingOfficialName;
    String signingOfficialEmail;
    String itDirectorName;
    String itDirectorEmail;

    public WhitelistEntry() {
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getCommonsId() {
        return commonsId;
    }

    public void setCommonsId(String commonsId) {
        this.commonsId = commonsId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSigningOfficialName() {
        return signingOfficialName;
    }

    public void setSigningOfficialName(String signingOfficialName) {
        this.signingOfficialName = signingOfficialName;
    }

    public String getSigningOfficialEmail() {
        return signingOfficialEmail;
    }

    public void setSigningOfficialEmail(String signingOfficialEmail) {
        this.signingOfficialEmail = signingOfficialEmail;
    }

    public String getItDirectorName() {
        return itDirectorName;
    }

    public void setItDirectorName(String itDirectorName) {
        this.itDirectorName = itDirectorName;
    }

    public String getItDirectorEmail() {
        return itDirectorEmail;
    }

    public void setItDirectorEmail(String itDirectorEmail) {
        this.itDirectorEmail = itDirectorEmail;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WhitelistEntry entry = (WhitelistEntry) o;
        return Objects.equal(organization, entry.organization) &&
                Objects.equal(commonsId, entry.commonsId) &&
                Objects.equal(name, entry.name) &&
                Objects.equal(email, entry.email) &&
                Objects.equal(signingOfficialName, entry.signingOfficialName) &&
                Objects.equal(signingOfficialEmail, entry.signingOfficialEmail) &&
                Objects.equal(itDirectorName, entry.itDirectorName) &&
                Objects.equal(itDirectorEmail, entry.itDirectorEmail);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(organization, commonsId, name, email, signingOfficialName, signingOfficialEmail, itDirectorName, itDirectorEmail);
    }

}
