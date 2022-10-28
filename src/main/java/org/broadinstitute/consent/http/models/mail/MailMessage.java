package org.broadinstitute.consent.http.models.mail;


import java.util.Date;

public class MailMessage {

    private Integer emailId;
    private Integer voteId;
    private Integer dacUserId;
    private String emailType;
    private Date dateSent;
    private String emailText;
    private String sendgridResponse;
    private Integer sendgridStatus;
    private Date createDate;

    public MailMessage(Integer emailId, Integer voteId, Integer dacUserId, String emailType, Date dateSent, String emailText, String sendgridResponse, Integer sendgridStatus, Date createDate) {
        this.emailId = emailId;
        this.voteId = voteId;
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

    public Integer getSendgridStatus() {
        return sendgridStatus;
    }

    public void setSendgridStatus(Integer sendgridStatus) {
        this.sendgridStatus = sendgridStatus;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }


}
