package org.broadinstitute.consent.http.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.enumeration.UserRoles;

/**
 * This class represents the limited amount of information that is available for update from an
 * admin-only interface.
 */
public class UserUpdateFields {

  // We can only update non-DAC-related roles so always filter those out for addition or removal
  protected static final List<Integer> IGNORE_ROLE_IDS =
      List.of(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.MEMBER.getRoleId());
  private static final List<Integer> VALID_ROLE_IDS =
      Arrays.stream(UserRoles.values()).map(UserRoles::getRoleId).toList();
  private String displayName;
  private Boolean emailPreference;
  private List<Integer> userRoleIds;
  private String eraCommonsId;
  private Boolean daaAcceptance;
  private Map<String, Object> userData;

  public UserUpdateFields() {
    // Default constructor
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
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

  public Boolean getDaaAcceptance() {
    return daaAcceptance;
  }

  public void setDaaAcceptance(Boolean daaAcceptance) {
    this.daaAcceptance = daaAcceptance;
  }

  public Map<String, Object> getUserData() {
    return userData;
  }

  public void setUserData(Map<String, Object> userData) {
    this.userData = userData;
  }

  public List<UserProperty> buildUserProperties(Integer userId) {
    List<UserProperty> userProps = new ArrayList<>();
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
   * Takes a list of current user roles and compares with roles that are being requested to be added
   * to the user. The result is a list of user roles that should be added to the user based on
   * allowable conditions.
   *
   * @param currentUserRoleIds List of current user role ids.
   * @return List of role ids that need to be added to the user.
   */
  public List<Integer> getRoleIdsToAdd(List<Integer> currentUserRoleIds) {
    return this.getUserRoleIds().stream()
        .filter(
            id ->
                !currentUserRoleIds.contains(id)
                    && // Don't add any that already exist
                    !IGNORE_ROLE_IDS.contains(id)
                    && // Never add ignorable roles
                    VALID_ROLE_IDS.contains(id) // Only add roles we know about
            )
        .toList();
  }

  /**
   * Takes a list of current user roles and compares with roles that are being requested to be
   * removed from the user. The result is a list of user roles that should be removed from the user
   * based on allowable conditions.
   *
   * @param currentUserRoleIds List of current user role ids.
   * @return List of role ids that need to be removed from the user.
   */
  public List<Integer> getRoleIdsToRemove(List<Integer> currentUserRoleIds) {
    return currentUserRoleIds.stream()
        .filter(
            id ->
                !getUserRoleIds().contains(id)
                    &&
                    // Remove roles that are NOT in the new role id list
                    !Objects.equals(id, UserRoles.RESEARCHER.getRoleId())
                    &&
                    // Never remove the researcher role
                    !IGNORE_ROLE_IDS.contains(id)
                    &&
                    // Never remove ignorable roles
                    VALID_ROLE_IDS.contains(id) // Only remove roles we know about
            )
        .toList();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UserUpdateFields that = (UserUpdateFields) o;
    return Objects.equals(displayName, that.displayName)
        && Objects.equals(emailPreference, that.emailPreference)
        && Objects.equals(userRoleIds, that.userRoleIds)
        && Objects.equals(eraCommonsId, that.eraCommonsId)
        && Objects.equals(daaAcceptance, that.daaAcceptance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(displayName, emailPreference, userRoleIds, eraCommonsId, daaAcceptance);
  }
}
