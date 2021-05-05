package org.broadinstitute.consent.http.models;

import java.util.Date;

import org.apache.commons.lang3.builder.EqualsBuilder;

public class Institution {

  private Integer id;
  private String name;
  private String itDirectorName;
  private String itDirectorEmail;
  private Date createDate;
  private Integer createUserId;
  private Date updateDate;
  private Integer updateUserId;

  private User createUser;
  private User updateUser;

  //empty constructor sets all null values except create Date
  public Institution() {
    this.createDate = new Date();
  }

  public Institution(Integer id, String name, String itDirectorName, String itDirectorEmail, Integer createUserId, Date createDate) {
    this.id = id;
    this.name = name;
    this.itDirectorName = itDirectorName;
    this.itDirectorEmail = itDirectorEmail;
    this.createDate = createDate;
    this.createUserId = createUserId;
    this.updateDate = this.createDate;
    this.updateUserId = this.createUserId;
  }

  public Institution(Integer id, String name, String itDirectorName, String itDirectorEmail,
                     Integer createUserId, Date createDate, Integer updateUserId, Date updateDate) {
    this.id = id;
    this.name = name;
    this.itDirectorName = itDirectorName;
    this.itDirectorEmail = itDirectorEmail;
    this.createDate = createDate;
    this.createUserId = createUserId;
    this.updateDate = updateDate;
    this.updateUserId = updateUserId;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setItDirectorEmail(String itDirectorEmail) {
    this.itDirectorEmail = itDirectorEmail;
  }

  public void setItDirectorName(String itDirectorName) {
    this.itDirectorName = itDirectorName;
  }

  public void setCreateUserId(Integer createUserId) {
    this.createUserId = createUserId;
  }

  public void setCreateDate(Date date) {
    this.createDate = date;
  }

  public void setUpdateUserId(Integer updateUserId) {
    this.updateUserId = updateUserId;
  }

  public void setUpdateDate(Date updateDate) {
    this.updateDate = updateDate;
  }

  public void setCreateUser(User createUser) {
    this.createUser = createUser;
  }

  public void setUpdateUser(User updateUser) {
    this.updateUser = updateUser;
  }

  public Integer getId() { return id; }

  public String getName() {
    return name;
  }

  public String getItDirectorName() {
    return itDirectorName;
  }

  public String getItDirectorEmail() {
    return itDirectorEmail;
  }

  public Date getCreateDate() {
    return createDate;
  }

  public Integer getCreateUserId() {
    return createUserId;
  }

  public Date getUpdateDate() {
    return updateDate;
  }

  public Integer getUpdateUserId() {
    return updateUserId;
  }

  public User getCreateUser() {
    return createUser;
  }

  public User getUpdateUser() {
    return updateUser;
  }

  @Override
  public boolean equals(Object institution) {
    if (institution == this) return true;
    if (institution == null || institution.getClass() != getClass()) return false;
    Institution other = (Institution) institution;
    return new EqualsBuilder()
          .append(id, other.getId())
          .isEquals();
  }
}
