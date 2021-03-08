package org.broadinstitute.consent.http.models;

import java.util.Date;

public class Institution {

  private Integer id;
  private String name;
  private String itDirectorName;
  private String itDirectorEmail;
  private Date createDate;
  private Integer createUser;
  private Date updateDate;
  private Integer updateUser;

  //empty constructor sets all null values except create Date
  public Institution() {
    this.createDate = new Date();
  }

  public Institution(Integer id, String name, String itDirectorName, String itDirectorEmail, Integer createUser, Date createDate) {
    this.id = id;
    this.name = name;
    this.itDirectorName = itDirectorName;
    this.itDirectorEmail = itDirectorEmail;
    this.createDate = createDate;
    this.createUser = createUser;
    this.updateDate = this.createDate;
    this.updateUser = this.createUser;
  }

  public Institution(Integer id, String name, String itDirectorName, String itDirectorEmail,
                     Integer createUser, Date createDate, Integer updateUser, Date updateDate) {
    this.id = id;
    this.name = name;
    this.itDirectorName = itDirectorName;
    this.itDirectorEmail = itDirectorEmail;
    this.createDate = createDate;
    this.createUser = createUser;
    this.updateDate = updateDate;
    this.updateUser = updateUser;
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

  public void setCreateUser(Integer createUser) {
    this.createUser = createUser;
  }

  public void setCreateDate(Date date) {
    this.createDate = date;
  }

  public void setUpdateUser(Integer updateUser) {
    this.updateUser = updateUser;
  }

  public void setUpdateDate(Date updateDate) {
    this.updateDate = updateDate;
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

  public Integer getCreateUser() {
    return createUser;
  }

  public Date getUpdateDate() {
    return updateDate;
  }

  public Integer getUpdateUser() {
    return updateUser;
  }

  @Override
  public boolean equals(Object institution) {
    if (institution == this) return true;
    if (institution == null || institution.getClass() != getClass()) return false;
    return (((Institution)institution).getId() == getId());
  }
}
