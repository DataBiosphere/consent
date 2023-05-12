package org.broadinstitute.consent.http.models;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.UserFields;

public class NIHUserAccount {

  @SerializedName("linkedNihUsername")
  private String nihUsername;

  private ArrayList datasetPermissions;

  @SerializedName("linkExpireTime")
  private String eraExpiration;

  private Boolean status;

  public NIHUserAccount() {
  }

  public NIHUserAccount(String nihUsername, ArrayList datasetPermissions, String eraExpiration,
      Boolean status) {
    this.nihUsername = nihUsername;
    this.datasetPermissions = datasetPermissions;
    this.eraExpiration = eraExpiration;
    this.status = status;
  }

  public Map<String, String> getNihMap() {
    Map<String, String> nihComponents = new HashMap<>();
    nihComponents.put(UserFields.ERA_STATUS.getValue(), Boolean.TRUE.toString());
    nihComponents.put(UserFields.ERA_EXPIRATION_DATE.getValue(), this.eraExpiration);
    return nihComponents;
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


  public ArrayList getDatasetPermissions() {
    return datasetPermissions;
  }

  public void setDatasetPermissions(ArrayList datasetPermissions) {
    this.datasetPermissions = datasetPermissions;
  }

  public Boolean getStatus() {
    return status;
  }

  public void setStatus(Boolean status) {
    this.status = status;
  }

}
