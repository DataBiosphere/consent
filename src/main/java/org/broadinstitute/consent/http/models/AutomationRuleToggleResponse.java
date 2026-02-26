package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class AutomationRuleToggleResponse {

  @JsonProperty private final int ruleId;

  @JsonProperty private final boolean isRuleEnabled;

  @JsonProperty private final long enabledTime;

  @JsonProperty private final String displayName;

  @JsonProperty private final String email;

  public AutomationRuleToggleResponse(
      int id, boolean enabled, long enabledTime, String displayName, String email) {
    this.ruleId = id;
    this.isRuleEnabled = enabled;
    this.enabledTime = enabledTime;
    this.displayName = displayName;
    this.email = email;
  }

  public int getRuleId() {
    return ruleId;
  }

  public boolean isRuleEnabled() {
    return isRuleEnabled;
  }

  public long getEnabledTime() {
    return enabledTime;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getEmail() {
    return email;
  }
}
