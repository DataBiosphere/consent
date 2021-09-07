package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
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
  Integer createUserId;

  @JsonProperty
  Timestamp updateDate;

  @JsonProperty
  Integer updateUserId;

  @JsonProperty
  List<DataAccessRequest> dars;

  public DarCollection() {this.createDate = new Timestamp(System.currentTimeMillis()); }

  public Integer getDarCollectionId() {
    return darCollectionId;
  }

  public DarCollection setDarCollectionId(Integer darCollectionId) {
    this.darCollectionId = darCollectionId;
    return this;
  }

  public String getDarCode() {
    return darCode;
  }

  public DarCollection setDarCode(String darCode) {
    this.darCode = darCode;
    return this;
  }

  public Timestamp getCreateDate() {
    return createDate;
  }

  public DarCollection setCreateDate(Timestamp createDate) {
    this.createDate = createDate;
    return this;
  }

  public Integer getCreateUserId() {
    return createUserId;
  }

  public DarCollection setCreateUserId(Integer createUserId) {
    this.createUserId = createUserId;
    return this;
  }

  public Date getUpdateDate() {
    return updateDate;
  }

  public DarCollection setUpdateDate(Timestamp updateDate) {
    this.updateDate = updateDate;
    return this;
  }

  public Integer getUpdateUserId() {
    return updateUserId;
  }

  public DarCollection setUpdateUserId(Integer updateUserId) {
    this.updateUserId = updateUserId;
    return this;
  }

  public List<DataAccessRequest> getDars() {
    if (Objects.isNull(dars)) {
      return new ArrayList<>();
    }
    return dars;
  }

  public DarCollection setDars(List<DataAccessRequest> dars) {
    this.dars = dars;
    return this;
  }

  public void addDar(DataAccessRequest dar) {
    if (Objects.isNull(dars)) {
      this.setDars(new ArrayList<>());
    }
    dars.add(dar);
  }

  @Override
  public boolean equals(Object obj){
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;

    DarCollection other = (DarCollection) obj;
    return new EqualsBuilder()
      .append(this.getDarCollectionId(), other.getDarCollectionId())
      .append(this.getDarCode(), other.getDarCode())
      .isEquals();
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
