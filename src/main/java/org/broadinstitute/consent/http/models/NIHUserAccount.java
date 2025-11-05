package org.broadinstitute.consent.http.models;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;

public class NIHUserAccount {

  @SerializedName("linkedNihUsername")
  private String nihUsername;

  @SerializedName("linkExpireTime")
  private String eraExpiration;

  private Boolean status;

  public NIHUserAccount() {}

  public NIHUserAccount(String nihUsername, String eraExpiration, Boolean status) {
    this.nihUsername = nihUsername;
    this.eraExpiration = eraExpiration;
    this.status = status;
  }

  public String getNihUsername() {
    return nihUsername;
  }

  public void setNihUsername(String nihUsername) {
    this.nihUsername = nihUsername;
  }

  public String getEraExpiration() {
    return eraExpiration;
  }

  public void setEraExpiration(String eraExpiration) {
    this.eraExpiration = eraExpiration;
  }

  public Boolean getStatus() {
    return status;
  }

  public void setStatus(Boolean status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof NIHUserAccount that)) {
      return false;
    }
    return Objects.equals(nihUsername, that.nihUsername)
        && Objects.equals(eraExpiration, that.eraExpiration)
        && Objects.equals(status, that.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nihUsername, eraExpiration, status);
  }
}
