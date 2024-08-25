package org.broadinstitute.consent.http.mail.freemarker;

/* This model works for the Send Reminder Message template */
public class TemplateModel {
  private final String userName;
  private final String electionType;
  private final String entityName;
  private final String serverUrl;

  public TemplateModel(String user, String election, String entityName, String serverUrl) {
    this.userName = user;
    this.electionType = election;
    this.entityName = entityName;
    this.serverUrl = serverUrl;
  }

  public String getUserName() {
    return userName;
  }

  public String getElectionType() {
    return electionType;
  }

  public String getEntityName() {
    return entityName;
  }

  public String getServerUrl() {
    return serverUrl;
  }

}
