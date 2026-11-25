package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Per-client-IP limit for the support form: the open {@code support/} routes, which need no DUOS
 * account, and the signed-in {@code api/support/} routes, which share the same allowance. Like the
 * showcase limit, it is on in every environment and keyed the same way (see {@link
 * PublicRateLimitConfiguration} for the trusted proxies). Unlike the other limits, its {@code
 * requestsPerMinute} applies on each pod and is not split across them: one person's form, its
 * attachments and a retry can all reach one pod, and a per-pod share would refuse them there. Each
 * pod gives a client IP a burst of {@code requestsPerMinute}, then refills at {@code
 * requestsPerMinute} a minute, so one IP can make almost twice that in any minute that starts with
 * a full bucket on a pod, and {@code n} times as many across {@code n} pods.
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
