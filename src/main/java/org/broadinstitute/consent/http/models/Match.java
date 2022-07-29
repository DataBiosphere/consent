package org.broadinstitute.consent.http.models;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class Match {

    @JsonProperty
    private Integer id;

    @JsonProperty
    private String consent;

    @JsonProperty
    private String purpose;

    @JsonProperty
    private Boolean match;

    @JsonProperty
    private Boolean failed;

    @JsonProperty
    private Date createDate;

    @JsonProperty
    private String algorithmVersion;


    public Match(Integer id, String consent, String purpose, Boolean match, Boolean failed, Date createDate, String algorithmVersion){
        this.id = id;
        this.consent = consent;
        this.purpose = purpose;
        this.match = match;
        this.failed = failed;
        this.createDate = createDate;
        this.algorithmVersion = algorithmVersion;
    }

    public Match(){
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getConsent() {
        return consent;
    }

    public void setConsent(String consent) {
        this.consent = consent;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Boolean getMatch() {
        return match;
    }

    public void setMatch(Boolean match) {
        this.match = match;
    }

    public Boolean getFailed() {
        return failed;
    }

    public void setFailed(Boolean failed) {
        this.failed = failed;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getAlgorithmVersion() {
        return algorithmVersion;
    }

    public void setAlgorithmVersion(String algorithmVersion) {
        this.algorithmVersion = algorithmVersion;
    }
}
