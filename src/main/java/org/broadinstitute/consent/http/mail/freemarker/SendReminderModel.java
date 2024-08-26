package org.broadinstitute.consent.http.mail.freemarker;

/* This model works for the Send Reminder Message template */
public class SendReminderModel {
  private final String userName;
  private final String entityName;
  private final String serverUrl;

  public SendReminderModel(String user, String entityName, String serverUrl) {
    this.userName = user;
    this.entityName = entityName;
    this.serverUrl = serverUrl;
  }

  public String getUserName() {
    return userName;
  }

  public String getEntityName() {
    return entityName;
  }

  public String getServerUrl() {
    return serverUrl;
  }

}
