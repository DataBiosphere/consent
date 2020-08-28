package org.broadinstitute.consent.http.models;

public class Collaborator {
  private Boolean approverStatus;
  private String email;
  private String eraCommonsId;
  private String name;
  private String title;
  private String uuid;

  public Boolean getApproverStatus() {
    return approverStatus;
  }

  public void setApproverStatus(Boolean approverStatus) {
    this.approverStatus = approverStatus;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getEraCommonsId() {
    return eraCommonsId;
  }

  public void setEraCommonsId(String eraCommonsId) {
    this.eraCommonsId = eraCommonsId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }
}
