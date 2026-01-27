package org.broadinstitute.consent.http.models;

import java.time.Instant;

public class FeatureFlag {

  private String id;
  private String value;
  private Instant createDate;
  private Instant updateDate;

  public FeatureFlag() {}

  public FeatureFlag(String id, String value) {
    this.id = id;
    this.value = value;
  }

  public FeatureFlag(String id, String value, Instant createDate, Instant updateDate) {
    this.id = id;
    this.value = value;
    this.createDate = createDate;
    this.updateDate = updateDate;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Instant getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Instant createDate) {
    this.createDate = createDate;
  }

  public Instant getUpdateDate() {
    return updateDate;
  }

  public void setUpdateDate(Instant updateDate) {
    this.updateDate = updateDate;
  }
}
