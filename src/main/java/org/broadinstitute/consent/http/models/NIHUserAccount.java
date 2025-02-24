package org.broadinstitute.consent.http.models;

import com.google.gson.annotations.SerializedName;
import java.util.Collection;

public class NIHUserAccount {

  @SerializedName("linkedNihUsername")
  private String nihUsername;

  private Collection<String> datasetPermissions;

  @SerializedName("linkExpireTime")
  private String eraExpiration;

  private Boolean status;

  public NIHUserAccount() {
  }

  public NIHUserAccount(String nihUsername, Collection<String> datasetPermissions, String eraExpiration,
      Boolean status) {
    this.nihUsername = nihUsername;
    this.datasetPermissions = datasetPermissions;
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

  public Collection<String> getDatasetPermissions() {
    return datasetPermissions;
  }

  public void setDatasetPermissions(Collection<String> datasetPermissions) {
    this.datasetPermissions = datasetPermissions;
  }

  public Boolean getStatus() {
    return status;
  }

  public void setStatus(Boolean status) {
    this.status = status;
  }

}
