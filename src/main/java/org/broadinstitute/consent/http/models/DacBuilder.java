package org.broadinstitute.consent.http.models;

import java.util.Date;
import java.util.List;

/**
 * Convenience class for building Dacs.
 */
public class DacBuilder {

  private Dac dac;

  public DacBuilder() {
    this.dac = new Dac();
  }

  public Dac build() {
    return this.dac;
  }

  public DacBuilder setDacId(Integer dacId) {
    this.dac.setDacId(dacId);
    return this;
  }

  public DacBuilder setName(String name) {
    this.dac.setName(name);
    return this;
  }

  public DacBuilder setDescription(String description) {
    this.dac.setDescription(description);
    return this;
  }

  public DacBuilder setEmail(String email) {
    this.dac.setEmail(email);
    return this;
  }

  public DacBuilder setAssociatedDaa(DataAccessAgreement associatedDaa) {
    this.dac.setAssociatedDaa(associatedDaa);
    return this;
  }

  public DacBuilder setCreateDate(Date createDate) {
    this.dac.setCreateDate(createDate);
    return this;
  }

  public DacBuilder setUpdateDate(Date updateDate) {
    this.dac.setUpdateDate(updateDate);
    return this;
  }

  public DacBuilder setChairpersons(List<User> chairpersons) {
    this.dac.setChairpersons(chairpersons);
    return this;
  }

  public DacBuilder setMembers(List<User> members) {
    this.dac.setMembers(members);
    return this;
  }

}
