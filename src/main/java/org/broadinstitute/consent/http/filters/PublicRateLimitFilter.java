package org.broadinstitute.consent.http.filters;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.configurations.PublicRateLimitConfiguration;
import org.broadinstitute.consent.http.configurations.RateLimitConfiguration;

/**
 * Token-bucket rate limiter for the anonymous public {@code showcase/} routes, keyed by client IP.
 * It is separate from {@link RateLimitFilter}, which skips every request without a principal, and
 * it does not read that filter's {@code enabled} switch: this limit is on in every environment.
 *
 * <p>The key comes from {@link ClientIpResolver}. It believes {@code X-Forwarded-For} only when the
 * socket peer is a configured trusted proxy, so a direct caller cannot choose its own key.
 *
 * <p>Buckets live in-process, as in {@link RateLimitFilter}: each pod enforces {@code
 * requestsPerMinute / podCount}. The filter is {@code @PreMatching}, so it also counts requests to
 * unknown {@code showcase/} paths and rejects before any body is read. It returns early for every
 * other path, so Kubernetes probes on {@code /status} and {@code /liveness} never reach a bucket.
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class PublicRateLimitFilter implements ContainerRequestFilter {

  static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
  private static final String LIMITED_PATH = "showcase";
  // Larger than RateLimitFilter's cap: anonymous keys are cheap for a caller to rotate, and an
  // evicted bucket starts full again.
  private static final int MAX_TRACKED_KEYS = 100_000;
  private static final int IDLE_EVICTION_MINUTES = 10;

  private final ClientIpResolver clientIpResolver;
  private final LoadingCache<String, Bucket> buckets;

  @Context private HttpServletRequest servletRequest;

  @Inject
  public PublicRateLimitFilter(RateLimitConfiguration config) {
    PublicRateLimitConfiguration showcase = config.getShowcase();
    this.clientIpResolver = new ClientIpResolver(showcase.getTrustedProxies());
    int capacityPerPod =
        Math.max(
            1, (int) Math.ceil(showcase.getRequestsPerMinute() / (double) config.getPodCount()));
    Bandwidth limitPerPod =
        Bandwidth.builder()
            .capacity(capacityPerPod)
            .refillGreedy(capacityPerPod, Duration.ofMinutes(1))
            .build();
    this.buckets =
        CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(IDLE_EVICTION_MINUTES))
            .maximumSize(MAX_TRACKED_KEYS)
            .build(CacheLoader.from(_ -> Bucket.builder().addLimit(limitPerPod).build()));
  }

  PublicRateLimitFilter(RateLimitConfiguration config, HttpServletRequest servletRequest) {
    this(config);
    this.servletRequest = servletRequest;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    String path = requestContext.getUriInfo().getPath();
    if (!path.equals(LIMITED_PATH) && !path.startsWith(LIMITED_PATH + "/")) {
      return;
    }
    List<String> forwardedFor =
        Objects.requireNonNullElse(
            requestContext.getHeaders().get(FORWARDED_FOR_HEADER), List.of());
    String key = clientIpResolver.resolve(servletRequest.getRemoteAddr(), forwardedFor);
    ConsumptionProbe probe = buckets.getUnchecked(key).tryConsumeAndReturnRemaining(1);
    if (!probe.isConsumed()) {
      requestContext.abortWith(RateLimitFilter.tooManyRequests(probe));
    }
  }
}
