package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.junit.jupiter.api.Test;

class UserCheckRoleInDacTest {

  @Test
  void testCheckIfUserHasRole_RoleNotFound() {
    User user = new User();
    user.setAdminRole();
    Boolean isUserChair = user.checkIfUserHasRole(UserRoles.CHAIRPERSON.getRoleName(), 2);
    assertFalse(isUserChair);
  }

  @Test
  void testCheckIfUserHasRole_RoleTypeFoundDifferentDacId() {
    User user = new User();
    user.setChairpersonRoleWithDAC(1);
    Boolean isUserChair = user.checkIfUserHasRole(UserRoles.CHAIRPERSON.getRoleName(), 2);
    assertFalse(isUserChair);
  }

  @Test
  void testCheckIfUserHasRole() {
    User user = new User();
    UserRole chairRole = UserRoles.ChairpersonRole();
    chairRole.setDacId(1);
    UserRole adminRole = UserRoles.AdminRole();
    user.setRoles(List.of(chairRole, adminRole));
    Boolean isUserChair = user.checkIfUserHasRole(UserRoles.CHAIRPERSON.getRoleName(), 1);
    Boolean isUserAdmin = user.checkIfUserHasRole(UserRoles.ADMIN.getRoleName(), null);
    assertTrue(isUserChair);
    assertTrue(isUserAdmin);
  }

}
