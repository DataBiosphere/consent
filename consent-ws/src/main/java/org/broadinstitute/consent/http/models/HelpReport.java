package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class HelpReport {

    @JsonProperty
    private Integer reportId;

    @JsonProperty
    private String userName;

    @JsonProperty
    private Date createDate;

    @JsonProperty
    private String subject;

    @JsonProperty
    private String description;

    @JsonProperty
    private Integer userId;

    public HelpReport() {
    }

    public HelpReport(Integer reportId, String userName, Date createDate, String subject, String description) {
        this.reportId = reportId;
        this.userName = userName;
        this.createDate = createDate;
        this.subject = subject;
        this.description = description;
    }

    public Integer getReportId() {
        return reportId;
    }

    public void setReportId(Integer reportId) {
        this.reportId = reportId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}
