package org.broadinstitute.consent.http.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;

public class Institution {

  public static final String QUERY_FIELDS_WITH_I_PREFIX =
      " i.institution_id as i_id, " +
          " i.institution_name as i_name, " +
          " i.it_director_name as i_it_director_name, " +
          " i.it_director_email as i_it_director_email, " +
          " i.create_date as i_create_date, " +
          " i.update_date as i_update_date ";

  public static final String QUERY_FIELDS_WITH_LCI_PREFIX =
      " lci.institution_id as lci_id, " +
          " lci.institution_name as lci_name, " +
          " lci.it_director_name as lci_it_director_name, " +
          " lci.it_director_email as lci_it_director_email, " +
          " lci.create_date as lci_create_date, " +
          " lci.update_date as lci_update_date ";

  private Integer id;
  private String name;
  private String itDirectorName;
  private String itDirectorEmail;
  private List<SimplifiedUser> signingOfficials;
  private String institutionUrl;
  private Integer dunsNumber;
  private String orgChartUrl;
  private String verificationUrl;
  private String verificationFilename;
  private OrganizationType organizationType;
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

  public Institution(Integer id,
      String name,
      String itDirectorName,
      String itDirectorEmail,
      String institutionUrl,
      Integer dunsNumber,
      String orgChartUrl,
      String verificationUrl,
      String verificationFilename,
      OrganizationType organizationType,
      Integer createUserId,
      Date createDate) {
    this.id = id;
    this.name = name;
    this.itDirectorName = itDirectorName;
    this.itDirectorEmail = itDirectorEmail;
    this.signingOfficials = new ArrayList<>();
    this.createDate = createDate;
    this.createUserId = createUserId;
    this.institutionUrl = institutionUrl;
    this.dunsNumber = dunsNumber;
    this.orgChartUrl = orgChartUrl;
    this.verificationUrl = verificationUrl;
    this.verificationFilename = verificationFilename;
    this.organizationType = organizationType;
    this.updateDate = this.createDate;
    this.updateUserId = this.createUserId;
  }

  public Institution(Integer id,
      String name,
      String itDirectorName,
      String itDirectorEmail,
      String institutionUrl,
      Integer dunsNumber,
      String orgChartUrl,
      String verificationUrl,
      String verificationFilename,
      OrganizationType organizationType,
      Integer createUserId,
      Date createDate,
      Integer updateUserId,
      Date updateDate) {
    this.id = id;
    this.name = name;
    this.itDirectorName = itDirectorName;
    this.itDirectorEmail = itDirectorEmail;
    this.signingOfficials = new ArrayList<>();
    this.institutionUrl = institutionUrl;
    this.dunsNumber = dunsNumber;
    this.orgChartUrl = orgChartUrl;
    this.verificationUrl = verificationUrl;
    this.verificationFilename = verificationFilename;
    this.organizationType = organizationType;
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

  public List<SimplifiedUser> getSigningOfficials() {
    return signingOfficials;
  }

  public void setSigningOfficials(List<SimplifiedUser> signingOfficials) {
    this.signingOfficials = signingOfficials;
  }

  public void addSigningOfficial(SimplifiedUser so) {
    if (Objects.isNull(signingOfficials)) {
      this.setSigningOfficials(new ArrayList<>());
    }
    if (!new HashSet<>(signingOfficials).contains(so)) {
      signingOfficials.add(so);
    }
  }

  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getItDirectorName() {
    return itDirectorName;
  }

  public String getItDirectorEmail() {
    return itDirectorEmail;
  }

  public String getInstitutionUrl() {
    return institutionUrl;
  }

  public void setInstitutionUrl(String institutionUrl) {
    this.institutionUrl = institutionUrl;
  }

  public Integer getDunsNumber() {
    return dunsNumber;
  }

  public void setDunsNumber(Integer dunsNumber) {
    this.dunsNumber = dunsNumber;
  }

  public String getOrgChartUrl() {
    return orgChartUrl;
  }

  public void setOrgChartUrl(String orgChartUrl) {
    this.orgChartUrl = orgChartUrl;
  }

  public String getVerificationUrl() {
    return verificationUrl;
  }

  public void setVerificationUrl(String verificationUrl) {
    this.verificationUrl = verificationUrl;
  }

  public String getVerificationFilename() {
    return verificationFilename;
  }

  public void setVerificationFilename(String verificationFilename) {
    this.verificationFilename = verificationFilename;
  }

  public OrganizationType getOrganizationType() {
    return organizationType;
  }

  public void setOrganizationType(OrganizationType organizationType) {
    this.organizationType = organizationType;
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


  @Override
  public boolean equals(Object institution) {
    if (institution == this) {
      return true;
    }
    if (institution == null || institution.getClass() != getClass()) {
      return false;
    }
    Institution other = (Institution) institution;
    return new EqualsBuilder()
        .append(id, other.getId())
        .isEquals();
  }
}
