package org.broadinstitute.consent.http.enumeration;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.resources.Resource;

public enum UserRoles {

  MEMBER(Resource.MEMBER, 1),
  CHAIRPERSON(Resource.CHAIRPERSON, 2),
  ALUMNI(Resource.ALUMNI, 3),
  ADMIN(Resource.ADMIN, 4),
  RESEARCHER(Resource.RESEARCHER, 5),
  SIGNINGOFFICIAL(Resource.SIGNINGOFFICIAL, 7),
  DATASUBMITTER(Resource.DATASUBMITTER, 8),
  ITDIRECTOR(Resource.ITDIRECTOR, 9),
  SERVICE_ACCOUNT(Resource.SERVICE_ACCOUNT, 10);

  private static final Set<UserRoles> NON_DAC_ROLES =
      Set.of(ALUMNI, ADMIN, RESEARCHER, SIGNINGOFFICIAL, DATASUBMITTER, ITDIRECTOR,
          SERVICE_ACCOUNT);
  private static final Set<UserRoles> SO_AUTHORIZED_ROLES_TO_ADJUST =
      Set.of(ITDIRECTOR, SIGNINGOFFICIAL, DATASUBMITTER);
  private final String roleName;
  private final Integer roleId;

  UserRoles(String roleName, Integer roleId) {
    this.roleName = roleName;
    this.roleId = roleId;
  }

  public static UserRole Admin() {
    return new UserRole(ADMIN.getRoleId(), ADMIN.getRoleName());
  }

  public static UserRole Alumni() {
    return new UserRole(ALUMNI.getRoleId(), ALUMNI.getRoleName());
  }

  public static UserRole Chairperson() {
    return new UserRole(CHAIRPERSON.getRoleId(), CHAIRPERSON.getRoleName());
  }

  public static UserRole DataSubmitter() {
    return new UserRole(DATASUBMITTER.getRoleId(), DATASUBMITTER.getRoleName());
  }

  public static UserRole ITDirector() {
    return new UserRole(ITDIRECTOR.getRoleId(), ITDIRECTOR.getRoleName());
  }

  public static UserRole Member() {
    return new UserRole(MEMBER.getRoleId(), MEMBER.getRoleName());
  }

  public static UserRole Researcher() {
    return new UserRole(RESEARCHER.getRoleId(), RESEARCHER.getRoleName());
  }

  public static UserRole SigningOfficial() {
    return new UserRole(SIGNINGOFFICIAL.getRoleId(), SIGNINGOFFICIAL.getRoleName());
  }

  public static UserRole ServiceAccount() {
    return new UserRole(SERVICE_ACCOUNT.getRoleId(), SERVICE_ACCOUNT.getRoleName());
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

  public static boolean isValidNonDACRoleId(UserRoles role) {
    return NON_DAC_ROLES.contains(role);
  }

  public static boolean isValidSoAdjustableRoleId(UserRoles role) {
    return SO_AUTHORIZED_ROLES_TO_ADJUST.contains(role);
  }

  public String getRoleName() {
    return roleName;
  }

  public Integer getRoleId() {
    return roleId;
  }

}
