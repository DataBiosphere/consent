package org.broadinstitute.consent.http.enumeration;

import org.broadinstitute.consent.http.resources.Resource;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;

public enum UserRoles {

    MEMBER(Resource.MEMBER, 1),
    CHAIRPERSON(Resource.CHAIRPERSON, 2),
    ALUMNI(Resource.ALUMNI, 3),
    ADMIN(Resource.ADMIN, 4),
    RESEARCHER(Resource.RESEARCHER, 5),
    DATAOWNER(Resource.DATAOWNER, 6),
    SIGNINGOFFICIAL(Resource.SIGNINGOFFICIAL, 7),
    DATASUBMITTER(Resource.DATASUBMITTER, 8),
    ITDIRECTOR(Resource.ITDIRECTOR, 9);

    private final String roleName;
    private final Integer roleId;

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

    private static final HashSet<Integer> listOfNonDacRoleIds = new HashSet<>(Arrays.asList(
            ALUMNI.getRoleId(),
            ADMIN.getRoleId(),
            RESEARCHER.getRoleId(),
            DATAOWNER.getRoleId(),
            SIGNINGOFFICIAL.getRoleId(),
            DATASUBMITTER.getRoleId(),
            ITDIRECTOR.getRoleId()));

    public static boolean isValidNonDACRoleId(Integer roleId) {
        return !Objects.isNull(roleId) && listOfNonDacRoleIds.contains(roleId);
    }

    private static final HashSet<Integer> listOfSoAuthorizedRolesToAdjust = new HashSet<>(Arrays.asList(
            ITDIRECTOR.getRoleId(),
            SIGNINGOFFICIAL.getRoleId(),
            DATASUBMITTER.getRoleId()));
    public static boolean isValidSoAdjustableRoleId(Integer roleId) {
        return !Objects.isNull(roleId) && listOfSoAuthorizedRolesToAdjust.contains(roleId);
    }

}
