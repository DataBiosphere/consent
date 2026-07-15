package org.broadinstitute.consent.http.filters;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import java.security.Principal;
import java.time.Duration;
import org.broadinstitute.consent.http.configurations.RateLimitConfiguration;
import org.broadinstitute.consent.http.models.AuthUser;

/**
 * Token-bucket rate limiter keyed by authenticated user (or by X-Forwarded-For for unauthenticated
 * requests). Buckets live in-process, so they are not shared across pods; {@code requestsPerMinute}
 * in {@link RateLimitConfiguration} is the intended limit for the whole deployment, and each pod
 * enforces its share of it, so the deployment-wide aggregate stays close to the configured value
 * instead of being multiplied by the pod count.
 *
 * <p>Only applies to requests under {@code api/} — the actual API surface this is meant to protect.
 * Infrastructure endpoints like {@code /status} and {@code /liveness} are hit continuously by
 * Kubernetes probes and must never be throttled, and {@code swagger/} docs traffic isn't the abuse
 * surface this filter targets either.
 */
@Provider
@Priority(Priorities.USER)
public class RateLimitFilter implements ContainerRequestFilter {

  private static final String LIMITED_PATH_PREFIX = "api/";
  private static final int MAX_TRACKED_KEYS = 10_000;
  private static final int IDLE_EVICTION_MINUTES = 10;

  private final RateLimitConfiguration config;
  private final LoadingCache<String, Bucket> buckets;

  @Inject
  public RateLimitFilter(RateLimitConfiguration config) {
    this.config = config;
    int capacityPerPod =
        Math.max(1, (int) Math.ceil(config.getRequestsPerMinute() / (double) config.getPodCount()));
    Bandwidth limitPerPod =
        Bandwidth.builder()
            .capacity(capacityPerPod)
            .refillGreedy(capacityPerPod, Duration.ofMinutes(1))
            .build();
    this.buckets =
        CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(IDLE_EVICTION_MINUTES))
            .maximumSize(MAX_TRACKED_KEYS)
            .build(CacheLoader.from(key -> Bucket.builder().addLimit(limitPerPod).build()));
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (!config.isEnabled()) {
      return;
    }
    if (!requestContext.getUriInfo().getPath().startsWith(LIMITED_PATH_PREFIX)) {
      return;
    }
    String key = extractKey(requestContext);
    ConsumptionProbe probe = buckets.getUnchecked(key).tryConsumeAndReturnRemaining(1);
    if (!probe.isConsumed()) {
      long retryAfterSeconds =
          Math.max(1, Math.ceilDiv(probe.getNanosToWaitForRefill(), 1_000_000_000L));
      requestContext.abortWith(
          Response.status(429).header("Retry-After", String.valueOf(retryAfterSeconds)).build());
    }
  }

  private String extractKey(ContainerRequestContext requestContext) {
    SecurityContext securityContext = requestContext.getSecurityContext();
    Principal principal = securityContext == null ? null : securityContext.getUserPrincipal();
    if (principal instanceof AuthUser authUser
        && authUser.getEmail() != null
        && !authUser.getEmail().isBlank()) {
      return authUser.getEmail();
    }
    String forwardedFor = requestContext.getHeaderString("X-Forwarded-For");
    if (forwardedFor != null) {
      int commaIndex = forwardedFor.indexOf(',');
      String clientIp =
          (commaIndex >= 0 ? forwardedFor.substring(0, commaIndex) : forwardedFor).trim();
      if (!clientIp.isEmpty()) {
        return clientIp;
      }
    }
    return "unknown";
  }
}
