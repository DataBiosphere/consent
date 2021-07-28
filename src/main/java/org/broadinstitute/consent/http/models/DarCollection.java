package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Timestamp;

//represents a multi-dataset access request
public class DarCollection {

  @JsonProperty
  Integer darCollectionId;

  @JsonProperty
  String darCode;

  @JsonProperty
  Timestamp createDate;

  @JsonProperty
  User createUser;

  @JsonProperty
  Timestamp updateDate;

  @JsonProperty
  User updateUser;

  public DarCollection() {this.createDate = new Timestamp(System.currentTimeMillis()); }

  public Integer getDarCollectionId() {
    return darCollectionId;
  }

  public void setDarCollectionId(Integer darCollectionId) {
    this.darCollectionId = darCollectionId;
  }

  public String getDarCode() {
    return darCode;
  }

  public void setDarCode(String darCode) {
    this.darCode = darCode;
  }

  public Timestamp getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Timestamp createDate) {
    this.createDate = createDate;
  }

  public User getCreateUser() {
    return createUser;
  }

  public void setCreateUser(User createUser) {
    this.createUser = createUser;
  }

  public Timestamp getUpdateDate() {
    return updateDate;
  }

  public void setUpdateDate(Timestamp updateDate) {
    this.updateDate = updateDate;
  }

  public User getUpdateUser() {
    return updateUser;
  }

  public void setUpdateUser(User updateUser) {
    this.updateUser = updateUser;
  }

}
