package org.broadinstitute.consent.http.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DataAccessAgreement {

  private Integer daaId;
  private Integer createUserId;
  private Instant createDate;
  private Integer updateUserId;
  private Instant updateDate;
  private Integer initialDacId;
  private FileStorageObject file;
  private List<Dac> dacs;

  public Integer getDaaId() {
    return daaId;
  }

  public void setDaaId(Integer daaId) {
    this.daaId = daaId;
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

  public List<Dac> getDacs() {
    return dacs;
  }

  public void setDacs(List<Dac> dacs) {
    this.dacs = dacs;
  }

  public void addDac(Dac dac) {
    if (this.dacs == null) {
      this.dacs = new ArrayList<>();
    }
    if (this.dacs
        .stream()
        .map(Dac::getDacId)
        .noneMatch(d -> d.equals(dac.getDacId()))) {
      this.dacs.add(dac);
    }
  }
}
