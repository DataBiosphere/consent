package org.broadinstitute.consent.http.mail.freemarker;

public class NewDarRequestModel {

  /* This model works for templates: new-request. */

  private final String serverUrl;
  private final String userName;
  private final String entityId;

  public NewDarRequestModel(String serverUrl, String userName, String entityId) {
    this.serverUrl = serverUrl;
    this.userName = userName;
    this.entityId = entityId;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public String getUserName() {
    return userName;
  }

  public String getEntityId() {
    return entityId;
  }
}
