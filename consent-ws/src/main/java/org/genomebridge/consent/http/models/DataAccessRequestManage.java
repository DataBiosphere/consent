package org.genomebridge.consent.http.models;

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
}
