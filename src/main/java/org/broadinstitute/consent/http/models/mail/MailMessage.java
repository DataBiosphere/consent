package org.broadinstitute.consent.http.models.mail;


import java.util.Date;

public class MailMessage {

    private Integer emailId;
    private Integer voteId;
    private Integer electionId;
    private Integer dacUserId;
    private String emailType;
    private Date dateSent;
    private String emailText;
    private String sendgridResponse;
    private Integer sendgridStatus;
    private Date createDate;

    public MailMessage(Integer emailId, Integer voteId, Integer electionId, Integer dacUserId, String emailType, Date dateSent, String emailText, String sendgrid_response, Integer sendgrid_status, Date create_date) {
        this.emailId = emailId;
        this.voteId = voteId;
        this.electionId = electionId;
        this.dacUserId = dacUserId;
        this.emailType = emailType;
        this.dateSent = dateSent;
        this.emailText = emailText;
        this.sendgridResponse = sendgridResponse;
        this.sendgridStatus = sendgridStatus;
        this.createDate = createDate;
    }

    public Integer getEmailId() {
        return emailId;
    }

    public void setEmailId(Integer emailId) {
        this.emailId = emailId;
    }

    public Integer getVoteId() {
        return voteId;
    }

    public void setVoteId(Integer voteId) {
        this.voteId = voteId;
    }

    public Integer getElectionId() {
        return electionId;
    }

    public void setElectionId(Integer electionId) {
        this.electionId = electionId;
    }

    public Integer getUserId() {
        return dacUserId;
    }

    public void setDacUserId(Integer dacUserId) {
        this.dacUserId = dacUserId;
    }

    public String getEmailType() {
        return emailType;
    }

    public void setEmailType(String emailType) {
        this.emailType = emailType;
    }

    public Date getDateSent() {
        return dateSent;
    }

    public void setDateSent(Date dateSent) {
        this.dateSent = dateSent;
    }

    public String getEmailText() {
        return emailText;
    }

    public void setEmailText(String emailText) {
        this.emailText = emailText;
    }

    public String getSendgridResponse() {
        return sendgridResponse;
    }

    public void setSendgridResponse(String sendgridResponse) {
        this.sendgridResponse = sendgridResponse;
    }

    public String getSendgridStatus() {
        return sendgridStatus;
    }

    public void setSendgridStatus(String sendgridStatus) {
        this.sendgridStatus = sendgridStatus;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }


}
