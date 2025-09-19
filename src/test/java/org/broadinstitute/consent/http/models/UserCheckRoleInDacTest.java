package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCheckRoleInDacTest {

  @Test
  void testVerifyDACRole_RoleNotFound() {
    User user = new User();
    user.setAdminRole();
    boolean isUserChair = user.verifyDACRole(UserRoles.CHAIRPERSON.getRoleName(), 2);
    assertFalse(isUserChair);
  }

  @Test
  void testVerifyDACRole_RoleTypeFoundDifferentDacId() {
    User user = new User();
    user.setChairpersonRoleWithDAC(1);
    boolean isUserChair = user.verifyDACRole(UserRoles.CHAIRPERSON.getRoleName(), 2);
    assertFalse(isUserChair);
  }

  @Test
  void testVerifyDACRole() {
    User user = new User();
    UserRole chairRole = UserRoles.Chairperson();
    chairRole.setDacId(1);
    UserRole adminRole = UserRoles.Admin();
    user.setRoles(List.of(chairRole, adminRole));
    boolean isUserChair = user.verifyDACRole(UserRoles.CHAIRPERSON.getRoleName(), 1);
    boolean isUserAdmin = user.verifyDACRole(UserRoles.ADMIN.getRoleName(), null);
    assertTrue(isUserChair);
    assertTrue(isUserAdmin);
  }

  @Test
  void testVerifyDACRole_NullDacId() {
    User user = new User();
    UserRole chairRole = UserRoles.Chairperson();
    chairRole.setDacId(1);
    UserRole adminRole = UserRoles.Admin();
    user.setRoles(List.of(chairRole, adminRole));
    boolean isUserChair = user.verifyDACRole(UserRoles.CHAIRPERSON.getRoleName(), null);
    boolean isUserAdmin = user.verifyDACRole(UserRoles.ADMIN.getRoleName(), null);
    assertFalse(isUserChair);
    assertTrue(isUserAdmin);
  }

}
