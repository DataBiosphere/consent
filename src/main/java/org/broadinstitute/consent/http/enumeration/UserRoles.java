package org.broadinstitute.consent.http.enumeration;

import org.broadinstitute.consent.http.resources.Resource;

import java.util.EnumSet;

public enum UserRoles {

    MEMBER(Resource.MEMBER, 1),
    CHAIRPERSON(Resource.CHAIRPERSON, 2),
    ALUMNI(Resource.ALUMNI, 3),
    ADMIN(Resource.ADMIN, 4),
    RESEARCHER(Resource.RESEARCHER, 5),
    DATAOWNER(Resource.DATAOWNER, 6),
    SIGNINGOFFICIAL(Resource.SIGNINGOFFICIAL, 7);

    private String roleName;
    private Integer roleId;

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
        return EnumSet.allOf(UserRoles.class)
          .stream()
          .map(UserRoles::getRoleName)
          .anyMatch(roleName::equalsIgnoreCase);
    }

}
