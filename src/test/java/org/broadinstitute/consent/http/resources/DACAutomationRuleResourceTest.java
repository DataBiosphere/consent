package org.broadinstitute.consent.http.resources;

import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.AutomationRuleToggleResponse;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.DACAutomationRuleService;
import org.broadinstitute.consent.http.service.DacService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DACAutomationRuleResourceTest extends AbstractTestHelper {

  @Mock private DACAutomationRuleService ruleService;
  @Mock private DacService dacService;

  @Mock private AuthUser authUser;

  private DACAutomationRuleResource resource;

  @BeforeEach
  void setUp() {
    resource = new DACAutomationRuleResource(ruleService, dacService);
  }

  @Test
  void testGetAllRules() {
    when(ruleService.findAll()).thenReturn(List.of());

    try (var response = resource.getAllRules(new DuosUser(authUser, new User()))) {
      Assertions.assertEquals(200, response.getStatus());
    }
  }

  @Test
  void testGetAvailableRulesAsAdmin() {
    User admin = createUserWithRole(UserRoles.Admin());
    when(dacService.findById(1)).thenReturn(new Dac());

    try (var response = resource.getAvailableRules(new DuosUser(authUser, admin), 1)) {
      Assertions.assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testGetAvailableRulesAsChair() {
    UserRole role = UserRoles.Chairperson();
    role.setDacId(1);
    User chairperson = createUserWithRole(role);
    when(dacService.findById(1)).thenReturn(new Dac());

    try (var response = resource.getAvailableRules(new DuosUser(authUser, chairperson), 1)) {
      Assertions.assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testGetAvailableRulesAsChairForbidden() {
    UserRole role = UserRoles.Chairperson();
    role.setDacId(2);
    User chairperson = createUserWithRole(role);

    try (var response = resource.getAvailableRules(new DuosUser(authUser, chairperson), 1)) {
      Assertions.assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testToggleRuleAsAdmin() {
    User admin = createUserWithRole(UserRoles.Admin());

    try (var response = resource.toggleRule(new DuosUser(authUser, admin), 1, 1)) {
      Assertions.assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testToggleRuleAsChair() {
    UserRole role = UserRoles.Chairperson();
    role.setDacId(1);
    User chairperson = createUserWithRole(role);
    when(dacService.findById(1)).thenReturn(new Dac());
    when(ruleService.toggleRule(1, 1, chairperson))
        .thenReturn(
            new AutomationRuleToggleResponse(
                1,
                true,
                Instant.now().toEpochMilli(),
                chairperson.getDisplayName(),
                chairperson.getEmail()));

    try (var response = resource.toggleRule(new DuosUser(authUser, chairperson), 1, 1)) {
      Assertions.assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testToggleRuleAsChairForbidden() {
    UserRole role = UserRoles.Chairperson();
    role.setDacId(2);
    User chairperson = createUserWithRole(role);

    try (var response = resource.toggleRule(new DuosUser(authUser, chairperson), 1, 1)) {
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
