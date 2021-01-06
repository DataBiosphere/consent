package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import java.util.Date;

public class Election {

    @JsonProperty
    private Integer electionId;

    @JsonProperty
    private String electionType;

    @JsonProperty
    private Boolean finalVote;

    @JsonProperty
    private String status;

    @JsonProperty
    private Date createDate;

    @JsonProperty
    private Date lastUpdate;

    @JsonProperty
    private Date finalVoteDate;

    @JsonProperty
    private String referenceId;

    @JsonProperty
    private String finalRationale;

    @JsonProperty
    private Boolean finalAccessVote;

    @JsonProperty
    private Integer dataSetId;

    @JsonProperty
    private String displayId;

    @JsonProperty
    private String dataUseLetter;

    @JsonProperty
    private String dulName;

    @JsonProperty
    private Boolean archived;

    @JsonProperty
    private Integer version;

    @JsonProperty
    private String consentGroupName;

    @JsonProperty
    private String projectTitle;

    public Election() {
    }

    public Election(Integer electionId, String electionType,
            String status, Date createDate,
            String referenceId, Date lastUpdate, Boolean finalAccessVote, Integer dataSetId) {
        this.electionId = electionId;
        this.electionType = electionType;
        this.status = status;
        this.createDate = createDate;
        this.referenceId = referenceId;
        this.lastUpdate = lastUpdate;
        this.finalAccessVote = finalAccessVote;
        this.dataSetId = dataSetId;
    }

    public Election(Integer electionId, String electionType,
                    String status, Date createDate,
                    String referenceId, Date lastUpdate, Boolean finalAccessVote, Integer dataSetId, Boolean archived,
                    String dulName, String dataUseLetter) {
        this.electionId = electionId;
        this.electionType = electionType;
        this.status = status;
        this.createDate = createDate;
        this.referenceId = referenceId;
        this.lastUpdate = lastUpdate;
        this.finalAccessVote = finalAccessVote;
        this.dataSetId = dataSetId;
        this.archived = archived;
        this.dulName = dulName;
        this.dataUseLetter = dataUseLetter;
    }

    public Integer getElectionId() {
        return electionId;
    }

    public void setElectionId(Integer electionId) {
        this.electionId = electionId;
    }

    public String getElectionType() {
        return electionType;
    }

    public void setElectionType(String electionType) {
        this.electionType = electionType;
    }

    public Boolean getFinalVote() {
        return finalVote;
    }

    public void setFinalVote(Boolean finalVote) {
        this.finalVote = finalVote;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getLastUpdateDate() {
        return lastUpdate;
    }

    public void setLastUpdateDate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getFinalVoteDate() {
        return finalVoteDate;
    }

    public void setFinalVoteDate(Date finalVoteDate) {
        this.finalVoteDate = finalVoteDate;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getFinalRationale() {
        return finalRationale;
    }

    public void setFinalRationale(String finalRationale) {
        this.finalRationale = finalRationale;
    }

    public Boolean getFinalAccessVote() {
        return finalAccessVote;
    }

    public void setFinalAccessVote(Boolean finalAccessVote) {
        this.finalAccessVote = finalAccessVote;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Integer getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(Integer dataSetId) {
        this.dataSetId = dataSetId;
    }

    public String getDisplayId() {
        return displayId;
    }

    public void setDisplayId(String displayId) {
        this.displayId = displayId;
    }

    public String getDataUseLetter() {
        return dataUseLetter;
    }

    public void setDataUseLetter(String dataUseLetter) {
        this.dataUseLetter = dataUseLetter;
    }

    public String getDulName() {
        return dulName;
    }

    public void setDulName(String dulName) { this.dulName = dulName; }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getConsentGroupName() { return consentGroupName; }

    public void setConsentGroupName(String consentGroupName) { this.consentGroupName = consentGroupName; }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Election election = (Election) o;
        return Objects.equal(electionId, election.electionId) &&
                Objects.equal(referenceId, election.referenceId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(electionId, referenceId);
    }

}
