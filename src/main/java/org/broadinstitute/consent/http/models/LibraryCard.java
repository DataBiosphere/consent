package org.broadinstitute.consent.http.models;

import java.util.Date;

import org.apache.commons.lang3.builder.EqualsBuilder;


public class LibraryCard {

    private Integer id;
    private Integer userId;
    private Integer institutionId;
    private String eraCommonsId;
    private String name;
    private String email;
    private Date createDate;
    private Integer createUser;
    private Date updateDate;
    private Integer updateUser;

  public LibraryCard() {
    this.createDate = new Date();
  }
  
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public Integer getInstitutionId() {
    return institutionId;
  }

  public void setInstitutionId(Integer institutionId) {
    this.institutionId = institutionId;
  }

  public String getEraCommonsId() {
    return eraCommonsId;
  }

  public void setEraCommonsId(String eraCommonsId) {
    this.eraCommonsId = eraCommonsId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Date getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Date createDate) {
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

  public void setUpdateDate(Date updateDate) {
    this.updateDate = updateDate;
  }

  public Integer getUpdateUser() {
    return updateUser;
  }

  public void setUpdateUser(Integer updateUser) {
    this.updateUser = updateUser;
  }

  @Override
  public boolean equals(Object libraryCard) {
    if (libraryCard == this) return true;
    if (libraryCard == null || libraryCard.getClass() != getClass()) return false;
    LibraryCard other = (LibraryCard) libraryCard;
    return new EqualsBuilder()
          .append(id, other.getId())
          .isEquals();
  }
    
}