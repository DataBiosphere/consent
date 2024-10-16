package org.broadinstitute.consent.http.models;

import java.util.Date;
import java.util.UUID;

public class DraftSummary {

  private UUID id;
  private String name;
  private Date createDate;
  private Date updateDate;

  public DraftSummary(UUID id, String name, Date createDate, Date updateDate) {
    this.setId(id);
    this.setName(name);
    this.setCreateDate(createDate);
    this.setUpdateDate(updateDate);
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Date getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Date createDate) {
    this.createDate = createDate;
  }

  public Date getUpdateDate() {
    return updateDate;
  }

  public void setUpdateDate(Date updateDate) {
    this.updateDate = updateDate;
  }
}
