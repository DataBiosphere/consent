package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserTest {

  @Test
  void testHasRoleNull() {
    User user = new User();
    user.setResearcherRole();
    assertFalse(user.hasUserRole(null));
  }

  @ParameterizedTest
  @EnumSource(UserRoles.class)
  void testHasRoleEmptyUserNoRoles(UserRoles roleToTest) {
    User user = new User();
    assertFalse(user.hasUserRole(roleToTest));
  }

  @ParameterizedTest
  @EnumSource(UserRoles.class)
  void testHasUserRole(UserRoles roleToTest) {
    Set<UserRoles> otherRoles = EnumSet.allOf(UserRoles.class).stream().filter(u -> u != roleToTest).collect(Collectors.toSet());
    User user = new User();
    user.addRole(new UserRole(roleToTest.getRoleId(), roleToTest.getRoleName()));
    assertTrue(user.hasUserRole(roleToTest));
    otherRoles.forEach(r -> assertFalse(user.hasUserRole(r)));
  }

  @Test
  void testHasAnyRoleEmptyRoles() {
    User user = new User();
    user.setAdminRole();
    assertFalse(user.hasAnyUserRole(List.of()));
  }

  @Test
  void testHasAnyRoleNull() {
    User user = new User();
    user.setResearcherRole();
    assertFalse(user.hasAnyUserRole(null));
  }

  @ParameterizedTest
  @EnumSource(UserRoles.class)
  void testHasAnyRoleEmptyUserNoRoles(UserRoles roleToTest) {
    User user = new User();
    assertFalse(user.hasAnyUserRole(List.of(roleToTest)));
  }

  @Test
  void testHasAnyRole() {
    User user = new User();
    user.addRole(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
    user.addRole(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
    // User has ADMIN and RESEARCHER roles, test cases where at least one matches
    assertTrue(user.hasAnyUserRole(List.of(UserRoles.ADMIN, UserRoles.RESEARCHER)));
    assertTrue(user.hasAnyUserRole(List.of(UserRoles.ADMIN, UserRoles.CHAIRPERSON)));
    assertTrue(user.hasAnyUserRole(List.of(UserRoles.ADMIN, UserRoles.MEMBER)));
    assertTrue(user.hasAnyUserRole(List.of(UserRoles.ADMIN, UserRoles.SIGNINGOFFICIAL)));
    assertTrue(user.hasAnyUserRole(List.of(UserRoles.RESEARCHER, UserRoles.CHAIRPERSON)));
    assertTrue(user.hasAnyUserRole(List.of(UserRoles.RESEARCHER, UserRoles.MEMBER)));
    assertTrue(user.hasAnyUserRole(List.of(UserRoles.RESEARCHER, UserRoles.SIGNINGOFFICIAL)));
    // Assert cases where none of the roles match
    assertFalse(user.hasAnyUserRole(List.of(UserRoles.CHAIRPERSON, UserRoles.MEMBER)));
    assertFalse(user.hasAnyUserRole(List.of(UserRoles.SIGNINGOFFICIAL, UserRoles.ITDIRECTOR)));
    assertFalse(user.hasAnyUserRole(List.of(UserRoles.ALUMNI, UserRoles.SERVICE_ACCOUNT)));
  }
}
