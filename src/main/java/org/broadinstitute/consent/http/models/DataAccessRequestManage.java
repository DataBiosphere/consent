package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;
import java.util.Date;


/**
 * DataAccessRequestManage will be used to manage which cases should be presented to the DAC for an evaluation.
 * Election status should be unreviewed if DataAccessRequest doesn't have any associated election.
 * If it has, the status should be either open, cancelled or closed.
 * It should always return information for the last election if it exists
 */
public class DataAccessRequestManage {

    @JsonProperty
    private String electionStatus;

    @JsonProperty
    private String status;

    @JsonProperty
    private String rus;

    @JsonProperty
    private String dataRequestId;

    @JsonProperty
    private String projectTitle;

    @JsonProperty
    private String frontEndId;

    @JsonProperty
    private Integer electionId;

    @JsonProperty
    private Timestamp createDate;

    @JsonProperty
    private Date sortDate;

    @JsonProperty
    private Boolean electionVote;

    @JsonProperty
    private Boolean isCanceled;

    @JsonProperty
    private Boolean needsApproval;

    @JsonProperty
    private String dataSetElectionResult;

    @JsonProperty
    private DACUser ownerUser;

    public DataAccessRequestManage() {
    }

    public String getElectionStatus() {
        return electionStatus;
    }

    public void setElectionStatus(String electionStatus) {
        this.electionStatus = electionStatus;
    }

    public Integer getElectionId() {
        return electionId;
    }

    public void setElectionId(Integer electionId) {
        this.electionId = electionId;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public String getRus() {
        return rus;
    }

    public void setRus(String rus) {
        this.rus = rus;
    }

    public String getDataRequestId() {
        return dataRequestId;
    }

    public void setDataRequestId(String dataRequestId) {
        this.dataRequestId = dataRequestId;
    }

    public String getFrontEndId() {
        return frontEndId;
    }

    public void setFrontEndId(String frontEndId) {
        this.frontEndId = frontEndId;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public Date getSortDate() {
        return sortDate;
    }

    public void setSortDate(Date sortDate) {
        this.sortDate = sortDate;
    }

    public Boolean getElectionVote() {
        return electionVote;
    }

    public void setElectionVote(Boolean electionVote) {
        this.electionVote = electionVote;
    }

    public Boolean getIsCanceled() {
        return isCanceled;
    }

    public void setIsCanceled(Boolean isCanceled) {
        this.isCanceled = isCanceled;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getNeedsApproval() {
        return needsApproval;
    }

    public void setNeedsApproval(Boolean needsApproval) {
        this.needsApproval = needsApproval;
    }

    public String getDataSetElectionResult() {
        return dataSetElectionResult;
    }

    public void setDataSetElectionResult(String dataSetElectionResult) {
        this.dataSetElectionResult = dataSetElectionResult;
    }

    public DACUser getOwnerUser() {
        return ownerUser;
    }

    public void setOwnerUser(DACUser ownerUser) {
        this.ownerUser = ownerUser;
    }
}