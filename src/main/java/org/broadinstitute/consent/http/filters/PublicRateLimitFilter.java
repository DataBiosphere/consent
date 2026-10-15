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
import org.broadinstitute.consent.http.filters.ClientIpResolver.ClientIp;
import org.broadinstitute.consent.http.util.ConsentLogger;

/**
 * Token-bucket rate limiter for the anonymous public {@code showcase/} routes, keyed by client IP.
 * It is separate from {@link RateLimitFilter}, which skips every request without a principal, and
 * it does not read that filter's {@code enabled} switch: this limit is on in every environment.
 *
 * <p>The key comes from {@link ClientIpResolver}. It believes the load balancer's {@code X-Real-IP}
 * only when the socket peer is a configured trusted proxy, so a direct caller cannot choose its own
 * key. Each 429 logs a warning with the key and its source, so the keying can be checked in a
 * deployed environment. The log is console only: a 429 is expected behavior, not an alert.
 *
 * <p>Buckets live in-process, as in {@link RateLimitFilter}: each pod enforces {@code
 * requestsPerMinute / podCount}. The filter is {@code @PreMatching}, so it also counts requests to
 * unknown {@code showcase/} paths and rejects before any body is read. It returns early for every
 * other path, so Kubernetes probes on {@code /status} and {@code /liveness} never reach a bucket.
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class PublicRateLimitFilter implements ContainerRequestFilter, ConsentLogger {

  static final String REAL_IP_HEADER = "X-Real-IP";
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
    List<String> realIp =
        Objects.requireNonNullElse(requestContext.getHeaders().get(REAL_IP_HEADER), List.of());
    ClientIp clientIp = clientIpResolver.resolve(servletRequest.getRemoteAddr(), realIp);
    ConsumptionProbe probe = buckets.getUnchecked(clientIp.key()).tryConsumeAndReturnRemaining(1);
    if (!probe.isConsumed()) {
      logWarn(
          "Showcase rate limit exceeded: key=%s, source=%s, path=%s"
              .formatted(clientIp.key(), clientIp.source(), path));
      requestContext.abortWith(RateLimitFilter.tooManyRequests(probe));
    }
  }
}
