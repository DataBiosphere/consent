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
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import java.time.Clock;
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
 * key. Each 429 logs a warning with the key, its source and the encoded path, so the keying can be
 * checked in a deployed environment. The log is console only: a 429 is expected behavior, not an
 * alert.
 *
 * <p>For a request that the DUOS BFF proxies, the load balancer's {@code X-Real-IP} is the DUOS
 * pod's egress address. The BFF signs the browser IP instead, and {@link SignedClientIpVerifier}
 * checks it. A verified IP is the key. A rejected one logs one warning with the reason only, and
 * the key falls back to {@code X-Real-IP}.
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
  private final SignedClientIpVerifier signedClientIpVerifier;
  private final LoadingCache<String, Bucket> buckets;

  @Context private HttpServletRequest servletRequest;

  @Inject
  public PublicRateLimitFilter(RateLimitConfiguration config) {
    this(config, Clock.systemUTC());
  }

  private PublicRateLimitFilter(RateLimitConfiguration config, Clock clock) {
    PublicRateLimitConfiguration showcase = config.getShowcase();
    this.clientIpResolver = new ClientIpResolver(showcase.getTrustedProxies());
    this.signedClientIpVerifier =
        new SignedClientIpVerifier(
            showcase.getClientIpSecret(), showcase.getClientIpPreviousSecret(), clock);
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
    this(config, servletRequest, Clock.systemUTC());
  }

  PublicRateLimitFilter(
      RateLimitConfiguration config, HttpServletRequest servletRequest, Clock clock) {
    this(config, clock);
    this.servletRequest = servletRequest;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    UriInfo uriInfo = requestContext.getUriInfo();
    // Logged as the caller sent it (still encoded), so an encoded %0A or %0D stays visible in the
    // log. Route matching uses the decoded segments instead.
    String path = uriInfo.getPath(false);
    if (!isLimitedPath(uriInfo)) {
      return;
    }
    ClientIp clientIp = resolveClientIp(requestContext);
    ConsumptionProbe probe = buckets.getUnchecked(clientIp.key()).tryConsumeAndReturnRemaining(1);
    if (!probe.isConsumed()) {
      logWarn(
          "Showcase rate limit exceeded: key=%s, source=%s, path=%s"
              .formatted(clientIp.key(), clientIp.source(), path));
      requestContext.abortWith(RateLimitFilter.tooManyRequests(probe));
    }
  }

  private ClientIp resolveClientIp(ContainerRequestContext requestContext) {
    SignedClientIpVerifier.Result signed =
        signedClientIpVerifier.verify(requestContext.getHeaders());
    if (signed.clientIp() != null) {
      return clientIpResolver.resolveSigned(signed.clientIp());
    }
    if (signed.rejection() != null) {
      logWarn("Showcase signed client IP rejected: reason=" + signed.rejection());
    }
    List<String> realIp =
        Objects.requireNonNullElse(requestContext.getHeaders().get(REAL_IP_HEADER), List.of());
    return clientIpResolver.resolve(servletRequest.getRemoteAddr(), realIp);
  }

  /**
   * True when the first non-empty path segment is {@code showcase}. Jersey matches resources on
   * segments without their matrix parameters, so {@code showcase;x=1/datasets} reaches the {@code
   * showcase/datasets} resource; a check on the raw path would let that request skip the limit.
   */
  private static boolean isLimitedPath(UriInfo uriInfo) {
    return uriInfo.getPathSegments().stream()
        .map(PathSegment::getPath)
        .filter(segment -> !segment.isEmpty())
        .findFirst()
        .map(LIMITED_PATH::equals)
        .orElse(false);
  }
}
