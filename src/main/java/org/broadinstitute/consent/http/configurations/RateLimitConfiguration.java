package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Rate limit buckets are in-memory per pod, not shared across the deployment. {@code
 * requestsPerMinute} is the intended limit for the whole deployment; each pod enforces {@code
 * requestsPerMinute / podCount} so the aggregate stays close to that value. {@code
 * requestsPerMinute} must be at least {@code podCount}, otherwise every pod would round its share
 * up to a 1-request-per-minute floor and the deployment-wide aggregate would exceed the configured
 * limit.
 *
 * <p>{@code showcase} configures the separate anonymous limit for the public {@code showcase/}
 * routes. It ignores {@code enabled} and {@code requestsPerMinute}, and shares {@code podCount}.
 *
 * <p>{@code clientIpSecret} and the optional {@code clientIpPreviousSecret} check the client IP
 * that the DUOS BFF signs for a request that it proxies; see {@link
 * org.broadinstitute.consent.http.filters.SignedClientIpVerifier}. They are not specific to one
 * route: any anonymous per-IP limiter can use them. A verified IP is only a rate-limit key, never a
 * reason to grant access. Without a secret, BFF-proxied requests share the bucket of the DUOS pod's
 * egress address. Deployed environments set both values from Google Secret Manager with {@code
 * -Ddw.rateLimit.clientIpSecret} and {@code -Ddw.rateLimit.clientIpPreviousSecret}. Each must be at
 * least 32 characters, and must never be the BFF's session secret.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RateLimitConfiguration {

  @NotNull private Boolean enabled;

  @NotNull @Positive private Integer requestsPerMinute;

  @NotNull @Positive private Integer podCount;

  @Valid @NotNull
  private PublicRateLimitConfiguration showcase = new PublicRateLimitConfiguration();

  private static final int MIN_CLIENT_IP_SECRET_LENGTH = 32;

  private String clientIpSecret;

  private String clientIpPreviousSecret;

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

  public PublicRateLimitConfiguration getShowcase() {
    return showcase;
  }

  public void setShowcase(PublicRateLimitConfiguration showcase) {
    this.showcase = showcase;
  }

  public String getClientIpSecret() {
    return clientIpSecret;
  }

  public void setClientIpSecret(String clientIpSecret) {
    this.clientIpSecret = clientIpSecret;
  }

  public String getClientIpPreviousSecret() {
    return clientIpPreviousSecret;
  }

  public void setClientIpPreviousSecret(String clientIpPreviousSecret) {
    this.clientIpPreviousSecret = clientIpPreviousSecret;
  }

  @SuppressWarnings("unused") // invoked reflectively by Bean Validation via @AssertTrue
  @AssertTrue(
      message =
          "clientIpSecret and clientIpPreviousSecret must each be unset or at least 32 characters, "
              + "and clientIpPreviousSecret needs a clientIpSecret")
  public boolean isClientIpSecretValid() {
    boolean hasCurrent = isConfigured(clientIpSecret);
    boolean hasPrevious = isConfigured(clientIpPreviousSecret);
    return isLongEnough(clientIpSecret)
        && isLongEnough(clientIpPreviousSecret)
        && (hasCurrent || !hasPrevious);
  }

  private static boolean isConfigured(String value) {
    return value != null && !value.isBlank();
  }

  private static boolean isLongEnough(String value) {
    return !isConfigured(value) || value.strip().length() >= MIN_CLIENT_IP_SECRET_LENGTH;
  }

  @SuppressWarnings("unused") // invoked reflectively by Bean Validation via @AssertTrue
  @AssertTrue(
      message =
          "requestsPerMinute must be >= podCount, or the per-pod share rounds up to a "
              + "1-request-per-minute floor and the deployment-wide aggregate exceeds the configured limit")
  public boolean isRequestsPerMinuteAtLeastPodCount() {
    return requestsPerMinute == null || podCount == null || requestsPerMinute >= podCount;
  }

  @SuppressWarnings("unused") // invoked reflectively by Bean Validation via @AssertTrue
  @AssertTrue(
      message =
          "showcase.requestsPerMinute must be >= podCount, for the same reason as requestsPerMinute")
  public boolean isShowcaseRequestsPerMinuteAtLeastPodCount() {
    return showcase == null || podCount == null || showcase.getRequestsPerMinute() >= podCount;
  }
}
