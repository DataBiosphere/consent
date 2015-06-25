package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DACUser {

	@JsonProperty
	private Integer dacUserId;

	@JsonProperty
	private String email;
	
	@JsonProperty
	private String displayName;
	
	@JsonProperty
	private String memberStatus;

	
	public DACUser() {
	}

	public DACUser(Integer dacUserId, String email, String displayName,
			String memberStatus) {
		this.dacUserId = dacUserId;
		this.email = email;
		this.displayName = displayName;
		this.memberStatus = memberStatus;
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

	public String getMemberStatus() {
		return memberStatus;
	}

	public void setMemberStatus(String memberStatus) {
		this.memberStatus = memberStatus;
	}
	
}
