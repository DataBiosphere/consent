package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Rate limit buckets are in-memory per pod, not shared across the deployment. {@code
 * requestsPerMinute} is the intended limit for the whole deployment; each pod enforces {@code
 * requestsPerMinute / podCount} so the aggregate stays close to that value.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RateLimitConfiguration {

  private boolean enabled = true;

  private int requestsPerMinute = 100;

  private int podCount = 3;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getRequestsPerMinute() {
    return requestsPerMinute;
  }

  public void setRequestsPerMinute(int requestsPerMinute) {
    this.requestsPerMinute = requestsPerMinute;
  }

  public int getPodCount() {
    return podCount;
  }

  public void setPodCount(int podCount) {
    this.podCount = podCount;
  }
}
