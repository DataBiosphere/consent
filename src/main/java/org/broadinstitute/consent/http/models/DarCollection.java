package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
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
  Integer createUser;

  @JsonProperty
  Timestamp updateDate;

  @JsonProperty
  Integer updateUser;

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

  public Integer getCreateUser() {
    return createUser;
  }

  public void setCreateUser(Integer createUser) {
    this.createUser = createUser;
  }

  public Date getUpdateDate() {
    return updateDate;
  }

  public void setUpdateDate(Timestamp updateDate) {
    this.updateDate = updateDate;
  }

  public Integer getUpdateUser() {
    return updateUser;
  }

  public void setUpdateUser(Integer updateUser) {
    this.updateUser = updateUser;
  }

  public List<DataAccessRequest> getDars() {
    if (Objects.isNull(dars)) {
      return new ArrayList<>();
    }
    return dars;
  }

  public void setDars(List<DataAccessRequest> dars) { this.dars = dars; }

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


}
