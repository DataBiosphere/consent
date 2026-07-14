package org.broadinstitute.consent.http.filters;

import com.google.inject.Inject;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import java.security.Principal;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.broadinstitute.consent.http.configurations.RateLimitConfiguration;
import org.broadinstitute.consent.http.models.AuthUser;

/**
 * Token-bucket rate limiter keyed by authenticated user (or by X-Forwarded-For for unauthenticated
 * requests). Buckets live in-process, so they are not shared across pods; {@code requestsPerMinute}
 * in {@link RateLimitConfiguration} is the intended limit for the whole deployment, and each pod
 * enforces its share of it, so the deployment-wide aggregate stays close to the configured value
 * instead of being multiplied by the pod count.
 */
@Provider
@Priority(Priorities.USER)
public class RateLimitFilter implements ContainerRequestFilter {

  private final RateLimitConfiguration config;
  private final Bandwidth limitPerPod;
  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  @Inject
  public RateLimitFilter(RateLimitConfiguration config) {
    this.config = config;
    int capacityPerPod =
        Math.max(1, (int) Math.ceil(config.getRequestsPerMinute() / (double) config.getPodCount()));
    this.limitPerPod =
        Bandwidth.builder()
            .capacity(capacityPerPod)
            .refillGreedy(capacityPerPod, Duration.ofMinutes(1))
            .build();
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (!config.isEnabled()) {
      return;
    }
    String key = extractKey(requestContext);
    Bucket bucket =
        buckets.computeIfAbsent(key, k -> Bucket.builder().addLimit(limitPerPod).build());
    if (!bucket.tryConsume(1)) {
      requestContext.abortWith(Response.status(429).header("Retry-After", "60").build());
    }
  }

  private String extractKey(ContainerRequestContext requestContext) {
    SecurityContext securityContext = requestContext.getSecurityContext();
    Principal principal = securityContext == null ? null : securityContext.getUserPrincipal();
    if (principal instanceof AuthUser authUser && authUser.getEmail() != null) {
      return authUser.getEmail();
    }
    String forwardedFor = requestContext.getHeaderString("X-Forwarded-For");
    return forwardedFor != null ? forwardedFor : "unknown";
  }
}
