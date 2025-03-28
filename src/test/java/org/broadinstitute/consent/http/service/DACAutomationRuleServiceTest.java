package org.broadinstitute.consent.http.service;

import static org.mockito.Mockito.when;

import java.util.List;
import org.broadinstitute.consent.http.db.DACAutomationRuleDAO;
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
        new DACAutomationRule(1, DACAutomationRuleType.GRU_V1, "Test Rule", RuleState.AVAILABLE)));
    DACAutomationRuleService service = new DACAutomationRuleService(mockRuleDAO);

    List<DACAutomationRule> rules = service.findAll();
    Assertions.assertNotNull(rules);
    Assertions.assertFalse(rules.isEmpty());
  }

  @Test
  void testFindAllAvailable() {
    when(mockRuleDAO.findAllAvailable()).thenReturn(List.of(
        new DACAutomationRule(1, DACAutomationRuleType.GRU_V1, "Test Rule", RuleState.AVAILABLE)));
    DACAutomationRuleService service = new DACAutomationRuleService(mockRuleDAO);

    List<DACAutomationRule> rules = service.findAllAvailable();
    Assertions.assertNotNull(rules);
    Assertions.assertFalse(rules.isEmpty());
  }

}
