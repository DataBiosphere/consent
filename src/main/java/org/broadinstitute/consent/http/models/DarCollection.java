package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jdbi.v3.json.Json;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

  @JsonProperty
  List<DataAccessRequest> dars;

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

  public List<DataAccessRequest> getDars() { return dars; }

  public void setDars(List<DataAccessRequest> dars) { this.dars = dars; }

  public void addDar(DataAccessRequest dar) {
    if (Objects.isNull(dars)) {
      this.setDars(new ArrayList<>());
    }
    dars.add(dar);

  }

}
