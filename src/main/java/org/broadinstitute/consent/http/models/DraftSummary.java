package org.broadinstitute.consent.http.models;

import java.util.Date;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.DraftType;

public class DraftSummary {

  private UUID id;
  private String name;
  private Date createDate;
  private Date updateDate;
  private DraftType draftType;

  public DraftSummary(UUID id, String name, Date createDate, Date updateDate, DraftType draftType) {
    this.setId(id);
    this.setName(name);
    this.setCreateDate(createDate);
    this.setUpdateDate(updateDate);
    this.setDraftType(draftType);
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

  public DraftType getDraftType() { return draftType; }

  public void setDraftType(DraftType draftType) { this.draftType = draftType; }
}
