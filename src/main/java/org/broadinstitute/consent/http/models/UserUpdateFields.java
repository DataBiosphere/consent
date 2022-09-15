package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.enumeration.UserRoles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class represents the limited amount of information that is available for update from an
 * admin-only interface.
 */
public class UserUpdateFields {

  // We can only update non-DAC-related roles so always filter those out for addition or removal
  protected static final List<Integer> IGNORE_ROLE_IDS = List.of(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.MEMBER.getRoleId());
  private static final List<Integer> VALID_ROLE_IDS = Arrays.stream(UserRoles.values()).map(UserRoles::getRoleId).collect(Collectors.toList());
  private String displayName;
  private Integer institutionId;
  private Boolean emailPreference;
  private List<Integer> userRoleIds;
  private String eraCommonsId;
  private Integer selectedSigningOfficialId;
  private String suggestedInstitution;
  private String suggestedSigningOfficial;
  private Boolean daaAcceptance;

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

  public Boolean getDaaAcceptance() {
    return daaAcceptance;
  }

  public void setDaaAcceptance(Boolean daaAcceptance) {
    this.daaAcceptance = daaAcceptance;
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
      prop.setPropertyValue(this.getSuggestedSigningOfficial());
      userProps.add(prop);
    }
    if (Objects.nonNull(this.getSuggestedInstitution())) {
      UserProperty prop = new UserProperty();
      prop.setUserId(userId);
      prop.setPropertyKey(UserFields.SUGGESTED_INSTITUTION.getValue());
      prop.setPropertyValue(this.getSuggestedInstitution());
      userProps.add(prop);
    }
    if (Objects.nonNull(this.getDaaAcceptance())) {
      UserProperty prop = new UserProperty();
      prop.setUserId(userId);
      prop.setPropertyKey(UserFields.DAA_ACCEPTANCE.getValue());
      prop.setPropertyValue(this.getDaaAcceptance().toString());
      userProps.add(prop);
    }
    return userProps;
  }

  /**
   * Takes a list of current user roles and compares with roles that are being
   * requested to be added to the user. The result is a list of user roles that
   * should be added to the user based on allowable conditions.
   *
   * @param currentUserRoleIds List of current user role ids.
   * @return List of role ids that need to be added to the user.
   */
  public List<Integer> getRoleIdsToAdd(List<Integer> currentUserRoleIds) {
    return this.getUserRoleIds().stream()
        .filter(
            id -> {
              return !currentUserRoleIds.contains(id) && // Don't add any that already exist
                     !IGNORE_ROLE_IDS.contains(id) &&    // Never add ignorable roles
                     VALID_ROLE_IDS.contains(id);        // Only add roles we know about
            })
        .collect(Collectors.toList());
  }

  /**
   * Takes a list of current user roles and compares with roles that are being
   * requested to be removed from the user. The result is a list of user roles that
   * should be removed from the user based on allowable conditions.
   *
   * @param currentUserRoleIds List of current user role ids.
   * @return List of role ids that need to be removed from the user.
   */
public List<Integer> getRoleIdsToRemove(List<Integer> currentUserRoleIds) {
    return currentUserRoleIds.stream()
        .filter(
            id -> {
              return !getUserRoleIds().contains(id) &&                        // Remove roles that are NOT in the new role id list
                     !Objects.equals(id, UserRoles.RESEARCHER.getRoleId()) && // Never remove the researcher role
                     !IGNORE_ROLE_IDS.contains(id) &&                         // Never remove ignorable roles
                      VALID_ROLE_IDS.contains(id);                            // Only remove roles we know about
                  })
        .collect(Collectors.toList());
  }
}
