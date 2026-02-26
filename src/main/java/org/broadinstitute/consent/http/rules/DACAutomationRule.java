package org.broadinstitute.consent.http.rules;

import java.sql.Timestamp;

public record DACAutomationRule(
    Integer id,
    DACAutomationRuleType ruleType,
    String description,
    RuleState ruleState,
    Timestamp activationDate,
    Integer enabledByUserId,
    String displayName,
    String userEmail) {}
