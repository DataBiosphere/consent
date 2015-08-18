package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;


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
	private String electionStatus;

	@JsonProperty
	private Integer electionId;

	public ConsentManage() {
	}

	public ConsentManage(Consent consent) {
		this.consentId = consent.consentId;
		this.consentName = consent.name;
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

}
