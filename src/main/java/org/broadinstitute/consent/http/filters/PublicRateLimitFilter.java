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
import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.configurations.PublicRateLimitConfiguration;
import org.broadinstitute.consent.http.configurations.RateLimitConfiguration;
import org.broadinstitute.consent.http.filters.ClientIpResolver.ClientIp;
import org.broadinstitute.consent.http.util.ConsentLogger;

/**
 * Token-bucket rate limiter for the anonymous public {@code showcase/} and {@code support/} routes,
 * and the signed-in {@code api/support/} routes, keyed by client IP. Each route group has its own
 * limit and buckets, so showcase browsing never uses up the support form's allowance. It is
 * separate from {@link RateLimitFilter}, which skips every request without a principal, and it does
 * not read that filter's {@code enabled} switch: this limit is on in every environment.
 *
 * <p>The key comes from {@link ClientIpResolver}. It believes the load balancer's {@code X-Real-IP}
 * only when the socket peer is a configured trusted proxy, so a direct caller cannot choose its own
 * key. Each 429 logs a warning with the key, its source and the encoded path, so the keying can be
 * checked in a deployed environment. The log is console only: a 429 is expected behavior, not an
 * alert.
 *
 * <p>For a request that the DUOS BFF proxies, the load balancer's {@code X-Real-IP} is the DUOS
 * pod's egress address. The BFF signs the browser IP instead, and {@link SignedClientIpVerifier}
 * checks it. A verified IP is the key. A rejected one falls back to {@code X-Real-IP}, and its
 * reason (never a header value) is added to the 429 log line.
 *
 * <p>Buckets live in-process, as in {@link RateLimitFilter}: each pod enforces {@code
 * requestsPerMinute / podCount}. The filter is {@code @PreMatching}, so it also counts requests to
 * unknown {@code showcase/} and {@code support/} paths and rejects before any body is read. It
 * returns early for every other path, so Kubernetes probes on {@code /status} and {@code /liveness}
 * never reach a bucket.
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class PublicRateLimitFilter implements ContainerRequestFilter, ConsentLogger {

  static final String REAL_IP_HEADER = "X-Real-IP";
  private static final String SHOWCASE_PATH = "showcase";
  private static final String SUPPORT_PATH = "support";
  private static final String API_PATH = "api";
  // Larger than RateLimitFilter's cap: anonymous keys are cheap for a caller to rotate, and an
  // evicted bucket starts full again.
  private static final int MAX_TRACKED_KEYS = 100_000;
  private static final int IDLE_EVICTION_MINUTES = 10;

  private final ClientIpResolver clientIpResolver;
  private final SignedClientIpVerifier signedClientIpVerifier;
  // Keyed by the first path segment of the limited route group.
  private final Map<String, LimitedRoute> routes;

  private record LimitedRoute(String label, LoadingCache<String, Bucket> buckets) {}

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
            config.getClientIpSecret(), config.getClientIpPreviousSecret(), clock);
    this.routes =
        Map.of(
            SHOWCASE_PATH,
            new LimitedRoute(
                "Showcase", buckets(showcase.getRequestsPerMinute(), config.getPodCount())),
            SUPPORT_PATH,
            new LimitedRoute(
                "Support",
                buckets(config.getSupport().getRequestsPerMinute(), config.getPodCount())));
  }

  private static LoadingCache<String, Bucket> buckets(int requestsPerMinute, int podCount) {
    int capacityPerPod = Math.max(1, (int) Math.ceil(requestsPerMinute / (double) podCount));
    Bandwidth limitPerPod =
        Bandwidth.builder()
            .capacity(capacityPerPod)
            .refillGreedy(capacityPerPod, Duration.ofMinutes(1))
            .build();
    return CacheBuilder.newBuilder()
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
    LimitedRoute route = limitedRoute(uriInfo);
    if (route == null) {
      return;
    }
    SignedClientIpVerifier.Result signed =
        signedClientIpVerifier.verify(requestContext.getHeaders());
    ClientIp clientIp =
        signed.clientIp() != null
            ? clientIpResolver.resolveSigned(signed.clientIp())
            : resolveFromRealIp(requestContext);
    ConsumptionProbe probe =
        route.buckets().getUnchecked(clientIp.key()).tryConsumeAndReturnRemaining(1);
    if (!probe.isConsumed()) {
      logWarn(limitExceededMessage(route.label(), clientIp, signed.rejection(), path));
      requestContext.abortWith(RateLimitFilter.tooManyRequests(probe));
    }
  }

  /**
   * The one log line per 429. A rejected signed header adds its reason here and writes no line of
   * its own, so a caller cannot add a WARN line to every request by sending a bad header.
   */
  private static String limitExceededMessage(
      String label, ClientIp clientIp, SignedClientIpVerifier.Rejection rejection, String path) {
    String signedRejection = rejection == null ? "" : ", signedRejection=" + rejection;
    return "%s rate limit exceeded: key=%s, source=%s%s, path=%s"
        .formatted(label, clientIp.key(), clientIp.source(), signedRejection, path);
  }

  private ClientIp resolveFromRealIp(ContainerRequestContext requestContext) {
    List<String> realIp =
        Objects.requireNonNullElse(requestContext.getHeaders().get(REAL_IP_HEADER), List.of());
    return clientIpResolver.resolve(servletRequest.getRemoteAddr(), realIp);
  }

  /**
   * The limited route group named by the first non-empty path segment, or null. {@code
   * api/support/...}, the signed-in support routes, counts as {@code support}, so a caller can't
   * get past the support limit by signing in with several accounts. The filter runs before
   * authentication, so every request on those paths counts, signed in or not. Jersey matches
   * resources on segments without their matrix parameters, so {@code showcase;x=1/datasets} reaches
   * the {@code showcase/datasets} resource; a check on the raw path would let that request skip the
   * limit.
   */
  private LimitedRoute limitedRoute(UriInfo uriInfo) {
    List<String> segments =
        uriInfo.getPathSegments().stream()
            .map(PathSegment::getPath)
            .filter(segment -> !segment.isEmpty())
            .limit(2)
            .toList();
    if (segments.isEmpty()) {
      return null;
    }
    if (segments.size() == 2 && API_PATH.equals(segments.get(0))) {
      return SUPPORT_PATH.equals(segments.get(1)) ? routes.get(SUPPORT_PATH) : null;
    }
    return routes.get(segments.get(0));
  }
}
