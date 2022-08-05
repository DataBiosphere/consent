package org.broadinstitute.consent.http.models;


import org.broadinstitute.consent.http.enumeration.MatchAlgorithm;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Match {

    private Integer id;

    private String consent;

    private String purpose;

    private Boolean match;

    private Boolean failed;

    private Date createDate;

    private String algorithmVersion;

    private List<String> failureReasons;

    @Deprecated
    public Match(Integer id, String consent, String purpose, Boolean match, Boolean failed, Date createDate){
        this.id = id;
        this.consent = consent;
        this.purpose = purpose;
        this.match = match;
        this.failed = failed;
        this.createDate = createDate;
        this.algorithmVersion = MatchAlgorithm.V1.getVersion();
    }

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

    public List<String> getFailureReasons() {
        if (Objects.isNull(this.failureReasons)) {
            return List.of();
        }
        return failureReasons;
    }

    public void setFailureReasons(List<String> failureReasons) {
        this.failureReasons = failureReasons;
    }

    public void addFailureReason(String reason) {
        if (Objects.isNull(this.failureReasons)) {
            this.failureReasons = new ArrayList<>();
        }
        if (!this.failureReasons.contains(reason) && !reason.isBlank()) {
            this.failureReasons.add(reason);
        }
    }
}
