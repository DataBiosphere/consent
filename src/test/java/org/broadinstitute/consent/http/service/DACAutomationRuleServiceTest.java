package org.broadinstitute.consent.http.service;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.List;
import org.broadinstitute.consent.http.db.DACAutomationRuleDAO;
import org.broadinstitute.consent.http.models.AutomationRuleToggleResponse;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.broadinstitute.consent.http.rules.RuleState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DACAutomationRuleServiceTest {

  @Mock
  private DACAutomationRuleDAO mockRuleDAO;

  @Test
  void testFindAll() {
    when(mockRuleDAO.findAll()).thenReturn(List.of(
        new DACAutomationRule(1, DACAutomationRuleType.GRU_V1, "Test Rule", RuleState.AVAILABLE,
            null, null, null)));
    DACAutomationRuleService service = new DACAutomationRuleService(mockRuleDAO);

    List<DACAutomationRule> rules = service.findAll();
    Assertions.assertNotNull(rules);
    Assertions.assertFalse(rules.isEmpty());
  }

  @Test
  void testFindById() {
    when(mockRuleDAO.findAllDACAutomationRulesByDACId(1)).thenReturn(List.of(
        new DACAutomationRule(1, DACAutomationRuleType.GRU_V1, "Test Rule", RuleState.AVAILABLE,
            null, null, null)));
    DACAutomationRuleService service = new DACAutomationRuleService(mockRuleDAO);
    List<DACAutomationRule> rules = service.findAllByDacId(1);
    Assertions.assertNotNull(rules);
    Assertions.assertFalse(rules.isEmpty());
  }

  @Test
  void testToggleRuleFromOffToOn() {
    when(mockRuleDAO.findAllDACAutomationRulesByDACId(1)).thenReturn(List.of(
        new DACAutomationRule(1, DACAutomationRuleType.GRU_V1, "Test Rule", RuleState.AVAILABLE,
            null, null, null)));
    when(mockRuleDAO.insertDACRuleSetting(1, 1, 1)).thenReturn(1);
    DACAutomationRuleService service = new DACAutomationRuleService(mockRuleDAO);
    AutomationRuleToggleResponse result = service.toggleRule(
        1, 1, 1);
    Assertions.assertTrue(result.isRuleEnabled());
    Assertions.assertEquals(1, (int) result.getRuleId());
  }

  @Test
  void testToggleRuleFromOnToOff() {
    when(mockRuleDAO.findAllDACAutomationRulesByDACId(1)).thenReturn(List.of(
        new DACAutomationRule(1, DACAutomationRuleType.GRU_V1, "Test Rule", RuleState.AVAILABLE,
            1, "alice", "alice@fake.org")));
    doNothing().when(mockRuleDAO).deleteDACRuleSetting(1, 1);
    DACAutomationRuleService service = new DACAutomationRuleService(mockRuleDAO);
    AutomationRuleToggleResponse result = service.toggleRule(1, 1, 1);
    Assertions.assertFalse(result.isRuleEnabled());
    Assertions.assertEquals(1, (int) result.getRuleId());
  }

}
