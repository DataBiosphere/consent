package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class AutomationRuleToggleResponse {
  @JsonProperty
  private final int ruleId;

  @JsonProperty
  private final boolean isRuleEnabled;

  public AutomationRuleToggleResponse(int id, boolean enabled) {
    ruleId = id;
    isRuleEnabled = enabled;
  }

  public int getRuleId() {
    return ruleId;
  }

  public boolean isRuleEnabled() {
    return isRuleEnabled;
  }
}
