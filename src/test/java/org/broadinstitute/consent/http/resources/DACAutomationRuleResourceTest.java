package org.broadinstitute.consent.http.resources;

import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.AutomationRuleToggleResponse;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.DACAutomationRuleService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DACAutomationRuleResourceTest extends AbstractTestHelper {

  @Mock
  private DACAutomationRuleService ruleService;
  @Mock
  private DacService dacService;
  @Mock
  private UserService userService;

  @Mock
  private AuthUser authUser;

  private DACAutomationRuleResource resource;

  @BeforeEach
  void setUp() {
    resource = new DACAutomationRuleResource(ruleService, dacService, userService);
  }

  @Test
  void testGetAllRules() {
    when(ruleService.findAll()).thenReturn(List.of());

    try (var response = resource.getAllRules()) {
      Assertions.assertEquals(200, response.getStatus());
    }
  }

  @Test
  void testGetAvailableRulesAsAdmin() {
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(createUserWithRole(UserRoles.Admin()));
    when(dacService.findById(1)).thenReturn(new Dac());

    try (var response = resource.getAvailableRules(authUser, 1)) {
      Assertions.assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testGetAvailableRulesAsChair() {
    UserRole role = UserRoles.Chairperson();
    role.setDacId(1);
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(createUserWithRole(role));
    when(dacService.findById(1)).thenReturn(new Dac());

    try (var response = resource.getAvailableRules(authUser, 1)) {
      Assertions.assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testGetAvailableRulesAsChairForbidden() {
    UserRole role = UserRoles.Chairperson();
    role.setDacId(2);
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(createUserWithRole(role));

    try (var response = resource.getAvailableRules(authUser, 1)) {
      Assertions.assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testToggleRuleAsAdmin() {
    User chairperson = createUserWithRole(UserRoles.Admin());
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(chairperson);

    try (var response = resource.toggleRule(authUser, 1, 1)) {
      Assertions.assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testToggleRuleAsChair() {
    UserRole role = UserRoles.Chairperson();
    role.setDacId(1);
    User chairperson = createUserWithRole(role);
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(chairperson);
    when(dacService.findById(1)).thenReturn(new Dac());
    when(ruleService.toggleRule(1, 1, chairperson.getUserId())).thenReturn(new AutomationRuleToggleResponse(1, true));

    try (var response = resource.toggleRule(authUser, 1, 1)) {
      Assertions.assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testToggleRuleAsChairForbidden() {
    UserRole role = UserRoles.Chairperson();
    role.setDacId(2);
    User chairperson = createUserWithRole(role);
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(chairperson);

    try (var response = resource.toggleRule(authUser, 1, 1)) {
      Assertions.assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }


  private User createUserWithRole(UserRole role) {
    User user = new User();
    user.setUserId(randomInt(1, 100));
    user.setDisplayName("Test");
    user.setEmail("Test");
    user.addRole(role);
    return user;
  }

}
