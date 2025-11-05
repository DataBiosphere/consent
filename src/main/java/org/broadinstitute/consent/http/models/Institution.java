package org.broadinstitute.consent.http.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;

public class Institution {

  public static final String QUERY_FIELDS_WITH_I_PREFIX =
      " i.institution_id as i_id, "
          + " i.institution_name as i_name, "
          + " i.it_director_name as i_it_director_name, "
          + " i.it_director_email as i_it_director_email, "
          + " i.create_date as i_create_date, "
          + " i.update_date as i_update_date ";

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
  private List<String> domains;
  private Date createDate;
  private Integer createUserId;
  private Date updateDate;
  private Integer updateUserId;
  private User createUser;
  private User updateUser;

  // empty constructor sets all null values except create Date
  public Institution() {
    this.createDate = new Date();
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

  public List<String> getDomains() {
    return domains;
  }

  public void setDomains(List<String> domains) {
    this.domains = domains;
  }

  public void addDomain(String domain) {
    if (Objects.isNull(domains)) {
      this.setDomains(new ArrayList<>());
    }
    if (!domains.contains(domain)) {
      domains.add(domain);
    }
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
    return new EqualsBuilder().append(id, other.getId()).isEquals();
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  /**
   * Merges the updatable fields into this institution from an existing institution. If a field in
   * this entity is null, it will be populated with the value of the existing institution. If a
   * field in this entity is blank, it will set that field to null to represent an instance of
   * intentional field deletion.
   *
   * @param existing The existing institution to merge from. Must be a full institution entity with
   *     all required fields populated.
   */
  public Institution mergeUpdatableFields(Institution existing) {
    // These fields are not updatable, so we set them directly from the existing institution.
    this.setId(existing.getId());
    this.setCreateDate(existing.getCreateDate());
    this.setCreateUserId(existing.getCreateUserId());
    this.setUpdateDate(existing.getUpdateDate());
    this.setUpdateUserId(existing.getUpdateUserId());

    // The following fields are updatable, so we merge them from the existing institution.
    // Institution.name is not nullable, but it is updatable to a valid value.
    if (this.getName() == null || StringUtils.isBlank(this.getName())) {
      this.setName(existing.getName());
    }
    mergeStringField(this::getItDirectorName, existing::getItDirectorName, this::setItDirectorName);
    mergeStringField(
        this::getItDirectorEmail, existing::getItDirectorEmail, this::setItDirectorEmail);
    if (this.getDunsNumber() == null) {
      this.setDunsNumber(existing.getDunsNumber());
    }
    mergeStringField(this::getInstitutionUrl, existing::getInstitutionUrl, this::setInstitutionUrl);
    mergeStringField(this::getOrgChartUrl, existing::getOrgChartUrl, this::setOrgChartUrl);
    mergeStringField(
        this::getVerificationUrl, existing::getVerificationUrl, this::setVerificationUrl);
    mergeStringField(
        this::getVerificationFilename,
        existing::getVerificationFilename,
        this::setVerificationFilename);
    if (this.getOrganizationType() == null) {
      this.setOrganizationType(existing.getOrganizationType());
    }
    // If domains are not provided, we want to keep the existing domains.
    // In the case that empty domains are provided, we want to ensure that they are removed.
    if (this.getDomains() == null) {
      this.setDomains(existing.getDomains());
    } else if (this.getDomains().isEmpty()) {
      this.setDomains(null);
    }
    return this;
  }

  /**
   * Helper method to merge a value from the existing institution into the payload institution if
   * the payload is null. If the payload value is an empty string, it will be set to null to
   * represent an intentionally empty value.
   *
   * @param payloadGetter The getter for the payload field.
   * @param existingGetter The getter for the existing field.
   * @param payloadSetter The setter for the payload field.
   */
  private void mergeStringField(
      java.util.function.Supplier<String> payloadGetter,
      java.util.function.Supplier<String> existingGetter,
      java.util.function.Consumer<String> payloadSetter) {
    String value = payloadGetter.get();
    if (value == null) {
      payloadSetter.accept(existingGetter.get());
    } else if (StringUtils.isBlank(value)) {
      payloadSetter.accept(null);
    }
  }
}
