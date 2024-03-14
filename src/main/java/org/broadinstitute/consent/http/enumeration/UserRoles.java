package org.broadinstitute.consent.http.enumeration;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.broadinstitute.consent.http.resources.Resource;

public enum UserRoles {

  MEMBER(Resource.MEMBER, 1),
  CHAIRPERSON(Resource.CHAIRPERSON, 2),
  ALUMNI(Resource.ALUMNI, 3),
  ADMIN(Resource.ADMIN, 4),
  RESEARCHER(Resource.RESEARCHER, 5),
  SIGNINGOFFICIAL(Resource.SIGNINGOFFICIAL, 7),
  DATASUBMITTER(Resource.DATASUBMITTER, 8),
  ITDIRECTOR(Resource.ITDIRECTOR, 9);

  private final String roleName;
  private final Integer roleId;
  private static final HashSet<Integer> LIST_OF_NON_DAC_ROLE_IDS = new HashSet<>(Set.of(
      ALUMNI.getRoleId(),
      ADMIN.getRoleId(),
      RESEARCHER.getRoleId(),
      SIGNINGOFFICIAL.getRoleId(),
      DATASUBMITTER.getRoleId(),
      ITDIRECTOR.getRoleId()));
  private static final HashSet<Integer> LIST_OF_SO_AUTHORIZED_ROLES_TO_ADJUST = new HashSet<>(
      Set.of(
          ITDIRECTOR.getRoleId(),
          SIGNINGOFFICIAL.getRoleId(),
          DATASUBMITTER.getRoleId()));

  UserRoles(String roleName, Integer roleId) {
    this.roleName = roleName;
    this.roleId = roleId;
  }

  public String getRoleName() {
    return roleName;
  }

  public Integer getRoleId() {
    return roleId;
  }

  public static UserRoles getUserRoleFromName(String value) {
    for (UserRoles e : UserRoles.values()) {
      if (e.getRoleName().equalsIgnoreCase(value)) {
        return e;
      }
    }
    return null;
  }

  public static UserRoles getUserRoleFromId(Integer roleId) {
    for (UserRoles e : UserRoles.values()) {
      if (e.getRoleId().equals(roleId)) {
        return e;
      }
    }
    return null;
  }

  public static boolean isValidRole(String roleName) {
    if (Objects.isNull(roleName)) {
      return false;
    }
    return EnumSet.allOf(UserRoles.class)
        .stream()
        .map(UserRoles::getRoleName)
        .anyMatch(roleName::equalsIgnoreCase);
  }

  public static boolean isValidNonDACRoleId(Integer roleId) {
    return !Objects.isNull(roleId) && LIST_OF_NON_DAC_ROLE_IDS.contains(roleId);
  }

  public static boolean isValidSoAdjustableRoleId(Integer roleId) {
    return !Objects.isNull(roleId) && LIST_OF_SO_AUTHORIZED_ROLES_TO_ADJUST.contains(roleId);
  }

}
