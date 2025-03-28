package org.broadinstitute.consent.http.rules;

public record DACAutomationRule(Integer id, DACAutomationRuleType ruleType, String description, RuleState ruleState) {

}
