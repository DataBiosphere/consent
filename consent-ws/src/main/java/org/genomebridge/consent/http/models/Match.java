package org.genomebridge.consent.http.models;


import com.fasterxml.jackson.annotation.JsonProperty;

public class Match {

    @JsonProperty
    private Integer id;

    @JsonProperty
    private String consent;

    @JsonProperty
    private String purpose;

    @JsonProperty
    private Boolean match;

    public Match(){

    }

    public Match(Integer id, String consent, String purpose, Boolean match){
        this.id = id;
        this.consent = consent;
        this.purpose = purpose;
        this.match = match;
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

}
