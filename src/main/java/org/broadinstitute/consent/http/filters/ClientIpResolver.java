package org.broadinstitute.consent.http.filters;

import com.google.common.net.InetAddresses;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.eclipse.jetty.util.InetAddressSet;

/**
 * Picks the client IP for an anonymous rate-limit key, so that a caller cannot choose its own key.
 *
 * <p>If the socket peer is a trusted proxy, the key is the {@code X-Real-IP} header. Otherwise the
 * key is the peer, and the header is ignored. In a deployed environment the peer is always the
 * Apache sidecar on loopback, so the header decides the key.
 *
 * <p>The Google load balancer sets {@code X-Real-IP} to the client address that it saw, through the
 * {@code customRequestHeaders} entry {@code X-Real-IP:{client_ip_address}} in the shared
 * terra-helmfile ingress chart's {@code BackendConfig}. Google documents that headers the load
 * balancer adds overwrite any client header with the same name, so a caller cannot spoof it. {@code
 * X-Forwarded-For} is not used: in dev on 2026-10-15 the load balancer logged the real client
 * address, but the {@code X-Forwarded-For} that reached the Apache sidecar held only a Google
 * front-end address.
 *
 * <p>Only the {@code gke} ingress class renders that {@code BackendConfig}. Behind another ingress,
 * such as the HAProxy ingress in BEEs, nothing overwrites a client-supplied {@code X-Real-IP}.
 *
 * <p>If the peer is trusted but the header is missing, repeated, or not a single IP literal, the
 * key falls back to the peer. All such requests then share one bucket, and {@link
 * KeySource#TRUSTED_PEER_WITHOUT_HEADER} in the 429 log shows the misconfiguration.
 *
 * <p>An IPv6 key is the address's /64 prefix, for example {@code 2001:db8::/64}. ISPs usually give
 * each subscriber at least a /64, so a key per full address would let one caller rotate through
 * addresses and get a fresh bucket for each request. IPv4 keys are full addresses. The Consent
 * ingresses had no IPv6 address on 2026-10-15, so this applies only if one is added.
 */
public class ClientIpResolver {

  static final String UNKNOWN = "unknown";
  private static final int IPV6_KEY_PREFIX_BYTES = 8;

  // An IP literal with an optional decimal prefix length. Jetty's InetAddressSet also accepts
  // hostnames (it resolves them with DNS), min-max ranges and a legacy wildcard form, and it parses
  // the prefix leniently, so each entry must match this before Jetty sees it.
  private static final Pattern PREFIX = Pattern.compile("\\d{1,3}");

  /** Where a rate-limit key came from. */
  public enum KeySource {
    /** The peer is not a trusted proxy, so the key is the peer. */
    UNTRUSTED_PEER,
    /** The peer is a trusted proxy, so the key is the {@code X-Real-IP} value. */
    REAL_IP_HEADER,
    /**
     * The peer is a trusted proxy, but it sent no usable {@code X-Real-IP}, so the key is the peer.
     */
    TRUSTED_PEER_WITHOUT_HEADER,
    /** The DUOS BFF signed the client IP, and {@link SignedClientIpVerifier} accepted it. */
    SIGNED_CLIENT_IP_HEADER
  }

  /** A rate-limit key and where it came from. */
  public record ClientIp(String key, KeySource source) {}

  private final InetAddressSet trustedProxies = new InetAddressSet();

  /**
   * @param trustedProxies IP address literals or CIDR ranges with no host bits set; hostnames and
   *     other range forms are rejected
   * @throws IllegalArgumentException if an entry is not an IP literal or valid CIDR range
   */
  public ClientIpResolver(List<String> trustedProxies) {
    for (String entry : trustedProxies) {
      String pattern = entry.strip();
      String[] parts = pattern.split("/", 2);
      if (!isLiteral(parts[0]) || (parts.length == 2 && !PREFIX.matcher(parts[1]).matches())) {
        throw new IllegalArgumentException("Not an IP literal or CIDR range: " + entry);
      }
      this.trustedProxies.add(pattern);
    }
  }

  /**
   * @param peerAddress the socket peer address
   * @param realIpValues each {@code X-Real-IP} header value, in received order
   * @return the normalized client key (an IPv4 address or an IPv6 /64 prefix) and its source
   */
  public ClientIp resolve(String peerAddress, List<String> realIpValues) {
    InetAddress peer = parse(peerAddress);
    if (peer == null) {
      return new ClientIp(peerAddress == null ? UNKNOWN : peerAddress, KeySource.UNTRUSTED_PEER);
    }
    String peerKey = key(peer);
    if (!isTrusted(peer)) {
      return new ClientIp(peerKey, KeySource.UNTRUSTED_PEER);
    }
    // The load balancer writes exactly one value. Anything else did not come from it.
    InetAddress client = realIpValues.size() == 1 ? parse(realIpValues.getFirst().strip()) : null;
    if (client == null) {
      return new ClientIp(peerKey, KeySource.TRUSTED_PEER_WITHOUT_HEADER);
    }
    return new ClientIp(key(client), KeySource.REAL_IP_HEADER);
  }

  /**
   * @param signedClientIp an IP literal that {@link SignedClientIpVerifier} accepted
   * @return the normalized key for that IP, whatever the peer is
   */
  public ClientIp resolveSigned(String signedClientIp) {
    return new ClientIp(
        key(InetAddresses.forString(signedClientIp)), KeySource.SIGNED_CLIENT_IP_HEADER);
  }

  private static String key(InetAddress address) {
    if (!(address instanceof Inet6Address)) {
      return InetAddresses.toAddrString(address);
    }
    byte[] bytes = address.getAddress();
    Arrays.fill(bytes, IPV6_KEY_PREFIX_BYTES, bytes.length, (byte) 0);
    try {
      return InetAddresses.toAddrString(InetAddress.getByAddress(bytes)) + "/64";
    } catch (UnknownHostException e) {
      // getByAddress throws only for an illegal array length, and this array has 16 bytes.
      throw new IllegalStateException(e);
    }
  }

  private boolean isTrusted(InetAddress address) {
    return trustedProxies.test(address);
  }

  /**
   * Parses an IP literal, or returns null. Jetty's {@code getRemoteAddr()} puts brackets around an
   * IPv6 peer ({@code [0:0:0:0:0:0:0:1]}), which Guava rejects, so one pair is removed first.
   */
  private static InetAddress parse(String value) {
    if (value == null) {
      return null;
    }
    String literal =
        value.length() > 1 && value.startsWith("[") && value.endsWith("]")
            ? value.substring(1, value.length() - 1)
            : value;
    return isLiteral(literal) ? InetAddresses.forString(literal) : null;
  }

  /**
   * True for an IP literal without a scope. Guava accepts a scoped IPv6 literal such as {@code
   * fe80::1%eth0}, but {@code forString} then throws for an unknown interface name. A scoped
   * (link-local) address is never a real client behind a load balancer, so it is rejected.
   */
  static boolean isLiteral(String value) {
    return value.indexOf('%') < 0 && InetAddresses.isInetAddress(value);
  }
}
