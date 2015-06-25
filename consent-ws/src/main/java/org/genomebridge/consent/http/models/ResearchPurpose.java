package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResearchPurpose {
	
	@JsonProperty
	private Integer purposeId;
	
	@JsonProperty
	private String purpose;
	
	public ResearchPurpose(){		
	}
	
	public ResearchPurpose(Integer purposeId, String purpose) {
		this.purposeId = purposeId;
		this.purpose = purpose;
	}
	public Integer getPurposeId() {
		return purposeId;
	}
	public void setPurposeId(Integer purposeId) {
		this.purposeId = purposeId;
	}
	public String getPurpose() {
		return purpose;
	}
	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}
	
	

}
