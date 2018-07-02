package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;


/**
 * ConsentManage will be used to manage which cases should be presented to the DAC for an evaluation.
 * Election status should be unreviewed if consent doesn't have any associated election.
 * If it has, the status should be either open, cancelled or closed.
 * It should always return information for the last election if it exists
 */
public class ConsentManage {

	@JsonProperty
	private String consentId;

	@JsonProperty
	private String consentName;

    @JsonProperty
    private Boolean editable;

    @JsonProperty
	private String electionStatus;

	@JsonProperty
	private Integer electionId;

	@JsonProperty
	private Timestamp createDate;

	@JsonProperty
	private Timestamp sortDate;

	@JsonProperty
	private String version;

	@JsonProperty
	private Boolean archived;

	@JsonProperty
	private String groupName;

	@JsonProperty
	private Boolean updateStatus;

	public ConsentManage() {
	}

	public ConsentManage(Consent consent) {
		this.consentId = consent.getConsentId();
		this.consentName = consent.getName();
		this.createDate = consent.getCreateDate();
		this.sortDate = consent.getSortDate();
	    this.editable = null;
	    this.groupName = consent.getGroupName();
	    this.updateStatus = consent.getUpdated();
    }

	public Timestamp getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Timestamp createDate) {
		this.createDate = createDate;
	}

	public Timestamp getSortDate() {
		return sortDate;
	}

	public void setSortDate(Timestamp sortDate) {
		this.sortDate = sortDate;
	}

	public String getConsentId() {
		return consentId;
	}

	public void setConsentId(String consentId) {
		this.consentId = consentId;
	}

	public String getConsentName() {
		return consentName;
	}

	public void setConsentName(String consentName) {
		this.consentName = consentName;
	}

    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
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

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Boolean getArchived() {
		return archived;
	}

	public void setArchived(Boolean archived) {
		this.archived = archived;
	}

	public String getGroupName() { return groupName; }

	public void setGroupName(String groupName) { this.groupName = groupName; }

	public Boolean getUpdateStatus() {
		return updateStatus;
	}

	public void setUpdateStatus(boolean updateStatus) {
		this.updateStatus = updateStatus;
	}

}
