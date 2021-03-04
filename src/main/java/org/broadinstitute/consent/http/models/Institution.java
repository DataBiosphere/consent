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

  public void setName(String name, Integer updateUser) {
    this.name = name;
    this.updateInstitution(updateUser);
  }

  public void setItDirectorEmail(String itDirectorEmail, Integer updateUser) {
    this.itDirectorEmail = itDirectorEmail;
    this.updateInstitution(updateUser);
  }

  public void setItDirectorName(String itDirectorName, Integer updateUser) {
    this.itDirectorName = itDirectorName;
    this.updateInstitution(updateUser);
  }

  private void updateInstitution(Integer updateUser) {
    this.updateUser = updateUser;
    this.updateDate = new Date();
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
}
