package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.enumeration.UserFields;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class represents the limited amount of information that is available for update from
 * an admin-only interface.
 */
public class UserUpdateFields {
  private String displayName;
  private Integer institutionId;
  private Boolean emailPreference;
  private List<Integer> userRoleIds;
  private String eraCommonsId;
  private Integer selectedSigningOfficialId;
  private String suggestedInstitution;
  private String suggestedSigningOfficial;

  public UserUpdateFields() {}

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public Integer getInstitutionId() {
    return institutionId;
  }

  public void setInstitutionId(Integer institutionId) {
    this.institutionId = institutionId;
  }

  public Boolean getEmailPreference() {
    return emailPreference;
  }

  public void setEmailPreference(Boolean emailPreference) {
    this.emailPreference = emailPreference;
  }

  public List<Integer> getUserRoleIds() {
    return userRoleIds;
  }

  public void setUserRoleIds(List<Integer> userRoleIds) {
    this.userRoleIds = userRoleIds;
  }

  public String getEraCommonsId() {
    return eraCommonsId;
  }

  public void setEraCommonsId(String eraCommonsId) {
    this.eraCommonsId = eraCommonsId;
  }

  public Integer getSelectedSigningOfficialId() {
    return selectedSigningOfficialId;
  }

  public void setSelectedSigningOfficialId(Integer selectedSigningOfficialId) {
    this.selectedSigningOfficialId = selectedSigningOfficialId;
  }

  public String getSuggestedInstitution() {
    return suggestedInstitution;
  }

  public void setSuggestedInstitution(String suggestedInstitution) {
    this.suggestedInstitution = suggestedInstitution;
  }

  public String getSuggestedSigningOfficial() {
    return suggestedSigningOfficial;
  }

  public void setSuggestedSigningOfficial(String suggestedSigningOfficial) {
    this.suggestedSigningOfficial = suggestedSigningOfficial;
  }

  public List<UserProperty> buildUserProperties(Integer userId) {
    List<UserProperty> userProps = new ArrayList<>();
    if (Objects.nonNull(this.getSelectedSigningOfficialId())) {
      UserProperty prop = new UserProperty();
      prop.setUserId(userId);
      prop.setPropertyKey(UserFields.SELECTED_SIGNING_OFFICIAL_ID.getValue());
      prop.setPropertyValue(this.getSelectedSigningOfficialId().toString());
      userProps.add(prop);
    }
    if (Objects.nonNull(this.getSuggestedSigningOfficial())) {
      UserProperty prop = new UserProperty();
      prop.setUserId(userId);
      prop.setPropertyKey(UserFields.SUGGESTED_SIGNING_OFFICIAL.getValue());
      prop.setPropertyValue(this.getSuggestedSigningOfficial().toString());
      userProps.add(prop);
    }
    if (Objects.nonNull(this.getSuggestedInstitution())) {
      UserProperty prop = new UserProperty();
      prop.setUserId(userId);
      prop.setPropertyKey(UserFields.SUGGESTED_INSTITUTION.getValue());
      prop.setPropertyValue(this.getSuggestedInstitution().toString());
      userProps.add(prop);
    }
    return userProps;
  }
}
