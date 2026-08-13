package org.broadinstitute.consent.http.rules;

/** A single DAC-to-rule pairing: this DAC currently has this automation rule enabled. */
public record DACRuleAssignment(Integer dacId, DACAutomationRuleType ruleType) {}
