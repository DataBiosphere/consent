package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.junit.Test;

import java.util.List;

import javax.ws.rs.NotFoundException;

public class UserCheckRoleInDacTest {

  @Test(expected = NotFoundException.class)
  public void testCheckIfUserHasRole_RoleNotFound() {
    User user = new User();
    UserRole adminRole = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    user.setRoles(List.of(adminRole));
    user.checkIfUserHasRole(UserRoles.CHAIRPERSON.getRoleName(), 2);
  }

  @Test(expected = NotFoundException.class)
  public void testCheckIfUserHasRole_RoleTypeFoundDifferentDacId() {
    User user = new User();
    UserRole chairRole = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    chairRole.setDacId(1);
    user.setRoles(List.of(chairRole));
    user.checkIfUserHasRole(UserRoles.CHAIRPERSON.getRoleName(), 2);
  }

  @Test
  public void testCheckIfUserHasRole() {
    User user = new User();
    UserRole chairRole = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    chairRole.setDacId(1);
    UserRole adminRole = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    user.setRoles(List.of(chairRole, adminRole));
    user.checkIfUserHasRole(UserRoles.CHAIRPERSON.getRoleName(), 1);
    user.checkIfUserHasRole(UserRoles.ADMIN.getRoleName(), null);
  }
  
  
}
