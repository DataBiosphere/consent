package org.broadinstitute.consent.http.mail.freemarker;

public class DarExpirationReminderModel {

  private final String userName;
  private final String darId;
  private final String serverUrl;

  public DarExpirationReminderModel(String userName, String darId, String serverUrl) {
    this.userName = userName;
    this.darId = darId;
    this.serverUrl = serverUrl;
  }

  public String getUserName() {
    return userName;
  }

  public String getDarId() {
    return darId;
  }

  public String getServerUrl() {
    return serverUrl;
  }
}
