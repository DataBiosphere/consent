package org.broadinstitute.consent.http.rules;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuleImplementationInterfaceTest {

  private static class TestRuleImplementation implements RuleImplementationInterface {
    @Override
    public DACAutomationRuleType getRuleType() {
      return DACAutomationRuleType.GRU_V1;
    }

    @Override
    public boolean compare(
        org.broadinstitute.consent.http.models.Dataset dataset,
        org.broadinstitute.consent.http.models.DataAccessRequest dataAccessRequest) {
      return false;
    }
  }

  @Test
  void testHasNoModifiersWithAiLlmUse() {
    TestRuleImplementation rule = new TestRuleImplementation();
    DataUse dataUse = new DataUse();

    assertTrue(rule.hasNoModifiers(dataUse));

    dataUse.setAiLlmUse(true);
    assertFalse(rule.hasNoModifiers(dataUse));

    dataUse.setAiLlmUse(false);
    assertTrue(rule.hasNoModifiers(dataUse));
  }

  @Test
  void testSecondaryConditionChecksWithAiLlmUse() {
    TestRuleImplementation rule = new TestRuleImplementation();
    DataAccessRequestData data = new DataAccessRequestData();

    assertTrue(rule.secondaryConditionChecks(data));

    data.setAiLlmUse(true);
    assertFalse(rule.secondaryConditionChecks(data));

    data.setAiLlmUse(false);
    assertTrue(rule.secondaryConditionChecks(data));
  }

  @Test
  void testRequestIsOnlyHMBWithAiLlmUse() {
    TestRuleImplementation rule = new TestRuleImplementation();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setHmb(true);

    assertTrue(rule.requestIsOnlyHMB(data));

    data.setAiLlmUse(true);
    assertFalse(rule.requestIsOnlyHMB(data));
  }

  @Test
  void testRequestHasDiseasesWithAiLlmUse() {
    TestRuleImplementation rule = new TestRuleImplementation();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setDiseases(true);
    data.setOntologies(
        java.util.List.of(new org.broadinstitute.consent.http.models.OntologyEntry()));

    assertTrue(rule.requestHasDiseases(data));

    data.setAiLlmUse(true);
    assertFalse(rule.requestHasDiseases(data));
  }

  @Test
  void testEnsureEachRuleHasImplementation() {
    for (DACAutomationRuleType value : DACAutomationRuleType.values()) {
      assertThat(
          Rules.implementationList.stream().filter(r -> r.getRuleType() == value).toList(),
          not(empty()));
    }
  }
}
