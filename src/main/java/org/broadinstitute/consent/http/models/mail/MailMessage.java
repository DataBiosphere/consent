package org.broadinstitute.consent.http.models.mail;

import java.util.Date;

public class MailMessage {

  private String entityReferenceId;
  private Integer emailId;
  private Integer voteId;
  private Integer userId;
  private Integer emailType;
  private Date dateSent;
  private String emailText;
  private String sendgridResponse;
  private Integer sendgridStatus;
  private Date createDate;

  public MailMessage(
      String entityReferenceId,
      Integer emailId,
      Integer voteId,
      Integer userId,
      Integer emailType,
      Date dateSent,
      String emailText,
      String sendgridResponse,
      Integer sendgridStatus,
      Date createDate) {
    this.entityReferenceId = entityReferenceId;
    this.emailId = emailId;
    this.voteId = voteId;
    this.userId = userId;
    this.emailType = emailType;
    this.dateSent = dateSent;
    this.emailText = emailText;
    this.sendgridResponse = sendgridResponse;
    this.sendgridStatus = sendgridStatus;
    this.createDate = createDate;
  }

  public String getEntityReferenceId() {
    return entityReferenceId;
  }

  public void setEntityReferenceId(String entityReferenceId) {
    this.entityReferenceId = entityReferenceId;
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
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public Integer getEmailType() {
    return emailType;
  }

  public void setEmailType(Integer emailType) {
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
