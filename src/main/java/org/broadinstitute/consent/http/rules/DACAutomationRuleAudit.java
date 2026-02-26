package org.broadinstitute.consent.http.rules;

import java.sql.Timestamp;

public record DACAutomationRuleAudit(
    RuleAuditAction action,
    Timestamp actionDate,
    DACAutomationRuleType rule,
    String email,
    String displayName) {}
