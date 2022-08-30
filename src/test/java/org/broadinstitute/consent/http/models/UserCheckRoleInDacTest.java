package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

public class UserCheckRoleInDacTest {

  public void testCheckIfUserHasRole_RoleNotFound() {
    User user = new User();
    UserRole adminRole = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    user.setRoles(List.of(adminRole));
    Boolean isUserChair = user.checkIfUserHasRole(UserRoles.CHAIRPERSON.getRoleName(), 2);
    assertFalse(isUserChair);
  }

  @Test
  public void testCheckIfUserHasRole_RoleTypeFoundDifferentDacId() {
    User user = new User();
    UserRole chairRole = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    chairRole.setDacId(1);
    user.setRoles(List.of(chairRole));
    Boolean isUserChair = user.checkIfUserHasRole(UserRoles.CHAIRPERSON.getRoleName(), 2);
    assertFalse(isUserChair);
  }

  @Test
  public void testCheckIfUserHasRole() {
    User user = new User();
    UserRole chairRole = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    chairRole.setDacId(1);
    UserRole adminRole = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    user.setRoles(List.of(chairRole, adminRole));
    Boolean isUserChair = user.checkIfUserHasRole(UserRoles.CHAIRPERSON.getRoleName(), 1);
    Boolean isUserAdmin = user.checkIfUserHasRole(UserRoles.ADMIN.getRoleName(), null);
   assertTrue(isUserChair);
   assertTrue(isUserAdmin);
  }
  
  
}
