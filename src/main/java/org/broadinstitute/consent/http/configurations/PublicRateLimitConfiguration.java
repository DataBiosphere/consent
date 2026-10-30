package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.broadinstitute.consent.http.filters.ClientIpResolver;

/**
 * Anonymous per-client-IP limit for the public {@code showcase/} routes. It has no {@code enabled}
 * switch: it is on in every environment, and the authenticated limiter's {@code enabled} flag does
 * not affect it. Both values have defaults, so a deployment config without this block still gets
 * the limit.
 *
 * <p>{@code requestsPerMinute} is the intended limit per client IP for the whole deployment, split
 * across pods with the parent {@link RateLimitConfiguration#getPodCount()}, as for the
 * authenticated limiter.
 *
 * <p>{@code trustedProxies} lists the socket peers, as IP addresses or CIDR ranges, whose {@code
 * X-Real-IP} header Consent believes. The default trusts only loopback, which is the Apache sidecar
 * in the same pod. Deployed environments need no other entry: the Google load balancer sets {@code
 * X-Real-IP}, and the sidecar passes it through. See {@link ClientIpResolver}.
 *
 * <p>{@code clientIpSecret} and the optional {@code clientIpPreviousSecret} check the client IP
 * that the DUOS BFF signs; see {@link
 * org.broadinstitute.consent.http.filters.SignedClientIpVerifier}. Without a secret, BFF-proxied
 * requests share the bucket of the DUOS pod's egress address. Deployed environments set both values
 * from Google Secret Manager with {@code -Ddw.rateLimit.showcase.*} overrides. Each must be at
 * least 32 characters, and must never be the BFF's session secret.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicRateLimitConfiguration {

  // Initial pilot value, pending the lookup/load spike and the pilot-load budget (S2.2). A
  // showcase page view makes a few lookups, so this allows roughly 20+ page views per IP per
  // minute.
  public static final int DEFAULT_REQUESTS_PER_MINUTE = 120;

  @NotNull @Positive private Integer requestsPerMinute = DEFAULT_REQUESTS_PER_MINUTE;

  @NotNull private List<String> trustedProxies = List.of("127.0.0.1/32", "::1/128");

  private static final int MIN_CLIENT_IP_SECRET_LENGTH = 32;

  private String clientIpSecret;

  private String clientIpPreviousSecret;

  public int getRequestsPerMinute() {
    return requestsPerMinute;
  }

  public void setRequestsPerMinute(int requestsPerMinute) {
    this.requestsPerMinute = requestsPerMinute;
  }

  public List<String> getTrustedProxies() {
    return trustedProxies;
  }

  public void setTrustedProxies(List<String> trustedProxies) {
    this.trustedProxies = trustedProxies;
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
  @AssertTrue(message = "trustedProxies must be IP address literals or CIDR ranges, not hostnames")
  public boolean isTrustedProxiesValid() {
    if (trustedProxies == null) {
      return true;
    }
    try {
      new ClientIpResolver(trustedProxies);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
