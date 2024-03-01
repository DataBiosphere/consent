package org.broadinstitute.consent.http.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DataAccessAgreement {

  private Integer id;
  private Integer createUserId;
  private Instant createDate;
  private Integer updateUserId;
  private Instant updateDate;
  private Integer initialDacId;
  private FileStorageObject file;
  private List<Dac> associatedDacs;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getCreateUserId() {
    return createUserId;
  }

  public void setCreateUserId(Integer createUserId) {
    this.createUserId = createUserId;
  }

  public Instant getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Instant createDate) {
    this.createDate = createDate;
  }

  public Integer getUpdateUserId() {
    return updateUserId;
  }

  public void setUpdateUserId(Integer updateUserId) {
    this.updateUserId = updateUserId;
  }

  public Instant getUpdateDate() {
    return updateDate;
  }

  public void setUpdateDate(Instant updateDate) {
    this.updateDate = updateDate;
  }

  public Integer getInitialDacId() {
    return initialDacId;
  }

  public void setInitialDacId(Integer initialDacId) {
    this.initialDacId = initialDacId;
  }

  public FileStorageObject getFile() {
    return file;
  }

  public void setFile(FileStorageObject file) {
    this.file = file;
  }

  public List<Dac> getAssociatedDacs() {
    return associatedDacs;
  }

  public void setAssociatedDacs(List<Dac> associatedDacs) {
    this.associatedDacs = associatedDacs;
  }

  public void addDac(Dac newDac) {
    if (this.associatedDacs == null) {
      this.associatedDacs = new ArrayList<Dac>();
    }
    if (this.associatedDacs.contains(newDac)) {
      return;
    }
    this.associatedDacs.add(newDac);
  }
}
