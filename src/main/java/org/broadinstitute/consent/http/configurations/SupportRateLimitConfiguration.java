package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Anonymous per-client-IP limit for the open {@code support/} routes: the Contact Us form and its
 * attachments, which need no DUOS account. Like the showcase limit, it is on in every environment
 * and keyed the same way (see {@link PublicRateLimitConfiguration} for the trusted proxies). Its
 * {@code requestsPerMinute} is for the whole deployment, split across pods with the parent {@link
 * RateLimitConfiguration#getPodCount()}. Unlike the other limits it is not required to be at least
 * {@code podCount}, so a small default can't stop a larger deployment from starting: each pod then
 * allows at least one request a minute.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupportRateLimitConfiguration {

  // The form sends one upload per attachment and then the request, and a retry sends them all
  // again: this leaves room for about ten attachments and a retry.
  public static final int DEFAULT_REQUESTS_PER_MINUTE = 30;

  @NotNull @Positive private Integer requestsPerMinute = DEFAULT_REQUESTS_PER_MINUTE;

  public int getRequestsPerMinute() {
    return requestsPerMinute;
  }

  public void setRequestsPerMinute(int requestsPerMinute) {
    this.requestsPerMinute = requestsPerMinute;
  }
}
