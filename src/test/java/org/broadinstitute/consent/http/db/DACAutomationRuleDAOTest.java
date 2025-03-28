package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.rules.RuleState;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DACAutomationRuleDAOTest extends DAOTestHelper {

  @Test
  void testFindAll() {
    List<DACAutomationRule> rules = dacAutomationRuleDAO.findAll();
    Assertions.assertFalse(rules.isEmpty());
    Assertions.assertTrue(
        rules.stream().anyMatch(rule -> rule.ruleType().equals(DACAutomationRuleType.GRU_V1)));
  }

  @Test
  void testFindAllAvailable() {
    List<DACAutomationRule> rules = dacAutomationRuleDAO.findAllAvailable();
    Assertions.assertFalse(rules.isEmpty());
    Assertions.assertTrue(
        rules.stream().allMatch(rule -> rule.ruleState().equals(RuleState.AVAILABLE)));
  }

}
