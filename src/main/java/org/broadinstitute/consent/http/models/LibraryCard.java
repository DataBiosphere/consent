package org.broadinstitute.consent.http.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;

public class LibraryCard {

  public static final String QUERY_FIELDS_WITH_LC_PREFIX =
      " lc.id AS lc_id, " +
          " lc.user_id AS lc_user_id, " +
          " lc.institution_id AS lc_institution_id, " +
          " lc.era_commons_id AS lc_era_commons_id, " +
          " lc.user_name AS lc_user_name, " +
          " lc.user_email AS lc_user_email, " +
          " lc.create_user_id AS lc_create_user_id, " +
          " lc.create_date AS lc_create_date, " +
          " lc.update_user_id AS lc_update_user_id ";

  private Integer id;
  private Integer userId;
  private Integer institutionId;
  private String eraCommonsId;
  private String userName;
  private String userEmail;
  private Date createDate;
  private Integer createUserId;
  private Date updateDate;
  private Integer updateUserId;

  private Institution institution;

  private List<Integer> daaIds;

  private List<DataAccessAgreement> daas;

  public LibraryCard() {
    this.createDate = new Date();
    this.daaIds = new ArrayList<>();
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

  public String getUserName() {
    return userName;
  }

  public void setUserName(String name) {
    this.userName = name;
  }

  public String getUserEmail() {
    return userEmail;
  }

  public void setUserEmail(String email) {
    this.userEmail = email;
  }

  public Date getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Date createDate) {
    this.createDate = createDate;
  }

  public Integer getCreateUserId() {
    return createUserId;
  }

  public void setCreateUserId(Integer createUser) {
    this.createUserId = createUser;
  }

  public Date getUpdateDate() {
    return updateDate;
  }

  public void setUpdateDate(Date updateDate) {
    this.updateDate = updateDate;
  }

  public Integer getUpdateUserId() {
    return updateUserId;
  }

  public void setUpdateUserId(Integer updateUser) {
    this.updateUserId = updateUser;
  }

  public Institution getInstitution() {
    return institution;
  }

  public void setInstitution(Institution institution) {this.institution = institution;}

  public List<Integer> getDaaIds() {return daaIds;}

  public void setDaaIds(List<Integer> daaIds) {this.daaIds = daaIds;}

  public List<DataAccessAgreement> getDaas() {return daas;}

  public void setDaas(List<DataAccessAgreement> daas) {this.daas = daas;}

  @Override
  public boolean equals(Object libraryCard) {
    if (libraryCard == this) {
      return true;
    }
    if (libraryCard == null || libraryCard.getClass() != getClass()) {
      return false;
    }
    LibraryCard other = (LibraryCard) libraryCard;
    return new EqualsBuilder().append(id, other.getId()).isEquals();
  }

  public void addDaa(Integer daaId) {
    if (this.daaIds == null) {
      this.daaIds = new ArrayList<>();
    }
    if (this.daaIds
        .stream()
        .noneMatch(d -> d.equals(daaId))) {
      this.daaIds.add(daaId);
    }
  }

  public void removeDaa(Integer daaId) {
    if (this.daaIds == null) {
      return;
    }
    if (this.daaIds
        .stream()
        .anyMatch(d -> d.equals(daaId))) {
      this.daaIds.remove(daaId);
    }
  }

  public void addDaaObject(DataAccessAgreement daa) {
    if (this.daas == null) {
      this.daas = new ArrayList<>();
    }
    if (this.daas
        .stream()
        .noneMatch(d -> d.equals(daa))) {
      this.daas.add(daa);
    }
  }
}
